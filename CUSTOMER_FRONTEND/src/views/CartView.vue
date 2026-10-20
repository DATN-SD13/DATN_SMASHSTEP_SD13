<script setup>
import { onMounted } from 'vue'
import { useCartStore } from '../stores/cart'
import { useAuthStore } from '../stores/auth'
import { money } from '../utils/money'
import ProductImage from '../components/ProductImage.vue'
import ProductPrice from '../components/ProductPrice.vue'
import QuantityStepper from '../components/QuantityStepper.vue'
const cart = useCartStore(), auth = useAuthStore()
onMounted(cart.refresh)
</script>
<template>
  <div class="container shop-page">
    <header class="shop-heading"><span class="eyebrow">SMASHSTEP BAG</span><h1>{{ auth.isCustomer ? 'Giỏ hàng của bạn' : 'Giỏ hàng khách' }}</h1><p v-if="auth.isCustomer">Giỏ hàng riêng của {{ auth.user?.name || 'bạn' }}. Đăng xuất không làm mất các sản phẩm đã lưu.</p><p v-else>Giỏ khách được lưu riêng. <RouterLink to="/dang-nhap?redirect=/gio-hang">Đăng nhập</RouterLink> để mở giỏ hàng tài khoản của bạn.</p></header>
    <p v-if="cart.error" class="shop-alert" role="alert">{{ cart.error }} <button class="text-button" @click="cart.refresh">Thử lại</button></p>
    <section v-if="!cart.items.length" class="shop-card shop-empty"><h2>Giỏ hàng của bạn đang trống.</h2><p>Khám phá bộ sưu tập và tìm đôi giày phù hợp.</p><RouterLink to="/san-pham" class="button button-primary">Khám phá sản phẩm</RouterLink></section>
    <div v-else class="shop-columns">
      <section aria-label="Sản phẩm trong giỏ" class="cart-list">
        <div class="shop-card cart-selection"><label><input type="checkbox" :checked="cart.allSelected" :indeterminate="cart.partlySelected" @change="cart.selectAll($event.target.checked)"><strong>Chọn tất cả ({{ cart.items.length }})</strong></label><span role="status">Đã chọn {{ cart.selectedItems.length }} sản phẩm</span></div>
        <p v-if="cart.loading" class="shop-muted" role="status">Đang cập nhật giá và tồn kho…</p>
        <article v-for="line in cart.lines" :key="line.variantId" class="shop-card cart-item" :class="{ 'cart-item-selected': cart.isSelected(line.variantId) }">
          <input class="cart-checkbox" type="checkbox" :checked="cart.isSelected(line.variantId)" :aria-label="'Chọn ' + (line.variant?.name || 'biến thể ' + line.variantId) + ', ' + (line.variant?.color || '') + ', size ' + (line.variant?.size || '')" @change="cart.setSelected(line.variantId, $event.target.checked)">
          <div class="cart-line-content">
            <ProductImage :src="line.variant?.image" :alt="line.variant?.name || 'Sản phẩm'" />
            <div class="cart-item-info"><RouterLink v-if="line.variant" :to="'/san-pham/' + line.variant.productId"><h2>{{ line.variant.name }}</h2></RouterLink><h2 v-else>Biến thể #{{ line.variantId }}</h2><p>{{ line.variant?.sku }}</p><p v-if="line.variant">{{ line.variant.color }} · Size {{ line.variant.size }}</p><ProductPrice v-if="line.variant" :item="line.variant" /><QuantityStepper :model-value="line.quantity" :max="line.variant?.stock || 0" :disabled="cart.loading || !line.variant?.available" @update:model-value="cart.setQuantity(line.variantId, $event)" /><p v-if="cart.verified && (!line.variant?.available || line.quantity > line.variant.stock)" class="shop-error">{{ !line.variant?.available ? 'Sản phẩm đã hết hàng hoặc ngừng bán. Bỏ chọn sản phẩm này để thanh toán các sản phẩm khác.' : 'Tồn kho vừa thay đổi. Chỉ còn ' + line.variant.stock + ' sản phẩm; vui lòng giảm số lượng hoặc bỏ chọn.' }}</p></div>
            <div class="cart-item-end"><button class="icon-button" type="button" :aria-label="'Bỏ ' + (line.variant?.name || 'sản phẩm')" @click="cart.remove(line.variantId)">×</button><strong>{{ line.variant ? money(Number(line.variant.giaSauGiam) * line.quantity) : '—' }}</strong></div>
          </div>
        </article>
      </section>
      <aside class="shop-card shop-summary"><h2>Sản phẩm đã chọn</h2><dl><dt>Số lượng thanh toán</dt><dd>{{ cart.selectedCount }}</dd><dt>Tạm tính</dt><dd class="summary-total">{{ cart.verified ? money(cart.selectedSubtotal) : 'Đang tải…' }}</dd></dl><p class="shop-muted">Chỉ thanh toán sản phẩm được tích chọn. Các sản phẩm còn lại vẫn nằm trong giỏ hàng.</p><RouterLink v-if="!cart.selectedInvalid && !cart.loading" to="/thanh-toan" class="button button-primary">Thanh toán ({{ cart.selectedCount }}) →</RouterLink><button v-else class="button button-primary" disabled>{{ !cart.selectedItems.length ? 'Chọn sản phẩm để thanh toán' : 'Kiểm tra sản phẩm đã chọn' }}</button><RouterLink to="/san-pham" class="shop-continue">Tiếp tục mua sắm</RouterLink><p class="cart-storage-note">Thêm vào giỏ chưa giữ hay trừ hàng trong kho. Giá và tồn kho sẽ được kiểm tra lại khi đặt hàng.</p></aside>
    </div>
  </div>
</template>
<style scoped>
.cart-selection{display:flex;justify-content:space-between;align-items:center;gap:12px;font-size:12px;padding:17px 22px}.cart-selection label{display:flex;align-items:center;gap:12px;cursor:pointer}.cart-selection span{color:var(--customer-muted)}.cart-checkbox,.cart-selection input{width:18px;height:18px;accent-color:var(--customer-primary);cursor:pointer;flex:0 0 18px}.cart-item{align-items:flex-start;flex-wrap:nowrap}.cart-checkbox{margin-top:7px}.cart-item-selected{border-color:#88b3ed;background:#fcfdff}.cart-line-content{display:flex;flex:1;gap:18px;min-width:0}.cart-line-content>.product-image{width:100px;height:100px;flex:0 0 100px;border-radius:9px;overflow:hidden;background:var(--customer-bg)}.cart-storage-note{font-size:11px;color:var(--customer-muted);line-height:1.8;margin-top:20px}@media(max-width:600px){.cart-selection{align-items:flex-start;flex-direction:column;padding:15px}.cart-line-content{flex-wrap:wrap;gap:12px}.cart-line-content>.product-image{width:76px;height:76px;flex-basis:76px}.cart-item{gap:9px}.cart-item-info{min-width:0}.cart-item-end{min-width:0}.cart-item-end strong{overflow-wrap:anywhere}}
</style>
