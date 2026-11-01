package com.fsociety.flarebackend.classification.llm;

/** The model's answer breaks an output rule; the caller uses the fallback. The message names the rule, never text. */
public class ClassificationRejected extends RuntimeException {

	public ClassificationRejected(String reason) {
		super(reason);
	}
}
