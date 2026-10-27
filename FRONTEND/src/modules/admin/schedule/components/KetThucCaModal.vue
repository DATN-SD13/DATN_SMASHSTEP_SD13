<template>
  <div v-if="show" class="modal-overlay" @click.self="handleCancel">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title">Kết thúc ca làm việc</h5>
        <button type="button" class="btn-close" @click="handleCancel" aria-label="Close"></button>
      </div>
      
      <div class="modal-body">
        <div v-if="caData" class="shift-info-section">
          <div class="info-row">
            <span class="label">Ca làm việc:</span>
            <span class="value">{{ caData.tenCa }}</span>
          </div>
          <div class="info-row">
            <span class="label">Thời gian:</span>
            <span class="value">{{ caData.gioBatDau }} - {{ caData.gioKetThuc }}</span>
          </div>
          <div class="info-row">
            <span class="label">Ngày làm:</span>
            <span class="value">{{ formatDate(caData.ngayLam) }}</span>
          </div>
        </div>

        <div class="financial-section">
          <div class="info-row">
            <span class="label">Tiền mặt cuối ca:</span>
            <span class="value money">{{ formatMoney(caData?.tienMatCuoiCa || 0) }}</span>
          </div>
          <div class="info-row">
            <span class="label">Chuyển khoản trên hệ thống:</span>
            <span class="value money">{{ formatMoney(caData?.chuyenKhoanCuoiCa || 0) }}</span>
          </div>
          <div class="info-row total">
            <span class="label">Tổng doanh thu:</span>
            <span class="value money">{{ formatMoney((caData?.tienMatCuoiCa || 0) + (caData?.chuyenKhoanCuoiCa || 0)) }}</span>
          </div>
        </div>

        <div class="note-section">
          <label for="ghiChuKetCa" class="form-label">Ghi chú kết ca (không bắt buộc)</label>
          <textarea
            id="ghiChuKetCa"
            v-model="ghiChu"
            class="form-control"
            rows="2"
            placeholder="Nhập ghi chú về ca làm việc..."
          ></textarea>
        </div>
      </div>

      <div class="modal-footer">
        <button type="button" class="btn btn-secondary" @click="handleCancel">
          <i class="bi bi-x-circle"></i> Hủy
        </button>
        <button type="button" class="btn btn-primary" @click="handleConfirm">
          <i class="bi bi-check-circle"></i> Xác nhận kết ca
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  caData: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'confirm'])

const ghiChu = ref('')

// Reset ghi chú khi modal đóng/mở
watch(() => props.show, (newVal) => {
  if (!newVal) {
    ghiChu.value = ''
  }
})

const handleCancel = () => {
  emit('close')
}

const handleConfirm = () => {
  emit('confirm', {
    ghiChu: ghiChu.value.trim()
  })
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric'
  })
}

const formatMoney = (amount) => {
  if (amount == null) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount)
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease-in-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 480px;
  max-height: 85vh;
  overflow-y: auto;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  animation: slideDown 0.3s ease-out;
}

@keyframes slideDown {
  from {
    transform: translateY(-50px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #e9ecef;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-title {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #2c3e50;
}

.btn-close {
  background: transparent;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #6c757d;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.btn-close:hover {
  background-color: #f8f9fa;
  color: #000;
}

.btn-close::before {
  content: '×';
  font-size: 2rem;
  line-height: 1;
}

.modal-body {
  padding: 1.25rem;
}

.shift-info-section,
.financial-section {
  margin-bottom: 1rem;
  padding: 0.875rem;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.financial-section {
  background-color: #e7f3ff;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.4rem 0;
}

.info-row:not(:last-child) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.info-row.total {
  margin-top: 0.5rem;
  padding-top: 0.5rem;
  border-top: 2px solid #0d6efd;
  font-weight: 600;
}

.label {
  color: #6c757d;
  font-size: 0.875rem;
}

.value {
  font-weight: 500;
  color: #2c3e50;
  font-size: 0.875rem;
}

.value.money {
  color: #0d6efd;
  font-size: 0.95rem;
  font-weight: 600;
}

.info-row.total .value.money {
  font-size: 1.05rem;
}

.note-section {
  margin-bottom: 0.75rem;
}

.form-label {
  display: block;
  margin-bottom: 0.4rem;
  font-weight: 500;
  color: #495057;
  font-size: 0.875rem;
}

.form-control {
  width: 100%;
  padding: 0.625rem;
  border: 1px solid #ced4da;
  border-radius: 6px;
  font-size: 0.875rem;
  transition: border-color 0.15s ease-in-out, box-shadow 0.15s ease-in-out;
  font-family: inherit;
  resize: vertical;
  min-height: 70px;
}

.form-control:focus {
  border-color: #80bdff;
  outline: 0;
  box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.25);
}

.form-control::placeholder {
  color: #adb5bd;
}

.modal-footer {
  padding: 0.875rem 1.25rem;
  border-top: 1px solid #e9ecef;
  display: flex;
  justify-content: flex-end;
  gap: 0.625rem;
}

.btn {
  padding: 0.5rem 1.125rem;
  border: none;
  border-radius: 6px;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
}

.btn-secondary {
  background-color: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background-color: #5a6268;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(108, 117, 125, 0.3);
}

.btn-primary {
  background-color: #0d6efd;
  color: white;
}

.btn-primary:hover {
  background-color: #0b5ed7;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(13, 110, 253, 0.3);
}

.btn:active {
  transform: translateY(0);
}

/* Responsive */
@media (max-width: 576px) {
  .modal-content {
    width: 95%;
    max-height: 95vh;
  }

  .modal-header {
    padding: 1rem;
  }

  .modal-body {
    padding: 1rem;
  }

  .modal-footer {
    padding: 0.75rem 1rem;
    flex-direction: column;
  }

  .btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
