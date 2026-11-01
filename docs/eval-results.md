# Evaluation results

Results of the evaluation runner (task C05) on the **simulated** labelled test set (`data/test-set/`, task D02). Run it with a `llama-server` running (`node scripts/llm/start-llama-server.mjs`):

```sh
cd apps/flare-backend && ./mvnw test -Dgroups=eval -Dtest=EvaluationRunnerTest
```

It writes `apps/flare-backend/target/eval-results.md`. Copy each run here, newest first, and add a row to the history. Run it once per prompt or model change (a full run takes about 30 minutes on the M4 Pro).

**Read the numbers honestly:** the test set is small (118 messages, 34 pairs) and simulated, written by the team. The classify prompt was written knowing the labelling conventions in `data/test-set/README.md` (but not copying its sentences), so real Passenger messages will score lower. Every miss is listed below each run.

## History

| Date | Model | Prompts | Kind | Category | Severity exact / ±1 | Emergency recall | False Bounces | Questions | Grouping | p50 / p90 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 2026-11-01 | **Gemma 4 12B Q5_K_M, C06 round 2 (chosen)** | C06 prompts | 99% | 96% | 81% / 100% | **100%** (11/11) | **0%** | 100% | 94% | 8.7 s / 10.0 s |
| 2026-11-01 | Qwen 3.5 9B Q6_K, C06 round 1 | C06 round 1 prompts | 98% | 93% | 79% / 99% | 100% (11/11) | 1% | 85% | 88% | 5.8 s / 6.4 s |
| 2026-11-01 | Gemma 4 12B Q5_K_M, C06 round 1 | C06 round 1 prompts | 98% | 96% | 78% / 99% | 100% (11/11) | 2% | 96% | 94% | 8.7 s / 10.0 s |
| 2026-11-01 | Gemma 4 12B Q5_K_M, C05 baseline | a9c7ea76 | 99% | 93% | 75% / 99% | **100%** (11/11) | **0%** | 100% | 91% | 8.6 s / 9.9 s |

## Run 2026-11-01, C06 round 2 (chosen: Gemma, tuned prompts)

| | |
| --- | --- |
| Model file | `gemma-4-12b-it-Q5_K_M.gguf` |
| llama.cpp | b10470-34af94cd9, 1 slot(s) |
| Machine | Apple M4 Pro, 26 GB |
| Temperature | 0.0 |
| Prompts | git b2991e09 + uncommitted prompt changes |
| Test set | 118 messages, 34 grouping pairs (simulated, D02) |

| Metric | Result |
| --- | --- |
| Kind accuracy | 117/118 (99%) |
| Category accuracy (Reports) | 70/73 (96%) |
| Severity exact (Reports) | 59/73 (81%) |
| Severity within one level (Reports) | 73/73 (100%) |
| Team routing (Reports without Emergencies) | 60/62 (97%) |
| **Emergency recall** (word list + LLM; must be 100%) | 11/11 (100%) |
| Emergency recall, word list alone | 6/11 (55%) |
| False Emergencies (flagged, not labelled) | 1/107 (1%) |
| Garbage catch rate | 17/18 (94%) |
| **False-Bounce rate** (genuine Reports and Questions Bounced) | 0/100 (0%) |
| Questions: covered / not covered right | 27/27 (100%) |
| Language | 117/118 (99%) |
| Grouping pairs right | 32/34 (94%) |
| Grouping: false merges / false splits | 0 / 2 |
| LLM failures (fallback used) | 0/118 (0%) |
| Classify latency p50 / p90 | 8708 ms / 10026 ms |
| Tokens (all calls) | 977314 prompt, 29143 completion |

### Emergency misses

None.

### False Emergencies

| ID | Expected | Got | Text |
| --- | --- | --- | --- |
| m035 | REPORT/LOST_ITEM_OR_PERSON/HIGH | REPORT/LOST_ITEM_OR_PERSON/CRITICAL/EMERGENCY | Nu-l găsesc pe tatăl meu, are 80 de ani și e confuz, poartă o haină gri |

### Misclassified (kind, category for Reports, or covered/not covered for Questions)

| ID | Expected | Got | Text |
| --- | --- | --- | --- |
| m018 | REPORT/BROKEN_OR_MAINTENANCE/HIGH | REPORT/LIFT_OR_ESCALATOR/HIGH | Belt 1 is making a grinding noise and bags are getting stuck |
| m027 | REPORT/SIGN_OR_INFORMATION/MEDIUM | REPORT/BROKEN_OR_MAINTENANCE/LOW | Ecranul cu zboruri de la sosiri e negru |
| m044 | REPORT/OTHER/MEDIUM | REPORT/BROKEN_OR_MAINTENANCE/HIGH | Passengers from flight ZZ4410 waiting at belt 1, nothing has come out in 50 minutes |
| m099 | GARBAGE | REPORT/OTHER/LOW | this airport is sh*t and you're all useless idiots |

### Grouping misses

| ID | Expected | Got | How | Notes |
| --- | --- | --- | --- | --- |
| g025 | same | different | matcher | nearby Locations in the same Area; trolleys are shared |
| g026 | same | different | different Areas, not compared | tricky: scanned elsewhere but mentions the same toilet |

### Reading this run (the frozen demo setup)

- **Compared with the C05 baseline:** category 93% → 96%, severity exact 75% → 81% (within one level 100%), routing 94% → 97%, grouping 91% → 94% with no false merges. Emergency recall stays 100% and false Bounces stay 0%.
- **One step back, accepted:** one abusive message (m099, insults with no problem in it) became a LOW "Other" Report for the duty manager instead of a Bounce. Erring this way costs a duty manager a glance; a false Bounce would reject a real Passenger.
- **Remaining misses:** a grinding baggage belt filed under lifts (m018), a black arrivals screen filed as broken equipment rather than information (m027; the prompt now calls a screen with no power maintenance), slow baggage delivery rated a HIGH maintenance problem (m044), the missing 80-year-old flagged as an Emergency (m035), shared trolleys at two nearby spots not merged (g025), and the cross-Area mention (g026, a design limit).
- **Speed:** unchanged, 8.7 s median: the prompts are slightly longer but the answers are not.

### Model comparison (C06)

Qwen 3.5 9B Q6_K on the round 1 prompts: about a third faster (5.8 s median) and lighter, but Questions 85% (it claimed Knowledge base answers for "pets on the plane" and "car rental", which the Knowledge base does not cover), 2 false merges, 2 false Emergencies, language 97%. **The team chose Gemma 4 12B** (2026-11-01): trust matters more than 3 seconds, and both are inside the 40 s timeout (ADR-0016).

### C06 round 1 (Gemma), for the record

Severity anchors, category boundaries and two grouping rules (see C06's notes) raised category to 96%, routing to 98% and grouping to 94% with no false merges, but **Bounced two genuine messages** (m056 "this queue is killing me lol", m087 "Unde pot schimba bani?"), which C06's rule does not allow. Round 2 added two rules (airport-related questions are never garbage; slang around a real complaint is still a Report) and fixed both.

## Run 2026-11-01, C05 baseline

| | |
| --- | --- |
| Model file | `gemma-4-12b-it-Q5_K_M.gguf` |
| llama.cpp | b10470-34af94cd9, 1 slot(s) |
| Machine | Apple M4 Pro, 26 GB |
| Temperature | 0.0 |
| Prompts | git a9c7ea76 |
| Test set | 118 messages, 34 grouping pairs (simulated, D02) |

| Metric | Result |
| --- | --- |
| Kind accuracy | 117/118 (99%) |
| Category accuracy (Reports) | 68/73 (93%) |
| Severity exact (Reports) | 55/73 (75%) |
| Severity within one level (Reports) | 72/73 (99%) |
| Team routing (Reports without Emergencies) | 58/62 (94%) |
| **Emergency recall** (word list + LLM; must be 100%) | 11/11 (100%) |
| Emergency recall, word list alone | 6/11 (55%) |
| False Emergencies (flagged, not labelled) | 1/107 (1%) |
| Garbage catch rate | 18/18 (100%) |
| **False-Bounce rate** (genuine Reports and Questions Bounced) | 0/100 (0%) |
| Questions: covered / not covered right | 27/27 (100%) |
| Language | 117/118 (99%) |
| Grouping pairs right | 31/34 (91%) |
| Grouping: false merges / false splits | 1 / 2 |
| LLM failures (fallback used) | 0/118 (0%) |
| Classify latency p50 / p90 | 8648 ms / 9929 ms |
| Tokens (all calls) | 933444 prompt, 29554 completion |

### Emergency misses

None.

### False Emergencies

| ID | Expected | Got | Text |
| --- | --- | --- | --- |
| m035 | REPORT/LOST_ITEM_OR_PERSON/HIGH | REPORT/LOST_ITEM_OR_PERSON/CRITICAL/EMERGENCY | Nu-l găsesc pe tatăl meu, are 80 de ani și e confuz, poartă o haină gri |

### Misclassified (kind, category for Reports, or covered/not covered for Questions)

| ID | Expected | Got | Text |
| --- | --- | --- | --- |
| m028 | REPORT/BROKEN_OR_MAINTENANCE/MEDIUM | REPORT/SIGN_OR_INFORMATION/MEDIUM | Fire exit sign is broken, the light is off |
| m033 | REPORT/LOST_ITEM_OR_PERSON/MEDIUM | QUESTION/found=true | I lost my wallet somewhere between security and the gate, black leather |
| m040 | REPORT/BROKEN_OR_MAINTENANCE/MEDIUM | REPORT/LIFT_OR_ESCALATOR/MEDIUM | Nu mai sunt cărucioare pentru bagaje |
| m050 | REPORT/ASSISTANCE_NEEDED/MEDIUM | REPORT/OTHER/MEDIUM | Info desk empty for 30 min, nobody to ask |
| m054 | REPORT/BROKEN_OR_MAINTENANCE/MEDIUM | REPORT/SAFETY_OR_SECURITY/HIGH | Alarma de incendiu a sunat din greșeală acum 10 minute, poate verifică cineva |

### Grouping misses

| ID | Expected | Got | How | Notes |
| --- | --- | --- | --- | --- |
| g020 | different | same | matcher | tricky: two different Passengers each needing their own assistance |
| g025 | same | different | matcher | nearby Locations in the same Area; trolleys are shared |
| g026 | same | different | different Areas, not compared | tricky: scanned elsewhere but mentions the same toilet |

### Reading this run (C05 baseline)

- **Emergencies:** all 11 caught. The word list alone catches 6; the LLM catches the other 5, which is why both layers stay. One false alarm: an 80-year-old confused man who is missing (m035), labelled HIGH, flagged as an Emergency. Erring that way is acceptable: it reaches Security and the duty manager sooner.
- **No genuine message was Bounced** and every garbage message was. All 27 Questions were answered or honestly "not covered" as labelled.
- **Category misses are borderline cases:** a fire exit sign's light (sign vs maintenance), missing trolleys (equipment), an empty information desk (assistance vs other), a false alarm that is over (rated a safety concern). m033, a lost wallet, became a Question answered with the lost-and-found information, which is arguably also useful.
- **Severity:** exact 75%, within one level 99%. The model rates one level higher more often than lower.
- **Grouping:** one false merge (two different Passengers needing assistance, g020) and two false splits: shared trolleys at nearby Locations (g025), and a Report scanned in another Area that mentions the same toilet (g026). g026 is a limit of the design: Reports in different Areas are never compared (pipeline.md section 5).
- **Speed:** 8.6 s median, 9.9 s at p90, inside the 40 s timeout but over C02's 5 s target. C06 compares Qwen 3.5 9B.
