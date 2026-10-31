package com.fsociety.flarebackend.grouping;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.fsociety.flarebackend.issue.Issue;
import com.fsociety.flarebackend.issue.IssueStatus;
import com.fsociety.flarebackend.location.Area;
import com.fsociety.flarebackend.location.Location;

/** Grouping candidates (pipeline.md section 5, step 3): same Area, open or recently resolved, at most 10, newest first. */
class CandidateSelectorTest {

	private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");

	private final CandidateSelector selector = new CandidateSelector(Duration.ofMinutes(30), 10);
	private final Location arrivalsToilet = location("ARR");
	private final Location arrivalsBelt = location("ARR");
	private final Location departures = location("DEP");

	@Test
	void openIssuesInTheSameAreaAreCandidatesEvenAtAnotherLocation() {
		Issue sameLocation = issue(arrivalsToilet, IssueStatus.NEW, null, 5);
		Issue sameArea = issue(arrivalsBelt, IssueStatus.IN_PROGRESS, null, 10);
		Issue otherArea = issue(departures, IssueStatus.NEW, null, 1);

		assertThat(selector.select(arrivalsToilet, List.of(sameLocation, sameArea, otherArea), NOW))
				.containsExactly(sameLocation, sameArea);
	}

	@Test
	void issuesResolvedWithinTheWindowAreCandidatesOlderOnesAreNot() {
		Issue resolvedJustNow = issue(arrivalsToilet, IssueStatus.RESOLVED, NOW.minus(Duration.ofMinutes(30)), 1);
		Issue resolvedLongAgo = issue(arrivalsToilet, IssueStatus.RESOLVED, NOW.minus(Duration.ofMinutes(31)), 2);

		assertThat(selector.select(arrivalsToilet, List.of(resolvedJustNow, resolvedLongAgo), NOW))
				.containsExactly(resolvedJustNow);
	}

	@Test
	void atMostTenTheMostRecentlyUpdatedFirst() {
		List<Issue> many = new ArrayList<>();
		IntStream.range(0, 15).forEach(i -> many.add(issue(arrivalsToilet, IssueStatus.NEW, null, i)));

		List<Issue> selected = selector.select(arrivalsToilet, many, NOW);

		assertThat(selected).hasSize(10);
		assertThat(selected.get(0)).isSameAs(many.get(0));
		assertThat(selected).isSortedAccordingTo((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()));
	}

	@Test
	void aReportWithoutALocationHasNoCandidates() {
		Issue open = issue(arrivalsToilet, IssueStatus.NEW, null, 1);

		assertThat(selector.select(null, List.of(open), NOW)).isEmpty();
	}

	@Test
	void issuesWithoutALocationAreNeverCandidates() {
		Issue noLocation = issue(null, IssueStatus.NEW, null, 1);

		assertThat(selector.select(arrivalsToilet, List.of(noLocation), NOW)).isEmpty();
	}

	@Test
	void theQueryLimitsComeFromTheConfig() {
		assertThat(selector.resolvedSince(NOW)).isEqualTo(NOW.minus(Duration.ofMinutes(30)));
		assertThat(selector.maxCandidates()).isEqualTo(10);
	}

	private static Location location(String areaId) {
		Area area = new Area();
		area.setId(areaId);
		Location location = new Location();
		location.setId(UUID.randomUUID());
		location.setArea(area);
		return location;
	}

	private static Issue issue(Location location, IssueStatus status, Instant resolvedAt, int minutesSinceUpdate) {
		Issue issue = new Issue();
		issue.setId(UUID.randomUUID());
		issue.setLocation(location);
		issue.setStatus(status);
		issue.setResolvedAt(resolvedAt);
		issue.setUpdatedAt(NOW.minus(Duration.ofMinutes(minutesSinceUpdate)));
		return issue;
	}
}
