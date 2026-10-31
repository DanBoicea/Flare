package com.fsociety.flarebackend.staff;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffMemberRepository extends JpaRepository<StaffMember, UUID> {
}
