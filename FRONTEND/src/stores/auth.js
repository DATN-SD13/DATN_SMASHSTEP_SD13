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

  // ── Getters ────────────────────────────────────────────
  const isAuthenticated = computed(() => !!token.value)
  const role            = computed(() => user.value?.role  || null)
  const userType        = computed(() => user.value?.userType || null)
  const isAdmin         = computed(() => role.value === 'ADMIN')
  const isStaff         = computed(() => role.value === 'STAFF' || role.value === 'ADMIN')
  const isCustomer      = computed(() => role.value === 'CUSTOMER')

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

  /** Đăng xuất — xóa token, gọi server, điều hướng về trang login tương ứng. */
  async function logout() {
    const wasCustomer = isCustomer.value
    try {
      await api.post('/auth/logout')
    } catch {
      // Bỏ qua lỗi server
    } finally {
      _clearState()
      router.push(wasCustomer ? '/khach-hang/dang-nhap' : '/dang-nhap')
    }
  }

  function _clearState() {
    token.value = null
    user.value  = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  /** Gọi bởi axios interceptor khi gặp 401. */
  function forceLogout() {
    const wasCustomer = isCustomer.value
    _clearState()
    router.push(wasCustomer ? '/khach-hang/dang-nhap' : '/dang-nhap')
  }

  return {
    token, user,
    isAuthenticated, role, userType,
    isAdmin, isStaff, isCustomer,
    login, logout, forceLogout
  }
})
