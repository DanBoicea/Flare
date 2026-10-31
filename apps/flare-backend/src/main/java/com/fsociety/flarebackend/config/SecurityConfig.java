package com.fsociety.flarebackend.config;

import java.io.IOException;

import com.fsociety.flarebackend.common.ErrorCode;
import com.fsociety.flarebackend.common.ProblemResponses;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CorsFilter;

/** Access rules for the whole API (docs/architecture/auth-and-security.md). */
@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				// No cookies or sessions: staff send a bearer token, Passengers send nothing, so CSRF can't apply.
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.addFilterAfter(new RequestSizeLimitFilter(), CorsFilter.class)
				.authorizeHttpRequests(auth -> auth
						// Lets Spring's own error responses through instead of turning them into 401s.
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers("/api/public/**", "/actuator/health").permitAll()
						.requestMatchers("/api/staff/**").authenticated()
						.anyRequest().denyAll())
				// Bearer tokens are checked by the JwtDecoder from SupabaseJwtConfig.
				.oauth2ResourceServer(resourceServer -> resourceServer
						.jwt(Customizer.withDefaults())
						.authenticationEntryPoint(SecurityConfig::unauthorized))
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint(SecurityConfig::unauthorized)
						.accessDeniedHandler((request, response, e) -> ProblemResponses.write(response, ErrorCode.FORBIDDEN)));
		return http.build();
	}

	/** Missing, expired or invalid token. The body never says which, so tokens can't be probed. */
	private static void unauthorized(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
			throws IOException {
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		ProblemResponses.write(response, ErrorCode.UNAUTHORIZED);
	}
}
