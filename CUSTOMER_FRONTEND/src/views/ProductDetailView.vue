<script setup>
import { computed,ref,watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import ProductReviews from '../components/ProductReviews.vue'
import RatingStars from '../components/RatingStars.vue'
import QuantityStepper from '../components/QuantityStepper.vue'
import ProductGallery from '../components/ProductGallery.vue'
import ProductPrice from '../components/ProductPrice.vue'
import SaleBadge from '../components/SaleBadge.vue'
import ProductSkeleton from '../components/ProductSkeleton.vue'
import EmptyState from '../components/EmptyState.vue'
import BaseIcon from '../components/BaseIcon.vue'
import { useResource } from '../composables/useResource'
import { storefrontService } from '../services/storefrontService'
const route=useRoute(), router=useRouter(), cart=useCartStore(), color=ref(null), size=ref(null)
const quantity=ref(1), adding=ref(false), feedback=ref(''), cartError=ref('')
async function addToCart(buyNow=false) {
  if(adding.value || !selected.value || selected.value.soLuong < quantity.value) return
  adding.value=true; feedback.value=''; cartError.value=''
  try { const variantId=selected.value.id; await cart.add(variantId, quantity.value); feedback.value='Đã thêm sản phẩm vào giỏ hàng.'; if(buyNow) { cart.selectAll(false); cart.setSelected(variantId,true); await router.push('/thanh-toan') } }
  catch(e) { cartError.value=e.response?.data?.message || e.message || 'Chưa thêm được sản phẩm. Vui lòng thử lại.' }
  finally { adding.value=false }
}
const { data,loading,error,reload }=useResource(signal => storefrontService.product(route.params.id,signal))
const variants=computed(() => data.value?.variants || []), product=computed(() => data.value?.product)
const colors=computed(() => [...new Map(variants.value.map(v => [v.mauSacId,{ id:v.mauSacId,name:v.tenMauSac,hex:/^#[0-9a-f]{6}$/i.test(v.maMauHex || '') ? v.maMauHex : null }])).values()])
const sizes=computed(() => [...new Map(variants.value.filter(v => color.value == null || v.mauSacId === color.value).map(v => [v.kichThuocId,{ id:v.kichThuocId,name:v.tenKichThuoc }])).values()])
const selected=computed(() => variants.value.find(v => v.mauSacId === color.value && v.kichThuocId === size.value))
const info=computed(() => [['Mã sản phẩm',product.value?.maSanPham],['Thương hiệu',product.value?.tenThuongHieu],['Danh mục',product.value?.tenDanhMuc],['Chất liệu',product.value?.tenChatLieu],['Kiểu dáng',product.value?.tenKieuDang],['Cổ giày',product.value?.tenCoGiay],['Xuất xứ',product.value?.tenXuatXu]].filter(([,value]) => value))
watch(() => selected.value?.id, () => { quantity.value=1; feedback.value=''; cartError.value='' })
function chooseColor(id) { color.value=id; if(!variants.value.some(v => v.mauSacId === id && v.kichThuocId === size.value)) size.value=null }
watch(() => route.params.id,() => { color.value=null; size.value=null; reload() },{ immediate:true })
watch(variants,() => { if(!colors.value.some(c => c.id === color.value)) color.value=null; if(!sizes.value.some(s => s.id === size.value)) size.value=null })
</script>
<template><div class="container detail-page"><nav class="breadcrumbs" aria-label="Đường dẫn"><RouterLink to="/">Trang chủ</RouterLink><span>/</span><RouterLink to="/san-pham">Sản phẩm</RouterLink><template v-if="product"><span>/</span><span>{{ product.tenSanPham }}</span></template></nav><RouterLink class="back-link" to="/san-pham"><BaseIcon name="left" /> Quay lại bộ sưu tập</RouterLink><EmptyState v-if="error" :title="error" error action="Thử lại" @action="reload" /><ProductSkeleton v-else-if="loading && !data" detail /><template v-else-if="product"><section class="detail-layout"><ProductGallery :images="data.images" :variant="selected" :product="product" /><div class="detail-copy"><RouterLink v-if="product.brandId" class="eyebrow" :to="{ path:'/san-pham',query:{ brandId:product.brandId } }">{{ product.tenThuongHieu }}</RouterLink><h1>{{ product.tenSanPham }}</h1><a href="#product-reviews" class="detail-rating"><RatingStars :value="product.averageRating || 0" /><strong>{{ product.reviewCount ? Number(product.averageRating).toFixed(1) : 'Chưa có đánh giá' }}</strong><span>({{ product.reviewCount || 0 }} đánh giá)</span></a><p class="detail-code">{{ product.maSanPham }}</p><div class="detail-price"><ProductPrice :item="selected || product" :from="!selected && product.soBienThe > 1" /><SaleBadge :item="selected || product" /></div><div class="variant-selector"><h2>Màu sắc <span>{{ colors.find(c => c.id === color)?.name || 'Chưa chọn' }}</span></h2><div class="choice-list"><button v-for="option in colors" :key="option.id" :class="{ selected:color === option.id }" :aria-pressed="color === option.id" @click="chooseColor(option.id)"><span v-if="option.hex" class="color-dot" :style="{ background:option.hex }"></span>{{ option.name || 'Chưa có tên màu' }}</button></div></div><div class="variant-selector"><h2>Kích thước <span>{{ sizes.find(s => s.id === size)?.name || 'Chưa chọn' }}</span></h2><div class="choice-list size-choices"><button v-for="option in sizes" :key="option.id" :class="{ selected:size === option.id }" :aria-pressed="size === option.id" @click="size=option.id">{{ option.name }}</button></div></div><div class="detail-stock" aria-live="polite"><template v-if="selected"><span class="stock-dot" :class="{ empty:!selected.soLuong }"></span><strong>{{ selected.soLuong > 0 ? 'Còn ' + selected.soLuong + ' sản phẩm' : 'Hết hàng' }}</strong><small>{{ selected.sku || selected.maChiTietSanPham }}</small></template><span v-else>Chọn màu sắc và kích thước để xem giá, ảnh và tồn kho chính xác.</span></div><div class="detail-shopping"><label>Số lượng</label><QuantityStepper v-model="quantity" :max="selected?.soLuong || 0" :disabled="!selected || !selected.soLuong || adding" /><div class="detail-shopping-actions"><button class="button button-outline" :disabled="adding || !selected || selected.soLuong < quantity || quantity < 1" @click="addToCart()"><BaseIcon name="cart" /> Thêm vào giỏ</button><button class="button button-primary" :disabled="adding || !selected || selected.soLuong < quantity || quantity < 1" @click="addToCart(true)">{{ adding ? 'Đang kiểm tra…' : 'Mua ngay' }} →</button></div><p v-if="feedback" class="shop-success" role="status">{{ feedback }} <RouterLink to="/gio-hang">Xem giỏ hàng →</RouterLink></p><p v-if="cartError" class="shop-error" role="alert">{{ cartError }}</p></div></div></section><section class="product-information"><div><span class="eyebrow">CHI TIẾT THIẾT KẾ</span><h2>Thông tin sản phẩm</h2><dl><template v-for="[label,value] in info" :key="label"><dt>{{ label }}</dt><dd>{{ value }}</dd></template></dl></div><div v-if="product.moTaChiTiet" class="product-description"><h3>Mô tả</h3><p>{{ product.moTaChiTiet }}</p></div></section><ProductReviews :product-id="product.id" @updated="value => { product.averageRating=value.average; product.reviewCount=value.count }" /></template></div></template>

<style scoped>
.detail-rating{display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin:12px 0;text-decoration:none;color:#60718a;font-size:13px}.detail-rating strong{color:#b5750b}.detail-rating:hover span{text-decoration:underline}
</style>
