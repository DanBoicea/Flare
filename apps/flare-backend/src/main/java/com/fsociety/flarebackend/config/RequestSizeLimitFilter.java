package com.fsociety.flarebackend.config;

import java.io.IOException;

import com.fsociety.flarebackend.common.ErrorCode;
import com.fsociety.flarebackend.common.ProblemResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rejects request bodies over 16 KB before anything reads them. A Passenger message is at most about
 * 1 KB of text, so anything bigger is abuse. Bodies without a Content-Length (chunked) are rejected too,
 * because their size can't be checked up front; browsers always send a Content-Length for fetch bodies.
 */
public class RequestSizeLimitFilter extends OncePerRequestFilter {

	static final long MAX_BODY_BYTES = 16 * 1024;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		long length = request.getContentLengthLong();
		boolean chunked = length < 0 && request.getHeader(HttpHeaders.TRANSFER_ENCODING) != null;
		if (length > MAX_BODY_BYTES || chunked) {
			ProblemResponses.write(response, HttpStatus.CONTENT_TOO_LARGE, ErrorCode.INVALID_REQUEST);
			return;
		}
		chain.doFilter(request, response);
	}
}
