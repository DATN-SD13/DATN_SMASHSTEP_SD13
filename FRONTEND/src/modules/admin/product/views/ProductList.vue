<script setup>
import { ref, onMounted } from 'vue'
import ProductShell from '../components/ProductShell.vue'
import ProductFilter from '../components/ProductFilter.vue'
import ProductTable from '../components/ProductTable.vue'
import ProductPagination from '../components/ProductPagination.vue'
import { productService, cleanParams, errorMessage } from '../services/productService'
import { productAttributeService } from '../services/productAttributeService'

const options = ref({}), busy = ref(false), error = ref(''), success = ref('')
const page = ref({ content: [], number: 0, size: 10, totalElements: 0, totalPages: 0 })
let applied = {}
async function load(number = 0, size = page.value.size) {
  busy.value = true; error.value = ''
  try {
    const result = await productService.list(cleanParams({ ...applied, page: number, size }))
    page.value = result
    if (number > 0 && !result.content.length && number >= result.totalPages)
      page.value = await productService.list(cleanParams({ ...applied, page: Math.max(0, result.totalPages - 1), size }))
  } catch (e) { error.value = errorMessage(e) } finally { busy.value = false }
}
function search(filters) { applied = filters; success.value = ''; load(0) }
async function changeStatus(product) {
  if (busy.value || !window.confirm(`${product.trangThai === 1 ? 'Ngừng hoạt động' : 'Bật hoạt động'} sản phẩm "${product.tenSanPham}"?`)) return
  busy.value = true; error.value = ''; success.value = ''
  try { await productService.status(product.id, product.trangThai === 1 ? 0 : 1); success.value = 'Đã cập nhật trạng thái sản phẩm.'; await load(page.value.number) }
  catch (e) { error.value = errorMessage(e) } finally { busy.value = false }
}
async function initialize() {
  try { options.value = await productAttributeService.options() } catch (e) { error.value = errorMessage(e); return }
  await load()
}
onMounted(initialize)
</script>
<template>
  <ProductShell title="Danh sách sản phẩm" description="Quản lý sản phẩm, thuộc tính và tồn kho theo biến thể." :error="error" :success="success">
    <template #actions><RouterLink class="p-btn primary" to="/san-pham/them"><i class="bi bi-plus-lg"></i> Thêm sản phẩm</RouterLink></template>
    <ProductFilter :options="options" :busy="busy" @search="search" />
    <section class="p-card"><h2><i class="bi bi-box-seam"></i> Danh sách sản phẩm</h2><ProductTable :rows="page.content" :offset="page.number * page.size" :busy="busy" @status="changeStatus" /><ProductPagination :page="page" :busy="busy" @change="load($event)" @size="load(0, $event)" /></section>
  </ProductShell>
</template>

