<script setup>
import { computed } from 'vue'
const props = defineProps({ value: { type: Number, default: 0 } })
const percent = computed(() => Math.min(100, Math.max(0, props.value / 5 * 100)))
</script>
<template><span class="rating-stars" role="img" :aria-label="`${value.toFixed(1)} trên 5 sao`"><span aria-hidden="true">★★★★★</span><span class="rating-stars-fill" :style="{ width: percent + '%' }" aria-hidden="true">★★★★★</span></span></template>
<style scoped>
.rating-stars{position:relative;display:inline-block;color:#d5dbe4;font-size:19px;line-height:1.3;letter-spacing:2px;white-space:nowrap;vertical-align:middle}.rating-stars-fill{position:absolute;inset:0 auto 0 0;overflow:hidden;color:#ed9b12}
</style>
