<script setup>
import GhnLogo from '../../../../components/GhnLogo.vue'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { money } from '../../../../utils/paging'
import { confirmAction, showSuccess, showError } from '../../../../utils/feedback'
import salesService from '../services/salesService'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import api from '../../../../utils/api'
import customerService from '../../customer/services/customerService'
import { useAuthStore } from '../../../../stores/auth'
import { productAttributeService } from '../../product/services/productAttributeService'
import ProductThumbnail from '../../product/components/ProductThumbnail.vue'
import { productService } from '../../product/services/productService'
import { bankConfig, qrLink } from '../services/bankQr'
import { imageUrl } from '../../product/services/imageUtils'

const router = useRouter()
const auth = useAuthStore()
const MAX_ORDERS = 5
const catalog = ref([]), catalogPage = ref(0), catalogPages = ref(0)
const loading = ref(false), busy = ref(false), ready = ref(false), error = ref('')
const customers = ref([]), customerSearch = ref(''), employeeName = ref('')
const choosingCustomer = ref(false), customerResults = ref([]), customerLoading = ref(false), customerSearchError = ref('')
const employeeId = ref(null), methods = ref([]), voucherSupported = ref(false)
let quoteVersion = 0, quoteTimer
const orders = ref([]), activeId = ref(null)
const order = computed(() => orders.value.find(o => o.id === activeId.value) || null)
const deliveryLoading = ref(false), syncingStock = ref(false)
const checkingVoucher = ref(false), betterVoucherDialog = ref(null)
let stockSyncTimer, stockPollTimer, stockSyncPromise = null, stockSyncPending = false
const operationLocked = computed(() => auth.loggingOut || checkingVoucher.value || Boolean(betterVoucherDialog.value) || shippingLoading.value || deliveryLoading.value || Boolean(deliveryEditor.value) || busy.value || !ready.value || Boolean(checkoutDialog.value) || Boolean(pendingPayment.value) || recoveryBlocked.value)
const locked = computed(() => operationLocked.value || Boolean(bankAccepted.value))
const picking = ref(false), q = ref(''), colorFilter = ref(''), sizeFilter = ref('')
const categoryFilter = ref(''), brandFilter = ref(''), attributes = ref({})
const shown = computed(() => catalog.value)
let catalogVersion = 0, customerVersion = 0, alive = true
let searchTimer, customerTimer
const message = e => e.response?.data?.message || e.message || 'Không thể kết nối máy chủ. Hãy thử lại.'
const payload = o => ({ revision: o.revision, customerId: o.customerId || null, note: o.note || '', voucherCode: o.voucherCode || '', delivery: o.delivery,
  items: o.items.map(i => ({ variantId: i.id, quantity: i.qty, unitPrice: i.price })) })
const signature = o => JSON.stringify(payload(o))
const hydrate = (d, previous = {}) => {
  const o = { ...d, name: d.invoiceCode,
    requestId: previous.requestId || crypto.randomUUID(),
    shippingQuote: d.delivery?.address === previous.delivery?.address ? previous.shippingQuote : null,
    shippingDestination: d.delivery?.address === previous.delivery?.address ? previous.shippingDestination : null,
    voucherCode: d.voucherCode ?? previous.voucherCode ?? '', paid: previous.paid ?? '',
    autoCash: previous.autoCash ?? (previous.paid == null || previous.paid === ''),
    paymentMode: ['CASH', 'BANK'].includes(previous.paymentMode) ? previous.paymentMode : 'CASH',
    autoVoucher: previous.autoVoucher ?? !d.voucherCode,
    delivery: d.delivery ?? previous.delivery ?? { enabled: false, addressId: null, recipientName: '', recipientPhone: '', address: '', shippingFee: 0 },
    paymentMethodId: previous.paymentMethodId ?? methods.value[0]?.id ?? null,
    quote: null, quotedSignature: '', quoteError: '', items: d.items.map(i => ({ ...i,
    discounted: i.discounted === true,
    discountPercent: i.discountPercent ?? 0 })) }
  loadImages(o.items)
  o.saved = signature(o)
  return o
}
const dirty = computed(() => !!order.value && signature(order.value) !== order.value.saved)
const selectedCustomer = computed(() => customers.value.find(c => c.id === order.value?.customerId))
const count = computed(() => order.value?.items.reduce((s, i) => s + i.qty, 0) || 0)
const total = computed(() => order.value?.items.reduce((s, i) => s + i.qty * i.price, 0) || 0)
const cartWarnings = computed(() => (order.value?.items || []).filter(i => i.available === false
  || i.qty > i.stock || (i.currentPrice != null && Number(i.currentPrice) !== Number(i.price))))

const RECOVERY_KEY = 'smashstep.pos.pending-checkout.v2'
const checkoutDialog = ref(null), completedReceipt = ref(null), pendingPayment = ref(null), recoveryBlocked = ref(false)
const qrFailed = ref(false)
const addresses = ref([]), addressLoading = ref(false)
let addressVersion = 0
const method = code => methods.value.find(m => m.code === code)
const roundMoney = value => Math.round((Number(value) + Number.EPSILON) * 100) / 100
const paymentChoices = computed(() => [
  ...(method('TIEN_MAT') ? [{ value: 'CASH', name: 'Tiền mặt' }] : []),
  ...(method('CHUYEN_KHOAN') ? [{ value: 'BANK', name: 'Chuyển khoản' }] : [])
])
const cashDisplay = computed(() => {
  const raw = String(order.value?.paid ?? '')
  if (!raw) return ''
  const [whole, fraction] = raw.split('.')
  return whole.replace(/\B(?=(\d{3})+(?!\d))/g, '.') + (fraction == null ? '' : ',' + fraction)
})
function inputCash(event) {
  if (locked.value || !order.value) return
  const input = event.target, typed = input.value
  const cursor = input.selectionStart ?? typed.length
  const significantBefore = typed.slice(0, cursor).replace(/[.\s]/g, '').length
  const raw = typed.replace(/[.\s]/g, '').replace(',', '.')
  if (raw && !/^\d{1,16}(?:\.\d{0,2})?$/.test(raw)) { input.value = cashDisplay.value; return }
  order.value.autoCash = false
  order.value.paid = raw.replace(/^0+(?=\d)/, '')
  const formatted = cashDisplay.value
  input.value = formatted
  let caret = 0, seen = 0
  while (caret < formatted.length && seen < significantBefore) {
    if (formatted[caret] !== '.') seen++
    caret++
  }
  input.setSelectionRange?.(caret, caret)
}
const paidNum = computed(() => Number(order.value?.paid) || 0)
const quoteInput = computed(() => order.value ? {
  requestId: order.value.requestId, invoiceId: order.value.id, revision: order.value.revision,
  customerId: order.value.customerId || null,
  paymentMethodId: (order.value.paymentMode === 'BANK' ? method('CHUYEN_KHOAN') : method('TIEN_MAT'))?.id,
  voucherCode: order.value.voucherCode.trim(), delivery: order.value.delivery,
  items: order.value.items.map(i => ({ variantId: i.id, quantity: i.qty, unitPrice: i.price }))
} : null)
const quoteSignature = computed(() => JSON.stringify({ id: activeId.value, input: quoteInput.value }))
const quoteReady = computed(() => Boolean(order.value?.quote && order.value.quotedSignature === quoteSignature.value))
const payable = computed(() => quoteReady.value ? Number(order.value.quote.total)
  : roundMoney(total.value + (order.value?.delivery.enabled ? Number(order.value.delivery.shippingFee) || 0 : 0)))
const transferred = computed(() => order.value?.paymentMode === 'BANK' ? payable.value : 0)
const cashDue = computed(() => roundMoney(payable.value - transferred.value))
const change = computed(() => order.value?.paymentMode === 'BANK' ? 0 : Math.max(0, roundMoney(paidNum.value - cashDue.value)))
const cashError = computed(() => {
  if (!order.value || order.value.paymentMode === 'BANK' || order.value.paid === '') return ''
  if (!/^(?:0|[1-9]\d{0,15})(?:\.\d{1,2})?$/.test(String(order.value.paid))) return 'Số tiền không âm, tối đa 16 chữ số và 2 số lẻ.'
  return paidNum.value < cashDue.value ? 'Số tiền khách đưa chưa đủ.' : ''
})
// Suggest exact cash from a valid server quote; never overwrite a cashier's manual edit.
watch([order, quoteReady, () => order.value?.quote?.total, () => order.value?.paymentMode, () => order.value?.autoCash], () => {
  const current = order.value
  if (!current || current.paymentMode !== 'CASH' || !current.items.length || !current.autoCash || !quoteReady.value) return
  if (bankAccepted.value || checkoutDialog.value || pendingPayment.value || recoveryBlocked.value) return
  const amount = Number(current.quote.total)
  if (Number.isFinite(amount) && amount >= 0) current.paid = String(roundMoney(amount))
})
const canPay = computed(() => !operationLocked.value && !shippingLoading.value && shippingReady.value && quoteReady.value && count.value > 0 && deliveryReady.value && !cartWarnings.value.length
  && (order.value.paymentMode === 'BANK' ? !!method('CHUYEN_KHOAN') && payable.value > 0
    : !!method('TIEN_MAT') && order.value.paid !== '' && Number.isFinite(Number(order.value.paid)) && paidNum.value >= cashDue.value && /^(?:0|[1-9]\d{0,15})(?:\.\d{1,2})?$/.test(String(order.value.paid))))
const selectedVoucher = computed(() => validVouchers.value.find(v => v.code.toLowerCase() === order.value?.voucherCode?.toLowerCase()))
const voucherLabel = v => v?.value == null ? '' : Number(v.type) === 1 ? `${Number(v.value)}%` : money(v.value)
const bestSelected = computed(() => selectedVoucher.value && Number(selectedVoucher.value.discount) === Number(validVouchers.value[0]?.discount))
const validVouchers = computed(() => order.value?.quote?.vouchers || [])
const buyMoreSuggestions = computed(() => (order.value?.quote?.buyMoreSuggestions || []).filter(v =>
  Number(v.amountNeeded) > 0 && Number(v.amountNeeded) <= 500000))

async function calculateQuote(suggest = true) {
  const target = order.value, version = ++quoteVersion
  if (bankAccepted.value || auth.loggingOut || checkingVoucher.value || betterVoucherDialog.value || !target?.items.length || !quoteInput.value.paymentMethodId || busy.value || pendingPayment.value || checkoutDialog.value || recoveryBlocked.value) return
  if (target.delivery.enabled && !shippingReady.value) return
  const fingerprint = quoteSignature.value
  target.quoteError = ''
  try {
    const result = await salesService.tinhTien(quoteInput.value)
    if (!alive || version !== quoteVersion || order.value?.id !== target.id || fingerprint !== quoteSignature.value) return
    if (bankAccepted.value && Number(bankAccepted.value.total) !== Number(result.total)) bankAccepted.value = null
    target.quote = result; target.quotedSignature = fingerprint
    const best = result.vouchers?.[0]
    const nextCode = best?.code || ''
    // Reconcile automatically after cart/customer changes without a confirmation popup.
    if ((target.autoVoucher && target.voucherCode.toLowerCase() !== nextCode.toLowerCase())
        || result.selectedVoucherError) {
      if (result.selectedVoucherError) target.autoVoucher = true
      target.voucherCode = nextCode
      target.quoteError = ''
      if (await saveOrder(target)) await calculateQuote(false)
    }
  } catch (e) {
    if (alive && version === quoteVersion && order.value?.id === target.id && fingerprint === quoteSignature.value) {
      target.quote = null; target.quoteError = message(e)
      if (e.response?.status === 409 && !busy.value && !locked.value) await reviewChangedCart(target)
    }
  }
}
async function applyVoucher(code, target = order.value, automatic = false) {
  const id = target?.id
  if (stockSyncPromise) { await stockSyncPromise; target = orders.value.find(o => o.id === id) }
  if (!target || locked.value) return
  if (!automatic) target.autoVoucher = false
  target.voucherCode = code
  await saveOrder(target, automatic ? '' : code ? `Áp dụng phiếu giảm giá ${code} thành công.` : 'Bỏ phiếu giảm giá thành công.')
}
async function loadPaymentOptions() {
  try {
    methods.value = (await salesService.danhSachPhuongThucThanhToan()).filter(m => ['TIEN_MAT', 'CHUYEN_KHOAN'].includes(m.code))
    voucherSupported.value = true
    for (const o of orders.value) if (!paymentChoices.value.some(m => m.value === o.paymentMode)) o.paymentMode = paymentChoices.value[0]?.value || 'CASH'
  } catch (e) { error.value = message(e) }
}
watch([quoteSignature, busy], () => {
  if (bankAccepted.value || checkoutDialog.value || pendingPayment.value) return
  ++quoteVersion; clearTimeout(quoteTimer)
  if (order.value) { order.value.quote = null; order.value.quotedSignature = '' }
  if (!busy.value) quoteTimer = setTimeout(calculateQuote, 250)
})

const deliveryEditor = ref(null), deliveryError = ref(''), structuredAddress = ref(true)
const deliveryFields = ref({})
function deliveryFieldErrors() {
  const d = deliveryEditor.value || {}, a = addressForm.value, fields = {}
  const name = String(d.recipientName || '').trim(), phone = String(d.recipientPhone || '').trim()
  if (!name) fields.recipientName = 'Vui lòng nhập tên người nhận.'
  else if (name.length > 255) fields.recipientName = 'Tên người nhận tối đa 255 ký tự.'
  if (!phone) fields.recipientPhone = 'Vui lòng nhập số điện thoại người nhận.'
  else if (!/^\d+$/.test(phone)) fields.recipientPhone = 'Số điện thoại chỉ được chứa chữ số.'
  else if (phone.length !== 10) fields.recipientPhone = `Số điện thoại phải đủ 10 chữ số (bạn đang nhập ${phone.length} số).`
  else if (!phone.startsWith('0')) fields.recipientPhone = 'Số điện thoại phải bắt đầu bằng số 0.'
  if (!d.addressId) {
    if (!provinces.value.some(v => v.id === a.province)) fields.province = 'Vui lòng chọn tỉnh/thành phố.'
    if (!wards.value.some(v => v.id === a.ward)) fields.ward = 'Vui lòng chọn phường/xã thuộc tỉnh/thành phố đã chọn.'
    if (!a.street.trim()) fields.street = 'Vui lòng nhập số nhà, tên đường hoặc địa chỉ cụ thể.'
    else if (a.street.trim().length > 350) fields.street = 'Địa chỉ cụ thể tối đa 350 ký tự.'
  } else if (!d.address?.trim() || d.address.length > 500) fields.address = 'Địa chỉ đã chọn chưa hợp lệ. Chọn địa chỉ khác để bổ sung.'
  return fields
}
function validateDeliveryField(field) {
  const message = deliveryFieldErrors()[field]
  deliveryFields.value = { ...deliveryFields.value, [field]: message || '' }
}
function clearDeliveryField(field) {
  deliveryFields.value = { ...deliveryFields.value, [field]: '' }
  deliveryError.value = ''
}
const completeDelivery = d => !d?.enabled || Boolean(d.recipientName?.trim() && /^0\d{9}$/.test(d.recipientPhone)
  && d.address?.trim() && d.address.length <= 500 && d.shippingFee !== ''
  && Number.isFinite(Number(d.shippingFee)) && Number(d.shippingFee) >= 0)
const deliveryReady = computed(() => completeDelivery(order.value?.delivery))
const addressForm = ref({ province: '', ward: '', street: '' })
const provinces = ref([]), wards = ref([]), areasLoading = ref(false)
const shippingLoading = ref(false), shippingError = ref('')
let shippingVersion = 0, areaVersion = 0
const shippingKey = o => JSON.stringify([o?.customerId, o?.delivery?.address,
  o?.items.reduce((sum, i) => sum + i.qty, 0)])
const shippingReady = computed(() => !order.value?.delivery.enabled || Boolean(order.value.shippingQuote
  && order.value.shippingQuote.key === shippingKey(order.value)
  && Date.parse(order.value.shippingQuote.expiresAt) > Date.now()
  && Number(order.value.delivery.shippingFee) === Number(order.value.shippingQuote.fee)))
const addressName = value => String(value || '').trim().toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/\s+/g, ' ')
const addressMatches = (row, address) => address.split(',').some(part => [row.ten, ...(row.tenKhac || [])].some(name => addressName(name) === addressName(part)))
async function loadShippingAreas() {
  const version = ++areaVersion, address = deliveryEditor.value?.address || ''
  areasLoading.value = true
  try {
    const rows = await salesService.tinhThanhGhn()
    if (!alive || version !== areaVersion || !deliveryEditor.value) return
    provinces.value = rows
    const matches = rows.filter(row => addressMatches(row, address))
    if (matches.length !== 1) return
    const province = matches[0].id
    addressForm.value.province = province
    const wardRows = await salesService.phuongXaGhn(province)
    if (!alive || version !== areaVersion || !deliveryEditor.value || province !== addressForm.value.province) return
    wards.value = wardRows
    const wardMatches = wardRows.filter(row => addressMatches(row, address))
    if (wardMatches.length === 1) {
      addressForm.value.ward = wardMatches[0].id
      addressForm.value.street = address.split(',').filter(part => !addressMatches(matches[0], part) && !addressMatches(wardMatches[0], part)).map(part => part.trim()).join(', ')
    }
  } catch (e) { if (alive && version === areaVersion) deliveryError.value = message(e) }
  finally { if (version === areaVersion) areasLoading.value = false }
}
async function changeProvince() {
  const version = ++areaVersion, province = addressForm.value.province
  addressForm.value.ward = ''; wards.value = []; deliveryError.value = ''; clearDeliveryField('province'); clearDeliveryField('ward')
  areasLoading.value = true
  try {
    if (!province) return
    const rows = await salesService.phuongXaGhn(province)
    if (alive && version === areaVersion && deliveryEditor.value && province === addressForm.value.province) wards.value = rows
  } catch (e) { if (alive && version === areaVersion) deliveryError.value = message(e) }
  finally { if (version === areaVersion) areasLoading.value = false }
}
async function fetchShipping(target, destination = target.shippingDestination || {}) {
  const result = await salesService.tinhPhiGhn(target, destination)
  target.delivery.shippingFee = Number(result.phiVanChuyen)
  target.shippingQuote = { key: shippingKey(target), fee: Number(result.phiVanChuyen), expiresAt: result.hetHan }
  target.shippingDestination = result.khuVuc ? {
    province: result.khuVuc.idTinhThanh, ward: result.khuVuc.maPhuongXa,
    provinceName: result.khuVuc.tenTinhThanh, wardName: result.khuVuc.tenPhuongXa
  } : { ...destination }
  shippingError.value = ''

}
async function refreshShipping() {
  if (stockSyncPromise) await stockSyncPromise
  if (locked.value || !order.value?.delivery.enabled || !completeDelivery(order.value.delivery) || !order.value.items.length || shippingLoading.value) return
  const target = order.value, version = ++shippingVersion, key = shippingKey(target), previousError = error.value
  shippingLoading.value = true; shippingError.value = ''
  try {
    const copy = { ...target, delivery: { ...target.delivery } }
    await fetchShipping(copy)
    if (!alive || version !== shippingVersion || order.value?.id !== target.id || key !== shippingKey(order.value)) return
    Object.assign(order.value, { delivery: copy.delivery, shippingQuote: copy.shippingQuote, shippingDestination: copy.shippingDestination })
    if (error.value === previousError && previousError.includes('GHN')) error.value = ''
    await saveOrder(order.value)
  } catch (e) { if (alive && version === shippingVersion) { target.shippingQuote = null; shippingError.value = message(e) } }
  finally { if (version === shippingVersion) shippingLoading.value = false }
}
let deliveryOrderId
function openDelivery() {
  if (stockSyncPromise) { stockSyncPromise.then(() => { if (alive) openDelivery() }); return }
  if (locked.value || !order.value) return
  deliveryOrderId = order.value.id
  deliveryEditor.value = JSON.parse(JSON.stringify(order.value.delivery))
  deliveryError.value = ''; deliveryFields.value = {}; structuredAddress.value = true
  addressForm.value = { province: '', ward: '', street: '' }; wards.value = []; loadShippingAreas()
}
function closeDelivery() { if (!busy.value && !shippingLoading.value) { deliveryEditor.value = null; ++areaVersion; areasLoading.value = false } }
function selectDeliveryAddress(a) {
  deliveryFields.value = {}; deliveryError.value = ''
  Object.assign(deliveryEditor.value, { recipientName: a.receiverName, recipientPhone: a.receiverPhone, address: a.fullAddress })
  addressForm.value = { province: '', ward: '', street: '' }; wards.value = []; loadShippingAreas()
}
async function toggleDelivery(enabled) {
  if (stockSyncPromise) await stockSyncPromise
  if (locked.value || !order.value) return
  const target = order.value
  if (!enabled) { target.delivery.enabled = false; target.shippingQuote = null; shippingError.value = ''; await saveOrder(target, 'Đã chuyển sang nhận hàng tại quầy.'); return }
  const customer = selectedCustomer.value
  let defaultAddress
  deliveryLoading.value = true; error.value = ''
  try {
    if (customer?.code) {
      const result = await customerService.getAddresses(customer.code)
      if (!alive || order.value?.id !== target.id || order.value?.customerId !== target.customerId) return
      addresses.value = (result || []).filter(a => a.status === 1)
      defaultAddress = addresses.value.find(a => a.isDefault === true)
    }
    const delivery = { ...target.delivery, enabled: true }
    if (defaultAddress) Object.assign(delivery, { addressId: defaultAddress.id,
      recipientName: defaultAddress.receiverName, recipientPhone: defaultAddress.receiverPhone, address: defaultAddress.fullAddress })
    else if (!delivery.address?.trim()) Object.assign(delivery, { addressId: null,
      recipientName: customer?.name || '', recipientPhone: customer?.phone || '', address: '' })
    if (!alive || order.value?.id !== target.id) return
    // Incomplete delivery stays in this tab until the operator edits it or disables delivery.
    if (!completeDelivery(delivery)) { target.delivery = delivery; return }
    deliveryLoading.value = false
    target.delivery = delivery
    target.shippingDestination = null; target.shippingQuote = null
    await saveOrder(target, 'Bật giao hàng thành công. Đã điền thông tin người nhận và địa chỉ giao hàng.')
  } catch (e) { if (alive && order.value?.id === target.id) { error.value = message(e); showError(error.value, 'Không thể tải thông tin giao hàng') } }
  finally { deliveryLoading.value = false }
}
async function saveDelivery() {
  if (stockSyncPromise) await stockSyncPromise
  if (busy.value || shippingLoading.value || areasLoading.value || !deliveryEditor.value || order.value?.id !== deliveryOrderId) return
  deliveryFields.value = deliveryFieldErrors()
  deliveryError.value = ''
  if (Object.keys(deliveryFields.value).length) return
  const candidate = JSON.parse(JSON.stringify(deliveryEditor.value))
  candidate.recipientName = candidate.recipientName.trim()
  candidate.recipientPhone = candidate.recipientPhone.trim()
  if (!candidate.addressId && structuredAddress.value) {
    const a = addressForm.value
    candidate.address = [a.street.trim(), wards.value.find(v => v.id === a.ward)?.ten, provinces.value.find(v => v.id === a.province)?.ten].filter(Boolean).join(', ')
  }
  if (!candidate.address?.trim() || candidate.address.length > 500) {
    deliveryFields.value.street = 'Địa chỉ giao hàng đầy đủ tối đa 500 ký tự.'; return
  }
  // A new carrier quote is mandatory below; there is no editable shipping-price input.
  candidate.shippingFee = 0
  const target = { ...order.value, delivery: candidate, shippingQuote: null, shippingDestination: candidate.addressId ? {} : { ...addressForm.value } }
  if (await saveOrder(target, 'Cập nhật thông tin giao hàng thành công.')) deliveryEditor.value = null
  else deliveryError.value = error.value
}
const imageRequests = new Map(), parentImages = new Map(), imageSources = ref({})
const badgeItem = row => ({ dangGiamGia: row.discounted, phanTramGiamHienTai: row.discountPercent })
async function loadImages(rows) {
  await Promise.allSettled(rows.map(async row => {
    const key = `${row.id}:${row.productId || ''}`
    if (!imageRequests.has(key)) imageRequests.set(key, (async () => {
      let images = []
      try { images = (await api.get(`/product-details/${row.id}/images`)).data || [] } catch {}
      const selected = images.find(i => i.isAnhChinh) || images[0]
      if (selected?.urlAnh) return imageUrl(selected.urlAnh)
      if (!row.productId) return ''
      if (!parentImages.has(row.productId)) parentImages.set(row.productId, productService.get(row.productId).then(d => imageUrl(d.product?.anhChinh || d.images?.find(i => i.isAnhChinh)?.urlAnh || d.images?.[0]?.urlAnh)).catch(() => ''))
      return parentImages.get(row.productId)
    })())
    imageSources.value[row.id] = await imageRequests.get(key)
  }))
}
async function loadAddresses() {
  const version = ++addressVersion
  addresses.value = []
  const customer = selectedCustomer.value
  if (!customer?.code) return
  addressLoading.value = true
  try {
    const result = await customerService.getAddresses(customer.code)
    if (alive && version === addressVersion) addresses.value = (result || []).filter(a => a.status === 1)
  } catch (e) { if (version === addressVersion) error.value = message(e) }
  finally { if (version === addressVersion) addressLoading.value = false }
}
async function changeCustomer() {
  if (stockSyncPromise) await stockSyncPromise
  if (locked.value || !order.value) return
  order.value.voucherCode = ''; order.value.autoVoucher = true
  order.value.shippingDestination = null; order.value.shippingQuote = null
  order.value.delivery = { enabled: false, addressId: null, recipientName: '', recipientPhone: '', address: '', shippingFee: 0 }
  const customer = selectedCustomer.value
  return await saveOrder(order.value, customer ? `Chọn khách hàng ${customer.name || customer.code} thành công.` : 'Đã bỏ khách hàng, chuyển sang khách lẻ.')
}
async function useAddress(event) {
  if (stockSyncPromise) await stockSyncPromise
  if (locked.value || !order.value) return
  const selected = addresses.value.find(a => a.id === Number(event.target.value))
  if (!selected) { order.value.delivery.addressId = null; return }
  Object.assign(order.value.delivery, { addressId: selected.id, recipientName: selected.receiverName,
    recipientPhone: selected.receiverPhone, address: selected.fullAddress })
  await saveOrder(order.value, 'Cập nhật địa chỉ giao hàng thành công.')
}
watch(() => selectedCustomer.value?.id, loadAddresses)

function checkoutPayload() {
  const o = order.value, payments = []
  if (o.paymentMode !== 'BANK') payments.push({ methodId: method('TIEN_MAT').id, amount: cashDue.value })
  if (o.paymentMode !== 'CASH') payments.push({ methodId: method('CHUYEN_KHOAN').id, amount: transferred.value })
  return JSON.parse(JSON.stringify({ ...quoteInput.value, paymentVersion: 2, requestId: o.requestId, note: o.note || '',
    payments, paidAmount: o.paymentMode === 'BANK' ? 0 : paidNum.value,
    transferConfirmed: false }))
}
const bankAccepted = ref(null)
const paymentContext = () => JSON.stringify({ quote: quoteSignature.value, cart: order.value ? signature(order.value) : null,
  mode: order.value?.paymentMode, cash: order.value?.paid })
watch(paymentContext, () => { bankAccepted.value = null })
watch(() => [activeId.value, order.value?.delivery.enabled, order.value?.delivery.address, count.value].join('|'), () => {
  if (order.value?.delivery.enabled && deliveryReady.value && order.value.items.length && !shippingReady.value && !locked.value && !shippingLoading.value && !shippingError.value) refreshShipping()
})
const bankConfirmed = computed(() => bankAccepted.value?.context === paymentContext()
  && quoteReady.value && Number(bankAccepted.value?.total) === payable.value)
async function readyCheckout() {
  if (!canPay.value) return false
  const id = order.value.id
  if (dirty.value) {
    if (!(await saveOrder()) || order.value?.id !== id) return false
    await calculateQuote(false)
  }
  return canPay.value
}
async function createPaymentDialog() {
  if (stockSyncPromise) await stockSyncPromise
  if (!(await readyCheckout())) return
  qrFailed.value = false
  checkoutDialog.value = { payload: checkoutPayload(), invoiceCode: order.value.name,
    total: payable.value, change: change.value, bank: transferred.value, context: paymentContext(), confirmed: false,
    qr: qrLink(bankConfig, transferred.value, order.value.name) }
}
async function checkoutQuote() {
  clearTimeout(quoteTimer); ++quoteVersion
  const id = order.value?.id, fingerprint = quoteSignature.value
  const input = JSON.parse(JSON.stringify(quoteInput.value))
  const result = await salesService.tinhTien(input)
  if (!alive || order.value?.id !== id || fingerprint !== quoteSignature.value)
    throw new Error('Hóa đơn đã thay đổi. Kiểm tra lại rồi bấm Thanh toán.')
  if (bankAccepted.value && Number(bankAccepted.value.total) !== Number(result.total)) bankAccepted.value = null
  order.value.quote = result; order.value.quotedSignature = fingerprint
  if (result.selectedVoucherError) {
    order.value.autoVoucher = false
    order.value.quoteError = result.selectedVoucherError
    order.value.quotedSignature = ''
    throw new Error(`Phiếu đang chọn không còn dùng được: ${result.selectedVoucherError}. Vui lòng chọn lại phiếu trước khi thanh toán.`)
  }
  return result
}
async function continuePayment() {
  if (!canPay.value) return
  if (order.value.paymentMode === 'BANK' && bankConfirmed.value) {
    const accepted = bankAccepted.value
    if (!accepted.payload) { error.value = 'Thiếu dữ liệu khoản chuyển khoản đã xác nhận. Kiểm tra hóa đơn, không chuyển khoản thêm.'; return }
    const request = JSON.parse(JSON.stringify(accepted.payload)); request.transferConfirmed = true
    await performPayment({ employeeId: employeeId.value, invoiceCode: accepted.invoiceCode, payload: request })
    return
  }
  await createPaymentDialog()
}
async function openQr() {
  if (bankAccepted.value) return
  await pay()
}
async function pay() {
  if (bankAccepted.value) {
    if (bankConfirmed.value) await continuePayment()
    else error.value = 'Đã xác nhận nhận tiền nhưng hóa đơn cần kiểm tra lại. Không chuyển khoản thêm.'
    return
  }
  if (checkingVoucher.value || betterVoucherDialog.value) return
  if (stockSyncPromise) await stockSyncPromise
  if (!(await readyCheckout())) return
  if (checkingVoucher.value || betterVoucherDialog.value) return
  checkingVoucher.value = true
  try {
    const result = await checkoutQuote()
    const best = [...(result.vouchers || [])].sort((a,b) => Number(b.discount) - Number(a.discount))[0]
    const currentDiscount = Number(result.discount || 0)
    if (best && best.code.toLowerCase() !== order.value.voucherCode.toLowerCase() && Number(best.discount) > currentDiscount) {
      betterVoucherDialog.value = {
        orderId: order.value.id, context: paymentContext(), currentCode: order.value.voucherCode,
        currentDiscount, nextCode: best.code, nextDiscount: Number(best.discount),
        saving: roundMoney(Number(best.discount) - currentDiscount), subtotal: Number(result.subtotal),
        shippingFee: order.value.delivery.enabled ? Number(order.value.delivery.shippingFee) : 0,
        total: roundMoney(Number(result.total) - Number(best.discount) + currentDiscount)
      }
      return
    }
  } catch (e) {
    error.value = message(e); showError(error.value, 'Chưa thể kiểm tra thanh toán'); return
  } finally { checkingVoucher.value = false }
  await continuePayment()
}
async function chooseCheckoutVoucher(useNew) {
  const dialog = betterVoucherDialog.value
  if (!dialog || checkingVoucher.value) return
  if (order.value?.id !== dialog.orderId || dialog.context !== paymentContext()) {
    betterVoucherDialog.value = null; error.value = 'Hóa đơn đã thay đổi. Bấm Thanh toán để kiểm tra lại.'; return
  }
  checkingVoucher.value = true
  betterVoucherDialog.value = null
  try {
    // Honor the operator's explicit choice; background suggestions must not overwrite it.
    order.value.autoVoucher = false
    if (useNew) {
      order.value.voucherCode = dialog.nextCode
      bankAccepted.value = null
      if (!(await saveOrder())) return
    }
    await checkoutQuote()
  } catch (e) {
    error.value = message(e); showError(error.value, 'Chưa thể áp dụng phiếu'); return
  } finally { checkingVoucher.value = false }
  await continuePayment()
}
function validRecovery(saved) {
  const p = saved?.payload
  return saved?.employeeId === employeeId.value && typeof saved?.invoiceCode === 'string'
    && p && Number.isSafeInteger(p.invoiceId) && p.invoiceId > 0 && Number.isSafeInteger(p.revision) && p.revision > 0
    && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(p.requestId)
    && Array.isArray(p.items) && p.items.length > 0 && p.items.every(i => Number.isSafeInteger(i.variantId)
      && Number.isInteger(i.quantity) && i.quantity > 0 && Number.isFinite(i.unitPrice) && i.unitPrice >= 0)
    && Array.isArray(p.payments) && p.payments.length > 0 && p.payments.length <= 2
    && p.payments.every(i => Number.isSafeInteger(i.methodId) && Number.isFinite(i.amount) && i.amount >= 0)
    && Number.isFinite(p.paidAmount) && p.paidAmount >= 0
}
function restoreCheckout() {
  try {
    const raw = sessionStorage.getItem(RECOVERY_KEY)
    if (!raw) return
    const saved = JSON.parse(raw)
    if (!validRecovery(saved)) throw new Error('Không thể đọc phiên thanh toán của nhân viên hiện tại')
    pendingPayment.value = saved
    error.value = 'Có yêu cầu thanh toán chưa rõ kết quả. Kiểm tra lại bằng đúng yêu cầu đã lưu.'
  } catch (e) { recoveryBlocked.value = true; error.value = message(e) + '. Cần kiểm tra hóa đơn trước khi tiếp tục.' }
}
function clearCheckout() {
  try { sessionStorage.removeItem(RECOVERY_KEY); pendingPayment.value = null; return true }
  catch { recoveryBlocked.value = true; error.value = 'Không cập nhật được phiên thanh toán; tải lại trang để kiểm tra.'; return false }
}
async function confirmPayment() {
  if (stockSyncPromise) await stockSyncPromise
  const dialog = checkoutDialog.value
  if (!dialog || busy.value || syncingStock.value || (dialog.bank > 0 && !dialog.confirmed)) return
  if (dialog.context !== paymentContext()) {
    checkoutDialog.value = null; error.value = 'Thông tin hóa đơn đã thay đổi. Vui lòng mở lại mã QR và kiểm tra số tiền.'; return
  }
  if (dialog.bank > 0) {
    // Persist a recovery request before sending the hold. A lost response must keep this exact request.
    dialog.payload.transferConfirmed = true
    const saved = { employeeId: employeeId.value, invoiceCode: dialog.invoiceCode, payload: JSON.parse(JSON.stringify(dialog.payload)) }
    try {
      sessionStorage.setItem(RECOVERY_KEY, JSON.stringify(saved))
      busy.value = true
      await salesService.baoVeKhoanThu(saved.payload)
    } catch (e) {
      pendingPayment.value = saved
      checkoutDialog.value = null
      error.value = 'Khoản chuyển khoản cần kiểm tra: ' + message(e) + '. Không chuyển khoản thêm; dùng Kiểm tra lại thanh toán.'
      showError(error.value, 'Cần đối soát khoản thu')
      return
    } finally { busy.value = false }
    clearTimeout(quoteTimer); ++quoteVersion
    order.value.autoVoucher = false
    bankAccepted.value = { context: dialog.context, total: dialog.total, invoiceCode: dialog.invoiceCode,
      payload: JSON.parse(JSON.stringify(dialog.payload)) }
    checkoutDialog.value = null
    showSuccess('Đã xác nhận nhận tiền chuyển khoản. Bấm Thanh toán để hoàn tất hóa đơn.', 'Xác nhận chuyển khoản')
    return
  }
  dialog.payload.transferConfirmed = false
  await performPayment({ employeeId: employeeId.value, invoiceCode: dialog.invoiceCode, payload: dialog.payload })
}
async function performPayment(saved) {
  if (busy.value || recoveryBlocked.value) return
  busy.value = true; error.value = ''
  try {
    if (!validRecovery(saved)) throw new Error('Dữ liệu khôi phục thanh toán không hợp lệ')
    sessionStorage.setItem(RECOVERY_KEY, JSON.stringify(saved))
  } catch (e) {
    error.value = 'Không lưu được phiên thanh toán. Yêu cầu chưa được gửi: ' + message(e)
    showError(error.value, 'Chưa gửi thanh toán')
    busy.value = false; return
  }
  pendingPayment.value = saved
  try {
    const receipt = await salesService.thanhToan(saved.payload)
    bankAccepted.value = null
    completedReceipt.value = { ...receipt, deliveryEnabled: saved.payload.delivery?.enabled === true }
    checkoutDialog.value = null
    clearCheckout()
    orders.value = orders.value.filter(o => o.id !== saved.payload.invoiceId)
    activeId.value = orders.value[0]?.id ?? null
    showSuccess(`Thanh toán hóa đơn ${receipt.invoiceCode} thành công. Tổng thanh toán: ${money(receipt.total)}.`)
    await Promise.allSettled([reloadOrders(), loadCatalog()])
  } catch (e) {
    checkoutDialog.value = null
    if (saved.payload.transferConfirmed) {
      error.value = 'Đã xác nhận nhận tiền nhưng hóa đơn chưa hoàn tất: ' + message(e) + '. Kiểm tra lại thanh toán bằng yêu cầu đã lưu; không chuyển khoản thêm.'
    } else if (!e.response || e.response.status >= 500) {
      error.value = 'Chưa xác định được kết quả thanh toán. Bấm Kiểm tra lại thanh toán để gửi lại đúng yêu cầu; không tạo yêu cầu mới.'
    } else {
      clearCheckout(); error.value = message(e)
      if (order.value) order.value.requestId = crypto.randomUUID()
      try { await reloadOrders() } catch {}
    }
    showError(error.value, pendingPayment.value ? 'Cần kiểm tra kết quả thanh toán' : 'Thanh toán chưa hoàn tất')
  } finally { busy.value = false }
}
async function retryPayment() { if (stockSyncPromise) await stockSyncPromise; if (pendingPayment.value) await performPayment(pendingPayment.value) }
function openInvoice(print = false) {
  if (!completedReceipt.value) return
  router.push({ path: '/hoa-don/' + encodeURIComponent(completedReceipt.value.invoiceCode), query: print ? { print: '1' } : {} })
}

function rememberCustomers(drafts) {
  for (const d of drafts) if (d.customerId && !customers.value.some(c => c.id === d.customerId))
    customers.value.push({ id: d.customerId, code: d.customerCode, name: d.customerName, phone: d.customerPhone })
}
function requestStockSync() {
  stockSyncPending = true
  clearTimeout(stockSyncTimer)
  if (!alive || bankAccepted.value || auth.loggingOut || checkingVoucher.value || betterVoucherDialog.value || busy.value || syncingStock.value || shippingLoading.value || deliveryLoading.value || deliveryEditor.value || pendingPayment.value || recoveryBlocked.value
      || (typeof document !== 'undefined' && document.visibilityState === 'hidden')) return
  stockSyncTimer = setTimeout(() => { stockSyncTimer = null; syncCartStock() }, 150)
}
watch([bankAccepted, () => auth.loggingOut, checkingVoucher, betterVoucherDialog, busy, shippingLoading, deliveryLoading, deliveryEditor, pendingPayment, recoveryBlocked], () => { if (stockSyncPending) requestStockSync() })

async function syncCartStock() {
  if (stockSyncPromise) { stockSyncPending = true; return stockSyncPromise }
  if (!alive || !ready.value || bankAccepted.value || auth.loggingOut || checkingVoucher.value || betterVoucherDialog.value || busy.value || syncingStock.value || shippingLoading.value || deliveryLoading.value || deliveryEditor.value
      || pendingPayment.value || recoveryBlocked.value
      || (typeof document !== 'undefined' && document.visibilityState === 'hidden')) { stockSyncPending = true; return }
  stockSyncPending = false
  syncingStock.value = true
  stockSyncPromise = (async () => {
  try {
    const rows = await salesService.dongBoHoaDonCho()
    if (!alive) return
    rememberCustomers(rows)
    const removed = []
    let changed = false
    const next = rows.map(d => {
      const previous = orders.value.find(o => o.id === d.id)
      if (!previous) { changed = true; return hydrate(d) }
      if (String(previous.revision) === String(d.revision)) {
        for (const fresh of d.items) {
          const item = previous.items.find(i => i.id === fresh.id)
          if (item) { item.stock = fresh.stock; item.available = fresh.available; item.currentPrice = fresh.currentPrice }
        }
        return previous
      }
      const persistedIds = new Set(JSON.parse(previous.saved).items.map(i => i.variantId))
      for (const item of previous.items) if (persistedIds.has(item.id) && !d.items.some(i => i.id === item.id)) removed.push(item.name || item.code)
      changed = true
      const server = hydrate(d, previous)
      if (signature(previous) !== previous.saved) {
        const savedItems = JSON.parse(previous.saved).items
        const removedIds = new Set(savedItems.filter(i => !d.items.some(fresh => fresh.id === i.variantId)).map(i => i.variantId))
        const items = previous.items.filter(i => !removedIds.has(i.id)).map(item => {
          const fresh = d.items.find(i => i.id === item.id)
          return fresh ? { ...item, stock: fresh.stock, available: fresh.available, currentPrice: fresh.currentPrice } : item
        })
        return { ...previous, revision: d.revision, saved: server.saved, items,
          voucherCode: removedIds.size ? '' : previous.voucherCode, quote: null, quotedSignature: '', quoteError: '' }
      }
      return server
    })
    const expiredCount = orders.value.filter(o => !rows.some(d => d.id === o.id)).length
    if (next.length !== orders.value.length) changed = true
    if (changed) {
      orders.value = next
      if (!next.some(o => o.id === activeId.value)) activeId.value = next[0]?.id ?? null
      checkoutDialog.value = null; bankAccepted.value = null
      if (expiredCount) showSuccess('Hóa đơn chờ đã qua ngày hoặc được xử lý ở phiên khác đã được dọn khỏi POS. Tồn kho do máy chủ cập nhật.', 'Giỏ chờ đã cập nhật')
      if (removed.length) showSuccess(`Đã tự xóa sản phẩm ngừng kinh doanh khỏi giỏ: ${[...new Set(removed)].join(', ')}. Vui lòng kiểm tra lại đơn hàng.`, 'Giỏ hàng đã cập nhật')
    }
    if (picking.value) await loadCatalog(true)
  } catch (e) {
    // Leave the current display untouched on a transient failure; retry at the next background check.
    if (alive && e.response?.status === 403) error.value = message(e)
  } finally { syncingStock.value = false }
  })()
  try { await stockSyncPromise }
  finally { stockSyncPromise = null; if (alive && stockSyncPending) requestStockSync() }
}

async function reloadOrders() {
  const rows = await salesService.danhSachHoaDonCho()
  if (!alive) return
  rememberCustomers(rows)
  orders.value = rows.map(d => hydrate(d, orders.value.find(o => o.id === d.id)))
  if (!orders.value.some(o => o.id === activeId.value)) activeId.value = orders.value[0]?.id ?? null
}
async function saveOrder(target = order.value, successMessage = '') {
  const wasCurrent = target === order.value
  if (stockSyncPromise) {
    await stockSyncPromise
    if (wasCurrent) target = orders.value.find(o => o.id === target?.id)
  }
  if (!target) return true
  if (busy.value || syncingStock.value || deliveryLoading.value) return false
  if (!target.items.length) {
    target.voucherCode = ''; target.quote = null; target.quotedSignature = ''; target.quoteError = ''; target.paid = ''; target.autoCash = true
    target.delivery.shippingFee = 0; target.shippingQuote = null
  }
  if (!target.items.length && !completeDelivery(target.delivery)) target.delivery.enabled = false
  if (!completeDelivery(target.delivery)) {
    error.value = 'Thông tin giao hàng chưa đầy đủ. Bấm Chỉnh sửa để bổ sung hoặc bỏ tích Giao hàng.'
    showError(error.value, 'Chưa thể lưu hóa đơn')
    return false
  }
  if (target.delivery.enabled && target.items.length && (!target.shippingQuote
      || target.shippingQuote.key !== shippingKey(target)
      || Date.parse(target.shippingQuote.expiresAt) <= Date.now())) {
    shippingLoading.value = true; shippingError.value = ''
    try { await fetchShipping(target) }
    catch (e) { shippingError.value = message(e); target.shippingQuote = null; error.value = shippingError.value; return false }
    finally { shippingLoading.value = false }
  }
  if (target.saved) {
    const previous = JSON.parse(target.saved), next = payload(target)
    if (previous.customerId !== next.customerId || JSON.stringify(previous.items) !== JSON.stringify(next.items)) {
      target.autoVoucher = true
      target.voucherCode = ''
    }
  }
  if (signature(target) === target.saved) return true
  busy.value = true; error.value = ''
  const sent = payload(target)
  try {
    const result = await salesService.luuHoaDonCho(target.id, sent)
    if (!alive) return false
    // Only replace this exact draft; UI edits and tab switches are locked during the request.
    const index = orders.value.findIndex(o => o.id === target.id)
    if (index >= 0) orders.value[index] = hydrate(result, target)
    await loadCatalog(true)
    if (successMessage) showSuccess(successMessage)
    return true
  } catch (e) {
    if (alive) { error.value = message(e) + ' Thay đổi chưa được lưu.'; showError(error.value, 'Không thể lưu hóa đơn') }
    if (e.response?.status === 404 && !pendingPayment.value && !bankAccepted.value) {
      try { await reloadOrders(); await loadCatalog(true) } catch {}
    }
    if (e.response?.status === 409 && !pendingPayment.value) await reviewChangedCart(target)
    return false
  } finally { busy.value = false }
}
async function reviewChangedCart(target) {
  const results = await Promise.allSettled(target.items.map(i => salesService.chiTietSanPhamBan(i.id)))
  let saved
  try { saved = await salesService.chiTietHoaDonCho(target.id) } catch { return }
  const changed = []
  results.forEach((r, idx) => {
    const item = target.items[idx]
    if (r.status !== 'fulfilled') { item.available = false; return }
    const current = r.value, previous = saved.items.find(i => i.id === item.id)
    item.stock = Number(current.stock) + Number(previous?.qty || 0)
    item.currentPrice = current.price; item.available = true
    if (Number(item.price) !== Number(current.price)) changed.push({ item, current })
  })
  const priceKey = changed.map(({ item, current }) => `${item.id}:${current.price}`).join('|')
  if (changed.length && target.lastPricePrompt !== priceKey) {
    target.lastPricePrompt = priceKey
    confirmAction('Giá sản phẩm đã thay đổi. Cập nhật giá mới rồi kiểm tra lại tổng tiền?', async () => {
    if (locked.value || order.value?.id !== target.id) return
    for (const { item, current } of changed) Object.assign(item, current, { qty: item.qty, currentPrice: current.price })
    await saveOrder(order.value, 'Cập nhật giá sản phẩm trong giỏ hàng thành công.')
  }, { title: 'Xác nhận giá mới', confirmText: 'Cập nhật giá' })
  }
}
async function switchOrder(id) {
  if (stockSyncPromise) await stockSyncPromise
  if (locked.value || !(await saveOrder())) return
  activeId.value = id
}
async function addOrder() {
  if (stockSyncPromise) await stockSyncPromise
  if (locked.value || orders.value.length >= MAX_ORDERS || !(await saveOrder())) return
  busy.value = true; error.value = ''
  try {
    const result = await salesService.taoHoaDonCho()
    if (!alive) return
    orders.value.push(hydrate(result)); activeId.value = result.id
    showSuccess(`Tạo hóa đơn chờ ${result.invoiceCode} thành công.`)
  } catch (e) {
    error.value = message(e)
    showError('Chưa tạo được hoặc chưa xác định được kết quả tạo hóa đơn. ' + error.value, 'Cần kiểm tra hóa đơn chờ')
    // A lost create response can still have committed; re-read instead of blindly retrying POST.
    try { await reloadOrders() } catch { ready.value = false; error.value += ' Tải lại trang để xác minh trước khi tạo tiếp.' }
  } finally { busy.value = false }
}
function removeOrder(id) {
  if (locked.value) return
  const target = orders.value.find(o => o.id === id)
  if (!target) return
  confirmAction(`Hủy hóa đơn ${target.name}? Giỏ hàng sẽ được lưu trong lịch sử, không xóa hóa đơn.`, async () => {
    if (stockSyncPromise) await stockSyncPromise
    if (locked.value) return
    if (order.value?.id !== target.id && !(await saveOrder())) return
    busy.value = true; error.value = ''
    try {
      await salesService.huyHoaDonCho(target.name)
      await reloadOrders(); await loadCatalog()
      showSuccess(`Hủy hóa đơn chờ ${target.name} thành công.`)
    } catch (e) { error.value = message(e); showError(error.value, 'Cần kiểm tra lại thao tác hủy hóa đơn'); try { await reloadOrders() } catch {} }
    finally { busy.value = false }
  }, { title: 'Hủy hóa đơn chờ', confirmText: 'Hủy hóa đơn' })
}
function cancelOrder() { if (order.value) removeOrder(order.value.id) }
function canAddItem(v) {
  const current = order.value?.items.find(i => i.id === v.id)
  return Number(v.stock) > 0 && (!current || current.qty < Math.min(Number(current.stock), 10000))
}
function cartSnapshot() {
  return { id: order.value.id, revision: order.value.revision, items: JSON.parse(JSON.stringify(order.value.items)), voucherCode: order.value.voucherCode, autoVoucher: order.value.autoVoucher }
}
function restoreUncommittedCart(snapshot) {
  const current = order.value
  if (!current || current.id !== snapshot.id || current.revision !== snapshot.revision) return
  const fresh = new Map(current.items.map(i => [i.id, i]))
  current.items = snapshot.items.map(i => ({ ...i, stock: fresh.get(i.id)?.stock ?? i.stock,
    available: fresh.get(i.id)?.available ?? i.available, currentPrice: fresh.get(i.id)?.currentPrice ?? i.currentPrice }))
  current.voucherCode = snapshot.voucherCode
  current.autoVoucher = snapshot.autoVoucher
  current.quote = null; current.quotedSignature = ''
}
async function addItem(v) {
  if (stockSyncPromise) await stockSyncPromise
  if (locked.value || shippingLoading.value || !order.value) return
  const cur = order.value.items.find(i => i.id === v.id)
  if (!canAddItem(v)) { error.value = 'Số lượng đã đạt tồn kho hoặc giới hạn hiện tại'; showError(error.value, 'Không thể thêm sản phẩm'); return }
  if (cur && cur.qty >= cur.stock) return
  const before = cartSnapshot()
  if (cur) cur.qty++
  else order.value.items.push({ ...v, qty: 1, currentPrice: v.price, available: true })
  if (!(await saveOrder(order.value, `Thêm sản phẩm ${v.name || v.code || ''} vào giỏ hàng thành công.`))) restoreUncommittedCart(before)
}
async function setQty(i, n, input) {
  if (stockSyncPromise) await stockSyncPromise
  i = order.value?.items.find(item => item.id === i.id)
  if (!i) return
  if (locked.value || shippingLoading.value) return
  const qty = Number(n)
  if (!Number.isInteger(qty) || qty < 1 || qty > Math.min(10000, Number(i.stock))) {
    if (input) input.value = i.qty
    showError(`Số lượng phải từ 1 đến ${Math.min(10000, Number(i.stock))}.`, 'Vượt giới hạn tồn kho'); return
  }
  if (qty === i.qty) return
  const before = cartSnapshot()
  i.qty = qty
  if (!(await saveOrder(order.value, `Cập nhật số lượng sản phẩm ${i.name || i.code || ''} thành ${qty} thành công.`))) restoreUncommittedCart(before)
  if (input) input.value = order.value?.items.find(item => item.id === i.id)?.qty ?? qty
}
async function removeItem(code) {
  if (stockSyncPromise) await stockSyncPromise
  if (locked.value || shippingLoading.value || !order.value) return
  const item = order.value.items.find(i => i.code === code)
  if (!item) return
  const before = cartSnapshot()
  order.value.items = order.value.items.filter(i => i.code !== code)
  if (!(await saveOrder(order.value, `Xóa sản phẩm ${item.name || item.code} khỏi giỏ hàng thành công.`))) restoreUncommittedCart(before)
}
async function refreshCart() {
  if (locked.value || !order.value?.items.length) return
  const target = order.value
  busy.value = true; error.value = ''
  try {
    const results = await Promise.allSettled(target.items.map(i => salesService.chiTietSanPhamBan(i.id)))
    if (!alive) return
    results.forEach((r, index) => {
      const item = target.items[index]
      if (r.status === 'fulfilled') {
        Object.assign(item, r.value, { currentPrice: r.value.price, available: true })
      } else { item.available = false; error.value = message(r.reason) }
    })
    error.value ||= 'Đã cập nhật giá hiện tại. Kiểm tra giá và số lượng rồi bấm Lưu thay đổi; tồn kho chưa được giữ.'
  } finally { busy.value = false }
}
function reloadCurrent() {
  if (locked.value || !order.value) return
  const target = order.value
  confirmAction('Tải dữ liệu trên máy chủ và bỏ các thay đổi chưa lưu của hóa đơn này?', async () => {
    if (locked.value) return
    busy.value = true
    try {
      const result = await salesService.chiTietHoaDonCho(target.id)
      const index = orders.value.findIndex(o => o.id === target.id)
      if (alive && index >= 0) orders.value[index] = hydrate(result, target)
      error.value = ''
      if (alive && index >= 0) showSuccess(`Tải lại hóa đơn chờ ${target.name} thành công.`)
    } catch (e) { error.value = message(e); showError(error.value, 'Không thể tải lại hóa đơn') }
    finally { busy.value = false }
  })
}
function openPicker() {
  if (locked.value || !order.value) return
  q.value = ''; catalogPage.value = 0; picking.value = true; loadCatalog()
}
async function loadCatalog(silent = false) {
  const version = ++catalogVersion; if (silent !== true) loading.value = true
  try {
    const result = await salesService.danhSachSanPhamBan({ page: catalogPage.value, size: 10, keyword: q.value.trim(),
      color: colorFilter.value.trim(), variantSize: sizeFilter.value.trim(), categoryId: categoryFilter.value || undefined, brandId: brandFilter.value || undefined })
    if (!alive || version !== catalogVersion) return
    const rows = result.content || []
    catalog.value = rows; catalogPages.value = result.totalPages || 0
    await loadImages(rows)
  } catch (e) { if (alive && version === catalogVersion) error.value = message(e) }
  finally { if (version === catalogVersion) loading.value = false }
}
async function loadCustomers() {
  const version = ++customerVersion
  customerLoading.value = true; customerSearchError.value = ''
  try {
    const result = await customerService.getCustomers({ page: 1, size: 20, trangThai: 1, tuKhoa: customerSearch.value.trim() })
    if (!alive || version !== customerVersion) return
    const found = result.content || []
    customerResults.value = found
    const selectedIds = new Set(orders.value.map(o => o.customerId).filter(Boolean))
    customers.value = [...customers.value.filter(c => selectedIds.has(c.id) && !found.some(x => x.id === c.id)), ...found]
  } catch (e) {
    if (alive && version === customerVersion) { customerResults.value = []; customerSearchError.value = message(e) }
  } finally { if (alive && version === customerVersion) customerLoading.value = false }
}
async function selectCustomer(customer) {
  if (stockSyncPromise) await stockSyncPromise
  if (locked.value || !order.value) return
  order.value.customerId = customer?.id ?? null
  choosingCustomer.value = false
  customerSearch.value = customer?.name || customer?.code || ''
  await changeCustomer()
}
async function clearCustomer() {
  if (stockSyncPromise) await stockSyncPromise
  if (locked.value || !order.value) return
  order.value.customerId = null
  choosingCustomer.value = false; customerSearch.value = ''
  await changeCustomer()
}
async function onCustomerInput(event) {
  choosingCustomer.value = true
  if (stockSyncPromise) await stockSyncPromise
  if (!customerSearch.value.trim() && order.value?.customerId != null) await clearCustomer()
}
watch(activeId, () => {
  shippingError.value = ''
  choosingCustomer.value = false
  customerSearch.value = selectedCustomer.value?.name || ''
})
watch([q, colorFilter, sizeFilter, categoryFilter, brandFilter], () => {
  catalogPage.value = 0; ++catalogVersion; clearTimeout(searchTimer); searchTimer = setTimeout(loadCatalog, 250)
})
watch(catalogPage, loadCatalog)
watch(customerSearch, () => {
  ++customerVersion; clearTimeout(customerTimer)
  customerResults.value = []; customerSearchError.value = ''; customerLoading.value = true
  if (!customerSearch.value.trim()) { customerLoading.value = false; return }
  customerTimer = setTimeout(loadCustomers, 250)
})
async function preparePosLogout() {
  if (bankAccepted.value || busy.value || shippingLoading.value || deliveryLoading.value || checkingVoucher.value
      || betterVoucherDialog.value || deliveryEditor.value || checkoutDialog.value || pendingPayment.value || recoveryBlocked.value) {
    showError('Hoàn tất hoặc đóng thao tác đang xử lý trước khi đăng xuất. Phiên đăng nhập vẫn được giữ.', 'Chưa thể đăng xuất')
    return false
  }
  ++quoteVersion; clearTimeout(quoteTimer); clearTimeout(searchTimer); clearTimeout(customerTimer)
  if (stockSyncPromise) await stockSyncPromise
  const current = order.value
  if (!current) return true
  // An unfinished delivery toggle has never been a valid saved order. Keep the last
  // persisted delivery mode while saving any other changes; do not cancel the cart or release stock.
  const target = { ...current, delivery: { ...current.delivery } }
  if (!completeDelivery(target.delivery)) {
    const persisted = JSON.parse(current.saved)
    target.delivery = persisted.delivery || { enabled: false, shippingFee: 0 }
    target.shippingQuote = null
    target.shippingDestination = null
  }
  if (signature(target) === current.saved) return true
  if (!(await saveOrder(target))) return false
  return true
}
const unregisterLogoutGuard = auth.setLogoutGuard(preparePosLogout)
onBeforeRouteLeave(async () => {
  if (auth.logoutAuthorized || auth.isAuthenticated === false) return true
  if (auth.loggingOut) return false
  return !bankAccepted.value && !checkingVoucher.value && !betterVoucherDialog.value && !busy.value && await saveOrder()
})
onMounted(async () => {
  try {
    const me = (await api.get('/auth/me')).data.data
    if (me.userType !== 'NHAN_VIEN' || !['ADMIN', 'STAFF'].includes(me.role)) throw new Error('Chỉ nhân viên được sử dụng POS')
    employeeId.value = me.userId
    employeeName.value = auth.user?.id === me.userId ? `${auth.user.code || ''} · ${auth.user.name || me.username}` : me.username
    restoreCheckout(); await reloadOrders(); ready.value = true
    await Promise.all([loadCatalog(), loadCustomers(), loadPaymentOptions(), productAttributeService.options(['colors', 'sizes', 'categories', 'brands']).then(v => { attributes.value = v })])
    if (typeof window !== 'undefined') {
      window.addEventListener('focus', requestStockSync)
      window.addEventListener('online', requestStockSync)
      document.addEventListener('visibilitychange', requestStockSync)
      stockPollTimer = window.setInterval(requestStockSync, 4000)
      requestStockSync()
    }
  } catch (e) { error.value = message(e) }
})
onBeforeUnmount(() => { unregisterLogoutGuard(); if (typeof window !== 'undefined') { window.clearInterval(stockPollTimer); window.removeEventListener('focus', requestStockSync); window.removeEventListener('online', requestStockSync); document.removeEventListener('visibilitychange', requestStockSync) } clearTimeout(stockSyncTimer); alive = false; ++quoteVersion; ++addressVersion; clearTimeout(quoteTimer); ++catalogVersion; ++customerVersion; clearTimeout(searchTimer); clearTimeout(customerTimer) })
</script>

<template>
  <AdminLayout>
    <div v-if="error || pendingPayment" class="ss-card" role="alert">{{ error }}
      <button v-if="pendingPayment && !recoveryBlocked" class="ss-btn" :disabled="busy" @click="retryPayment">Kiểm tra lại thanh toán</button>
      <button v-if="pendingPayment" class="ss-btn" :disabled="busy" @click="router.push('/hoa-don/' + encodeURIComponent(pendingPayment.invoiceCode))">Xem hóa đơn</button>
      <button v-if="!pendingPayment && !recoveryBlocked" class="ss-btn sm" @click="error = ''">Đóng</button>
    </div>
    <main class="ss-page pos">
      <div class="left ss-stack">
        <!-- Hóa đơn chờ -->
        <section class="ss-card">
          <div class="ss-head">
            <button class="ss-btn" :disabled="locked || orders.length >= MAX_ORDERS" @click="addOrder"><i class="bi bi-plus-lg"></i> Thêm hóa đơn chờ</button>
            <span class="ss-spacer"></span>
            <span class="ss-count strong">{{ orders.length }}/{{ MAX_ORDERS }} hóa đơn</span>
          </div>
          <p v-if="!orders.length" class="muted-center">Chưa có hóa đơn chờ nào.</p>
          <div v-else class="tabs">
            <button v-for="o in orders" :key="o.id" :disabled="locked" :class="{ on: o.id === activeId }" @click="switchOrder(o.id)">
              {{ o.name }} <b v-if="o.items.length">{{ o.items.length }}</b>
              <i class="bi bi-x" title="Đóng" @click.stop="removeOrder(o.id)"></i>
            </button>
          </div>
        </section>

        <!-- Giỏ hàng -->
        <section class="ss-card grow">
          <div class="ss-head">
            <h2>Giỏ hàng</h2><span class="ss-pill">{{ count }} sản phẩm</span>
            <span class="ss-spacer"></span>
            <button class="ss-btn" :disabled="locked || !order" @click="openPicker"><i class="bi bi-qr-code-scan"></i> Nhập mã</button>
            <button class="ss-btn primary" :disabled="locked || !order" @click="openPicker">Chọn sản phẩm</button>
          </div>
          <div class="ss-table-wrap">
            <table class="ss-table" style="min-width:640px">
              <thead><tr><th class="w-stt c">STT</th><th>Ảnh sản phẩm</th><th>Mã sản phẩm</th><th>Tên sản phẩm</th><th>Màu sắc</th><th class="c">Kích cỡ</th><th class="c">Số lượng</th><th class="r">Đơn giá</th><th class="c">Thao tác</th></tr></thead>
              <tbody>
                <tr v-for="(i, idx) in order?.items || []" :key="i.code">
                  <td class="c">{{ idx + 1 }}</td>
                  <td><ProductThumbnail :url="imageSources[i.id]" :item="badgeItem(i)" :alt="i.name" /></td><td :title="i.code"><span class="ss-code">{{ i.productCode || i.code }}</span></td>
                  <td class="cart-product-name"><span class="ss-strong">{{ i.name }}</span></td>
                  <td>{{ i.color }}</td>
                  <td class="c">{{ i.size }}</td>
                  <td class="c"><div class="qty"><button :disabled="locked || i.qty <= 1" aria-label="Giảm số lượng" @click="setQty(i, i.qty - 1)">−</button><input :disabled="locked" :value="i.qty" inputmode="numeric" @change="setQty(i, $event.target.value, $event.target)" /><button :disabled="locked || i.qty >= i.stock || i.qty >= 10000" aria-label="Tăng số lượng" @click="setQty(i, i.qty + 1)">+</button></div></td>
                  <td class="r nowrap" :title="i.campaignName || ''"><del v-if="i.discounted" class="pos-price-old">{{ money(i.originalPrice) }}</del><strong :class="{'sale-price': i.discounted}">{{ money(i.price) }}</strong></td>
                  <td class="c"><button class="ss-icon-btn danger" :disabled="locked" title="Xóa" @click="removeItem(i.code)"><i class="bi bi-trash3"></i></button></td>
                </tr>
              </tbody>
            </table>
            <p v-if="!order?.items.length" class="empty">Giỏ hàng trống.</p>
          </div>
        </section>
        <section v-if="order?.delivery.enabled" class="ss-card delivery-card"><div class="ss-head"><div class="ss-head-icon"><i class="bi bi-truck"></i></div><h3>Thông tin giao hàng</h3><span class="ss-spacer"></span><button class="ss-btn sm" :disabled="locked" @click="openDelivery">Chỉnh sửa</button></div><p v-if="!deliveryReady" class="delivery-hint" role="status">Chưa có thông tin giao hàng đầy đủ. Bấm “Chỉnh sửa” để bổ sung địa chỉ trước khi thanh toán.</p><div class="delivery-grid"><label>Người nhận<strong>{{ order.delivery.recipientName || 'Chưa nhập' }}</strong></label><label>Số điện thoại<strong>{{ order.delivery.recipientPhone || 'Chưa nhập' }}</strong></label><label class="address-line">Địa chỉ giao hàng<strong>{{ order.delivery.address || 'Chưa nhập địa chỉ' }}</strong></label></div></section>
      </div>

      <aside class="right ss-stack">
        <section class="ss-card tight">
          <h3 class="side-title">Khách hàng</h3>
          <div v-if="!selectedCustomer" class="cust">
            <input class="ss-input" v-model="customerSearch" placeholder="Tìm theo tên, mã hoặc SĐT" :disabled="locked" @input="onCustomerInput" @focus="choosingCustomer = true" @keydown.esc="choosingCustomer = false" aria-label="Tìm khách hàng theo tên, mã hoặc số điện thoại" />
            <button class="ss-icon-btn sq" :disabled="locked" title="Thêm khách hàng" @click="router.push('/khach-hang/them')"><i class="bi bi-person-plus"></i></button>
          </div>
          <div v-if="!selectedCustomer && choosingCustomer && customerSearch.trim()" class="customer-results" aria-label="Kết quả tìm khách hàng">
            <div class="customer-results-head"><strong>Kết quả tìm kiếm</strong><button type="button" class="ss-icon-btn" @click="choosingCustomer = false" aria-label="Đóng kết quả tìm kiếm"><i class="bi bi-x"></i></button></div>
            <p v-if="customerLoading" role="status">Đang tìm khách hàng…</p>
            <p v-else-if="customerSearchError" role="alert">{{ customerSearchError }}</p>
            <template v-else>
              <button v-for="c in customerResults" :key="c.id" type="button" class="customer-result" :disabled="locked" @click="selectCustomer(c)">
                <strong>{{ c.name || 'Chưa cập nhật tên' }}</strong>
                <small>{{ c.code || '—' }} · {{ c.phone || 'Chưa có SĐT' }}</small>
              </button>
              <p v-if="!customerResults.length">Không tìm thấy khách hàng phù hợp.</p>
              <p v-else-if="customerResults.length === 20">Hiển thị 20 kết quả. Nhập thêm tên hoặc mã để tìm chính xác hơn.</p>
            </template>
          </div>
          <div v-if="selectedCustomer" class="selected-customer">
            <div><strong>{{ selectedCustomer.name }}</strong><small>{{ selectedCustomer.code }} · {{ selectedCustomer.phone }}</small></div>
            <button type="button" class="ss-icon-btn" :disabled="locked" @click="clearCustomer" aria-label="Bỏ khách hàng">×</button>
          </div>
          <div v-if="order && !selectedCustomer" class="ss-hint" style="margin-top:8px">
            Khách vãng lai → <b>{{ order.delivery.enabled ? 'Giao hàng' : 'Nhận tại quầy' }}</b>
          </div>

        </section>

        <section class="ss-card" v-if="order">
          <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-receipt"></i></div><h2>Thông tin đơn hàng</h2><span class="ss-spacer"></span><label class="delivery-toggle"><span>Giao hàng</span><input type="checkbox" role="switch" aria-label="Giao hàng" :checked="order.delivery.enabled" :disabled="locked" @change="toggleDelivery($event.target.checked)" /><span class="delivery-switch-track" aria-hidden="true"></span></label></div>
          <div v-if="selectedVoucher && quoteReady" class="voucher-card"><div><i class="bi bi-check2-circle"></i> {{ bestSelected ? 'Đang áp dụng phiếu tốt nhất' : 'Phiếu đang áp dụng' }}<button class="ss-icon-btn" :disabled="locked" aria-label="Bỏ phiếu" @click="applyVoucher('')">×</button></div><div><b>{{ selectedVoucher.code }} <small>{{ selectedVoucher.form === 2 ? 'Cá nhân' : 'Công khai' }}</small></b><span class="voucher-badge">{{ voucherLabel(selectedVoucher) }}</span></div><div><span>Giá trị giảm</span><b>−{{ money(order.quote.discount) }}</b></div></div>
          <div class="ss-field voucher-selector"><label class="ss-label" for="pos-voucher">Phiếu giảm giá</label>
            <select id="pos-voucher" class="ss-select" :value="order.voucherCode" :disabled="locked || !count" @change="applyVoucher($event.target.value)">
              <option value="">Không sử dụng phiếu</option>
              <optgroup v-for="form in [2, 1]" :key="form" :label="form === 2 ? 'Phiếu cá nhân của khách' : 'Phiếu công khai'">
                <option v-for="v in validVouchers.filter(v => Number(v.form || 1) === form)" :key="v.code" :value="v.code">{{ v.code }} · {{ voucherLabel(v) }} · Giảm {{ money(v.discount) }}</option>
              </optgroup>
              <optgroup v-if="order.quote?.ineligibleVouchers?.length" label="Chưa đủ điều kiện">
                <option v-for="v in order.quote.ineligibleVouchers" :key="v.code" disabled>{{ v.form === 2 ? 'Cá nhân · ' : '' }}{{ v.code }} · {{ v.reason }}</option>
              </optgroup>
              <option v-if="order.voucherCode && !validVouchers.some(v => v.code.toLowerCase() === order.voucherCode.toLowerCase())" :value="order.voucherCode">{{ order.voucherCode }}</option>
            </select>
            <small v-if="validVouchers[0]" class="ss-hint">Phiếu tốt nhất: {{ validVouchers[0].code }} · giảm {{ money(validVouchers[0].discount) }}</small>
          </div>
          <div class="kv"><span>Tổng tiền hàng ({{ count }} sản phẩm):</span><b>{{ money(total) }}</b></div>
          <div class="kv" v-if="order.delivery.enabled"><span class="shipping-provider-label"><GhnLogo /> Phí vận chuyển:</span><div class="shipping-fee"><b>{{ shippingReady ? money(order.delivery.shippingFee) : 'Chưa tính phí' }}</b><button type="button" class="ss-icon-btn" :disabled="locked || !count || !deliveryReady" @click="refreshShipping" title="Cập nhật phí GHN"><i class="bi bi-arrow-clockwise"></i></button></div></div><p v-if="shippingLoading" class="ss-hint" role="status">Đang lấy phí GHN…</p><p v-if="order.delivery.enabled && shippingError" role="alert">{{ shippingError }}</p>
          <div class="kv reduction-row"><span>Tiền giảm từ phiếu:</span><b>−{{ money(quoteReady ? order.quote.discount : 0) }}</b></div>
          <section v-if="quoteReady && buyMoreSuggestions.length" class="buy-more-card">
            <h3>Gợi ý mua thêm <small>{{ buyMoreSuggestions.length }} đề xuất</small></h3>
            <div v-for="v in buyMoreSuggestions" :key="v.code" class="buy-more-item">
              <div><span class="voucher-badge">{{ voucherLabel(v) }}</span> <b>{{ v.code }}</b></div>
              <div><span>Cần mua thêm:</span><strong>{{ money(v.amountNeeded) }}</strong></div>
              <div><span>Sẽ được giảm:</span><strong class="saving">{{ money(v.projectedDiscount) }}</strong></div>
            </div>
          </section>
          <div class="kv payable-row"><span>Khách cần trả:</span><b>{{ money(payable) }}</b></div>
          <p v-if="order.quoteError" role="alert">{{ order.quoteError }}</p>


          <div class="ss-field"><span class="ss-label strong">Hình thức thanh toán</span>
            <div class="ss-radios" style="min-height:24px;gap:12px;flex-wrap:wrap">
              <label v-for="m in paymentChoices" :key="m.value"><input type="radio" :value="m.value" v-model="order.paymentMode" :disabled="locked" /> {{ m.name }}</label>
            </div>
          </div>
          <small v-if="order.paymentMode !== 'CASH' && !quoteReady" class="ss-hint">Thêm sản phẩm và hoàn tất thông tin giao hàng để tính số tiền chuyển khoản.</small>
          <button v-if="order.paymentMode !== 'CASH' && !bankAccepted" class="ss-btn" :disabled="!canPay" @click="openQr">{{ bankConfig ? 'Hiển thị QR chuyển khoản' : 'Xác nhận chuyển khoản thủ công' }}</button>
          <div v-if="order.paymentMode !== 'BANK'" class="ss-field"><span class="ss-label strong">Tiền mặt khách đưa</span>
            <input class="ss-input" type="text" inputmode="decimal" :value="cashDisplay" @input="inputCash" :disabled="locked" aria-label="Tiền mặt khách đưa" autocomplete="off" placeholder="Tự điền bằng số tiền khách cần trả" /><small class="ss-hint">Điền sẵn đúng số tiền cần trả. Có thể sửa nếu khách đưa số tiền khác.</small></div>
          <div v-else class="kv"><span>Cần chuyển khoản:</span><b>{{ money(payable) }}</b></div>
          <small v-if="cashError" class="cash-error" role="alert">{{ cashError }}</small>
          <p v-if="order.paymentMode === 'BANK' && bankConfirmed" class="bank-confirmed" role="status"><i class="bi bi-check-circle"></i> Đã xác nhận nhận tiền. Phiếu và số tiền đã được chốt; bấm Thanh toán để hoàn tất, không chuyển khoản thêm.</p>
          <div class="kv"><span>Tiền thừa trả khách</span><b>{{ money(change) }}</b></div>
          <div class="ss-field"><span class="ss-label strong">Ghi chú thanh toán</span>
            <textarea class="ss-textarea" v-model="order.note" @change="saveOrder(order, 'Cập nhật ghi chú thanh toán thành công.')" maxlength="960" :disabled="locked" placeholder="Ghi chú thêm nếu cần"></textarea></div>

          <div v-if="dirty || error" class="pos-save-actions">
            <button v-if="dirty" class="ss-btn sm" :disabled="locked" @click="saveOrder(order, 'Lưu hóa đơn chờ thành công.')">Lưu</button>
            <button v-if="error" class="ss-btn sm" :disabled="locked" @click="reloadCurrent">Tải lại hóa đơn</button>
          </div>
          <p v-if="cartWarnings.length" role="alert">Có sản phẩm thay đổi giá hoặc tồn kho. Vui lòng kiểm tra lại.</p>
          <div class="btns">
            <button class="ss-btn" :disabled="locked" @click="cancelOrder">Hủy hóa đơn</button>
            <button class="ss-btn primary" :disabled="!canPay" @click="pay">{{ checkingVoucher ? 'Đang kiểm tra phiếu…' : 'Thanh toán' }}</button>
          </div>
        </section>
        <section v-else class="ss-card"><p class="muted-center">Tạo hóa đơn chờ để bắt đầu bán hàng.</p></section>
      </aside>
    </main>

    <div v-if="deliveryEditor" class="ss-modal-bg" @click.self="closeDelivery">
      <section class="ss-modal delivery-modal" role="dialog" aria-modal="true" aria-labelledby="delivery-title">
        <div class="ss-head"><h2 id="delivery-title">Chỉnh sửa thông tin giao hàng</h2><span class="ss-spacer"></span><button class="ss-icon-btn" :disabled="busy || shippingLoading" @click="closeDelivery" aria-label="Đóng">×</button></div>
        <p v-if="deliveryError || shippingError" role="alert">{{ deliveryError || shippingError }}</p>
        <section class="delivery-section"><h3>Thông tin liên hệ</h3><div class="delivery-grid">
          <label>Tên người nhận *<input class="ss-input" v-model="deliveryEditor.recipientName" maxlength="255" :disabled="busy || shippingLoading" @input="deliveryEditor.addressId = null; clearDeliveryField('recipientName')" @blur="validateDeliveryField('recipientName')" :class="{'is-invalid': deliveryFields.recipientName}" :aria-invalid="Boolean(deliveryFields.recipientName)" aria-describedby="delivery-name-error" /><small v-if="deliveryFields.recipientName" id="delivery-name-error" class="field-error" role="alert">{{ deliveryFields.recipientName }}</small></label>
          <label>Số điện thoại *<input class="ss-input" v-model="deliveryEditor.recipientPhone" type="tel" inputmode="numeric" autocomplete="tel" maxlength="20" :disabled="busy || shippingLoading" @input="deliveryEditor.addressId = null; clearDeliveryField('recipientPhone')" @blur="validateDeliveryField('recipientPhone')" :class="{'is-invalid': deliveryFields.recipientPhone}" :aria-invalid="Boolean(deliveryFields.recipientPhone)" aria-describedby="delivery-phone-error" /><small v-if="deliveryFields.recipientPhone" id="delivery-phone-error" class="field-error" role="alert">{{ deliveryFields.recipientPhone }}</small></label>
        </div></section>
        <section class="delivery-section"><h3>Địa chỉ giao hàng</h3><p v-if="deliveryFields.address" class="field-error" role="alert">{{ deliveryFields.address }}</p>
          <label v-for="a in addresses" :key="a.id" class="address-choice"><input type="radio" :value="a.id" v-model="deliveryEditor.addressId" :disabled="busy || shippingLoading" @change="selectDeliveryAddress(a)" /><span><b>{{ a.receiverName }}</b> · {{ a.receiverPhone }}<small v-if="a.isDefault" class="voucher-badge">Mặc định</small><br />{{ a.fullAddress }}</span></label>
          <label class="address-choice"><input type="radio" :value="null" v-model="deliveryEditor.addressId" :disabled="busy || shippingLoading" /> Sử dụng địa chỉ khác</label>
          <div v-if="!deliveryEditor.addressId" class="delivery-grid">
            <label>Tỉnh/Thành phố *<select class="ss-select" v-model="addressForm.province" :disabled="busy || shippingLoading || areasLoading" @change="changeProvince" @blur="validateDeliveryField('province')" :class="{'is-invalid': deliveryFields.province}" :aria-invalid="Boolean(deliveryFields.province)" aria-describedby="delivery-province-error"><option value="">Chọn tỉnh/thành</option><option v-for="a in provinces" :key="a.id" :value="a.id">{{ a.ten }}</option></select><small v-if="deliveryFields.province" id="delivery-province-error" class="field-error" role="alert">{{ deliveryFields.province }}</small></label>
            <label>Phường/Xã *<select class="ss-select" v-model="addressForm.ward" @change="validateDeliveryField('ward')" @blur="validateDeliveryField('ward')" :class="{'is-invalid': deliveryFields.ward}" :aria-invalid="Boolean(deliveryFields.ward)" aria-describedby="delivery-ward-error" :disabled="busy || shippingLoading || !addressForm.province || areasLoading"><option value="">Chọn phường/xã</option><option v-for="a in wards" :key="a.id" :value="a.id">{{ a.ten }}</option></select><small v-if="deliveryFields.ward" id="delivery-ward-error" class="field-error" role="alert">{{ deliveryFields.ward }}</small></label>
            <label class="address-line">Địa chỉ cụ thể *<input class="ss-input" v-model="addressForm.street" @input="clearDeliveryField('street')" @blur="validateDeliveryField('street')" :class="{'is-invalid': deliveryFields.street}" :aria-invalid="Boolean(deliveryFields.street)" aria-describedby="delivery-street-error" :disabled="busy || shippingLoading || areasLoading" maxlength="350" /><small v-if="deliveryFields.street" id="delivery-street-error" class="field-error" role="alert">{{ deliveryFields.street }}</small></label>
          </div>
        </section>
        <p v-if="areasLoading" class="ss-hint" role="status">Đang tải danh mục địa chỉ…</p>
        <p class="ss-hint">Phí vận chuyển được lấy từ GHN khi lưu, không nhập thủ công.</p>
        <div class="ss-actions"><button class="ss-btn" :disabled="busy || shippingLoading" @click="closeDelivery">Hủy</button><button class="ss-btn primary" :disabled="busy || shippingLoading || areasLoading || !count" @click="saveDelivery">{{ busy || shippingLoading ? 'Đang lấy phí và lưu…' : 'Lưu thay đổi' }}</button></div>
      </section>
    </div>

    <!-- Modal chọn sản phẩm -->
    <div v-if="picking" class="ss-modal-bg" @click.self="picking = false">
      <div class="ss-modal" role="dialog" aria-modal="true" aria-label="Chọn sản phẩm">
        <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-box-seam"></i></div><h2>Chọn sản phẩm</h2><span class="ss-spacer"></span>
          <button class="ss-icon-btn" aria-label="Đóng" @click="picking = false"><i class="bi bi-x-lg"></i></button></div>
        <p v-if="error" role="alert">{{ error }}</p>
        <div class="ss-search"><i class="bi bi-search"></i><input class="ss-input" v-model="q" placeholder="Tìm theo mã, tên, màu sắc..." autofocus /></div>
        <div class="pos-filters">
          <label>Màu sắc<select class="ss-select" v-model="colorFilter"><option value="">Tất cả màu sắc</option><option v-for="a in attributes.colors || []" :key="a.id" :value="a.ten">{{ a.ten }}</option></select></label>
          <label>Size<select class="ss-select" v-model="sizeFilter"><option value="">Tất cả size</option><option v-for="a in attributes.sizes || []" :key="a.id" :value="a.giaTri || a.ten">{{ a.giaTri || a.ten }}</option></select></label><label>Danh mục<select class="ss-select" v-model="categoryFilter"><option value="">Tất cả danh mục</option><option v-for="a in attributes.categories || []" :key="a.id" :value="a.id">{{ a.ten }}</option></select></label><label>Thương hiệu<select class="ss-select" v-model="brandFilter"><option value="">Tất cả thương hiệu</option><option v-for="a in attributes.brands || []" :key="a.id" :value="a.id">{{ a.ten }}</option></select></label>
        </div>
        <div class="ss-table-wrap">
          <table class="ss-table" style="min-width:600px">
            <thead><tr><th>STT</th><th>Ảnh sản phẩm</th><th>Mã sản phẩm</th><th>Tên sản phẩm</th><th>Màu</th><th class="c">Size</th><th class="r">Tồn kho</th><th class="r">Giá</th><th class="c"></th></tr></thead>
            <tbody>
              <tr v-for="(v, idx) in shown" :key="v.code">
                <td>{{ catalogPage * 10 + idx + 1 }}</td><td><ProductThumbnail :url="imageSources[v.id]" :item="badgeItem(v)" :alt="v.name" /></td><td :title="v.code"><span class="ss-code">{{ v.productCode }}</span><small class="sku">{{ v.code }}</small></td><td class="ss-strong">{{ v.name }}</td><td>{{ v.color }}</td><td class="c">{{ v.size }}</td>
                <td class="r">{{ v.stock }}</td><td class="r nowrap" :title="[v.campaignCode, v.campaignName].filter(Boolean).join(' · ')"><del v-if="v.discounted" class="pos-price-old">{{ money(v.originalPrice) }}</del><strong :class="{'sale-price': v.discounted}">{{ money(v.price) }}</strong></td>
                <td class="c"><button class="ss-btn sm" :disabled="locked || !order || !canAddItem(v)" @click="addItem(v)"><i class="bi bi-plus-lg"></i> Thêm</button></td>
              </tr>
              <tr v-if="loading"><td colspan="9" class="ss-empty">Đang tải sản phẩm...</td></tr>
              <tr v-else-if="!shown.length"><td colspan="9" class="ss-empty"><i class="bi bi-inbox"></i>Không tìm thấy sản phẩm.</td></tr>
            </tbody>
          </table>
        </div>
        <div class="ss-actions">
          <button class="ss-btn" :disabled="loading || catalogPage === 0" @click="catalogPage--">Trước</button>
          <span>Trang {{ catalogPage + 1 }}/{{ catalogPages || 1 }}</span>
          <button class="ss-btn" :disabled="loading || catalogPage + 1 >= catalogPages" @click="catalogPage++">Sau</button>
          <button class="ss-btn primary" @click="picking = false">Xong</button>
        </div>
      </div>
    </div>

    <div v-if="betterVoucherDialog" class="ss-modal-bg" @click.self="betterVoucherDialog = null">
      <section class="ss-modal voucher-upgrade" role="dialog" aria-modal="true" aria-labelledby="better-voucher-title">
        <div class="ss-head"><h2 id="better-voucher-title"><i class="bi bi-info-circle-fill"></i> Có phiếu giảm giá tốt hơn</h2><span class="ss-spacer"></span><button class="ss-icon-btn" aria-label="Đóng" @click="betterVoucherDialog = null">×</button></div>
        <p class="voucher-upgrade-notice">Có phiếu giúp đơn hàng này giảm được nhiều tiền hơn!</p>
        <div class="voucher-comparison">
          <div><span>Phiếu hiện tại</span><b>{{ betterVoucherDialog.currentCode || 'Không sử dụng phiếu' }}</b><small>Giảm: {{ money(betterVoucherDialog.currentDiscount) }}</small></div>
          <div class="voucher-new"><span>Phiếu mới</span><b>{{ betterVoucherDialog.nextCode }}</b><small>Giảm: {{ money(betterVoucherDialog.nextDiscount) }}</small></div>
        </div>
        <div class="voucher-saving"><span>Bạn tiết kiệm thêm</span><strong>+{{ money(betterVoucherDialog.saving) }}</strong></div>
        <dl class="voucher-totals"><div><dt>Tổng tiền hàng</dt><dd>{{ money(betterVoucherDialog.subtotal) }}</dd></div><div><dt>Giảm giá với phiếu mới</dt><dd>−{{ money(betterVoucherDialog.nextDiscount) }}</dd></div><div v-if="order.delivery.enabled"><dt>Phí vận chuyển</dt><dd>{{ money(betterVoucherDialog.shippingFee) }}</dd></div><div class="voucher-new-total"><dt>Tổng thanh toán với phiếu mới</dt><dd>{{ money(betterVoucherDialog.total) }}</dd></div></dl>
        
        <div class="ss-actions"><button class="ss-btn" @click="chooseCheckoutVoucher(false)">Giữ phiếu cũ</button><button class="ss-btn primary" @click="chooseCheckoutVoucher(true)">Dùng phiếu mới (tiết kiệm hơn)</button></div>
      </section>
    </div>

    <div v-if="checkoutDialog" class="ss-modal-bg">
      <section class="ss-modal pos-receipt" role="dialog" aria-modal="true" aria-label="Xác nhận thanh toán">
        <div class="ss-head"><h2>{{ checkoutDialog.bank > 0 ? 'Quét mã QR để thanh toán' : 'Xác nhận thanh toán' }}</h2></div>
        <template v-if="checkoutDialog.bank > 0"><div v-if="checkoutDialog.qr" class="bank-qr"><img v-if="!qrFailed" :src="checkoutDialog.qr.url" alt="QR chuyển khoản theo hóa đơn đang chọn" @error="qrFailed = true" /><p v-else role="alert">Không tải được QR. Đóng và mở lại để thử lại hoặc đối chiếu thông tin tài khoản bên dưới.</p><dl class="receipt-details"><dt>Ngân hàng (BIN)</dt><dd>{{ bankConfig.label }} ({{ bankConfig.bank }})</dd><dt>Số tài khoản</dt><dd>{{ bankConfig.account }}</dd><dt>Chủ tài khoản</dt><dd>{{ bankConfig.name }}</dd><dt>Số tiền</dt><dd>{{ money(checkoutDialog.bank) }}</dd><dt>Nội dung</dt><dd>{{ checkoutDialog.qr.memo }}</dd></dl></div><p v-else class="qr-unavailable" role="status">Chưa cấu hình tài khoản nhận tiền hoặc số tiền chưa phù hợp QR VND. Đối chiếu tài khoản nhận trước khi xác nhận chuyển khoản thủ công.</p><p>Mã hóa đơn: <b>{{ checkoutDialog.invoiceCode }}</b></p></template>
        <p>Tổng cần trả: <b>{{ money(checkoutDialog.total) }}</b></p>
        <p v-for="(p, index) in checkoutDialog.payload.payments" :key="index">{{ methods.find(m => m.id === p.methodId)?.name }}: {{ money(p.amount) }}</p>
        <p>Tiền mặt khách đưa: {{ money(checkoutDialog.payload.paidAmount) }} · Tiền thừa: {{ money(checkoutDialog.change) }}</p>
        <template v-if="checkoutDialog.bank > 0">
          <p>Kiểm tra khoản tiền đã nhận trong tài khoản ngân hàng trước khi xác nhận. Hệ thống ghi nhận xác nhận thủ công của nhân viên.</p>
          <label><input type="checkbox" v-model="checkoutDialog.confirmed" :disabled="busy" /> Tôi đã kiểm tra đủ {{ money(checkoutDialog.bank) }} tiền chuyển khoản.</label>
        </template>
        <div class="ss-actions"><button class="ss-btn" :disabled="busy" @click="checkoutDialog = null">Quay lại</button>
          <button class="ss-btn primary" :disabled="busy || checkoutDialog.bank > 0 && !checkoutDialog.confirmed" @click="confirmPayment">{{ busy ? 'Đang xử lý...' : checkoutDialog.bank > 0 ? 'Đã nhận tiền' : 'Thanh toán' }}</button></div>
      </section>
    </div>
    <div v-if="completedReceipt" class="ss-modal-bg">
      <section class="ss-modal pos-receipt" role="dialog" aria-modal="true" aria-label="Thanh toán thành công">
        <h2>Đã thanh toán {{ completedReceipt.invoiceCode }}</h2>
        <p>{{ completedReceipt.deliveryEnabled ? 'Đã thanh toán · Giao hàng' : 'Hoàn thành · Nhận tại quầy' }}</p>
        <dl class="receipt-details">
          <dt>Tiền hàng</dt><dd>{{ money(completedReceipt.subtotal) }}</dd><dt>Giảm phiếu</dt><dd>{{ money(completedReceipt.discount) }}</dd>
          <template v-if="completedReceipt.deliveryEnabled"><dt>Vận chuyển</dt><dd>{{ money(completedReceipt.shippingFee) }}</dd></template><dt>Tổng thanh toán</dt><dd>{{ money(completedReceipt.total) }}</dd>
          <dt>Tiền thừa</dt><dd>{{ money(completedReceipt.change) }}</dd>
        </dl>
        <p v-for="p in completedReceipt.payments" :key="p.transactionCode">{{ p.methodName }}: {{ money(p.amount) }}</p>
        <div class="ss-actions"><button class="ss-btn" @click="completedReceipt = null">Tiếp tục bán</button><button class="ss-btn" @click="openInvoice()">Xem chi tiết hóa đơn</button></div>
      </section>
    </div>
  </AdminLayout>
</template>

<style scoped>
.shipping-fee{display:flex;align-items:center;gap:8px}

.selected-customer{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:12px;margin-top:10px;background:#f5f9ff;border:1px solid var(--ss-border);border-radius:10px}.selected-customer small{display:block;color:var(--ss-muted);margin-top:4px}
.buy-more-card{border:1px solid #f2e4b3;background:#fffdf5;border-radius:12px;padding:12px;margin:14px 0}.buy-more-card h3{display:flex;justify-content:space-between;font-size:13px;color:#138366}.buy-more-card h3 small{font-size:10px;color:#b57b0c}.buy-more-item{background:white;border-radius:8px;padding:10px;margin-top:8px;font-size:12px}.buy-more-item>div{display:flex;justify-content:space-between;gap:8px;margin-bottom:6px}.buy-more-item .saving{color:#149a78}

.cash-error{color:#c63249;font-size:11px;line-height:1.5}
.pos{grid-template-columns:minmax(0,1fr) clamp(390px,30vw,440px)!important;gap:18px!important}.pos .ss-card{border:1px solid #e3eaf0;box-shadow:0 3px 14px #18384d06;border-radius:16px;padding:17px}.pos .ss-head{margin-bottom:12px}.pos .ss-head h2{font-size:15px}.pos .ss-table th{background:#f6f8fb;font-size:11px;padding:13px 10px;white-space:nowrap}.pos .ss-table td{padding:14px 10px;vertical-align:middle}.pos .ss-table .cart-product-name .ss-strong{white-space:normal;overflow-wrap:anywhere;min-width:130px;display:block}.pos .grow{min-height:390px}.right{gap:14px}.right .ss-card{gap:12px}.voucher-card{padding:15px!important}.bank-confirmed{padding:12px;border:1px solid #b8e3d3;background:#f0faf5;color:#167253;border-radius:10px;font-size:12px;line-height:1.6}.pos .btns{margin-top:8px;position:sticky;bottom:0;background:white;padding:12px 0 0}.pos .btns .ss-btn{min-height:42px;font-weight:650}.pos .delivery-card{background:#fff}.pos .qty button{width:25px}.pos .sale-price{font-size:13px}.pos .kv{font-size:12px}.pos .payable-row{font-size:14px}.pos .empty{padding:90px 0}.pos .tabs button{border-radius:9px;min-height:36px}
@media(max-width:1150px){.pos{grid-template-columns:minmax(0,1fr) 370px!important}.pos .ss-card{padding:13px}}@media(max-width:950px){.pos{grid-template-columns:1fr!important}.pos .btns{position:static}.right{max-height:none}}

.bank-qr{display:grid;justify-items:center}.bank-qr>img{width:280px;max-width:100%;height:auto}.bank-qr .receipt-details{width:100%;margin:10px 0}.qr-unavailable{padding:12px;background:#fff7e8;border-radius:9px;font-size:12px}
.pos-photo{position:relative;width:64px;height:64px;border:1px solid var(--ss-border);border-radius:9px;background:var(--ss-surface);display:grid;place-items:center;overflow:hidden}.pos-photo img{width:100%;height:100%;object-fit:cover}.pos-photo>i{font-size:24px;color:var(--ss-muted)}.photo-discount{position:absolute;top:0;right:0;background:#e73355;color:white;font-size:10px;font-weight:700;padding:3px 4px;border-radius:0 0 0 5px}.sale-price{color:#d32c50}.sku{display:block;color:var(--ss-muted);font-size:10px}.pos-filters>label{flex:1;min-width:120px;font-size:11px;color:var(--ss-muted)}.pos-filters{flex-wrap:wrap}.delivery-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.delivery-grid label{display:grid;gap:7px;font-size:11px;color:var(--ss-muted)}.delivery-grid strong{font-size:12px;font-weight:500;color:var(--ss-ink);padding:10px;background:var(--ss-surface);border-radius:7px;overflow-wrap:anywhere}.address-line{grid-column:1/-1}.delivery-card h3,.delivery-section h3{font-size:13px}.delivery-toggle{font-size:11px;white-space:nowrap}.delivery-modal{max-width:590px;max-height:90vh;overflow-y:auto;gap:16px}.delivery-section{padding:16px;border:1px solid var(--ss-border);border-radius:14px;display:grid;gap:14px}.address-choice{display:flex;align-items:flex-start;gap:9px;font-size:12px;padding:12px;border:1px solid var(--ss-border);border-radius:10px}.voucher-card{background:#eefaf5;border:1px solid #d1eee2;border-radius:12px;padding:13px;display:grid;gap:10px;font-size:11px;color:#208969}.voucher-card>div{display:flex;align-items:center;justify-content:space-between;gap:6px}.voucher-card .ss-icon-btn{width:22px;height:22px}.voucher-badge{display:inline-block;background:#d0f3e6;color:#208969;font-size:10px;padding:3px 6px;border-radius:4px;font-weight:700}.payable-row{border-top:1px solid var(--ss-border);padding-top:14px;font-size:14px}.payable-row b{color:var(--ss-primary)}.kv.reduction-row b{color:#208969}.right>.ss-card{border-radius:15px}.kv{padding:9px 0;border-bottom:1px solid var(--ss-border)}

.pos-thumb{width:54px;height:54px;object-fit:cover;border-radius:8px}.pos-filters{display:flex;gap:12px;margin:12px 0}.pos-save-actions{display:flex;align-items:center;gap:8px;color:var(--ss-muted)}
.pos-price-old{display:block;opacity:.6;font-size:10px}.pos-discount{display:inline-block;margin-left:5px;padding:2px 5px;border-radius:5px;background:#e7f5ff;color:#147fb4;font-size:10px;font-weight:700}
.pos-receipt{max-width:570px;overflow-y:auto}.pos-receipt>.ss-head,.pos-receipt>.ss-actions{flex-shrink:0}.receipt-details{display:grid;grid-template-columns:minmax(95px,140px) minmax(0,1fr);gap:8px 16px;margin:20px 0;flex-shrink:0}.receipt-details dt{font-weight:500;color:var(--ss-muted)}.receipt-details dd{margin:0;overflow-wrap:anywhere;font-weight:600}

.pos { display: grid; grid-template-columns: minmax(0, 1fr) 320px; gap: 16px; align-items: start; }
.grow { min-height: 520px; }
.ss-count.strong { font-weight: 600; color: var(--ss-muted); }
.muted-center { text-align: center; color: var(--ss-faint); font-size: 11px; padding: 4px 0; }
.empty { text-align: center; color: var(--ss-faint); padding: 90px 0; }
.tabs { display: flex; gap: 8px; flex-wrap: wrap; }
.tabs button { height: 34px; padding: 0 8px 0 14px; border: 1px solid var(--ss-border); border-radius: 999px; background: #fff; color: var(--ss-muted); font-size: 11.5px; font-weight: 600; display: inline-flex; align-items: center; gap: 6px; cursor: pointer; }
.tabs button.on { background: var(--ss-primary-soft); border-color: var(--ss-primary); color: var(--ss-primary); }
.tabs button b { min-width: 18px; height: 18px; border-radius: 9px; background: #fff; display: grid; place-items: center; font-size: 10px; padding: 0 5px; }
.tabs button i { font-size: 14px; border-radius: 50%; }
.tabs button i:hover { background: rgba(40, 121, 227, .15); }
.tight { gap: 10px; }
.side-title { font-size: 13px; font-weight: 700; color: var(--ss-text); }
.cust { display: flex; gap: 8px; }
.ss-icon-btn.sq { width: 40px; height: 40px; flex-basis: 40px; border-radius: 10px; background: var(--ss-primary-soft); color: var(--ss-primary); border-color: var(--ss-primary-soft); }
.kv { display: flex; justify-content: space-between; gap: 10px; font-size: 11px; color: var(--ss-muted); }
.kv b { color: var(--ss-ink); font-weight: 700; }
.btns { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.qty { display: inline-flex; align-items: center; border: 1px solid var(--ss-border); border-radius: 8px; overflow: hidden; }
.qty button { width: 26px; height: 28px; border: 0; background: var(--ss-surface); color: var(--ss-text); cursor: pointer; font-weight: 700; }
.qty button:hover { background: var(--ss-primary-soft); color: var(--ss-primary); }
.qty input { width: 36px; height: 28px; border: 0; text-align: center; font-size: 12px; outline: none; }
@media (max-width: 1100px) { .pos { grid-template-columns: 1fr; } }
</style>

<style scoped>
.shipping-fee{display:flex;align-items:center;gap:8px}

.selected-customer{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:12px;margin-top:10px;background:#f5f9ff;border:1px solid var(--ss-border);border-radius:10px}.selected-customer small{display:block;color:var(--ss-muted);margin-top:4px}
.buy-more-card{border:1px solid #f2e4b3;background:#fffdf5;border-radius:12px;padding:12px;margin:14px 0}.buy-more-card h3{display:flex;justify-content:space-between;font-size:13px;color:#138366}.buy-more-card h3 small{font-size:10px;color:#b57b0c}.buy-more-item{background:white;border-radius:8px;padding:10px;margin-top:8px;font-size:12px}.buy-more-item>div{display:flex;justify-content:space-between;gap:8px;margin-bottom:6px}.buy-more-item .saving{color:#149a78}

.cust .ss-input{min-width:0;flex:1}.cust .sq{flex-shrink:0}
.customer-results{border:1px solid var(--ss-border);border-radius:10px;max-height:260px;overflow:auto;background:white}
.customer-results-head{display:flex;align-items:center;justify-content:space-between;padding:8px 12px;background:var(--ss-surface);font-size:12px}
.customer-results p{margin:0;padding:12px;font-size:12px;color:var(--ss-muted)}
.customer-result{display:flex;flex-direction:column;gap:5px;width:100%;padding:11px 12px;text-align:left;border:0;border-top:1px solid var(--ss-border);background:white;color:var(--ss-ink);cursor:pointer;overflow-wrap:anywhere}
.customer-result:hover,.customer-result:focus-visible{background:#eef6ff}.customer-result strong{font-size:13px}.customer-result small{font-size:12px;color:var(--ss-muted)}
.pos .ss-table .cart-product-name{vertical-align:middle}
.voucher-selector{display:grid;grid-template-columns:max-content minmax(0,1fr);align-items:center;gap:8px 12px}
.voucher-selector .ss-label{margin:0;white-space:nowrap}.voucher-selector .ss-select{min-width:0;width:100%}
.voucher-selector .ss-hint{grid-column:1/-1}
.pos .payable-row>span{font-weight:650;color:var(--ss-ink)}
.delivery-toggle{display:inline-flex;align-items:center;gap:7px;position:relative;color:#208969;cursor:pointer}
.delivery-toggle input{position:absolute;opacity:0;width:1px;height:1px}
.delivery-switch-track{display:inline-block;position:relative;width:34px;height:19px;border-radius:999px;background:#cbd5e1;transition:background .15s;flex-shrink:0}
.delivery-switch-track::after{content:'';position:absolute;left:2px;top:2px;width:15px;height:15px;background:white;border-radius:50%;box-shadow:0 1px 3px #0002;transition:transform .15s}
.delivery-toggle input:checked+.delivery-switch-track{background:#20b891}
.delivery-toggle input:checked+.delivery-switch-track::after{transform:translateX(15px)}
.delivery-toggle input:focus-visible+.delivery-switch-track{outline:2px solid var(--ss-primary);outline-offset:3px}
.delivery-toggle input:disabled+.delivery-switch-track{opacity:.5;cursor:not-allowed}
.delivery-hint{padding:10px 12px;border-radius:8px;background:#fff7e8;color:#925b13;font-size:12px;line-height:1.6}
</style>

<style scoped>
.qty button:disabled,.qty button:disabled:hover{opacity:.4;color:var(--ss-muted);background:var(--ss-surface);cursor:not-allowed}
.ss-btn:disabled{opacity:.45;cursor:not-allowed}
</style>

<style scoped>
.shipping-provider-label { display: inline-flex; align-items: center; gap: 6px; flex-wrap: wrap; }
</style>

<style scoped>
.field-error { display: block; margin-top: 5px; color: #dc3545; font-size: 12px; line-height: 1.4; }
.delivery-modal .is-invalid { border-color: #dc3545; }
.delivery-modal .is-invalid:focus { box-shadow: 0 0 0 3px rgba(220, 53, 69, .1); }
.voucher-upgrade { width: min(480px, calc(100vw - 32px)); }
.voucher-upgrade h2 { display: flex; align-items: center; gap: 8px; font-size: 18px; }
.voucher-upgrade h2 i { color: #2484ed; }
.voucher-upgrade-notice, .voucher-saving { background: #edf6ff; color: #216cc0; border-radius: 8px; padding: 12px; }
.voucher-upgrade-notice { margin: 16px 0; font-size: 13px; }
.voucher-comparison { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin: 18px 0; }
.voucher-comparison > div { display: flex; flex-direction: column; align-items: flex-start; gap: 8px; min-width: 0; }
.voucher-comparison span, .voucher-comparison small { color: var(--ss-muted); font-size: 12px; }
.voucher-comparison b { background: #f0f4f9; color: #235390; padding: 5px 8px; border-radius: 5px; overflow-wrap: anywhere; }
.voucher-comparison .voucher-new b { background: #e8f8f1; color: #10855d; }
.voucher-comparison .voucher-new small { color: #10855d; font-weight: 700; }
.voucher-saving, .voucher-totals > div { display: flex; justify-content: space-between; gap: 16px; align-items: center; }
.voucher-saving { font-size: 13px; }
.voucher-totals { margin: 16px 0; font-size: 13px; }
.voucher-totals > div { padding: 9px 0; }
.voucher-totals dt { font-weight: 400; color: var(--ss-muted); }
.voucher-totals dd { margin: 0; font-weight: 700; white-space: nowrap; }
.voucher-totals .voucher-new-total { border-top: 1px solid var(--ss-border); margin-top: 6px; padding-top: 14px; }
.voucher-new-total dt, .voucher-new-total dd { color: #1679de; font-weight: 700; }
.voucher-upgrade .ss-actions { flex-wrap: wrap; }
@media (max-width: 480px) { .voucher-upgrade .ss-actions > button { width: 100%; } }
</style>
