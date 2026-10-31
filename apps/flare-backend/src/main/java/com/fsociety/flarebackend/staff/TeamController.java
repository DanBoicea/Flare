package com.fsociety.flarebackend.staff;

import java.util.Comparator;
import java.util.List;

import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.staff.dto.TeamResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** All Teams, for the hand-over picker. */
@RestController
@RequiredArgsConstructor
public class TeamController {

	private final TeamRepository teams;

	@GetMapping("/api/staff/teams")
	public List<TeamResponse> teams() {
		return teams.findAll().stream()
				.sorted(Comparator.comparing(Team::getId))
				.map(team -> new TeamResponse(team.getId(), new LocalizedText(team.getNameRo(), team.getNameEn())))
				.toList();
	}
}
