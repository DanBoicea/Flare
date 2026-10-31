import { color, font, radius, severity, size, space, tap } from '@/constants/theme';

const theme = { color, severity, font, size, space, radius, tap } as const;

/** The design tokens. Light theme only, so this never changes; a hook keeps a night theme (stretch) possible. */
export function useTheme() {
  return theme;
}
