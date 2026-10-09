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
  description: '',
  customerIds: []
  
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
  const f = form.value

  /*hinh thuc va loai */
  if (![1, 2].includes(Number(f.form))) {
    return 'Hình thức phiếu giảm giá không hợp lệ!'
  }

  if (![1, 2].includes(Number(f.discountType))) {
    return 'Loại giảm giá không hợp lệ!'
  }

  // 2. Kiểm tra tên và mã phiếu
  if (!f.name?.trim()) {
    return 'Vui lòng nhập tên phiếu!'
  }

  if (!f.code?.trim()) {
    return 'Mã phiếu chưa được tạo!'
  }

  /*ltra gia tri giam*/
  const discountValue = Number(f.discountValue)
  const minOrderValue = Number(f.minOrderValue)
  const maxDiscount = Number(f.maxDiscount)

  if (
    f.discountValue == null ||
    f.discountValue === '' ||
    !Number.isFinite(discountValue) ||
    discountValue <= 0
  ) {
    return 'Giá trị giảm phải lớn hơn 0!'
  }

  if (Number(f.discountType) === 1 && discountValue > 100) {
    return 'Phần trăm giảm không được vượt quá 100%!'
  }

  /*ktra dkien giam*/
  if (
    f.minOrderValue == null ||
    f.minOrderValue === '' ||
    !Number.isFinite(Number(f.minOrderValue)) ||
    Number(f.minOrderValue) < 0
  ) {
    return 'Giá trị đơn tối thiểu phải lớn hơn hoặc bằng 0!'
  }

/*ktra giam*/
  if (
    f.maxDiscount == null ||
    f.maxDiscount === '' ||
    !Number.isFinite(Number(f.maxDiscount)) ||
    Number(f.maxDiscount) < 0
  ) {
    return 'Giảm tối đa phải lớn hơn hoặc bằng 0!'
  }

  if (
    Number(f.discountType) === 1 &&
    Number(f.maxDiscount) <= 0
  ) {
    return 'Giảm theo phần trăm phải có mức giảm tối đa lớn hơn 0!'
  }

  if (
    Number(f.discountType) === 1 &&
    Number(f.maxDiscount) > Number(f.minOrderValue)
  ) {
    return 'Giảm tối đa không được lớn hơn giá trị đơn tối thiểu!'
  }

  /* so luong*/
  if (Number(f.form) === 2) {
    if (!Array.isArray(f.customerIds) || f.customerIds.length === 0) {
      return 'Vui lòng chọn khách hàng nhận phiếu!'
    }
  } else if (!f.unlimited) {
    const quantity = Number(f.quantity)

    if (
      f.quantity == null ||
      f.quantity === '' ||
      !Number.isInteger(quantity) ||
      quantity <= 0
    ) {
      return 'Số lượng phải là số nguyên lớn hơn 0!'
    }
  }

  /*ngay gio*/
  if (!f.startDate || !f.endDate) {
    return 'Vui lòng nhập đầy đủ ngày bắt đầu và ngày kết thúc!'
  }

  const start = new Date(f.startDate)
  const end = new Date(f.endDate)

  if (Number.isNaN(start.getTime())) {
    return 'Ngày giờ bắt đầu không hợp lệ!'
  }

  if (Number.isNaN(end.getTime())) {
    return 'Ngày giờ kết thúc không hợp lệ!'
  }

  if (end <= start) {
    return 'Ngày giờ kết thúc phải sau ngày giờ bắt đầu!'
  }

  return null
}


function save() {
  if (saving.value) return
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
  if (saving.value) return
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
        :saving="saving"
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
