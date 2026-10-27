import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../utils/api'
import router from '../router'

const TOKEN_KEY = 'ss_token'
const USER_KEY  = 'ss_user'

export const useAuthStore = defineStore('auth', () => {
  // ── State ──────────────────────────────────────────────
  const token = ref(localStorage.getItem(TOKEN_KEY) || null)
  const user  = ref(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))
  const loggingOut = ref(false), logoutAuthorized = ref(false)
  let logoutGuard = null
  function setLogoutGuard(handler) {
    logoutGuard = handler
    return () => { if (logoutGuard === handler) logoutGuard = null }
  }

  // ── Getters ────────────────────────────────────────────
  const isAuthenticated = computed(() => !!token.value)
  const role            = computed(() => user.value?.role  || null)
  const userType        = computed(() => user.value?.userType || null)
  const isAdmin         = computed( () => user.value?.userType === 'NHAN_VIEN' && user.value?.role === 'ADMIN')
  const isStaff         = computed( () => user.value?.userType === 'NHAN_VIEN' && ['STAFF', 'ADMIN'].includes(user.value?.role))
  const isCustomer      = computed( () => user.value?.userType === 'KHACH_HANG' || user.value?.role === 'CUSTOMER')


  // ── Actions ────────────────────────────────────────────

  /**
   * Đăng nhập.
   * @param {string} username
   * @param {string} password
   * @param {'NHAN_VIEN'|'KHACH_HANG'} [userType] - nếu truyền thì BE ưu tiên tìm đúng loại
   */
  async function login(username, password, userType) {
    const body = userType
      ? { username, password, userType }
      : { username, password }
    const response = await api.post('/auth/login', body)
    const data = response.data?.data

    token.value = data.accessToken
    user.value  = {
      id:       data.id,
      code:     data.code,
      name:     data.name,
      email:    data.email,
      role:     data.role,
      userType: data.userType,
      avatar:   data.avatar
    }

    localStorage.setItem(TOKEN_KEY, token.value)
    localStorage.setItem(USER_KEY,  JSON.stringify(user.value))
  }

  let sessionRefresh = null
  async function refreshSession() {
    if (!token.value || user.value?.userType !== 'NHAN_VIEN') return null
    if (sessionRefresh) return sessionRefresh
    const checkingToken = token.value, checkingId = user.value.id
    const operation = (async () => {
      const response = await api.get('/auth/me', { skipSessionRefresh: true })
      if (token.value !== checkingToken || user.value?.id !== checkingId) return null
      const current = response.data?.data
      if (!current || Number(current.userId) !== Number(checkingId) || current.userType !== 'NHAN_VIEN'
          || !['ADMIN', 'STAFF'].includes(current.role)) throw new Error('Thông tin phiên đăng nhập không hợp lệ')
      if (user.value.role !== current.role) {
        user.value = { ...user.value, role: current.role }
        localStorage.setItem(USER_KEY, JSON.stringify(user.value))
      }
      return current
    })()
    sessionRefresh = operation
    try { return await operation }
    finally { if (sessionRefresh === operation) sessionRefresh = null }
  }

  /** Prepare the active page while credentials still exist, then end the session. */
  async function logout() {
    if (loggingOut.value) return false
    loggingOut.value = true
    const wasCustomer = isCustomer.value
    try {
      if (logoutGuard && (await logoutGuard()) === false) return false
      logoutAuthorized.value = true
      try { await api.post('/auth/logout') } catch { /* Local logout still works when the server is unavailable. */ }
      _clearState()
      await router.replace(wasCustomer ? '/khach-hang/dang-nhap' : '/dang-nhap')
      return true
    } finally {
      loggingOut.value = false
      logoutAuthorized.value = false
    }
  }

  function _clearState() {
    sessionRefresh = null
    token.value = null
    user.value  = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  /** Gọi bởi axios interceptor khi gặp 401. */
  function forceLogout() {
    if (!token.value && !user.value) return Promise.resolve(false)
    const wasCustomer = isCustomer.value
    _clearState()
    return router.replace(wasCustomer ? '/khach-hang/dang-nhap' : '/dang-nhap')
  }

  return {
    token, user,
    isAuthenticated, role, userType,
    isAdmin, isStaff, isCustomer,
    loggingOut, logoutAuthorized, setLogoutGuard, login, logout, forceLogout, refreshSession
  }
})
