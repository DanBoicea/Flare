package com.fsociety.flarebackend.issue.dto;

import java.util.UUID;

import com.fsociety.flarebackend.staff.StaffMember;
import lombok.Value;

/** A staff member as shown on an Issue (contract schema {@code StaffRef}). */
@Value
public class StaffRef {
	UUID id;
	String displayName;

	public static StaffRef of(StaffMember member) {
		return member == null ? null : new StaffRef(member.getId(), member.getDisplayName());
	}
}
