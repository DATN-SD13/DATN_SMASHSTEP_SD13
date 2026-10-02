<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const logo = new URL('../assets/logo3D.png', import.meta.url).href
const collapsed = ref(typeof window !== 'undefined' && window.innerWidth < 992)

// Menu theo Figma: Thống kê → Bán hàng → Hóa đơn → Sản phẩm → Khách hàng → Nhân viên → Giảm giá
const nav = [
  { label: 'Thống kê', icon: 'bi-grid-1x2', to: '/thong-ke' },
  { label: 'Bán hàng tại quầy', icon: 'bi-cart3', to: '/ban-hang' },
  { label: 'Quản lý hóa đơn', icon: 'bi-receipt', to: '/hoa-don' },
  {
    key: 'product', label: 'Quản lý sản phẩm', icon: 'bi-box-seam',
    children: [
      { label: 'Sản phẩm', to: '/san-pham' },
      { label: 'Biến thể sản phẩm', to: '/bien-the-san-pham' },
      { label: 'Thiết lập biến thể sản phẩm', to: '/thiet-lap-bien-the' }
    ]
  },
  { label: 'Quản lý khách hàng', icon: 'bi-people', to: '/khach-hang' },
  { label: 'Quản lý nhân viên', icon: 'bi-person-badge', to: '/nhan-vien' },
  {
    key: 'promo', label: 'Quản lý giảm giá', icon: 'bi-tags',
    children: [
      { label: 'Phiếu giảm giá', to: '/giam-gia' },
      { label: 'Đợt giảm giá', to: '/dot-giam-gia' }
    ]
  }
]

const isOn = (to) => route.path === to || route.path.startsWith(to + '/')
const groupActive = (item) => item.children ? item.children.some(c => isOn(c.to)) : isOn(item.to)
const open = reactive({ product: true, promo: true })

const crumbs = computed(() => ({
  parent: route.meta?.parent || null,
  title: route.meta?.title || ''
}))
</script>

<template>
  <div class="shell" :class="{ collapsed }">
    <aside class="sidebar">
      <div class="brand"><img :src="logo" alt="SmashStep" /></div>
      <div class="group-label">Quản trị cửa hàng</div>

      <nav class="menu">
        <template v-for="item in nav" :key="item.label">
          <!-- Mục có menu con -->
          <div v-if="item.children" class="nav-group">
            <button type="button" class="nav-item" :class="{ active: groupActive(item) }" :title="item.label" @click="open[item.key] = !open[item.key]">
              <i class="bi nav-icon" :class="item.icon"></i>
              <span class="nav-text">{{ item.label }}</span>
              <i class="bi bi-chevron-down chev" :class="{ up: open[item.key] }"></i>
            </button>
            <div v-show="open[item.key] && !collapsed" class="sub">
              <RouterLink v-for="c in item.children" :key="c.to" :to="c.to" class="sub-item" :class="{ on: isOn(c.to) }">
                <span class="bullet"></span><span>{{ c.label }}</span>
              </RouterLink>
            </div>
          </div>
          <!-- Mục thường -->
          <RouterLink v-else :to="item.to" class="nav-item" :class="{ active: groupActive(item) }" :title="item.label">
            <i class="bi nav-icon" :class="item.icon"></i>
            <span class="nav-text">{{ item.label }}</span>
          </RouterLink>
        </template>
      </nav>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div class="crumb">
          <button type="button" class="menu-btn" aria-label="Thu gọn menu" @click="collapsed = !collapsed"><i class="bi bi-list"></i></button>
          <template v-if="crumbs.parent">
            <RouterLink :to="crumbs.parent.to" class="crumb-parent">{{ crumbs.parent.label }}</RouterLink>
            <span class="crumb-slash">/</span>
          </template>
          <h1 class="crumb-current">{{ crumbs.title }}</h1>
        </div>

        <div class="right">
          <button type="button" class="pill shift"><i class="bi bi-arrow-left-right"></i><span>Ca làm việc</span></button>
          <button type="button" class="bell" aria-label="Thông báo">4</button>
          <button type="button" class="pill user">
            <span class="avatar">TA</span>
            <span class="user-name">Vũ Chí Tuấn Anh</span>
            <i class="bi bi-chevron-down"></i>
          </button>
        </div>
      </header>

      <slot />
    </section>
  </div>
</template>

<style scoped>
.shell { display: flex; min-height: 100vh; background: var(--ss-bg); }

/* ---------- Sidebar (248px) ---------- */
.sidebar {
  width: 248px; flex: 0 0 248px;
  background: #fff; border-right: 1px solid var(--ss-border);
  padding: 20px 16px; position: sticky; top: 0; height: 100vh;
  overflow-y: auto; overflow-x: hidden; z-index: 30;
  display: flex; flex-direction: column; gap: 14px;
  transition: width .2s, flex-basis .2s;
}
.sidebar::-webkit-scrollbar { width: 4px; }
.sidebar::-webkit-scrollbar-thumb { background: var(--ss-border); border-radius: 4px; }
.brand { height: 112px; flex: 0 0 112px; display: flex; align-items: center; justify-content: center; }
.brand img { width: 138px; height: 108px; object-fit: contain; display: block; }
.group-label { font-size: 10px; font-weight: 700; letter-spacing: 1.1px; text-transform: uppercase; color: var(--ss-faint); white-space: nowrap; }
.menu { display: flex; flex-direction: column; gap: 4px; }

.nav-item {
  width: 100%; height: 40px; padding: 0 12px;
  display: flex; align-items: center; gap: 12px;
  border: 1px solid transparent; border-radius: 8px;
  background: #fff; color: var(--ss-text);
  font-size: 12px; font-weight: 600; text-align: left; cursor: pointer;
  transition: background-color .15s, border-color .15s, color .15s;
}
.nav-item:hover { background: var(--ss-surface); }
.nav-item.active { background: var(--ss-primary-soft); border-color: var(--ss-primary); color: var(--ss-primary); }
.nav-icon { width: 18px; text-align: center; font-size: 15px; color: var(--ss-muted); flex: 0 0 18px; }
.nav-item.active .nav-icon { color: var(--ss-primary); }
.nav-text { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.chev { font-size: 10px; color: var(--ss-muted); transition: transform .2s; }
.chev.up { transform: rotate(180deg); }
.nav-item.active .chev { color: var(--ss-primary); }

.sub { display: flex; flex-direction: column; gap: 2px; padding: 2px 0 2px 30px; }
.sub-item {
  height: 30px; padding: 0 10px; border-radius: 6px;
  display: flex; align-items: center; gap: 8px;
  font-size: 11.5px; font-weight: 500; color: var(--ss-muted);
  transition: background-color .15s, color .15s;
}
.sub-item:hover { background: var(--ss-surface); color: var(--ss-text); }
.sub-item .bullet { width: 5px; height: 5px; border-radius: 50%; background: var(--ss-border); flex: 0 0 5px; }
.sub-item.on { color: var(--ss-primary); font-weight: 700; background: var(--ss-primary-soft); }
.nav-group .nav-item.active { border-color: transparent; box-shadow: inset 3px 0 0 var(--ss-primary); }
.sub-item.on .bullet { background: var(--ss-primary); }

/* thu gọn */
.shell.collapsed .sidebar { width: 72px; flex-basis: 72px; padding: 20px 10px; }
.shell.collapsed .brand img { width: 48px; height: 48px; }
.shell.collapsed .brand { height: 64px; flex-basis: 64px; }
.shell.collapsed .group-label, .shell.collapsed .nav-text, .shell.collapsed .chev { display: none; }
.shell.collapsed .nav-item { justify-content: center; padding: 0; }

/* ---------- Topbar (72px) ---------- */
.workspace { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.topbar {
  height: 72px; flex: 0 0 72px; padding: 0 28px;
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  background: #fff; border-bottom: 1px solid var(--ss-border);
  position: sticky; top: 0; z-index: 20;
}
.crumb { display: flex; align-items: center; gap: 10px; min-width: 0; }
.menu-btn {
  width: 40px; height: 40px; flex: 0 0 40px;
  border: 1px solid var(--ss-border); border-radius: 10px; background: #fff;
  color: var(--ss-muted); font-size: 19px; display: grid; place-items: center; cursor: pointer;
  transition: all .15s;
}
.menu-btn:hover { border-color: var(--ss-primary); color: var(--ss-primary); background: var(--ss-primary-soft); }
.crumb-parent { font-size: 16px; font-weight: 600; color: var(--ss-faint); white-space: nowrap; }
.crumb-parent:hover { color: var(--ss-primary); }
.crumb-slash { font-size: 16px; color: var(--ss-faint); }
.crumb-current { font-size: 17px; font-weight: 700; color: var(--ss-ink); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.right { display: flex; align-items: center; gap: 10px; flex: 0 0 auto; }
.pill {
  height: 38px; border: 1px solid var(--ss-border); background: #fff;
  border-radius: 999px; display: inline-flex; align-items: center; gap: 8px;
  font-size: 11.5px; font-weight: 600; color: var(--ss-text); cursor: pointer;
  transition: border-color .15s, background-color .15s;
}
.pill:hover { border-color: var(--ss-primary); }
.shift { padding: 0 18px; }
.shift i { color: var(--ss-primary); font-size: 13px; }
.bell {
  width: 38px; height: 38px; border-radius: 50%;
  border: 1px solid var(--ss-border); background: var(--ss-surface);
  color: var(--ss-primary); font-size: 11px; font-weight: 700; cursor: pointer;
  transition: border-color .15s;
}
.bell:hover { border-color: var(--ss-primary); }
.user { height: 40px; padding: 0 12px 0 6px; }
.avatar { width: 30px; height: 30px; border-radius: 50%; background: var(--ss-primary-soft); color: var(--ss-primary); display: grid; place-items: center; font-size: 10.5px; font-weight: 700; }
.user .bi { font-size: 10px; color: var(--ss-muted); }

@media (max-width: 991px) {
  .topbar { padding: 0 16px; }
  .shift span, .user-name { display: none; }
  .shift { padding: 0 12px; }
  .crumb-parent, .crumb-slash { display: none; }
}
</style>
