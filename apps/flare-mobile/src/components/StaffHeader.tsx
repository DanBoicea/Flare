import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import type { Me } from '@/api/types';
import { Icon } from '@/components/Icon';
import { color, font, radius, size, space, tap } from '@/constants/theme';
import { useI18n } from '@/i18n/context';

interface Props {
  /** The signed-in staff member; without it (sign-in screen) only the first row shows. */
  me?: Me;
  onSignOut?: () => void;
}

/**
 * The navy Staff header from the V1 mock-ups (07-staff-list.html): logo, "Personal" pill, rounded
 * bottom corners. The language button, identity row and sign-out are ours (B02), drawn in the
 * header's outline style. It makes a staff phone easy to tell from a passenger phone at a glance.
 */
export function StaffHeader({ me, onSignOut }: Props) {
  const { t, language, setLanguage } = useI18n();
  const insets = useSafeAreaInsets();
  const other = language === 'ro' ? 'en' : 'ro';
  const roleWords = me?.roles.map((role) => t(`role.${role}`)).join(', ');

  return (
    <View style={[styles.header, { paddingTop: insets.top + space[3] }]}>
      <View style={styles.row}>
        <View style={styles.brand}>
          <View style={styles.logo}>
            <Icon name="beacon" size={20} color={color.paper} />
          </View>
          <Text style={styles.logoText}>Flare</Text>
          <Text style={styles.staffPill}>{t('app.staff')}</Text>
        </View>
        <Pressable
          style={styles.outlineButton}
          accessibilityRole="button"
          accessibilityLabel={t('language.switchLabel')}
          accessibilityLanguage={other}
          onPress={() => setLanguage(other)}>
          <Icon name="globe" size={18} color={color.paper} />
          <Text style={styles.outlineButtonText}>{t('language.switch')}</Text>
        </Pressable>
      </View>

      {me && (
        <View style={styles.row}>
          <View style={styles.identity}>
            {/* From our back end; shown as plain text. */}
            <Text style={styles.name} accessibilityRole="header">
              {me.displayName}
              {roleWords ? <Text style={styles.role}>, {roleWords}</Text> : null}
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
              style={styles.outlineButton}
              accessibilityRole="button"
              accessibilityLabel={t('header.signOutLabel')}
              onPress={onSignOut}>
              <Icon name="log-out" size={18} color={color.paper} />
              <Text style={styles.outlineButtonText}>{t('header.signOut')}</Text>
            </Pressable>
          )}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    backgroundColor: color.navy,
    borderBottomLeftRadius: radius.header,
    borderBottomRightRadius: radius.header,
    paddingHorizontal: space[5],
    paddingBottom: space[4] + 2,
    gap: space[3],
  },
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: space[3] },
  brand: { flexDirection: 'row', alignItems: 'center', gap: space[3] },
  logo: {
    width: 32,
    height: 32,
    borderRadius: radius.logo,
    backgroundColor: color.cobalt,
    alignItems: 'center',
    justifyContent: 'center',
  },
  logoText: { color: color.paper, fontSize: size.logo, fontFamily: font.bold },
  staffPill: {
    color: color.paper,
    backgroundColor: color.cobalt,
    borderRadius: radius.pill,
    overflow: 'hidden',
    paddingHorizontal: space[4],
    paddingVertical: 4,
    fontSize: size.caption,
    fontFamily: font.semiBold,
  },
  outlineButton: {
    minHeight: tap.min,
    flexDirection: 'row',
    alignItems: 'center',
    gap: space[2],
    paddingHorizontal: space[4],
    borderColor: color.onNavyLine,
    borderWidth: 1.5,
    borderRadius: radius.pill,
  },
  outlineButtonText: { color: color.paper, fontSize: size.caption, fontFamily: font.semiBold },
  identity: { flex: 1, minWidth: 160, gap: space[2] },
  name: { color: color.paper, fontSize: size.body, fontFamily: font.bold },
  role: { color: color.onNavy2, fontFamily: font.regular },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: space[2] },
  chip: {
    color: color.onNavy2,
    fontSize: size.caption,
    fontFamily: font.semiBold,
    borderColor: color.onNavyLine,
    borderWidth: 1,
    borderRadius: radius.pill,
    paddingHorizontal: space[3],
    paddingVertical: 2,
  },
});
