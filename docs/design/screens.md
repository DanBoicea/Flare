# Screens

Screen-by-screen layout and behaviour. Each screen is rendered in [reference/](reference/README.md) (`?screen=<id>`). Copy follows [content](content.md), and the RO/EN strings live in each app's `i18n` files.

## Passenger web app

Every screen has the same frame, top to bottom: the emergency strip, the top bar (logo plate, airport name, language button), the content column (at most 36 rem, 20 px side padding), and the demo footer. On each screen change, focus moves to the screen's `h1` and the change is announced.

### `compose`: write the message
```
[Urgență? Sună la               (☎ 112)]
[Flare]                       (🌐 English)
Aeroportul Internațional Craiova

Cu ce te putem ajuta?           ← h1 = label
┌──────────────────────────────┐
│ text, 20px, grows            │
└──────────────────────────────┘
Scrie cu cuvintele tale. Poți și dicta…
┌ ☎ Vreau să fiu sunat înapoi ⌄ ┐
│   Opțional                    │
└───────────────────────────────┘
[████████ Trimite ████████]     ← 56px, yellow
🔒 Nu cerem niciodată plăți…
ⓘ Aceasta nu este o linie de urgență.
```
- **Keyboard behaviour:** when the field gains focus and the keyboard opens (`visualViewport` resize), scroll so the field *and* Trimite are visible. Use the textarea's `scroll-margin-bottom` (about 140 px), with a JS fallback that scrolls the field-to-button group to `block: "end"`.
- **Send:** disabled while the text is only whitespace. Tapping it shows `sending` at once (no double submits).
- **Compose-open state** (`compose-open`): the call-back row expands into the phone field with its retention hint, between the message and Trimite. The row stays visible with its chevron turned up, so the field can be closed again, and the trust line stays.

### `sending`: waiting
Centred: the beacon, then "Verificăm mesajul…" (h1), "Durează câteva secunde. Nu închide pagina.", and the echo of the Passenger's text.
- After 6 s, add "Poate dura puțin mai mult." After 20 s, give up with the network error on `compose`, keeping the text.
- `aria-live="polite"`, `aria-busy="true"`.

### `answer`: automatic answer (Question)
"Ai întrebat: „…”", then the answer card, then two stacked buttons.
- **Answer found:** **M-a ajutat** (primary) and **Întreabă personalul** (secondary).
- **Not found:** the card says the airport information doesn't cover it, and **Întreabă personalul** becomes primary.
- "M-a ajutat" clears the field and returns to `compose`.

### `bounced`: not understood
Warning notice, then the echo ("Ai scris: „…”"), then **Modifică mesajul** (primary, returns to `compose` with the text kept and focus in the field).
- From the third Bounce, add **Trimite oricum** (secondary) and the note "Mesajele trimise oricum sunt verificate de un om înainte să ajungă la echipe."

### `confirm`: sent
Round green tick, then the h1 "Mulțumim. Raportul a ajuns la personalul aeroportului.", then the lead "Echipa potrivită l-a primit pe telefon. Poți închide pagina." If a phone number was given, add "Te pot suna pentru detalii." Then **Trimite alt mesaj** (secondary). No status page.

### `emergency`: emergency instructions
The strip is hidden. Full-bleed red block with the h1 "Dacă cineva este în pericol, sună acum la 112.", the English line "If someone is in danger, call 112 now." (always both languages), and the 64 px **Sună la 112**. Below it: "Dacă poți, anunță cel mai apropiat angajat al aeroportului." and "Mesajul tău a fost trimis și personalului." Then a full-width **Înapoi** (secondary), which starts fresh.
- Fixed text, never generated. `role="alert"`.

### Error and offline (state of `compose`)
An inline alert above the field: "Nu am putut trimite mesajul. Verifică conexiunea și încearcă din nou." The text is kept, and Trimite works again.

## Staff app

Frame: the black header (on every top-level tab), the content, and the tab bar. Detail screens are pushed, with no tab bar.

### Sign-in
- **Header:** black, with the logo plate and "Personal".
- **Fields:** "E-mail" and "Parolă", each 56 dp with a 2 dp black frame. Autofill and paste are allowed.
- **Button:** **Intră** (primary).
- **Note:** "Conturi demonstrative cu date simulate."
- **Errors:** wrong details, no connection, not a staff member (each in plain words).

### `staff-list`: Active
Header (logo, "Personal", name and role, "Actualizat acum N s"), then Issue rows sorted by priority (Critical first), then the tab bar.
- **Refresh:** pull to refresh, plus 5 s polling while the app is in the foreground.
- **New Issues:** a soft highlight for 10 s and the "Nou" dot. A new **Critical** Issue is also announced to screen readers.
- **Empty:** "Nicio problemă activă pentru echipele tale."

### `staff-detail`: one Issue
See [components: detail](components.md#detail).
- **Conflicts:** a version conflict shows the toast "{Nume} se ocupă deja de asta." and reloads.
- **Gone:** an Issue that disappears shows "Problema nu mai este disponibilă." and goes back.

### Resolved
The same rows, read-only, with "Rezolvat de X" and the time. Opening one offers **Redeschide** in the sheet.

### Needs review (duty manager only)
- **Strict mode alerts first:** a `danger-soft` banner with the code, place, "de 12 min" and "37 de mesaje".
- **Then held Reports:** the reason as a tag (Limită depășită, Cod QR necunoscut, Cod QR oprit, Verificare roboți, Trimis oricum, Mod strict), the quoted text, the time, and the place or "Cod QR necunoscut: zz99…".
- **Actions:** **Trimite unei echipe** (opens the Team sheet, with "Fără echipă" first) and **Respinge**.
- **Empty:** "Nimic de verificat."

## QR poster (`poster`, A4 or A3 portrait)

```
┌───────────────────────────────────────────┐
│ ARR-WC-02              Toalete sosiri      │  ← location sign: yellow on black, yellow border
│                        lângă banda 2       │
├───────────────────────────────────────────┤
│        [Flare] Aeroportul Internațional Craiova
│          Ceva nu e în regulă? Scrie-ne.    │  ← Atkinson 800
│            Something wrong? Tell us.       │
│        ┌──────── yellow, black frame ──┐   │
│        │  [ QR, ≥ 60 mm, printed ]     │   │  ← direction panel
│        │  flare.example/q/k7f3m9q2xd   │   │  ← short URL, always printed
│        └───────────────────────────────┘   │
│  Nu cerem niciodată plăți, parole sau date de card.
│  Dacă codul QR pare lipit peste altul, nu-l scana.
├───────────────────────────────────────────┤
│  Doar pentru urgențe: 112    Emergencies only: 112   ← red band
└───────────────────────────────────────────┘
  Demonstrație cu date simulate
```
- **The QR code is printed on the poster, not stuck on:** a sticker on top is then visibly a tamper sign, and the poster says so.
- **Print:** three inks (black, yellow, red) on white. Proof the yellow on the actual printer, because office lasers push it towards orange. The black frames keep it readable on greyscale copies.
- **The code in the header matches the code staff see** in the Staff app (`ARR-WC-02`).
- **Generated by** [D07](../tasks/D07-qr-posters.md) from `data/airport/`.
