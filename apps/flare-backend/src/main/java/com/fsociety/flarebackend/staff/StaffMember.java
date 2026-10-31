package com.fsociety.flarebackend.staff;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A person with a Staff app account. The ID is their Supabase Auth user ID (the JWT "sub").
 * Teams and roles are many-to-many link tables, mapped as sets of IDs: the access policy only needs the IDs.
 */
@Getter
@Setter
@Entity
@Table(schema = "flare", name = "staff_member")
public class StaffMember {

	@Id
	@Column(name = "id")
	private UUID id;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	/** Inactive members get 403 on every staff endpoint. */
	@Column(name = "active", nullable = false)
	private boolean active;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(schema = "flare", name = "staff_member_team", joinColumns = @JoinColumn(name = "staff_member_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "team_id", nullable = false)
	private Set<TeamId> teams = new HashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(schema = "flare", name = "staff_member_role", joinColumns = @JoinColumn(name = "staff_member_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "role_id", nullable = false)
	private Set<StaffRole> roles = new HashSet<>();

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public boolean hasRole(StaffRole role) {
		return roles.contains(role);
	}
}
