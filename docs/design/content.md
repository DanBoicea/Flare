# Content and copy

Words are part of the design. They decide whether a stressed Passenger understands Flare in two seconds and whether staff trust the summary. The string files (`ro.json`, `en.json` in each app) must follow these rules. The team's Romanian speaker reviews all Romanian text.

## Voice

- **Plain, calm and direct.** Like a helpful information desk, not a marketing page and not a robot.
- **Second person, informal "tu" in Romanian** (decided). "Scrie mesajul", not "Scrieți mesajul". English uses "you".
- **Active verbs that say exactly what happens.** "Trimite", not "OK". "Mă ocup eu", not "Preia".
- **Sentence case everywhere.** No ALL CAPS, no Title Case.
- **No exclamation marks, no emoji, no jokes, no filler** ("Oops", "Super", "Ne pare rău pentru neplăcere").
- **One idea per sentence,** and sentences under about 20 words on the passenger page.

## Honesty rules (from [principles](../principles.md))

- **AI is labelled:** every automatic answer says "Răspuns automat" and, in small text, "Generat de AI din informațiile aeroportului". Staff summaries say "Rezumat scris de AI din N rapoarte".
- **Never pretend to be a person.** No first-person chatbot voice ("Sunt aici să te ajut"), no typing animations, no human names or avatars.
- **Never promise what we don't control.** Say "Raportul a ajuns la personalul aeroportului", not "Rezolvăm imediat".
- **Simulated data is labelled** in every demo screen: "Demonstrație cu date simulate".

## Errors and empty states

- **Say what happened and what to do next,** in the interface's voice. Don't apologise and don't be vague.
  - Good: "Nu am putut trimite mesajul. Verifică conexiunea și încearcă din nou." The text the Passenger typed is still there.
  - Bad: "A apărut o eroare."
- **Bounce messages never accuse.** They say what to change and give an example: "Încearcă din nou, în câteva cuvinte. De exemplu: „Lift stricat la poarta 3”."
- **Empty states invite action, or reassure:** "Nicio problemă activă pentru echipa ta." for staff, not "Nu există date".

## Romanian typography

| Rule | Right | Wrong |
| --- | --- | --- |
| Comma-below letters | ș ț Ș Ț (U+0219, U+021B) | ş ţ (cedilla, U+015F, U+0163) |
| Quotation marks | „Lift stricat” | "Lift stricat" |
| Ellipsis | … (one character) | ... |
| "check-in" | check‑in (non-breaking hyphen U+2011) so it never breaks across lines | check- / in |
| Time | 10:02 (24 h) | 10:02 AM |
| Relative time | acum 5 min, acum 1 oră | 5 minutes ago (in RO UI) |
| Numbers | digits: 3 persoane | trei persoane |
| Phone | 112 (never spaced) | 1 1 2 |

The fonts we use must contain ș ț Ș Ț ă â î. Atkinson Hyperlegible Next was checked on 2026-10-31 and has them all.

## UI vocabulary (Romanian / English)

Use these words in the UI, and match [GLOSSARY.md](../../GLOSSARY.md):

| Concept | RO (UI) | EN (UI) | Never |
| --- | --- | --- | --- |
| Report | raport | report | tichet / ticket, plângere |
| Issue (staff) | problemă | issue | incident, caz, tichet |
| Question | întrebare | question | |
| Automatic answer | răspuns automat | automatic answer | răspunsul asistentului |
| Location | locație (the name) + cod (the label) | location, code | zonă |
| Team | echipă | team | departament |
| Severity | gravitate: Critic, Ridicat, Mediu, Scăzut | severity: Critical, High, Medium, Low | prioritate |
| Status (list) | Nou, Nimeni nu se ocupă, „Ana se ocupă”, Rezolvat | New, Nobody on it, "Ana is on it", Resolved | Confirmat (history only) |
| Take an Issue | Mă ocup eu | I'm on it | Asignează-mi |
| Bounced (shown to Passenger) | (never named) "Nu am înțeles mesajul…" | "We couldn't understand…" | respins, blocat |
| Quarantined (staff) | De verificat | Needs review | carantină, spam |
| Emergency | urgență | emergency | |

## Fixed texts (never generated, never shortened)

- The emergency strip and the emergency screen ([frontends copy deck](../architecture/frontends.md)).
- "Nu cerem niciodată plăți, parole sau documente personale." on the compose screen and on every poster.
- "Aceasta nu este o linie de urgență."

## Poster copy (D07)

The poster uses the same words as the app, so people recognise the service. Bilingual, with Romanian first and larger:
- the invitation
- the short URL
- the "never asks" line
- the 112 line
- the Location label for staff

Draft in [marketing](../marketing.md#poster-contents-draft).
