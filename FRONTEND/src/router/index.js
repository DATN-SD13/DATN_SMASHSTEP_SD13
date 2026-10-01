import { createRouter, createWebHistory } from 'vue-router'
import InvoiceList from '../modules/admin/invoice/views/InvoiceList.vue'
import InvoiceDetail from '../modules/admin/invoice/views/InvoiceDetail.vue'
import DiscountList from '../modules/admin/promotion/views/DiscountList.vue'
import CampaignList from '../modules/admin/promotion/views/CampaignList.vue'
import AddDiscount from '../modules/admin/promotion/views/AddDiscount.vue'
import AddCampaign from '../modules/admin/promotion/views/AddCampaign.vue'

const router=createRouter({history:createWebHistory(),routes:[
 {path:'/',redirect:'/hoa-don'},
 {path:'/hoa-don',component:InvoiceList},
 {path:'/hoa-don/:code',component:InvoiceDetail},
 {path:'/giam-gia',component:DiscountList},
 {path:'/giam-gia/them',component:AddDiscount},
 {path:'/dot-giam-gia',component:CampaignList},
 {path:'/dot-giam-gia/them',component:AddCampaign},
 {path:'/invoice',redirect:'/hoa-don'}
]})
export default router
