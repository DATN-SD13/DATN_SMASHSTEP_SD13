<script setup>
import { ref, watch } from 'vue'
import RatingStars from './RatingStars.vue'
import ProductImage from './ProductImage.vue'
import ProductPrice from './ProductPrice.vue'
import SaleBadge from './SaleBadge.vue'
import BaseIcon from './BaseIcon.vue'
import { imageUrl } from '../utils/image'
const props = defineProps({ product: { type: Object, required: true } })
const imageFailed = ref(false)
watch(() => props.product.anhChinh, () => { imageFailed.value = false })
</script>
<template><article class="product-card"><RouterLink :to="'/san-pham/' + product.id" class="product-card-link" :aria-label="'Xem ' + product.tenSanPham"><div class="product-card-media"><ProductImage :src="product.anhChinh" :alt="product.tenSanPham" @unavailable="imageFailed = true" /><SaleBadge :item="product" /><span class="card-view" aria-hidden="true"><BaseIcon name="arrow" /></span></div><div class="product-card-body"><p v-if="product.tenThuongHieu" class="product-brand">{{ product.tenThuongHieu }}</p><h3>{{ product.tenSanPham }}</h3><ProductPrice :item="product" :from="product.soBienThe > 1" /><div class="card-rating"><RatingStars :value="product.averageRating || 0"/><span>{{ product.reviewCount ? Number(product.averageRating).toFixed(1) + ' (' + product.reviewCount + ')' : 'Chưa có đánh giá' }}</span></div><div class="product-card-meta"><span>{{ product.tenDanhMuc }}</span><span v-if="product.soLuong === 0" class="stock-empty">Hết hàng</span><span v-else>{{ product.soBienThe }} lựa chọn</span></div></div></RouterLink></article></template>

<style scoped>
.card-rating{display:flex;align-items:center;gap:6px;margin-top:10px;font-size:12px;color:#718096;flex-wrap:wrap}
</style>
