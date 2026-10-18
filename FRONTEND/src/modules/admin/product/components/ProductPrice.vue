<script setup>
import { computed } from 'vue'
import { priceRange, effectivePriceRange, money, hasProductDiscount, hasVariantDiscount } from '../services/productService'
const props = defineProps({ item: { type: Object, required: true }, range: Boolean })
const discounted = computed(() => props.range ? hasProductDiscount(props.item) : hasVariantDiscount(props.item))
const original = computed(() => props.range ? priceRange(props.item) : money(props.item.giaBan))
const effective = computed(() => props.range ? effectivePriceRange(props.item) : money(props.item.giaSauGiam))
</script>
<template>
  <span class="p-price-stack">
    <template v-if="discounted"><span class="p-price-original">{{ original }}</span><span class="p-price-effective">{{ effective }}</span></template>
    <template v-else>{{ original }}</template>
  </span>
</template>
