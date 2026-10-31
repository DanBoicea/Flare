package com.fsociety.flarebackend.issue;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fsociety.flarebackend.issue.dto.IssueDetail;
import com.fsociety.flarebackend.issue.dto.IssueList;
import com.fsociety.flarebackend.staff.CurrentStaff;

/** The Staff app's Issue list (polled every 5 s, ADR-0011) and Issue detail. Sign-in is required by SecurityConfig. */
@RestController
@RequestMapping("/api/staff/issues")
@RequiredArgsConstructor
public class StaffIssueController {

	private final CurrentStaff currentStaff;
	private final IssueQueryService queries;

	@GetMapping
	public IssueList list(@RequestParam IssueView view) {
		return queries.list(currentStaff.require(), view);
	}

	@GetMapping("/{issueId}")
	public IssueDetail detail(@PathVariable UUID issueId) {
		return queries.detail(currentStaff.require(), issueId);
	}
}
