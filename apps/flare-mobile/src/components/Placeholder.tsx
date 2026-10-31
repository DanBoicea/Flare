import { StyleSheet, Text, View } from 'react-native';

import { color, size, space } from '@/constants/theme';

/** A screen that a later task fills in. Says which, so nobody mistakes it for a bug. */
export function Placeholder({ message }: { message: string }) {
  return (
    <View style={styles.screen}>
      <Text style={styles.text}>{message}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: color.paper, padding: space[5], justifyContent: 'center' },
  text: { fontSize: size.body, color: color.ink2, textAlign: 'center' },
});
