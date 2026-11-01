import { Link } from 'expo-router';
import { useEffect, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';

import { ApiError, listIssues, USE_MOCK } from '@/api/client';
import type { IssueCard } from '@/api/types';
import { Icon } from '@/components/Icon';
import { SeverityBadge } from '@/components/SeverityBadge';
import { color, font, radius, severity as severityTokens, size, space } from '@/constants/theme';
import { useI18n } from '@/i18n/context';
import { hasKey } from '@/i18n/strings';
import { issueIcon } from '@/lib/issueIcon';

/**
 * Placeholder Active list with V1 cards (07-staff-list.html). Loads once; B03 adds polling,
 * "Actualizat acum N s", the category tag and the one-tap action.
 */
export default function ActiveScreen() {
  const { t } = useI18n();
  const [issues, setIssues] = useState<IssueCard[] | null>(null);
  // Ages use the server's clock, so a phone with a wrong clock still shows the right minutes.
  const [serverNow, setServerNow] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listIssues('ACTIVE')
      .then((list) => {
        setServerNow(Date.parse(list.serverTime));
        setIssues(list.issues);
      })
      .catch((e: unknown) => {
        const code = e instanceof ApiError ? `error.${e.code}` : 'error.INTERNAL_ERROR';
        setError(hasKey(code) ? t(code) : t('error.INTERNAL_ERROR'));
      });
  }, [t]);

  return (
    <View style={styles.screen}>
      {USE_MOCK && <Text style={styles.mockNote}>{t('active.mockNote')}</Text>}
      {error && <Text style={styles.message}>{error}</Text>}
      {!error && issues === null && <Text style={styles.message}>{t('list.loading')}</Text>}
      {issues && (
        <FlatList
          data={issues}
          keyExtractor={(issue) => issue.id}
          contentContainerStyle={styles.list}
          ListEmptyComponent={
            <View style={styles.empty}>
              <Icon name="check-circle" size={32} color={color.ink2} />
              <Text style={styles.message}>{t('active.empty')}</Text>
            </View>
          }
          ListFooterComponent={<Text style={styles.footer}>{t('app.simulated')}</Text>}
          renderItem={({ item }) => <IssueRow issue={item} now={serverNow} />}
        />
      )}
    </View>
  );
}

function IssueRow({ issue, now }: { issue: IssueCard; now: number }) {
  const { t, language } = useI18n();
  const minutes = Math.max(0, Math.round((now - Date.parse(issue.firstReportedAt)) / 60_000));
  const isNew = issue.status === 'NEW' && !issue.takenBy;
  const status = issue.takenBy
    ? t('card.takenBy', { name: issue.takenBy.displayName })
    : isNew ? t('card.new') : t('card.nobody');
  const reported = issue.reporterCount === 1
    ? t('card.reportedOne', { minutes })
    : t('card.reportedMany', { count: issue.reporterCount, minutes });
  const summary = issue.summary[language];
  const place = issue.location ? `${issue.location.name[language]}, ${issue.location.label}` : null;
  const critical = issue.severity === 'CRITICAL';

  return (
    <Link href={{ pathname: '/issue/[id]', params: { id: issue.id } }} asChild>
      <Pressable
        // Link passes props through a Slot, which wants one style object, not an array.
        style={StyleSheet.flatten([styles.card, { backgroundColor: severityTokens[issue.severity].cardBg }])}
        accessibilityRole="button"
        // The whole card reads as one sentence (docs/design/components.md, Issue row).
        accessibilityLabel={[t(`severity.${issue.severity}`), summary, place, reported, status].filter(Boolean).join('. ')}>
        {/* What kind of place: toilets, baggage belt, lift… Recognised before reading. */}
        <View style={[styles.placeTile, critical && styles.placeTileCritical]}>
          <Icon name={issueIcon(issue)} size={26} color={critical ? color.danger : color.navy} />
        </View>

        <View style={styles.content}>
          <View style={styles.topRow}>
            <SeverityBadge severity={issue.severity} />
            <View style={styles.status}>
              {isNew && <View style={styles.newDot} />}
              {issue.takenBy && <Icon name="user-check" size={16} color={color.ink} />}
              <Text style={styles.statusText} numberOfLines={1}>{status}</Text>
            </View>
          </View>
          {/* Server text, shown as plain text only. */}
          <Text style={styles.title} numberOfLines={2}>{summary}</Text>
          {issue.location && (
            <View style={styles.place}>
              <Text style={styles.placeName}>{issue.location.name[language]}</Text>
              <Text style={styles.code}>{issue.location.label}</Text>
            </View>
          )}
          <View style={styles.meta}>
            <Icon name="users" size={16} color={color.ink2} />
            <Text style={styles.metaText}>{reported}</Text>
          </View>
        </View>

        <Icon name="chevron-right" size={20} color={color.ink2} />
      </Pressable>
    </Link>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: color.page },
  list: { padding: space[4], gap: space[3] },
  mockNote: {
    fontSize: size.caption,
    fontFamily: font.semiBold,
    color: color.ink,
    backgroundColor: color.tint,
    borderRadius: radius.pill,
    overflow: 'hidden',
    alignSelf: 'flex-start',
    marginHorizontal: space[4],
    marginTop: space[3],
    paddingHorizontal: space[4],
    paddingVertical: 4,
  },
  message: { fontSize: size.body, fontFamily: font.regular, color: color.ink2, padding: space[5], textAlign: 'center' },
  empty: { alignItems: 'center', paddingTop: space[8] },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: space[3],
    borderRadius: radius.card,
    paddingVertical: 14,
    paddingLeft: space[3],
    paddingRight: space[2],
  },
  placeTile: {
    width: 48,
    height: 48,
    alignSelf: 'flex-start',
    borderRadius: radius.tile,
    backgroundColor: color.tint,
    alignItems: 'center',
    justifyContent: 'center',
  },
  placeTileCritical: { backgroundColor: color.paper },
  content: { flex: 1, gap: space[2] },
  topRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: space[2] },
  status: { flexDirection: 'row', alignItems: 'center', gap: 6, flexShrink: 1 },
  newDot: { width: 9, height: 9, borderRadius: 5, backgroundColor: color.navy },
  statusText: { fontSize: size.caption, fontFamily: font.semiBold, color: color.ink, flexShrink: 1 },
  title: { fontSize: size.rowTitle, fontFamily: font.bold, color: color.ink, lineHeight: 24 },
  place: { flexDirection: 'row', alignItems: 'center', flexWrap: 'wrap', gap: space[2] },
  placeName: { fontSize: size.caption, fontFamily: font.bold, color: color.ink, flexShrink: 1 },
  code: {
    fontSize: size.caption,
    fontFamily: font.semiBold,
    color: color.ink,
    borderColor: color.navy,
    borderWidth: 1.5,
    borderRadius: radius.code,
    paddingHorizontal: space[2],
  },
  meta: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  metaText: { fontSize: size.caption, fontFamily: font.regular, color: color.ink2, flexShrink: 1 },
  footer: { fontSize: size.caption, fontFamily: font.regular, color: color.ink2, paddingVertical: space[2], paddingHorizontal: space[1] },
});
