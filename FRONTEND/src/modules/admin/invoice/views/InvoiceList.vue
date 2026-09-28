<script setup>
import { computed, ref } from 'vue'
import InvoiceFilter from '../components/InvoiceFilter.vue'
import InvoiceTable from '../components/InvoiceTable.vue'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
const query = ref({ code: '', from: '', to: '', type: 'all' })
const showDetail = ref(false)
const showCreate = ref(false)
const selected = ref(null)
const toast = ref('')

const allInvoices = ref([
  { code:'HD000081', employee:'Khánh Hà', customer:'Trần Minh Bảo Hoàng', date:'26/09/2026', total:2200000, phone:'0909 899 999', status:'Đã hoàn thành', statusClass:'done', type:'store', payment:'Tiền mặt', address:'Hà Nội' },
  { code:'HD000080', employee:'Nguyễn Văn A', customer:'Nguyễn Thị An', date:'26/09/2026', total:2300000, phone:'0911 111 111', status:'Đã hoàn thành', statusClass:'done', type:'online', payment:'Chuyển khoản', address:'Hà Nội' },
  { code:'HD000079', employee:'Nguyễn Văn A', customer:'Lê Quốc Hưng', date:'25/09/2026', total:2100000, phone:'0911 111 111', status:'Đang giao', statusClass:'shipping', type:'delivery', payment:'COD', address:'Cầu Giấy, Hà Nội' },
  { code:'HD000078', employee:'Khánh Hà', customer:'Phạm Thanh Tú', date:'25/09/2026', total:1850000, phone:'0983 214 567', status:'Đã giao hàng', statusClass:'done', type:'online', payment:'Chuyển khoản', address:'Nam Từ Liêm, Hà Nội' },
  { code:'HD000077', employee:'Trần Huy', customer:'Võ Gia Hân', date:'24/09/2026', total:3420000, phone:'0905 882 114', status:'Chờ xác nhận', statusClass:'waiting', type:'store', payment:'Tiền mặt', address:'Hà Nội' },
  { code:'HD000076', employee:'Khánh Hà', customer:'Đặng Hoài Nam', date:'24/09/2026', total:1290000, phone:'0934 625 881', status:'Đã hủy', statusClass:'cancel', type:'online', payment:'Chuyển khoản', address:'Hà Nội' },
  { code:'HD000075', employee:'Trần Huy', customer:'Bùi Mỹ Linh', date:'23/09/2026', total:2650000, phone:'0972 230 456', status:'Đã giao hàng', statusClass:'done', type:'delivery', payment:'COD', address:'Thanh Xuân, Hà Nội' },
  { code:'HD000074', employee:'Nguyễn Văn A', customer:'Ngô Đức Anh', date:'23/09/2026', total:1760000, phone:'0902 718 663', status:'Đã hoàn thành', statusClass:'done', type:'store', payment:'Thẻ', address:'Hà Nội' },
  { code:'HD000073', employee:'Trần Huy', customer:'Đỗ Phương Vy', date:'22/09/2026', total:2980000, phone:'0968 440 127', status:'Chờ xác nhận', statusClass:'waiting', type:'online', payment:'Chuyển khoản', address:'Hà Nội' },
  { code:'HD000072', employee:'Khánh Hà', customer:'Mai Tiến Thành', date:'22/09/2026', total:4150000, phone:'0918 305 902', status:'Đang giao', statusClass:'shipping', type:'delivery', payment:'COD', address:'Hoàng Mai, Hà Nội' }
])

const invoices = computed(() => allInvoices.value.filter(x => {
  const q = query.value
  const codeOk = !q.code || x.code.toLowerCase().includes(q.code.toLowerCase())
  const typeOk = q.type === 'all' || x.type === q.type
  const fromOk = !q.from || toDate(x.date) >= new Date(`${q.from}T00:00:00`)
  const toOk = !q.to || toDate(x.date) <= new Date(`${q.to}T23:59:59`)
  return codeOk && typeOk && fromOk && toOk
}))

const stats = computed(() => ({
  all: allInvoices.value.length,
  waiting: allInvoices.value.filter(i => i.statusClass === 'waiting').length,
  shipping: allInvoices.value.filter(i => i.statusClass === 'shipping').length,
  done: allInvoices.value.filter(i => i.statusClass === 'done').length,
  cancel: allInvoices.value.filter(i => i.statusClass === 'cancel').length
}))

function toDate(value) {
  const [day, month, year] = value.split('/').map(Number)
  return new Date(year, month - 1, day)
}
function search(filters) { query.value = filters }
function reset() { query.value = { code: '', from: '', to: '', type: 'all' } }
function view(invoice) { selected.value = invoice; showDetail.value = true }
function createInvoice() { showCreate.value = true }
function notify(message) {
  toast.value = message
  window.setTimeout(() => { toast.value = '' }, 2200)
}
function exportExcel() { notify('Đã chuẩn bị dữ liệu xuất Excel') }
</script>

<template>
  <AdminLayout>
<main class="content">
        <div class="page-heading">
          <div>
            <div class="breadcrumb"><span>Trang chủ</span><b>/</b><strong>Quản lý hóa đơn</strong></div>
            <div class="heading-row"><div class="heading-mark"></div><div><h1>Quản lý hóa đơn</h1><p>Theo dõi và quản lý toàn bộ hóa đơn bán hàng của SmashStep</p></div></div>
          </div>
          <div class="heading-actions"><button class="btn secondary" @click="exportExcel">⇩ <span>Xuất Excel</span></button><button class="btn primary" @click="createInvoice">＋ <span>Tạo hóa đơn</span></button></div>
        </div>

        <div class="summary-grid">
          <div class="summary-card"><div class="summary-icon blue">▣</div><div><span>Tổng hóa đơn</span><strong>{{ stats.all }}</strong></div><small>Trong hệ thống</small></div>
          <div class="summary-card"><div class="summary-icon orange">◷</div><div><span>Chờ xác nhận</span><strong>{{ stats.waiting }}</strong></div><small>Cần xử lý</small></div>
          <div class="summary-card"><div class="summary-icon cyan">➜</div><div><span>Đang giao</span><strong>{{ stats.shipping }}</strong></div><small>Đang vận chuyển</small></div>
          <div class="summary-card"><div class="summary-icon green">✓</div><div><span>Hoàn thành</span><strong>{{ stats.done }}</strong></div><small>Đã hoàn tất</small></div>
        </div>

        <InvoiceFilter @search="search" @reset="reset" @export="exportExcel" />
        <InvoiceTable :invoices="invoices" :stats="stats" @view="view" />
      </main>

      <div v-if="showDetail" class="modal-backdrop" @click.self="showDetail=false">
        <div class="detail-modal">
          <button class="modal-close" @click="showDetail=false">×</button>
          <div class="modal-title"><div class="modal-icon">▣</div><div><div class="modal-kicker">CHI TIẾT HÓA ĐƠN</div><h2>{{ selected?.code }}</h2><p>{{ selected?.date }} · {{ selected?.status }}</p></div></div>
          <div class="customer-card"><div class="mini-avatar">{{ selected?.customer?.charAt(0) }}</div><div><span>Khách hàng</span><strong>{{ selected?.customer }}</strong><small>{{ selected?.phone }} · {{ selected?.address }}</small></div></div>
          <div class="detail-grid"><div><span>Nhân viên</span><b>{{ selected?.employee }}</b></div><div><span>Loại đơn</span><b>{{ selected?.type === 'online' ? 'Online' : selected?.type === 'delivery' ? 'Giao hàng' : 'Tại quầy' }}</b></div><div><span>Thanh toán</span><b>{{ selected?.payment }}</b></div><div><span>Trạng thái</span><b>{{ selected?.status }}</b></div></div>
          <div class="order-lines"><div class="line-head"><span>Sản phẩm</span><span>Thành tiền</span></div><div class="product-line"><div><strong>Giày thể thao SmashStep</strong><small>Phiên bản tiêu chuẩn · SL 1</small></div><b>{{ selected?.total?.toLocaleString('vi-VN') }}đ</b></div></div>
          <div class="total-row"><span>Tổng thanh toán</span><strong>{{ selected?.total?.toLocaleString('vi-VN') }}đ</strong></div>
          <button class="btn primary full" @click="showDetail=false">Đóng chi tiết</button>
        </div>
      </div>

      <div v-if="showCreate" class="modal-backdrop" @click.self="showCreate=false">
        <div class="create-modal"><button class="modal-close" @click="showCreate=false">×</button><div class="modal-title"><div class="modal-icon">＋</div><div><div class="modal-kicker">BÁN HÀNG</div><h2>Tạo hóa đơn mới</h2><p>Giao diện chuẩn bị cho nghiệp vụ bán hàng của SmashStep</p></div></div><div class="create-placeholder"><div>＋</div><strong>Chức năng tạo hóa đơn</strong><span>Phần này sẽ được nối với sản phẩm, khách hàng và thanh toán ở bước phát triển nghiệp vụ.</span></div><button class="btn primary full" @click="showCreate=false">Đóng</button></div>
      </div>

      <transition name="toast">
        <div v-if="toast" class="toast">✓ {{ toast }}</div>
      </transition>
  </AdminLayout>
</template>

<style scoped>.content{padding:24px 30px 34px;max-width:1500px;margin:auto}.page-heading{display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:17px}.breadcrumb{font-size:9px;color:#a1afb5;margin-bottom:8px}.breadcrumb b{padding:0 7px;color:#ccd5d9}.breadcrumb strong{color:#71828b;font-weight:600}.heading-row{display:flex;gap:11px;align-items:flex-start}.heading-mark{width:4px;height:38px;border-radius:5px;background:linear-gradient(#08b7d5,#079bbd);box-shadow:0 4px 12px rgba(8,183,213,.2)}.page-heading h1{margin:0;font-size:23px;letter-spacing:-.45px;color:#243740}.page-heading p{margin:5px 0 0;color:#94a2a8;font-size:10px}.heading-actions{display:flex;gap:9px}.btn{height:39px;border:0;border-radius:9px;padding:0 15px;font-size:10px;font-weight:700;cursor:pointer;display:inline-flex;align-items:center;justify-content:center;gap:7px;transition:.18s}.btn:hover{transform:translateY(-1px)}.btn.secondary{background:#fff;border:1px solid #dfe8eb;color:#60727c}.btn.primary{background:linear-gradient(100deg,#08b7d5,#09a9ca);color:#fff;box-shadow:0 7px 16px rgba(8,176,207,.18)}
.summary-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px;margin-bottom:15px}.summary-card{min-height:82px;background:#fff;border:1px solid #e7eef1;border-radius:13px;padding:13px 14px;display:grid;grid-template-columns:36px 1fr auto;align-items:center;gap:10px;box-shadow:0 5px 18px rgba(30,66,80,.035)}.summary-icon{width:36px;height:36px;border-radius:10px;display:grid;place-items:center;font-size:15px;font-weight:800}.summary-icon.blue{background:#e9f8fb;color:#08accb}.summary-icon.orange{background:#fff5e4;color:#e99c24}.summary-icon.cyan{background:#e9f8fc;color:#0ca6c5}.summary-icon.green{background:#e8faf2;color:#16a874}.summary-card span{display:block;color:#8b9ba2;font-size:9px}.summary-card strong{display:block;color:#273a43;font-size:18px;margin-top:3px}.summary-card small{font-size:8px;color:#a8b4b9;align-self:end}
.modal-backdrop{position:fixed;inset:0;background:rgba(19,39,48,.42);display:grid;place-items:center;z-index:50;padding:20px;backdrop-filter:blur(3px)}.detail-modal,.create-modal{width:min(610px,94vw);background:#fff;border-radius:18px;padding:25px;box-shadow:0 28px 80px rgba(20,46,58,.25);position:relative}.modal-close{position:absolute;right:15px;top:14px;width:31px;height:31px;border:1px solid #e4ecef;border-radius:50%;background:#fff;color:#819099;font-size:19px;cursor:pointer}.modal-title{display:flex;align-items:center;gap:12px;margin-bottom:18px}.modal-icon{width:42px;height:42px;border-radius:12px;background:#e8f9fc;color:#08accb;display:grid;place-items:center;font-size:18px;font-weight:800}.modal-kicker{font-size:8px;color:#08a9c8;font-weight:800;letter-spacing:1px}.modal-title h2{margin:3px 0 0;color:#273942;font-size:18px}.modal-title p{margin:4px 0 0;color:#9aa7ad;font-size:9px}.customer-card{display:flex;gap:11px;padding:13px;border:1px solid #e7eef0;background:#f8fbfc;border-radius:12px;margin-bottom:13px}.mini-avatar{width:38px;height:38px;border-radius:10px;background:#102d39;color:#fff;display:grid;place-items:center;font-weight:800}.customer-card span,.customer-card small{display:block;color:#9aa8ae;font-size:8px}.customer-card strong{display:block;color:#334750;font-size:11px;margin:3px 0}.customer-card small{font-size:9px}.detail-grid{display:grid;grid-template-columns:1fr 1fr;gap:9px;margin-bottom:14px}.detail-grid>div{border:1px solid #e8eef0;border-radius:10px;padding:11px}.detail-grid span{display:block;color:#9aa8ae;font-size:8px;margin-bottom:5px}.detail-grid b{color:#465b65;font-size:10px}.order-lines{border:1px solid #e7edef;border-radius:11px;overflow:hidden}.line-head{display:flex;justify-content:space-between;padding:9px 12px;background:#f6f9fa;color:#91a0a7;font-size:8px;font-weight:700}.product-line{display:flex;justify-content:space-between;align-items:center;padding:12px}.product-line strong{display:block;color:#3d515a;font-size:10px}.product-line small{display:block;color:#9daab0;font-size:8px;margin-top:4px}.product-line>b{font-size:10px;color:#344b55}.total-row{display:flex;justify-content:space-between;align-items:center;padding:15px 1px 0;color:#75868e;font-size:10px}.total-row strong{font-size:17px;color:#08a5c5}.btn.full{width:100%;margin-top:17px}.create-placeholder{border:1px dashed #b9dce4;border-radius:13px;background:#f7fcfd;padding:38px 25px;text-align:center}.create-placeholder>div{margin:auto;width:45px;height:45px;border-radius:13px;background:#e7f8fb;color:#08accb;display:grid;place-items:center;font-size:23px}.create-placeholder strong{display:block;margin-top:11px;color:#344951;font-size:12px}.create-placeholder span{display:block;max-width:400px;margin:6px auto 0;color:#98a7ad;font-size:9px;line-height:1.6}.toast{position:fixed;right:25px;bottom:24px;background:#102d39;color:#fff;padding:11px 15px;border-radius:10px;font-size:10px;box-shadow:0 12px 30px rgba(15,44,56,.22);z-index:80}.toast-enter-active,.toast-leave-active{transition:.2s}.toast-enter-from,.toast-leave-to{opacity:0;transform:translateY(8px)}
@media(max-width:1100px){.sidebar{width:205px;flex-basis:205px}.content{padding-left:20px;padding-right:20px}.summary-grid{grid-template-columns:1fr 1fr}.summary-card small{display:none}}
@media(max-width:760px){.sidebar{width:72px;flex-basis:72px}.brand img{width:56px}.sidebar-label,.menu a:not(.active){font-size:0}.menu a{justify-content:center;padding:0}.menu-icon{margin:0}.menu a.active{font-size:0}.sidebar-bottom{display:none}.topbar{padding:0 15px}.quick-search{width:180px}.account-info,.chevron{display:none}.content{padding:16px 12px}.page-heading{align-items:flex-start;gap:10px}.page-heading p{display:none}.heading-actions .secondary span{display:none}.btn{padding:0 11px}.summary-grid{grid-template-columns:1fr 1fr}.detail-grid{grid-template-columns:1fr}}
@media(max-width:520px){.quick-search{width:140px}.summary-grid{gap:8px}.summary-card{grid-template-columns:32px 1fr;padding:10px}.summary-icon{width:32px;height:32px}.summary-card strong{font-size:16px}.heading-actions{gap:5px}.heading-actions .btn{font-size:9px}.heading-mark{height:31px}.page-heading h1{font-size:19px}}
</style>