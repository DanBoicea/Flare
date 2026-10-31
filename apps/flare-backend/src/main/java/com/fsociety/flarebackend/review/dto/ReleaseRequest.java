package com.fsociety.flarebackend.review.dto;

import lombok.Getter;
import lombok.Setter;

import com.fsociety.flarebackend.staff.TeamId;

/** Body of release (contract schema {@code ReleaseRequest}). {@code team} null means "route normally". */
@Getter
@Setter
public class ReleaseRequest {

	private TeamId team;
}
