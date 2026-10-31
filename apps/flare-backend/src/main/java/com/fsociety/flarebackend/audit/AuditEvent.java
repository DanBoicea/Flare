package com.fsociety.flarebackend.audit;

import java.time.Instant;
import java.util.UUID;

import com.fsociety.flarebackend.issue.Issue;
import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.staff.StaffMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One entry in the history of an Issue or Report. */
@Getter
@Setter
@Entity
@Table(schema = "flare", name = "audit_event")
public class AuditEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "issue_id")
	private Issue issue;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "report_id")
	private Report report;

	/** Null means the system did it. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "staff_member_id")
	private StaffMember staffMember;

	@Enumerated(EnumType.STRING)
	@Column(name = "action", nullable = false)
	private AuditAction action;

	/** A JSON object such as {"from":"MEDIUM","to":"HIGH"}. Never Passenger text. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "details", nullable = false)
	private String details = "{}";

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
