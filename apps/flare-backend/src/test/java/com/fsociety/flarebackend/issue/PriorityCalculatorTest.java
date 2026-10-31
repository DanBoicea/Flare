package com.fsociety.flarebackend.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.issue.dto.Priority;
import com.fsociety.flarebackend.issue.dto.PriorityReason;
import com.fsociety.flarebackend.issue.dto.PriorityReasonCode;
import com.fsociety.flarebackend.location.Location;
import org.junit.jupiter.api.Test;

/** The priority score and its reasons (pipeline.md section 6), with the weights from application.yml. */
class PriorityCalculatorTest {

	private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");

	private final PriorityCalculator calculator = new PriorityCalculator(new FlareProperties.Priority(
			Map.of("CRITICAL", 10000, "HIGH", 600, "MEDIUM", 300, "LOW", 100), 25, 10, 120, 50, 50));

	@Test
	void aFreshSingleReportScoresItsSeverityWeight() {
		Priority priority = calculator.compute(issue(Severity.MEDIUM, 1, 0, 0), NOW);

		assertThat(priority.getScore()).isEqualTo(300);
		assertThat(priority.getReasons()).extracting(PriorityReason::getCode).containsExactly(PriorityReasonCode.SEVERITY);
		assertThat(priority.getReasons().get(0).getSeverity()).isEqualTo(Severity.MEDIUM);
	}

	@Test
	void extraReportersAddUpToTheCap() {
		assertThat(calculator.compute(issue(Severity.LOW, 3, 0, 0), NOW).getScore()).isEqualTo(100 + 2 * 25);
		// 30 reporters count as 10 extra, not 29.
		assertThat(calculator.compute(issue(Severity.LOW, 30, 0, 0), NOW).getScore()).isEqualTo(100 + 10 * 25);
	}

	@Test
	void theReportersReasonGivesTheCountAndTheMinutesFromFirstToLastReport() {
		Issue issue = issue(Severity.MEDIUM, 3, 0, 0);
		issue.setFirstReportedAt(NOW.minus(Duration.ofMinutes(8)));
		issue.setLastReportedAt(NOW);

		PriorityReason reporters = reason(calculator.compute(issue, NOW), PriorityReasonCode.REPORTERS);

		assertThat(reporters.getCount()).isEqualTo(3);
		assertThat(reporters.getMinutes()).isEqualTo(8);
	}

	@Test
	void waitingAddsAMinuteAtATimeUpToTheCap() {
		assertThat(calculator.compute(issue(Severity.MEDIUM, 1, 0, 0, 45), NOW).getScore()).isEqualTo(300 + 45);
		assertThat(calculator.compute(issue(Severity.MEDIUM, 1, 0, 0, 500), NOW).getScore()).isEqualTo(300 + 120);
	}

	@Test
	void waitingIsAReasonFromTenMinutes() {
		assertThat(codes(calculator.compute(issue(Severity.MEDIUM, 1, 0, 0, 9), NOW))).doesNotContain(PriorityReasonCode.WAITING);

		PriorityReason waiting = reason(calculator.compute(issue(Severity.MEDIUM, 1, 0, 0, 10), NOW), PriorityReasonCode.WAITING);
		assertThat(waiting.getMinutes()).isEqualTo(10);
	}

	@Test
	void criticalLocationsAndReopenedIssuesScoreHigher() {
		Priority priority = calculator.compute(issue(Severity.MEDIUM, 1, 2, 1), NOW);

		assertThat(priority.getScore()).isEqualTo(300 + 2 * 50 + 50);
		assertThat(codes(priority)).contains(PriorityReasonCode.CRITICAL_LOCATION, PriorityReasonCode.REOPENED);
	}

	@Test
	void emergenciesSayWhy() {
		Issue issue = issue(Severity.CRITICAL, 1, 0, 0);
		issue.setEmergency(true);

		assertThat(codes(calculator.compute(issue, NOW))).contains(PriorityReasonCode.EMERGENCY);
	}

	@Test
	void aCriticalIssueAlwaysOutranksEverythingElse() {
		Issue worstHigh = issue(Severity.HIGH, 50, 2, 3, 10_000);
		Issue freshCritical = issue(Severity.CRITICAL, 1, 0, 0, 0);

		assertThat(calculator.compute(freshCritical, NOW).getScore())
				.isGreaterThan(calculator.compute(worstHigh, NOW).getScore());
	}

	@Test
	void sortsByScoreThenOldestFirstReport() {
		Issue older = issue(Severity.MEDIUM, 1, 0, 0, 5);
		Issue newer = issue(Severity.MEDIUM, 1, 0, 0, 5);
		newer.setFirstReportedAt(older.getFirstReportedAt().plusSeconds(30));
		Issue critical = issue(Severity.CRITICAL, 1, 0, 0, 0);

		List<Issue> issues = new ArrayList<>(List.of(newer, critical, older));
		Comparator<Issue> order = calculator.order(NOW);
		issues.sort(order);

		assertThat(issues).containsExactly(critical, older, newer);
	}

	@Test
	void aMissingSeverityWeightStopsTheAppFromStarting() {
		assertThatThrownBy(() -> new PriorityCalculator(new FlareProperties.Priority(Map.of("CRITICAL", 1), 25, 10, 120, 50, 50)))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private static Issue issue(Severity severity, int reporters, int criticality, int reopenedCount) {
		return issue(severity, reporters, criticality, reopenedCount, 0);
	}

	private static Issue issue(Severity severity, int reporters, int criticality, int reopenedCount, int minutesOld) {
		Issue issue = new Issue();
		issue.setSeverity(severity);
		issue.setReporterCount(reporters);
		issue.setReportCount(reporters);
		issue.setReopenedCount(reopenedCount);
		issue.setFirstReportedAt(NOW.minus(Duration.ofMinutes(minutesOld)));
		issue.setLastReportedAt(NOW.minus(Duration.ofMinutes(minutesOld)));
		Location location = new Location();
		location.setCriticality((short) criticality);
		issue.setLocation(location);
		return issue;
	}

	private static List<PriorityReasonCode> codes(Priority priority) {
		return priority.getReasons().stream().map(PriorityReason::getCode).toList();
	}

	private static PriorityReason reason(Priority priority, PriorityReasonCode code) {
		return priority.getReasons().stream().filter(r -> r.getCode() == code).findFirst().orElseThrow();
	}
}
