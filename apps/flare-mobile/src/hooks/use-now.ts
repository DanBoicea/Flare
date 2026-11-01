import { useEffect, useState } from 'react';

/** The current time, refreshed every `everyMs`, so ages ("12 min ago") stay right without calling Date.now() while rendering. */
export function useNow(everyMs = 30_000): number {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const interval = setInterval(() => setNow(Date.now()), everyMs);
    return () => clearInterval(interval);
  }, [everyMs]);
  return now;
}
