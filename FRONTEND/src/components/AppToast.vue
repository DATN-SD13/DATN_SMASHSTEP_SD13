<script setup>
defineProps({
  show: {
    type: Boolean,
    default: false
  },

  type: {
    type: String,
    default: 'success'
  },

  title: {
    type: String,
    default: 'Thành công'
  },

  message: {
    type: String,
    default: ''
  }
})

const emit = defineEmits([
  'close'
])
</script>

<template>
  <Transition name="toast-slide">
    <div
      v-if="show"
      class="ss-toast"
      :class="type"
    >
      <div class="toast-icon">
        <i
          v-if="type === 'success'"
          class="bi bi-check-lg"
        ></i>

        <i
          v-else-if="type === 'error'"
          class="bi bi-x-lg"
        ></i>

        <i
          v-else
          class="bi bi-info-lg"
        ></i>
      </div>

      <div class="toast-body">
        <strong>{{ title }}</strong>
        <span>{{ message }}</span>
      </div>

      <button
        type="button"
        class="toast-close"
        @click="emit('close')"
      >
        <i class="bi bi-x-lg"></i>
      </button>
    </div>
  </Transition>
</template>

<style scoped>
.ss-toast {
  position: fixed;
  top: 24px;
  right: 28px;

  z-index: 999999;

  width: 420px;
  max-width: calc(100vw - 32px);

  display: flex;
  align-items: center;

  gap: 14px;

  padding: 18px;

  background: #fff;

  border: 1px solid #e2e8f0;
  border-radius: 14px;

  box-shadow:
    0 14px 40px rgba(15, 23, 42, 0.16);
}

.toast-icon {
  width: 42px;
  height: 42px;

  flex: 0 0 42px;

  display: flex;
  align-items: center;
  justify-content: center;

  border-radius: 50%;

  font-size: 20px;
}

.ss-toast.success .toast-icon {
  color: #16a34a;
  background: #ecfdf5;
}

.ss-toast.error .toast-icon {
  color: #dc2626;
  background: #fef2f2;
}

.ss-toast.info .toast-icon {
  color: #0284c7;
  background: #f0f9ff;
}

.toast-body {
  min-width: 0;
  flex: 1;

  display: flex;
  flex-direction: column;

  gap: 3px;
}

.toast-body strong {
  color: #1e293b;

  font-size: 15px;
  font-weight: 700;
}

.toast-body span {
  color: #64748b;

  font-size: 13px;
  line-height: 1.45;
}

.toast-close {
  width: 34px;
  height: 34px;

  flex: 0 0 34px;

  display: flex;
  align-items: center;
  justify-content: center;

  border: none;
  border-radius: 8px;

  background: transparent;

  color: #94a3b8;

  cursor: pointer;
}

.toast-close:hover {
  color: #475569;
  background: #f1f5f9;
}

.toast-slide-enter-active,
.toast-slide-leave-active {
  transition: all 0.25s ease;
}

.toast-slide-enter-from {
  opacity: 0;
  transform: translateX(40px);
}

.toast-slide-leave-to {
  opacity: 0;
  transform: translateX(40px);
}

@media (max-width: 640px) {
  .ss-toast {
    top: 16px;
    left: 16px;
    right: 16px;

    width: auto;
  }
}
</style>