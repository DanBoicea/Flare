package com.fsociety.flarebackend.issue;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fsociety.flarebackend.audit.AuditAction;
import com.fsociety.flarebackend.audit.AuditService;
import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ErrorCode;
import com.fsociety.flarebackend.common.FlareException;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.issue.IssueTransitions.Action;
import com.fsociety.flarebackend.issue.dto.IssueDetail;
import com.fsociety.flarebackend.staff.StaffAccessPolicy;
import com.fsociety.flarebackend.staff.StaffMember;
import com.fsociety.flarebackend.staff.TeamId;

/**
 * Staff actions on an Issue (api.md "Issue actions", A05). Each one checks, in this order: the caller may act on
 * it (else 404, so IDs are never confirmed), the version the app saw is current (else 409 VERSION_CONFLICT), and
 * the action is allowed in this state (else 409 INVALID_TRANSITION). Then it applies the change, writes the audit
 * log and returns the new detail. Two requests with the same version can both pass the check; the second write
 * then fails on the {@code @Version} column, which {@code ApiExceptionHandler} also turns into VERSION_CONFLICT.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueActionService {

	private final IssueRepository issues;
	private final AuditService audit;
	private final IssueQueryService queries;
	private final Clock clock;

	@Transactional
	public IssueDetail acknowledge(StaffMember by, UUID issueId, long version) {
		Issue issue = load(by, issueId, version, Action.ACKNOWLEDGE);
		issue.setStatus(IssueStatus.ACKNOWLEDGED);
		issue.setAcknowledgedAt(clock.instant());
		return save(issue, by, AuditAction.ACKNOWLEDGED, Map.of());
	}

	/** "I'm on it". Taking a NEW Issue acknowledges it too. */
	@Transactional
	public IssueDetail take(StaffMember by, UUID issueId, long version) {
		Issue issue = load(by, issueId, version, Action.TAKE);
		issue.setStatus(IssueStatus.IN_PROGRESS);
		issue.setTakenBy(by);
		if (issue.getAcknowledgedAt() == null) {
			issue.setAcknowledgedAt(clock.instant());
		}
		return save(issue, by, AuditAction.TAKEN, Map.of());
	}

	@Transactional
	public IssueDetail resolve(StaffMember by, UUID issueId, long version) {
		Issue issue = load(by, issueId, version, Action.RESOLVE);
		issue.setStatus(IssueStatus.RESOLVED);
		issue.setResolvedAt(clock.instant());
		return save(issue, by, AuditAction.RESOLVED, Map.of());
	}

	@Transactional
	public IssueDetail reopen(StaffMember by, UUID issueId, long version) {
		Issue issue = load(by, issueId, version, Action.REOPEN);
		issue.setStatus(IssueStatus.NEW);
		issue.setReopenedCount(issue.getReopenedCount() + 1);
		issue.setTakenBy(null);
		issue.setResolvedAt(null);
		return save(issue, by, AuditAction.REOPENED, Map.of());
	}

	/**
	 * Staff override of category and/or Severity. The {@code *_overridden} flag sticks, so later Reports don't
	 * change the value back. Only real changes are written to the history. The Team does not follow the category;
	 * moving an Issue is a hand over.
	 */
	@Transactional
	public IssueDetail override(StaffMember by, UUID issueId, long version, Category category, Severity severity) {
		if (category == null && severity == null) {
			throw new FlareException(ErrorCode.INVALID_REQUEST);
		}
		Issue issue = load(by, issueId, version, Action.OVERRIDE);
		Category oldCategory = issue.getCategory();
		Severity oldSeverity = issue.getSeverity();
		if (category != null) {
			issue.setCategory(category);
			issue.setCategoryOverridden(true);
		}
		if (severity != null) {
			issue.setSeverity(severity);
			issue.setSeverityOverridden(true);
		}
		issues.saveAndFlush(issue);
		if (category != null && category != oldCategory) {
			audit.record(issue, null, by, AuditAction.CATEGORY_CHANGED, fromTo(oldCategory, category));
		}
		if (severity != null && severity != oldSeverity) {
			audit.record(issue, null, by, AuditAction.SEVERITY_CHANGED, fromTo(oldSeverity, severity));
		}
		log.info("Issue {} overridden by staff member {}", issue.getId(), by.getId());
		return queries.detailOf(issue);
	}

	/** Moves the Issue to another Team and clears "I'm on it"; the status stays. Handing over to the same Team is refused. */
	@Transactional
	public IssueDetail handOver(StaffMember by, UUID issueId, long version, TeamId team) {
		Issue issue = load(by, issueId, version, Action.HAND_OVER);
		if (team == issue.getTeam()) {
			throw new FlareException(ErrorCode.INVALID_TRANSITION);
		}
		TeamId oldTeam = issue.getTeam();
		issue.setTeam(team);
		issue.setTakenBy(null);
		return save(issue, by, AuditAction.HANDED_OVER, fromTo(oldTeam, team));
	}

	private Issue load(StaffMember by, UUID issueId, long version, Action action) {
		Issue issue = issues.findById(issueId)
				.filter(found -> StaffAccessPolicy.canAct(by, found))
				.orElseThrow(() -> new FlareException(ErrorCode.NOT_FOUND));
		if (issue.getVersion() != version) {
			throw new FlareException(ErrorCode.VERSION_CONFLICT);
		}
		if (!IssueTransitions.allowed(action, issue.getStatus(), issue.getTakenBy() != null)) {
			throw new FlareException(ErrorCode.INVALID_TRANSITION);
		}
		return issue;
	}

	/** Flushes inside the transaction, so a concurrent change fails here (409) before the history is written. */
	private IssueDetail save(Issue issue, StaffMember by, AuditAction action, Map<String, ?> details) {
		issues.saveAndFlush(issue);
		audit.record(issue, null, by, action, details);
		log.info("Issue {} {} by staff member {}", issue.getId(), action, by.getId());
		return queries.detailOf(issue);
	}

	/** {@code from} may be null (an unrouted Issue handed to a Team), so not Map.of. */
	private static Map<String, Object> fromTo(Object from, Object to) {
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("from", from == null ? null : from.toString());
		details.put("to", Objects.toString(to));
		return details;
	}
}
