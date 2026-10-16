<script setup>
import { ref, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseIcon from './BaseIcon.vue'
import logo from '../assets/logo3D.png'
const route = useRoute(), router = useRouter(), menuOpen = ref(false), searchOpen = ref(false), keyword = ref(''), searchInput = ref(null)
watch(() => route.fullPath, () => { menuOpen.value = false; searchOpen.value = false; keyword.value = typeof route.query.keyword === 'string' ? route.query.keyword : '' })
function toggleMenu() { menuOpen.value = !menuOpen.value; searchOpen.value = false }
async function openSearch() { menuOpen.value = false; searchOpen.value = !searchOpen.value; if (searchOpen.value) { await nextTick(); searchInput.value?.focus() } }
function search() { router.push({ path: '/san-pham', query: keyword.value.trim() ? { keyword: keyword.value.trim().slice(0,120) } : {} }); searchOpen.value = false }
</script>
<template><header class="customer-header" :class="{ 'customer-header--home': route.path === '/' }"><div class="header-inner container"><RouterLink to="/" class="brand-logo" aria-label="SmashStep — Trang chủ"><img :src="logo" alt="Logo SmashStep"><span>SMASHSTEP<small>MOVE YOUR WAY</small></span></RouterLink><nav class="desktop-nav" aria-label="Điều hướng chính"><RouterLink to="/" :class="{ active: route.path === '/' && !route.hash }">Trang chủ</RouterLink><RouterLink to="/san-pham" :class="{ active: route.path.startsWith('/san-pham') }">Sản phẩm</RouterLink><RouterLink to="/#thuong-hieu">Thương hiệu</RouterLink><RouterLink to="/#noi-bat">Nổi bật</RouterLink></nav><div class="header-actions"><button class="icon-button" aria-label="Tìm kiếm sản phẩm" :aria-expanded="searchOpen" aria-controls="header-search" @click="openSearch"><BaseIcon :name="searchOpen ? 'close' : 'search'" /></button><button class="icon-button mobile-menu-button" aria-label="Mở menu" :aria-expanded="menuOpen" aria-controls="mobile-navigation" @click="toggleMenu"><BaseIcon :name="menuOpen ? 'close' : 'menu'" /></button></div></div><form v-if="searchOpen" id="header-search" class="header-search container" role="search" @submit.prevent="search"><label class="sr-only" for="header-keyword">Tên hoặc mã sản phẩm</label><BaseIcon name="search" /><input id="header-keyword" ref="searchInput" v-model="keyword" maxlength="120" placeholder="Bạn đang tìm đôi giày nào?" @keydown.esc="searchOpen = false"><button class="button button-primary" type="submit">Tìm kiếm</button></form><nav v-if="menuOpen" id="mobile-navigation" class="mobile-nav container" aria-label="Điều hướng trên điện thoại"><RouterLink to="/">Trang chủ</RouterLink><RouterLink to="/san-pham">Sản phẩm</RouterLink><RouterLink to="/#thuong-hieu">Thương hiệu</RouterLink><RouterLink to="/#noi-bat">Nổi bật</RouterLink></nav></header></template>

<style scoped>
.customer-header--home .header-inner{width:min(1100px,calc(100% - 64px));height:60px}
.customer-header--home .brand-logo>span{display:none}
.customer-header--home .brand-logo img{width:60px;height:42px}
.customer-header--home .desktop-nav{gap:28px}
.customer-header--home .desktop-nav a{font-size:12px;padding:18px 0}
.customer-header--home .desktop-nav a.active:after{bottom:10px}
@media(max-width:600px){.customer-header--home .header-inner{width:calc(100% - 32px);height:58px}}
</style>
