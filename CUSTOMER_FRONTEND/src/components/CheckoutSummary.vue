<script setup>
import { computed } from 'vue'
import ProductImage from './ProductImage.vue'
import { money } from '../utils/money'

const props = defineProps({
  lines: { type: Array, default: () => [] },
  subtotal: [Number, String], discount: [Number, String], shipping: [Number, String], total: [Number, String],
  message: String, submitting: Boolean, refreshing: Boolean, pending: Boolean,
  confirmDisabled: Boolean, refreshDisabled: Boolean, vnpay: Boolean
})
defineEmits(['refresh'])
const count = computed(() => props.lines.reduce((sum, line) => sum + line.quantity, 0))
function hasProductDiscount(variant) {
  const original = Number(variant?.giaGoc)
  const current = Number(variant?.giaSauGiam)
  return variant?.giaSauGiam != null && Number.isFinite(original) && Number.isFinite(current) && current >= 0 && original > current
}
</script>

<template>
  <aside class="shop-card shop-summary checkout-summary">
    <h2>Đơn hàng <span class="shop-muted">({{ count }} sản phẩm)</span></h2>
    <div v-for="line in lines" :key="line.variantId" class="summary-product">
      <ProductImage :src="line.variant?.image" :alt="line.variant?.name || 'Sản phẩm'" />
      <div><strong>{{ line.variant?.name || 'Đang tải sản phẩm…' }}</strong><small>{{ line.variant?.color }} · Size {{ line.variant?.size }} · SL {{ line.quantity }}</small></div>
      <span class="line-total">
        <template v-if="line.variant">
          <del v-if="hasProductDiscount(line.variant)" aria-label="Giá gốc">{{ money(Number(line.variant.giaGoc) * line.quantity) }}</del>
          <strong>{{ money(Number(line.variant.giaSauGiam) * line.quantity) }}</strong>
        </template>
        <template v-else>Đang tải…</template>
      </span>
    </div>
    <slot name="voucher" />
    <dl>
      <dt>Tiền hàng</dt><dd>{{ money(subtotal) }}</dd>
      <template v-if="Number(discount) > 0"><dt>Giảm voucher</dt><dd class="checkout-discount">−{{ money(discount) }}</dd></template>
      <dt>Phí vận chuyển</dt><dd>{{ shipping == null ? 'Chưa tính được' : money(shipping) }}</dd>
      <dt class="summary-total">Tổng thanh toán</dt><dd class="summary-total">{{ money(total) }}</dd>
    </dl>
    <p v-if="message" class="shop-alert" role="status">{{ message }}</p>
    <button v-if="!pending" type="button" class="text-button refresh-quote" :disabled="refreshDisabled" @click="$emit('refresh')">Cập nhật giá và phí giao hàng</button>
    <p v-if="refreshing" class="shop-muted" role="status">Đang cập nhật tổng tiền…</p>
    <button type="submit" class="button button-primary confirm-order-button" :disabled="confirmDisabled" :aria-busy="submitting">{{ submitting ? 'Đang xác nhận…' : pending ? 'Kiểm tra kết quả đặt hàng' : 'Xác nhận đặt hàng' }}</button>
    <p v-if="vnpay && !pending" class="payment-next-step">Bạn sẽ được chuyển sang VNPAY để thanh toán.</p>
    <RouterLink to="/gio-hang" class="shop-continue">Quay lại giỏ hàng</RouterLink>
  </aside>
</template>

<style scoped>
.checkout-summary{min-width:0}.checkout-summary h2 .shop-muted{font-size:12px;font-weight:500}.summary-product .line-total{display:flex;flex-direction:column;align-items:flex-end;gap:3px;flex-shrink:0;white-space:nowrap;font-size:12px;color:var(--customer-text)}.line-total del{color:var(--customer-muted);font-size:10px;font-weight:400}.line-total strong{font-size:13px;font-weight:750}.checkout-discount{color:#168456}.refresh-quote{margin-bottom:10px}.confirm-order-button{width:100%;min-height:50px;margin-top:14px;font-size:14px;font-weight:750;border-radius:9px;box-shadow:0 5px 14px #2879e329}.confirm-order-button:disabled{opacity:.65}.payment-next-step{margin-top:9px;color:var(--customer-muted);font-size:11px;text-align:center;line-height:1.6}
</style>
