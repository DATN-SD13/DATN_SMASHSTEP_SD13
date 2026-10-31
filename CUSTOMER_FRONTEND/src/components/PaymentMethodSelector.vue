<script setup>
import vnpayLogo from '../assets/brands/vnpay-logo.svg'
defineProps({ methods: { type: Array, default: () => [] }, modelValue: [Number, String], vnpayEnabled: Boolean, vnpayValue: String, disabled: Boolean })
defineEmits(['update:modelValue'])
</script>

<template>
  <section class="shop-card payment-section" aria-labelledby="payment-heading">
    <h2 id="payment-heading">Phương thức thanh toán</h2>
    <p class="payment-caption">Chọn cách thanh toán phù hợp với bạn.</p>
    <div class="payment-options">
      <label v-for="m in methods" :key="m.id" class="payment-choice" :class="{ selected: modelValue === m.id, disabled: disabled || !m.supported }">
        <input type="radio" name="checkout-payment" :value="m.id" :checked="modelValue === m.id" :disabled="disabled || !m.supported" @change="$emit('update:modelValue', m.id)">
        <span class="payment-brand payment-cod" aria-hidden="true"><svg viewBox="0 0 32 32" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="4" y="7" width="24" height="18" rx="3"/><circle cx="16" cy="16" r="4"/><path d="M8 12h2m12 8h2"/></svg><b>COD</b></span>
        <span class="payment-copy"><strong>{{ m.code === 'COD' ? 'Thanh toán khi nhận hàng' : m.name }}</strong><small>{{ m.message || 'Thanh toán trực tiếp khi nhận đơn hàng.' }}</small></span>
        <span class="payment-check" aria-hidden="true"></span>
      </label>
      <label v-if="vnpayEnabled" class="payment-choice" :class="{ selected: modelValue === vnpayValue, disabled }">
        <input type="radio" name="checkout-payment" :value="vnpayValue" :checked="modelValue === vnpayValue" :disabled="disabled" @change="$emit('update:modelValue', vnpayValue)">
        <span class="payment-brand"><img :src="vnpayLogo" alt="VNPAY" width="100" height="32"></span>
        <span class="payment-copy"><strong>Thanh toán qua VNPAY</strong><small>Chuyển đến cổng VNPAY để hoàn tất thanh toán.</small></span>
        <span class="payment-check" aria-hidden="true"></span>
      </label>
    </div>
    <p v-if="!methods.length && !vnpayEnabled" class="shop-error">Chưa có phương thức thanh toán đang hoạt động.</p>
  </section>
</template>

<style scoped>
.payment-section h2{margin-bottom:6px}.payment-caption{font-size:12px;color:var(--customer-muted);margin:0 0 20px;line-height:1.6}.payment-options{display:grid;gap:12px}.payment-choice{position:relative;display:flex;align-items:center;gap:18px;padding:18px;border:1px solid var(--customer-border);border-radius:12px;background:#fff;cursor:pointer;transition:border-color .15s,background .15s}.payment-choice:hover{border-color:#8ab8f4}.payment-choice.selected{border-color:var(--customer-primary);background:#f3f8ff;box-shadow:inset 0 0 0 1px var(--customer-primary)}.payment-choice:focus-within{outline:3px solid #c0d9ff;outline-offset:3px}.payment-choice.disabled{opacity:.6;cursor:not-allowed}.payment-choice input{position:absolute;width:1px;height:1px;opacity:0;margin:0}.payment-brand{display:flex;align-items:center;justify-content:center;width:108px;height:48px;flex-shrink:0;border:1px solid #e5ecf5;border-radius:8px;background:#fff}.payment-brand img{width:90px;height:30px;object-fit:contain}.payment-cod{gap:4px;color:#146147}.payment-cod svg{width:28px;height:28px}.payment-cod b{font-size:14px}.payment-copy{flex:1;min-width:0}.payment-copy strong{display:block;font-size:13px;color:var(--customer-ink);line-height:1.5}.payment-copy small{display:block;color:var(--customer-muted);font-size:11px;line-height:1.65;margin-top:4px}.payment-check{width:19px;height:19px;border:1.5px solid #bbc9d9;border-radius:50%;flex-shrink:0;background:#fff}.selected .payment-check{border:5px solid var(--customer-primary)}
@media(max-width:600px){.payment-choice{padding:14px 12px;gap:12px}.payment-brand{width:78px;height:42px}.payment-brand img{width:66px;height:24px}.payment-cod svg{width:23px}.payment-cod b{font-size:11px}.payment-copy strong{font-size:12px}.payment-copy small{font-size:10px}.payment-check{width:17px;height:17px}}
</style>
