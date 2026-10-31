package com.fsociety.flarebackend.common;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into RFC 9457 Problem Details with a {@code code} property (docs/architecture/api.md).
 * Details never echo request values, so Passenger text can't leak into responses or logs.
 */
@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(FlareException.class)
	public ProblemDetail handleFlareException(FlareException e) {
		return problem(e.getCode());
	}

	@ExceptionHandler(Exception.class)
	public ProblemDetail handleUnexpected(Exception e) {
		log.error("Unhandled exception", e);
		return problem(ErrorCode.INTERNAL_ERROR);
	}

	/** Adds {@code code} to the Problem Details Spring builds for its own exceptions (validation, bad JSON, 404, 405, ...). */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
		if (response != null && response.getBody() instanceof ProblemDetail problem) {
			problem.setProperty("code", codeFor(ex, statusCode).name());
		}
		return response;
	}

	private static ErrorCode codeFor(Exception ex, HttpStatusCode statusCode) {
		if (ex instanceof MethodArgumentNotValidException invalid) {
			// DTO constraints name their code in the message, e.g. @NotBlank(message = "TEXT_EMPTY").
			return invalid.getBindingResult().getFieldErrors().stream()
					.map(FieldError::getDefaultMessage)
					.map(ErrorCode::fromValidationMessage)
					.filter(Objects::nonNull)
					.findFirst()
					.orElse(ErrorCode.INVALID_REQUEST);
		}
		return ErrorCode.forStatus(statusCode.value());
	}

	private static ProblemDetail problem(ErrorCode code) {
		ProblemDetail problem = ProblemDetail.forStatus(code.getStatus());
		problem.setProperty("code", code.name());
		return problem;
	}
}
