<script setup>
import { useProductToast } from '../../product/composables/useProductToast'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DiscountForm from '../components/DiscountForm.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { getDiscountById, updateDiscount } from '../services/discountService'
import { validateDiscount } from '../utils/validateDiscount'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const saving = ref(false)
const showConfirm = ref(false)

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

function toDate(value) {
  if (!value) return ''

  return String(value).substring(0, 16)
}

async function loadDetail() {
  try {
    const response =
      await getDiscountById(route.params.id)

    const data = response.data.data

    form.value = {
      code: data.code || '',
      name: data.name || '',
      form: data.form ?? 1,
      discountType: data.discountType ?? 1,
      discountValue: data.discountValue ?? 0,
      minOrderValue: data.minOrderValue ?? 0,
      maxDiscount: data.maxDiscount ?? 0,
      quantity: data.quantity ?? 0,
      unlimited: data.unlimited ?? false,
      startDate: toDate(data.startDate),
      endDate: toDate(data.endDate),
      status: data.status ?? 1,
      description: data.description || '',
      customerIds: data.customerIds || []
    }

  } catch (error) {
    console.error(error)

    showLocalToast(
      'error',
      'Tải dữ liệu thất bại',
      error.response?.data?.message ||
      'Không thể tải phiếu giảm giá!'
    )

    setTimeout(() => {
      router.push('/giam-gia')
    }, 1000)

  } finally {
    loading.value = false
  }
}

function validate() {
  return validateDiscount(form.value, false)
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
    const response = await updateDiscount(
      route.params.id,
      {
        ...form.value
      }
    )

    showConfirm.value = false

    if (response.data?.success !== true) throw new Error(response.data?.message || 'Lưu phiếu thất bại')
    const email = response.data?.data?.emailResult
    showLocalToast(email?.failed > 0 ? 'warning' : 'success',
      email?.failed > 0 ? 'Đã lưu phiếu; email chưa gửi đủ' : 'Thành công',
      'Cập nhật phiếu giảm giá thành công!' + (email && email.status !== 'NOT_REQUESTED' ? ' ' + email.message : ''))

    setTimeout(() => {
      router.push('/giam-gia')
    }, 1000)

  } catch (error) {
    console.error(error)

    showConfirm.value = false

    let message = 'Cập nhật phiếu giảm giá thất bại!'
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      message = 'Hết thời gian chờ phản hồi từ máy chủ (Backend phản hồi quá lâu hoặc chưa được khởi động)!'
    } else if (!error.response) {
      message = 'Không thể kết nối đến máy chủ Backend (vui lòng kiểm tra server port 8080)!'
    } else if (error.response?.data?.message) {
      message = error.response.data.message
    }

    showLocalToast(
      'error',
      'Cập nhật thất bại',
      message
    )

  } finally {
    saving.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <AdminLayout>

    <main class="ss-page">

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

      <div
        v-if="loading"
        class="ss-card ss-empty"
      >
        <i class="bi bi-arrow-repeat"></i>
        Đang tải dữ liệu...
      </div>

      <DiscountForm
        v-else
        :form="form"
        :saving="saving"
        :edit-mode="true"
        @submit="save"
        @cancel="router.back()"
      />

    </main>

    <!-- tbao xac nhan -->
    <ConfirmModal
      :visible="showConfirm"
      title="Xác nhận cập nhật"
      message="Bạn có chắc chắn muốn cập nhật phiếu giảm giá này không?"
      confirm-text="Cập nhật"
      :loading="saving"
      @confirm="handleConfirm"
      @cancel="showConfirm = false"
    />

    <!-- tbao popup -->
    

  </AdminLayout>
</template>
