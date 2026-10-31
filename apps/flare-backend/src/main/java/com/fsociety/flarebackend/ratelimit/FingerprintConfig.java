package com.fsociety.flarebackend.ratelimit;

import com.fsociety.flarebackend.config.FlareProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The {@link ClientFingerprint} bean, keyed with {@code flare.security.hash-secret}. */
@Configuration
public class FingerprintConfig {

	@Bean
	public ClientFingerprint clientFingerprint(FlareProperties properties) {
		return new ClientFingerprint(properties.getSecurity().getHashSecret());
	}
}
