import { useLocalSearchParams, useRouter } from 'expo-router';
import { useCallback, useState, type ReactNode } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { handOverIssue, issueAction, overrideIssue } from '@/api/client';
import type { Category, HistoryEntry, ReportView, Severity, TeamId } from '@/api/types';
import { useMe } from '@/auth/session';
import { IssueCard } from '@/components/IssueCard';
import { PickerSheet } from '@/components/PickerSheet';
import { color, font, radius, size, space, tap } from '@/constants/theme';
import { useAction } from '@/hooks/use-action';
import { useIssue } from '@/hooks/use-issue';
import { useNow } from '@/hooks/use-now';
import { useTeams } from '@/hooks/use-teams';
import { useI18n } from '@/i18n/context';
import { hasKey } from '@/i18n/strings';
import { ageText, clockTime, minutesSince } from '@/issues/format';
import { CATEGORIES, SEVERITIES } from '@/issues/options';
import { canDo, mainAction } from '@/issues/transitions';

type Picker = 'team' | 'category' | 'severity' | null;

/**
 * Issue detail (B04): the card, the Passengers' Reports and the history, with the actions at the bottom within
 * thumb reach. Only the actions valid for the Issue's state are shown; Resolve and Hand over ask for one extra tap.
 */
export default function IssueDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { t, language } = useI18n();
  const router = useRouter();
  const insets = useSafeAreaInsets();
  const me = useMe();
  const teams = useTeams();
  const now = useNow();
  const { issue, setIssue, error } = useIssue(id);
  const [picker, setPicker] = useState<Picker>(null);
  const goBack = useCallback(() => router.back(), [router]);
  const { busy, run } = useAction({ onUpdated: setIssue, onGone: goBack });

  if (!issue) {
    return (
      <View style={styles.screen}>
        <Text style={styles.message}>{error ?? t('list.loading')}</Text>
      </View>
    );
  }

  const act = (action: 'acknowledge' | 'take' | 'resolve' | 'reopen') =>
    run(issue, () => issueAction(issue.id, action, issue.version));

  const confirmResolve = () => Alert.alert(t('confirm.resolve'), undefined, [
    { text: t('confirm.cancel'), style: 'cancel' },
    { text: t('action.resolve'), onPress: () => act('resolve') },
  ]);

  const confirmHandOver = (team: TeamId) => {
    setPicker(null);
    const name = teams.find((candidate) => candidate.id === team)?.name[language] ?? t(`team.${team}`);
    Alert.alert(t('confirm.handOver', { team: name }), undefined, [
      { text: t('confirm.cancel'), style: 'cancel' },
      { text: t('action.handOver'), onPress: () => run(issue, () => handOverIssue(issue.id, issue.version, team)) },
    ]);
  };

  const override = (change: { category?: Category; severity?: Severity }) => {
    setPicker(null);
    run(issue, () => overrideIssue(issue.id, issue.version, change));
  };

  const main = mainAction(issue, me.id);

  return (
    <View style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content}>
        <IssueCard issue={issue} now={now} asLink={false} />
        <Text style={styles.category}>
          {t('detail.category')}: {t(`category.${issue.category}`)}
          {issue.team ? ` · ${t(`team.${issue.team}`)}` : ` · ${t('detail.noTeam')}`}
        </Text>

        <Section title={t('detail.more')}>
          <View style={styles.row}>
            {canDo('handOver', issue) && <Button label={t('action.handOver')} onPress={() => setPicker('team')} busy={busy} />}
            <Button label={t('action.editCategory')} onPress={() => setPicker('category')} busy={busy} />
            <Button label={t('action.editSeverity')} onPress={() => setPicker('severity')} busy={busy} />
          </View>
        </Section>

        <Section title={t('detail.reports')}>
          {issue.reports.map((report) => <ReportRow key={report.id} report={report} now={now} />)}
        </Section>

        <Section title={t('detail.history')}>
          {issue.history.map((entry, index) => <HistoryRow key={`${entry.at}-${index}`} entry={entry} />)}
        </Section>
        <Text style={styles.footer}>{t('app.simulated')}</Text>
      </ScrollView>

      <View style={[styles.actions, { paddingBottom: insets.bottom + space[3] }]}>
        {main === 'take' && <Button label={t('action.take')} onPress={() => act('take')} busy={busy} primary />}
        {main === 'resolve' && <Button label={t('action.resolve')} onPress={confirmResolve} busy={busy} primary />}
        {canDo('reopen', issue) && <Button label={t('action.reopen')} onPress={() => act('reopen')} busy={busy} primary />}
        <View style={styles.row}>
          {canDo('acknowledge', issue) && <Button label={t('action.ack')} onPress={() => act('acknowledge')} busy={busy} />}
          {main !== 'resolve' && canDo('resolve', issue) && <Button label={t('action.resolve')} onPress={confirmResolve} busy={busy} />}
        </View>
      </View>

      <PickerSheet
        visible={picker === 'team'}
        title={t('picker.chooseTeam')}
        options={teams.filter((team) => team.id !== issue.team).map((team) => ({ value: team.id, label: team.name[language] }))}
        onPick={confirmHandOver}
        onClose={() => setPicker(null)}
      />
      <PickerSheet
        visible={picker === 'category'}
        title={t('picker.category')}
        options={CATEGORIES.map((category) => ({ value: category, label: t(`category.${category}`) }))}
        selected={issue.category}
        onPick={(category) => override({ category })}
        onClose={() => setPicker(null)}
      />
      <PickerSheet
        visible={picker === 'severity'}
        title={t('picker.severity')}
        options={SEVERITIES.map((severity) => ({ value: severity, label: t(`severity.${severity}`) }))}
        selected={issue.severity}
        onPick={(severity) => override({ severity })}
        onClose={() => setPicker(null)}
      />
    </View>
  );
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle} accessibilityRole="header">{title}</Text>
      {children}
    </View>
  );
}

/** One Passenger's message, exactly as they wrote it (plain text), with what they chose to give. */
function ReportRow({ report, now }: { report: ReportView; now: number }) {
  const { t, language } = useI18n();
  return (
    <View style={styles.box}>
      <Text style={styles.reportText}>{report.text}</Text>
      <Text style={styles.meta}>
        {clockTime(report.createdAt, language)} · {ageText(t, minutesSince(report.createdAt, now))} · {t(`lang.${report.language}`)}
      </Text>
      {(report.contact || report.flightNumber) && (
        <View style={styles.provided}>
          <Text style={styles.providedLabel}>{t('detail.providedByPassenger')}</Text>
          {report.contact && <Text style={styles.meta}>{t('detail.contact', { contact: report.contact })}</Text>}
          {report.flightNumber && <Text style={styles.meta}>{t('detail.flight', { flight: report.flightNumber })}</Text>}
        </View>
      )}
      {report.mentionedPlace && <Text style={styles.meta}>{t('card.mentioned', { place: report.mentionedPlace })}</Text>}
    </View>
  );
}

/** "Ana (simulat) · Severity changed: Medium → High · 14:05". Details hold only our own values, never Passenger text. */
function HistoryRow({ entry }: { entry: HistoryEntry }) {
  const { t, language } = useI18n();
  const from = describe(entry.action, entry.details.from, t);
  const to = describe(entry.action, entry.details.to, t);
  return (
    <View style={styles.historyRow}>
      <Text style={styles.historyText}>
        <Text style={styles.historyWho}>{entry.staffDisplayName ?? t('detail.system')}</Text>
        {' · '}{t(`history.${entry.action}`)}
        {to ? `: ${from ?? '—'} → ${to}` : ''}
      </Text>
      <Text style={styles.meta}>{clockTime(entry.at, language)}</Text>
    </View>
  );
}

/** Turns a from/to value (an enum name) into words; unknown values are left out. */
function describe(action: HistoryEntry['action'], value: unknown, t: ReturnType<typeof useI18n>['t']): string | null {
  if (typeof value !== 'string') return null;
  const prefix = action === 'CATEGORY_CHANGED' ? 'category' : action === 'SEVERITY_CHANGED' ? 'severity' : action === 'HANDED_OVER' ? 'team' : null;
  const key = prefix ? `${prefix}.${value}` : '';
  return hasKey(key) ? t(key) : null;
}

function Button({ label, onPress, busy, primary }: { label: string; onPress: () => void; busy: boolean; primary?: boolean }) {
  return (
    <Pressable
      onPress={onPress}
      disabled={busy}
      accessibilityRole="button"
      accessibilityState={{ disabled: busy, busy }}
      style={({ pressed }) => [
        primary ? styles.primary : styles.secondary,
        pressed && (primary ? styles.primaryPressed : styles.secondaryPressed),
        busy && styles.busy,
      ]}>
      <Text style={primary ? styles.primaryText : styles.secondaryText}>{label}</Text>
    </Pressable>
  );
}

const button = { minHeight: tap.main, borderRadius: radius.control, alignItems: 'center', justifyContent: 'center', paddingHorizontal: space[4] } as const;

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: color.page },
  content: { padding: space[4], gap: space[4] },
  message: { fontSize: size.body, fontFamily: font.regular, color: color.ink2, padding: space[5], textAlign: 'center' },
  category: { fontSize: size.caption, fontFamily: font.semiBold, color: color.ink2, paddingHorizontal: space[1] },
  section: { gap: space[2] },
  sectionTitle: { fontSize: size.lead, fontFamily: font.bold, color: color.ink, paddingHorizontal: space[1] },
  box: { backgroundColor: color.paper, borderRadius: radius.control, padding: space[4], gap: space[1] },
  reportText: { fontSize: size.body, fontFamily: font.regular, color: color.ink },
  provided: { marginTop: space[2], gap: 2 },
  providedLabel: { fontSize: size.caption, fontFamily: font.semiBold, color: color.ink },
  meta: { fontSize: size.caption, fontFamily: font.regular, color: color.ink2 },
  historyRow: { backgroundColor: color.paper, borderRadius: radius.control, paddingHorizontal: space[4], paddingVertical: space[3], gap: 2 },
  historyText: { fontSize: size.body, fontFamily: font.regular, color: color.ink },
  historyWho: { fontFamily: font.semiBold },
  footer: { fontSize: size.caption, fontFamily: font.regular, color: color.ink2, paddingHorizontal: space[1] },
  actions: {
    backgroundColor: color.paper,
    borderTopColor: color.line,
    borderTopWidth: 1,
    paddingTop: space[3],
    paddingHorizontal: space[4],
    gap: space[2],
  },
  row: { flexDirection: 'row', flexWrap: 'wrap', gap: space[2] },
  primary: { ...button, backgroundColor: color.cobalt },
  primaryPressed: { backgroundColor: color.cobaltPress },
  primaryText: { fontSize: size.lead, fontFamily: font.semiBold, color: color.paper },
  secondary: { ...button, backgroundColor: color.paper, borderColor: color.navy, borderWidth: 2, flexGrow: 1 },
  secondaryPressed: { backgroundColor: color.tint },
  secondaryText: { fontSize: size.body, fontFamily: font.semiBold, color: color.ink },
  busy: { opacity: 0.6 },
});
