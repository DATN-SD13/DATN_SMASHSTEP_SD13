<script setup>
import { ref, watch } from 'vue'
import { commerceService, commerceError } from '../services/commerceService'
import { money } from '../utils/money'
const props = defineProps({ subtotal: Number, modelValue: String, disabled: Boolean })
const emit = defineEmits(['update:modelValue'])
const open = ref(false), rows = ref([]), loading = ref(false), error = ref('')
let version = 0
async function load() {
  const current = ++version; loading.value = true; error.value = ''
  try { const data = await commerceService.vouchers(props.subtotal || 0); if (current === version) rows.value = data }
  catch (e) { if (current === version) error.value = commerceError(e) }
  finally { if (current === version) loading.value = false }
}
function toggle() { open.value = !open.value; if (open.value) load() }
watch(() => props.subtotal, () => { if (open.value) load() })
</script>
<template><div class="voucher-selector"><button type="button" class="voucher-trigger" :disabled="disabled" :aria-expanded="open" @click="toggle"><span>◇ {{ modelValue || 'Chọn voucher' }}</span><span>{{ open ? '−' : '+' }}</span></button><div v-if="open" class="voucher-panel"><div class="voucher-panel-heading"><strong>Voucher có thể dùng</strong><button class="text-button" type="button" @click="open = false">Đóng</button></div><p v-if="loading" class="shop-muted" role="status">Đang kiểm tra ưu đãi…</p><p v-else-if="error" class="shop-error" role="alert">{{ error }}</p><p v-else-if="!rows.length" class="shop-muted">Hiện chưa có voucher phù hợp.</p><template v-else><button v-for="(v, i) in rows" :key="v.code" type="button" class="voucher-option" :class="{ selected: modelValue === v.code }" @click="emit('update:modelValue', v.code); open = false"><span v-if="i === 0" class="shop-tag">Tiết kiệm nhất</span><strong>{{ v.name }}</strong><span class="voucher-saving">−{{ money(v.discount) }}</span><small>Mã: {{ v.code }}</small><small>{{ v.type === 1 ? 'Giảm ' + v.value + '%' : 'Giảm ' + money(v.value) }}<template v-if="v.maximum"> · Tối đa {{ money(v.maximum) }}</template></small><small>Đơn từ {{ money(v.minimum || 0) }} · Hết hạn {{ new Date(v.expiresAt).toLocaleDateString('vi-VN') }}</small></button></template><button v-if="modelValue" type="button" class="text-button" @click="emit('update:modelValue', ''); open = false">Bỏ voucher đang chọn</button></div></div></template>
