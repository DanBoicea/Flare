import { router } from 'expo-router';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { USE_MOCK } from '@/api/client';
import { color, radius, size, space, tap } from '@/constants/theme';
import { useI18n } from '@/i18n/context';

/** Placeholder until B02 adds email and password sign-in. In mock mode it opens the app directly. */
export default function SignInScreen() {
  const { t } = useI18n();
  return (
    <SafeAreaView style={styles.screen}>
      <View style={styles.header}>
        <View style={styles.plate}>
          <Text style={styles.plateText}>Flare</Text>
        </View>
        <Text style={styles.headerLabel}>{t('app.staff')}</Text>
      </View>
      <View style={styles.body}>
        <Text style={styles.title} accessibilityRole="header">{t('signIn.title')}</Text>
        <Text style={styles.text}>{t('signIn.placeholder')}</Text>
        {USE_MOCK && (
          <Pressable style={styles.button} accessibilityRole="button" onPress={() => router.replace('/')}>
            <Text style={styles.buttonText}>{t('signIn.openMock')}</Text>
          </Pressable>
        )}
        <Text style={styles.note}>{t('app.demoNote')}</Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: color.black },
  header: { flexDirection: 'row', alignItems: 'center', gap: space[3], paddingHorizontal: space[5], paddingVertical: space[3] },
  plate: { backgroundColor: color.black, borderColor: color.signal, borderWidth: 2, borderRadius: radius.sign, paddingHorizontal: space[2], paddingVertical: 2 },
  plateText: { color: color.signal, fontSize: size.lead, fontWeight: '800' },
  headerLabel: { color: color.headerMuted, fontSize: size.body },
  body: { flex: 1, backgroundColor: color.paper, padding: space[5], gap: space[4] },
  title: { fontSize: size.title, fontWeight: '800', color: color.ink },
  text: { fontSize: size.body, color: color.ink2 },
  button: {
    minHeight: tap.main,
    backgroundColor: color.signal,
    borderColor: color.black,
    borderWidth: 2,
    borderRadius: radius.control,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: space[4],
  },
  buttonText: { fontSize: size.lead, fontWeight: '800', color: color.black },
  note: { fontSize: size.caption, color: color.ink2 },
});
