<script setup>
import { ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseIcon from './BaseIcon.vue'
import NotificationDropdown from './NotificationDropdown.vue'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'
import { useLiveRefresh } from '../composables/useLiveRefresh'
import logo from '../assets/logo3D.png'
const route=useRoute(), router=useRouter(), auth=useAuthStore(), cart=useCartStore()
const menuOpen=ref(false), searchOpen=ref(false), userOpen=ref(false), keyword=ref(''), searchInput=ref(null), accountRoot=ref(null)
const links=[{to:'/',label:'Trang chủ'},{to:'/san-pham',label:'Sản phẩm'},{to:'/#noi-bat',label:'Nổi bật'},{to:'/don-hang',label:'Theo dõi đơn hàng'},{to:'/goi-y-giay',label:'Gợi ý giày'},{to:'/ve-chung-toi',label:'Về chúng tôi'}]
function active(to) { return to.includes('#') ? route.fullPath===to : to==='/' ? route.path==='/'&&!route.hash : route.path===to || route.path.startsWith(to+'/') }
watch(()=>route.fullPath,()=>{menuOpen.value=false;searchOpen.value=false;userOpen.value=false;keyword.value=typeof route.query.keyword==='string'?route.query.keyword:''})
async function openSearch(){menuOpen.value=false;userOpen.value=false;searchOpen.value=!searchOpen.value;if(searchOpen.value){await nextTick();searchInput.value?.focus()}}
function search(){router.push({path:'/san-pham',query:keyword.value.trim()?{keyword:keyword.value.trim().slice(0,120)}:{}});searchOpen.value=false}
function outside(e){if(!accountRoot.value?.contains(e.target)) userOpen.value=false}
function escape(e){if(e.key==='Escape'){menuOpen.value=false;searchOpen.value=false;userOpen.value=false}}
onMounted(()=>{cart.refresh();window.addEventListener('storage',cart.syncStorage);document.addEventListener('pointerdown',outside);document.addEventListener('keydown',escape)})
onBeforeUnmount(()=>{window.removeEventListener('storage',cart.syncStorage);document.removeEventListener('pointerdown',outside);document.removeEventListener('keydown',escape)})
useLiveRefresh(cart.refresh)
</script>
<template>
<header class="customer-header commerce-header">
  <div class="header-inner container">
    <RouterLink to="/" class="brand-logo" aria-label="SmashStep — Trang chủ"><img :src="logo" alt="Logo SmashStep"><span>SMASHSTEP<small>MOVE YOUR WAY</small></span></RouterLink>
    <nav class="desktop-nav" aria-label="Điều hướng chính"><RouterLink v-for="link in links" :key="link.to" :to="link.to" :class="{active:active(link.to)}" :aria-current="active(link.to)?'page':undefined">{{link.label}}</RouterLink></nav>
    <div class="header-actions">
      <button class="icon-button header-action" type="button" aria-label="Tìm kiếm sản phẩm" :aria-expanded="searchOpen" aria-controls="header-search" @click="openSearch"><BaseIcon :name="searchOpen?'close':'search'"/></button>
      <NotificationDropdown/>
      <div ref="accountRoot" class="user-menu-wrap">
        <button class="icon-button header-action user-avatar-btn" type="button" aria-label="Menu tài khoản" :aria-expanded="userOpen" aria-controls="customer-account-menu" @click="userOpen=!userOpen;searchOpen=false">
          <img v-if="auth.user?.avatar" :src="auth.user.avatar" alt="Avatar" class="avatar-img"><span v-else-if="auth.isAuthenticated" class="avatar-placeholder">{{(auth.user?.name||'K').charAt(0).toUpperCase()}}</span><BaseIcon v-else name="user"/>
        </button>
        <div v-if="userOpen" id="customer-account-menu" class="user-dropdown">
          <template v-if="auth.isAuthenticated"><div class="account-menu-heading"><strong>{{auth.user?.name||'Tài khoản'}}</strong><small>{{auth.user?.email}}</small></div><RouterLink to="/tai-khoan" class="dropdown-item"><BaseIcon name="user"/>Tài khoản của tôi</RouterLink><RouterLink to="/don-hang" class="dropdown-item"><BaseIcon name="box"/>Đơn hàng của tôi</RouterLink><button class="dropdown-item" @click="userOpen=false;auth.logout()">Đăng xuất</button></template>
          <template v-else><div class="account-menu-heading"><strong>Chào mừng đến SmashStep</strong><small>Đăng nhập để mua sắm thuận tiện hơn.</small></div><RouterLink to="/dang-nhap" class="dropdown-item">Đăng nhập</RouterLink><RouterLink to="/dang-ky" class="dropdown-item">Tạo tài khoản</RouterLink></template>
        </div>
      </div>
      <RouterLink to="/gio-hang" class="icon-button header-action" :aria-label="'Giỏ hàng, '+cart.count+' sản phẩm'"><BaseIcon name="cart"/><span v-if="cart.count" class="header-badge">{{cart.count>99?'99+':cart.count}}</span></RouterLink>
      <button class="icon-button mobile-menu-button" type="button" aria-label="Mở menu" :aria-expanded="menuOpen" aria-controls="mobile-navigation" @click="menuOpen=!menuOpen;searchOpen=false"><BaseIcon :name="menuOpen?'close':'menu'"/></button>
    </div>
  </div>
  <form v-if="searchOpen" id="header-search" class="header-search container" role="search" @submit.prevent="search"><BaseIcon name="search"/><label class="sr-only" for="header-keyword">Tìm sản phẩm</label><input id="header-keyword" ref="searchInput" v-model="keyword" type="search" maxlength="120" placeholder="Tìm đôi giày tiếp theo của bạn…"><button class="button button-primary">Tìm kiếm</button></form>
  <nav v-if="menuOpen" id="mobile-navigation" class="mobile-nav container" aria-label="Điều hướng điện thoại"><RouterLink v-for="link in links" :key="link.to" :to="link.to" :class="{active:active(link.to)}">{{link.label}}</RouterLink><RouterLink to="/tai-khoan">Tài khoản của tôi</RouterLink><RouterLink v-if="!auth.isAuthenticated" to="/dang-nhap">Đăng nhập / Đăng ký</RouterLink></nav>
</header>
</template>
