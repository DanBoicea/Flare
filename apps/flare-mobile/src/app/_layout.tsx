import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { SessionProvider, useSession } from '@/auth/session';
import { color, radius, size, space, tap } from '@/constants/theme';
import { useI18n } from '@/i18n/context';
import { I18nProvider } from '@/i18n/I18nProvider';

// Keep the splash screen up while the saved session and /me load, so sign-in never flashes.
SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  return (
    <I18nProvider>
      <SessionProvider>
        <StatusBar style="light" />
        <Gate />
      </SessionProvider>
    </I18nProvider>
  );
}

/** The session gate: signed-in staff get the tabs and Issue detail; everyone else gets sign-in. */
function Gate() {
  const { status } = useSession();
  const signedIn = status === 'signedIn';

  useEffect(() => {
    if (status !== 'loading') SplashScreen.hideAsync();
  }, [status]);

  return (
    <>
      <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: color.paper } }}>
        <Stack.Protected guard={signedIn}>
          <Stack.Screen name="(tabs)" />
          <Stack.Screen name="issue/[id]" options={{ headerShown: true, headerTitle: '', headerTintColor: color.ink }} />
        </Stack.Protected>
        <Stack.Protected guard={!signedIn}>
          <Stack.Screen name="sign-in" />
        </Stack.Protected>
      </Stack>
      {status === 'offline' && <OfflineOverlay />}
    </>
  );
}

/** A saved session exists but our back end can't be reached: keep the session and offer a retry. */
function OfflineOverlay() {
  const { t } = useI18n();
  const { retry, signOut } = useSession();
  return (
    <View style={styles.overlay}>
      <Text style={styles.message} accessibilityRole="alert">{t('session.offline')}</Text>
      <Pressable style={styles.primary} accessibilityRole="button" onPress={retry}>
        <Text style={styles.primaryText}>{t('session.retry')}</Text>
      </Pressable>
      <Pressable style={styles.secondary} accessibilityRole="button" onPress={signOut}>
        <Text style={styles.secondaryText}>{t('header.signOut')}</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    ...StyleSheet.absoluteFill,
    backgroundColor: color.paper,
    padding: space[5],
    justifyContent: 'center',
    gap: space[3],
  },
  message: { fontSize: size.lead, color: color.ink, textAlign: 'center', marginBottom: space[3] },
  primary: {
    minHeight: tap.main,
    backgroundColor: color.signal,
    borderColor: color.black,
    borderWidth: 2,
    borderRadius: radius.control,
    alignItems: 'center',
    justifyContent: 'center',
  },
  primaryText: { fontSize: size.lead, fontWeight: '800', color: color.black },
  secondary: {
    minHeight: tap.main,
    borderColor: color.black,
    borderWidth: 2,
    borderRadius: radius.control,
    alignItems: 'center',
    justifyContent: 'center',
  },
  secondaryText: { fontSize: size.lead, fontWeight: '700', color: color.ink },
});
