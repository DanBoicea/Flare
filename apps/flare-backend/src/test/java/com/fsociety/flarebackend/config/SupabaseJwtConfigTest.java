package com.fsociety.flarebackend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

/** The claim checks every Supabase access token must pass (the signature is checked separately by the decoder). */
class SupabaseJwtConfigTest {

	private static final String ISSUER = "https://nnyinitbozugulimhvvy.supabase.co/auth/v1";

	private final OAuth2TokenValidator<Jwt> validator = SupabaseJwtConfig.validator(ISSUER);

	@Test
	void acceptsASignedInUsersToken() {
		assertThat(validator.validate(token(ISSUER, List.of("authenticated"), Instant.now().plusSeconds(600))).hasErrors()).isFalse();
	}

	@Test
	void rejectsAnotherProjectsToken() {
		Jwt otherProject = token("https://someoneelse.supabase.co/auth/v1", List.of("authenticated"), Instant.now().plusSeconds(600));

		assertThat(validator.validate(otherProject).hasErrors()).isTrue();
	}

	@Test
	void rejectsATokenNotMeantForSignedInUsers() {
		Jwt anonymous = token(ISSUER, List.of("anon"), Instant.now().plusSeconds(600));

		assertThat(validator.validate(anonymous).hasErrors()).isTrue();
	}

	@Test
	void rejectsAnExpiredToken() {
		Jwt expired = token(ISSUER, List.of("authenticated"), Instant.now().minus(Duration.ofMinutes(5)));

		assertThat(validator.validate(expired).hasErrors()).isTrue();
	}

	private static Jwt token(String issuer, List<String> audience, Instant expiresAt) {
		return Jwt.withTokenValue("token")
				.header("alg", "ES256")
				.issuer(issuer)
				.audience(audience)
				.subject("2b1c0e4a-7f3d-4c2b-9a8e-1d6f5c4b3a29")
				.issuedAt(expiresAt.minusSeconds(3600))
				.expiresAt(expiresAt)
				.build();
	}
}
