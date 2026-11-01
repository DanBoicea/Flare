package com.fsociety.flarebackend.classification;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.staff.TeamId;

/**
 * Which Team gets a new Issue (pipeline.md section 7, config {@code flare.routing}). OTHER, Unclassified
 * (fallback) and confidence below the threshold get no Team, so only the duty manager sees them. Pure.
 */
public final class CategoryTeamRouting {

	private final Map<Category, TeamId> teamByCategory = new EnumMap<>(Category.class);
	private final double lowConfidenceThreshold;

	/** Throws on an unknown category or Team name, so a typo in application.yml stops the app from starting. */
	public CategoryTeamRouting(Map<String, String> categoryTeam, double lowConfidenceThreshold) {
		categoryTeam.forEach((category, team) -> teamByCategory.put(Category.valueOf(category), TeamId.valueOf(team)));
		this.lowConfidenceThreshold = lowConfidenceThreshold;
	}

	public Optional<TeamId> teamFor(Category category, ClassificationSource source, double confidence) {
		if (category == null || source != ClassificationSource.LLM || confidence < lowConfidenceThreshold) {
			return Optional.empty();
		}
		return Optional.ofNullable(teamByCategory.get(category));
	}
}
