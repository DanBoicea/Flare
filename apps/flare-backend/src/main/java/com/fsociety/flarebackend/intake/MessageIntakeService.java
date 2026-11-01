package com.fsociety.flarebackend.intake;

import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.fsociety.flarebackend.classification.Classification;
import com.fsociety.flarebackend.classification.ClassificationInput;
import com.fsociety.flarebackend.classification.FallbackClassifier;
import com.fsociety.flarebackend.classification.LocationInfo;
import com.fsociety.flarebackend.classification.MessageClassifier;
import com.fsociety.flarebackend.common.Language;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.grouping.GroupingWorker;
import com.fsociety.flarebackend.intake.dto.MessageRequest;
import com.fsociety.flarebackend.intake.dto.MessageResponse;
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
import com.fsociety.flarebackend.report.ReportClassifications;
import com.fsociety.flarebackend.report.ReportRepository;
import com.fsociety.flarebackend.report.ReportStatus;
import com.fsociety.flarebackend.safety.BotCheck;
import com.fsociety.flarebackend.safety.EmergencyDetector;

/**
 * The synchronous intake of a Passenger message, step by step as in pipeline.md section 1. The first step that
 * decides the outcome ends the flow. Deliberately not transactional: each save commits on its own, so a Report
 * is in the database before it is queued for grouping and before the response is sent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageIntakeService {

	private final ClientFingerprint fingerprint;
	private final QrCodeService qrCodes;
	private final EmergencyDetector emergencyDetector;
	private final BotCheck botCheck;
	private final RateLimitService rateLimits;
	private final BounceTracker bounces;
	private final MessageClassifier classifier;
	private final FallbackClassifier fallbackClassifier;
	private final ReportRepository reports;
	private final QuestionRepository questions;
	private final QuestionService questionService;
	private final GroupingWorker groupingWorker;
	private final FlareProperties properties;

	public MessageResponse handle(MessageRequest request, String networkAddress) {
		long start = System.nanoTime();
		Incoming in = incoming(request, networkAddress);

		// Step 4: the emergency word list. Nothing may block it: no bot check, no limit, no flagged QR code.
		if (emergencyDetector.isEmergency(in.text)) {
			Report report = newReport(in, ReportStatus.ACCEPTED, null);
			report.setEmergency(true);
			report.setSeverity(Severity.CRITICAL);
			return storeReport(report, MessageResponse.emergency(), start);
		}

		// Step 5: bot check.
		if (botCheck.looksAutomated(request.getWebsite(), request.getElapsedMs())) {
			return storeReport(newReport(in, ReportStatus.QUARANTINED, QuarantineReason.BOT_CHECK), MessageResponse.reported(), start);
		}

		// Step 6: rate limits and QR code Strict mode. They count every message from here on, including
		// Questions and Bounced ones.
		Optional<QuarantineReason> limited = rateLimits.check(in.deviceHash, in.networkHash, in.qrCodeId);

		// Step 7: "Send anyway" after enough Bounces goes to the duty manager unclassified. Bounces count towards
		// the device limit, so this message is usually over it; it is Quarantined either way, and SEND_ANYWAY tells
		// the duty manager why. Strict mode, and a device blocked for too many Bounces, keep their own reason.
		int sendAnywayAfter = properties.getLimits().getBounce().getSendAnywayAfter();
		int bounceCount = request.isSendAnyway() ? bounces.bounceCount(in.deviceHash) : 0;
		boolean sendAnyway = request.isSendAnyway() && bounceCount >= sendAnywayAfter
				&& bounceCount < properties.getLimits().getBounce().getBlockAfter()
				&& limited.filter(reason -> reason == QuarantineReason.QR_CODE_STRICT_MODE).isEmpty();
		if (sendAnyway) {
			return storeReport(newReport(in, ReportStatus.QUARANTINED, QuarantineReason.SEND_ANYWAY), MessageResponse.reported(), start);
		}
		if (limited.isPresent()) {
			return storeReport(newReport(in, ReportStatus.QUARANTINED, limited.get()), MessageResponse.reported(), start);
		}

		// Steps 8 and 9: classify, then act on the result.
		Classification classification = classify(in);

		if (classification.isEmergency()) {
			Report report = newReport(in, ReportStatus.ACCEPTED, null);
			ReportClassifications.apply(report, classification);
			report.setLanguage(languageOf(classification, in));
			return storeReport(report, MessageResponse.emergency(), start);
		}

		switch (classification.getKind()) {
			case QUESTION -> {
				// Answered even from a flagged QR code: a Question reaches no staff member, so it pollutes no list.
				Question question = new Question();
				question.setScannedQrCodeId(in.qrCodeId);
				question.setLocation(in.location);
				question.setText(in.text);
				question.setLanguage(languageOf(classification, in));
				question.setAnswerText(classification.getAnswer());
				question.setAnswerFound(classification.isAnswerFound());
				question.setDeviceHash(in.deviceHash);
				questions.save(question);
				log.info("Message answered: question={} found={} latencyMs={}", question.getId(),
						question.isAnswerFound(), millisSince(start));
				return MessageResponse.answered(question.getId(), classification.getAnswer(), classification.isAnswerFound());
			}
			case GARBAGE -> {
				// Bounced text is stored nowhere; only the device's Bounce counter moves.
				int count = bounces.recordBounce(in.deviceHash);
				log.info("Message bounced: reason={} latencyMs={}", classification.getBounceReason(), millisSince(start));
				return MessageResponse.bounced(classification.getBounceReason(), count >= sendAnywayAfter);
			}
			default -> {
				Report report = in.qrQuarantine
						.map(reason -> newReport(in, ReportStatus.QUARANTINED, reason))
						.orElseGet(() -> newReport(in, ReportStatus.ACCEPTED, null));
				ReportClassifications.apply(report, classification);
				report.setLanguage(languageOf(classification, in));
				return storeReport(report, MessageResponse.reported(), start);
			}
		}
	}

	/** "Send to staff anyway": the Report is committed by {@link QuestionService} before it is queued. */
	public void sendToStaff(UUID questionId, UUID deviceId, String networkAddress) {
		questionService.sendToStaff(questionId, deviceId, networkAddress).ifPresent(groupingWorker::enqueue);
	}

	/** Step 8. Any exception, or a result that breaks the output rules, falls back to Unclassified. */
	private Classification classify(Incoming in) {
		ClassificationInput input = new ClassificationInput(in.text, in.uiLanguage,
				Optional.ofNullable(in.location).map(l -> new LocationInfo(l.getNameRo(), l.getNameEn())), false);
		try {
			Classification classification = classifier.classify(input);
			if (isUsable(classification)) {
				return classification;
			}
			log.warn("Classifier returned an unusable result, using the fallback");
		}
		catch (RuntimeException e) {
			// The exception type only: a message could contain Passenger text.
			log.warn("Classifier failed, using the fallback ({})", e.getClass().getSimpleName());
		}
		return fallbackClassifier.classify(input);
	}

	/** The minimum the intake needs to act on a result; Track C's validator checks the rest (pipeline.md 4.1). */
	static boolean isUsable(Classification classification) {
		if (classification == null || classification.getKind() == null) {
			return false;
		}
		return switch (classification.getKind()) {
			case QUESTION -> classification.getAnswer() != null && !classification.getAnswer().isBlank();
			case GARBAGE -> classification.getBounceReason() != null;
			case REPORT -> true;
		};
	}

	private MessageResponse storeReport(Report report, MessageResponse response, long start) {
		reports.save(report);
		if (report.getStatus() == ReportStatus.ACCEPTED) {
			groupingWorker.enqueue(report.getId());
		}
		log.info("Message stored: outcome={} report={} status={} reason={} latencyMs={}", response.getOutcome(),
				report.getId(), report.getStatus(), report.getQuarantineReason(), millisSince(start));
		return response;
	}

	private static Report newReport(Incoming in, ReportStatus status, QuarantineReason reason) {
		Report report = new Report();
		report.setScannedQrCodeId(in.qrCodeId);
		report.setLocation(in.location);
		report.setText(in.text);
		report.setLanguage(in.uiLanguage);
		report.setContact(in.contact);
		report.setDeviceHash(in.deviceHash);
		report.setNetworkHash(in.networkHash);
		report.setStatus(status);
		report.setQuarantineReason(reason);
		return report;
	}

	/** Step 2 and 3: trimmed text and contact, hashed device and network, the resolved QR code. */
	private Incoming incoming(MessageRequest request, String networkAddress) {
		QrCodeService.Resolution qr = qrCodes.resolve(request.getQrCodeId());
		return new Incoming(
				request.getText().strip(),
				Language.fromCode(request.getUiLanguage()),
				request.getQrCodeId(),
				qr.getLocation().orElse(null),
				qr.quarantineReason(),
				blankToNull(request.getContact()),
				fingerprint.hash(request.getDeviceId().toString()),
				fingerprint.hash(networkAddress == null ? "unknown" : networkAddress));
	}

	private static Language languageOf(Classification classification, Incoming in) {
		return classification.getLanguage() != null ? classification.getLanguage() : in.uiLanguage;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	private static long millisSince(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000;
	}

	/** One message on its way through the steps. Holds Passenger text and contact, so no toString. */
	private static final class Incoming {

		final String text;
		final Language uiLanguage;
		final String qrCodeId;
		final Location location;
		final Optional<QuarantineReason> qrQuarantine;
		final String contact;
		final String deviceHash;
		final String networkHash;

		Incoming(String text, Language uiLanguage, String qrCodeId, Location location,
				Optional<QuarantineReason> qrQuarantine, String contact, String deviceHash, String networkHash) {
			this.text = text;
			this.uiLanguage = uiLanguage;
			this.qrCodeId = qrCodeId;
			this.location = location;
			this.qrQuarantine = qrQuarantine;
			this.contact = contact;
			this.deviceHash = deviceHash;
			this.networkHash = networkHash;
		}
	}
}
