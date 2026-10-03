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
            <option value="store">Tại quầy</option>
          </select>
      </label>
      <button class="reset-btn btn btn-outline-primary col-12 col-md-6 col-lg-3" type="button" @click="reset"><i class="bi bi-arrow-counterclockwise"></i> Đặt lại bộ lọc</button>
    </div>
    </div>
  </section>
</template>

<style scoped>
.filter-card{background:#fff;border-radius:14px;margin-bottom:18px;border:1px solid #e7eef4!important}
.filter-card .card-body{padding:22px}
.filter-card h2{font-size:15px;font-weight:700;color:#203744;margin:0 0 18px}
.field{min-width:0;display:block}
.field>span{display:block;font-size:12px;font-weight:600;color:#687b85;margin-bottom:8px}
.field .form-control,.field .form-select{min-height:42px;border-color:#dce6ed;border-radius:10px;font-size:13px;color:#40545e;box-shadow:none}
.field .form-control:focus,.field .form-select:focus{border-color:#1689cf;box-shadow:0 0 0 .2rem rgba(22,137,207,.12)}
.reset-btn{min-height:42px;align-self:end;border-radius:10px;font-size:12px;font-weight:600;white-space:nowrap;--bs-btn-color:#137fb7;--bs-btn-border-color:#b9d9ec;--bs-btn-hover-bg:#1689cf;--bs-btn-hover-border-color:#1689cf}
@media(max-width:650px){.filter-card .card-body{padding:16px}.reset-btn{width:100%}}
</style>
