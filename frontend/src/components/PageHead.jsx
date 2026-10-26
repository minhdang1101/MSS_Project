export default function PageHead({ title, subtitle, children }) {
  return (
    <div className="head">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="sub">{subtitle}</p>}
      </div>
      {children}
    </div>
  )
}
