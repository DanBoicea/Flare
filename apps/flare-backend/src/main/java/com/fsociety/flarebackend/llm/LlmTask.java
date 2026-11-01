package com.fsociety.flarebackend.llm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** The kinds of LLM call; each has its own settings under {@code flare.llm.tasks.<configKey>} (ADR-0016). */
@Getter
@RequiredArgsConstructor
public enum LlmTask {
	CLASSIFY("classify"),
	MATCH("match"),
	SUMMARIZE("summarize");

	private final String configKey;
}
