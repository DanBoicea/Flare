package com.fsociety.flarebackend.grouping;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.report.ReportRepository;

/** The worker: one retry, then a new Unclassified Issue; ungrouped Reports are picked up again on startup. */
class GroupingWorkerTest {

	private final ReportGrouper grouper = mock(ReportGrouper.class);
	private final ReportRepository reports = mock(ReportRepository.class);
	private final GroupingWorker worker = new GroupingWorker(grouper, reports);
	private final UUID reportId = UUID.randomUUID();

	@Test
	void aSuccessfulGroupingNeedsNoFallback() {
		worker.process(reportId);

		verify(grouper).group(reportId);
		verify(grouper, never()).groupAsUnclassified(reportId);
	}

	@Test
	void aFailureIsRetriedOnce() {
		doThrow(new IllegalStateException("db hiccup")).doNothing().when(grouper).group(reportId);

		worker.process(reportId);

		verify(grouper, times(2)).group(reportId);
		verify(grouper, never()).groupAsUnclassified(reportId);
	}

	@Test
	void twoFailuresMakeItANewUnclassifiedIssue() {
		doThrow(new IllegalStateException("broken")).when(grouper).group(reportId);

		worker.process(reportId);

		verify(grouper, times(2)).group(reportId);
		verify(grouper).groupAsUnclassified(reportId);
	}

	@Test
	void evenAFailedFallbackNeverThrowsOutOfTheWorker() {
		doThrow(new IllegalStateException("broken")).when(grouper).group(reportId);
		doThrow(new IllegalStateException("still broken")).when(grouper).groupAsUnclassified(reportId);

		worker.process(reportId);

		verify(grouper).groupAsUnclassified(reportId);
	}

	@Test
	void ungroupedReportsArePickedUpOnStartup() {
		Report waiting = new Report();
		waiting.setId(reportId);
		when(reports.findUngrouped()).thenReturn(List.of(waiting));
		doNothing().when(grouper).group(reportId);

		worker.start();
		try {
			verify(grouper, timeout(2000)).group(reportId);
		}
		finally {
			worker.stop();
		}
	}
}
