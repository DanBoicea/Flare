package com.fsociety.flarebackend.intake;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fsociety.flarebackend.intake.dto.SendToStaffRequest;
import com.fsociety.flarebackend.intake.dto.SendToStaffResponse;

/** "Send to staff anyway" for an answered Question (api.md). Idempotent; 404 for an unknown, old or foreign Question. */
@RestController
@RequestMapping("/api/public/questions")
@RequiredArgsConstructor
public class PublicQuestionController {

	private final MessageIntakeService intake;

	@PostMapping("/{questionId}/send-to-staff")
	public SendToStaffResponse sendToStaff(@PathVariable UUID questionId, @Valid @RequestBody SendToStaffRequest request,
			HttpServletRequest http) {
		intake.sendToStaff(questionId, request.getDeviceId(), http.getRemoteAddr());
		return new SendToStaffResponse();
	}
}
