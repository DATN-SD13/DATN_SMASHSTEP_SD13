<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import api from '../services/api'
import { commerceService } from '../services/commerceService'
import ProductImage from './ProductImage.vue'

const props = defineProps({
  item: { type: Object, required: true },
  orderCode: { type: String, default: '' }
})
const emit = defineEmits(['close', 'saved'])

const dialog = ref(null)
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const formError = ref('')
const success = ref('')
const stars = ref(5)
const comment = ref('')
const existingReview = ref(null)
const resolvedProductId = ref(props.item.productId || null)

const labels = ['', 'Rất không hài lòng', 'Không hài lòng', 'Bình thường', 'Hài lòng', 'Rất hài lòng']

let lastFocus, priorOverflow

function keydown(e) {
  if (e.key === 'Escape' && !saving.value) emit('close')
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
      if (vars && vars.length > 0) {
        resolvedProductId.value = vars[0].productId
      }
    }

    if (resolvedProductId.value) {
      const res = await api.get(`/customer/products/${resolvedProductId.value}/review`)
      if (res?.review) {
        existingReview.value = res.review
        stars.value = res.review.stars || 5
        comment.value = res.review.comment || ''
      }
    }
  } catch (e) {
    // If error loading existing review, keep default empty form
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  document.body.style.overflow = priorOverflow
  lastFocus?.focus()
})

async function submit() {
  if (saving.value) return
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
      comment: text
    })
    success.value = existingReview.value
      ? 'Đã cập nhật đánh giá thành công! Cảm ơn bạn.'
      : 'Đã gửi đánh giá thành công! Cảm ơn bạn đã chia sẻ.'
    existingReview.value = res?.review || { stars: stars.value, comment: text }
    emit('saved', { productId: resolvedProductId.value, stars: stars.value, comment: text })
    setTimeout(() => {
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
    <div class="shop-modal-backdrop" @click.self="!saving && emit('close')">
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
          <button class="icon-button" type="button" aria-label="Đóng" :disabled="saving" @click="emit('close')">
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

            <fieldset v-else :disabled="saving">
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

              <p v-if="formError" class="shop-error" role="alert">{{ formError }}</p>
              <p v-if="success" class="shop-success" role="status">{{ success }}</p>
            </fieldset>
          </div>

          <footer>
            <button type="button" class="button button-outline" :disabled="saving" @click="emit('close')">
              Đóng
            </button>
            <button class="button button-primary" :disabled="saving || loading" type="submit">
              {{ saving ? 'Đang lưu…' : existingReview ? 'Cập nhật đánh giá' : 'Gửi đánh giá' }}
            </button>
          </footer>
        </form>
      </section>
    </div>
  </Teleport>
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
.comment-section textarea {
  display: block;
  width: 100%;
  box-sizing: border-box;
  padding: 12px 14px;
  font: inherit;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  resize: vertical;
  min-height: 110px;
}
.comment-section textarea:focus {
  outline: none;
  border-color: #1868bd;
  box-shadow: 0 0 0 3px rgba(24, 104, 189, 0.15);
}
.char-count {
  display: block;
  color: #64748b;
  margin-top: 6px;
  font-size: 12px;
  text-align: right;
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
