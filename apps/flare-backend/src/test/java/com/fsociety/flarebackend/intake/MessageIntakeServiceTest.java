package com.fsociety.flarebackend.intake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import com.fsociety.flarebackend.classification.BounceReason;
import com.fsociety.flarebackend.classification.Classification;
import com.fsociety.flarebackend.classification.FallbackClassifier;
import com.fsociety.flarebackend.classification.MessageClassifier;
import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.Language;
import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.grouping.GroupingWorker;
import com.fsociety.flarebackend.intake.dto.MessageRequest;
import com.fsociety.flarebackend.intake.dto.MessageResponse;
import com.fsociety.flarebackend.intake.dto.Outcome;
import com.fsociety.flarebackend.llm.LlmFailure;
import com.fsociety.flarebackend.location.Location;
import com.fsociety.flarebackend.location.QrCodeService;
import com.fsociety.flarebackend.question.Question;
import com.fsociety.flarebackend.question.QuestionRepository;
import com.fsociety.flarebackend.question.QuestionService;
import com.fsociety.flarebackend.ratelimit.BounceTracker;
import com.fsociety.flarebackend.ratelimit.ClientFingerprint;
import com.fsociety.flarebackend.ratelimit.RateLimitService;
import com.fsociety.flarebackend.report.QuarantineReason;
import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.report.ReportRepository;
import com.fsociety.flarebackend.report.ReportStatus;
import com.fsociety.flarebackend.safety.BotCheck;
import com.fsociety.flarebackend.safety.EmergencyTerms;

/** The intake steps of pipeline.md section 1, with every collaborator faked (A06 "Done when"). */
class MessageIntakeServiceTest {

	private static final String ACTIVE_QR = "skp4fq3r5e";
	private static final String MUTED_QR = "9wa6zzsz5d";
	private static final String UNKNOWN_QR = "zzzzzzzzzz";

	private final QrCodeService qrCodes = mock(QrCodeService.class);
	private final BotCheck botCheck = mock(BotCheck.class);
	private final RateLimitService rateLimits = mock(RateLimitService.class);
	private final BounceTracker bounces = mock(BounceTracker.class);
	private final MessageClassifier classifier = mock(MessageClassifier.class);
	private final FallbackClassifier fallback = new FallbackClassifier(EmergencyTerms.loadDetector());
	private final ReportRepository reports = mock(ReportRepository.class);
	private final QuestionRepository questions = mock(QuestionRepository.class);
	private final QuestionService questionService = mock(QuestionService.class);
	private final GroupingWorker groupingWorker = mock(GroupingWorker.class);
	private final FlareProperties properties = mock(FlareProperties.class, RETURNS_DEEP_STUBS);

	private final MessageIntakeService intake = new MessageIntakeService(new ClientFingerprint("test-secret"), qrCodes,
			EmergencyTerms.loadDetector(), botCheck, rateLimits, bounces, classifier, fallback, reports, questions,
			questionService, groupingWorker, properties);

	private final Location location = new Location();

	@BeforeEach
	void setUp() {
		location.setNameRo("Toalete sosiri, lângă banda 2");
		location.setNameEn("Arrivals toilets near belt 2");
		when(qrCodes.resolve(ACTIVE_QR)).thenReturn(new QrCodeService.Resolution(QrCodeService.Status.ACTIVE, Optional.of(location)));
		when(qrCodes.resolve(MUTED_QR)).thenReturn(new QrCodeService.Resolution(QrCodeService.Status.MUTED, Optional.of(location)));
		when(qrCodes.resolve(UNKNOWN_QR)).thenReturn(new QrCodeService.Resolution(QrCodeService.Status.UNKNOWN, Optional.empty()));
		when(rateLimits.check(anyString(), anyString(), any())).thenReturn(Optional.empty());
		when(properties.getLimits().getBounce().getSendAnywayAfter()).thenReturn(3);
		when(properties.getLimits().getBounce().getBlockAfter()).thenReturn(6);
		when(reports.save(any(Report.class))).thenAnswer(call -> {
			Report report = call.getArgument(0);
			report.setId(UUID.randomUUID());
			return report;
		});
		when(questions.save(any(Question.class))).thenAnswer(call -> {
			Question question = call.getArgument(0);
			question.setId(UUID.randomUUID());
			return question;
		});
	}

	@Test
	void anLlmFailureFromTheLlamaServerClientAlsoGivesTheFallback() {
		// What LlamaServerClient throws when llama-server is down, slow or answers badly (C01).
		when(classifier.classify(any())).thenThrow(new LlmFailure("llama-server unreachable"));

		MessageResponse response = intake.handle(request("Nu mai e hârtie în toaleta de lângă banda 2", ACTIVE_QR), "10.0.0.1");

		assertThat(response.getOutcome()).isEqualTo(Outcome.REPORTED);
		assertThat(savedReport().getClassificationSource()).isEqualTo(ClassificationSource.FALLBACK);
	}

	@Test
	void withTheLlmFailingAMessageToASeededQrCodeIsAFallbackReport() {
		when(classifier.classify(any())).thenThrow(new IllegalStateException("LLM down"));

		MessageResponse response = intake.handle(request("Nu mai e hârtie în toaleta de lângă banda 2", ACTIVE_QR), "10.0.0.1");

		assertThat(response.getOutcome()).isEqualTo(Outcome.REPORTED);
		assertThat(response.getQuestionId()).isNull();
		assertThat(response.getAnswer()).isNull();
		assertThat(response.getBounce()).isNull();
		Report report = savedReport();
		assertThat(report.getStatus()).isEqualTo(ReportStatus.ACCEPTED);
		assertThat(report.getClassificationSource()).isEqualTo(ClassificationSource.FALLBACK);
		assertThat(report.getCategory()).isEqualTo(Category.OTHER);
		assertThat(report.getSeverity()).isEqualTo(Severity.MEDIUM);
		assertThat(report.getLocation()).isSameAs(location);
		assertThat(report.getScannedQrCodeId()).isEqualTo(ACTIVE_QR);
		assertThat(report.getLanguage()).isEqualTo(Language.RO);
		verify(groupingWorker).enqueue(report.getId());
	}

	@Test
	void theReportIsSavedBeforeItIsQueued() {
		when(classifier.classify(any())).thenReturn(report(Category.TOILET, Severity.MEDIUM));

		intake.handle(request("No toilet paper", ACTIVE_QR), "10.0.0.1");

		InOrder order = inOrder(reports, groupingWorker);
		order.verify(reports).save(any(Report.class));
		order.verify(groupingWorker).enqueue(any());
	}

	@Test
	void aClassifiedReportKeepsTheClassification() {
		when(classifier.classify(any())).thenReturn(report(Category.TOILET, Severity.MEDIUM));

		intake.handle(request("No toilet paper", ACTIVE_QR), "10.0.0.1");

		Report report = savedReport();
		assertThat(report.getStatus()).isEqualTo(ReportStatus.ACCEPTED);
		assertThat(report.getCategory()).isEqualTo(Category.TOILET);
		assertThat(report.getClassificationSource()).isEqualTo(ClassificationSource.LLM);
		assertThat(report.getStaffSummaryEn()).isEqualTo("No toilet paper");
		assertThat(report.getLanguage()).isEqualTo(Language.EN);
	}

	@Test
	void anUnknownQrCodeGivesAQuarantinedReportThatIsNotQueued() {
		when(classifier.classify(any())).thenReturn(report(Category.TOILET, Severity.MEDIUM));

		MessageResponse response = intake.handle(request("Toaleta e murdară", UNKNOWN_QR), "10.0.0.1");

		assertThat(response.getOutcome()).isEqualTo(Outcome.REPORTED);
		Report report = savedReport();
		assertThat(report.getStatus()).isEqualTo(ReportStatus.QUARANTINED);
		assertThat(report.getQuarantineReason()).isEqualTo(QuarantineReason.UNKNOWN_QR_CODE);
		assertThat(report.getLocation()).isNull();
		verify(groupingWorker, never()).enqueue(any());
	}

	@Test
	void aMutedQrCodeGivesAQuarantinedReport() {
		when(classifier.classify(any())).thenReturn(report(Category.BROKEN_OR_MAINTENANCE, Severity.HIGH));

		intake.handle(request("Ușa de serviciu e blocată", MUTED_QR), "10.0.0.1");

		assertThat(savedReport().getQuarantineReason()).isEqualTo(QuarantineReason.MUTED_QR_CODE);
		verify(groupingWorker, never()).enqueue(any());
	}

	@Test
	void aMissingQrCodeIsUnknown() {
		when(qrCodes.resolve(null)).thenReturn(new QrCodeService.Resolution(QrCodeService.Status.UNKNOWN, Optional.empty()));
		when(classifier.classify(any())).thenReturn(report(Category.TOILET, Severity.MEDIUM));

		intake.handle(request("Toaleta e murdară", null), "10.0.0.1");

		assertThat(savedReport().getQuarantineReason()).isEqualTo(QuarantineReason.UNKNOWN_QR_CODE);
	}

	@Test
	void aQuestionCreatesAQuestionRowAndIsAnswered() {
		when(classifier.classify(any())).thenReturn(Classification.builder()
				.kind(Classification.Kind.QUESTION).language(Language.RO).answer("Zona pentru fumători este afară.")
				.answerFound(true).source(ClassificationSource.LLM).build());

		MessageResponse response = intake.handle(request("Unde pot fuma?", ACTIVE_QR), "10.0.0.1");

		ArgumentCaptor<Question> saved = ArgumentCaptor.forClass(Question.class);
		verify(questions).save(saved.capture());
		Question question = saved.getValue();
		assertThat(question.getAnswerText()).isEqualTo("Zona pentru fumători este afară.");
		assertThat(question.isAnswerFound()).isTrue();
		assertThat(question.getLocation()).isSameAs(location);
		assertThat(response.getOutcome()).isEqualTo(Outcome.ANSWERED);
		assertThat(response.getQuestionId()).isEqualTo(question.getId());
		assertThat(response.getAnswer().getText()).isEqualTo("Zona pentru fumători este afară.");
		assertThat(response.getAnswer().isFound()).isTrue();
		assertThat(response.getBounce()).isNull();
		verify(reports, never()).save(any());
		verifyNoInteractions(groupingWorker);
	}

	@Test
	void aQuestionFromAnUnknownQrCodeIsStillAnswered() {
		when(classifier.classify(any())).thenReturn(Classification.builder()
				.kind(Classification.Kind.QUESTION).language(Language.EN).answer("I don't know.")
				.answerFound(false).source(ClassificationSource.LLM).build());

		MessageResponse response = intake.handle(request("Is there a pharmacy?", UNKNOWN_QR), "10.0.0.1");

		assertThat(response.getOutcome()).isEqualTo(Outcome.ANSWERED);
		assertThat(response.getAnswer().isFound()).isFalse();
	}

	@Test
	void garbageStoresNothingAndIsBounced() {
		when(classifier.classify(any())).thenReturn(garbage());
		when(bounces.recordBounce(anyString())).thenReturn(1);

		MessageResponse response = intake.handle(request("knock knock", ACTIVE_QR), "10.0.0.1");

		assertThat(response.getOutcome()).isEqualTo(Outcome.BOUNCED);
		assertThat(response.getBounce().getReason()).isEqualTo(BounceReason.JOKE_OR_NONSENSE);
		assertThat(response.getBounce().isSendAnywayAvailable()).isFalse();
		verify(reports, never()).save(any());
		verify(questions, never()).save(any());
		verifyNoInteractions(groupingWorker);
	}

	@Test
	void theThirdBounceOffersSendAnyway() {
		when(classifier.classify(any())).thenReturn(garbage());
		when(bounces.recordBounce(anyString())).thenReturn(3);

		MessageResponse response = intake.handle(request("lol", ACTIVE_QR), "10.0.0.1");

		assertThat(response.getBounce().isSendAnywayAvailable()).isTrue();
	}

	@Test
	void sendAnywayAfterEnoughBouncesIsQuarantinedUnclassified() {
		when(bounces.bounceCount(anyString())).thenReturn(3);
		MessageRequest request = request("lol", ACTIVE_QR);
		request.setSendAnyway(true);

		MessageResponse response = intake.handle(request, "10.0.0.1");

		assertThat(response.getOutcome()).isEqualTo(Outcome.REPORTED);
		Report report = savedReport();
		assertThat(report.getQuarantineReason()).isEqualTo(QuarantineReason.SEND_ANYWAY);
		assertThat(report.getClassificationSource()).isNull();
		verifyNoInteractions(classifier, groupingWorker);
	}

	@Test
	void sendAnywayOverTheDeviceLimitStillSaysSendAnyway() {
		when(bounces.bounceCount(anyString())).thenReturn(3);
		when(rateLimits.check(anyString(), anyString(), any())).thenReturn(Optional.of(QuarantineReason.RATE_LIMIT));
		MessageRequest request = request("lol", ACTIVE_QR);
		request.setSendAnyway(true);

		intake.handle(request, "10.0.0.1");

		assertThat(savedReport().getQuarantineReason()).isEqualTo(QuarantineReason.SEND_ANYWAY);
	}

	@Test
	void sendAnywayDuringStrictModeKeepsTheStrictModeReason() {
		when(bounces.bounceCount(anyString())).thenReturn(3);
		when(rateLimits.check(anyString(), anyString(), any())).thenReturn(Optional.of(QuarantineReason.QR_CODE_STRICT_MODE));
		MessageRequest request = request("lol", ACTIVE_QR);
		request.setSendAnyway(true);

		intake.handle(request, "10.0.0.1");

		assertThat(savedReport().getQuarantineReason()).isEqualTo(QuarantineReason.QR_CODE_STRICT_MODE);
	}

	@Test
	void sendAnywayFromADeviceBlockedForTooManyBouncesIsARateLimit() {
		when(bounces.bounceCount(anyString())).thenReturn(6);
		when(rateLimits.check(anyString(), anyString(), any())).thenReturn(Optional.of(QuarantineReason.RATE_LIMIT));
		MessageRequest request = request("lol", ACTIVE_QR);
		request.setSendAnyway(true);

		intake.handle(request, "10.0.0.1");

		assertThat(savedReport().getQuarantineReason()).isEqualTo(QuarantineReason.RATE_LIMIT);
	}

	@Test
	void sendAnywayWithoutEnoughBouncesIsClassifiedNormally() {
		when(bounces.bounceCount(anyString())).thenReturn(1);
		when(classifier.classify(any())).thenReturn(report(Category.TOILET, Severity.MEDIUM));
		MessageRequest request = request("No toilet paper", ACTIVE_QR);
		request.setSendAnyway(true);

		intake.handle(request, "10.0.0.1");

		assertThat(savedReport().getStatus()).isEqualTo(ReportStatus.ACCEPTED);
	}

	@Test
	void anEmergencyPhraseSkipsEverythingAndIsQueuedEvenFromAnUnknownQrCode() {
		when(botCheck.looksAutomated(any(), any())).thenReturn(true);
		when(rateLimits.check(anyString(), anyString(), any())).thenReturn(Optional.of(QuarantineReason.RATE_LIMIT));

		MessageResponse response = intake.handle(request("Un om a căzut și nu mai respiră", UNKNOWN_QR), "10.0.0.1");

		assertThat(response.getOutcome()).isEqualTo(Outcome.EMERGENCY);
		Report report = savedReport();
		assertThat(report.getStatus()).isEqualTo(ReportStatus.ACCEPTED);
		assertThat(report.isEmergency()).isTrue();
		assertThat(report.getSeverity()).isEqualTo(Severity.CRITICAL);
		assertThat(report.getClassificationSource()).isNull();
		verifyNoInteractions(classifier, botCheck, rateLimits);
		verify(groupingWorker).enqueue(report.getId());
	}

	@Test
	void anEmergencyFoundByTheLlmIsCriticalAndAccepted() {
		when(classifier.classify(any())).thenReturn(Classification.builder()
				.kind(Classification.Kind.REPORT).emergency(true).category(Category.ASSISTANCE_NEEDED)
				.severity(Severity.HIGH).language(Language.RO).confidence(0.9)
				.staffSummary(new LocalizedText("Pasageră cu dureri în piept", "Passenger with chest pain"))
				.source(ClassificationSource.LLM).build());

		MessageResponse response = intake.handle(request("Doamna de lângă mine se ține de piept", MUTED_QR), "10.0.0.1");

		assertThat(response.getOutcome()).isEqualTo(Outcome.EMERGENCY);
		Report report = savedReport();
		assertThat(report.getStatus()).isEqualTo(ReportStatus.ACCEPTED);
		assertThat(report.getSeverity()).isEqualTo(Severity.CRITICAL);
		verify(groupingWorker).enqueue(report.getId());
	}

	@Test
	void aBotIsQuarantinedWithoutCallingTheClassifier() {
		when(botCheck.looksAutomated(any(), any())).thenReturn(true);

		MessageResponse response = intake.handle(request("No toilet paper", ACTIVE_QR), "10.0.0.1");

		assertThat(response.getOutcome()).isEqualTo(Outcome.REPORTED);
		assertThat(savedReport().getQuarantineReason()).isEqualTo(QuarantineReason.BOT_CHECK);
		verifyNoInteractions(classifier, groupingWorker);
	}

	@Test
	void aRateLimitedMessageIsQuarantinedSilently() {
		when(rateLimits.check(anyString(), anyString(), any())).thenReturn(Optional.of(QuarantineReason.QR_CODE_STRICT_MODE));

		MessageResponse response = intake.handle(request("Toaleta e murdara", ACTIVE_QR), "10.0.0.1");

		assertThat(response.getOutcome()).isEqualTo(Outcome.REPORTED);
		assertThat(savedReport().getQuarantineReason()).isEqualTo(QuarantineReason.QR_CODE_STRICT_MODE);
		verifyNoInteractions(classifier, groupingWorker);
	}

	@Test
	void anUnusableResultFromTheClassifierFallsBack() {
		when(classifier.classify(any())).thenReturn(Classification.builder()
				.kind(Classification.Kind.QUESTION).source(ClassificationSource.LLM).build());

		MessageResponse response = intake.handle(request("Unde e toaleta?", ACTIVE_QR), "10.0.0.1");

		assertThat(response.getOutcome()).isEqualTo(Outcome.REPORTED);
		assertThat(savedReport().getClassificationSource()).isEqualTo(ClassificationSource.FALLBACK);
	}

	@Test
	void onlyHashesAndTrimmedValuesAreStored() {
		when(classifier.classify(any())).thenReturn(report(Category.TOILET, Severity.MEDIUM));
		MessageRequest request = request("  No toilet paper  ", ACTIVE_QR);
		request.setContact("   ");

		intake.handle(request, "10.0.0.1");

		Report report = savedReport();
		assertThat(report.getText()).isEqualTo("No toilet paper");
		assertThat(report.getContact()).isNull();
		assertThat(report.getDeviceHash()).hasSize(64).doesNotContain(request.getDeviceId().toString());
		assertThat(report.getNetworkHash()).hasSize(64).doesNotContain("10.0.0.1");
	}

	@Test
	void sendToStaffQueuesTheNewReportOnlyWhenThereIsOne() {
		UUID questionId = UUID.randomUUID();
		UUID deviceId = UUID.randomUUID();
		UUID reportId = UUID.randomUUID();
		when(questionService.sendToStaff(questionId, deviceId, "10.0.0.1")).thenReturn(Optional.of(reportId));

		intake.sendToStaff(questionId, deviceId, "10.0.0.1");

		verify(groupingWorker).enqueue(reportId);
	}

	private Report savedReport() {
		ArgumentCaptor<Report> saved = ArgumentCaptor.forClass(Report.class);
		verify(reports).save(saved.capture());
		return saved.getValue();
	}

	private static MessageRequest request(String text, String qrCodeId) {
		MessageRequest request = new MessageRequest();
		request.setText(text);
		request.setUiLanguage("ro");
		request.setQrCodeId(qrCodeId);
		request.setDeviceId(UUID.fromString("3f0e8f8e-2c4b-4d6c-9a51-6f1f3f0c2b7a"));
		request.setWebsite("");
		request.setElapsedMs(5000L);
		return request;
	}

	private static Classification report(Category category, Severity severity) {
		return Classification.builder()
				.kind(Classification.Kind.REPORT).category(category).severity(severity).language(Language.EN)
				.confidence(0.9).staffSummary(new LocalizedText("Nu e hârtie igienică", "No toilet paper"))
				.source(ClassificationSource.LLM).build();
	}

	private static Classification garbage() {
		return Classification.builder()
				.kind(Classification.Kind.GARBAGE).language(Language.EN).bounceReason(BounceReason.JOKE_OR_NONSENSE)
				.source(ClassificationSource.LLM).build();
	}
}
