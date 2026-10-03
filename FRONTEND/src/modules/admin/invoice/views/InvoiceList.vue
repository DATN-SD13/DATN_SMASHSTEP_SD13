<script setup>
import { onMounted, ref, watch } from 'vue'
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
const stats = ref({ all: 0, waiting: 0, confirmed: 0, ready: 0, shipping: 0, delivered: 0, done: 0, cancel: 0, refund: 0 })

const statusCodes = {
  waiting: 0,
  confirmed: 1,
  ready: 2,
  shipping: 3,
  delivered: 4,
  done: 5,
  cancel: 6,
  refund: 7
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
  const date = new Date(`${value}T00:00:00`)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('vi-VN')
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

function buildParams(page = currentPage.value, status = activeStatus.value) {
  const params = {
    page,
    size: pageSize
  }
  if (query.value.code.trim()) params.ma = query.value.code.trim()
  if (query.value.from) params.tuNgay = query.value.from
  if (query.value.to) params.denNgay = query.value.to
  if (status !== 'all') params.trangThai = statusCodes[status]
  if (query.value.type !== 'all') params.loaiDon = query.value.type === 'online' ? 1 : 0
  return params
}

async function fetchPage(page = 1) {
  const response = await invoiceService.getAll(buildParams(page))
  if (!response?.success || !response?.data) {
    throw new Error(response?.message || 'Không thể tải danh sách hóa đơn')
  }
  const data = response.data
  invoices.value = (data.content || []).map(mapInvoice)
  currentPage.value = Number(data.page || page)
  totalPages.value = Math.max(1, Number(data.totalPages || 1))
  totalElements.value = Number(data.totalElements || 0)
}

async function fetchStats() {
  const base = buildParams(1, 'all')
  const keys = Object.keys(statusCodes)
  const results = await Promise.all(keys.map(async key => {
    const response = await invoiceService.getAll({ ...base, trangThai: statusCodes[key], page: 1, size: 1 })
    if (!response?.success || !response?.data) return [key, 0]
    return [key, Number(response.data.totalElements || 0)]
  }))
  const next = { all: 0, waiting: 0, confirmed: 0, ready: 0, shipping: 0, delivered: 0, done: 0, cancel: 0, refund: 0 }
  results.forEach(([key, value]) => { next[key] = value })
  const allResponse = await invoiceService.getAll({ ...base, page: 1, size: 1 })
  next.all = Number(allResponse?.data?.totalElements || 0)
  stats.value = next
}

async function loadInvoices(page = 1, refreshStats = false) {
  loading.value = true
  loadError.value = ''
  try {
    await fetchPage(page)
    if (refreshStats) await fetchStats()
  } catch (error) {
    invoices.value = []
    totalPages.value = 1
    totalElements.value = 0
    loadError.value = error?.response?.data?.message || error?.message || 'Không thể tải danh sách hóa đơn.'
  } finally {
    loading.value = false
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
  await loadInvoices(1, false)
}

async function changePage(page) {
  if (page === currentPage.value || page < 1 || page > totalPages.value) return
  await loadInvoices(page, false)
}

function view(invoice) { router.push(`/hoa-don/${encodeURIComponent(invoice.code)}`) }
function exportExcel() { /* Xuất Excel không thay đổi database nên không hiện thông báo */ }

watch(() => query.value.type, () => {
  // InvoiceFilter emits the complete filter object; the explicit search handler
  // performs the API request and resets pagination.
})

onMounted(() => loadInvoices(1, true))
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
        @export="exportExcel"
      />
    </main>
  </AdminLayout>
</template>

<style scoped>
.content{padding:24px 24px 36px;max-width:1700px;margin:0 auto}
.page-heading{margin-bottom:18px}
.breadcrumb{font-size:11px;color:#8c9ba5;margin-bottom:5px}.breadcrumb b{padding:0 9px;color:#c4d0d7}.breadcrumb strong{color:#536c79;font-weight:600}
.page-heading h1{margin:0;font-size:23px;letter-spacing:-.3px;color:#203744;font-weight:750}
@media(max-width:850px){.content{padding:16px}.page-heading h1{font-size:20px}}
</style>
