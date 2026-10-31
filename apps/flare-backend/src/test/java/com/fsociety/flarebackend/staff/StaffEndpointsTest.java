package com.fsociety.flarebackend.staff;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.fsociety.flarebackend.config.CorsConfig;
import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.config.SecurityConfig;
import com.fsociety.flarebackend.config.SupabaseJwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web layer only (no database). jwt() stands in for a token that already passed signature, expiry,
 * issuer and audience checks; SupabaseJwtConfigTest covers those checks.
 */
@WebMvcTest(controllers = { MeController.class, TeamController.class }, properties = {
		"FLARE_HASH_SECRET=test-secret",
		"SUPABASE_JWKS_URI=http://localhost/jwks.json",
		"SUPABASE_JWT_ISSUER=http://localhost/auth/v1"
})
@Import({ SecurityConfig.class, CorsConfig.class, SupabaseJwtConfig.class, CurrentStaff.class })
@EnableConfigurationProperties(FlareProperties.class)
class StaffEndpointsTest {

	private static final UUID MIHAI = UUID.fromString("2b1c0e4a-7f3d-4c2b-9a8e-1d6f5c4b3a29");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private StaffMemberRepository staffMembers;

	@MockitoBean
	private TeamRepository teams;

	@Test
	void meReturnsTheSignedInStaffMember() throws Exception {
		when(staffMembers.findById(MIHAI)).thenReturn(Optional.of(
				member(true, Set.of(StaffRole.STAFF), Set.of(TeamId.PASSENGER_ASSISTANCE, TeamId.MAINTENANCE))));

		mvc.perform(get("/api/staff/me").with(jwt().jwt(token -> token.subject(MIHAI.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(MIHAI.toString()))
				.andExpect(jsonPath("$.displayName").value("Mihai (simulat)"))
				.andExpect(jsonPath("$.roles[0]").value("STAFF"))
				.andExpect(jsonPath("$.teams[0]").value("MAINTENANCE"))
				.andExpect(jsonPath("$.teams[1]").value("PASSENGER_ASSISTANCE"));
	}

	@Test
	void noTokenIs401() throws Exception {
		mvc.perform(get("/api/staff/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", "Bearer"))
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void aMalformedTokenIs401() throws Exception {
		mvc.perform(get("/api/staff/me").header("Authorization", "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void aValidTokenWithoutAStaffMemberRowIs403() throws Exception {
		when(staffMembers.findById(MIHAI)).thenReturn(Optional.empty());

		mvc.perform(get("/api/staff/me").with(jwt().jwt(token -> token.subject(MIHAI.toString()))))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("NOT_A_STAFF_MEMBER"));
	}

	@Test
	void anInactiveStaffMemberIs403() throws Exception {
		when(staffMembers.findById(MIHAI)).thenReturn(Optional.of(member(false, Set.of(StaffRole.STAFF), Set.of(TeamId.CLEANING))));

		mvc.perform(get("/api/staff/me").with(jwt().jwt(token -> token.subject(MIHAI.toString()))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_A_STAFF_MEMBER"));
	}

	@Test
	void aSubjectThatIsNotAUuidIs403() throws Exception {
		mvc.perform(get("/api/staff/me").with(jwt().jwt(token -> token.subject("service-account"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_A_STAFF_MEMBER"));
	}

	@Test
	void everyStaffEndpointChecksTheStaffMemberRow() throws Exception {
		when(staffMembers.findById(MIHAI)).thenReturn(Optional.empty());

		mvc.perform(get("/api/staff/teams").with(jwt().jwt(token -> token.subject(MIHAI.toString()))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_A_STAFF_MEMBER"));
	}

	@Test
	void teamsListsEveryTeamWithBothNames() throws Exception {
		when(staffMembers.findById(MIHAI)).thenReturn(Optional.of(member(true, Set.of(StaffRole.STAFF), Set.of(TeamId.CLEANING))));
		when(teams.findAll()).thenReturn(List.of(team(TeamId.SECURITY, "Securitate", "Security"), team(TeamId.CLEANING, "Curățenie", "Cleaning")));

		mvc.perform(get("/api/staff/teams").with(jwt().jwt(token -> token.subject(MIHAI.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value("CLEANING"))
				.andExpect(jsonPath("$[0].name.ro").value("Curățenie"))
				.andExpect(jsonPath("$[1].id").value("SECURITY"))
				.andExpect(jsonPath("$[1].name.en").value("Security"));
	}

	private static StaffMember member(boolean active, Set<StaffRole> roles, Set<TeamId> teamIds) {
		StaffMember member = new StaffMember();
		member.setId(MIHAI);
		member.setDisplayName("Mihai (simulat)");
		member.setActive(active);
		member.getRoles().addAll(roles);
		member.getTeams().addAll(teamIds);
		return member;
	}

	private static Team team(TeamId id, String nameRo, String nameEn) {
		Team team = new Team();
		team.setId(id);
		team.setNameRo(nameRo);
		team.setNameEn(nameEn);
		return team;
	}
}
