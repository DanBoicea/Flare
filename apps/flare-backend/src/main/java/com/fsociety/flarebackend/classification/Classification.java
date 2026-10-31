package com.fsociety.flarebackend.classification;

import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.Language;
import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.common.Severity;
import lombok.Builder;
import lombok.Value;

/**
 * The result of classifying one message (backend.md; field rules in pipeline.md section 4.1).
 * Holds Passenger-derived text (summary, answer), so toString is replaced to keep it out of logs.
 */
@Value
@Builder
public class Classification {

	public enum Kind {
		REPORT,
		QUESTION,
		GARBAGE
	}

	Kind kind;
	boolean emergency;
	/** Required for REPORT. */
	Category category;
	/** Required for REPORT; CRITICAL whenever {@code emergency}. */
	Severity severity;
	Language language;
	/** 0 to 1; below the routing threshold the Report gets no Team. */
	double confidence;
	/** At most 140 characters per language. Required for REPORT. */
	LocalizedText staffSummary;
	String mentionedPlace;
	String flightNumber;
	/** Required for QUESTION. */
	String answer;
	boolean answerFound;
	/** Required for GARBAGE. */
	BounceReason bounceReason;
	ClassificationSource source;

	@Override
	public String toString() {
		return "Classification[kind=" + kind + ", emergency=" + emergency + ", category=" + category
				+ ", severity=" + severity + ", source=" + source + "]";
	}
}
