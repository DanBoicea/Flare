package com.fsociety.flarebackend.safety;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fsociety.flarebackend.config.FlareProperties;

/** The {@link BotCheck} bean, with the minimum fill time from {@code flare.limits.bot-check}. */
@Configuration
public class BotCheckConfig {

	@Bean
	public BotCheck botCheck(FlareProperties properties) {
		return new HoneypotBotCheck(properties.getLimits().getBotCheck().getMinElapsedMs());
	}
}
