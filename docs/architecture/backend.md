# Back end (Spring Boot)

Java 21, Maven wrapper, Spring Boot ([ADR-0013](../adr/0013-tooling-and-dependencies.md)). Package by feature ([code rules](../code-rules.md)). Root package `com.fsociety.flarebackend`. The owner track is in brackets.

```
com.fsociety.flarebackend
├── FlareBackendApplication
├── common/            [A] ProblemDetail error handling, ErrorCode enum, LocalizedText, Clock bean, text limits
├── config/            [A] FlareProperties (typed @ConfigurationProperties, Lombok @Value classes), SecurityConfig, CorsConfig, WorkerConfig
├── location/          [A] Area, Location, QrCode (entities + repositories), QrCodeService (resolve ID → ACTIVE/MUTED/UNKNOWN + Location)
├── safety/            [A] TextNormalizer, EmergencyDetector (+ emergency-terms.yml), BotCheck → HoneypotBotCheck — pure
├── ratelimit/         [A] SlidingWindowCounter, RateLimitService → InMemoryRateLimitService, QrCodeFloodGuard (Strict mode),
│                          BounceTracker → InMemoryBounceTracker, ClientFingerprint (HMAC) — pure; wired by RateLimitConfig
├── intake/            [A] PublicMessageController, PublicQuestionController, MessageIntakeService (orchestrator), dto/
├── question/          [A] Question entity + repository, QuestionService (send to staff)
├── report/            [A] Report entity + repository, ReportClassifications (copies a Classification onto a Report, pure)
├── classification/    [A] MessageClassifier (interface), ClassificationInput, Classification, FallbackClassifier,
│                          CategoryTeamRouting (category → Team, confidence rule)
│   └── llm/           [C] LlmMessageClassifier, ClassificationSchema, ClassificationValidator
├── knowledge/         [C] KnowledgeBase (loads knowledge-base/*.md from the classpath at startup)
├── llm/               [C] LlmClient (interface), LlamaServerClient (llama.cpp over HTTP), LlmTask enum, PromptLoader, LlmConfig,
│                          LlmFailure exception, LlmUsageCounter (daily cap, token logging)
├── grouping/          [A] GroupingWorker (single thread + queue + startup recovery), CandidateSelector (pure),
│                          ReportGrouper (the steps), IssueMatcher (interface), NoMatchIssueMatcher (fallback),
│                          NewReportView, CandidateIssueView, GroupingConfig
│   └── llm/           [C] LlmIssueMatcher
├── issue/             [A] Issue entity + repository, IssueService (create/merge/reopen), IssueActionService,
│                          IssueQueryService (list and detail), IssueCards (DTO mapping), IssueTransitions (pure),
│                          PriorityCalculator (pure), IssueConfig, StaffIssueController, dto/
│   └── summary/       [A] IssueSummarizer (interface), FallbackIssueSummarizer, IssueSummaryView, ReportTextView
│       └── llm/       [C] LlmIssueSummarizer
├── review/            [A] ReviewController, ReviewService (Quarantined list, Strict mode alerts, release, dismiss)
├── staff/             [A] StaffMember, Team, entities + repositories, CurrentStaff (JWT → staff member),
│                          StaffAccessPolicy (visibility, pure), MeController, TeamController
├── audit/             [A] AuditEvent entity + repository, AuditService (every history line, staff or system)
└── retention/         [A] RetentionJob (@Scheduled daily)
```

## Seams between Track A and Track C

Track A owns the interfaces and the fallbacks, and wires the LLM-backed implementations only when `flare.llm.enabled=true` (`@ConditionalOnProperty`). They call the self-hosted `llama-server` ([ADR-0022](../adr/0022-self-hosted-llm-llama-cpp.md)). Until Track C merges, the whole system runs on the fallbacks.

```java
// classification
public interface MessageClassifier {
    Classification classify(ClassificationInput input);       // throws LlmFailure (or any RuntimeException) on failure
}
@Value public class ClassificationInput {          // Lombok @Value: immutable, getters, all-args constructor
    String text; Language uiLanguage; Optional<LocationInfo> scannedLocation; boolean passengerInsisted;
}
@Value public class Classification {
    Kind kind; boolean emergency; Category category; Severity severity;
    Language language; double confidence; LocalizedText staffSummary;
    String mentionedPlace; String flightNumber;
    String answer; boolean answerFound; BounceReason bounceReason;
    ClassificationSource source;
}

// grouping
public interface IssueMatcher {
    Optional<UUID> findMatch(NewReportView report, List<CandidateIssueView> candidates);  // throws on failure
}

// issue.summary
public interface IssueSummarizer {
    LocalizedText summarize(IssueSummaryView issue, List<ReportTextView> newestReports);     // throws on failure
}
```

- The callers (`MessageIntakeService`, `GroupingWorker`) catch every exception from an LLM-backed implementation and use the fallback. Track C implementations never need to know the fallback exists.
- The `*View` classes hold only what the LLM needs: text, category, Location name, summary. Never contact details, hashes or IDs beyond the candidate Issue IDs.
- **Fallbacks:**
  - `FallbackClassifier`: `REPORT`, category `OTHER`, Severity `MEDIUM`, emergency from the word list, `source=FALLBACK`, summary = text cut to 140 characters for both languages.
  - `NoMatchIssueMatcher`: always empty.
  - `FallbackIssueSummarizer`: keeps the current summary.

## Configuration (`application.yml`; secrets only from env vars)

```yaml
flare:
  llm:
    enabled: ${FLARE_LLM_ENABLED:false}
    daily-call-limit: 2000
    base-url: ${FLARE_LLM_BASE_URL:http://localhost:8081}   # llama-server
    api-key: ${FLARE_LLM_API_KEY:}                          # llama-server --api-key; empty when it listens on 127.0.0.1 only
    tasks:   # timeouts are set from the S04 measurements
      classify:  { model: flare-llm, temperature: 0.0, max-tokens: 1024, timeout: 40s }   # Passengers wait rather than get the fallback
      match:     { model: flare-llm, temperature: 0.0, max-tokens: 512, timeout: 20s }
      summarize: { model: flare-llm, temperature: 0.0, max-tokens: 512, timeout: 20s }
  limits:
    device:   { per-10-min: 3, per-day: 10 }
    network:  { per-10-min: 30 }
    qr-code:  { per-10-min: 20, strict-mode-duration: 30m }
    bounce:   { window: 30m, send-anyway-after: 3, block-after: 6 }
    bot-check:{ min-elapsed-ms: 2000 }
  grouping:
    window: 30m
    max-candidates: 10
  questions:
    send-to-staff-window: 30m
  routing:
    low-confidence-threshold: 0.5
    category-team:
      CLEANING_OR_SPILL: CLEANING
      TOILET: CLEANING
      LIFT_OR_ESCALATOR: MAINTENANCE
      BROKEN_OR_MAINTENANCE: MAINTENANCE
      SIGN_OR_INFORMATION: PASSENGER_ASSISTANCE
      ASSISTANCE_NEEDED: PASSENGER_ASSISTANCE
      LOST_ITEM_OR_PERSON: PASSENGER_ASSISTANCE
      SAFETY_OR_SECURITY: SECURITY
      # OTHER → no Team (duty manager)
  priority:
    severity-weight: { CRITICAL: 10000, HIGH: 600, MEDIUM: 300, LOW: 100 }
    per-extra-reporter: 25
    max-extra-reporters: 10
    max-age-minutes: 120
    per-criticality: 50
    reopened-bonus: 50
  retention:
    contact-days-after-resolve: 30
    question-days: 30
    issue-days-after-resolve: 90
    dismissed-days: 7
  cors:
    allowed-origins: ${FLARE_ALLOWED_ORIGINS:http://localhost:5173}
  security:
    hash-secret: ${FLARE_HASH_SECRET}            # required; HMAC key for device/network hashes
spring:
  datasource: { url: "${SPRING_DATASOURCE_URL}", username: "${SPRING_DATASOURCE_USERNAME}", password: "${SPRING_DATASOURCE_PASSWORD}" }
  jpa: { open-in-view: false, hibernate.ddl-auto: validate, properties.hibernate.default_schema: flare }
  flyway: { schemas: flare, create-schemas: true, locations: classpath:db/migration }
  security.oauth2.resourceserver.jwt: { jwk-set-uri: "${SUPABASE_JWKS_URI}", issuer-uri: "${SUPABASE_JWT_ISSUER}" }   # see auth-and-security.md
management.endpoints.web.exposure.include: health
```

The `local` and `demo` profiles add `classpath:db/seed` to `spring.flyway.locations`. The LLM server's address and optional key come from `FLARE_LLM_BASE_URL` and `FLARE_LLM_API_KEY`.

## Conventions specific to the back end

- **Controllers** return DTO classes (`dto/` subpackages) whose names match the contract schemas (`IssueCard`, `IssueDetail`, `MessageResponse`, …).
- **Lombok instead of records** ([ADR-0013](../adr/0013-tooling-and-dependencies.md)): `@Value` for immutable DTOs, config and value classes; `@Getter @Setter` for request bodies and entities. Never `@Data` or `@ToString` on a class that holds Passenger text or contact details, so they can't end up in logs.
- **Errors:** throw `FlareException(ErrorCode)`. `common.ApiExceptionHandler` turns it, and Spring's own errors, into Problem Details with `code`. Bean Validation messages on DTOs name the code, for example `@NotBlank(message = "TEXT_EMPTY")`.
- **Entities** use JPA with explicit column names, `@Version` on `Issue`, and enums as `@Enumerated(STRING)`.
- **Transactions** live in services. The orchestrator commits the Report before enqueueing it (use `TransactionSynchronization.afterCommit`, or enqueue after the service call returns).
- **Pure classes** (`safety`, `CandidateSelector`, `PriorityCalculator`, `StaffAccessPolicy`, `SlidingWindowCounter`, `ClassificationValidator`, `CategoryTeamRouting`) have no Spring annotations and are unit tested first ([code rules](../code-rules.md) section 6).
- **Logging** goes through SLF4J and uses IDs only. Never log `text`, `contact`, raw IPs or device IDs.
- **No new libraries** beyond [ADR-0013](../adr/0013-tooling-and-dependencies.md).
