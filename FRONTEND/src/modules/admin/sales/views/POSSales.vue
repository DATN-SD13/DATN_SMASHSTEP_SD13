<script setup>
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
import { useAddressApi } from '../../../../composables/useAddressApi'
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
let stockSyncTimer, stockPollTimer, stockSyncPromise = null, stockSyncPending = false
const locked = computed(() => deliveryLoading.value || Boolean(deliveryEditor.value) || busy.value || !ready.value || Boolean(checkoutDialog.value) || Boolean(pendingPayment.value) || recoveryBlocked.value)
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
    voucherCode: d.voucherCode ?? previous.voucherCode ?? '', paid: previous.paid ?? '',
    paymentMode: ['CASH', 'BANK'].includes(previous.paymentMode) ? previous.paymentMode : 'CASH',
    autoVoucher: previous.autoVoucher ?? true,
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
const canPay = computed(() => !locked.value && quoteReady.value && count.value > 0 && deliveryReady.value && !cartWarnings.value.length
  && (order.value.paymentMode === 'BANK' ? !!method('CHUYEN_KHOAN') && payable.value > 0
    : !!method('TIEN_MAT') && order.value.paid !== '' && Number.isFinite(Number(order.value.paid)) && paidNum.value >= cashDue.value && /^(?:0|[1-9]\d{0,15})(?:\.\d{1,2})?$/.test(String(order.value.paid))))
const selectedVoucher = computed(() => validVouchers.value.find(v => v.code.toLowerCase() === order.value?.voucherCode?.toLowerCase()))
const voucherLabel = v => v?.value == null ? '' : Number(v.type) === 1 ? `${Number(v.value)}%` : money(v.value)
const bestSelected = computed(() => selectedVoucher.value && Number(selectedVoucher.value.discount) === Number(validVouchers.value[0]?.discount))
const validVouchers = computed(() => order.value?.quote?.vouchers || [])

async function calculateQuote(suggest = true) {
  const target = order.value, version = ++quoteVersion
  if (!target?.items.length || !quoteInput.value.paymentMethodId || busy.value || pendingPayment.value || checkoutDialog.value || recoveryBlocked.value) return
  const fingerprint = quoteSignature.value
  target.quoteError = ''
  try {
    const result = await salesService.tinhTien(quoteInput.value)
    if (!alive || version !== quoteVersion || order.value?.id !== target.id || fingerprint !== quoteSignature.value) return
    if (bankAccepted.value && Number(bankAccepted.value.total) !== Number(result.total)) bankAccepted.value = null
    target.quote = result; target.quotedSignature = fingerprint
    if (result.selectedVoucherError) {
      target.voucherCode = ''; target.quoteError = `Đã bỏ phiếu: ${result.selectedVoucherError}`
      error.value = target.quoteError
      if (!busy.value) await saveOrder(target)
      // saveOrder clears the general error after a successful save; retain the reason for removing the voucher.
      error.value = `Đã bỏ phiếu: ${result.selectedVoucherError}`
      showError(error.value)
      return
    }
    const best = result.vouchers?.[0]
    if (!suggest || !best) return
    if (!target.voucherCode && target.autoVoucher) { await applyVoucher(best.code, target, true); return }
    const promptKey = `${fingerprint}|${best.code}|${best.discount}`
    if (Number(best.discount) > Number(result.discount) && best.code.toLowerCase() !== target.voucherCode.toLowerCase()
        && target.lastSuggestion !== promptKey) {
      target.lastSuggestion = promptKey
      confirmAction(`Phiếu ${best.code} giảm ${money(best.discount)}, tốt hơn phiếu đang dùng (${money(result.discount)}). Bạn muốn đổi phiếu?`, async () => {
        if (order.value?.id !== target.id || fingerprint !== quoteSignature.value || locked.value) return
        await applyVoucher(best.code)
      }, { title: 'Có phiếu giảm giá tốt hơn', confirmText: 'Đổi phiếu', cancelText: 'Giữ phiếu cũ' })
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
  if (!automatic) target.autoVoucher = Boolean(code)
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
  ++quoteVersion; clearTimeout(quoteTimer)
  if (order.value) { order.value.quote = null; order.value.quotedSignature = '' }
  if (!busy.value) quoteTimer = setTimeout(calculateQuote, 250)
})

const deliveryEditor = ref(null), deliveryError = ref(''), structuredAddress = ref(false)
const completeDelivery = d => !d?.enabled || Boolean(d.recipientName?.trim() && /^0\d{9}$/.test(d.recipientPhone)
  && d.address?.trim() && d.address.length <= 500 && d.shippingFee !== ''
  && Number.isFinite(Number(d.shippingFee)) && Number(d.shippingFee) >= 0)
const deliveryReady = computed(() => completeDelivery(order.value?.delivery))
const addressForm = ref({ province: '', ward: '', district: '', street: '' })
const { provinces, wards, loadingProvinces, loadingWards, addressError, changeProvince, resetAddress } = useAddressApi(addressForm)
let deliveryOrderId
function openDelivery() {
  if (stockSyncPromise) { stockSyncPromise.then(() => { if (alive) openDelivery() }); return }
  if (locked.value || !order.value) return
  deliveryOrderId = order.value.id
  deliveryEditor.value = JSON.parse(JSON.stringify(order.value.delivery))
  deliveryError.value = ''; structuredAddress.value = !deliveryEditor.value.address
  addressForm.value = { province: '', ward: '', district: '', street: '' }; resetAddress()
}
function closeDelivery() { if (!busy.value) deliveryEditor.value = null }
function selectDeliveryAddress(a) {
  Object.assign(deliveryEditor.value, { recipientName: a.receiverName, recipientPhone: a.receiverPhone, address: a.fullAddress })
}
async function toggleDelivery(enabled) {
  if (stockSyncPromise) await stockSyncPromise
  if (locked.value || !order.value) return
  const target = order.value
  if (!enabled) { target.delivery.enabled = false; await saveOrder(target, 'Đã chuyển sang nhận hàng tại quầy.'); return }
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
    await saveOrder({ ...target, delivery }, 'Bật giao hàng thành công. Đã điền thông tin người nhận và địa chỉ giao hàng.')
  } catch (e) { if (alive && order.value?.id === target.id) { error.value = message(e); showError(error.value, 'Không thể tải thông tin giao hàng') } }
  finally { deliveryLoading.value = false }
}
async function saveDelivery() {
  if (stockSyncPromise) await stockSyncPromise
  if (busy.value || !deliveryEditor.value || order.value?.id !== deliveryOrderId) return
  const candidate = JSON.parse(JSON.stringify(deliveryEditor.value))
  if (!candidate.addressId && structuredAddress.value) {
    const a = addressForm.value
    if (!a.province || !a.ward || !a.street.trim()) { deliveryError.value = 'Nhập đầy đủ tỉnh/thành, phường/xã và địa chỉ cụ thể.'; return }
    candidate.address = [a.street.trim(), a.ward, a.province].join(', ')
  }
  if (!candidate.recipientName.trim() || !/^0\d{9}$/.test(candidate.recipientPhone) || !candidate.address.trim()
      || candidate.address.length > 500 || candidate.shippingFee === '' || !Number.isFinite(Number(candidate.shippingFee)) || Number(candidate.shippingFee) < 0) {
    deliveryError.value = 'Kiểm tra người nhận, số điện thoại 10 số, địa chỉ và phí vận chuyển không âm.'; return
  }
  const target = { ...order.value, delivery: candidate }
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
async function openQr() {
  if (stockSyncPromise) await stockSyncPromise
  if (!(await readyCheckout())) return
  qrFailed.value = false
  checkoutDialog.value = { payload: checkoutPayload(), invoiceCode: order.value.name,
    total: payable.value, change: change.value, bank: transferred.value, context: paymentContext(), confirmed: false,
    qr: qrLink(bankConfig, transferred.value, order.value.name) }
}
async function pay() {
  if (stockSyncPromise) await stockSyncPromise
  if (!(await readyCheckout())) return
  if (order.value.paymentMode === 'BANK' && bankConfirmed.value) {
    const request = checkoutPayload(); request.transferConfirmed = true
    await performPayment({ employeeId: employeeId.value, invoiceCode: order.value.name, payload: request })
    return
  }
  await openQr()
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
    bankAccepted.value = { context: dialog.context, total: dialog.total }
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
    completedReceipt.value = receipt
    checkoutDialog.value = null
    clearCheckout()
    orders.value = orders.value.filter(o => o.id !== saved.payload.invoiceId)
    activeId.value = orders.value[0]?.id ?? null
    showSuccess(`Thanh toán hóa đơn ${receipt.invoiceCode} thành công. Tổng thanh toán: ${money(receipt.total)}.`)
    await Promise.allSettled([reloadOrders(), loadCatalog()])
  } catch (e) {
    checkoutDialog.value = null
    if (!e.response || e.response.status >= 500) {
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
  if (!alive || busy.value || syncingStock.value || deliveryLoading.value || deliveryEditor.value || pendingPayment.value || recoveryBlocked.value
      || (typeof document !== 'undefined' && document.visibilityState === 'hidden')) return
  stockSyncTimer = setTimeout(() => { stockSyncTimer = null; syncCartStock() }, 150)
}
watch([busy, deliveryLoading, deliveryEditor, pendingPayment, recoveryBlocked], () => { if (stockSyncPending) requestStockSync() })

async function syncCartStock() {
  if (stockSyncPromise) { stockSyncPending = true; return stockSyncPromise }
  if (!alive || !ready.value || busy.value || syncingStock.value || deliveryLoading.value || deliveryEditor.value
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
    if (next.length !== orders.value.length) changed = true
    if (changed) {
      orders.value = next
      if (!next.some(o => o.id === activeId.value)) activeId.value = next[0]?.id ?? null
      checkoutDialog.value = null; bankAccepted.value = null
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
    target.voucherCode = ''; target.quote = null; target.quotedSignature = ''; target.quoteError = ''; target.paid = ''
  }
  if (!target.items.length && !completeDelivery(target.delivery)) target.delivery.enabled = false
  if (!completeDelivery(target.delivery)) {
    error.value = 'Thông tin giao hàng chưa đầy đủ. Bấm Chỉnh sửa để bổ sung hoặc bỏ tích Giao hàng.'
    showError(error.value, 'Chưa thể lưu hóa đơn')
    return false
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
async function addItem(v) {
  if (stockSyncPromise) await stockSyncPromise
  if (locked.value || !order.value) return
  const cur = order.value.items.find(i => i.id === v.id)
  if (v.stock < 1) { error.value = 'Số lượng đã đạt tồn kho hiện tại'; showError(error.value, 'Không thể thêm sản phẩm'); return }
  if (cur) cur.qty++
  else order.value.items.push({ ...v, qty: 1, currentPrice: v.price, available: true })
  await saveOrder(order.value, `Thêm sản phẩm ${v.name || v.code || ''} vào giỏ hàng thành công.`)
}
async function setQty(i, n) {
  if (stockSyncPromise) await stockSyncPromise
  i = order.value?.items.find(item => item.id === i.id)
  if (!i) return
  if (locked.value) return
  const qty = Number(n)
  if (!Number.isInteger(qty) || qty < 1 || qty > 10000) { error.value = 'Số lượng phải là số nguyên từ 1 đến 10000'; showError(error.value, 'Số lượng không hợp lệ'); return }
  if (qty === i.qty) return
  i.qty = qty; await saveOrder(order.value, `Cập nhật số lượng sản phẩm ${i.name || i.code || ''} thành ${qty} thành công.`)
}
async function removeItem(code) {
  if (stockSyncPromise) await stockSyncPromise
  if (locked.value || !order.value) return
  const item = order.value.items.find(i => i.code === code)
  if (!item) return
  order.value.items = order.value.items.filter(i => i.code !== code)
  await saveOrder(order.value, `Xóa sản phẩm ${item.name || item.code} khỏi giỏ hàng thành công.`)
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
  if (locked.value || !order.value) return
  order.value.customerId = customer?.id ?? null
  choosingCustomer.value = false
  await changeCustomer()
}
watch([q, colorFilter, sizeFilter, categoryFilter, brandFilter], () => {
  catalogPage.value = 0; ++catalogVersion; clearTimeout(searchTimer); searchTimer = setTimeout(loadCatalog, 250)
})
watch(catalogPage, loadCatalog)
watch(customerSearch, () => {
  ++customerVersion; clearTimeout(customerTimer)
  customerResults.value = []; customerSearchError.value = ''; customerLoading.value = true
  choosingCustomer.value = true
  customerTimer = setTimeout(loadCustomers, 250)
})
onBeforeRouteLeave(async () => !busy.value && await saveOrder())
onMounted(async () => {
  try {
    const me = (await api.get('/auth/me')).data.data
    if (me.userType !== 'NHAN_VIEN' || !['ADMIN', 'STAFF'].includes(me.role)) throw new Error('Chỉ nhân viên được sử dụng POS')
    employeeId.value = me.userId
    employeeName.value = auth.user?.id === me.userId ? `${auth.user.code || ''} · ${auth.user.name || me.username}` : me.username
    await reloadOrders(); restoreCheckout(); ready.value = true
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
onBeforeUnmount(() => { if (typeof window !== 'undefined') { window.clearInterval(stockPollTimer); window.removeEventListener('focus', requestStockSync); window.removeEventListener('online', requestStockSync); document.removeEventListener('visibilitychange', requestStockSync) } clearTimeout(stockSyncTimer); alive = false; ++quoteVersion; ++addressVersion; clearTimeout(quoteTimer); ++catalogVersion; ++customerVersion; clearTimeout(searchTimer); clearTimeout(customerTimer) })
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
                  <td class="c"><div class="qty"><button :disabled="locked" @click="setQty(i, i.qty - 1)">−</button><input :disabled="locked" :value="i.qty" @change="setQty(i, $event.target.value)" /><button :disabled="locked" @click="setQty(i, i.qty + 1)">+</button></div></td>
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
          <div class="cust">
            <input class="ss-input" v-model="customerSearch" placeholder="Tìm theo tên, mã hoặc SĐT" :disabled="locked" @focus="choosingCustomer = true" @keydown.esc="choosingCustomer = false" aria-label="Tìm khách hàng theo tên, mã hoặc số điện thoại" />
            <button class="ss-icon-btn sq" :disabled="locked" title="Thêm khách hàng" @click="router.push('/khach-hang/them')"><i class="bi bi-person-plus"></i></button>
          </div>
          <div v-if="choosingCustomer && customerSearch.trim()" class="customer-results" aria-label="Kết quả tìm khách hàng">
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
          <select v-if="order" class="ss-select" v-model="order.customerId" @change="changeCustomer" :disabled="locked" aria-label="Khách hàng">
            <option :value="null">Khách lẻ</option>
            <option v-for="c in customers" :key="c.id" :value="c.id">{{ c.code || '—' }} · {{ c.name || '—' }} · {{ c.phone || '—' }}</option>
          </select>
          <div v-if="order" class="ss-hint" style="margin-top:8px">
            <template v-if="order.customerId == null">Khách vãng lai → <b>{{ order.delivery.enabled ? 'Giao hàng' : 'Nhận tại quầy' }}</b></template>
            <template v-else>Đã chọn khách hàng → <b>{{ order.delivery.enabled ? 'Giao hàng' : 'Nhận tại quầy' }}</b></template>
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
          <div class="kv" v-if="order.delivery.enabled"><span>Phí vận chuyển:</span><b>{{ money(order.delivery.shippingFee) }}</b></div>
          <div class="kv reduction-row"><span>Tiền giảm từ phiếu:</span><b>−{{ money(quoteReady ? order.quote.discount : 0) }}</b></div>
          <div class="kv payable-row"><span>Khách cần trả:</span><b>{{ money(payable) }}</b></div>
          <p v-if="order.quoteError" role="alert">{{ order.quoteError }}</p>


          <div class="ss-field"><span class="ss-label strong">Hình thức thanh toán</span>
            <div class="ss-radios" style="min-height:24px;gap:12px;flex-wrap:wrap">
              <label v-for="m in paymentChoices" :key="m.value"><input type="radio" :value="m.value" v-model="order.paymentMode" :disabled="locked" /> {{ m.name }}</label>
            </div>
          </div>
          <small v-if="order.paymentMode !== 'CASH' && !quoteReady" class="ss-hint">Thêm sản phẩm và hoàn tất thông tin giao hàng để tính số tiền chuyển khoản.</small>
          <button v-if="order.paymentMode !== 'CASH'" class="ss-btn" :disabled="!canPay" @click="openQr">{{ bankConfig ? 'Hiển thị QR chuyển khoản' : 'Xác nhận chuyển khoản thủ công' }}</button>
          <div v-if="order.paymentMode !== 'BANK'" class="ss-field"><span class="ss-label strong">Tiền mặt khách đưa</span>
            <input class="ss-input" type="text" inputmode="decimal" :value="cashDisplay" @input="inputCash" :disabled="locked" aria-label="Tiền mặt khách đưa" autocomplete="off" placeholder="Nhập số tiền khách đưa" /></div>
          <div v-else class="kv"><span>Cần chuyển khoản:</span><b>{{ money(payable) }}</b></div>
          <small v-if="cashError" class="cash-error" role="alert">{{ cashError }}</small>
          <p v-if="order.paymentMode === 'BANK' && bankConfirmed" class="bank-confirmed" role="status"><i class="bi bi-check-circle"></i> Đã xác nhận chuyển khoản, chưa hoàn tất hóa đơn. Bấm Thanh toán để hoàn tất.</p>
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
            <button class="ss-btn primary" :disabled="!canPay" @click="pay">Thanh toán</button>
          </div>
        </section>
        <section v-else class="ss-card"><p class="muted-center">Tạo hóa đơn chờ để bắt đầu bán hàng.</p></section>
      </aside>
    </main>

    <div v-if="deliveryEditor" class="ss-modal-bg" @click.self="closeDelivery"><section class="ss-modal delivery-modal" role="dialog" aria-modal="true" aria-labelledby="delivery-title"><div class="ss-head"><h2 id="delivery-title">Chỉnh sửa thông tin giao hàng</h2><span class="ss-spacer"></span><button class="ss-icon-btn" :disabled="busy" @click="closeDelivery" aria-label="Đóng">×</button></div><p v-if="deliveryError" role="alert">{{ deliveryError }}</p><section class="delivery-section"><h3>Thông tin liên hệ</h3><div class="delivery-grid"><label>Tên người nhận *<input class="ss-input" v-model="deliveryEditor.recipientName" maxlength="255" :disabled="busy" @input="deliveryEditor.addressId = null" /></label><label>Số điện thoại *<input class="ss-input" v-model="deliveryEditor.recipientPhone" maxlength="10" :disabled="busy" @input="deliveryEditor.addressId = null" /></label></div></section><section class="delivery-section"><h3>Địa chỉ giao hàng</h3><p v-if="addressLoading">Đang tải địa chỉ...</p><label v-for="a in addresses" :key="a.id" class="address-choice"><input type="radio" :value="a.id" v-model="deliveryEditor.addressId" :disabled="busy" @change="selectDeliveryAddress(a)" /><span><b>{{ a.receiverName }}</b> · {{ a.receiverPhone }} <small v-if="a.isDefault" class="voucher-badge">Mặc định</small><br />{{ a.fullAddress }}</span></label><label class="address-choice"><input type="radio" :value="null" v-model="deliveryEditor.addressId" :disabled="busy" /> Sử dụng địa chỉ khác</label><template v-if="!deliveryEditor.addressId"><label class="address-choice"><input type="checkbox" v-model="structuredAddress" :disabled="busy" /> Chọn tỉnh/thành và phường/xã</label><div v-if="structuredAddress" class="delivery-grid"><label>Tỉnh/Thành phố *<select class="ss-select" v-model="addressForm.province" :disabled="busy || loadingProvinces" @change="changeProvince"><option value="">Chọn tỉnh/thành</option><option v-for="a in provinces" :key="a.code" :value="a.name">{{ a.name }}</option></select></label><label>Phường/Xã *<select class="ss-select" v-model="addressForm.ward" :disabled="busy || loadingWards || !addressForm.province"><option value="">Chọn phường/xã</option><option v-for="a in wards" :key="a.code" :value="a.name">{{ a.name }}</option></select></label><label class="address-line">Địa chỉ cụ thể *<input class="ss-input" v-model="addressForm.street" :disabled="busy" maxlength="350" /></label><p v-if="addressError" role="alert">{{ addressError }}</p></div><label v-else>Địa chỉ đầy đủ *<textarea class="ss-textarea" v-model="deliveryEditor.address" :disabled="busy" maxlength="500" /></label></template></section><label>Phí vận chuyển (nhập thủ công)<input class="ss-input" v-model="deliveryEditor.shippingFee" type="number" min="0" step="1" :disabled="busy" /></label><div class="ss-actions"><button class="ss-btn" :disabled="busy" @click="closeDelivery">Hủy</button><button class="ss-btn primary" :disabled="busy" @click="saveDelivery">{{ busy ? 'Đang lưu...' : 'Lưu thay đổi' }}</button></div></section></div>

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
            <thead><tr><th>STT</th><th>Ảnh sản phẩm</th><th>Mã sản phẩm</th><th>Tên sản phẩm</th><th>Màu</th><th class="c">Size</th><th class="r">Tồn khả dụng</th><th class="r">Giá</th><th class="c"></th></tr></thead>
            <tbody>
              <tr v-for="(v, idx) in shown" :key="v.code">
                <td>{{ catalogPage * 10 + idx + 1 }}</td><td><ProductThumbnail :url="imageSources[v.id]" :item="badgeItem(v)" :alt="v.name" /></td><td :title="v.code"><span class="ss-code">{{ v.productCode }}</span><small class="sku">{{ v.code }}</small></td><td class="ss-strong">{{ v.name }}</td><td>{{ v.color }}</td><td class="c">{{ v.size }}</td>
                <td class="r">{{ v.stock }}</td><td class="r nowrap" :title="[v.campaignCode, v.campaignName].filter(Boolean).join(' · ')"><del v-if="v.discounted" class="pos-price-old">{{ money(v.originalPrice) }}</del><strong :class="{'sale-price': v.discounted}">{{ money(v.price) }}</strong></td>
                <td class="c"><button class="ss-btn sm" :disabled="locked || !order || v.stock < 1" @click="addItem(v)"><i class="bi bi-plus-lg"></i> Thêm</button></td>
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
        <p>{{ completedReceipt.status === 5 ? 'Hoàn thành · Nhận tại quầy' : 'Đã thanh toán · Giao hàng chờ xác nhận' }}</p>
        <dl class="receipt-details">
          <dt>Tiền hàng</dt><dd>{{ money(completedReceipt.subtotal) }}</dd><dt>Giảm phiếu</dt><dd>{{ money(completedReceipt.discount) }}</dd>
          <dt>Vận chuyển</dt><dd>{{ money(completedReceipt.shippingFee) }}</dd><dt>Tổng thanh toán</dt><dd>{{ money(completedReceipt.total) }}</dd>
          <dt>Tiền thừa</dt><dd>{{ money(completedReceipt.change) }}</dd>
        </dl>
        <p v-for="p in completedReceipt.payments" :key="p.transactionCode">{{ p.methodName }}: {{ money(p.amount) }}</p>
        <div class="ss-actions"><button class="ss-btn" @click="completedReceipt = null">Tiếp tục bán</button><button class="ss-btn" @click="openInvoice()">Xem chi tiết</button><button class="ss-btn primary" @click="openInvoice(true)">In hóa đơn</button></div>
      </section>
    </div>
  </AdminLayout>
</template>

<style scoped>
.cash-error{color:#c63249;font-size:11px;line-height:1.5}
.pos{grid-template-columns:minmax(0,1fr) clamp(390px,30vw,440px)!important;gap:18px!important}.pos .ss-card{border:1px solid #e3eaf0;box-shadow:0 3px 14px #18384d06;border-radius:16px;padding:17px}.pos .ss-head{margin-bottom:12px}.pos .ss-head h2{font-size:15px}.pos .ss-table th{background:#f6f8fb;font-size:11px;padding:13px 10px;white-space:nowrap}.pos .ss-table td{padding:14px 10px;vertical-align:middle}.pos .ss-table .cart-product-name .ss-strong{white-space:normal;overflow-wrap:anywhere;min-width:130px;display:block}.pos .grow{min-height:390px}.right{gap:14px}.right .ss-card{gap:12px}.voucher-card{padding:15px!important}.bank-confirmed{padding:12px;border:1px solid #b8e3d3;background:#f0faf5;color:#167253;border-radius:10px;font-size:12px;line-height:1.6}.pos .btns{margin-top:8px;position:sticky;bottom:0;background:white;padding:12px 0 0}.pos .btns .ss-btn{min-height:42px;font-weight:650}.pos .delivery-card{background:#fff}.pos .qty button{width:25px}.pos .sale-price{font-size:13px}.pos .kv{font-size:12px}.pos .payable-row{font-size:14px}.pos .empty{padding:90px 0}.pos .tabs button{border-radius:9px;min-height:36px}
@media(max-width:1150px){.pos{grid-template-columns:minmax(0,1fr) 370px!important}.pos .ss-card{padding:13px}}@media(max-width:950px){.pos{grid-template-columns:1fr!important}.pos .btns{position:static}.right{max-height:none}}

.bank-qr{display:grid;justify-items:center}.bank-qr>img{width:280px;max-width:100%;height:auto}.bank-qr .receipt-details{width:100%;margin:10px 0}.qr-unavailable{padding:12px;background:#fff7e8;border-radius:9px;font-size:12px}
.pos-photo{position:relative;width:64px;height:64px;border:1px solid var(--ss-border);border-radius:9px;background:var(--ss-surface);display:grid;place-items:center;overflow:hidden}.pos-photo img{width:100%;height:100%;object-fit:cover}.pos-photo>i{font-size:24px;color:var(--ss-muted)}.photo-discount{position:absolute;top:0;right:0;background:#e73355;color:white;font-size:10px;font-weight:700;padding:3px 4px;border-radius:0 0 0 5px}.sale-price{color:#d32c50}.sku{display:block;color:var(--ss-muted);font-size:10px}.pos-filters>label{flex:1;min-width:120px;font-size:11px;color:var(--ss-muted)}.pos-filters{flex-wrap:wrap}.delivery-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.delivery-grid label{display:grid;gap:7px;font-size:11px;color:var(--ss-muted)}.delivery-grid strong{font-size:12px;font-weight:500;color:var(--ss-ink);padding:10px;background:var(--ss-surface);border-radius:7px;overflow-wrap:anywhere}.address-line{grid-column:1/-1}.delivery-card h3,.delivery-section h3{font-size:13px}.delivery-toggle{font-size:11px;white-space:nowrap}.delivery-modal{max-width:590px;max-height:90vh;overflow-y:auto;gap:16px}.delivery-section{padding:16px;border:1px solid var(--ss-border);border-radius:14px;display:grid;gap:14px}.address-choice{display:flex;align-items:flex-start;gap:9px;font-size:12px;padding:12px;border:1px solid var(--ss-border);border-radius:10px}.voucher-card{background:#eefaf5;border:1px solid #d1eee2;border-radius:12px;padding:13px;display:grid;gap:10px;font-size:11px;color:#208969}.voucher-card>div{display:flex;align-items:center;justify-content:space-between;gap:6px}.voucher-card .ss-icon-btn{width:22px;height:22px}.voucher-badge{display:inline-block;background:#d0f3e6;color:#208969;font-size:10px;padding:3px 6px;border-radius:4px;font-weight:700}.payable-row{border-top:1px solid var(--ss-border);padding-top:14px;font-size:14px}.payable-row b{color:var(--ss-primary)}.reduction-row b{color:#208969}.right>.ss-card{border-radius:15px}.kv{padding:9px 0;border-bottom:1px solid var(--ss-border)}

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
