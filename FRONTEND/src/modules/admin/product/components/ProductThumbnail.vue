<script setup>
import SaleImageBadge from './SaleImageBadge.vue'
import { ref, watch } from 'vue'
import { imageUrl } from '../services/imageUtils'
const props = defineProps({ url: String, item: Object, range: Boolean, alt: { type: String, default: 'Ảnh sản phẩm' } })
const failed = ref(false)
watch(() => props.url, () => { failed.value = false })
</script>

<template>
  <span v-if="imageUrl(url) && !failed" class="p-product-image-wrap">
    <img class="product-thumbnail" :src="imageUrl(url)" :alt="alt" loading="lazy"
      referrerpolicy="no-referrer" @error="failed = true" />
    <SaleImageBadge :item="item" :range="range" />
  </span>
  <span v-else class="product-thumbnail placeholder-image" :aria-label="url ? 'Không tải được ảnh' : 'Chưa có ảnh'">
    <i class="bi bi-image" aria-hidden="true"></i>
  </span>
</template>

<style scoped>
.p-product-image-wrap{position:relative;display:inline-block;width:56px;height:56px;vertical-align:middle}
.product-thumbnail{display:block;width:56px;height:56px;object-fit:contain;border:1px solid #d6e2e8;border-radius:9px;background:#f5fafc}
.placeholder-image{display:grid;place-items:center;color:#607782;font-size:22px}
</style>
