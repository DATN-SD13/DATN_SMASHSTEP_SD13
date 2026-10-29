<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import api from '../services/api'
import RatingStars from './RatingStars.vue'
import ReviewImageViewer from './ReviewImageViewer.vue'

const props = defineProps({ productId: { type: [Number, String], required: true } })
const emit = defineEmits(['updated'])

const result = ref(null), page = ref(0), filter = ref(null)
const loading = ref(false), error = ref('')
const previewImage = ref(null)
let controller, version = 0

const summary = computed(() => result.value?.summary || { average: 0, count: 0 })

async function load() {
  controller?.abort()
  controller = new AbortController()
  const signal = controller.signal, current = ++version
  loading.value = true
  error.value = ''
  try {
    const list = await api.get(`/store/products/${props.productId}/reviews`, {
      params: { page: page.value, stars: filter.value },
      signal
    })
    if (current !== version) return
    result.value = list
    emit('updated', list.summary)
  } catch (e) {
    if (current === version && !signal.aborted) {
      error.value = e.response?.data?.message || 'Chưa tải được đánh giá. Vui lòng thử lại.'
    }
  } finally {
    if (current === version) loading.value = false
  }
}

watch(() => props.productId, () => {
  page.value = 0
  filter.value = null
  result.value = null
  previewImage.value = null
  load()
}, { immediate: true })

function selectFilter(value) {
  if (loading.value) return
  filter.value = value
  page.value = 0
  load()
}

function changePage(value) {
  page.value = value
  load()
}

function date(value) {
  return new Date(value).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onBeforeUnmount(() => {
  version++
  controller?.abort()
})
</script>

<template>
  <section id="product-reviews" class="reviews-panel" aria-labelledby="review-heading">
    <div class="review-heading">
      <div>
        <span class="eyebrow">TRẢI NGHIỆM TỪ KHÁCH HÀNG</span>
        <h2 id="review-heading">Đánh giá sản phẩm</h2>
      </div>
      <span>{{ summary.count }} đánh giá</span>
    </div>

    <p v-if="error" class="shop-error" role="alert">
      {{ error }} <button class="review-link" @click="load">Thử lại</button>
    </p>

    <div class="review-overview" :aria-busy="loading">
      <div class="review-score">
        <strong>{{ summary.count ? summary.average.toFixed(1) : '—' }}<small> / 5</small></strong>
        <RatingStars :value="summary.average" />
        <span>{{ summary.count ? 'Điểm trung bình từ khách hàng' : 'Chưa có đánh giá' }}</span>
      </div>
      <div class="review-filters" aria-label="Lọc đánh giá">
        <button :class="{ active: filter === null }" :disabled="loading" @click="selectFilter(null)">
          Tất cả ({{ summary.count }})
        </button>
        <button
          v-for="n in [5, 4, 3, 2, 1]"
          :key="n"
          :class="{ active: filter === n }"
          :disabled="loading"
          @click="selectFilter(n)"
        >
          {{ n }} sao ({{ result?.distribution?.[n] || 0 }})
        </button>
      </div>
    </div>

    <div class="review-policy-notice">
      <span>💬 <em>Chỉ khách hàng đã mua và nhận sản phẩm thành công mới có thể gửi đánh giá trong mục <strong>Đơn hàng của bạn</strong>.</em></span>
    </div>

    <p v-if="loading" role="status" class="review-notice">Đang tải đánh giá…</p>

    <template v-else-if="result">
      <article v-for="item in result.items" :key="item.id" class="review-item">
        <div class="review-avatar" aria-hidden="true">{{ item.customerName[0] }}</div>
        <div class="review-content">
          <strong>{{ item.customerName }}</strong>
          <div>
            <RatingStars :value="item.stars" />
            <span class="review-verified">✓ Đã mua hàng</span>
          </div>
          <time :datetime="item.createdAt">
            {{ date(item.createdAt) }}
            <span v-if="item.createdAt !== item.updatedAt"> · Đã chỉnh sửa</span>
          </time>
          <p>{{ item.comment }}</p>

          <div v-if="item.images && item.images.length" class="review-images-row">
            <button
              v-for="(img, idx) in item.images"
              :key="idx"
              type="button"
              class="review-thumb-btn"
              title="Nhấn để xem ảnh phóng to"
              @click="previewImage = {images:item.images,initial:idx}"
            >
              <img :src="img" alt="Ảnh thực tế từ khách hàng" loading="lazy" />
            </button>
          </div>
        </div>
      </article>

      <p v-if="!result.items.length" class="review-notice">
        {{ filter ? 'Chưa có đánh giá ' + filter + ' sao.' : 'Chưa có đánh giá nào cho sản phẩm này.' }}
      </p>

      <nav v-if="result.total > 10" class="review-pagination" aria-label="Trang đánh giá">
        <button class="button button-outline" :disabled="page === 0" @click="changePage(page - 1)">
          ← Trước
        </button>
        <span>Trang {{ page + 1 }} / {{ Math.ceil(result.total / 10) }}</span>
        <button class="button button-outline" :disabled="(page + 1) * 10 >= result.total" @click="changePage(page + 1)">
          Sau →
        </button>
      </nav>
    </template>

    <ReviewImageViewer v-if="previewImage" v-bind="previewImage" @close="previewImage=null" />
  </section>
</template>

<style scoped>
.reviews-panel {
  background: #fff;
  border: 1px solid #e0e7ef;
  border-radius: 20px;
  margin: 32px 0 56px;
  padding: 32px;
  scroll-margin-top: 100px;
}
.review-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.review-heading h2 {
  margin: 8px 0 24px;
}
.review-heading > span,
.review-score > span {
  color: #64748b;
  font-size: 13px;
}
.review-overview {
  display: flex;
  gap: 32px;
  align-items: center;
  padding: 24px;
  background: #f5f9ff;
  border: 1px solid #dce9f8;
  border-radius: 12px;
}
.review-score {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 190px;
}
.review-score strong {
  font-size: 36px;
  color: #1868bd;
}
.review-score small {
  font-size: 18px;
  font-weight: 400;
}
.review-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.review-filters button {
  background: #fff;
  padding: 10px 16px;
  border: 1px solid #dce3ed;
  border-radius: 7px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}
.review-filters button:hover:not(:disabled) {
  border-color: #1868bd;
}
.review-filters button.active {
  color: #1868bd;
  border-color: #1868bd;
  background: #eaf3ff;
}
.review-policy-notice {
  margin-top: 18px;
  padding: 12px 16px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  color: #64748b;
  font-size: 13.5px;
}
.review-policy-notice strong {
  color: #1e293b;
}
.review-notice {
  padding: 20px 0;
  color: #64748b;
  font-size: 14px;
}
.review-link {
  color: #1868bd;
  text-decoration: underline;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  font: inherit;
}
.review-item {
  display: flex;
  gap: 14px;
  padding: 24px 0;
  border-bottom: 1px solid #e8edf3;
}
.review-avatar {
  display: grid;
  place-items: center;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  background: #eaf3ff;
  color: #1868bd;
  font-weight: 700;
}
.review-content {
  min-width: 0;
  flex: 1;
}
.review-content > strong {
  display: block;
  margin-bottom: 6px;
}
.review-verified {
  font-size: 12px;
  color: #14865a;
  margin-left: 12px;
}
.review-content time {
  display: block;
  font-size: 12px;
  color: #8793a4;
  margin-top: 7px;
}
.review-content p {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  line-height: 1.8;
  margin-bottom: 8px;
}
.review-images-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}
.review-thumb-btn {
  padding: 0;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  width: 72px;
  height: 72px;
  cursor: pointer;
  background: #f8fafc;
  transition: transform 0.15s, border-color 0.15s;
}
.review-thumb-btn:hover {
  transform: scale(1.05);
  border-color: #1868bd;
}
.review-thumb-btn img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.review-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 24px;
}
.review-pagination span {
  font-size: 13px;
}
.lightbox-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.75);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.lightbox-modal {
  position: relative;
  max-width: 90vw;
  max-height: 85vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.lightbox-modal img {
  max-width: 100%;
  max-height: 85vh;
  border-radius: 8px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.5);
  object-fit: contain;
}
.lightbox-close {
  position: absolute;
  top: -16px;
  right: -16px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff;
  color: #1e293b;
  border: none;
  font-size: 24px;
  line-height: 1;
  display: grid;
  place-items: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
}
@media (max-width: 640px) {
  .reviews-panel {
    padding: 18px;
  }
  .review-overview {
    flex-direction: column;
    align-items: flex-start;
    padding: 18px;
    gap: 18px;
  }
  .review-heading {
    align-items: flex-start;
  }
  .review-verified {
    display: block;
    margin: 4px 0;
  }
}
</style>
