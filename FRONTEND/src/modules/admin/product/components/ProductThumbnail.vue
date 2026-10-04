<script setup>
import { ref, watch } from 'vue'
const props = defineProps({ url: String, alt: { type: String, default: 'Ảnh sản phẩm' } })
const failed = ref(false)
watch(() => props.url, () => { failed.value = false })
</script>

<template>
  <img v-if="url && !failed" class="product-thumbnail" :src="url" :alt="alt" loading="lazy"
    referrerpolicy="no-referrer" @error="failed = true" />
  <span v-else class="product-thumbnail placeholder-image" :aria-label="url ? 'Không tải được ảnh' : 'Chưa có ảnh'">
    <i class="bi bi-image" aria-hidden="true"></i>
  </span>
</template>

<style scoped>
.product-thumbnail{display:block;width:56px;height:56px;object-fit:contain;border:1px solid #d6e2e8;border-radius:9px;background:#f5fafc}
.placeholder-image{display:grid;place-items:center;color:#607782;font-size:22px}
</style>
