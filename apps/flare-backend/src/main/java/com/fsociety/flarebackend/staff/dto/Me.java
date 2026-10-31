package com.fsociety.flarebackend.staff.dto;

import java.util.List;
import java.util.UUID;

import com.fsociety.flarebackend.staff.StaffRole;
import com.fsociety.flarebackend.staff.TeamId;
import lombok.Value;

/** Response of {@code GET /api/staff/me} (contract schema {@code Me}). */
@Value
public class Me {
	UUID id;
	String displayName;
	List<StaffRole> roles;
	List<TeamId> teams;
}
