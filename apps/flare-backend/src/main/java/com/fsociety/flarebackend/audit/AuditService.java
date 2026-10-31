package com.fsociety.flarebackend.audit;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import com.fsociety.flarebackend.issue.Issue;
import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.staff.StaffMember;

/**
 * Writes the audit log: one {@code audit_event} per change, shown as the Issue's history. {@code details} holds
 * only our own values (enum names, IDs), never Passenger text (data-model.md).
 */
@Service
@RequiredArgsConstructor
public class AuditService {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final AuditEventRepository auditEvents;

	/** {@code by} is null when the system did it; {@code report} is null for staff actions. Values may be null. */
	public void record(Issue issue, Report report, StaffMember by, AuditAction action, Map<String, ?> details) {
		AuditEvent event = new AuditEvent();
		event.setIssue(issue);
		event.setReport(report);
		event.setStaffMember(by);
		event.setAction(action);
		event.setDetails(JSON.writeValueAsString(details));
		auditEvents.save(event);
	}
}
