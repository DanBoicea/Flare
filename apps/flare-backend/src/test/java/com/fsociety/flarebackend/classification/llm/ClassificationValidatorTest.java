package com.fsociety.flarebackend.classification.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Optional;

import com.fsociety.flarebackend.classification.BounceReason;
import com.fsociety.flarebackend.classification.Classification;
import com.fsociety.flarebackend.classification.ClassificationInput;
import com.fsociety.flarebackend.classification.LocationInfo;
import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.Language;
import com.fsociety.flarebackend.common.Severity;
import org.junit.jupiter.api.Test;

/** The output rules from docs/architecture/pipeline.md section 4.1, checked in our own code. */
class ClassificationValidatorTest {

	private final ClassificationValidator validator = new ClassificationValidator();
	private final ClassificationInput input = input(false);

	@Test
	void aValidReportBecomesAnLlmClassification() {
		Classification c = validator.validate(report(), input);

		assertThat(c.getKind()).isEqualTo(Classification.Kind.REPORT);
		assertThat(c.getCategory()).isEqualTo(Category.TOILET);
		assertThat(c.getSeverity()).isEqualTo(Severity.MEDIUM);
		assertThat(c.getLanguage()).isEqualTo(Language.RO);
		assertThat(c.getStaffSummary().getEn()).isEqualTo("No toilet paper near belt 2.");
		assertThat(c.getConfidence()).isEqualTo(0.9);
		assertThat(c.getSource()).isEqualTo(ClassificationSource.LLM);
	}

	@Test
	void aReportNeedsCategorySeverityAndBothSummaries() {
		ClassificationAnswer noCategory = report();
		noCategory.setCategory(null);
		ClassificationAnswer noSeverity = report();
		noSeverity.setSeverity(null);
		ClassificationAnswer noEnglishSummary = report();
		noEnglishSummary.setStaffSummary(new ClassificationAnswer.Summary("Lipsește hârtia.", " "));

		assertRejected(noCategory, "category");
		assertRejected(noSeverity, "severity");
		assertRejected(noEnglishSummary, "staffSummary");
	}

	@Test
	void unknownEnumValuesAreRejected() {
		ClassificationAnswer badKind = report();
		badKind.setKind("COMPLAINT");
		ClassificationAnswer badCategory = report();
		badCategory.setCategory("PLUMBING");
		ClassificationAnswer badLanguage = report();
		badLanguage.setLanguage("fr");

		assertRejected(badKind, "kind");
		assertRejected(badCategory, "category");
		assertRejected(badLanguage, "language");
	}

	@Test
	void textsOverTheirLimitsAreRejected() {
		ClassificationAnswer longSummary = report();
		longSummary.setStaffSummary(new ClassificationAnswer.Summary("ă".repeat(141), "ok"));
		ClassificationAnswer longAnswer = question();
		longAnswer.setAnswer("a".repeat(601));
		ClassificationAnswer longPlace = report();
		longPlace.setMentionedPlace("p".repeat(81));

		assertRejected(longSummary, "staffSummary");
		assertRejected(longAnswer, "answer");
		assertRejected(longPlace, "mentionedPlace");
	}

	@Test
	void limitsCountCharactersNotBytes() {
		ClassificationAnswer romanian = report();
		romanian.setStaffSummary(new ClassificationAnswer.Summary("ș".repeat(140), "ok"));

		assertThat(validator.validate(romanian, input).getStaffSummary().getRo()).hasSize(140);
	}

	@Test
	void aQuestionNeedsAnAnswerAndAnswerFound() {
		Classification answered = validator.validate(question(), input);
		ClassificationAnswer noAnswer = question();
		noAnswer.setAnswer("");
		ClassificationAnswer noFound = question();
		noFound.setAnswerFound(null);

		assertThat(answered.getKind()).isEqualTo(Classification.Kind.QUESTION);
		assertThat(answered.getAnswer()).startsWith("Biroul de informații");
		assertThat(answered.isAnswerFound()).isTrue();
		assertRejected(noAnswer, "answer");
		assertRejected(noFound, "answerFound");
	}

	@Test
	void garbageNeedsABounceReason() {
		Classification bounced = validator.validate(garbage(), input);
		ClassificationAnswer noReason = garbage();
		noReason.setBounceReason(null);

		assertThat(bounced.getBounceReason()).isEqualTo(BounceReason.JOKE_OR_NONSENSE);
		assertRejected(noReason, "bounceReason");
	}

	@Test
	void anAnswerOnANonQuestionOrABounceReasonOnANonGarbageMessageIsRejected() {
		ClassificationAnswer reportWithAnswer = report();
		reportWithAnswer.setAnswer("Here is some advice.");
		ClassificationAnswer questionWithBounce = question();
		questionWithBounce.setBounceReason("TOO_UNCLEAR");

		assertRejected(reportWithAnswer, "answer");
		assertRejected(questionWithBounce, "bounceReason");
	}

	@Test
	void anEmergencyIsAlwaysACriticalReport() {
		ClassificationAnswer highEmergency = report();
		highEmergency.setEmergency(true);
		highEmergency.setSeverity("HIGH");

		Classification c = validator.validate(highEmergency, input);

		assertThat(c.isEmergency()).isTrue();
		assertThat(c.getSeverity()).isEqualTo(Severity.CRITICAL);
	}

	@Test
	void anEmergencyIsNeverBouncedOrAnswered() {
		// CLAUDE.md safety boundary: nothing may turn an Emergency into a Bounce or an automatic answer.
		ClassificationAnswer bouncedEmergency = garbage();
		bouncedEmergency.setEmergency(true);
		ClassificationAnswer answeredEmergency = question();
		answeredEmergency.setEmergency(true);

		Classification fromGarbage = validator.validate(bouncedEmergency, input);
		Classification fromQuestion = validator.validate(answeredEmergency, input);

		for (Classification c : new Classification[] { fromGarbage, fromQuestion }) {
			assertThat(c.getKind()).isEqualTo(Classification.Kind.REPORT);
			assertThat(c.getSeverity()).isEqualTo(Severity.CRITICAL);
			assertThat(c.getCategory()).isEqualTo(Category.OTHER);
			assertThat(c.getAnswer()).isNull();
			assertThat(c.getBounceReason()).isNull();
			assertThat(c.getStaffSummary().getRo()).isEqualTo("Nu mai e hârtie în toaleta de lângă banda 2");
		}
	}

	@Test
	void whenThePassengerInsistedOnlyAReportIsAccepted() {
		assertThat(validator.validate(report(), input(true)).getKind()).isEqualTo(Classification.Kind.REPORT);
		assertRejected(question(), input(true), "insisted");
		assertRejected(garbage(), input(true), "insisted");
	}

	@Test
	void aMalformedFlightNumberIsDroppedAndAValidOneNormalised() {
		ClassificationAnswer bad = report();
		bad.setFlightNumber("flight to Rome");
		ClassificationAnswer good = report();
		good.setFlightNumber("w6 3221");

		assertThat(validator.validate(bad, input).getFlightNumber()).isNull();
		assertThat(validator.validate(good, input).getFlightNumber()).isEqualTo("W63221");
	}

	@Test
	void confidenceIsClampedAndABlankMentionedPlaceIsNull() {
		ClassificationAnswer high = report();
		high.setConfidence(7.0);
		high.setMentionedPlace("  ");
		ClassificationAnswer missing = report();
		missing.setConfidence(null);

		assertThat(validator.validate(high, input).getConfidence()).isEqualTo(1.0);
		assertThat(validator.validate(high, input).getMentionedPlace()).isNull();
		assertThat(validator.validate(missing, input).getConfidence()).isZero();
	}

	@Test
	void aMentionedPlaceThatRepeatsTheScannedLocationIsDropped() {
		ClassificationInput fromToilet = new ClassificationInput("Chiuveta e înfundată", Language.RO,
				Optional.of(new LocationInfo("Toalete sosiri, lângă banda 2", "Arrivals toilets near belt 2")), false);
		ClassificationAnswer repeated = report();
		repeated.setMentionedPlace("Toalete sosiri, langa banda 2");
		ClassificationAnswer partOfIt = report();
		partOfIt.setMentionedPlace("near belt 2");
		ClassificationAnswer elsewhere = report();
		elsewhere.setMentionedPlace("scara rulantă de la check-in");

		assertThat(validator.validate(repeated, fromToilet).getMentionedPlace()).isNull();
		assertThat(validator.validate(partOfIt, fromToilet).getMentionedPlace()).isNull();
		assertThat(validator.validate(elsewhere, fromToilet).getMentionedPlace()).isEqualTo("scara rulantă de la check-in");
	}

	private void assertRejected(ClassificationAnswer answer, String field) {
		assertRejected(answer, input, field);
	}

	private void assertRejected(ClassificationAnswer answer, ClassificationInput in, String field) {
		assertThatThrownBy(() -> validator.validate(answer, in))
				.isInstanceOf(ClassificationRejected.class)
				.hasMessageContaining(field);
	}

	private static ClassificationInput input(boolean passengerInsisted) {
		return new ClassificationInput("Nu mai e hârtie în toaleta de lângă banda 2", Language.RO, Optional.empty(), passengerInsisted);
	}

	private static ClassificationAnswer base(String kind) {
		ClassificationAnswer a = new ClassificationAnswer();
		a.setKind(kind);
		a.setEmergency(false);
		a.setLanguage("ro");
		a.setConfidence(0.9);
		a.setAnswerFound(false);
		return a;
	}

	private static ClassificationAnswer report() {
		ClassificationAnswer a = base("REPORT");
		a.setCategory("TOILET");
		a.setSeverity("MEDIUM");
		a.setStaffSummary(new ClassificationAnswer.Summary("Lipsește hârtia igienică lângă banda 2.", "No toilet paper near belt 2."));
		return a;
	}

	private static ClassificationAnswer question() {
		ClassificationAnswer a = base("QUESTION");
		a.setAnswer("Biroul de informații este în hala de check-in.");
		a.setAnswerFound(true);
		return a;
	}

	private static ClassificationAnswer garbage() {
		ClassificationAnswer a = base("GARBAGE");
		a.setBounceReason("JOKE_OR_NONSENSE");
		return a;
	}
}
