<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  getDiscountById
} from '../services/discountService'

const route = useRoute()
const router = useRouter()

const voucher = ref(null)
const loading = ref(true)

function formatMoney(value) {

  if (value == null) {
    return '-'
  }

  return `${Number(
    value
  ).toLocaleString('vi-VN')}đ`
}

function formatDate(value) {

  if (!value) {
    return '-'
  }

  const date =
    String(value).substring(0, 10)

  const [y, m, d] =
    date.split('-')

  return y && m && d
    ? `${d}/${m}/${y}`
    : date
}

async function loadDetail() {

  try {

    const response =
      await getDiscountById(
        route.params.id
      )

    voucher.value =
      response.data.data

  } catch (error) {

    console.error(error)

    alert(
      error.response?.data?.message ||
      'Không thể tải chi tiết phiếu giảm giá!'
    )

    router.push('/giam-gia')

  } finally {

    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<template>

  <AdminLayout>

    <main class="ss-page">

      <div class="detail-top">

        <button
          class="ss-back"
          aria-label="Quay lại"
          @click="router.back()"
        >
          <i class="bi bi-arrow-left"></i>
        </button>

        <div>

          <h2 class="detail-title">
            Chi tiết phiếu giảm giá
          </h2>

          <p class="detail-subtitle">
            Xem thông tin phiếu giảm giá
          </p>

        </div>

      </div>

      <div
        v-if="loading"
        class="ss-card ss-empty"
      >
        <i class="bi bi-arrow-repeat"></i>
        Đang tải dữ liệu...
      </div>

      <section
        v-else-if="voucher"
        class="ss-card ss-form"
      >

        <div class="ss-head">

          <div class="ss-head-icon">
            <i class="bi bi-ticket-perforated"></i>
          </div>

          <div>

            <h2>
              Thông tin phiếu
            </h2>

            <p>
              Thông tin chi tiết của phiếu giảm giá.
            </p>

          </div>

        </div>

        <div class="detail-grid">

          <div class="detail-item">
            <span>Mã phiếu</span>
            <strong>{{ voucher.code }}</strong>
          </div>

          <div class="detail-item">
            <span>Tên phiếu</span>
            <strong>{{ voucher.name }}</strong>
          </div>

          <div class="detail-item">
            <span>Hình thức</span>
            <strong>{{ voucher.formLabel }}</strong>
          </div>

          <div class="detail-item">
            <span>Loại giảm</span>
            <strong>
              {{ voucher.discountTypeLabel }}
            </strong>
          </div>

          <div class="detail-item">
            <span>Giá trị giảm</span>

            <strong>
              {{
                voucher.discountType === 1
                  ? `${voucher.discountValue}%`
                  : formatMoney(voucher.discountValue)
              }}
            </strong>
          </div>

          <div class="detail-item">
            <span>Đơn tối thiểu</span>
            <strong>
              {{ formatMoney(voucher.minOrderValue) }}
            </strong>
          </div>

          <div class="detail-item">
            <span>Giảm tối đa</span>
            <strong>
              {{ formatMoney(voucher.maxDiscount) }}
            </strong>
          </div>

          <div class="detail-item">
            <span>Số lượng</span>
            <strong>
              {{ voucher.quantity }}
            </strong>
          </div>

          <div class="detail-item">
            <span>Đã sử dụng</span>
            <strong>
              {{ voucher.usedQuantity }}
            </strong>
          </div>

          <div class="detail-item">
            <span>Ngày bắt đầu</span>
            <strong>
              {{ formatDate(voucher.startDate) }}
            </strong>
          </div>

          <div class="detail-item">
            <span>Ngày kết thúc</span>
            <strong>
              {{ formatDate(voucher.endDate) }}
            </strong>
          </div>

          <div class="detail-item">
            <span>Trạng thái</span>
            <strong>
              {{ voucher.statusLabel }}
            </strong>
          </div>

          <div class="detail-item full">
            <span>Mô tả</span>

            <strong>
              {{ voucher.description || 'Không có mô tả' }}
            </strong>
          </div>

        </div>

        <div class="ss-actions left">

          <button
            class="ss-btn"
            @click="router.back()"
          >
            <i class="bi bi-arrow-left"></i>
            Quay lại
          </button>

          <button
            class="ss-btn primary"
            @click="router.push(`/giam-gia/sua/${voucher.id}`)"
          >
            <i class="bi bi-pencil"></i>
            Sửa phiếu
          </button>

        </div>

      </section>

    </main>

    
  </AdminLayout>

</template>

<style scoped>

.detail-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.detail-title {
  margin: 0;
  font-size: 18px;
}

.detail-subtitle {
  margin: 3px 0 0;
  color: var(--ss-muted);
  font-size: 13px;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 24px;
}

.detail-item {
  padding: 12px 14px;
  border: 1px solid var(--ss-border, #e8edf2);
  border-radius: 10px;
  background: #fff;
}

.detail-item span {
  display: block;
  color: var(--ss-muted);
  font-size: 12px;
  margin-bottom: 5px;
}

.detail-item strong {
  display: block;
  font-size: 14px;
}

.detail-item.full {
  grid-column: 1 / -1;
}

@media (max-width: 760px) {

  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-item.full {
    grid-column: auto;
  }

}

</style>