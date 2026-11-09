import { useEffect, useId, useRef, useState } from 'react'

/** The bell outline used on the button and in the empty state. */
export function BellIcon({ size = 22 }) {
  return (
    <svg viewBox="0 0 24 24" width={size} height={size} fill="none" stroke="currentColor" strokeWidth="1.9"
      strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      <path d="M6 8a6 6 0 1 1 12 0c0 7 3 8.5 3 8.5H3S6 15 6 8Z" />
      <path d="M10.3 20a1.9 1.9 0 0 0 3.4 0" />
    </svg>
  )
}

/**
 * A bell button with a red count, opening a panel under it. Closes on a click outside, on Esc, and when a link
 * inside calls close(). The bell rings once whenever the count goes up (a new notification arrived).
 * children is a function: ({ close }) => panel content.
 */
export default function BellMenu({ count = 0, label, title, actions, footer, children }) {
  const [open, setOpen] = useState(false)
  const [ringing, setRinging] = useState(false)
  const rootRef = useRef(null)
  const buttonRef = useRef(null)
  const previous = useRef(count)
  const panelId = useId()

  useEffect(() => {
    if (count > previous.current) setRinging(true)
    previous.current = count
  }, [count])

  useEffect(() => {
    if (!open) return undefined
    const onPointer = (e) => {
      if (!rootRef.current?.contains(e.target)) setOpen(false)
    }
    const onKey = (e) => {
      if (e.key === 'Escape') {
        setOpen(false)
        buttonRef.current?.focus()
      }
    }
    document.addEventListener('mousedown', onPointer)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onPointer)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  const close = () => setOpen(false)
  const shown = count > 99 ? '99+' : count

  return (
    <div className="bell" ref={rootRef}>
      <button ref={buttonRef} type="button" className={`bell-btn${ringing ? ' ringing' : ''}${open ? ' open' : ''}`}
        aria-label={count > 0 ? `${label}, ${count} mới` : label} aria-expanded={open} aria-controls={panelId}
        onClick={() => setOpen((o) => !o)} onAnimationEnd={() => setRinging(false)}>
        <BellIcon />
        {count > 0 && <span className="bell-badge" aria-hidden="true">{shown}</span>}
      </button>

      {open && (
        <div className="bell-panel" id={panelId} role="dialog" aria-label={title}>
          <div className="bell-head">
            <h2>{title}</h2>
            {actions}
          </div>
          <div className="bell-body">{children({ close })}</div>
          {footer && <div className="bell-foot">{footer({ close })}</div>}
        </div>
      )}
    </div>
  )
}
