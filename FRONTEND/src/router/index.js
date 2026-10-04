import { createRouter, createWebHistory } from 'vue-router'
import InvoiceList from '../modules/admin/invoice/views/InvoiceList.vue'
import InvoiceDetail from '../modules/admin/invoice/views/InvoiceDetail.vue'
import DiscountList from '../modules/admin/promotion/views/DiscountList.vue'
import CampaignList from '../modules/admin/promotion/views/CampaignList.vue'
import AddDiscount from '../modules/admin/promotion/views/AddDiscount.vue'
import AddCampaign from '../modules/admin/promotion/views/AddCampaign.vue'
import CustomerList from '../modules/admin/customer/views/CustomerList.vue'
import AddCustomer from '../modules/admin/customer/views/AddCustomer.vue'
import EmployeeList from '../modules/admin/employee/views/EmployeeList.vue'
import EmployeeDetail from '../modules/admin/employee/views/EmployeeDetail.vue'
import AddEmployee from '../modules/admin/employee/views/AddEmployee.vue'
import POSSales from '../modules/admin/sales/views/POSSales.vue'
import ComingSoon from '../layouts/ComingSoon.vue'

const promo = { label: 'Quản lý giảm giá', to: '/giam-gia' }
const prod = { label: 'Quản lý sản phẩm', to: '/san-pham' }
const productParent = { label: 'Sản phẩm', to: '/san-pham' }

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/hoa-don' },
    { path: '/hoa-don', component: InvoiceList, meta: { title: 'Quản lý hóa đơn' } },
    { path: '/hoa-don/:code', component: InvoiceDetail, meta: { title: 'Chi tiết hóa đơn', parent: { label: 'Hóa đơn', to: '/hoa-don' } } },
    { path: '/giam-gia', component: DiscountList, meta: { title: 'Phiếu giảm giá', parent: promo } },
    { path: '/giam-gia/them', component: AddDiscount, meta: { title: 'Thêm phiếu giảm giá', parent: { label: 'Phiếu giảm giá', to: '/giam-gia' } } },
    { path: '/dot-giam-gia', component: CampaignList, meta: { title: 'Đợt giảm giá', parent: promo } },
    { path: '/dot-giam-gia/them', component: AddCampaign, meta: { title: 'Thêm đợt giảm giá', parent: { label: 'Đợt giảm giá', to: '/dot-giam-gia' } } },
    { path: '/ban-hang', component: POSSales, meta: { title: 'Bán hàng tại quầy' } },
    { path: '/thong-ke', component: ComingSoon, meta: { title: 'Thống kê' } },
    { path: '/san-pham', component: () => import('../modules/admin/product/views/ProductList.vue'), meta: { title: 'Sản phẩm', parent: prod } },
    { path: '/san-pham/them', component: () => import('../modules/admin/product/views/ProductCreate.vue'), meta: { title: 'Thêm sản phẩm', parent: productParent } },
    { path: '/san-pham/:id/sua', component: () => import('../modules/admin/product/views/ProductEdit.vue'), meta: { title: 'Sửa sản phẩm', parent: productParent } },
    { path: '/san-pham/:id', component: () => import('../modules/admin/product/views/ProductDetail.vue'), meta: { title: 'Chi tiết sản phẩm', parent: productParent } },
    { path: '/bien-the-san-pham', component: () => import('../modules/admin/product/views/ProductVariantList.vue'), meta: { title: 'Biến thể sản phẩm', parent: prod } },
    { path: '/thuoc-tinh', component: () => import('../modules/admin/product/views/ProductAttributeList.vue'), meta: { title: 'Thiết lập biến thể sản phẩm', parent: prod } },
    { path: '/thiet-lap-bien-the', redirect: '/thuoc-tinh' },
    { path: '/khach-hang', component: CustomerList, meta: { title: 'Khách hàng' } },
    { path: '/khach-hang/them', component: AddCustomer, meta: { title: 'Thêm khách hàng', parent: { label: 'Khách hàng', to: '/khach-hang' } } },
    { path: '/nhan-vien', component: EmployeeList, meta: { title: 'Nhân viên' } },
    { path: '/nhan-vien/them', component: AddEmployee, meta: { title: 'Thêm nhân viên', parent: { label: 'Nhân viên', to: '/nhan-vien' } } },
    { path: '/nhan-vien/:code', component: EmployeeDetail, meta: { title: 'Chi tiết nhân viên', parent: { label: 'Nhân viên', to: '/nhan-vien' } } },
    { path: '/invoice', redirect: '/hoa-don' },
    { path: '/:pathMatch(.*)*', component: ComingSoon, meta: { title: 'Đang phát triển' } }
  ]
})

export default router
