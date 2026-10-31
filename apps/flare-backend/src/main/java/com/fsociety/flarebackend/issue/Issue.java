package com.fsociety.flarebackend.issue;

import java.time.Instant;
import java.util.UUID;

import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.location.Location;
import com.fsociety.flarebackend.staff.StaffMember;
import com.fsociety.flarebackend.staff.TeamId;
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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** A real-world problem that one or more Reports describe. What staff see and act on. */
@Getter
@Setter
@Entity
@Table(schema = "flare", name = "issue")
public class Issue {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private UUID id;

	/** Location of the first Report; null when unknown. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id")
	private Location location;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false)
	private Category category;

	@Enumerated(EnumType.STRING)
	@Column(name = "severity", nullable = false)
	private Severity severity;

	@Column(name = "category_overridden", nullable = false)
	private boolean categoryOverridden;

	@Column(name = "severity_overridden", nullable = false)
	private boolean severityOverridden;

	/** Null means unrouted: only the duty manager sees it. */
	@Enumerated(EnumType.STRING)
	@Column(name = "team_id")
	private TeamId team;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private IssueStatus status;

	/** "I'm on it". */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "taken_by")
	private StaffMember takenBy;

	@Column(name = "summary_ro", nullable = false)
	private String summaryRo;

	@Column(name = "summary_en", nullable = false)
	private String summaryEn;

	@Enumerated(EnumType.STRING)
	@Column(name = "summary_source", nullable = false)
	private ClassificationSource summarySource;

	@Column(name = "mentioned_place")
	private String mentionedPlace;

	@Column(name = "emergency", nullable = false)
	private boolean emergency;

	/** True while every Report is a fallback. */
	@Column(name = "unclassified", nullable = false)
	private boolean unclassified;

	/** Accepted Reports. */
	@Column(name = "report_count", nullable = false)
	private int reportCount;

	/** Distinct device hashes among accepted Reports: independent Passengers. */
	@Column(name = "reporter_count", nullable = false)
	private int reporterCount;

	@Column(name = "first_reported_at", nullable = false)
	private Instant firstReportedAt;

	@Column(name = "last_reported_at", nullable = false)
	private Instant lastReportedAt;

	@Column(name = "acknowledged_at")
	private Instant acknowledgedAt;

	@Column(name = "resolved_at")
	private Instant resolvedAt;

	@Column(name = "reopened_count", nullable = false)
	private int reopenedCount;

	/** Optimistic locking: staff actions send the version they saw. */
	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
