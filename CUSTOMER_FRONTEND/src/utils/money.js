const formatter = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })
export function money(value) {
  if (typeof value === 'string' && /^\d+(?:\.\d+)?$/.test(value)) {
    const [whole, fraction = ''] = value.split('.')
    const rounded = BigInt(whole) + (fraction[0] >= '5' ? 1n : 0n)
    return new Intl.NumberFormat('vi-VN').format(rounded) + '\u00a0₫'
  }
  return value !== null && value !== '' && value !== undefined && Number.isFinite(Number(value)) ? formatter.format(Number(value)) : '—'
}
export function discountPercent(value) { const n = Number(value); return Number.isFinite(n) && n > 0 && n <= 100 ? String(Number(n.toFixed(2))) : '' }
export function hasDiscount(item) { return item?.dangGiamGia === true && Boolean(discountPercent(item.phanTramGiam)) }
