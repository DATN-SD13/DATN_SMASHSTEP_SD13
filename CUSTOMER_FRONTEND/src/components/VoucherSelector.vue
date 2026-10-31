<script setup>
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { commerceService, commerceError } from '../services/commerceService'
import { money } from '../utils/money'
import BaseIcon from './BaseIcon.vue'
const props = defineProps({ subtotal: Number, modelValue: String, disabled: Boolean, appliedCode: String, discount: Number, quotePending: Boolean })
const emit = defineEmits(['select'])
const open = ref(false), rows = ref([]), loading = ref(false), error = ref('')
const selected = computed(() => rows.value.find(v => v.code === props.modelValue))
const applied = computed(() => !!props.modelValue && props.modelValue === props.appliedCode && Number(props.discount) > 0)
let version = 0
async function load() {
  const current = ++version; loading.value = true; error.value = ''
  try { const data = await commerceService.vouchers(props.subtotal || 0); if (current === version) rows.value = data }
  catch (e) { if (current === version) error.value = commerceError(e) }
  finally { if (current === version) loading.value = false }
}
function toggle() { if (props.disabled) return; open.value = !open.value; if (open.value) load() }
function select(code) { if (props.disabled || loading.value) return; emit('select', code); open.value = false }
watch([() => props.subtotal, () => props.modelValue], () => { if (open.value || props.modelValue) load() }, { immediate: true })
onBeforeUnmount(() => { version++ })
</script>
<template>
  <div class="voucher-selector">
    <article v-if="modelValue" class="chosen-voucher" :class="{ applied }" aria-label="Phiếu giảm giá đã chọn">
      <div class="chosen-voucher-main"><span class="voucher-ticket"><BaseIcon name="ticket" /></span><div class="chosen-voucher-copy"><span class="chosen-voucher-state" role="status">{{ quotePending ? 'Đang kiểm tra…' : applied ? 'Đã áp dụng phiếu' : 'Phiếu đã chọn' }}</span><strong>{{ applied ? 'Giảm ' + money(discount) : modelValue }}</strong><span>{{ selected?.name || 'Phiếu giảm giá cho đơn hàng' }}</span></div><BaseIcon v-if="applied && !quotePending" class="voucher-applied-check" name="check" /></div>
      <div class="chosen-voucher-details"><b>{{ modelValue }}</b><small v-if="selected">Đơn từ {{ money(selected.minimum || 0) }}<template v-if="selected.expiresAt"> · HSD {{ new Date(selected.expiresAt).toLocaleDateString('vi-VN') }}</template></small></div>
      <div class="chosen-voucher-actions"><button type="button" :disabled="disabled" :aria-expanded="open" @click="toggle">{{ open ? 'Đóng danh sách' : 'Đổi phiếu' }} <span aria-hidden="true">↗</span></button><button type="button" :disabled="disabled || loading" @click="select('')">Bỏ phiếu</button></div>
    </article>
    <button v-else type="button" class="voucher-trigger voucher-add" :disabled="disabled" :aria-expanded="open" @click="toggle"><span><BaseIcon name="ticket" /> Chọn phiếu giảm giá</span><span aria-hidden="true">{{ open ? '−' : '+' }}</span></button>
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
.chosen-voucher{border:1px solid #c9dcf5;border-radius:11px;background:#f5f9ff;overflow:hidden}.chosen-voucher.applied{border-color:#b8dfcd;background:#f5fbf8}.chosen-voucher-main{display:flex;align-items:center;gap:12px;padding:15px 14px 11px}.voucher-ticket{display:grid;place-items:center;width:39px;height:43px;flex-shrink:0;background:#e5effe;border-radius:8px;color:#2878d6}.voucher-ticket .icon{width:24px;height:24px}.applied .voucher-ticket{background:#def2e8;color:#20835b}.chosen-voucher-copy{display:flex;flex-direction:column;gap:3px;min-width:0;flex:1}.chosen-voucher-state{font-size:9px;font-weight:600;color:#597caa}.applied .chosen-voucher-state{color:#498268}.chosen-voucher-copy strong{font-size:17px;color:#194d85;line-height:1.4}.applied .chosen-voucher-copy strong{color:#18724f}.chosen-voucher-copy>span:last-child{font-size:10px;color:#6b7f96;line-height:1.5;overflow-wrap:anywhere}.voucher-applied-check{width:19px;height:19px;color:#20835b;flex-shrink:0}.chosen-voucher-details{display:flex;align-items:center;flex-wrap:wrap;gap:6px 9px;padding:0 14px 13px}.chosen-voucher-details b{padding:3px 7px;background:#fff;border:1px dashed #b8cde0;border-radius:4px;font-size:10px;letter-spacing:.4px;color:#395878;overflow-wrap:anywhere;max-width:100%}.chosen-voucher-details small{font-size:9px;color:#71849b}.chosen-voucher-actions{display:flex;align-items:center;justify-content:space-between;border-top:1px dashed #c8dfd4;padding:9px 14px;background:#ffffff85}.chosen-voucher-actions button{border:0;padding:3px 0;background:none;font:inherit;font-size:10px;color:#2c75c7;font-weight:650;cursor:pointer}.chosen-voucher-actions button:last-child{color:#78899b;font-weight:500}.chosen-voucher-actions button:disabled,.voucher-add:disabled{opacity:.55;cursor:not-allowed}.voucher-add{padding:14px;border:1px dashed #abc9ef;background:#f8fbff}.voucher-add>span:first-child{display:flex;align-items:center;gap:9px;font-weight:600}.voucher-add .icon{width:20px;height:20px;color:#3d83d4}.voucher-trigger:focus-visible,.chosen-voucher-actions button:focus-visible{outline:3px solid #b4d4ff;outline-offset:3px}
</style>
