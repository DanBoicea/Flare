package com.fsociety.flarebackend.intake;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fsociety.flarebackend.intake.dto.MessageRequest;
import com.fsociety.flarebackend.intake.dto.MessageResponse;

/** Passenger messages (api.md). Validation errors become Problem Details with a {@code code}. */
@RestController
@RequestMapping("/api/public/messages")
@RequiredArgsConstructor
public class PublicMessageController {

	private final MessageIntakeService intake;

	@PostMapping
	public MessageResponse sendMessage(@Valid @RequestBody MessageRequest request, HttpServletRequest http) {
		// The network address is only hashed (ADR-0018); the raw value is never stored or logged.
		return intake.handle(request, http.getRemoteAddr());
	}
}
