<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import BaseIcon from './BaseIcon.vue'
import ProductImage from './ProductImage.vue'
import { imageUrl } from '../utils/image'
const props=defineProps({products:{type:Array,default:()=>[]},total:Number,brandCount:Number,loading:Boolean})
const index=ref(0), media=ref({}), pending=new Map()
let disposed=false
// Prefer catalogue photographs whose proportions can fill a wide hero without cropping the shoe.
watch(()=>props.products,products=>{for(const p of products){const url=imageUrl(p.anhChinh);if(!url||pending.has(url))continue;const img=new Image();pending.set(url,img);img.onload=()=>{if(!disposed)media.value={...media.value,[url]:img.naturalWidth/img.naturalHeight}};img.onerror=()=>{if(!disposed)media.value={...media.value,[url]:0}};img.src=url}},{immediate:true})
const slides=computed(()=>{const available=props.products.filter(p=>imageUrl(p.anhChinh));const wide=available.filter(p=>media.value[imageUrl(p.anhChinh)]>=.9);return (wide.length?wide:available.length?available:props.products).slice(0,4)})
const product=computed(()=>slides.value[index.value])
watch(slides,()=>{index.value=Math.min(index.value,Math.max(0,slides.value.length-1))})
function move(delta){if(slides.value.length)index.value=(index.value+delta+slides.value.length)%slides.value.length}
onBeforeUnmount(()=>{disposed=true;for(const img of pending.values()){img.onload=null;img.onerror=null}})
</script>
<template><section class="hero" aria-label="Bộ sưu tập SmashStep">
  <RouterLink v-if="product" :to="'/san-pham/'+product.id" class="hero-banner-image" :aria-label="'Khám phá '+product.tenSanPham"><ProductImage :src="product.anhChinh" :alt="product.tenSanPham" eager/></RouterLink>
  <div class="container hero-inner"><div class="hero-copy"><span class="hero-eyebrow">Bộ sưu tập SmashStep {{new Date().getFullYear()}}</span><span class="hero-kicker">Giày thể thao · Phong cách của bạn</span><h1>Bứt phá mọi bước<br>chân cùng<br>SmashStep</h1><p>Khám phá những đôi giày trong bộ sưu tập hiện tại.<br>Tìm thiết kế đồng hành cùng phong cách của bạn.</p><div class="hero-actions"><RouterLink to="/san-pham" class="button button-primary">Mua ngay <BaseIcon name="arrow"/></RouterLink><RouterLink to="/#thuong-hieu" class="button button-hero-outline">Khám phá theo hãng</RouterLink></div><div v-if="!loading&&total" class="hero-stats"><div><strong>{{total}}</strong><span>Sản phẩm đang kinh doanh</span></div><div v-if="brandCount"><strong>{{brandCount}}</strong><span>Thương hiệu</span></div></div></div></div>
  <template v-if="slides.length>1"><button class="hero-arrow hero-arrow-left" aria-label="Sản phẩm trước" @click="move(-1)"><BaseIcon name="left"/></button><button class="hero-arrow hero-arrow-right" aria-label="Sản phẩm tiếp theo" @click="move(1)"><BaseIcon name="right"/></button><div class="hero-carousel" aria-label="Chọn sản phẩm trong banner"><button v-for="(slide,i) in slides" :key="slide.id" class="hero-dot" :class="{selected:i===index}" :aria-label="'Xem '+slide.tenSanPham" :aria-current="i===index?'true':undefined" @click="index=i"></button></div></template>
</section></template>
