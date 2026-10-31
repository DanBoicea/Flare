import AsyncStorage from '@react-native-async-storage/async-storage';
import { createClient, type SupabaseClient } from '@supabase/supabase-js';
import { AppState } from 'react-native';

import { USE_MOCK } from '@/api/client';

/**
 * Supabase, for staff sign-in only (ADR-0010): no database queries and no Realtime. Everything else
 * goes through our back end. Set up as in Supabase's Expo example: the session is kept in AsyncStorage
 * and refreshed automatically. Null in mock mode, which never talks to Supabase.
 */
export const supabase: SupabaseClient | null = USE_MOCK
  ? null
  : createClient(process.env.EXPO_PUBLIC_SUPABASE_URL ?? '', process.env.EXPO_PUBLIC_SUPABASE_ANON_KEY ?? '', {
      auth: {
        storage: AsyncStorage,
        autoRefreshToken: true,
        persistSession: true,
        detectSessionInUrl: false,
      },
    });

// Refresh tokens only while the app is in the foreground; a suspended app can't keep a timer running.
if (supabase) {
  AppState.addEventListener('change', (state) => {
    if (state === 'active') {
      supabase.auth.startAutoRefresh();
    } else {
      supabase.auth.stopAutoRefresh();
    }
  });
}
