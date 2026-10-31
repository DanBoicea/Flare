package com.fsociety.flarebackend.staff;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A group of staff members responsible for one kind of Issue. Rows come from migration V2. */
@Getter
@Setter
@Entity
@Table(schema = "flare", name = "team")
public class Team {

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "id")
	private TeamId id;

	@Column(name = "name_ro", nullable = false)
	private String nameRo;

	@Column(name = "name_en", nullable = false)
	private String nameEn;
}
