import { useId } from 'react'
import type { UiLanguage } from '../api/types'

/**
 * Flag of the country most associated with a UI language: Romania for Romanian, the United Kingdom
 * for English. Decorative: it always sits next to the language code ("RO", "EN"), because a flag is a
 * country, not a language (decisions round 8).
 */
export function Flag({ language }: { language: UiLanguage }) {
  // useId may contain characters that break url(#…) references; keep letters, digits, _ and -.
  const id = `flag${useId().replace(/[^a-zA-Z0-9_-]/g, '')}`
  return (
    <svg className="flag" width="28" height="20" viewBox="0 0 60 40" aria-hidden="true" focusable="false">
      {language === 'ro' ? (
        <>
          <rect width="20" height="40" fill="#002B7F" />
          <rect x="20" width="20" height="40" fill="#FCD116" />
          <rect x="40" width="20" height="40" fill="#CE1126" />
        </>
      ) : (
        // Union Jack, drawn on a 60x30 grid and stretched to 3:2 so both flags have the same size.
        <g transform="scale(1 1.3333)">
          <clipPath id={`${id}-t`}>
            <path d="M30,15 h30 v15 z v15 h-30 z h-30 v-15 z v-15 h30 z" />
          </clipPath>
          <rect width="60" height="30" fill="#012169" />
          <path d="M0,0 L60,30 M60,0 L0,30" stroke="#fff" strokeWidth="6" />
          <path d="M0,0 L60,30 M60,0 L0,30" clipPath={`url(#${id}-t)`} stroke="#C8102E" strokeWidth="4" />
          <path d="M30,0 v30 M0,15 h60" stroke="#fff" strokeWidth="10" />
          <path d="M30,0 v30 M0,15 h60" stroke="#C8102E" strokeWidth="6" />
        </g>
      )}
    </svg>
  )
}
