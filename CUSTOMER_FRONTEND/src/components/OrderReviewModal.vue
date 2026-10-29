<script setup>
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useAuthStore } from '../stores/auth'
import api from '../services/api'
import { commerceService } from '../services/commerceService'
import ProductImage from './ProductImage.vue'
import ReviewImageViewer from './ReviewImageViewer.vue'
const preview = ref(null)

const props = defineProps({
  item: { type: Object, required: true },
  orderCode: { type: String, default: '' }
})
const emit = defineEmits(['close', 'saved'])
const auth = useAuthStore(), ownerToken = auth.token
const controller = new AbortController()
const eligible = ref(false)
let disposed = false, closeTimer
watch(() => auth.token, value => { if(value !== ownerToken) { controller.abort(); emit('close') } })

const dialog = ref(null)
const fileInput = ref(null)
const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)
const error = ref('')
const formError = ref('')
const success = ref('')
const stars = ref(5)
const comment = ref('')
const images = ref([])
const existingReview = ref(null)
const resolvedProductId = ref(props.item.productId || null)

const labels = ['', 'Rất không hài lòng', 'Không hài lòng', 'Bình thường', 'Hài lòng', 'Rất hài lòng']

let lastFocus, priorOverflow

function keydown(e) {
  if(preview.value)return
  if (e.key === 'Escape' && !saving.value && !uploading.value) emit('close')
  if (e.key !== 'Tab') return
  const controls = [...dialog.value.querySelectorAll('button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), a[href]')]
  const first = controls[0], last = controls.at(-1)
  if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last?.focus() }
  else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first?.focus() }
}

onMounted(async () => {
  lastFocus = document.activeElement
  priorOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  dialog.value?.focus()

  loading.value = true
  error.value = ''
  try {
    if (!resolvedProductId.value && props.item.variantId) {
      const vars = await commerceService.variants([props.item.variantId])
      if(disposed || auth.token !== ownerToken)return
      if (vars && vars.length > 0) {
        resolvedProductId.value = vars[0].productId
      }
    }

    if (resolvedProductId.value) {
      const res = await api.get(`/customer/products/${resolvedProductId.value}/review`, { signal: controller.signal })
      if(disposed || auth.token !== ownerToken)return
      eligible.value = !!res?.eligible
      if (!eligible.value) error.value = 'Bạn chỉ được đánh giá sản phẩm trong đơn đã giao hoặc hoàn thành.'
      if (res?.review) {
        existingReview.value = res.review
        stars.value = res.review.stars || 5
        comment.value = res.review.comment || ''
        images.value = res.review.images ? [...res.review.images] : []
      }
    } else error.value = 'Không tìm thấy sản phẩm để đánh giá.'
  } catch (e) {
    if(!disposed) error.value = e.response?.data?.message || 'Chưa tải được đánh giá. Vui lòng đóng và mở lại.'
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  disposed = true; controller.abort(); clearTimeout(closeTimer)
  document.body.style.overflow = priorOverflow
  lastFocus?.focus()
})

async function handleFileSelect(e) {
  if(uploading.value || saving.value || loading.value || !eligible.value || auth.token !== ownerToken)return
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  if (images.value.length + files.length > 5) {
    formError.value = 'Bạn chỉ được tải lên tối đa 5 hình ảnh.'
    e.target.value = ''
    return
  }

  if(files.some(file => !['image/jpeg','image/png'].includes(file.type) || file.size === 0 || file.size > 5*1024*1024)) {
    formError.value = 'Chỉ chọn ảnh JPG/PNG, mỗi ảnh tối đa 5 MB.'; e.target.value=''; return
  }
  uploading.value = true
  formError.value = ''
  try {
    for (const file of files) {
      if (file.size > 5 * 1024 * 1024) {
        formError.value = `Ảnh "${file.name}" vượt quá dung lượng cho phép 5MB.`
        continue
      }
      const formData = new FormData()
      formData.append('file', file)
      formData.append('productId', resolvedProductId.value)
      const res = await api.post('/customer/reviews/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }, signal: controller.signal
      })
      if(disposed || auth.token !== ownerToken)return
      if (res?.url) {
        images.value.push(res.url)
      }
    }
  } catch (err) {
    formError.value = err.response?.data?.message || 'Không thể tải ảnh lên. Vui lòng thử lại.'
  } finally {
    uploading.value = false
    if (fileInput.value) fileInput.value.value = ''
  }
}

function removeImage(idx) {
  images.value.splice(idx, 1)
}

async function submit() {
  if (saving.value || uploading.value || loading.value || !eligible.value || auth.token !== ownerToken) return
  formError.value = ''
  success.value = ''
  const text = comment.value.trim()
  if (!Number.isInteger(stars.value) || stars.value < 1 || stars.value > 5) {
    formError.value = 'Vui lòng chọn từ 1 đến 5 sao.'
    return
  }
  if (!text || text.length > 2000) {
    formError.value = 'Vui lòng nhập nhận xét từ 1 đến 2.000 ký tự.'
    return
  }
  if (!resolvedProductId.value) {
    formError.value = 'Không tìm thấy mã sản phẩm để đánh giá.'
    return
  }

  saving.value = true
  try {
    const res = await api.put(`/customer/products/${resolvedProductId.value}/review`, {
      stars: stars.value,
      comment: text,
      images: images.value
    }, { signal: controller.signal })
    if(disposed || auth.token !== ownerToken)return
    success.value = existingReview.value
      ? 'Đã cập nhật đánh giá thành công! Cảm ơn bạn.'
      : 'Đã gửi đánh giá thành công! Cảm ơn bạn đã chia sẻ.'
    existingReview.value = res?.review || { stars: stars.value, comment: text, images: images.value }
    emit('saved', { productId: resolvedProductId.value, stars: stars.value, comment: text, images: images.value })
    closeTimer = setTimeout(() => {
      emit('close')
    }, 1200)
  } catch (e) {
    formError.value = e.response?.data?.message || 'Chưa lưu được đánh giá. Vui lòng thử lại.'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <div class="shop-modal-backdrop" @click.self="!saving && !uploading && emit('close')">
      <section
        ref="dialog"
        class="shop-modal review-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="review-modal-title"
        tabindex="-1"
        @keydown="keydown"
      >
        <header>
          <div>
            <span class="eyebrow">ĐÁNH GIÁ SẢN PHẨM</span>
            <h2 id="review-modal-title">
              {{ existingReview ? 'Chỉnh sửa đánh giá của bạn' : 'Đánh giá sau khi mua hàng' }}
            </h2>
            <p v-if="orderCode">Đơn hàng #{{ orderCode }}</p>
          </div>
          <button class="icon-button" type="button" aria-label="Đóng" :disabled="saving || uploading" @click="emit('close')">
            ×
          </button>
        </header>

        <form @submit.prevent="submit">
          <div class="shop-modal-body">
            <div class="product-preview-bar">
              <ProductImage :src="item.image" :alt="item.name" class="modal-product-img" />
              <div>
                <strong>{{ item.name }}</strong>
                <p class="shop-muted">{{ item.color }} · Size {{ item.size }}</p>
              </div>
            </div>

            <p v-if="loading" class="review-notice">Đang tải thông tin đánh giá…</p>

            <p v-if="error" class="shop-error" role="alert">{{ error }}</p>
            <fieldset v-if="!loading && !error && eligible" :disabled="saving || uploading">
              <div class="rating-selection">
                <label class="rating-label">Mức độ hài lòng <span aria-hidden="true">*</span></label>
                <div class="star-picker" role="radiogroup" aria-label="Chọn số sao">
                  <label v-for="n in 5" :key="n" :class="{ chosen: n <= stars }">
                    <input
                      v-model="stars"
                      type="radio"
                      name="order-review-stars"
                      :value="n"
                      :aria-label="`${n} sao`"
                      required
                    />
                    <span aria-hidden="true">★</span>
                  </label>
                  <span class="star-caption">{{ labels[stars] || 'Chọn từ 1 đến 5 sao' }}</span>
                </div>
              </div>

              <div class="comment-section">
                <label for="order-review-comment" class="rating-label">
                  Nhận xét của bạn <span aria-hidden="true">*</span>
                </label>
                <textarea
                  id="order-review-comment"
                  v-model="comment"
                  rows="4"
                  maxlength="2000"
                  required
                  placeholder="Chia sẻ trải nghiệm về chất lượng sản phẩm, độ vừa chân, cảm giác khi mang..."
                ></textarea>
                <small class="char-count">{{ comment.length }} / 2.000 ký tự</small>
              </div>

              <div class="image-upload-section">
                <label class="rating-label">Hình ảnh thực tế (tùy chọn, tối đa 5 ảnh JPG/PNG, 5 MB/ảnh)</label>
                <div class="image-upload-grid">
                  <div v-for="(img, idx) in images" :key="img" class="uploaded-image-card">
                    <button class="preview-upload" type="button" aria-label="Xem ảnh đã chọn" @click="preview=idx"><img :src="img" alt="Ảnh thực tế" /></button>
                    <button
                      type="button"
                      class="remove-img-btn"
                      title="Xóa ảnh này"
                      :disabled="saving || uploading"
                      @click="removeImage(idx)"
                    >
                      ×
                    </button>
                  </div>

                  <label v-if="images.length < 5" class="upload-trigger-card" :class="{ disabled: uploading || saving }">
                    <input
                      ref="fileInput"
                      type="file"
                      accept="image/png,image/jpeg"
                      multiple
                      :disabled="uploading || saving"
                      @change="handleFileSelect"
                    />
                    <span class="upload-camera-icon">📷</span>
                    <span class="upload-text">{{ uploading ? 'Đang tải…' : 'Thêm ảnh' }}</span>
                    <span class="upload-count">{{ images.length }}/5</span>
                  </label>
                </div>
              </div>

              <p v-if="formError" class="shop-error" role="alert">{{ formError }}</p>
              <p v-if="success" class="shop-success" role="status">{{ success }}</p>
            </fieldset>
          </div>

          <footer>
            <button type="button" class="button button-outline" :disabled="saving || uploading" @click="emit('close')">
              Đóng
            </button>
            <button class="button button-primary" :disabled="saving || loading || uploading || !eligible || !!error || !!success" type="submit">
              {{ saving ? 'Đang lưu…' : uploading ? 'Đang tải ảnh…' : existingReview ? 'Cập nhật đánh giá' : 'Gửi đánh giá' }}
            </button>
          </footer>
        </form>
      </section>
    </div>
  </Teleport>
  <ReviewImageViewer v-if="preview !== null" :images="images" :initial="preview" @close="preview=null" />
</template>

<style scoped>
.review-modal {
  max-width: 540px;
}
.product-preview-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  margin-bottom: 20px;
}
.modal-product-img {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 8px;
}
.product-preview-bar strong {
  display: block;
  font-size: 14.5px;
  color: #1e293b;
}
.product-preview-bar p {
  margin: 2px 0 0;
  font-size: 13px;
}
.rating-selection {
  margin-bottom: 18px;
}
.rating-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}
.star-picker {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.star-picker label {
  position: relative;
  color: #cbd5e1;
  cursor: pointer;
  font-size: 36px;
  line-height: 1;
  transition: color 0.15s;
}
.star-picker label.chosen {
  color: #ed9b12;
}
.star-picker input {
  position: absolute;
  opacity: 0;
  width: 100%;
  height: 100%;
  cursor: pointer;
  inset: 0;
  margin: 0;
}
.star-picker label:focus-within {
  outline: 2px solid #1868bd;
  border-radius: 4px;
}
.star-caption {
  font-size: 13.5px;
  color: #64748b;
  margin-left: 12px;
  font-weight: 500;
}
.comment-section {
  margin-bottom: 18px;
}
.comment-section textarea {
  display: block;
  width: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  font: inherit;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  resize: vertical;
  min-height: 100px;
}
.comment-section textarea:focus {
  outline: none;
  border-color: #1868bd;
  box-shadow: 0 0 0 3px rgba(24, 104, 189, 0.15);
}
.char-count {
  display: block;
  color: #64748b;
  margin-top: 4px;
  font-size: 12px;
  text-align: right;
}
.image-upload-section {
  margin-top: 12px;
  margin-bottom: 16px;
}
.image-upload-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}
.uploaded-image-card {
  position: relative;
  width: 72px;
  height: 72px;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  background: #f1f5f9;
}
.uploaded-image-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.preview-upload{border:0;padding:0;background:none;width:100%;height:100%;cursor:zoom-in}
.remove-img-btn {
  position: absolute;
  top: 3px;
  right: 3px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  border: none;
  display: grid;
  place-items: center;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  transition: background 0.15s;
}
.remove-img-btn:hover {
  background: #dc2626;
}
.upload-trigger-card {
  position: relative;
  width: 72px;
  height: 72px;
  border: 2px dashed #cbd5e1;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: #f8fafc;
  transition: all 0.2s;
}
.upload-trigger-card:hover:not(.disabled) {
  border-color: #1868bd;
  background: #f0f7ff;
}
.upload-trigger-card.disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.upload-trigger-card input {
  position: absolute;
  opacity: 0;
  width: 100%;
  height: 100%;
  inset: 0;
  cursor: pointer;
}
.upload-camera-icon {
  font-size: 18px;
  line-height: 1;
}
.upload-text {
  font-size: 11px;
  color: #475569;
  margin-top: 3px;
  font-weight: 500;
}
.upload-count {
  font-size: 10px;
  color: #94a3b8;
}
.review-notice {
  padding: 16px 0;
  color: #64748b;
  font-size: 14px;
  text-align: center;
}
.shop-error {
  color: #dc2626;
  font-size: 13.5px;
  margin-top: 10px;
}
.shop-success {
  color: #16a34a;
  font-size: 13.5px;
  margin-top: 10px;
  font-weight: 500;
}
</style>
