import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';

import { color } from '@/constants/theme';
import { I18nProvider } from '@/i18n/I18nProvider';

// TODO(B02): the session gate. No session goes to sign-in; with one, load /me and hold it in context.
export default function RootLayout() {
  return (
    <I18nProvider>
      <StatusBar style="light" />
      <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: color.paper } }}>
        <Stack.Screen name="sign-in" />
        <Stack.Screen name="(tabs)" />
        <Stack.Screen name="issue/[id]" options={{ headerShown: true, headerTitle: '', headerTintColor: color.ink }} />
      </Stack>
    </I18nProvider>
  );
}
