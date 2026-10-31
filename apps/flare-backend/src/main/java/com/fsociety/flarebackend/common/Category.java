package com.fsociety.flarebackend.common;

/** Kind of problem a Report or Issue is about. Tentative; changing it needs a migration (docs/architecture/data-model.md). */
public enum Category {
	CLEANING_OR_SPILL,
	TOILET,
	LIFT_OR_ESCALATOR,
	BROKEN_OR_MAINTENANCE,
	SIGN_OR_INFORMATION,
	ASSISTANCE_NEEDED,
	LOST_ITEM_OR_PERSON,
	SAFETY_OR_SECURITY,
	OTHER
}
