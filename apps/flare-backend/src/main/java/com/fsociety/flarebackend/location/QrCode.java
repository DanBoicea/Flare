package com.fsociety.flarebackend.location;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** A printed QR code. Its opaque ID is in the URL the Passenger scans (ADR-0017). */
@Getter
@Setter
@Entity
@Table(schema = "flare", name = "qr_code")
public class QrCode {

	@Id
	@Column(name = "id")
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "location_id", nullable = false)
	private Location location;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private QrCodeStatus status;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
