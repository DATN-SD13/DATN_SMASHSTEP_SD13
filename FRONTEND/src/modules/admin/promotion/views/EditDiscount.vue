<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import api from '../../../../utils/api'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const id = route.params.id

// =========================
// FORM
// =========================

const form = ref({
  code: '',
  name: '',
  type: 'Công khai',
  discountType: 'Phần trăm (%)',
  value: '',
  min: '',
  max: '',
  quantity: '',
  start: '',
  end: '',
  status: 1,
  description: ''
})

// =========================
// STATE
// =========================

const loading = ref(false)
const saving = ref(false)

const errorMessage = ref('')
const successMessage = ref('')

// =========================
// COMPUTED
// =========================

const isPercent = computed(() => {
  return form.value.discountType === 'Phần trăm (%)'
})

// =========================
// LOAD DETAIL
// =========================

async function loadDetail() {

  try {

    loading.value = true
    errorMessage.value = ''

    const response = await api.get(
      `/phieu-giam-gia/${id}`
    )

    const data = response.data.data

    form.value = {
      code: data.code || '',
      name: data.name || '',

      type:
        data.form === 2
          ? 'Cá nhân'
          : 'Công khai',

      discountType:
        data.discountType === 2
          ? 'Tiền mặt (VNĐ)'
          : 'Phần trăm (%)',

      value:
        data.discountValue ?? '',

      min:
        data.minOrderValue ?? '',

      max:
        data.maxDiscount ?? '',

      quantity:
        data.quantity ?? '',

      start:
        data.startDate || '',

      end:
        data.endDate || '',

      status:
        data.status ?? 1,

      description:
        data.description || ''
    }

  } catch (error) {

    console.error(
      'Lỗi tải chi tiết phiếu giảm giá:',
      error
    )

    errorMessage.value =
      error?.response?.data?.message ||
      'Không thể tải thông tin phiếu giảm giá.'

  } finally {

    loading.value = false

  }
}

// =========================
// VALIDATE
// =========================

function validateForm() {

  errorMessage.value = ''

  if (!form.value.code.trim()) {
    errorMessage.value =
      'Vui lòng nhập mã phiếu.'
    return false
  }

  if (!form.value.name.trim()) {
    errorMessage.value =
      'Vui lòng nhập tên phiếu.'
    return false
  }

  if (
    form.value.value === '' ||
    Number(form.value.value) < 0
  ) {
    errorMessage.value =
      'Giá trị giảm không hợp lệ.'
    return false
  }

  if (
    isPercent.value &&
    Number(form.value.value) > 100
  ) {
    errorMessage.value =
      'Giá trị giảm phần trăm không được lớn hơn 100%.'
    return false
  }

  if (
    form.value.min !== '' &&
    Number(form.value.min) < 0
  ) {
    errorMessage.value =
      'Giá trị đơn tối thiểu không hợp lệ.'
    return false
  }

  if (
    form.value.max !== '' &&
    Number(form.value.max) < 0
  ) {
    errorMessage.value =
      'Giảm tối đa không hợp lệ.'
    return false
  }

  if (
    form.value.quantity === '' ||
    Number(form.value.quantity) < 0
  ) {
    errorMessage.value =
      'Số lượng không hợp lệ.'
    return false
  }

  if (!form.value.start) {
    errorMessage.value =
      'Vui lòng chọn ngày bắt đầu.'
    return false
  }

  if (!form.value.end) {
    errorMessage.value =
      'Vui lòng chọn ngày kết thúc.'
    return false
  }

  if (form.value.end < form.value.start) {
    errorMessage.value =
      'Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu.'
    return false
  }

  return true
}

// =========================
// UPDATE
// =========================

async function updateVoucher() {

  if (!validateForm()) {
    return
  }

  const confirmed = window.confirm(
    'Bạn có chắc chắn muốn cập nhật phiếu giảm giá này không?'
  )

  if (!confirmed) {
    return
  }

  try {

    saving.value = true
    errorMessage.value = ''
    successMessage.value = ''

    const request = {

      code:
        form.value.code.trim(),

      name:
        form.value.name.trim(),

      form:
        form.value.type === 'Công khai'
          ? 1
          : 2,

      discountType:
        form.value.discountType === 'Phần trăm (%)'
          ? 1
          : 2,

      discountValue:
        Number(form.value.value),

      minOrderValue:
        form.value.min === ''
          ? null
          : Number(form.value.min),

      maxDiscount:
        form.value.max === ''
          ? null
          : Number(form.value.max),

      startDate:
        form.value.start,

      endDate:
        form.value.end,

      quantity:
        Number(form.value.quantity),

      status:
        Number(form.value.status),

      description:
        form.value.description.trim()
    }

    await api.put(
      `/phieu-giam-gia/${id}`,
      request
    )

    successMessage.value =
      'Cập nhật phiếu giảm giá thành công.'

    setTimeout(() => {
      router.push('/giam-gia')
    }, 700)

  } catch (error) {

    console.error(
      'Lỗi cập nhật phiếu giảm giá:',
      error
    )

    errorMessage.value =
      error?.response?.data?.message ||
      'Không thể cập nhật phiếu giảm giá.'

  } finally {

    saving.value = false

  }
}

// =========================
// BACK
// =========================

function cancel() {
  router.back()
}

// =========================
// INIT
// =========================

onMounted(() => {
  loadDetail()
})
</script>

<template>
  <AdminLayout>

    <main class="ss-page">

      <!-- Loading -->

      <section
        v-if="loading"
        class="ss-card loading-card"
      >
        <div class="spinner-border"></div>

        <span>
          Đang tải thông tin phiếu giảm giá...
        </span>
      </section>


      <!-- FORM -->

      <section
        v-else
        class="ss-card ss-form"
      >

        <!-- Back -->

        <div>
          <button
            class="ss-back"
            aria-label="Quay lại"
            @click="cancel"
          >
            <i class="bi bi-arrow-left"></i>
          </button>
        </div>


        <!-- Header -->

        <div class="ss-head">

          <div class="ss-head-icon">
            <i class="bi bi-pencil-square"></i>
          </div>

          <div>
            <h2>Chỉnh sửa phiếu</h2>

            <p>
              Cập nhật thông tin phiếu giảm giá.
            </p>
          </div>

        </div>


        <!-- Error -->

        <div
          v-if="errorMessage"
          class="form-message error"
        >
          <i class="bi bi-exclamation-circle"></i>

          <span>
            {{ errorMessage }}
          </span>
        </div>


        <!-- Success -->

        <div
          v-if="successMessage"
          class="form-message success"
        >
          <i class="bi bi-check-circle"></i>

          <span>
            {{ successMessage }}
          </span>
        </div>


        <!-- Form -->

        <div class="form-grid">

          <!-- Mã -->

          <div class="ss-field">

            <label class="ss-label">
              Mã phiếu
              <span class="req">*</span>
            </label>

            <input
              class="ss-input"
              v-model="form.code"
              maxlength="50"
              :disabled="saving"
            />

          </div>


          <!-- Tên -->

          <div class="ss-field">

            <label class="ss-label">
              Tên phiếu
              <span class="req">*</span>
            </label>

            <input
              class="ss-input"
              v-model="form.name"
              maxlength="255"
              :disabled="saving"
            />

          </div>


          <!-- Hình thức -->

          <div class="ss-field">

            <span class="ss-label">
              Hình thức phiếu
            </span>

            <div class="ss-radios">

              <label>
                <input
                  type="radio"
                  value="Công khai"
                  v-model="form.type"
                  :disabled="saving"
                />

                Công khai
              </label>

              <label>
                <input
                  type="radio"
                  value="Cá nhân"
                  v-model="form.type"
                  :disabled="saving"
                />

                Cá nhân
              </label>

            </div>

          </div>


          <!-- Loại giảm -->

          <div class="ss-field">

            <span class="ss-label">
              Loại giảm
            </span>

            <div class="ss-radios">

              <label>
                <input
                  type="radio"
                  value="Phần trăm (%)"
                  v-model="form.discountType"
                  :disabled="saving"
                />

                Phần trăm (%)
              </label>

              <label>
                <input
                  type="radio"
                  value="Tiền mặt (VNĐ)"
                  v-model="form.discountType"
                  :disabled="saving"
                />

                Tiền mặt (VNĐ)
              </label>

            </div>

          </div>


          <!-- Giá trị -->

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
              :max="isPercent ? 100 : undefined"
              v-model="form.value"
              :disabled="saving"
            />

          </div>


          <!-- Tối thiểu -->

          <div class="ss-field">

            <label class="ss-label">
              Giá trị đơn tối thiểu (VNĐ)
            </label>

            <input
              class="ss-input"
              type="number"
              min="0"
              v-model="form.min"
              :disabled="saving"
            />

          </div>


          <!-- Tối đa -->

          <div class="ss-field">

            <label class="ss-label">
              Giảm tối đa (VNĐ)
            </label>

            <input
              class="ss-input"
              type="number"
              min="0"
              v-model="form.max"
              :disabled="saving || !isPercent"
            />

          </div>


          <!-- Số lượng -->

          <div class="ss-field">

            <label class="ss-label">
              Số lượng
              <span class="req">*</span>
            </label>

            <input
              class="ss-input"
              type="number"
              min="0"
              v-model="form.quantity"
              :disabled="saving"
            />

          </div>


          <!-- Ngày bắt đầu -->

          <div class="ss-field">

            <label class="ss-label">
              Ngày bắt đầu
              <span class="req">*</span>
            </label>

            <input
              class="ss-input"
              type="date"
              v-model="form.start"
              :disabled="saving"
            />

          </div>


          <!-- Ngày kết thúc -->

          <div class="ss-field">

            <label class="ss-label">
              Ngày kết thúc
              <span class="req">*</span>
            </label>

            <input
              class="ss-input"
              type="date"
              v-model="form.end"
              :disabled="saving"
            />

          </div>


          <!-- Trạng thái -->

          <div class="ss-field">

            <label class="ss-label">
              Trạng thái
            </label>

            <select
              class="ss-select"
              v-model="form.status"
              :disabled="saving"
            >
              <option :value="1">
                Hoạt động
              </option>

              <option :value="0">
                Ngừng hoạt động
              </option>
            </select>

          </div>


          <!-- Mô tả -->

          <div class="ss-field full">

            <label class="ss-label">
              Mô tả
            </label>

            <textarea
              class="ss-input ss-textarea"
              v-model="form.description"
              maxlength="1000"
              rows="4"
              :disabled="saving"
            ></textarea>

          </div>

        </div>


        <!-- Actions -->

        <div class="ss-actions left">

          <button
            class="ss-btn primary"
            :disabled="saving"
            @click="updateVoucher"
          >

            <span
              v-if="saving"
              class="spinner-border spinner-border-sm"
            ></span>

            <i
              v-else
              class="bi bi-check2"
            ></i>

            {{
              saving
                ? 'Đang lưu...'
                : 'Lưu thay đổi'
            }}

          </button>


          <button
            class="ss-btn"
            :disabled="saving"
            @click="cancel"
          >
            Hủy
          </button>

        </div>

      </section>

    </main>

  </AdminLayout>
</template>


<style scoped>

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 24px;
}

.ss-field.full {
  grid-column: 1 / -1;
}

.ss-textarea {
  resize: vertical;
  min-height: 100px;
}

.loading-card {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.form-message {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 12px 14px;
  border-radius: 8px;
  margin-bottom: 18px;
}

.form-message.error {
  color: #b42318;
  background: #fef3f2;
  border: 1px solid #fecdca;
}

.form-message.success {
  color: #067647;
  background: #ecfdf3;
  border: 1px solid #abefc6;
}

@media (max-width: 760px) {

  .form-grid {
    grid-template-columns: 1fr;
  }

  .ss-field.full {
    grid-column: auto;
  }

}

</style>