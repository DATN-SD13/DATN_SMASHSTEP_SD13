<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import DiscountForm from '../components/DiscountForm.vue'
import { createDiscount } from '../services/discountService'
import ConfirmModal from '../components/ConfirmModal.vue'

const router = useRouter()

const showConfirm = ref(false)
const saving = ref(false)

const form = ref({
  code: '',
  name: '',
  form: 1,
  discountType: 1,
  discountValue: 0,
  minOrderValue: 0,
  maxDiscount: 0,
  quantity: 0,
  unlimited: false,
  startDate: '',
  endDate: '',
  status: 1,
  description: ''
  
})

const toast = ref({
  visible: false,
  type: 'success',
  title: '',
  message: ''
})

let toastTimer = null

function showLocalToast(type, title, message) {
  toast.value = {
    visible: true,
    type,
    title,
    message
  }

  if (toastTimer) {
    window.clearTimeout(toastTimer)
  }

  toastTimer = window.setTimeout(() => {
    toast.value.visible = false
  }, 3000)
}


function generateCode() {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
  let code = 'VCH'

  for (let i = 0; i < 6; i++) {
    code += chars.charAt(
      Math.floor(Math.random() * chars.length)
    )
  }

  form.value.code = code
}

generateCode()

function validate() {

  if (!form.value.name?.trim()) {
    return 'Vui lòng nhập tên phiếu!'
  }

  if (!form.value.code?.trim()) {
    return 'Mã phiếu chưa được tạo!'
  }

  if (
    form.value.discountValue == null ||
    form.value.discountValue < 0
  ) {
    return 'Giá trị giảm không hợp lệ!'
  }

  if (
    form.value.discountType === 1 &&
    form.value.discountValue > 100
  ) {
    return 'Phần trăm giảm không được vượt quá 100%!'
  }

  if (
    form.value.minOrderValue != null &&
    form.value.minOrderValue < 0
  ) {
    return 'Giá trị đơn tối thiểu không hợp lệ!'
  }

  if (
    form.value.maxDiscount != null &&
    form.value.maxDiscount < 0
  ) {
    return 'Giảm tối đa không hợp lệ!'
  }

  if (!form.value.unlimited) {

    if (
      !form.value.quantity ||
      form.value.quantity < 1
    ) {
      return 'Số lượng phải lớn hơn 0!'
    }

  }

  if (
    !form.value.startDate ||
    !form.value.endDate
  ) {
    return 'Vui lòng nhập đầy đủ ngày bắt đầu và ngày kết thúc!'
  }

  if (
    form.value.endDate <
    form.value.startDate
  ) {
    return 'Ngày kết thúc phải sau ngày bắt đầu!'
  }

  return null
}


function save() {
  const message = validate()

  if (message) {
    showLocalToast(
      'error',
      'Dữ liệu không hợp lệ',
      message
    )
    return
  }

  showConfirm.value = true
}

async function handleConfirm() {
  saving.value = true

  try {
    await createDiscount({
      ...form.value
    })

    showConfirm.value = false

    showLocalToast(
      'success',
      'Tạo thành công',
      'Tạo phiếu giảm giá thành công!'
    )

    setTimeout(() => {
      router.push('/giam-gia')
    }, 1000)

  } catch (error) {
    console.error(error)

    showConfirm.value = false

    showLocalToast(
      'error',
      'Tạo thất bại',
      error.response?.data?.message ||
      'Tạo phiếu giảm giá thất bại!'
    )
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <AdminLayout>

    <main class="ss-page">

      <!-- nut back -->
      <div>
        <button
          class="ss-back"
          type="button"
          aria-label="Quay lại"
          @click="router.back()"
        >
          <i class="bi bi-arrow-left"></i>
        </button>
      </div>

      <!-- form them phieu -->
      <DiscountForm
        :form="form"
        :edit-mode="false"
        @submit="save"
        @cancel="router.back()"
        @generate-code="generateCode"
      />

    </main>

    <!-- thong bao popup -->

    <div
      v-if="toast.visible"
      class="ss-toast"
      :class="`ss-toast-${toast.type}`"
    >

      <!-- Icon -->
      <div class="ss-toast-icon">

        <i
          v-if="toast.type === 'success'"
          class="bi bi-check-circle-fill"
        ></i>

        <i
          v-else
          class="bi bi-x-circle-fill"
        ></i>

      </div>

      <!-- noi dung -->
      <div class="ss-toast-content">

        <strong>
          {{ toast.title }}
        </strong>

        <span>
          {{ toast.message }}
        </span>

      </div>

      <!-- dong -->
      <button
        type="button"
        class="ss-toast-close"
        @click="toast.visible = false"
      >
        ×
      </button>

      
    </div>
    
    <ConfirmModal
            :visible="showConfirm"
            title="Xác nhận thêm"
            message="Bạn có chắc chắn muốn thêm phiếu giảm giá này không?"
            confirm-text="Đồng ý"
            :loading="saving"
            @confirm="handleConfirm"
            @cancel="showConfirm = false"
          />
  </AdminLayout>
</template>