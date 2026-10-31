# Frontends

Both apps use TypeScript, have no state library, and keep strings in `ro.json` / `en.json` ([ADR-0013](../adr/0013-tooling-and-dependencies.md)). Both have a **mock mode** that serves fixtures shaped by the contract, so they can be built before the back end exists. Mock mode also doubles as the demo fallback ([operations](operations.md)).

## Shared conventions

- **API types:** `src/api/schema.d.ts` is generated from `contract/openapi.yaml` (`npm run gen:api`) and committed. `src/api/client.ts` is a small typed `fetch` wrapper. `src/api/mock.ts` returns fixtures when `MOCK=true`.
- **Strings:** `src/i18n/{ro,en}.json` hold flat dotted keys (`compose.placeholder`). The `t(key, vars)` helper is about 20 lines. Problem Details `code` values and priority reason codes map to keys (`error.TEXT_TOO_LONG`, `priority.REPORTERS`). `scripts/check-i18n.mjs` fails if the two files differ in keys.
- **Language:** default from the device (`navigator.language` on the web, `expo-localization` in the Staff app). Romanian is the default; English only when the device language is English (`en*`). The manual choice is saved (`localStorage` / AsyncStorage).
- **Plain text only:** data is always rendered as text nodes. Never `dangerouslySetInnerHTML`, never `WebView`.
- **Accessibility:**
  - labels on every control
  - touch targets at least 48 px, and main passenger buttons at least 56 px tall
  - WCAG 2.2 AA contrast
  - Severity shown with colour, icon and word together
  - system text scaling respected
  - focus order follows the visual order
- **Design:** follow [docs/design](../design/README.md) (tokens, components, screens, reference HTML). Calm, high-contrast, human-made. Neutral surfaces, one accent colour, system fonts, generous spacing. No gradients, glow, glassmorphism or emoji icons. The design tokens (colours, type scale, spacing, radii) are defined once per app in `styles/tokens.css` and `constants/theme.ts` with the same values, so both apps feel related. Use the frontend-design skill for W04 and B06.

## Passenger web app (`apps/flare-webapp`)

**Entry:**
- The URL is `/q/{qrCodeId}`. `lib/qrCode.ts` reads it from `location.pathname`. Any other path means "no QR code" and still works; the back end Quarantines the message.
- The host must serve `index.html` for every path (SPA fallback).

**Device ID:** `lib/deviceId.ts` returns a UUID stored in `localStorage`. Generate it with `crypto.getRandomValues`, because `crypto.randomUUID` is missing on plain-HTTP LAN testing.

**Screen state machine (`App.tsx`), no router:**

```
Compose ──send──▶ Sending ──┬─ REPORTED  ─▶ Confirmation ──"Send another"──▶ Compose (cleared)
   ▲                        ├─ ANSWERED  ─▶ Answer ──"Send to staff"──▶ Sending(send-to-staff) ─▶ Confirmation
   │                        │                 └─"That helped"──▶ Compose (cleared)
   │                        ├─ BOUNCED   ─▶ Bounced ──"Edit message"──▶ Compose (text kept)
   │                        │                 └─(sendAnywayAvailable)"Send anyway"──▶ Sending(sendAnyway) ─▶ Confirmation
   │                        ├─ EMERGENCY ─▶ Emergency (112 instructions; "Back" → Compose)
   └──── error/offline ─────┘ (Compose shows an inline error, text kept, "Try again")
```

- **On every screen:** the `EmergencyStrip` at the top, and the `LanguageButton` at the top right.
- **Compose:**
  - the title, which is the big text field's label, then the field with a dictation hint and a counter near the limit (from 800 of 1000 characters). Enter sends, Shift+Enter adds a new line
  - a "I would like a call back (optional)" checkbox; ticking it reveals the phone input and one line on retention. Unticked sends no contact ([ADR-0021](../adr/0021-design-v1-navy-cobalt-instrument-sans.md))
  - the hidden honeypot `website` (visually hidden, `tabIndex=-1`, `aria-hidden`, `autocomplete="off"`)
  - the Send button, disabled while the field is empty
  - the privacy line and "This is not an emergency line." under it
  - `elapsedMs` measured from page load
- **Sending:** "Checking your message…" with the pulsing beacon, "Don't close this page.", an `aria-live` region, and the Passenger's text quoted under "You wrote". After 6 s it adds "This can take a few seconds." The request times out after 20 s and shows the error state.
- **Focus:** on every screen change, focus moves to the new screen's heading (not on first load).
- **Answer:** labelled "Automatic answer". It shows the answer text and two buttons, "That helped" and "Send to staff". If `found` is false, "Send to staff" is the main button.
- **Bounced:** the reason text, chosen by `bounce.reason`, and "Edit message". From the third Bounce, also "Send anyway".
- **Confirmation:** "Thank you. Your report was sent to airport staff." plus a line on what happens next. No status page.
- **Emergency:** the fixed instructions. The message has also gone to staff.

**Mock mode:** `VITE_MOCK=true` routes each send to a fixture by keyword (`"?"` gives an answer, `"joke"` a Bounce, `"fire"` an Emergency, anything else a Report), with a 1.5 s delay.

**Env:** `VITE_API_BASE_URL`, `VITE_MOCK`.

### Passenger copy deck (draft; D04 turns it into string files)

| Key | EN | RO |
| --- | --- | --- |
| `emergency.strip` | Emergency? Call (followed by the 112 button) | Urgență? Sună la (followed by the 112 button) |
| `emergency.strip.note` | This is not an emergency line. | Aceasta nu este o linie de urgență. |
| `compose.title` | How can we help? | Cu ce te putem ajuta? |
| `compose.placeholder` | Report an issue in the airport or ask for help... | Raportează o problemă în aeroport sau cere ajutor... |
| `compose.hint` | Write in your own words. You can also dictate: tap the microphone on your keyboard. | Scrie cu cuvintele tale. Poți și dicta: apasă microfonul de pe tastatură. |
| `compose.contact.toggle` | I would like a call back | Vreau să fiu sunat înapoi |
| `compose.optional` | (optional) | (opțional) |
| `compose.contact.label` | Phone number | Număr de telefon |
| `compose.contact.note` | Only used for this report. Deleted 30 days after it is resolved. | Folosit doar pentru acest raport. Șters la 30 de zile după rezolvare. |
| `compose.send` | Send | Trimite |
| `compose.privacy` | We never ask for payments, passwords or personal documents. | Nu cerem niciodată plăți, parole sau documente personale. |
| `sending.checking` | Checking your message… | Verificăm mesajul… |
| `sending.slow` | This can take a few seconds. | Poate dura câteva secunde. |
| `sending.keepOpen` | Don't close this page. | Nu închide pagina. |
| `sending.youWrote` | You wrote | Ai scris |
| `answer.label` | Automatic answer | Răspuns automat |
| `answer.helped` | That helped | M-a ajutat |
| `answer.sendToStaff` | Ask staff | Întreabă personalul |
| `answer.notFound` | We don't have an answer for this. Send it to airport staff? | Nu avem un răspuns pentru asta. O trimitem personalului aeroportului? |
| `bounce.NOT_AIRPORT_RELATED` | This doesn't seem to be about the airport or your journey. Please describe a problem here or ask a question. | Mesajul nu pare să fie despre aeroport sau călătoria ta. Descrie o problemă de aici sau pune o întrebare. |
| `bounce.JOKE_OR_NONSENSE` | We couldn't understand this as a problem or a question. Please try again in a few words. | Nu am înțeles mesajul ca problemă sau întrebare. Încearcă din nou, în câteva cuvinte. |
| `bounce.SPAM_OR_ADVERTISING` | This looks like advertising or spam, so it was not sent. | Mesajul pare reclamă sau spam, așa că nu a fost trimis. |
| `bounce.ABUSIVE` | Please describe the problem without offensive language. | Te rugăm să descrii problema fără limbaj ofensator. |
| `bounce.TOO_UNCLEAR` | Please add a little more detail: what is wrong, and where? | Adaugă puțin mai multe detalii: ce nu e în regulă și unde? |
| `bounce.edit` | Edit message | Modifică mesajul |
| `bounce.sendAnyway` | Send anyway | Trimite oricum |
| `confirm.title` | Thank you. Your report was sent to airport staff. | Mulțumim. Raportul tău a fost trimis personalului aeroportului. |
| `confirm.next` | Staff will look at it as soon as possible. You don't need to do anything else. | Personalul îl va verifica cât mai curând. Nu mai trebuie să faci nimic. |
| `confirm.another` | Send another message | Trimite alt mesaj |
| `emergency.title` | If someone is in danger, call 112 now. | Dacă cineva este în pericol, sună acum la 112. |
| `emergency.body` | If you can, tell the nearest airport staff member. Your message has also been sent to airport staff. | Dacă poți, anunță cel mai apropiat angajat al aeroportului. Mesajul tău a fost trimis și personalului. |
| `emergency.call` | Call 112 | Sună la 112 |
| `error.network` | We couldn't send your message. Check your connection and try again. | Nu am putut trimite mesajul. Verifică conexiunea și încearcă din nou. |
| `demo.simulated` | Demo with simulated data | Demonstrație cu date simulate |

`emergency.call` is a `tel:112` link. The simulated-data label shows in the footer during the demo.

## Staff app (`apps/flare-mobile`, Expo Router)

**Routes:**
- `src/app/_layout.tsx`: session gate. No session sends you to `sign-in`. Loads `/me` and holds it in context.
- `src/app/sign-in.tsx`: email and password, errors, a "simulated demo accounts" note.
- `src/app/(tabs)/_layout.tsx`: tabs Active, Resolved, and Needs review only if `roles` includes `DUTY_MANAGER`. The header shows the staff member's name, their Teams, a language toggle and sign-out.
- `src/app/(tabs)/index.tsx`: Active list.
- `src/app/(tabs)/resolved.tsx`: Resolved list (fetched when opened, no polling).
- `src/app/(tabs)/review.tsx`: Quarantined Reports and alerts, with release (optional Team) and dismiss.
- `src/app/issue/[id].tsx`: Issue detail.

**Hooks:**
- `useIssueFeed(view)`: polls every 5 s while the app is active (`AppState`) and stops in the background. It keeps the last good list on error and shows an "offline" banner. It tracks seen IDs, so new Issues get a highlight for 10 s.
- `useMe()`.
- `useIssue(id)`: refetches after every action.
- `useAction()`: sends `version`. On `409`, it refetches and shows a toast: "Ana already took this".

**`IssueCard`:**
- `SeverityBadge` (colour, icon and word)
- the summary, with a small "AI" tag (or "Unclassified" when `unclassified`)
- the Location name and label
- `Mentioned: …` when `mentionedPlace` is set
- "3 people reported · 8 min" from the priority reasons
- the age, and `takenBy`
- badges for Emergency and Reopened
- a one-tap main action: `NEW` gives "I'm on it", `IN_PROGRESS` by me gives "Resolve"

**Detail:**
- the card content
- the action bar: Acknowledge, I'm on it, Resolve, Reopen, Hand over (`TeamPicker` from `/teams`), Edit category or Severity
- the original Reports, each with its time and language, plus contact and flight number when present
- the history

**Signals:** Critical Issues pin to the top with a strong but static colour band. Sound is stretch ([ADR-0013](../adr/0013-tooling-and-dependencies.md)).

**Mock mode:** `EXPO_PUBLIC_MOCK=true` skips Supabase. A role picker on the sign-in screen chooses one of the fixture staff members, and actions mutate the fixtures in memory.

**Env:** `EXPO_PUBLIC_API_BASE_URL`, `EXPO_PUBLIC_SUPABASE_URL`, `EXPO_PUBLIC_SUPABASE_ANON_KEY`, `EXPO_PUBLIC_MOCK`.

### Staff copy (draft; D04 completes it)

Key strings:
- `tabs.active`: Active / Active
- `tabs.resolved`: Resolved / Rezolvate
- `tabs.review`: Needs review / De verificat
- `action.ack`: Acknowledge / Confirmă
- `action.take`: I'm on it / Mă ocup eu
- `action.resolve`: Resolve / Rezolvat
- `action.reopen`: Reopen / Redeschide
- `action.handOver`: Hand over / Predă altei echipe
- `priority.REPORTERS`: {count} people reported · {minutes} min / {count} persoane au raportat · {minutes} min
- `card.ai`: AI summary / Rezumat AI
- `card.unclassified`: Unclassified / Neclasificat
- `card.mentioned`: Mentioned: {place} / Menționat: {place}
- `severity.CRITICAL`: Critical / Critic, and so on
- Category and Team names, the reason codes and the error codes.
