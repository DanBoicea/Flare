import { Image } from 'expo-image';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import type { Me } from '@/api/types';
import { Flag } from '@/components/Flag';
import { Icon } from '@/components/Icon';
import { color, font, radius, size, space, tap } from '@/constants/theme';
import { useI18n } from '@/i18n/context';
import { teamIcons } from '@/lib/issueIcon';

interface Props {
  /** The signed-in staff member; without it (sign-in screen) only the first row shows. */
  me?: Me;
  onSignOut?: () => void;
}

/**
 * The navy Staff header from the V1 mock-ups (07-staff-list.html): logo, "Personal" pill, rounded
 * bottom corners. The language button (the other language's flag and code, like the web app), the
 * identity row with Team chips and sign-out are ours (B02), drawn in the header's outline style.
 * It makes a staff phone easy to tell from a passenger phone at a glance.
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
          {/* The logo is navy on white, so it sits on its own white tile. Decorative: "Flare" is next to it. */}
          <Image
            source={require('../../assets/images/flare-mark.png')}
            style={styles.logo}
            contentFit="cover"
            accessible={false}
          />
          <Text style={styles.logoText}>Flare</Text>
          <Text style={styles.staffPill}>{t('app.staff')}</Text>
        </View>
        <Pressable
          style={({ pressed }) => [styles.outlineButton, pressed && styles.outlinePressed]}
          accessibilityRole="button"
          accessibilityLabel={t('language.switchLabel')}
          accessibilityLanguage={other}
          hitSlop={4}
          onPress={() => setLanguage(other)}>
          <Flag language={other} />
          <Text style={styles.outlineButtonText}>{t('language.code')}</Text>
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
                  <View key={team} style={styles.chip}>
                    <Icon name={teamIcons[team]} size={16} color={color.onNavy2} />
                    <Text style={styles.chipText}>{t(`team.${team}`)}</Text>
                  </View>
                ))}
              </View>
            )}
          </View>
          {onSignOut && (
            <Pressable
              style={({ pressed }) => [styles.outlineButton, pressed && styles.outlinePressed]}
              accessibilityRole="button"
              accessibilityLabel={t('header.signOutLabel')}
              hitSlop={4}
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
    gap: space[4],
  },
  row: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: space[3] },
  brand: { flexDirection: 'row', alignItems: 'center', gap: space[3] },
  logo: { width: 40, height: 40, borderRadius: radius.logo, backgroundColor: color.paper },
  logoText: { color: color.paper, fontSize: size.logo, fontFamily: font.bold },
  staffPill: {
    color: color.paper,
    backgroundColor: color.cobalt,
    borderRadius: radius.pill,
    overflow: 'hidden',
    paddingHorizontal: space[3],
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
  outlinePressed: { backgroundColor: 'rgba(255, 255, 255, 0.12)' },
  outlineButtonText: { color: color.paper, fontSize: size.caption, fontFamily: font.semiBold },
  identity: { flex: 1, minWidth: 160, gap: space[2] },
  name: { color: color.paper, fontSize: size.body, fontFamily: font.bold },
  role: { color: color.onNavy2, fontFamily: font.regular },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: space[2] },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    borderColor: color.onNavyLine,
    borderWidth: 1,
    borderRadius: radius.pill,
    paddingHorizontal: space[3],
    paddingVertical: 3,
  },
  chipText: { color: color.onNavy2, fontSize: size.caption, fontFamily: font.semiBold },
});
