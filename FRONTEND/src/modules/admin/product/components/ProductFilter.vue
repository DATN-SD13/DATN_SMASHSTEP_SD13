<script setup>
import { reactive } from 'vue'
import { attributeTypes } from '../services/productAttributeService'
defineProps({ options: { type: Object, default: () => ({}) }, busy: Boolean })
const emit = defineEmits(['search'])
const initial = () => ({ keyword: '', status: '', categoryId: '', brandId: '', materialId: '', styleId: '', collarId: '', originId: '' })
const filters = reactive(initial())
const types = attributeTypes.filter(t => t.filter)
function reset() { Object.assign(filters, initial()); emit('search', { ...filters }) }
</script>
<template>
  <form class="p-card" @submit.prevent="emit('search', { ...filters })">
    <h2><i class="bi bi-funnel"></i> Bộ lọc sản phẩm</h2>
    <div class="p-filter-grid">
      <label>Mã / tên sản phẩm<input v-model="filters.keyword" placeholder="Tìm kiếm sản phẩm..." /></label>
      <label v-for="type in types" :key="type.key">{{ type.label }}<select v-model="filters[type.filter]"><option value="">Tất cả</option><option v-for="a in options[type.key] || []" :key="a.id" :value="a.id">{{ a.ten }}{{ a.trangThai === 1 ? '' : ' (ngừng hoạt động)' }}</option></select></label>
      <label>Trạng thái<select v-model="filters.status"><option value="">Tất cả trạng thái</option><option :value="1">Hoạt động</option><option :value="0">Ngừng hoạt động</option></select></label>
    </div>
    <div class="p-filter-actions"><button type="button" class="p-btn" :disabled="busy" @click="reset"><i class="bi bi-arrow-counterclockwise"></i> Đặt lại</button><button class="p-btn primary" :disabled="busy"><i class="bi bi-search"></i> Tìm kiếm / Lọc</button></div>
  </form>
</template>

