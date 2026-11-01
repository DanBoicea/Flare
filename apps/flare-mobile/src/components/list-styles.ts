import { StyleSheet } from 'react-native';

import { color, font, radius, size, space } from '@/constants/theme';

/** Shared by the list screens (Active, Resolved, Needs review). */
export const listStyles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: color.page },
  list: { padding: space[4], gap: space[3] },
  mockNote: {
    fontSize: size.caption,
    fontFamily: font.semiBold,
    color: color.ink,
    backgroundColor: color.tint,
    borderRadius: radius.pill,
    overflow: 'hidden',
    alignSelf: 'flex-start',
    marginHorizontal: space[4],
    marginTop: space[3],
    paddingHorizontal: space[4],
    paddingVertical: 4,
  },
  banner: {
    fontSize: size.body,
    fontFamily: font.semiBold,
    color: color.paper,
    backgroundColor: color.navy,
    borderRadius: radius.control,
    overflow: 'hidden',
    marginHorizontal: space[4],
    marginTop: space[3],
    paddingHorizontal: space[4],
    paddingVertical: space[3],
  },
  message: { fontSize: size.body, fontFamily: font.regular, color: color.ink2, padding: space[5], textAlign: 'center' },
  footer: { fontSize: size.caption, fontFamily: font.regular, color: color.ink2, paddingVertical: space[2], paddingHorizontal: space[1] },
});
