<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../../../../utils/api'

const props = defineProps({
  form: {
    type: Object,
    required: true
  },

  editMode: {
    type: Boolean,
    default: false
  },
  saving: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits([
  'submit',
  'cancel',
  'generate-code'
])

const isPercent = computed(() => {
  return props.form.discountType === 1
})

const customers = ref([])
const formSupported = ref(false)
const capabilityError = ref('')
onMounted(async () => {
  try {
    const response = await api.get('/phieu-giam-gia/capabilities')
    formSupported.value = response.data?.data?.formSupported === true
    if (!formSupported.value) capabilityError.value = 'Cấu hình hiện tại chưa hỗ trợ lưu hình thức phiếu. Không thể tạo hoặc sửa phiếu.'
  } catch { capabilityError.value = 'Không thể kiểm tra khả năng lưu phiếu. Vui lòng tải lại trang.' }
})
const loadingCustomers = ref(false)
const customerError = ref('')
watch(() => props.form.form, async value => {
  if (value !== 2 || customers.value.length || loadingCustomers.value) return
  loadingCustomers.value = true
  customerError.value = ''
  try {
    const result = []
    let nextPage = 1
    let pages = 1
    do {
      const response = await api.get('/khach-hang', { params: { trangThai: 1, page: nextPage, size: 100 } })
      const data = response.data?.data
      result.push(...(data?.content || []))
      pages = data?.totalPages ?? 1
      nextPage++
    } while (nextPage <= pages)
    customers.value = result
  } catch (error) {
    customerError.value = error.response?.data?.message || 'Không thể tải khách hàng nhận phiếu.'
  } finally {
    loadingCustomers.value = false
  }
}, { immediate: true })
</script>

<template>
  <section class="ss-card ss-form">
    <p v-if="capabilityError" class="ss-hint warn" role="alert">{{ capabilityError }}</p>

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

        <div class="code-input-wrapper">

          <input
            class="ss-input"
            v-model="form.code"
            readonly
            placeholder="Tự động tạo"
          />

          <button
            v-if="!editMode"
            type="button"
            class="generate-code-btn"
            title="Tạo mã mới"
            @click="emit('generate-code')"
          >
            <i class="bi bi-arrow-clockwise"></i>
          </button>

        </div>

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

      <div v-if="form.form === 2" class="ss-field full-width">
        <label class="ss-label" for="voucher-customers">Khách hàng nhận phiếu <span class="req">*</span></label>
        <select id="voucher-customers" class="ss-select" multiple v-model="form.customerIds" :disabled="loadingCustomers || saving">
          <option v-for="customer in customers" :key="customer.id" :value="customer.id">
            {{ customer.code || '—' }} — {{ customer.name || '—' }}
          </option>
        </select>
        <span v-if="loadingCustomers" class="ss-hint">Đang tải khách hàng...</span>
        <span v-else-if="customerError" class="ss-hint warn" role="alert">{{ customerError }}</span>
        <span v-else class="ss-hint">Chọn ít nhất một khách hàng. Giữ Ctrl để chọn nhiều.</span>
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
          min="0.01"
          step="0.01"
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
          <span
            v-if="!form.unlimited"
            class="req"
          >*</span>
        </label>

        <input
          v-if="!form.unlimited"
          class="ss-input"
          type="number"
          min="1"
          v-model.number="form.quantity"
        />

        <div
          v-else
          class="ss-input"
          style="background: #f5f5f5;"
        >
          Không giới hạn
        </div>

        <label style="margin-top: 8px;">
          <input
            type="checkbox"
            v-model="form.unlimited"
          />
          Không giới hạn số lượng
        </label>

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

      <div class="ss-field full-width">

        <label class="ss-label">
          Mô tả phiếu giảm giá
        </label>

        <textarea
          class="ss-input ss-textarea"
          v-model="form.description"
          rows="4"
          placeholder="Nhập mô tả cho phiếu giảm giá..."
        ></textarea>

      </div>
    </div>

    <div class="ss-actions left">

      <button
        class="ss-btn primary"
        type="button"
        :disabled="saving || !formSupported || (form.form === 2 && (loadingCustomers || !!customerError))"
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
        :disabled="saving"
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
