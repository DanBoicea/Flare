import { FlatList, RefreshControl, Text, View } from 'react-native';

import { IssueCard } from '@/components/IssueCard';
import { listStyles as styles } from '@/components/list-styles';
import { useIssueFeed } from '@/hooks/use-issue-feed';
import { useI18n } from '@/i18n/context';

/** Issues resolved in the last 24 hours (B03): loaded when the tab opens, plus pull to refresh. No polling. */
export default function ResolvedScreen() {
  const { t } = useI18n();
  const feed = useIssueFeed('RESOLVED', { poll: false });

  return (
    <View style={styles.screen}>
      {feed.offline && <Text style={styles.banner} accessibilityRole="alert">{t('feed.offline')}</Text>}
      {feed.error && <Text style={styles.message}>{feed.error}</Text>}
      {!feed.error && feed.issues === null && <Text style={styles.message}>{t('list.loading')}</Text>}
      {feed.issues && (
        <FlatList
          data={feed.issues}
          keyExtractor={(issue) => issue.id}
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl refreshing={feed.refreshing} onRefresh={feed.refresh} />}
          ListEmptyComponent={<Text style={styles.message}>{t('resolved.empty')}</Text>}
          ListFooterComponent={<Text style={styles.footer}>{t('app.simulated')}</Text>}
          renderItem={({ item }) => <IssueCard issue={item} now={feed.serverNow} />}
        />
      )}
    </View>
  );
}
