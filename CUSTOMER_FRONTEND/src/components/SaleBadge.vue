<script setup>
import { hasDiscount, discountPercent } from '../utils/money'
defineProps({ item: Object })
</script>
<template><span v-if="discountPercent(item?.phanTramGiamCaoNhat) || hasDiscount(item)" class="sale-badge">-{{ discountPercent(item?.phanTramGiamCaoNhat ?? item?.phanTramGiam) }}%</span></template>
