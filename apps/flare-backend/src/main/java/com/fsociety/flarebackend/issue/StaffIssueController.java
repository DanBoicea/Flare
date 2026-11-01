package com.fsociety.flarebackend.issue;

import java.util.UUID;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fsociety.flarebackend.issue.dto.HandOverRequest;
import com.fsociety.flarebackend.issue.dto.IssueDetail;
import com.fsociety.flarebackend.issue.dto.IssueList;
import com.fsociety.flarebackend.issue.dto.OverrideRequest;
import com.fsociety.flarebackend.issue.dto.VersionRequest;
import com.fsociety.flarebackend.staff.CurrentStaff;

/**
 * The Staff app's Issue list (polled every 5 s, ADR-0011), Issue detail and the staff actions (api.md). Sign-in is
 * required by SecurityConfig; who may see or act is decided in the services.
 */
@RestController
@RequestMapping("/api/staff/issues")
@RequiredArgsConstructor
public class StaffIssueController {

	private final CurrentStaff currentStaff;
	private final IssueQueryService queries;
	private final IssueActionService actions;

	@GetMapping
	public IssueList list(@RequestParam IssueView view) {
		return queries.list(currentStaff.require(), view);
	}

	@GetMapping("/{issueId}")
	public IssueDetail detail(@PathVariable UUID issueId) {
		return queries.detail(currentStaff.require(), issueId);
	}

	@PostMapping("/{issueId}/acknowledge")
	public IssueDetail acknowledge(@PathVariable UUID issueId, @Valid @RequestBody VersionRequest request) {
		return actions.acknowledge(currentStaff.require(), issueId, request.getVersion());
	}

	@PostMapping("/{issueId}/take")
	public IssueDetail take(@PathVariable UUID issueId, @Valid @RequestBody VersionRequest request) {
		return actions.take(currentStaff.require(), issueId, request.getVersion());
	}

	@PostMapping("/{issueId}/resolve")
	public IssueDetail resolve(@PathVariable UUID issueId, @Valid @RequestBody VersionRequest request) {
		return actions.resolve(currentStaff.require(), issueId, request.getVersion());
	}

	@PostMapping("/{issueId}/reopen")
	public IssueDetail reopen(@PathVariable UUID issueId, @Valid @RequestBody VersionRequest request) {
		return actions.reopen(currentStaff.require(), issueId, request.getVersion());
	}

	@PatchMapping("/{issueId}")
	public IssueDetail override(@PathVariable UUID issueId, @Valid @RequestBody OverrideRequest request) {
		return actions.override(currentStaff.require(), issueId, request.getVersion(), request.getCategory(), request.getSeverity());
	}

	@PostMapping("/{issueId}/hand-over")
	public IssueDetail handOver(@PathVariable UUID issueId, @Valid @RequestBody HandOverRequest request) {
		return actions.handOver(currentStaff.require(), issueId, request.getVersion(), request.getTeam());
	}
}
