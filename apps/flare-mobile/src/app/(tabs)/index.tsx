import { Link } from 'expo-router';
import { useEffect, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { ApiError, listIssues, USE_MOCK } from '@/api/client';
import type { IssueCard } from '@/api/types';
import { SeverityBadge } from '@/components/SeverityBadge';
import { color, severity as severityTokens, size, space } from '@/constants/theme';
import { useI18n } from '@/i18n/context';
import { hasKey } from '@/i18n/strings';

/** Placeholder Active list: loads once and shows plain rows. B03 adds polling, the header and the full IssueCard. */
export default function ActiveScreen() {
  const { t, language } = useI18n();
  const [issues, setIssues] = useState<IssueCard[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listIssues('ACTIVE')
      .then((list) => setIssues(list.issues))
      .catch((e: unknown) => {
        const code = e instanceof ApiError ? `error.${e.code}` : 'error.INTERNAL_ERROR';
        setError(hasKey(code) ? t(code) : t('error.INTERNAL_ERROR'));
      });
  }, [t]);

  return (
    <SafeAreaView style={styles.screen} edges={['top']}>
      {USE_MOCK && <Text style={styles.mockNote}>{t('active.mockNote')}</Text>}
      {error && <Text style={styles.message}>{error}</Text>}
      {!error && issues === null && <Text style={styles.message}>{t('list.loading')}</Text>}
      {issues && (
        <FlatList
          data={issues}
          keyExtractor={(issue) => issue.id}
          ListEmptyComponent={<Text style={styles.message}>{t('active.empty')}</Text>}
          renderItem={({ item }) => (
            <Link href={{ pathname: '/issue/[id]', params: { id: item.id } }} asChild>
              <Pressable
                // Link passes props through a Slot, which wants one style object, not an array.
                style={StyleSheet.flatten([styles.row, { backgroundColor: severityTokens[item.severity].rowBg }])}
                accessibilityRole="button">
                <View style={[styles.bar, { backgroundColor: severityTokens[item.severity].bar }]} />
                <View style={styles.rowBody}>
                  <SeverityBadge severity={item.severity} />
                  {/* Server text, shown as plain text only. */}
                  <Text style={styles.title} numberOfLines={2}>{item.summary[language]}</Text>
                  {item.location && (
                    <Text style={styles.place}>
                      {item.location.name[language]} · {item.location.label}
                    </Text>
                  )}
                  <Text style={styles.meta}>
                    {item.unclassified ? t('card.unclassified') : t('card.ai')} · {t('card.reporters', { count: item.reporterCount })}
                  </Text>
                </View>
              </Pressable>
            </Link>
          )}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: color.paper },
  mockNote: { fontSize: size.caption, color: color.ink2, paddingHorizontal: space[5], paddingVertical: space[2], backgroundColor: color.paper2 },
  message: { fontSize: size.body, color: color.ink2, padding: space[5], textAlign: 'center' },
  row: { flexDirection: 'row', borderBottomWidth: 1, borderBottomColor: color.line },
  bar: { width: 6 },
  rowBody: { flex: 1, paddingVertical: space[4], paddingHorizontal: space[4], gap: space[1] },
  title: { fontSize: size.rowTitle, fontWeight: '700', color: color.ink },
  place: { fontSize: size.body, fontWeight: '700', color: color.ink },
  meta: { fontSize: size.caption, color: color.ink2 },
});
