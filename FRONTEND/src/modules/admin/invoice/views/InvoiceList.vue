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
    <main class="ss-page">
      <InvoiceFilter @search="search" @reset="reset" />
      <InvoiceTable :invoices="invoices" :stats="stats" @view="view" @export="exportExcel" />
    </main>

    <transition name="ss-toast">
      <div v-if="toast" class="ss-toast"><i class="bi bi-check-circle-fill"></i> {{ toast }}</div>
    </transition>
  </AdminLayout>
</template>
