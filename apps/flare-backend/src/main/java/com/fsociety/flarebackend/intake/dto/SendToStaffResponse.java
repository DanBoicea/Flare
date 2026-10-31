package com.fsociety.flarebackend.intake.dto;

import lombok.Value;

/** Response of "Send to staff anyway" (contract schema {@code SendToStaffResponse}): always {@code REPORTED}. */
@Value
public class SendToStaffResponse {

	Outcome outcome = Outcome.REPORTED;
}
