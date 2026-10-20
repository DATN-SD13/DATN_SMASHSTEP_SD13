<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { invoiceService } from '../services/invoiceService'
const rows = ref([]), error = ref(''), loading = ref(false)
let timer, disposed = false
const money = value => Number(value || 0).toLocaleString('vi-VN') + ' đ'
async function load() {
  if (loading.value || disposed || document.hidden) return
  loading.value = true
  try {
    const response = await invoiceService.paidDemoOrders()
    if (!disposed) { rows.value = response.data || []; error.value = '' }
  } catch (e) { if (!disposed) error.value = e?.response?.data?.message || 'Chưa tải được đơn thanh toán thử.' }
  finally { loading.value = false }
}
onMounted(() => { load(); timer = setInterval(load, 15000); window.addEventListener('focus', load) })
onBeforeUnmount(() => { disposed = true; clearInterval(timer); window.removeEventListener('focus', load) })
</script>
<template>
  <section v-if="rows.length || error" class="demo-orders" aria-labelledby="paid-demo-heading">
    <header><div><span class="demo-badge">GIẢ LẬP</span><h2 id="paid-demo-heading">Đơn thử đã thanh toán ({{ rows.length }})</h2></div><button type="button" :disabled="loading" @click="load">{{ loading ? 'Đang tải…' : 'Làm mới' }}</button></header>
    <p>Chỉ hiện sau khi thẻ thử và OTP đã được xác thực. Không phải hóa đơn thật, không cộng doanh thu, không giao hàng. Dữ liệu giữ tối đa 24 giờ và mất khi backend khởi động lại.</p>
    <p v-if="error" class="demo-error" role="alert">{{ error }}</p>
    <details v-for="order in rows" :key="order.code" class="demo-order">
      <summary><span><strong>{{ order.receiver?.name }}</strong><small>{{ order.code }}</small></span><span class="demo-amount">{{ money(order.amount) }}<small>Đã thanh toán thử ✓</small></span></summary>
      <dl><dt>Thời gian</dt><dd>{{ new Date(order.paidAt).toLocaleString('vi-VN') }}</dd><dt>Giao dịch thử</dt><dd>{{ order.reference }}</dd><dt>Người nhận minh họa</dt><dd>{{ order.receiver?.name }} · {{ order.receiver?.phone }}</dd><dt>Địa chỉ minh họa</dt><dd>{{ [order.receiver?.street, order.receiver?.ward, order.receiver?.district, order.receiver?.province].filter(Boolean).join(', ') }}</dd></dl>
      <ul><li v-for="line in order.lines" :key="line.variant.variantId"><span>{{ line.variant.name }} · {{ line.variant.color }} / {{ line.variant.size }} × {{ line.quantity }}</span><strong>{{ money(line.total) }}</strong></li></ul>
      <div class="demo-totals"><span>Tạm tính: {{ money(order.subtotal) }}</span><span>Voucher minh họa: −{{ money(order.discount) }}</span><span>Vận chuyển: {{ money(order.shipping) }}</span></div>
    </details>
  </section>
</template>
<style scoped>
.demo-orders{margin-top:26px;border:1px solid #e6d7b4;border-radius:12px;padding:20px;background:#fffdf7;color:#3f5262}.demo-orders>header,.demo-orders>header>div{display:flex;align-items:center;gap:12px;flex-wrap:wrap}.demo-orders>header{justify-content:space-between}.demo-badge{font-size:9px;font-weight:750;padding:5px 8px;border-radius:5px;background:#f3e1ba;color:#785820;letter-spacing:1px}.demo-orders h2{font-size:16px;margin:0}.demo-orders>p{font-size:11px;color:#807150;line-height:1.8;margin:14px 0}.demo-orders button{border:1px solid #deceac;background:white;border-radius:7px;padding:7px 10px;font:inherit;font-size:11px;color:#77623a}.demo-order{border-top:1px solid #e9dfca;padding:14px 0}.demo-order summary{display:flex;justify-content:space-between;gap:16px;cursor:pointer}.demo-order summary strong{font-size:12px}.demo-order small{display:block;font-size:10px;margin-top:5px;overflow-wrap:anywhere}.demo-amount{font-size:14px;font-weight:700;text-align:right;flex-shrink:0}.demo-amount small{color:#278461;font-weight:500}.demo-order dl{display:grid;grid-template-columns:140px 1fr;font-size:11px;gap:9px;margin:20px 0}.demo-order dt{color:#81919e}.demo-order dd{margin:0;overflow-wrap:anywhere}.demo-order ul{list-style:none;padding:0;font-size:11px}.demo-order li{display:flex;justify-content:space-between;gap:15px;margin-top:10px}.demo-totals{display:flex;gap:20px;flex-wrap:wrap;font-size:10px;color:#7a8998;margin-top:20px}.demo-error{color:#b34d4d!important}@media(max-width:600px){.demo-orders{padding:16px}.demo-order dl{grid-template-columns:1fr}.demo-order dl dd{margin-bottom:8px}.demo-order summary{flex-wrap:wrap}.demo-amount{text-align:left}}
</style>
