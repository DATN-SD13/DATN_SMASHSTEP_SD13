<script setup>
import { ref, computed, onMounted, watch, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { useAuthStore } from '../stores/auth'
import { accountService } from '../services/accountService'
import { commerceService, commerceError } from '../services/commerceService'
import { useLiveRefresh } from '../composables/useLiveRefresh'
import { nameError, phoneError } from '../utils/accountValidation'
import { money } from '../utils/money'
import ProductImage from '../components/ProductImage.vue'
import CheckoutReceiverForm from '../components/CheckoutReceiverForm.vue'
import VoucherSelector from '../components/VoucherSelector.vue'
const attempt = ref(null), auth = useAuthStore(), cart = useCartStore(), router = useRouter(), route = useRoute()
const checkoutOwner = cart.owner
const pendingKey = 'ss_checkout_pending_' + auth.user?.id
const profile = ref(null), quote = ref(null), loading = ref(true), updating = ref(false), submitting = ref(false)
const error = ref(''), notice = ref(''), profileError = ref(''), profileLoading = ref(false)
const selectedAddress = ref(null), voucherCode = ref(''), method = ref(null), note = ref('')
const saveAddress = ref(false), makeDefaultAddress = ref(false), addressSaving = ref(false), addressResolving = ref(false)
const receiver = ref({ name: '', phone: '', email: '', province: '', district: '', ward: '', street: '', provinceId: null, districtId: null, wardCode: '' })
let requestId = crypto.randomUUID(), timer, version = 0, disposed = false
watch(() => cart.owner, next => {
  if (next !== checkoutOwner) { version++; clearTimeout(timer); loading.value = true; quote.value = null; router.replace('/gio-hang') }
}, { flush: 'sync' })
const vnpayEnabled = ref(false), DEMO = 'DEMO', VNPAY = 'VNPAY_SANDBOX'
const testPayment = computed(() => method.value === DEMO)
const paymentMethods = computed(() => (quote.value?.methods || []).filter(method => method.supported))
const checkoutItems = computed(() => attempt.value?.items || cart.selectedItems)
const checkoutCount = computed(() => checkoutItems.value.reduce((sum, i) => sum + i.quantity, 0))
const checkoutLines = computed(() => checkoutItems.value.map(i => ({ ...i, variant: cart.lines.find(l => l.variantId === i.variantId)?.variant })))
const addresses = computed(() => profile.value?.addresses || [])
const addressLocked = computed(() => submitting.value || !!attempt.value || addressSaving.value)
function rememberAttempt() {
  try { if (attempt.value) sessionStorage.setItem(pendingKey, JSON.stringify(attempt.value)); else sessionStorage.removeItem(pendingKey) }
  catch { notice.value = 'Không lưu được phiên trong trình duyệt. Hãy giữ tab này mở nếu cần thử lại đặt hàng.' }
}
function chooseAddress(a) {
  selectedAddress.value = a?.id || null
  receiver.value = { name: a?.receiverName || profile.value?.name || auth.user?.name || '', phone: a?.receiverPhone || profile.value?.phone || '',
    email: profile.value?.email || auth.user?.email || '', province: a?.province || '', district: a?.district || '', ward: a?.ward || '', street: a?.street || '',
    provinceId: null, districtId: null, wardCode: '' }
  saveAddress.value = !a
  makeDefaultAddress.value = !addresses.value.length
}
async function loadProfile(prefill = false) {
  profileLoading.value = true; profileError.value = ''
  try {
    const data = await accountService.profile()
    if (disposed || cart.owner !== checkoutOwner) return
    profile.value = { ...data, addresses: Array.isArray(data.addresses) ? data.addresses : [] }
    auth.updateProfile(data)
  } catch (e) {
    if (disposed || cart.owner !== checkoutOwner) return
    profileError.value = 'Chưa tải được hồ sơ. Bạn vẫn có thể điền thông tin nhận hàng bên dưới. ' + commerceError(e)
    try { profile.value = { ...auth.user, addresses: await accountService.addresses() } }
    catch { profile.value = { ...auth.user, addresses: [] } }
  } finally {
    if (!disposed && cart.owner === checkoutOwner) {
      if (prefill && !attempt.value) chooseAddress(profile.value?.defaultAddress || addresses.value.find(a => a.isDefault) || addresses.value[0])
      profileLoading.value = false
    }
  }
}
function payload() {
  const r = Object.fromEntries(Object.entries(receiver.value).map(([key, value]) => [key, typeof value === 'string' ? value.trim() : value]))
  return { items: cart.selectedItems.map(i => ({ ...i })), voucherCode: voucherCode.value || null, paymentMethodId: [DEMO, VNPAY].includes(method.value) ? null : method.value,
    receiver: { ...r, email: null }, note: note.value.trim(), saveAddress: !testPayment.value && saveAddress.value, makeDefaultAddress: !testPayment.value && saveAddress.value && makeDefaultAddress.value }
}
function receiverError() {
  const r = receiver.value
  return nameError(r.name) || phoneError(r.phone)
    || (!r.street.trim() || !r.province || !r.ward ? 'Vui lòng nhập đầy đủ địa chỉ nhận hàng.' : '')
    || (!r.provinceId || !r.wardCode ? 'Vui lòng chọn tỉnh / thành phố và phường / xã để tính phí giao hàng.' : '')
    || ([r.street,r.ward,r.province].filter(Boolean).join(', ').length > 500 ? 'Địa chỉ giao hàng đầy đủ tối đa 500 ký tự.' : '')
}
async function refresh() {
  if (!cart.selectedItems.length || submitting.value || attempt.value || disposed || cart.owner !== checkoutOwner) { if (!submitting.value) { updating.value = false; if (!cart.selectedItems.length && !attempt.value) quote.value = null } return }
  const current = ++version; updating.value = true
  try {
    await cart.refresh()
    if (disposed || current !== version || cart.owner !== checkoutOwner || !cart.selectedItems.length) return
    const body = payload(), invalidReceiver = receiverError(); if (invalidReceiver) body.receiver = null
    const next = await (method.value === VNPAY ? commerceService.vnpayQuote(body) : method.value === DEMO ? commerceService.demoQuote(body) : commerceService.quote(body))
    if (disposed || cart.owner !== checkoutOwner || current !== version) return
    if (quote.value?.total != null && next.total != null && quote.value.total !== next.total)
      notice.value = 'Giá hoặc phí giao hàng vừa được cập nhật. Vui lòng kiểm tra tổng trước khi đặt hàng.'
    quote.value = invalidReceiver ? { ...next, message: invalidReceiver } : next; error.value = ''
    if (method.value == null) method.value = next.methods.find(m => m.supported)?.id || null
  } catch (e) { if (current === version) { error.value = commerceError(e); quote.value = null } }
  finally { if (current === version) updating.value = false }
}
watch([receiver, voucherCode, method, note, saveAddress, makeDefaultAddress, () => cart.selectedItems], () => {
  if (!loading.value && !submitting.value && !attempt.value) {
    clearTimeout(timer); version++; updating.value = true; timer = setTimeout(refresh, 500); requestId = crypto.randomUUID()
  }
}, { deep: true, flush: 'sync' })
async function saveAddressNow() {
  if (addressLocked.value || addressResolving.value || testPayment.value) return
  const invalid = receiverError(); if (invalid) { error.value = invalid; return }
  addressSaving.value = true; error.value = ''
  try {
    const r = receiver.value
    const saved = await accountService.createAddress({ receiverName: r.name.trim(), receiverPhone: r.phone.trim(),
      province: r.province, district: '', ward: r.ward, street: r.street.trim(), addressType: 1, isDefault: makeDefaultAddress.value })
    if (disposed || cart.owner !== checkoutOwner) return
    profile.value = { ...(profile.value || auth.user), addresses: [saved, ...addresses.value.filter(a => a.id !== saved.id)] }
    selectedAddress.value = saved.id; saveAddress.value = false
    notice.value = 'Đã lưu địa chỉ vào tài khoản. Lần mua sau sẽ tự điền địa chỉ này.'
    try { const data = await accountService.profile(); if (!disposed && cart.owner === checkoutOwner) profile.value = data }
    catch (e) { if (!disposed && cart.owner === checkoutOwner) profileError.value = 'Địa chỉ đã được lưu; chưa tải lại được hồ sơ. ' + commerceError(e) }
  } catch (e) { error.value = commerceError(e) }
  finally { addressSaving.value = false }
}
async function placeOrder() {
  if (submitting.value || updating.value || addressSaving.value || (!attempt.value && addressResolving.value)) return
  const invalid = attempt.value ? '' : receiverError(); if (invalid) { error.value = invalid; return }
  if (!attempt.value && (!quote.value?.canCheckout || cart.selectedInvalid)) return
  submitting.value = true; error.value = ''
  try {
    if (!attempt.value) { attempt.value = { ...payload(), requestId, expectedTotal: quote.value.total, demo: method.value === DEMO, vnpay: method.value === VNPAY }; rememberAttempt() }
    const sent = attempt.value
    const { demo, vnpay, ...body } = sent
    const order = await (vnpay ? commerceService.vnpayCreate(body) : demo ? commerceService.demoCreate(body) : commerceService.checkout(body))
    if (vnpay && order.paymentUrl && new URL(order.paymentUrl).origin !== 'https://sandbox.vnpayment.vn') throw new Error('Địa chỉ VNPAY Sandbox không hợp lệ')
    if (!demo && !vnpay) cart.purchased(sent.items, checkoutOwner)
    attempt.value = null; rememberAttempt()
    if (disposed || cart.owner !== checkoutOwner) return
    if (vnpay) {
      if (order.paymentUrl) window.location.assign(order.paymentUrl)
      else await router.push('/thanh-toan-vnpay/phien/' + order.code)
    } else await router.push((demo ? '/thanh-toan-thu/' : '/don-hang/') + order.code)
  } catch (e) {
    if (disposed || cart.owner !== checkoutOwner) return
    error.value = commerceError(e)
    if (e.response && e.response.status < 500) { attempt.value = null; rememberAttempt(); submitting.value = false; requestId = crypto.randomUUID(); await refresh(); notice.value = commerceError(e) }
    else notice.value = 'Chưa xác định được kết quả đặt hàng. Bấm thử lại cùng phiên hoặc xem Đơn hàng của bạn trước khi đặt đơn khác.'
  } finally { submitting.value = false }
}
onMounted(async () => {
  try {
    const saved = JSON.parse(sessionStorage.getItem(pendingKey) || 'null')
    if (saved?.requestId && Array.isArray(saved.items) && saved.receiver) {
      attempt.value = saved; receiver.value = saved.receiver; voucherCode.value = saved.voucherCode || ''; method.value = saved.vnpay ? VNPAY : saved.demo ? DEMO : saved.paymentMethodId; note.value = saved.note || ''
      notice.value = 'Có phiên đặt hàng chưa xác định kết quả. Bấm thử lại cùng phiên để tránh tạo đơn trùng.'
    }
  } catch { try { sessionStorage.removeItem(pendingKey) } catch { /* Storage disabled. */ } }
  await loadProfile(true)
  const [gatewayConfig] = await Promise.allSettled([commerceService.vnpayConfig()])
  vnpayEnabled.value = gatewayConfig.status === 'fulfilled' && gatewayConfig.value.enabled === true
  if (disposed || cart.owner !== checkoutOwner) return
  if (!attempt.value && vnpayEnabled.value && ['vnpay', 'demo'].includes(route.query.payment)) method.value = VNPAY
  loading.value = false
  await refresh()
})
onBeforeUnmount(() => { disposed = true; version++; clearTimeout(timer) })
useLiveRefresh(refresh)
</script>
<template><div class="container shop-page"><header class="shop-heading"><span class="eyebrow">THE NEXT STEP</span><h1>Thanh toán</h1><p>Kiểm tra thông tin để đơn hàng đến đúng nơi.</p></header><p v-if="loading" class="shop-card" role="status">Đang chuẩn bị đơn hàng…</p><section v-else-if="!cart.selectedItems.length && !attempt" class="shop-card shop-empty"><h2>Bạn chưa chọn sản phẩm để thanh toán.</h2><RouterLink to="/gio-hang" class="button button-primary">Chọn sản phẩm trong giỏ</RouterLink></section><form v-else class="shop-columns" @submit.prevent="placeOrder"><div class="checkout-main"><p v-if="profileError" class="shop-alert" role="alert">{{ profileError }} <button type="button" class="text-button" :disabled="profileLoading" @click="loadProfile()">Tải lại hồ sơ</button></p><p v-if="error" class="shop-alert" role="alert">{{ error }}</p><p v-if="notice" class="shop-alert shop-alert--info" role="status">{{ notice }}</p><section class="shop-card"><h2>Chọn địa chỉ đã lưu</h2><p v-if="!addresses.length" class="shop-muted">Bạn chưa có địa chỉ đã lưu. Nhập thông tin bên dưới hoặc <RouterLink to="/tai-khoan">thêm vào sổ địa chỉ</RouterLink>.</p><label v-for="a in addresses" :key="a.id" class="address-option" :class="{ selected: selectedAddress === a.id }"><input type="radio" name="address" :checked="selectedAddress === a.id" :disabled="addressLocked" @change="chooseAddress(a)"><span><strong>{{ a.receiverName }} · {{ a.receiverPhone }}</strong> <small v-if="a.isDefault" class="shop-tag">Mặc định</small><small>{{ [a.street, a.ward, a.district, a.province].filter(Boolean).join(', ') }}</small></span></label><button class="text-button" type="button" :disabled="addressLocked" @click="chooseAddress(null)">+ Sử dụng địa chỉ khác</button></section><section class="shop-card"><h2>Thông tin người nhận</h2><fieldset :disabled="addressLocked"><CheckoutReceiverForm v-model="receiver" @busy="addressResolving = $event" /></fieldset><div v-if="!testPayment" class="checkout-address-save"><label class="address-option"><input v-model="saveAddress" type="checkbox" :disabled="addressLocked"><span>Lưu địa chỉ này vào sổ địa chỉ khi đặt hàng thành công</span></label><label v-if="saveAddress" class="address-option"><input v-model="makeDefaultAddress" type="checkbox" :disabled="addressLocked || !addresses.length"><span>Đặt làm địa chỉ mặc định</span></label><button class="button button-outline" type="button" :disabled="addressLocked || addressResolving" @click="saveAddressNow">{{ addressSaving ? 'Đang lưu…' : 'Lưu địa chỉ ngay' }}</button></div><p v-else class="shop-muted">Địa chỉ được dùng cho đơn hàng này.</p></section><section class="shop-card"><h2>Phương thức thanh toán</h2><label v-for="m in paymentMethods" :key="m.id" class="address-option" :class="{ selected: method === m.id }"><input v-model="method" type="radio" :value="m.id" :disabled="!m.supported || submitting || !!attempt"><span><strong>{{ m.code === 'CHUYEN_KHOAN' ? 'Chuyển khoản ngân hàng bằng QR' : m.name }}</strong><small>{{ m.message }}</small></span></label><label v-if="vnpayEnabled" class="address-option demo-payment-choice" :class="{ selected: method === VNPAY }"><input v-model="method" type="radio" :value="VNPAY" :disabled="submitting || !!attempt"><span><strong>Thanh toán bằng VNPAY</strong></span></label><p v-if="quote && !paymentMethods.length && !vnpayEnabled" class="shop-error">Chưa có phương thức thanh toán đang hoạt động.</p></section><section class="shop-card"><label class="shop-label">Ghi chú giao hàng<textarea v-model="note" maxlength="900" rows="3" :disabled="addressLocked" placeholder="Ghi chú cho cửa hàng (không bắt buộc)"></textarea></label></section></div><aside class="shop-card shop-summary"><h2>Đơn hàng <span class="shop-muted">({{ checkoutCount }} sản phẩm)</span></h2><div v-for="line in checkoutLines" :key="line.variantId" class="summary-product"><ProductImage :src="line.variant?.image" :alt="line.variant?.name || 'Sản phẩm'" /><div><strong>{{ line.variant?.name || 'Đang tải sản phẩm…' }}</strong><small>{{ line.variant?.color }} · {{ line.variant?.size }} × {{ line.quantity }}</small></div><span>{{ line.variant ? money(Number(line.variant.giaSauGiam) * line.quantity) : 'Đang tải…' }}</span></div><VoucherSelector v-model="voucherCode" :subtotal="Number(quote?.subtotal ?? cart.selectedSubtotal)" :disabled="addressLocked" /><dl><dt>Tạm tính</dt><dd>{{ money(quote?.subtotal ?? cart.selectedSubtotal) }}</dd><dt>Giảm voucher</dt><dd>−{{ money(quote?.discount || 0) }}</dd><dt>Phí vận chuyển GHN</dt><dd>{{ quote?.shipping == null ? 'Chưa tính được' : money(quote.shipping) }}</dd><dt class="summary-total">Tổng cộng</dt><dd class="summary-total">{{ money(attempt?.expectedTotal ?? quote?.total) }}</dd></dl><p v-if="quote?.message" class="shop-alert" role="status">{{ quote.message }}</p><button v-if="!attempt" type="button" class="text-button" :disabled="submitting || updating || addressResolving" @click="refresh">Cập nhật giá và phí giao hàng</button><p v-if="updating" class="shop-muted" role="status">Đang kiểm tra giá, ưu đãi và phí vận chuyển…</p><button type="submit" class="button button-primary" :disabled="submitting || updating || addressSaving || (!attempt && addressResolving) || (!attempt && (!quote?.canCheckout || cart.selectedInvalid))">{{ submitting ? 'Đang xử lý…' : attempt ? 'Kiểm tra / thử lại phiên đặt hàng' : method === VNPAY ? 'Thanh toán bằng VNPAY' : method === DEMO ? 'Tiếp tục thanh toán thử →' : (quote?.methods?.find(m => m.id === method)?.code === 'CHUYEN_KHOAN' ? 'Đặt hàng và lấy mã QR' : 'Đặt hàng') }}</button><RouterLink to="/gio-hang" class="shop-continue">Quay lại giỏ hàng</RouterLink></aside></form></div></template>

<style scoped>
.demo-payment-choice{background:#fffbef;border-color:#e5cf9a}.demo-payment-choice strong{color:#806022}.demo-payment-choice.selected{background:#fff8e6;border-color:#b08a38}
</style>
