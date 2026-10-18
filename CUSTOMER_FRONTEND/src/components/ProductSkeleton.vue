<script setup>
defineProps({ count: { type: Number, default: 8 }, detail: Boolean })
</script>
<template><div v-if="detail" class="detail-skeleton" aria-busy="true" aria-label="Đang tải sản phẩm"><div class="skeleton skeleton-large"></div><div><div v-for="n in 6" :key="n" class="skeleton skeleton-line"></div></div></div><div v-else class="product-grid" aria-busy="true" aria-label="Đang tải sản phẩm"><div v-for="n in count" :key="n" class="skeleton-card"><div class="skeleton skeleton-photo"></div><div class="skeleton skeleton-line"></div><div class="skeleton skeleton-line short"></div></div></div></template>
