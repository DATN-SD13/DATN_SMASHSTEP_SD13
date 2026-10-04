<script setup>
import { ref, reactive, watch } from 'vue'
import { productService, errorMessage } from '../services/productService'
const props = defineProps({ productId: Number, images: { type: Array, default: () => [] } })
const emit = defineEmits(['changed'])
const newImage = reactive({ urlAnh: '', isAnhChinh: false })
const urls = ref({}), failed = ref({}), busy = ref(false), error = ref('')
watch(() => props.images, images => {
  urls.value = Object.fromEntries(images.map(i => [i.id, i.urlAnh]))
  failed.value = {}
}, { immediate: true })
async function act(operation) {
  if (busy.value) return
  busy.value = true; error.value = ''
  try { await operation(); emit('changed') } catch (e) { error.value = errorMessage(e) } finally { busy.value = false }
}
function add() {
  act(async () => {
    await productService.addImage(props.productId, { urlAnh: newImage.urlAnh.trim(), isAnhChinh: newImage.isAnhChinh })
    newImage.urlAnh = ''; newImage.isAnhChinh = false
  })
}
function update(image, main = image.isAnhChinh) {
  act(() => productService.updateImage(props.productId, image.id, { urlAnh: urls.value[image.id].trim(), isAnhChinh: main }))
}
function remove(image) {
  if (window.confirm('Gỡ ảnh này khỏi sản phẩm?')) act(() => productService.removeImage(props.productId, image.id))
}
</script>
<template>
  <section class="p-card">
    <h2><i class="bi bi-images"></i> Hình ảnh sản phẩm</h2>
    <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
    <form @submit.prevent="add"><div class="p-form-grid"><label>URL ảnh HTTP/HTTPS<input v-model="newImage.urlAnh" type="url" required maxlength="1000" placeholder="https://..." :disabled="busy" /></label><label>Ảnh chính<select v-model="newImage.isAnhChinh" :disabled="busy"><option :value="false">Không</option><option :value="true">Có</option></select></label></div><div class="p-form-actions"><button class="p-btn primary" :disabled="busy"><i class="bi bi-plus-lg"></i> Thêm ảnh</button><small>Ảnh đầu tiên tự được chọn làm ảnh chính.</small></div></form>
    <div class="p-images" style="margin-top:18px">
      <form v-for="image in images" :key="image.id" class="p-image" @submit.prevent="update(image)">
        <img v-if="!failed[image.id]" :src="image.urlAnh" alt="Ảnh sản phẩm" loading="lazy" referrerpolicy="no-referrer" @error="failed[image.id] = true" /><div v-else class="p-image-error">Không tải được ảnh từ URL</div>
        <span v-if="image.isAnhChinh" class="p-badge active">Ảnh chính</span>
        <input v-model="urls[image.id]" type="url" required maxlength="1000" aria-label="URL ảnh" :disabled="busy" />
        <div class="p-actions"><button class="p-btn" :disabled="busy">Lưu URL</button><button v-if="!image.isAnhChinh" class="p-btn" type="button" :disabled="busy" @click="update(image, true)">Chọn ảnh chính</button><button class="p-btn danger" type="button" :disabled="busy" @click="remove(image)">Gỡ ảnh</button></div>
      </form>
    </div>
    <div v-if="!images.length" class="p-empty">Sản phẩm chưa có ảnh.</div>
  </section>
</template>

