package com.fsociety.flarebackend.common;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Writes a Problem Details body from a servlet filter, where {@link ApiExceptionHandler} can't reach
 * (security errors, oversized bodies). The body only holds fixed values, so it is written by hand.
 */
public final class ProblemResponses {

	private ProblemResponses() {
	}

	public static void write(HttpServletResponse response, ErrorCode code) throws IOException {
		write(response, code.getStatus(), code);
	}

	public static void write(HttpServletResponse response, HttpStatus status, ErrorCode code) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.getWriter().write("{\"title\":\"%s\",\"status\":%d,\"code\":\"%s\"}"
				.formatted(status.getReasonPhrase(), status.value(), code.name()));
	}
}
