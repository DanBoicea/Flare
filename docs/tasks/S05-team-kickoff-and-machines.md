# S05: Kick-off: assign tracks, check machines, accept the proposed ADRs

Status: IN PROGRESS
Owner: all
Track: S (all) · Phase 0 · Estimate: 15 min · Agent-friendly: no
Depends on: none
Unblocks: everything

## Do
1. Write a name next to each track in [the board](README.md) "Tracks" table: A, B, C, and the OpenCode owner for D.
2. On every laptop, check:
   - `java -version` (21)
   - `node -v` (22)
   - `git`
   - Expo Go on each phone (iOS and Android)
   
   Write any gaps below. The back-end person needs Java 21, the Staff app person needs Node and Expo Go, and every phone used in the demo needs Expo Go.
3. Skim ADRs 0009 to 0019 (status `proposed`). For each, accept it (change its status to `accepted`) or write an objection below. Especially:
   - the dependency list in [ADR-0013](../adr/0013-tooling-and-dependencies.md)
   - the model choice in [ADR-0016](../adr/0016-llm-model-per-task-in-config.md)
4. Agree on the sync times for gates G1, G2 and G3 ([board](README.md)).

## Notes
- Track owners: A: Cristi · B: Cristi · C: Dan · D (OpenCode): Luciano
- Machine gaps: _
- ADR objections: _

## Done when
- [ ] Every track has an owner.
- [ ] Machine gaps are fixed or noted.
- [ ] The ADR statuses are updated.
