import { useNavigation } from 'expo-router';
import { useEffect } from 'react';
import { Alert, FlatList, RefreshControl, Text, View } from 'react-native';

import { issueAction, USE_MOCK } from '@/api/client';
import type { IssueCard as Issue } from '@/api/types';
import { useMe } from '@/auth/session';
import { CardAction, IssueCard } from '@/components/IssueCard';
import { listStyles } from '@/components/list-styles';
import { useAction } from '@/hooks/use-action';
import { useIssueFeed } from '@/hooks/use-issue-feed';
import { useI18n } from '@/i18n/context';
import { mainAction } from '@/issues/transitions';

/**
 * The Active list (B03): polled every 5 s in the foreground, Critical first (the server sorts), new Issues
 * highlighted with a count on the tab, pull to refresh, and an offline banner that keeps the last list.
 */
export default function ActiveScreen() {
  const { t } = useI18n();
  const navigation = useNavigation();
  const feed = useIssueFeed('ACTIVE', { poll: true });

  useEffect(() => {
    navigation.setOptions({ tabBarBadge: feed.newIds.size > 0 ? feed.newIds.size : undefined });
  }, [navigation, feed.newIds.size]);

  return (
    <View style={styles.screen}>
      {USE_MOCK && <Text style={styles.mockNote}>{t('active.mockNote')}</Text>}
      {feed.offline && <Text style={styles.banner} accessibilityRole="alert">{t('feed.offline')}</Text>}
      {feed.error && <Text style={styles.message}>{feed.error}</Text>}
      {!feed.error && feed.issues === null && <Text style={styles.message}>{t('list.loading')}</Text>}
      {feed.issues && (
        <FlatList
          data={feed.issues}
          keyExtractor={(issue) => issue.id}
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl refreshing={feed.refreshing} onRefresh={feed.refresh} />}
          ListEmptyComponent={<Text style={styles.message}>{t('active.empty')}</Text>}
          ListFooterComponent={<Text style={styles.footer}>{t('app.simulated')}</Text>}
          renderItem={({ item }) => (
            <ActiveCard issue={item} now={feed.serverNow} highlighted={feed.newIds.has(item.id)} onChanged={feed.refresh} />
          )}
        />
      )}
    </View>
  );
}

/** A card with its one quick action: "I'm on it", or "Resolve" (with a confirmation) when I hold it. */
function ActiveCard({ issue, now, highlighted, onChanged }: { issue: Issue; now: number; highlighted: boolean; onChanged: () => void }) {
  const { t } = useI18n();
  const me = useMe();
  const { busy, run } = useAction({ onUpdated: onChanged, onGone: onChanged });
  const action = mainAction(issue, me.id);

  const take = () => run(issue, () => issueAction(issue.id, 'take', issue.version));
  const resolve = () => Alert.alert(t('confirm.resolve'), undefined, [
    { text: t('confirm.cancel'), style: 'cancel' },
    { text: t('action.resolve'), onPress: () => run(issue, () => issueAction(issue.id, 'resolve', issue.version)) },
  ]);

  return (
    <IssueCard
      issue={issue}
      now={now}
      highlighted={highlighted}
      action={action && (
        <CardAction
          label={action === 'take' ? t('action.take') : t('action.resolve')}
          onPress={action === 'take' ? take : resolve}
          busy={busy}
        />
      )}
    />
  );
}

const styles = listStyles;
