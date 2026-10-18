<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import ProductToast from '../modules/admin/product/components/ProductToast.vue'
import { useProductToast } from '../modules/admin/product/composables/useProductToast'
import '../modules/admin/product/product.css'

const confirmation = ref(null)
const busy = ref(false)
const { show } = useProductToast()
function cancel() {
  if (busy.value) return
  confirmation.value?.onCancel?.()
  confirmation.value = null
}
function confirmEvent(event) {
  if (!busy.value && !confirmation.value && typeof event.detail?.onConfirm === 'function') confirmation.value = event.detail
  else event.detail?.onCancel?.()
}
function toastEvent(event) {
  show(event.detail.type, event.detail.message, event.detail.title)
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
onBeforeUnmount(() => { window.removeEventListener('ss:confirm', confirmEvent); window.removeEventListener('ss:toast', toastEvent) })
</script>

<template>
  <Teleport to="body">
    <div v-if="confirmation" class="ss-modal-bg feedback-overlay" @click.self="cancel">
      <section class="ss-modal feedback-dialog" role="dialog" aria-modal="true" aria-labelledby="feedback-title">
        <h2 id="feedback-title">{{ confirmation.title || 'Xác nhận' }}</h2>
        <p>{{ confirmation.message }}</p>
        <div class="ss-actions">
          <button class="ss-btn" :disabled="busy" @click="cancel">{{ confirmation.cancelText || 'Hủy' }}</button>
          <button class="ss-btn primary" :disabled="busy" @click="accept">{{ busy ? 'Đang xử lý...' : confirmation.confirmText || 'Đồng ý' }}</button>
        </div>
      </section>
    </div>
    <ProductToast />
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
