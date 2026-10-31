package com.fsociety.flarebackend.issue;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import com.fsociety.flarebackend.audit.AuditEvent;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.issue.dto.HistoryEntry;
import com.fsociety.flarebackend.issue.dto.IssueCard;
import com.fsociety.flarebackend.issue.dto.LocationRef;
import com.fsociety.flarebackend.issue.dto.Priority;
import com.fsociety.flarebackend.issue.dto.ReportView;
import com.fsociety.flarebackend.issue.dto.StaffRef;
import com.fsociety.flarebackend.report.Report;

/** Maps entities to the Staff app's DTOs (api.md). Pure apart from reading entity fields. */
@Slf4j
final class IssueCards {

	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final TypeReference<Map<String, Object>> DETAILS = new TypeReference<>() {
	};

	private IssueCards() {
	}

	static IssueCard card(Issue issue, Priority priority) {
		return new IssueCard(
				issue.getId(),
				issue.getVersion(),
				issue.getStatus(),
				issue.getSeverity(),
				issue.getCategory(),
				issue.getTeam(),
				LocationRef.of(issue.getLocation()),
				issue.getMentionedPlace(),
				new LocalizedText(issue.getSummaryRo(), issue.getSummaryEn()),
				issue.getSummarySource(),
				issue.getReportCount(),
				issue.getReporterCount(),
				issue.getFirstReportedAt(),
				issue.getLastReportedAt(),
				StaffRef.of(issue.getTakenBy()),
				issue.isEmergency(),
				issue.isUnclassified(),
				issue.getReopenedCount() > 0,
				priority);
	}

	/** The text is returned as stored: the Staff app shows it as plain text only (code rules, section 2). */
	static ReportView report(Report report) {
		return new ReportView(
				report.getId(),
				report.getText(),
				report.getLanguage(),
				report.getCreatedAt(),
				report.getContact(),
				report.getFlightNumber(),
				report.getMentionedPlace(),
				// Grouped Reports are always classified; FALLBACK keeps the contract's non-null field safe anyway.
				report.getClassificationSource() != null ? report.getClassificationSource() : ClassificationSource.FALLBACK);
	}

	static HistoryEntry history(AuditEvent event) {
		return new HistoryEntry(
				event.getAction(),
				event.getStaffMember() == null ? null : event.getStaffMember().getDisplayName(),
				event.getCreatedAt(),
				details(event));
	}

	/** {@code details} is a JSON object written by our own code; if it can't be read, the line still shows. */
	private static Map<String, Object> details(AuditEvent event) {
		String json = event.getDetails();
		if (json == null || json.isBlank()) {
			return Map.of();
		}
		try {
			Map<String, Object> details = JSON.readValue(json, DETAILS);
			return details == null ? Map.of() : details;
		}
		catch (JacksonException e) {
			log.warn("Audit event {} has unreadable details", event.getId());
			return Map.of();
		}
	}
}
