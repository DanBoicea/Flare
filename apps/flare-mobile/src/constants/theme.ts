/**
 * Design tokens for the Staff app, copied from docs/design/foundations.md ("Tokens in code").
 * Light theme only (foundations, colour rule 6). Change a value there first, then here and in the
 * web app's tokens.css, so both apps stay the same.
 */

export const color = {
  black: '#000000', ink: '#1A1C1F', ink2: '#4A4F57', ink3: '#646B74',
  paper: '#FFFFFF', paper2: '#F2F4F5', line: '#D5D9DD', signal: '#FFC20E',
  danger: '#C8102E', dangerInk: '#A0102A', dangerSoft: '#FDECEE',
  warn: '#D9480F', warnInk: '#9C3B00', warnSoft: '#FFF0E6',
  info: '#4A86DC', infoInk: '#1A4F91', infoSoft: '#E9F1FB',
  quiet: '#8A9099', quietInk: '#474D55', quietSoft: '#EEF0F2',
  ok: '#1E7B4A', okInk: '#17623B', okSoft: '#E7F4EC',
  headerMuted: '#C4C8CE', live: '#3DD68C',
} as const;

/** Severity is never colour alone: always colour, icon shape and word together. */
export const severity = {
  CRITICAL: { bar: color.danger, badgeBg: color.danger, badgeText: color.paper, rowBg: color.dangerSoft, icon: 'octagon-filled' },
  HIGH: { bar: color.warn, badgeBg: color.warnSoft, badgeText: color.warnInk, rowBg: color.paper, icon: 'triangle' },
  MEDIUM: { bar: color.info, badgeBg: color.infoSoft, badgeText: color.infoInk, rowBg: color.paper, icon: 'circle-bar' },
  LOW: { bar: color.quiet, badgeBg: color.quietSoft, badgeText: color.quietInk, rowBg: color.paper, icon: 'circle-down' },
} as const;

/**
 * Atkinson Hyperlegible Next, once B06 adds the font files (ADR-0020): then these become
 * 'AtkinsonHyperlegibleNext-Regular', '-Bold' and '-ExtraBold'. Until then undefined means the
 * system font, as in the web app. Weights still come from fontWeight.
 */
export const font: { regular?: string; bold?: string; extraBold?: string } = {};

export const size = { caption: 16, body: 17, lead: 18, rowTitle: 20, detailTitle: 24, title: 30 } as const;
export const space = { 1: 4, 2: 8, 3: 12, 4: 16, 5: 20, 6: 24, 8: 32 } as const;
export const radius = { sign: 3, control: 8, card: 12, sheet: 16 } as const;
export const tap = { min: 48, main: 56 } as const;
