<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProductShell from '../components/ProductShell.vue'
import ProductForm from '../components/ProductForm.vue'
import { productService, errorMessage } from '../services/productService'
import { productAttributeService } from '../services/productAttributeService'
const route = useRoute(), router = useRouter()
const product = ref(null), options = ref({}), saving = ref(false), error = ref('')
let loadVersion = 0
async function load() {
  const version = ++loadVersion
  error.value = ''; product.value = null
  try {
    const [detail, attributes] = await Promise.all([productService.get(route.params.id), productAttributeService.options()])
    if (version !== loadVersion) return
    product.value = detail.product; options.value = attributes
  } catch (e) { if (version === loadVersion) error.value = errorMessage(e) }
}
async function save(data) {
  if (saving.value) return
  saving.value = true; error.value = ''
  try { await productService.update(product.value.id, data); await router.push('/san-pham/' + product.value.id) }
  catch (e) { error.value = errorMessage(e) } finally { saving.value = false }
}
watch(() => route.params.id, load, { immediate: true })
</script>
<template>
  <ProductShell title="Sửa sản phẩm" description="Cập nhật thông tin sản phẩm và giữ nguyên các biến thể hiện có." :error="error">
    <template #actions><RouterLink :to="'/san-pham/' + route.params.id" class="p-btn"><i class="bi bi-arrow-left"></i> Quay lại</RouterLink></template>
    <ProductForm v-if="product" :key="product.id" :initial="product" :options="options" :saving="saving" editing @save="save" />
    <div v-else class="p-card p-empty">{{ error ? 'Không tải được sản phẩm.' : 'Đang tải...' }} <button v-if="error" class="p-btn" @click="load">Thử lại</button></div>
  </ProductShell>
</template>

