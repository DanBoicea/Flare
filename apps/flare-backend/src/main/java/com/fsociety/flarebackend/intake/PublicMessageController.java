package com.fsociety.flarebackend.intake;

import com.fsociety.flarebackend.intake.dto.MessageRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Passenger messages. A stub until A06: it validates the request, then answers 501. */
@RestController
@RequestMapping("/api/public/messages")
public class PublicMessageController {

	@PostMapping
	public void sendMessage(@Valid @RequestBody MessageRequest request) {
		// TODO(A06): hand the message to MessageIntakeService and return a MessageResponse.
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED);
	}
}
