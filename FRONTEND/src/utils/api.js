import axios from 'axios'
import { enforceAccountRequest } from './accountValidation'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ── Request interceptor: đính kèm Bearer token ──────────
api.interceptors.request.use(config => {
  enforceAccountRequest(config)
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
  async error => {
    const validation = error.response?.data?.errors
    if (validation && typeof validation === 'object') error.response.data.message = Object.values(validation).join(' · ')
    const requestToken = error.config?.headers?.Authorization || error.config?.headers?.get?.('Authorization')
    const activeToken = localStorage.getItem('ss_token')
    // Phản hồi chậm của phiên cũ không được đăng xuất hoặc cập nhật phiên vừa đăng nhập.
    if (activeToken && requestToken === `Bearer ${activeToken}` && error.config?.url !== '/auth/login') {
      if (error.response?.status === 401) {
        const { useAuthStore } = await import('../stores/auth')
        const auth = useAuthStore()
        if (auth.token === activeToken && localStorage.getItem('ss_token') === activeToken) await auth.forceLogout()
      } else if (error.response?.status === 403 && !error.config?.skipSessionRefresh) {
        try {
          const { useAuthStore } = await import('../stores/auth')
          const auth = useAuthStore()
          if (auth.token === activeToken) await auth.refreshSession()
        } catch { /* Giữ lỗi gốc; API luôn tự kiểm tra quyền hiện tại. */ }
      }
    }
    return Promise.reject(error)
  }
)

export default api
