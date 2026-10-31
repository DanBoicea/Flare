package com.fsociety.flarebackend.review;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fsociety.flarebackend.review.dto.ReleaseRequest;
import com.fsociety.flarebackend.review.dto.ReviewList;
import com.fsociety.flarebackend.staff.CurrentStaff;

/** Needs review (api.md): the duty manager only; everyone else gets 403 FORBIDDEN from {@link ReviewService}. */
@RestController
@RequestMapping("/api/staff/review")
@RequiredArgsConstructor
public class ReviewController {

	private final CurrentStaff currentStaff;
	private final ReviewService reviews;

	@GetMapping
	public ReviewList list() {
		return reviews.list(currentStaff.require());
	}

	@PostMapping("/{reportId}/release")
	public void release(@PathVariable UUID reportId, @RequestBody ReleaseRequest request) {
		reviews.release(currentStaff.require(), reportId, request.getTeam());
	}

	@PostMapping("/{reportId}/dismiss")
	public void dismiss(@PathVariable UUID reportId) {
		reviews.dismiss(currentStaff.require(), reportId);
	}
}
