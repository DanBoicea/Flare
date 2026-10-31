package com.fsociety.flarebackend.issue.summary;

import lombok.Value;

import com.fsociety.flarebackend.common.Language;

/** One Report's text for the summariser: untrusted data, never instructions. toString is replaced. */
@Value
public class ReportTextView {

	String text;
	Language language;

	@Override
	public String toString() {
		return "ReportTextView[language=" + language + "]";
	}
}
