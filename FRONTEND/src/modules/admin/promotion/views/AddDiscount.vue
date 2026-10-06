<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import DiscountForm from '../components/DiscountForm.vue'
import { createDiscount } from '../services/discountService'

const router = useRouter()

const form = ref({
  code: '',
  name: '',
  form: 1,
  discountType: 1,
  discountValue: 0,
  minOrderValue: 0,
  maxDiscount: 0,
  quantity: '',
  startDate: '',
  endDate: ''
})

function validate() {

  if (!form.value.code.trim()) {
    return 'Vui lòng nhập mã phiếu!'
  }

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
    'Bạn có chắc chắn muốn tạo phiếu giảm giá này không?'
  )

  if (!confirmed) return

  try {

    await createDiscount({
      ...form.value
    })

    alert(
      'Tạo phiếu giảm giá thành công!'
    )

    router.push('/giam-gia')

  } catch (error) {

    console.error(error)

    alert(
      error.response?.data?.message ||
      'Tạo phiếu giảm giá thất bại!'
    )
  }
}
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

      <DiscountForm
        :form="form"
        @submit="save"
        @cancel="router.back()"
      />

    </main>

  </AdminLayout>

</template>