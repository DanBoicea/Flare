package com.fsociety.flarebackend.question;

import java.time.Instant;
import java.util.UUID;

import com.fsociety.flarebackend.common.Language;
import com.fsociety.flarebackend.location.Location;
import com.fsociety.flarebackend.report.Report;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** A Passenger message answered from the Knowledge base. Holds Passenger text, so no generated toString. */
@Getter
@Setter
@Entity
@Table(schema = "flare", name = "question")
public class Question {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private UUID id;

	@Column(name = "scanned_qr_code_id")
	private String scannedQrCodeId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "location_id")
	private Location location;

	@Column(name = "text", nullable = false)
	private String text;

	@Column(name = "language", nullable = false)
	private Language language;

	@Column(name = "answer_text")
	private String answerText;

	/** False means the Knowledge base didn't cover it. */
	@Column(name = "answer_found", nullable = false)
	private boolean answerFound;

	@Column(name = "device_hash", nullable = false)
	private String deviceHash;

	/** Set by "Send to staff anyway". */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sent_to_staff_report_id")
	private Report sentToStaffReport;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
