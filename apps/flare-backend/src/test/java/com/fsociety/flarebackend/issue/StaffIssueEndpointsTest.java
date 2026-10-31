package com.fsociety.flarebackend.issue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.fsociety.flarebackend.audit.AuditAction;
import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.ErrorCode;
import com.fsociety.flarebackend.common.FlareException;
import com.fsociety.flarebackend.common.Language;
import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.config.CorsConfig;
import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.config.SecurityConfig;
import com.fsociety.flarebackend.config.SupabaseJwtConfig;
import com.fsociety.flarebackend.issue.dto.HistoryEntry;
import com.fsociety.flarebackend.issue.dto.IssueCard;
import com.fsociety.flarebackend.issue.dto.IssueDetail;
import com.fsociety.flarebackend.issue.dto.IssueList;
import com.fsociety.flarebackend.issue.dto.Priority;
import com.fsociety.flarebackend.issue.dto.PriorityReason;
import com.fsociety.flarebackend.issue.dto.PriorityReasonCode;
import com.fsociety.flarebackend.issue.dto.ReportView;
import com.fsociety.flarebackend.staff.CurrentStaff;
import com.fsociety.flarebackend.staff.StaffMember;
import com.fsociety.flarebackend.staff.StaffMemberRepository;
import com.fsociety.flarebackend.staff.StaffRole;
import com.fsociety.flarebackend.staff.TeamId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Web layer only (no database): the Issue endpoints' JSON shape against the contract, sign-in and errors. */
@WebMvcTest(controllers = StaffIssueController.class, properties = {
		"FLARE_HASH_SECRET=test-secret",
		"SUPABASE_JWKS_URI=http://localhost/jwks.json",
		"SUPABASE_JWT_ISSUER=http://localhost/auth/v1"
})
@Import({ SecurityConfig.class, CorsConfig.class, SupabaseJwtConfig.class, CurrentStaff.class })
@EnableConfigurationProperties(FlareProperties.class)
class StaffIssueEndpointsTest {

	private static final UUID ANA = UUID.fromString("5a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");
	private static final UUID ISSUE = UUID.fromString("0d9c8b7a-6f5e-4d3c-9b2a-1f0e9d8c7b6a");
	private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private StaffMemberRepository staffMembers;

	@MockitoBean
	private IssueQueryService queries;

	@MockitoBean
	private IssueActionService actions;

	@Test
	void theListMatchesTheContractWithNullsSentNotLeftOut() throws Exception {
		signedIn();
		when(queries.list(any(), eq(IssueView.ACTIVE))).thenReturn(new IssueList(NOW, List.of(card())));

		mvc.perform(get("/api/staff/issues").param("view", "ACTIVE").with(asAna()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.serverTime").value("2026-10-31T12:00:00Z"))
				.andExpect(jsonPath("$.issues[0].id").value(ISSUE.toString()))
				.andExpect(jsonPath("$.issues[0].version").value(3))
				.andExpect(jsonPath("$.issues[0].status").value("NEW"))
				.andExpect(jsonPath("$.issues[0].severity").value("MEDIUM"))
				.andExpect(jsonPath("$.issues[0].category").value("TOILET"))
				.andExpect(jsonPath("$.issues[0].summary.ro").value("Nu mai e hârtie"))
				.andExpect(jsonPath("$.issues[0].summarySource").value("LLM"))
				.andExpect(jsonPath("$.issues[0].reportCount").value(3))
				.andExpect(jsonPath("$.issues[0].reporterCount").value(3))
				.andExpect(jsonPath("$.issues[0].emergency").value(false))
				.andExpect(jsonPath("$.issues[0].unclassified").value(false))
				.andExpect(jsonPath("$.issues[0].reopened").value(false))
				// Required but nullable in the contract: present as null.
				.andExpect(jsonPath("$.issues[0].team").isEmpty())
				.andExpect(jsonPath("$.issues[0].location").isEmpty())
				.andExpect(jsonPath("$.issues[0].mentionedPlace").isEmpty())
				.andExpect(jsonPath("$.issues[0].takenBy").isEmpty())
				.andExpect(jsonPath("$.issues[0].priority.score").value(350))
				.andExpect(jsonPath("$.issues[0].priority.reasons[1].code").value("REPORTERS"))
				.andExpect(jsonPath("$.issues[0].priority.reasons[1].count").value(3))
				// Optional, not nullable: unused reason fields are left out.
				.andExpect(jsonPath("$.issues[0].priority.reasons[1].severity").doesNotExist());
	}

	@Test
	void theDetailHasTheCardFieldsAtTheTopLevelPlusReportsAndHistory() throws Exception {
		signedIn();
		ReportView report = new ReportView(UUID.randomUUID(), "Nu mai e hârtie", Language.RO, NOW, null, null, null,
				ClassificationSource.LLM);
		HistoryEntry created = new HistoryEntry(AuditAction.ISSUE_CREATED, null, NOW, Map.of());
		when(queries.detail(any(), eq(ISSUE))).thenReturn(new IssueDetail(card(), List.of(report), List.of(created)));

		mvc.perform(get("/api/staff/issues/" + ISSUE).with(asAna()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ISSUE.toString()))
				.andExpect(jsonPath("$.card").doesNotExist())
				.andExpect(jsonPath("$.reports[0].text").value("Nu mai e hârtie"))
				.andExpect(jsonPath("$.reports[0].language").value("ro"))
				.andExpect(jsonPath("$.reports[0].contact").isEmpty())
				.andExpect(jsonPath("$.reports[0].classificationSource").value("LLM"))
				.andExpect(jsonPath("$.history[0].action").value("ISSUE_CREATED"))
				.andExpect(jsonPath("$.history[0].staffDisplayName").isEmpty())
				.andExpect(jsonPath("$.history[0].details").isMap());
	}

	@Test
	void anIssueTheCallerCannotSeeIs404() throws Exception {
		signedIn();
		when(queries.detail(any(), eq(ISSUE))).thenThrow(new FlareException(ErrorCode.NOT_FOUND));

		mvc.perform(get("/api/staff/issues/" + ISSUE).with(asAna()))
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void anUnknownViewIsInvalidRequest() throws Exception {
		signedIn();

		mvc.perform(get("/api/staff/issues").param("view", "ALL").with(asAna()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void theViewIsRequired() throws Exception {
		signedIn();

		mvc.perform(get("/api/staff/issues").with(asAna()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void theListNeedsAToken() throws Exception {
		mvc.perform(get("/api/staff/issues").param("view", "ACTIVE"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void aValidTokenWithoutAStaffMemberIs403() throws Exception {
		when(staffMembers.findById(ANA)).thenReturn(Optional.empty());

		mvc.perform(get("/api/staff/issues").param("view", "ACTIVE").with(asAna()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_A_STAFF_MEMBER"));
	}

	@Test
	void takeReturnsTheNewDetail() throws Exception {
		signedIn();
		when(actions.take(any(), eq(ISSUE), eq(3L))).thenReturn(new IssueDetail(card(), List.of(), List.of()));

		postAction("take", "{\"version\":3}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(ISSUE.toString()))
				.andExpect(jsonPath("$.reports").isArray());
	}

	@Test
	void everyVersionActionHasItsEndpoint() throws Exception {
		signedIn();
		IssueDetail detail = new IssueDetail(card(), List.of(), List.of());
		when(actions.acknowledge(any(), eq(ISSUE), eq(3L))).thenReturn(detail);
		when(actions.resolve(any(), eq(ISSUE), eq(3L))).thenReturn(detail);
		when(actions.reopen(any(), eq(ISSUE), eq(3L))).thenReturn(detail);

		postAction("acknowledge", "{\"version\":3}").andExpect(status().isOk());
		postAction("resolve", "{\"version\":3}").andExpect(status().isOk());
		postAction("reopen", "{\"version\":3}").andExpect(status().isOk());
	}

	@Test
	void anActionWithoutAVersionIsInvalidRequest() throws Exception {
		signedIn();

		postAction("take", "{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void aStaleVersionIs409VersionConflict() throws Exception {
		signedIn();
		when(actions.take(any(), eq(ISSUE), eq(2L))).thenThrow(new FlareException(ErrorCode.VERSION_CONFLICT));

		postAction("take", "{\"version\":2}")
				.andExpect(status().isConflict())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
	}

	@Test
	void aSecondParallelTakeThatFailsOnTheVersionColumnIs409NotA500() throws Exception {
		signedIn();
		when(actions.take(any(), eq(ISSUE), eq(3L))).thenThrow(new ObjectOptimisticLockingFailureException(Issue.class, ISSUE));

		postAction("take", "{\"version\":3}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
	}

	@Test
	void aRefusedTransitionIs409InvalidTransition() throws Exception {
		signedIn();
		when(actions.reopen(any(), eq(ISSUE), eq(3L))).thenThrow(new FlareException(ErrorCode.INVALID_TRANSITION));

		postAction("reopen", "{\"version\":3}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
	}

	@Test
	void overrideSendsTheNewValues() throws Exception {
		signedIn();
		when(actions.override(any(), eq(ISSUE), eq(3L), eq(Category.CLEANING_OR_SPILL), eq(Severity.HIGH)))
				.thenReturn(new IssueDetail(card(), List.of(), List.of()));

		mvc.perform(patch("/api/staff/issues/" + ISSUE).with(asAna()).contentType(MediaType.APPLICATION_JSON)
						.content("{\"version\":3,\"category\":\"CLEANING_OR_SPILL\",\"severity\":\"HIGH\"}"))
				.andExpect(status().isOk());
		verify(actions).override(any(), eq(ISSUE), eq(3L), eq(Category.CLEANING_OR_SPILL), eq(Severity.HIGH));
	}

	@Test
	void handOverToATeamThatDoesNotExistIsInvalidRequest() throws Exception {
		signedIn();

		postAction("hand-over", "{\"version\":3,\"team\":\"CATERING\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void handOverPassesTheTeam() throws Exception {
		signedIn();
		when(actions.handOver(any(), eq(ISSUE), eq(3L), eq(TeamId.SECURITY))).thenReturn(new IssueDetail(card(), List.of(), List.of()));

		postAction("hand-over", "{\"version\":3,\"team\":\"SECURITY\"}").andExpect(status().isOk());
	}

	@Test
	void actionsNeedAToken() throws Exception {
		mvc.perform(post("/api/staff/issues/" + ISSUE + "/take").contentType(MediaType.APPLICATION_JSON).content("{\"version\":3}"))
				.andExpect(status().isUnauthorized());
	}

	private ResultActions postAction(String action, String json) throws Exception {
		return mvc.perform(post("/api/staff/issues/" + ISSUE + "/" + action).with(asAna())
				.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private void signedIn() {
		StaffMember ana = new StaffMember();
		ana.setId(ANA);
		ana.setDisplayName("Ana (simulat)");
		ana.setActive(true);
		ana.getRoles().addAll(Set.of(StaffRole.STAFF));
		ana.getTeams().addAll(Set.of(TeamId.CLEANING));
		when(staffMembers.findById(ANA)).thenReturn(Optional.of(ana));
	}

	private static RequestPostProcessor asAna() {
		return jwt().jwt(token -> token.subject(ANA.toString()));
	}

	private static IssueCard card() {
		Priority priority = new Priority(350, List.of(
				new PriorityReason(PriorityReasonCode.SEVERITY, Severity.MEDIUM, null, null),
				new PriorityReason(PriorityReasonCode.REPORTERS, null, 3, 4)));
		return new IssueCard(ISSUE, 3, IssueStatus.NEW, Severity.MEDIUM, Category.TOILET, null, null, null,
				new LocalizedText("Nu mai e hârtie", "No toilet paper"), ClassificationSource.LLM, 3, 3,
				NOW.minusSeconds(600), NOW.minusSeconds(360), null, false, false, false, priority);
	}
}
