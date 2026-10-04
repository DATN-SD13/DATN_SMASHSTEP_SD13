<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ProductShell from '../components/ProductShell.vue'
import ProductForm from '../components/ProductForm.vue'
import { productService, errorMessage } from '../services/productService'
import { productAttributeService } from '../services/productAttributeService'
const router = useRouter()
const options = ref({}), ready = ref(false), saving = ref(false), error = ref('')
async function load() {
  error.value = ''
  try { options.value = await productAttributeService.options(); ready.value = true } catch (e) { error.value = errorMessage(e) }
}
async function save(data) {
  if (saving.value) return
  saving.value = true; error.value = ''
  try { const product = await productService.create(data); await router.push('/san-pham/' + product.id + '?created=1') }
  catch (e) { error.value = errorMessage(e) } finally { saving.value = false }
}
onMounted(load)
</script>
<template>
  <ProductShell title="Thêm sản phẩm" description="Sau khi tạo sản phẩm, thêm biến thể màu sắc × kích thước và URL ảnh tại trang chi tiết." :error="error">
    <template #actions><RouterLink to="/san-pham" class="p-btn"><i class="bi bi-arrow-left"></i> Quay lại</RouterLink></template>
    <ProductForm v-if="ready" :options="options" :saving="saving" @save="save" />
    <div v-else class="p-card p-empty">Đang chờ dữ liệu thuộc tính. <button v-if="error" class="p-btn" @click="load">Thử lại</button></div>
  </ProductShell>
</template>

