import { useFocusEffect } from 'expo-router';
import { useCallback, useRef, useState } from 'react';
import { FlatList, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';

import { dismissReport, getReview, releaseReport } from '@/api/client';
import type { ReviewAlert, ReviewItem, ReviewList, TeamId } from '@/api/types';
import { listStyles } from '@/components/list-styles';
import { PickerSheet } from '@/components/PickerSheet';
import { useToast } from '@/components/Toast';
import { color, font, radius, size, space, tap } from '@/constants/theme';
import { useNow } from '@/hooks/use-now';
import { useTeams } from '@/hooks/use-teams';
import { useI18n } from '@/i18n/context';
import { ageText, clockTime, errorText, minutesSince } from '@/issues/format';

const POLL_MS = 10_000;
const NO_TEAM = 'NO_TEAM';

/**
 * Needs review (B05), for the duty manager only (the tab is hidden for everyone else, and the server answers 403):
 * Strict mode alerts first, then the Quarantined Reports with Release (optionally to a Team) and Dismiss. Polls
 * every 10 s while the tab is open.
 */
export default function ReviewScreen() {
  const { t, language } = useI18n();
  const toast = useToast();
  const teams = useTeams();
  const [review, setReview] = useState<ReviewList | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [releasing, setReleasing] = useState<ReviewItem | null>(null);
  const inFlight = useRef(false);
  const now = useNow();

  const load = useCallback(async () => {
    if (inFlight.current) return;
    inFlight.current = true;
    try {
      setReview(await getReview());
      setError(null);
    } catch (e) {
      setError(errorText(t, e));
    } finally {
      inFlight.current = false;
    }
  }, [t]);

  useFocusEffect(useCallback(() => {
    load();
    const interval = setInterval(load, POLL_MS);
    return () => clearInterval(interval);
  }, [load]));

  const refresh = async () => {
    setRefreshing(true);
    await load();
    setRefreshing(false);
  };

  const act = async (item: ReviewItem, send: () => Promise<void>, done: string) => {
    setBusyId(item.reportId);
    try {
      await send();
      toast(done);
    } catch (e) {
      toast(errorText(t, e));
    } finally {
      setBusyId(null);
      await load();
    }
  };

  const release = (choice: TeamId | typeof NO_TEAM) => {
    const item = releasing;
    setReleasing(null);
    if (!item) return;
    const team = choice === NO_TEAM ? null : choice;
    act(item, () => releaseReport(item.reportId, team), t('review.released'));
  };

  return (
    <View style={listStyles.screen}>
      {error && <Text style={listStyles.banner} accessibilityRole="alert">{error}</Text>}
      {!error && review === null && <Text style={listStyles.message}>{t('list.loading')}</Text>}
      {review && (
        <FlatList
          data={review.items}
          keyExtractor={(item) => item.reportId}
          contentContainerStyle={listStyles.list}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={refresh} />}
          ListHeaderComponent={review.alerts.length > 0 ? (
            <View style={styles.alerts}>{review.alerts.map((alert) => <AlertBanner key={alert.qrCodeId} alert={alert} now={now} />)}</View>
          ) : undefined}
          ListEmptyComponent={<Text style={listStyles.message}>{t('review.empty')}</Text>}
          ListFooterComponent={<Text style={listStyles.footer}>{t('app.simulated')}</Text>}
          renderItem={({ item }) => (
            <View style={styles.card}>
              <View style={styles.topRow}>
                <Text style={styles.reason}>{t(`reviewReason.${item.reason}`)}</Text>
                <Text style={styles.meta}>{clockTime(item.createdAt, language)} · {ageText(t, minutesSince(item.createdAt, now))}</Text>
              </View>
              {/* The Passenger's text, untrusted: plain text only. */}
              <Text style={styles.text}>{item.text}</Text>
              <Text style={styles.meta}>
                {item.location
                  ? `${item.location.name[language]} · ${item.location.label}`
                  : item.scannedQrCodeId ? t('review.unknownQr', { id: item.scannedQrCodeId }) : t('review.noQr')}
              </Text>
              <View style={styles.buttons}>
                <Pressable
                  onPress={() => setReleasing(item)}
                  disabled={busyId === item.reportId}
                  accessibilityRole="button"
                  style={({ pressed }) => [styles.primary, pressed && styles.primaryPressed, busyId === item.reportId && styles.busy]}>
                  <Text style={styles.primaryText}>{t('review.release')}</Text>
                </Pressable>
                <Pressable
                  onPress={() => act(item, () => dismissReport(item.reportId), t('review.dismissed'))}
                  disabled={busyId === item.reportId}
                  accessibilityRole="button"
                  style={({ pressed }) => [styles.secondary, pressed && styles.secondaryPressed, busyId === item.reportId && styles.busy]}>
                  <Text style={styles.secondaryText}>{t('review.dismiss')}</Text>
                </Pressable>
              </View>
            </View>
          )}
        />
      )}

      <PickerSheet<TeamId | typeof NO_TEAM>
        visible={releasing !== null}
        title={t('picker.releaseTo')}
        options={[
          { value: NO_TEAM, label: t('picker.noTeam') },
          ...teams.map((team) => ({ value: team.id, label: team.name[language] })),
        ]}
        onPick={release}
        onClose={() => setReleasing(null)}
      />
    </View>
  );
}

/** "DEP-WC-02 · Strict mode for 6 min · 7 messages": a QR code under a flood of messages. */
function AlertBanner({ alert, now }: { alert: ReviewAlert; now: number }) {
  const { t } = useI18n();
  const where = alert.location ? alert.location.label : t('review.unknownQr', { id: alert.qrCodeId });
  return (
    <Text style={styles.alert} accessibilityRole="alert">
      {t('review.alert', { place: where, minutes: minutesSince(alert.since, now), count: alert.messageCount })}
    </Text>
  );
}

const button = { minHeight: tap.main, borderRadius: radius.control, alignItems: 'center', justifyContent: 'center', paddingHorizontal: space[4], flexGrow: 1 } as const;

const styles = StyleSheet.create({
  alerts: { gap: space[2], marginBottom: space[1] },
  alert: {
    fontSize: size.body,
    fontFamily: font.semiBold,
    color: color.paper,
    backgroundColor: color.danger,
    borderRadius: radius.control,
    overflow: 'hidden',
    paddingHorizontal: space[4],
    paddingVertical: space[3],
  },
  card: { backgroundColor: color.paper, borderRadius: radius.card, paddingVertical: 14, paddingHorizontal: space[4], gap: space[2] },
  topRow: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: space[2] },
  reason: {
    fontSize: size.caption,
    fontFamily: font.semiBold,
    color: color.ink,
    backgroundColor: color.tint,
    borderRadius: radius.pill,
    overflow: 'hidden',
    paddingHorizontal: space[3],
    paddingVertical: 2,
  },
  text: { fontSize: size.body, fontFamily: font.regular, color: color.ink },
  meta: { fontSize: size.caption, fontFamily: font.regular, color: color.ink2 },
  buttons: { flexDirection: 'row', flexWrap: 'wrap', gap: space[2], marginTop: space[1] },
  primary: { ...button, backgroundColor: color.cobalt },
  primaryPressed: { backgroundColor: color.cobaltPress },
  primaryText: { fontSize: size.lead, fontFamily: font.semiBold, color: color.paper },
  secondary: { ...button, backgroundColor: color.paper, borderColor: color.navy, borderWidth: 2 },
  secondaryPressed: { backgroundColor: color.tint },
  secondaryText: { fontSize: size.lead, fontFamily: font.semiBold, color: color.ink },
  busy: { opacity: 0.6 },
});
