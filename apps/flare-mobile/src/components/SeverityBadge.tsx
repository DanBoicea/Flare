import { StyleSheet, Text, View } from 'react-native';

import type { Severity } from '@/api/types';
import { Icon } from '@/components/Icon';
import { font, radius, severity as severityTokens, size, space } from '@/constants/theme';
import { useI18n } from '@/i18n/context';

/** A pill with colour, icon shape and word together, so severity survives colour blindness and greyscale. */
export function SeverityBadge({ severity }: { severity: Severity }) {
  const { t } = useI18n();
  const tokens = severityTokens[severity];
  return (
    <View style={[styles.badge, { backgroundColor: tokens.badgeBg, borderColor: tokens.badgeBorder }]}>
      <Icon name={tokens.icon} size={16} color={tokens.badgeText} />
      <Text style={[styles.label, { color: tokens.badgeText }]}>{t(`severity.${severity}`)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    gap: 6,
    paddingLeft: space[2] + 2,
    paddingRight: space[3],
    paddingVertical: 2,
    borderRadius: radius.pill,
    borderWidth: 1.5,
  },
  label: { fontSize: size.caption, fontFamily: font.semiBold },
});
