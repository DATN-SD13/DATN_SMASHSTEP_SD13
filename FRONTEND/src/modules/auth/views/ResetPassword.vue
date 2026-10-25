<script setup>
import { passwordError } from '../../../utils/accountValidation'
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../../utils/api'

const route  = useRoute()
const router = useRouter()
const employeeAccount = route.path.startsWith('/nhan-vien/')
const loginPath = employeeAccount ? '/dang-nhap' : '/khach-hang/dang-nhap'
const forgotPath = employeeAccount ? '/quen-mat-khau-quan-ly' : '/khach-hang/quen-mat-khau'

const token       = typeof route.query.token === 'string' ? route.query.token : ''
const newPassword = ref('')
const confirmPass = ref('')
const showPass    = ref(false)
const showConfirm = ref(false)
const loading     = ref(false)
const error       = ref('')
const success     = ref(false)

// Trạng thái kiểm tra token
const tokenStatus = ref('checking') // 'checking' | 'valid' | 'invalid'

onMounted(async () => {
  if (!token) {
    tokenStatus.value = 'invalid'
    return
  }
  try {
    const res = await api.get(`/auth/kiem-tra-token?token=${encodeURIComponent(token)}`)
    tokenStatus.value = res.data?.data === true ? 'valid' : 'invalid'
  } catch {
    tokenStatus.value = 'invalid'
  }
})

function validate() {
  const invalid = passwordError(newPassword.value)
  if (invalid) { error.value = invalid; return false }
  if (!newPassword.value || newPassword.value.length < 6) {
    error.value = 'Mật khẩu phải có ít nhất 6 ký tự.'
    return false
  }
  if (newPassword.value !== confirmPass.value) {
    error.value = 'Mật khẩu xác nhận không khớp.'
    return false
  }
  return true
}

async function handleSubmit() {
  if (loading.value) return
  error.value = ''
  if (!validate()) return

  loading.value = true
  try {
    await api.post('/auth/dat-lai-mat-khau', {
      token,
      newPassword: newPassword.value
    })
    success.value = true
  } catch (e) {
    error.value = e.response?.data?.message || 'Đã có lỗi xảy ra. Vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="shell">
    <div class="card">
      <div class="logo">
        <img src="../../../assets/logo3D.png" alt="SmashStep" />
      </div>

      <!-- Đang kiểm tra token -->
      <template v-if="tokenStatus === 'checking'">
        <div class="checking">
          <i class="bi bi-arrow-repeat spin"></i>
          <p>Đang xác minh link…</p>
        </div>
      </template>

      <!-- Token không hợp lệ hoặc hết hạn -->
      <template v-else-if="tokenStatus === 'invalid'">
        <div class="icon-box error-icon">🔗</div>
        <h1 class="title">Link không hợp lệ</h1>
        <p class="sub">
          Link đặt lại mật khẩu đã hết hạn hoặc không hợp lệ.<br/>
          Vui lòng gửi lại yêu cầu.
        </p>
        <button class="btn" @click="router.push(forgotPath)">
          Gửi lại yêu cầu
        </button>
        <p class="back-link">
          <RouterLink :to="loginPath">
            <i class="bi bi-arrow-left"></i> Về trang đăng nhập
          </RouterLink>
        </p>
      </template>

      <!-- Đặt lại thành công -->
      <template v-else-if="success">
        <div class="icon-box">✅</div>
        <h1 class="title">Đặt lại thành công!</h1>
        <p class="sub">
          Mật khẩu của bạn đã được cập nhật. Bạn có thể đăng nhập ngay bây giờ.
        </p>
        <button class="btn" @click="router.push(loginPath)">
          Đăng nhập ngay
        </button>
      </template>

      <!-- Form đặt mật khẩu mới -->
      <template v-else>
        <h1 class="title">Tạo mật khẩu mới</h1>
        <p class="sub">Nhập mật khẩu mới cho tài khoản của bạn.</p>

        <form @submit.prevent="handleSubmit" novalidate>
          <div class="field">
            <label>Mật khẩu mới</label>
            <div class="pass-wrap">
              <input maxlength="72"
                v-model="newPassword"
                :type="showPass ? 'text' : 'password'"
                placeholder="Ít nhất 6 ký tự, gồm chữ và số"
                autocomplete="new-password"
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

          <div class="field">
            <label>Xác nhận mật khẩu</label>
            <div class="pass-wrap">
              <input maxlength="72"
                v-model="confirmPass"
                :type="showConfirm ? 'text' : 'password'"
                placeholder="Nhập lại mật khẩu mới"
                autocomplete="new-password"
                :disabled="loading"
                required
              />
              <button
                type="button"
                class="toggle-pass"
                tabindex="-1"
                @click="showConfirm = !showConfirm"
                :aria-label="showConfirm ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'"
              >
                <i :class="showConfirm ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
              </button>
            </div>
          </div>

          <p v-if="error" class="error-msg" role="alert">
            <i class="bi bi-exclamation-circle"></i> {{ error }}
          </p>

          <button class="btn" type="submit" :disabled="loading">
            <span v-if="loading"><i class="bi bi-arrow-repeat spin"></i> Đang lưu…</span>
            <span v-else>Đặt lại mật khẩu</span>
          </button>
        </form>
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
  background: linear-gradient(135deg, #1a0533 0%, #3d1a6e 100%);
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

.icon-box {
  font-size: 48px;
  margin-bottom: 12px;
}

.checking {
  padding: 24px 0;
  color: #7a8fa6;
  font-size: 14px;
}

.checking .spin { font-size: 28px; display: block; margin-bottom: 12px; }

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

.pass-wrap { position: relative; }

.pass-wrap input {
  width: 100%;
  height: 44px;
  border: 1.5px solid #d0dbe6;
  border-radius: 8px;
  padding: 0 44px 0 14px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  transition: border-color .2s;
}

.pass-wrap input:focus { border-color: #7c4dff; }

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
  background: #7c4dff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background .2s, opacity .2s;
  margin-bottom: 4px;
}

.btn:hover:not(:disabled) { background: #6a3de8; }
.btn:disabled { opacity: .6; cursor: not-allowed; }

.back-link {
  margin-top: 14px;
  font-size: 13px;
}

.back-link a {
  color: #7c4dff;
  text-decoration: none;
  font-weight: 600;
}

.spin {
  display: inline-block;
  animation: spin .8s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }
</style>
