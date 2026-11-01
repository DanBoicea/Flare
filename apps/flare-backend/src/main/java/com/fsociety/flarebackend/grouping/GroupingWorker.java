package com.fsociety.flarebackend.grouping;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.report.ReportRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Turns accepted Reports into Issues off the request thread (pipeline.md section 5, ADR-0015): one thread,
 * one queue, so Reports are grouped one at a time and never race each other. On startup it re-queues every
 * accepted Report that has no Issue yet, so nothing is lost if the app stopped mid-way.
 */
@Slf4j
@Component
public class GroupingWorker {

	private final LinkedBlockingQueue<UUID> queue = new LinkedBlockingQueue<>();
	private final ExecutorService thread = Executors.newSingleThreadExecutor(runnable -> {
		Thread worker = new Thread(runnable, "grouping-worker");
		worker.setDaemon(true);
		return worker;
	});
	private final ReportGrouper grouper;
	private final ReportRepository reports;

	public GroupingWorker(ReportGrouper grouper, ReportRepository reports) {
		this.grouper = grouper;
		this.reports = reports;
	}

	/** Queues an accepted Report. Call it only after the Report's transaction has committed. */
	public void enqueue(UUID reportId) {
		queue.add(reportId);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void start() {
		var ungrouped = reports.findUngrouped();
		ungrouped.stream().map(Report::getId).forEach(queue::add);
		if (!ungrouped.isEmpty()) {
			log.info("Re-queued {} ungrouped reports on startup", ungrouped.size());
		}
		thread.submit(this::run);
	}

	@PreDestroy
	public void stop() {
		thread.shutdownNow();
	}

	private void run() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				process(queue.take());
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Each Report gets one retry; if that fails too, it becomes its own Unclassified Issue (pipeline.md section 5).
	 * Only if even that fails does it stay ungrouped, to be tried again on the next startup.
	 */
	void process(UUID reportId) {
		for (int attempt = 1; attempt <= 2; attempt++) {
			try {
				grouper.group(reportId);
				return;
			}
			catch (RuntimeException e) {
				// Only the exception type and the ID: messages could contain Passenger text from SQL errors.
				log.warn("Grouping report {} failed (attempt {}): {}", reportId, attempt, e.getClass().getName());
			}
		}
		try {
			grouper.groupAsUnclassified(reportId);
		}
		catch (RuntimeException e) {
			log.error("Report {} stays ungrouped ({}); it is retried on the next startup", reportId, e.getClass().getName());
		}
	}
}
