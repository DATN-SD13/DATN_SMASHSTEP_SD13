<script setup>
import { computed } from 'vue'
const props = defineProps({ page: { type: Object, required: true }, busy: Boolean })
const emit = defineEmits(['change', 'size'])
const pages = computed(() => {
  const count = props.page.totalPages || 0
  const start = Math.max(0, Math.min(props.page.number - 2, count - 5))
  return Array.from({ length: Math.min(5, count) }, (_, i) => start + i)
})
</script>
<template>
  <div class="p-pagination">
    <div><span>Tổng {{ page.totalElements }} bản ghi</span><select :value="page.size" :disabled="busy" aria-label="Số bản ghi mỗi trang" @change="emit('size', Number($event.target.value))"><option :value="5">5 / trang</option><option :value="10">10 / trang</option><option :value="20">20 / trang</option></select></div>
    <div class="p-pages"><button class="p-btn" :disabled="busy || page.number === 0" aria-label="Trang trước" @click="emit('change', page.number - 1)"><i class="bi bi-chevron-left"></i></button><button v-for="n in pages" :key="n" class="p-btn" :class="{ primary: n === page.number }" :disabled="busy" :aria-current="n === page.number ? 'page' : undefined" @click="emit('change', n)">{{ n + 1 }}</button><span v-if="page.totalPages > 5">{{ page.number + 1 }} / {{ page.totalPages }}</span><button class="p-btn" :disabled="busy || page.number + 1 >= page.totalPages" aria-label="Trang sau" @click="emit('change', page.number + 1)"><i class="bi bi-chevron-right"></i></button></div>
  </div>
</template>

