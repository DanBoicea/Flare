package com.fsociety.flarebackend.llm;

/**
 * Any LLM call that did not produce a usable answer: server down or loading, timeout, HTTP error, cut-off or
 * invalid output, or the daily cap. Callers catch it (like any RuntimeException) and use their fallback.
 * The message names the reason only; it never contains prompt or answer text.
 */
public class LlmFailure extends RuntimeException {

	public LlmFailure(String reason) {
		super(reason);
	}

	public LlmFailure(String reason, Throwable cause) {
		super(reason, cause);
	}
}
