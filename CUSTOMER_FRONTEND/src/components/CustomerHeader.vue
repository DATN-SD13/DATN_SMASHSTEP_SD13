<script setup>
import { ref, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseIcon from './BaseIcon.vue'
import { useAuthStore } from '../stores/auth'
import logo from '../assets/logo3D.png'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menuOpen    = ref(false)
const searchOpen  = ref(false)
const userMenuOpen = ref(false)
const keyword     = ref('')
const searchInput = ref(null)

watch(() => route.fullPath, () => {
  menuOpen.value     = false
  searchOpen.value   = false
  userMenuOpen.value = false
  keyword.value = typeof route.query.keyword === 'string' ? route.query.keyword : ''
})

function toggleMenu() { menuOpen.value = !menuOpen.value; searchOpen.value = false }

async function openSearch() {
  menuOpen.value = false
  searchOpen.value = !searchOpen.value
  if (searchOpen.value) { await nextTick(); searchInput.value?.focus() }
}

function search() {
  router.push({ path: '/san-pham', query: keyword.value.trim() ? { keyword: keyword.value.trim().slice(0, 120) } : {} })
  searchOpen.value = false
}

function toggleUserMenu() { userMenuOpen.value = !userMenuOpen.value }

async function handleLogout() {
  userMenuOpen.value = false
  await auth.logout()
}

// Đóng user menu khi click ra ngoài
function onClickOutside(e) {
  if (!e.target.closest('.user-menu-wrap')) userMenuOpen.value = false
}
</script>

<template>
  <header class="customer-header" :class="{ 'customer-header--home': route.path === '/' }" @click.self="userMenuOpen = false">
    <div class="header-inner container">
      <RouterLink to="/" class="brand-logo" aria-label="SmashStep — Trang chủ">
        <img :src="logo" alt="Logo SmashStep">
        <span>SMASHSTEP<small>MOVE YOUR WAY</small></span>
      </RouterLink>

      <nav class="desktop-nav" aria-label="Điều hướng chính">
        <RouterLink to="/" :class="{ active: route.path === '/' && !route.hash }">Trang chủ</RouterLink>
        <RouterLink to="/san-pham" :class="{ active: route.path.startsWith('/san-pham') }">Sản phẩm</RouterLink>
        <RouterLink to="/#thuong-hieu">Thương hiệu</RouterLink>
        <RouterLink to="/#noi-bat">Nổi bật</RouterLink>
      </nav>

      <div class="header-actions">
        <!-- Icon tìm kiếm -->
        <button class="icon-button" aria-label="Tìm kiếm sản phẩm" :aria-expanded="searchOpen" aria-controls="header-search" @click="openSearch">
          <BaseIcon :name="searchOpen ? 'close' : 'search'" />
        </button>

        <!-- Đã đăng nhập: hiện avatar + dropdown -->
        <div v-if="auth.isAuthenticated" class="user-menu-wrap" v-click-outside="onClickOutside">
          <button class="user-avatar-btn" @click="toggleUserMenu" :aria-expanded="userMenuOpen" aria-label="Menu tài khoản">
            <img v-if="auth.user?.avatar" :src="auth.user.avatar" class="avatar-img" alt="Avatar" />
            <span v-else class="avatar-placeholder">{{ (auth.user?.name || 'K').charAt(0).toUpperCase() }}</span>
            <span class="user-name">{{ auth.user?.name || 'Tài khoản' }}</span>
            <svg class="chevron" :class="{ open: userMenuOpen }" viewBox="0 0 20 20" fill="currentColor" width="14" height="14"><path fill-rule="evenodd" d="M5.23 7.21a.75.75 0 011.06.02L10 11.17l3.71-3.94a.75.75 0 111.08 1.04l-4.25 4.5a.75.75 0 01-1.08 0l-4.25-4.5a.75.75 0 01.02-1.06z" clip-rule="evenodd"/></svg>
          </button>

          <div v-if="userMenuOpen" class="user-dropdown">
            <RouterLink to="/tai-khoan" class="dropdown-item">
              <svg viewBox="0 0 20 20" fill="currentColor" width="15"><path d="M10 10a4 4 0 100-8 4 4 0 000 8zm-7 8a7 7 0 0114 0H3z"/></svg>
              Tài khoản của tôi
            </RouterLink>
            <RouterLink to="/don-hang" class="dropdown-item">
              <svg viewBox="0 0 20 20" fill="currentColor" width="15"><path d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13l-1.4-7M7 13a2 2 0 100 4 2 2 0 000-4zm10 0a2 2 0 100 4 2 2 0 000-4z"/></svg>
              Đơn hàng của tôi
            </RouterLink>
            <div class="dropdown-divider"></div>
            <button class="dropdown-item dropdown-logout" @click="handleLogout">
              <svg viewBox="0 0 20 20" fill="currentColor" width="15"><path fill-rule="evenodd" d="M3 4a1 1 0 011-1h6a1 1 0 010 2H5v10h5a1 1 0 010 2H4a1 1 0 01-1-1V4zm12.293 4.293a1 1 0 011.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 011.414-1.414L12 9.586V6a1 1 0 012 0v3.586l1.293-1.293z" clip-rule="evenodd"/></svg>
              Đăng xuất
            </button>
          </div>
        </div>

        <!-- Chưa đăng nhập: nút Đăng nhập + Đăng ký -->
        <template v-else>
          <RouterLink to="/dang-nhap" class="auth-btn auth-btn--login">Đăng nhập</RouterLink>
          <RouterLink to="/dang-ky"   class="auth-btn auth-btn--register">Đăng ký</RouterLink>
        </template>

        <!-- Mobile menu button -->
        <button class="icon-button mobile-menu-button" aria-label="Mở menu" :aria-expanded="menuOpen" aria-controls="mobile-navigation" @click="toggleMenu">
          <BaseIcon :name="menuOpen ? 'close' : 'menu'" />
        </button>
      </div>
    </div>

    <!-- Search bar -->
    <form v-if="searchOpen" id="header-search" class="header-search container" role="search" @submit.prevent="search">
      <label class="sr-only" for="header-keyword">Tên hoặc mã sản phẩm</label>
      <BaseIcon name="search" />
      <input id="header-keyword" ref="searchInput" v-model="keyword" maxlength="120" placeholder="Bạn đang tìm đôi giày nào?" @keydown.esc="searchOpen = false">
      <button class="button button-primary" type="submit">Tìm kiếm</button>
    </form>

    <!-- Mobile nav -->
    <nav v-if="menuOpen" id="mobile-navigation" class="mobile-nav container" aria-label="Điều hướng trên điện thoại">
      <RouterLink to="/">Trang chủ</RouterLink>
      <RouterLink to="/san-pham">Sản phẩm</RouterLink>
      <RouterLink to="/#thuong-hieu">Thương hiệu</RouterLink>
      <RouterLink to="/#noi-bat">Nổi bật</RouterLink>
      <div class="mobile-auth" v-if="!auth.isAuthenticated">
        <RouterLink to="/dang-nhap" class="mobile-auth-link">Đăng nhập</RouterLink>
        <RouterLink to="/dang-ky"   class="mobile-auth-link mobile-auth-link--register">Đăng ký</RouterLink>
      </div>
      <template v-else>
        <RouterLink to="/tai-khoan">Tài khoản</RouterLink>
        <RouterLink to="/don-hang">Đơn hàng</RouterLink>
        <button class="mobile-logout" @click="handleLogout">Đăng xuất</button>
      </template>
    </nav>
  </header>
</template>

<style scoped>
.customer-header--home .header-inner{width:min(1100px,calc(100% - 64px));height:60px}
.customer-header--home .brand-logo>span{display:none}
.customer-header--home .brand-logo img{width:60px;height:42px}
.customer-header--home .desktop-nav{gap:28px}
.customer-header--home .desktop-nav a{font-size:12px;padding:18px 0}
.customer-header--home .desktop-nav a.active:after{bottom:10px}
@media(max-width:600px){.customer-header--home .header-inner{width:calc(100% - 32px);height:58px}}

/* ── Auth buttons ─────────────────────────────────────── */
.auth-btn {
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 14px;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
  transition: background .18s, color .18s, border-color .18s;
}

.auth-btn--login {
  color: #17365F;
  border: 1.5px solid #DDE6F0;
  background: transparent;
}
.auth-btn--login:hover {
  border-color: #2879E3;
  color: #2879E3;
}

.auth-btn--register {
  background: #2879E3;
  color: #fff;
  border: 1.5px solid #2879E3;
  margin-left: 6px;
}
.auth-btn--register:hover { background: #1F6BD1; border-color: #1F6BD1; }

/* ── User avatar button & dropdown ───────────────────── */
.user-menu-wrap {
  position: relative;
}

.user-avatar-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 34px;
  padding: 0 10px 0 4px;
  background: transparent;
  border: 1.5px solid #DDE6F0;
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: #17365F;
  transition: border-color .18s;
}
.user-avatar-btn:hover { border-color: #2879E3; color: #2879E3; }

.avatar-img {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #2879E3;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-name {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chevron {
  transition: transform .2s;
  color: #718096;
}
.chevron.open { transform: rotate(180deg); }

.user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 190px;
  background: #fff;
  border: 1px solid #DDE6F0;
  border-radius: 10px;
  box-shadow: 0 8px 28px rgba(16,42,76,.13);
  padding: 6px;
  z-index: 200;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 9px 10px;
  font-size: 13.5px;
  color: #17365F;
  text-decoration: none;
  border-radius: 7px;
  background: none;
  border: none;
  cursor: pointer;
  text-align: left;
  transition: background .15s;
}
.dropdown-item:hover { background: #EAF4FF; }
.dropdown-item svg { flex-shrink: 0; color: #718096; }

.dropdown-divider {
  height: 1px;
  background: #DDE6F0;
  margin: 5px 4px;
}

.dropdown-logout { color: #c62828; }
.dropdown-logout:hover { background: #fff5f5; }
.dropdown-logout svg { color: #c62828; }

/* ── Mobile auth ─────────────────────────────────────── */
.mobile-auth {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 4px;
  border-top: 1px solid #DDE6F0;
  margin-top: 4px;
}

.mobile-auth-link {
  display: block;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  color: #17365F;
  border: 1.5px solid #DDE6F0;
  text-align: center;
}

.mobile-auth-link--register {
  background: #2879E3;
  color: #fff;
  border-color: #2879E3;
}

.mobile-logout {
  width: 100%;
  padding: 10px 14px;
  background: none;
  border: 1.5px solid #fed7d7;
  border-radius: 8px;
  color: #c62828;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  text-align: center;
}

/* Ẩn text username trên màn nhỏ */
@media (max-width: 768px) {
  .auth-btn { display: none; }
  .user-name { display: none; }
}
</style>
