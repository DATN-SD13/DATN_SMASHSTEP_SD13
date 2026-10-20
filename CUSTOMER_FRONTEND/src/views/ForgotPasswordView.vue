<script setup>
import { emailError } from '../utils/accountValidation'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../services/api'

const router  = useRouter()

const email   = ref('')
const loading = ref(false)
const error   = ref('')
const success = ref(false)

async function handleSubmit() {
  if (loading.value) return
  const invalid = emailError(email.value)
  if (invalid) { error.value = invalid; return }
  error.value   = ''
  loading.value = true
  try {
    await api.post('/auth/quen-mat-khau', { email: email.value.trim() })
    success.value = true
  } catch (e) {
    error.value = e.response?.data?.message || e.message || 'Đã có lỗi xảy ra. Vui lòng thử lại.'
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

      <!-- Trạng thái thành công -->
      <template v-if="success">
        <div class="success-icon">✉️</div>
        <h1 class="title">Kiểm tra hộp thư!</h1>
        <p class="sub">
          Nếu email <b>{{ email }}</b> tồn tại trong hệ thống, chúng tôi đã gửi
          link đặt lại mật khẩu. Link có hiệu lực trong <b>30 phút</b>.
        </p>
        <p class="hint">Không thấy email? Kiểm tra thư mục Spam.</p>
        <button class="btn" @click="router.push('/dang-nhap')">
          Quay về đăng nhập
        </button>
      </template>

      <!-- Form nhập email -->
      <template v-else>
        <h1 class="title">Quên mật khẩu?</h1>
        <p class="sub">Nhập email tài khoản, chúng tôi sẽ gửi link đặt lại mật khẩu cho bạn.</p>

        <form @submit.prevent="handleSubmit" novalidate>
          <div class="field">
            <label>Email</label>
            <input maxlength="254"
              v-model="email"
              type="email"
              placeholder="Nhập email đã đăng ký"
              autocomplete="email"
              :disabled="loading"
              required
            />
          </div>

          <p v-if="error" class="error-msg" role="alert">
            <i class="bi bi-exclamation-circle"></i> {{ error }}
          </p>

          <button class="btn" type="submit" :disabled="loading || !email.trim()">
            <span v-if="loading"><i class="bi bi-arrow-repeat spin"></i> Đang gửi…</span>
            <span v-else>Gửi link đặt lại mật khẩu</span>
          </button>
        </form>

        <p class="back-link">
          <RouterLink to="/dang-nhap">
            <i class="bi bi-arrow-left"></i> Quay về đăng nhập
          </RouterLink>
        </p>
      </template>
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
  padding: 20px;
}

.card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 20px 60px rgba(0,0,0,.35);
  text-align: center;
}

.logo img {
  height: 56px;
  object-fit: contain;
  margin-bottom: 16px;
}

.success-icon { font-size: 48px; margin-bottom: 12px; }

.title {
  font-size: 22px;
  font-weight: 700;
  color: #1a2b3c;
  margin: 0 0 8px;
}

.sub {
  font-size: 13.5px;
  color: #5f7384;
  margin: 0 0 24px;
  line-height: 1.6;
}

.hint {
  font-size: 12px;
  color: #9aabbc;
  margin: -16px 0 20px;
}

.field {
  text-align: left;
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
  box-sizing: border-box;
  transition: border-color .2s;
}

.field input:focus { border-color: #2879E3; }

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
  text-align: left;
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

.back-link {
  margin-top: 18px;
  font-size: 13px;
}

.back-link a {
  color: #2879E3;
  text-decoration: none;
  font-weight: 600;
}

.spin {
  display: inline-block;
  animation: spin .8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }
</style>
