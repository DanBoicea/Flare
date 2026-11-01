import type { ReactNode } from 'react';
import Svg, { Circle, Path, Rect } from 'react-native-svg';

import { color as palette } from '@/constants/theme';

/**
 * Icons on a 24 grid: the Design V1 shapes (docs/design/flare-design-v1/), Feather/Lucide-style
 * strokes (MIT/ISC) for the spec's icons (docs/design/foundations.md#icons), and our own simple
 * pictograms for places (toilets, baggage belts, lifts) so staff recognise a place at a glance.
 * Icons always sit next to a word, so they are hidden from screen readers.
 */
export type IconName =
  // severity
  | 'octagon-filled' | 'triangle-filled' | 'circle-bar' | 'circle-down'
  // navigation and status
  | 'list' | 'check-circle' | 'inbox' | 'globe' | 'log-out' | 'chevron-left' | 'chevron-right'
  | 'users' | 'user-check'
  // places, categories and Teams
  | 'toilet' | 'luggage' | 'lift' | 'escalator' | 'info' | 'seat' | 'door' | 'shield' | 'family'
  | 'droplet' | 'wrench' | 'accessibility' | 'search' | 'map-pin';

interface Props {
  name: IconName;
  size?: number;
  color?: string;
}

interface Stroke {
  stroke: string;
  strokeWidth: number;
  strokeLinecap: 'round';
  strokeLinejoin: 'round';
  fill: 'none';
}

const shapes: Record<IconName, (s: Stroke, color: string) => ReactNode> = {
  'octagon-filled': (_, c) => <Path d="M8 2h8l6 6v8l-6 6H8l-6-6V8z" fill={c} />,
  'triangle-filled': (_, c) => <Path d="M12 3l10 18H2z" fill={c} />,
  'circle-bar': (s) => (
    <>
      <Circle cx="12" cy="12" r="9.5" {...s} strokeWidth={2.4} />
      <Path d="M7.5 12h9" {...s} strokeWidth={2.4} />
    </>
  ),
  'circle-down': (s) => (
    <>
      <Circle cx="12" cy="12" r="9.5" {...s} strokeWidth={2.4} />
      <Path d="M12 7.5v8M8.5 12.5 12 16l3.5-3.5" {...s} strokeWidth={2.4} />
    </>
  ),

  list: (s) => <Path d="M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01" {...s} />,
  'check-circle': (s) => (
    <>
      <Circle cx="12" cy="12" r="9" {...s} />
      <Path d="M8 12.5l3 3 5-6" {...s} />
    </>
  ),
  inbox: (s) => (
    <>
      <Path d="M22 12h-6l-2 3h-4l-2-3H2" {...s} />
      <Path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z" {...s} />
    </>
  ),
  globe: (s) => (
    <>
      <Circle cx="12" cy="12" r="10" {...s} />
      <Path d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" {...s} />
    </>
  ),
  'log-out': (s) => <Path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9" {...s} />,
  'chevron-left': (s) => <Path d="M15 5l-7 7 7 7" {...s} strokeWidth={2.4} />,
  'chevron-right': (s) => <Path d="M9 18l6-6-6-6" {...s} strokeWidth={2.4} />,
  users: (s) => (
    <>
      <Path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" {...s} />
      <Circle cx="9" cy="7" r="4" {...s} />
    </>
  ),
  'user-check': (s) => (
    <>
      <Path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2M17 11l2 2 4-4" {...s} />
      <Circle cx="8.5" cy="7" r="4" {...s} />
    </>
  ),

  // Toilets: the man and woman pictogram with a divider.
  toilet: (s) => (
    <>
      <Circle cx="6" cy="4.5" r="1.75" {...s} />
      <Path d="M4.5 21v-6h-1v-4.5A1.5 1.5 0 0 1 5 9h2a1.5 1.5 0 0 1 1.5 1.5V15h-1v6M12 3v18" {...s} />
      <Circle cx="18" cy="4.5" r="1.75" {...s} />
      <Path d="M16.75 9h2.5l2.75 7.5h-8zM16.5 16.5V21M19.5 16.5V21" {...s} />
    </>
  ),
  luggage: (s) => (
    <>
      <Rect x="6" y="7" width="12" height="12" rx="2" {...s} />
      <Path d="M9.5 7V4.5A1.5 1.5 0 0 1 11 3h2a1.5 1.5 0 0 1 1.5 1.5V7M10 10.5v5M14 10.5v5M9 19v2M15 19v2" {...s} />
    </>
  ),
  // A lift: doors frame with up and down arrows.
  lift: (s) => (
    <>
      <Rect x="4" y="3" width="16" height="18" rx="2" {...s} />
      <Path d="M9 10l3-3 3 3M9 14l3 3 3-3" {...s} />
    </>
  ),
  escalator: (s) => <Path d="M4 20h3.5L16 9h3a2 2 0 0 0 0-4h-4L6.5 16H4a2 2 0 0 0 0 4z" {...s} />,
  info: (s) => (
    <>
      <Circle cx="12" cy="12" r="10" {...s} />
      <Path d="M12 16v-4M12 8h.01" {...s} />
    </>
  ),
  seat: (s) => (
    <Path
      d="M19 9V6a2 2 0 0 0-2-2H7a2 2 0 0 0-2 2v3M3 16a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-5a2 2 0 0 0-4 0v1.5a.5.5 0 0 1-.5.5h-9a.5.5 0 0 1-.5-.5V11a2 2 0 0 0-4 0zM5 18v2M19 18v2"
      {...s}
    />
  ),
  door: (s) => <Path d="M3 21h18M6 21V4a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v17M14.5 12h.01" {...s} />,
  shield: (s) => <Path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" {...s} />,
  // Family room: an adult and a child.
  family: (s) => (
    <>
      <Circle cx="8" cy="5" r="2" {...s} />
      <Path d="M5 21v-8a3 3 0 0 1 6 0v8M14.75 21v-4.5a2.25 2.25 0 0 1 4.5 0V21" {...s} />
      <Circle cx="17" cy="10" r="1.75" {...s} />
    </>
  ),
  droplet: (s) => <Path d="M12 2.69l5.66 5.66a8 8 0 1 1-11.31 0z" {...s} />,
  wrench: (s) => (
    <Path
      d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"
      {...s}
    />
  ),
  // The wheelchair pictogram: passenger assistance.
  accessibility: (s) => (
    <>
      <Circle cx="11" cy="4" r="1.75" {...s} />
      <Path d="M11 8v5h5l2.5 5.5M11 10.5h4M7.5 11.67A5 5 0 1 0 14.83 17.29" {...s} />
    </>
  ),
  search: (s) => (
    <>
      <Circle cx="11" cy="11" r="8" {...s} />
      <Path d="M21 21l-4.35-4.35" {...s} />
    </>
  ),
  'map-pin': (s) => (
    <>
      <Path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0z" {...s} />
      <Circle cx="12" cy="10" r="3" {...s} />
    </>
  ),
};

export function Icon({ name, size = 20, color = palette.ink }: Props) {
  const stroke: Stroke = { stroke: color, strokeWidth: 2.2, strokeLinecap: 'round', strokeLinejoin: 'round', fill: 'none' };
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
      {shapes[name](stroke, color)}
    </Svg>
  );
}
