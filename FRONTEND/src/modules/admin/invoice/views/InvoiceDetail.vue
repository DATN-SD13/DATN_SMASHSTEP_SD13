<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { invoiceData } from '../services/invoiceData'

const route = useRoute()
const router = useRouter()
const invoice = computed(() => invoiceData.find(item => item.code === route.params.code) || invoiceData[0])

const steps = [
  { key: 'waiting', label: 'Chờ xác nhận', icon: 'bi-hourglass-split' },
  { key: 'confirmed', label: 'Đã xác nhận', icon: 'bi-check2-circle' },
  { key: 'ready', label: 'Chờ giao hàng', icon: 'bi-box-seam' },
  { key: 'shipping', label: 'Đang giao hàng', icon: 'bi-truck' },
  { key: 'delivered', label: 'Đã giao hàng', icon: 'bi-bag-check' },
  { key: 'done', label: 'Hoàn thành', icon: 'bi-flag' }
]
const progress = computed(() => {
  const s = invoice.value.statusClass
  if (s === 'cancel' || s === 'refund') return -1
  if (s === 'waiting') return 0
  if (s === 'confirmed') return 1
  if (s === 'ready') return 2
  if (s === 'shipping') return 3
  if (s === 'delivered') return 4
  return 5
})
const tone = computed(() => ({ done: 'success', cancel: 'danger', refund: 'danger', waiting: 'warn' }[invoice.value.statusClass] || ''))

const subtotal = computed(() => (invoice.value.items || []).reduce((sum, item) => sum + item.price * item.quantity, 0))
const discountPct = computed(() => (invoice.value.discount > 0 && subtotal.value > 0 ? Math.round(invoice.value.discount / subtotal.value * 100) : 0))
const unitAfter = (price) => Math.round(price * (1 - discountPct.value / 100))

function money(value) { return `${Number(value || 0).toLocaleString('vi-VN')} đ` }
function orderType(type) { return type === 'online' ? 'Trực tuyến' : type === 'delivery' ? 'Giao hàng' : 'Cửa hàng' }
function printInvoice() { window.print() }

/* ---------- Danh sách sản phẩm: lọc / sắp xếp / khoảng giá ---------- */
const productQuery = ref('')
const productVariant = ref('all')
const sortBy = ref('default')
const maxPrice = ref(0)

const items = computed(() => (invoice.value.items || []).map(item => {
  const [color = '', size = ''] = String(item.variant || '').split('/').map(s => s.trim())
  return { ...item, color, size }
}))
const priceCeil = computed(() => Math.max(0, ...items.value.map(i => i.price)))
watch(priceCeil, (v) => { maxPrice.value = v }, { immediate: true })

const productVariants = computed(() => [...new Set(items.value.map(item => item.variant).filter(Boolean))])
const filteredItems = computed(() => {
  const q = productQuery.value.trim().toLowerCase()
  const list = items.value.filter(item => {
    const text = `${item.name || ''} ${item.variant || ''} ${item.sku || ''}`.toLowerCase()
    return (!q || text.includes(q)) &&
      (productVariant.value === 'all' || item.variant === productVariant.value) &&
      item.price <= maxPrice.value
  })
  if (sortBy.value === 'asc') list.sort((a, b) => a.price - b.price)
  if (sortBy.value === 'desc') list.sort((a, b) => b.price - a.price)
  return list
})
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <!-- Dòng đầu trang -->
      <div class="meta-row">
        <div class="meta">
          <span>Mã đơn hàng: <b>{{ invoice.code }}</b></span>
          <span class="dim">Ngày tạo: {{ invoice.date }}</span>
          <span class="ss-pill dot" :class="tone">{{ invoice.status }}</span>
        </div>
        <div class="ss-actions">
          <button v-if="invoice.type === 'store'" class="ss-btn primary" @click="router.push('/ban-hang')">Quay lại Bán hàng tại quầy</button>
          <button class="ss-btn" @click="router.push('/hoa-don')">Quay lại danh sách</button>
        </div>
      </div>

      <!-- Hàng 1: trạng thái + tổng kết -->
      <div class="grid-top">
        <section class="ss-card">
          <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-clipboard2-check"></i></div><h2>Trạng thái đơn hàng</h2></div>

          <div v-if="progress >= 0" class="timeline">
            <div v-for="(step, index) in steps" :key="step.key" class="step" :class="{ reached: index <= progress, current: index === progress }">
              <div class="node"><i :class="['bi', step.icon]"></i></div>
              <div class="step-label">{{ step.label }}</div>
              <small v-if="index <= progress">{{ invoice.date }}</small>
            </div>
          </div>
          <div v-else class="cancel-state">
            <span class="cancel-icon"><i class="bi bi-x-circle"></i></span>
            <div><strong>{{ invoice.status }}</strong><p>Đơn hàng đã dừng ở trạng thái này.</p></div>
          </div>
        </section>

        <section class="ss-card">
          <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-cash-stack"></i></div><h2>Tổng kết thanh toán</h2></div>
          <div class="kv"><span>Tổng tiền hàng</span><b>{{ money(subtotal) }}</b></div>
          <div class="kv"><span>Phiếu giảm giá</span><b>{{ invoice.discount ? `${discountPct}% (-${money(invoice.discount)})` : '—' }}</b></div>
          <div class="kv"><span>Phí vận chuyển</span><b>{{ invoice.shippingFee ? `+ ${money(invoice.shippingFee)}` : '—' }}</b></div>
          <div class="total"><span>Tổng tiền</span><strong>{{ money(invoice.total) }}</strong></div>
        </section>
      </div>

      <!-- Hàng 2: khách hàng / giao hàng / thanh toán -->
      <div class="grid-mid">
        <section class="ss-card">
          <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-person"></i></div><h2>Thông tin khách hàng</h2></div>
          <div class="kv"><span>Tên khách hàng</span><b>{{ invoice.customer || 'Khách vãng lai' }}</b></div>
          <div class="kv"><span>Số điện thoại</span><b>{{ invoice.phone || '—' }}</b></div>
          <div class="kv"><span>Email</span><b>{{ invoice.email || 'Không có' }}</b></div>
        </section>

        <section class="ss-card">
          <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-geo-alt"></i></div><h2>Thông tin giao hàng</h2></div>
          <div class="kv"><span>Địa chỉ</span><b class="addr">{{ invoice.address || '—' }}</b></div>
          <div class="kv"><span>Loại đơn</span><b>{{ orderType(invoice.type) }}</b></div>
          <div class="kv"><span>Nhân viên</span><b>{{ invoice.employee || '—' }}</b></div>
        </section>

        <section class="ss-card">
          <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-clock-history"></i></div><h2>Lịch sử thanh toán</h2></div>
          <div class="pay">
            <div>
              <strong>{{ invoice.payment === 'COD' ? 'Thanh toán khi nhận hàng (COD)' : invoice.payment }}</strong>
              <small>Thanh toán đơn hàng lúc {{ invoice.date }}</small>
              <span class="paid" :class="{ unpaid: invoice.statusClass === 'cancel' }">{{ invoice.statusClass === 'cancel' ? 'Chưa thanh toán' : 'Đã thanh toán' }}</span>
            </div>
            <strong class="amount">{{ money(invoice.total) }}</strong>
          </div>
          <button class="ss-btn primary block print" @click="printInvoice"><i class="bi bi-printer"></i> In hóa đơn</button>
        </section>
      </div>

      <!-- Hàng 3: danh sách sản phẩm -->
      <section class="ss-card">
        <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-box-seam"></i></div><h2>Danh sách sản phẩm ({{ filteredItems.length }})</h2></div>

        <div class="filters">
          <div class="ss-field"><span class="ss-label strong">Tìm kiếm</span>
            <div class="ss-search"><i class="bi bi-search"></i><input class="ss-input" v-model="productQuery" placeholder="Tên sản phẩm, màu, size..." /></div>
          </div>
          <div class="ss-field"><span class="ss-label strong">Loại sản phẩm</span>
            <select class="ss-select" v-model="productVariant"><option value="all">Tất cả loại</option><option v-for="variant in productVariants" :key="variant" :value="variant">{{ variant }}</option></select>
          </div>
          <div class="ss-field"><span class="ss-label strong">Sắp xếp</span>
            <select class="ss-select" v-model="sortBy"><option value="default">Mặc định</option><option value="asc">Giá tăng dần</option><option value="desc">Giá giảm dần</option></select>
          </div>
        </div>

        <div class="range">
          <span class="ss-hint">Khoảng giá: 0 – {{ money(maxPrice) }}</span>
          <input type="range" min="0" :max="priceCeil" step="10000" v-model.number="maxPrice" />
        </div>

        <div class="ss-table-wrap">
          <table class="ss-table">
            <thead>
              <tr><th class="w-stt c">STT</th><th>Mã SPCT</th><th>Ảnh</th><th>Sản phẩm</th><th>Màu sắc</th><th class="c">Size</th><th class="c">Số lượng</th><th>Thời gian</th><th class="r">Giảm giá</th><th class="r">Đơn giá</th></tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in filteredItems" :key="index">
                <td class="c">{{ index + 1 }}</td>
                <td><span class="ss-code">{{ item.sku || '—' }}</span></td>
                <td><span class="thumb"><img v-if="item.image" :src="item.image" alt="" /><i v-else class="bi bi-image"></i></span></td>
                <td><span class="ss-strong">{{ item.name }}</span><span v-if="item.category" class="ss-sub">{{ item.category }}</span></td>
                <td>{{ item.color || '—' }}</td>
                <td class="c">{{ item.size || '—' }}</td>
                <td class="c">{{ item.quantity }}</td>
                <td class="nowrap">{{ invoice.date }}</td>
                <td class="r">{{ discountPct ? `-${discountPct}%` : '—' }}</td>
                <td class="r nowrap">
                  <span v-if="discountPct" class="ss-sub strike">{{ money(item.price) }}</span>
                  <span class="ss-code">{{ money(unitAfter(item.price)) }}</span>
                </td>
              </tr>
              <tr v-if="!filteredItems.length"><td colspan="10" class="ss-empty"><i class="bi bi-search"></i>Không tìm thấy sản phẩm phù hợp.</td></tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  </AdminLayout>
</template>

<style scoped>
.meta-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.meta { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; font-size: 12px; color: var(--ss-ink); }
.meta b { font-weight: 700; }
.meta .dim { color: var(--ss-faint); font-size: 11px; }

.grid-top { display: grid; grid-template-columns: minmax(0, 2fr) minmax(0, 1fr); gap: 16px; }
.grid-mid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
@media (max-width: 1100px) { .grid-top, .grid-mid { grid-template-columns: 1fr; } }

/* key / value */
.kv { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; padding: 9px 0; font-size: 12px; }
.kv + .kv { border-top: 1px dashed var(--ss-line); }
.kv span { color: var(--ss-muted); flex: 0 0 auto; }
.kv b { font-weight: 700; color: var(--ss-text); text-align: right; }
.kv b.addr { max-width: 62%; line-height: 1.45; }
.total { display: flex; align-items: center; justify-content: space-between; margin-top: auto; padding-top: 14px; border-top: 1px solid var(--ss-border); }
.total span { font-size: 13px; font-weight: 700; color: var(--ss-text); }
.total strong { font-size: 18px; font-weight: 700; color: var(--ss-primary); }

/* timeline */
.timeline { display: flex; padding: 14px 0 6px; }
.step { flex: 1; position: relative; display: flex; flex-direction: column; align-items: center; gap: 7px; text-align: center; min-width: 0; }
.step::before { content: ""; position: absolute; top: 19px; right: 50%; width: 100%; height: 2px; background: var(--ss-border); }
.step:first-child::before { display: none; }
.step.reached::before { background: var(--ss-primary); }
.node { position: relative; z-index: 1; width: 38px; height: 38px; border-radius: 50%; display: grid; place-items: center; font-size: 16px; background: #fff; border: 2px solid var(--ss-border); color: var(--ss-faint); }
.step.reached .node { background: var(--ss-primary); border-color: var(--ss-primary); color: #fff; }
.step.current .node { box-shadow: 0 0 0 5px rgba(40, 121, 227, .15); }
.step-label { font-size: 11px; font-weight: 700; color: var(--ss-faint); }
.step.reached .step-label { color: var(--ss-primary); }
.step small { font-size: 10px; color: var(--ss-faint); }
.cancel-state { display: flex; align-items: center; gap: 14px; padding: 18px; border-radius: 10px; background: var(--ss-danger-bg); }
.cancel-icon { width: 38px; height: 38px; border-radius: 50%; display: grid; place-items: center; background: #fff; color: var(--ss-danger); font-size: 18px; }
.cancel-state strong { color: var(--ss-danger); font-size: 13px; }
.cancel-state p { color: var(--ss-muted); font-size: 11px; margin-top: 2px; }

/* thanh toán */
.pay { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; }
.pay > div { display: flex; flex-direction: column; gap: 4px; }
.pay strong { font-size: 12px; color: var(--ss-ink); }
.pay small { font-size: 10.5px; color: var(--ss-faint); }
.paid { margin-top: 2px; font-size: 11px; font-weight: 700; color: var(--ss-success); }
.paid.unpaid { color: var(--ss-danger); }
.amount { font-size: 13px; color: var(--ss-primary) !important; white-space: nowrap; }
.print { margin-top: auto; }

/* sản phẩm */
.filters { display: grid; grid-template-columns: 2fr 1.2fr 1fr; gap: 14px; }
@media (max-width: 760px) { .filters { grid-template-columns: 1fr; } }
.range { display: flex; flex-direction: column; gap: 4px; }
.range input[type="range"] { width: 100%; accent-color: var(--ss-primary); }
.thumb { width: 44px; height: 44px; border-radius: 8px; background: var(--ss-surface); border: 1px solid var(--ss-line); display: grid; place-items: center; color: var(--ss-faint); font-size: 18px; overflow: hidden; }
.thumb img { width: 100%; height: 100%; object-fit: cover; }
.strike { text-decoration: line-through; margin: 0 0 2px; }
</style>
