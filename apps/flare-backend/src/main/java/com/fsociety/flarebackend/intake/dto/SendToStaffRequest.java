package com.fsociety.flarebackend.intake.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Body of {@code POST /api/public/questions/{questionId}/send-to-staff} (contract schema {@code SendToStaffRequest}). */
@Getter
@Setter
public class SendToStaffRequest {

	@NotNull
	private UUID deviceId;
}
