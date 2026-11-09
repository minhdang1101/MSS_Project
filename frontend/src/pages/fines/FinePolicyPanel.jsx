import { formatCurrency } from '../../utils/format.js'

/** The rules behind every amount in the list, read from the policy in effect today. */
export default function FinePolicyPanel({ policy }) {
  return (
    <aside className="panel policy" aria-labelledby="policy-title">
      <h2 id="policy-title">Cách tính phạt</h2>
      {policy ? (
        <dl>
          <div><dt>Mức phạt mỗi ngày</dt><dd>{formatCurrency(policy.dailyRate)}</dd></div>
          <div><dt>Ngày ân hạn</dt><dd>{policy.graceDays} ngày</dd></div>
          <div>
            <dt>Mức trần mỗi lượt mượn</dt>
            <dd>{policy.maxAmount == null ? 'Không giới hạn' : formatCurrency(policy.maxAmount)}</dd>
          </div>
          <div><dt>Sách điện tử</dt><dd>Không phạt</dd></div>
        </dl>
      ) : (
        <p className="error">Không tải được chính sách phạt.</p>
      )}
      <p className="policy-note">
        Ngày trễ được đếm từ hạn trả đến ngày nhận lại sách. Tiền phạt = (số ngày trễ - ngày ân hạn) x mức phạt mỗi ngày, không quá mức trần.
      </p>
    </aside>
  )
}
