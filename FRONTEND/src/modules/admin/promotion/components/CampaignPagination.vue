<script setup>
import { computed } from 'vue';
const props = defineProps({
  page: { type: Number, required: true },
  total: { type: Number, required: true },
  pageSize: { type: Number, default: 10 },
  label: { type: String, default: 'sản phẩm' },
});
const emit = defineEmits(['change']);
const pageCount = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)));
const visiblePages = computed(() => {
  const from = Math.max(1, Math.min(props.page - 2, pageCount.value - 4));
  return Array.from({ length: Math.min(5, pageCount.value) }, (_, i) => from + i);
});
const start = computed(() => props.total ? (props.page - 1) * props.pageSize + 1 : 0);
const end = computed(() => Math.min(props.page * props.pageSize, props.total));
</script>

<template>
  <nav class="campaign-pagination" :aria-label="'Phân trang ' + label">
    <span class="campaign-page-count" aria-live="polite">Hiển thị {{ start }}–{{ end }} / {{ total }} {{ label }}</span>
    <div class="ss-pages campaign-page-actions">
      <button type="button" :disabled="page <= 1" aria-label="Trang trước" @click="emit('change', page - 1)"><i class="bi bi-chevron-left" aria-hidden="true"></i></button>
      <button v-for="n in visiblePages" :key="n" type="button" :class="{ active: n === page }" :aria-label="'Trang ' + n" :aria-current="n === page ? 'page' : undefined" @click="emit('change', n)">{{ n }}</button>
      <button type="button" :disabled="page >= pageCount" aria-label="Trang sau" @click="emit('change', page + 1)"><i class="bi bi-chevron-right" aria-hidden="true"></i></button>
    </div>
  </nav>
</template>

<style scoped>
.campaign-pagination { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px; padding-top:14px; border-top:1px solid var(--ss-border, #e0e8f0); margin-top:12px; }
.campaign-page-count { color:var(--ss-muted, #7b8da5); font-size:12px; }
.campaign-page-actions { align-items:center; margin-left:auto; }
</style>
