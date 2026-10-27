<script setup>
import { computed } from 'vue'
const props = defineProps({ modelValue: { type: Number, default: 1 }, total: { type: Number, default: 0 }, pageSize: { type: Number, default: 5 }, label: { type: String, default: 'bản ghi' } })
const emit = defineEmits(['update:modelValue'])
const pages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
const visiblePages = computed(() => {
  const from = Math.max(1, Math.min(props.modelValue - 2, pages.value - 4))
  return Array.from({ length: Math.min(5, pages.value) }, (_, i) => from + i)
})
const first = computed(() => props.total ? (props.modelValue - 1) * props.pageSize + 1 : 0)
const last = computed(() => Math.min(props.total, props.modelValue * props.pageSize))
function go(page) { if (page >= 1 && page <= pages.value) emit('update:modelValue', page) }
</script>
<template>
  <div class="stats-pagination">
    <span>Hiển thị <strong>{{ first }}–{{ last }}</strong> trong {{ total }} {{ label }}</span>
    <nav :aria-label="'Phân trang ' + label">
      <button type="button" :disabled="modelValue <= 1" @click="go(modelValue - 1)" aria-label="Trang trước">‹</button>
      <button v-if="visiblePages[0] > 1" type="button" @click="go(1)">1</button><span v-if="visiblePages[0] > 2">…</span>
      <button v-for="page in visiblePages" :key="page" type="button" :class="{ active: page === modelValue }" :aria-current="page === modelValue ? 'page' : undefined" @click="go(page)">{{ page }}</button>
      <span v-if="visiblePages.at(-1) < pages - 1">…</span><button v-if="visiblePages.at(-1) < pages" type="button" @click="go(pages)">{{ pages }}</button>
      <button type="button" :disabled="modelValue >= pages" @click="go(modelValue + 1)" aria-label="Trang sau">›</button>
    </nav>
  </div>
</template>
<style scoped>
.stats-pagination{display:flex;justify-content:space-between;align-items:center;gap:14px;padding:18px 24px;font-size:12px;color:#879ba8}.stats-pagination strong{color:#466272}.stats-pagination nav{display:flex;align-items:center;gap:5px}.stats-pagination button{min-width:34px;height:34px;background:#fff;border:1px solid #dce7ed;border-radius:8px;color:#567786;font:inherit;cursor:pointer}.stats-pagination button.active{color:#fff;background:#168fce;border-color:#168fce}.stats-pagination button:disabled{opacity:.4;cursor:default}.stats-pagination button:focus-visible{outline:2px solid #168fce;outline-offset:2px}@media(max-width:600px){.stats-pagination{flex-direction:column;padding:16px}}
</style>
