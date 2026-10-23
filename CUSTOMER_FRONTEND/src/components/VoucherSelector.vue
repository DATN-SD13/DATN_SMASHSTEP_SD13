<script setup>
import { ref, watch } from 'vue'
import { commerceService, commerceError } from '../services/commerceService'
import { money } from '../utils/money'
const props = defineProps({ subtotal: Number, modelValue: String, disabled: Boolean })
const open = ref(false), rows = ref([]), loading = ref(false), error = ref('')
let version = 0
async function load() {
  const current = ++version; loading.value = true; error.value = ''
  try { const data = await commerceService.vouchers(props.subtotal || 0); if (current === version) rows.value = data }
  catch (e) { if (current === version) error.value = commerceError(e) }
  finally { if (current === version) loading.value = false }
}
function toggle() { open.value = !open.value; if (open.value) load() }
watch([() => props.subtotal, () => props.modelValue], () => { if (open.value) load() })
</script>
<template>
  <div class="voucher-selector">
    <button type="button" class="voucher-trigger" :disabled="disabled" :aria-expanded="open" @click="toggle">
      <span>◇ {{ modelValue ? 'Đã áp dụng: ' + modelValue : 'Voucher tự động' }}</span><span>{{ open ? '−' : '+' }}</span>
    </button>
    <p class="shop-muted voucher-auto-note" role="status">{{ modelValue ? 'Đã tự chọn voucher hợp lệ giảm nhiều nhất cho đơn này.' : 'Tự áp dụng voucher giảm nhiều nhất khi đơn đủ điều kiện.' }}</p>
    <div v-if="open" class="voucher-panel">
      <div class="voucher-panel-heading"><strong>Ưu đãi cho đơn hàng</strong><button class="text-button" type="button" @click="open = false">Đóng</button></div>
      <p v-if="loading" class="shop-muted" role="status">Đang kiểm tra ưu đãi…</p>
      <p v-else-if="error" class="shop-error" role="alert">{{ error }}</p>
      <p v-else-if="!rows.length" class="shop-muted">Hiện chưa có voucher phù hợp.</p>
      <template v-else>
        <article v-for="v in rows" :key="v.code" class="voucher-option" :class="{ selected: modelValue === v.code }">
          <span v-if="modelValue === v.code" class="shop-tag">Đã tự áp dụng · Tiết kiệm nhất</span>
          <strong>{{ v.name }}</strong><span class="voucher-saving">−{{ money(v.discount) }}</span>
          <small>Mã: {{ v.code }}</small>
          <small>{{ v.type === 1 ? 'Giảm ' + v.value + '%' : 'Giảm ' + money(v.value) }}<template v-if="v.maximum"> · Tối đa {{ money(v.maximum) }}</template></small>
          <small>Đơn từ {{ money(v.minimum || 0) }} · Hết hạn {{ new Date(v.expiresAt).toLocaleDateString('vi-VN') }}</small>
        </article>
        <p class="shop-muted voucher-auto-note">Mỗi đơn áp dụng một voucher. Mức giảm được tính trên tiền hàng sau khuyến mại sản phẩm.</p>
      </template>
    </div>
  </div>
</template>
<style scoped>
.voucher-auto-note{font-size:11px;line-height:1.6;margin:9px 0}.voucher-option{cursor:default}
</style>
