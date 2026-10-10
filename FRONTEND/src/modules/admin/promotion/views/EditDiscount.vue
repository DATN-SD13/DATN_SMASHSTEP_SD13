<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import DiscountForm from '../components/DiscountForm.vue'
import ConfirmModal from '../components/ConfirmModal.vue'

import {
  getDiscountById,
  updateDiscount
} from '../services/discountService'

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
  if (![1, 2].includes(Number(form.value.form)) || ![1, 2].includes(Number(form.value.discountType))) return 'Hình thức hoặc loại giảm không hợp lệ!'
  for (const field of ['discountValue', 'minOrderValue', 'maxDiscount']) {
    const value = form.value[field]
    if (value == null || value === '' || !Number.isFinite(Number(value)) || Number(value) < 0) return 'Giá trị giảm và các giới hạn phải là số hợp lệ!'
  }
  if (!form.value.unlimited && (!Number.isInteger(Number(form.value.quantity)) || Number(form.value.quantity) <= 0)) return 'Số lượng phải là số nguyên lớn hơn 0!'
  if (form.value.form === 2 && !form.value.customerIds?.length) return 'Vui lòng chọn khách hàng nhận phiếu!'
  if (!form.value.name.trim()) {
    return 'Vui lòng nhập tên phiếu!'
  }

  if (
    form.value.discountValue == null ||
    form.value.discountValue <= 0
  ) {
    return 'Giá trị giảm không hợp lệ!'
  }

  if (
    form.value.discountType === 1 &&
    form.value.discountValue > 100
  ) {
    return 'Phần trăm giảm không được vượt quá 100%!'
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
    await updateDiscount(
      route.params.id,
      {
        ...form.value
      }
    )

    showConfirm.value = false

    showLocalToast(
      'success',
      'Cập nhật thành công',
      'Cập nhật phiếu giảm giá thành công!'
    )

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
    <div
      v-if="toast.visible"
      class="ss-toast"
      :class="`ss-toast-${toast.type}`"
    >
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

      <div class="ss-toast-content">
        <strong>{{ toast.title }}</strong>
        <span>{{ toast.message }}</span>
      </div>

      <button
        type="button"
        class="ss-toast-close"
        @click="toast.visible = false"
      >
        ×
      </button>
    </div>

  </AdminLayout>
</template>
