package com.fsociety.flarebackend.config;

import com.fsociety.flarebackend.common.ErrorCode;
import com.fsociety.flarebackend.common.ProblemResponses;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
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
				// TODO(A03): .oauth2ResourceServer(...) with the Supabase JWKS decoder, issuer and audience checks.
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint((request, response, e) -> ProblemResponses.write(response, ErrorCode.UNAUTHORIZED))
						.accessDeniedHandler((request, response, e) -> ProblemResponses.write(response, ErrorCode.FORBIDDEN)));
		return http.build();
	}
}
