import { parsePriceResponse, formatPriceValue } from './priceValidation'
import { validateVariantFields } from './productValidation'
import api from '../../../../utils/api'
import { attributeTypes } from './productAttributeService'

const priceResponse = { transformResponse: [parsePriceResponse] }

export const productService = {
  list: params => api.get('/products', { ...priceResponse, params: cleanParams(params) }).then(r => r.data),
  codeExists: async (code, excludeId) => {
    let page = 0, result
    do {
      result = await productService.list({ keyword: code, page: page++, size: 100 })
      if (result.content.some(item => item.id !== excludeId && item.maSanPham?.trim().toLowerCase() === code.trim().toLowerCase())) return true
    } while (page < result.totalPages)
    return false
  },
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
  get: id => api.get(`/products/${id}`, priceResponse).then(r => r.data),
  checkDuplicate: data => api.post('/products/check-duplicate', data).then(r => r.data),
  create: data => api.post('/products', data, priceResponse).then(r => r.data),
  update: (id, data) => api.put(`/products/${id}`, data, priceResponse).then(r => r.data),
  status: (id, trangThai) => api.patch(`/products/${id}/status`, { trangThai }, priceResponse).then(r => r.data),
  createVariants: (id, variants) => api.post(`/products/${id}/variants`, { variants }, priceResponse).then(r => r.data),
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
  all: async () => {
    const first = await variantService.list({ page: 0, size: 100 })
    const rows = [...first.content]
    for (let page = 1; page < first.totalPages; page++) rows.push(...(await variantService.list({ page, size: 100 })).content)
    return rows
  },
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
  list: params => api.get('/product-details', { ...priceResponse, params }).then(r => r.data),
  update: (id, data) => api.put(`/product-details/${id}`, data, priceResponse).then(r => r.data),
  status: (id, trangThai) => api.patch(`/product-details/${id}/status`, { trangThai }, priceResponse).then(r => r.data)
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
    { label: 'Trạng thái', value: data.trangThai === 1 ? 'Kinh doanh' : 'Ngừng kinh doanh' }
  ]
}

export function validateVariant(row) { return Object.values(validateVariantFields(row))[0] || '' }
const currencyFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency', currency: 'VND', minimumFractionDigits: 0, maximumFractionDigits: 2
})
export function money(value) {
  if (value == null || !Number.isFinite(Number(value))) return 'Chưa có giá'
  return typeof value === 'string' ? formatPriceValue(value) + ' ₫' : currencyFormatter.format(value)
}
export function priceRange(product) {
  if (product.giaThapNhat == null || product.giaCaoNhat == null) return 'Chưa có giá'
  const min = product.giaThapNhat
  const max = product.giaCaoNhat
  if (!Number.isFinite(Number(min)) || !Number.isFinite(Number(max))) return 'Chưa có giá'
  return String(min) === String(max) ? money(min) : `${money(min)} - ${money(max)}`
}
// Compare canonical decimals exactly, including large prices preserved by the API parser.
function priceUnits(value) {
  if (value == null || !/^(?:\d+)(?:\.\d{1,2})?$/.test(String(value))) return null
  const [integer, fraction = ''] = String(value).split('.')
  return BigInt(integer) * 100n + BigInt(fraction.padEnd(2, '0'))
}
export function hasVariantDiscount(variant) {
  const base = priceUnits(variant?.giaBan), sale = priceUnits(variant?.giaSauGiam)
  return variant?.dangGiamGia === true && base != null && sale != null && sale < base
}
export function hasProductDiscount(product) {
  const baseMin = priceUnits(product?.giaThapNhat), baseMax = priceUnits(product?.giaCaoNhat)
  const saleMin = priceUnits(product?.giaSauGiamThapNhat), saleMax = priceUnits(product?.giaSauGiamCaoNhat)
  return product?.dangGiamGia === true && [baseMin, baseMax, saleMin, saleMax].every(value => value != null)
    && baseMin <= baseMax && saleMin <= saleMax && saleMin <= baseMin && saleMax <= baseMax
    && (saleMin < baseMin || saleMax < baseMax)
}
export function effectivePriceRange(product) {
  return hasProductDiscount(product) ? priceRange({ giaThapNhat: product.giaSauGiamThapNhat, giaCaoNhat: product.giaSauGiamCaoNhat }) : priceRange(product)
}
export function mergeCurrentPriceFields(rows, freshRows) {
  const fresh = new Map(freshRows.map(row => [row.id, row]))
  const keys = ['giaBan', 'giaThapNhat', 'giaCaoNhat', 'giaSauGiam', 'giaSauGiamThapNhat', 'giaSauGiamCaoNhat',
    'dangGiamGia', 'phanTramGiamHienTai', 'phanTramGiamCaoNhat', 'maDotGiamGia', 'tenDotGiamGia']
  for (const row of rows) {
    const current = fresh.get(row.id)
    if (current) for (const key of keys) if (key in current) row[key] = current[key]
  }
}
export function dateTime(value) {
  return value ? new Date(value).toLocaleString('vi-VN') : '—'
}
export function cleanParams(params = {}) {
  return Object.fromEntries(Object.entries(params)
    .map(([key, value]) => [key, typeof value === 'string' ? value.trim() : value])
    .filter(([, value]) => value !== '' && value != null))
}
