<script setup>
import { computed, ref } from 'vue'
import { initials } from '../utils/paging'
import { avatarUrl, avatarTypes, avatarMaxSize } from '../utils/avatar'
import { showError } from '../utils/feedback'

const props = defineProps({ name: String, email: String, fallback: { type: String, default: 'KH' }, image: String, disabled: Boolean, hint: { type: Boolean, default: true } })
const emit = defineEmits(['update:image'])
const input = ref(null)
const text = computed(() => initials(props.name) || props.fallback)

function pick(e) {
  const f = e.target.files?.[0]
  if (!f) return
  if (props.disabled) return
  if (!avatarTypes.split(',').includes(f.type) || !f.size || f.size > avatarMaxSize) {
    showError('Chỉ chọn ảnh JPG, PNG hoặc WEBP không quá 5 MB.')
    e.target.value = ''
    return
  }
  const r = new FileReader()
  r.onload = () => emit('update:image', r.result)
  r.onerror = () => showError('Không thể đọc tệp ảnh đã chọn.')
  r.readAsDataURL(f)
}
</script>

<template>
  <section class="ss-card">
    <button type="button" class="big" :disabled="disabled" title="Chọn ảnh đại diện" @click="input.click()">
      <img v-if="avatarUrl(image)" :src="avatarUrl(image)" alt="Ảnh đại diện" /><span v-else>{{ text }}</span>
    </button>
    <input ref="input" type="file" :accept="avatarTypes" :disabled="disabled" hidden @change="pick" />
    <div class="who">
      <strong>{{ name || 'Chưa nhập tên' }}</strong>
      <small>{{ email || 'Chưa cập nhật email' }}</small>
      <small v-if="hint" class="hint">(Bấm vào ảnh để chọn avatar)</small>
      <slot />
    </div>
  </section>
</template>

<style scoped>
.big { width: 96px; height: 96px; border-radius: 50%; border: 0; background: var(--ss-surface); color: var(--ss-text); font-size: 30px; font-weight: 500; display: grid; place-items: center; cursor: pointer; overflow: hidden; padding: 0; transition: box-shadow .15s; }
.big:hover { box-shadow: 0 0 0 4px var(--ss-primary-soft); }
.big img { width: 100%; height: 100%; object-fit: cover; }
.who { display: flex; flex-direction: column; gap: 6px; align-items: flex-start; }
.who strong { font-size: 13px; font-weight: 700; color: var(--ss-ink); }
.who small { font-size: 10.5px; color: var(--ss-muted); word-break: break-all; }
.who .hint { color: var(--ss-faint); font-size: 9.5px; }
</style>
