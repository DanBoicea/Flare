package com.fsociety.flarebackend.staff.dto;

import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.staff.TeamId;
import lombok.Value;

/** One item of {@code GET /api/staff/teams} (contract schema {@code Team}; renamed to avoid clashing with the entity). */
@Value
public class TeamResponse {
	TeamId id;
	LocalizedText name;
}
