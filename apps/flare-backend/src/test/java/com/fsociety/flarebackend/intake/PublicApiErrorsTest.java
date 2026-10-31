package com.fsociety.flarebackend.intake;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fsociety.flarebackend.config.CorsConfig;
import com.fsociety.flarebackend.config.FlareProperties;
import com.fsociety.flarebackend.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Web layer only (no database): Problem Details, security rules, CORS and the body size limit. */
@WebMvcTest(controllers = PublicMessageController.class, properties = {
		"FLARE_HASH_SECRET=test-secret",
		"SUPABASE_JWKS_URI=http://localhost/jwks.json",
		"SUPABASE_JWT_ISSUER=http://localhost/auth/v1"
})
@Import({ SecurityConfig.class, CorsConfig.class })
@EnableConfigurationProperties(FlareProperties.class)
class PublicApiErrorsTest {

	private static final String DEVICE_ID = "3f0e8f8e-2c4b-4d6c-9a51-6f1f3f0c2b7a";

	@Autowired
	private MockMvc mvc;

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
	void validMessageReachesTheStub() throws Exception {
		postMessage("{\"text\":\"No paper in the toilet\",\"uiLanguage\":\"en\",\"deviceId\":\"" + DEVICE_ID + "\"}")
				.andExpect(status().isNotImplemented())
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));
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
}
