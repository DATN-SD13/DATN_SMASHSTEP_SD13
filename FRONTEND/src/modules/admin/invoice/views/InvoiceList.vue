<script setup>
import { computed, ref } from 'vue'
import InvoiceFilter from '../components/InvoiceFilter.vue'
import InvoiceTable from '../components/InvoiceTable.vue'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { useRouter } from 'vue-router'
import { invoiceData } from '../services/invoiceData'
const router = useRouter()
const query = ref({ code: '', from: '', to: '', type: 'all' })
const toast = ref('')

const allInvoices = ref(invoiceData)

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
function view(invoice) { router.push(`/hoa-don/${encodeURIComponent(invoice.code)}`) }
function notify(message) {
  toast.value = message
  window.setTimeout(() => { toast.value = '' }, 2200)
}
function exportExcel() { notify('Đã chuẩn bị dữ liệu xuất Excel') }
</script>

<template>
  <AdminLayout>
<main class="content container-fluid">
        <div class="page-heading">
          <div class="breadcrumb small"><span>Trang chủ</span><b>/</b><strong>Hóa đơn</strong></div>
          <h1 class="fw-bold">Quản Lý Hóa Đơn</h1>
        </div>

        <InvoiceFilter @search="search" @reset="reset" />
        <InvoiceTable :invoices="invoices" :stats="stats" @view="view" @export="exportExcel" />
      </main>

      <transition name="toast">
        <div v-if="toast" class="toast">✓ {{ toast }}</div>
      </transition>
  </AdminLayout>
</template>

<style scoped>

.content{padding:22px 28px 40px;max-width:1750px;margin:0 auto}.page-heading{margin-bottom:18px}.breadcrumb{font-size:11px;color:#58707c;margin-bottom:7px}.breadcrumb b{padding:0 9px;color:#91a3ab}.breadcrumb strong{color:#18323e;font-weight:800}.page-heading h1{margin:0;font-size:26px;letter-spacing:-.4px;color:#102c38;font-weight:900}.toast{position:fixed;right:25px;bottom:25px;background:#102f3c;color:#fff;border-radius:10px;padding:13px 18px;font-size:12px;box-shadow:0 10px 30px rgba(10,48,66,.2);z-index:1200}.toast-enter-active,.toast-leave-active{transition:.2s}.toast-enter-from,.toast-leave-to{opacity:0;transform:translateY(8px)}@media(max-width:850px){.content{padding:17px 15px 30px}.page-heading h1{font-size:22px}}

</style>
