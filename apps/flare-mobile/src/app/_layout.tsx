import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { SessionProvider, useSession } from '@/auth/session';
import { ToastProvider } from '@/components/Toast';
import { color, font, radius, size, space, tap } from '@/constants/theme';
import { useI18n } from '@/i18n/context';
import { I18nProvider } from '@/i18n/I18nProvider';

// Keep the splash screen up while the fonts, the saved session and /me load, so nothing flashes.
SplashScreen.preventAutoHideAsync();

export default function RootLayout() {
  // Instrument Sans, self-hosted (ADR-0021). If loading fails, text falls back to the system font.
  const [fontsLoaded, fontError] = useFonts({
    [font.regular]: require('../../assets/fonts/InstrumentSans-Regular.ttf'),
    [font.semiBold]: require('../../assets/fonts/InstrumentSans-SemiBold.ttf'),
    [font.bold]: require('../../assets/fonts/InstrumentSans-Bold.ttf'),
  });

  return (
    <I18nProvider>
      <SessionProvider>
        <ToastProvider>
          <StatusBar style="light" />
          <Gate fontsReady={fontsLoaded || fontError !== null} />
        </ToastProvider>
      </SessionProvider>
    </I18nProvider>
  );
}

/** The session gate: signed-in staff get the tabs and Issue detail; everyone else gets sign-in. */
function Gate({ fontsReady }: { fontsReady: boolean }) {
  const { status } = useSession();
  const signedIn = status === 'signedIn';

  useEffect(() => {
    if (fontsReady && status !== 'loading') SplashScreen.hideAsync();
  }, [fontsReady, status]);

  return (
    <>
      <Stack screenOptions={{ headerShown: false, contentStyle: { backgroundColor: color.page } }}>
        <Stack.Protected guard={signedIn}>
          <Stack.Screen name="(tabs)" />
          <Stack.Screen
            name="issue/[id]"
            options={{
              headerShown: true,
              headerTitle: '',
              headerStyle: { backgroundColor: color.navy },
              headerTintColor: color.paper,
              headerShadowVisible: false,
            }}
          />
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
      <Pressable
        style={({ pressed }) => [styles.primary, pressed && styles.primaryPressed]}
        accessibilityRole="button"
        onPress={retry}>
        <Text style={styles.primaryText}>{t('session.retry')}</Text>
      </Pressable>
      <Pressable style={styles.secondary} accessibilityRole="button" onPress={signOut}>
        <Text style={styles.secondaryText}>{t('header.signOut')}</Text>
      </Pressable>
    </View>
  );
}

const button = { minHeight: tap.main, borderRadius: radius.control, alignItems: 'center', justifyContent: 'center' } as const;

const styles = StyleSheet.create({
  overlay: {
    ...StyleSheet.absoluteFill,
    backgroundColor: color.page,
    padding: space[5],
    justifyContent: 'center',
    gap: space[3],
  },
  message: { fontSize: size.lead, fontFamily: font.regular, color: color.ink, textAlign: 'center', marginBottom: space[3] },
  primary: { ...button, backgroundColor: color.cobalt },
  primaryPressed: { backgroundColor: color.cobaltPress },
  primaryText: { fontSize: size.lead, fontFamily: font.semiBold, color: color.paper },
  secondary: { ...button, backgroundColor: color.paper, borderColor: color.navy, borderWidth: 2 },
  secondaryText: { fontSize: size.lead, fontFamily: font.semiBold, color: color.ink },
});
