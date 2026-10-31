package com.fsociety.flarebackend.grouping;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Decides whether a new Report describes one of the candidate Issues (pipeline.md 4.2, ADR-0007). The fallback is
 * {@link NoMatchIssueMatcher}; Track C adds the LLM implementation, which must be marked {@code @Primary}. The
 * caller catches every exception and rejects an ID that is not a candidate, so an implementation may just throw.
 */
public interface IssueMatcher {

	Optional<UUID> findMatch(NewReportView report, List<CandidateIssueView> candidates);
}
