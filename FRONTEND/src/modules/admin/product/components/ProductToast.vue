<script setup>
import { useProductToast } from '../composables/useProductToast'
const { toast, close } = useProductToast()
const icons = { success: 'bi-check-lg', error: 'bi-x-lg', warning: 'bi-exclamation-lg', info: 'bi-info-lg' }
</script>
<template>
  <Transition name="p-toast" mode="out-in">
    <div v-if="toast" :key="toast.id" class="product-toast" :class="toast.type" :role="toast.type === 'error' ? 'alert' : 'status'" aria-live="polite" aria-atomic="true">
      <span class="p-toast-icon"><i class="bi" :class="icons[toast.type]"></i></span>
      <div class="p-toast-body"><strong>{{ toast.title }}</strong><span>{{ toast.message }}</span></div>
      <button type="button" class="p-toast-close" aria-label="Đóng thông báo" @click="close">×</button>
    </div>
  </Transition>
</template>
