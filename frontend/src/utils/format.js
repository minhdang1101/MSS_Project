export const formatDate = (value) => (value ? new Date(value).toLocaleDateString('vi-VN') : '-')

// 39000 -> "39.000đ", the way amounts are written at the counter
export const formatCurrency = (value) =>
  value == null ? '-' : `${Number(value).toLocaleString('vi-VN', { maximumFractionDigits: 0 })}đ`
