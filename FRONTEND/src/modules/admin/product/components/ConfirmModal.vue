<script setup>
import { nextTick, ref, watch } from 'vue'

const props = defineProps({
  show: Boolean, title: String, message: String,
  details: { type: Array, default: () => [] },
  confirmText: { type: String, default: 'Xác nhận' },
  cancelText: { type: String, default: 'Hủy' },
  loading: Boolean, error: String
})
const emit = defineEmits(['confirm', 'cancel'])
const dialog = ref(null), cancelButton = ref(null)
let previousFocus = null
watch(() => props.show, async show => {
  if (show) {
    previousFocus = document.activeElement
    await nextTick()
    if (props.show) cancelButton.value?.focus()
  } else if (previousFocus?.isConnected) previousFocus.focus()
}, { flush: 'post' })
function cancel() { if (!props.loading) emit('cancel') }
function onKeydown(event) {
  if (event.key === 'Escape') {
    event.preventDefault(); event.stopPropagation(); cancel()
  } else if (event.key === 'Tab') {
    const buttons = [...dialog.value.querySelectorAll('button:not(:disabled), a[href], [tabindex="0"]')]
      .filter(element => element.getClientRects().length)
    if (!buttons.length) { event.preventDefault(); dialog.value.focus(); return }
    const first = buttons[0], last = buttons[buttons.length - 1]
    if (event.shiftKey && (document.activeElement === first || document.activeElement === dialog.value)) {
      event.preventDefault(); last.focus()
    } else if (!event.shiftKey && (document.activeElement === last || document.activeElement === dialog.value)) {
      event.preventDefault(); first.focus()
    }
  }
}
</script>

<template>
  <div v-if="show" class="p-overlay p-confirm-overlay" @click.self="cancel" @keydown="onKeydown">
    <section ref="dialog" class="p-card p-dialog" role="dialog" aria-modal="true" :aria-label="title" :aria-busy="loading" tabindex="-1">
      <h2>{{ title }}</h2>
      <p v-if="message" class="p-confirm-message">{{ message }}</p>
      <dl v-if="details.length" class="p-confirm-details">
        <div v-for="(item, index) in details" :key="index"><dt>{{ item.label }}</dt><dd>{{ item.value }}</dd></div>
      </dl>
      <slot />
      <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
      <div v-if="loading" role="status">Đang xử lý...</div>
      <div class="p-form-actions">
        <button ref="cancelButton" type="button" class="p-btn" :disabled="loading" @click="cancel">{{ cancelText }}</button>
        <button type="button" class="p-btn primary" :disabled="loading" @click="!loading && emit('confirm')">{{ loading ? 'Đang lưu...' : confirmText }}</button>
      </div>
    </section>
  </div>
</template>
