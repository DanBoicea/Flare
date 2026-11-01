package com.fsociety.flarebackend.eval;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import com.fsociety.flarebackend.classification.CategoryTeamRouting;
import com.fsociety.flarebackend.classification.Classification;
import com.fsociety.flarebackend.classification.ClassificationInput;
import com.fsociety.flarebackend.classification.FallbackClassifier;
import com.fsociety.flarebackend.classification.LocationInfo;
import com.fsociety.flarebackend.classification.llm.ClassificationValidator;
import com.fsociety.flarebackend.classification.llm.LlmMessageClassifier;
import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.Language;
import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.grouping.CandidateIssueView;
import com.fsociety.flarebackend.grouping.NewReportView;
import com.fsociety.flarebackend.grouping.llm.LlmIssueMatcher;
import com.fsociety.flarebackend.issue.IssueStatus;
import com.fsociety.flarebackend.knowledge.KnowledgeBase;
import com.fsociety.flarebackend.llm.LlamaServerClient;
import com.fsociety.flarebackend.llm.LlmUsageCounter;
import com.fsociety.flarebackend.llm.PromptLoader;
import com.fsociety.flarebackend.safety.EmergencyDetector;
import com.fsociety.flarebackend.safety.EmergencyTerms;
import com.fsociety.flarebackend.staff.TeamId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The evaluation (task C05): runs the SIMULATED labelled test set (data/test-set, D02) through the real classifier
 * and matcher on a running llama-server, prints the metrics from triage-and-dedup.md "Measuring quality" and writes
 * them to target/eval-results.md. Tagged "eval": only {@code ./mvnw test -Dgroups=eval} runs it. It takes tens of
 * minutes on a laptop; run it once per prompt or model change, then copy the result into docs/eval-results.md.
 * No database and no Spring context: the same classes the app uses, built by hand.
 */
@Tag("eval")
class EvaluationRunnerTest {

	private static final Path TEST_SET = Path.of("../../data/test-set");
	private static final Path AIRPORT = Path.of("../../data/airport/airport.json");
	private static final Path OUTPUT = Path.of("target/eval-results.md");
	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final List<Severity> SEVERITY_ORDER = List.of(Severity.CRITICAL, Severity.HIGH, Severity.MEDIUM, Severity.LOW);

	record Place(String label, String area, LocalizedText name) {
	}

	record Message(String id, String text, Language uiLanguage, Place place, Expected expected) {
	}

	record Expected(Classification.Kind kind, boolean emergency, Category category, Severity severity, Language language,
			Boolean answerFound) {
	}

	record Result(Message message, Classification got, boolean failed, String failure, boolean wordList, long millis) {
		boolean emergencyDetected() {
			return wordList || got.isEmergency();
		}
	}

	record Pair(String id, String textA, Place placeA, String textB, Place placeB, boolean sameIssue, String notes) {
	}

	record PairResult(Pair pair, boolean predictedSame, String how) {
	}

	@Test
	void evaluate() throws Exception {
		FlareProperties properties = bindProperties();
		FlareProperties.Llm llmSettings = properties.getLlm();
		JsonNode props = serverProps(llmSettings.getBaseUrl());

		LlamaServerClient llm = new LlamaServerClient(llmSettings, new LlmUsageCounter(100_000, Clock.systemUTC()));
		PromptLoader prompts = new PromptLoader();
		LlmMessageClassifier classifier = new LlmMessageClassifier(llm, prompts, new KnowledgeBase(), new ClassificationValidator());
		LlmIssueMatcher matcher = new LlmIssueMatcher(llm, prompts);
		EmergencyDetector wordList = EmergencyTerms.loadDetector();
		FallbackClassifier fallback = new FallbackClassifier(wordList);
		CategoryTeamRouting routing = new CategoryTeamRouting(properties.getRouting().getCategoryTeam(),
				properties.getRouting().getLowConfidenceThreshold());

		Map<String, Place> places = loadPlaces();
		List<Message> messages = loadMessages(places);
		List<Pair> pairs = loadPairs(places);
		System.out.printf("%nEvaluating %d messages and %d grouping pairs on %s (%s). This takes a while.%n%n",
				messages.size(), pairs.size(), fileName(props.path("model_path").asString("?")), llmSettings.getBaseUrl());

		List<Result> results = new ArrayList<>();
		for (Message m : messages) {
			ClassificationInput input = input(m.text(), m.uiLanguage(), m.place());
			long started = System.nanoTime();
			Classification got;
			boolean failed = false;
			String failure = null;
			try {
				got = classifier.classify(input);
			}
			catch (RuntimeException e) {
				// What production does: the fallback answers.
				got = fallback.classify(input);
				failed = true;
				failure = e.getClass().getSimpleName() + ": " + e.getMessage();
			}
			Result r = new Result(m, got, failed, failure, wordList.isEmergency(m.text()), (System.nanoTime() - started) / 1_000_000);
			results.add(r);
			System.out.printf("%s %6d ms %s%n", m.id(), r.millis(), describe(r.got()) + (failed ? " (FALLBACK)" : ""));
		}

		List<PairResult> pairResults = new ArrayList<>();
		for (Pair p : pairs) {
			PairResult r = groupPair(p, classifier, matcher, fallback);
			pairResults.add(r);
			System.out.printf("%s same=%s predicted=%s (%s)%n", p.id(), p.sameIssue(), r.predictedSame(), r.how());
		}

		String report = report(results, pairResults, routing, props, llmSettings, llm);
		System.out.println("\n" + report);
		Files.writeString(OUTPUT, report, StandardCharsets.UTF_8);
		System.out.println("Written to " + OUTPUT.toAbsolutePath());
		assertThat(results).hasSize(messages.size());
	}

	// ---------------------------------------------------------------- grouping pairs

	/**
	 * As the grouping worker does it: Reports in different Areas are never candidates for each other, so they stay
	 * apart without an LLM call; otherwise A becomes an Issue and the matcher decides whether B joins it.
	 */
	private static PairResult groupPair(Pair p, LlmMessageClassifier classifier, LlmIssueMatcher matcher, FallbackClassifier fallback) {
		if (!Objects.equals(p.placeA().area(), p.placeB().area())) {
			return new PairResult(p, false, "different Areas, not compared");
		}
		Classification a = classifyOrFallback(classifier, fallback, input(p.textA(), Language.EN, p.placeA()));
		Classification b = classifyOrFallback(classifier, fallback, input(p.textB(), Language.EN, p.placeB()));
		LocalizedText summaryA = a.getStaffSummary() != null ? a.getStaffSummary() : new LocalizedText(p.textA(), p.textA());
		CandidateIssueView issueA = new CandidateIssueView(java.util.UUID.randomUUID(), category(a), p.placeA().name(), summaryA,
				IssueStatus.NEW, List.of(p.textA()));
		try {
			boolean same = matcher.findMatch(new NewReportView(p.textB(), category(b), p.placeB().name()), List.of(issueA)).isPresent();
			return new PairResult(p, same, "matcher");
		}
		catch (RuntimeException e) {
			return new PairResult(p, false, "matcher failed: " + e.getClass().getSimpleName());
		}
	}

	private static Classification classifyOrFallback(LlmMessageClassifier classifier, FallbackClassifier fallback, ClassificationInput input) {
		try {
			return classifier.classify(input);
		}
		catch (RuntimeException e) {
			return fallback.classify(input);
		}
	}

	private static Category category(Classification c) {
		return c.getCategory() != null ? c.getCategory() : Category.OTHER;
	}

	// ---------------------------------------------------------------- the report

	private static String report(List<Result> results, List<PairResult> pairs, CategoryTeamRouting routing, JsonNode props,
			FlareProperties.Llm settings, LlamaServerClient llm) {
		List<Result> reports = filter(results, r -> r.message().expected().kind() == Classification.Kind.REPORT);
		List<Result> plainReports = filter(reports, r -> !r.message().expected().emergency());
		List<Result> questions = filter(results, r -> r.message().expected().kind() == Classification.Kind.QUESTION);
		List<Result> garbage = filter(results, r -> r.message().expected().kind() == Classification.Kind.GARBAGE);
		List<Result> genuine = filter(results, r -> r.message().expected().kind() != Classification.Kind.GARBAGE);
		List<Result> emergencies = filter(results, r -> r.message().expected().emergency());
		List<Result> notEmergencies = filter(results, r -> !r.message().expected().emergency());
		List<Long> latencies = results.stream().filter(r -> !r.failed()).map(Result::millis).sorted().toList();

		StringBuilder md = new StringBuilder();
		md.append("## Run ").append(LocalDate.now()).append("\n\n")
				.append("| | |\n| --- | --- |\n")
				.append("| Model file | `").append(fileName(props.path("model_path").asString("?"))).append("` |\n")
				.append("| llama.cpp | ").append(props.path("build_info").asString("?")).append(", ")
				.append(props.path("total_slots").asInt(0)).append(" slot(s) |\n")
				.append("| Machine | ").append(machine()).append(" |\n")
				.append("| Temperature | ").append(settings.getTasks().get("classify").getTemperature()).append(" |\n")
				.append("| Prompts | git ").append(gitVersion()).append(" |\n")
				.append("| Test set | ").append(results.size()).append(" messages, ").append(pairs.size()).append(" grouping pairs (simulated, D02) |\n\n");

		md.append("| Metric | Result |\n| --- | --- |\n")
				.append(row("Kind accuracy", count(results, r -> r.got().getKind() == r.message().expected().kind()), results.size()))
				.append(row("Category accuracy (Reports)", count(reports, r -> r.got().getCategory() == r.message().expected().category()), reports.size()))
				.append(row("Severity exact (Reports)", count(reports, r -> r.got().getSeverity() == r.message().expected().severity()), reports.size()))
				.append(row("Severity within one level (Reports)", count(reports, r -> withinOne(r.got().getSeverity(), r.message().expected().severity())), reports.size()))
				.append(row("Team routing (Reports without Emergencies)", count(plainReports, r -> sameTeam(routing, r)), plainReports.size()))
				.append(row("**Emergency recall** (word list + LLM; must be 100%)", count(emergencies, Result::emergencyDetected), emergencies.size()))
				.append(row("Emergency recall, word list alone", count(emergencies, Result::wordList), emergencies.size()))
				.append(row("False Emergencies (flagged, not labelled)", count(notEmergencies, Result::emergencyDetected), notEmergencies.size()))
				.append(row("Garbage catch rate", count(garbage, r -> r.got().getKind() == Classification.Kind.GARBAGE), garbage.size()))
				.append(row("**False-Bounce rate** (genuine Reports and Questions Bounced)", count(genuine, r -> r.got().getKind() == Classification.Kind.GARBAGE), genuine.size()))
				.append(row("Questions: covered / not covered right", count(questions, r -> r.got().getKind() == Classification.Kind.QUESTION
						&& Objects.equals(r.got().isAnswerFound(), r.message().expected().answerFound())), questions.size()))
				.append(row("Language", count(results, r -> r.got().getLanguage() == r.message().expected().language()), results.size()))
				.append(row("Grouping pairs right", count(pairs, p -> p.predictedSame() == p.pair().sameIssue()), pairs.size()))
				.append("| Grouping: false merges / false splits | ")
				.append(count(pairs, p -> p.predictedSame() && !p.pair().sameIssue())).append(" / ")
				.append(count(pairs, p -> !p.predictedSame() && p.pair().sameIssue())).append(" |\n")
				.append(row("LLM failures (fallback used)", count(results, Result::failed), results.size()))
				.append("| Classify latency p50 / p90 | ").append(percentile(latencies, 50)).append(" ms / ")
				.append(percentile(latencies, 90)).append(" ms |\n")
				.append("| Tokens (all calls) | ").append(llm.promptTokensUsed()).append(" prompt, ")
				.append(llm.completionTokensUsed()).append(" completion |\n\n");

		md.append("### Emergency misses\n\n");
		List<Result> missed = filter(emergencies, r -> !r.emergencyDetected());
		md.append(missed.isEmpty() ? "None.\n\n" : table(missed));
		md.append("### False Emergencies\n\n");
		List<Result> falseAlarms = filter(notEmergencies, Result::emergencyDetected);
		md.append(falseAlarms.isEmpty() ? "None.\n\n" : table(falseAlarms));
		md.append("### Misclassified (kind, category for Reports, or covered/not covered for Questions)\n\n");
		List<Result> wrong = filter(results, r -> !rightEnough(r));
		md.append(wrong.isEmpty() ? "None.\n\n" : table(wrong));
		md.append("### Grouping misses\n\n");
		List<PairResult> pairMisses = pairs.stream().filter(p -> p.predictedSame() != p.pair().sameIssue()).toList();
		if (pairMisses.isEmpty()) {
			md.append("None.\n\n");
		}
		else {
			md.append("| ID | Expected | Got | How | Notes |\n| --- | --- | --- | --- | --- |\n");
			pairMisses.forEach(p -> md.append("| ").append(p.pair().id()).append(" | ").append(p.pair().sameIssue() ? "same" : "different")
					.append(" | ").append(p.predictedSame() ? "same" : "different").append(" | ").append(p.how()).append(" | ")
					.append(cell(p.pair().notes())).append(" |\n"));
			md.append('\n');
		}
		List<Result> failures = filter(results, Result::failed);
		if (!failures.isEmpty()) {
			md.append("### LLM failures\n\n| ID | Reason |\n| --- | --- |\n");
			failures.forEach(r -> md.append("| ").append(r.message().id()).append(" | ").append(cell(r.failure())).append(" |\n"));
			md.append('\n');
		}
		return md.toString();
	}

	private static boolean rightEnough(Result r) {
		Expected e = r.message().expected();
		Classification got = r.got();
		if (got.getKind() != e.kind()) {
			return false;
		}
		return switch (e.kind()) {
			case REPORT -> got.getCategory() == e.category();
			case QUESTION -> Objects.equals(got.isAnswerFound(), e.answerFound());
			case GARBAGE -> true;
		};
	}

	private static boolean sameTeam(CategoryTeamRouting routing, Result r) {
		Optional<TeamId> expected = routing.teamFor(r.message().expected().category(), ClassificationSource.LLM, 1.0);
		Optional<TeamId> got = routing.teamFor(category(r.got()), r.got().getSource(), r.got().getConfidence());
		return expected.equals(got);
	}

	private static boolean withinOne(Severity got, Severity expected) {
		return got != null && expected != null && Math.abs(SEVERITY_ORDER.indexOf(got) - SEVERITY_ORDER.indexOf(expected)) <= 1;
	}

	private static String table(List<Result> results) {
		StringBuilder md = new StringBuilder("| ID | Expected | Got | Text |\n| --- | --- | --- | --- |\n");
		for (Result r : results) {
			Expected e = r.message().expected();
			String expected = e.kind() + (e.category() != null ? "/" + e.category() + "/" + e.severity() : "")
					+ (e.emergency() ? "/EMERGENCY" : "") + (e.answerFound() != null ? "/found=" + e.answerFound() : "");
			String got = describe(r.got()) + (r.wordList() ? "/word-list" : "") + (r.failed() ? " (FALLBACK)" : "");
			md.append("| ").append(r.message().id()).append(" | ").append(expected).append(" | ").append(got).append(" | ")
					.append(cell(r.message().text())).append(" |\n");
		}
		return md.append('\n').toString();
	}

	private static String describe(Classification c) {
		return c.getKind() + (c.getCategory() != null ? "/" + c.getCategory() + "/" + c.getSeverity() : "")
				+ (c.isEmergency() ? "/EMERGENCY" : "")
				+ (c.getKind() == Classification.Kind.QUESTION ? "/found=" + c.isAnswerFound() : "")
				+ (c.getBounceReason() != null ? "/" + c.getBounceReason() : "");
	}

	private static String row(String metric, long right, long total) {
		String percent = total == 0 ? "n/a" : String.format("%.0f%%", 100.0 * right / total);
		return "| " + metric + " | " + right + "/" + total + " (" + percent + ") |\n";
	}

	private static <T> List<T> filter(List<T> items, Predicate<T> keep) {
		return items.stream().filter(keep).toList();
	}

	private static <T> long count(List<T> items, Predicate<T> test) {
		return items.stream().filter(test).count();
	}

	private static long percentile(List<Long> sorted, int p) {
		return sorted.isEmpty() ? 0 : sorted.get(Math.min(sorted.size() - 1, (int) Math.ceil(p / 100.0 * sorted.size()) - 1));
	}

	private static String cell(String text) {
		String oneLine = text == null ? "" : text.replace('\n', ' ').replace("|", "\\|");
		return oneLine.length() > 90 ? oneLine.substring(0, 89) + "…" : oneLine;
	}

	// ---------------------------------------------------------------- inputs

	private static ClassificationInput input(String text, Language uiLanguage, Place place) {
		return new ClassificationInput(text, uiLanguage,
				Optional.ofNullable(place).map(p -> new LocationInfo(p.name().getRo(), p.name().getEn())), false);
	}

	private static Map<String, Place> loadPlaces() throws IOException {
		Map<String, Place> places = new HashMap<>();
		for (JsonNode l : JSON.readTree(Files.readString(AIRPORT)).path("locations")) {
			places.put(l.path("label").asString(), new Place(l.path("label").asString(), l.path("area").asString(),
					new LocalizedText(l.path("name").path("ro").asString(), l.path("name").path("en").asString())));
		}
		return places;
	}

	private static List<Message> loadMessages(Map<String, Place> places) throws IOException {
		List<Message> messages = new ArrayList<>();
		for (JsonNode m : jsonl("messages.jsonl")) {
			JsonNode e = m.path("expected");
			messages.add(new Message(m.path("id").asString(), m.path("text").asString(),
					Language.fromCode(m.path("uiLanguage").asString()), places.get(text(m, "scannedLocation")),
					new Expected(Classification.Kind.valueOf(e.path("kind").asString()), e.path("emergency").asBoolean(),
							enumOrNull(Category.class, text(e, "category")), enumOrNull(Severity.class, text(e, "severity")),
							Language.fromCode(e.path("language").asString()),
							e.path("answerFound").isBoolean() ? e.path("answerFound").asBoolean() : null)));
		}
		return messages;
	}

	private static List<Pair> loadPairs(Map<String, Place> places) throws IOException {
		List<Pair> pairs = new ArrayList<>();
		for (JsonNode p : jsonl("grouping-pairs.jsonl")) {
			pairs.add(new Pair(p.path("id").asString(), p.path("a").path("text").asString(), places.get(p.path("a").path("location").asString()),
					p.path("b").path("text").asString(), places.get(p.path("b").path("location").asString()),
					p.path("sameIssue").asBoolean(), text(p, "notes")));
		}
		return pairs;
	}

	private static List<JsonNode> jsonl(String file) throws IOException {
		return Files.readAllLines(TEST_SET.resolve(file)).stream().filter(line -> !line.isBlank()).map(JSON::readTree).toList();
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.path(field);
		return value.isNull() || value.isMissingNode() ? null : value.asString();
	}

	private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
		return value == null ? null : Enum.valueOf(type, value);
	}

	// ---------------------------------------------------------------- environment

	/** The real application.yml, with LLM settings from the environment (FLARE_LLM_BASE_URL, FLARE_LLM_API_KEY). */
	private static FlareProperties bindProperties() throws IOException {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addFirst(new MapPropertySource("eval", Map.of("FLARE_HASH_SECRET", "eval-only", "FLARE_LLM_ENABLED", "true")));
		sources.addLast(new SystemEnvironmentPropertySource("env", new HashMap<>(System.getenv())));
		new YamlPropertySourceLoader().load("application.yml", new ClassPathResource("application.yml")).forEach(sources::addLast);
		return new Binder(ConfigurationPropertySources.from(sources), new PropertySourcesPlaceholdersResolver(sources))
				.bind("flare", FlareProperties.class).get();
	}

	/** Fails early, with a clear message, when llama-server is not running. */
	private static JsonNode serverProps(String baseUrl) throws Exception {
		HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
		try {
			HttpResponse<String> health = http.send(HttpRequest.newBuilder(URI.create(baseUrl + "/health")).build(),
					HttpResponse.BodyHandlers.ofString());
			if (health.statusCode() != 200) {
				throw new IllegalStateException("llama-server at " + baseUrl + " is not ready: " + health.body());
			}
			return JSON.readTree(http.send(HttpRequest.newBuilder(URI.create(baseUrl + "/props")).build(),
					HttpResponse.BodyHandlers.ofString()).body());
		}
		catch (IOException e) {
			throw new IllegalStateException("llama-server is not reachable at " + baseUrl
					+ ". Start it with: node scripts/llm/start-llama-server.mjs", e);
		}
	}

	private static String machine() {
		String cpu = run("sysctl", "-n", "machdep.cpu.brand_string");
		String memory = run("sysctl", "-n", "hw.memsize");
		String gb = memory.matches("\\d+") ? Math.round(Long.parseLong(memory) / 1e9) + " GB" : "";
		return (cpu.isBlank() ? System.getProperty("os.name") + " " + System.getProperty("os.arch") : cpu + ", " + gb).strip();
	}

	private static String gitVersion() {
		String hash = run("git", "rev-parse", "--short", "HEAD");
		boolean dirty = !run("git", "status", "--porcelain", "--", "src/main/resources/prompts").isBlank();
		return (hash.isBlank() ? "unknown" : hash) + (dirty ? " + uncommitted prompt changes" : "");
	}

	private static String run(String... command) {
		try {
			Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
			String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
			return process.waitFor() == 0 ? out : "";
		}
		catch (IOException | InterruptedException e) {
			return "";
		}
	}

	private static String fileName(String path) {
		return Path.of(path).getFileName().toString();
	}
}
