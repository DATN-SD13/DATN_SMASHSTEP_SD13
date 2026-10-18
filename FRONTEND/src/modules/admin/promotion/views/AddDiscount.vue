<script setup>
import { useProductToast } from '../../product/composables/useProductToast'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import DiscountForm from '../components/DiscountForm.vue'
import { createDiscount } from '../services/discountService'
import ConfirmModal from '../components/ConfirmModal.vue'
import { validateDiscount } from '../utils/validateDiscount'

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
  return validateDiscount(form.value, true)
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
