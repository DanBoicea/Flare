package com.fsociety.flarebackend.staff;

import com.fsociety.flarebackend.staff.dto.Me;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Who is signed in: the Staff app loads this after sign-in to show the right tabs and Teams. */
@RestController
@RequiredArgsConstructor
public class MeController {

	private final CurrentStaff currentStaff;

	@GetMapping("/api/staff/me")
	public Me me() {
		StaffMember member = currentStaff.require();
		// Sorted, so the response is the same every time.
		return new Me(member.getId(), member.getDisplayName(),
				member.getRoles().stream().sorted().toList(),
				member.getTeams().stream().sorted().toList());
	}
}
