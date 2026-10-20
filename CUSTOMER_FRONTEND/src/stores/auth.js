import { defineStore } from 'pinia'
import { ref, computed, onScopeDispose } from 'vue'
import api from '../services/api'
import router from '../router'
import { useCartStore } from './cart'

import { readCustomerSession, TOKEN_KEY, USER_KEY } from '../utils/customerSession'

export const useAuthStore = defineStore('auth', () => {
  // ── State ──────────────────────────────────────────────
  const saved = readCustomerSession(localStorage)
  const token = ref(saved.token)
  const user = ref(saved.user)
  function syncSession() { const saved = readCustomerSession(localStorage); token.value = saved.token; user.value = saved.user; useCartStore().syncOwner() }
  function storageChanged(e) { if (e.key === TOKEN_KEY || e.key === USER_KEY || e.key === null) syncSession() }
  window.addEventListener('storage', storageChanged)
  onScopeDispose(() => window.removeEventListener('storage', storageChanged))

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

    if (!data?.accessToken || data.userType !== 'KHACH_HANG' || !data.id) throw new Error('Phản hồi đăng nhập không hợp lệ. Vui lòng thử lại.')
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
    useCartStore().syncOwner()
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
    if (!user.value || !isCustomer.value || String(user.value.id) !== String(profile.id)) return
    user.value = { ...user.value, code: profile.code, name: profile.name, email: profile.email, avatar: profile.image }
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  function _clearState() {
    token.value = null
    user.value  = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    useCartStore().syncOwner()
  }

  /** Gọi bởi api interceptor khi gặp 401 */
  function forceLogout() {
    const current = router.currentRoute.value
    _clearState()
    if (current.meta.requiresAuth) router.replace({ path: '/dang-nhap', query: { redirect: current.fullPath, reason: 'expired' } })
  }

  return {
    token, user,
    isAuthenticated, isCustomer,
    login, logout, forceLogout, updateProfile, syncSession
  }
})
