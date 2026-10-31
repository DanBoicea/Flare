package com.fsociety.flarebackend.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fsociety.flarebackend.audit.AuditAction;
import com.fsociety.flarebackend.audit.AuditService;
import com.fsociety.flarebackend.common.ErrorCode;
import com.fsociety.flarebackend.common.FlareException;
import com.fsociety.flarebackend.common.MutableClock;
import com.fsociety.flarebackend.grouping.GroupingWorker;
import com.fsociety.flarebackend.location.Location;
import com.fsociety.flarebackend.location.QrCode;
import com.fsociety.flarebackend.location.QrCodeRepository;
import com.fsociety.flarebackend.ratelimit.QrCodeFloodGuard;
import com.fsociety.flarebackend.report.QuarantineReason;
import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.report.ReportRepository;
import com.fsociety.flarebackend.report.ReportStatus;
import com.fsociety.flarebackend.review.dto.ReviewList;
import com.fsociety.flarebackend.staff.StaffMember;
import com.fsociety.flarebackend.staff.StaffRole;
import com.fsociety.flarebackend.staff.TeamId;

/** Needs review (A09): duty manager only; release, dismiss, and the Strict mode alerts. */
class ReviewServiceTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-10-31T12:00:00Z"));
	private final ReportRepository reports = mock(ReportRepository.class);
	private final QrCodeRepository qrCodes = mock(QrCodeRepository.class);
	private final QrCodeFloodGuard floodGuard = new QrCodeFloodGuard(clock, 20, Duration.ofMinutes(30));
	private final AuditService audit = mock(AuditService.class);
	private final GroupingWorker groupingWorker = mock(GroupingWorker.class);
	private final ReviewService service = new ReviewService(reports, qrCodes, floodGuard, audit, groupingWorker);

	private final StaffMember radu = member(Set.of(StaffRole.DUTY_MANAGER), Set.of());
	private final StaffMember ioana = member(Set.of(StaffRole.STAFF), Set.of(TeamId.SECURITY));
	private Report quarantined;

	@BeforeEach
	void setUp() {
		quarantined = report(QuarantineReason.UNKNOWN_QR_CODE);
		when(reports.findById(quarantined.getId())).thenReturn(Optional.of(quarantined));
	}

	@Test
	void theListShowsQuarantinedReportsAndStrictModeAlerts() {
		Location location = new Location();
		location.setLabel("DEP-WC-02");
		QrCode qrCode = new QrCode();
		qrCode.setLocation(location);
		when(qrCodes.findById("xw9fwv52ua")).thenReturn(Optional.of(qrCode));
		IntStream.range(0, 21).forEach(i -> floodGuard.countAndCheck("xw9fwv52ua"));
		IntStream.range(0, 21).forEach(i -> floodGuard.countAndCheck("zzzzzzzzzz"));
		when(reports.findQuarantined()).thenReturn(List.of(quarantined));

		ReviewList list = service.list(radu);

		assertThat(list.getItems()).singleElement().satisfies(item -> {
			assertThat(item.getReportId()).isEqualTo(quarantined.getId());
			assertThat(item.getText()).isEqualTo("Toaleta e murdară");
			assertThat(item.getReason()).isEqualTo(QuarantineReason.UNKNOWN_QR_CODE);
			assertThat(item.getScannedQrCodeId()).isEqualTo("zzzzzzzzzz");
			assertThat(item.getLocation()).isNull();
		});
		assertThat(list.getAlerts()).hasSize(2);
		assertThat(list.getAlerts()).filteredOn(a -> a.getQrCodeId().equals("xw9fwv52ua")).singleElement()
				.satisfies(alert -> {
					assertThat(alert.getLocation().getLabel()).isEqualTo("DEP-WC-02");
					assertThat(alert.getKind()).isEqualTo("STRICT_MODE");
					assertThat(alert.getMessageCount()).isEqualTo(1);
				});
		assertThat(list.getAlerts()).filteredOn(a -> a.getQrCodeId().equals("zzzzzzzzzz")).singleElement()
				.satisfies(alert -> assertThat(alert.getLocation()).isNull());
	}

	@Test
	void theListIsCappedAt100() {
		List<Report> many = new ArrayList<>();
		IntStream.range(0, 120).forEach(i -> many.add(report(QuarantineReason.RATE_LIMIT)));
		when(reports.findQuarantined()).thenReturn(many);

		assertThat(service.list(radu).getItems()).hasSize(100);
	}

	@Test
	void releaseToATeamAcceptsTheReportStoresTheTeamAuditsAndQueuesIt() {
		service.release(radu, quarantined.getId(), TeamId.SECURITY);

		assertThat(quarantined.getStatus()).isEqualTo(ReportStatus.ACCEPTED);
		assertThat(quarantined.getTeamOverride()).isEqualTo(TeamId.SECURITY);
		verify(audit).record(isNull(), eq(quarantined), eq(radu), eq(AuditAction.REPORT_RELEASED),
				eq(Map.of("reason", "UNKNOWN_QR_CODE", "team", "SECURITY")));
		verify(groupingWorker).enqueue(quarantined.getId());
	}

	@Test
	void releaseWithoutATeamRoutesNormally() {
		service.release(radu, quarantined.getId(), null);

		assertThat(quarantined.getStatus()).isEqualTo(ReportStatus.ACCEPTED);
		assertThat(quarantined.getTeamOverride()).isNull();
		verify(groupingWorker).enqueue(quarantined.getId());
	}

	@Test
	void dismissTakesItOutOfReviewWithoutQueueing() {
		service.dismiss(radu, quarantined.getId());

		assertThat(quarantined.getStatus()).isEqualTo(ReportStatus.DISMISSED);
		verify(audit).record(isNull(), eq(quarantined), eq(radu), eq(AuditAction.REPORT_DISMISSED), anyMap());
		verifyNoInteractions(groupingWorker);
	}

	@Test
	void aReportNoLongerInReviewIsNotFound() {
		quarantined.setStatus(ReportStatus.DISMISSED);

		assertCode(() -> service.release(radu, quarantined.getId(), null), ErrorCode.NOT_FOUND);
		assertCode(() -> service.dismiss(radu, quarantined.getId()), ErrorCode.NOT_FOUND);
		verify(groupingWorker, never()).enqueue(any());
	}

	@Test
	void aMissingReportIsNotFound() {
		UUID missing = UUID.randomUUID();
		when(reports.findById(missing)).thenReturn(Optional.empty());

		assertCode(() -> service.release(radu, missing, null), ErrorCode.NOT_FOUND);
	}

	@Test
	void onlyTheDutyManagerMayReview() {
		assertCode(() -> service.list(ioana), ErrorCode.FORBIDDEN);
		assertCode(() -> service.release(ioana, quarantined.getId(), TeamId.SECURITY), ErrorCode.FORBIDDEN);
		assertCode(() -> service.dismiss(ioana, quarantined.getId()), ErrorCode.FORBIDDEN);
		assertThat(quarantined.getStatus()).isEqualTo(ReportStatus.QUARANTINED);
		verifyNoInteractions(audit, groupingWorker);
	}

	private static void assertCode(Runnable call, ErrorCode code) {
		assertThatThrownBy(call::run)
				.isInstanceOf(FlareException.class)
				.extracting(e -> ((FlareException) e).getCode())
				.isEqualTo(code);
	}

	private static Report report(QuarantineReason reason) {
		Report report = new Report();
		report.setId(UUID.randomUUID());
		report.setText("Toaleta e murdară");
		report.setScannedQrCodeId("zzzzzzzzzz");
		report.setStatus(ReportStatus.QUARANTINED);
		report.setQuarantineReason(reason);
		report.setCreatedAt(Instant.parse("2026-10-31T11:55:00Z"));
		return report;
	}

	private static StaffMember member(Set<StaffRole> roles, Set<TeamId> teams) {
		StaffMember member = new StaffMember();
		member.setId(UUID.randomUUID());
		member.setDisplayName("Test (simulat)");
		member.setActive(true);
		member.setRoles(roles);
		member.setTeams(teams);
		return member;
	}
}
