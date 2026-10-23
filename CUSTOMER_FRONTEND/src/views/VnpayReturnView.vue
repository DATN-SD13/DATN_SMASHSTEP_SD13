<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'
import { commerceService, commerceError } from '../services/commerceService'
import { money } from '../utils/money'
const auth = useAuthStore(), cart = useCartStore(), cartNotice = ref('')
const route = useRoute(), result = ref(null), loading = ref(false), error = ref(''), automaticLimit = ref(false)
const confirmed = computed(() => result.value?.status === 'PAID_SANDBOX' && result.value?.paymentConfirmed === true)
const paid = computed(() => confirmed.value && !!result.value?.orderCode)
const pending = computed(() => ['AWAITING_GATEWAY', 'EXPIRED'].includes(result.value?.status) || (confirmed.value && !paid.value))
let timer, generation = 0, disposed = false, startedAt = performance.now()
const AUTOMATIC_WAIT_MS = 90000
async function updateCart(orderCode) {
  if (!auth.isAuthenticated || !auth.isCustomer || !orderCode) return
  const owner = cart.owner
  try {
    // Read the protected order API before changing a cart: signed return URLs alone do not identify its owner.
    const order = await commerceService.order(orderCode)
    if (disposed || cart.owner !== owner) return
    const key = 'ss_paid_order_cart:' + owner + ':' + orderCode
    const removePurchased = () => {
      if (cart.owner !== owner || localStorage.getItem(key) === 'done') return
      localStorage.setItem(key, 'done')
      cart.purchased(order.items.map(item => ({ variantId: item.variantId, quantity: item.quantity })), owner)
    }
    if (navigator.locks?.request) await navigator.locks.request(key, removePurchased)
    else removePurchased()
  } catch { if (!disposed && cart.owner === owner) cartNotice.value = 'Đơn hàng đã được lưu. Chưa cập nhật được giỏ hàng; vui lòng kiểm tra giỏ của bạn.' }
}
async function load(manual = false) {
  if (manual === true) { startedAt = performance.now(); automaticLimit.value = false }
  clearTimeout(timer)
  if (loading.value || disposed) return
  const current = generation
  loading.value = true; error.value = ''
  try {
    let next
    if (route.params.code) {
      next = await commerceService.vnpayReconcile(route.params.code)
    } else {
      if (!route.query.vnp_TxnRef) { error.value = 'Chưa có kết quả thanh toán. Vui lòng thanh toán từ giỏ hàng.'; return }
      const params = Object.fromEntries(Object.entries(route.query).filter(([key]) => key.startsWith('vnp_')))
      const proof = Object.fromEntries(Object.entries(params).map(([key, value]) => [key, Array.isArray(value) ? value : [value]]))
      next = await commerceService.vnpayReconcileReturn(proof)
    }
    if (disposed || current !== generation) return
    result.value = next
    if (next.valid && (['AWAITING_GATEWAY', 'EXPIRED'].includes(next.status) || (next.paymentConfirmed && !next.orderCode))) {
      if (performance.now() - startedAt < AUTOMATIC_WAIT_MS) timer = setTimeout(load, 10000)
      else automaticLimit.value = true
    }
    if (next.paymentConfirmed && next.orderCode) await updateCart(next.orderCode)
  } catch (e) { if (!disposed && current === generation) { error.value = commerceError(e); automaticLimit.value = true } }
  finally { if (!disposed && current === generation) loading.value = false }
}
watch([() => route.fullPath, () => cart.owner], () => { generation++; startedAt = performance.now(); automaticLimit.value = false; loading.value = false; result.value = null; cartNotice.value = ''; load() }, { immediate: true })
onBeforeUnmount(() => { disposed = true; generation++; clearTimeout(timer) })
</script>
<template>
  <div class="container shop-page vnpay-result-page">
    <RouterLink class="back-link" to="/gio-hang">← Quay lại giỏ hàng</RouterLink>
    <header class="shop-heading"><span class="eyebrow">VNPAY · SANDBOX</span><h1>Kết quả thanh toán</h1></header>
    <section class="shop-card vnpay-result" :class="{ paid }">
      <p v-if="loading && !result" role="status">Đang xác minh kết quả từ máy chủ…</p>
      <p v-if="error" class="shop-alert" role="alert">{{ error }}</p>
      <template v-if="result">
        <span class="result-icon" aria-hidden="true">{{ paid ? '✓' : pending && !automaticLimit ? '…' : '!' }}</span>
        <h2>{{ paid ? 'Đặt hàng thành công' : confirmed ? 'Đã thanh toán, đang ghi nhận đơn hàng' : !result.valid ? 'Kết quả chưa được xác minh' : pending ? automaticLimit ? 'Chưa xác nhận được thanh toán' : 'Đang xác nhận thanh toán' : 'Thanh toán chưa hoàn tất' }}</h2>
        <p v-if="!paid && result.orderMessage" class="shop-alert" role="alert">{{ result.orderMessage }}</p>
        <p v-else-if="!paid && !automaticLimit" role="status">{{ result.message }}</p>
        <p v-else-if="!paid && !confirmed" role="status">Chưa nhận được xác nhận từ VNPAY. Bạn có thể kiểm tra lại kết quả của giao dịch này.</p>
        <strong v-if="result.amount != null" class="result-amount">{{ money(result.amount) }}</strong>
        <p v-if="result.code" class="result-code">Mã đơn hàng: {{ result.orderCode || result.code }}</p>
        <p v-if="paid">Đơn hàng đã được lưu và đang chờ cửa hàng xác nhận.</p>
        <p v-else-if="pending && !automaticLimit && !confirmed" class="pending-note">Đang kiểm tra với VNPAY. Trang sẽ tự cập nhật kết quả.</p>
        <p v-if="result.paidAt">Xác nhận thanh toán lúc {{ new Date(result.paidAt).toLocaleString('vi-VN') }}</p>
      </template>
      <p v-if="cartNotice" class="shop-alert" role="status">{{ cartNotice }}</p>
      <p class="sandbox-note">Thanh toán qua VNPAY Sandbox, không thu tiền thật.</p>
      <div class="result-actions"><button v-if="!paid" class="button button-outline" type="button" :disabled="loading" @click="load(true)">{{ loading ? 'Đang kiểm tra…' : 'Kiểm tra lại kết quả' }}</button><RouterLink v-if="paid" class="button button-primary" :to="'/don-hang/' + result.orderCode">Xem đơn hàng</RouterLink><RouterLink v-else class="button button-primary" to="/gio-hang">Về giỏ hàng</RouterLink></div>
    </section>
  </div>
</template>
<style scoped>
.vnpay-result-page{max-width:850px}.vnpay-result{text-align:center;padding:35px}.result-icon{display:grid;place-items:center;width:70px;height:70px;margin:0 auto 22px;border-radius:50%;background:#fff4dc;color:#a9791f;font-size:36px}.paid .result-icon{background:#e6f7ed;color:#258157}.vnpay-result h2{font-size:23px;line-height:1.5}.vnpay-result p{font-size:13px;line-height:1.9;color:var(--customer-muted);margin:18px 0}.result-amount{display:block;font-size:29px;color:var(--customer-primary);margin:22px 0}.result-code{overflow-wrap:anywhere;font-size:11px!important}.pending-note{padding:16px;background:#fff9eb;border-radius:10px}.result-actions{display:flex;justify-content:center;gap:12px;flex-wrap:wrap;margin-top:25px}.result-actions .button{font-size:12px}@media(max-width:600px){.vnpay-result{padding:24px 18px}.vnpay-result h2{font-size:19px}.result-actions .button{width:100%}}
.sandbox-note{font-size:11px!important;margin-bottom:0!important}</style>
