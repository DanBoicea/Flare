package com.fsociety.flarebackend.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.fsociety.flarebackend.classification.Classification;
import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.common.Severity;

/** Copying a classification onto a Report: an Emergency always stays a CRITICAL Emergency. */
class ReportClassificationsTest {

	@Test
	void copiesTheClassification() {
		Report report = new Report();

		ReportClassifications.apply(report, Classification.builder()
				.kind(Classification.Kind.REPORT).category(Category.TOILET).severity(Severity.LOW).confidence(0.876)
				.staffSummary(new LocalizedText("Fără hârtie", "No paper")).mentionedPlace("belt 2").flightNumber("ZZ123")
				.source(ClassificationSource.LLM).build());

		assertThat(report.getCategory()).isEqualTo(Category.TOILET);
		assertThat(report.getSeverity()).isEqualTo(Severity.LOW);
		assertThat(report.getConfidence()).isEqualByComparingTo(new BigDecimal("0.88"));
		assertThat(report.getStaffSummaryRo()).isEqualTo("Fără hârtie");
		assertThat(report.getMentionedPlace()).isEqualTo("belt 2");
		assertThat(report.getFlightNumber()).isEqualTo("ZZ123");
		assertThat(report.getClassificationSource()).isEqualTo(ClassificationSource.LLM);
		assertThat(report.isEmergency()).isFalse();
	}

	@Test
	void anEmergencyFromTheWordListStaysCriticalWhateverTheClassifierSays() {
		Report report = new Report();
		report.setEmergency(true);
		report.setSeverity(Severity.CRITICAL);

		ReportClassifications.apply(report, Classification.builder()
				.kind(Classification.Kind.REPORT).emergency(false).category(Category.OTHER).severity(Severity.LOW)
				.source(ClassificationSource.LLM).build());

		assertThat(report.isEmergency()).isTrue();
		assertThat(report.getSeverity()).isEqualTo(Severity.CRITICAL);
	}

	@Test
	void missingValuesBecomeOtherAndMediumAndConfidenceIsClamped() {
		Report report = new Report();

		ReportClassifications.apply(report, Classification.builder()
				.kind(Classification.Kind.REPORT).confidence(1.7).source(ClassificationSource.FALLBACK).build());

		assertThat(report.getCategory()).isEqualTo(Category.OTHER);
		assertThat(report.getSeverity()).isEqualTo(Severity.MEDIUM);
		assertThat(report.getConfidence()).isEqualByComparingTo(new BigDecimal("1.00"));
	}
}
