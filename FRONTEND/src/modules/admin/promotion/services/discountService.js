import api from '../../../../utils/api'

const normalize = data => ({
  ...data,
  minOrderValue: data.minOrderValue === '' ? null : data.minOrderValue,
  maxDiscount: data.maxDiscount === '' ? null : data.maxDiscount,
  quantity: data.unlimited ? null : data.quantity,
  customerIds: data.form === 2 ? data.customerIds : []
})

export function getDiscounts(params) {
  return api.get('/phieu-giam-gia', { params })
}

export function getDiscountById(id) {
  return api.get(`/phieu-giam-gia/${id}`)
}

export function createDiscount(data) {
  return api.post('/phieu-giam-gia', normalize(data))
}

export function updateDiscount(id, data) {
  return api.put(`/phieu-giam-gia/${id}`, normalize(data))
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
