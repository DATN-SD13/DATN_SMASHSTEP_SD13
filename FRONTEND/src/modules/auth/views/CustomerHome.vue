<script setup>
import { ref, computed } from 'vue'
import { useAuthStore } from '../../../stores/auth'

const authStore = useAuthStore()

const initials = computed(() => {
  const name = authStore.user?.name || ''
  return name.split(' ').slice(-2).map(w => w[0]).join('').toUpperCase() || 'KH'
})

const showLogoutConfirm = ref(false)

async function doLogout() {
  showLogoutConfirm.value = false
  await authStore.logout()
}
</script>

<template>
  <div class="customer-home">
    <div class="welcome-card">
      <div class="avatar-circle">{{ initials }}</div>
      <h1>Xin chào, {{ authStore.user?.name }}!</h1>
      <p>Bạn đã đăng nhập thành công với tài khoản khách hàng.</p>
      <p class="muted">Trang khách hàng đang được phát triển...</p>
      <button class="logout-btn" @click="showLogoutConfirm = true">
        <i class="bi bi-box-arrow-right"></i> Đăng xuất
      </button>
    </div>
  </div>

  <!-- Modal xác nhận đăng xuất -->
  <Teleport to="body">
    <div v-if="showLogoutConfirm" class="logout-overlay" @click.self="showLogoutConfirm = false">
      <div class="logout-modal" role="dialog" aria-modal="true" aria-labelledby="logout-title">
        <div class="logout-icon">
          <i class="bi bi-box-arrow-right"></i>
        </div>
        <h3 id="logout-title">Xác nhận đăng xuất</h3>
        <p>Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?</p>
        <div class="logout-actions">
          <button class="btn-cancel" @click="showLogoutConfirm = false">Hủy</button>
          <button class="btn-confirm" @click="doLogout">Đăng xuất</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.customer-home {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a0533 0%, #3d1a6e 100%);
}

.welcome-card {
  background: #fff;
  border-radius: 16px;
  padding: 48px 40px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0, 0, 0, .3);
  max-width: 440px;
  width: 100%;
}

.avatar-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #7c4dff;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.welcome-card h1 {
  font-size: 22px;
  font-weight: 700;
  color: #1a2b3c;
  margin-bottom: 10px;
}

.welcome-card p { color: #4f5e6d; margin-bottom: 6px; }
.muted { color: #9aabbc !important; font-size: 13px; }

.logout-btn {
  margin-top: 24px;
  padding: 10px 28px;
  background: #7c4dff;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: background .2s;
}

.logout-btn:hover { background: #6a3de8; }

/* ── Modal ── */
.logout-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fade-in .15s ease;
}

.logout-modal {
  background: #fff;
  border-radius: 14px;
  padding: 36px 32px 28px;
  width: 340px;
  text-align: center;
  box-shadow: 0 16px 48px rgba(0, 0, 0, .2);
  animation: slide-up .2s ease;
}

.logout-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #fff1f1;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  font-size: 24px;
  color: #e53935;
}

.logout-modal h3 {
  font-size: 17px;
  font-weight: 700;
  color: #1a2b3c;
  margin: 0 0 8px;
}

.logout-modal p {
  font-size: 13.5px;
  color: #5f7384;
  margin: 0 0 24px;
}

.logout-actions {
  display: flex;
  gap: 10px;
}

.btn-cancel {
  flex: 1;
  height: 40px;
  border: 1.5px solid #d0dbe6;
  border-radius: 8px;
  background: #fff;
  color: #4f5e6d;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color .15s, color .15s;
}

.btn-cancel:hover { border-color: #7c4dff; color: #7c4dff; }

.btn-confirm {
  flex: 1;
  height: 40px;
  border: none;
  border-radius: 8px;
  background: #e53935;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s;
}

.btn-confirm:hover { background: #c62828; }

@keyframes fade-in {
  from { opacity: 0; }
  to   { opacity: 1; }
}

@keyframes slide-up {
  from { transform: translateY(16px); opacity: 0; }
  to   { transform: translateY(0);    opacity: 1; }
}
</style>
