interface Props {
  label: string
  text: string
}

/** Echoes the Passenger's own words ("You wrote", "You asked") so they know what the screen is about. */
export function Quote({ label, text }: Props) {
  if (!text) return null
  return (
    <blockquote className="quote">
      <span className="quote__label">{label}</span>
      {/* lang="": the Passenger may write in any language, not necessarily the UI's. */}
      <span lang="">{text}</span>
    </blockquote>
  )
}
