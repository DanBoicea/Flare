package com.fsociety.flarebackend.classification.llm;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The classify answer exactly as the model returned it (pipeline.md section 4.1). Nothing here is trusted:
 * {@link ClassificationValidator} turns it into a {@code Classification} or rejects it. Holds Passenger-derived
 * text, so it has no generated toString.
 */
@Getter
@Setter
public class ClassificationAnswer {

	private String kind;
	private Boolean emergency;
	private String category;
	private String severity;
	private String language;
	private Double confidence;
	private Summary staffSummary;
	private String mentionedPlace;
	private String flightNumber;
	private String answer;
	private Boolean answerFound;
	private String bounceReason;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Summary {
		private String ro;
		private String en;
	}
}
