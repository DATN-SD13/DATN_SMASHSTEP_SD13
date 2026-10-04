<script setup>
import { reactive, ref, onMounted } from 'vue'
import ProductShell from '../components/ProductShell.vue'
import ProductPagination from '../components/ProductPagination.vue'
import VariantTable from '../components/VariantTable.vue'
import VariantEditModal from '../components/VariantEditModal.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { useConfirmation, statusConfirmation } from '../composables/useConfirmation'
import { variantService, cleanParams, errorMessage } from '../services/productService'
import { productAttributeService } from '../services/productAttributeService'
const options = ref({}), busy = ref(false), error = ref(''), success = ref(''), editing = ref(null)
const { confirmation, confirming, confirmError, askConfirmation, cancelConfirmation, confirmAction } = useConfirmation()
const filters = reactive({ keyword: '', colorId: '', sizeId: '', status: '' })
const page = ref({ content: [], number: 0, size: 10, totalElements: 0, totalPages: 0 })
let applied = {}
async function load(number = 0, size = page.value.size) {
  busy.value = true; error.value = ''
  try {
    const result = await variantService.list(cleanParams({ ...applied, page: number, size }))
    page.value = result
    if (number > 0 && !result.content.length && number >= result.totalPages)
      page.value = await variantService.list(cleanParams({ ...applied, page: Math.max(0, result.totalPages - 1), size }))
  } catch (e) { error.value = errorMessage(e) } finally { busy.value = false }
}
function search() { applied = { ...filters }; success.value = ''; load(0) }
function reset() { Object.assign(filters, { keyword: '', colorId: '', sizeId: '', status: '' }); search() }
function changeStatus(v) {
  if (busy.value || confirming.value) return
  const id = v.id, status = v.trangThai === 1 ? 0 : 1
  askConfirmation(statusConfirmation('biến thể', `${v.sku} - ${v.tenSanPham}`, v.trangThai), async () => {
    error.value = ''; success.value = ''
    await variantService.status(id, status)
    success.value = 'Thay đổi trạng thái biến thể thành công.'
    await load(page.value.number)
  })
}
function saved() { editing.value = null; success.value = 'Cập nhật biến thể thành công.'; load(page.value.number) }
async function initialize() {
  try { options.value = await productAttributeService.options() } catch (e) { error.value = errorMessage(e); return }
  await load()
}
onMounted(initialize)
</script>
<template>
  <ProductShell title="Biến thể sản phẩm" description="Quản lý SKU, giá bán, tồn kho và trạng thái theo màu sắc / kích thước." :error="error" :success="success">
    <template #actions><RouterLink to="/san-pham" class="p-btn primary"><i class="bi bi-plus-lg"></i> Chọn sản phẩm để thêm biến thể</RouterLink></template>
    <form class="p-card" @submit.prevent="search"><h2><i class="bi bi-funnel"></i> Bộ lọc biến thể</h2><div class="p-filter-grid"><label>Tìm kiếm<input v-model="filters.keyword" placeholder="Mã SP, SKU, mã chi tiết, tên sản phẩm..." /></label><label>Màu sắc<select v-model="filters.colorId"><option value="">Tất cả màu sắc</option><option v-for="a in options.colors || []" :key="a.id" :value="a.id">{{ a.ten }}</option></select></label><label>Kích thước<select v-model="filters.sizeId"><option value="">Tất cả kích thước</option><option v-for="a in options.sizes || []" :key="a.id" :value="a.id">{{ a.ten }}</option></select></label><label>Trạng thái<select v-model="filters.status"><option value="">Tất cả trạng thái</option><option :value="1">Hoạt động</option><option :value="0">Ngừng hoạt động</option></select></label></div><div class="p-filter-actions"><button type="button" class="p-btn" :disabled="busy" @click="reset">Đặt lại</button><button class="p-btn primary" :disabled="busy"><i class="bi bi-search"></i> Tìm kiếm / Lọc</button></div></form>
    <section class="p-card"><h2><i class="bi bi-layers"></i> Danh sách biến thể</h2><VariantTable :rows="page.content" :offset="page.number * page.size" :busy="busy" @edit="editing = $event" @status="changeStatus" /><ProductPagination :page="page" :busy="busy" @change="load($event)" @size="load(0, $event)" /></section>
    <VariantEditModal v-if="editing" :key="editing.id" :variant="editing" :options="options" @close="editing = null" @saved="saved" />
    <ConfirmModal v-bind="confirmation || {}" :show="!!confirmation" :loading="confirming" :error="confirmError" @confirm="confirmAction" @cancel="cancelConfirmation" />
  </ProductShell>
</template>
