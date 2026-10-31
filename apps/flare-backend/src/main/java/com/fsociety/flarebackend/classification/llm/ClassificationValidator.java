package com.fsociety.flarebackend.classification.llm;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import com.fsociety.flarebackend.classification.BounceReason;
import com.fsociety.flarebackend.classification.Classification;
import com.fsociety.flarebackend.classification.ClassificationInput;
import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.Language;
import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.common.TextLimits;

/**
 * Checks the model's classify answer against the output rules in pipeline.md section 4.1, in our own code,
 * because the LLM makes mistakes. Pure: no Spring, no I/O. Anything that breaks a rule throws
 * {@link ClassificationRejected}, so the caller falls back to an Unclassified Report.
 */
public class ClassificationValidator {

	static final int SUMMARY_MAX = 140;
	static final int ANSWER_MAX = 600;
	static final int MENTIONED_PLACE_MAX = 80;
	private static final Pattern FLIGHT_NUMBER = Pattern.compile("^[A-Z0-9]{2}\\d{1,4}[A-Z]?$");

	public Classification validate(ClassificationAnswer a, ClassificationInput input) {
		Classification.Kind kind = parse(Classification.Kind.class, a.getKind(), "kind");
		Language language = parseLanguage(a.getLanguage());
		boolean emergency = Boolean.TRUE.equals(a.getEmergency());
		if (input.isPassengerInsisted() && kind != Classification.Kind.REPORT && !emergency) {
			throw new ClassificationRejected("passenger insisted, so the kind must be REPORT, not " + kind);
		}

		Classification.ClassificationBuilder c = Classification.builder()
				.emergency(emergency)
				.language(language)
				.confidence(clamp(a.getConfidence()))
				.mentionedPlace(mentionedPlace(a.getMentionedPlace(), input))
				.flightNumber(flightNumber(a.getFlightNumber()))
				.source(ClassificationSource.LLM);

		// The CLAUDE.md safety boundary: an Emergency is never Bounced or answered, whatever kind the model chose.
		if (emergency) {
			return c.kind(Classification.Kind.REPORT)
					.category(a.getCategory() == null ? Category.OTHER : parse(Category.class, a.getCategory(), "category"))
					.severity(Severity.CRITICAL)
					.staffSummary(emergencySummary(a.getStaffSummary(), input.getText()))
					.build();
		}

		switch (kind) {
			case REPORT -> {
				noAnswer(a);
				noBounceReason(a);
				c.category(parse(Category.class, required(a.getCategory(), "category"), "category"))
						.severity(parse(Severity.class, required(a.getSeverity(), "severity"), "severity"))
						.staffSummary(summary(a.getStaffSummary()));
			}
			case QUESTION -> {
				noBounceReason(a);
				String answer = required(a.getAnswer(), "answer");
				checkLength(answer, ANSWER_MAX, "answer");
				if (a.getAnswerFound() == null) {
					throw new ClassificationRejected("answerFound is required for a QUESTION");
				}
				c.answer(answer.strip()).answerFound(a.getAnswerFound());
			}
			case GARBAGE -> {
				noAnswer(a);
				c.bounceReason(parse(BounceReason.class, required(a.getBounceReason(), "bounceReason"), "bounceReason"));
			}
		}
		return c.kind(kind).build();
	}

	private static LocalizedText summary(ClassificationAnswer.Summary summary) {
		if (summary == null) {
			throw new ClassificationRejected("staffSummary is required for a REPORT");
		}
		String ro = required(summary.getRo(), "staffSummary.ro");
		String en = required(summary.getEn(), "staffSummary.en");
		checkLength(ro, SUMMARY_MAX, "staffSummary.ro");
		checkLength(en, SUMMARY_MAX, "staffSummary.en");
		return new LocalizedText(ro.strip(), en.strip());
	}

	/** The model's summary if it is usable, otherwise the Passenger's text cut to length (as the fallback does). */
	private static LocalizedText emergencySummary(ClassificationAnswer.Summary summary, String text) {
		try {
			return summary(summary);
		}
		catch (ClassificationRejected e) {
			String cut = TextLimits.summary(text, SUMMARY_MAX);
			return new LocalizedText(cut, cut);
		}
	}

	private static void noAnswer(ClassificationAnswer a) {
		if (a.getAnswer() != null && !a.getAnswer().isBlank()) {
			throw new ClassificationRejected("answer is only allowed on a QUESTION");
		}
	}

	private static void noBounceReason(ClassificationAnswer a) {
		if (a.getBounceReason() != null && !a.getBounceReason().isBlank()) {
			throw new ClassificationRejected("bounceReason is only allowed on GARBAGE");
		}
	}

	/**
	 * Only a place that differs from the scanned Location is kept. The model often repeats the scanned Location
	 * here (seen in C02's checks), which would show staff a misleading "Mentioned:" line.
	 */
	private static String mentionedPlace(String place, ClassificationInput input) {
		if (place == null || place.isBlank()) {
			return null;
		}
		checkLength(place, MENTIONED_PLACE_MAX, "mentionedPlace");
		String mentioned = normalise(place);
		boolean sameAsScanned = input.getScannedLocation()
				.map(location -> List.of(normalise(location.getNameRo()), normalise(location.getNameEn())))
				.orElse(List.of())
				.stream()
				.anyMatch(scanned -> !scanned.isEmpty() && (scanned.contains(mentioned) || mentioned.contains(scanned)));
		return sameAsScanned ? null : place.strip();
	}

	/** Lower case, without diacritics or punctuation, so "Toalete sosiri, lângă banda 2" matches its variants. */
	private static String normalise(String text) {
		String plain = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return plain.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
	}

	/** Only a plausible flight number is kept (code rules: format-checked); anything else is dropped, not rejected. */
	private static String flightNumber(String flightNumber) {
		if (flightNumber == null) {
			return null;
		}
		String normalised = flightNumber.replaceAll("\\s+", "").toUpperCase();
		return FLIGHT_NUMBER.matcher(normalised).matches() ? normalised : null;
	}

	private static double clamp(Double confidence) {
		return confidence == null ? 0 : Math.max(0, Math.min(1, confidence));
	}

	private static Language parseLanguage(String code) {
		try {
			return Language.fromCode(required(code, "language"));
		}
		catch (IllegalArgumentException e) {
			throw new ClassificationRejected("language is not ro, en or other");
		}
	}

	private static <E extends Enum<E>> E parse(Class<E> type, String value, String field) {
		try {
			return Enum.valueOf(type, required(value, field));
		}
		catch (IllegalArgumentException e) {
			throw new ClassificationRejected(field + " is not a known value");
		}
	}

	private static String required(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new ClassificationRejected(field + " is required");
		}
		return value;
	}

	private static void checkLength(String value, int max, String field) {
		if (value.strip().codePointCount(0, value.strip().length()) > max) {
			throw new ClassificationRejected(field + " is longer than " + max + " characters");
		}
	}
}
