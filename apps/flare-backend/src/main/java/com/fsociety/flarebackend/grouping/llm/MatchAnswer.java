package com.fsociety.flarebackend.grouping.llm;

import lombok.Getter;
import lombok.Setter;

/** The match answer as the model returned it (pipeline.md 4.2): a candidate label such as "C2", or null, and why. */
@Getter
@Setter
public class MatchAnswer {
	private String matchIssueId;
	private String reason;
}
