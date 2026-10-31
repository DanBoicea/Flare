package com.fsociety.flarebackend.issue.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fsociety.flarebackend.common.Severity;
import lombok.Value;

/**
 * One reason for an Issue's priority (contract schema {@code PriorityReason}). Unused fields are left out of
 * the JSON rather than sent as null, because the contract declares them optional but not nullable.
 */
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PriorityReason {
	PriorityReasonCode code;
	/** SEVERITY only. */
	Severity severity;
	/** REPORTERS only. */
	Integer count;
	/** REPORTERS (first to last report) and WAITING. */
	Integer minutes;

	public static PriorityReason of(PriorityReasonCode code) {
		return new PriorityReason(code, null, null, null);
	}
}
