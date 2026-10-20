<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { useCartStore } from '../stores/cart'
import { commerceService, commerceError } from '../services/commerceService'
import { money } from '../utils/money'
import { countdownAnchor, remainingSeconds, countdownLabel } from '../utils/qrPayment'
const props = defineProps({ code: { type: String, required: true } })
const emit = defineEmits(['paid'])
const cart = useCartStore(), owner = cart.owner
const payment = ref(null), loading = ref(false), error = ref(''), imageFailed = ref(false), copied = ref('')
const deadline = ref(null), monotonicNow = ref(performance.now())
const seconds = computed(() => remainingSeconds(deadline.value, monotonicNow.value))
const expired = computed(() => payment.value?.status === 'EXPIRED' || (payment.value?.payable && seconds.value === 0))
const active = computed(() => payment.value?.payable && !expired.value)
const clockTimer = setInterval(() => {
  monotonicNow.value = performance.now()
  if (payment.value?.payable && seconds.value === 0 && !expiryChecked && !loading.value) { expiryChecked = true; clearTimeout(timer); load() }
}, 1000)
let timer, version = 0, disposed = false, announced = false, expiryChecked = false
async function load() {
  clearTimeout(timer)
  if (disposed || cart.owner !== owner || loading.value) return
  const current = ++version, started = performance.now(); loading.value = true
  try {
    const next = await commerceService.payment(props.code)
    if (disposed || cart.owner !== owner || current !== version) return
    if (payment.value?.qrUrl !== next.qrUrl) imageFailed.value = false
    monotonicNow.value = performance.now()
    const nextDeadline = countdownAnchor(next, started)
    deadline.value = payment.value?.expiresAt === next.expiresAt && Number.isFinite(deadline.value)
      ? Math.min(deadline.value, nextDeadline ?? started) : nextDeadline
    payment.value = next; error.value = ''
    if (next.status === 'PAID' && !announced) { announced = true; emit('paid') }
  } catch (e) {
    if (!disposed && cart.owner === owner && current === version) { payment.value = null; error.value = commerceError(e) }
  } finally {
    if (!disposed && cart.owner === owner && current === version) {
      loading.value = false
      if (payment.value?.status === 'PENDING') timer = setTimeout(load, seconds.value > 0 ? Math.min(15000, seconds.value * 1000) : 15000)
    }
  }
}
async function copy(value, label) {
  try { await navigator.clipboard.writeText(value); copied.value = 'Đã sao chép ' + label + '.' }
  catch { copied.value = 'Không thể tự sao chép. Bạn có thể chọn và sao chép thông tin bên dưới.' }
}
watch(() => props.code, () => { version++; loading.value = false; payment.value = null; imageFailed.value = false; announced = false; expiryChecked = false; load() }, { immediate: true })
watch(() => cart.owner, next => { if (next !== owner) { version++; clearTimeout(timer); payment.value = null; error.value = ''; loading.value = false } }, { flush: 'sync' })
onBeforeUnmount(() => { disposed = true; version++; clearTimeout(timer); clearInterval(clockTimer) })
</script>
<template>
  <section class="shop-card qr-payment" aria-labelledby="qr-payment-heading">
    <header class="shop-card-heading"><h2 id="qr-payment-heading">Thanh toán chuyển khoản QR</h2><span class="shop-tag" :class="{ 'qr-paid': payment?.status === 'PAID' }">{{ payment?.status === 'PAID' ? 'Đã thanh toán' : expired ? 'Đã hết hạn' : active ? 'Chờ thanh toán' : 'Thông tin thanh toán' }}</span></header>
    <p v-if="loading && !payment" class="shop-muted" role="status">Đang tải thông tin thanh toán…</p>
    <p v-if="error" class="shop-alert" role="alert">{{ error }}</p>
    <p v-if="payment?.message" class="shop-alert shop-alert--info" role="status">{{ payment.message }}</p>
    <p v-if="active" class="qr-countdown" :class="{ urgent: seconds <= 60 }">Mã QR sẽ ẩn sau <strong role="timer" aria-label="Thời gian thanh toán còn lại">{{ countdownLabel(seconds) }}</strong><small>Thời hạn 5 phút tính từ lúc tạo đơn, không đổi khi tải lại trang.</small></p>
    <p v-if="expired && payment?.status !== 'EXPIRED'" class="shop-alert" role="status">Đã hết 5 phút. Mã QR đã được ẩn. Nếu đã chuyển tiền, chờ cửa hàng đối soát và không chuyển lại.</p>
    <div v-if="active" class="qr-payment-grid">
      <div class="qr-payment-image"><img v-if="!imageFailed" :key="payment.qrUrl" :src="payment.qrUrl" referrerpolicy="no-referrer" alt="Mã VietQR chuyển khoản cho đơn hàng" @error="imageFailed = true"><div v-else class="qr-image-error"><strong>Chưa tải được ảnh QR.</strong><p>Bạn vẫn có thể chuyển khoản theo đúng thông tin bên cạnh.</p><button type="button" class="text-button" @click="imageFailed = false">Tải lại ảnh QR</button></div></div>
      <div class="qr-payment-info"><h3>Quét mã bằng ứng dụng ngân hàng</h3><p class="shop-muted">Kiểm tra người nhận, số tiền và nội dung trước khi xác nhận chuyển khoản.</p><dl><dt>Ngân hàng</dt><dd>{{ payment.bankLabel }}</dd><dt>Chủ tài khoản</dt><dd>{{ payment.accountName }}</dd><dt>Số tài khoản</dt><dd><strong>{{ payment.accountNumber }}</strong><button type="button" class="text-button" @click="copy(payment.accountNumber, 'số tài khoản')">Sao chép</button></dd><dt>Số tiền cần chuyển</dt><dd class="qr-amount">{{ money(payment.amount) }}</dd><dt>Nội dung chuyển khoản</dt><dd><strong>{{ payment.memo }}</strong><button type="button" class="text-button" @click="copy(payment.memo, 'nội dung chuyển khoản')">Sao chép</button></dd></dl><p class="qr-warning">Chuyển đúng số tiền và giữ nguyên nội dung để cửa hàng đối soát đơn. Không chuyển nhiều lần. Nếu đã chuyển, hãy chờ cửa hàng xác nhận.</p><p class="shop-muted">Địa chỉ của đơn QR đã được chốt để tổng tiền không thay đổi.</p></div>
    </div>
    <p v-if="payment?.paidAt" class="shop-muted">Xác nhận lúc {{ new Date(payment.paidAt).toLocaleString('vi-VN') }}</p>
    <p v-if="copied" role="status" class="shop-muted">{{ copied }}</p>
    <button type="button" class="button button-outline" :disabled="loading" @click="load">{{ loading ? 'Đang kiểm tra…' : 'Kiểm tra trạng thái thanh toán' }}</button>
    <p v-if="active" class="qr-poll-note">Trang tự cập nhật trạng thái mỗi 15 giây. Nút kiểm tra không xác nhận đã nhận tiền.</p>
    <RouterLink to="/thanh-toan-thu" class="qr-demo-link">Thử thanh toán giả lập</RouterLink>
  </section>
</template>
<style scoped>
.qr-payment-grid{display:grid;grid-template-columns:minmax(210px,300px) minmax(0,1fr);gap:30px;margin:24px 0}.qr-payment-image{align-self:start;border:1px solid #e0e8f1;background:#fff;border-radius:16px;padding:12px}.qr-payment-image img{display:block;width:100%;height:auto}.qr-payment-info h3{font-size:18px;margin-bottom:12px}.qr-payment-info dl{margin:18px 0;display:grid;gap:8px}.qr-payment-info dt{font-size:12px;color:#65758c}.qr-payment-info dd{margin:0 0 10px;display:flex;align-items:center;flex-wrap:wrap;gap:12px;overflow-wrap:anywhere}.qr-payment-info dd strong{user-select:all}.qr-payment-info .text-button{font-size:12px}.qr-amount{font-size:24px;color:#2879e3;font-weight:750}.qr-warning{background:#fff8e8;color:#76500d;padding:14px;border-radius:10px;font-size:12px;line-height:1.8}.qr-paid{background:#e5f6ec;color:#126d3a}.qr-image-error{padding:24px 8px;font-size:13px;line-height:1.8}.qr-poll-note{font-size:11px;color:#65758c;margin-top:12px}@media(max-width:700px){.qr-payment-grid{grid-template-columns:1fr;gap:24px}.qr-payment-image{max-width:300px;width:100%;justify-self:center}}
</style>
<style scoped>
.qr-countdown{display:flex;align-items:center;flex-wrap:wrap;gap:10px;margin:18px 0;padding:14px 16px;border:1px solid #cfe1fb;border-radius:12px;background:#f0f6ff;font-size:13px;color:#164d92}.qr-countdown strong{font-size:25px;font-variant-numeric:tabular-nums;letter-spacing:2px}.qr-countdown small{width:100%;font-size:11px;color:#65758c}.qr-countdown.urgent{background:#fff3e6;border-color:#ffdbab;color:#a84e0a}.qr-demo-link{display:inline-block;margin:16px 0 0 18px;font-size:12px;color:#2879e3;text-decoration:underline}
</style>
