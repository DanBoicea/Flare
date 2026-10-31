package com.fsociety.flarebackend.issue.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.Severity;

/** Body of {@code PATCH /api/staff/issues/{id}} (contract schema {@code OverrideRequest}). At least one of the two. */
@Getter
@Setter
public class OverrideRequest {

	@NotNull
	private Long version;

	private Category category;

	private Severity severity;
}
