<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductShell from '../components/ProductShell.vue'
import ProductForm from '../components/ProductForm.vue'
import ProductImages from '../components/ProductImages.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { useConfirmation } from '../composables/useConfirmation'
import { productService, productSummary, errorMessage } from '../services/productService'
import { productAttributeService } from '../services/productAttributeService'
const route = useRoute(), router = useRouter()
const product = ref(null), options = ref({}), error = ref(''), images = ref([])
const { confirmation, confirming: saving, confirmError, askConfirmation, cancelConfirmation, confirmAction } = useConfirmation()
let loadVersion = 0
async function load() {
  const version = ++loadVersion
  error.value = ''; product.value = null
  try {
    const [detail, attributes] = await Promise.all([productService.get(route.params.id), productAttributeService.options()])
    if (version !== loadVersion) return
    product.value = detail.product; options.value = attributes; images.value = detail.images
  } catch (e) { if (version === loadVersion) error.value = errorMessage(e) }
}
function save(data) {
  if (saving.value || !product.value) return
  const id = product.value.id, payload = { ...data }
  askConfirmation({ title: 'Xác nhận cập nhật sản phẩm', message: `Bạn có chắc muốn lưu các thay đổi của sản phẩm ${payload.maSanPham} - ${payload.tenSanPham}?`,
    confirmText: 'Xác nhận cập nhật', details: productSummary(payload, options.value) }, async () => {
    error.value = ''
    await productService.update(id, payload)
    await router.push({ path: '/san-pham/' + id, query: { updated: '1' } })
  })
}
watch(() => route.params.id, load, { immediate: true })
async function refreshImages() {
  const id = product.value.id
  try {
    const detail = await productService.get(id)
    if (product.value?.id === id) images.value = detail.images
  } catch (e) { error.value = errorMessage(e) }
}
</script>
<template>
  <ProductShell title="Sửa sản phẩm" description="Cập nhật thông tin sản phẩm và giữ nguyên các biến thể hiện có." :error="error">
    <template #actions><RouterLink :to="'/san-pham/' + route.params.id" class="p-btn"><i class="bi bi-arrow-left"></i> Quay lại</RouterLink></template>
    <ProductForm v-if="product" :key="product.id" :initial="product" :options="options" :saving="saving" editing @save="save" />
    <div v-else class="p-card p-empty">{{ error ? 'Không tải được sản phẩm.' : 'Đang tải...' }} <button v-if="error" class="p-btn" @click="load">Thử lại</button></div>
    <ProductImages v-if="product" :key="product.id" :product-id="product.id" :product-code="product.maSanPham" :images="images" :loading="saving" @changed="refreshImages" />
    <ConfirmModal v-bind="confirmation || {}" :show="!!confirmation" :loading="saving" :error="confirmError" @confirm="confirmAction" @cancel="cancelConfirmation" />
  </ProductShell>
</template>
