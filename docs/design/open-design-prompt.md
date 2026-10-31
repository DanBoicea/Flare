# Open design brief (for Claude Design, Lovable, v0 and similar)

Use this to get a **new, different design** from another tool. The look is left free; the product, the content and the hard constraints are not. For visuals that follow our chosen design exactly, use [claude-design-prompt.md](claude-design-prompt.md) instead.

**How to use it**
1. Paste everything inside the fence. Optionally attach screenshots from `docs/design/reference/shots/` and say "this is our current direction; propose something different" (or "improve on it").
2. **Lovable or v0:** add "Build static screens only, with mock data, no backend, no login" so they don't spend effort wiring a real app.
3. Bring back screenshots of every screen at phone size (390 px wide), and a link or exported code if the tool gives one. I'll check them against the constraints below, can run the persona reviewers on them next to the current design, and fold what wins into `docs/design/reference/`.

```text
Design the mobile UI for "Flare", a hackathon prototype for Craiova International Airport (Romania).
All data is simulated; show "Demonstrație cu date simulate" on every screen. Do not use any official
airport logo; write the airport name as plain text.

PRODUCT
Passengers scan a QR code on a poster in the airport. A phone web page opens (no app, no account) with
ONE text field: they describe a problem or ask a question, in their own words. Then:
- a Question gets an instant automatic answer (AI, from airport information), with an option to ask staff;
- a real problem is sent to airport staff; reports of the same problem are grouped ("3 people reported");
- nonsense or spam is bounced back politely, asking them to rewrite it;
- an emergency phrase shows "call 112" instructions immediately.
Staff (cleaning, maintenance, security, passenger assistance, a duty manager) use a separate mobile
Staff app: a live list of Issues sorted by severity, each with a location, who reported it, who is
handling it, and one-tap actions.

USERS (design for the hardest first)
Elderly people with low vision and low digital confidence; stressed travellers using one thumb in
bright, glary light; tourists who don't read Romanian; non-technical staff glancing for 2 seconds while
walking, some colour-blind. People must trust the page within 2 seconds: fake QR stickers are a known
scam, so it must feel official and calm, never like an ad.

HARD CONSTRAINTS (non-negotiable)
- Mobile-first: designed at 390 x 844, must work at 320 px wide and at 200% text zoom. No horizontal scroll.
- WCAG 2.2 AA: text contrast 4.5:1, UI borders 3:1; touch targets at least 48 px, main buttons 56 px;
  body text at least 18 px on the passenger page, nothing below 16 px anywhere.
- Colour never carries meaning alone (severity = colour + icon + word).
- The emergency number 112 is visible on every passenger screen, clearly but without alarming people
  (it is NOT an emergency service; it must say so).
- The page never asks for payments, passwords or personal documents, and says so.
- One main action per screen. Plain words, sentence case, no emoji, no exclamation marks.
- AI is labelled honestly ("Răspuns automat", "poate greși"); nothing pretends to be a person.
- Romanian first, with an English switch. Use correct diacritics (ș ț with comma below, ă â î) and
  Romanian quotes „…”. Choose a typeface that clearly distinguishes I/l/1 and O/0.
- No generic "AI app" look: no purple gradients, glassmorphism, glows, sparkle icons for AI, identical
  rounded cards with soft shadows everywhere.

SCREENS AND COPY (Romanian; keep the meaning, you may improve the wording)
Passenger page:
1. Compose: title "Cu ce te putem ajuta?"; the text field (example text "Liftul de la poarta 3 nu merge.");
   hint "Scrie cu cuvintele tale. Poți și dicta: apasă microfonul de pe tastatură."; an optional
   "Vreau să fiu sunat înapoi" (phone number only); button "Trimite"; "Nu cerem niciodată plăți, parole
   sau documente personale."; "Aceasta nu este o linie de urgență."; an "Urgență? Sună la 112" call
   button; language switch "English"; brand "Flare" with "Aeroportul Internațional Craiova".
2. Sending: "Verificăm mesajul…", "Durează câteva secunde. Nu închide pagina.", their text quoted.
3. Answer: "Ai întrebat: „Nu mi-a venit bagajul. Unde mă duc?”"; answer "Mergi la biroul de bagaje din
   zona de sosiri, lângă banda 2, înainte să ieși din zona de bagaje. Ia cu tine cartea de îmbarcare și
   eticheta bagajului primită la check-in."; note "Răspuns generat automat (AI) din informațiile
   aeroportului. Poate greși."; buttons "M-a ajutat" and "Întreabă personalul".
4. Bounced: "Nu am înțeles mesajul." + "Scrie din nou, în câteva cuvinte, ce s-a întâmplat și unde.
   De exemplu: „Lift stricat la poarta 3”."; "Ai scris: „asdfgh qwerty”"; "Modifică mesajul";
   "Trimite oricum".
5. Sent: "Mulțumim. Raportul a ajuns la personalul aeroportului." + "Echipa potrivită l-a primit pe
   telefon. Poți închide pagina."
6. Emergency: "Dacă cineva este în pericol, sună acum la 112." / "If someone is in danger, call 112 now.";
   big "Sună la 112" button; "Dacă poți, anunță cel mai apropiat angajat al aeroportului."; "Mesajul tău
   a fost trimis și personalului."; "Înapoi".
Staff app (must look clearly different from the passenger page, so the two phones are told apart):
7. Active list (signed in as "Mihai (simulat), manager de tură", "Actualizat acum 4 s"), four Issues:
   - Critic, "Persoană căzută lângă poarta 3, nu răspunde", Poarta 3, code DEP-G03, Raportat de 2
     persoane, de 1 min, Nimeni nu se ocupă, Team Securitate, tag Urgență, button "Mă ocup eu";
   - Ridicat, "Liftul de la check-in nu funcționează", Check-in lângă intrare, CHK-LFT-01, 4 persoane,
     de 12 min, Nimeni nu se ocupă, Mentenanță;
   - Mediu, "Lipsește hârtia igienică, cabina 2", Toalete sosiri lângă banda 2, ARR-WC-02, 3 persoane,
     de 14 min, Nou, Curățenie;
   - Mediu, "Toaleta este murdară", same toilets, 1 persoană, de 20 min, "Ana se ocupă", Curățenie.
   Tabs: Active, Rezolvate, De verificat.
8. Issue detail for the toilet-paper Issue: AI summary "Lipsește hârtia igienică în toaleta de lângă
   banda 2, cabina 2." labelled "Rezumat scris de AI din 3 rapoarte"; location; the three original
   passenger messages with times (10:02, 10:06 in English "No toilet paper in cubicle 2", 10:10);
   main action "Mă ocup eu" plus more actions.
9. Optional: an A4 portrait QR poster for the toilets above (location code ARR-WC-02, "Ceva nu e în
   regulă? Scrie-ne." / "Something wrong? Tell us.", the QR code with its short URL printed under it,
   the "never asks for payment" line, "Dacă codul QR pare lipit peste altul, nu-l scana.", 112).

DELIVERABLE
High-fidelity screens 1-8 (and 9 if possible) at 390 x 844, same visual language across both apps and
the poster. Give the design a clear concept rooted in airports or public service, and briefly explain
your palette, typeface and key decisions.
```
