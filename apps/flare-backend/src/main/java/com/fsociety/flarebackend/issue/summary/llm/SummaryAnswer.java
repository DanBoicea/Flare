package com.fsociety.flarebackend.issue.summary.llm;

import lombok.Getter;
import lombok.Setter;

/** The summarise answer as the model returned it (pipeline.md 4.3). Holds Passenger-derived text: no toString. */
@Getter
@Setter
public class SummaryAnswer {

	private Summary summary;

	@Getter
	@Setter
	public static class Summary {
		private String ro;
		private String en;
	}
}
