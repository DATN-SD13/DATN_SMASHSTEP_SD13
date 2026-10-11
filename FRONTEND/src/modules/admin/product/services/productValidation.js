import { validatePriceValue } from './priceValidation'
const codePattern = /^[A-Za-z0-9][A-Za-z0-9._-]*$/
export const normalizeName = value => String(value ?? '').trim().replace(/\s+/gu, ' ').toLocaleLowerCase('vi-VN')
export function validateCode(value, label, max = 100) {
  if (!String(value ?? '').trim()) return label + ' không được để trống.'
  if (String(value).trim().length > max || !codePattern.test(String(value).trim())) return label + ' tối đa ' + max + ' ký tự, chỉ gồm chữ Latin, số, dấu chấm, gạch dưới hoặc gạch ngang.'
  return ''
}
export function validateVariantFields(row) {
  const errors = {}
  for (const [field, label] of [['maChiTietSanPham', 'Mã biến thể'], ['sku', 'SKU']]) {
    const message = validateCode(row[field], label)
    if (message) errors[field] = message
  }
  for (const [field, label] of [['mauSacId', 'màu sắc'], ['kichThuocId', 'kích thước']])
    if (!Number.isInteger(Number(row[field])) || Number(row[field]) <= 0) errors[field] = 'Vui lòng chọn ' + label + '.'
  if (row.soLuong == null || row.soLuong === '' || !Number.isInteger(Number(row.soLuong)) || Number(row.soLuong) < 0 || Number(row.soLuong) > 2147483647)
    errors.soLuong = 'Số lượng phải là số nguyên từ 0 đến 2.147.483.647.'
  const price = validatePriceValue(row.giaBan)
  if (!price.valid) errors.giaBan = price.message
  if (row.trangThai == null || row.trangThai === '' || ![0, 1].includes(Number(row.trangThai))) errors.trangThai = 'Vui lòng chọn trạng thái hợp lệ.'
  if (typeof row.kichHoat !== 'boolean') errors.kichHoat = 'Vui lòng chọn kích hoạt.'
  return errors
}
export function duplicateVariantFields(row, variants, excludeId) {
  const errors = {}, others = variants.filter(v => excludeId == null || v.id !== excludeId)
  if (others.some(v => normalizeName(v.maChiTietSanPham) === normalizeName(row.maChiTietSanPham))) errors.maChiTietSanPham = 'Mã biến thể đã tồn tại.'
  if (others.some(v => normalizeName(v.sku) === normalizeName(row.sku))) errors.sku = 'SKU đã tồn tại.'
  if (others.some(v => Number(v.sanPhamId) === Number(row.sanPhamId) && Number(v.mauSacId) === Number(row.mauSacId) && Number(v.kichThuocId) === Number(row.kichThuocId))) {
    errors.mauSacId = errors.kichThuocId = 'Tổ hợp sản phẩm, màu sắc và kích thước đã tồn tại.'
  }
  return errors
}
export function apiFieldErrors(error) {
  const response = error?.response?.data
  if (response?.errors) return response.errors
  const message = response?.message || ''
  const fields = [['SKU', 'sku'], ['Mã chi tiết', 'maChiTietSanPham'], ['Mã biến thể', 'maChiTietSanPham'], ['Mã sản phẩm', 'maSanPham'], ['HEX', 'maMauHex'], ['Sản phẩm với tên', 'tenSanPham'], ['Tên sản phẩm', 'tenSanPham'], ['Tên thuộc tính', 'ten'], ['Giá trị kích thước', 'ten'], ['Tổ hợp', 'kichThuocId'], ['Thuộc tính đã tồn tại', 'ten']]
  const match = fields.find(([label]) => message.toLowerCase().includes(label.toLowerCase()))
  return match ? { [match[1]]: message } : {}
}
