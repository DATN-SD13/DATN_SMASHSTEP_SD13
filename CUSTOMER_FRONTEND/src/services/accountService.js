import api from './api'

const path = '/customer/account'
export const accountService = {
  profile: () => api.get(path),
  addresses: () => api.get(`${path}/addresses`),
  update: data => api.put(path, data),
  changePassword: data => api.put(`${path}/password`, data),
  createAddress: data => api.post(`${path}/addresses`, data),
  updateAddress: (id, data) => api.put(`${path}/addresses/${id}`, data),
  setDefaultAddress: id => api.patch(`${path}/addresses/${id}/default`)
}
