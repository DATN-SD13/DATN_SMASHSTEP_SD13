<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { invoiceData } from '../services/invoiceData'

const route = useRoute()
const router = useRouter()
const invoice = computed(() => invoiceData.find(item => item.code === route.params.code) || invoiceData[0])
const steps = [
  { key: 'waiting', label: 'Chờ xác nhận', icon: 'bi-hourglass-split' },
  { key: 'confirmed', label: 'Đã xác nhận', icon: 'bi-check2-circle' },
  { key: 'ready', label: 'Chờ giao hàng', icon: 'bi-box-seam' },
  { key: 'shipping', label: 'Đang giao hàng', icon: 'bi-truck' },
  { key: 'delivered', label: 'Đã giao hàng', icon: 'bi-bag-check' },
  { key: 'done', label: 'Hoàn thành', icon: 'bi-flag' }
]
const progress = computed(() => {
  const s = invoice.value.statusClass
  if (s === 'cancel' || s === 'refund') return -1
  if (s === 'waiting') return 0
  if (s === 'confirmed') return 1
  if (s === 'ready') return 2
  if (s === 'shipping') return 3
  if (s === 'delivered') return 4
  return 5
})
const subtotal = computed(() => (invoice.value.items || []).reduce((sum, item) => sum + item.price * item.quantity, 0))
function money(value) { return `${Number(value || 0).toLocaleString('vi-VN')} đ` }
function orderType(type) { return type === 'online' ? 'Trực tuyến' : type === 'delivery' ? 'Giao hàng' : 'Tại quầy' }
const productQuery = ref('')
const productVariant = ref('all')
const filteredItems = computed(() => {
  const q = productQuery.value.trim().toLowerCase()
  return (invoice.value.items || []).filter(item => {
    const text = `${item.name || ''} ${item.variant || ''}`.toLowerCase()
    const queryOk = !q || text.includes(q)
    const variantOk = productVariant.value === 'all' || item.variant === productVariant.value
    return queryOk && variantOk
  })
})
const productVariants = computed(() => [...new Set((invoice.value.items || []).map(item => item.variant).filter(Boolean))])
function printInvoice() { window.print() }
</script>

<template>
  <AdminLayout>
    <main class="detail-page container-fluid">
      <div class="page-title-row">
        <div>
          <div class="breadcrumb"><span>Trang chủ</span><b>/</b><RouterLink to="/hoa-don">Hóa đơn</RouterLink><b>/</b><strong>Chi tiết</strong></div>
          <h1>Quản Lý Hóa Đơn</h1>
        </div>
      </div>

      <button class="back-link" @click="router.push('/hoa-don')"><i class="bi bi-chevron-left"></i> <span>Chi tiết hóa đơn: <b>{{ invoice.code }}</b></span></button>

      <div class="detail-layout">
        <div class="main-column">
          <section class="panel card shadow-sm timeline-panel">
            <h2><i class="bi bi-clipboard2-check"></i> Trạng thái đơn hàng</h2>
            <div v-if="progress >= 0" class="timeline">
              <div v-for="(step, index) in steps" :key="step.key" class="timeline-step" :class="{ reached: index <= progress, current: index === progress }">
                <div class="timeline-node"><i :class="['bi', step.icon]"></i></div>
                <div class="step-label">{{ step.label }}</div>
                <small v-if="index < progress || index === progress">{{ invoice.date }}</small>
              </div>
            </div>
            <div v-else class="cancel-state"><span class="cancel-icon"><i class="bi bi-x-circle"></i></span><div><strong>{{ invoice.status }}</strong><p>Đơn hàng đã dừng ở trạng thái này.</p></div></div>
            <div class="timeline-actions"><button class="soft-button btn btn-outline-primary btn-sm" @click="$router.push('/hoa-don')"><i class="bi bi-clock-history"></i> Lịch sử thao tác</button></div>
          </section>

          <div class="info-grid">
            <section class="panel card shadow-sm info-panel">
              <h2><i class="bi bi-person-lock"></i> Thông tin khách hàng</h2>
              <div class="info-row"><span>Tên khách hàng</span><strong>{{ invoice.customer }}</strong></div>
              <div class="info-row"><span>Số điện thoại</span><strong>{{ invoice.phone }}</strong></div>
              <div class="info-row"><span>Email</span><strong>Chưa cập nhật</strong></div>
            </section>
            <section class="panel card shadow-sm info-panel">
              <h2><i class="bi bi-geo-alt"></i> Thông tin giao hàng</h2>
              <div class="info-row address-row"><span>Địa chỉ</span><strong>{{ invoice.address || 'Chưa cập nhật' }}</strong></div>
              <div class="info-row"><span>Loại đơn</span><strong><span class="type-pill">{{ orderType(invoice.type) }}</span></strong></div>
            </section>
          </div>

          <section class="panel card shadow-sm products-panel">
            <div class="products-heading">
              <h2><i class="bi bi-box-seam"></i> Danh sách sản phẩm</h2>
              <div class="product-filter">
                <div class="product-search">
                  <i class="bi bi-search"></i>
                  <input v-model="productQuery" type="text" placeholder="Lọc theo tên sản phẩm..." aria-label="Lọc sản phẩm" />
                </div>
                <select v-model="productVariant" aria-label="Lọc phân loại sản phẩm">
                  <option value="all">Tất cả phân loại</option>
                  <option v-for="variant in productVariants" :key="variant" :value="variant">{{ variant }}</option>
                </select>
              </div>
            </div>
            <div class="product-table-wrap">
              <table class="product-table table table-hover align-middle mb-0">
                <thead><tr><th>Sản phẩm</th><th>Phân loại</th><th>Đơn giá</th><th>Số lượng</th><th>Thành tiền</th></tr></thead>
                <tbody>
                  <tr v-for="(item, index) in filteredItems" :key="index">
                    <td><div class="product-name"><span class="product-thumb"><i class="bi bi-bag"></i></span><strong>{{ item.name }}</strong></div></td>
                    <td>{{ item.variant }}</td><td>{{ money(item.price) }}</td><td>{{ item.quantity }}</td><td class="product-total">{{ money(item.price * item.quantity) }}</td>
                  </tr>
                  <tr v-if="!filteredItems.length">
                    <td colspan="5" class="product-empty"><i class="bi bi-search"></i><span>Không tìm thấy sản phẩm phù hợp.</span></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <aside class="side-column">
          <section class="panel card shadow-sm payment-panel">
            <h2><i class="bi bi-calendar2-check"></i> Tổng kết thanh toán</h2>
            <div class="amount-row"><span>Tổng tiền hàng</span><strong>{{ money(subtotal) }}</strong></div>
            <div class="amount-row"><span>Giảm giá</span><strong class="discount">− {{ money(invoice.discount) }}</strong></div>
            <div class="amount-row"><span>Phí vận chuyển</span><strong>+ {{ money(invoice.shippingFee) }}</strong></div>
            <div class="grand-total"><span>TỔNG TIỀN</span><strong>{{ money(invoice.total) }}</strong></div>
          </section>

          <section class="panel card shadow-sm payment-history">
            <h2><i class="bi bi-clock-history"></i> Lịch sử thanh toán</h2>
            <div class="payment-method"><div><strong>{{ invoice.payment === 'COD' ? 'Thanh toán khi nhận hàng (COD)' : invoice.payment }}</strong><small>Lúc {{ invoice.date }}</small></div><span class="paid-tag">{{ invoice.statusClass === 'cancel' ? 'Chưa thanh toán' : 'Đã thanh toán' }}</span></div>
            <strong class="paid-amount">{{ money(invoice.total) }}</strong>
            <button class="print-button btn btn-primary" @click="printInvoice"><i class="bi bi-printer"></i> In hóa đơn</button>
          </section>
          <section class="panel card shadow-sm invoice-meta">
            <h2><i class="bi bi-info-circle"></i> Thông tin hóa đơn</h2>
            <div class="info-row"><span>Mã hóa đơn</span><strong>{{ invoice.code }}</strong></div>
            <div class="info-row"><span>Nhân viên</span><strong>{{ invoice.employee }}</strong></div>
            <div class="info-row"><span>Ngày tạo</span><strong>{{ invoice.date }}</strong></div>
            <div class="info-row"><span>Trạng thái</span><strong><span :class="['status-pill', invoice.statusClass]">{{ invoice.status }}</span></strong></div>
          </section>
        </aside>
      </div>
    </main>
  </AdminLayout>
</template>

<style scoped>

.detail-page{max-width:1750px;margin:0 auto;padding:22px 28px 42px;color:#172d38}.page-title-row{margin-bottom:14px}.breadcrumb{font-size:11px;color:#5f7580;margin-bottom:7px}.breadcrumb b{padding:0 9px;color:#9aabb3}.breadcrumb a{color:#52707d;text-decoration:none}.breadcrumb strong{color:#087fb8;font-weight:800}.page-title-row h1{margin:0;font-size:26px;color:#102c38;font-weight:900}.back-link{display:flex;align-items:center;gap:10px;border:0;background:transparent;color:#25404c;padding:0 0 15px 3px;font-size:13px;font-weight:750;cursor:pointer}.back-link i{font-size:15px;color:#087fb8}.back-link b{color:#087fb8}.detail-layout{display:grid;grid-template-columns:minmax(0,1.85fr) minmax(330px,.75fr);gap:18px;align-items:start}.main-column,.side-column{display:grid;gap:18px;min-width:0}.panel{background:#fff;border:1px solid #d6e2e8!important;border-radius:16px;box-shadow:0 5px 18px rgba(21,54,70,.055)!important;padding:20px}.panel h2{display:flex;align-items:center;gap:9px;margin:0 0 20px;color:#102c38;font-size:14px;font-weight:850}.panel h2 i{color:#087fb8;font-size:15px}.timeline-panel{padding:20px 21px 16px;min-height:190px}.timeline{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));position:relative;padding:8px 0 0}.timeline:before{content:"";position:absolute;left:8%;right:8%;height:3px;background:#d5e4eb;top:24px}.timeline-step{position:relative;display:flex;align-items:center;flex-direction:column;text-align:center;min-width:0}.timeline-node{width:36px;height:36px;border-radius:50%;display:grid;place-items:center;background:#e8f0f4;color:#29434f;border:2px solid #fff;box-shadow:0 0 0 1px #cbdde5;z-index:1;font-size:14px}.timeline-step.reached .timeline-node{background:#087fb8;color:#fff;box-shadow:0 0 0 1px #087fb8}.timeline-step.current .timeline-node{box-shadow:0 0 0 4px #d8eff9,0 0 0 5px #087fb8}.step-label{font-size:11px;font-weight:750;color:#26414d;margin-top:11px;line-height:1.4}.timeline-step.reached .step-label{color:#0874a8}.timeline-step small{font-size:9px;color:#607782;margin-top:5px}.timeline-actions{display:flex;justify-content:flex-end;margin-top:18px}.soft-button{height:35px;border:1px solid #b9d9e7;border-radius:18px;background:#f1f9fc;color:#16627e;padding:0 15px;font-size:11px;font-weight:750;cursor:pointer}.soft-button:hover{background:#dff2fa;color:#0874a8}.info-grid{display:grid;grid-template-columns:1fr 1fr;gap:18px}.info-panel{padding:18px 20px 9px;min-height:148px}.info-panel h2{margin-bottom:13px}.info-row{display:flex;justify-content:space-between;align-items:flex-start;gap:15px;padding:11px 0;border-bottom:1px solid #e7eef1;font-size:11px}.info-row:last-child{border-bottom:0}.info-row>span{color:#5c7480;flex:0 0 auto}.info-row>strong{color:#19333f;font-weight:750;text-align:right;line-height:1.55;min-width:0}.address-row strong{max-width:65%}.type-pill{display:inline-block;border-radius:6px;background:#dff1fb;color:#086d9e;padding:5px 8px;font-size:10px;font-weight:800}.products-panel{padding:19px 0 0}.products-heading{display:flex;align-items:center;justify-content:space-between;gap:18px;padding:0 20px 17px}.products-heading h2{padding:0;margin:0}.product-filter{display:flex;align-items:center;gap:9px;min-width:0}.product-search{height:38px;width:235px;display:flex;align-items:center;gap:8px;padding:0 11px;border:1px solid #cbdde5;border-radius:9px;background:#fff;color:#52707d}.product-search input{width:100%;border:0;outline:0;background:transparent;color:#19333f;font-size:11px}.product-search input::placeholder{color:#879aa3}.product-search:focus-within{border-color:#087fb8;box-shadow:0 0 0 3px rgba(8,127,184,.1)}.product-filter select{height:38px;min-width:150px;border:1px solid #cbdde5;border-radius:9px;background:#fff;color:#19333f;padding:0 10px;font-size:11px;outline:0}.product-filter select:focus{border-color:#087fb8}.product-table-wrap{overflow-x:auto}.product-table{width:100%;border-collapse:collapse;min-width:650px}.product-table thead{background:#e5f1f7}.product-table th{height:42px;padding:0 13px;text-align:left;vertical-align:middle!important;font-size:10px;line-height:1.2;color:#19333f;font-weight:850;white-space:nowrap}.product-table td{padding:12px 13px;border-bottom:1px solid #e7eef1;color:#27414d;font-size:11px;vertical-align:middle!important;white-space:nowrap}.product-name{display:flex;align-items:center;gap:9px}.product-name strong{color:#19333f;font-size:11px}.product-thumb{width:36px;height:36px;border-radius:8px;background:#e2f2f9;color:#087fb8;display:grid;place-items:center;font-size:16px}.product-total{color:#111827!important;font-weight:850}.product-empty{text-align:center!important;height:90px!important;color:#55707d!important}.product-empty i{margin-right:7px;color:#8eb8cc}.product-empty span{font-size:11px}.payment-panel h2,.payment-history h2,.invoice-meta h2{margin-bottom:17px}.amount-row{display:flex;justify-content:space-between;gap:12px;margin:0 0 14px;font-size:11px}.amount-row span{color:#5b7380}.amount-row strong{color:#19333f;font-weight:800;white-space:nowrap}.amount-row .discount{color:#111827}.grand-total{border-top:1px solid #dbe7ec;margin-top:18px;padding-top:16px;display:flex;justify-content:space-between;gap:10px;align-items:center}.grand-total span{font-size:12px;color:#29434f;font-weight:800}.grand-total strong{font-size:19px;color:#111827}.payment-history{min-height:194px;display:flex;flex-direction:column}.payment-method{display:flex;justify-content:space-between;gap:12px;align-items:flex-start}.payment-method strong{display:block;color:#19333f;font-size:11px;font-weight:750;line-height:1.5}.payment-method small{display:block;color:#607782;font-size:10px;margin-top:7px}.paid-tag{color:#148765;font-size:10px;font-weight:800;white-space:nowrap}.paid-amount{text-align:right;color:#111827;font-size:14px;margin-top:11px}.print-button{height:42px;width:100%;margin-top:auto;border:1px solid #087fb8;border-radius:10px;background:#087fb8;color:#fff;font-size:12px;font-weight:800;cursor:pointer;--bs-btn-bg:#087fb8;--bs-btn-border-color:#087fb8;--bs-btn-hover-bg:#066b9c;--bs-btn-hover-border-color:#066b9c}.invoice-meta{padding-bottom:10px;min-height:190px}.status-pill{display:inline-block;border-radius:13px;padding:5px 9px;background:#dff1fb;color:#086f9f;font-size:10px;font-weight:800}.status-pill.done,.status-pill.delivered{background:#dcf5e9;color:#147c58}.status-pill.waiting{background:#fff0cc;color:#9a5e00}.status-pill.cancel{background:#ffe1e5;color:#b73d4c}.status-pill.shipping{background:#dff1fb;color:#086f9f}.cancel-state{display:flex;align-items:center;gap:13px;padding:8px 0 18px}.cancel-icon{width:42px;height:42px;border-radius:50%;background:#ffe1e5;color:#b73d4c;display:grid;place-items:center;font-size:19px}.cancel-state strong{color:#b73d4c;font-size:13px}.cancel-state p{margin:5px 0 0;color:#607782;font-size:11px}@media(max-width:1050px){.products-heading{align-items:flex-start;flex-direction:column}.product-filter{width:100%}.product-search{flex:1;width:auto}.detail-layout{grid-template-columns:minmax(0,1.35fr) minmax(280px,.9fr)}.info-grid{grid-template-columns:1fr}.timeline{grid-template-columns:repeat(3,minmax(0,1fr));row-gap:20px}.timeline:before{display:none}}@media(max-width:760px){.detail-page{padding:15px 14px 25px}.products-heading{padding-left:16px;padding-right:16px}.product-filter{flex-direction:column;align-items:stretch}.product-search,.product-filter select{width:100%;min-width:0}.detail-layout{grid-template-columns:1fr}.info-grid{grid-template-columns:1fr}.panel{padding:16px}.timeline{grid-template-columns:repeat(3,minmax(0,1fr));row-gap:18px}.step-label{font-size:10px}.back-link{font-size:13px}}@media print{.detail-page{padding:0}.back-link,.timeline-actions,.print-button{display:none}.detail-layout{grid-template-columns:1fr 1fr}.panel{box-shadow:none;break-inside:avoid}}

</style>
