interface Props {
  label: string
  text: string
  /** Plain text instead of a white box, when it sits above a card that is the real subject (design 03). */
  plain?: boolean
}

/** Echoes the Passenger's own words ("You wrote", "You asked") so they know what the screen is about. */
export function Quote({ label, text, plain = false }: Props) {
  if (!text) return null
  return (
    <blockquote className={plain ? 'quote quote--plain' : 'quote'}>
      <span className="quote__label">{label}</span>
      {/* lang="": the Passenger may write in any language, not necessarily the UI's. */}
      <span lang="">{text}</span>
    </blockquote>
  )
}
