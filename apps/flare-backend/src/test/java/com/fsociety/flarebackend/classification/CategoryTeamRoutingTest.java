package com.fsociety.flarebackend.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.staff.TeamId;
import org.junit.jupiter.api.Test;

/** Category to Team (pipeline.md section 7): OTHER, Unclassified and low confidence go to no Team. */
class CategoryTeamRoutingTest {

	private final CategoryTeamRouting routing = new CategoryTeamRouting(Map.of(
			"TOILET", "CLEANING",
			"LIFT_OR_ESCALATOR", "MAINTENANCE",
			"SAFETY_OR_SECURITY", "SECURITY"), 0.5);

	@Test
	void routesAConfidentLlmClassificationByCategory() {
		assertThat(routing.teamFor(Category.TOILET, ClassificationSource.LLM, 0.9)).contains(TeamId.CLEANING);
		assertThat(routing.teamFor(Category.LIFT_OR_ESCALATOR, ClassificationSource.LLM, 0.5)).contains(TeamId.MAINTENANCE);
	}

	@Test
	void unclassifiedReportsGoToNoTeam() {
		assertThat(routing.teamFor(Category.TOILET, ClassificationSource.FALLBACK, 1.0)).isEmpty();
		assertThat(routing.teamFor(Category.OTHER, ClassificationSource.FALLBACK, 0.0)).isEmpty();
	}

	@Test
	void otherAndUnmappedCategoriesGoToNoTeam() {
		assertThat(routing.teamFor(Category.OTHER, ClassificationSource.LLM, 0.9)).isEmpty();
		assertThat(routing.teamFor(Category.SIGN_OR_INFORMATION, ClassificationSource.LLM, 0.9)).isEmpty();
		assertThat(routing.teamFor(null, ClassificationSource.LLM, 0.9)).isEmpty();
	}

	@Test
	void lowConfidenceGoesToNoTeam() {
		assertThat(routing.teamFor(Category.TOILET, ClassificationSource.LLM, 0.49)).isEmpty();
	}

	@Test
	void aTypoInTheConfigStopsTheAppFromStarting() {
		assertThatThrownBy(() -> new CategoryTeamRouting(Map.of("TOILET", "CLEANERS"), 0.5))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new CategoryTeamRouting(Map.of("TOILETS", "CLEANING"), 0.5))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
