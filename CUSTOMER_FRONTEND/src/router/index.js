import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // ── Auth (public, guest only) ────────────────────────
    {
      path: '/dang-nhap',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { title: 'Đăng nhập | SmashStep', public: true, guestOnly: true }
    },
    {
      path: '/quen-mat-khau',
      name: 'forgot-password',
      component: () => import('../views/ForgotPasswordView.vue'),
      meta: { title: 'Quên mật khẩu | SmashStep', public: true, guestOnly: true }
    },
    {
      path: '/dat-lai-mat-khau',
      name: 'reset-password',
      component: () => import('../views/ResetPasswordView.vue'),
      meta: { title: 'Đặt lại mật khẩu | SmashStep', public: true }
    },
    {
      path: '/dang-ky',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
      meta: { title: 'Đăng ký | SmashStep', public: true, guestOnly: true }
    },

    {
      path: '/tai-khoan',
      name: 'account',
      component: () => import('../views/AccountView.vue'),
      meta: { title: 'Tài khoản của tôi | SmashStep', requiresAuth: true, customerOnly: true }
    },

    { path: '/gio-hang', component: () => import('../views/CartView.vue'), meta: { title: 'Giỏ hàng | SmashStep' } },
    { path: '/thanh-toan', component: () => import('../views/CheckoutView.vue'), meta: { title: 'Thanh toán | SmashStep', requiresAuth: true, customerOnly: true } },
    { path: '/thanh-toan-vnpay/ket-qua', component: () => import('../views/VnpayReturnView.vue'), meta: { title: 'Kết quả thanh toán | SmashStep', public: true } },
    { path: '/thanh-toan-vnpay/phien/:code', component: () => import('../views/VnpayReturnView.vue'), meta: { title: 'Thanh toán VNPAY | SmashStep', requiresAuth: true, customerOnly: true } },
    { path: '/thanh-toan-thu', redirect: '/thanh-toan' },
    { path: '/thanh-toan-thu/:code', redirect: '/don-hang' },
    { path: '/don-hang', component: () => import('../views/OrderListView.vue'), meta: { title: 'Đơn hàng của bạn | SmashStep', requiresAuth: true, customerOnly: true } },
    { path: '/don-hang/:ma', component: () => import('../views/OrderDetailView.vue'), meta: { title: 'Chi tiết đơn hàng | SmashStep', requiresAuth: true, customerOnly: true } },
    { path: '/goi-y-giay', component: () => import('../views/RecommendationView.vue'), meta: { title: 'Gợi ý giày | SmashStep' } },

    { path: '/ve-chung-toi', name: 'about', component: () => import('../views/AboutView.vue'), meta: { title: 'Về chúng tôi | SmashStep' } },

    // ── Public pages ─────────────────────────────────────
    {
      path: '/',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: { title: 'SmashStep | Mỗi bước chân, một dấu ấn' }
    },
    {
      path: '/san-pham',
      name: 'products',
      component: () => import('../views/ProductListView.vue'),
      meta: { title: 'Bộ sưu tập giày | SmashStep' }
    },
    {
      path: '/san-pham/:id',
      name: 'product-detail',
      component: () => import('../views/ProductDetailView.vue'),
      meta: { title: 'Chi tiết sản phẩm | SmashStep' }
    },

    { path: '/:pathMatch(.*)*', redirect: '/' }
  ],
  scrollBehavior(to, from, saved) {
    return saved || (to.hash ? { el: to.hash, top: 90, behavior: 'smooth' } : { top: 0 })
  }
})

// ── Navigation Guard ──────────────────────────────────────
router.beforeEach((to, _from, next) => {
  import('../stores/auth').then(({ useAuthStore }) => {
    const auth = useAuthStore()
    auth.syncSession()

    // Trang chỉ dành cho guest: nếu đã đăng nhập thì về trang chủ
    if (to.meta.guestOnly && auth.isAuthenticated) {
      return next('/')
    }

    // Route yêu cầu đăng nhập
    if (to.meta.requiresAuth && !auth.isAuthenticated) {
      return next({ path: '/dang-nhap', query: { redirect: to.fullPath } })
    }

    if (to.meta.customerOnly && !auth.isCustomer) {
      return next('/')
    }

    next()
  })
})

router.afterEach(to => {
  document.title = to.meta.title || 'SmashStep'
})

export default router
