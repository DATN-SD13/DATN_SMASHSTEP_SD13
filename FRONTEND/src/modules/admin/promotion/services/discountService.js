import api from '../../../../utils/api'

export function getDiscounts(params) {
  return api.get('/phieu-giam-gia', { params })
}

export function getDiscountById(id) {
  return api.get(`/phieu-giam-gia/${id}`)
}

export function createDiscount(data) {
  return api.post('/phieu-giam-gia', data)
}

export function updateDiscount(id, data) {
  return api.put(`/phieu-giam-gia/${id}`, data)
}

export function deactivateDiscount(id) {
  return api.delete(`/phieu-giam-gia/${id}`)
}

export function activateDiscount(id) {
  return api.put(`/phieu-giam-gia/${id}/kich-hoat`)
}

export function deleteDiscount(id) {
  return api.delete(`/phieu-giam-gia/${id}/xoa`)
}