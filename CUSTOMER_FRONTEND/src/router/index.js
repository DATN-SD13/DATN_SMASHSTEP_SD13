import { createRouter, createWebHistory } from 'vue-router'
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('../views/HomeView.vue'), meta: { title: 'SmashStep | Mỗi bước chân, một dấu ấn' } },
    { path: '/san-pham', name: 'products', component: () => import('../views/ProductListView.vue'), meta: { title: 'Bộ sưu tập giày | SmashStep' } },
    { path: '/san-pham/:id', name: 'product-detail', component: () => import('../views/ProductDetailView.vue'), meta: { title: 'Chi tiết sản phẩm | SmashStep' } },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ],
  scrollBehavior(to, from, saved) { return saved || (to.hash ? { el: to.hash, top: 90, behavior: 'smooth' } : { top: 0 }) }
})
router.afterEach(to => { document.title = to.meta.title })
export default router
