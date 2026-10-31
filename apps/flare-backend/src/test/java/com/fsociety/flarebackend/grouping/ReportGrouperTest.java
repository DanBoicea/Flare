package com.fsociety.flarebackend.grouping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Limit;

import com.fsociety.flarebackend.classification.ClassificationInput;
import com.fsociety.flarebackend.classification.FallbackClassifier;
import com.fsociety.flarebackend.classification.MessageClassifier;
import com.fsociety.flarebackend.common.Category;
import com.fsociety.flarebackend.common.ClassificationSource;
import com.fsociety.flarebackend.common.Language;
import com.fsociety.flarebackend.common.LocalizedText;
import com.fsociety.flarebackend.issue.Issue;
import com.fsociety.flarebackend.issue.IssueRepository;
import com.fsociety.flarebackend.issue.IssueService;
import com.fsociety.flarebackend.issue.IssueStatus;
import com.fsociety.flarebackend.issue.summary.FallbackIssueSummarizer;
import com.fsociety.flarebackend.issue.summary.IssueSummarizer;
import com.fsociety.flarebackend.location.Area;
import com.fsociety.flarebackend.location.Location;
import com.fsociety.flarebackend.question.QuestionRepository;
import com.fsociety.flarebackend.report.QuarantineReason;
import com.fsociety.flarebackend.report.Report;
import com.fsociety.flarebackend.report.ReportRepository;
import com.fsociety.flarebackend.report.ReportStatus;
import com.fsociety.flarebackend.safety.EmergencyTerms;
import com.fsociety.flarebackend.staff.TeamId;

/** The grouping steps of pipeline.md section 5 (A08), plus the A09 branches. */
class ReportGrouperTest {

	private static final Instant NOW = Instant.parse("2026-10-31T12:00:00Z");

	private final ReportRepository reports = mock(ReportRepository.class);
	private final IssueRepository issues = mock(IssueRepository.class);
	private final IssueService issueService = mock(IssueService.class);
	private final MessageClassifier classifier = mock(MessageClassifier.class);
	private final FallbackClassifier fallback = new FallbackClassifier(EmergencyTerms.loadDetector());
	private final QuestionRepository questions = mock(QuestionRepository.class);
	private final IssueMatcher matcher = mock(IssueMatcher.class);
	private final IssueSummarizer summarizer = mock(IssueSummarizer.class);
	private final CandidateSelector selector = new CandidateSelector(Duration.ofMinutes(30), 10);

	private final ReportGrouper grouper = grouper(matcher, summarizer);

	private final Location toilet = location("ARR");
	private Report report;
	private Issue candidate;

	@BeforeEach
	void setUp() {
		report = report(toilet);
		candidate = issue(IssueStatus.NEW);
		when(issueService.createFromReport(any())).thenAnswer(call -> issue(IssueStatus.NEW));
		when(classifier.classify(any())).thenAnswer(call -> fallback.classify(call.getArgument(0)));
		when(issues.findCandidates(eq("ARR"), any(), any(Limit.class))).thenReturn(List.of(candidate));
		when(matcher.findMatch(any(), any())).thenReturn(Optional.empty());
		when(summarizer.summarize(any(), any())).thenAnswer(call -> new FallbackIssueSummarizer()
				.summarize(call.getArgument(0), call.getArgument(1)));
	}

	@Test
	void withTheFallbackMatcherThreeReportsAtOneLocationBecomeThreeIssues() {
		ReportGrouper noLlm = grouper(new NoMatchIssueMatcher(), new FallbackIssueSummarizer());
		Report second = report(toilet);
		Report third = report(toilet);

		noLlm.group(report.getId());
		noLlm.group(second.getId());
		noLlm.group(third.getId());

		verify(issueService, times(3)).createFromReport(any());
		verify(issueService, never()).attachReport(any(), any());
	}

	@Test
	void withAMatcherAlwaysAnsweringTheFirstCandidateTheReportsJoinOneIssue() {
		when(matcher.findMatch(any(), any())).thenAnswer(call -> {
			List<CandidateIssueView> candidates = call.getArgument(1);
			return Optional.of(candidates.get(0).getId());
		});
		Report second = report(toilet);
		Report third = report(toilet);

		grouper.group(report.getId());
		grouper.group(second.getId());
		grouper.group(third.getId());

		verify(issueService).attachReport(candidate, report);
		verify(issueService).attachReport(candidate, second);
		verify(issueService).attachReport(candidate, third);
		verify(issueService, never()).createFromReport(any());
	}

	@Test
	void theMatcherSeesTheReportAndTheCandidatesButNoIds() {
		Report recent = report(toilet);
		when(reports.findByIssueIdAndStatusOrderByCreatedAtDesc(eq(candidate.getId()), eq(ReportStatus.ACCEPTED), any(Limit.class)))
				.thenReturn(List.of(recent));

		grouper.group(report.getId());

		ArgumentCaptor<NewReportView> view = ArgumentCaptor.forClass(NewReportView.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<CandidateIssueView>> candidates = ArgumentCaptor.forClass(List.class);
		verify(matcher).findMatch(view.capture(), candidates.capture());
		assertThat(view.getValue().getText()).isEqualTo("Nu mai e hârtie");
		assertThat(view.getValue().getLocationName().getEn()).isEqualTo("Arrivals toilets near belt 2");
		assertThat(candidates.getValue()).singleElement().satisfies(c -> {
			assertThat(c.getId()).isEqualTo(candidate.getId());
			assertThat(c.getSummary().getRo()).isEqualTo("Rezumat vechi");
			assertThat(c.getRecentReportTexts()).containsExactly("Nu mai e hârtie");
		});
	}

	@Test
	void anAnswerThatIsNotACandidateIsIgnored() {
		when(matcher.findMatch(any(), any())).thenReturn(Optional.of(UUID.randomUUID()));

		grouper.group(report.getId());

		verify(issueService).createFromReport(report);
		verify(issueService, never()).attachReport(any(), any());
	}

	@Test
	void aMatcherFailureCreatesANewIssue() {
		when(matcher.findMatch(any(), any())).thenThrow(new IllegalStateException("LLM down"));

		grouper.group(report.getId());

		verify(issueService).createFromReport(report);
		assertThat(report.getGroupedAt()).isEqualTo(NOW);
	}

	@Test
	void aMatchOnARecentlyResolvedIssueReopensIt() {
		candidate.setStatus(IssueStatus.RESOLVED);
		candidate.setResolvedAt(NOW.minus(Duration.ofMinutes(10)));
		when(matcher.findMatch(any(), any())).thenReturn(Optional.of(candidate.getId()));

		grouper.group(report.getId());

		verify(issueService).attachReport(candidate, report);
		verify(issueService).reopenByReport(candidate, report);
	}

	@Test
	void aMatchOnAnOpenIssueDoesNotReopen() {
		when(matcher.findMatch(any(), any())).thenReturn(Optional.of(candidate.getId()));

		grouper.group(report.getId());

		verify(issueService, never()).reopenByReport(any(), any());
	}

	@Test
	void aMergeRefreshesTheSummary() {
		when(matcher.findMatch(any(), any())).thenReturn(Optional.of(candidate.getId()));
		doReturn(new LocalizedText("3 persoane: fără hârtie", "3 people: no paper")).when(summarizer).summarize(any(), any());

		grouper.group(report.getId());

		assertThat(candidate.getSummaryRo()).isEqualTo("3 persoane: fără hârtie");
		assertThat(candidate.getSummaryEn()).isEqualTo("3 people: no paper");
		assertThat(candidate.getSummarySource()).isEqualTo(ClassificationSource.LLM);
	}

	@Test
	void aFailedOrEmptySummaryKeepsTheOldOne() {
		when(matcher.findMatch(any(), any())).thenReturn(Optional.of(candidate.getId()));
		doThrow(new IllegalStateException("LLM down")).when(summarizer).summarize(any(), any());

		grouper.group(report.getId());

		assertThat(candidate.getSummaryRo()).isEqualTo("Rezumat vechi");
		assertThat(candidate.getSummarySource()).isEqualTo(ClassificationSource.FALLBACK);
	}

	@Test
	void aReportWithoutALocationIsNeverMatched() {
		report.setLocation(null);

		grouper.group(report.getId());

		verifyNoInteractions(matcher);
		verify(issueService).createFromReport(report);
	}

	@Test
	void aReleasedReportWithAChosenTeamGetsItsOwnIssueEvenIfItIsAnEmergency() {
		report.setEmergency(true);
		report.setTeamOverride(TeamId.SECURITY);

		grouper.group(report.getId());

		verify(issueService).createFromReport(report);
		verify(issueService, never()).attachReport(any(), any());
		verifyNoInteractions(matcher);
	}

	@Test
	void anEmergencyJoinsTheOpenEmergencyIssueWithoutTheMatcher() {
		report.setEmergency(true);
		Issue open = issue(IssueStatus.NEW);
		when(issues.findFirstByLocationIdAndEmergencyTrueAndStatusNotOrderByCreatedAtDesc(toilet.getId(), IssueStatus.RESOLVED))
				.thenReturn(Optional.of(open));

		grouper.group(report.getId());

		verify(issueService).attachReport(open, report);
		verifyNoInteractions(matcher);
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

	@Test
	void theLastResortIsANewUnclassifiedIssue() {
		report.setClassificationSource(ClassificationSource.LLM);
		report.setCategory(Category.TOILET);

		grouper.groupAsUnclassified(report.getId());

		assertThat(report.getClassificationSource()).isEqualTo(ClassificationSource.FALLBACK);
		assertThat(report.getCategory()).isEqualTo(Category.OTHER);
		verify(issueService).createFromReport(report);
		verifyNoInteractions(matcher);
		assertThat(report.getGroupedAt()).isEqualTo(NOW);
	}

	@Test
	void anAlreadyGroupedReportIsLeftAlone() {
		report.setIssue(new Issue());

		grouper.group(report.getId());
		grouper.groupAsUnclassified(report.getId());

		verifyNoInteractions(issueService, matcher);
	}

	private ReportGrouper grouper(IssueMatcher issueMatcher, IssueSummarizer issueSummarizer) {
		return new ReportGrouper(reports, issues, issueService, classifier, fallback, questions, selector, issueMatcher,
				issueSummarizer, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private ClassificationInput classifiedInput() {
		ArgumentCaptor<ClassificationInput> input = ArgumentCaptor.forClass(ClassificationInput.class);
		verify(classifier).classify(input.capture());
		return input.getValue();
	}

	private Report report(Location location) {
		Report report = new Report();
		report.setId(UUID.randomUUID());
		report.setText("Nu mai e hârtie");
		report.setLanguage(Language.RO);
		report.setLocation(location);
		report.setStatus(ReportStatus.ACCEPTED);
		report.setDeviceHash("device-" + UUID.randomUUID());
		when(reports.findById(report.getId())).thenReturn(Optional.of(report));
		return report;
	}

	private Issue issue(IssueStatus status) {
		Issue issue = new Issue();
		issue.setId(UUID.randomUUID());
		issue.setStatus(status);
		issue.setLocation(toilet);
		issue.setCategory(Category.TOILET);
		issue.setSummaryRo("Rezumat vechi");
		issue.setSummaryEn("Old summary");
		issue.setSummarySource(ClassificationSource.FALLBACK);
		issue.setUpdatedAt(NOW.minus(Duration.ofMinutes(2)));
		return issue;
	}

	private static Location location(String areaId) {
		Area area = new Area();
		area.setId(areaId);
		Location location = new Location();
		location.setId(UUID.randomUUID());
		location.setArea(area);
		location.setNameRo("Toalete sosiri, lângă banda 2");
		location.setNameEn("Arrivals toilets near belt 2");
		return location;
	}
}
