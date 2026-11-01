package com.fsociety.flarebackend.review.dto;

import java.util.List;

import lombok.Value;

/** {@code GET /api/staff/review} (contract schema {@code ReviewList}). */
@Value
public class ReviewList {

	List<ReviewItem> items;
	List<ReviewAlert> alerts;
}
