<script setup>
import { showError } from '../../../../utils/feedback'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../../../utils/api'

const route = useRoute()
const router = useRouter()

const campaign = ref(null)
const loading = ref(true)

// ==============================
// FORMAT NGÀY
// ==============================
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

// ==============================
// TRẠNG THÁI THỜI GIAN
// ==============================
function formatTimeStatus(value) {
  if (value === 'SAP_DIEN_RA') {
    return 'Sắp diễn ra'
  }

  if (value === 'DANG_DIEN_RA') {
    return 'Đang diễn ra'
  }

  if (value === 'DA_KET_THUC') {
    return 'Đã kết thúc'
  }

  if (value === 'NGUNG_HOAT_DONG') {
    return 'Ngừng hoạt động'
  }

  return value || '-'
}

// ==============================
// LOAD CHI TIẾT
// ==============================
async function loadDetail() {
  try {
    loading.value = true

    const response =
      await api.get(
        `/dot-giam-gia/${route.params.code}`
      )

    campaign.value =
      response.data?.data

  } catch (error) {
    console.error(error)

    showError(
      error.response?.data?.message ||
      'Không thể tải chi tiết đợt giảm giá!'
    )

    router.push('/dot-giam-gia')

  } finally {
    loading.value = false
  }
  
}

onMounted(loadDetail)
</script>

<template>

  <AdminLayout>

    <main class="ss-page">

      <!-- HEADER -->
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
            Chi tiết đợt giảm giá
          </h2>

          <p class="detail-subtitle">
            Xem thông tin đợt giảm giá
          </p>

        </div>

      </div>

      <!-- LOADING -->
      <div
        v-if="loading"
        class="ss-card ss-empty"
      >
        <i class="bi bi-arrow-repeat"></i>
        Đang tải dữ liệu...
      </div>

      <!-- CHI TIẾT -->
      <section
        v-else-if="campaign"
        class="ss-card ss-form"
      >

        <div class="ss-head">

          <div class="ss-head-icon">
            <i class="bi bi-tag"></i>
          </div>

          <div>

            <h2>
              Thông tin đợt giảm
            </h2>

            <p>
              Thông tin chi tiết của đợt giảm giá.
            </p>

          </div>

        </div>

        <div class="detail-grid">

          <!-- MÃ -->
          <div class="detail-item">
            <span>Mã đợt</span>

            <strong>
              {{ campaign.code }}
            </strong>
          </div>

          <!-- TÊN -->
          <div class="detail-item">
            <span>Tên đợt</span>

            <strong>
              {{ campaign.name }}
            </strong>
          </div>

          <!-- GIÁ TRỊ -->
          <div class="detail-item">
            <span>Giá trị giảm</span>

            <strong>
              {{ campaign.discountValue }}%
            </strong>
          </div>

          <!-- TRẠNG THÁI -->
          <div class="detail-item">
            <span>Trạng thái</span>

            <strong>
              {{ campaign.statusLabel }}
            </strong>
          </div>

          <!-- BẮT ĐẦU -->
          <div class="detail-item">
            <span>Ngày bắt đầu</span>

            <strong>
              {{ formatDate(campaign.startDate) }}
            </strong>
          </div>

          <!-- KẾT THÚC -->
          <div class="detail-item">
            <span>Ngày kết thúc</span>

            <strong>
              {{ formatDate(campaign.endDate) }}
            </strong>
          </div>

          <!-- TÌNH TRẠNG THỜI GIAN -->
          <div class="detail-item">
            <span>Tình trạng thời gian</span>

            <strong>
              {{ formatTimeStatus(campaign.timeStatus) }}
            </strong>
          </div>

          <!-- SỐ BIẾN THỂ -->
          <div class="detail-item">
            <span>Số biến thể áp dụng</span>

            <strong>
              {{
                campaign.productDetailIds?.length || 0
              }}
            </strong>
          </div>

          <!-- MÔ TẢ -->
          <div class="detail-item full">

            <span>Mô tả</span>

            <strong>
              {{
                campaign.description ||
                'Không có mô tả'
              }}
            </strong>

          </div>

        </div>

        <!-- BUTTON -->
        <div class="ss-actions left">

          <button
            class="ss-btn"
            @click="router.back()"
          >
            <i class="bi bi-arrow-left"></i>
            Quay lại
          </button>

          <!-- Bước sau mới làm trang sửa -->
          <button
            class="ss-btn primary"
            @click="
              router.push(
                `/dot-giam-gia/sua/${campaign.code}`
              )
            "
          >
            <i class="bi bi-pencil"></i>
            Sửa đợt
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