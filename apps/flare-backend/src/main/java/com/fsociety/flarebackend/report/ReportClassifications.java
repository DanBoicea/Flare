package com.fsociety.flarebackend.report;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.fsociety.flarebackend.classification.Classification;
import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.Severity;

/**
 * Copies a {@link Classification} onto a Report, the same way at intake and in the grouping worker. An
 * Emergency stays an Emergency and is always CRITICAL, whatever the classifier says (pipeline.md). Pure.
 */
public final class ReportClassifications {

	private ReportClassifications() {
	}

	public static void apply(Report report, Classification classification) {
		boolean emergency = report.isEmergency() || classification.isEmergency();
		report.setEmergency(emergency);
		report.setCategory(classification.getCategory() != null ? classification.getCategory() : Category.OTHER);
		report.setSeverity(emergency ? Severity.CRITICAL
				: classification.getSeverity() != null ? classification.getSeverity() : Severity.MEDIUM);
		double confidence = Math.max(0, Math.min(1, classification.getConfidence()));
		report.setConfidence(BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP));
		if (classification.getStaffSummary() != null) {
			report.setStaffSummaryRo(classification.getStaffSummary().getRo());
			report.setStaffSummaryEn(classification.getStaffSummary().getEn());
		}
		report.setMentionedPlace(classification.getMentionedPlace());
		report.setFlightNumber(classification.getFlightNumber());
		report.setClassificationSource(classification.getSource());
	}
}
