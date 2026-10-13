<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../../stores/auth'

const router   = useRouter()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const showPass = ref(false)
const loading  = ref(false)
const error    = ref('')

async function handleLogin() {
  error.value   = ''
  loading.value = true
  try {
    await authStore.login(username.value.trim(), password.value, 'NHAN_VIEN')
    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || 'Tên đăng nhập hoặc mật khẩu không đúng.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-shell">
    <div class="login-card">
      <div class="login-logo">
        <img src="../../../assets/logo3D.png" alt="SmashStep" />
      </div>

      <h1 class="login-title">Đăng nhập quản trị</h1>
      <p class="login-sub">Dành cho Admin &amp; Nhân viên</p>

      <form @submit.prevent="handleLogin" novalidate>
        <div class="field">
          <label>Tên đăng nhập</label>
          <input
            v-model="username"
            type="text"
            placeholder="Nhập tên đăng nhập"
            autocomplete="username"
            :disabled="loading"
            required
          />
        </div>

        <div class="field">
          <label>Mật khẩu</label>
          <div class="pass-wrap">
            <input
              v-model="password"
              :type="showPass ? 'text' : 'password'"
              placeholder="Nhập mật khẩu"
              autocomplete="current-password"
              :disabled="loading"
              required
            />
            <button
              type="button"
              class="toggle-pass"
              tabindex="-1"
              @click="showPass = !showPass"
              :aria-label="showPass ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
            >
              <i :class="showPass ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
            </button>
          </div>
        </div>

        <p v-if="error" class="login-error" role="alert">
          <i class="bi bi-exclamation-circle"></i> {{ error }}
        </p>

        <button class="login-btn" type="submit" :disabled="loading">
          <span v-if="loading"><i class="bi bi-arrow-repeat spin"></i> Đang đăng nhập…</span>
          <span v-else>Đăng nhập</span>
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-shell {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f1b2d 0%, #1a3a5c 100%);
}

.login-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 20px 60px rgba(0,0,0,.35);
}

.login-logo {
  text-align: center;
  margin-bottom: 16px;
}

.login-logo img {
  height: 60px;
  object-fit: contain;
}

.login-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a2b3c;
  text-align: center;
  margin: 0 0 4px;
}

.login-sub {
  font-size: 13px;
  color: #7a8fa6;
  text-align: center;
  margin: 0 0 28px;
}

.field {
  margin-bottom: 18px;
}

.field label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #3d5166;
  margin-bottom: 6px;
}

.field input {
  width: 100%;
  height: 44px;
  border: 1.5px solid #d0dbe6;
  border-radius: 8px;
  padding: 0 14px;
  font-size: 14px;
  outline: none;
  transition: border-color .2s;
  box-sizing: border-box;
}

.field input:focus {
  border-color: #168dcc;
}

.pass-wrap {
  position: relative;
}

.pass-wrap input {
  padding-right: 44px;
}

.toggle-pass {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #7a8fa6;
  cursor: pointer;
  padding: 0;
  font-size: 16px;
  line-height: 1;
}

.login-error {
  color: #c62828;
  font-size: 13px;
  background: #fff1f1;
  border-radius: 7px;
  padding: 9px 12px;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.login-btn {
  width: 100%;
  height: 46px;
  background: #168dcc;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background .2s, opacity .2s;
}

.login-btn:hover:not(:disabled) {
  background: #117ab5;
}

.login-btn:disabled {
  opacity: .6;
  cursor: not-allowed;
}

.spin {
  display: inline-block;
  animation: spin .8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
