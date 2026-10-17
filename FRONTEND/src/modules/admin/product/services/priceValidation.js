const messages = {
  empty: 'Giá bán không được để trống.',
  positive: 'Giá bán phải lớn hơn 0.',
  format: 'Giá bán không đúng định dạng.',
  integer: 'Giá bán tối đa 16 chữ số phần nguyên.',
  fraction: 'Giá bán tối đa 2 chữ số thập phân.'
}
function expandNumber(value) {
  const text = String(value)
  if (!/[eE]/.test(text)) return text
  const [mantissa, exponent] = text.toLowerCase().split('e')
  const sign = mantissa.startsWith('-') ? '-' : ''
  const unsigned = mantissa.replace(/^-/, ''), digits = unsigned.replace('.', '')
  const point = (unsigned.indexOf('.') < 0 ? unsigned.length : unsigned.indexOf('.')) + Number(exponent)
  return sign + (point <= 0 ? '0.' + '0'.repeat(-point) + digits : point >= digits.length ? digits + '0'.repeat(point - digits.length) : digits.slice(0, point) + '.' + digits.slice(point))
}
function normalizeDecimal(text) {
  const [integer, fraction = ''] = text.split('.')
  const cleanFraction = fraction.replace(/0+$/, '')
  return integer.replace(/^0+(?=\d)/, '') + (cleanFraction ? '.' + cleanFraction : '')
}
export function parseCanonicalPriceValue(canonical) {
  if (typeof canonical !== 'string' || !/^-?\d+(?:\.\d+)?$/.test(canonical)) return null
  const value = Number(canonical)
  // Only use a JSON number when its decimal spelling survives the round trip.
  // Larger values stay canonical decimal strings, accepted by the BigDecimal DTO.
  return Number.isSafeInteger(Number(canonical.split('.')[0])) && normalizeDecimal(expandNumber(value)) === normalizeDecimal(canonical) ? value : canonical
}
export function validatePriceValue(value, { raw = false, required = true } = {}) {
  const fail = code => ({ valid: false, code, message: messages[code], value: null })
  if (value == null || (typeof value === 'string' && !value.trim()))
    return required ? { ...fail('empty'), empty: true } : { valid: true, empty: true, message: '', value: null }
  if (typeof value !== 'string' && typeof value !== 'number') return fail('format')
  if (typeof value === 'number' && !Number.isFinite(value)) return fail('format')
  const text = typeof value === 'number' ? expandNumber(value) : value.trim()
  const pattern = raw ? /^-?(?:\d+|\d{1,3}(?:\.\d{3})+)(?:,\d+)?$/ : /^-?\d+(?:\.\d+)?$/
  if (!pattern.test(text)) return fail('format')
  const canonical = (raw ? text.replaceAll('.', '').replace(',', '.') : text).replace(/^(-?)0+(?=\d)/, '$1')
  if (canonical.startsWith('-') || !/[1-9]/.test(canonical)) return fail('positive')
  const [integer, fraction = ''] = canonical.split('.')
  if (integer.length > 16) return fail('integer')
  if (fraction.length > 2) return fail('fraction')
  return { valid: true, message: '', canonical, value: parseCanonicalPriceValue(canonical) }
}
export function formatPriceValue(value) {
  if (value == null || value === '') return ''
  if (typeof value === 'number' && !Number.isFinite(value)) return ''
  const canonical = typeof value === 'number' ? expandNumber(value) : String(value)
  if (!/^-?\d+(?:\.\d+)?$/.test(canonical)) return ''
  const [integer, fraction] = canonical.split('.')
  return integer.replace(/\B(?=(\d{3})+(?!\d))/g, '.') + (fraction == null ? '' : ',' + fraction)
}
export function priceValidationNotice(errors, priceField = 'giaBan') {
  if (Object.keys(errors).length === 1 && errors[priceField]) {
    return errors[priceField] === messages.empty
      ? { title: 'Vui lòng kiểm tra dữ liệu', message: messages.empty }
      : { title: 'Giá bán không hợp lệ', message: 'Vui lòng nhập giá bán lớn hơn 0, ví dụ 1.250.000.' }
  }
  return { title: 'Vui lòng kiểm tra dữ liệu', message: 'Vui lòng kiểm tra các trường bắt buộc.' }
}
export function parsePriceResponse(json) {
  if (typeof json !== 'string') return json
  // Restrict precision preservation to Product base-price fields; other API data is unchanged.
  const preserved = json.replace(/"(giaBan|giaThapNhat|giaCaoNhat|giaSauGiam|giaSauGiamThapNhat|giaSauGiamCaoNhat)"\s*:\s*(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)(?=\s*[,}\]])/g, (token, field, decimal) => {
    const result = validatePriceValue(decimal)
    return result.valid && typeof result.value === 'string' ? '"' + field + '":' + JSON.stringify(result.value) : token
  })
  return JSON.parse(preserved)
}
