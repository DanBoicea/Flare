# D03: Simulated Knowledge base, RO + EN

Status: DONE
Owner: Luciano
Reviewer: Track C owner
Track: D (OpenCode) · Phase 1 · Estimate: 1 h · Agent-friendly: yes
Depends on: D01 (to use the same simulated place names)
Unblocks: C02

## Goal
The facts the LLM may use to answer Questions ([ADR-0005](../adr/0005-knowledge-base-answers-questions.md), [ADR-0014](../adr/0014-single-classification-call-with-knowledge-base.md)). It is **simulated**: it describes the fictional layout from D01, not the real airport.

## Do
1. **About 25 short Markdown files** in `data/knowledge-base/`, named `NN-topic.md` (for example `01-baggage-missing.md`). Each file is one topic: a `# Title`, then `## RO` and `## EN` sections of 2-6 plain sentences. Topics:
   - checked bag didn't arrive (go to the baggage services desk in the baggage claim area before leaving, bring the boarding pass and bag tag receipt; the desk is run by the airline's handling agent)
   - damaged bag
   - item lost in the terminal
   - item left on the plane
   - where to check in and drop bags
   - security screening basics, kept generic, for example "follow the instructions of security staff; liquids rules are shown at the checkpoint" (no invented specific rules)
   - assistance for reduced mobility (how to request it, where to wait)
   - information desk location
   - toilets and family room
   - drinking water
   - Wi-Fi (simulated network name)
   - lifts
   - smoking areas
   - transport (taxi and bus area, generic)
   - parking payment (generic)
   - lost and found
   - "this service is not an emergency line: call 112"
   - what Flare does with messages (for "is anyone reading this?")
2. **Top-level `data/knowledge-base/README.md`:** "SIMULATED, for the hackathon demo. Not real information about Craiova International Airport." The loader skips `README.md`, so tell C02.
3. **Write for answers:** plain words and concrete places from D01's simulated Locations. No opening hours, prices, phone numbers or company names, unless clearly marked fictional.
4. **Size:** keep the whole folder under about 15k words. C02 logs the size.

## Notes
- **For C02:** the folder has 22 topic files (`01-…` to `22-…`), about 2,300 words in total. The loader must skip `README.md`.
- Places come from `data/airport/airport.json` (D01). Details not in that file (the baggage services desk near belt 2, Wi-Fi name, smoking area, water fountains, trolley spots, family room equipment) are invented and marked "(simulat)" / "(simulated)" in the text.
- Deliberately not covered, for D02's "not covered" Questions: flight status, shops, restaurants, currency exchange, pharmacy, lounges, prayer room, car rental, pets, visas and customs. This list is also in the folder README.
- General rules that are not airport-specific are stated generically: the 48-hour advance request for assistance (an EU rule for air passengers) and the indoor smoking ban (a Romanian national rule).

## Done when
- [x] Every Question in D02 marked "covered" can be answered from these files, and the uncovered ones really can't. (Checked in D02.)
- [x] Every file has both RO and EN sections.
- [x] The reviewer approves. (Merged in PR #7.)
