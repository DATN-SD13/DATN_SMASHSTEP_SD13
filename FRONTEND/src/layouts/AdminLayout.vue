<script setup>
import { computed, ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import api from '../utils/api'
import { avatarUrl } from '../utils/avatar'
import { showError } from '../utils/feedback'
import AdminAiWidget from '../modules/admin/admin-ai/components/AdminAiWidget.vue'

const route     = useRoute()
const router    = useRouter()
const authStore = useAuthStore()
const logo      = new URL('../assets/logo3D.png', import.meta.url).href

// directive click-outside
const vClickOutside = {
  mounted(el, binding) {
    el._clickOutside = (e) => { if (!el.contains(e.target)) binding.value(e) }
    document.addEventListener('click', el._clickOutside)
  },
  unmounted(el) { document.removeEventListener('click', el._clickOutside) }
}
const collapsed = ref(typeof window !== 'undefined' && window.innerWidth < 992)

const userAvatarInitials = computed(() => {
  const name = authStore.user?.name || ''
  return name.split(' ').slice(-2).map(w => w[0]).join('').toUpperCase() || 'NV'
})

const avatarFailed = ref(false)
const userAvatarUrl = computed(() => avatarUrl(authStore.user?.avatar))
watch(userAvatarUrl, () => { avatarFailed.value = false })

const userName = computed(() => authStore.user?.name || 'Nhân viên')

const showUserMenu = ref(false)
const showLogoutConfirm = ref(false)

function toggleUserMenu() { showUserMenu.value = !showUserMenu.value }
function closeUserMenu() { showUserMenu.value = false }

function confirmLogout() {
  showUserMenu.value = false
  showLogoutConfirm.value = true
}

async function doLogout() {
  if (authStore.loggingOut) return
  try {
    if (await authStore.logout()) showLogoutConfirm.value = false
  } catch { showError('Chưa lưu được trạng thái trước khi đăng xuất. Phiên đăng nhập vẫn được giữ.', 'Chưa thể đăng xuất') }
}

// ── Panel ca làm việc ─────────────────────────────────────
const showCaPanel    = ref(false)
const caList         = ref([])
const caLoading      = ref(false)

// Số ca hôm nay của nhân viên đang đăng nhập (badge xanh trong button)
const caNhanVienCount = ref(0)
// Số thứ tự ca hiện tại của cửa hàng (nút tròn bên ngoài, vd: 1=sáng, 2=chiều, 3=tối)
const caHienTaiSo     = ref(0)

// ── Panel ca cửa hàng (nút tròn bên ngoài) ────────────────
const showCaActivePanel  = ref(false)
const caActiveList       = ref([])
const caActiveLoading    = ref(false)

async function openCaActivePanel() {
  showCaActivePanel.value = true
  if (caActiveLoading.value) return
  caActiveLoading.value = true
  try {
    const res       = await api.get('/ca-lam-viec/active')
    caActiveList.value = res.data?.data || []
    caHienTaiSo.value  = soThuTuCaHienTai(caActiveList.value)
  } catch { caActiveList.value = [] }
  finally { caActiveLoading.value = false }
}

// Trả về true nếu ca này đang diễn ra ngay lúc này
function laCaHienTai(ca) {
  const now    = new Date()
  const nowMin = now.getHours() * 60 + now.getMinutes()
  const bd = ca.gioBatDau || ca.thoiGian?.split(' - ')[0]
  const kt = ca.gioKetThuc || ca.thoiGian?.split(' - ')[1]
  if (!bd || !kt) return false
  const [bh, bm] = bd.split(':').map(Number)
  const [kh, km] = kt.split(':').map(Number)
  const bdMin = bh * 60 + bm
  const ktMin = kh * 60 + km
  if (ktMin < bdMin) return nowMin >= bdMin || nowMin <= ktMin
  return nowMin >= bdMin && nowMin <= ktMin
}

const STATUS_CA = {
  0: { label: 'Chờ giao ca',  cls: 'warn'    },
  1: { label: 'Giao ca',      cls: 'info'    },
  2: { label: 'Vắng mặt',    cls: 'danger'  },
  3: { label: 'Đã kết thúc', cls: 'success' }
}

function caStatusInfo(trangThai) {
  return STATUS_CA[trangThai] || { label: 'Không xác định', cls: '' }
}

function fmtTime(str) {
  if (!str) return ''
  return str.replace('\n', ' ')
}

// Trả về số thứ tự (1-based) của ca đang diễn ra ngay lúc này trong danh sách
// activeCas đã được sort theo gioBatDau tăng dần từ backend
function soThuTuCaHienTai(activeCas) {
  const now    = new Date()
  const nowMin = now.getHours() * 60 + now.getMinutes()
  const idx = activeCas.findIndex(ca => {
    const bd = ca.gioBatDau || ca.thoiGian?.split(' - ')[0]
    const kt = ca.gioKetThuc || ca.thoiGian?.split(' - ')[1]
    if (!bd || !kt) return false
    const [bh, bm] = bd.split(':').map(Number)
    const [kh, km] = kt.split(':').map(Number)
    const bdMin = bh * 60 + bm
    const ktMin = kh * 60 + km
    if (ktMin < bdMin) return nowMin >= bdMin || nowMin <= ktMin   // ca qua đêm
    return nowMin >= bdMin && nowMin <= ktMin
  })
  return idx === -1 ? 0 : idx + 1
}

async function loadCaData() {
  try {
    const [resActive, resHoNay] = await Promise.all([
      api.get('/ca-lam-viec/active'),
      api.get('/lich-lam-viec/ca-hom-nay')
    ])
    const activeCas      = resActive.data?.data || []
    caHienTaiSo.value    = soThuTuCaHienTai(activeCas)
    caList.value         = resHoNay.data?.data || []
    caNhanVienCount.value = caList.value.length
  } catch { /* ignore */ }
}

async function openCaPanel() {
  showCaPanel.value = true
  if (caLoading.value) return
  caLoading.value = true
  try { await loadCaData() }
  finally { caLoading.value = false }
}

onMounted(loadCaData)

let sessionTimer
const refreshPermission = () => {
  if (document.visibilityState === 'hidden' || !authStore.isAuthenticated) return
  authStore.refreshSession().catch(() => {})
}
onMounted(() => {
  refreshPermission()
  sessionTimer = window.setInterval(refreshPermission, 10000)
  window.addEventListener('focus', refreshPermission)
  document.addEventListener('visibilitychange', refreshPermission)
})
onBeforeUnmount(() => {
  clearInterval(sessionTimer)
  window.removeEventListener('focus', refreshPermission)
  document.removeEventListener('visibilitychange', refreshPermission)
})
watch(() => [authStore.role, route.fullPath], () => {
  if (authStore.isAuthenticated && route.meta?.role && route.meta.role !== authStore.role)
    router.replace(authStore.isAdmin ? '/thong-ke-tong-hop' : '/ban-hang')
})


// Menu quản trị cửa hàng
const nav = [
  { label: 'Thống kê tổng hợp', icon: 'bi-bar-chart-line', to: '/thong-ke-tong-hop', adminOnly: true },
  { label: 'Bán hàng tại quầy', icon: 'bi-cart3', to: '/ban-hang' },
  { label: 'Quản lý hóa đơn', icon: 'bi-receipt', to: '/hoa-don' },
  {
    key: 'product', label: 'Quản lý sản phẩm', icon: 'bi-box-seam', adminOnly: true,
    children: [
      { label: 'Sản phẩm', to: '/san-pham' },
      { label: 'Biến thể sản phẩm', to: '/bien-the-san-pham' }
    ]
  },
  {
    key: 'attributes', label: 'Thuộc tính', icon: 'bi-sliders', adminOnly: true,
    children: [
      { label: 'Thương hiệu', to: '/thuoc-tinh/thuong-hieu' },
      { label: 'Danh mục', to: '/thuoc-tinh/danh-muc' },
      { label: 'Chất liệu', to: '/thuoc-tinh/chat-lieu' },
      { label: 'Kiểu dáng', to: '/thuoc-tinh/kieu-dang' },
      { label: 'Cổ giày', to: '/thuoc-tinh/co-giay' },
      { label: 'Xuất xứ', to: '/thuoc-tinh/xuat-xu' },
      { label: 'Màu sắc', to: '/thuoc-tinh/mau-sac' },
      { label: 'Kích thước', to: '/thuoc-tinh/kich-thuoc' }
    ]
  },
  {
    key: 'promo', label: 'Quản lý giảm giá', icon: 'bi-tags', adminOnly: true,
    children: [
      { label: 'Phiếu giảm giá', to: '/giam-gia' },
      { label: 'Đợt giảm giá', to: '/dot-giam-gia' }
    ]
  },
  { label: 'Quản lý khách hàng', icon: 'bi-people', to: '/khach-hang' },
  { label: 'Hỗ trợ trực tuyến', icon: 'bi-chat-dots', to: '/ho-tro-truc-tuyen' },
  { label: 'Quản lý nhân viên', icon: 'bi-person-badge', to: '/nhan-vien', adminOnly: true },
  {
    key: 'schedule', label: 'Quản lý lịch làm', icon: 'bi-calendar-week', adminOnly: true,
    children: [
      { label: 'Lịch làm việc',     to: '/lich-lam-viec' },
      { label: 'Lịch ca làm',       to: '/ca-lam-viec'   },
      { label: 'Lịch sử hoạt động', to: '/lich-su-hoat-dong' }
    ]
  }
]

const isOn = (to) => route.path === to || route.path.startsWith(to + '/')
const groupActive = (item) => item.children ? item.children.some(c => isOn(c.to)) : isOn(item.to)
const openGroup = ref(null)

watch(() => route.path, () => {
  openGroup.value = nav.find(item => item.children && groupActive(item))?.key ?? null
}, { immediate: true })

function toggleGroup(key) {
  openGroup.value = openGroup.value === key ? null : key
}

// Chỉ hiện menu có adminOnly khi role là ADMIN
const visibleNav = computed(() =>
  nav.filter(item => !item.adminOnly || authStore.isAdmin)
)

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
        <template v-for="item in visibleNav" :key="item.label">
          <!-- Mục có menu con -->
          <div v-if="item.children" class="nav-group">
            <button type="button" class="nav-item" :class="{ active: groupActive(item) }" :title="item.label" :aria-expanded="openGroup === item.key && !collapsed" @click="toggleGroup(item.key)">
              <i class="bi nav-icon" :class="item.icon"></i>
              <span class="nav-text">{{ item.label }}</span>
              <i class="bi bi-chevron-down chev" :class="{ up: openGroup === item.key }"></i>
            </button>
            <div v-show="openGroup === item.key && !collapsed" class="sub">
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
          <button type="button" class="pill shift" @click="openCaPanel">
            <i class="bi bi-arrow-left-right"></i>
            <span>Ca làm việc</span>
            <span v-if="caNhanVienCount > 0" class="ca-badge">{{ caNhanVienCount }}</span>
          </button>
          <button type="button" class="bell" @click="openCaActivePanel" :title="caHienTaiSo ? 'Đang ở ca ' + caHienTaiSo : 'Ngoài giờ ca'">{{ caHienTaiSo || '—' }}</button>

          <!-- User dropdown -->
          <div class="user-wrap" v-click-outside="closeUserMenu">
            <button type="button" class="pill user" @click="toggleUserMenu">
              <span class="avatar">
                <img v-if="userAvatarUrl && !avatarFailed" :src="userAvatarUrl" alt="Ảnh đại diện" @error="avatarFailed = true" />
                <span v-else>{{ userAvatarInitials }}</span>
              </span>
              <span class="user-name">{{ userName }}</span>
              <i class="bi bi-chevron-down" :class="{ 'chev-up': showUserMenu }"></i>
            </button>
            <div v-if="showUserMenu" class="user-dropdown">
              <RouterLink to="/ca-nhan" class="user-dd-item" @click="closeUserMenu">
                <i class="bi bi-person-circle"></i>
                Trang cá nhân
              </RouterLink>
              <div class="user-dd-divider"></div>
              <button type="button" class="user-dd-item user-dd-logout" @click="confirmLogout">
                <i class="bi bi-box-arrow-right"></i>
                Đăng xuất
              </button>
            </div>
          </div>

        </div>

      </header>

      <slot />
    </section>
  </div>

  <AdminAiWidget v-if="authStore.isAdmin" />

  <!-- Panel ca làm việc hôm nay -->
  <Teleport to="body">
    <Transition name="ca-panel">
      <div v-if="showCaPanel" class="ca-overlay" @click.self="showCaPanel=false">
        <div class="ca-panel">
          <!-- Header -->
          <div class="ca-panel-head">
            <div class="ca-panel-title">
              <i class="bi bi-calendar-check"></i>
              <span>Ca làm việc hôm nay</span>
            </div>
            <button class="ca-close" @click="showCaPanel=false"><i class="bi bi-x-lg"></i></button>
          </div>

          <!-- Ngày -->
          <div class="ca-date-label">
            <i class="bi bi-calendar3"></i>
            {{ new Date().toLocaleDateString('vi-VN', { weekday:'long', day:'2-digit', month:'long', year:'numeric' }) }}
          </div>

          <!-- Loading -->
          <div v-if="caLoading" class="ca-empty">
            <i class="bi bi-arrow-repeat ca-spin"></i> Đang tải...
          </div>

          <!-- Không có ca -->
          <div v-else-if="!caList.length" class="ca-empty">
            <i class="bi bi-calendar-x" style="font-size:32px;color:var(--ss-faint)"></i>
            <p>Bạn không có ca làm việc hôm nay</p>
          </div>

          <!-- Danh sách ca -->
          <div v-else class="ca-list">
            <div v-for="ca in caList" :key="ca.id" class="ca-card">
              <div class="ca-card-top">
                <div class="ca-shift-name">{{ ca.tenCa }}</div>
                <span class="ca-pill" :class="caStatusInfo(ca.trangThai).cls">
                  {{ caStatusInfo(ca.trangThai).label }}
                </span>
              </div>
              <div class="ca-time-row">
                <i class="bi bi-clock"></i>
                <span>{{ ca.thoiGian }}</span>
              </div>
              <div v-if="ca.thoiGianBatDau || ca.thoiGianKetThuc" class="ca-time-detail">
                <div v-if="ca.thoiGianBatDau" class="ca-time-item">
                  <span class="ca-time-lbl">Bắt đầu</span>
                  <span class="ca-time-val">{{ fmtTime(ca.thoiGianBatDau) }}</span>
                </div>
                <div v-if="ca.thoiGianKetThuc" class="ca-time-item">
                  <span class="ca-time-lbl">Kết thúc</span>
                  <span class="ca-time-val">{{ fmtTime(ca.thoiGianKetThuc) }}</span>
                </div>
              </div>
              <div v-if="ca.ghiChu" class="ca-note">
                <i class="bi bi-chat-left-text"></i> {{ ca.ghiChu }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- Panel ca đang hoạt động (nút tròn bên ngoài) -->
  <Teleport to="body">
    <Transition name="ca-panel">
      <div v-if="showCaActivePanel" class="ca-overlay" @click.self="showCaActivePanel=false">
        <div class="ca-panel">

          <div class="ca-panel-head">
            <div class="ca-panel-title">
              <i class="bi bi-clock-history"></i>
              <span>Ca làm việc cửa hàng</span>
            </div>
            <button class="ca-close" @click="showCaActivePanel=false"><i class="bi bi-x-lg"></i></button>
          </div>

          <div class="ca-date-label">
            <i class="bi bi-calendar3"></i>
            {{ new Date().toLocaleDateString('vi-VN', { weekday:'long', day:'2-digit', month:'long', year:'numeric' }) }}
          </div>

          <!-- Loading -->
          <div v-if="caActiveLoading" class="ca-empty">
            <i class="bi bi-arrow-repeat ca-spin"></i> Đang tải...
          </div>

          <!-- Không có ca -->
          <div v-else-if="!caActiveList.length" class="ca-empty">
            <i class="bi bi-calendar-x" style="font-size:32px;color:var(--ss-faint)"></i>
            <p>Không có ca nào đang hoạt động</p>
          </div>

          <!-- Danh sách ca cửa hàng -->
          <div v-else class="ca-list">
            <div
              v-for="(ca, idx) in caActiveList"
              :key="ca.id"
              class="ca-card"
              :class="{ 'ca-card-active': laCaHienTai(ca) }"
            >
              <div class="ca-card-top">
                <div class="ca-shift-name">
                  <span class="ca-index">Ca {{ idx + 1 }}</span>
                  {{ ca.tenCa }}
                </div>
                <span v-if="laCaHienTai(ca)" class="ca-pill ca-pill-now">
                  <i class="bi bi-circle-fill" style="font-size:7px"></i> Đang diễn ra
                </span>
              </div>
              <div class="ca-time-row">
                <i class="bi bi-clock"></i>
                <span>{{ ca.thoiGian || (ca.gioBatDau + ' - ' + ca.gioKetThuc) }}</span>
              </div>
              <div v-if="ca.moTa" class="ca-note">
                <i class="bi bi-info-circle"></i> {{ ca.moTa }}
              </div>
            </div>
          </div>

        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- Modal xác nhận đăng xuất -->
  <Teleport to="body">
    <div v-if="showLogoutConfirm" class="logout-overlay" @click.self="!authStore.loggingOut && (showLogoutConfirm = false)">      <div class="logout-modal" role="dialog" aria-modal="true" aria-labelledby="logout-title">
        <div class="logout-icon">
          <i class="bi bi-box-arrow-right"></i>
        </div>
        <h3 id="logout-title">Xác nhận đăng xuất</h3>
        <p>Bạn có chắc chắn muốn đăng xuất khỏi hệ thống?</p>
        <p v-if="route.path === '/ban-hang'" class="logout-pos-hint">Giỏ hàng đã lưu vẫn được giữ. Thông tin giao hàng chưa hoàn tất sẽ không được lưu vào đơn.</p>
        <div class="logout-actions">
          <button class="btn-cancel" :disabled="authStore.loggingOut" @click="showLogoutConfirm = false">Hủy</button>
          <button class="btn-confirm" :disabled="authStore.loggingOut" @click="doLogout">{{ authStore.loggingOut ? 'Đang đăng xuất…' : 'Đăng xuất' }}</button>
        </div>
      </div>
    </div>
  </Teleport>
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
.pill.logout {
  padding: 0 14px;
  color: #e53935;
  border-color: #fcc;
}
.pill.logout:hover { background: #fff1f1; border-color: #e53935; }

/* ── Modal xác nhận đăng xuất ── */
.logout-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fade-in .15s ease;
}

.logout-modal {
  background: #fff;
  border-radius: 14px;
  padding: 36px 32px 28px;
  width: 340px;
  text-align: center;
  box-shadow: 0 16px 48px rgba(0,0,0,.2);
  animation: slide-up .2s ease;
}

.logout-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #fff1f1;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  font-size: 24px;
  color: #e53935;
}

.logout-modal h3 {
  font-size: 17px;
  font-weight: 700;
  color: #1a2b3c;
  margin: 0 0 8px;
}

.logout-modal p {
  font-size: 13.5px;
  color: #5f7384;
  margin: 0 0 24px;
}

.logout-actions {
  display: flex;
  gap: 10px;
}

.btn-cancel {
  flex: 1;
  height: 40px;
  border: 1.5px solid #d0dbe6;
  border-radius: 8px;
  background: #fff;
  color: #4f5e6d;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color .15s;
}

.btn-cancel:hover { border-color: #168dcc; color: #168dcc; }

.btn-confirm {
  flex: 1;
  height: 40px;
  border: none;
  border-radius: 8px;
  background: #e53935;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s;
}

.btn-confirm:hover { background: #c62828; }

@keyframes fade-in {
  from { opacity: 0; }
  to   { opacity: 1; }
}

@keyframes slide-up {
  from { transform: translateY(16px); opacity: 0; }
  to   { transform: translateY(0);    opacity: 1; }
}
.bell {
  width: 38px; height: 38px; border-radius: 50%;
  border: 1px solid var(--ss-border); background: var(--ss-surface);
  color: var(--ss-primary); font-size: 11px; font-weight: 700; cursor: pointer;
  transition: border-color .15s;
}
.bell:hover { border-color: var(--ss-primary); }
.user { height: 40px; padding: 0 12px 0 6px; }
.avatar img { width: 100%; height: 100%; object-fit: cover; display: block; border-radius: inherit; }
.avatar { overflow: hidden; flex-shrink: 0; width: 30px; height: 30px; border-radius: 50%; background: var(--ss-primary-soft); color: var(--ss-primary); display: grid; place-items: center; font-size: 10.5px; font-weight: 700; }
.user .bi { font-size: 10px; color: var(--ss-muted); }

/* ── User dropdown ──────────────────────────────────────── */
.user-wrap { position: relative; }

.chev-up { transform: rotate(180deg) !important; }

.user-dropdown {
  position: absolute; top: calc(100% + 8px); right: 0;
  min-width: 180px; background: #fff;
  border: 1px solid var(--ss-border); border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0,0,0,.12);
  padding: 6px; z-index: 300;
}

.user-dd-item {
  display: flex; align-items: center; gap: 9px;
  width: 100%; padding: 9px 10px; border-radius: 7px;
  font-size: 13px; font-weight: 500; color: var(--ss-text);
  text-decoration: none; background: none; border: none;
  cursor: pointer; text-align: left;
  transition: background .15s;
}
.user-dd-item:hover { background: var(--ss-surface); }
.user-dd-item i { color: var(--ss-muted); font-size: 14px; }

.user-dd-divider { height: 1px; background: var(--ss-border); margin: 4px 0; }

.user-dd-logout { color: #e53935; }
.user-dd-logout:hover { background: #fff1f1; }
.user-dd-logout i { color: #e53935; }

@media (max-width: 991px) {
  .topbar { padding: 0 16px; }
  .shift span, .user-name { display: none; }
  .shift { padding: 0 12px; }
  .crumb-parent, .crumb-slash { display: none; }
}

/* ── Ca badge ─────────────────────────────────────────────── */
.ca-badge {
  min-width: 18px; height: 18px; border-radius: 999px;
  background: var(--ss-primary); color: #fff;
  font-size: 10px; font-weight: 700;
  display: inline-flex; align-items: center; justify-content: center;
  padding: 0 5px; margin-left: 2px;
}

/* ── Panel overlay ─────────────────────────────────────────── */
.ca-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,.35);
  z-index: 9998;
  display: flex; justify-content: flex-end;
}

.ca-panel {
  width: 340px; max-width: 100%;
  height: 100vh; overflow-y: auto;
  background: #fff;
  box-shadow: -4px 0 24px rgba(0,0,0,.15);
  display: flex; flex-direction: column;
  padding: 0;
}

/* transition */
.ca-panel-enter-active, .ca-panel-leave-active { transition: all .25s ease; }
.ca-panel-enter-from .ca-panel, .ca-panel-leave-to .ca-panel { transform: translateX(100%); }

/* ── Panel header ──────────────────────────────────────────── */
.ca-panel-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid var(--ss-border);
  background: var(--ss-primary);
  color: #fff;
  flex-shrink: 0;
}
.ca-panel-title {
  display: flex; align-items: center; gap: 10px;
  font-size: 15px; font-weight: 700;
}
.ca-close {
  width: 32px; height: 32px; border-radius: 50%;
  border: none; background: rgba(255,255,255,.2);
  color: #fff; font-size: 14px; cursor: pointer;
  display: grid; place-items: center;
  transition: background .15s;
}
.ca-close:hover { background: rgba(255,255,255,.35); }

/* ── Ngày ──────────────────────────────────────────────────── */
.ca-date-label {
  padding: 12px 20px;
  font-size: 12px; font-weight: 600; color: var(--ss-muted);
  display: flex; align-items: center; gap: 6px;
  background: var(--ss-surface);
  border-bottom: 1px solid var(--ss-border);
  flex-shrink: 0;
}

/* ── Empty ─────────────────────────────────────────────────── */
.ca-empty {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 12px; padding: 40px 20px;
  font-size: 13px; color: var(--ss-muted); text-align: center;
}
.ca-spin { animation: ca-spin-kf .7s linear infinite; display: inline-block; font-size: 24px; }
@keyframes ca-spin-kf { to { transform: rotate(360deg); } }

/* ── List ──────────────────────────────────────────────────── */
.ca-list { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 12px; }

.ca-card {
  border: 1px solid var(--ss-border);
  border-radius: 10px;
  padding: 14px 16px;
  background: #fff;
  display: flex; flex-direction: column; gap: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,.05);
  transition: box-shadow .15s;
}
.ca-card:hover { box-shadow: 0 3px 10px rgba(0,0,0,.1); }

.ca-card-top {
  display: flex; align-items: center;
  justify-content: space-between; gap: 8px;
}
.ca-shift-name { font-size: 14px; font-weight: 700; color: var(--ss-text); }

.ca-pill {
  padding: 2px 10px; border-radius: 20px;
  font-size: 11px; font-weight: 600; white-space: nowrap;
}
.ca-pill.warn    { background: #fff8e1; color: #f57f17; }
.ca-pill.info    { background: #e3f2fd; color: #1565c0; }
.ca-pill.danger  { background: #fff5f5; color: #c62828; }
.ca-pill.success { background: #e8f5e9; color: #2e7d32; }

.ca-time-row {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; font-weight: 600; color: var(--ss-primary);
}

.ca-time-detail {
  display: flex; gap: 16px;
  background: var(--ss-surface);
  border-radius: 6px; padding: 8px 12px;
}
.ca-time-item { display: flex; flex-direction: column; gap: 2px; }
.ca-time-lbl { font-size: 10px; font-weight: 600; color: var(--ss-muted); text-transform: uppercase; }
.ca-time-val { font-size: 12px; font-weight: 700; color: var(--ss-text); }

.ca-note {
  font-size: 12px; color: var(--ss-muted);
  display: flex; align-items: flex-start; gap: 6px;
  padding-top: 4px; border-top: 1px dashed var(--ss-border);
}

/* ── Ca card hiện tại ──────────────────────────────────── */
.ca-card-active {
  border-color: var(--ss-primary);
  background: linear-gradient(135deg, #f0f7ff 0%, #fff 100%);
  box-shadow: 0 2px 8px rgba(var(--ss-primary-rgb, 21,101,192), .12);
}

.ca-index {
  font-size: 11px; font-weight: 700;
  color: var(--ss-muted);
  margin-right: 4px;
}

.ca-pill-now {
  background: #e8f5e9; color: #2e7d32;
  display: inline-flex; align-items: center; gap: 5px;
  padding: 3px 10px; border-radius: 20px;
  font-size: 11px; font-weight: 600;
  animation: ca-pulse .8s ease-in-out infinite alternate;
}

@keyframes ca-pulse {
  from { opacity: 1; }
  to   { opacity: .6; }
}
</style>

<style scoped>
.logout-modal .logout-pos-hint { font-size: 12px; line-height: 1.5; color: #718096; }
.logout-actions button:disabled { opacity: .6; cursor: wait; }
</style>
