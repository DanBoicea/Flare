package com.fsociety.flarebackend.issue.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Body of acknowledge, take, resolve and reopen (contract schema {@code VersionRequest}): the version the app saw. */
@Getter
@Setter
public class VersionRequest {

	@NotNull
	private Long version;
}
