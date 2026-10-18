<script setup>
import { ref, computed, onMounted, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { useAuthStore } from '../stores/auth'
import { accountService } from '../services/accountService'
import { commerceService, commerceError } from '../services/commerceService'
import { useLiveRefresh } from '../composables/useLiveRefresh'
import { nameError, phoneError, emailError } from '../utils/accountValidation'
import { money } from '../utils/money'
import ProductImage from '../components/ProductImage.vue'
import CheckoutReceiverForm from '../components/CheckoutReceiverForm.vue'
import VoucherSelector from '../components/VoucherSelector.vue'
const attempt = ref(null), auth = useAuthStore()
const pendingKey = 'ss_checkout_pending_' + auth.user?.id
function rememberAttempt() { if (attempt.value) sessionStorage.setItem(pendingKey, JSON.stringify(attempt.value)); else sessionStorage.removeItem(pendingKey) }
const cart = useCartStore(), router = useRouter(), profile = ref(null), quote = ref(null), loading = ref(true), updating = ref(false), submitting = ref(false), error = ref(''), notice = ref('')
const selectedAddress = ref(null), voucherCode = ref(''), method = ref(null), note = ref(''), receiver = ref({ name: '', phone: '', email: '', province: '', district: '', ward: '', street: '', provinceId: null, districtId: null, wardCode: '' })
let requestId = crypto.randomUUID(), timer, version = 0, disposed = false
const addresses = computed(() => profile.value?.addresses || [])
function chooseAddress(a) {
  selectedAddress.value = a?.id || null
  receiver.value = { name: a?.receiverName || profile.value?.name || '', phone: a?.receiverPhone || profile.value?.phone || '', email: profile.value?.email || '', province: a?.province || '', district: a?.district || '', ward: a?.ward || '', street: a?.street || '', provinceId: null, districtId: null, wardCode: '' }
}
function payload() { return { items: cart.items.map(i => ({ ...i })), voucherCode: voucherCode.value || null, paymentMethodId: method.value, receiver: { ...receiver.value }, note: note.value.trim() } }
function receiverError() { const r = receiver.value; return nameError(r.name) || phoneError(r.phone) || (r.email ? emailError(r.email) : '') || (!r.street.trim() || !r.province || !r.ward ? 'Vui lòng nhập đầy đủ địa chỉ nhận hàng.' : '') }
async function refresh() {
  if (!cart.items.length || submitting.value || attempt.value || disposed) return
  const current = ++version; updating.value = true
  try {
    await cart.refresh()
    // Receiver validation happens once the customer has completed the fields.
    const body = payload(); if (receiverError()) body.receiver = null
    const next = await commerceService.quote(body)
    if (disposed || current !== version) return
    if (quote.value?.total != null && next.total != null && quote.value.total !== next.total) notice.value = 'Giá hoặc phí giao hàng vừa được cập nhật. Vui lòng kiểm tra tổng trước khi đặt hàng.'
    quote.value = next; error.value = ''
    if (method.value == null) method.value = next.methods.find(m => m.supported)?.id || null
  } catch (e) { if (current === version) { error.value = commerceError(e); quote.value = null } }
  finally { if (current === version) updating.value = false }
}
watch([receiver, voucherCode, method, note, () => cart.items], () => { if (!loading.value && !submitting.value && !attempt.value) { clearTimeout(timer); version++; timer = setTimeout(refresh, 500); requestId = crypto.randomUUID() } }, { deep: true })
async function placeOrder() {
  if (submitting.value || updating.value) return
  const invalid = attempt.value ? '' : receiverError(); if (invalid) { error.value = invalid; return }
  if (!attempt.value && (!quote.value?.canCheckout || cart.invalid)) return
  submitting.value = true; error.value = ''
  try {
    if (!attempt.value) { attempt.value = { ...payload(), requestId, expectedTotal: quote.value.total }; rememberAttempt() }
    const order = await commerceService.checkout(attempt.value)
    cart.purchased(attempt.value.items); attempt.value = null; rememberAttempt(); await router.push('/don-hang/' + order.code)
  } catch (e) {
    error.value = commerceError(e)
    if (e.response && e.response.status < 500) { attempt.value = null; rememberAttempt(); submitting.value = false; requestId = crypto.randomUUID(); await refresh(); notice.value = commerceError(e) }
    else notice.value = 'Chưa xác định được kết quả đặt hàng. Giữ nguyên phiên này và bấm thử lại; hoặc xem Đơn hàng của bạn trước khi đặt đơn khác.'
  } finally { submitting.value = false }
}
onMounted(async () => {
  try {
    const saved = JSON.parse(sessionStorage.getItem(pendingKey) || 'null')
    if (saved?.requestId && Array.isArray(saved.items)) { attempt.value = saved; receiver.value = saved.receiver; voucherCode.value = saved.voucherCode || ''; method.value = saved.paymentMethodId; note.value = saved.note || ''; notice.value = 'Có phiên đặt hàng chưa xác định kết quả. Bấm kiểm tra / thử lại cùng phiên để tránh tạo đơn trùng.' }
  } catch { sessionStorage.removeItem(pendingKey) }
  try { profile.value = await accountService.profile(); if (!attempt.value) chooseAddress(profile.value.defaultAddress || addresses.value[0]); await refresh() }
  catch (e) { error.value = commerceError(e) }
  finally { loading.value = false }
})
onBeforeUnmount(() => { disposed = true; version++; clearTimeout(timer) })
useLiveRefresh(refresh)
</script>
<template><div class="container shop-page"><header class="shop-heading"><span class="eyebrow">THE NEXT STEP</span><h1>Thanh toán</h1><p>Kiểm tra thông tin để đơn hàng đến đúng nơi.</p></header><p v-if="loading" class="shop-card" role="status">Đang chuẩn bị đơn hàng…</p><section v-else-if="!cart.items.length && !attempt" class="shop-card shop-empty"><h2>Giỏ hàng của bạn đang trống.</h2><RouterLink to="/san-pham" class="button button-primary">Chọn sản phẩm</RouterLink></section><form v-else class="shop-columns" @submit.prevent="placeOrder"><div class="checkout-main"><p v-if="error" class="shop-alert" role="alert">{{ error }}</p><p v-if="notice" class="shop-alert shop-alert--info" role="status">{{ notice }}</p><section class="shop-card"><h2>Chọn địa chỉ đã lưu</h2><p v-if="!addresses.length" class="shop-muted">Bạn chưa có địa chỉ đã lưu. Nhập thông tin bên dưới hoặc <RouterLink to="/tai-khoan">thêm vào sổ địa chỉ</RouterLink>.</p><label v-for="a in addresses" :key="a.id" class="address-option" :class="{ selected: selectedAddress === a.id }"><input type="radio" name="address" :checked="selectedAddress === a.id" :disabled="submitting || !!attempt" @change="chooseAddress(a)"><span><strong>{{ a.receiverName }} · {{ a.receiverPhone }}</strong> <small v-if="a.isDefault" class="shop-tag">Mặc định</small><small>{{ [a.street, a.ward, a.district, a.province].filter(Boolean).join(', ') }}</small></span></label><button class="text-button" type="button" :disabled="submitting || !!attempt" @click="chooseAddress(null)">+ Sử dụng địa chỉ khác</button></section><section class="shop-card"><h2>Thông tin người nhận</h2><fieldset :disabled="submitting || !!attempt"><CheckoutReceiverForm v-model="receiver" /></fieldset></section><section class="shop-card"><h2>Phương thức thanh toán</h2><label v-for="m in quote?.methods || []" :key="m.id" class="address-option" :class="{ selected: method === m.id }"><input v-model="method" type="radio" :value="m.id" :disabled="!m.supported || submitting || !!attempt"><span><strong>{{ m.name }}</strong><small>{{ m.message }}</small></span></label><p v-if="quote && !quote.methods.length" class="shop-error">Chưa có phương thức thanh toán đang hoạt động.</p></section><section class="shop-card"><label class="shop-label">Ghi chú giao hàng<textarea v-model="note" maxlength="900" rows="3" :disabled="submitting || !!attempt" placeholder="Ghi chú cho cửa hàng (không bắt buộc)"></textarea></label></section></div><aside class="shop-card shop-summary"><h2>Đơn hàng <span class="shop-muted">({{ cart.count }} sản phẩm)</span></h2><div v-for="line in cart.lines" :key="line.variantId" class="summary-product"><ProductImage :src="line.variant?.image" :alt="line.variant?.name || 'Sản phẩm'" /><div><strong>{{ line.variant?.name || 'Đang tải sản phẩm…' }}</strong><small>{{ line.variant?.color }} · {{ line.variant?.size }} × {{ line.quantity }}</small></div><span>{{ money(Number(line.variant?.giaSauGiam || 0) * line.quantity) }}</span></div><VoucherSelector v-model="voucherCode" :subtotal="Number(quote?.subtotal ?? cart.subtotal)" :disabled="submitting || !!attempt" /><dl><dt>Tạm tính</dt><dd>{{ money(quote?.subtotal ?? cart.subtotal) }}</dd><dt>Giảm voucher</dt><dd>−{{ money(quote?.discount || 0) }}</dd><dt>Phí vận chuyển GHN</dt><dd>{{ quote?.shipping == null ? 'Chưa tính được' : money(quote.shipping) }}</dd><dt class="summary-total">Tổng cộng</dt><dd class="summary-total">{{ money(attempt?.expectedTotal ?? quote?.total) }}</dd></dl><p v-if="quote?.message" class="shop-alert" role="status">{{ quote.message }}</p><p v-if="updating" class="shop-muted" role="status">Đang kiểm tra giá, ưu đãi và phí vận chuyển…</p><button type="submit" class="button button-primary" :disabled="submitting || updating || (!attempt && (!quote?.canCheckout || cart.invalid))">{{ submitting ? 'Đang đặt hàng…' : attempt ? 'Kiểm tra / thử lại phiên đặt hàng' : 'Đặt hàng' }}</button><RouterLink to="/gio-hang" class="shop-continue">Quay lại giỏ hàng</RouterLink></aside></form></div></template>
