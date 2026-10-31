import { useFocusEffect } from 'expo-router';
import { useCallback, useState } from 'react';

import { getIssue } from '@/api/client';
import type { IssueDetail } from '@/api/types';
import { errorText } from '@/issues/format';
import { useI18n } from '@/i18n/context';

/** One Issue's detail (B04), loaded when the screen gets focus. Actions replace it with the server's answer. */
export function useIssue(id: string) {
  const { t } = useI18n();
  const [issue, setIssue] = useState<IssueDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refetch = useCallback(async () => {
    try {
      setIssue(await getIssue(id));
      setError(null);
    } catch (e) {
      setError(errorText(t, e));
    }
  }, [id, t]);

  useFocusEffect(useCallback(() => {
    refetch();
  }, [refetch]));

  return { issue, setIssue, error, refetch };
}
