package com.fsociety.flarebackend.location;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A part of the airport, such as arrivals. Grouping looks for matching Issues within one Area. */
@Getter
@Setter
@Entity
@Table(schema = "flare", name = "area")
public class Area {

	@Id
	@Column(name = "id")
	private String id;

	@Column(name = "name_ro", nullable = false)
	private String nameRo;

	@Column(name = "name_en", nullable = false)
	private String nameEn;
}
