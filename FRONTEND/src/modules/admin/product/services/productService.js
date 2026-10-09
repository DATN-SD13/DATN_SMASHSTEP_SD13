import api from '../../../../utils/api'
import { attributeTypes } from './productAttributeService'

export const productService = {
  list: params => api.get('/products', { params: cleanParams(params) }).then(r => r.data),
  nextCode: async () => {
    let highest = 0n
    let page = 0, totalPages = 1
    do {
      const result = await productService.list({ page, size: 100 })
      for (const product of result.content) {
        const match = /^SP(\d+)$/i.exec(String(product.maSanPham || '').trim())
        if (match && BigInt(match[1]) > highest) highest = BigInt(match[1])
      }
      if (page === 0) totalPages = result.totalPages
      page++
    } while (page < totalPages)
    const code = 'SP' + String(highest + 1n).padStart(3, '0')
    if (code.length > 50) throw new Error('Không thể sinh mã sản phẩm tiếp theo.')
    return code
  },
  get: id => api.get(`/products/${id}`).then(r => r.data),
  checkDuplicate: data => api.post('/products/check-duplicate', data).then(r => r.data),
  create: data => api.post('/products', data).then(r => r.data),
  update: (id, data) => api.put(`/products/${id}`, data).then(r => r.data),
  status: (id, trangThai) => api.patch(`/products/${id}/status`, { trangThai }).then(r => r.data),
  createVariants: (id, variants) => api.post(`/products/${id}/variants`, { variants }).then(r => r.data),
  addImage: (id, data) => api.post(`/products/${id}/images`, data).then(r => r.data),
  uploadImage: (id, image) => {
    const form = new FormData()
    form.append('file', image.file)
    form.append('isAnhChinh', String(image.isAnhChinh))
    return api.post(`/products/${id}/images/upload`, form, {
      headers: { 'Content-Type': undefined }, timeout: 30000
    }).then(r => r.data)
  },
  updateImage: (id, imageId, data) => api.put(`/products/${id}/images/${imageId}`, data).then(r => r.data),
  removeImage: (id, imageId) => api.delete(`/products/${id}/images/${imageId}`)
}

export const variantService = {
  images: id => api.get(`/product-details/${id}/images`).then(r => r.data),
  uploadImage: (id, image) => {
    const form = new FormData()
    form.append('file', image.file)
    form.append('isAnhChinh', String(image.isAnhChinh))
    return api.post(`/product-details/${id}/images/upload`, form, {
      headers: { 'Content-Type': undefined }, timeout: 30000
    }).then(r => r.data)
  },
  updateImage: (id, imageId, data) => api.put(`/product-details/${id}/images/${imageId}`, data).then(r => r.data),
  removeImage: (id, imageId) => api.delete(`/product-details/${id}/images/${imageId}`),
  list: params => api.get('/product-details', { params }).then(r => r.data),
  update: (id, data) => api.put(`/product-details/${id}`, data).then(r => r.data),
  status: (id, trangThai) => api.patch(`/product-details/${id}/status`, { trangThai }).then(r => r.data)
}

export function errorMessage(error) {
  const message = error?.response?.data?.message
  return typeof message === 'string' && message.trim() ? message : 'Đã xảy ra lỗi. Vui lòng thử lại.'
}

export function productSummary(data, options) {
  return [
    { label: 'Mã sản phẩm', value: data.maSanPham },
    { label: 'Tên sản phẩm', value: data.tenSanPham },
    ...attributeTypes.filter(type => type.field).map(type => ({ label: type.label,
      value: data['ten' + type.field[0].toUpperCase() + type.field.slice(1, -2)]
        || options?.[type.key]?.find(item => item.id === data[type.field])?.ten || '—' })),
    { label: 'Trạng thái', value: data.trangThai === 1 ? 'Hoạt động' : 'Ngừng hoạt động' }
  ]
}

export function validateVariant(row) {
  if (![row.maChiTietSanPham, row.sku].every(value => typeof value === 'string' && value.trim().length <= 100 && /^[A-Za-z0-9][A-Za-z0-9._-]*$/.test(value.trim())))
    return 'Mã biến thể và SKU phải có tối đa 100 ký tự, chỉ gồm chữ Latin, số, dấu chấm, gạch dưới hoặc gạch ngang.'
  if (![row.mauSacId, row.kichThuocId].every(value => Number.isInteger(Number(value)) && Number(value) > 0))
    return 'Vui lòng chọn màu sắc và kích thước.'
  if (row.soLuong === '' || row.soLuong == null || !Number.isInteger(Number(row.soLuong)) || Number(row.soLuong) < 0 || Number(row.soLuong) > 2147483647)
    return 'Số lượng phải là số nguyên từ 0 đến 2147483647.'
  if (row.giaBan === '' || row.giaBan == null || !Number.isFinite(Number(row.giaBan)) || Number(row.giaBan) <= 0 || !/^\d{1,16}(\.\d{1,2})?$/.test(String(row.giaBan)))
    return 'Giá bán phải lớn hơn 0, có tối đa 16 chữ số phần nguyên và 2 chữ số thập phân.'
  if (![0, 1].includes(Number(row.trangThai)) || typeof row.kichHoat !== 'boolean') return 'Trạng thái hoặc kích hoạt không hợp lệ.'
  return ''
}
const currencyFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: 'VND', minimumFractionDigits: 0, maximumFractionDigits: 2
})
export function money(value) {
  if (value == null || !Number.isFinite(Number(value))) return 'Chưa có giá'
  return currencyFormatter.format(Number(value))
}
export function priceRange(product) {
  if (product.giaThapNhat == null || product.giaCaoNhat == null) return 'Chưa có giá'
  const min = Number(product.giaThapNhat)
  const max = Number(product.giaCaoNhat)
  if (!Number.isFinite(min) || !Number.isFinite(max)) return 'Chưa có giá'
  return min === max ? money(min) : `${money(min)} - ${money(max)}`
}
export function dateTime(value) {
  return value ? new Date(value).toLocaleString('vi-VN') : '—'
}
export function cleanParams(params = {}) {
  return Object.fromEntries(Object.entries(params)
    .map(([key, value]) => [key, typeof value === 'string' ? value.trim() : value])
    .filter(([, value]) => value !== '' && value != null))
}
