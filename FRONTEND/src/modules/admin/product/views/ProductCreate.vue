<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductShell from '../components/ProductShell.vue'
import ProductForm from '../components/ProductForm.vue'
import ProductImages from '../components/ProductImages.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { useConfirmation } from '../composables/useConfirmation'
import { productService, productSummary, errorMessage } from '../services/productService'
import { productAttributeService } from '../services/productAttributeService'
import { validateImageFile } from '../services/imageUtils'
const router = useRouter()
const route = useRoute()
const options = ref({}), ready = ref(false), error = ref(''), code = ref('')
const { confirmation, confirming: saving, confirmError, askConfirmation, cancelConfirmation, confirmAction } = useConfirmation()
const product = ref(null), success = ref('')
const images = ref([]), newImages = ref([]), progress = ref('')
async function load() {
  error.value = ''
  try {
    const passedCode = typeof route.query.code === 'string' && /^SP\d+$/i.test(route.query.code) && route.query.code.length <= 50 ? route.query.code : ''
    const [attributes, nextCode] = await Promise.all([productAttributeService.options(), passedCode || productService.nextCode()])
    options.value = attributes; code.value = nextCode; ready.value = true
  } catch (e) { error.value = errorMessage(e) }
}
function save(data) {
  if (saving.value || product.value) return
  const payload = { ...data }
  const selection = newImages.value.map(image => ({ ...image }))
  for (const image of selection) {
    const message = validateImageFile(image.file)
    if (message) { error.value = message; return }
  }
  askConfirmation({ title: 'Xác nhận thêm sản phẩm', message: 'Bạn có chắc muốn thêm sản phẩm này?',
    confirmText: 'Xác nhận thêm', details: [...productSummary(payload, options.value),
      { label: 'Ảnh đã chọn', value: selection.length },
      ...(selection.length ? [{ label: 'Tệp ảnh', value: selection.map(image => image.file.name).join('\n') }] : [])] }, async () => {
    error.value = ''
    if (!product.value) product.value = await productService.create(payload)
    await completeCreation(selection)
  })
}
async function completeCreation(selection) {
  const remaining = selection.filter(image => newImages.value.some(item => item.key === image.key))
  try {
    for (const [index, image] of remaining.entries()) {
      progress.value = `Đang tải ảnh ${index + 1}/${remaining.length}...`
      const uploaded = await productService.uploadImage(product.value.id, image)
      images.value = [...images.value.map(existing => uploaded.isAnhChinh ? { ...existing, isAnhChinh: false } : existing), uploaded]
      newImages.value = newImages.value.filter(item => item.key !== image.key)
    }
  } catch (e) {
    error.value = 'Sản phẩm đã được tạo nhưng có ảnh tải lên thất bại. Các ảnh còn lại được giữ để thử lại. ' + errorMessage(e)
    if (confirmation.value) confirmation.value = { ...confirmation.value,
      title: 'Xác nhận hoàn tất sản phẩm', confirmText: 'Thử lại ảnh',
      message: 'Sản phẩm đã được tạo. Chỉ tải những ảnh chưa được lưu.',
      details: [{ label: 'Sản phẩm', value: product.value.maSanPham },
        { label: 'Ảnh còn lại', value: newImages.value.length },
        { label: 'Tệp ảnh', value: newImages.value.map(image => image.file.name).join('\n') }] }
    throw e
  } finally { progress.value = '' }
  error.value = ''
  try { await refreshImages() }
  catch (e) { error.value = 'Sản phẩm và ảnh đã được lưu nhưng không tải được chi tiết. ' + errorMessage(e); throw e }
  success.value = 'Thêm sản phẩm thành công.'
  await router.push({ path: '/san-pham', query: { created: product.value.maSanPham } })
}
function retryImages() {
  if (saving.value || confirmation.value || !product.value) return
  const selection = newImages.value.map(image => ({ ...image }))
  askConfirmation({ title: 'Xác nhận hoàn tất sản phẩm', message: 'Sản phẩm đã được tạo. Chỉ tải những ảnh chưa được lưu và kiểm tra lại kết quả.',
    details: [{ label: 'Sản phẩm', value: product.value.maSanPham }, { label: 'Ảnh còn lại', value: selection.length }] },
    () => completeCreation(selection))
}
onMounted(load)
async function refreshImages() {
  error.value = ''
  const detail = await productService.get(product.value.id)
  product.value = detail.product; images.value = detail.images
}
</script>
<template>
  <ProductShell title="Thêm sản phẩm" description="Nhập thông tin và xác nhận để tạo sản phẩm. Sau khi lưu thành công, bạn sẽ trở về danh sách." :error="error" :success="success">
    <template #actions><RouterLink to="/san-pham" class="p-btn"><i class="bi bi-arrow-left"></i> Quay lại</RouterLink></template>
    <template v-if="product">
      <section class="p-card">
        <h2>Sản phẩm {{ product.maSanPham }} đã được tạo</h2>
        <p>{{ product.tenSanPham }}</p>
        <p>Thông tin sản phẩm đã được lưu. Có thể thử lại những ảnh còn lại hoặc xem sản phẩm để thêm biến thể.</p>
        <button type="button" class="p-btn primary" :disabled="saving" @click="retryImages">Thử lại / Hoàn tất</button>
        <RouterLink :to="'/san-pham/' + product.id" class="p-btn">Hoàn tất / Xem sản phẩm</RouterLink>
      </section>
    </template>
    <ProductForm v-else-if="ready" :code="code" :options="options" :saving="saving" @save="save" />
    <div v-else class="p-card p-empty">Đang chờ dữ liệu thuộc tính. <button v-if="error" class="p-btn" @click="load">Thử lại</button></div>
    <ProductImages v-if="ready" v-model="newImages" :product-id="product?.id" :product-code="product?.maSanPham || code" :images="images" deferred :loading="saving" @changed="refreshImages().catch(e => error = errorMessage(e))" />
    <ConfirmModal v-bind="confirmation || {}" :show="!!confirmation" :loading="saving" :error="product && error ? error : confirmError" @confirm="confirmAction" @cancel="cancelConfirmation">
      <p v-if="progress" role="status">{{ progress }}</p>
    </ConfirmModal>
  </ProductShell>
</template>
