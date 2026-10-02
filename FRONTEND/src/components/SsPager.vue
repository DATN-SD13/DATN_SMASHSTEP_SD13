<script setup>
defineProps({ page: { type: Number, default: 1 }, size: { type: Number, default: 5 }, pages: { type: Number, default: 1 } })
const emit = defineEmits(['update:page', 'update:size'])
</script>

<template>
  <div class="ss-foot">
    <select class="ss-select" :value="size" @change="emit('update:size', Number($event.target.value))">
      <option :value="5">5</option><option :value="10">10</option><option :value="20">20</option>
    </select>
    <div class="ss-pages">
      <button :disabled="page === 1" @click="emit('update:page', page - 1)"><i class="bi bi-chevron-left"></i></button>
      <button v-for="n in pages" :key="n" :class="{ active: n === page }" @click="emit('update:page', n)">{{ n }}</button>
      <button :disabled="page === pages" @click="emit('update:page', page + 1)"><i class="bi bi-chevron-right"></i></button>
    </div>
  </div>
</template>
