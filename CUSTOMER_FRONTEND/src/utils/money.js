const formatter = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })
export function money(value) { return value !== null && value !== '' && value !== undefined && Number.isFinite(Number(value)) ? formatter.format(Number(value)) : '—' }
export function discountPercent(value) { const n = Number(value); return Number.isFinite(n) && n > 0 && n <= 100 ? String(Number(n.toFixed(2))) : '' }
export function hasDiscount(item) { return item?.dangGiamGia === true && Boolean(discountPercent(item.phanTramGiam)) }
