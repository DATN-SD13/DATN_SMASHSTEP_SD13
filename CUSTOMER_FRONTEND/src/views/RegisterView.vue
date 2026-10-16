<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import api from '../services/api'

const router    = useRouter()
const authStore = useAuthStore()

const name     = ref('')
const email    = ref('')
const phone    = ref('')
const password = ref('')
const confirm  = ref('')
const showPass = ref(false)
const showCfm  = ref(false)
const loading  = ref(false)
const error    = ref('')

function validate() {
  if (!name.value.trim())  { error.value = 'Vui lòng nhập họ tên.'; return false }
  if (!email.value.trim()) { error.value = 'Vui lòng nhập email.'; return false }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value.trim())) {
    error.value = 'Email không hợp lệ.'; return false
  }
  if (phone.value && !/^0\d{9}$/.test(phone.value.trim())) {
    error.value = 'Số điện thoại phải gồm 10 số và bắt đầu bằng 0.'; return false
  }
  if (password.value.length < 6) { error.value = 'Mật khẩu phải có ít nhất 6 ký tự.'; return false }
  if (password.value !== confirm.value) { error.value = 'Mật khẩu xác nhận không khớp.'; return false }
  return true
}

async function handleRegister() {
  error.value = ''
  if (!validate()) return

  loading.value = true
  try {
    // Gọi API đăng ký — backend trả về token + user giống login
    const data = await api.post('/auth/dang-ky', {
      name:     name.value.trim(),
      email:    email.value.trim(),
      phone:    phone.value.trim() || undefined,
      password: password.value
    })

    // Lưu token vào store (tương tự login)
    const payload = data?.accessToken ? data : data?.data
    authStore.token = payload.accessToken
    authStore.user  = {
      id:       payload.id,
      code:     payload.code,
      name:     payload.name,
      email:    payload.email,
      role:     payload.role,
      userType: payload.userType,
      avatar:   payload.avatar
    }
    localStorage.setItem('ss_token', payload.accessToken)
    localStorage.setItem('ss_user',  JSON.stringify(authStore.user))

    router.push('/')
  } catch (e) {
    error.value = e.response?.data?.message || e.message || 'Đăng ký thất bại. Vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="shell">
    <div class="card">
      <div class="logo">
        <img src="../assets/logo3D.png" alt="SmashStep" />
      </div>

      <h1 class="title">Tạo tài khoản</h1>
      <p class="sub">Đăng ký để mua sắm dễ dàng hơn tại SmashStep</p>

      <form @submit.prevent="handleRegister" novalidate>
        <!-- Họ tên -->
        <div class="field">
          <label>Họ và tên <span class="req">*</span></label>
          <input
            v-model="name"
            type="text"
            placeholder="Nguyễn Văn A"
            autocomplete="name"
            :disabled="loading"
            required
          />
        </div>

        <!-- Email -->
        <div class="field">
          <label>Email <span class="req">*</span></label>
          <input
            v-model="email"
            type="email"
            placeholder="email@example.com"
            autocomplete="email"
            :disabled="loading"
            required
          />
        </div>

        <!-- Số điện thoại -->
        <div class="field">
          <label>Số điện thoại <span class="optional">(tuỳ chọn)</span></label>
          <input
            v-model="phone"
            type="tel"
            placeholder="0xxxxxxxxx"
            autocomplete="tel"
            :disabled="loading"
          />
        </div>

        <!-- Mật khẩu -->
        <div class="field">
          <label>Mật khẩu <span class="req">*</span></label>
          <div class="pass-wrap">
            <input
              v-model="password"
              :type="showPass ? 'text' : 'password'"
              placeholder="Ít nhất 6 ký tự"
              autocomplete="new-password"
              :disabled="loading"
              required
            />
            <button type="button" class="toggle-pass" tabindex="-1"
              @click="showPass = !showPass"
              :aria-label="showPass ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'">
              <i :class="showPass ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
            </button>
          </div>
        </div>

        <!-- Xác nhận mật khẩu -->
        <div class="field">
          <label>Xác nhận mật khẩu <span class="req">*</span></label>
          <div class="pass-wrap">
            <input
              v-model="confirm"
              :type="showCfm ? 'text' : 'password'"
              placeholder="Nhập lại mật khẩu"
              autocomplete="new-password"
              :disabled="loading"
              required
            />
            <button type="button" class="toggle-pass" tabindex="-1"
              @click="showCfm = !showCfm"
              :aria-label="showCfm ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'">
              <i :class="showCfm ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
            </button>
          </div>
        </div>

        <p v-if="error" class="error-msg" role="alert">
          <i class="bi bi-exclamation-circle"></i> {{ error }}
        </p>

        <button class="btn" type="submit" :disabled="loading">
          <span v-if="loading"><i class="bi bi-arrow-repeat spin"></i> Đang đăng ký…</span>
          <span v-else>Tạo tài khoản</span>
        </button>
      </form>

      <p class="login-link">
        Đã có tài khoản?
        <RouterLink to="/dang-nhap">Đăng nhập ngay</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.shell {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(110deg, #102A4C 0%, #17365F 42%, #2879E3 100%);
  padding: 28px 20px;
}

.card {
  width: 100%;
  max-width: 440px;
  background: #fff;
  border-radius: 16px;
  padding: 36px 36px 28px;
  box-shadow: 0 20px 60px rgba(0,0,0,.35);
}

.logo {
  text-align: center;
  margin-bottom: 14px;
}
.logo img {
  height: 54px;
  object-fit: contain;
}

.title {
  font-size: 22px;
  font-weight: 700;
  color: #17365F;
  text-align: center;
  margin: 0 0 4px;
}

.sub {
  font-size: 13px;
  color: #718096;
  text-align: center;
  margin: 0 0 24px;
}

.field {
  margin-bottom: 16px;
}

.field label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #17365F;
  margin-bottom: 5px;
}

.req   { color: #e53e3e; }
.optional { font-size: 11px; font-weight: 400; color: #718096; }

.field input {
  width: 100%;
  height: 44px;
  border: 1.5px solid #DDE6F0;
  border-radius: 8px;
  padding: 0 14px;
  font-size: 14px;
  outline: none;
  transition: border-color .2s;
  box-sizing: border-box;
}
.field input:focus { border-color: #2879E3; }

.pass-wrap { position: relative; }
.pass-wrap input { padding-right: 44px; }

.toggle-pass {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: #718096;
  cursor: pointer;
  padding: 0;
  font-size: 16px;
  line-height: 1;
}

.error-msg {
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

.btn {
  width: 100%;
  height: 46px;
  background: #2879E3;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background .2s, opacity .2s;
}
.btn:hover:not(:disabled) { background: #1F6BD1; }
.btn:disabled { opacity: .6; cursor: not-allowed; }

.login-link {
  text-align: center;
  font-size: 13px;
  color: #718096;
  margin-top: 18px;
}
.login-link a {
  color: #2879E3;
  font-weight: 600;
  text-decoration: none;
}

.spin {
  display: inline-block;
  animation: spin .8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
