import { useRef, useState, type ComponentRef } from 'react';
import {
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';

import { USE_MOCK } from '@/api/client';
import { useSession } from '@/auth/session';
import { StaffHeader } from '@/components/StaffHeader';
import { color, font, radius, size, space, tap } from '@/constants/theme';
import { fixtureStaff } from '@/fixtures/staff';
import { useI18n } from '@/i18n/context';
import type { StringKey } from '@/i18n/strings';

/** Email and password sign-in through Supabase (ADR-0010). Mock mode picks a fixture staff member instead. */
export default function SignInScreen() {
  const { t } = useI18n();
  const { notice } = useSession();

  return (
    <KeyboardAvoidingView style={styles.screen} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <StaffHeader />
      <ScrollView contentContainerStyle={styles.body} keyboardShouldPersistTaps="handled">
        <Text style={styles.title} accessibilityRole="header">{t('signIn.title')}</Text>
        {notice && <Text style={styles.error} accessibilityRole="alert">{t(notice)}</Text>}
        {USE_MOCK ? <MockPicker /> : <SignInForm />}
        <Text style={styles.note}>{t('app.demoNote')}</Text>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

function SignInForm() {
  const { t } = useI18n();
  const { signIn } = useSession();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<StringKey | null>(null);
  const [busy, setBusy] = useState(false);
  const passwordRef = useRef<ComponentRef<typeof TextInput>>(null);

  async function submit() {
    if (busy) return;
    setBusy(true);
    setError(null);
    const problem = await signIn(email, password);
    // On success the gate swaps this screen for the tabs, so only failures update state here.
    if (problem) {
      setError(problem);
      setBusy(false);
    }
  }

  return (
    <View style={styles.form}>
      <View style={styles.field}>
        <Text style={styles.label} nativeID="email-label">{t('signIn.email')}</Text>
        <TextInput
          style={styles.input}
          value={email}
          onChangeText={setEmail}
          accessibilityLabelledBy="email-label"
          autoCapitalize="none"
          autoCorrect={false}
          autoComplete="email"
          textContentType="username"
          keyboardType="email-address"
          returnKeyType="next"
          onSubmitEditing={() => passwordRef.current?.focus()}
          editable={!busy}
        />
      </View>
      <View style={styles.field}>
        <Text style={styles.label} nativeID="password-label">{t('signIn.password')}</Text>
        <TextInput
          ref={passwordRef}
          style={styles.input}
          value={password}
          onChangeText={setPassword}
          accessibilityLabelledBy="password-label"
          secureTextEntry
          autoComplete="current-password"
          textContentType="password"
          returnKeyType="go"
          onSubmitEditing={submit}
          editable={!busy}
        />
      </View>
      {error && <Text style={styles.error} accessibilityRole="alert">{t(error)}</Text>}
      <Pressable
        style={({ pressed }) => [styles.primary, pressed && styles.pressed, busy && styles.busy]}
        accessibilityRole="button"
        accessibilityState={{ busy, disabled: busy }}
        onPress={submit}>
        <Text style={styles.primaryText}>{busy ? t('signIn.submitting') : t('signIn.submit')}</Text>
      </Pressable>
    </View>
  );
}

function MockPicker() {
  const { t } = useI18n();
  const { signInAsMock } = useSession();
  return (
    <View style={styles.form}>
      <Text style={styles.label}>{t('signIn.pickStaff')}</Text>
      {fixtureStaff.map((member) => (
        <Pressable
          key={member.id}
          style={styles.secondary}
          accessibilityRole="button"
          onPress={() => signInAsMock(member.id)}>
          <Text style={styles.secondaryText}>
            {member.displayName}, {member.roles.map((role) => t(`role.${role}`)).join(', ')}
          </Text>
        </Pressable>
      ))}
    </View>
  );
}

const button = {
  minHeight: tap.main,
  borderRadius: radius.control,
  alignItems: 'center',
  justifyContent: 'center',
  paddingHorizontal: space[5],
} as const;

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: color.page },
  body: { padding: space[5], gap: space[5] },
  title: { fontSize: size.title, fontFamily: font.bold, color: color.ink },
  form: { gap: space[4] },
  field: { gap: space[2] },
  label: { fontSize: size.body, fontFamily: font.semiBold, color: color.ink },
  input: {
    minHeight: 56,
    borderColor: color.cobalt,
    borderWidth: 2,
    borderRadius: radius.control,
    paddingHorizontal: space[4],
    fontSize: size.lead,
    fontFamily: font.regular,
    color: color.ink,
    backgroundColor: color.paper,
  },
  // V1 inline error: tint with a navy bar on the left.
  error: {
    fontSize: size.body,
    fontFamily: font.regular,
    color: color.ink,
    backgroundColor: color.tint,
    borderLeftColor: color.navy,
    borderLeftWidth: 4,
    borderRadius: radius.control,
    paddingVertical: space[3],
    paddingHorizontal: space[4],
  },
  primary: { ...button, backgroundColor: color.cobalt },
  pressed: { backgroundColor: color.cobaltPress },
  busy: { opacity: 0.6 },
  primaryText: { fontSize: size.lead, fontFamily: font.semiBold, color: color.paper },
  secondary: { ...button, backgroundColor: color.paper, borderColor: color.navy, borderWidth: 2 },
  secondaryText: { fontSize: size.lead, fontFamily: font.semiBold, color: color.ink },
  note: { fontSize: size.caption, fontFamily: font.regular, color: color.ink2 },
});
