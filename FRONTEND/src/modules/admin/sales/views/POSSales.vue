<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { computed, ref } from 'vue'
import { money } from '../../../../utils/paging'
import { products } from '../../product/services/productData'

const MAX_ORDERS = 5

/* ---------- Danh sách biến thể có thể bán ---------- */
const catalog = products.flatMap(p => p.variants.map(v => ({
  code: v.code, productCode: p.code, name: p.name, color: v.color, size: v.size,
  price: Math.round(v.price * (1 - v.discount / 100)), stock: v.qty
})))

/* ---------- Hóa đơn chờ ---------- */
let seq = 0
const newOrder = () => ({ id: ++seq, name: `HĐ chờ ${seq}`, items: [], customer: '', method: 'Tiền mặt', paid: '', note: '' })
const orders = ref([])
const activeId = ref(null)
const order = computed(() => orders.value.find(o => o.id === activeId.value) || null)

function addOrder() {
  if (orders.value.length >= MAX_ORDERS) return
  const o = newOrder()
  orders.value.push(o)
  activeId.value = o.id
}
function removeOrder(id) {
  orders.value = orders.value.filter(o => o.id !== id)
  if (activeId.value === id) activeId.value = orders.value[0]?.id ?? null
}
addOrder()

/* ---------- Giỏ hàng ---------- */
const count = computed(() => order.value?.items.reduce((s, i) => s + i.qty, 0) || 0)
const total = computed(() => order.value?.items.reduce((s, i) => s + i.qty * i.price, 0) || 0)
function addItem(v) {
  if (!order.value) addOrder()
  const cur = order.value.items.find(i => i.code === v.code)
  if (cur) { if (cur.qty < cur.stock) cur.qty++ }
  else order.value.items.push({ ...v, qty: 1 })
}
const setQty = (i, n) => { i.qty = Math.min(i.stock, Math.max(1, Number(n) || 1)) }
const removeItem = (code) => { order.value.items = order.value.items.filter(i => i.code !== code) }

/* ---------- Thanh toán ---------- */
const paidNum = computed(() => Number(order.value?.paid) || 0)
const change = computed(() => Math.max(0, paidNum.value - total.value))
const canPay = computed(() => count.value > 0 && (order.value.method !== 'Tiền mặt' || paidNum.value >= total.value))
const toast = ref('')
function flash(t) { toast.value = t; setTimeout(() => (toast.value = ''), 2200) }
function pay() {
  if (!canPay.value) return
  flash(`Thanh toán thành công ${money(total.value)}`)
  removeOrder(order.value.id)
}
function cancelOrder() {
  if (!order.value) return
  if (order.value.items.length && !confirm('Hủy hóa đơn này?')) return
  removeOrder(order.value.id)
}

/* ---------- Modal chọn sản phẩm ---------- */
const picking = ref(false)
const q = ref('')
const shown = computed(() => {
  const k = q.value.trim().toLowerCase()
  return catalog.filter(v => !k || v.code.toLowerCase().includes(k) || v.name.toLowerCase().includes(k) || v.color.toLowerCase().includes(k))
})
function openPicker() { if (!order.value) addOrder(); q.value = ''; picking.value = true }
</script>

<template>
  <AdminLayout>
    <main class="ss-page pos">
      <div class="left ss-stack">
        <!-- Hóa đơn chờ -->
        <section class="ss-card">
          <div class="ss-head">
            <button class="ss-btn" :disabled="orders.length >= MAX_ORDERS" @click="addOrder"><i class="bi bi-plus-lg"></i> Thêm hóa đơn chờ</button>
            <span class="ss-spacer"></span>
            <span class="ss-count strong">{{ orders.length }}/{{ MAX_ORDERS }} hóa đơn</span>
          </div>
          <p v-if="!orders.length" class="muted-center">Chưa có hóa đơn chờ nào.</p>
          <div v-else class="tabs">
            <button v-for="o in orders" :key="o.id" :class="{ on: o.id === activeId }" @click="activeId = o.id">
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
            <button class="ss-btn"><i class="bi bi-qr-code-scan"></i> QR</button>
            <button class="ss-btn" @click="openPicker">Chọn sản phẩm</button>
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
                  <td class="c"><div class="qty"><button @click="setQty(i, i.qty - 1)">−</button><input :value="i.qty" @change="setQty(i, $event.target.value)" /><button @click="setQty(i, i.qty + 1)">+</button></div></td>
                  <td class="r nowrap">{{ money(i.price) }}</td>
                  <td class="c"><button class="ss-icon-btn danger" title="Xóa" @click="removeItem(i.code)"><i class="bi bi-trash3"></i></button></td>
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
            <input class="ss-input" v-if="order" v-model="order.customer" placeholder="Nhập tên hoặc số điện thoại khách hàng" />
            <button class="ss-icon-btn sq" title="Thêm khách hàng"><i class="bi bi-person-plus"></i></button>
          </div>
        </section>

        <section class="ss-card" v-if="order">
          <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-receipt"></i></div><h2>Thông tin đơn hàng</h2></div>
          <div class="kv"><span>Tổng tiền hàng ({{ count }} sản phẩm):</span><b>{{ money(total) }}</b></div>

          <div class="ss-field"><span class="ss-label strong">Hình thức thanh toán</span>
            <div class="ss-radios" style="min-height:24px;gap:16px">
              <label v-for="m in ['Tiền mặt', 'Chuyển khoản', 'Kết hợp']" :key="m"><input type="radio" :value="m" v-model="order.method" /> {{ m }}</label>
            </div>
          </div>

          <div class="ss-field"><span class="ss-label strong">Khách thanh toán</span>
            <input class="ss-input" type="number" min="0" v-model="order.paid" placeholder="Nhập số tiền khách đưa" /></div>
          <div class="kv"><span>Tiền thừa trả khách</span><b>{{ money(change) }}</b></div>

          <div class="ss-field"><span class="ss-label strong">Ghi chú thanh toán</span>
            <textarea class="ss-textarea" v-model="order.note" placeholder="Ghi chú thêm nếu cần"></textarea></div>

          <div class="btns">
            <button class="ss-btn" @click="cancelOrder">Hủy hóa đơn</button>
            <button class="ss-btn primary" :disabled="!canPay" @click="pay">Thanh toán</button>
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
                <td class="r">{{ v.stock }}</td><td class="r nowrap">{{ money(v.price) }}</td>
                <td class="c"><button class="ss-btn sm" @click="addItem(v)"><i class="bi bi-plus-lg"></i> Thêm</button></td>
              </tr>
              <tr v-if="!shown.length"><td colspan="7" class="ss-empty"><i class="bi bi-inbox"></i>Không tìm thấy sản phẩm.</td></tr>
            </tbody>
          </table>
        </div>
        <div class="ss-actions"><button class="ss-btn primary" @click="picking = false">Xong</button></div>
      </div>
    </div>

    <transition name="ss-toast"><div v-if="toast" class="ss-toast"><i class="bi bi-check-circle-fill"></i> {{ toast }}</div></transition>
  </AdminLayout>
</template>

<style scoped>
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
