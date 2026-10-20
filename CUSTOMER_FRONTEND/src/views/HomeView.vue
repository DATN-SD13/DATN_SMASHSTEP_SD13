<script setup>
import { computed } from 'vue'
import '../assets/styles/home.css'
import HeroSection from '../components/HeroSection.vue'
import BrandSection from '../components/BrandSection.vue'
import ProductGrid from '../components/ProductGrid.vue'
import ProductSkeleton from '../components/ProductSkeleton.vue'
import EmptyState from '../components/EmptyState.vue'
import BaseIcon from '../components/BaseIcon.vue'
import { useResource } from '../composables/useResource'
import { storefrontService } from '../services/storefrontService'
const { data,loading,error,reload } = useResource(async signal => { const [products,brands] = await Promise.all([storefrontService.products({ size: 8, sort: 'featured' },signal),storefrontService.brands(signal)]); return { products,brands } })
const products = computed(() => data.value?.products.content || [])
reload()
</script>
<template>
  <div class="home-page">
    <HeroSection :products="products" :total="data?.products.totalElements" :brand-count="data?.brands.length" :loading="loading" />
    <BrandSection :brands="data?.brands || []" :loading="loading" />
    <section id="noi-bat" class="section featured-section container">
      <div class="section-heading"><h2>Sản phẩm nổi bật</h2><RouterLink to="/san-pham" class="button button-outline">Xem tất cả <BaseIcon name="arrow" /></RouterLink></div>
      <EmptyState v-if="error" :title="error" error action="Thử lại" @action="reload" />
      <ProductSkeleton v-else-if="loading && !data" />
      <ProductGrid v-else-if="products.length" :products="products" />
      <EmptyState v-else title="Hiện chưa có sản phẩm." description="Bộ sưu tập sẽ được cập nhật khi có sản phẩm đang kinh doanh." />
    </section>
    <section class="container home-about"><div><span class="eyebrow">VỀ SMASHSTEP</span><h2>Giày thể thao. Phong cách của bạn.</h2><p>Từ lựa chọn hằng ngày đến những bước tiến mới, hãy tìm đôi giày phù hợp với nhu cầu và dấu ấn của riêng bạn.</p></div><RouterLink to="/ve-chung-toi" class="button button-outline">Câu chuyện của chúng tôi <BaseIcon name="arrow" /></RouterLink></section>
  </div>
</template>
