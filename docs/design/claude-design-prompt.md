# Prompt for Claude Design (or any Claude session)

Use this to get polished mock-ups, a pitch slide set or a poster from another Claude session, consistent with our design system. Attach the reference screenshots (`node docs/design/reference/render.mjs`) so it works from the real screens. Copy everything inside the fence.

```text
You are a senior product designer producing high-fidelity visuals for "Flare", a hackathon project
for Craiova International Airport (Romania). All data is simulated and must be labelled
"Demonstrație cu date simulate". Do not use any official airport logo; write the airport's name as plain text.

WHAT FLARE IS
Passengers scan a QR code on a poster in the airport and type a problem or a question into ONE text
field on a phone web page (no app, no account). Questions get an instant automatic answer from airport
information; real problems are grouped (e.g. "Raportat de 3 persoane") and routed to the right staff
team's mobile Staff app; nonsense is bounced back politely; emergencies show "call 112" instructions.
Users: stressed travellers, elderly people with low vision, people with one free hand, non-Romanian
speakers, and non-technical airport staff (some colour-blind). Romanian first, English second.

THE DESIGN LANGUAGE (already decided and user-tested; follow it exactly)
Concept: airfield signage (ICAO sign families), each with ONE meaning:
- White on red (mandatory) = EMERGENCY only: the 112 button, the emergency screen, Critical severity.
- Black on yellow with a black frame (direction) = PRESS THIS: the single main action per screen.
- Yellow on black with a yellow border (location) = THIS PLACE: location plates like ARR-WC-02 and the
  "Flare" logo plate.
Everything else is quiet: white paper, dark ink, 1px hairlines. No gradients, no shadows (except bottom
sheets), no glow, no glassmorphism, no emoji, no sparkle/robot icons for AI, no ALL-CAPS labels, no
arrows appended to buttons, no left accent bars except the staff severity stripe.

Colour tokens: black #000000, ink #1A1C1F, ink-2 #4A4F57, ink-3 #646B74, paper #FFFFFF,
paper-2 #F2F4F5, line #D5D9DD, signal (yellow) #FFC20E, danger #C8102E / danger-ink #A0102A /
danger-soft #FDECEE, warn #D9480F / #9C3B00 / #FFF0E6, info #4A86DC / #1A4F91 / #E9F1FB,
quiet #8A9099 / #474D55 / #EEF0F2, ok #1E7B4A / #17623B / #E7F4EC.
Yellow never sits on white without a 2px black frame.
Type: Atkinson Hyperlegible Next (400/700/800). Passenger page: body 18px, answers and typed text 20px,
titles 28px/700, emergency title 32px/800, nothing below 16px. Sentence case. Romanian quotes „…”,
comma-below ș ț.
Shape: plates and badges 3px radius, buttons and fields 8px, cards 12px. Buttons 56px tall (staff 48px),
2px black frame. Severity badge = colour + icon + word: Critic (filled red octagon, white text),
Ridicat (triangle, orange tint), Mediu (circle with bar, blue tint), Scăzut (circle with down arrow,
grey tint).

SCREENS (390 x 844 phone frames; reference screenshots attached)
Passenger web app, every screen: a calm pink strip "Urgență? Sună la" followed by a red "112" call button;
the "Flare" logo plate with "Aeroportul Internațional Craiova" under it; an "English" language button
with a globe icon; footer "Demonstrație cu date simulate".
1. Compose: "Cu ce te putem ajuta?" (the heading is the field label), big framed text field, hint
   "Scrie cu cuvintele tale. Poți și dicta: apasă microfonul de pe tastatură.", a row
   "Vreau să fiu sunat înapoi (Opțional)" with a phone icon, yellow "Trimite", trust lines
   "Nu cerem niciodată plăți, parole sau documente personale." and "Aceasta nu este o linie de urgență."
2. Sending: a yellow beacon with a black pulse ring, "Verificăm mesajul…", "Durează câteva secunde.
   Nu închide pagina.", and the person's own text quoted.
3. Answer: "Ai întrebat: „Nu mi-a venit bagajul. Unde mă duc?”", a black-framed card "Răspuns automat"
   with a clear answer about the baggage desk near belt 2, a note "Răspuns generat automat (AI) din
   informațiile aeroportului. Poate greși.", then "M-a ajutat" (yellow) and
   "Întreabă personalul" (white).
4. Bounced: an orange-tint notice "Nu am înțeles mesajul." with an example, "Ai scris: „…”",
   "Modifică mesajul" (yellow), "Trimite oricum" (white).
5. Confirmation: round green tick, "Mulțumim. Raportul a ajuns la personalul aeroportului.",
   "Echipa potrivită l-a primit pe telefon. Poți închide pagina."
6. Emergency: full red block "Dacă cineva este în pericol, sună acum la 112." plus the English line
   "If someone is in danger, call 112 now." with a big white
   "Sună la 112" button; below, "Mesajul tău a fost trimis și personalului." and "Înapoi".
Staff app (black header with the logo plate and "Personal", "Actualizat acum 4 s" with a green dot):
7. Active list: rows with a 6px severity stripe, badge + status ("Nimeni nu se ocupă" / "Ana se ocupă" /
   "Nou" with a black dot), bold title, bold place name with an outlined code
   (e.g. ARR-WC-02), "Raportat de 3 persoane", "de 14 min", a Team tag, one quick action; Critical rows
   have a pale red background. Bottom tab bar: Active, Rezolvate, De verificat.
8. Issue detail: badges, title, "Rezumat scris de AI din 3 rapoarte…", a grey box with the yellow-on-black
   plate ARR-WC-02 and "Toalete sosiri, lângă banda 2", facts, the original passenger quotes with times,
   and a pinned action bar "Mă ocup eu" + "⋯".
9. QR poster (A4 portrait): a yellow-on-black location sign header (ARR-WC-02 / Toalete sosiri), the logo
   plate with the airport name, "Ceva nu e în regulă? Scrie-ne." / "Something wrong? Tell us.", the QR
   code inside a yellow panel with a black frame and the printed short URL, "Nu cerem niciodată plăți,
   parole sau date de card." and "Dacă codul QR pare lipit peste altul, nu-l scana.", and a red band
   "Doar pentru urgențe: 112 / Emergencies only: 112".

DELIVERABLE
[Pick one: (a) high-fidelity mock-ups of screens 1-9, plus English versions of 1 and 3;
 (b) a 6-slide pitch deck: problem, how it works (phone + staff phone side by side), why it is safe
 (QR stickers, spam limits, quality check, 112), the design story (airfield signs, Atkinson Hyperlegible,
 tested with five persona reviews), measured results (leave placeholders: we fill numbers from our eval),
 cheap pilot; (c) the poster at A3, print-ready.]
Keep every rule above. If something is ambiguous, choose the calmer, more legible option and say what
you chose. Do not invent airport facts (gate numbers, desk names, opening hours): mark anything like
that as simulated.
```

**Tip:** for the pitch, put a passenger phone (compose, then answer) next to a staff phone (list) on one slide. The white passenger screen and the black staff header make the two roles obvious from the back of the room.
