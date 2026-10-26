import { useEffect, useRef } from 'react'

/**
 * Native <dialog>: focus is trapped and Esc closes it. Mount it only while it should be open.
 * No close() in the effect cleanup: StrictMode runs effects twice in dev, and close() would fire a
 * "close" event that tells the parent to unmount the dialog right after it opened. Unmounting removes
 * the element (and its backdrop) on its own.
 */
export default function Modal({ title, onClose, children }) {
  const ref = useRef(null)

  useEffect(() => {
    const dialog = ref.current
    if (!dialog.open) dialog.showModal()
  }, [])

  return (
    <dialog ref={ref} className="modal" aria-labelledby="modal-title" onClose={onClose}>
      <h2 id="modal-title">{title}</h2>
      {children}
    </dialog>
  )
}
