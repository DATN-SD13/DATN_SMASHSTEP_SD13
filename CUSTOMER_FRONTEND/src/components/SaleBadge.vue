<script setup>
import { hasDiscount, discountPercent } from '../utils/money'
defineProps({ item: Object })
</script>
<template><span v-if="hasDiscount(item)" class="sale-badge">-{{ discountPercent(item.phanTramGiam) }}%</span></template>
