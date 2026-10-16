<script setup>
import ProductCard from './ProductCard.vue'
defineProps({ products: { type: Array, default: () => [] } })
</script>
<template><div class="product-grid"><ProductCard v-for="product in products" :key="product.id" :product="product" /></div></template>
