import { Modal, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { color, font, radius, size, space, tap } from '@/constants/theme';
import { useI18n } from '@/i18n/context';

export interface PickerOption<T> {
  value: T;
  label: string;
}

interface Props<T> {
  visible: boolean;
  title: string;
  options: PickerOption<T>[];
  /** Marked as the current value. */
  selected?: T;
  onPick: (value: T) => void;
  onClose: () => void;
}

/**
 * A bottom sheet with a list of choices (Team, category, Severity), built on React Native's Modal, so no new
 * library (ADR-0013). Android's back button closes it.
 */
export function PickerSheet<T>({ visible, title, options, selected, onPick, onClose }: Props<T>) {
  const { t } = useI18n();
  const insets = useSafeAreaInsets();

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={styles.backdrop} onPress={onClose} accessibilityLabel={t('picker.close')} />
      <View style={[styles.sheet, { paddingBottom: insets.bottom + space[4] }]}>
        <Text style={styles.title} accessibilityRole="header">{title}</Text>
        <ScrollView contentContainerStyle={styles.options}>
          {options.map((option) => {
            const isSelected = option.value === selected;
            return (
              <Pressable
                key={String(option.value)}
                onPress={() => onPick(option.value)}
                accessibilityRole="button"
                accessibilityState={{ selected: isSelected }}
                style={({ pressed }) => [styles.option, isSelected && styles.optionSelected, pressed && styles.optionPressed]}>
                <Text style={styles.optionText}>{option.label}</Text>
                {isSelected && <Text style={styles.current}>{t('picker.current')}</Text>}
              </Pressable>
            );
          })}
        </ScrollView>
        <Pressable onPress={onClose} accessibilityRole="button" style={({ pressed }) => [styles.close, pressed && styles.optionPressed]}>
          <Text style={styles.closeText}>{t('picker.close')}</Text>
        </Pressable>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(11, 37, 69, 0.4)' },
  sheet: {
    backgroundColor: color.paper,
    borderTopLeftRadius: radius.header,
    borderTopRightRadius: radius.header,
    paddingTop: space[5],
    paddingHorizontal: space[4],
    gap: space[3],
    maxHeight: '75%',
  },
  title: { fontSize: size.rowTitle, fontFamily: font.bold, color: color.ink, paddingHorizontal: space[1] },
  options: { gap: space[2] },
  option: {
    minHeight: tap.main,
    borderRadius: radius.control,
    borderWidth: 1.5,
    borderColor: color.line,
    paddingHorizontal: space[4],
    paddingVertical: space[3],
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: space[2],
  },
  optionSelected: { borderColor: color.navy, backgroundColor: color.tint },
  optionPressed: { backgroundColor: color.tint },
  optionText: { fontSize: size.lead, fontFamily: font.semiBold, color: color.ink, flexShrink: 1 },
  current: { fontSize: size.caption, fontFamily: font.regular, color: color.ink2 },
  close: {
    minHeight: tap.main,
    borderRadius: radius.control,
    borderWidth: 2,
    borderColor: color.navy,
    alignItems: 'center',
    justifyContent: 'center',
  },
  closeText: { fontSize: size.lead, fontFamily: font.semiBold, color: color.ink },
});
