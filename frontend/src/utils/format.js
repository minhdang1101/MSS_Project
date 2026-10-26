export const formatDate = (value) => (value ? new Date(value).toLocaleDateString('vi-VN') : '-')

export const formatCurrency = (value) =>
  value == null ? '-' : Number(value).toLocaleString('vi-VN', { style: 'currency', currency: 'VND' })
