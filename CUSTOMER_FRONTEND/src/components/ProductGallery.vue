<script setup>
import { computed, ref, watch } from 'vue'
import ProductImage from './ProductImage.vue'
import SaleBadge from './SaleBadge.vue'
import { imageUrl } from '../utils/image'
const props = defineProps({ images: { type: Array, default: () => [] }, variant: Object, product: Object })
const selected = ref(''), failed = ref(false)
const available = computed(() => {
  const own = props.variant?.images || [], common = props.images.filter(i => !i.variantId).map(i => i.urlAnh)
  return [...new Set([...own,...common,...(!props.variant ? props.images.map(i=>i.urlAnh) : []),props.variant?.anhChinh || props.product?.anhChinh].filter(imageUrl))]
})
watch(available, urls => { if (!urls.includes(selected.value)) selected.value = urls.includes(props.product?.anhChinh) ? props.product.anhChinh : urls[0] || '' }, { immediate: true })
watch(() => props.variant?.id, () => { selected.value = props.variant?.anhChinh || available.value[0] || '' })
watch(selected, () => { failed.value = false })
</script>
<template><div class="product-gallery"><div class="gallery-main"><ProductImage :src="selected" :alt="product?.tenSanPham" eager @unavailable="failed = true" /><SaleBadge v-if="selected && !failed" :item="variant || product" /></div><div class="gallery-thumbnails" v-if="available.length > 1" aria-label="Ảnh sản phẩm"><button v-for="(url,i) in available" :key="url" :class="{ selected: url === selected }" :aria-label="'Xem ảnh sản phẩm ' + (i+1)" :aria-pressed="url === selected" @click="selected = url"><ProductImage :src="url" :alt="product?.tenSanPham + ' — ảnh ' + (i+1)" /></button></div></div></template>
