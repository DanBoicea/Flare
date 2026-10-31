package com.fsociety.flarebackend.grouping;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.fsociety.flarebackend.issue.Issue;
import com.fsociety.flarebackend.issue.IssueStatus;
import com.fsociety.flarebackend.location.Location;

/**
 * Which Issues a new Report may join (pipeline.md section 5, step 3): Issues in the same Area that are open, or were
 * resolved within {@code flare.grouping.window}, the most recently updated first, at most
 * {@code flare.grouping.max-candidates}. A Report without a Location has no candidates. Pure: the query narrows the
 * list, and this class decides, so the rules live in one tested place.
 */
public final class CandidateSelector {

	private final Duration window;
	private final int maxCandidates;

	public CandidateSelector(Duration window, int maxCandidates) {
		this.window = window;
		this.maxCandidates = maxCandidates;
	}

	/** Issues resolved before this instant are no longer candidates. */
	public Instant resolvedSince(Instant now) {
		return now.minus(window);
	}

	public int maxCandidates() {
		return maxCandidates;
	}

	public List<Issue> select(Location reportLocation, List<Issue> issues, Instant now) {
		if (reportLocation == null || reportLocation.getArea() == null) {
			return List.of();
		}
		String areaId = reportLocation.getArea().getId();
		Instant resolvedSince = resolvedSince(now);
		return issues.stream()
				.filter(issue -> issue.getLocation() != null && issue.getLocation().getArea() != null
						&& Objects.equals(issue.getLocation().getArea().getId(), areaId))
				.filter(issue -> issue.getStatus() != IssueStatus.RESOLVED
						|| (issue.getResolvedAt() != null && !issue.getResolvedAt().isBefore(resolvedSince)))
				.sorted(Comparator.comparing(Issue::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
				.limit(maxCandidates)
				.toList();
	}
}
