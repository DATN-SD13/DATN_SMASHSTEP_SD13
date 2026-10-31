<script setup>
import { onMounted, onBeforeUnmount, ref } from 'vue'
import { money } from '../utils/money'
import ghnLogo from '../assets/brands/ghn-logo.png'
import vnpayLogo from '../assets/brands/vnpay-logo.svg'

defineProps({ summary: { type: Object, required: true } })
const emit = defineEmits(['close', 'confirm'])
const dialog = ref(null), cancelButton = ref(null)
let previousFocus, previousOverflow
function keydown(event) {
  if (event.key === 'Escape') { event.preventDefault(); emit('close'); return }
  if (event.key !== 'Tab') return
  const buttons = [...dialog.value.querySelectorAll('button:not(:disabled)')]
  const first = buttons[0], last = buttons.at(-1)
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
}
onMounted(() => {
  previousFocus = document.activeElement
  previousOverflow = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  cancelButton.value?.focus()
})
onBeforeUnmount(() => {
  document.body.style.overflow = previousOverflow
  previousFocus?.focus()
})
</script>

<template>
  <Teleport to="body">
    <div class="shop-modal-backdrop order-confirm-backdrop" @click.self="emit('close')">
      <section ref="dialog" class="shop-modal order-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="order-confirm-title" aria-describedby="order-confirm-description" @keydown="keydown">
        <header>
          <div><span class="eyebrow">KIỂM TRA ĐƠN HÀNG</span><h2 id="order-confirm-title">Xác nhận đặt hàng</h2><p id="order-confirm-description">Kiểm tra người nhận, địa chỉ và số tiền trước khi đặt đơn.</p></div>
          <button type="button" class="icon-button" aria-label="Đóng xác nhận đặt hàng" @click="emit('close')">×</button>
        </header>
        <div class="shop-modal-body">
          <section class="confirmation-receiver"><h3>Thông tin nhận hàng</h3><strong>{{ summary.receiverName }} · {{ summary.phone }}</strong><p>{{ summary.address }}</p></section>
          <ul class="confirmation-lines"><li v-for="line in summary.lines" :key="line.variantId"><span><strong>{{ line.name }}</strong><small>{{ line.color }} · Size {{ line.size }} · SL {{ line.quantity }}</small></span><strong>{{ money(line.total) }}</strong></li></ul>
          <dl class="confirmation-totals"><dt>Tiền hàng</dt><dd>{{ money(summary.subtotal) }}</dd><template v-if="summary.discount > 0"><dt class="confirmation-voucher-label"><span>Phiếu giảm giá</span><b v-if="summary.voucherCode" class="voucher-code">{{ summary.voucherCode }}</b><small v-else>Ưu đãi của đơn hàng</small></dt><dd class="confirmation-discount">−{{ money(summary.discount) }}</dd></template><dt class="confirmation-shipping"><span>Phí vận chuyển</span><img :src="ghnLogo" alt="GHN – Giao Hàng Nhanh" width="64" height="22"></dt><dd>{{ money(summary.shipping) }}</dd><dt class="summary-total">Tổng thanh toán</dt><dd class="summary-total">{{ money(summary.total) }}</dd></dl>
          <div class="confirmation-payment"><span>Phương thức thanh toán</span><span class="confirmation-payment-value"><img v-if="summary.vnpay" :src="vnpayLogo" alt="VNPAY" width="84" height="28"><template v-else><span class="confirmation-cod" aria-hidden="true">COD</span><strong>{{ summary.paymentLabel === 'COD' ? 'Thanh toán khi nhận hàng' : summary.paymentLabel }}</strong></template></span></div>
          <p v-if="summary.email" class="shop-muted">Email xác nhận và cập nhật đơn hàng sẽ gửi đến {{ summary.email }}.</p>
          <p v-if="summary.vnpay" class="shop-muted">Sau khi xác nhận, bạn sẽ chuyển sang VNPAY để thanh toán.</p>
        </div>
        <footer><button ref="cancelButton" type="button" class="button button-outline" @click="emit('close')">Kiểm tra lại</button><button type="button" class="button button-primary" @click="emit('confirm')">Xác nhận đặt hàng</button></footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.order-confirm-backdrop{z-index:1300}.order-confirm-dialog{width:min(100%,540px)}.order-confirm-dialog header p{margin:7px 0 0;font-size:12px;line-height:1.6;color:var(--customer-muted)}.confirmation-receiver{padding:14px;border-radius:9px;background:var(--customer-bg);font-size:13px}.confirmation-receiver h3{margin:0 0 8px;font-size:14px}.confirmation-receiver p{margin:6px 0 0;line-height:1.6;overflow-wrap:anywhere}.confirmation-lines{list-style:none;padding:0;margin:16px 0}.confirmation-lines li{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:12px 0;border-bottom:1px solid var(--customer-border);font-size:13px}.confirmation-lines li>span{min-width:0;overflow-wrap:anywhere}.confirmation-lines li>strong{flex-shrink:0}.confirmation-lines small{display:block;margin-top:4px;font-size:11px;color:var(--customer-muted)}.confirmation-totals{display:grid;grid-template-columns:1fr auto;gap:10px;font-size:13px}.confirmation-totals dd{margin:0;text-align:right;font-weight:650}.confirmation-discount{color:#168456}.confirmation-totals .summary-total{border-top:1px solid var(--customer-border);padding-top:13px;font-size:17px;font-weight:750;color:var(--customer-primary)}.confirmation-payment{font-size:13px;margin:16px 0 8px}.order-confirm-dialog footer{display:flex;gap:10px;justify-content:flex-end}.order-confirm-dialog footer button{min-height:44px}@media(max-width:480px){.order-confirm-dialog footer button{flex:1;font-size:12px;padding:10px}.confirmation-totals .summary-total{font-size:15px}}
.confirmation-shipping{display:flex;align-items:center;gap:8px;flex-wrap:wrap}.confirmation-shipping img{width:64px;height:22px;object-fit:cover;background:#fff;border-radius:3px}.confirmation-voucher-label{display:flex;align-items:center;flex-wrap:wrap;gap:6px 9px}.confirmation-voucher-label small{font-size:10px;color:var(--customer-muted)}.voucher-code{display:inline-block;font-size:10px;letter-spacing:.3px;color:#1b7653;background:#eaf7f0;border:1px dashed #a8d5bd;padding:2px 7px;border-radius:4px;overflow-wrap:anywhere;max-width:100%}.confirmation-payment{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:12px 0;border-block:1px solid var(--customer-border);color:var(--customer-muted)}.confirmation-payment-value{display:flex;align-items:center;gap:8px;color:var(--customer-text)}.confirmation-payment-value>img{width:84px;height:28px;object-fit:contain}.confirmation-cod{font-size:10px;font-weight:750;color:#206b50;background:#eaf7f0;border-radius:4px;padding:3px 6px}
</style>
