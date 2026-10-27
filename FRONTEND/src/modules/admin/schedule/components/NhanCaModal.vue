<script setup>
import { ref, computed } from 'vue'
import api from '../../../../utils/api'

const props = defineProps({
  data: { type: Object, required: true }
})

const emit = defineEmits(['close', 'confirmed', 'rejected'])

const loading  = ref(false)
const error    = ref('')

function fmt(val) {
  if (val == null) return '0 đ'
  return Number(val).toLocaleString('vi-VN') + ' đ'
}

const tongTien = computed(() =>
  (Number(props.data.tienMatDauCa || 0) + Number(props.data.chuyenKhoanDauCa || 0))
)

async function xacNhan() {
  error.value   = ''
  loading.value = true
  try {
    await api.post(`/lich-lam-viec/${props.data.lichLamViecId}/xac-nhan-nhan-ca`)
    emit('confirmed')
  } catch (e) {
    error.value = e?.response?.data?.message || 'Xác nhận thất bại, vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}

async function chuaNhan() {
  error.value   = ''
  loading.value = true
  try {
    await api.post(`/lich-lam-viec/${props.data.lichLamViecId}/chua-nhan-ca`)
    emit('rejected')
  } catch (e) {
    error.value = e?.response?.data?.message || 'Thao tác thất bại, vui lòng thử lại.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="nhanCa-overlay" role="dialog" aria-modal="true" aria-labelledby="nhanCa-title">
      <div class="nhanCa-modal">

        <!-- Header -->
        <div class="nhanCa-header">
          <div class="nhanCa-icon">
            <i class="bi bi-arrow-left-right"></i>
          </div>
          <h2 id="nhanCa-title" class="nhanCa-title">Nhận Ca Làm Việc</h2>
          <p class="nhanCa-sub">
            Ca <strong>{{ data.tenCa }}</strong>
            ({{ data.gioBatDau }} – {{ data.gioKetThuc }}) sắp bắt đầu
          </p>
        </div>

        <!-- Thông tin ca trước -->
        <div v-if="data.tenCaTruoc" class="nhanCa-info-box">
          <i class="bi bi-info-circle-fill"></i>
          <div>
            Vui lòng kiểm đếm tiền trước khi nhận ca.
            <br/>
            Tiền mặt bàn giao từ ca trước: <strong>{{ fmt(data.tienMatCaTruoc) }}</strong>
            <br/>
            Tiền chuyển khoản bàn giao từ ca trước: <strong>{{ fmt(data.chuyenKhoanCaTruoc) }}</strong>
          </div>
        </div>
        <div v-else class="nhanCa-info-box nhanCa-info-box--neutral">
          <i class="bi bi-info-circle-fill"></i>
          <div>Đây là ca đầu tiên. Không có dữ liệu ca trước.</div>
        </div>

        <!-- Các trường tiền -->
        <div class="nhanCa-fields">
          <div class="nhanCa-field">
            <label>Tiền mặt đầu ca <span class="req">*</span></label>
            <div class="nhanCa-value">
              <span>{{ fmt(data.tienMatDauCa) }}</span>
            </div>
          </div>

          <div class="nhanCa-field">
            <label>Tiền chuyển khoản đầu ca <span class="req">*</span></label>
            <div class="nhanCa-value">
              <span>{{ fmt(data.chuyenKhoanDauCa) }}</span>
            </div>
          </div>

          <div class="nhanCa-field nhanCa-field--total">
            <label>Tổng tiền</label>
            <div class="nhanCa-value nhanCa-value--total">
              <span>{{ fmt(tongTien) }}</span>
            </div>
          </div>
        </div>

        <!-- Lỗi -->
        <p v-if="error" class="nhanCa-error">
          <i class="bi bi-exclamation-circle"></i> {{ error }}
        </p>

        <!-- Actions -->
        <div class="nhanCa-actions">
          <button
            class="btn-chua-nhan"
            :disabled="loading"
            @click="chuaNhan"
          >
            <i class="bi bi-x-circle"></i>
            Chưa nhận ca
          </button>
          <button
            class="btn-xac-nhan"
            :disabled="loading"
            @click="xacNhan"
          >
            <span v-if="loading">
              <i class="bi bi-arrow-repeat spin"></i> Đang xử lý…
            </span>
            <span v-else>
              <i class="bi bi-check-circle"></i>
              Xác Nhận Nhận Ca
            </span>
          </button>
        </div>

      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.nhanCa-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  animation: fade-in 0.2s ease;
}

.nhanCa-modal {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 440px;
  padding: 28px 28px 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  animation: slide-up 0.22s ease;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* Header */
.nhanCa-header { text-align: center; }

.nhanCa-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #e3f2fd;
  color: #1565c0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-bottom: 12px;
}

.nhanCa-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a2b3c;
  margin: 0 0 6px;
}

.nhanCa-sub {
  font-size: 13px;
  color: #5f7384;
  margin: 0;
  line-height: 1.5;
}

/* Info box */
.nhanCa-info-box {
  background: #fff8e1;
  border: 1px solid #ffe082;
  border-radius: 8px;
  padding: 12px 14px;
  font-size: 13px;
  color: #5d4037;
  line-height: 1.6;
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.nhanCa-info-box--neutral {
  background: #f0f7ff;
  border-color: #90caf9;
  color: #1565c0;
}

.nhanCa-info-box i {
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 1px;
  color: #f57f17;
}

.nhanCa-info-box--neutral i {
  color: #1565c0;
}

/* Fields */
.nhanCa-fields {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nhanCa-field label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: #5f7384;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.req { color: #e53935; }

.nhanCa-value {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  background: #f8fafc;
  border: 1.5px solid #d0dbe6;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 15px;
  font-weight: 600;
  color: #1a2b3c;
}

.nhanCa-field--total .nhanCa-value {
  background: #e3f2fd;
  border-color: #90caf9;
}

.nhanCa-value--total {
  font-size: 16px;
  color: #0d47a1;
}

/* Error */
.nhanCa-error {
  background: #fff1f1;
  border-radius: 7px;
  padding: 9px 12px;
  font-size: 13px;
  color: #c62828;
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0;
}

/* Actions */
.nhanCa-actions {
  display: flex;
  gap: 10px;
}

.btn-chua-nhan {
  flex: 1;
  height: 44px;
  border: 1.5px solid #d0dbe6;
  border-radius: 8px;
  background: #fff;
  color: #5f7384;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: border-color 0.15s, color 0.15s;
}

.btn-chua-nhan:hover:not(:disabled) {
  border-color: #e53935;
  color: #e53935;
}

.btn-chua-nhan:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-xac-nhan {
  flex: 2;
  height: 44px;
  border: none;
  border-radius: 8px;
  background: #168dcc;
  color: #fff;
  font-size: 13.5px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: background 0.15s;
}

.btn-xac-nhan:hover:not(:disabled) { background: #117ab5; }
.btn-xac-nhan:disabled { opacity: 0.6; cursor: not-allowed; }

.spin {
  display: inline-block;
  animation: spin-kf 0.8s linear infinite;
}

@keyframes spin-kf { to { transform: rotate(360deg); } }

@keyframes fade-in {
  from { opacity: 0; }
  to   { opacity: 1; }
}

@keyframes slide-up {
  from { transform: translateY(20px); opacity: 0; }
  to   { transform: translateY(0);    opacity: 1; }
}
</style>
