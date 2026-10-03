<script setup>
import { computed } from 'vue'

const props = defineProps({
  invoices: { type: Array, default: () => [] },
  stats: { type: Object, default: () => ({}) },
  loading: { type: Boolean, default: false },
  loadError: { type: String, default: '' },
  page: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  totalPages: { type: Number, default: 1 },
  totalElements: { type: Number, default: 0 },
  activeStatus: { type: String, default: 'all' }
})
const emit = defineEmits(['view', 'export', 'page-change', 'status-change'])

const tabDefs = [
  ['all', 'Tất cả'],
  ['waiting', 'Chờ xác nhận'],
  ['confirmed', 'Đã xác nhận'],
  ['ready', 'Chờ giao hàng'],
  ['shipping', 'Đang giao hàng'],
  ['delivered', 'Đã giao hàng'],
  ['done', 'Đã hoàn thành'],
  ['cancel', 'Đã hủy'],
  ['refund', 'Hoàn tiền']
]
const tabs = computed(() => tabDefs.map(([key, label]) => [key, label, Number(props.stats[key] || 0)]))
const pageStart = computed(() => props.totalElements ? (props.page - 1) * props.pageSize + 1 : 0)
const pageEnd = computed(() => Math.min(props.page * props.pageSize, props.totalElements))
const pageNumbers = computed(() => {
  const total = Math.max(1, props.totalPages)
  const current = props.page
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  if (current <= 4) return [1, 2, 3, 4, 5, '...', total]
  if (current >= total - 3) return [1, '...', total - 4, total - 3, total - 2, total - 1, total]
  return [1, '...', current - 1, current, current + 1, '...', total]
})

function typeName(type) {
  return type === 'online' ? 'Trực tuyến' : type === 'store' ? 'Tại quầy' : 'Chưa xác định'
}
function money(value) { return `${Number(value || 0).toLocaleString('vi-VN')} đ` }
</script>

<template>
  <section class="table-card card shadow-sm border-0">
    <div class="table-head">
      <div class="list-heading">
        <h2>Danh sách hóa đơn</h2>
        <button class="export-btn btn btn-outline-primary btn-sm" type="button" @click="emit('export')"><i class="bi bi-box-arrow-up"></i> Xuất Excel</button>
      </div>
      <div class="status-tabs">
        <button v-for="tab in tabs" :key="tab[0]" type="button" :class="{ active: activeStatus === tab[0] }" @click="emit('status-change', tab[0])">
          {{ tab[1] }} <b v-if="tab[2]">{{ tab[2] }}</b>
        </button>
      </div>
    </div>

    <div class="table-wrap table-responsive">
      <table class="table table-hover align-middle mb-0">
        <thead>
          <tr>
            <th class="stt">STT</th><th>Mã HĐ</th><th>Mã NV</th><th>Tên KH</th><th>SĐT KH</th>
            <th>Tổng tiền TT</th><th>Loại đơn</th><th>Ngày tạo</th><th>Trạng thái</th><th class="action">Hành động</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(invoice, index) in invoices" :key="invoice.code">
            <td class="stt">{{ (page - 1) * pageSize + index + 1 }}</td>
            <td><strong class="code">{{ invoice.code }}</strong></td>
            <td>{{ invoice.employeeCode || 'NV001' }}</td>
            <td class="customer">{{ invoice.customer }}</td>
            <td>{{ invoice.phone }}</td>
            <td><strong class="money">{{ money(invoice.total) }}</strong></td>
            <td><span class="type-tag" :class="invoice.type">{{ typeName(invoice.type) }}</span></td>
            <td>{{ invoice.date }}</td>
            <td><span :class="['status', invoice.statusClass]">{{ invoice.status }}</span></td>
            <td class="action"><button class="view" title="Xem chi tiết" aria-label="Xem chi tiết" @click="emit('view', invoice)"><i class="bi bi-eye"></i></button></td>
          </tr>
          <tr v-if="props.loading">
            <td colspan="10" class="empty"><i class="bi bi-arrow-repeat"></i><strong>Đang tải danh sách hóa đơn...</strong></td>
          </tr>
          <tr v-else-if="props.loadError">
            <td colspan="10" class="empty"><i class="bi bi-exclamation-circle"></i><strong>{{ props.loadError }}</strong><span>Vui lòng tải lại trang.</span></td>
          </tr>
          <tr v-else-if="!props.invoices.length">
            <td colspan="10" class="empty"><i class="bi bi-receipt"></i><strong>Không tìm thấy hóa đơn</strong><span>Thử thay đổi bộ lọc hoặc trạng thái hóa đơn.</span></td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="table-footer">
      <span>Hiển thị <b>{{ pageStart }}–{{ pageEnd }}</b> trong {{ totalElements }} hóa đơn</span>
      <div class="pagination" aria-label="Phân trang danh sách hóa đơn">
        <button type="button" :disabled="page === 1" aria-label="Trang trước" @click="emit('page-change', page - 1)">‹</button>
        <template v-for="(item, index) in pageNumbers" :key="`${item}-${index}`">
          <span v-if="item === '...'" class="ellipsis">…</span>
          <button v-else type="button" :class="{ current: page === item }" :aria-current="page === item ? 'page' : undefined" @click="emit('page-change', item)">{{ item }}</button>
        </template>
        <button type="button" :disabled="page === totalPages" aria-label="Trang sau" @click="emit('page-change', page + 1)">›</button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.table-card{background:#fff;border:1px solid #e7eef4!important;border-radius:14px;overflow:hidden}
.table-head{padding:20px 22px 0}.list-heading{display:flex;align-items:center;justify-content:space-between;gap:12px}.list-heading h2{margin:0;color:#203744;font-size:15px;font-weight:700}
.export-btn{height:36px;padding:0 15px;border-radius:9px;font-size:12px;font-weight:600;display:flex;align-items:center;gap:7px;--bs-btn-color:#137fb7;--bs-btn-border-color:#b9d9ec;--bs-btn-hover-bg:#1689cf;--bs-btn-hover-border-color:#1689cf}
.status-tabs{display:flex;gap:8px;overflow-x:auto;padding:18px 0 15px}.status-tabs::-webkit-scrollbar{height:4px}.status-tabs::-webkit-scrollbar-thumb{background:#dce8ef;border-radius:5px}
.status-tabs button{flex:0 0 auto;border:1px solid #dfe6eb;background:#fff;color:#697d87;border-radius:9px;padding:8px 13px;white-space:nowrap;font-size:11px;font-weight:600;cursor:pointer;transition:.15s}.status-tabs button b{font-size:10px;margin-left:3px;font-weight:700}.status-tabs button:hover{border-color:#8fc5e7;color:#0878bd;background:#f5fbff}.status-tabs button.active{background:#1689cf;border-color:#1689cf;color:#fff;box-shadow:0 3px 9px rgba(22,137,207,.15)}
.table-wrap{overflow-x:auto}table{width:100%;min-width:1060px;border-collapse:separate;border-spacing:0}thead{background:#eaf4fa}th{height:46px;padding:0 13px!important;text-align:left;color:#536c7b!important;font-size:11px;font-weight:700!important;white-space:nowrap;border-bottom:1px solid #dce7ee!important;background:#eaf4fa!important}td{height:52px;padding:0 13px!important;color:#4f626d;font-size:11px;white-space:nowrap;border-bottom:1px solid #edf1f4}tbody tr{transition:background .12s}tbody tr:hover>*{background:#f5faff!important}
.stt{width:48px;text-align:center;color:#72838c}.code{color:#293e49;font-size:11px;font-weight:700}.customer{max-width:180px;overflow:hidden;text-overflow:ellipsis}.money{color:#1681c3;font-size:11px;font-weight:750}
.type-tag{display:inline-block;padding:6px 10px;border-radius:7px;font-size:10px;font-weight:700}.type-tag.online{background:#e6f4ff;color:#147bb8}.type-tag.store{background:#edf0ff;color:#5b68b4}
.status{display:inline-flex;align-items:center;border-radius:7px;padding:7px 10px;font-size:10px;font-weight:700;background:#e8f3ff;color:#1675bd}.status.done,.status.delivered{background:#e8f7ef;color:#168455}.status.shipping,.status.confirmed,.status.ready{background:#e6f4ff;color:#1678b8}.status.waiting{background:#fff5df;color:#a86a08}.status.cancel{background:#ffebed;color:#c64f5b}.status.refund{background:#f1eaff;color:#7b55b5}
.view{width:32px;height:32px;border:1px solid #dcebf4;background:#f3faff;color:#1689cf;border-radius:9px;cursor:pointer;font-size:14px;transition:.15s}.view:hover{background:#1689cf;color:#fff;border-color:#1689cf}.action{text-align:center}
.empty{text-align:center;height:150px!important;color:#8d9da6}.empty i{display:block;font-size:24px;color:#a9cde2;margin-bottom:8px}.empty strong,.empty span{display:block}.empty strong{color:#5c707b;font-size:13px}.empty span{margin-top:5px;font-size:11px}
.table-footer{min-height:58px;padding:0 22px;display:flex;align-items:center;justify-content:space-between;color:#8b9ba4;font-size:11px}.table-footer b{color:#526873}.pagination{display:flex;align-items:center;gap:5px}.pagination button{width:31px;height:31px;border:1px solid #dfe7ec;background:#fff;border-radius:8px;color:#6d818c;cursor:pointer}.pagination button.current{background:#1689cf;color:#fff;border-color:#1689cf}.pagination button:disabled{color:#c6d1d7;cursor:not-allowed}.pagination .ellipsis{width:20px;text-align:center;color:#8b9ba4;font-size:12px}
@media(max-width:700px){.table-head{padding:15px 14px 0}.status-tabs{gap:6px}.status-tabs button{font-size:10px;padding:8px 10px}.table-footer{padding:0 14px}}
</style>
