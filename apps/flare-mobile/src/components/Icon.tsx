import Svg, { Circle, Line, Path, Polyline } from 'react-native-svg';

import { color as palette } from '@/constants/theme';

/**
 * The icon shapes from the Design V1 mock-ups (docs/design/flare-design-v1/), on a 24 grid.
 * Icons always sit next to a word, so they are hidden from screen readers.
 */
export type IconName =
  | 'octagon-filled' | 'triangle-filled' | 'circle-bar' | 'circle-down'
  | 'list' | 'check-circle' | 'clock'
  | 'globe' | 'log-out' | 'chevron-left' | 'beacon';

interface Props {
  name: IconName;
  size?: number;
  color?: string;
}

export function Icon({ name, size = 20, color = palette.ink }: Props) {
  const stroke = { stroke: color, strokeWidth: 2.2, strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const, fill: 'none' };
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
      {name === 'octagon-filled' && <Path d="M8 2h8l6 6v8l-6 6H8l-6-6V8z" fill={color} />}
      {name === 'triangle-filled' && <Path d="M12 3l10 18H2z" fill={color} />}
      {name === 'circle-bar' && (
        <>
          <Circle cx="12" cy="12" r="9.5" {...stroke} strokeWidth={2.4} />
          <Path d="M7.5 12h9" {...stroke} strokeWidth={2.4} />
        </>
      )}
      {name === 'circle-down' && (
        <>
          <Circle cx="12" cy="12" r="9.5" {...stroke} strokeWidth={2.4} />
          <Path d="M12 7.5v8M8.5 12.5 12 16l3.5-3.5" {...stroke} strokeWidth={2.4} />
        </>
      )}
      {name === 'list' && <Path d="M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01" {...stroke} />}
      {name === 'check-circle' && (
        <>
          <Circle cx="12" cy="12" r="9" {...stroke} />
          <Path d="M8 12.5l3 3 5-6" {...stroke} />
        </>
      )}
      {name === 'clock' && (
        <>
          <Circle cx="12" cy="12" r="9" {...stroke} />
          <Path d="M12 7v5l3 2" {...stroke} />
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
      {name === 'chevron-left' && <Path d="M15 5l-7 7 7 7" {...stroke} strokeWidth={2.4} />}
      {name === 'beacon' && (
        <>
          <Circle cx="12" cy="12" r="3.5" fill={color} />
          <Path d="M12 2.5v3M12 18.5v3M2.5 12h3M18.5 12h3" {...stroke} strokeWidth={2.4} />
        </>
      )}
    </Svg>
  );
}
