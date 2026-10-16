<script setup>
import { ref, watch, nextTick, onBeforeUnmount } from 'vue'
import { priceError } from '../utils/query'
import BaseIcon from './BaseIcon.vue'
const props = defineProps({ options: Object, filters: { type: Object, required: true }, open: Boolean })
const emit = defineEmits(['change','reset','close'])
const min = ref(''), max = ref(''), error = ref(''), panel = ref(null)
let previousFocus, previousOverflow
watch(() => props.open, async open => {
  if (open) { previousFocus = document.activeElement; previousOverflow = document.body.style.overflow; document.body.style.overflow = 'hidden'; await nextTick(); panel.value?.focus() }
  else { if (previousOverflow !== undefined) document.body.style.overflow = previousOverflow; if (previousFocus?.isConnected) previousFocus.focus() }
})
function onKeydown(event) {
  if (event.key === 'Escape') { emit('close'); return }
  if (!props.open || event.key !== 'Tab') return
  const controls = [...panel.value.querySelectorAll('button, input, select, a[href]')].filter(el => !el.disabled && el.getClientRects().length)
  const first = controls[0], last = controls.at(-1)
  if (!controls.length) { event.preventDefault(); return }
  if (event.shiftKey && (document.activeElement === first || document.activeElement === panel.value)) { event.preventDefault(); last.focus() }
  else if (!event.shiftKey && (document.activeElement === last || document.activeElement === panel.value)) { event.preventDefault(); first.focus() }
}
onBeforeUnmount(() => { if (previousOverflow !== undefined) document.body.style.overflow = previousOverflow })
watch(() => [props.filters.minPrice,props.filters.maxPrice], ([a,b]) => { min.value = a ?? ''; max.value = b ?? ''; error.value = '' }, { immediate: true })
const groups = [{ key: 'brandId', options: 'brands', label: 'Thương hiệu' },{ key: 'categoryId', options: 'categories', label: 'Danh mục' },{ key: 'colorId', options: 'colors', label: 'Màu sắc' },{ key: 'sizeId', options: 'sizes', label: 'Kích thước' },{ key: 'styleId', options: 'styles', label: 'Kiểu dáng' }]
function change(key,id,checked) { const values = new Set(props.filters[key] || []); checked ? values.add(id) : values.delete(id); emit('change',{ [key]: [...values].map(String) }) }
function applyPrice() { error.value = priceError(min.value,max.value); if (!error.value) emit('change',{ minPrice: min.value, maxPrice: max.value }) }
</script>
<template><div v-if="open" class="filter-backdrop" @click="$emit('close')"></div><aside id="catalogue-filter" ref="panel" tabindex="-1" :role="open ? 'dialog' : undefined" :aria-modal="open ? 'true' : undefined" class="product-filter" :class="{ 'is-open': open }" aria-label="Bộ lọc sản phẩm" @keydown="onKeydown"><div class="filter-heading"><h2>Bộ lọc</h2><button class="text-button" @click="$emit('reset')">Xóa tất cả</button><button class="icon-button filter-close" aria-label="Đóng bộ lọc" @click="$emit('close')"><BaseIcon name="close" /></button></div><form class="filter-group" @submit.prevent="applyPrice"><h3>Khoảng giá (₫)</h3><div class="price-fields"><label><span>Giá từ</span><input v-model="min" type="number" min="0" step="1" inputmode="numeric" placeholder="0" aria-label="Giá từ"></label><span class="price-dash">–</span><label><span>Giá đến</span><input v-model="max" type="number" min="0" step="1" inputmode="numeric" placeholder="Không giới hạn" aria-label="Giá đến"></label></div><p v-if="error" class="field-error" role="alert">{{ error }}</p><button class="button button-soft price-apply" type="submit">Áp dụng khoảng giá</button></form><fieldset v-for="group in groups.filter(g => options?.[g.options]?.length)" :key="group.key" class="filter-group"><legend>{{ group.label }}</legend><div class="filter-options"><label v-for="option in options[group.options]" :key="option.id" class="filter-option"><input type="checkbox" :value="option.id" :checked="(filters[group.key] || []).includes(option.id)" @change="change(group.key,option.id,$event.target.checked)"><span>{{ option.name }}</span><small>{{ option.productCount }}</small></label></div></fieldset><button class="button button-primary filter-mobile-done" @click="$emit('close')">Xem sản phẩm</button></aside></template>
