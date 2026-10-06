<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const confirmation = ref(null)
const busy = ref(false)
const toast = ref(null)
let timer
function confirmEvent(event) {
  if (!busy.value && !confirmation.value && typeof event.detail?.onConfirm === 'function') confirmation.value = event.detail
}
function toastEvent(event) {
  clearTimeout(timer)
  toast.value = event.detail
  timer = setTimeout(() => { toast.value = null }, 5000)
}
async function accept() {
  if (busy.value || !confirmation.value) return
  busy.value = true
  try { await confirmation.value.onConfirm(); confirmation.value = null }
  catch (error) {
    confirmation.value = null
    toastEvent({ detail: { type: 'error', title: 'Thất bại', message: error.response?.data?.message || 'Thao tác thất bại. Hãy thử lại.' } })
  } finally { busy.value = false }
}
onMounted(() => { window.addEventListener('ss:confirm', confirmEvent); window.addEventListener('ss:toast', toastEvent) })
onBeforeUnmount(() => { window.removeEventListener('ss:confirm', confirmEvent); window.removeEventListener('ss:toast', toastEvent); clearTimeout(timer) })
</script>

<template>
  <Teleport to="body">
    <div v-if="confirmation" class="ss-modal-bg feedback-overlay" @click.self="!busy && (confirmation = null)">
      <section class="ss-modal feedback-dialog" role="dialog" aria-modal="true" aria-labelledby="feedback-title">
        <h2 id="feedback-title">{{ confirmation.title || 'Xác nhận' }}</h2>
        <p>{{ confirmation.message }}</p>
        <div class="ss-actions">
          <button class="ss-btn" :disabled="busy" @click="confirmation = null">{{ confirmation.cancelText || 'Hủy' }}</button>
          <button class="ss-btn primary" :disabled="busy" @click="accept">{{ busy ? 'Đang xử lý...' : confirmation.confirmText || 'Đồng ý' }}</button>
        </div>
      </section>
    </div>
    <div v-if="toast" class="feedback-toast" :class="toast.type" role="status" aria-live="polite">
      <strong>{{ toast.title }}</strong><p>{{ toast.message }}</p>
      <button class="ss-icon-btn" aria-label="Đóng thông báo" @click="toast = null">×</button>
    </div>
  </Teleport>
</template>

<style scoped>
.feedback-overlay { z-index: 3000; }
.feedback-dialog { max-width: 460px; width: calc(100% - 32px); padding: 24px; gap: 18px; }
.feedback-dialog h2 { font-size: 18px; }
.feedback-dialog p { margin: 0; }
.feedback-toast { position: fixed; z-index: 3100; right: 24px; bottom: 24px; max-width: min(480px, calc(100% - 48px)); background: #fff; color: #166534; border: 1px solid #86efac; border-radius: 12px; padding: 18px 48px 18px 20px; box-shadow: 0 8px 30px #0002; }
.feedback-toast.error { color: #991b1b; border-color: #fca5a5; }
.feedback-toast p { margin: 6px 0 0; }
.feedback-toast button { position: absolute; right: 6px; top: 8px; }
</style>
