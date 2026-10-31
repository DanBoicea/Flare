import { isAuthApiError } from '@supabase/supabase-js';
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';

import { ApiError, getMe, setAccessTokenProvider, USE_MOCK } from '@/api/client';
import { setMockStaff } from '@/api/mock';
import type { Me } from '@/api/types';
import type { StringKey } from '@/i18n/strings';
import { supabase } from './supabase';

/**
 * - loading: checking for a saved session, or loading /me
 * - signedOut: show sign-in, with `notice` explaining why when there is a reason
 * - offline: there is a session, but /me couldn't be reached; offer a retry
 * - signedIn: `me` is the active staff member from our back end
 */
export type SessionStatus = 'loading' | 'signedOut' | 'offline' | 'signedIn';

interface SessionValue {
  status: SessionStatus;
  me: Me | null;
  notice: StringKey | null;
  /** Resolves to an error string key, or null when sign-in worked. */
  signIn: (email: string, password: string) => Promise<StringKey | null>;
  /** Mock mode only: "signs in" as one of the fixture staff members. */
  signInAsMock: (staffId: string) => Promise<void>;
  signOut: () => Promise<void>;
  retry: () => void;
}

const SessionContext = createContext<SessionValue | null>(null);

// client.ts asks for the token on every request; supabase-js returns the current one, refreshing it if needed.
setAccessTokenProvider(async () => {
  if (!supabase) return null;
  const { data } = await supabase.auth.getSession();
  return data.session?.access_token ?? null;
});

export function SessionProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<SessionStatus>(USE_MOCK ? 'signedOut' : 'loading');
  const [me, setMe] = useState<Me | null>(null);
  const [notice, setNotice] = useState<StringKey | null>(null);

  const signOut = useCallback(async (reason: StringKey | null = null) => {
    setMe(null);
    setNotice(reason);
    setStatus('signedOut');
    // Revokes the refresh token on the server; the local session is cleared even if that call fails.
    await supabase?.auth.signOut().catch(() => {});
  }, []);

  /** Our back end decides who is staff: a Supabase account alone isn't enough (A03). */
  const loadMe = useCallback(async () => {
    setStatus('loading');
    try {
      setMe(await getMe());
      setNotice(null);
      setStatus('signedIn');
    } catch (e) {
      if (e instanceof ApiError && e.code === 'NOT_A_STAFF_MEMBER') {
        await signOut('error.NOT_A_STAFF_MEMBER');
      } else if (e instanceof ApiError && e.status === 401) {
        await signOut('error.UNAUTHORIZED');
      } else {
        setStatus('offline');
      }
    }
  }, [signOut]);

  // On start: reopen a saved session. Afterwards, follow sign-outs that supabase-js does itself
  // (a refresh token that stopped working, for example a disabled account).
  useEffect(() => {
    if (!supabase) return;
    supabase.auth.getSession().then(({ data }) => {
      if (data.session) {
        loadMe();
      } else {
        setStatus('signedOut');
      }
    });
    const { data: listener } = supabase.auth.onAuthStateChange((event) => {
      if (event === 'SIGNED_OUT') {
        setMe(null);
        setStatus('signedOut');
      }
    });
    return () => listener.subscription.unsubscribe();
  }, [loadMe]);

  const signIn = useCallback(async (email: string, password: string): Promise<StringKey | null> => {
    if (!supabase) return 'error.INTERNAL_ERROR';
    if (!email.trim() || !password) return 'signIn.missing';
    const { error } = await supabase.auth.signInWithPassword({ email: email.trim(), password });
    if (error) {
      if (isAuthApiError(error) && error.code === 'invalid_credentials') return 'signIn.wrongCredentials';
      // No HTTP status means the request never got an answer.
      return isAuthApiError(error) ? 'error.INTERNAL_ERROR' : 'error.NETWORK';
    }
    await loadMe();
    return null;
  }, [loadMe]);

  const signInAsMock = useCallback(async (staffId: string) => {
    setMockStaff(staffId);
    await loadMe();
  }, [loadMe]);

  const value = useMemo<SessionValue>(
    () => ({ status, me, notice, signIn, signInAsMock, signOut: () => signOut(), retry: loadMe }),
    [status, me, notice, signIn, signInAsMock, signOut, loadMe],
  );

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionValue {
  const value = useContext(SessionContext);
  if (!value) throw new Error('useSession must be used inside <SessionProvider>');
  return value;
}

/** The signed-in staff member. Only for screens behind the gate in _layout.tsx. */
export function useMe(): Me {
  const { me } = useSession();
  if (!me) throw new Error('useMe used outside the signed-in screens');
  return me;
}
