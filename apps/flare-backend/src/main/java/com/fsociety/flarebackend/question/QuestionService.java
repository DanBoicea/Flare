package com.fsociety.flarebackend.question;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fsociety.flarebackend.common.ErrorCode;
import com.fsociety.flarebackend.common.FlareException;
import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.location.QrCodeService;
import com.fsociety.flarebackend.ratelimit.ClientFingerprint;
import com.fsociety.flarebackend.report.QuarantineReason;
import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.report.ReportRepository;
import com.fsociety.flarebackend.report.ReportStatus;

/**
 * "Send to staff anyway" for an answered Question (pipeline.md section 2, api.md). Turns the Question into an
 * unclassified Report; the grouping worker classifies it, telling the classifier the Passenger insisted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionService {

	private final QuestionRepository questions;
	private final ReportRepository reports;
	private final QrCodeService qrCodes;
	private final ClientFingerprint fingerprint;
	private final FlareProperties properties;
	private final Clock clock;

	/**
	 * Creates the Report, once. Returns its ID when it should be grouped; empty when it was already sent
	 * (idempotent) or the new Report is Quarantined. An unknown ID, another device's Question or one older
	 * than the window all give the same {@code NOT_FOUND}, so Question IDs can't be probed.
	 */
	@Transactional
	public Optional<UUID> sendToStaff(UUID questionId, UUID deviceId, String networkAddress) {
		Question question = questions.findById(questionId).orElseThrow(QuestionService::notFound);
		boolean sameDevice = question.getDeviceHash().equals(fingerprint.hash(deviceId.toString()));
		boolean inWindow = question.getCreatedAt()
				.plus(properties.getQuestions().getSendToStaffWindow())
				.isAfter(clock.instant());
		if (!sameDevice || !inWindow) {
			throw notFound();
		}
		if (question.getSentToStaffReport() != null) {
			return Optional.empty();
		}

		Report report = new Report();
		report.setScannedQrCodeId(question.getScannedQrCodeId());
		report.setLocation(question.getLocation());
		report.setText(question.getText());
		report.setLanguage(question.getLanguage());
		report.setDeviceHash(question.getDeviceHash());
		report.setNetworkHash(fingerprint.hash(networkAddress));
		// The QR code is checked again: a muted or unknown one keeps the Report out of every Team's list.
		Optional<QuarantineReason> quarantine = qrCodes.resolve(question.getScannedQrCodeId()).quarantineReason();
		report.setStatus(quarantine.isPresent() ? ReportStatus.QUARANTINED : ReportStatus.ACCEPTED);
		report.setQuarantineReason(quarantine.orElse(null));
		reports.save(report);
		question.setSentToStaffReport(report);

		log.info("Question {} sent to staff as report {} ({})", question.getId(), report.getId(), report.getStatus());
		return report.getStatus() == ReportStatus.ACCEPTED ? Optional.of(report.getId()) : Optional.empty();
	}

	private static FlareException notFound() {
		return new FlareException(ErrorCode.NOT_FOUND);
	}
}
