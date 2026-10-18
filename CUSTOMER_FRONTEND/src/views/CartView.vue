<script setup>
import { onMounted } from 'vue'
import { useCartStore } from '../stores/cart'
import { money } from '../utils/money'
import ProductImage from '../components/ProductImage.vue'
import ProductPrice from '../components/ProductPrice.vue'
import QuantityStepper from '../components/QuantityStepper.vue'
const cart = useCartStore()
onMounted(cart.refresh)
</script>
<template><div class="container shop-page"><header class="shop-heading"><span class="eyebrow">SMASHSTEP BAG</span><h1>Giỏ hàng của bạn</h1><p>Những lựa chọn cho bước chân tiếp theo.</p></header>
  <p v-if="cart.error" class="shop-alert" role="alert">{{ cart.error }} <button class="text-button" @click="cart.refresh">Thử lại</button></p>
  <section v-if="!cart.items.length" class="shop-card shop-empty"><h2>Giỏ hàng của bạn đang trống.</h2><p>Khám phá bộ sưu tập và tìm đôi giày phù hợp.</p><RouterLink to="/san-pham" class="button button-primary">Khám phá sản phẩm</RouterLink></section>
  <div v-else class="shop-columns"><section aria-label="Sản phẩm trong giỏ" class="cart-list"><p v-if="cart.loading" class="shop-muted" role="status">Đang cập nhật giá và tồn kho…</p>
    <article v-for="line in cart.lines" :key="line.variantId" class="shop-card cart-item"><ProductImage :src="line.variant?.image" :alt="line.variant?.name || 'Sản phẩm'" /><div class="cart-item-info"><RouterLink v-if="line.variant" :to="'/san-pham/' + line.variant.productId"><h2>{{ line.variant.name }}</h2></RouterLink><h2 v-else>Biến thể #{{ line.variantId }}</h2><p>{{ line.variant?.sku }}</p><p v-if="line.variant">{{ line.variant.color }} · Size {{ line.variant.size }}</p><ProductPrice v-if="line.variant" :item="line.variant" /><QuantityStepper :model-value="line.quantity" :max="line.variant?.stock || 0" :disabled="cart.loading || !line.variant?.available" @update:model-value="cart.setQuantity(line.variantId, $event)" /><p v-if="cart.verified && (!line.variant?.available || line.quantity > line.variant.stock)" class="shop-error">{{ !line.variant?.available ? 'Sản phẩm đã hết hàng hoặc ngừng bán. Vui lòng bỏ khỏi giỏ.' : 'Tồn kho vừa thay đổi. Chỉ còn ' + line.variant.stock + ' sản phẩm; vui lòng giảm số lượng.' }}</p></div><div class="cart-item-end"><button class="icon-button" type="button" :aria-label="'Bỏ ' + (line.variant?.name || 'sản phẩm')" @click="cart.remove(line.variantId)">×</button><strong>{{ line.variant ? money(Number(line.variant.giaSauGiam) * line.quantity) : '—' }}</strong></div></article>
  </section><aside class="shop-card shop-summary"><h2>Tóm tắt đơn hàng</h2><dl><dt>Tổng số lượng</dt><dd>{{ cart.count }}</dd><dt>Tạm tính</dt><dd class="summary-total">{{ cart.verified ? money(cart.subtotal) : 'Đang tải…' }}</dd></dl><p class="shop-muted">Phí giao hàng và voucher được tính ở bước thanh toán.</p><RouterLink v-if="!cart.invalid && !cart.loading" to="/thanh-toan" class="button button-primary">Tiến hành thanh toán →</RouterLink><button v-else class="button button-primary" disabled>Kiểm tra lại giỏ hàng</button><RouterLink to="/san-pham" class="shop-continue">Tiếp tục mua sắm</RouterLink></aside></div>
</div></template>
