package com.fsociety.flarebackend.grouping;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Used while the LLM is off or failing: never matches, so every Report gets its own Issue (backend.md "Fallbacks"). */
public class NoMatchIssueMatcher implements IssueMatcher {

	@Override
	public Optional<UUID> findMatch(NewReportView report, List<CandidateIssueView> candidates) {
		return Optional.empty();
	}
}
