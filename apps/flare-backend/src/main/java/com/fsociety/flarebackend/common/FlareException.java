package com.fsociety.flarebackend.common;

import lombok.Getter;

/** Thrown by our code to end a request with a given {@link ErrorCode}; mapped to Problem Details by {@link ApiExceptionHandler}. */
@Getter
public class FlareException extends RuntimeException {

	private final ErrorCode code;

	public FlareException(ErrorCode code) {
		super(code.name());
		this.code = code;
	}

	public FlareException(ErrorCode code, String detail) {
		super(detail);
		this.code = code;
	}
}
