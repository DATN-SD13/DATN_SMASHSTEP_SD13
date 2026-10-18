import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ── Request interceptor: đính kèm Bearer token ──────────
api.interceptors.request.use(config => {
  const token = localStorage.getItem('ss_token')
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

// ── Response interceptor ─────────────────────────────────
api.interceptors.response.use(
  response => {
    // Kiểm tra success=false cho các module cũ
    if (
      /^\/(hoa-don|san-pham|san-pham-chi-tiet|thuoc-tinh|phieu-giam-gia|dot-giam-gia)(\/|$)/.test(
        response.config.url || ''
      ) &&
      response.data?.success === false
    ) {
      const error = new Error(response.data.message || 'Thao tác thất bại')
      error.response = response
      throw error
    }
    return response
  },
  error => {
    if (error.response?.status === 401) {
      // Lazy import để tránh circular dependency
      import('../stores/auth').then(({ useAuthStore }) => {
        useAuthStore().forceLogout()
      })
    }
    return Promise.reject(error)
  }
)

export default api
