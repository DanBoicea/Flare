package com.fsociety.flarebackend.intake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fsociety.flarebackend.classification.BounceReason;
import com.fsociety.flarebackend.classification.Classification;
import com.fsociety.flarebackend.classification.FallbackClassifier;
import com.fsociety.flarebackend.classification.MessageClassifier;
import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.Language;
import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.common.MutableClock;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.grouping.GroupingWorker;
import com.fsociety.flarebackend.intake.dto.MessageRequest;
import com.fsociety.flarebackend.intake.dto.MessageResponse;
import com.fsociety.flarebackend.intake.dto.Outcome;
import com.fsociety.flarebackend.location.Location;
import com.fsociety.flarebackend.location.QrCodeService;
import com.fsociety.flarebackend.question.QuestionRepository;
import com.fsociety.flarebackend.question.QuestionService;
import com.fsociety.flarebackend.ratelimit.ClientFingerprint;
import com.fsociety.flarebackend.ratelimit.InMemoryBounceTracker;
import com.fsociety.flarebackend.ratelimit.InMemoryRateLimitService;
import com.fsociety.flarebackend.ratelimit.QrCodeFloodGuard;
import com.fsociety.flarebackend.report.QuarantineReason;
import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.report.ReportRepository;
import com.fsociety.flarebackend.report.ReportStatus;
import com.fsociety.flarebackend.safety.EmergencyTerms;
import com.fsociety.flarebackend.safety.HoneypotBotCheck;

/**
 * A07 "Done when": 25 messages from one device to one QR code give 3 accepted, then RATE_LIMIT, then Strict mode,
 * and an Emergency phrase still returns EMERGENCY. The real intake flow and real limits; only storage is faked.
 */
class SpamWaveTest {

	private static final String QR = "xw9fwv52ua";

	private final MutableClock clock = new MutableClock(Instant.parse("2026-10-31T12:00:00Z"));
	private final FlareProperties.Limits limits = new FlareProperties.Limits(
			new FlareProperties.Device(3, 10),
			new FlareProperties.Network(30),
			new FlareProperties.QrCode(20, Duration.ofMinutes(30)),
			new FlareProperties.Bounce(Duration.ofMinutes(30), 3, 6),
			new FlareProperties.BotCheck(2000));
	private final InMemoryBounceTracker bounces = new InMemoryBounceTracker(clock, Duration.ofMinutes(30));
	private final QrCodeFloodGuard floodGuard = new QrCodeFloodGuard(clock, 20, Duration.ofMinutes(30));
	private final ReportRepository reports = mock(ReportRepository.class);
	private final MessageClassifier classifier = mock(MessageClassifier.class);
	private final QrCodeService qrCodes = mock(QrCodeService.class);
	private final FlareProperties properties = mock(FlareProperties.class, RETURNS_DEEP_STUBS);
	private final List<Report> saved = new ArrayList<>();

	private final MessageIntakeService intake = new MessageIntakeService(new ClientFingerprint("test-secret"), qrCodes,
			EmergencyTerms.loadDetector(), new HoneypotBotCheck(2000),
			new InMemoryRateLimitService(clock, limits, floodGuard, bounces), bounces, classifier,
			new FallbackClassifier(EmergencyTerms.loadDetector()), reports, mock(QuestionRepository.class),
			mock(QuestionService.class), mock(GroupingWorker.class), properties);

	@Test
	void oneSpammerAtOneQrCode() {
		when(qrCodes.resolve(QR)).thenReturn(new QrCodeService.Resolution(QrCodeService.Status.ACTIVE, Optional.of(new Location())));
		when(properties.getLimits().getBounce().getSendAnywayAfter()).thenReturn(3);
		when(properties.getLimits().getBounce().getBlockAfter()).thenReturn(6);
		when(classifier.classify(any())).thenReturn(Classification.builder()
				.kind(Classification.Kind.REPORT).category(Category.TOILET).severity(Severity.MEDIUM).language(Language.RO)
				.confidence(0.9).staffSummary(new LocalizedText("Toaletă murdară", "Dirty toilet"))
				.source(ClassificationSource.LLM).build());
		when(reports.save(any(Report.class))).thenAnswer(call -> {
			Report report = call.getArgument(0);
			report.setId(UUID.randomUUID());
			saved.add(report);
			return report;
		});

		List<MessageResponse> responses = new ArrayList<>();
		for (int i = 0; i < 25; i++) {
			responses.add(intake.handle(request("Toaleta e murdara"), "10.0.0.1"));
		}
		MessageResponse emergency = intake.handle(request("Iese fum din tavan la toalete"), "10.0.0.1");

		// The spammer sees REPORTED every time: Quarantine is silent.
		assertThat(responses).allMatch(r -> r.getOutcome() == Outcome.REPORTED);
		assertThat(saved.subList(0, 3)).allMatch(r -> r.getStatus() == ReportStatus.ACCEPTED);
		assertThat(saved.subList(3, 20)).allMatch(r -> r.getQuarantineReason() == QuarantineReason.RATE_LIMIT);
		assertThat(saved.subList(20, 25)).allMatch(r -> r.getQuarantineReason() == QuarantineReason.QR_CODE_STRICT_MODE);
		assertThat(floodGuard.activeAlerts()).singleElement().satisfies(alert -> assertThat(alert.getQrCodeId()).isEqualTo(QR));

		assertThat(emergency.getOutcome()).isEqualTo(Outcome.EMERGENCY);
		Report last = saved.get(25);
		assertThat(last.getStatus()).isEqualTo(ReportStatus.ACCEPTED);
		assertThat(last.isEmergency()).isTrue();
	}

	/** Demo scenario 05 with the real limits: the fourth message is over the device limit but says SEND_ANYWAY. */
	@Test
	void threeBouncesThenSendAnyway() {
		when(qrCodes.resolve(QR)).thenReturn(new QrCodeService.Resolution(QrCodeService.Status.ACTIVE, Optional.of(new Location())));
		when(properties.getLimits().getBounce().getSendAnywayAfter()).thenReturn(3);
		when(properties.getLimits().getBounce().getBlockAfter()).thenReturn(6);
		when(classifier.classify(any())).thenReturn(Classification.builder()
				.kind(Classification.Kind.GARBAGE).language(Language.EN)
				.bounceReason(BounceReason.JOKE_OR_NONSENSE)
				.source(ClassificationSource.LLM).build());
		when(reports.save(any(Report.class))).thenAnswer(call -> {
			Report report = call.getArgument(0);
			report.setId(UUID.randomUUID());
			saved.add(report);
			return report;
		});

		MessageResponse first = intake.handle(request("knock knock"), "10.0.0.1");
		MessageResponse second = intake.handle(request("asdfghjkl"), "10.0.0.1");
		MessageResponse third = intake.handle(request("lol"), "10.0.0.1");
		MessageRequest insist = request("lol");
		insist.setSendAnyway(true);
		MessageResponse fourth = intake.handle(insist, "10.0.0.1");

		assertThat(first.getOutcome()).isEqualTo(Outcome.BOUNCED);
		assertThat(second.getBounce().isSendAnywayAvailable()).isFalse();
		assertThat(third.getBounce().isSendAnywayAvailable()).isTrue();
		assertThat(fourth.getOutcome()).isEqualTo(Outcome.REPORTED);
		assertThat(saved).singleElement().satisfies(report -> {
			assertThat(report.getStatus()).isEqualTo(ReportStatus.QUARANTINED);
			assertThat(report.getQuarantineReason()).isEqualTo(QuarantineReason.SEND_ANYWAY);
		});
	}

	private static MessageRequest request(String text) {
		MessageRequest request = new MessageRequest();
		request.setText(text);
		request.setUiLanguage("ro");
		request.setQrCodeId(QR);
		request.setDeviceId(UUID.fromString("3f0e8f8e-2c4b-4d6c-9a51-6f1f3f0c2b7a"));
		request.setWebsite("");
		request.setElapsedMs(5000L);
		return request;
	}
}
