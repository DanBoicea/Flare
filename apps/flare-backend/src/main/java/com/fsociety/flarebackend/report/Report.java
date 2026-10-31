package com.fsociety.flarebackend.report;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.Language;
import com.fsociety.flarebackend.common.Severity;
import com.fsociety.flarebackend.issue.Issue;
import com.fsociety.flarebackend.location.Location;
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
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * One Passenger's message about a problem. Holds Passenger text and contact details, so it has no
 * generated toString: nothing here may reach the logs.
 */
@Getter
@Setter
@Entity
@Table(schema = "flare", name = "report")
public class Report {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private UUID id;

	/** Null until grouped, or while Quarantined. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "issue_id")
	private Issue issue;

	/** The raw scanned value; no foreign key, since it may be unknown. */
	@Column(name = "scanned_qr_code_id")
	private String scannedQrCodeId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id")
	private Location location;

	@Column(name = "text", nullable = false)
	private String text;

	@Column(name = "language", nullable = false)
	private Language language;

	/** Optional; erased by retention. */
	@Column(name = "contact")
	private String contact;

	@Column(name = "device_hash", nullable = false)
	private String deviceHash;

	@Column(name = "network_hash", nullable = false)
	private String networkHash;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private ReportStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "quarantine_reason")
	private QuarantineReason quarantineReason;

	/** Set when the duty manager releases a Report to a chosen Team; the worker then skips matching. */
	@Enumerated(EnumType.STRING)
	@Column(name = "team_override")
	private TeamId teamOverride;

	/** Null means not classified yet (the Emergency path). */
	@Enumerated(EnumType.STRING)
	@Column(name = "classification_source")
	private ClassificationSource classificationSource;

	@Enumerated(EnumType.STRING)
	@Column(name = "category")
	private Category category;

	@Enumerated(EnumType.STRING)
	@Column(name = "severity")
	private Severity severity;

	@Column(name = "emergency", nullable = false)
	private boolean emergency;

	@Column(name = "confidence", precision = 3, scale = 2)
	private BigDecimal confidence;

	@Column(name = "mentioned_place")
	private String mentionedPlace;

	@Column(name = "flight_number")
	private String flightNumber;

	@Column(name = "staff_summary_ro")
	private String staffSummaryRo;

	@Column(name = "staff_summary_en")
	private String staffSummaryEn;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "grouped_at")
	private Instant groupedAt;
}
