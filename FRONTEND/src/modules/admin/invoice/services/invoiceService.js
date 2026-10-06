import api from '../../../../utils/api'

export const invoiceService = {
  async getAll(params = {}) {
    const response = await api.get('/hoa-don', { params })
    return response.data
  },
  async getById(ma) {
    const response = await api.get(`/hoa-don/${encodeURIComponent(ma)}`)
    return response.data
  },
  async getHistory(ma) {
    const response = await api.get(`/hoa-don/${encodeURIComponent(ma)}/lich-su`)
    return response.data
  },
  async updateStatus(ma, payload) {
    const response = await api.put(`/hoa-don/${encodeURIComponent(ma)}/trang-thai`, payload)
    return response.data
  }
}
