<script setup>
import { money, hasDiscount } from '../utils/money'
defineProps({ item: Object, from: Boolean })
</script>
<template><div class="product-price" v-if="item"><span class="price-prefix" v-if="from">Từ</span><strong>{{ money(item.giaSauGiam) }}</strong><del v-if="hasDiscount(item)">{{ money(item.giaGoc) }}</del></div></template>
