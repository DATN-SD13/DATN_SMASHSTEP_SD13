// Public VNPAY sandbox examples. Only these test cards are accepted by the local simulator.
export const DEMO_HOLDER = 'NGUYEN VAN A'
export const DEMO_ISSUE_DATE = '07/15'
export const DEMO_OTP = '123456'
export const DEMO_CARDS = [
  { number: '9704198526191432198', label: 'Thanh toán thành công', result: 'OK' },
  { number: '9704195798459170488', label: 'Không đủ số dư', result: 'Thẻ thử không đủ số dư' },
  { number: '9704192181368742', label: 'Chưa kích hoạt', result: 'Thẻ thử chưa kích hoạt' },
  { number: '9704193370791314', label: 'Bị khóa', result: 'Thẻ thử bị khóa' },
  { number: '9704194841945513', label: 'Hết hạn', result: 'Thẻ thử đã hết hạn' }
]
export const normalizeDemoCard = value => String(value || '').replace(/ /g, '')
export function demoCardError(card) {
  if (!/^[0-9 ]{16,24}$/.test(card.number || '') || !DEMO_CARDS.some(c => c.number === normalizeDemoCard(card.number)))
    return 'Chỉ dùng số thẻ thử NCB được liệt kê bên dưới. Không nhập thẻ thật.'
  if (String(card.holder || '').trim().replace(/\s+/g, ' ').toUpperCase() !== DEMO_HOLDER)
    return 'Tên chủ thẻ thử phải là NGUYEN VAN A.'
  if (card.issueDate !== DEMO_ISSUE_DATE) return 'Ngày phát hành thẻ thử phải là 07/15 (tháng/năm).'
  return ''
}
export function demoCardResult(card) {
  return demoCardError(card) || DEMO_CARDS.find(c => c.number === normalizeDemoCard(card.number)).result
}
