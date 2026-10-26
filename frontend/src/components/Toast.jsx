import { useEffect } from 'react'

/** A short message that dismisses itself. `notice` is { text, kind: 'ok' | 'error' } or null. */
export default function Toast({ notice, onDone, duration = 5000 }) {
  useEffect(() => {
    if (!notice) return undefined
    const timer = setTimeout(onDone, duration)
    return () => clearTimeout(timer)
  }, [notice, onDone, duration])

  return (
    <div className={`toast ${notice ? `on ${notice.kind}` : ''}`} role="status" aria-live="polite">
      {notice?.text}
    </div>
  )
}
