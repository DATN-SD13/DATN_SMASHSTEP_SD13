import api from './api'
const base = '/customer/shop'
export const commerceService = {
  variants: ids => api.get('/store/variants', { params: { ids } }),
  resolveAddress: data => api.post(`${base}/shipping/resolve`, data),
  quote: data => api.post(`${base}/quote`, data),
  checkout: data => api.post(`${base}/checkout`, data),
  demoConfig: () => api.get(`${base}/demo-payments/config`),
  demoQuote: data => api.post(`${base}/demo-payments/quote`, data),
  demoCreate: data => api.post(`${base}/demo-payments`, data),
  demoPayment: code => api.get(`${base}/demo-payments/${encodeURIComponent(code)}`),
  demoCard: (code, data) => api.post(`${base}/demo-payments/${encodeURIComponent(code)}/card`, data),
  demoOtp: (code, data) => api.post(`${base}/demo-payments/${encodeURIComponent(code)}/otp`, data),
  demoCancel: code => api.post(`${base}/demo-payments/${encodeURIComponent(code)}/cancel`),
  vouchers: subtotal => api.get(`${base}/vouchers`, { params: { subtotal } }),
  orders: page => api.get(`${base}/orders`, { params: { page } }),
  order: code => api.get(`${base}/orders/${encodeURIComponent(code)}`),
  payment: code => api.get(`${base}/orders/${encodeURIComponent(code)}/payment`),
  shipping: (code, data) => api.put(`${base}/orders/${encodeURIComponent(code)}/shipping`, data),
  cancel: code => api.post(`${base}/orders/${encodeURIComponent(code)}/cancel`),
  notifications: () => api.get(`${base}/notifications`),
  areas: (level, parent) => api.get(`${base}/shipping/${level}`, { params: { parent } })
}
export const commerceError = error => error?.response?.data?.message || 'Không thể thực hiện lúc này. Vui lòng thử lại.'
