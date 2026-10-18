<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import api from '../../../../utils/api'

const from = ref('')
const to = ref('')
const summary = ref(null)
const loading = ref(false)
const error = ref('')
let loadVersion = 0
const money = value => `${Number(value || 0).toLocaleString('vi-VN')} đ`

async function load() {
  const version = ++loadVersion
  error.value = ''
  if (from.value && to.value && from.value > to.value) {
    loading.value = false
    error.value = 'Từ ngày không được sau đến ngày.'
    return
  }
  loading.value = true
  try {
    const params = {}
    if (from.value) params.tuNgay = from.value
    if (to.value) params.denNgay = to.value
    const response = await api.get('/thong-ke', { params })
    if (!response.data?.success || !response.data?.data) throw new Error(response.data?.message || 'Không thể tải thống kê.')
    if (version === loadVersion) summary.value = response.data.data
  } catch (failure) {
    if (version === loadVersion) {
      summary.value = null
      error.value = failure?.response?.data?.message || failure?.message || 'Không thể tải thống kê.'
    }
  } finally {
    if (version === loadVersion) loading.value = false
  }
}
function reset() {
  from.value = ''
  to.value = ''
  load()
}
onMounted(load)
onUnmounted(() => { loadVersion += 1 })
</script>

<template>
  <AdminLayout>
    <main class="statistics-page">
      <h1>Thống kê hóa đơn</h1>
      <form class="card filter-card" @submit.prevent="load">
        <label>Từ ngày tạo <input v-model="from" type="date" class="form-control" /></label>
        <label>Đến ngày tạo <input v-model="to" type="date" class="form-control" /></label>
        <button class="btn btn-primary" type="submit" :disabled="loading">{{ loading ? 'Đang tải...' : 'Áp dụng' }}</button>
        <button class="btn btn-outline-secondary" type="button" :disabled="loading" @click="reset">Đặt lại</button>
      </form>
      <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
      <div v-if="loading" class="alert alert-info" role="status">Đang tải dữ liệu hóa đơn...</div>
      <template v-if="summary && !loading">
        <section class="summary-grid">
          <div class="card summary-card"><span>Tổng hóa đơn</span><strong>{{ summary.tongHoaDon }}</strong></div>
          <div class="card summary-card"><span>Hoàn thành</span><strong>{{ summary.hoaDonHoanThanh }}</strong></div>
          <div class="card summary-card"><span>Đang xử lý</span><strong>{{ summary.hoaDonDangXuLy }}</strong></div>
          <div class="card summary-card"><span>Đã hủy</span><strong>{{ summary.hoaDonDaHuy }}</strong></div>
          <div class="card summary-card revenue"><span>Doanh thu đã thanh toán</span><strong>{{ money(summary.doanhThuDaThanhToan) }}</strong><small>{{ summary.hoaDonDaThanhToan }} hóa đơn hoàn thành và có ngày thanh toán</small></div>
        </section>
        <p class="note">Thống kê theo ngày tạo hóa đơn. Doanh thu chỉ tính hóa đơn hoàn thành đã thanh toán; loại trừ hóa đơn hủy, hoàn tiền và đang xử lý.</p>
        <section class="breakdown-grid">
          <div class="card breakdown"><h2>Theo trạng thái</h2><table class="table"><thead><tr><th>Trạng thái</th><th class="text-end">Số lượng</th></tr></thead><tbody><tr v-for="item in summary.theoTrangThai || []" :key="item.khoa"><td>{{ item.ten }}</td><td class="text-end">{{ item.soLuong }}</td></tr></tbody></table></div>
          <div class="card breakdown"><h2>Theo loại đơn</h2><table class="table"><thead><tr><th>Loại đơn</th><th class="text-end">Số lượng</th></tr></thead><tbody><tr v-for="item in summary.theoLoaiDon || []" :key="item.khoa"><td>{{ item.ten }}</td><td class="text-end">{{ item.soLuong }}</td></tr></tbody></table><RouterLink to="/hoa-don">Xem danh sách hóa đơn</RouterLink></div>
        </section>
      </template>
    </main>
  </AdminLayout>
</template>

<style scoped>
.statistics-page{padding:24px;max-width:1700px;margin:auto;color:#203744}.statistics-page h1{font-size:24px;margin-bottom:20px}.filter-card{display:flex;flex-direction:row;align-items:end;gap:14px;padding:20px;margin-bottom:18px;flex-wrap:wrap}.filter-card label{font-size:13px;display:grid;gap:7px}.summary-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:16px}.summary-card{padding:20px;gap:10px}.summary-card span,.summary-card small{color:#687b85;font-size:13px}.summary-card strong{font-size:28px;color:#1689cf}.revenue{grid-column:1 / -1}.note{font-size:13px;color:#687b85;margin:16px 0}.breakdown-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}.breakdown{padding:20px}.breakdown h2{font-size:17px;margin-bottom:16px}.table{font-size:13px}@media(max-width:800px){.summary-grid{grid-template-columns:1fr 1fr}.breakdown-grid{grid-template-columns:1fr}.statistics-page{padding:16px}}@media(max-width:420px){.summary-grid{grid-template-columns:1fr}.revenue strong{font-size:23px}}
</style>
