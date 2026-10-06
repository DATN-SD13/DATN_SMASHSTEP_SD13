<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import DiscountForm from '../components/DiscountForm.vue'

import {
  getDiscountById,
  updateDiscount
} from '../services/discountService'

const route = useRoute()
const router = useRouter()

const loading = ref(true)

const form = ref({
  code: '',
  name: '',
  form: 1,
  discountType: 1,
  discountValue: 0,
  minOrderValue: 0,
  maxDiscount: 0,
  quantity: 0,
  startDate: '',
  endDate: ''
})

function toDate(value) {
  return value
    ? String(value).substring(0, 10)
    : ''
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
      startDate: toDate(data.startDate),
      endDate: toDate(data.endDate)
    }

  } catch (error) {

    console.error(error)

    alert(
      error.response?.data?.message ||
      'Không thể tải phiếu giảm giá!'
    )

    router.push('/giam-gia')

  } finally {

    loading.value = false
  }
}

function validate() {

  if (!form.value.name.trim()) {
    return 'Vui lòng nhập tên phiếu!'
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
    !form.value.quantity ||
    form.value.quantity < 1
  ) {
    return 'Số lượng phải lớn hơn 0!'
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

async function save() {

  const message = validate()

  if (message) {
    alert(message)
    return
  }

  const confirmed = window.confirm(
    'Bạn có chắc chắn muốn lưu thay đổi phiếu giảm giá này không?'
  )

  if (!confirmed) return

  try {

    await updateDiscount(
      route.params.id,
      {
        ...form.value
      }
    )

    alert(
      'Cập nhật phiếu giảm giá thành công!'
    )

    router.push('/giam-gia')

  } catch (error) {

    console.error(error)

    alert(
      error.response?.data?.message ||
      'Cập nhật phiếu giảm giá thất bại!'
    )
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
        :edit-mode="true"
        @submit="save"
        @cancel="router.back()"
      />

    </main>

  </AdminLayout>

</template>