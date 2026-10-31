import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import type { Me } from '@/api/types';
import { Icon } from '@/components/Icon';
import { color, radius, size, space, tap } from '@/constants/theme';
import { useI18n } from '@/i18n/context';

interface Props {
  /** The signed-in staff member; without it (sign-in screen) only the first row shows. */
  me?: Me;
  onSignOut?: () => void;
}

/**
 * The black Staff header (docs/design/components.md, Staff app "Header"). It makes a staff phone
 * easy to tell from a passenger phone at a glance.
 */
export function StaffHeader({ me, onSignOut }: Props) {
  const { t, language, setLanguage } = useI18n();
  const insets = useSafeAreaInsets();
  const other = language === 'ro' ? 'en' : 'ro';
  const roleWords = me?.roles.map((role) => t(`role.${role}`)).join(', ');

  return (
    <View style={[styles.header, { paddingTop: insets.top + space[2] }]}>
      <View style={styles.row}>
        <View style={styles.plate}>
          <Text style={styles.plateText}>Flare</Text>
        </View>
        <Text style={styles.muted}>{t('app.staff')}</Text>
        <View style={styles.spacer} />
        <Pressable
          style={styles.headerButton}
          accessibilityRole="button"
          accessibilityLabel={t('language.switchLabel')}
          accessibilityLanguage={other}
          onPress={() => setLanguage(other)}>
          <Icon name="globe" size={20} color={color.paper} />
          <Text style={styles.headerButtonText}>{t('language.switch')}</Text>
        </Pressable>
      </View>

      {me && (
        <View style={styles.row}>
          <View style={styles.identity}>
            {/* From our back end; shown as plain text. */}
            <Text style={styles.name} accessibilityRole="header">
              {me.displayName}
              {roleWords ? <Text style={styles.muted}>, {roleWords}</Text> : null}
            </Text>
            {me.teams.length > 0 && (
              <View style={styles.chips}>
                {me.teams.map((team) => (
                  <Text key={team} style={styles.chip}>{t(`team.${team}`)}</Text>
                ))}
              </View>
            )}
          </View>
          {onSignOut && (
            <Pressable
              style={styles.headerButton}
              accessibilityRole="button"
              accessibilityLabel={t('header.signOutLabel')}
              onPress={onSignOut}>
              <Icon name="log-out" size={20} color={color.paper} />
              <Text style={styles.headerButtonText}>{t('header.signOut')}</Text>
            </Pressable>
          )}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  header: { backgroundColor: color.black, paddingHorizontal: space[5], paddingBottom: space[3], gap: space[3] },
  row: { flexDirection: 'row', alignItems: 'center', gap: space[3] },
  spacer: { flex: 1 },
  plate: { borderColor: color.signal, borderWidth: 2, borderRadius: radius.sign, paddingHorizontal: space[2], paddingVertical: 2 },
  plateText: { color: color.signal, fontSize: size.lead, fontWeight: '800' },
  muted: { color: color.headerMuted, fontSize: size.body, fontWeight: '400' },
  headerButton: {
    minHeight: tap.min,
    flexDirection: 'row',
    alignItems: 'center',
    gap: space[2],
    paddingHorizontal: space[3],
    borderColor: color.quiet,
    borderWidth: 1.5,
    borderRadius: radius.control,
  },
  headerButtonText: { color: color.paper, fontSize: size.caption, fontWeight: '700' },
  identity: { flex: 1, gap: space[2] },
  name: { color: color.paper, fontSize: size.body, fontWeight: '700' },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: space[2] },
  chip: {
    color: color.paper,
    fontSize: size.caption,
    borderColor: color.headerMuted,
    borderWidth: 1,
    borderRadius: radius.sign,
    paddingHorizontal: space[2],
    paddingVertical: 2,
  },
});
