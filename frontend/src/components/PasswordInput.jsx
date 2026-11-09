import { useState } from 'react'

/** A password box with a button that shows or hides what was typed. Takes the same props as <input>. */
export default function PasswordInput({ id, ...input }) {
  const [visible, setVisible] = useState(false)

  return (
    <div className="pw-wrap">
      <input id={id} type={visible ? 'text' : 'password'} {...input} />
      <button type="button" className="pw-toggle" onClick={() => setVisible((v) => !v)}
        aria-pressed={visible} aria-controls={id}>
        {visible ? 'Ẩn' : 'Hiện'}
      </button>
    </div>
  )
}
