# Components

Every component used in Flare, with anatomy, sizes, states and accessibility. Values refer to [foundations](foundations.md) tokens. See them rendered in [reference/](reference/README.md). Web class names follow `reference/flare.css`; Staff app components keep the same names in PascalCase.

## Shared

### Button

| Variant | Look | Use |
| --- | --- | --- |
| Primary | `signal` fill, black text, `frame` | The one main action: Trimite, M-a ajutat, Modifică mesajul, Mă ocup eu |
| Secondary | white fill, `ink` text, `frame` | Other actions: Întreabă personalul, Trimite oricum, Înapoi, Mai multe |
| Call | white on the red emergency area, `danger-ink` text, 64 px | Sună la 112 (Emergency screen) |
| Icon | 56 × 56 secondary with one icon and an `aria-label` | Mai multe acțiuni (⋯) |

- **Size:** 56 px tall on the passenger page, full width. 48 px (`--sm`) in Staff rows. Label is 20 px weight 800 (18 px for small buttons). Labels wrap onto two balanced lines rather than overflow, so keep them short (22 characters or fewer). Only small row buttons stay on one line.
- **States:**
  - pressed: 1 px down, and primary darkens to #E6AE00
  - disabled: never shown. Remove the action or explain why it isn't possible
  - busy: the screen changes to a waiting state instead of a spinner inside the button
- **Labels:** verbs that say what happens. Keep the same word through the flow (Trimite → "Raportul a ajuns…").

### Focus ring
A two-tone ring on every interactive element ([foundations](foundations.md#focus)). It must never be hidden by a sticky bar: use `scroll-padding-bottom` and `scroll-margin`.

### Severity badge
A 3 px radius badge with an icon and a word, 16 px weight 800.
- **Critical:** filled `danger` with white text and a filled octagon icon.
- **High, Medium and Low:** their tint as background, the matching `-ink` for text, and their own icon ([foundations](foundations.md#icons)).

Never shorten the word, and never show the badge without it.

### Status
Text of 16 px weight 700, with an optional icon:

| Status | Look |
| --- | --- |
| Nou | Black 10 px dot and the word |
| Nimeni nu se ocupă | Seen, but nobody has taken it. `danger-ink` for Critical and High, `ink-2` otherwise |
| Confirmat de X | History only, never a list status, so the list always says who is handling the Issue |
| X se ocupă | `user-check` icon, `ink` |
| Rezolvat de X | `check-circle`, `ok-ink` |

### Location code and plate
- **Code** (lists): an outline label with a 1.5 px `ink` border, 3 px radius, 16 px weight 700, tabular figures, `nowrap`. It always follows the bold **place name**, which people navigate by.
- **Plate** (detail, posters): yellow on black with a 2 px yellow inner border, 20 px weight 800. The yellow-on-black location sign.
- **Code format:** `AREA-TYPE-NN`, such as `ARR-WC-02`. Codes **never use the letters I or O**, to avoid confusion with 1 and 0 (simulated data in [D01](../tasks/D01-simulated-airport-data.md)).

### Tag
A 28 px tall label with a 1 px `line` border, 16 px weight 700, `ink-2`. Used for Team ("Curățenie") and "Urgență" (red border, `danger-ink`). Tags are not buttons, so they never look pressable.

### Logo plate
See [foundations: brand](foundations.md#brand). On a black header it needs no extra frame.

## Passenger web app

### Emergency strip
Pink `danger-soft` band, 56 px tall, with a 1 px #F3C5CC bottom border. On the left, **"Urgență? Sună la"** (EN "Emergency? Call") in bold `danger-ink`, 18 px. On the right, the **112 button**: red fill, white text, phone icon, 48 px tall, at least 96 px wide, linking to `tel:112`. Its accessible name starts with its visible text: "112, apel de urgență" (visually hidden suffix).
- It sits in the same place on every screen (WCAG 3.2.6 consistent help) and is hidden on the Emergency screen, which replaces it.
- `role="note"`. It is never a full red band: tested users read that as an error.

### Top bar
- **First row:** the logo plate on the left, the language button on the right.
- **Second row:** **"Aeroportul Internațional Craiova"** across the full width (16 px weight 700, `ink-2`), so it stays on one line.
- **Language button:** It is 48 px tall with a 1.5 px `ink-3` border, a globe icon, and the *other* language's own name ("English" or "Română", `lang` set accordingly). It is lighter than the main action on purpose.

### Message field
- **The heading is the label:** `<h1><label for>Cu ce te putem ajuta?</label></h1>`. There is no second label.
- **Field:** a textarea with a 2 px black frame and 8 px radius. Text is 20 px. Minimum height 8 rem; it grows with content (`field-sizing: content`) up to 40% of the viewport, then scrolls.
- **Attributes:** `enterkeyhint="send"`; the hint is linked with `aria-describedby`.
- **Hint:** "Scrie cu cuvintele tale. Poți și dicta: apasă microfonul de pe tastatură."
- **Counter:** appears only above 900 characters ("940 din 1000").
- **Error:** an inline message under the field with `role="alert"`, in `danger-ink` with a pink tint background. The text is kept.

### Call-back row
A full-width row with a 1.5 px `ink-3` border and 8 px radius. It shows a phone icon, **"Vreau să fiu sunat înapoi"** (bold) over "Opțional", and a chevron. It is a disclosure button (`aria-expanded`). When open, the phone field (`type="tel"`, 56 px) appears with the retention note: "Te sunăm doar pentru acest raport. Numărul se șterge la 30 de zile după rezolvare."

### Trust notes
Two lines, 18 px in `ink-2`, each with an icon:
- lock: "Nu cerem niciodată plăți, parole sau documente personale."
- info: "Aceasta nu este o linie de urgență."

### Beacon (sending)
A 120 px circle. The centre is a 48 px `signal` dot with a 3 px black ring. Around it, a 3 px black ring pulses outwards and fades (1.8 s loop). It is static under reduced motion.

### Echo
The Passenger's own text in Romanian quotes „…”, in a `paper-2` box with 12 px radius and `ink-2` text. Used while sending ("„Liftul de la poarta 3 nu merge.”") and on Bounce ("Ai scris: „…”"). Plain text only.

### Answer card
- **Card:** 2 px black frame, 12 px radius.
- **Label:** an info icon and **"Răspuns automat"** (18 px weight 800).
- **Answer:** 20 px text in paragraphs.
- **Source line:** below a hairline, 16 px `ink-2`: "Răspuns generat automat (AI) din informațiile aeroportului. Poate greși."
- **Above the card:** "Ai întrebat: „…”" in `ink-2`.
- **No** yellow band and no arrow icon. Both tested as noise or as "external link".

### Notice (Bounce)
`warn-soft` card with 12 px radius, a pencil icon in `warn-ink`, and the title **"Nu am înțeles mesajul."** (22 px weight 700). The body explains what to write and gives an example in quotes. `role="alert"`. Never accusing, never showing thresholds.

### Done mark
A 72 px green (`ok`) circle with a white 3 px check. Never a square, because it tested as a checkbox.

### Emergency block
Full-bleed `danger` area at the top of the Emergency screen:
- a white 32 px weight-800 title, "Dacă cineva este în pericol, sună acum la 112."
- the same in English underneath, 22 px, whatever the page language: "If someone is in danger, call 112 now."
- the 64 px white **Sună la 112** call button

`role="alert"`. Below it on white: the fixed body text and a full-width secondary **Înapoi** button.

### Demo footer
"Demonstrație cu date simulate", 16 px `ink-2`, above a hairline, on every screen. It stays during the demo.

## Staff app

### Header
- **One row:** logo plate, "Personal" (#C4C8CE), and the language button (white text, #8A9099 border). There is no visible screen title: the tab label names the screen, and the heading stays for screen readers.
- **Second line:** "Nume (simulat), rol", plus **"Actualizat acum N s"** with a green live dot. It becomes an offline banner when polling fails: "Fără conexiune. Ultima actualizare acum 2 min."
- The black header lets you tell the staff phone from a passenger phone at a glance, including on stage.

### Issue row
- **Container:** full width, 1 px hairline below, and a **6 px severity bar** on the left (the only coloured bar in the system). Critical rows also get the `danger-soft` background.
- **Content, in this order:**
  1. severity badge and status
  2. title (20 dp weight 700, at most 2 lines)
  3. **place name** (bold) and the code label
  4. "Raportat de N persoane" with the `users` icon (on every card), and the age on the right ("de 14 min" = time since first report, the same meaning everywhere)
  5. tags (Team, and Urgență if relevant), with the one quick action on the right
- **Quick action:** primary (yellow) only on Critical or High with nobody on it; otherwise secondary.
- A chevron on the right shows the row opens.
- **Screen readers:** the whole row reads as one sentence, for example "Critic. Persoană căzută lângă poarta 3. Poarta 3, zona de așteptare. Raportat de 2 persoane. De 1 minut. Nimeni nu se ocupă."
- **New Issues:** a soft `paper-2` fade for 10 s, and the black "Nou" dot.

### Detail
- **Top line:** a slim black bar with a white back button ("‹ Active") and the "Actualizat" line, so the staff cue stays on the detail screen.
- **Badges:** severity, status, Team.
- **Title:** 24 dp weight 800, followed by "Rezumat scris de AI din N rapoarte. Rapoartele originale sunt mai jos."
- **Place box:** `paper-2`, 12 px radius, with the **yellow-on-black plate**, the place name and the Area.
- **Facts:** two columns, "Raportat de / 3 persoane" and "Prima raportare / acum 14 min (10:02)".
- **Reports:** a list headed "Ce au scris pasagerii". Each is a quote „…” with its time and language, separated by hairlines. No left bars.
- **Action bar:** pinned at the bottom above the safe area. One primary action and the "Mai multe" (⋯) button, which opens a sheet with Confirmă, Predă altei echipe, Schimbă gravitatea, Redeschide, and Sună la 112 for Emergencies.

### Tab bar
- **Tabs:** white bar, 60 dp tall: Active, Rezolvate, De verificat (duty manager only).
- **Current tab:** black label and icon, plus a 3 px black marker. Yellow fill is reserved for actions.
- **Counts:** black pills with white 16 px numbers.

### Bottom sheet
- 16 dp top radius, the only shadow in the system, and a 48 dp tall handle area.
- Options are full-width 56 dp rows with icon and label.
- Destructive or important actions ask once: "Rezolvi problema?" with **Da, e rezolvată** / Anulează.
- The Android back button closes the sheet.

### Banner and toast
- **Banner:** full width under the header. `warn-soft` for offline; `danger-soft` for "Cod QR în modul strict" alerts in Needs review.
- **Toast:** black with white text, at the bottom above the tab bar, for 4 s, announced to screen readers. Example: "Ana se ocupă deja de asta."

### Empty state
An icon, one calm sentence and no illustration: "Nicio problemă activă pentru echipele tale." For Needs review: "Nimic de verificat."
