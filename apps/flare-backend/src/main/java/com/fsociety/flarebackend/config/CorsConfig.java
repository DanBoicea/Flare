package com.fsociety.flarebackend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Only the passenger web app's origins may call the public API from a browser. The Staff app is native,
 * so staff endpoints get no CORS rules at all.
 */
@Configuration
public class CorsConfig {

	@Bean
	public CorsConfigurationSource corsConfigurationSource(FlareProperties properties) {
		CorsConfiguration publicApi = new CorsConfiguration();
		publicApi.setAllowedOrigins(properties.getCors().getAllowedOrigins());
		publicApi.setAllowedMethods(List.of("POST", "OPTIONS"));
		publicApi.setAllowedHeaders(List.of("Content-Type"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/public/**", publicApi);
		return source;
	}
}
