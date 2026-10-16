<script setup>
import logo from '../assets/logo3D.png'
</script>
<template><footer class="customer-footer"><div class="container footer-inner"><RouterLink to="/" class="brand-logo"><img :src="logo" alt="Logo SmashStep"><span>SMASHSTEP<small>MOVE YOUR WAY</small></span></RouterLink><p>Chọn đôi giày phù hợp. Tạo dấu ấn của riêng bạn.</p><nav aria-label="Điều hướng cuối trang"><RouterLink to="/san-pham">Bộ sưu tập</RouterLink><RouterLink to="/#thuong-hieu">Thương hiệu</RouterLink></nav></div><div class="container footer-bottom">© {{ new Date().getFullYear() }} SmashStep</div></footer></template>
