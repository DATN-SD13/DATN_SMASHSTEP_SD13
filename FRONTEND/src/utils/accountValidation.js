// Keep these format rules aligned with BACKEND AccountRules and the admin copy.
const emailTypos = { 'gmai.com': 'gmail.com', 'gmial.com': 'gmail.com', 'gamil.com': 'gmail.com', 'gmail.con': 'gmail.com', 'gmail.comm': 'gmail.com', 'yahoo.con': 'yahoo.com', 'hotmial.com': 'hotmail.com', 'outlok.com': 'outlook.com' }
const text = value => typeof value === 'string' ? value.trim() : ''
const controls = /[\u0000-\u001f\u007f-\u009f]/

export function emailError(raw) {
  const value = text(raw)
  if (!value) return 'Vui lòng nhập email.'
  if (value.length > 254 || !/^[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$/.test(value)) return 'Email không đúng định dạng. Ví dụ: ten@gmail.com; không có khoảng trắng, hai dấu @ hoặc dấu chấm liên tiếp.'
  const [local, domainPart] = value.split('@'), domain = domainPart.toLowerCase(), labels = domain.split('.')
  if (local.length > 64 || labels.some(label => label.length > 63) || !/^[a-z]{2,63}$|^xn--[a-z0-9-]{2,59}$/.test(labels.at(-1))) return 'Tên hoặc tên miền email không đúng định dạng.'
  return emailTypos[domain] ? `Tên miền email có thể bị gõ nhầm. Bạn có muốn dùng ${emailTypos[domain]}? Vui lòng nhập lại.` : ''
}
export function nameError(raw) {
  const value = text(raw)
  return value.length <= 255 && (value.match(/\p{L}/gu) || []).length >= 2 && /^[\p{L}\p{M}]+(?:[ '.-][\p{L}\p{M}]+)*\.?$/u.test(value) && !controls.test(raw)
    ? '' : 'Họ tên cần ít nhất 2 chữ cái; không chứa số, biểu tượng hoặc ký tự đặc biệt.'
}
export function phoneError(raw, required = true) {
  const value = text(raw)
  if (!value) return required ? 'Vui lòng nhập số điện thoại.' : ''
  return /^0[35789][0-9]{8}$/.test(value) ? '' : 'Số điện thoại gồm 10 chữ số, bắt đầu bằng 03, 05, 07, 08 hoặc 09.'
}
export function loginError(raw, password) {
  const value = text(raw)
  if (!value) return 'Vui lòng nhập tên đăng nhập hoặc email.'
  const invalid = value.includes('@') ? emailError(value) : /^[A-Za-z0-9_][A-Za-z0-9_.-]{0,63}$/.test(value) ? '' : 'Tên đăng nhập tối đa 64 ký tự, chỉ gồm chữ không dấu, số, dấu chấm, gạch dưới hoặc gạch ngang.'
  if (invalid) return invalid
  if (typeof password !== 'string' || !password.trim()) return 'Vui lòng nhập mật khẩu.'
  return password.length > 100 ? 'Mật khẩu đăng nhập tối đa 100 ký tự.' : ''
}
export function passwordError(raw) {
  if (typeof raw !== 'string' || raw.length < 6 || new TextEncoder().encode(raw).length > 72) return 'Mật khẩu cần ít nhất 6 ký tự và không quá dài (tối đa 72 ký tự không dấu).'
  if (raw !== raw.trim() || controls.test(raw)) return 'Mật khẩu không được có khoảng trắng ở đầu/cuối hoặc ký tự xuống dòng.'
  return /\p{L}/u.test(raw) && /\p{Nd}/u.test(raw) ? '' : 'Mật khẩu phải có ít nhất một chữ cái và một chữ số.'
}
export function birthDateError(raw, minAge, required = false) {
  if (!raw) return required ? 'Vui lòng nhập ngày sinh.' : ''
  if (typeof raw !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(raw)) return 'Ngày sinh không đúng định dạng.'
  const date = new Date(`${raw}T00:00:00`), today = new Date()
  if (Number.isNaN(date.getTime()) || date.getFullYear() !== Number(raw.slice(0, 4)) || date.getMonth() + 1 !== Number(raw.slice(5, 7)) || date.getDate() !== Number(raw.slice(8, 10)) || date.getFullYear() < 1900) return 'Ngày sinh không hợp lệ, phải từ năm 1900.'
  const year = today.getFullYear() - minAge, month = today.getMonth()
  const min = new Date(year, month, Math.min(today.getDate(), new Date(year, month + 1, 0).getDate()))
  return date > min ? `Phải đủ ${minAge} tuổi trở lên.` : ''
}
function addressErrors(value) {
  if (!value || typeof value !== 'object') return 'Vui lòng nhập địa chỉ nhận hàng.'
  const invalid = nameError(value.receiverName) || phoneError(value.receiverPhone)
  if (invalid) return invalid
  for (const [field, label, max] of [['province', 'Tỉnh/Thành phố', 100], ['ward', 'Phường/Xã', 100], ['street', 'Địa chỉ cụ thể', 500]]) {
    if (!text(value[field]) || value[field].length > max || /[<>]/.test(value[field]) || controls.test(value[field])) return `${label} không được trống, vượt ${max} ký tự hoặc chứa ký tự không hợp lệ.`
  }
  if (value.district && (value.district.length > 100 || /[<>]/.test(value.district) || controls.test(value.district))) return 'Quận/Huyện không hợp lệ.'
  if (value.addressType != null && ![1, 2].includes(value.addressType)) return 'Loại địa chỉ không hợp lệ.'
  return ''
}
export function accountRequestError(config) {
  if (!['post', 'put', 'patch'].includes((config.method || '').toLowerCase()) || !config.data || typeof config.data !== 'object' || config.data instanceof FormData) return ''
  const data = config.data, url = (config.url || '').replace(/^\/api/, '').split('?')[0]
  if (url === '/auth/login') return loginError(data.username, data.password)
  if (url === '/auth/quen-mat-khau') return emailError(data.email)
  if (url === '/auth/dat-lai-mat-khau') return /^[A-Za-z0-9_-]{32,128}$/.test(data.token || '') ? passwordError(data.newPassword) : 'Link đặt lại mật khẩu không hợp lệ.'
  if (url === '/auth/dang-ky' || url === '/auth/register') return nameError(data.name) || emailError(data.email) || phoneError(data.phone, false) || passwordError(data.password)
  if (url === '/customer/account/password') return !data.currentPassword?.trim() ? 'Vui lòng nhập mật khẩu hiện tại.' : passwordError(data.newPassword) || (data.newPassword !== data.confirmPassword ? 'Mật khẩu xác nhận không khớp.' : '')
  if (url === '/nhan-vien/me/doi-mat-khau') return !data.matKhauCu?.trim() ? 'Vui lòng nhập mật khẩu hiện tại.' : passwordError(data.matKhauMoi) || (data.matKhauCu === data.matKhauMoi ? 'Mật khẩu mới không được trùng mật khẩu hiện tại.' : '')
  if (/^\/customer\/account\/addresses(?:\/\d+)?$/.test(url) || /^\/khach-hang\/[^/]+\/dia-chi(?:\/\d+)?$/.test(url)) return addressErrors(data)
  const employee = /^\/nhan-vien(?:\/[^/]+)?$/.test(url), customer = /^\/khach-hang(?:\/[^/]+)?$/.test(url), profile = url === '/customer/account'
  if (employee || customer || profile) {
    const create = config.method.toLowerCase() === 'post'
    const invalid = nameError(data.name) || (!profile ? emailError(data.email) : '') || phoneError(data.phone, employee || (customer && create)) || birthDateError(data.dob, employee ? 18 : 15, employee || (customer && create))
    if (invalid) return invalid
    if ((employee && data.gender == null) || (data.gender != null && ![0, 1, 2].includes(data.gender))) return 'Giới tính không hợp lệ.'
    if (employee && (!Number.isInteger(data.roleId) || data.roleId < 1)) return 'Vui lòng chọn vai trò hợp lệ.'
    if (employee) {
      for (const [field, label, max] of [['province', 'Tỉnh/Thành phố', 100], ['ward', 'Phường/Xã', 100], ['street', 'Địa chỉ cụ thể', 500]]) {
        if (data[field] && (typeof data[field] !== 'string' || data[field].length > max || /[<>]/.test(data[field]) || controls.test(data[field]))) return `${label} tối đa ${max} ký tự và không chứa ký tự không hợp lệ.`
      }
    }
    if (customer && create && !data.defaultAddress) return 'Vui lòng nhập địa chỉ mặc định.'
    if (data.defaultAddress) return addressErrors(data.defaultAddress)
  }
  return ''
}
export function enforceAccountRequest(config) {
  const message = accountRequestError(config)
  if (message) {
    const error = new Error(message)
    error.isClientValidation = true
    error.response = { status: 400, data: { success: false, message } }
    throw error
  }
  return config
}
