import api from '../../../../utils/api'

export const invoiceService = {
  async paidDemoOrders() {
    return (await api.get('/hoa-don/paid-demo-orders')).data
  },
  async qrPayment(ma) {
    return (await api.get(`/hoa-don/${encodeURIComponent(ma)}/thanh-toan-qr`)).data
  },
  async confirmQrPayment(ma, payload) {
    return (await api.post(`/hoa-don/${encodeURIComponent(ma)}/thanh-toan-qr`, payload)).data
  },
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
  },
  async resolveFailedDelivery(ma, payload) {
    const response = await api.put(`/hoa-don/${encodeURIComponent(ma)}/xu-ly-giao-that-bai`, payload)
    return response.data
  },
  async getSummary(params = {}) {
    const response = await api.get('/thong-ke', { params })
    return response.data
  }
}
