package com.fsociety.flarebackend.review;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fsociety.flarebackend.audit.AuditAction;
import com.fsociety.flarebackend.audit.AuditService;
import com.fsociety.flarebackend.common.ErrorCode;
import com.fsociety.flarebackend.common.FlareException;
import com.fsociety.flarebackend.grouping.GroupingWorker;
import com.fsociety.flarebackend.issue.dto.LocationRef;
import com.fsociety.flarebackend.location.QrCode;
import com.fsociety.flarebackend.location.QrCodeRepository;
import com.fsociety.flarebackend.ratelimit.QrCodeFloodGuard;
import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.report.ReportRepository;
import com.fsociety.flarebackend.report.ReportStatus;
import com.fsociety.flarebackend.review.dto.ReviewAlert;
import com.fsociety.flarebackend.review.dto.ReviewItem;
import com.fsociety.flarebackend.review.dto.ReviewList;
import com.fsociety.flarebackend.staff.StaffAccessPolicy;
import com.fsociety.flarebackend.staff.StaffMember;
import com.fsociety.flarebackend.staff.TeamId;

/**
 * Needs review (api.md, A09): everything held back, for the duty manager only. Quarantined Reports can be released
 * to staff (optionally to a chosen Team) or dismissed; Strict mode alerts are shown until they expire.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

	/** The list shows the newest Quarantined Reports, at most this many (A09). */
	static final int MAX_ITEMS = 100;

	private final ReportRepository reports;
	private final QrCodeRepository qrCodes;
	private final QrCodeFloodGuard floodGuard;
	private final AuditService audit;
	private final GroupingWorker groupingWorker;

	@Transactional(readOnly = true)
	public ReviewList list(StaffMember member) {
		requireReviewer(member);
		List<ReviewItem> items = reports.findQuarantined().stream()
				.limit(MAX_ITEMS)
				.map(report -> new ReviewItem(report.getId(), report.getText(), report.getCreatedAt(),
						report.getQuarantineReason(), report.getScannedQrCodeId(), LocationRef.of(report.getLocation())))
				.toList();
		List<ReviewAlert> alerts = floodGuard.activeAlerts().stream()
				.map(alert -> new ReviewAlert(alert.getQrCodeId(),
						qrCodes.findById(alert.getQrCodeId()).map(QrCode::getLocation).map(LocationRef::of).orElse(null),
						alert.getSince(), alert.getMessageCount()))
				.toList();
		return new ReviewList(items, alerts);
	}

	/**
	 * Sends a Quarantined Report to staff. A chosen Team is stored as {@code team_override}: the worker then skips
	 * matching and creates a new Issue for that Team. Without one, it is routed like any Report. The Report is
	 * queued only after this transaction commits.
	 */
	@Transactional
	public void release(StaffMember member, UUID reportId, TeamId team) {
		requireReviewer(member);
		Report report = quarantined(reportId);
		report.setStatus(ReportStatus.ACCEPTED);
		report.setTeamOverride(team);
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("reason", report.getQuarantineReason().name());
		details.put("team", team == null ? null : team.name());
		audit.record(null, report, member, AuditAction.REPORT_RELEASED, details);
		log.info("Report {} released by staff member {} (team {})", report.getId(), member.getId(), team);
		enqueueAfterCommit(report.getId());
	}

	@Transactional
	public void dismiss(StaffMember member, UUID reportId) {
		requireReviewer(member);
		Report report = quarantined(reportId);
		report.setStatus(ReportStatus.DISMISSED);
		audit.record(null, report, member, AuditAction.REPORT_DISMISSED, Map.of("reason", report.getQuarantineReason().name()));
		log.info("Report {} dismissed by staff member {}", report.getId(), member.getId());
	}

	private static void requireReviewer(StaffMember member) {
		if (!StaffAccessPolicy.canReview(member)) {
			throw new FlareException(ErrorCode.FORBIDDEN);
		}
	}

	/** Only a Report still in Needs review can be released or dismissed; anything else is 404. */
	private Report quarantined(UUID reportId) {
		return reports.findById(reportId)
				.filter(report -> report.getStatus() == ReportStatus.QUARANTINED)
				.orElseThrow(() -> new FlareException(ErrorCode.NOT_FOUND));
	}

	/** The worker must never see the Report before its new status is committed (backend.md, "Transactions"). */
	private void enqueueAfterCommit(UUID reportId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			groupingWorker.enqueue(reportId);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				groupingWorker.enqueue(reportId);
			}
		});
	}
}
