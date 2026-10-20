<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { commerceService, commerceError } from '../services/commerceService'
import { money } from '../utils/money'
import { countdownAnchor, remainingSeconds, countdownLabel } from '../utils/qrPayment'
import { DEMO_CARDS, DEMO_HOLDER, DEMO_ISSUE_DATE, DEMO_OTP, demoCardError, demoCardResult } from '../utils/demoCard'

const route = useRoute(), cart = useCartStore()
const session = ref(null), card = ref({ number: '', holder: '', issueDate: '' }), otp = ref('')
const loading = ref(false), busy = ref(false), error = ref(''), fields = ref({}), storageNotice = ref('')
const deadline = ref(null), monotonicNow = ref(performance.now())
const connected = computed(() => !!route.params.code)
const seconds = computed(() => remainingSeconds(deadline.value, monotonicNow.value))
const waiting = computed(() => ['AWAITING_CARD', 'AWAITING_OTP'].includes(session.value?.status))
const status = computed(() => waiting.value && seconds.value <= 0 ? 'EXPIRED' : session.value?.status)
const active = computed(() => waiting.value && seconds.value > 0)
const title = computed(() => ({ AWAITING_CARD: 'Thông tin thẻ thử', AWAITING_OTP: 'Xác thực giao dịch thử', PAID_DEMO: 'Thanh toán thử thành công', FAILED: 'Thanh toán thử thất bại', CANCELED: 'Đã hủy thanh toán thử', EXPIRED: 'Phiên thanh toán đã hết hạn' }[status.value] || 'Thanh toán thẻ giả lập'))
let generation = 0, disposed = false
function localKey() { return 'ss_card_demo_v1:' + cart.owner }
function apply(next, started = performance.now()) {
  const nextDeadline = countdownAnchor(next, started)
  deadline.value = session.value?.code === next.code && Number.isFinite(deadline.value)
    ? Math.min(deadline.value, nextDeadline ?? started) : nextDeadline
  session.value = next; monotonicNow.value = performance.now()
}
function persist() {
  if (connected.value) return
  try { sessionStorage.setItem(localKey(), JSON.stringify(session.value)); storageNotice.value = '' }
  catch { storageNotice.value = 'Không lưu được phiên thử. Hãy giữ tab này mở.' }
}
function restart() {
  if (connected.value || busy.value) return
  const now = new Date(), expiry = new Date(now.getTime() + 300000)
  session.value = null; deadline.value = null
  apply({ code: 'DEMO-' + crypto.randomUUID(), status: 'AWAITING_CARD', amount: '999000',
    createdAt: now.toISOString(), expiresAt: expiry.toISOString(), serverTime: now.toISOString(), failures: 0 })
  card.value = { number: '', holder: '', issueDate: '' }; otp.value = ''; fields.value = {}; error.value = ''; persist()
}
async function load() {
  const current = ++generation, started = performance.now(); loading.value = true
  session.value = null; deadline.value = null; error.value = ''; fields.value = {}; otp.value = ''; card.value = { number: '', holder: '', issueDate: '' }
  try {
    if (connected.value) {
      const next = await commerceService.demoPayment(route.params.code)
      if (current === generation && !disposed) apply(next, started)
    } else {
      let saved
      try { saved = JSON.parse(sessionStorage.getItem(localKey()) || 'null') } catch { /* Start a new local session. */ }
      if (saved?.code?.startsWith('DEMO-') && ['AWAITING_CARD', 'AWAITING_OTP', 'PAID_DEMO', 'FAILED', 'CANCELED', 'EXPIRED'].includes(saved.status)
          && Date.parse(saved.expiresAt) - Date.parse(saved.createdAt) === 300000 && Date.parse(saved.createdAt) <= Date.now()) {
        apply({ ...saved, serverTime: new Date().toISOString() }); expireLocal()
      } else restart()
    }
  } catch (e) { if (current === generation && !disposed) error.value = commerceError(e) }
  finally { if (current === generation && !disposed) loading.value = false }
}
function fillSample(sample = DEMO_CARDS[0]) {
  if (!active.value || status.value !== 'AWAITING_CARD' || busy.value) return
  card.value = { number: sample.number, holder: DEMO_HOLDER, issueDate: DEMO_ISSUE_DATE }; fields.value = {}; error.value = ''
}
function issueInput(event) {
  const digits = event.target.value.replace(/\D/g, '').slice(0, 4)
  card.value.issueDate = digits.length > 2 ? digits.slice(0, 2) + '/' + digits.slice(2) : digits
}
function expireLocal() {
  if (!connected.value && waiting.value && seconds.value <= 0) {
    session.value = { ...session.value, status: 'EXPIRED', challenge: null, message: 'Phiên thử đã hết hạn 5 phút.' }; otp.value = ''; persist()
  }
}
async function send(action) {
  if (busy.value || !active.value) return
  fields.value = {}; error.value = ''
  if (action === 'card') {
    const invalid = demoCardError(card.value)
    if (invalid) { fields.value = { card: invalid }; return }
  }
  if (action === 'otp' && !/^[0-9]{6}$/.test(otp.value)) { fields.value = { otp: 'Nhập OTP thử gồm đúng 6 chữ số.' }; return }
  const current = generation, started = performance.now(), code = session.value.code
  busy.value = true
  try {
    if (connected.value) {
      const next = action === 'card' ? await commerceService.demoCard(code, { ...card.value })
        : action === 'otp' ? await commerceService.demoOtp(code, { otp: otp.value, challenge: session.value.challenge })
        : await commerceService.demoCancel(code)
      if (current !== generation || disposed) return
      apply(next, started)
    } else {
      monotonicNow.value = performance.now(); expireLocal()
      if (!active.value) return
      const next = { ...session.value }
      if (action === 'cancel') { next.status = 'CANCELED'; next.challenge = null; next.message = 'Đã hủy giao dịch thử.' }
      else if (action === 'card' && next.status === 'AWAITING_CARD') {
        const result = demoCardResult(card.value)
        next.status = result === 'OK' ? 'AWAITING_OTP' : 'FAILED'
        next.message = result === 'OK' ? 'Nhập OTP thử 123456 để hoàn tất.' : result
        next.challenge = result === 'OK' ? crypto.randomUUID() : null
      } else if (action === 'otp' && next.status === 'AWAITING_OTP') {
        if (otp.value === DEMO_OTP) { next.status = 'PAID_DEMO'; next.paidAt = new Date().toISOString(); next.reference = 'SIM-' + crypto.randomUUID(); next.challenge = null; next.message = 'Không có tiền thật được thu.' }
        else { next.failures = (next.failures || 0) + 1; next.message = 'OTP thử không đúng. Dùng 123456. Còn ' + Math.max(0, 5 - next.failures) + ' lần thử.'; if (next.failures >= 5) { next.status = 'FAILED'; next.challenge = null } }
      }
      session.value = next; persist()
    }
    if (action === 'card' && status.value !== 'AWAITING_CARD') card.value = { number: '', holder: '', issueDate: '' }
    if (action === 'otp') otp.value = ''
  } catch (e) { if (current === generation && !disposed) error.value = commerceError(e) }
  finally { if (current === generation && !disposed) busy.value = false }
}
watch([() => route.params.code, () => cart.owner], () => { busy.value = false; load() }, { immediate: true })
const timer = setInterval(() => { monotonicNow.value = performance.now(); expireLocal() }, 500)
onBeforeUnmount(() => { disposed = true; generation++; clearInterval(timer); card.value = {}; otp.value = '' })
</script>
<template>
  <div class="container shop-page card-demo-page">
    <RouterLink to="/gio-hang" class="back-link">← Quay lại giỏ hàng</RouterLink>
    <header class="payment-heading"><div><span class="eyebrow">SMASHSTEP · CHẾ ĐỘ GIẢ LẬP</span><h1>Thanh toán thẻ nội địa</h1><p>Trải nghiệm luồng nhập thẻ và OTP theo mẫu VNPAY.</p></div><span class="demo-seal">GIẢ LẬP<br><small>KHÔNG THU TIỀN</small></span></header>
    <div class="demo-banner" role="note"><strong>Chỉ dùng thông tin thẻ thử bên dưới.</strong><p>Đây là mô phỏng tại SmashStep, không kết nối cổng VNPAY hay ngân hàng thật. Không nhập số thẻ hoặc mật khẩu ngân hàng của bạn.</p><p>{{ connected ? 'Đơn thử thành công hiển thị trong mục riêng ở quản lý, không trừ kho, dùng voucher hoặc cộng doanh thu thật. Giỏ hàng vẫn được giữ nguyên.' : 'Trang trải nghiệm độc lập: kết quả chỉ lưu trong tab này. Muốn thấy đơn thử ở quản lý, chọn sản phẩm trong giỏ và chọn thanh toán thẻ GIẢ LẬP ở bước thanh toán.' }}</p></div>
    <p v-if="loading" class="shop-card" role="status">Đang tải phiên thanh toán…</p>
    <p v-if="error" class="shop-alert" role="alert">{{ error }} <button v-if="!session" type="button" class="text-button" @click="load">Tải lại phiên</button></p>
    <p v-if="storageNotice" class="shop-alert" role="status">{{ storageNotice }}</p>
    <div v-if="session && !loading" class="payment-columns">
      <section class="shop-card payment-main" :class="{ success: status === 'PAID_DEMO' }">
        <div class="bank-heading"><div class="bank-mark">NCB</div><div><strong>Thẻ ATM nội địa</strong><small>Ngân hàng NCB · dữ liệu thử công khai</small></div></div>
        <ol class="payment-steps" aria-label="Các bước thanh toán"><li :class="{ current: status === 'AWAITING_CARD' }">1. Thông tin thẻ</li><li :class="{ current: status === 'AWAITING_OTP' }">2. Xác thực OTP</li><li :class="{ current: status === 'PAID_DEMO' }">3. Kết quả</li></ol>
        <h2 id="payment-state" role="status">{{ title }}</h2>
        <div v-if="active" class="payment-countdown" :class="{ urgent: seconds <= 60 }"><span>Phiên có hiệu lực trong</span><strong role="timer">{{ countdownLabel(seconds) }}</strong></div>
        <p v-if="session.message" class="payment-message" role="status">{{ session.message }}</p>
        <form v-if="active && status === 'AWAITING_CARD'" autocomplete="off" novalidate @submit.prevent="send('card')">
          <label class="payment-label" for="demo-card-number">Số thẻ thử <span>*</span></label><input id="demo-card-number" v-model="card.number" class="payment-input card-number" inputmode="numeric" maxlength="24" autocomplete="off" placeholder="Nhập số thẻ NCB thử" :disabled="busy" :aria-invalid="!!fields.card" aria-describedby="demo-card-help">
          <div class="card-field-grid"><div><label class="payment-label" for="demo-card-holder">Tên chủ thẻ thử <span>*</span></label><input id="demo-card-holder" v-model="card.holder" class="payment-input holder-input" maxlength="50" autocomplete="off" placeholder="NGUYEN VAN A" :disabled="busy"></div><div><label class="payment-label" for="demo-card-issued">Ngày phát hành <span>*</span></label><input id="demo-card-issued" :value="card.issueDate" class="payment-input" inputmode="numeric" maxlength="5" autocomplete="off" placeholder="MM/YY" :disabled="busy" @input="issueInput"></div></div>
          <p id="demo-card-help" class="payment-help">Dùng ngày phát hành 07/15, không phải ngày hết hạn. Không cần CVV hay mật khẩu ngân hàng thật.</p>
          <p v-if="fields.card" class="shop-error" role="alert">{{ fields.card }}</p>
          <button class="button button-primary payment-submit" type="submit" :disabled="busy">{{ busy ? 'Đang kiểm tra thẻ…' : 'Tiếp tục xác thực →' }}</button>
        </form>
        <form v-else-if="active && status === 'AWAITING_OTP'" autocomplete="off" novalidate @submit.prevent="send('otp')">
          <p class="otp-hint">Mã OTP thử là <strong>123456</strong>. Mã này hiển thị sẵn để bạn thử, không gửi SMS.</p>
          <label class="payment-label" for="demo-otp">Mật khẩu OTP thử <span>*</span></label><input id="demo-otp" v-model="otp" class="payment-input otp-input" inputmode="numeric" maxlength="6" autocomplete="off" placeholder="6 chữ số" :disabled="busy" :aria-invalid="!!fields.otp">
          <p v-if="fields.otp" class="shop-error" role="alert">{{ fields.otp }}</p>
          <button class="button button-primary payment-submit" type="submit" :disabled="busy">{{ busy ? 'Đang xác thực…' : 'Xác nhận thanh toán thử' }}</button>
        </form>
        <div v-else class="payment-result"><span class="result-icon" aria-hidden="true">{{ status === 'PAID_DEMO' ? '✓' : status === 'EXPIRED' ? '⌛' : '×' }}</span><h3>{{ status === 'PAID_DEMO' ? 'Giao dịch giả lập hoàn tất' : 'Giao dịch chưa hoàn tất' }}</h3><p v-if="status === 'PAID_DEMO'">{{ money(session.amount) }} · Không thu tiền thật.<br>{{ connected ? 'Đơn thử đã xuất hiện ở mục Đơn thử đã thanh toán trong quản lý hóa đơn.' : 'Đây là phiên trải nghiệm độc lập, không tạo đơn trong quản lý.' }}</p><p v-else-if="status === 'EXPIRED'">Đã hết 5 phút. Thông tin thẻ và ô OTP đã được ẩn. Phiên cũ không thể tiếp tục thanh toán.</p><p v-else>Không thu tiền và không tạo đơn đã thanh toán trong quản lý.</p><p v-if="session.reference" class="result-reference">Mã giao dịch thử: {{ session.reference }}</p><button v-if="!connected" class="button button-primary" type="button" @click="restart">Tạo phiên thử mới</button><RouterLink v-else class="button button-primary" to="/gio-hang">Quay lại giỏ hàng</RouterLink><button v-if="connected" type="button" class="text-button check-result" :disabled="busy" @click="load">Kiểm tra lại kết quả từ máy chủ</button></div>
        <button v-if="active" type="button" class="cancel-payment text-button" :disabled="busy" @click="send('cancel')">Hủy giao dịch thử</button>
      </section>
      <aside class="payment-aside">
        <section class="shop-card payment-summary"><span class="eyebrow">CHI TIẾT GIAO DỊCH THỬ</span><h2>Đơn hàng SmashStep</h2><dl><dt>Mã phiên thử</dt><dd class="reference">{{ session.code }}</dd><dt>Số tiền thanh toán thử</dt><dd class="payment-total">{{ money(session.amount) }}</dd><template v-if="session.lines"><dt>Sản phẩm đã chọn</dt><dd><ul><li v-for="line in session.lines" :key="line.variant.variantId">{{ line.variant.name }} · {{ line.variant.color }} / {{ line.variant.size }} × {{ line.quantity }}</li></ul></dd></template><dt>Thời hạn</dt><dd>5 phút từ khi tạo phiên</dd><template v-if="session.paidAt"><dt>Xác thực thành công lúc</dt><dd>{{ new Date(session.paidAt).toLocaleString('vi-VN') }}</dd></template></dl><p class="payment-help" v-if="connected">Dữ liệu thử giữ tối đa 24 giờ trong bộ nhớ máy chủ, sẽ mất khi backend khởi động lại.</p></section>
        <section class="shop-card test-card-guide"><h2>Thông tin thẻ mẫu</h2><p>NCB · {{ DEMO_HOLDER }} · Ngày phát hành {{ DEMO_ISSUE_DATE }}</p><button type="button" class="sample-success" :disabled="!active || status !== 'AWAITING_CARD' || busy" @click="fillSample()"><strong>Điền thẻ thử thành công</strong><code>{{ DEMO_CARDS[0].number }}</code></button><details><summary>Thử các trường hợp thất bại</summary><button v-for="sample in DEMO_CARDS.slice(1)" :key="sample.number" type="button" class="sample-failure" :disabled="!active || status !== 'AWAITING_CARD' || busy" @click="fillSample(sample)"><span>{{ sample.label }}</span><code>{{ sample.number }}</code></button></details><a href="https://sandbox.vnpayment.vn/apis/vnpay-demo/" target="_blank" rel="noopener noreferrer" class="test-source">Nguồn thông tin thẻ thử: VNPAY ↗</a></section>
      </aside>
    </div>
  </div>
</template>
<style scoped>
.card-demo-page{max-width:1100px}.check-result{display:block;margin:18px auto 0;font-size:12px}.payment-heading{display:flex;align-items:center;justify-content:space-between;gap:20px;margin:10px 0 24px}.payment-heading h1{font-size:30px;margin:10px 0;color:var(--customer-ink)}.payment-heading p{font-size:13px;color:var(--customer-muted)}.demo-seal{padding:12px 18px;background:#fff2d6;color:#805b19;text-align:center;border:1px solid #e9d199;border-radius:12px;font-size:13px;font-weight:800;white-space:nowrap;letter-spacing:1px}.demo-seal small{font-size:8px;letter-spacing:.6px}.demo-banner{background:#fff9eb;border:1px solid #ecdbb1;border-left:4px solid #c49337;border-radius:10px;padding:17px 20px;margin-bottom:24px;font-size:12px;line-height:1.8;color:#765821}.demo-banner strong{font-size:13px}.demo-banner p{margin:4px 0 0}.payment-columns{display:grid;grid-template-columns:minmax(0,1.35fr) minmax(280px,1fr);align-items:start;gap:24px}.payment-main{padding:28px}.bank-heading{display:flex;align-items:center;gap:14px;padding-bottom:22px;border-bottom:1px solid var(--customer-border)}.bank-mark{color:#0068a8;font-size:25px;font-weight:900;letter-spacing:1px;background:#f1f7fc;border-radius:9px;padding:12px 15px}.bank-heading strong,.bank-heading small{display:block}.bank-heading strong{font-size:14px}.bank-heading small{font-size:10px;color:var(--customer-muted);margin-top:6px}.payment-steps{display:flex;justify-content:space-between;list-style:none;padding:18px 0 20px;gap:10px;font-size:10px;color:#8895a8}.payment-steps li.current{color:var(--customer-primary);font-weight:750}.payment-main h2{font-size:20px;margin:3px 0 18px}.payment-countdown{display:flex;align-items:center;justify-content:space-between;gap:10px;padding:11px 15px;background:#eef5ff;border-radius:9px;color:#345e93;font-size:11px;margin:16px 0}.payment-countdown strong{font-size:24px;letter-spacing:2px;font-variant-numeric:tabular-nums}.payment-countdown.urgent{background:#fff0e4;color:#a64c14}.payment-message{font-size:12px;line-height:1.7;margin:15px 0;color:#61728a}.payment-label{display:block;font-size:12px;font-weight:650;margin:20px 0 9px}.payment-label span{color:#c14242}.payment-input{display:block;width:100%;min-width:0;border:1px solid #ccd9e8;border-radius:8px;background:#fff;min-height:46px;padding:11px 13px;color:#18324e;font:inherit;font-size:14px}.payment-input:focus{outline:3px solid #dceaff;border-color:#5c98e7}.payment-input[aria-invalid=true]{border-color:#c65b5b}.card-number{font-variant-numeric:tabular-nums;letter-spacing:1px}.holder-input{text-transform:uppercase}.card-field-grid{display:grid;grid-template-columns:minmax(0,1.6fr) minmax(0,1fr);gap:16px}.payment-help{color:var(--customer-muted);font-size:10px;line-height:1.8;margin:12px 0}.payment-submit{width:100%;margin-top:22px}.cancel-payment{display:block;margin:22px auto 0;font-size:12px;color:#718199}.payment-aside{display:grid;gap:20px}.payment-summary h2{font-size:20px;margin:13px 0 22px}.payment-summary dt{font-size:11px;color:var(--customer-muted);margin:17px 0 7px}.payment-summary dd{font-size:12px;margin:0;font-weight:600}.payment-summary dd.reference,.result-reference{font-size:10px;font-weight:400;overflow-wrap:anywhere;line-height:1.9}.payment-summary dd.payment-total{font-size:27px;color:var(--customer-primary)}.payment-summary ul{padding-left:16px;line-height:1.9}.test-card-guide h2{font-size:15px;margin-bottom:12px}.test-card-guide>p{font-size:11px;color:var(--customer-muted);line-height:1.8}.test-card-guide button{width:100%;text-align:left;cursor:pointer;font:inherit;border-radius:8px;padding:12px;margin:14px 0 0}.sample-success{border:1px solid #b9d5ee;background:#f0f7ff;color:#275b90}.test-card-guide button strong,.test-card-guide button span{display:block;font-size:11px;margin-bottom:7px}.test-card-guide code{font-size:12px;overflow-wrap:anywhere}.test-card-guide button:disabled{opacity:.55;cursor:default}.test-card-guide details{margin:17px 0;font-size:11px}.test-card-guide summary{cursor:pointer;color:#63758b}.sample-failure{border:1px solid #e3e7ed;background:#fafbfc;color:#627086}.test-source{font-size:10px;text-decoration:underline;color:#65758c}.otp-hint{font-size:12px;background:#f1f7ff;padding:16px;border-radius:10px;line-height:1.9}.otp-hint strong{letter-spacing:3px;color:#2468b8}.otp-input{letter-spacing:8px;font-size:23px;text-align:center}.payment-result{text-align:center;padding:26px 0 12px}.result-icon{display:grid;place-items:center;width:70px;height:70px;border-radius:50%;margin:0 auto 20px;background:#f0f3f8;font-size:35px;color:#7a889c}.success .result-icon{background:#e9f8ee;color:#248557}.payment-result h3{font-size:20px}.payment-result p{font-size:12px;line-height:1.9;margin:17px 0;color:#65758c}.payment-result .button{margin-top:12px}@media(max-width:760px){.payment-columns{grid-template-columns:1fr}.payment-heading h1{font-size:24px}.payment-main{padding:20px}.demo-seal{padding:10px;font-size:11px}.demo-seal small{font-size:6px}.card-field-grid{gap:12px;grid-template-columns:1fr}.payment-heading{align-items:flex-start}.payment-steps{font-size:9px;gap:6px}}
</style>
