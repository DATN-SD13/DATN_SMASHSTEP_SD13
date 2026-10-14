import api from '../../../../utils/api'

const unwrap = response => response.data.data
export default {
  async pending() { return unwrap(await api.get('/sales/pending')) },
  async pendingItem(id) { return unwrap(await api.get(`/sales/pending/${id}`)) },
  async createPending() { return unwrap(await api.post('/sales/pending')) },
  async savePending(id, payload) { return unwrap(await api.put(`/sales/pending/${id}`, payload)) },
  async cancelPending(code) { return unwrap(await api.put(`/hoa-don/${encodeURIComponent(code)}/trang-thai`,
    { trangThai: 6, ghiChu: 'Hủy hóa đơn chờ tại POS (chưa thanh toán)' })) },
  async catalog(params) { return unwrap(await api.get('/sales/catalog', { params })) },
  async catalogItem(id) { return unwrap(await api.get(`/sales/catalog/${id}`)) },
  async paymentMethods() { return unwrap(await api.get('/sales/payment-methods')) },
  async quote(payload) { return unwrap(await api.post('/sales/quote', payload)) },
  async checkout(payload) { return unwrap(await api.post('/sales/checkout', payload)) }
}
