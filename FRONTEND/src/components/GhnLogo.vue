<script setup>
import ghnLogo from '../assets/ghn-logo.png'
</script>

<template>
  <img class="ghn-logo" :src="ghnLogo" alt="GHN" title="Giao Hàng Nhanh" width="60" height="18" />
</template>

<style scoped>
.ghn-logo {
  display: inline-block;
  width: 60px;
  height: 18px;
  flex-shrink: 0;
  object-fit: cover;
  object-position: 50% 46%;
  vertical-align: middle;
  mix-blend-mode: multiply;
}
</style>
