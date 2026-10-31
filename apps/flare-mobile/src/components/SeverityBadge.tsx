import { StyleSheet, Text, View } from 'react-native';

import type { Severity } from '@/api/types';
import { Icon } from '@/components/Icon';
import { radius, severity as severityTokens, size, space } from '@/constants/theme';
import { useI18n } from '@/i18n/context';

/** Colour, icon shape and word together, so severity survives colour blindness and greyscale. */
export function SeverityBadge({ severity }: { severity: Severity }) {
  const { t } = useI18n();
  const tokens = severityTokens[severity];
  return (
    <View style={[styles.badge, { backgroundColor: tokens.badgeBg }]}>
      <Icon name={tokens.icon} size={18} color={tokens.badgeText} />
      <Text style={[styles.label, { color: tokens.badgeText }]}>{t(`severity.${severity}`)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    gap: space[1],
    paddingHorizontal: space[2],
    paddingVertical: 2,
    borderRadius: radius.sign,
  },
  label: { fontSize: size.caption, fontWeight: '700' },
});
