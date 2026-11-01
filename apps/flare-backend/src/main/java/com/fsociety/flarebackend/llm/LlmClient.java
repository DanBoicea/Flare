package com.fsociety.flarebackend.llm;

/**
 * The one gateway to the LLM (ADR-0022). Implementations constrain the answer to {@code jsonSchema} and parse it
 * into {@code type}; anything else throws {@link LlmFailure}. Callers still validate the result in their own code.
 */
public interface LlmClient {

	/**
	 * @param systemPrompt our instructions only; keep it identical across calls of one task so the server can
	 *                     reuse the cached prompt
	 * @param userContent  the per-call input, with untrusted Passenger text inside tags
	 * @param jsonSchema   a JSON Schema document the answer must follow
	 */
	<T> T call(LlmTask task, String systemPrompt, String userContent, String jsonSchema, Class<T> type);
}
