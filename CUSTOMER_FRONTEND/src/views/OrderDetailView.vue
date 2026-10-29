<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import OrderQrPayment from '../components/OrderQrPayment.vue'
import { useRoute } from 'vue-router'
import { commerceService, commerceError } from '../services/commerceService'
import { useLiveRefresh } from '../composables/useLiveRefresh'
import { money } from '../utils/money'
import OrderStatusTimeline from '../components/OrderStatusTimeline.vue'
import ShippingInfoModal from '../components/ShippingInfoModal.vue'
import OrderReviewModal from '../components/OrderReviewModal.vue'
import ProductImage from '../components/ProductImage.vue'

const route = useRoute(), order = ref(null), error = ref(''), loading = ref(false), editing = ref(false), canceling = ref(false), confirmCancel = ref(false), notice = ref('')
const router = useRouter(), cart = useCartStore(), owner = cart.owner
const emailStatus = ref(null)
const reviewingItem = ref(null)

let version = 0

async function loadEmail() {
  if (!order.value || cart.owner !== owner) return
  const code = order.value.code, current = version
  try {
    const result = await commerceService.orderEmail(code)
    if (current === version && order.value?.code === code && cart.owner === owner) emailStatus.value = result
  } catch {
    /* Order details stay usable when the mail status cannot be refreshed. */
  }
}

watch(() => cart.owner, next => {
  if (next !== owner) {
    version++
    order.value = null
    editing.value = false
    router.replace('/don-hang')
  }
}, { flush: 'sync' })

onBeforeUnmount(() => { version++ })

async function load() {
  if (editing.value || canceling.value || cart.owner !== owner) return
  const current = ++version
  loading.value = true
  try {
    const next = await commerceService.order(route.params.ma)
    if (current === version && cart.owner === owner) {
      order.value = next
      error.value = ''
      loadEmail()
    }
  } catch (e) {
    if (current === version && cart.owner === owner) error.value = commerceError(e)
  } finally {
    if (current === version && cart.owner === owner) loading.value = false
  }
}

watch(() => route.params.ma, () => {
  order.value = null
  emailStatus.value = null
  editing.value = false
  confirmCancel.value = false
  reviewingItem.value = null
  load()
}, { immediate: true })

async function cancel() {
  if (canceling.value || cart.owner !== owner || !order.value) return
  const current = version, code = order.value.code
  canceling.value = true
  try {
    const next = await commerceService.cancel(code)
    if (current === version && cart.owner === owner) {
      order.value = next
      confirmCancel.value = false
      notice.value = 'Đã hủy đơn và hoàn lại tồn kho, lượt voucher tương ứng.'
    }
  } catch (e) {
    if (current === version && cart.owner === owner) error.value = commerceError(e)
  } finally {
    canceling.value = false
  }
}

function saved(next) {
  order.value = next
  editing.value = false
  notice.value = 'Đã cập nhật thông tin nhận hàng. Vui lòng kiểm tra lại phí vận chuyển và tổng tiền.'
}

function openReview(item) {
  reviewingItem.value = item
}

function onReviewSaved() {
  notice.value = 'Cảm ơn bạn! Đánh giá sản phẩm đã được lưu thành công.'
}

useLiveRefresh(load, 60000)
useLiveRefresh(() => { if (emailStatus.value?.state === 'QUEUED') return loadEmail() }, 15000)
</script>

<template>
  <div class="container shop-page order-detail-page">
    <RouterLink to="/don-hang" class="back-link">← Đơn hàng của bạn</RouterLink>

    <p v-if="error" class="shop-alert" role="alert">
      {{ error }} <button class="text-button" @click="load">Thử lại</button>
    </p>

    <p v-if="loading && !order" class="shop-card" role="status">Đang tải chi tiết đơn hàng…</p>

    <template v-if="order">
      <header class="shop-heading order-detail-heading">
        <div>
          <span class="eyebrow">ORDER DETAILS</span>
          <h1>Đơn hàng #{{ order.code }}</h1>
          <p>Đặt lúc {{ new Date(order.createdAt).toLocaleString('vi-VN') }}</p>
        </div>
        <span class="order-status" :class="{ canceled: order.status >= 6 }">{{ order.statusLabel }}</span>
      </header>

      <p v-if="notice" class="shop-alert shop-alert--info" role="status">{{ notice }}</p>

      <p v-if="emailStatus && emailStatus.state !== 'NOT_REQUESTED'" class="shop-alert shop-alert--info" role="status">
        <strong>{{ emailStatus.state === 'SENT' ? 'Đã gửi email thông báo' : emailStatus.state === 'OPTED_OUT' ? 'Không nhận email cho đơn này' : emailStatus.state === 'UNDELIVERABLE' ? 'Chưa gửi được email' : 'Đang gửi email thông báo' }}</strong><br />
        {{ emailStatus.message }}
      </p>

      <OrderStatusTimeline :status="order.status" :history="order.history" />

      <OrderQrPayment
        v-if="order.paymentMethodCode === 'CHUYEN_KHOAN'"
        :key="order.code + ':' + order.status"
        :code="order.code"
        @paid="load"
      />

      <div v-if="order.editable" class="order-actions">
        <button class="button button-outline" :disabled="canceling" @click="confirmCancel = true">Hủy đơn</button>
        <div v-if="confirmCancel" class="shop-card" role="alert">
          <strong>Bạn muốn hủy đơn hàng này?</strong>
          <p>Chỉ hủy khi chưa chuyển tiền và đơn đang chờ xác nhận. Nếu đã chuyển khoản, hãy giữ đơn và liên hệ cửa hàng đối soát.</p>
          <button class="button button-outline" :disabled="canceling" @click="confirmCancel = false">Giữ đơn</button> 
          <button class="button button-primary" :disabled="canceling" @click="cancel">{{ canceling ? 'Đang hủy…' : 'Xác nhận hủy đơn' }}</button>
        </div>
      </div>

      <section class="shop-card">
        <header class="shop-card-heading">
          <h2>Thông tin nhận hàng</h2>
          <button v-if="order.shippingEditable" class="button button-outline" :disabled="canceling" @click="editing = true">Chỉnh sửa thông tin</button>
        </header>
        <div class="shipping-snapshot">
          <strong>{{ order.receiverName }} · {{ order.phone }}</strong>
          <p>{{ order.address }}</p>
          <a
            v-if="order.deliveryPoint"
            :href="'https://www.google.com/maps/search/?api=1&query=' + order.deliveryPoint.latitude + ',' + order.deliveryPoint.longitude"
            target="_blank"
            rel="noopener noreferrer"
            class="text-button"
          >
            ⌖ Xem vị trí giao hàng trên bản đồ
          </a>
          <p class="shop-muted">{{ order.shippingEditable ? 'Bạn còn 1 lần đổi thông tin nhận hàng khi đơn chờ xác nhận.' : 'Thông tin nhận hàng đang được khóa. Liên hệ cửa hàng nếu cần hỗ trợ.' }}</p>
          <p class="shop-muted">Phương thức thanh toán: {{ order.paymentMethod || 'Chưa có thông tin' }}</p>
          <p v-if="order.note" class="shop-muted">Ghi chú: {{ order.note }}</p>
        </div>
      </section>

      <section class="shop-card">
        <h2>Sản phẩm</h2>
        <article v-for="item in order.items" :key="item.variantId" class="order-product">
          <ProductImage :src="item.image" :alt="item.name" />
          <div class="order-product-info">
            <h3>{{ item.name }}</h3>
            <p class="shop-muted">{{ item.color }} · Size {{ item.size }} × {{ item.quantity }}</p>
            <p>{{ money(item.unitPrice) }} / sản phẩm</p>
            <div v-if="order.status === 4 || order.status === 5" class="order-product-review-action">
              <button
                type="button"
                class="button button-outline review-btn"
                @click="openReview(item)"
              >
                ⭐ Đánh giá sản phẩm
              </button>
            </div>
          </div>
          <strong>{{ money(item.total) }}</strong>
        </article>

        <dl class="order-totals">
          <dt>Tạm tính</dt>
          <dd>{{ money(order.subtotal) }}</dd>
          <dt>Giảm giá</dt>
          <dd>−{{ money(order.discount) }}</dd>
          <dt>Phí vận chuyển</dt>
          <dd>{{ money(order.shipping || 0) }}</dd>
          <dt class="summary-total">Tổng cộng</dt>
          <dd class="summary-total">{{ money(order.total) }}</dd>
        </dl>
      </section>

      <section v-if="order.history.length" class="shop-card">
        <h2>Lịch sử đơn hàng</h2>
        <ol class="order-history">
          <li v-for="(event, i) in order.history" :key="i">
            <time>{{ new Date(event.at).toLocaleString('vi-VN') }}</time>
            <strong>{{ event.label }}</strong>
            <p v-if="event.note">{{ event.note }}</p>
          </li>
        </ol>
      </section>

      <ShippingInfoModal v-if="editing" :order="order" @close="editing = false" @saved="saved" />
      <OrderReviewModal
        v-if="reviewingItem"
        :item="reviewingItem"
        :order-code="order.code"
        @close="reviewingItem = null"
        @saved="onReviewSaved"
      />
    </template>
  </div>
</template>

<style scoped>
.order-product-info {
  flex: 1;
  min-width: 0;
}
.order-product-review-action {
  margin-top: 8px;
}
.review-btn {
  padding: 6px 14px;
  font-size: 13px;
  border-radius: 6px;
  color: #1868bd;
  border-color: #1868bd;
  background: #f0f7ff;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
  transition: all 0.2s;
}
.review-btn:hover {
  background: #1868bd;
  color: #fff;
}
</style>
