import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

api.interceptors.response.use(response => {
  if (/^\/(hoa-don|san-pham|san-pham-chi-tiet|thuoc-tinh|phieu-giam-gia|dot-giam-gia)(\/|$)/.test(response.config.url || '') && response.data?.success === false) {
    const error = new Error(response.data.message || 'Thao tác thất bại')
    error.response = response
    throw error
  }
  return response
})

export default api
