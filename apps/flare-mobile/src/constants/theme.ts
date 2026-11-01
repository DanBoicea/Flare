/**
 * Design V1 tokens for the Staff app (ADR-0021; docs/design/foundations.md, "Design V1 tokens").
 * The source of truth is docs/design/flare-design-v1/. Light theme only. Change a value in
 * foundations.md first, then here and in the web app's tokens.css, so both apps stay the same.
 */

export const color = {
  navy: '#0B2545', // header background, text
  cobalt: '#2457E6', // the one main action per screen (6.1:1 on white)
  cobaltPress: '#1C46BD',
  page: '#EAF0FB', // screen background
  tint: '#DCE6FA', // notices, tags, the selected tab
  paper: '#FFFFFF', // cards, fields, the tab bar
  ink: '#0B2545', // text (= navy)
  ink2: '#44587A', // muted text (6.3:1 on page)
  line: '#CBD8EF', // hairlines
  onNavy2: '#BCD0F5', // secondary text on the navy header
  onNavyLine: '#8FB2FF', // outlines and the live dot on the navy header
  danger: '#C8102E', // only Critical (and 112 / Emergency in the web app)
  dangerSoft: '#FDECEE',
  mediumInk: '#1B3E9E', // Medium severity text on tint, from the staff mock-up
} as const;

/**
 * Severity is never colour alone: every level has its own colour, icon shape and word.
 * Critical, High and Medium are taken from the staff mock-up (07-staff-list.html); Low completes the
 * set inside the V1 palette (decided 2026-10-31).
 */
export const severity = {
  CRITICAL: { badgeBg: color.danger, badgeText: color.paper, badgeBorder: color.danger, cardBg: color.dangerSoft, icon: 'octagon-filled' },
  HIGH: { badgeBg: color.navy, badgeText: color.paper, badgeBorder: color.navy, cardBg: color.paper, icon: 'triangle-filled' },
  MEDIUM: { badgeBg: color.tint, badgeText: color.mediumInk, badgeBorder: color.tint, cardBg: color.paper, icon: 'circle-bar' },
  LOW: { badgeBg: color.paper, badgeText: color.ink2, badgeBorder: color.ink2, cardBg: color.paper, icon: 'circle-down' },
} as const;

/**
 * Instrument Sans, self-hosted as static files in assets/fonts (loaded in src/app/_layout.tsx).
 * React Native picks a weight by font file, not fontWeight, so styles set fontFamily only.
 */
export const font = {
  regular: 'InstrumentSans-Regular', // 400: text
  semiBold: 'InstrumentSans-SemiBold', // 600: buttons, tags, labels
  bold: 'InstrumentSans-Bold', // 700: titles, names, the logo
} as const;

/** Nothing below 16. */
export const size = { caption: 16, body: 17, lead: 18, rowTitle: 20, title: 28, logo: 26 } as const;
export const space = { 1: 4, 2: 8, 3: 12, 4: 16, 5: 20, 6: 24, 8: 32 } as const;
/** logo: the white logo tile in the header; tile: the place icon tile on Issue cards. */
export const radius = { code: 8, logo: 12, tile: 14, control: 16, card: 20, header: 28, pill: 999 } as const;
/** Staff main buttons are 48 tall; header buttons (language, back, sign-out) 44. */
export const tap = { min: 44, main: 48 } as const;
