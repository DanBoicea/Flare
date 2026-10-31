package com.fsociety.flarebackend.classification;

import lombok.Value;

/** The scanned Location as the classifier sees it: names only, no IDs (backend.md, "View classes"). */
@Value
public class LocationInfo {
	String nameRo;
	String nameEn;
}
