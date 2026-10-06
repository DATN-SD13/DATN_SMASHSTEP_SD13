import api from '../../../../utils/api'

const unwrap = response => response.data.data
export default {
  async catalog(params) { return unwrap(await api.get('/sales/catalog', { params })) },
  async catalogItem(id) { return unwrap(await api.get(`/sales/catalog/${id}`)) },
  async paymentMethods() { return unwrap(await api.get('/sales/payment-methods')) },
  async quote(payload) { return unwrap(await api.post('/sales/quote', payload)) },
  async checkout(payload) { return unwrap(await api.post('/sales/checkout', payload)) }
}
