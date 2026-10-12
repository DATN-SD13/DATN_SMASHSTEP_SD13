<script setup>
import { discountPercent, hasDiscount } from '../services/discountBadge'
defineProps({ item: Object, range: Boolean })
</script>

<template>
  <span v-if="hasDiscount(item, range)" class="p-discount-badge" :aria-label="'Giảm ' + discountPercent(item, range) + '%'">
    -{{ discountPercent(item, range) }}%
  </span>
</template>

<style scoped>
.p-discount-badge{position:absolute;top:4px;left:4px;z-index:2;padding:2px 4px;border-radius:4px;background:#dc3545;color:#fff;font-size:11px;font-weight:700;line-height:1.2;white-space:nowrap;pointer-events:none}
</style>
