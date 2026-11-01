package com.fsociety.flarebackend.issue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.issue.dto.Priority;
import com.fsociety.flarebackend.issue.dto.PriorityReason;
import com.fsociety.flarebackend.issue.dto.PriorityReasonCode;

/**
 * The priority of an Issue, computed when the list is read (pipeline.md section 6). Pure; the weights come
 * from {@code flare.priority}. CRITICAL's weight is far above any other total, so Critical always sorts first.
 *
 * <pre>
 * score = severityWeight + perExtraReporter × min(reporters − 1, maxExtraReporters)
 *       + min(minutesSinceFirstReport, maxAgeMinutes) + perCriticality × location.criticality
 *       + (reopened ? reopenedBonus : 0)
 * </pre>
 */
public final class PriorityCalculator {

	/** A waiting time shown as a reason from this many minutes. */
	static final int WAITING_REASON_MINUTES = 10;

	private final Map<Severity, Integer> severityWeight = new EnumMap<>(Severity.class);
	private final FlareProperties.Priority weights;

	/** Throws if a Severity has no weight, so a gap in application.yml stops the app from starting. */
	public PriorityCalculator(FlareProperties.Priority weights) {
		for (Severity severity : Severity.values()) {
			Integer weight = weights.getSeverityWeight().get(severity.name());
			if (weight == null) {
				throw new IllegalArgumentException("flare.priority.severity-weight has no " + severity);
			}
			severityWeight.put(severity, weight);
		}
		this.weights = weights;
	}

	public Priority compute(Issue issue, Instant now) {
		int waitingMinutes = minutesBetween(issue.getFirstReportedAt(), now);
		int criticality = issue.getLocation() == null ? 0 : issue.getLocation().getCriticality();
		boolean reopened = issue.getReopenedCount() > 0;

		int score = severityWeight.get(issue.getSeverity())
				+ weights.getPerExtraReporter() * Math.min(Math.max(issue.getReporterCount() - 1, 0), weights.getMaxExtraReporters())
				+ Math.min(waitingMinutes, weights.getMaxAgeMinutes())
				+ weights.getPerCriticality() * criticality
				+ (reopened ? weights.getReopenedBonus() : 0);

		List<PriorityReason> reasons = new ArrayList<>();
		reasons.add(new PriorityReason(PriorityReasonCode.SEVERITY, issue.getSeverity(), null, null));
		if (issue.isEmergency()) {
			reasons.add(PriorityReason.of(PriorityReasonCode.EMERGENCY));
		}
		if (issue.getReporterCount() >= 2) {
			reasons.add(new PriorityReason(PriorityReasonCode.REPORTERS, null, issue.getReporterCount(),
					minutesBetween(issue.getFirstReportedAt(), issue.getLastReportedAt())));
		}
		if (waitingMinutes >= WAITING_REASON_MINUTES) {
			reasons.add(new PriorityReason(PriorityReasonCode.WAITING, null, null, waitingMinutes));
		}
		if (criticality > 0) {
			reasons.add(PriorityReason.of(PriorityReasonCode.CRITICAL_LOCATION));
		}
		if (reopened) {
			reasons.add(PriorityReason.of(PriorityReasonCode.REOPENED));
		}
		return new Priority(score, List.copyOf(reasons));
	}

	/** Highest score first; on a tie, the Issue first reported earliest. */
	public Comparator<Issue> order(Instant now) {
		return Comparator.comparingInt((Issue issue) -> compute(issue, now).getScore()).reversed()
				.thenComparing(Issue::getFirstReportedAt);
	}

	private static int minutesBetween(Instant from, Instant to) {
		return (int) Math.max(0, Duration.between(from, to).toMinutes());
	}
}
