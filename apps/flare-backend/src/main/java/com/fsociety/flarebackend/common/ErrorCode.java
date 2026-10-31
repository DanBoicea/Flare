package com.fsociety.flarebackend.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** Stable machine codes sent as the {@code code} property of every error (contract schema {@code ErrorCode}). */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
	TEXT_EMPTY(HttpStatus.BAD_REQUEST),
	TEXT_TOO_LONG(HttpStatus.BAD_REQUEST),
	INVALID_REQUEST(HttpStatus.BAD_REQUEST),
	NOT_FOUND(HttpStatus.NOT_FOUND),
	FORBIDDEN(HttpStatus.FORBIDDEN),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
	NOT_A_STAFF_MEMBER(HttpStatus.FORBIDDEN),
	VERSION_CONFLICT(HttpStatus.CONFLICT),
	INVALID_TRANSITION(HttpStatus.CONFLICT),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

	private final HttpStatus status;

	/** The code for an error that Spring raised before reaching our code (404, 405, 415, 5xx, ...). */
	public static ErrorCode forStatus(int status) {
		return switch (status) {
			case 401 -> UNAUTHORIZED;
			case 403 -> FORBIDDEN;
			case 404 -> NOT_FOUND;
			default -> status >= 500 ? INTERNAL_ERROR : INVALID_REQUEST;
		};
	}

	/** The code named by a validation message such as {@code @NotBlank(message = "TEXT_EMPTY")}, if any. */
	public static ErrorCode fromValidationMessage(String message) {
		for (ErrorCode code : values()) {
			if (code.name().equals(message)) {
				return code;
			}
		}
		return null;
	}
}
