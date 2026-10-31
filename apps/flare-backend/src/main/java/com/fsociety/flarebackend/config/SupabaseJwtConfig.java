package com.fsociety.flarebackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Verifies Supabase access tokens (docs/architecture/auth-and-security.md). flare-dev uses asymmetric
 * signing keys published as a JWKS (S03), so no shared secret is needed. A token must have a valid
 * signature, not be expired, come from our project's issuer, and be meant for signed-in users.
 */
@Configuration
public class SupabaseJwtConfig {

	/** The audience Supabase puts in every signed-in user's access token. */
	static final String AUDIENCE = "authenticated";

	@Bean
	public JwtDecoder jwtDecoder(
			@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
			@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
				// flare-dev signs with ES256; RS256 is the other algorithm Supabase signing keys can use.
				.jwsAlgorithms(algorithms -> {
					algorithms.add(SignatureAlgorithm.ES256);
					algorithms.add(SignatureAlgorithm.RS256);
				})
				.build();
		decoder.setJwtValidator(validator(issuer));
		return decoder;
	}

	/** Expiry (with Spring's default clock skew), issuer and audience. */
	static OAuth2TokenValidator<Jwt> validator(String issuer) {
		return JwtValidators.createDefaultWithValidators(
				JwtValidators.createDefaultWithIssuer(issuer),
				new JwtAudienceValidator(AUDIENCE));
	}
}
