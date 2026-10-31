<script setup>
import { ref, computed, onMounted } from 'vue'
import { commerceService, commerceError } from '../services/commerceService'
import { money } from '../utils/money'
import { useLiveRefresh } from '../composables/useLiveRefresh'
import ProductImage from '../components/ProductImage.vue'
import BaseIcon from '../components/BaseIcon.vue'
const orders = ref([]), loading = ref(false), error = ref(''), page = ref(0), status = ref(''), search = ref(''), emptyNext = ref(false)
const statuses = [{ value: '', label: 'Tất cả trạng thái' }, { value: 0, label: 'Chờ xác nhận' }, { value: 1, label: 'Đã xác nhận' }, { value: 2, label: 'Chờ lấy hàng' }, { value: 3, label: 'Đang giao hàng' }, { value: 4, label: 'Đã giao hàng' }, { value: 5, label: 'Hoàn thành' }, { value: 6, label: 'Đã hủy' }, { value: 7, label: 'Hoàn tiền' }, { value: 9, label: 'Giao hàng thất bại' }]
const visible = computed(() => {
  const term = search.value.trim().toLocaleLowerCase('vi-VN')
  return orders.value.filter(o => (status.value === '' || o.status === Number(status.value)) && (!term || [o.code, ...o.items.map(i => i.name)].join(' ').toLocaleLowerCase('vi-VN').includes(term)))
})
const count = order => order.items.reduce((n, item) => n + item.quantity, 0)
const dateLabel = value => new Intl.DateTimeFormat('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh', day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(value))
const statusTone = status => ({ 0: 'waiting', 1: 'processing', 2: 'processing', 3: 'shipping', 4: 'success', 5: 'success', 6: 'canceled', 7: 'refund', 9: 'failed' }[status] || 'canceled')
async function load() { if (loading.value) return; loading.value = true; try { const rows = await commerceService.orders(page.value); orders.value = rows; emptyNext.value = rows.length < 20; error.value = '' } catch (e) { error.value = commerceError(e) } finally { loading.value = false } }
function changePage(delta) { page.value += delta; load() }
function clearFilters() { status.value = ''; search.value = '' }
onMounted(load); useLiveRefresh(load, 60000)
</script>

<template>
  <div class="container shop-page orders-page">
    <header class="orders-heading">
      <div><span class="eyebrow">THEO DÕI ĐƠN HÀNG</span><h1>Đơn hàng của bạn</h1><p>Xem trạng thái và thông tin các đơn hàng đã đặt.</p></div>
      <RouterLink to="/san-pham" class="orders-shopping">Tiếp tục mua sắm <BaseIcon name="arrow" /></RouterLink>
    </header>
    <div class="orders-toolbar">
      <label class="orders-search"><BaseIcon name="search" /><input v-model="search" type="search" aria-label="Tìm mã đơn hoặc sản phẩm trong trang" placeholder="Tìm mã đơn hoặc tên sản phẩm"></label>
      <label class="orders-filter"><span>Trạng thái</span><select v-model="status"><option v-for="s in statuses" :key="s.value" :value="s.value">{{ s.label }}</option></select></label>
    </div>
    <div class="orders-result"><span>{{ visible.length }} / {{ orders.length }} đơn trong trang {{ page + 1 }}<span class="orders-scope"> · Tìm kiếm và lọc trong trang hiện tại</span></span><span v-if="loading" role="status">Đang cập nhật…</span></div>
    <p v-if="error" class="shop-alert" role="alert">{{ error }} <button type="button" class="text-button" @click="load">Thử lại</button></p>
    <p v-if="loading && !orders.length" class="shop-card" role="status">Đang tải đơn hàng…</p>
    <section v-else-if="!orders.length && !error" class="shop-card orders-empty"><BaseIcon name="box" /><h2>{{ page === 0 ? 'Bạn chưa có đơn hàng' : 'Không có thêm đơn hàng' }}</h2><p>Những đơn hàng của bạn sẽ được hiển thị tại đây.</p><RouterLink class="button button-primary" to="/san-pham">Khám phá sản phẩm</RouterLink></section>
    <section v-else-if="!visible.length && !error" class="shop-card orders-empty"><BaseIcon name="search" /><h2>Không tìm thấy đơn phù hợp</h2><p>Thử đổi từ khóa hoặc trạng thái trong trang này.</p><button class="button button-outline" type="button" @click="clearFilters">Xóa bộ lọc</button></section>
    <div class="orders-list" :aria-busy="loading">
      <article v-for="order in visible" :key="order.code" class="shop-card order-entry">
        <header class="order-entry-heading">
          <div class="order-entry-reference"><span class="order-box-icon"><BaseIcon name="box" /></span><div><RouterLink :to="'/don-hang/' + order.code" class="order-code">#{{ order.code }}</RouterLink><time :datetime="order.createdAt">{{ dateLabel(order.createdAt) }}</time></div></div>
          <span class="order-state" :class="statusTone(order.status)"><i aria-hidden="true"></i>{{ order.statusLabel }}</span>
        </header>
        <div class="order-entry-products">
          <ProductImage :src="order.items[0]?.image" :alt="order.items[0]?.name || 'Đơn hàng'" />
          <div class="order-entry-copy"><h2>{{ order.items[0]?.name || 'Đơn hàng' }}</h2><p v-if="order.items[0]">{{ [order.items[0].color, order.items[0].size ? 'Size ' + order.items[0].size : ''].filter(Boolean).join(' · ') }}<span v-if="order.items[0].quantity"> · SL {{ order.items[0].quantity }}</span></p><span class="order-items-count">{{ count(order) }} sản phẩm<template v-if="order.items.length > 1"> · {{ order.items.length }} dòng sản phẩm</template></span><span v-if="order.items.length > 1" class="order-more">+ {{ order.items.length - 1 }} dòng sản phẩm khác</span></div>
        </div>
        <footer class="order-entry-footer"><div class="order-amount"><span>Tổng thanh toán</span><strong>{{ money(order.total) }}</strong></div><RouterLink :to="'/don-hang/' + order.code" class="button button-outline order-detail-link">Xem chi tiết <BaseIcon name="arrow" /></RouterLink></footer>
      </article>
    </div>
    <nav v-if="page > 0 || orders.length" class="pagination orders-pagination" aria-label="Trang đơn hàng"><button type="button" :disabled="page === 0 || loading" @click="changePage(-1)">← Trước</button><span>Trang {{ page + 1 }}</span><button type="button" :disabled="emptyNext || loading" @click="changePage(1)">Sau →</button></nav>
  </div>
</template>

<style scoped>
.orders-page{max-width:1120px;padding-top:36px}.orders-heading{display:flex;justify-content:space-between;align-items:center;gap:24px;margin-bottom:26px}.orders-heading .eyebrow{font-size:10px;letter-spacing:2px}.orders-heading h1{font-size:30px;letter-spacing:-.8px;line-height:1.25;margin:9px 0}.orders-heading p{font-size:12px;color:var(--customer-muted);line-height:1.7;margin:0}.orders-shopping{display:flex;align-items:center;gap:9px;color:var(--customer-primary);font-size:12px;font-weight:650;white-space:nowrap}.orders-shopping .icon{width:17px;height:17px}.orders-toolbar{display:flex;gap:24px;align-items:center;padding:16px;background:#fff;border:1px solid var(--customer-border);border-radius:12px}.orders-search{position:relative;flex:1;min-width:0}.orders-search>.icon{position:absolute;left:13px;top:50%;transform:translateY(-50%);width:18px;height:18px;color:#8193ab}.orders-search input{width:100%;margin:0;padding:11px 13px 11px 39px;border:1px solid var(--customer-border);border-radius:8px;background:#f8fafd;font:inherit;font-size:12px}.orders-filter{display:flex;align-items:center;gap:12px;flex-shrink:0;font-size:12px;color:var(--customer-muted)}.orders-filter select{margin:0;width:185px;min-height:40px;padding:10px 30px 10px 12px;border:1px solid var(--customer-border);border-radius:8px;background:#fff;font:inherit;color:var(--customer-text)}.orders-result{display:flex;justify-content:space-between;gap:12px;color:var(--customer-muted);font-size:10px;padding:13px 2px}.orders-list{display:grid;gap:16px}.order-entry.shop-card{padding:0;overflow:hidden;box-shadow:0 3px 12px #16355804}.order-entry-heading{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:16px 22px;border-bottom:1px solid #eef2f7}.order-entry-reference{display:flex;align-items:center;gap:11px}.order-box-icon{width:34px;height:34px;display:grid;place-items:center;background:#f1f6fc;border:1px solid #e7eef7;border-radius:9px;color:#557da5}.order-box-icon .icon{width:18px;height:18px}.order-code{display:block;font-size:13px;font-weight:750;color:var(--customer-ink)}.order-entry-reference time{display:block;font-size:10px;margin-top:3px;color:var(--customer-muted)}.order-state{display:inline-flex;align-items:center;gap:6px;padding:6px 10px;font-size:10px;font-weight:650;border-radius:20px;background:#eef4fc;color:#3169a1;white-space:nowrap}.order-state i{width:5px;height:5px;border-radius:50%;background:currentColor}.order-state.waiting{color:#92600a;background:#fff6df}.order-state.shipping{color:#206bc3;background:#eaf3ff}.order-state.success{color:#187451;background:#eaf8f0}.order-state.canceled{color:#6d7787;background:#f1f3f6}.order-state.refund{color:#7557a7;background:#f3edff}.order-state.failed{color:#bb4141;background:#fff0f0}.order-entry-products{display:flex;align-items:center;gap:18px;padding:20px 22px}.order-entry-products>.product-image{width:76px;height:76px;flex:0 0 76px;background:#f5f7fb;border:1px solid #edf1f6;border-radius:10px;overflow:hidden}.order-entry-copy{min-width:0}.order-entry-copy h2{font-size:14px;margin:0 0 5px;line-height:1.5;overflow-wrap:anywhere}.order-entry-copy p{font-size:11px;color:var(--customer-muted);margin:0 0 5px}.order-items-count{font-size:10px;color:var(--customer-muted)}.order-more{display:inline-block;margin-left:9px;font-size:10px;color:#497cab}.order-entry-footer{display:flex;justify-content:space-between;align-items:center;gap:20px;padding:14px 22px;border-top:1px solid #edf2f8;background:#fbfcfe}.order-amount{display:flex;align-items:baseline;gap:14px}.order-amount>span{font-size:11px;color:var(--customer-muted)}.order-amount strong{font-size:17px;color:var(--customer-ink);letter-spacing:-.3px}.order-detail-link{min-height:36px;padding:8px 14px;font-size:11px;display:inline-flex;align-items:center;gap:10px;background:#fff}.order-detail-link .icon{width:16px;height:16px}.orders-empty{text-align:center;padding:44px 20px}.orders-empty>.icon{width:40px;height:40px;color:#8aa8cd;margin-bottom:12px}.orders-empty h2{font-size:18px}.orders-empty p{font-size:12px;color:var(--customer-muted);margin:12px 0 22px}.orders-pagination{margin-top:24px}
@media(max-width:650px){.orders-page{padding-top:25px}.orders-heading{align-items:flex-start;margin-bottom:22px}.orders-heading h1{font-size:25px}.orders-shopping{display:none}.orders-toolbar{flex-direction:column;align-items:stretch;gap:12px;padding:12px}.orders-filter{justify-content:space-between}.orders-filter select{flex:1;max-width:250px}.orders-scope{display:none}.order-entry-heading{padding:14px;gap:9px}.order-entry-products{padding:17px 14px;gap:12px}.order-entry-copy h2{font-size:13px}.order-entry-products>.product-image{width:65px;height:65px;flex-basis:65px}.order-entry-footer{padding:12px 14px;gap:10px}.order-amount{display:flex;flex-direction:column;gap:3px}.order-amount strong{font-size:16px}.order-more{display:block;margin:4px 0 0}.order-state{font-size:9px;padding:5px 8px}.order-box-icon{display:none}}
</style>
