package com.fsociety.flarebackend.intake.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Body of {@code POST /api/public/messages} (contract schema {@code MessageRequest}).
 * No generated toString on purpose: it would print Passenger text and contact details into logs.
 */
@Getter
@Setter
public class MessageRequest {

	@NotBlank(message = "TEXT_EMPTY")
	@Size(max = 1000, message = "TEXT_TOO_LONG")
	private String text;

	@NotNull
	@Pattern(regexp = "ro|en")
	private String uiLanguage;

	/** Any value that isn't a known QR code ID is treated as unknown, not rejected. */
	@Size(max = 32)
	private String qrCodeId;

	@NotNull
	private UUID deviceId;

	@Size(max = 120)
	private String contact;

	private boolean sendAnyway;

	/** Honeypot (ADR-0019): must be empty, but checked by the bot check, not rejected here. */
	@Size(max = 200)
	private String website;

	@PositiveOrZero
	private Long elapsedMs;
}
