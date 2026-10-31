package com.fsociety.flarebackend.common;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Language of a Passenger message. Stored and sent as lowercase codes, so it uses a converter instead of @Enumerated. */
@Getter
@RequiredArgsConstructor
public enum Language {
	RO("ro"),
	EN("en"),
	OTHER("other");

	@JsonValue
	private final String code;

	public static Language fromCode(String code) {
		for (Language language : values()) {
			if (language.code.equals(code)) {
				return language;
			}
		}
		throw new IllegalArgumentException("Unknown language code: " + code);
	}

	@Converter(autoApply = true)
	public static class JpaConverter implements AttributeConverter<Language, String> {

		@Override
		public String convertToDatabaseColumn(Language language) {
			return language == null ? null : language.code;
		}

		@Override
		public Language convertToEntityAttribute(String code) {
			return code == null ? null : fromCode(code);
		}
	}
}
