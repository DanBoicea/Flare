import { Tabs } from 'expo-router/tabs';

import { Icon } from '@/components/Icon';
import { color, size } from '@/constants/theme';
import { useI18n } from '@/i18n/context';

// TODO(B02): show Needs review only when /me roles include DUTY_MANAGER, and add the black header
// with the name, Teams, language button and sign-out.
export default function TabsLayout() {
  const { t } = useI18n();
  return (
    <Tabs
      screenOptions={{
        headerShown: false,
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
        options={{ title: t('tabs.review'), tabBarIcon: ({ color: tint }) => <Icon name="inbox" color={String(tint)} size={24} /> }}
      />
    </Tabs>
  );
}
