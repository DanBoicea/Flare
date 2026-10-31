# data/test-set

**All content is simulated.** Every message was invented for the hackathon. None is a real Passenger message, a real social-media post or a real complaint, and no real people are named. Flight numbers use the fictional prefix `ZZ`.

The labelled Romanian and English test set: messages with their expected classification, plus grouping pairs (task D02). The evaluation tests in `apps/flare-backend` read it from `../../data/test-set` (task C05).

## `messages.jsonl`

One JSON object per line:

```json
{"id":"m001","text":"Nu mai e hârtie în toaleta de lângă banda 2","uiLanguage":"ro","scannedLocation":"ARR-WC-02",
 "expected":{"kind":"REPORT","emergency":false,"category":"TOILET","severity":"MEDIUM","language":"ro","answerFound":null,"bounceReason":null},
 "notes":"basic RO report"}
```

| Field | Meaning |
| --- | --- |
| `uiLanguage` | `ro` or `en`, the language the web app was showing |
| `scannedLocation` | A Location label from [`data/airport/airport.json`](../airport/airport.json), or `null` for an unknown QR code |
| `expected.kind` | `REPORT`, `QUESTION` or `GARBAGE` |
| `expected.emergency` | `true` only for Emergencies |
| `expected.category`, `expected.severity` | Set for Reports, `null` otherwise |
| `expected.language` | `ro`, `en` or `other`. Added to the task's example shape, because the classifier returns it and the task asks for `other` on foreign-language messages |
| `expected.answerFound` | For Questions: `true` if the Knowledge base covers it, `false` if not. `null` otherwise |
| `expected.bounceReason` | For garbage, `null` otherwise |
| `notes` | Why it is labelled this way. Covered Questions name their Knowledge base file (`kb: 01-baggage-missing`). Labels a reviewer should double-check say "ambiguous" or "reviewer check" |

### Counts (118 messages)

| Group | Count | Expected kind |
| --- | --- | --- |
| Reports | 52 | `REPORT` |
| Emergencies | 11 | `REPORT`, `emergency: true`, `CRITICAL` |
| Emergency near-misses (not Emergencies: "fire exit sign", "killing me lol", feeling unwell) | 5 | 4 `REPORT`, 1 `QUESTION` |
| Questions covered by the Knowledge base | 18 | `QUESTION`, `answerFound: true` |
| Questions not covered | 6 | `QUESTION`, `answerFound: false` |
| Garbage | 15 | `GARBAGE` |
| Injection attempts | 7 | 3 `GARBAGE`, 2 `REPORT`, 2 `QUESTION` (what a sane system does) |
| Other languages (German, French, Italian, Spanish) | 4 | `REPORT`, `language: other` |

By kind: 73 `REPORT`, 27 `QUESTION` (20 covered, 7 not covered), 18 `GARBAGE`. By language: 60 `ro`, 51 `en`, 7 `other`. Every category and every Severity appears at least once. One message comes from an unknown QR code and one from the muted QR code (`LND-ENT-03`). Their classification is unchanged; the code Quarantines them.

## `grouping-pairs.jsonl`

One pair of Reports per line, and whether they describe the same Issue:

```json
{"id":"g001","a":{"text":"No toilet paper","location":"ARR-WC-02"},"b":{"text":"Toilet roll empty in the second cubicle","location":"ARR-WC-02"},"sameIssue":true,"notes":"same symptom, same place"}
```

34 pairs: 18 `true`, 16 `false`. They cover the same symptom at the same place, a different symptom at the same toilet, the same symptom in a different Area, RO vs EN (and one Spanish) paraphrases, and tricky near-duplicates: identical text at different toilets, different seats in the same row of seating, and a Report scanned elsewhere that mentions the same toilet.

## How the labels were decided

- **Categories and Severities** follow the definitions in [AI pipeline](../../docs/triage-and-dedup.md#severity-scale-tentative-will-probably-change). For example, a broken seat is `MEDIUM`, a blocked exit is `HIGH`, and an unclear sign is `LOW`.
- **Emergencies are always `CRITICAL`** ([AI pipeline](../../docs/triage-and-dedup.md#emergencies), [pipeline](../../docs/architecture/pipeline.md): "Code forces `CRITICAL` if `emergency`").
- **Category of an Emergency (an assumption, to be confirmed by the Track C owner):** medical → `ASSISTANCE_NEEDED`; missing child → `LOST_ITEM_OR_PERSON` (this one is in the AI pipeline doc); fire, smoke or threat → `SAFETY_OR_SECURITY`. Routing does not depend on it, because Emergencies always go to Security and the duty manager. It only affects category accuracy.
- **Luggage:** "my bag didn't arrive" is a Question ([product](../../docs/product.md#lost-luggage-and-tickets-decided-2026-10-31)). A broken belt and missing trolleys are Reports for Maintenance.
- **Short, rude or misspelled messages about a real problem are Reports,** not garbage ("wc murdar", "the lfit is stuk", "Toaleta e o porcărie…").
- **Garbage with no recognisable language** ("asdfghjkl", "?", "lol") is labelled `language: other`.
- **Injection attempts** are labelled as if the injected instruction were not there: the underlying Report or Question if there is one, otherwise `GARBAGE` with `emergency: false`.
- **Covered vs not covered** was checked against the files in [`data/knowledge-base/`](../knowledge-base/README.md). The not-covered Questions use topics that the Knowledge base README lists as deliberately missing.
