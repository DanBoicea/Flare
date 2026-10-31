import { useEffect, useState } from 'react';

import { listTeams } from '@/api/client';
import type { Team } from '@/api/types';

let cached: Team[] | null = null;

/** The Teams for the hand-over and release pickers, loaded once per app run. */
export function useTeams(): Team[] {
  const [teams, setTeams] = useState<Team[]>(cached ?? []);
  useEffect(() => {
    if (cached) return;
    listTeams()
      .then((loaded) => {
        cached = loaded;
        setTeams(loaded);
      })
      .catch(() => {
        // The picker stays empty; the action can be tried again later.
      });
  }, []);
  return teams;
}
