package com.fsociety.flarebackend.intake;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fsociety.flarebackend.classification.BounceReason;
import com.fsociety.flarebackend.common.ErrorCode;
import com.fsociety.flarebackend.common.FlareException;
import com.fsociety.flarebackend.config.CorsConfig;
import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.config.SecurityConfig;
import com.fsociety.flarebackend.config.SupabaseJwtConfig;
import com.fsociety.flarebackend.intake.dto.MessageResponse;
import com.fsociety.flarebackend.staff.CurrentStaff;
import com.fsociety.flarebackend.staff.StaffMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Web layer only (no database): the public endpoints, Problem Details, security rules, CORS and the body size limit. */
@WebMvcTest(controllers = { PublicMessageController.class, PublicQuestionController.class }, properties = {
		"FLARE_HASH_SECRET=test-secret",
		"SUPABASE_JWKS_URI=http://localhost/jwks.json",
		"SUPABASE_JWT_ISSUER=http://localhost/auth/v1"
})
@Import({ SecurityConfig.class, CorsConfig.class, SupabaseJwtConfig.class, CurrentStaff.class })
@EnableConfigurationProperties(FlareProperties.class)
class PublicApiErrorsTest {

	private static final String DEVICE_ID = "3f0e8f8e-2c4b-4d6c-9a51-6f1f3f0c2b7a";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private StaffMemberRepository staffMembers;

	@MockitoBean
	private MessageIntakeService intake;

	@Test
	void blankTextIsTextEmpty() throws Exception {
		postMessage("{\"text\":\"   \",\"uiLanguage\":\"ro\",\"deviceId\":\"" + DEVICE_ID + "\"}")
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("TEXT_EMPTY"));
	}

	@Test
	void textOver1000CharactersIsTextTooLong() throws Exception {
		postMessage("{\"text\":\"" + "a".repeat(1001) + "\",\"uiLanguage\":\"en\",\"deviceId\":\"" + DEVICE_ID + "\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TEXT_TOO_LONG"));
	}

	@Test
	void malformedJsonIsInvalidRequest() throws Exception {
		postMessage("{not json")
				.andExpect(status().isBadRequest())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void aValidMessageReturnsTheOutcomeWithEveryFieldPresent() throws Exception {
		when(intake.handle(any(), anyString())).thenReturn(MessageResponse.reported());

		postMessage("{\"text\":\"No paper in the toilet\",\"uiLanguage\":\"en\",\"deviceId\":\"" + DEVICE_ID + "\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.outcome").value("REPORTED"))
				// The contract requires all four fields, null when they don't apply.
				.andExpect(jsonPath("$.questionId").isEmpty())
				.andExpect(jsonPath("$.answer").isEmpty())
				.andExpect(jsonPath("$.bounce").isEmpty());
	}

	@Test
	void aBouncedMessageCarriesTheReason() throws Exception {
		when(intake.handle(any(), anyString())).thenReturn(MessageResponse.bounced(BounceReason.JOKE_OR_NONSENSE, true));

		postMessage("{\"text\":\"knock knock\",\"uiLanguage\":\"en\",\"deviceId\":\"" + DEVICE_ID + "\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.outcome").value("BOUNCED"))
				.andExpect(jsonPath("$.bounce.reason").value("JOKE_OR_NONSENSE"))
				.andExpect(jsonPath("$.bounce.sendAnywayAvailable").value(true));
	}

	@Test
	void anUnknownUiLanguageIsInvalidRequest() throws Exception {
		postMessage("{\"text\":\"Hallo\",\"uiLanguage\":\"de\",\"deviceId\":\"" + DEVICE_ID + "\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void sendToStaffReturnsReported() throws Exception {
		UUID questionId = UUID.randomUUID();

		postSendToStaff(questionId.toString(), "{\"deviceId\":\"" + DEVICE_ID + "\"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.outcome").value("REPORTED"));
		verify(intake).sendToStaff(eq(questionId), eq(UUID.fromString(DEVICE_ID)), anyString());
	}

	@Test
	void sendToStaffForAnUnknownQuestionIsNotFound() throws Exception {
		UUID questionId = UUID.randomUUID();
		doThrow(new FlareException(ErrorCode.NOT_FOUND)).when(intake).sendToStaff(eq(questionId), any(), anyString());

		postSendToStaff(questionId.toString(), "{\"deviceId\":\"" + DEVICE_ID + "\"}")
				.andExpect(status().isNotFound())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void sendToStaffWithoutADeviceIdIsInvalidRequest() throws Exception {
		postSendToStaff(UUID.randomUUID().toString(), "{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void sendToStaffWithAMalformedIdIsInvalidRequest() throws Exception {
		postSendToStaff("not-a-uuid", "{\"deviceId\":\"" + DEVICE_ID + "\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void bodyOver16KbIsRejected() throws Exception {
		postMessage("{\"text\":\"" + "a".repeat(17 * 1024) + "\"}")
				.andExpect(status().isContentTooLarge())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void staffEndpointsNeedAToken() throws Exception {
		mvc.perform(get("/api/staff/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void corsAllowsTheWebAppOrigin() throws Exception {
		mvc.perform(options("/api/public/messages")
						.header("Origin", "http://localhost:5173")
						.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
	}

	@Test
	void corsRejectsOtherOrigins() throws Exception {
		mvc.perform(options("/api/public/messages")
						.header("Origin", "https://evil.example.invalid")
						.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden());
	}

	private ResultActions postMessage(String json) throws Exception {
		return mvc.perform(post("/api/public/messages").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions postSendToStaff(String questionId, String json) throws Exception {
		return mvc.perform(post("/api/public/questions/" + questionId + "/send-to-staff")
				.contentType(MediaType.APPLICATION_JSON).content(json));
	}
}
