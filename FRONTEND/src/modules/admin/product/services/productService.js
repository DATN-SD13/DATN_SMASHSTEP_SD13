import api from '../../../../utils/api'

export const productService = {
  list: params => api.get('/products', { params }).then(r => r.data),
  get: id => api.get(`/products/${id}`).then(r => r.data),
  create: data => api.post('/products', data).then(r => r.data),
  update: (id, data) => api.put(`/products/${id}`, data).then(r => r.data),
  status: (id, trangThai) => api.patch(`/products/${id}/status`, { trangThai }).then(r => r.data),
  createVariants: (id, variants) => api.post(`/products/${id}/variants`, { variants }).then(r => r.data),
  addImage: (id, data) => api.post(`/products/${id}/images`, data).then(r => r.data),
  updateImage: (id, imageId, data) => api.put(`/products/${id}/images/${imageId}`, data).then(r => r.data),
  removeImage: (id, imageId) => api.delete(`/products/${id}/images/${imageId}`)
}

export const variantService = {
  list: params => api.get('/product-details', { params }).then(r => r.data),
  update: (id, data) => api.put(`/product-details/${id}`, data).then(r => r.data),
  status: (id, trangThai) => api.patch(`/product-details/${id}/status`, { trangThai }).then(r => r.data)
}

export function errorMessage(error) {
  if (!error.response) return 'Không kết nối được backend. Vui lòng kiểm tra server rồi thử lại.'
  return error.response.data?.message || 'Không thể xử lý yêu cầu. Vui lòng thử lại.'
}
export function money(value) {
  return value == null ? 'Chưa có giá' : Number(value).toLocaleString('vi-VN', { style: 'currency', currency: 'VND' })
}
export function priceRange(product) {
  if (product.giaThapNhat == null) return 'Chưa có biến thể'
  return product.giaThapNhat === product.giaCaoNhat ? money(product.giaThapNhat)
    : `${money(product.giaThapNhat)} – ${money(product.giaCaoNhat)}`
}
export function dateTime(value) {
  return value ? new Date(value).toLocaleString('vi-VN') : '—'
}
export function cleanParams(params) {
  return Object.fromEntries(Object.entries(params).filter(([, v]) => v !== '' && v != null))
}

