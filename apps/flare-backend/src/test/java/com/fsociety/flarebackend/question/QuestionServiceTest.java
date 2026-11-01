package com.fsociety.flarebackend.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fsociety.flarebackend.common.ErrorCode;
import com.fsociety.flarebackend.common.FlareException;
import com.fsociety.flarebackend.common.Language;
import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.location.Location;
import com.fsociety.flarebackend.location.QrCodeService;
import com.fsociety.flarebackend.ratelimit.ClientFingerprint;
import com.fsociety.flarebackend.report.QuarantineReason;
import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.report.ReportRepository;
import com.fsociety.flarebackend.report.ReportStatus;

/** "Send to staff anyway" (pipeline.md section 2): once, same device only, within the window. */
class QuestionServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");
	private static final UUID DEVICE = UUID.fromString("3f0e8f8e-2c4b-4d6c-9a51-6f1f3f0c2b7a");

	private final QuestionRepository questions = mock(QuestionRepository.class);
	private final ReportRepository reports = mock(ReportRepository.class);
	private final QrCodeService qrCodes = mock(QrCodeService.class);
	private final ClientFingerprint fingerprint = new ClientFingerprint("test-secret");
	private final FlareProperties properties = mock(FlareProperties.class, RETURNS_DEEP_STUBS);
	private final QuestionService service = new QuestionService(questions, reports, qrCodes, fingerprint, properties,
			Clock.fixed(NOW, ZoneOffset.UTC));

	private final Location location = new Location();
	private Question question;

	@BeforeEach
	void setUp() {
		when(properties.getQuestions().getSendToStaffWindow()).thenReturn(Duration.ofMinutes(30));
		when(qrCodes.resolve("zb8mt6agr3")).thenReturn(new QrCodeService.Resolution(QrCodeService.Status.ACTIVE, Optional.of(location)));
		when(reports.save(any(Report.class))).thenAnswer(call -> {
			Report report = call.getArgument(0);
			report.setId(UUID.randomUUID());
			return report;
		});
		question = new Question();
		question.setId(UUID.randomUUID());
		question.setScannedQrCodeId("zb8mt6agr3");
		question.setLocation(location);
		question.setText("Cum cer asistență pentru scaun cu rotile?");
		question.setLanguage(Language.RO);
		question.setDeviceHash(fingerprint.hash(DEVICE.toString()));
		question.setCreatedAt(NOW.minus(Duration.ofMinutes(5)));
		when(questions.findById(question.getId())).thenReturn(Optional.of(question));
	}

	@Test
	void createsAnUnclassifiedAcceptedReportAndLinksIt() {
		Optional<UUID> queued = service.sendToStaff(question.getId(), DEVICE, "10.0.0.1");

		ArgumentCaptor<Report> saved = ArgumentCaptor.forClass(Report.class);
		verify(reports).save(saved.capture());
		Report report = saved.getValue();
		assertThat(report.getStatus()).isEqualTo(ReportStatus.ACCEPTED);
		assertThat(report.getClassificationSource()).isNull();
		assertThat(report.getText()).isEqualTo(question.getText());
		assertThat(report.getLocation()).isSameAs(location);
		assertThat(report.getDeviceHash()).isEqualTo(question.getDeviceHash());
		assertThat(report.getNetworkHash()).hasSize(64);
		assertThat(question.getSentToStaffReport()).isSameAs(report);
		assertThat(queued).contains(report.getId());
	}

	@Test
	void aSecondCallCreatesNothing() {
		question.setSentToStaffReport(new Report());

		assertThat(service.sendToStaff(question.getId(), DEVICE, "10.0.0.1")).isEmpty();
		verify(reports, never()).save(any());
	}

	@Test
	void anotherDeviceGetsNotFound() {
		assertNotFound(() -> service.sendToStaff(question.getId(), UUID.randomUUID(), "10.0.0.1"));
	}

	@Test
	void aQuestionOlderThanTheWindowGetsNotFound() {
		question.setCreatedAt(NOW.minus(Duration.ofMinutes(31)));

		assertNotFound(() -> service.sendToStaff(question.getId(), DEVICE, "10.0.0.1"));
	}

	@Test
	void anUnknownQuestionGetsNotFound() {
		assertNotFound(() -> service.sendToStaff(UUID.randomUUID(), DEVICE, "10.0.0.1"));
	}

	@Test
	void aQuestionFromAMutedQrCodeBecomesAQuarantinedReportThatIsNotQueued() {
		when(qrCodes.resolve("zb8mt6agr3")).thenReturn(new QrCodeService.Resolution(QrCodeService.Status.MUTED, Optional.of(location)));

		Optional<UUID> queued = service.sendToStaff(question.getId(), DEVICE, "10.0.0.1");

		ArgumentCaptor<Report> saved = ArgumentCaptor.forClass(Report.class);
		verify(reports).save(saved.capture());
		assertThat(saved.getValue().getStatus()).isEqualTo(ReportStatus.QUARANTINED);
		assertThat(saved.getValue().getQuarantineReason()).isEqualTo(QuarantineReason.MUTED_QR_CODE);
		assertThat(queued).isEmpty();
	}

	private static void assertNotFound(Runnable call) {
		assertThatThrownBy(call::run)
				.isInstanceOf(FlareException.class)
				.extracting(e -> ((FlareException) e).getCode())
				.isEqualTo(ErrorCode.NOT_FOUND);
	}
}
