package com.fsociety.flarebackend.issue.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import com.fsociety.flarebackend.staff.TeamId;

/** Body of hand over (contract schema {@code HandOverRequest}). An unknown Team name is a 400. */
@Getter
@Setter
public class HandOverRequest {

	@NotNull
	private Long version;

	@NotNull
	private TeamId team;
}
