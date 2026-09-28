import { createRouter, createWebHistory } from 'vue-router'
import InvoiceList from '../modules/admin/invoice/views/InvoiceList.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/hoa-don' },
    { path: '/hoa-don', name: 'hoa-don', component: InvoiceList },
    { path: '/invoice', redirect: '/hoa-don' }
  ]
})

export default router
