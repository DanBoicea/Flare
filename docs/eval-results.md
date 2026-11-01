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
| 2026-11-01 | Gemma 4 12B Q5_K_M | a9c7ea76 | 99% | 93% | 75% / 99% | **100%** (11/11) | **0%** | 100% | 91% | 8.6 s / 9.9 s |

## Run 2026-11-01

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

### Reading this run

- **Emergencies:** all 11 caught. The word list alone catches 6; the LLM catches the other 5, which is why both layers stay. One false alarm: an 80-year-old confused man who is missing (m035), labelled HIGH, flagged as an Emergency. Erring that way is acceptable: it reaches Security and the duty manager sooner.
- **No genuine message was Bounced** and every garbage message was. All 27 Questions were answered or honestly "not covered" as labelled.
- **Category misses are borderline cases:** a fire exit sign's light (sign vs maintenance), missing trolleys (equipment), an empty information desk (assistance vs other), a false alarm that is over (rated a safety concern). m033, a lost wallet, became a Question answered with the lost-and-found information, which is arguably also useful.
- **Severity:** exact 75%, within one level 99%. The model rates one level higher more often than lower.
- **Grouping:** one false merge (two different Passengers needing assistance, g020) and two false splits: shared trolleys at nearby Locations (g025), and a Report scanned in another Area that mentions the same toilet (g026). g026 is a limit of the design: Reports in different Areas are never compared (pipeline.md section 5).
- **Speed:** 8.6 s median, 9.9 s at p90, inside the 40 s timeout but over C02's 5 s target. C06 compares Qwen 3.5 9B.
