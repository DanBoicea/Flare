package com.fsociety.flarebackend.common;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** One UTC clock for the whole app, so time-based logic can be tested with a fixed clock. */
@Configuration
public class TimeConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
