import { Link } from 'expo-router';
import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import type { IssueCard as Issue } from '@/api/types';
import { Icon } from '@/components/Icon';
import { SeverityBadge } from '@/components/SeverityBadge';
import { color, font, radius, severity as severityTokens, size, space, tap } from '@/constants/theme';
import { ageText, minutesSince, reportersText } from '@/issues/format';
import { useI18n } from '@/i18n/context';
import { issueIcon } from '@/lib/issueIcon';

interface Props {
  issue: Issue;
  /** The server's clock, for ages. */
  now: number;
  /** Arrived since the list opened: highlighted for a few seconds. */
  highlighted?: boolean;
  /** Opens the detail when pressed (list); false for the detail screen's own header. */
  asLink?: boolean;
  /** The one quick action under the card, if any. */
  action?: ReactNode;
}

/**
 * One Issue as staff see it (B03, Design V1 07-staff-list.html): Severity with colour, icon and word; the summary
 * marked as AI or Unclassified; place; who reported and when; who is on it; Emergency and Reopened badges.
 * A tile on the left shows what kind of place it is (toilets, baggage belt, lift…), so staff recognise it
 * before reading. Every value from the server is shown as plain text.
 */
export function IssueCard({ issue, now, highlighted = false, asLink = true, action }: Props) {
  const { t, language } = useI18n();
  const status = issue.takenBy
    ? t('card.takenBy', { name: issue.takenBy.displayName })
    : issue.status === 'NEW' ? t('card.new') : t('card.nobody');
  const age = ageText(t, minutesSince(issue.firstReportedAt, now));
  const reported = `${reportersText(t, issue, now)} · ${age}`;
  const summary = issue.summary[language];
  const critical = issue.severity === 'CRITICAL';
  const isNew = issue.status === 'NEW' && !issue.takenBy;
  // The whole card reads as one sentence (docs/design/components.md, Issue row).
  const sentence = [
    t(`severity.${issue.severity}`),
    issue.emergency ? t('card.emergency') : null,
    summary,
    issue.location ? `${issue.location.name[language]}, ${issue.location.label}` : null,
    reported,
    status,
  ].filter(Boolean).join('. ');

  const body = (
    <View style={styles.row}>
      <View style={[styles.placeTile, critical && styles.placeTileCritical]}>
        <Icon name={issueIcon(issue)} size={26} color={critical ? color.danger : color.navy} />
      </View>
      <View style={styles.content}>
        <View style={styles.topRow}>
          <View style={styles.badges}>
            <SeverityBadge severity={issue.severity} />
            {issue.emergency && <Text style={[styles.tag, styles.emergencyTag]}>{t('card.emergency')}</Text>}
            {issue.reopened && <Text style={styles.tag}>{t('card.reopened')}</Text>}
            {highlighted && <Text style={[styles.tag, styles.newTag]}>{t('card.new')}</Text>}
          </View>
          <View style={styles.status}>
            {isNew && <View style={styles.newDot} />}
            {issue.takenBy && <Icon name="user-check" size={16} color={color.ink} />}
            <Text style={styles.statusText}>{status}</Text>
          </View>
        </View>

        <Text style={styles.source}>{issue.unclassified ? t('card.unclassified') : issue.summarySource === 'LLM' ? t('card.ai') : ''}</Text>
        <Text style={styles.title} numberOfLines={asLink ? 2 : undefined}>{summary}</Text>

        {issue.location && (
          <View style={styles.place}>
            <Text style={styles.placeName}>{issue.location.name[language]}</Text>
            <Text style={styles.code}>{issue.location.label}</Text>
          </View>
        )}
        {issue.mentionedPlace && <Text style={styles.meta}>{t('card.mentioned', { place: issue.mentionedPlace })}</Text>}
        <View style={styles.metaRow}>
          <Icon name="users" size={16} color={color.ink2} />
          <Text style={[styles.meta, styles.shrink]}>{reported}</Text>
        </View>
      </View>
      {asLink && <Icon name="chevron-right" size={20} color={color.ink2} />}
    </View>
  );

  const cardStyle = StyleSheet.flatten([
    styles.card,
    { backgroundColor: severityTokens[issue.severity].cardBg },
    highlighted && styles.highlighted,
  ]);

  return (
    <View style={cardStyle}>
      {asLink ? (
        <Link href={{ pathname: '/issue/[id]', params: { id: issue.id } }} asChild>
          <Pressable style={styles.body} accessibilityRole="button" accessibilityLabel={sentence} accessibilityHint={t('card.openHint')}>
            {body}
          </Pressable>
        </Link>
      ) : (
        <View style={styles.body} accessible accessibilityLabel={sentence}>{body}</View>
      )}
      {action}
    </View>
  );
}

/** The card's main button: full width, 48 tall, cobalt (the one main action), disabled while sending. */
export function CardAction({ label, onPress, busy }: { label: string; onPress: () => void; busy?: boolean }) {
  return (
    <Pressable
      onPress={onPress}
      disabled={busy}
      accessibilityRole="button"
      accessibilityState={{ disabled: !!busy, busy: !!busy }}
      style={({ pressed }) => [styles.action, pressed && styles.actionPressed, busy && styles.actionBusy]}>
      <Text style={styles.actionText}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: { borderRadius: radius.card, paddingVertical: 14, paddingLeft: space[3], paddingRight: space[3], gap: space[3] },
  highlighted: { borderWidth: 3, borderColor: color.cobalt },
  body: { gap: space[2] },
  row: { flexDirection: 'row', alignItems: 'center', gap: space[3] },
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
  metaRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  shrink: { flexShrink: 1 },
  topRow: { flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between', gap: space[2] },
  badges: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: 6, flexShrink: 1 },
  tag: {
    fontSize: size.caption,
    fontFamily: font.semiBold,
    color: color.ink,
    backgroundColor: color.tint,
    borderRadius: radius.pill,
    overflow: 'hidden',
    paddingHorizontal: space[3],
    paddingVertical: 2,
  },
  emergencyTag: { color: color.paper, backgroundColor: color.danger },
  newTag: { color: color.paper, backgroundColor: color.cobalt },
  status: { flexDirection: 'row', alignItems: 'center', gap: 6, flexShrink: 1 },
  newDot: { width: 9, height: 9, borderRadius: 5, backgroundColor: color.navy },
  statusText: { fontSize: size.caption, fontFamily: font.semiBold, color: color.ink, textAlign: 'right' },
  source: { fontSize: size.caption, fontFamily: font.semiBold, color: color.ink2 },
  title: { fontSize: size.rowTitle, fontFamily: font.bold, color: color.ink, lineHeight: 26 },
  place: { flexDirection: 'row', alignItems: 'center', flexWrap: 'wrap', gap: 10 },
  placeName: { fontSize: size.caption, fontFamily: font.bold, color: color.ink },
  code: {
    fontSize: size.caption,
    fontFamily: font.semiBold,
    color: color.ink,
    borderColor: color.navy,
    borderWidth: 1.5,
    borderRadius: radius.code,
    paddingHorizontal: space[2],
  },
  meta: { fontSize: size.caption, fontFamily: font.regular, color: color.ink2 },
  action: {
    minHeight: tap.main,
    borderRadius: radius.control,
    backgroundColor: color.cobalt,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: space[4],
  },
  actionPressed: { backgroundColor: color.cobaltPress },
  actionBusy: { opacity: 0.6 },
  actionText: { fontSize: size.lead, fontFamily: font.semiBold, color: color.paper },
});
