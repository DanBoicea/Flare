package com.fsociety.flarebackend.audit;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

	/** An Issue's history for the detail view, oldest first (A04). */
	@EntityGraph(attributePaths = "staffMember")
	List<AuditEvent> findByIssueIdOrderByCreatedAt(UUID issueId);
}
