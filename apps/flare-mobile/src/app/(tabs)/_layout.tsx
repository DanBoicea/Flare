import { Tabs } from 'expo-router/tabs';

import { useMe, useSession } from '@/auth/session';
import { Icon } from '@/components/Icon';
import { StaffHeader } from '@/components/StaffHeader';
import { color, size } from '@/constants/theme';
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
        tabBarActiveTintColor: color.black,
        tabBarInactiveTintColor: color.ink2,
        tabBarLabelStyle: { fontSize: size.caption, fontWeight: '700' },
        tabBarAllowFontScaling: true,
      }}>
      <Tabs.Screen
        name="index"
        options={{ title: t('tabs.active'), tabBarIcon: ({ color: tint }) => <Icon name="list" color={String(tint)} size={24} /> }}
      />
      <Tabs.Screen
        name="resolved"
        options={{ title: t('tabs.resolved'), tabBarIcon: ({ color: tint }) => <Icon name="check-circle" color={String(tint)} size={24} /> }}
      />
      <Tabs.Screen
        name="review"
        options={{
          title: t('tabs.review'),
          // href: null removes the tab and its route for everyone else.
          href: isDutyManager ? undefined : null,
          tabBarIcon: ({ color: tint }) => <Icon name="inbox" color={String(tint)} size={24} />,
        }}
      />
    </Tabs>
  );
}
