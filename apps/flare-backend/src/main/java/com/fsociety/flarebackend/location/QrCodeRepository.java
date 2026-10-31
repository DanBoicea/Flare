package com.fsociety.flarebackend.location;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrCodeRepository extends JpaRepository<QrCode, String> {

	/** Loads the Location and its Area too, since intake needs both for every message. */
	@Override
	@EntityGraph(attributePaths = { "location", "location.area" })
	Optional<QrCode> findById(String id);
}
