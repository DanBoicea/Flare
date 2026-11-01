package com.fsociety.flarebackend.issue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fsociety.flarebackend.audit.AuditEventRepository;
import com.fsociety.flarebackend.common.ErrorCode;
import com.fsociety.flarebackend.common.FlareException;
import com.fsociety.flarebackend.issue.dto.IssueCard;
import com.fsociety.flarebackend.issue.dto.IssueDetail;
import com.fsociety.flarebackend.issue.dto.IssueList;
import com.fsociety.flarebackend.issue.dto.ReportView;
import com.fsociety.flarebackend.report.ReportRepository;
import com.fsociety.flarebackend.report.ReportStatus;
import com.fsociety.flarebackend.staff.StaffAccessPolicy;
import com.fsociety.flarebackend.staff.StaffMember;

/**
 * The Staff app's Issue list and detail (api.md, A04). Visibility is decided by {@link StaffAccessPolicy} in Java,
 * never in the queries, so the rules live in one tested place. Demo scale: the lists are filtered in memory.
 */
@Service
@RequiredArgsConstructor
public class IssueQueryService {

	/** The RESOLVED list: Issues resolved in the last 24 hours, newest first, at most 50 (api.md, the contract). */
	static final Duration RESOLVED_WINDOW = Duration.ofHours(24);
	static final int RESOLVED_MAX = 50;
	/** Reports in the detail view, newest first (the contract's {@code maxItems}). */
	static final int REPORTS_MAX = 50;

	private final IssueRepository issues;
	private final ReportRepository reports;
	private final AuditEventRepository auditEvents;
	private final PriorityCalculator priority;
	private final Clock clock;

	@Transactional(readOnly = true)
	public IssueList list(StaffMember member, IssueView view) {
		Instant now = clock.instant();
		Stream<IssueCard> cards;
		if (view == IssueView.ACTIVE) {
			cards = issues.findActive().stream()
					.filter(issue -> StaffAccessPolicy.canSee(member, issue))
					.sorted(priority.order(now))
					.map(issue -> IssueCards.card(issue, priority.compute(issue, now)));
		}
		else {
			// The cap is applied after filtering, so other Teams' Issues never shorten this member's list.
			cards = issues.findByStatusAndResolvedAtAfterOrderByResolvedAtDesc(IssueStatus.RESOLVED,
							now.minus(RESOLVED_WINDOW), Limit.unlimited()).stream()
					.filter(issue -> StaffAccessPolicy.canSee(member, issue))
					.limit(RESOLVED_MAX)
					.map(issue -> IssueCards.card(issue, priority.compute(issue, now)));
		}
		return new IssueList(now, cards.toList());
	}

	/** 404 both when the Issue doesn't exist and when the member can't see it, so IDs are never confirmed. */
	@Transactional(readOnly = true)
	public IssueDetail detail(StaffMember member, UUID issueId) {
		Issue issue = issues.findById(issueId)
				.filter(found -> StaffAccessPolicy.canSee(member, found))
				.orElseThrow(() -> new FlareException(ErrorCode.NOT_FOUND));
		return detailOf(issue);
	}

	/**
	 * The detail of an Issue the caller was already allowed to see or act on. Used after an action, when a hand
	 * over may have moved the Issue out of the caller's view. Runs in the caller's transaction.
	 */
	IssueDetail detailOf(Issue issue) {
		Instant now = clock.instant();
		List<ReportView> reportViews = reports
				.findByIssueIdAndStatusOrderByCreatedAtDesc(issue.getId(), ReportStatus.ACCEPTED, Limit.of(REPORTS_MAX)).stream()
				.map(IssueCards::report)
				.toList();
		return new IssueDetail(
				IssueCards.card(issue, priority.compute(issue, now)),
				reportViews,
				auditEvents.findByIssueIdOrderByCreatedAt(issue.getId()).stream().map(IssueCards::history).toList());
	}
}
