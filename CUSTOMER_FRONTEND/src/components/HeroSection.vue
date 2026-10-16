<script setup>
import { computed, ref, watch } from 'vue'
import BaseIcon from './BaseIcon.vue'
import ProductImage from './ProductImage.vue'
import { imageUrl } from '../utils/image'
const props = defineProps({ products: { type: Array, default: () => [] }, total: Number, brandCount: Number, loading: Boolean })
const index = ref(0)
const slides = computed(() => props.products.filter(p => imageUrl(p.anhChinh)).slice(0,4))
const product = computed(() => slides.value[index.value])
watch(slides, () => { index.value = Math.min(index.value, Math.max(0, slides.value.length - 1)) })
function move(delta) { if (slides.value.length) index.value = (index.value + delta + slides.value.length) % slides.value.length }
</script>
<template>
  <section class="hero" aria-label="Bộ sưu tập SmashStep">
    <div class="container hero-inner">
      <div class="hero-copy">
        <span class="hero-eyebrow">Bộ sưu tập SmashStep</span>
        <span class="hero-kicker">Khám phá phong cách của bạn</span>
        <h1>Bứt phá mọi bước<br>chân cùng<br>SmashStep</h1>
        <p>Khám phá những đôi giày trong bộ sưu tập hiện tại.<br>Tìm thiết kế đồng hành cùng phong cách của bạn.</p>
        <div class="hero-actions">
          <RouterLink to="/san-pham" class="button button-primary">Mua ngay <BaseIcon name="arrow" /></RouterLink>
          <RouterLink to="/#thuong-hieu" class="button button-hero-outline">Khám phá theo hãng</RouterLink>
        </div>
        <div v-if="!loading && total" class="hero-stats">
          <div><strong>{{ total }}</strong><span>Sản phẩm đang kinh doanh</span></div>
          <div v-if="brandCount"><strong>{{ brandCount }}</strong><span>Thương hiệu</span></div>
        </div>
      </div>
      <RouterLink v-if="product" :to="'/san-pham/' + product.id" class="hero-banner-image" :aria-label="'Khám phá ' + product.tenSanPham">
        <ProductImage :src="product.anhChinh" :alt="product.tenSanPham" eager />
      </RouterLink>
    </div>
    <template v-if="slides.length > 1">
      <button class="hero-arrow hero-arrow-left" aria-label="Sản phẩm trước" @click="move(-1)"><BaseIcon name="left" /></button>
      <button class="hero-arrow hero-arrow-right" aria-label="Sản phẩm tiếp theo" @click="move(1)"><BaseIcon name="right" /></button>
      <div class="hero-carousel" aria-label="Chọn sản phẩm trong banner">
        <button v-for="(slide,i) in slides" :key="slide.id" class="hero-dot" :class="{ selected: i === index }" :aria-label="'Xem ' + slide.tenSanPham" :aria-current="i === index ? 'true' : undefined" @click="index = i"></button>
      </div>
    </template>
  </section>
</template>
