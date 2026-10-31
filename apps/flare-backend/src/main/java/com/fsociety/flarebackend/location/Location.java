package com.fsociety.flarebackend.location;

import java.util.UUID;

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

/** A specific place with a QR code, such as "Arrivals toilets near belt 2". Only staff see it. */
@Getter
@Setter
@Entity
@Table(schema = "flare", name = "location")
public class Location {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "area_id", nullable = false)
	private Area area;

	/** Printed under the QR code, e.g. ARR-WC-02. */
	@Column(name = "label", nullable = false, unique = true)
	private String label;

	@Column(name = "name_ro", nullable = false)
	private String nameRo;

	@Column(name = "name_en", nullable = false)
	private String nameEn;

	/** 0 normal to 2 most critical; raises Issue priority. */
	@Column(name = "criticality", nullable = false)
	private short criticality;
}
