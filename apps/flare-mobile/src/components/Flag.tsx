import { useId } from 'react';
import { StyleSheet, View } from 'react-native';
import Svg, { ClipPath, Defs, Path, Rect } from 'react-native-svg';

import type { UiLanguage } from '@/api/types';

/**
 * Flag of the country most associated with a UI language: Romania for Romanian, the United Kingdom
 * for English. The same drawing as the web app's Flag.tsx. Decorative: it always sits next to the
 * language code ("RO", "EN"), because a flag is a country, not a language (decisions round 8).
 */
export function Flag({ language }: { language: UiLanguage }) {
  // useId may contain characters that break url(#…) references; keep letters, digits, _ and -.
  const id = `flag${useId().replace(/[^a-zA-Z0-9_-]/g, '')}`;
  return (
    <View style={styles.frame} accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
      {language === 'ro' ? (
        <Svg width={28} height={20} viewBox="0 0 60 40">
          <Rect width="20" height="40" fill="#002B7F" />
          <Rect x="20" width="20" height="40" fill="#FCD116" />
          <Rect x="40" width="20" height="40" fill="#CE1126" />
        </Svg>
      ) : (
        // Union Jack, drawn on a 60x30 grid and stretched to 3:2 so both flags have the same size.
        <Svg width={28} height={20} viewBox="0 0 60 30" preserveAspectRatio="none">
          <Defs>
            <ClipPath id={`${id}-t`}>
              <Path d="M30,15 h30 v15 z v15 h-30 z h-30 v-15 z v-15 h30 z" />
            </ClipPath>
          </Defs>
          <Rect width="60" height="30" fill="#012169" />
          <Path d="M0,0 L60,30 M60,0 L0,30" stroke="#fff" strokeWidth="6" />
          <Path d="M0,0 L60,30 M60,0 L0,30" clipPath={`url(#${id}-t)`} stroke="#C8102E" strokeWidth="4" />
          <Path d="M30,0 v30 M0,15 h60" stroke="#fff" strokeWidth="10" />
          <Path d="M30,0 v30 M0,15 h60" stroke="#C8102E" strokeWidth="6" />
        </Svg>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  // A thin light edge keeps the flag's dark blue visible on the navy header.
  frame: { borderRadius: 3, overflow: 'hidden', borderWidth: 1, borderColor: 'rgba(255, 255, 255, 0.55)' },
});
