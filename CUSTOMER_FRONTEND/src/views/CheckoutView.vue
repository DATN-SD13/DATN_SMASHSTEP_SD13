<script setup>
import { ref, computed, onMounted, watch, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { useAuthStore } from '../stores/auth'
import { accountService } from '../services/accountService'
import { commerceService, commerceError } from '../services/commerceService'
import { useLiveRefresh } from '../composables/useLiveRefresh'
import { useToast } from '../composables/useToast'
import { nameError, phoneError } from '../utils/accountValidation'
import { money } from '../utils/money'
import CheckoutSummary from '../components/CheckoutSummary.vue'
import OrderConfirmationDialog from '../components/OrderConfirmationDialog.vue'
import CheckoutReceiverForm from '../components/CheckoutReceiverForm.vue'
import VoucherSelector from '../components/VoucherSelector.vue'
import PaymentMethodSelector from '../components/PaymentMethodSelector.vue'
const attempt = ref(null), auth = useAuthStore(), cart = useCartStore(), router = useRouter(), route = useRoute()
const checkoutOwner = cart.owner
const pendingKey = 'ss_checkout_pending_' + auth.user?.id
const profile = ref(null), quote = ref(null), loading = ref(true), updating = ref(false), submitting = ref(false)
const error = ref(''), notice = ref(''), profileError = ref(''), profileLoading = ref(false)
const confirmation = ref(null)
const toast = useToast(), selectedVoucher = ref('')
const selectedAddress = ref(null), method = ref(null), note = ref('')
const voucherCode = computed(() => attempt.value?.voucherCode || selectedVoucher.value || '')
const saveAddress = ref(false), makeDefaultAddress = ref(false), addressSaving = ref(false), addressResolving = ref(false)
const receiver = ref({ name: '', phone: '', email: '', province: '', district: '', ward: '', street: '', provinceId: null, districtId: null, wardCode: '' })
let requestId = crypto.randomUUID(), timer, version = 0, disposed = false
watch(() => cart.owner, next => {
  if (next !== checkoutOwner) { version++; clearTimeout(timer); confirmation.value = null; attempt.value = null; loading.value = true; quote.value = null; router.replace('/gio-hang') }
}, { flush: 'sync' })
const vnpayEnabled = ref(false), VNPAY = 'VNPAY_SANDBOX'
// Manual bank QR is temporarily hidden until automatic confirmation is ready.
const selectableMethod = method => method.supported && method.code !== 'CHUYEN_KHOAN'
const paymentMethods = computed(() => (quote.value?.methods || []).filter(selectableMethod))
const checkoutItems = computed(() => attempt.value?.items || cart.selectedItems)
const checkoutLines = computed(() => checkoutItems.value.map(i => {
  const quoted = quote.value?.lines?.find(l => l.variant.variantId === i.variantId && l.quantity === i.quantity)
  return { ...i, variant: quoted?.variant || cart.lines.find(l => l.variantId === i.variantId)?.variant }
}))
const addresses = computed(() => profile.value?.addresses || [])
const addressLocked = computed(() => submitting.value || !!attempt.value || addressSaving.value || !!confirmation.value)
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
  const point = r.point; delete r.point
  return { items: cart.selectedItems.map(i => ({ ...i })), voucherCode: voucherCode.value || null, paymentMethodId: method.value === VNPAY ? null : method.value,
    receiveOrderEmails: true, receiver: { ...r, email: null }, deliveryPoint: point ? { ...point } : null, note: note.value.trim(), saveAddress: saveAddress.value, makeDefaultAddress: saveAddress.value && makeDefaultAddress.value }
}
function receiverError() {
  const r = receiver.value
  return nameError(r.name) || phoneError(r.phone)
    || (!r.street.trim() || !r.province || !r.ward ? 'Vui lòng nhập đầy đủ địa chỉ nhận hàng.' : '')
    || (!r.provinceId || !r.wardCode ? 'Vui lòng chọn tỉnh / thành phố và phường / xã để tính phí giao hàng.' : '')
    || ([r.street,r.ward,r.province].filter(Boolean).join(', ').length > 500 ? 'Địa chỉ giao hàng đầy đủ tối đa 500 ký tự.' : '')
}
async function refresh(feedback = false) {
  if (!cart.selectedItems.length || submitting.value || attempt.value || confirmation.value || disposed || cart.owner !== checkoutOwner) { if (!submitting.value) { updating.value = false; if (!cart.selectedItems.length && !attempt.value) quote.value = null } return }
  const current = ++version; updating.value = true
  try {
    await cart.refresh()
    if (disposed || current !== version || cart.owner !== checkoutOwner || !cart.selectedItems.length) return
    const body = { ...payload(), voucherCode: selectedVoucher.value }, invalidReceiver = receiverError(); if (invalidReceiver) body.receiver = null
    const next = await (method.value === VNPAY ? commerceService.vnpayQuote(body) : commerceService.quote(body))
    if (disposed || cart.owner !== checkoutOwner || current !== version) return
    if (quote.value?.total != null && next.total != null && quote.value.total !== next.total)
      notice.value = 'Giá hoặc phí giao hàng vừa được cập nhật. Vui lòng kiểm tra tổng trước khi đặt hàng.'
    quote.value = invalidReceiver ? { ...next, message: invalidReceiver } : next; error.value = ''
    if (method.value !== VNPAY && !next.methods.some(m => selectableMethod(m) && m.id === method.value))
      method.value = next.methods.find(selectableMethod)?.id || null
    if (feedback === true) toast.success(next.voucherCode ? 'Đã áp dụng phiếu ' + next.voucherCode + ', giảm ' + money(next.discount) + '.' : 'Đã cập nhật đơn hàng. Không sử dụng phiếu giảm giá.')
  } catch (e) { if (current === version) { error.value = commerceError(e); quote.value = null; if (feedback === true) toast.error(error.value) } }
  finally { if (current === version) updating.value = false }
}
async function chooseVoucher(code) {
  if (addressLocked.value || updating.value) return
  clearTimeout(timer); version++; requestId = crypto.randomUUID(); selectedVoucher.value = typeof code === 'string' ? code.trim() : ''
  notice.value = ''; await refresh(true)
}
watch([receiver, method, note, saveAddress, makeDefaultAddress, () => cart.selectedItems], () => {
  if (!loading.value && !submitting.value && !attempt.value) {
    clearTimeout(timer); version++; updating.value = true; timer = setTimeout(refresh, 500); requestId = crypto.randomUUID()
  }
}, { deep: true, flush: 'sync' })
async function saveAddressNow() {
  if (disposed || cart.owner !== checkoutOwner) return
  if (addressLocked.value || addressResolving.value) return
  const invalid = receiverError(); if (invalid) { error.value = invalid; toast.error(invalid); return }
  addressSaving.value = true; error.value = ''
  try {
    const r = receiver.value
    const saved = await accountService.createAddress({ receiverName: r.name.trim(), receiverPhone: r.phone.trim(),
      province: r.province, district: '', ward: r.ward, street: r.street.trim(), addressType: 1, isDefault: makeDefaultAddress.value })
    if (disposed || cart.owner !== checkoutOwner) return
    profile.value = { ...(profile.value || auth.user), addresses: [saved, ...addresses.value.filter(a => a.id !== saved.id)] }
    selectedAddress.value = saved.id; saveAddress.value = false
    notice.value = 'Đã lưu địa chỉ vào tài khoản. Lần mua sau sẽ tự điền địa chỉ này.'
    toast.success(notice.value)
    try { const data = await accountService.profile(); if (!disposed && cart.owner === checkoutOwner) profile.value = data }
    catch (e) { if (!disposed && cart.owner === checkoutOwner) profileError.value = 'Địa chỉ đã được lưu; chưa tải lại được hồ sơ. ' + commerceError(e) }
  } catch (e) { error.value = commerceError(e); toast.error(error.value) }
  finally { addressSaving.value = false }
}
async function placeOrder() {
  if (disposed || cart.owner !== checkoutOwner) return
  if (submitting.value || updating.value || addressSaving.value || (!attempt.value && addressResolving.value)) return
  const invalid = attempt.value ? '' : receiverError(); if (invalid) { error.value = invalid; toast.error(invalid); return }
  if (!attempt.value && (!quote.value?.canCheckout || cart.selectedInvalid)) return
  if (!attempt.value && method.value !== VNPAY && !paymentMethods.value.some(m => m.id === method.value)) {
    error.value = 'Vui lòng chọn phương thức thanh toán đang hoạt động.'; toast.error(error.value); return
  }
  submitting.value = true; error.value = ''
  try {
    if (!attempt.value) { attempt.value = { ...payload(), requestId, expectedTotal: quote.value.total, vnpay: method.value === VNPAY }; rememberAttempt() }
    const sent = attempt.value
    const { demo: _legacyDemo, vnpay, ...body } = sent
    const order = await (vnpay ? commerceService.vnpayCreate(body) : commerceService.checkout(body))
    if (vnpay && order.paymentUrl && new URL(order.paymentUrl).origin !== 'https://sandbox.vnpayment.vn') throw new Error('Địa chỉ VNPAY Sandbox không hợp lệ')
    if (!vnpay) cart.purchased(sent.items, checkoutOwner)
    attempt.value = null; rememberAttempt()
    if (disposed || cart.owner !== checkoutOwner) return
    if (vnpay) {
      if (order.paymentUrl) window.location.assign(order.paymentUrl)
      else await router.push('/thanh-toan-vnpay/phien/' + order.code)
    } else { toast.success('Đặt hàng thành công. Mã đơn hàng: ' + order.code); await router.push('/don-hang/' + order.code) }
  } catch (e) {
    if (disposed || cart.owner !== checkoutOwner) return
    error.value = commerceError(e)
    toast.error(error.value)
    if (e.response && e.response.status < 500) { attempt.value = null; rememberAttempt(); submitting.value = false; requestId = crypto.randomUUID(); await refresh(); notice.value = commerceError(e) }
    else notice.value = 'Chưa xác định được kết quả đặt hàng. Bấm thử lại cùng phiên hoặc xem Đơn hàng của bạn trước khi đặt đơn khác.'
  } finally { submitting.value = false }
}
function orderFingerprint() {
  return JSON.stringify({ ...payload(), expectedTotal: quote.value?.total, vnpay: method.value === VNPAY })
}
function reviewOrder() {
  if (disposed || cart.owner !== checkoutOwner) return
  if (submitting.value || updating.value || addressSaving.value || addressResolving.value || confirmation.value) return
  if (attempt.value) { placeOrder(); return }
  const invalid = receiverError() || (!quote.value?.canCheckout || cart.selectedInvalid ? quote.value?.message || 'Vui lòng cập nhật đơn hàng trước khi xác nhận.' : '')
    || (method.value !== VNPAY && !paymentMethods.value.some(m => m.id === method.value) ? 'Vui lòng chọn phương thức thanh toán đang hoạt động.' : '')
  if (invalid) { error.value = invalid; toast.error(invalid); return }
  confirmation.value = {
    fingerprint: orderFingerprint(), receiverName: receiver.value.name.trim(), phone: receiver.value.phone.trim(),
    address: [receiver.value.street.trim(), receiver.value.ward, receiver.value.district, receiver.value.province].filter(Boolean).join(', '),
    lines: checkoutLines.value.map(line => ({ variantId: line.variantId, name: line.variant.name, color: line.variant.color, size: line.variant.size, quantity: line.quantity, total: Number(line.variant.giaSauGiam) * line.quantity })),
    subtotal: Number(quote.value.subtotal), discount: Number(quote.value.discount || 0), shipping: Number(quote.value.shipping), total: Number(quote.value.total),
    voucherCode: quote.value.voucherCode || null,
    vnpay: method.value === VNPAY, paymentLabel: method.value === VNPAY ? 'VNPAY' : paymentMethods.value.find(m => m.id === method.value)?.name,
    email: profile.value?.email || auth.user?.email
  }
}
async function confirmOrder() {
  if (!confirmation.value || submitting.value || disposed || cart.owner !== checkoutOwner) return
  const unchanged = confirmation.value.fingerprint === orderFingerprint()
  confirmation.value = null
  if (!unchanged) { error.value = 'Đơn hàng vừa thay đổi. Vui lòng kiểm tra và xác nhận lại.'; toast.error(error.value); await refresh(); return }
  await placeOrder()
}
onMounted(async () => {
  try {
    const saved = JSON.parse(sessionStorage.getItem(pendingKey) || 'null')
    if (saved?.demo === true) sessionStorage.removeItem(pendingKey)
    else if (saved?.requestId && Array.isArray(saved.items) && saved.receiver) {
      attempt.value = saved; receiver.value = { ...saved.receiver, point: saved.deliveryPoint || null }; method.value = saved.vnpay ? VNPAY : saved.paymentMethodId; note.value = saved.note || ''
      notice.value = 'Có phiên đặt hàng chưa xác định kết quả. Bấm thử lại cùng phiên để tránh tạo đơn trùng.'
    }
  } catch { try { sessionStorage.removeItem(pendingKey) } catch { /* Storage disabled. */ } }
  await loadProfile(true)
  const [gatewayConfig] = await Promise.allSettled([commerceService.vnpayConfig()])
  vnpayEnabled.value = gatewayConfig.status === 'fulfilled' && gatewayConfig.value.enabled === true
  if (disposed || cart.owner !== checkoutOwner) return
  if (!attempt.value && vnpayEnabled.value && route.query.payment === 'vnpay') method.value = VNPAY
  if (!attempt.value && !vnpayEnabled.value && route.query.payment === 'vnpay')
    notice.value = 'VNPAY đang tạm ngưng. Bạn có thể chọn phương thức thanh toán khác.'
  loading.value = false
  await refresh()
})
onBeforeUnmount(() => { disposed = true; version++; clearTimeout(timer) })
useLiveRefresh(refresh)
</script>
<template><div class="container shop-page"><header class="shop-heading"><span class="eyebrow">THE NEXT STEP</span><h1>Thanh toán</h1><p>Kiểm tra thông tin để đơn hàng đến đúng nơi.</p></header><p v-if="loading" class="shop-card" role="status">Đang chuẩn bị đơn hàng…</p><section v-else-if="!cart.selectedItems.length && !attempt" class="shop-card shop-empty"><h2>Bạn chưa chọn sản phẩm để thanh toán.</h2><RouterLink to="/gio-hang" class="button button-primary">Chọn sản phẩm trong giỏ</RouterLink></section><form v-else class="shop-columns" @submit.prevent="reviewOrder"><div class="checkout-main"><p v-if="profileError" class="shop-alert" role="alert">{{ profileError }} <button type="button" class="text-button" :disabled="profileLoading" @click="loadProfile()">Tải lại hồ sơ</button></p><p v-if="error" class="shop-alert" role="alert">{{ error }}</p><p v-if="notice" class="shop-alert shop-alert--info" role="status">{{ notice }}</p><section class="shop-card"><h2>Chọn địa chỉ đã lưu</h2><p v-if="!addresses.length" class="shop-muted">Bạn chưa có địa chỉ đã lưu. Nhập thông tin bên dưới hoặc <RouterLink to="/tai-khoan">thêm vào sổ địa chỉ</RouterLink>.</p><label v-for="a in addresses" :key="a.id" class="address-option" :class="{ selected: selectedAddress === a.id }"><input type="radio" name="address" :checked="selectedAddress === a.id" :disabled="addressLocked" @change="chooseAddress(a)"><span><strong>{{ a.receiverName }} · {{ a.receiverPhone }}</strong> <small v-if="a.isDefault" class="shop-tag">Mặc định</small><small>{{ [a.street, a.ward, a.district, a.province].filter(Boolean).join(', ') }}</small></span></label><button class="text-button" type="button" :disabled="addressLocked" @click="chooseAddress(null)">+ Sử dụng địa chỉ khác</button></section><section class="shop-card"><h2>Thông tin người nhận</h2><p class="shop-muted checkout-email-note">Email xác nhận và cập nhật đơn hàng sẽ gửi đến <strong>{{ profile?.email || auth.user?.email || 'email tài khoản của bạn' }}</strong>.</p><fieldset :disabled="addressLocked"><CheckoutReceiverForm v-model="receiver" :disabled="addressLocked" @busy="addressResolving = $event" /></fieldset><div class="checkout-address-save"><label class="address-option"><input v-model="saveAddress" type="checkbox" :disabled="addressLocked"><span>Lưu địa chỉ này vào sổ địa chỉ khi đặt hàng thành công</span></label><label v-if="saveAddress" class="address-option"><input v-model="makeDefaultAddress" type="checkbox" :disabled="addressLocked || !addresses.length"><span>Đặt làm địa chỉ mặc định</span></label><button class="button button-outline" type="button" :disabled="addressLocked || addressResolving" @click="saveAddressNow">{{ addressSaving ? 'Đang lưu…' : 'Lưu địa chỉ ngay' }}</button></div></section><PaymentMethodSelector v-model="method" :methods="paymentMethods" :vnpay-enabled="vnpayEnabled" :vnpay-value="VNPAY" :disabled="addressLocked" /><section class="shop-card"><label class="shop-label">Ghi chú giao hàng<textarea v-model="note" maxlength="900" rows="3" :disabled="addressLocked" placeholder="Ghi chú cho cửa hàng (không bắt buộc)"></textarea></label></section></div><CheckoutSummary :lines="checkoutLines" :subtotal="quote?.subtotal ?? cart.selectedSubtotal" :discount="quote?.discount || 0" :shipping="quote?.shipping" :total="attempt?.expectedTotal ?? quote?.total" :message="quote?.message" :submitting="submitting" :refreshing="updating" :pending="!!attempt" :vnpay="method === VNPAY" :refresh-disabled="addressLocked || updating || addressResolving" :confirm-disabled="!!confirmation || submitting || updating || addressSaving || (!attempt && addressResolving) || (!attempt && (!quote?.canCheckout || cart.selectedInvalid))" @refresh="refresh(true)"><template #voucher><VoucherSelector :model-value="voucherCode" :applied-code="quote?.voucherCode || ''" :discount="Number(quote?.discount || 0)" :quote-pending="updating" :subtotal="Number(quote?.subtotal ?? cart.selectedSubtotal)" :disabled="addressLocked || updating" @select="chooseVoucher" /></template></CheckoutSummary></form><OrderConfirmationDialog v-if="confirmation" :summary="confirmation" @close="confirmation = null" @confirm="confirmOrder" /></div></template>

<style scoped>
.checkout-email-note{font-size:12px;line-height:1.7;margin:12px 0 20px}


</style>
