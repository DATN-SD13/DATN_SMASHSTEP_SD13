import { formatPriceValue, parseCanonicalPriceValue } from './priceValidation'

export function formatNumber(value, currency = false) {
  if (currency) return formatPriceValue(value)
  if (value == null || value === '' || !Number.isFinite(Number(value))) return ''
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(Number(value))
}
export function parseNumber(text, currency = false) {
  if (currency) {
    const raw = String(text ?? '').trim()
    if (!raw) return { value: null, empty: true, valid: true }
    if (!/^(?:\d+|\d{1,3}(?:\.\d{3})+)(?:,\d{1,2})?$/.test(raw)) return { value: null, valid: false }
    const canonical = raw.replaceAll('.', '').replace(',', '.')
    return { valid: true, canonical, value: parseCanonicalPriceValue(canonical) }
  }
  const raw = String(text ?? '').trim()
  if (!raw) return { value: null, empty: true, valid: true }
  if (!/^(?:\d+|\d{1,3}(?:\.\d{3})+)$/.test(raw)) return { value: null, valid: false }
  const value = Number(raw.replaceAll('.', ''))
  if (!Number.isSafeInteger(value)) return { value: null, valid: false }
  return { value, valid: true }
}

export function formatCurrencyWhileTyping(raw) {
  const text = String(raw ?? '').trim()
  // Accept grouping dots independently of InputEvent metadata. Keep malformed text
  // (letters, negatives or multiple commas) for blur/submit to reject.
  if (!/^[\d.]+(?:,\d*)?$/.test(text)) return String(raw ?? '')
  const [integerRaw, fractionRaw] = text.split(',')
  const digits = integerRaw.replaceAll('.', '').replace(/^0+(?=\d)/, '')
  const integer = formatPriceValue(digits)
  return fractionRaw == null ? integer : integer + ',' + fractionRaw.slice(0, 2)
}
