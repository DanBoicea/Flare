package com.fsociety.flarebackend.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fsociety.flarebackend.issue.Issue;
import com.fsociety.flarebackend.staff.StaffMember;

/** The audit log keeps who did what, with {from, to} details as JSON. */
class AuditServiceTest {

	private final AuditEventRepository repository = mock(AuditEventRepository.class);
	private final AuditService service = new AuditService(repository);

	@Test
	void recordsTheStaffMemberAndTheDetailsAsJson() {
		Issue issue = new Issue();
		StaffMember ana = new StaffMember();
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("from", null);
		details.put("to", "CLEANING");

		service.record(issue, null, ana, AuditAction.HANDED_OVER, details);

		AuditEvent event = saved();
		assertThat(event.getIssue()).isSameAs(issue);
		assertThat(event.getStaffMember()).isSameAs(ana);
		assertThat(event.getAction()).isEqualTo(AuditAction.HANDED_OVER);
		assertThat(event.getDetails()).isEqualTo("{\"from\":null,\"to\":\"CLEANING\"}");
	}

	@Test
	void theSystemHasNoStaffMemberAndEmptyDetails() {
		service.record(new Issue(), null, null, AuditAction.ISSUE_CREATED, Map.of());

		AuditEvent event = saved();
		assertThat(event.getStaffMember()).isNull();
		assertThat(event.getDetails()).isEqualTo("{}");
	}

	private AuditEvent saved() {
		ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
		verify(repository).save(captor.capture());
		return captor.getValue();
	}
}
