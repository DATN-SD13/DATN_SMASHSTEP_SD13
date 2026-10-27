<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { commerceService, commerceError } from '../services/commerceService'
import { money } from '../utils/money'
import { countdownAnchor, remainingSeconds, countdownLabel } from '../utils/qrPayment'
import { DEMO_CARDS, DEMO_HOLDER, DEMO_ISSUE_DATE, demoCardError } from '../utils/demoCard'

const route = useRoute(), cart = useCartStore()
// Public simulator fixtures. These are not bank, customer or VNPAY merchant credentials.
const TEST_USERNAME = 'smashstep_test', TEST_PASSWORD = 'Test@123456'
const session = ref(null), mode = ref('account'), account = ref({ username: '', password: '' })
const card = ref({ number: '', holder: '', issueDate: '' }), otp = ref('')
const loading = ref(false), busy = ref(false), error = ref(''), fields = ref({})
const deadline = ref(null), monotonicNow = ref(performance.now())
const seconds = computed(() => remainingSeconds(deadline.value, monotonicNow.value))
const waiting = computed(() => ['AWAITING_CARD', 'AWAITING_OTP'].includes(session.value?.status))
const status = computed(() => waiting.value && seconds.value <= 0 ? 'EXPIRED' : session.value?.status)
const active = computed(() => waiting.value && seconds.value > 0)
const title = computed(() => ({ AWAITING_CARD: mode.value === 'account' ? 'Đăng nhập tài khoản thanh toán thử' : 'Thông tin thẻ thử', AWAITING_OTP: 'Xác thực giao dịch thử', PAID_DEMO: 'Thanh toán thử thành công', FAILED: 'Thanh toán thử thất bại', CANCELED: 'Đã hủy thanh toán thử', EXPIRED: 'Phiên thanh toán đã hết hạn' }[status.value] || 'Thanh toán giả lập'))
let generation = 0, disposed = false
function clearCredentials() { account.value = { username: '', password: '' }; card.value = { number: '', holder: '', issueDate: '' }; otp.value = '' }
function apply(next, started = performance.now()) {
  const nextDeadline = countdownAnchor(next, started)
  deadline.value = session.value?.code === next.code && Number.isFinite(deadline.value)
    ? Math.min(deadline.value, nextDeadline ?? started) : nextDeadline
  session.value = next; monotonicNow.value = performance.now()
  if (!waiting.value) clearCredentials()
}
async function load() {
  const current = ++generation, started = performance.now(); loading.value = true
  session.value = null; deadline.value = null; error.value = ''; fields.value = {}; clearCredentials()
  try {
    const next = await commerceService.demoPayment(route.params.code)
    if (current === generation && !disposed) apply(next, started)
  } catch (e) { if (current === generation && !disposed) error.value = commerceError(e) }
  finally { if (current === generation && !disposed) loading.value = false }
}
function selectMode(next) { mode.value = next; fields.value = {}; error.value = ''; clearCredentials() }
function fillAccount() {
  if (!active.value || status.value !== 'AWAITING_CARD' || busy.value) return
  selectMode('account'); account.value = { username: TEST_USERNAME, password: TEST_PASSWORD }
}
function fillSample(sample = DEMO_CARDS[0]) {
  if (!active.value || status.value !== 'AWAITING_CARD' || busy.value) return
  selectMode('card'); card.value = { number: sample.number, holder: DEMO_HOLDER, issueDate: DEMO_ISSUE_DATE }
}
function issueInput(event) {
  const digits = event.target.value.replace(/\D/g, '').slice(0, 4)
  card.value.issueDate = digits.length > 2 ? digits.slice(0, 2) + '/' + digits.slice(2) : digits
}
async function send(action) {
  if (busy.value || !active.value) return
  fields.value = {}; error.value = ''
  if (action === 'account' && (!account.value.username.trim() || !account.value.password)) {
    fields.value = { account: 'Nhập đầy đủ tài khoản và mật khẩu thử.' }; return
  }
  if (action === 'card') {
    const invalid = demoCardError(card.value)
    if (invalid) { fields.value = { card: invalid }; return }
  }
  if (action === 'otp' && !/^[0-9]{6}$/.test(otp.value)) { fields.value = { otp: 'Nhập OTP thử gồm đúng 6 chữ số.' }; return }
  const current = generation, started = performance.now(), code = session.value.code
  busy.value = true
  try {
    const next = action === 'account' ? await commerceService.demoAccount(code, { ...account.value })
      : action === 'card' ? await commerceService.demoCard(code, { ...card.value })
      : action === 'otp' ? await commerceService.demoOtp(code, { otp: otp.value, challenge: session.value.challenge })
      : await commerceService.demoCancel(code)
    if (current !== generation || disposed) return
    apply(next, started)
    if (next.status !== 'AWAITING_CARD') { account.value = { username: '', password: '' }; card.value = { number: '', holder: '', issueDate: '' } }
  } catch (e) { if (current === generation && !disposed) error.value = commerceError(e) }
  finally {
    if (current === generation && !disposed) { busy.value = false; otp.value = ''; account.value.password = '' }
  }
}
watch([() => route.params.code, () => cart.owner], () => { busy.value = false; load() }, { immediate: true })
const timer = setInterval(() => { monotonicNow.value = performance.now(); if (waiting.value && seconds.value <= 0) clearCredentials() }, 500)
onBeforeUnmount(() => { disposed = true; generation++; clearInterval(timer); clearCredentials() })
</script>
<template>
  <div class="container shop-page card-demo-page">
    <RouterLink to="/gio-hang" class="back-link">← Quay lại giỏ hàng</RouterLink>
    <header class="payment-heading"><div><span class="eyebrow">SMASHSTEP · CHẾ ĐỘ GIẢ LẬP</span><h1>Thanh toán thử</h1><p>Nhập tài khoản thử, mật khẩu và OTP để hoàn tất đơn thử.</p></div><span class="demo-seal">GIẢ LẬP<br><small>KHÔNG THU TIỀN</small></span></header>
    <div class="demo-banner" role="note"><strong>Chỉ dùng bộ tài khoản mẫu bên dưới.</strong><p>Đây là mô phỏng tại SmashStep. Không nhập tài khoản, mật khẩu ngân hàng, mật khẩu đăng nhập cửa hàng hoặc mật khẩu Merchant VNPAY.</p><p>Khi OTP đúng, đơn thử được lưu và hiện trong quản lý hóa đơn. Giỏ hàng được giữ nguyên; không trừ kho, tiêu voucher hoặc cộng doanh thu thật.</p></div>
    <p v-if="loading" class="shop-card" role="status">Đang tải phiên thanh toán…</p>
    <p v-if="error" class="shop-alert" role="alert">{{ error }} <button v-if="!session" type="button" class="text-button" @click="load">Tải lại phiên</button></p>
    <div v-if="session && !loading" class="payment-columns">
      <section class="shop-card payment-main" :class="{ success: status === 'PAID_DEMO' }">
        <div class="bank-heading"><div class="bank-mark">TEST</div><div><strong>Cổng thanh toán giả lập</strong><small>Tài khoản thử → Mật khẩu → OTP</small></div></div>
        <ol class="payment-steps" aria-label="Các bước thanh toán"><li :class="{ current: status === 'AWAITING_CARD' }">1. Tài khoản / thẻ thử</li><li :class="{ current: status === 'AWAITING_OTP' }">2. Xác thực OTP</li><li :class="{ current: status === 'PAID_DEMO' }">3. Lưu đơn thành công</li></ol>
        <h2 id="payment-state" role="status">{{ title }}</h2>
        <div v-if="active" class="payment-countdown" :class="{ urgent: seconds <= 60 }"><span>Phiên có hiệu lực trong</span><strong role="timer">{{ countdownLabel(seconds) }}</strong></div>
        <p v-if="session.message" class="payment-message" role="status">{{ session.message }}</p>
        <div v-if="active && status === 'AWAITING_CARD'" class="payment-modes" aria-label="Cách thanh toán thử"><button type="button" :class="{ selected: mode === 'account' }" :aria-pressed="mode === 'account'" :disabled="busy" @click="selectMode('account')">Tài khoản thử</button><button type="button" :class="{ selected: mode === 'card' }" :aria-pressed="mode === 'card'" :disabled="busy" @click="selectMode('card')">Thẻ NCB thử</button></div>
        <form v-if="active && status === 'AWAITING_CARD' && mode === 'account'" autocomplete="off" novalidate @submit.prevent="send('account')">
          <label class="payment-label" for="demo-username">Tài khoản thử <span>*</span></label><input id="demo-username" v-model="account.username" class="payment-input" maxlength="50" autocomplete="off" autocapitalize="none" spellcheck="false" placeholder="smashstep_test" :disabled="busy" :aria-invalid="!!fields.account" aria-describedby="demo-account-help">
          <label class="payment-label" for="demo-password">Mật khẩu thử <span>*</span></label><input id="demo-password" v-model="account.password" type="password" class="payment-input" maxlength="100" autocomplete="off" placeholder="Nhập mật khẩu thử bên cạnh" :disabled="busy" :aria-invalid="!!fields.account" aria-describedby="demo-account-help">
          <p id="demo-account-help" class="payment-help">Tài khoản mẫu dùng chung cho mô phỏng, không cần đăng ký tài khoản ngân hàng.</p>
          <p v-if="fields.account" class="shop-error" role="alert">{{ fields.account }}</p>
          <button class="button button-primary payment-submit" type="submit" :disabled="busy">{{ busy ? 'Đang kiểm tra…' : 'Tiếp tục nhập OTP →' }}</button>
        </form>
        <form v-else-if="active && status === 'AWAITING_CARD'" autocomplete="off" novalidate @submit.prevent="send('card')">
          <label class="payment-label" for="demo-card-number">Số thẻ thử <span>*</span></label><input id="demo-card-number" v-model="card.number" class="payment-input card-number" inputmode="numeric" maxlength="24" autocomplete="off" placeholder="Nhập số thẻ NCB thử" :disabled="busy" :aria-invalid="!!fields.card">
          <div class="card-field-grid"><div><label class="payment-label" for="demo-card-holder">Tên chủ thẻ thử <span>*</span></label><input id="demo-card-holder" v-model="card.holder" class="payment-input holder-input" maxlength="50" autocomplete="off" placeholder="NGUYEN VAN A" :disabled="busy"></div><div><label class="payment-label" for="demo-card-issued">Ngày phát hành <span>*</span></label><input id="demo-card-issued" :value="card.issueDate" class="payment-input" inputmode="numeric" maxlength="5" autocomplete="off" placeholder="MM/YY" :disabled="busy" @input="issueInput"></div></div>
          <p class="payment-help">Dùng ngày phát hành 07/15, không cần CVV hay mật khẩu ngân hàng thật.</p><p v-if="fields.card" class="shop-error" role="alert">{{ fields.card }}</p>
          <button class="button button-primary payment-submit" type="submit" :disabled="busy">{{ busy ? 'Đang kiểm tra thẻ…' : 'Tiếp tục nhập OTP →' }}</button>
        </form>
        <form v-else-if="active && status === 'AWAITING_OTP'" autocomplete="off" novalidate @submit.prevent="send('otp')">
          <p class="otp-hint">Mã OTP thử là <strong>123456</strong>. Mã công khai để tự kiểm thử, không gửi SMS.</p>
          <label class="payment-label" for="demo-otp">Mã OTP thử <span>*</span></label><input id="demo-otp" v-model="otp" class="payment-input otp-input" inputmode="numeric" maxlength="6" autocomplete="off" placeholder="6 chữ số" :disabled="busy" :aria-invalid="!!fields.otp">
          <p v-if="fields.otp" class="shop-error" role="alert">{{ fields.otp }}</p>
          <button class="button button-primary payment-submit" type="submit" :disabled="busy">{{ busy ? 'Đang xác thực và lưu đơn…' : 'Xác nhận thanh toán thử' }}</button>
        </form>
        <div v-else class="payment-result"><span class="result-icon" aria-hidden="true">{{ status === 'PAID_DEMO' ? '✓' : status === 'EXPIRED' ? '⌛' : '×' }}</span><h3>{{ status === 'PAID_DEMO' ? 'Đã thanh toán thử và lưu đơn' : 'Giao dịch chưa hoàn tất' }}</h3><p v-if="status === 'PAID_DEMO'">{{ money(session.amount) }} · Không thu tiền thật.<br>Đã lưu trong Quản lý hóa đơn → Đơn thử đã thanh toán. Kết quả vẫn còn khi tải lại trang hoặc khởi động lại backend.</p><p v-else-if="status === 'EXPIRED'">Đã hết 5 phút. Vui lòng quay lại giỏ hàng để tạo phiên thử mới.</p><p v-else>Không thu tiền và không tạo đơn đã thanh toán trong quản lý.</p><p v-if="session.reference" class="result-reference">Mã giao dịch thử: {{ session.reference }}</p><RouterLink class="button button-primary" to="/gio-hang">Quay lại giỏ hàng</RouterLink><button type="button" class="text-button check-result" :disabled="busy" @click="load">Kiểm tra lại kết quả từ máy chủ</button></div>
        <button v-if="active" type="button" class="cancel-payment text-button" :disabled="busy" @click="send('cancel')">Hủy giao dịch thử</button>
      </section>
      <aside class="payment-aside">
        <section class="shop-card test-card-guide"><h2>Tài khoản thanh toán thử</h2><dl class="sample-account"><dt>Tài khoản</dt><dd><code>{{ TEST_USERNAME }}</code></dd><dt>Mật khẩu</dt><dd><code>{{ TEST_PASSWORD }}</code></dd><dt>OTP</dt><dd><code>123456</code></dd></dl><p>Nhập đúng tài khoản và mật khẩu, sau đó nhập OTP để lưu đơn thử vào quản trị. Nhập sai tối đa 5 lần; phiên hết hạn sau 5 phút.</p><button type="button" class="sample-success" :disabled="!active || status !== 'AWAITING_CARD' || busy" @click="fillAccount"><strong>Điền tài khoản thử</strong><span>Tự điền tài khoản và mật khẩu mẫu</span></button></section>
        <section class="shop-card payment-summary"><span class="eyebrow">CHI TIẾT GIAO DỊCH THỬ</span><h2>Đơn hàng SmashStep</h2><dl><dt>Mã phiên thử</dt><dd class="reference">{{ session.code }}</dd><dt>Số tiền thanh toán thử</dt><dd class="payment-total">{{ money(session.amount) }}</dd><dt>Sản phẩm đã chọn</dt><dd><ul><li v-for="line in session.lines" :key="line.variant.variantId">{{ line.variant.name }} · {{ line.variant.color }} / {{ line.variant.size }} × {{ line.quantity }}</li></ul></dd><dt>Thời hạn</dt><dd>5 phút từ khi tạo phiên</dd><template v-if="session.paidAt"><dt>Xác thực thành công lúc</dt><dd>{{ new Date(session.paidAt).toLocaleString('vi-VN') }}</dd></template></dl><p class="payment-help">Chỉ đơn thử đã xác thực thành công mới được lưu và hiển thị trong quản trị.</p></section>
        <section class="shop-card test-card-guide"><details><summary>Thông tin thẻ NCB thử (tùy chọn)</summary><p>NCB · {{ DEMO_HOLDER }} · Ngày phát hành {{ DEMO_ISSUE_DATE }}</p><button type="button" class="sample-success" :disabled="!active || status !== 'AWAITING_CARD' || busy" @click="fillSample()"><strong>Điền thẻ thử thành công</strong><code>{{ DEMO_CARDS[0].number }}</code></button><button v-for="sample in DEMO_CARDS.slice(1)" :key="sample.number" type="button" class="sample-failure" :disabled="!active || status !== 'AWAITING_CARD' || busy" @click="fillSample(sample)"><span>{{ sample.label }}</span><code>{{ sample.number }}</code></button></details></section>
      </aside>
    </div>
  </div>
</template>
<style scoped>
.card-demo-page{max-width:1100px}.check-result{display:block;margin:18px auto 0;font-size:12px}.payment-heading{display:flex;align-items:center;justify-content:space-between;gap:20px;margin:10px 0 24px}.payment-heading h1{font-size:30px;margin:10px 0;color:var(--customer-ink)}.payment-heading p{font-size:13px;color:var(--customer-muted)}.demo-seal{padding:12px 18px;background:#fff2d6;color:#805b19;text-align:center;border:1px solid #e9d199;border-radius:12px;font-size:13px;font-weight:800;white-space:nowrap;letter-spacing:1px}.demo-seal small{font-size:8px;letter-spacing:.6px}.demo-banner{background:#fff9eb;border:1px solid #ecdbb1;border-left:4px solid #c49337;border-radius:10px;padding:17px 20px;margin-bottom:24px;font-size:12px;line-height:1.8;color:#765821}.demo-banner strong{font-size:13px}.demo-banner p{margin:4px 0 0}.payment-columns{display:grid;grid-template-columns:minmax(0,1.35fr) minmax(280px,1fr);align-items:start;gap:24px}.payment-main{padding:28px}.bank-heading{display:flex;align-items:center;gap:14px;padding-bottom:22px;border-bottom:1px solid var(--customer-border)}.bank-mark{color:#0068a8;font-size:25px;font-weight:900;letter-spacing:1px;background:#f1f7fc;border-radius:9px;padding:12px 15px}.bank-heading strong,.bank-heading small{display:block}.bank-heading strong{font-size:14px}.bank-heading small{font-size:10px;color:var(--customer-muted);margin-top:6px}.payment-steps{display:flex;justify-content:space-between;list-style:none;padding:18px 0 20px;gap:10px;font-size:10px;color:#8895a8}.payment-steps li.current{color:var(--customer-primary);font-weight:750}.payment-main h2{font-size:20px;margin:3px 0 18px}.payment-countdown{display:flex;align-items:center;justify-content:space-between;gap:10px;padding:11px 15px;background:#eef5ff;border-radius:9px;color:#345e93;font-size:11px;margin:16px 0}.payment-countdown strong{font-size:24px;letter-spacing:2px;font-variant-numeric:tabular-nums}.payment-countdown.urgent{background:#fff0e4;color:#a64c14}.payment-message{font-size:12px;line-height:1.7;margin:15px 0;color:#61728a}.payment-label{display:block;font-size:12px;font-weight:650;margin:20px 0 9px}.payment-label span{color:#c14242}.payment-input{display:block;width:100%;min-width:0;border:1px solid #ccd9e8;border-radius:8px;background:#fff;min-height:46px;padding:11px 13px;color:#18324e;font:inherit;font-size:14px}.payment-input:focus{outline:3px solid #dceaff;border-color:#5c98e7}.payment-input[aria-invalid=true]{border-color:#c65b5b}.card-number{font-variant-numeric:tabular-nums;letter-spacing:1px}.holder-input{text-transform:uppercase}.card-field-grid{display:grid;grid-template-columns:minmax(0,1.6fr) minmax(0,1fr);gap:16px}.payment-help{color:var(--customer-muted);font-size:10px;line-height:1.8;margin:12px 0}.payment-submit{width:100%;margin-top:22px}.cancel-payment{display:block;margin:22px auto 0;font-size:12px;color:#718199}.payment-aside{display:grid;gap:20px}.payment-summary h2{font-size:20px;margin:13px 0 22px}.payment-summary dt{font-size:11px;color:var(--customer-muted);margin:17px 0 7px}.payment-summary dd{font-size:12px;margin:0;font-weight:600}.payment-summary dd.reference,.result-reference{font-size:10px;font-weight:400;overflow-wrap:anywhere;line-height:1.9}.payment-summary dd.payment-total{font-size:27px;color:var(--customer-primary)}.payment-summary ul{padding-left:16px;line-height:1.9}.test-card-guide h2{font-size:15px;margin-bottom:12px}.test-card-guide>p{font-size:11px;color:var(--customer-muted);line-height:1.8}.test-card-guide button{width:100%;text-align:left;cursor:pointer;font:inherit;border-radius:8px;padding:12px;margin:14px 0 0}.sample-success{border:1px solid #b9d5ee;background:#f0f7ff;color:#275b90}.test-card-guide button strong,.test-card-guide button span{display:block;font-size:11px;margin-bottom:7px}.test-card-guide code{font-size:12px;overflow-wrap:anywhere}.test-card-guide button:disabled{opacity:.55;cursor:default}.test-card-guide details{margin:17px 0;font-size:11px}.test-card-guide summary{cursor:pointer;color:#63758b}.sample-failure{border:1px solid #e3e7ed;background:#fafbfc;color:#627086}.test-source{font-size:10px;text-decoration:underline;color:#65758c}.otp-hint{font-size:12px;background:#f1f7ff;padding:16px;border-radius:10px;line-height:1.9}.otp-hint strong{letter-spacing:3px;color:#2468b8}.otp-input{letter-spacing:8px;font-size:23px;text-align:center}.payment-result{text-align:center;padding:26px 0 12px}.result-icon{display:grid;place-items:center;width:70px;height:70px;border-radius:50%;margin:0 auto 20px;background:#f0f3f8;font-size:35px;color:#7a889c}.success .result-icon{background:#e9f8ee;color:#248557}.payment-result h3{font-size:20px}.payment-result p{font-size:12px;line-height:1.9;margin:17px 0;color:#65758c}.payment-result .button{margin-top:12px}@media(max-width:760px){.payment-columns{grid-template-columns:1fr}.payment-heading h1{font-size:24px}.payment-main{padding:20px}.demo-seal{padding:10px;font-size:11px}.demo-seal small{font-size:6px}.card-field-grid{gap:12px;grid-template-columns:1fr}.payment-heading{align-items:flex-start}.payment-steps{font-size:9px;gap:6px}}
.payment-modes{display:flex;gap:8px;margin:18px 0}.payment-modes button{flex:1;border:1px solid #cdd9e8;border-radius:8px;padding:10px;background:#fff;font:inherit;font-size:12px;color:#52677e;cursor:pointer}.payment-modes button.selected{background:#eef5ff;border-color:#5c98e7;color:#2468b8;font-weight:700}.payment-modes button:disabled{opacity:.6;cursor:default}.sample-account{display:grid;grid-template-columns:80px 1fr;gap:12px;font-size:12px;margin:20px 0}.sample-account dt{color:#718199}.sample-account dd{margin:0;overflow-wrap:anywhere}.sample-account code{font-weight:700;color:#275b90}</style>
