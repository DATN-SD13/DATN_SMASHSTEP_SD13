<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const logo = new URL('../assets/logo3D.png', import.meta.url).href
const productOpen = ref(true)
const promotionOpen = ref(true)
const isPromotion = computed(() => route.path.startsWith('/giam-gia') || route.path.startsWith('/dot-giam-gia'))
const isProduct = computed(() => route.path.startsWith('/san-pham') || route.path.startsWith('/bien-the-san-pham'))
</script>

<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="brand"><img :src="logo" alt="SmashStep" /></div>
      <nav class="menu">
        <RouterLink to="/thong-ke" class="menu-item"><i class="bi bi-grid-1x2-fill"></i><span>Thống kê</span></RouterLink>
        <RouterLink to="/ban-hang" class="menu-item"><i class="bi bi-cart3"></i><span>Bán hàng tại quầy</span></RouterLink>
        <RouterLink to="/hoa-don" class="menu-item"><i class="bi bi-receipt-cutoff"></i><span>Quản lý hóa đơn</span></RouterLink>

        <button class="menu-item menu-toggle" :class="{ open: productOpen, active: isProduct }" @click="productOpen = !productOpen">
          <i class="bi bi-box-seam-fill"></i><span>Quản lý sản phẩm</span><i class="bi bi-chevron-down arrow"></i>
        </button>
        <div v-show="productOpen" class="submenu">
          <RouterLink to="/san-pham"><i class="bi bi-box-fill"></i><span>Sản phẩm</span></RouterLink>
          <RouterLink to="/bien-the-san-pham"><i class="bi bi-layers-fill"></i><span>Biến thể sản phẩm</span></RouterLink>
        </div>

        <RouterLink to="/thuoc-tinh" class="menu-item"><i class="bi bi-sliders2-vertical"></i><span>Danh sách thuộc tính</span></RouterLink>

        <button class="menu-item menu-toggle" :class="{ open: promotionOpen, active: isPromotion }" @click="promotionOpen = !promotionOpen">
          <i class="bi bi-tags-fill"></i><span>Quản lý giảm giá</span><i class="bi bi-chevron-down arrow"></i>
        </button>
        <div v-show="promotionOpen" class="submenu">
          <RouterLink to="/giam-gia" :class="{ selected: route.path.startsWith('/giam-gia') }"><i class="bi bi-ticket-perforated-fill"></i><span>Phiếu giảm giá</span></RouterLink>
          <RouterLink to="/dot-giam-gia" :class="{ selected: route.path.startsWith('/dot-giam-gia') }"><i class="bi bi-tag-fill"></i><span>Đợt giảm giá</span></RouterLink>
        </div>

        <RouterLink to="/tai-khoan" class="menu-item"><i class="bi bi-people-fill"></i><span>Quản lý tài khoản</span></RouterLink>
      </nav>

      <div class="sidebar-bottom"><span class="online-dot"></span><div><strong>Hệ thống hoạt động</strong><small>SmashStep Admin</small></div></div>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div class="topbar-left"><button class="collapse" aria-label="Menu"><i class="bi bi-list"></i></button><div class="top-title"></div></div>
        <div class="account"><button class="notification" aria-label="Thông báo"><i class="bi bi-bell-fill"></i><b></b></button><div class="avatar">TA</div><div class="account-info"><strong>Vũ Chí Tuấn Anh</strong><span>Nhân viên</span></div><i class="bi bi-chevron-down chevron"></i></div>
      </header>
      <slot />
    </section>
  </div>
</template>

<style scoped>
:global(*){box-sizing:border-box}:global(html),:global(body),:global(#app){margin:0;min-height:100%;font-family:Inter,Segoe UI,Roboto,Arial,sans-serif}:global(body){background:#eef4f7;color:#14232c}:global(button),:global(input),:global(select),:global(textarea){font-family:inherit}
.admin-shell{min-height:100vh;display:flex;background:#eef4f7}.sidebar{width:252px;flex:0 0 252px;background:#fff;border-right:1px solid #d8e3e9;display:flex;flex-direction:column;min-height:100vh;position:sticky;top:0;height:100vh;z-index:30}.brand{height:112px;display:flex;align-items:center;justify-content:center;border-bottom:1px solid #e3ebef;background:#fff}.brand img{width:126px;height:88px;object-fit:contain;display:block}
.menu{padding:20px 13px 12px;overflow-y:auto}.menu::-webkit-scrollbar{width:4px}.menu::-webkit-scrollbar-thumb{background:#cbdce5;border-radius:5px}.menu-item{width:100%;min-height:46px;display:flex;align-items:center;gap:12px;padding:0 14px;margin:4px 0;border:1px solid transparent;background:transparent;border-radius:11px;text-decoration:none;color:#172831;font-size:13px;font-weight:700;cursor:pointer;text-align:left;transition:.16s}.menu-item>i:first-child{width:20px;text-align:center;font-size:16px;color:#3b5664}.menu-item .arrow{margin-left:auto;font-size:11px;color:#526b78;transition:.16s}.menu-toggle.open .arrow{transform:rotate(0)}.menu-item:hover{background:#edf8fc;color:#087cb8}.menu-item:hover>i:first-child{color:#087cb8}.menu-item.active{color:#066f9f;background:#dff2fa;border-color:#c3e5f3}.menu-item.active>i:first-child{color:#087cb8}.submenu{margin:1px 0 8px 29px;padding-left:12px;border-left:2px solid #d8ebf3}.submenu a{min-height:38px;display:flex;align-items:center;gap:10px;padding:0 12px;border-radius:9px;text-decoration:none;color:#465e6a;font-size:12px;font-weight:650;margin:3px 0;transition:.16s}.submenu a i{width:16px;text-align:center;font-size:12px}.submenu a:hover{background:#f1f9fc;color:#087cb8}.submenu a.selected{background:#e5f5fb;color:#0575ac;font-weight:800;box-shadow:inset 3px 0 0 #0b84bd}.sidebar-bottom{margin:auto 13px 16px;padding:13px 14px;border:1px solid #d7e7ed;border-radius:12px;background:#f5fbfd;display:flex;gap:11px;align-items:center}.sidebar-bottom strong{display:block;font-size:11px;color:#18303b}.sidebar-bottom small{display:block;font-size:10px;color:#607782;margin-top:3px}.online-dot{width:9px;height:9px;flex:0 0 9px;border-radius:50%;background:#16b67a;box-shadow:0 0 0 4px #dff8ed}
.workspace{min-width:0;flex:1}.topbar{height:72px;background:#fff;border-bottom:1px solid #d8e3e9;display:flex;align-items:center;justify-content:space-between;padding:0 30px;position:sticky;top:0;z-index:20;box-shadow:0 2px 10px rgba(26,61,76,.035)}.topbar-left{display:flex;align-items:center;gap:13px}.collapse{width:38px;height:38px;border:1px solid #d5e1e7;background:#fff;border-radius:10px;color:#1e3540;font-size:19px;display:grid;place-items:center;cursor:pointer}.top-title{display:flex;flex-direction:column;line-height:1.05}.top-title span{font-size:14px;letter-spacing:.8px;font-weight:900;color:#102f3c}.top-title small{font-size:8px;letter-spacing:1.2px;font-weight:700;color:#6b808b;margin-top:4px}.account{display:flex;align-items:center;gap:11px}.notification{position:relative;width:38px;height:38px;border:1px solid #d9e5ea;border-radius:50%;background:#fff;color:#27434f;cursor:pointer}.notification b{position:absolute;width:7px;height:7px;background:#0b84bd;border-radius:50%;right:6px;top:6px;border:2px solid #fff}.avatar{width:38px;height:38px;border-radius:50%;display:grid;place-items:center;background:#102f3c;color:#fff;font-size:11px;font-weight:900}.account-info strong{display:block;font-size:12px;color:#162a34}.account-info span{display:block;font-size:10px;color:#68808b;margin-top:3px}.chevron{font-size:11px;color:#5e747f;margin-left:2px}@media(max-width:900px){.sidebar{width:82px;flex-basis:82px}.brand img{width:68px;height:68px}.menu{padding:15px 8px}.menu-item{justify-content:center;padding:0}.menu-item span,.submenu,.sidebar-bottom,.menu-item .arrow,.top-title,.account-info,.chevron{display:none}.menu-item>i:first-child{margin:0}.topbar{padding:0 15px}}
</style>
