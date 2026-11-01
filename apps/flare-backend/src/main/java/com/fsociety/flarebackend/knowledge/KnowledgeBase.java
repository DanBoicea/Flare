package com.fsociety.flarebackend.knowledge;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * The simulated airport Knowledge base (data/knowledge-base/, packaged as classpath:knowledge-base/ by the pom;
 * ADR-0005, ADR-0014). Loaded once at startup and put whole into the classify system prompt: each file is one
 * article, starting with its "# Title" heading, in file-name order.
 */
@Slf4j
public class KnowledgeBase {

	/** A rough average for Romanian and English text; enough to notice the prompt growing too big. */
	private static final double CHARS_PER_TOKEN = 3.5;
	private static final int WARN_TOKENS = 30_000;

	private final String text;

	public KnowledgeBase() {
		this(loadArticles());
	}

	/** A Knowledge base from given text, for example a small stub in a test. */
	public KnowledgeBase(String text) {
		this.text = text;
		int tokens = approximateTokens();
		if (text.isBlank()) {
			log.warn("Knowledge base is empty: Questions will all be 'not covered'");
		}
		else if (tokens > WARN_TOKENS) {
			log.warn("Knowledge base is about {} tokens, over {}: the classify prompt may get slow; consider retrieval (ADR-0014)", tokens, WARN_TOKENS);
		}
		else {
			log.info("Knowledge base loaded: {} characters, about {} tokens", text.length(), tokens);
		}
	}

	public String text() {
		return text;
	}

	public int approximateTokens() {
		return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
	}

	private static String loadArticles() {
		try {
			Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath*:knowledge-base/*.md");
			return Arrays.stream(files)
					.filter(file -> !"README.md".equals(file.getFilename()))
					.sorted(Comparator.comparing(Resource::getFilename))
					.map(KnowledgeBase::read)
					.collect(Collectors.joining("\n\n"));
		}
		catch (IOException e) {
			throw new UncheckedIOException("Could not list the Knowledge base", e);
		}
	}

	private static String read(Resource file) {
		try {
			return file.getContentAsString(StandardCharsets.UTF_8).strip();
		}
		catch (IOException e) {
			throw new UncheckedIOException("Could not read " + file.getFilename(), e);
		}
	}
}
