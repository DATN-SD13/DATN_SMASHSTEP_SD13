<script setup>
import { computed, ref } from 'vue'

const props = defineProps({
  invoices: { type: Array, default: () => [] },
  stats: { type: Object, default: () => ({}) }
})
const emit = defineEmits(['view', 'export'])
const activeTab = ref('all')

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
function matchesTab(invoice, tab) {
  const s = `${invoice.status || ''}`.toLowerCase()
  if (tab === 'all') return true
  if (tab === 'waiting') return invoice.statusClass === 'waiting' || s.includes('chờ xác nhận')
  if (tab === 'confirmed') return invoice.statusClass === 'confirmed' || s.includes('đã xác nhận')
  if (tab === 'ready') return invoice.statusClass === 'ready' || s.includes('chờ giao')
  if (tab === 'shipping') return invoice.statusClass === 'shipping' || s.includes('đang giao')
  if (tab === 'delivered') return invoice.statusClass === 'delivered' || s.includes('đã giao hàng')
  if (tab === 'done') return invoice.statusClass === 'done' || s.includes('hoàn thành')
  if (tab === 'cancel') return invoice.statusClass === 'cancel' || s.includes('đã hủy')
  if (tab === 'refund') return invoice.statusClass === 'refund' || s.includes('hoàn tiền') || s.includes('hoàn phí')
  return true
}
const tabs = computed(() => tabDefs.map(([key, label]) => [
  key, label, key === 'all' ? props.invoices.length : props.invoices.filter(i => matchesTab(i, key)).length
]))
const visible = computed(() => props.invoices.filter(i => matchesTab(i, activeTab.value)))
function typeName(type) {
  return type === 'online' ? 'Trực tuyến' : type === 'delivery' ? 'Giao hàng' : 'Tại quầy'
}
function money(value) { return `${Number(value || 0).toLocaleString('vi-VN')} đ` }

const tone = (c) => ({ done: 'success', cancel: 'danger', refund: 'danger', waiting: 'warn' }[c] || '')
const typeTone = (t) => (t === 'delivery' ? 'warn' : t === 'store' ? 'gray' : '')
</script>

<template>
  <section class="ss-card">
    <div class="ss-head">
      <h2>Danh sách hóa đơn</h2>
      <span class="ss-spacer"></span>
      <span class="ss-count">{{ visible.length }} bản ghi hiển thị.</span>
      <button class="ss-btn sm" type="button" @click="emit('export')"><i class="bi bi-file-earmark-excel"></i> Xuất Excel</button>
    </div>

    <div class="tabs">
      <button v-for="tab in tabs" :key="tab[0]" type="button" :class="{ active: activeTab === tab[0] }" @click="activeTab = tab[0]">
        {{ tab[1] }} <b v-if="tab[2]">{{ tab[2] }}</b>
      </button>
    </div>

    <div class="ss-table-wrap">
      <table class="ss-table">
        <thead>
          <tr>
            <th class="w-stt c">STT</th><th>Mã HĐ</th><th>Mã NV</th><th>Tên KH</th><th>SĐT KH</th>
            <th class="r">Tổng tiền TT</th><th>Loại đơn</th><th>Ngày tạo</th><th>Trạng thái</th><th>Hành động</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(invoice, index) in visible" :key="invoice.code">
            <td class="c">{{ index + 1 }}</td>
            <td><span class="ss-code">{{ invoice.code }}</span></td>
            <td>{{ invoice.employeeCode || 'NV001' }}</td>
            <td>{{ invoice.customer }}</td>
            <td class="nowrap">{{ invoice.phone }}</td>
            <td class="r nowrap"><span class="ss-strong">{{ money(invoice.total) }}</span></td>
            <td><span class="ss-pill" :class="typeTone(invoice.type)">{{ typeName(invoice.type) }}</span></td>
            <td class="nowrap">{{ invoice.date }}</td>
            <td><span class="ss-pill dot" :class="tone(invoice.statusClass)">{{ invoice.status }}</span></td>
            <td><div class="ss-row-actions"><button class="ss-icon-btn" title="Xem chi tiết" aria-label="Xem chi tiết" @click="emit('view', invoice)"><i class="bi bi-eye"></i></button></div></td>
          </tr>
          <tr v-if="!visible.length">
            <td colspan="10" class="ss-empty"><i class="bi bi-receipt"></i>Không tìm thấy hóa đơn. Thử thay đổi bộ lọc hoặc trạng thái.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="ss-foot">
      <span class="ss-foot-info">Hiển thị {{ visible.length ? 1 : 0 }}–{{ visible.length }} trong {{ props.invoices.length }} hóa đơn</span>
      <div class="ss-pages"><button disabled><i class="bi bi-chevron-left"></i></button><button class="active">1</button><button disabled><i class="bi bi-chevron-right"></i></button></div>
    </div>
  </section>
</template>

<style scoped>
.tabs { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 2px; }
.tabs button {
  height: 32px; padding: 0 12px; flex: 0 0 auto;
  display: inline-flex; align-items: center; gap: 6px;
  border: 1px solid var(--ss-border); background: #fff; border-radius: 999px;
  font-size: 11.5px; font-weight: 600; color: var(--ss-muted); cursor: pointer; transition: all .15s;
}
.tabs button:hover { border-color: var(--ss-primary); color: var(--ss-primary); }
.tabs button.active { background: var(--ss-primary-soft); border-color: var(--ss-primary); color: var(--ss-primary); }
.tabs button b { min-width: 18px; height: 18px; padding: 0 5px; border-radius: 9px; background: var(--ss-surface); border: 1px solid var(--ss-border); font-size: 10px; display: grid; place-items: center; color: inherit; }
.tabs button.active b { background: #fff; border-color: #b9d5f7; }
</style>
