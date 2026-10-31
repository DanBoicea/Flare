package com.fsociety.flarebackend.staff;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, TeamId> {
}
