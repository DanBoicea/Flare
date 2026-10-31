package com.fsociety.flarebackend.issue.dto;

import java.util.UUID;

import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.location.Location;
import lombok.Value;

/** A Location as staff see it (contract schema {@code LocationRef}). */
@Value
public class LocationRef {
	UUID id;
	String label;
	LocalizedText name;

	public static LocationRef of(Location location) {
		return location == null ? null
				: new LocationRef(location.getId(), location.getLabel(), new LocalizedText(location.getNameRo(), location.getNameEn()));
	}
}
