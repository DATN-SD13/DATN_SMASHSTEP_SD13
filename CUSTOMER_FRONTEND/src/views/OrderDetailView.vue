<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import OrderQrPayment from '../components/OrderQrPayment.vue'
import { useRoute } from 'vue-router'
import { commerceService, commerceError } from '../services/commerceService'
import { useLiveRefresh } from '../composables/useLiveRefresh'
import { money } from '../utils/money'
import OrderStatusTimeline from '../components/OrderStatusTimeline.vue'
import ShippingInfoModal from '../components/ShippingInfoModal.vue'
import ProductImage from '../components/ProductImage.vue'
const route = useRoute(), order = ref(null), error = ref(''), loading = ref(false), editing = ref(false), canceling = ref(false), confirmCancel = ref(false), notice = ref('')
const router = useRouter(), cart = useCartStore(), owner = cart.owner
let version = 0
watch(() => cart.owner, next => { if (next !== owner) { version++; order.value = null; editing.value = false; router.replace('/don-hang') } }, { flush: 'sync' })
onBeforeUnmount(() => { version++ })
async function load() { if (editing.value || canceling.value || cart.owner !== owner) return; const current = ++version; loading.value = true; try { const next = await commerceService.order(route.params.ma); if (current === version && cart.owner === owner) { order.value = next; error.value = '' } } catch (e) { if (current === version && cart.owner === owner) error.value = commerceError(e) } finally { if (current === version && cart.owner === owner) loading.value = false } }
watch(() => route.params.ma, () => { order.value = null; editing.value = false; confirmCancel.value = false; load() }, { immediate: true })
async function cancel() {
  if (canceling.value || cart.owner !== owner || !order.value) return
  const current = version, code = order.value.code; canceling.value = true
  try { const next = await commerceService.cancel(code); if (current === version && cart.owner === owner) { order.value = next; confirmCancel.value = false; notice.value = 'Đã hủy đơn và hoàn lại tồn kho, lượt voucher tương ứng.' } }
  catch (e) { if (current === version && cart.owner === owner) error.value = commerceError(e) }
  finally { canceling.value = false }
}
function saved(next) { order.value = next; editing.value = false; notice.value = 'Đã cập nhật thông tin nhận hàng. Vui lòng kiểm tra lại phí vận chuyển và tổng tiền.' }
useLiveRefresh(load, 60000)
</script>
<template><div class="container shop-page order-detail-page"><RouterLink to="/don-hang" class="back-link">← Đơn hàng của bạn</RouterLink><p v-if="error" class="shop-alert" role="alert">{{ error }} <button class="text-button" @click="load">Thử lại</button></p><p v-if="loading && !order" class="shop-card" role="status">Đang tải chi tiết đơn hàng…</p><template v-if="order"><header class="shop-heading order-detail-heading"><div><span class="eyebrow">ORDER DETAILS</span><h1>Đơn hàng #{{ order.code }}</h1><p>Đặt lúc {{ new Date(order.createdAt).toLocaleString('vi-VN') }}</p></div><span class="order-status" :class="{ canceled: order.status >= 6 }">{{ order.statusLabel }}</span></header><p v-if="notice" class="shop-alert shop-alert--info" role="status">{{ notice }}</p><OrderStatusTimeline :status="order.status" :history="order.history" /><OrderQrPayment v-if="order.paymentMethodCode === 'CHUYEN_KHOAN'" :key="order.code + ':' + order.status" :code="order.code" @paid="load" /><div v-if="order.editable" class="order-actions"><button class="button button-outline" :disabled="canceling" @click="confirmCancel = true">Hủy đơn</button><div v-if="confirmCancel" class="shop-card" role="alert"><strong>Bạn muốn hủy đơn hàng này?</strong><p>Chỉ hủy khi chưa chuyển tiền và đơn đang chờ xác nhận. Nếu đã chuyển khoản, hãy giữ đơn và liên hệ cửa hàng đối soát.</p><button class="button button-outline" :disabled="canceling" @click="confirmCancel = false">Giữ đơn</button> <button class="button button-primary" :disabled="canceling" @click="cancel">{{ canceling ? 'Đang hủy…' : 'Xác nhận hủy đơn' }}</button></div></div><section class="shop-card"><header class="shop-card-heading"><h2>Thông tin nhận hàng</h2><button v-if="order.shippingEditable" class="button button-outline" :disabled="canceling" @click="editing = true">Chỉnh sửa thông tin</button></header><div class="shipping-snapshot"><strong>{{ order.receiverName }} · {{ order.phone }}</strong><p>{{ order.address }}</p><p class="shop-muted">Phương thức thanh toán: {{ order.paymentMethod || 'Chưa có thông tin' }}</p><p v-if="order.note" class="shop-muted">Ghi chú: {{ order.note }}</p></div></section><section class="shop-card"><h2>Sản phẩm</h2><article v-for="item in order.items" :key="item.variantId" class="order-product"><ProductImage :src="item.image" :alt="item.name" /><div><h3>{{ item.name }}</h3><p class="shop-muted">{{ item.color }} · Size {{ item.size }} × {{ item.quantity }}</p><p>{{ money(item.unitPrice) }} / sản phẩm</p></div><strong>{{ money(item.total) }}</strong></article><dl class="order-totals"><dt>Tạm tính</dt><dd>{{ money(order.subtotal) }}</dd><dt>Giảm giá</dt><dd>−{{ money(order.discount) }}</dd><dt>Phí vận chuyển</dt><dd>{{ money(order.shipping || 0) }}</dd><dt class="summary-total">Tổng cộng</dt><dd class="summary-total">{{ money(order.total) }}</dd></dl></section><section v-if="order.history.length" class="shop-card"><h2>Lịch sử đơn hàng</h2><ol class="order-history"><li v-for="(event, i) in order.history" :key="i"><time>{{ new Date(event.at).toLocaleString('vi-VN') }}</time><strong>{{ event.label }}</strong><p v-if="event.note">{{ event.note }}</p></li></ol></section><ShippingInfoModal v-if="editing" :order="order" @close="editing = false" @saved="saved" /></template></div></template>
