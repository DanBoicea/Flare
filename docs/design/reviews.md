# Design reviews

How the visual direction was chosen. Three directions were built with the same screens and copy ([explorations](explorations/README.md)), rendered as 390 × 844 phone screenshots (plus deuteranopia and greyscale simulations of the Staff list), and reviewed by five independent persona reviewers (Claude subagents). None of them knew which direction the designer favoured. All data was simulated.

## Round 1 (2026-10-31): three directions

| Reviewer persona | 1st | 2nd | 3rd |
| --- | --- | --- | --- |
| Elena, 72, retired teacher, low vision, Romanian only | A | B | C |
| Andrei, 34, rushed one-handed traveller, and Sarah, 41, tourist who doesn't read Romanian | A | B | C |
| Ionela, 45, cleaning team lead, and Mihai, 50, duty manager | A | B | C |
| Accessibility specialist (WCAG 2.2 AA) | A | B | C |
| Design lead and hackathon judge | A | B | C |

**Direction A, Semnalistică (airfield signage), won unanimously.** The reasons the reviewers gave:
- It is readable without glasses and in glare.
- It is the only direction whose typeface (Atkinson Hyperlegible Next) separates I/l and 0/O. In B and C, "Ia cu tine" read as "la cu tine".
- Critical stays the most visible severity under colour-blindness and greyscale simulation.
- It has a concept rooted in aviation (the ICAO airfield sign families) that reads as official, prints well, and works on a projector.

**Why the others lost:**
- **B, civic calm:** closely mirrors the NHS.UK palette, so it is generic and forgettable. Its pale secondary buttons looked disabled, and blue had five different jobs.
- **C, departures board:** dark navy with amber, which reviewers said looked like a betting or crypto app, a risk for a QR page people must trust. It had glare and astigmatism problems, condensed type, and Critical was its least visible severity. Not to be shipped for passengers. It might return as an optional night theme for the Staff app once Critical is fixed.

### Must-fixes found in A, and what we changed

| Finding (reviewers) | Change in the refined design |
| --- | --- |
| No airport name; "Flare" meant nothing (Elena, Andrei: blocker) | The logo became a yellow-on-black location-sign plate, with "Aeroportul Internațional Craiova" under it. No official airport logo |
| Yellow meant four things (brand, new, action, location) | A yellow **fill** means only "press this". Yellow **ink on black** means only a location sign (the logo and Location plates). "Nou" is a black dot |
| The yellow answer header shouted over the answer; ↗ read as an external link | The answer is a black-framed card with a plain "Răspuns automat" label. The arrow is gone, and the note says "Poate greși" |
| Bounce card too neutral; it didn't show what the Passenger wrote | Orange warning tint, plus "Ai scris: „…”" |
| Send hidden by the iOS keyboard | Heading doubles as the field label (saves 50 px), shorter field that grows, scroll behaviour in [mobile](mobile.md) |
| Phone call-back hidden behind a small arrow | A clear row: phone icon, "Vreau să fiu sunat înapoi", "Opțional" |
| Hint and footer text too small and pale | Nothing below 16 px; body and hints 18 px in a darker grey |
| Solid red strip on every screen frightened people and looked like an error; 112 mistaken for the lost-bag number | Calm pink strip "Doar pentru urgențe" with a red 112 button. Solid red only on the Emergency screen |
| Waiting screen looked frozen | A 120 px beacon with a black-framed pulse, "Nu închide pagina", and the Passenger's text quoted |
| Square tick looked like a checkbox; "nothing else to do" misleading | Round green tick; "Poți închide pagina" |
| Staff: plates louder than severity; name in grey | The location **name** is bold. On lists the code is an outlined label; the yellow-on-black plate appears only on the detail screen |
| Critical and Medium icons too similar; greyscale order wrong | Filled octagon, triangle, circle with bar, circle with down arrow. Bar luminance 0.128 < 0.194 < 0.237 < 0.276 (Critical darkest) |
| "3 persoane în 8 minute" was ambiguous; times inconsistent | "Raportat de 3 persoane" and "de 14 min" (time since first report), used the same way on list and detail |
| No owner or "who confirmed"; no Team; no freshness | "Nimeni nu se ocupă", "Confirmat de Mihai", "Ana se ocupă", a Team tag on every card, "Actualizat acum 4 s" |
| Action bar wrapped and was cut off | One primary button plus "Mai multe"; `nowrap`; pinned with safe-area padding |
| Staff and passenger phones looked alike on stage | Black staff header with the logo plate and "Personal" |
| Left accent bars read as a template tell | Removed. Only the severity stripe remains; quotes and hairlines replace the other bars |
| Accessibility: no focus style, targets 40-44 px, `overflow: hidden`, unlinked hints, SVGs not hidden | Two-tone focus ring, 48/56 px targets, scrolling instead of clipping, `aria-describedby`, `aria-hidden` icons, a forced-colours fallback |

### Raised by reviewers, not decided (needs the team)

- **"tu" vs "dumneavoastră".** Elena (72) found the informal "tu" rude at her age. The docs currently say "tu" is fine. This is a tone decision for the team.
- **Showing the scanned Location on the passenger page** (design lead). This would let a Passenger check that the page matches the poster, which helps against QR stickers. It conflicts with [ADR-0006](../adr/0006-location-specific-qr-codes.md) ("the Passenger is never shown the Location").
- **A dim or night theme** for the Staff app on late shifts (Ionela). Stretch.
- **The slashed zero** in times ("10:02"), from the typeface. Use an unslashed alternate in running text if the font offers one. Keep the slashed zero in codes.
- **Location codes avoid the letters I and O** (accessibility). That is why the lift code became `CHK-LFT-01`.

## Round 2 (2026-10-31): verification of the refined A

A fresh reviewer checked the refined reference screens against the round-1 must-fixes. **Most were confirmed fixed:**
- the airport name
- the call-back row
- larger and darker text
- the honest answer card
- the Bounce look
- the waiting screen
- the lighter language button
- the round tick
- the staff severity icons and greyscale order, with ownership, Team and freshness shown

**New problems found, and what we did:**

| Finding | Severity | Change |
| --- | --- | --- |
| Buttons were set to `nowrap`, so "Trimite întrebarea personalului" overflowed at 360-375 px and at 200% zoom | Blocker | Labels now wrap (balanced). Shorter labels: "Întreabă personalul", "Trimite oricum". Renders added at 360 px |
| Poster overflowed A4 and cut off its "date simulate" label | Major | Title 40 → 34 px, tighter gaps, bold 16 px label kept visible |
| "Doar pentru urgențe" as the first line could read as "this service is for emergencies only" | Major | Strip copy: "Urgență? Sună la [112]" / "Emergency? Call [112]" |
| Emergency screen was Romanian only | Major | English line always shown under the Romanian title |
| Small screens: the strip and a 3-line brand pushed Trimite below the fold; the staff header was 184 px | Major | Strip 56 px. The airport name sits on its own full-width line (one line from 330 px up). One-row staff header with no repeated "Active" title. A slim black bar on the staff detail keeps the staff cue |
| "Confirmat de Mihai" next to an available "Mă ocup eu" was ambiguous | Major | List status always says ownership: "Nou", "Nimeni nu se ocupă" or "Ana se ocupă". "Confirmat" moves to history |
| Yellow active-tab marker broke the one-meaning rule; 14 px tab counts | Minor | Black 3 px marker, 16 px counts |
| The 112 link's accessible name didn't start with its visible text (WCAG 2.5.3) | Minor | The name is "112, apel de urgență" |
| Also | Minor | Every card shows "Raportat de N". Row buttons are `aria-describedby` their card title. "Prima raportare" replaces the ambiguous "Deschis de". The phone field can be closed again and keeps the trust line. Ages don't wrap |

**Left as polish, not risk:**
- the stock Feather icon shapes
- tinted rounded notice boxes
- the sending screen still being a kind of spinner

**Verdict quoted from the reviewer:** "The system is coherent and reads like actual airfield signage, not a SaaS template… the poster is the best single piece." After the fixes above it judged the design good enough to win on looks.
