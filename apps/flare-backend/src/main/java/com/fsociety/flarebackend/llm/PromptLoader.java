package com.fsociety.flarebackend.llm;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * The system prompts in {@code src/main/resources/prompts/*.md}, versioned in git and changed only together with
 * an eval run. Loaded once at startup; LLM components ask for theirs in their constructor, so a missing prompt
 * stops the app from starting instead of failing on the first Passenger message.
 */
public class PromptLoader {

	private final Map<String, String> prompts = new TreeMap<>();

	public PromptLoader() {
		try {
			for (Resource resource : new PathMatchingResourcePatternResolver().getResources("classpath*:prompts/*.md")) {
				String name = resource.getFilename().replaceFirst("\\.md$", "");
				prompts.put(name, resource.getContentAsString(StandardCharsets.UTF_8));
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException("Could not read the prompts", e);
		}
	}

	/** The prompt from {@code prompts/<name>.md}, for example {@code get("classify.system")}. */
	public String get(String name) {
		String prompt = prompts.get(name);
		if (prompt == null) {
			throw new IllegalStateException("Missing prompt file prompts/" + name + ".md (have: " + prompts.keySet() + ")");
		}
		return prompt;
	}
}
