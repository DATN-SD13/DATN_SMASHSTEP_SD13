<script setup>
import { useProductToast } from '../../product/composables/useProductToast'
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

const { show: showToast } = useProductToast()

function showLocalToast(type, title, message) { showToast(type, message, title) }


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
    const response = await createDiscount({
      ...form.value
    })

    showConfirm.value = false

    if (response.data?.success !== true) throw new Error(response.data?.message || 'Lưu phiếu thất bại')
    const email = response.data?.data?.emailResult
    showLocalToast(email?.failed > 0 ? 'warning' : 'success',
      email?.failed > 0 ? 'Đã lưu phiếu; email chưa gửi đủ' : 'Thành công',
      'Tạo phiếu giảm giá thành công!' + (email && email.status !== 'NOT_REQUESTED' ? ' ' + email.message : ''))

    setTimeout(() => {
      router.push('/giam-gia')
    }, 1000)

  } catch (error) {
    console.error(error)

    showConfirm.value = false

    let message = 'Tạo phiếu giảm giá thất bại!'
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      message = 'Hết thời gian chờ phản hồi từ máy chủ (Backend phản hồi quá lâu hoặc chưa được khởi động)!'
    } else if (!error.response) {
      message = 'Không thể kết nối đến máy chủ Backend (vui lòng kiểm tra server port 8080)!'
    } else if (error.response?.data?.message) {
      message = error.response.data.message
    }

    showLocalToast(
      'error',
      'Tạo thất bại',
      message
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
