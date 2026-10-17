import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '../services/api'
import router from '../router'

const TOKEN_KEY = 'ss_token'
const USER_KEY  = 'ss_user'

export const useAuthStore = defineStore('auth', () => {
  // ── State ──────────────────────────────────────────────
  const token = ref(localStorage.getItem(TOKEN_KEY) || null)
  const user  = ref(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))

  // ── Getters ────────────────────────────────────────────
  const isAuthenticated = computed(() => !!token.value)
  const isCustomer      = computed(() => user.value?.userType === 'KHACH_HANG')

  // ── Actions ────────────────────────────────────────────
  async function login(username, password) {
    const response = await api.post('/auth/login', {
      username,
      password,
      userType: 'KHACH_HANG'
    })
    // api.js trả về response.data.data nên response ở đây đã là data object
    const data = response?.accessToken ? response : response?.data

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

  async function logout() {
    try {
      await api.post('/auth/logout')
    } catch {
      // bỏ qua lỗi server
    } finally {
      _clearState()
      router.push('/dang-nhap')
    }
  }

  function updateProfile(profile) {
    if (!user.value || !isCustomer.value || user.value.id !== profile.id) return
    user.value = { ...user.value, code: profile.code, name: profile.name, email: profile.email, avatar: profile.image }
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  function _clearState() {
    token.value = null
    user.value  = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  /** Gọi bởi api interceptor khi gặp 401 */
  function forceLogout() {
    _clearState()
    router.push('/dang-nhap')
  }

  return {
    token, user,
    isAuthenticated, isCustomer,
    login, logout, forceLogout, updateProfile
  }
})
