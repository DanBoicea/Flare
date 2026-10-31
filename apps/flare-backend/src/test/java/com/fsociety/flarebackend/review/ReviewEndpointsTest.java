package com.fsociety.flarebackend.review;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.fsociety.flarebackend.common.ErrorCode;
import com.fsociety.flarebackend.common.FlareException;
import com.fsociety.flarebackend.config.CorsConfig;
import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.config.SecurityConfig;
import com.fsociety.flarebackend.config.SupabaseJwtConfig;
import com.fsociety.flarebackend.report.QuarantineReason;
import com.fsociety.flarebackend.review.dto.ReviewAlert;
import com.fsociety.flarebackend.review.dto.ReviewItem;
import com.fsociety.flarebackend.review.dto.ReviewList;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Web layer only (no database): Needs review JSON shape, the release body and the 403 for non-managers. */
@WebMvcTest(controllers = ReviewController.class, properties = {
		"FLARE_HASH_SECRET=test-secret",
		"SUPABASE_JWKS_URI=http://localhost/jwks.json",
		"SUPABASE_JWT_ISSUER=http://localhost/auth/v1"
})
@Import({ SecurityConfig.class, CorsConfig.class, SupabaseJwtConfig.class, CurrentStaff.class })
@EnableConfigurationProperties(FlareProperties.class)
class ReviewEndpointsTest {

	private static final UUID RADU = UUID.fromString("7c6b5a49-3827-4d16-9f05-e4d3c2b1a098");
	private static final UUID REPORT = UUID.fromString("1a2b3c4d-5e6f-4a7b-8c9d-0e1f2a3b4c5d");

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private StaffMemberRepository staffMembers;

	@MockitoBean
	private ReviewService reviews;

	@Test
	void theListMatchesTheContract() throws Exception {
		signedIn();
		ReviewItem item = new ReviewItem(REPORT, "Toaleta e murdară", Instant.parse("2026-10-31T11:55:00Z"),
				QuarantineReason.UNKNOWN_QR_CODE, "zzzzzzzzzz", null);
		ReviewAlert alert = new ReviewAlert("xw9fwv52ua", null, Instant.parse("2026-10-31T11:50:00Z"), 37);
		when(reviews.list(any())).thenReturn(new ReviewList(List.of(item), List.of(alert)));

		mvc.perform(get("/api/staff/review").with(asRadu()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].reportId").value(REPORT.toString()))
				.andExpect(jsonPath("$.items[0].text").value("Toaleta e murdară"))
				.andExpect(jsonPath("$.items[0].reason").value("UNKNOWN_QR_CODE"))
				.andExpect(jsonPath("$.items[0].scannedQrCodeId").value("zzzzzzzzzz"))
				.andExpect(jsonPath("$.items[0].location").isEmpty())
				.andExpect(jsonPath("$.alerts[0].qrCodeId").value("xw9fwv52ua"))
				.andExpect(jsonPath("$.alerts[0].kind").value("STRICT_MODE"))
				.andExpect(jsonPath("$.alerts[0].messageCount").value(37))
				.andExpect(jsonPath("$.alerts[0].location").isEmpty());
	}

	@Test
	void releaseToATeam() throws Exception {
		signedIn();

		postRelease("{\"team\":\"SECURITY\"}").andExpect(status().isOk());
		verify(reviews).release(any(), eq(REPORT), eq(TeamId.SECURITY));
	}

	@Test
	void releaseWithANullTeamRoutesNormally() throws Exception {
		signedIn();

		postRelease("{\"team\":null}").andExpect(status().isOk());
		verify(reviews).release(any(), eq(REPORT), isNull());
	}

	@Test
	void releaseToAnUnknownTeamIsInvalidRequest() throws Exception {
		signedIn();

		postRelease("{\"team\":\"CATERING\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void dismiss() throws Exception {
		signedIn();

		mvc.perform(post("/api/staff/review/" + REPORT + "/dismiss").with(asRadu())).andExpect(status().isOk());
		verify(reviews).dismiss(any(), eq(REPORT));
	}

	@Test
	void aNonManagerGets403() throws Exception {
		signedIn();
		when(reviews.list(any())).thenThrow(new FlareException(ErrorCode.FORBIDDEN));

		mvc.perform(get("/api/staff/review").with(asRadu()))
				.andExpect(status().isForbidden())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void aReportNotInReviewIs404() throws Exception {
		signedIn();
		doThrow(new FlareException(ErrorCode.NOT_FOUND)).when(reviews).dismiss(any(), eq(REPORT));

		mvc.perform(post("/api/staff/review/" + REPORT + "/dismiss").with(asRadu()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void reviewNeedsAToken() throws Exception {
		mvc.perform(get("/api/staff/review")).andExpect(status().isUnauthorized());
	}

	private ResultActions postRelease(String json) throws Exception {
		return mvc.perform(post("/api/staff/review/" + REPORT + "/release").with(asRadu())
				.contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private void signedIn() {
		StaffMember radu = new StaffMember();
		radu.setId(RADU);
		radu.setDisplayName("Radu (simulat)");
		radu.setActive(true);
		radu.getRoles().addAll(Set.of(StaffRole.DUTY_MANAGER));
		when(staffMembers.findById(RADU)).thenReturn(Optional.of(radu));
	}

	private static RequestPostProcessor asRadu() {
		return jwt().jwt(token -> token.subject(RADU.toString()));
	}
}
