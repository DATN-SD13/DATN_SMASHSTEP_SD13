import { createRouter, createWebHistory } from 'vue-router'
import InvoiceList from '../modules/admin/invoice/views/InvoiceList.vue'
import InvoiceDetail from '../modules/admin/invoice/views/InvoiceDetail.vue'
import DiscountList from '../modules/admin/promotion/views/DiscountList.vue'
import CampaignList from '../modules/admin/promotion/views/CampaignList.vue'
import AddDiscount from '../modules/admin/promotion/views/AddDiscount.vue'
import AddCampaign from '../modules/admin/promotion/views/AddCampaign.vue'

const ComingSoon = () => import('../components/ComingSoon.vue')

const router=createRouter({history:createWebHistory(),routes:[
 {path:'/san-pham',component:() => import('../modules/admin/product/views/ProductList.vue'),meta:{title:'Sản phẩm',parent:{label:'Quản lý sản phẩm',to:'/san-pham'}}},
 {path:'/san-pham/them',component:() => import('../modules/admin/product/views/ProductCreate.vue'),meta:{title:'Thêm sản phẩm',parent:{label:'Sản phẩm',to:'/san-pham'}}},
 {path:'/san-pham/:id/sua',component:() => import('../modules/admin/product/views/ProductEdit.vue'),meta:{title:'Sửa sản phẩm',parent:{label:'Sản phẩm',to:'/san-pham'}}},
 {path:'/san-pham/:id',component:() => import('../modules/admin/product/views/ProductDetail.vue'),meta:{title:'Chi tiết sản phẩm',parent:{label:'Sản phẩm',to:'/san-pham'}}},
 {path:'/bien-the-san-pham',component:() => import('../modules/admin/product/views/ProductVariantList.vue'),meta:{title:'Biến thể sản phẩm',parent:{label:'Quản lý sản phẩm',to:'/san-pham'}}},
 {path:'/thuoc-tinh',component:() => import('../modules/admin/product/views/ProductAttributeList.vue'),meta:{title:'Thiết lập biến thể sản phẩm',parent:{label:'Quản lý sản phẩm',to:'/san-pham'}}},
 {path:'/',redirect:'/hoa-don'},
 {path:'/hoa-don',component:InvoiceList,meta:{title:'Quản lý hóa đơn'}},
 {path:'/hoa-don/:code',component:InvoiceDetail,meta:{title:'Chi tiết hóa đơn',parent:{label:'Hóa đơn',to:'/hoa-don'}}},
 {path:'/giam-gia',component:DiscountList,meta:{title:'Phiếu giảm giá',parent:{label:'Quản lý giảm giá',to:'/giam-gia'}}},
 {path:'/giam-gia/them',component:AddDiscount,meta:{title:'Thêm phiếu giảm giá',parent:{label:'Phiếu giảm giá',to:'/giam-gia'}}},
 {path:'/dot-giam-gia',component:CampaignList,meta:{title:'Đợt giảm giá',parent:{label:'Quản lý giảm giá',to:'/giam-gia'}}},
 {path:'/dot-giam-gia/them',component:AddCampaign,meta:{title:'Thêm đợt giảm giá',parent:{label:'Đợt giảm giá',to:'/dot-giam-gia'}}},
 {path:'/thong-ke',component:ComingSoon,meta:{title:'Thống kê'}},
 {path:'/ban-hang',component:ComingSoon,meta:{title:'Bán hàng tại quầy'}},
 {path:'/khach-hang',component:ComingSoon,meta:{title:'Quản lý khách hàng'}},
 {path:'/nhan-vien',component:ComingSoon,meta:{title:'Quản lý nhân viên'}},
 {path:'/invoice',redirect:'/hoa-don'}
]})
export default router
