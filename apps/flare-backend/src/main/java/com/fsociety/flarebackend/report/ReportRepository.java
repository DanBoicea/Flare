package com.fsociety.flarebackend.report;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, UUID> {

	/** Accepted Reports not yet in an Issue, oldest first. The worker re-queues them on startup (A08). */
	@Query("""
			select r from Report r
			where r.status = com.fsociety.flarebackend.report.ReportStatus.ACCEPTED and r.issue is null
			order by r.createdAt
			""")
	List<Report> findUngrouped();

	/** The duty manager's Needs review list, newest first (A09). */
	@Query("""
			select r from Report r
			left join fetch r.location
			where r.status = com.fsociety.flarebackend.report.ReportStatus.QUARANTINED
			order by r.createdAt desc
			""")
	List<Report> findQuarantined();

	/** Reports in an Issue with the given status (the Issue's {@code report_count} counts ACCEPTED ones). */
	long countByIssueIdAndStatus(UUID issueId, ReportStatus status);

	/** Independent Passengers behind an Issue: distinct device hashes among its accepted Reports. */
	@Query("""
			select count(distinct r.deviceHash) from Report r
			where r.issue.id = :issueId and r.status = com.fsociety.flarebackend.report.ReportStatus.ACCEPTED
			""")
	long countReporters(@Param("issueId") UUID issueId);

	/** An Issue's Reports for the detail view, newest first (A04). */
	List<Report> findByIssueIdAndStatusOrderByCreatedAtDesc(UUID issueId, ReportStatus status, Limit limit);
}
