import Svg, { Circle, Line, Path, Polygon, Polyline } from 'react-native-svg';

import { color as palette } from '@/constants/theme';

/**
 * The Feather/Lucide icon shapes from docs/design/foundations.md ("Icons"): 24 grid, 2 stroke, round caps.
 * Icons always sit next to a word, so they are hidden from screen readers.
 */
export type IconName =
  | 'octagon-filled' | 'triangle' | 'circle-bar' | 'circle-down'
  | 'list' | 'check-circle' | 'inbox' | 'globe' | 'log-out';

interface Props {
  name: IconName;
  size?: number;
  color?: string;
}

export function Icon({ name, size = 20, color = palette.ink }: Props) {
  const stroke = { stroke: color, strokeWidth: 2, strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const, fill: 'none' };
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
      {name === 'octagon-filled' && (
        <>
          <Polygon points="7.86 2 16.14 2 22 7.86 22 16.14 16.14 22 7.86 22 2 16.14 2 7.86" fill={color} stroke={color} strokeWidth={2} strokeLinejoin="round" />
          <Line x1="12" y1="7" x2="12" y2="13" stroke={palette.paper} strokeWidth={2.5} strokeLinecap="round" />
          <Line x1="12" y1="17" x2="12.01" y2="17" stroke={palette.paper} strokeWidth={2.5} strokeLinecap="round" />
        </>
      )}
      {name === 'triangle' && (
        <>
          <Path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z" {...stroke} />
          <Line x1="12" y1="9" x2="12" y2="13" {...stroke} />
          <Line x1="12" y1="17" x2="12.01" y2="17" {...stroke} />
        </>
      )}
      {name === 'circle-bar' && (
        <>
          <Circle cx="12" cy="12" r="10" {...stroke} />
          <Line x1="8" y1="12" x2="16" y2="12" {...stroke} />
        </>
      )}
      {name === 'circle-down' && (
        <>
          <Circle cx="12" cy="12" r="10" {...stroke} />
          <Polyline points="8 12 12 16 16 12" {...stroke} />
          <Line x1="12" y1="8" x2="12" y2="16" {...stroke} />
        </>
      )}
      {name === 'list' && (
        <>
          <Line x1="8" y1="6" x2="21" y2="6" {...stroke} />
          <Line x1="8" y1="12" x2="21" y2="12" {...stroke} />
          <Line x1="8" y1="18" x2="21" y2="18" {...stroke} />
          <Line x1="3" y1="6" x2="3.01" y2="6" {...stroke} />
          <Line x1="3" y1="12" x2="3.01" y2="12" {...stroke} />
          <Line x1="3" y1="18" x2="3.01" y2="18" {...stroke} />
        </>
      )}
      {name === 'check-circle' && (
        <>
          <Path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" {...stroke} />
          <Polyline points="22 4 12 14.01 9 11.01" {...stroke} />
        </>
      )}
      {name === 'globe' && (
        <>
          <Circle cx="12" cy="12" r="10" {...stroke} />
          <Line x1="2" y1="12" x2="22" y2="12" {...stroke} />
          <Path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" {...stroke} />
        </>
      )}
      {name === 'log-out' && (
        <>
          <Path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" {...stroke} />
          <Polyline points="16 17 21 12 16 7" {...stroke} />
          <Line x1="21" y1="12" x2="9" y2="12" {...stroke} />
        </>
      )}
      {name === 'inbox' && (
        <>
          <Polyline points="22 12 16 12 14 15 10 15 8 12 2 12" {...stroke} />
          <Path d="M5.45 5.11 2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z" {...stroke} />
        </>
      )}
    </Svg>
  );
}
