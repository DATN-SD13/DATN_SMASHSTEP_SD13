<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductShell from '../components/ProductShell.vue'
import ProductFilter from '../components/ProductFilter.vue'
import ProductTable from '../components/ProductTable.vue'
import ProductPagination from '../components/ProductPagination.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { useConfirmation, statusConfirmation } from '../composables/useConfirmation'
import { productService, errorMessage } from '../services/productService'
import { productAttributeService, attributeTypes } from '../services/productAttributeService'

const route = useRoute(), router = useRouter()
const options = ref({}), codeBusy = ref(false)
const { confirmation, confirming, confirmError, askConfirmation, cancelConfirmation, confirmAction } = useConfirmation()
const optionsBusy = ref(false)
const optionsError = ref('')
const busy = ref(true)
const error = ref('')
const success = ref(typeof route.query.created === 'string' ? `Thêm sản phẩm ${route.query.created} thành công.` : '')
const page = ref({ content: [], number: 0, size: 10, totalElements: 0, totalPages: 0 })
let applied = {}
let loadVersion = 0

async function load(number = 0, size = page.value.size) {
  const version = ++loadVersion
  const params = { ...applied, page: number, size }
  busy.value = true
  error.value = ''
  try {
    let result = await productService.list(params)
    if (version !== loadVersion) return
    if (number > 0 && !result.content.length && number >= result.totalPages) {
      params.page = Math.max(0, result.totalPages - 1)
      result = await productService.list(params)
    }
    if (version === loadVersion) page.value = result
  } catch (e) {
    if (version !== loadVersion) return
    page.value = { content: [], number, size, totalElements: 0, totalPages: 0 }
    error.value = errorMessage(e)
  } finally {
    if (version === loadVersion) busy.value = false
  }
}

async function loadOptions() {
  optionsBusy.value = true
  optionsError.value = ''
  try {
    const keys = attributeTypes.filter(type => type.filter).map(type => type.key)
    options.value = await productAttributeService.options(keys)
  } catch (e) {
    optionsError.value = 'Không thể tải bộ lọc sản phẩm. ' + errorMessage(e)
  } finally {
    optionsBusy.value = false
  }
}

function search(filters) {
  applied = { ...filters }
  success.value = ''
  load(0)
}

async function openCreate() {
  if (codeBusy.value || confirming.value || confirmation.value) return
  codeBusy.value = true
  error.value = ''
  try {
    const code = await productService.nextCode()
    askConfirmation({ title: 'Tạo sản phẩm mới',
      message: 'Bạn có chắc muốn tạo một sản phẩm mới?\nMã này được hệ thống tự động tạo theo thứ tự hiện tại.',
      details: [{ label: 'Mã sản phẩm dự kiến', value: code }] }, () => router.push({ path: '/san-pham/them', query: { code } }))
  } catch (e) { error.value = errorMessage(e) }
  finally { codeBusy.value = false }
}

function changeStatus(product) {
  if (busy.value || confirming.value) return
  const id = product.id, status = product.trangThai === 1 ? 0 : 1
  askConfirmation(statusConfirmation('sản phẩm', `${product.maSanPham} - ${product.tenSanPham}`, product.trangThai), async () => {
    error.value = ''; success.value = ''
    await productService.status(id, status)
    success.value = 'Thay đổi trạng thái sản phẩm thành công.'
    await load(page.value.number)
  })
}

onMounted(() => {
  load()
  loadOptions()
})
onBeforeUnmount(() => { loadVersion++ })
</script>

<template>
  <ProductShell title="Danh sách sản phẩm" description="Quản lý sản phẩm, thuộc tính và tồn kho theo biến thể." :error="error || optionsError" :success="success">
    <template #actions><RouterLink class="p-btn" to="/bien-the-san-pham"><i class="bi bi-layers"></i> Biến thể</RouterLink><RouterLink class="p-btn" to="/thuoc-tinh"><i class="bi bi-sliders"></i> Thuộc tính</RouterLink><button type="button" class="p-btn primary" :disabled="codeBusy || confirming" @click="openCreate"><i class="bi bi-plus-lg"></i>{{ codeBusy ? 'Đang lấy mã...' : 'Thêm sản phẩm' }}</button></template>
    <ProductFilter :options="options" :busy="busy || confirming" :options-busy="optionsBusy" @search="search" />
    <div v-if="optionsError" class="p-filter-actions">
      <button class="p-btn" :disabled="optionsBusy" @click="loadOptions">Tải lại bộ lọc</button>
    </div>
    <section class="p-card" :aria-busy="busy">
      <h2><i class="bi bi-box-seam"></i> Danh sách sản phẩm</h2>
      <div v-if="error" class="p-empty">
        Không thể tải danh sách sản phẩm.
        <button class="p-btn" :disabled="busy" @click="load(page.number, page.size)">Thử lại</button>
      </div>
      <template v-else>
        <ProductTable :rows="page.content" :offset="page.number * page.size" :busy="busy || confirming" @status="changeStatus" />
        <ProductPagination :page="page" :busy="busy || confirming" @change="load($event)" @size="load(0, $event)" />
      </template>
    </section>
    <ConfirmModal v-bind="confirmation || {}" :show="!!confirmation" :loading="confirming" :error="confirmError" @confirm="confirmAction" @cancel="cancelConfirmation" />
  </ProductShell>
</template>
