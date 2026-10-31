import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { color, font, radius, size, space } from '@/constants/theme';

const SHOW_MS = 3500;

const ToastContext = createContext<(message: string) => void>(() => {});

/** Shows one short message at a time above the tab bar, announced to screen readers. No library needed. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [message, setMessage] = useState<string | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const insets = useSafeAreaInsets();

  const show = useCallback((text: string) => {
    if (timer.current) clearTimeout(timer.current);
    setMessage(text);
    timer.current = setTimeout(() => setMessage(null), SHOW_MS);
  }, []);

  useEffect(() => () => {
    if (timer.current) clearTimeout(timer.current);
  }, []);

  return (
    <ToastContext.Provider value={show}>
      {children}
      {message && (
        <View pointerEvents="none" style={[styles.wrap, { bottom: insets.bottom + 72 }]}>
          <Text style={styles.toast} accessibilityRole="alert" accessibilityLiveRegion="polite">{message}</Text>
        </View>
      )}
    </ToastContext.Provider>
  );
}

export function useToast(): (message: string) => void {
  return useContext(ToastContext);
}

const styles = StyleSheet.create({
  wrap: { position: 'absolute', left: space[4], right: space[4], alignItems: 'center' },
  toast: {
    fontSize: size.body,
    fontFamily: font.semiBold,
    color: color.paper,
    backgroundColor: color.navy,
    borderRadius: radius.control,
    overflow: 'hidden',
    paddingHorizontal: space[4],
    paddingVertical: space[3],
  },
});
