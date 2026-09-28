import api from '../../../../utils/api'

export const invoiceService = {
  async getAll(params = {}) {
    const response = await api.get('/invoices', { params })
    return response.data
  },
  async getById(id) {
    const response = await api.get(`/invoices/${id}`)
    return response.data
  }
}
