package com.fsociety.flarebackend.issue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Issue queries. Visibility (Teams, duty manager, Critical for Security) is decided in Java by
 * StaffAccessPolicy, not here, so the rules live in one tested place (A03, A04).
 */
public interface IssueRepository extends JpaRepository<Issue, UUID> {

	/** Every Issue that isn't resolved, with what the Issue cards show. The ACTIVE list (A04). */
	@Query("""
			select i from Issue i
			left join fetch i.location l
			left join fetch l.area
			left join fetch i.takenBy
			where i.status <> com.fsociety.flarebackend.issue.IssueStatus.RESOLVED
			""")
	List<Issue> findActive();

	/** Issues resolved after {@code since}, newest first. The RESOLVED list (A04). */
	@EntityGraph(attributePaths = { "location", "location.area", "takenBy" })
	List<Issue> findByStatusAndResolvedAtAfterOrderByResolvedAtDesc(IssueStatus status, Instant since, Limit limit);

	/**
	 * Grouping candidates (A08): Issues in the same Area that are open, or were resolved after
	 * {@code resolvedSince}, most recently updated first.
	 */
	@Query("""
			select i from Issue i
			join fetch i.location l
			where l.area.id = :areaId
			  and (i.status <> com.fsociety.flarebackend.issue.IssueStatus.RESOLVED or i.resolvedAt >= :resolvedSince)
			order by i.updatedAt desc
			""")
	List<Issue> findCandidates(@Param("areaId") String areaId, @Param("resolvedSince") Instant resolvedSince, Limit limit);
}
