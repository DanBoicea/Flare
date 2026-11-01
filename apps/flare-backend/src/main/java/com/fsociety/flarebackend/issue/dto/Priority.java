package com.fsociety.flarebackend.issue.dto;

import java.util.List;

import lombok.Value;

/** The score the Active list is sorted by, and the reasons shown to staff (contract schema {@code Priority}). */
@Value
public class Priority {
	int score;
	List<PriorityReason> reasons;
}
