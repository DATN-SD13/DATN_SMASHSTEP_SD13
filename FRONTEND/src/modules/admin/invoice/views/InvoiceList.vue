<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import InvoiceFilter from '../components/InvoiceFilter.vue'
import InvoiceTable from '../components/InvoiceTable.vue'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { useRouter } from 'vue-router'
import { invoiceService } from '../services/invoiceService'

const router = useRouter()
const query = ref({ code: '', from: '', to: '', type: 'all' })
const invoices = ref([])
const loading = ref(false)
const loadError = ref('')
const currentPage = ref(1)
const pageSize = 10
const totalPages = ref(1)
const totalElements = ref(0)
const activeStatus = ref('all')
const exporting = ref(false)
const exportToast = ref({ visible: false, type: 'success', message: '' })
let exportToastTimer = null
let loadVersion = 0
const stats = ref({ all: 0, waiting: 0, confirmed: 0, ready: 0, shipping: 0, delivered: 0, done: 0, cancel: 0, refund: 0, pending: 0 })

const statusCodes = {
  waiting: 0,
  confirmed: 1,
  ready: 2,
  shipping: 3,
  delivered: 4,
  done: 5,
  cancel: 6,
  refund: 7,
  pending: 8
}

function normalizeType(value) {
  if (!value) return ''
  const v = String(value).toLowerCase()
  if (v === 'online' || v.includes('trực tuyến')) return 'online'
  if (v === 'store' || v.includes('tại quầy')) return 'store'
  return v
}

function formatDate(value) {
  if (!value) return '—'
  return String(value)
}

function mapInvoice(item) {
  return {
    id: item?.id,
    code: item?.maHoaDon || '',
    employee: item?.tenNhanVien || 'Chưa cập nhật',
    employeeCode: item?.maNhanVien || '—',
    customer: item?.tenKhachHang || 'Khách lẻ',
    phone: item?.soDienThoai || '—',
    date: formatDate(item?.ngayTao),
    total: Number(item?.thanhTien ?? 0),
    status: item?.trangThai || 'Không xác định',
    statusClass: item?.lopTrangThai || 'unknown',
    statusCode: Number(item?.maTrangThai),
    type: normalizeType(item?.loaiHoaDon),
    payment: item?.phuongThucThanhToan || 'Chưa cập nhật'
  }
}

function buildParams(page = currentPage.value, status = activeStatus.value, size = pageSize) {
  const params = {
    page,
    size
  }
  if (query.value.code.trim()) params.ma = query.value.code.trim()
  if (query.value.from) params.tuNgay = query.value.from
  if (query.value.to) params.denNgay = query.value.to
  if (status !== 'all') params.trangThai = statusCodes[status]
  if (query.value.type !== 'all') params.loaiDon = { store: 0, online: 1 }[query.value.type]
  return params
}

async function fetchPage(params) {
  const response = await invoiceService.getAll(params)
  if (!response?.success || !response?.data) {
    throw new Error(response?.message || 'Không thể tải danh sách hóa đơn')
  }
  return response.data
}

async function fetchStats(base) {
  const { page, size, ...params } = base
  const response = await invoiceService.getSummary(params)
  if (!response?.success || !response?.data) throw new Error(response?.message || 'Không thể tải thống kê hóa đơn.')
  const next = { all: 0, waiting: 0, confirmed: 0, ready: 0, shipping: 0, delivered: 0, done: 0, cancel: 0, refund: 0, pending: 0 }
  for (const item of response.data.theoTrangThai || []) {
    if (Object.hasOwn(next, item.khoa)) next[item.khoa] = Number(item.soLuong || 0)
  }
  next.all = Number(response.data.tongHoaDon || 0)
  return next
}

async function loadInvoices(page = 1, refreshStats = false) {
  const version = ++loadVersion
  const params = buildParams(page)
  const statsParams = buildParams(1, 'all')
  loading.value = true
  loadError.value = ''
  try {
    const data = await fetchPage(params)
    if (version !== loadVersion) return
    invoices.value = (Array.isArray(data.content) ? data.content : []).map(mapInvoice)
    currentPage.value = Number(data.page || page)
    totalPages.value = Math.max(1, Number(data.totalPages || 1))
    totalElements.value = Number(data.totalElements || 0)
    if (refreshStats) {
      try {
        const next = await fetchStats(statsParams)
        if (version === loadVersion) stats.value = next
      } catch (error) {
        if (version === loadVersion) showExportToast('error', error?.response?.data?.message || 'Không thể tải số lượng theo trạng thái.')
      }
    }
  } catch (error) {
    if (version !== loadVersion) return
    invoices.value = []
    totalPages.value = 1
    totalElements.value = 0
    loadError.value = error?.response?.data?.message || error?.message || 'Không thể tải danh sách hóa đơn.'
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

async function search(filters) {
  query.value = { ...filters }
  currentPage.value = 1
  await loadInvoices(1, true)
}

async function reset() {
  query.value = { code: '', from: '', to: '', type: 'all' }
  activeStatus.value = 'all'
  currentPage.value = 1
  await loadInvoices(1, true)
}

async function changeStatus(status) {
  activeStatus.value = status
  currentPage.value = 1
  await loadInvoices(1, true)
}

async function changePage(page) {
  if (page === currentPage.value || page < 1 || page > totalPages.value) return
  await loadInvoices(page, false)
}

function view(invoice) { router.push(`/hoa-don/${encodeURIComponent(invoice.code)}`) }
function escapeExcelHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function showExportToast(type, message) {
  exportToast.value = { visible: true, type, message }
  if (exportToastTimer) window.clearTimeout(exportToastTimer)
  exportToastTimer = window.setTimeout(() => { exportToast.value.visible = false }, 3500)
}

function downloadExcelFile(rows) {
  const headers = [
    'STT', 'Mã hóa đơn', 'Mã nhân viên', 'Tên khách hàng',
    'Số điện thoại khách hàng', 'Tổng tiền thanh toán', 'Loại đơn',
    'Ngày tạo', 'Trạng thái'
  ]
  const body = rows.map((item, index) => [
    index + 1,
    item.code,
    item.employeeCode,
    item.customer,
    item.phone,
    Number(item.total || 0).toLocaleString('vi-VN') + ' đ',
    typeNameForExport(item.type),
    item.date,
    item.status
  ])
  const html = `\ufeff<html><head><meta charset="UTF-8"></head><body><table border="1"><thead><tr>${headers.map(h => `<th>${escapeExcelHtml(h)}</th>`).join('')}</tr></thead><tbody>${body.map(row => `<tr>${row.map(cell => `<td>${escapeExcelHtml(cell)}</td>`).join('')}</tr>`).join('')}</tbody></table></body></html>`
  const blob = new Blob([html], { type: 'application/vnd.ms-excel;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  const stamp = new Date().toISOString().slice(0, 10)
  link.href = url
  link.download = `hoa-don-${stamp}.xls`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

function typeNameForExport(type) {
  return type === 'online' ? 'Trực tuyến' : type === 'store' ? 'Tại quầy' : 'Chưa xác định'
}

async function exportExcel() {
  if (exporting.value) return
  exporting.value = true
  try {
    const params = buildParams(1, activeStatus.value, 100)
    const first = await invoiceService.getAll(params)
    if (!first?.success || !first?.data) throw new Error(first?.message || 'Không thể lấy dữ liệu để xuất Excel')

    const data = first.data
    const all = (data.content || []).map(mapInvoice)
    const totalPagesForExport = Math.max(1, Number(data.totalPages || 1))

    for (let page = 2; page <= totalPagesForExport; page += 1) {
      const response = await invoiceService.getAll({ ...params, page })
      if (!response?.success || !response?.data) throw new Error(response?.message || 'Không thể lấy đủ dữ liệu để xuất Excel')
      all.push(...(response.data.content || []).map(mapInvoice))
    }

    downloadExcelFile(all)
    showExportToast('success', `Đã xuất ${all.length} hóa đơn ra Excel.`)
  } catch (error) {
    showExportToast('error', error?.response?.data?.message || error?.message || 'Xuất Excel thất bại.')
  } finally {
    exporting.value = false
  }
}

onMounted(() => loadInvoices(1, true))
onUnmounted(() => {
  loadVersion += 1
  if (exportToastTimer) window.clearTimeout(exportToastTimer)
})
</script>
<template>
  <AdminLayout>
    <main class="content container-fluid">
      <div class="page-heading">
        <div class="breadcrumb small"><span>Trang chủ</span><b>/</b><strong>Hóa đơn</strong></div>
        <h1 class="fw-bold">Quản Lý Hóa Đơn</h1>
      </div>

      <InvoiceFilter @search="search" @reset="reset" />
      <InvoiceTable
        :invoices="invoices"
        :stats="stats"
        :loading="loading"
        :load-error="loadError"
        :page="currentPage"
        :page-size="pageSize"
        :total-pages="totalPages"
        :total-elements="totalElements"
        :active-status="activeStatus"
        @status-change="changeStatus"
        @page-change="changePage"
        @view="view"
        :exporting="exporting"
        @export="exportExcel"
      />
      <div v-if="exportToast.visible" class="export-toast" :class="exportToast.type" role="status">
        <i :class="exportToast.type === 'success' ? 'bi bi-check-circle-fill' : 'bi bi-exclamation-circle-fill'"></i>
        <span>{{ exportToast.message }}</span>
      </div>
    </main>
  </AdminLayout>
</template>

<style scoped>
.content{padding:24px 24px 36px;max-width:1700px;margin:0 auto}
.page-heading{margin-bottom:18px}
.breadcrumb{font-size:11px;color:#8c9ba5;margin-bottom:5px}.breadcrumb b{padding:0 9px;color:#c4d0d7}.breadcrumb strong{color:#536c79;font-weight:600}
.page-heading h1{margin:0;font-size:23px;letter-spacing:-.3px;color:#203744;font-weight:750}
.export-toast{position:fixed;right:24px;top:88px;z-index:1200;display:flex;align-items:center;gap:10px;min-width:280px;max-width:420px;padding:12px 15px;border:1px solid #e4edf2;border-radius:11px;background:#fff;box-shadow:0 12px 30px rgba(32,55,68,.16);font-size:12px;color:#506875}.export-toast i{font-size:17px}.export-toast.success i{color:#168b68}.export-toast.error i{color:#d14958}@media(max-width:850px){.content{padding:16px}.page-heading h1{font-size:20px}.export-toast{left:16px;right:16px;top:78px;min-width:0}}
</style>
