<script setup>
import { reactive, watch } from 'vue'

const emit = defineEmits(['search', 'reset'])
const filters = reactive({ code: '', from: '', to: '', type: 'all' })

watch(filters, () => emit('search', { ...filters }), { deep: true })

function reset() {
  Object.assign(filters, { code: '', from: '', to: '', type: 'all' })
  emit('reset')
}
</script>

<template>
  <section class="ss-card">
    <div class="ss-head">
      <div class="ss-head-icon"><i class="bi bi-funnel"></i></div>
      <div><h2>Bộ lọc</h2><p>Tra cứu nhanh dữ liệu.</p></div>
    </div>

    <div class="filter-row">
      <div class="ss-field"><span class="ss-label">Mã hóa đơn</span>
        <div class="ss-search"><i class="bi bi-search"></i><input class="ss-input" v-model="filters.code" placeholder="Nhập mã hóa đơn..." /></div>
      </div>
      <div class="ss-field"><span class="ss-label">Từ ngày</span><input class="ss-input" v-model="filters.from" type="date" /></div>
      <div class="ss-field"><span class="ss-label">Đến ngày</span><input class="ss-input" v-model="filters.to" type="date" /></div>
      <div class="ss-field"><span class="ss-label">Loại đơn</span>
        <select class="ss-select" v-model="filters.type">
          <option value="all">Tất cả loại đơn</option>
          <option value="online">Trực tuyến</option>
          <option value="delivery">Giao hàng</option>
          <option value="store">Tại quầy</option>
        </select>
      </div>
    </div>

    <div class="ss-actions">
      <button class="ss-btn" type="button" @click="reset">Đặt lại bộ lọc</button>
    </div>
  </section>
</template>

<style scoped>
.filter-row { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
@media (max-width: 1000px) { .filter-row { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) { .filter-row { grid-template-columns: 1fr; } .ss-actions > * { flex: 1; } }
</style>
