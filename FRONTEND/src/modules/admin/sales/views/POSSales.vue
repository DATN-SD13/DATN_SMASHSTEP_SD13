<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { money } from '../../../../utils/paging'
import { confirmAction, showSuccess } from '../../../../utils/feedback'
import salesService from '../services/salesService'
import { useRouter } from 'vue-router'
const router = useRouter()
const completedReceipt = ref(null)
import api from '../../../../utils/api'
import customerService from '../../customer/services/customerService'
import employeeService from '../../employee/services/employeeService'

const MAX_ORDERS = 5
const PENDING_CHECKOUT_KEY = 'smashstep.pos.pending-checkout.v1'

/* ---------- Danh sách biến thể có thể bán ---------- */
const catalog = ref([])
const catalogPage = ref(0)
const catalogPages = ref(0)
const loading = ref(false)
const paying = ref(false)
const recoveryBlocked = ref(false)
const error = ref('')
const customers = ref([])
const employees = ref([])
const methods = ref([])
const voucherSupported = ref(false)
const employeeId = ref(null)
const customerSearch = ref('')
let catalogVersion = 0
let customerVersion = 0
let quoteVersion = 0
let searchTimer, customerTimer, quoteTimer
const message = e => e.response?.data?.message || 'Không thể kết nối máy chủ. Hãy thử lại.'

/* ---------- Hóa đơn chờ ---------- */
let seq = 0
const newOrder = () => ({ id: ++seq, requestId: crypto.randomUUID(), name: `HĐ chờ ${seq}`,
  items: [], customerId: null, paymentMethodId: methods.value[0]?.id ?? null, voucherCode: '',
  paid: '', note: '', quote: null, quotedSignature: '', quoteError: '' })
const orders = ref([])
const activeId = ref(null)
const order = computed(() => orders.value.find(o => o.id === activeId.value) || null)
const locked = computed(() => paying.value || recoveryBlocked.value || Boolean(order.value?.retryPayload))

const positiveId = value => Number.isSafeInteger(value) && value > 0
function validRecoveryPayload(payload) {
  return payload && typeof payload === 'object' && !Array.isArray(payload)
    && typeof payload.requestId === 'string'
    && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(payload.requestId)
    && positiveId(payload.employeeId) && positiveId(payload.paymentMethodId)
    && (payload.customerId == null || positiveId(payload.customerId))
    && Number.isFinite(payload.paidAmount) && payload.paidAmount >= 0
    && (payload.voucherCode == null || (typeof payload.voucherCode === 'string' && payload.voucherCode.length <= 50))
    && (payload.note == null || (typeof payload.note === 'string' && payload.note.length <= 1000))
    && Array.isArray(payload.items) && payload.items.length > 0 && payload.items.length <= 50
    && payload.items.every(item => item && positiveId(item.variantId) && Number.isInteger(item.quantity)
      && item.quantity > 0 && item.quantity <= 10000 && Number.isFinite(item.unitPrice) && item.unitPrice >= 0)
    && new Set(payload.items.map(item => item.variantId)).size === payload.items.length
}

function restorePendingCheckout() {
  try {
    const stored = sessionStorage.getItem(PENDING_CHECKOUT_KEY)
    if (!stored) return false
    const pending = JSON.parse(stored)
    if (pending?.version !== 1 || !validRecoveryPayload(pending.payload)) throw new Error('Invalid recovery data')
    const restored = newOrder()
    const payload = pending.payload
    restored.requestId = payload.requestId
    restored.customerId = payload.customerId ?? null
    restored.paymentMethodId = payload.paymentMethodId
    restored.voucherCode = payload.voucherCode ?? ''
    restored.note = payload.note ?? ''
    restored.paid = payload.paidAmount
    restored.retryPayload = payload
    restored.items = payload.items.map(item => {
      const saved = Array.isArray(pending.cart) ? pending.cart.find(row => row?.id === item.variantId) : null
      return { id: item.variantId, code: saved?.code || `CTSP-${item.variantId}`,
        name: saved?.name || `Biến thể #${item.variantId}`, color: saved?.color || '—',
        size: saved?.size || '—', price: item.unitPrice, qty: item.quantity,
        stock: Number.isInteger(saved?.stock) && saved.stock >= item.quantity ? saved.stock : item.quantity }
    })
    orders.value = [restored]
    activeId.value = restored.id
    employeeId.value = payload.employeeId
    error.value = 'Có phiên thanh toán chưa xác định kết quả. Kiểm tra lại đúng yêu cầu đã lưu trước khi bán tiếp.'
    return true
  } catch {
    recoveryBlocked.value = true
    error.value = 'Không thể khôi phục thông tin thanh toán. Hãy kiểm tra hóa đơn đã ghi trước khi tiếp tục; thanh toán mới đang bị khóa.'
    return true
  }
}

function savePendingCheckout(target, payload) {
  if (!validRecoveryPayload(payload)) throw new Error('Invalid checkout payload')
  const existing = sessionStorage.getItem(PENDING_CHECKOUT_KEY)
  if (existing) {
    const pending = JSON.parse(existing)
    if (pending?.version !== 1 || !validRecoveryPayload(pending.payload)
      || pending.payload.requestId !== payload.requestId) throw new Error('Another checkout is pending')
  }
  sessionStorage.setItem(PENDING_CHECKOUT_KEY, JSON.stringify({ version: 1, payload, cart: target.items }))
}

function clearPendingCheckout(requestId) {
  try {
    const existing = sessionStorage.getItem(PENDING_CHECKOUT_KEY)
    if (existing && JSON.parse(existing)?.payload?.requestId === requestId) sessionStorage.removeItem(PENDING_CHECKOUT_KEY)
    return true
  } catch {
    recoveryBlocked.value = true
    error.value = 'Không thể cập nhật thông tin khôi phục thanh toán. Hãy tải lại trang để kiểm tra hóa đơn trước khi tiếp tục.'
    return false
  }
}

function addOrder() {
  if (locked.value || orders.value.length >= MAX_ORDERS) return
  const o = newOrder()
  orders.value.push(o)
  activeId.value = o.id
}
function removeOrder(id) {
  if (locked.value) return
  orders.value = orders.value.filter(o => o.id !== id)
  if (activeId.value === id) activeId.value = orders.value[0]?.id ?? null
}
if (!restorePendingCheckout()) addOrder()

/* ---------- Giỏ hàng ---------- */
const count = computed(() => order.value?.items.reduce((s, i) => s + i.qty, 0) || 0)
const total = computed(() => order.value?.items.reduce((s, i) => s + i.qty * i.price, 0) || 0)
function addItem(v) {
  if (locked.value) return
  if (!order.value) addOrder()
  const cur = order.value.items.find(i => i.code === v.code)
  if (cur) { if (cur.qty < cur.stock) cur.qty++ }
  else order.value.items.push({ ...v, qty: 1 })
}
const setQty = (i, n) => { if (!locked.value) i.qty = Math.min(i.stock, Math.max(1, Math.floor(Number(n) || 1))) }
const removeItem = (code) => { if (!locked.value && order.value) order.value.items = order.value.items.filter(i => i.code !== code) }

/* ---------- Thanh toán ---------- */
const paidNum = computed(() => Number(order.value?.paid) || 0)
const quoteInput = computed(() => order.value ? {
  customerId: order.value.customerId || null, employeeId: employeeId.value,
  paymentMethodId: order.value.paymentMethodId, voucherCode: order.value.voucherCode.trim(),
  items: order.value.items.map(i => ({ variantId: i.id, quantity: i.qty, unitPrice: i.price }))
} : null)
const signature = computed(() => JSON.stringify({ payload: quoteInput.value,
  prices: order.value?.items.map(i => [i.id, i.originalPrice, i.price, i.discountPercent, i.campaignCode]) }))
const quoteReady = computed(() => order.value?.quote && order.value.quotedSignature === signature.value)
const payable = computed(() => quoteReady.value ? Number(order.value.quote.total) : total.value)
const change = computed(() => Math.max(0, paidNum.value - payable.value))
const canPay = computed(() => !locked.value && quoteReady.value && count.value > 0 && paidNum.value >= payable.value)
async function calculateQuote() {
  const target = order.value
  const version = ++quoteVersion
  if (!target || !target.items.length || !employeeId.value || !target.paymentMethodId) return
  const fingerprint = signature.value
  target.quoteError = ''
  try {
    const result = await salesService.quote({ ...quoteInput.value, requestId: target.requestId })
    if (version === quoteVersion && order.value?.id === target.id && fingerprint === signature.value) {
      target.quote = result; target.quotedSignature = fingerprint
    }
  } catch (e) {
    if (version === quoteVersion && order.value?.id === target.id && fingerprint === signature.value) {
      target.quote = null; target.quoteError = message(e)
    }
  }
}
function pay() {
  if (!canPay.value) return
  const target = order.value
  confirmAction(`Xác nhận thanh toán ${money(payable.value)}?`, async () => {
    if (paying.value || order.value?.id !== target.id || !canPay.value) return
    await performPayment(target, { ...quoteInput.value, requestId: target.requestId,
      paidAmount: Number(target.paid), note: target.note })
  }, { title: 'Thanh toán hóa đơn', confirmText: 'Thanh toán' })
}
async function performPayment(target, payload) {
    paying.value = true; error.value = ''
    try {
      savePendingCheckout(target, payload)
    } catch {
      recoveryBlocked.value = true
      error.value = 'Không thể lưu thông tin khôi phục. Thanh toán chưa được gửi; hãy kiểm tra khả năng lưu phiên và tải lại trang.'
      paying.value = false
      return
    }
    try {
      const receipt = await salesService.checkout(payload)
      completedReceipt.value = { ...receipt, paidAmount: receipt.paidAmount ?? payload.paidAmount }
      clearPendingCheckout(payload.requestId)
      orders.value = orders.value.filter(o => o.id !== target.id)
      activeId.value = orders.value[0]?.id ?? null
      showSuccess(`Đã thanh toán hóa đơn ${receipt.invoiceCode}: ${money(receipt.total)}. Tiền thừa: ${money(receipt.change)}`)
      await loadCatalog()
    } catch (e) {
      if (!e.response || e.response.status >= 500) {
        target.retryPayload = payload
        error.value = 'Chưa xác định được kết quả thanh toán. Bấm kiểm tra lại thanh toán để gửi lại đúng yêu cầu cũ; hóa đơn sẽ chỉ được ghi một lần.'
      } else {
        if (clearPendingCheckout(payload.requestId)) {
          target.retryPayload = null
          error.value = message(e)
        }
      }
      target.quote = null
      await loadCatalog()
    } finally { paying.value = false }
}
async function retryPayment() {
  if (paying.value || recoveryBlocked.value || !order.value?.retryPayload) return
  await performPayment(order.value, order.value.retryPayload)
}
async function refreshCart() {
  if (!order.value?.items.length || locked.value) return
  const target = order.value
  target.quote = null; target.quotedSignature = ''; ++quoteVersion
  paying.value = true; error.value = ''
  try {
    const fresh = await Promise.all(target.items.map(i => salesService.catalogItem(i.id)))
    for (const item of target.items) {
      const current = fresh.find(v => v.id === item.id)
      Object.assign(item, current, { qty: Math.min(item.qty, current.stock) })
    }
    await calculateQuote()
  } catch (e) { error.value = message(e) }
  finally { paying.value = false }
}
function cancelOrder() {
  if (!order.value || locked.value) return
  const id = order.value.id
  if (order.value.items.length) confirmAction('Hủy giỏ hàng chờ này?', () => removeOrder(id))
  else removeOrder(id)
}

/* ---------- Modal chọn sản phẩm ---------- */
const picking = ref(false)
const q = ref('')
const shown = computed(() => catalog.value)
function openPicker() { if (locked.value) return; if (!order.value) addOrder(); q.value = ''; picking.value = true; catalogPage.value = 0; loadCatalog() }
async function loadCatalog() {
  const version = ++catalogVersion
  loading.value = true
  try {
    const result = await salesService.catalog({ page: catalogPage.value, size: 10, keyword: q.value.trim() })
    if (version !== catalogVersion) return
    catalog.value = result.content || []; catalogPages.value = result.totalPages || 0
  } catch (e) { if (version === catalogVersion) error.value = message(e) }
  finally { if (version === catalogVersion) loading.value = false }
}
async function loadCustomers() {
  const version = ++customerVersion
  try {
    const result = await customerService.getCustomers({ page: 1, size: 20, trangThai: 1, tuKhoa: customerSearch.value.trim() })
    if (version === customerVersion) {
      const found = result.content || []
      const selectedIds = new Set(orders.value.map(current => current.customerId).filter(Boolean))
      // Keep selected customers visible when searching for customers of another pending order.
      customers.value = [...customers.value.filter(customer => selectedIds.has(customer.id)
        && !found.some(match => match.id === customer.id)), ...found]
    }
  } catch (e) { if (version === customerVersion) error.value = message(e) }
}
watch(q, () => { catalogPage.value = 0; ++catalogVersion; clearTimeout(searchTimer); searchTimer = setTimeout(loadCatalog, 250) })
watch(catalogPage, loadCatalog)
watch(customerSearch, () => { ++customerVersion; clearTimeout(customerTimer); customerTimer = setTimeout(loadCustomers, 250) })
watch([activeId, signature], () => {
  ++quoteVersion; clearTimeout(quoteTimer)
  if (order.value) { order.value.quoteError = ''; order.value.quotedSignature = '' }
  quoteTimer = setTimeout(calculateQuote, 250)
})
onMounted(async () => {
  try {
    const [employeePage, paymentMethods, voucherCapabilities] = await Promise.all([
      employeeService.getEmployees({ page: 1, size: 100, trangThai: 1 }), salesService.paymentMethods(), api.get('/phieu-giam-gia/capabilities')
    ])
    voucherSupported.value = voucherCapabilities.data?.data?.formSupported === true
    employees.value = employeePage.content || []; methods.value = paymentMethods
    employeeId.value = order.value?.retryPayload?.employeeId ?? employees.value[0]?.id ?? null
    for (const current of orders.value) current.paymentMethodId ??= methods.value[0]?.id ?? null
    await Promise.all([loadCatalog(), loadCustomers()])
    if (!employeeId.value || !methods.value.length) error.value = 'Cần nhân viên và phương thức thanh toán đang hoạt động để bán hàng.'
  } catch (e) { error.value = message(e) }
})
onBeforeUnmount(() => { ++quoteVersion; ++catalogVersion; ++customerVersion; clearTimeout(searchTimer); clearTimeout(customerTimer); clearTimeout(quoteTimer) })
</script>

<template>
  <AdminLayout>
    <div v-if="error || order?.retryPayload || recoveryBlocked" class="ss-card" role="alert">{{ error || 'Cần kiểm tra lại kết quả thanh toán trước khi tiếp tục.' }} <button v-if="order?.retryPayload && !recoveryBlocked" class="ss-btn" :disabled="paying" @click="retryPayment">Kiểm tra lại thanh toán</button><button v-else-if="!recoveryBlocked" class="ss-btn sm" @click="error = ''">Đóng</button></div>
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
            <button v-for="o in orders" :key="o.id" :disabled="locked" :class="{ on: o.id === activeId }" @click="activeId = o.id">
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
            <button class="ss-btn" :disabled="locked" @click="openPicker"><i class="bi bi-qr-code-scan"></i> Nhập mã</button>
            <button class="ss-btn" :disabled="locked" @click="openPicker">Chọn sản phẩm</button>
          </div>
          <div class="ss-table-wrap">
            <table class="ss-table" style="min-width:640px">
              <thead><tr><th class="w-stt c">STT</th><th>Mã sản phẩm</th><th>Tên sản phẩm</th><th>Màu sắc</th><th class="c">Kích cỡ</th><th class="c">Số lượng</th><th class="r">Đơn giá</th><th class="c">Thao tác</th></tr></thead>
              <tbody>
                <tr v-for="(i, idx) in order?.items || []" :key="i.code">
                  <td class="c">{{ idx + 1 }}</td>
                  <td><span class="ss-code">{{ i.code }}</span></td>
                  <td class="ss-strong">{{ i.name }}</td>
                  <td>{{ i.color }}</td>
                  <td class="c">{{ i.size }}</td>
                  <td class="c"><div class="qty"><button :disabled="locked" @click="setQty(i, i.qty - 1)">−</button><input :disabled="locked" :value="i.qty" @change="setQty(i, $event.target.value)" /><button :disabled="locked" @click="setQty(i, i.qty + 1)">+</button></div></td>
                  <td class="r nowrap" :title="i.campaignName || ''"><del v-if="i.discounted" class="pos-price-old">{{ money(i.originalPrice) }}</del><strong>{{ money(i.price) }}</strong><span v-if="i.discounted" class="pos-discount">-{{ Number(i.discountPercent) }}%</span></td>
                  <td class="c"><button class="ss-icon-btn danger" :disabled="locked" title="Xóa" @click="removeItem(i.code)"><i class="bi bi-trash3"></i></button></td>
                </tr>
              </tbody>
            </table>
            <p v-if="!order?.items.length" class="empty">Giỏ hàng trống.</p>
          </div>
        </section>
      </div>

      <aside class="right ss-stack">
        <section class="ss-card tight">
          <h3 class="side-title">Khách hàng</h3>
          <div class="cust">
            <input class="ss-input" v-model="customerSearch" placeholder="Tìm khách hàng theo tên, mã hoặc số điện thoại" :disabled="locked" />
            <router-link class="ss-icon-btn sq" title="Thêm khách hàng" to="/khach-hang/them"><i class="bi bi-person-plus"></i></router-link>
          </div>
          <select v-if="order" class="ss-select" v-model="order.customerId" :disabled="locked" aria-label="Khách hàng">
            <option :value="null">Khách lẻ</option>
            <option v-for="c in customers" :key="c.id" :value="c.id">{{ c.code || '—' }} · {{ c.name || '—' }} · {{ c.phone || '—' }}</option>
          </select>
          <label class="ss-label">Nhân viên bán hàng</label>
          <select class="ss-select" v-model="employeeId" :disabled="locked" aria-label="Nhân viên bán hàng">
            <option v-for="e in employees" :key="e.id" :value="e.id">{{ e.code || '—' }} · {{ e.name || '—' }}</option>
          </select>
        </section>

        <section class="ss-card" v-if="order">
          <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-receipt"></i></div><h2>Thông tin đơn hàng</h2></div>
          <div class="kv"><span>Tổng tiền hàng ({{ count }} sản phẩm):</span><b>{{ money(total) }}</b></div>
          <div class="ss-field"><label class="ss-label" for="pos-voucher">Mã phiếu giảm giá</label>
            <small v-if="!voucherSupported" class="ss-hint warn">Phiếu giảm giá chưa khả dụng trong cấu hình hiện tại.</small>
            <input id="pos-voucher" class="ss-input" v-model="order.voucherCode" maxlength="50" :disabled="locked || !voucherSupported" placeholder="Không bắt buộc" /></div>
          <div class="kv"><span>Giảm giá:</span><b>{{ money(quoteReady ? order.quote.discount : 0) }}</b></div>
          <div class="kv"><span>Cần thanh toán:</span><b>{{ money(payable) }}</b></div>
          <p v-if="order.quoteError" role="alert">{{ order.quoteError }}</p>
          <button class="ss-btn sm" :disabled="locked || !count" @click="refreshCart">Kiểm tra lại giá và tồn kho</button>

          <div class="ss-field"><span class="ss-label strong">Hình thức thanh toán</span>
            <div class="ss-radios" style="min-height:24px;gap:16px">
              <label v-for="m in methods" :key="m.id"><input type="radio" :value="m.id" v-model="order.paymentMethodId" :disabled="locked" /> {{ m.name }}</label>
            </div>
          </div>

          <div class="ss-field"><span class="ss-label strong">Khách thanh toán</span>
            <input class="ss-input" type="number" min="0" step="1" v-model="order.paid" :disabled="locked" placeholder="Nhập số tiền khách đưa" /></div>
          <div class="kv"><span>Tiền thừa trả khách</span><b>{{ money(change) }}</b></div>

          <div class="ss-field"><span class="ss-label strong">Ghi chú thanh toán</span>
            <textarea class="ss-textarea" v-model="order.note" maxlength="1000" :disabled="locked" placeholder="Ghi chú thêm nếu cần"></textarea></div>

          <div class="btns">
            <button class="ss-btn" :disabled="locked" @click="cancelOrder">Hủy hóa đơn</button>
            <button class="ss-btn primary" :disabled="!canPay" @click="pay">{{ paying ? 'Đang thanh toán...' : 'Thanh toán' }}</button>
          </div>
        </section>
        <section v-else class="ss-card"><p class="muted-center">Tạo hóa đơn chờ để bắt đầu bán hàng.</p></section>
      </aside>
    </main>

    <!-- Modal chọn sản phẩm -->
    <div v-if="picking" class="ss-modal-bg" @click.self="picking = false">
      <div class="ss-modal">
        <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-box-seam"></i></div><h2>Chọn sản phẩm</h2><span class="ss-spacer"></span>
          <button class="ss-icon-btn" aria-label="Đóng" @click="picking = false"><i class="bi bi-x-lg"></i></button></div>
        <div class="ss-search"><i class="bi bi-search"></i><input class="ss-input" v-model="q" placeholder="Tìm theo mã, tên, màu sắc..." autofocus /></div>
        <div class="ss-table-wrap">
          <table class="ss-table" style="min-width:600px">
            <thead><tr><th>Mã CTSP</th><th>Tên sản phẩm</th><th>Màu</th><th class="c">Size</th><th class="r">Tồn</th><th class="r">Giá</th><th class="c"></th></tr></thead>
            <tbody>
              <tr v-for="v in shown" :key="v.code">
                <td><span class="ss-code">{{ v.code }}</span></td><td class="ss-strong">{{ v.name }}</td><td>{{ v.color }}</td><td class="c">{{ v.size }}</td>
                <td class="r">{{ v.stock }}</td><td class="r nowrap" :title="[v.campaignCode, v.campaignName].filter(Boolean).join(' · ')"><del v-if="v.discounted" class="pos-price-old">{{ money(v.originalPrice) }}</del><strong>{{ money(v.price) }}</strong><span v-if="v.discounted" class="pos-discount">-{{ Number(v.discountPercent) }}%</span></td>
                <td class="c"><button class="ss-btn sm" @click="addItem(v)"><i class="bi bi-plus-lg"></i> Thêm</button></td>
              </tr>
              <tr v-if="loading"><td colspan="7" class="ss-empty">Đang tải sản phẩm...</td></tr>
              <tr v-else-if="!shown.length"><td colspan="7" class="ss-empty"><i class="bi bi-inbox"></i>Không tìm thấy sản phẩm.</td></tr>
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

    <div v-if="completedReceipt" class="ss-modal-bg" @click.self="completedReceipt = null">
      <section class="ss-modal pos-receipt" role="dialog" aria-modal="true" aria-labelledby="receipt-title">
        <div class="ss-head"><h2 id="receipt-title">Thanh toán thành công</h2><span class="ss-spacer"></span><button class="ss-icon-btn" aria-label="Đóng biên nhận" @click="completedReceipt = null">×</button></div>
        <dl class="receipt-details">
          <dt>Mã HĐ</dt><dd>{{ completedReceipt.invoiceCode }}</dd>
          <dt>Mã NV</dt><dd>{{ completedReceipt.employeeCode }}</dd>
          <dt>Nhân viên</dt><dd>{{ completedReceipt.employeeName }}</dd>
          <dt>Mã KH</dt><dd>{{ completedReceipt.customerCode || '—' }}</dd>
          <dt>Khách hàng</dt><dd>{{ completedReceipt.customerName || 'Khách lẻ' }}</dd>
          <dt>SĐT</dt><dd>{{ completedReceipt.customerPhone || '—' }}</dd>
          <dt>Phương thức</dt><dd>{{ completedReceipt.paymentMethodName }}</dd>
          <dt>Phiếu giảm giá</dt><dd>{{ completedReceipt.voucherCode || '—' }}</dd>
          <dt>Tổng tiền hàng</dt><dd>{{ money(completedReceipt.subtotal) }}</dd>
          <dt>Giảm phiếu</dt><dd>{{ money(completedReceipt.discount) }}</dd>
          <dt>Thanh toán</dt><dd>{{ money(completedReceipt.total) }}</dd>
          <dt>Khách đưa</dt><dd>{{ money(completedReceipt.paidAmount) }}</dd>
          <dt>Tiền thừa</dt><dd>{{ money(completedReceipt.change) }}</dd>
          <dt>Ngày tạo</dt><dd>{{ new Date(completedReceipt.createdAt).toLocaleString('vi-VN') }}</dd>
        </dl>
        <div class="ss-actions"><button class="ss-btn" @click="completedReceipt = null">Tiếp tục bán hàng</button><button class="ss-btn primary" @click="router.push('/hoa-don/' + encodeURIComponent(completedReceipt.invoiceCode))">Xem hóa đơn</button></div>
      </section>
    </div>
  </AdminLayout>
</template>

<style scoped>
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
