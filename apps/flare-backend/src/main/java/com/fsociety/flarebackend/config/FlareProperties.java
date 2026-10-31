package com.fsociety.flarebackend.config;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** The whole {@code flare:} block of application.yml (docs/architecture/backend.md), bound once at startup. */
@Value
@Validated
@ConfigurationProperties("flare")
public class FlareProperties {

	Llm llm;
	Limits limits;
	Grouping grouping;
	Routing routing;
	Priority priority;
	Retention retention;
	Cors cors;
	@Valid
	@NotNull
	Security security;

	@Value
	public static class Llm {
		boolean enabled;
		int dailyCallLimit;
		/** Keyed by task name: classify, match, summarize. */
		Map<String, LlmTask> tasks;
	}

	@Value
	public static class LlmTask {
		String model;
		String effort;
		int maxTokens;
		Duration timeout;
	}

	@Value
	public static class Limits {
		Device device;
		Network network;
		QrCode qrCode;
		Bounce bounce;
		BotCheck botCheck;
	}

	@Value
	public static class Device {
		int per10Min;
		int perDay;
	}

	@Value
	public static class Network {
		int per10Min;
	}

	@Value
	public static class QrCode {
		int per10Min;
		Duration strictModeDuration;
	}

	@Value
	public static class Bounce {
		Duration window;
		int sendAnywayAfter;
		int blockAfter;
	}

	@Value
	public static class BotCheck {
		long minElapsedMs;
	}

	@Value
	public static class Grouping {
		Duration window;
		int maxCandidates;
	}

	@Value
	public static class Routing {
		double lowConfidenceThreshold;
		/** Category name to TeamId name; a Category missing here (OTHER) goes to no Team, so the duty manager. */
		Map<String, String> categoryTeam;
	}

	@Value
	public static class Priority {
		/** Keyed by Severity name. */
		Map<String, Integer> severityWeight;
		int perExtraReporter;
		int maxExtraReporters;
		int maxAgeMinutes;
		int perCriticality;
		int reopenedBonus;
	}

	@Value
	public static class Retention {
		int contactDaysAfterResolve;
		int questionDays;
		int issueDaysAfterResolve;
		int dismissedDays;
	}

	@Value
	public static class Cors {
		List<String> allowedOrigins;
	}

	@Value
	public static class Security {
		/** HMAC key for device and network hashes (ADR-0018). The app refuses to start without it. */
		@NotBlank
		String hashSecret;
	}
}
