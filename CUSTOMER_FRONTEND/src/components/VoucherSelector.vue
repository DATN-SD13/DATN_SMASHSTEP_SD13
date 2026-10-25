<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { commerceService, commerceError } from '../services/commerceService'
import { money } from '../utils/money'
const props = defineProps({ subtotal: Number, modelValue: String, disabled: Boolean })
const emit = defineEmits(['select'])
const open = ref(false), rows = ref([]), loading = ref(false), error = ref('')
let version = 0
async function load() {
  const current = ++version; loading.value = true; error.value = ''
  try { const data = await commerceService.vouchers(props.subtotal || 0); if (current === version) rows.value = data }
  catch (e) { if (current === version) error.value = commerceError(e) }
  finally { if (current === version) loading.value = false }
}
function toggle() { if (props.disabled) return; open.value = !open.value; if (open.value) load() }
function select(code) { if (props.disabled || loading.value) return; emit('select', code); open.value = false }
watch([() => props.subtotal, () => props.modelValue], () => { if (open.value) load() })
onBeforeUnmount(() => { version++ })
</script>
<template>
  <div class="voucher-selector">
    <button type="button" class="voucher-trigger" :disabled="disabled" :aria-expanded="open" @click="toggle">
      <span>◇ {{ modelValue ? 'Phiếu đã chọn: ' + modelValue : 'Chọn phiếu giảm giá (không bắt buộc)' }}</span><span>{{ open ? '−' : '+' }}</span>
    </button>
    <p class="shop-muted voucher-auto-note" role="status">{{ modelValue ? 'Bạn có thể đổi hoặc bỏ phiếu giảm giá đã chọn.' : 'Chưa sử dụng phiếu giảm giá. Mở danh sách để tự chọn ưu đãi.' }}</p>
    <div v-if="open" class="voucher-panel">
      <div class="voucher-panel-heading"><strong>Ưu đãi cho đơn hàng</strong><button class="text-button" type="button" @click="open = false">Đóng</button></div>
      <button type="button" class="voucher-clear-choice" :disabled="disabled || loading || !modelValue" @click="select('')">{{ modelValue ? 'Không sử dụng phiếu giảm giá' : '✓ Không sử dụng phiếu giảm giá' }}</button>
      <p v-if="loading" class="shop-muted" role="status">Đang kiểm tra ưu đãi…</p>
      <p v-else-if="error" class="shop-error" role="alert">{{ error }}</p>
      <p v-else-if="!rows.length" class="shop-muted">Hiện chưa có voucher phù hợp.</p>
      <template v-else>
        <article v-for="(v, index) in rows" :key="v.code" class="voucher-option" :class="{ selected: modelValue === v.code }">
          <span v-if="index === 0" class="shop-tag">Tiết kiệm nhất</span>
          <strong>{{ v.name }}</strong><span class="voucher-saving">−{{ money(v.discount) }}</span>
          <small>Mã: {{ v.code }}</small>
          <small>{{ v.type === 1 ? 'Giảm ' + v.value + '%' : 'Giảm ' + money(v.value) }}<template v-if="v.maximum"> · Tối đa {{ money(v.maximum) }}</template></small>
          <small>Đơn từ {{ money(v.minimum || 0) }} · Hết hạn {{ new Date(v.expiresAt).toLocaleDateString('vi-VN') }}</small>
          <button type="button" class="voucher-select-button" :disabled="disabled || modelValue === v.code" @click="select(v.code)">{{ modelValue === v.code ? '✓ Đang áp dụng' : 'Áp dụng phiếu này' }}</button>
        </article>
        <p class="shop-muted voucher-auto-note">Mỗi đơn áp dụng một voucher. Mức giảm được tính trên tiền hàng sau khuyến mại sản phẩm.</p>
      </template>
    </div>
  </div>
</template>
<style scoped>
.voucher-auto-note{font-size:11px;line-height:1.6;margin:9px 0}.voucher-option{cursor:default}.voucher-clear-choice,.voucher-select-button{border:1px solid #b9d6ed;border-radius:8px;background:#f2f8ff;color:#14588b;padding:9px 12px;font:inherit;font-size:12px;cursor:pointer}.voucher-clear-choice{width:100%;margin:10px 0}.voucher-select-button{margin-top:8px;align-self:flex-start}.voucher-select-button:disabled,.voucher-clear-choice:disabled{cursor:default;opacity:.65}
</style>
