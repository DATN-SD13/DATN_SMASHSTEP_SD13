<script setup>
import { ref, computed, watch } from 'vue'
import { productService, errorMessage } from '../services/productService'
import { imageUrl, validateImageFile } from '../services/imageUtils'
import ProductImagePicker from './ProductImagePicker.vue'
import ConfirmModal from './ConfirmModal.vue'
import { useConfirmation } from '../composables/useConfirmation'

const props = defineProps({
  productId: Number, productCode: String, images: { type: Array, default: () => [] },
  modelValue: Array, deferred: Boolean, loading: Boolean
})
const emit = defineEmits(['changed', 'update:modelValue'])
const localImages = ref([]), failed = ref({}), error = ref(''), success = ref(''), progress = ref('')
const drafts = computed({
  get: () => props.modelValue ?? localImages.value,
  set: images => { localImages.value = images; emit('update:modelValue', images) }
})
const { confirmation, confirming: busy, confirmError, askConfirmation, cancelConfirmation, confirmAction } = useConfirmation()
const locked = computed(() => busy.value || props.loading)
watch(() => props.images, () => { failed.value = {} })

function add() {
  if (locked.value || confirmation.value || !props.productId || !drafts.value.length) return
  error.value = ''; success.value = ''
  const selection = drafts.value.map(image => ({ ...image }))
  for (const image of selection) {
    const message = validateImageFile(image.file)
    if (message) { error.value = message; return }
  }
  const id = props.productId
  askConfirmation({ title: 'Xác nhận thêm hình ảnh',
    message: `Bạn có chắc muốn thêm các hình ảnh đã chọn cho sản phẩm ${props.productCode || id}?`,
    details: [{ label: 'Số ảnh', value: selection.length },
      { label: 'Tệp ảnh', value: selection.map(image => image.file.name).join('\n') },
      { label: 'Ảnh chính được chọn', value: selection.find(image => image.isAnhChinh)?.file.name || 'Giữ ảnh chính hiện tại; ảnh đầu tiên tự làm chính nếu chưa có ảnh.' }]
  }, async () => {
    let uploaded = 0
    const remaining = selection.filter(image => drafts.value.some(item => item.key === image.key))
    error.value = ''
    try {
      for (const image of remaining) {
        progress.value = `Đang tải ảnh ${uploaded + 1}/${remaining.length}...`
        await productService.uploadImage(id, image)
        drafts.value = drafts.value.filter(item => item.key !== image.key)
        uploaded++
      }
      success.value = 'Thêm hình ảnh thành công.'
    } catch (e) {
      error.value = uploaded ? `Đã tải ${uploaded} ảnh; các ảnh còn lại chưa được lưu. Bạn có thể thử lại. ${errorMessage(e)}` : errorMessage(e)
      throw e
    } finally {
      progress.value = ''
      if (uploaded) emit('changed')
    }
  })
}

function askImage(title, image, action) {
  if (locked.value || confirmation.value) return
  error.value = ''; success.value = ''
  const id = props.productId, imageId = image.id, urlAnh = image.urlAnh
  askConfirmation({ title, message: `Bạn có chắc muốn ${action} cho sản phẩm ${props.productCode || id}?`,
    details: [{ label: 'Ảnh', value: urlAnh }] }, async () => {
    if (action === 'gỡ ảnh này') await productService.removeImage(id, imageId)
    else await productService.updateImage(id, imageId, { urlAnh, isAnhChinh: true })
    success.value = action === 'gỡ ảnh này' ? 'Gỡ hình ảnh thành công.' : 'Chọn ảnh chính thành công.'
    emit('changed')
  })
}
</script>

<template>
  <section class="p-card">
    <h2><i class="bi bi-images"></i> Hình ảnh sản phẩm</h2>
    <p>Ảnh được dùng chung cho các biến thể của sản phẩm. Chọn ảnh từ máy để xem trước khi lưu.</p>
    <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
    <div v-if="success" class="alert alert-success" role="status">{{ success }}</div>
    <ProductImagePicker v-model="drafts" :disabled="locked" />
    <small v-if="deferred" class="p-image-help">Ảnh đã chọn sẽ được tải lên sau khi xác nhận lưu sản phẩm.</small>
    <div v-else class="p-form-actions">
      <button type="button" class="p-btn primary" :disabled="locked || !drafts.length || !productId" @click="add"><i class="bi bi-plus-lg"></i> {{ busy ? 'Đang tải ảnh...' : 'Thêm ' + drafts.length + ' ảnh' }}</button>
      <small>Ảnh đầu tiên tự được chọn làm ảnh chính.</small>
    </div>
    <div class="p-images" style="margin-top:18px">
      <div v-for="image in images" :key="image.id" class="p-image">
        <img v-if="imageUrl(image.urlAnh) && !failed[image.id]" :src="imageUrl(image.urlAnh)" alt="Ảnh sản phẩm" loading="lazy" referrerpolicy="no-referrer" @error="failed[image.id] = true" />
        <div v-else class="p-image-error">Không tải được ảnh</div>
        <span v-if="image.isAnhChinh" class="p-badge active">Ảnh chính</span>
        <div class="p-actions">
          <button v-if="!image.isAnhChinh" class="p-btn" type="button" :disabled="locked" @click="askImage('Xác nhận chọn ảnh chính', image, 'chọn ảnh này làm ảnh chính')">Chọn ảnh chính</button>
          <button class="p-btn danger" type="button" :disabled="locked" @click="askImage('Xác nhận gỡ hình ảnh', image, 'gỡ ảnh này')">Gỡ ảnh</button>
        </div>
      </div>
    </div>
    <div v-if="!images.length && !drafts.length" class="p-empty">Sản phẩm chưa có ảnh.</div>
    <ConfirmModal v-bind="confirmation || {}" :show="!!confirmation" :loading="busy" :error="confirmError" @confirm="confirmAction" @cancel="cancelConfirmation">
      <p v-if="progress" role="status">{{ progress }}</p>
      <p v-if="error && !busy" class="p-field-error">{{ error }}</p>
    </ConfirmModal>
  </section>
</template>
