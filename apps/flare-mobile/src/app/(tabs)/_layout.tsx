import { Tabs } from 'expo-router/tabs';
import { StyleSheet, View } from 'react-native';

import { useMe, useSession } from '@/auth/session';
import { Icon, type IconName } from '@/components/Icon';
import { StaffHeader } from '@/components/StaffHeader';
import { color, font, radius, size } from '@/constants/theme';
import { useI18n } from '@/i18n/context';

/** Active and Resolved for everyone; Needs review only for duty managers (ADR-0008). The server checks it too. */
export default function TabsLayout() {
  const { t } = useI18n();
  const me = useMe();
  const { signOut } = useSession();
  const isDutyManager = me.roles.includes('DUTY_MANAGER');

  return (
    <Tabs
      screenOptions={{
        header: () => <StaffHeader me={me} onSignOut={signOut} />,
        sceneStyle: { backgroundColor: color.page },
        tabBarStyle: styles.bar,
        tabBarActiveTintColor: color.cobalt,
        tabBarInactiveTintColor: color.ink2,
        tabBarLabelStyle: styles.label,
        tabBarAllowFontScaling: true,
      }}>
      <Tabs.Screen name="index" options={{ title: t('tabs.active'), tabBarIcon: tabIcon('list') }} />
      <Tabs.Screen name="resolved" options={{ title: t('tabs.resolved'), tabBarIcon: tabIcon('check-circle') }} />
      <Tabs.Screen
        name="review"
        options={{
          title: t('tabs.review'),
          // href: null removes the tab and its route for everyone else.
          href: isDutyManager ? undefined : null,
          tabBarIcon: tabIcon('clock'),
        }}
      />
    </Tabs>
  );
}

/** The V1 tab icon: the selected tab's icon sits in a tint pill. */
function tabIcon(name: IconName) {
  function TabIcon({ focused, color: tint }: { focused: boolean; color: unknown }) {
    return (
      <View style={[styles.iconPill, focused && styles.iconPillFocused]}>
        <Icon name={name} size={22} color={String(tint)} />
      </View>
    );
  }
  return TabIcon;
}

const styles = StyleSheet.create({
  bar: { backgroundColor: color.paper, borderTopColor: color.line, borderTopWidth: 1 },
  label: { fontSize: size.caption, fontFamily: font.semiBold },
  iconPill: { width: 56, height: 30, borderRadius: radius.pill, alignItems: 'center', justifyContent: 'center' },
  iconPillFocused: { backgroundColor: color.tint },
});
