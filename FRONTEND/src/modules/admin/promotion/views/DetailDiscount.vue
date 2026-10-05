<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import api from '../../../../utils/api'

const route = useRoute()
const router = useRouter()

const voucher = ref(null)
const loading = ref(true)

async function loadDetail() {
  try {
    const response = await api.get(
      `/phieu-giam-gia/${route.params.id}`
    )

    voucher.value = response.data.data
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

function formatMoney(value) {
  if (value == null) return '-'

  return Number(value).toLocaleString('vi-VN') + ' ₫'
}

function formatDate(value) {
  if (!value) return '-'

  return value.replace('T', ' ')
}

onMounted(loadDetail)
</script>

<template>
  <AdminLayout>
    <div class="ss-page">

      <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h3 class="fw-bold mb-1">Chi tiết phiếu giảm giá</h3>
          <p class="text-muted mb-0">
            Thông tin chi tiết của phiếu giảm giá
          </p>
        </div>

        <button
          class="btn btn-secondary"
          @click="router.push('/giam-gia')"
        >
          <i class="bi bi-arrow-left me-1"></i>
          Quay lại
        </button>
      </div>

      <div v-if="loading" class="text-center py-5">
        Đang tải dữ liệu...
      </div>

      <div v-else-if="voucher" class="card border-0 shadow-sm">
        <div class="card-body">

          <div class="row g-4">

            <div class="col-md-6">
              <label>Mã phiếu giảm giá</label>
              <div class="detail-value">
                {{ voucher.code }}
              </div>
            </div>

            <div class="col-md-6">
              <label>Tên phiếu giảm giá</label>
              <div class="detail-value">
                {{ voucher.name }}
              </div>
            </div>

            <div class="col-md-6">
              <label>Hình thức phiếu</label>
              <div class="detail-value">
                {{ voucher.formLabel }}
              </div>
            </div>

            <div class="col-md-6">
              <label>Loại giảm giá</label>
              <div class="detail-value">
                {{ voucher.discountTypeLabel }}
              </div>
            </div>

            <div class="col-md-6">
              <label>Giá trị giảm</label>
              <div class="detail-value">
                {{ voucher.discountValue }}
                <span v-if="voucher.discountType === 1">%</span>
                <span v-else>₫</span>
              </div>
            </div>

            <div class="col-md-6">
              <label>Giá trị đơn tối thiểu</label>
              <div class="detail-value">
                {{ formatMoney(voucher.minOrderValue) }}
              </div>
            </div>

            <div class="col-md-6">
              <label>Giảm tối đa</label>
              <div class="detail-value">
                {{ formatMoney(voucher.maxDiscount) }}
              </div>
            </div>

            <div class="col-md-6">
              <label>Số lượng</label>
              <div class="detail-value">
                {{ voucher.quantity }}
              </div>
            </div>

            <div class="col-md-6">
              <label>Đã sử dụng</label>
              <div class="detail-value">
                {{ voucher.usedQuantity }}
              </div>
            </div>

            <div class="col-md-6">
              <label>Ngày bắt đầu</label>
              <div class="detail-value">
                {{ formatDate(voucher.startDate) }}
              </div>
            </div>

            <div class="col-md-6">
              <label>Ngày kết thúc</label>
              <div class="detail-value">
                {{ formatDate(voucher.endDate) }}
              </div>
            </div>

            <div class="col-md-6">
              <label>Trạng thái</label>
              <div class="detail-value">
                {{ voucher.statusLabel }}
              </div>
            </div>

            <div class="col-12">
              <label>Mô tả</label>
              <div class="detail-value">
                {{ voucher.description || 'Không có mô tả' }}
              </div>
            </div>

          </div>

        </div>
      </div>

    </div>
  </AdminLayout>
</template>

<style scoped>
.detail-value {
  margin-top: 6px;
  padding: 10px 12px;
  background: #f8f9fa;
  border-radius: 6px;
  font-weight: 500;
}

label {
  font-weight: 600;
  color: #555;
}
</style>