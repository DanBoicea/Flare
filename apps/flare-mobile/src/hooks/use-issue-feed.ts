import { useFocusEffect } from 'expo-router';
import { useCallback, useEffect, useRef, useState } from 'react';
import { AppState } from 'react-native';

import { listIssues } from '@/api/client';
import type { IssueCard, IssueView } from '@/api/types';
import { errorText } from '@/issues/format';
import { useI18n } from '@/i18n/context';

const POLL_MS = 5000;
const HIGHLIGHT_MS = 10_000;

export interface IssueFeed {
  issues: IssueCard[] | null;
  /** The server's clock at the last load, so ages are right even if the phone's clock is wrong. */
  serverNow: number;
  /** The last load failed but an earlier list is still shown. */
  offline: boolean;
  /** The first load failed: nothing to show. */
  error: string | null;
  /** Issues that appeared since the screen opened, highlighted for 10 s. */
  newIds: ReadonlySet<string>;
  refreshing: boolean;
  refresh: () => Promise<void>;
}

/**
 * An Issue list (B03, ADR-0011). With `poll`, it loads every 5 s while the app is in the foreground, stops in the
 * background, and loads at once on return. Without it, it loads each time the screen gets focus. A failed load
 * keeps the last good list and sets `offline`.
 */
export function useIssueFeed(view: IssueView, { poll }: { poll: boolean }): IssueFeed {
  const { t } = useI18n();
  const [issues, setIssues] = useState<IssueCard[] | null>(null);
  const [serverNow, setServerNow] = useState(0);
  const [offline, setOffline] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [newIds, setNewIds] = useState<ReadonlySet<string>>(new Set());
  const [refreshing, setRefreshing] = useState(false);

  const seen = useRef(new Set<string>());
  const loadedOnce = useRef(false);
  const inFlight = useRef(false);
  const timers = useRef<ReturnType<typeof setTimeout>[]>([]);

  const load = useCallback(async () => {
    if (inFlight.current) return;
    inFlight.current = true;
    try {
      const list = await listIssues(view);
      setServerNow(Date.parse(list.serverTime));
      setIssues(list.issues);
      setOffline(false);
      setError(null);
      const fresh = list.issues.map((issue) => issue.id).filter((id) => !seen.current.has(id));
      fresh.forEach((id) => seen.current.add(id));
      // The first load is what was already there; only later arrivals are "new".
      if (loadedOnce.current && fresh.length > 0) {
        setNewIds((previous) => new Set([...previous, ...fresh]));
        timers.current.push(setTimeout(() => {
          setNewIds((previous) => new Set([...previous].filter((id) => !fresh.includes(id))));
        }, HIGHLIGHT_MS));
      }
      loadedOnce.current = true;
    } catch (e) {
      if (loadedOnce.current) {
        setOffline(true);
      } else {
        setError(errorText(t, e));
      }
    } finally {
      inFlight.current = false;
    }
  }, [view, t]);

  // Polling: only while the app is in the foreground.
  useEffect(() => {
    if (!poll) return;
    // The first load runs on the next tick, then every 5 s.
    const first = setTimeout(load, 0);
    let interval: ReturnType<typeof setInterval> | null = setInterval(load, POLL_MS);
    const subscription = AppState.addEventListener('change', (state) => {
      if (state === 'active') {
        load();
        if (!interval) interval = setInterval(load, POLL_MS);
      } else if (interval) {
        clearInterval(interval);
        interval = null;
      }
    });
    return () => {
      clearTimeout(first);
      if (interval) clearInterval(interval);
      subscription.remove();
    };
  }, [poll, load]);

  // Without polling: load whenever the screen comes into view.
  useFocusEffect(useCallback(() => {
    if (!poll) load();
  }, [poll, load]));

  useEffect(() => () => timers.current.forEach(clearTimeout), []);

  const refresh = useCallback(async () => {
    setRefreshing(true);
    await load();
    setRefreshing(false);
  }, [load]);

  return { issues, serverNow, offline, error, newIds, refreshing, refresh };
}
