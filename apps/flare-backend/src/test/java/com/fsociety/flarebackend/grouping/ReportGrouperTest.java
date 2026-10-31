package com.fsociety.flarebackend.grouping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.fsociety.flarebackend.classification.ClassificationInput;
import com.fsociety.flarebackend.classification.FallbackClassifier;
import com.fsociety.flarebackend.classification.MessageClassifier;
import com.fsociety.flarebackend.common.Language;
import com.fsociety.flarebackend.issue.Issue;
import com.fsociety.flarebackend.issue.IssueRepository;
import com.fsociety.flarebackend.issue.IssueService;
import com.fsociety.flarebackend.issue.IssueStatus;
import com.fsociety.flarebackend.location.Location;
import com.fsociety.flarebackend.question.QuestionRepository;
import com.fsociety.flarebackend.report.QuarantineReason;
import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.report.ReportRepository;
import com.fsociety.flarebackend.report.ReportStatus;
import com.fsociety.flarebackend.safety.EmergencyTerms;
import com.fsociety.flarebackend.staff.TeamId;

/** The grouping worker's branches added with A09: a released Report with a chosen Team, and "the Passenger insisted". */
class ReportGrouperTest {

	private final ReportRepository reports = mock(ReportRepository.class);
	private final IssueRepository issues = mock(IssueRepository.class);
	private final IssueService issueService = mock(IssueService.class);
	private final MessageClassifier classifier = mock(MessageClassifier.class);
	private final FallbackClassifier fallback = new FallbackClassifier(EmergencyTerms.loadDetector());
	private final QuestionRepository questions = mock(QuestionRepository.class);
	private final ReportGrouper grouper = new ReportGrouper(reports, issues, issueService, classifier, fallback, questions,
			Clock.fixed(Instant.parse("2026-10-31T12:00:00Z"), ZoneOffset.UTC));

	private Report report;

	@BeforeEach
	void setUp() {
		report = new Report();
		report.setId(UUID.randomUUID());
		report.setText("Toaleta e murdară");
		report.setLanguage(Language.RO);
		report.setStatus(ReportStatus.ACCEPTED);
		when(reports.findById(report.getId())).thenReturn(Optional.of(report));
		when(issueService.createFromReport(any())).thenReturn(new Issue());
		when(classifier.classify(any())).thenAnswer(call -> fallback.classify(call.getArgument(0)));
	}

	@Test
	void aReleasedReportWithAChosenTeamGetsItsOwnIssueEvenIfItIsAnEmergency() {
		Location location = new Location();
		location.setId(UUID.randomUUID());
		report.setLocation(location);
		report.setEmergency(true);
		report.setTeamOverride(TeamId.SECURITY);

		grouper.group(report.getId());

		verify(issueService).createFromReport(report);
		verify(issueService, never()).attachReport(any(), any());
		verify(issues, never()).findFirstByLocationIdAndEmergencyTrueAndStatusNotOrderByCreatedAtDesc(any(), any());
		assertThat(report.getGroupedAt()).isNotNull();
	}

	@Test
	void anEmergencyWithoutAChosenTeamStillJoinsTheOpenEmergencyIssue() {
		Location location = new Location();
		location.setId(UUID.randomUUID());
		report.setLocation(location);
		report.setEmergency(true);
		Issue open = new Issue();
		when(issues.findFirstByLocationIdAndEmergencyTrueAndStatusNotOrderByCreatedAtDesc(location.getId(), IssueStatus.RESOLVED))
				.thenReturn(Optional.of(open));

		grouper.group(report.getId());

		verify(issueService).attachReport(open, report);
		verify(issueService, never()).createFromReport(any());
	}

	@Test
	void aReleasedSendAnywayReportTellsTheClassifierThePassengerInsisted() {
		report.setQuarantineReason(QuarantineReason.SEND_ANYWAY);

		grouper.group(report.getId());

		assertThat(classifiedInput().isPassengerInsisted()).isTrue();
	}

	@Test
	void aQuestionSentToStaffAlsoInsists() {
		when(questions.existsBySentToStaffReportId(report.getId())).thenReturn(true);

		grouper.group(report.getId());

		assertThat(classifiedInput().isPassengerInsisted()).isTrue();
	}

	@Test
	void anOrdinaryUnclassifiedReportDoesNotInsist() {
		grouper.group(report.getId());

		assertThat(classifiedInput().isPassengerInsisted()).isFalse();
	}

	private ClassificationInput classifiedInput() {
		ArgumentCaptor<ClassificationInput> input = ArgumentCaptor.forClass(ClassificationInput.class);
		verify(classifier).classify(input.capture());
		return input.getValue();
	}
}
