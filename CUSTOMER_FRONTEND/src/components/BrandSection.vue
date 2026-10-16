<script setup>
import ProductImage from './ProductImage.vue'
defineProps({ brands: { type: Array, default: () => [] }, loading: Boolean })
</script>
<template>
  <section id="thuong-hieu" class="section container home-brands">
    <div class="section-heading"><h2>Mua sắm theo hãng</h2></div>
    <div v-if="brands.length" class="brand-grid">
      <RouterLink v-for="brand in brands.slice(0, 4)" :key="brand.id" :to="{ path: '/san-pham', query: { brandId: brand.id } }" class="brand-card">
        <ProductImage v-if="brand.image" :src="brand.image" :alt="'Giày ' + brand.name" />
        <div class="brand-card-copy"><h3>{{ brand.name }}</h3><p>{{ brand.productCount }} sản phẩm</p></div>
      </RouterLink>
    </div>
    <div v-else-if="loading" class="brand-grid" aria-label="Đang tải thương hiệu"><div v-for="n in 4" :key="n" class="skeleton brand-card"></div></div>
    <p v-else class="section-note">Thương hiệu sẽ xuất hiện khi có sản phẩm đang kinh doanh.</p>
  </section>
</template>
