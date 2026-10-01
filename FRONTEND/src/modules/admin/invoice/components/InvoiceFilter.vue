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
  <section class="filter-card card shadow-sm border-0">
    <div class="card-body">
    <h2>Bộ lọc</h2>
    <div class="filter-grid row g-3">
      <label class="field code-field col-12 col-lg-3">
        <span>Mã hóa đơn</span>
        <input class="form-control" v-model="filters.code" placeholder="Nhập mã hóa đơn..." />
      </label>
      <label class="field col-6 col-lg-2">
        <span>Từ ngày</span>
        <input class="form-control" v-model="filters.from" type="date" />
      </label>
      <label class="field col-6 col-lg-2">
        <span>Đến ngày</span>
        <input class="form-control" v-model="filters.to" type="date" />
      </label>
      <label class="field type-field col-12 col-md-6 col-lg-2">
        <span>Loại đơn</span>
        <select class="form-select" v-model="filters.type">
            <option value="all">Tất cả</option>
            <option value="online">Trực tuyến</option>
            <option value="delivery">Giao hàng</option>
            <option value="store">Tại quầy</option>
          </select>
      </label>
      <button class="reset-btn btn btn-outline-primary col-12 col-md-6 col-lg-3" type="button" @click="reset"><i class="bi bi-arrow-counterclockwise"></i> Đặt lại bộ lọc</button>
    </div>
    </div>
  </section>
</template>

<style scoped>

.filter-card{background:#fff;border:1px solid #d6e2e8!important;border-radius:16px;margin-bottom:18px;box-shadow:0 5px 18px rgba(21,54,70,.055)}
.filter-card .card-body{padding:22px 24px}.filter-card h2{font-size:16px;font-weight:850;color:#102c38;margin:0 0 18px}.field{min-width:0;display:block}.field>span{display:block;font-size:12px;font-weight:750;color:#203943;margin-bottom:8px}.field .form-control,.field .form-select{min-height:44px;border:1px solid #ccdbe2;border-radius:10px;font-size:12px;color:#142a34;background:#fff;box-shadow:none}.field .form-control::placeholder{color:#82959e}.field .form-control:focus,.field .form-select:focus{border-color:#1687bb;box-shadow:0 0 0 3px rgba(22,135,187,.11)}.reset-btn{min-height:44px;align-self:end;border-radius:10px;font-size:12px;font-weight:800;white-space:nowrap;--bs-btn-color:#086e9d;--bs-btn-border-color:#a8cfdf;--bs-btn-hover-bg:#087fb8;--bs-btn-hover-border-color:#087fb8}.filter-card .card-body>.row{row-gap:15px}@media(max-width:650px){.filter-card .card-body{padding:17px}.reset-btn{width:100%}}

</style>
