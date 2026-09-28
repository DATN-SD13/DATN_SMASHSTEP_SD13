<script setup>
import { RouterLink } from 'vue-router'

const logo = new URL('../assets/logo3D.png', import.meta.url).href
</script>
<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="brand"><img :src="logo" alt="SmashStep" /></div>
      <div class="sidebar-label">QUẢN TRỊ BÁN HÀNG</div>
      <nav class="menu">
        <RouterLink to="/"><span class="menu-icon"><i class="bi bi-house-door"></i></span><span>Trang chủ</span></RouterLink>
        <RouterLink to="/thong-ke"><span class="menu-icon"><i class="bi bi-bar-chart-line"></i></span><span>Thống kê</span></RouterLink>
        <RouterLink to="/ban-hang"><span class="menu-icon"><i class="bi bi-cart3"></i></span><span>Bán hàng</span></RouterLink>
        <RouterLink to="/hoa-don" class="active"><span class="menu-icon"><i class="bi bi-receipt"></i></span><span>Quản lý hóa đơn</span></RouterLink>
        <RouterLink to="/san-pham"><span class="menu-icon"><i class="bi bi-box-seam"></i></span><span>Quản lý sản phẩm</span><span class="menu-chevron"><i class="bi bi-chevron-down"></i></span></RouterLink>
        <RouterLink to="/thuoc-tinh"><span class="menu-icon"><i class="bi bi-sliders2"></i></span><span>Danh sách thuộc tính</span><span class="menu-chevron"><i class="bi bi-chevron-down"></i></span></RouterLink>
        <RouterLink to="/giam-gia"><span class="menu-icon"><i class="bi bi-tags"></i></span><span>Quản lý giảm giá</span><span class="menu-chevron"><i class="bi bi-chevron-down"></i></span></RouterLink>
        <RouterLink to="/tai-khoan"><span class="menu-icon"><i class="bi bi-people"></i></span><span>Quản lý tài khoản</span><span class="menu-chevron"><i class="bi bi-chevron-down"></i></span></RouterLink>
        <RouterLink to="/lich-lam-viec"><span class="menu-icon"><i class="bi bi-calendar3"></i></span><span>Lịch làm việc</span><span class="menu-chevron"><i class="bi bi-chevron-down"></i></span></RouterLink>
        <RouterLink to="/chat"><span class="menu-icon"><i class="bi bi-chat-dots"></i></span><span>Quản lý Chat</span></RouterLink>
      </nav>
      <div class="sidebar-bottom"><div class="online-dot"></div><div><strong>Hệ thống hoạt động</strong><span>SmashStep Admin</span></div></div>
    </aside>
    <section class="workspace">
      <header class="topbar">
        <div class="quick-search"><span><i class="bi bi-search"></i></span><input placeholder="Tìm kiếm nhanh..." /></div>
        <div class="account">
          <button class="notification" aria-label="Thông báo"><i class="bi bi-bell"></i><i class="notification-dot"></i></button>
          <div class="avatar">TA</div>
          <div class="account-info"><strong>Vũ Chí Tuấn Anh</strong><span>Nhân viên</span></div>
          <span class="chevron"><i class="bi bi-chevron-down"></i></span>
        </div>
      </header>
      <slot />
    </section>
  </div>
</template>

<style scoped>
:global(*){box-sizing:border-box}
:global(body){margin:0;font-family:Inter,Segoe UI,Roboto,Helvetica Neue,Arial,sans-serif;background:#f4f8fa;color:#263943}
.admin-shell{min-height:100vh;display:flex;background:linear-gradient(135deg,#f5f9fa 0%,#f1f6f8 100%)}
.sidebar{width:226px;flex:0 0 226px;background:#fff;border-right:1px solid #e5edf0;display:flex;flex-direction:column;min-height:100vh;position:sticky;top:0;height:100vh}
.brand{height:88px;display:flex;align-items:center;justify-content:center;border-bottom:1px solid #edf2f4}
.brand img{width:92px;height:70px;object-fit:contain}
.sidebar-label{padding:18px 18px 8px;font-size:8px;font-weight:800;letter-spacing:1.15px;color:#a4b1b7}
.menu{padding:0 10px}
.menu a{height:43px;display:flex;align-items:center;gap:12px;padding:0 13px;margin:3px 0;border-radius:9px;text-decoration:none;color:#667982;font-size:11.5px;font-weight:600;transition:.18s;position:relative}
.menu a span:nth-child(2){white-space:nowrap}
.menu-chevron{margin-left:auto;font-size:13px;color:#93a1a7;line-height:1}
.menu a:hover{background:#effbfd;color:#05a9c9;transform:translateX(1px)}
.menu a.active{background:linear-gradient(100deg,#08b6d4,#12a9cb);color:#fff;font-weight:750;box-shadow:0 8px 18px rgba(7,174,207,.22)}
.menu-icon{width:22px;height:22px;display:grid;place-items:center;flex:0 0 22px;color:#7a8c95}
.menu-icon i{font-size:16px;line-height:1}
.menu-chevron i{font-size:11px}
.menu a.active .menu-icon{color:#fff}
.menu a:hover .menu-icon{color:#08abc9}
.sidebar-bottom{margin:auto 12px 16px;padding:11px;border:1px solid #e8eff2;border-radius:11px;background:#fbfdfe;display:flex;gap:9px;align-items:center}
.sidebar-bottom strong{display:block;font-size:9px;color:#536771}
.sidebar-bottom span{display:block;font-size:8px;color:#a0adb3;margin-top:2px}
.online-dot{width:8px;height:8px;border-radius:50%;background:#13bf83;box-shadow:0 0 0 4px #e5faf2}
.workspace{min-width:0;flex:1}
.topbar{height:76px;background:rgba(255,255,255,.96);border-bottom:1px solid #e5edf0;display:flex;align-items:center;justify-content:space-between;padding:0 30px;backdrop-filter:blur(8px)}
.quick-search{width:260px;height:39px;background:#f6f9fa;border:1px solid #e2eaed;border-radius:10px;display:flex;align-items:center;padding:0 12px;color:#9aabb2}
.quick-search span{font-size:18px;margin-right:8px}
.quick-search input{width:100%;border:0;outline:0;background:transparent;color:#52656e;font-size:11px}
.account{display:flex;align-items:center;gap:10px}
.notification{position:relative;width:35px;height:35px;border:1px solid #e3ebee;border-radius:50%;background:#fff;color:#70838d;cursor:pointer}
.notification > i:first-child{font-size:15px}
.notification-dot{position:absolute;width:6px;height:6px;background:#08b5d2;border-radius:50%;top:7px;right:7px;border:1px solid #fff}
.avatar{width:35px;height:35px;border-radius:50%;display:grid;place-items:center;background:#102c38;color:#fff;font-weight:800;font-size:10px}
.account-info strong{display:block;font-size:11px;color:#2d3e47}
.account-info span{display:block;margin-top:3px;font-size:9px;color:#91a0a7}
.chevron{color:#70818b;margin-left:3px}
@media(max-width:760px){.sidebar{width:72px;flex-basis:72px}.brand img{width:56px}.sidebar-label,.menu a:not(.active){font-size:0}.menu a{justify-content:center;padding:0}.menu-icon{margin:0}.menu a.active{font-size:0}.sidebar-bottom{display:none}.topbar{padding:0 15px}.quick-search{width:180px}.account-info,.chevron{display:none}}
</style>