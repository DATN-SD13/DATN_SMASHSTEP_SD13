import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
  paramsSerializer(params) {
    const query = new URLSearchParams()
    for (const [key, value] of Object.entries(params || {}))
      for (const entry of Array.isArray(value) ? value : [value])
        if (entry !== '' && entry !== null && entry !== undefined)
          query.append(key, entry)
    return query.toString()
  }
})

// ── Request: đính kèm Bearer token ──────────────────────
api.interceptors.request.use(config => {
  const token = localStorage.getItem('ss_token')
  if (token) config.headers['Authorization'] = `Bearer ${token}`
  return config
})

// ── Response: unwrap data, xử lý 401 ────────────────────
api.interceptors.response.use(
  response => {
    if (response.data?.success === false)
      return Promise.reject(new Error(response.data.message || 'Thao tác thất bại'))
    return response.data?.data ?? response.data
  },
  error => {
    if (error.response?.status === 401) {
      // Lazy import tránh circular dep
      import('../stores/auth').then(({ useAuthStore }) => {
        useAuthStore().forceLogout()
      })
    }
    return Promise.reject(error)
  }
)

export default api
