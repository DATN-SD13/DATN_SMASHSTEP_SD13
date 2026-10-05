<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { imageTypes, validateImageFile } from '../services/imageUtils'

const props = defineProps({ modelValue: { type: Array, default: () => [] }, disabled: Boolean })
const emit = defineEmits(['update:modelValue'])
const input = ref(null), error = ref('')
const previews = new Set()
function release(url) {
  if (previews.delete(url)) URL.revokeObjectURL(url)
}
watch(() => props.modelValue, images => {
  const current = new Set(images.map(image => image.previewUrl))
  for (const url of previews) if (!current.has(url)) release(url)
}, { flush: 'post' })
onBeforeUnmount(() => { for (const url of previews) release(url) })

function choose(event) {
  error.value = ''
  const images = [...props.modelValue], errors = []
  try {
    if (props.disabled) return
    for (const file of event.target.files || []) {
      const message = validateImageFile(file)
      if (message) { errors.push(message); continue }
      const key = JSON.stringify([file.name, file.size, file.lastModified, file.type])
      if (images.some(image => image.key === key)) continue
      try {
        const previewUrl = URL.createObjectURL(file)
        previews.add(previewUrl)
        images.push({ key, file, previewUrl, isAnhChinh: false, existing: false })
      } catch { errors.push('Không thể đọc tệp ảnh đã chọn.') }
    }
    emit('update:modelValue', images)
    error.value = [...new Set(errors)].join(' ')
  } finally { event.target.value = '' }
}
function remove(image) {
  if (props.disabled) return
  emit('update:modelValue', props.modelValue.filter(item => item.key !== image.key))
}
function main(image, checked) {
  if (props.disabled) return
  emit('update:modelValue', props.modelValue.map(item => ({ ...item, isAnhChinh: checked && item.key === image.key })))
}
function failed(image) {
  error.value = 'Tệp ảnh không hợp lệ: ' + image.file.name
  emit('update:modelValue', props.modelValue.filter(item => item.key !== image.key))
}
</script>

<template>
  <div>
    <input ref="input" type="file" :accept="imageTypes" multiple :disabled="disabled" class="visually-hidden" aria-label="Tệp ảnh sản phẩm" @change="choose" />
    <button type="button" class="p-btn" :disabled="disabled" @click="input.click()"><i class="bi bi-folder2-open"></i> Chọn ảnh từ máy</button>
    <small class="p-image-help">JPG, PNG, WEBP · Tối đa 5 MB/ảnh. Có thể chọn nhiều ảnh.</small>
    <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
    <div v-if="modelValue.length" class="p-images p-image-previews">
      <div v-for="image in modelValue" :key="image.key" class="p-image">
        <img :src="image.previewUrl" :alt="'Xem trước ' + image.file.name" @error="failed(image)" />
        <p class="p-image-name">{{ image.file.name }}</p>
        <small>Chưa tải lên · {{ (image.file.size / 1024).toFixed(1) }} KB</small>
        <label class="p-image-main"><input type="checkbox" :checked="image.isAnhChinh" :disabled="disabled" @change="main(image, $event.target.checked)" /> Đặt làm ảnh chính</label>
        <button type="button" class="p-btn danger" :disabled="disabled" :aria-label="'Xóa ảnh chưa lưu ' + image.file.name" @click="remove(image)">Xóa ảnh chưa lưu</button>
      </div>
    </div>
  </div>
</template>
