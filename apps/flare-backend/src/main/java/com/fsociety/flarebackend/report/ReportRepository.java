package com.fsociety.flarebackend.report;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
