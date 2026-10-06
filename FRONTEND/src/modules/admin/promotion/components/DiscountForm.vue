<script setup>
import { computed } from 'vue'

const props = defineProps({
  form: {
    type: Object,
    required: true
  },

  editMode: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits([
  'submit',
  'cancel'
])

const isPercent = computed(() => {
  return props.form.discountType === 1
})
</script>

<template>
  <section class="ss-card ss-form">

    <div class="ss-head">

      <div class="ss-head-icon">
        <i class="bi bi-ticket-perforated"></i>
      </div>

      <h2>Thông tin phiếu</h2>

    </div>

    <div class="form-grid">

      <div class="ss-field">

        <label class="ss-label">
          Mã phiếu
          <span class="req">*</span>
        </label>

        <input
          class="ss-input"
          v-model="form.code"
          :disabled="editMode"
        />

      </div>

      <div class="ss-field">

        <label class="ss-label">
          Tên phiếu
          <span class="req">*</span>
        </label>

        <input
          class="ss-input"
          v-model="form.name"
          placeholder="Ví dụ: Giảm giá hè 2024"
        />

      </div>

      <div class="ss-field">

        <span class="ss-label">
          Hình thức phiếu
        </span>

        <div class="ss-radios">

          <label>
            <input
              type="radio"
              :value="1"
              v-model="form.form"
            />
            Công khai
          </label>

          <label>
            <input
              type="radio"
              :value="2"
              v-model="form.form"
            />
            Cá nhân
          </label>

        </div>

      </div>

      <div class="ss-field">

        <span class="ss-label">
          Loại giảm
        </span>

        <div class="ss-radios">

          <label>
            <input
              type="radio"
              :value="1"
              v-model="form.discountType"
            />
            Phần trăm (%)
          </label>

          <label>
            <input
              type="radio"
              :value="2"
              v-model="form.discountType"
            />
            Tiền mặt (VNĐ)
          </label>

        </div>

      </div>

      <div class="ss-field">

        <label class="ss-label">
          Giá trị giảm
          ({{ isPercent ? '%' : 'VNĐ' }})

          <span class="req">*</span>
        </label>

        <input
          class="ss-input"
          type="number"
          min="0"
          v-model.number="form.discountValue"
        />

      </div>

      <div class="ss-field">

        <label class="ss-label muted">
          Giá trị đơn tối thiểu (VNĐ)
        </label>

        <input
          class="ss-input"
          type="number"
          min="0"
          v-model.number="form.minOrderValue"
        />

      </div>

      <div class="ss-field">

        <label class="ss-label muted">
          Giảm tối đa (VNĐ)
        </label>

        <input
          class="ss-input"
          type="number"
          min="0"
          v-model.number="form.maxDiscount"
        />

        <span class="ss-hint warn">
          <i class="bi bi-exclamation-triangle"></i>
          Nếu không giới hạn, để trống hoặc 0.
        </span>

      </div>

      <div class="ss-field">

        <label class="ss-label">
          Số lượng
          <span class="req">*</span>
        </label>

        <input
          class="ss-input"
          type="number"
          min="1"
          v-model.number="form.quantity"
        />

      </div>

      <div class="ss-field">

        <label class="ss-label">
          Ngày bắt đầu
          <span class="req">*</span>
        </label>

        <input
          class="ss-input"
          type="date"
          v-model="form.startDate"
        />

      </div>

      <div class="ss-field">

        <label class="ss-label">
          Ngày kết thúc
          <span class="req">*</span>
        </label>

        <input
          class="ss-input"
          type="date"
          v-model="form.endDate"
        />

      </div>

    </div>

    <div class="ss-actions left">

      <button
        class="ss-btn primary"
        type="button"
        @click="emit('submit')"
      >
        <i class="bi bi-check2"></i>

        {{ editMode
          ? 'Lưu thay đổi'
          : 'Tạo phiếu giảm giá'
        }}
      </button>

      <button
        class="ss-btn"
        type="button"
        @click="emit('cancel')"
      >
        Hủy
      </button>

    </div>

  </section>
</template>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 24px;
}

.ss-label.muted {
  font-weight: 500;
  color: var(--ss-muted);
}

.ss-hint i {
  margin-right: 3px;
}

@media (max-width: 760px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>