import { createRouter, createWebHistory } from 'vue-router'
import InvoiceList from '../modules/admin/invoice/views/InvoiceList.vue'
import InvoiceDetail from '../modules/admin/invoice/views/InvoiceDetail.vue'
import DiscountList from '../modules/admin/promotion/views/DiscountList.vue'
import CampaignList from '../modules/admin/promotion/views/CampaignList.vue'
import AddDiscount from '../modules/admin/promotion/views/AddDiscount.vue'
import AddCampaign from '../modules/admin/promotion/views/AddCampaign.vue'
import DetailCampaign from '../modules/admin/promotion/views/DetailCampaign.vue'
import CustomerList from '../modules/admin/customer/views/CustomerList.vue'
import AddCustomer from '../modules/admin/customer/views/AddCustomer.vue'
import EmployeeList from '../modules/admin/employee/views/EmployeeList.vue'
import EmployeeDetail from '../modules/admin/employee/views/EmployeeDetail.vue'
import AddEmployee from '../modules/admin/employee/views/AddEmployee.vue'
import POSSales from '../modules/admin/sales/views/POSSales.vue'
import StatisticsView from '../modules/admin/statistics/view/StatisticsDashboard.vue'
import ComingSoon from '../layouts/ComingSoon.vue'
import EditDiscount from '../modules/admin/promotion/views/EditDiscount.vue'
import DetailDiscount from '../modules/admin/promotion/views/DetailDiscount.vue'
import EditCampaign from '../modules/admin/promotion/views/EditCampaign.vue'
import CaLamViecList from '../modules/admin/schedule/views/CaLamViecList.vue'
import AdminLogin from '../modules/auth/views/AdminLogin.vue'
import AdminForgotPassword from '../modules/auth/views/AdminForgotPassword.vue'
import CustomerLogin from '../modules/auth/views/CustomerLogin.vue'
import CustomerHome from '../modules/auth/views/CustomerHome.vue'
import LoginView from '../modules/auth/views/LoginView.vue'
import ForgotPassword from '../modules/auth/views/ForgotPassword.vue'
import ResetPassword from '../modules/auth/views/ResetPassword.vue'

const promo = { label: 'Quản lý giảm giá', to: '/giam-gia' }
const prod  = { label: 'Quản lý sản phẩm', to: '/san-pham' }
const productParent = { label: 'Sản phẩm', to: '/san-pham' }

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ── Auth (public) ───────────────────────────────────
    { path: '/dang-nhap',                    component: AdminLogin,         meta: { public: true, guestOnly: true } },
    { path: '/quen-mat-khau-quan-ly',        component: AdminForgotPassword, meta: { public: true, guestOnly: true } },
    { path: '/khach-hang/dang-nhap',         component: CustomerLogin,      meta: { public: true, guestOnly: true } },
    { path: '/khach-hang/quen-mat-khau',     component: ForgotPassword, meta: { public: true, guestOnly: true } },
    { path: '/khach-hang/dat-lai-mat-khau',  component: ResetPassword,  meta: { public: true } },
    { path: '/khach-hang/trang-chu',         component: CustomerHome,  meta: { requiresAuth: true, userType: 'KHACH_HANG' } },

    // ── Admin area (requiresAuth + NHAN_VIEN) ───────────
    { path: '/', redirect: '/ban-hang' },
    { path: '/hoa-don',         component: InvoiceList,   meta: { title: 'Quản lý hóa đơn', requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/hoa-don/:code',   component: InvoiceDetail, meta: { title: 'Chi tiết hóa đơn', parent: { label: 'Hóa đơn', to: '/hoa-don' }, requiresAuth: true, userType: 'NHAN_VIEN' } },

    { path: '/giam-gia',          component: DiscountList, meta: { title: 'Phiếu giảm giá', parent: promo, requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/giam-gia/them',     component: AddDiscount,  meta: { title: 'Thêm phiếu giảm giá', parent: { label: 'Phiếu giảm giá', to: '/giam-gia' }, requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/giam-gia/sua/:id',  component: EditDiscount, meta: { requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/giam-gia/chi-tiet/:id', component: DetailDiscount, meta: { requiresAuth: true, userType: 'NHAN_VIEN' } },

    { path: '/dot-giam-gia',              component: CampaignList,   meta: { title: 'Đợt giảm giá', parent: promo, requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/dot-giam-gia/them',         component: AddCampaign,    meta: { title: 'Thêm đợt giảm giá', parent: { label: 'Đợt giảm giá', to: '/dot-giam-gia' }, requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/dot-giam-gia/chi-tiet/:code', component: DetailCampaign, meta: { title: 'Chi tiết đợt giảm giá', parent: { label: 'Đợt giảm giá', to: '/dot-giam-gia' }, requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/dot-giam-gia/sua/:code',    component: EditCampaign,   meta: { title: 'Sửa đợt giảm giá', parent: { label: 'Đợt giảm giá', to: '/dot-giam-gia' }, requiresAuth: true, userType: 'NHAN_VIEN' } },

    { path: '/ban-hang',  component: POSSales,       meta: { title: 'Bán hàng tại quầy', requiresAuth: true, userType: 'NHAN_VIEN' } },

    { path: '/thong-ke-tong-hop', component: StatisticsView, meta: { title: 'Thống kê tổng hợp', requiresAuth: true, userType: 'NHAN_VIEN', role: 'ADMIN' } },
    { path: '/thong-ke', redirect: '/thong-ke-tong-hop' },

    { path: '/san-pham',           component: () => import('../modules/admin/product/views/ProductList.vue'),    meta: { title: 'Sản phẩm', parent: prod, requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/san-pham/them',      component: () => import('../modules/admin/product/views/ProductCreate.vue'),  meta: { title: 'Thêm sản phẩm', parent: productParent, requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/san-pham/:id/sua',   component: () => import('../modules/admin/product/views/ProductEdit.vue'),    meta: { title: 'Sửa sản phẩm', parent: productParent, requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/san-pham/:id',       component: () => import('../modules/admin/product/views/ProductDetail.vue'),  meta: { title: 'Chi tiết sản phẩm', parent: productParent, requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/bien-the-san-pham',  component: () => import('../modules/admin/product/views/ProductVariantList.vue'), meta: { title: 'Biến thể sản phẩm', parent: prod, requiresAuth: true, userType: 'NHAN_VIEN' } },

    { path: '/thuoc-tinh',              redirect: '/thuoc-tinh/thuong-hieu' },
    { path: '/thuoc-tinh/thuong-hieu',  component: () => import('../modules/admin/product/views/ProductAttributeList.vue'), meta: { title: 'Thương hiệu', parent: { label: 'Thuộc tính', to: '/thuoc-tinh' }, attributeType: 'brands',      requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/thuoc-tinh/danh-muc',     component: () => import('../modules/admin/product/views/ProductAttributeList.vue'), meta: { title: 'Danh mục',    parent: { label: 'Thuộc tính', to: '/thuoc-tinh' }, attributeType: 'categories',  requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/thuoc-tinh/chat-lieu',    component: () => import('../modules/admin/product/views/ProductAttributeList.vue'), meta: { title: 'Chất liệu',   parent: { label: 'Thuộc tính', to: '/thuoc-tinh' }, attributeType: 'materials',   requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/thuoc-tinh/kieu-dang',    component: () => import('../modules/admin/product/views/ProductAttributeList.vue'), meta: { title: 'Kiểu dáng',   parent: { label: 'Thuộc tính', to: '/thuoc-tinh' }, attributeType: 'styles',      requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/thuoc-tinh/co-giay',      component: () => import('../modules/admin/product/views/ProductAttributeList.vue'), meta: { title: 'Cổ giày',     parent: { label: 'Thuộc tính', to: '/thuoc-tinh' }, attributeType: 'collars',     requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/thuoc-tinh/xuat-xu',      component: () => import('../modules/admin/product/views/ProductAttributeList.vue'), meta: { title: 'Xuất xứ',     parent: { label: 'Thuộc tính', to: '/thuoc-tinh' }, attributeType: 'origins',     requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/thuoc-tinh/mau-sac',      component: () => import('../modules/admin/product/views/ProductAttributeList.vue'), meta: { title: 'Màu sắc',     parent: { label: 'Thuộc tính', to: '/thuoc-tinh' }, attributeType: 'colors',      requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/thuoc-tinh/kich-thuoc',   component: () => import('../modules/admin/product/views/ProductAttributeList.vue'), meta: { title: 'Kích thước',  parent: { label: 'Thuộc tính', to: '/thuoc-tinh' }, attributeType: 'sizes',       requiresAuth: true, userType: 'NHAN_VIEN' } },

    { path: '/thiet-lap-bien-the', redirect: '/thuoc-tinh' },

    { path: '/khach-hang',      component: CustomerList, meta: { title: 'Khách hàng', requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/ho-tro-truc-tuyen', component: () => import('../modules/admin/chat/views/ChatSupportView.vue'), meta: { title: 'Hỗ trợ trực tuyến', requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/khach-hang/them', component: AddCustomer,  meta: { title: 'Thêm khách hàng', parent: { label: 'Khách hàng', to: '/khach-hang' }, requiresAuth: true, userType: 'NHAN_VIEN' } },

    { path: '/nhan-vien',       component: EmployeeList,   meta: { title: 'Nhân viên', requiresAuth: true, userType: 'NHAN_VIEN', role: 'ADMIN' } },
    { path: '/nhan-vien/them',  component: AddEmployee,    meta: { title: 'Thêm nhân viên', parent: { label: 'Nhân viên', to: '/nhan-vien' }, requiresAuth: true, userType: 'NHAN_VIEN', role: 'ADMIN' } },
    { path: '/nhan-vien/:code', component: EmployeeDetail, meta: { title: 'Chi tiết nhân viên', parent: { label: 'Nhân viên', to: '/nhan-vien' }, requiresAuth: true, userType: 'NHAN_VIEN', role: 'ADMIN' } },

    // ── Lịch làm việc ───────────────────────────────────
    { path: '/lich-lam-viec',       component: () => import('../modules/admin/schedule/views/ScheduleList.vue'), meta: { title: 'Lịch làm việc', parent: { label: 'Quản lý lịch làm', to: '/lich-lam-viec' }, requiresAuth: true, userType: 'NHAN_VIEN', role: 'ADMIN' } },
    { path: '/ca-lam-viec',          component: CaLamViecList, meta: { title: 'Lịch ca làm', parent: { label: 'Quản lý lịch làm', to: '/lich-lam-viec' }, requiresAuth: true, userType: 'NHAN_VIEN', role: 'ADMIN' } },
    { path: '/lich-su-hoat-dong',    component: () => import('../modules/admin/schedule/views/LichSuHoatDong.vue'), meta: { title: 'Lịch sử hoạt động', parent: { label: 'Quản lý lịch làm', to: '/lich-lam-viec' }, requiresAuth: true, userType: 'NHAN_VIEN', role: 'ADMIN' } },

    { path: '/invoice', redirect: '/hoa-don' },
    { path: '/ca-nhan', component: () => import('../modules/admin/profile/views/ProfileView.vue'), meta: { title: 'Thông tin cá nhân', requiresAuth: true, userType: 'NHAN_VIEN' } },
    { path: '/:pathMatch(.*)*', component: ComingSoon, meta: { title: 'Đang phát triển' } }
  ]
})

// ── Navigation Guard ──────────────────────────────────────
router.beforeEach((to, _from, next) => {
  // Lazy import store để tránh circular dep trước khi app mount
  import('../stores/auth').then(async ({ useAuthStore }) => {
    const auth = useAuthStore()

    // Trang chỉ dành cho guest: nếu đã login thì redirect đúng chỗ
    if (to.meta.guestOnly && auth.isAuthenticated) {
      return next(auth.isCustomer ? '/khach-hang/trang-chu' : (auth.isAdmin ? '/thong-ke-tong-hop' : '/ban-hang'))
    }

    // Route không yêu cầu auth → cho qua
    if (!to.meta.requiresAuth) return next()

    // Chưa đăng nhập → về trang login tương ứng
    if (!auth.isAuthenticated) {
      const loginPath = to.meta.userType === 'KHACH_HANG'
        ? '/khach-hang/dang-nhap'
        : '/dang-nhap'
      return next({ path: loginPath, query: { redirect: to.fullPath } })
    }

    if (auth.userType === 'NHAN_VIEN') {
      try { await auth.refreshSession() }
      catch {
        if (!auth.isAuthenticated) return next('/dang-nhap')
        return next(false)
      }
      if (!auth.isAuthenticated) return next('/dang-nhap')
    }

    // Khách hàng cố vào khu vực admin → về trang khách hàng
    if (to.meta.userType === 'NHAN_VIEN' && auth.isCustomer) {
      return next('/khach-hang/trang-chu')
    }

    // Kiểm tra role (trang có role: 'ADMIN' thì STAFF không vào được)
    if (to.meta.role && to.meta.role !== auth.role) {
      return next(auth.isAdmin ? '/thong-ke-tong-hop' : '/ban-hang')
    }

    next()
  })
})

export default router
