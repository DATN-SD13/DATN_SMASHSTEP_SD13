<script setup>
import { reactive } from 'vue'
const emit = defineEmits(['search','reset','export'])
const filters = reactive({ code:'', from:'', to:'', type:'all' })
function submit(){ emit('search',{...filters}) }
function reset(){ Object.assign(filters,{code:'',from:'',to:'',type:'all'}); emit('reset') }
</script>
<template>
  <section class="filter-card">
    <div class="filter-heading"><div class="filter-title"><span class="filter-icon">⌕</span><div><strong>Bộ lọc tìm kiếm</strong><small>Tìm nhanh theo mã, thời gian và hình thức bán hàng</small></div></div><button class="clear-top" @click="reset">↻ Làm mới</button></div>
    <div class="filter-grid">
      <label class="search-field"><span>Mã hóa đơn</span><div class="control search-control"><b>⌕</b><input v-model="filters.code" placeholder="Nhập mã hóa đơn..." @keyup.enter="submit"></div></label>
      <label><span>Ngày bắt đầu</span><div class="control"><input v-model="filters.from" type="date"></div></label>
      <label><span>Ngày kết thúc</span><div class="control"><input v-model="filters.to" type="date"></div></label>
      <label><span>Loại đơn</span><div class="control"><select v-model="filters.type"><option value="all">Tất cả</option><option value="online">Online</option><option value="delivery">Giao hàng</option><option value="store">Tại quầy</option></select></div></label>
      <button class="reset" @click="reset">Đặt lại bộ lọc</button><button class="excel" @click="emit('export')">⇩ Xuất Excel</button>
    </div>
  </section>
</template>
<style scoped>
.filter-card{background:#fff;border:1px solid #e5edef;border-radius:14px;padding:16px 17px 17px;margin-bottom:15px;box-shadow:0 6px 20px rgba(30,64,78,.045)}.filter-heading{display:flex;justify-content:space-between;align-items:center;margin-bottom:14px}.filter-title{display:flex;align-items:center;gap:9px}.filter-icon{width:31px;height:31px;border-radius:9px;background:#e8f9fc;color:#06abc9;display:grid;place-items:center;font-size:17px}.filter-title strong{display:block;color:#34464f;font-size:12px}.filter-title small{display:block;color:#9aa8ae;font-size:8px;margin-top:3px}.clear-top{border:0;background:transparent;color:#7f9199;font-size:9px;cursor:pointer}.clear-top:hover{color:#06a9c8}.filter-grid{display:grid;grid-template-columns:1.4fr 1fr 1fr 1fr auto auto;gap:10px;align-items:end}.filter-grid label>span{display:block;font-size:8px;color:#73848d;margin-bottom:6px;font-weight:700}.control{height:39px;border:1px solid #e1e9ec;border-radius:9px;background:#fbfcfd;display:flex;align-items:center;padding:0 10px;transition:.15s}.control:focus-within{border-color:#8ad9e5;box-shadow:0 0 0 3px rgba(9,180,210,.07);background:#fff}.control input,.control select{width:100%;height:100%;border:0;outline:0;background:transparent;color:#52656f;font-size:10px}.control input::placeholder{color:#a7b3b8}.search-control b{color:#9aaab2;font-size:15px;margin-right:7px;font-weight:400}.reset,.excel{height:39px;border-radius:9px;padding:0 12px;white-space:nowrap;font-size:9px;cursor:pointer;font-weight:700}.reset{border:1px solid #dfe8eb;background:#fff;color:#71828b}.reset:hover{border-color:#bcdce2;color:#06a8c7}.excel{border:1px solid #102d39;background:#102d39;color:#fff;box-shadow:0 4px 10px rgba(16,45,57,.12)}.excel:hover{background:#173d4b}@media(max-width:1050px){.filter-grid{grid-template-columns:1fr 1fr 1fr}.reset,.excel{width:100%}}@media(max-width:650px){.filter-grid{grid-template-columns:1fr 1fr}.search-field{grid-column:1/-1}}
</style>
