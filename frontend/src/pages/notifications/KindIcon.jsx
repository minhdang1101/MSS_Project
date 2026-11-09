/** A round icon for the kind of notification: a clock for "due soon", an exclamation mark for "overdue". */
export default function KindIcon({ type }) {
  return (
    <span className={`kind-icon ${type === 'OVERDUE' ? 'late' : 'soon'}`} aria-hidden="true">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2"
        strokeLinecap="round" strokeLinejoin="round">
        {type === 'OVERDUE' ? (
          <>
            <path d="M12 3 2.5 20h19L12 3Z" />
            <path d="M12 10v4" />
            <path d="M12 17.5h.01" />
          </>
        ) : (
          <>
            <circle cx="12" cy="12" r="9" />
            <path d="M12 7v5l3 2" />
          </>
        )}
      </svg>
    </span>
  )
}
