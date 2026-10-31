import { useCallback, useState } from 'react';

import { ApiError, getIssue } from '@/api/client';
import type { IssueCard, IssueDetail } from '@/api/types';
import { useToast } from '@/components/Toast';
import { errorText } from '@/issues/format';
import { useI18n } from '@/i18n/context';

interface Handlers {
  /** The Issue after the action, or after reloading it when someone else changed it first. */
  onUpdated: (issue: IssueDetail) => void;
  /** The Issue is gone or no longer visible to this staff member. */
  onGone: () => void;
}

/**
 * Runs a staff action on an Issue (B04). The button is disabled while it is sent. On 409 VERSION_CONFLICT it
 * reloads the Issue and says who got there first; on 404 it says the Issue is no longer available.
 */
export function useAction({ onUpdated, onGone }: Handlers) {
  const { t } = useI18n();
  const toast = useToast();
  const [busy, setBusy] = useState(false);

  const run = useCallback(async (before: IssueCard, send: () => Promise<IssueDetail>) => {
    if (busy) return;
    setBusy(true);
    try {
      onUpdated(await send());
    } catch (e) {
      if (e instanceof ApiError && e.code === 'VERSION_CONFLICT') {
        try {
          const fresh = await getIssue(before.id);
          const someoneTookIt = fresh.takenBy && fresh.takenBy.id !== before.takenBy?.id;
          toast(someoneTookIt ? t('toast.tookIt', { name: fresh.takenBy!.displayName }) : t('toast.changed'));
          onUpdated(fresh);
        } catch {
          toast(t('toast.gone'));
          onGone();
        }
      } else if (e instanceof ApiError && e.code === 'NOT_FOUND') {
        toast(t('toast.gone'));
        onGone();
      } else {
        toast(errorText(t, e));
      }
    } finally {
      setBusy(false);
    }
  }, [busy, onUpdated, onGone, t, toast]);

  return { busy, run };
}
