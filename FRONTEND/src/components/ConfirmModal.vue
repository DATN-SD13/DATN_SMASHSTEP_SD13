<script setup>
defineProps({
  show: {
    type: Boolean,
    default: false
  },

  title: {
    type: String,
    default: 'Xác nhận'
  },

  message: {
    type: String,
    default: 'Bạn có chắc chắn muốn thực hiện thao tác này?'
  },

  confirmText: {
    type: String,
    default: 'Đồng ý'
  },

  cancelText: {
    type: String,
    default: 'Hủy'
  },

  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits([
  'confirm',
  'cancel'
])
</script>

<template>
  <Teleport to="body">
    <Transition name="modal-fade">

      <div
        v-if="show"
        class="confirm-mask"
        @click.self="
          !loading && emit('cancel')
        "
      >
        <div class="confirm-card">

          <div class="confirm-icon">
            ?
          </div>

          <h2>
            {{ title }}
          </h2>

          <p>
            {{ message }}
          </p>

          <div class="confirm-actions">

            <button
              type="button"
              class="confirm-btn cancel"
              :disabled="loading"
              @click="emit('cancel')"
            >
              {{ cancelText }}
            </button>

            <button
              type="button"
              class="confirm-btn primary"
              :disabled="loading"
              @click="emit('confirm')"
            >
              <span
                v-if="loading"
                class="spinner-border spinner-border-sm"
              ></span>

              {{
                loading
                  ? 'Đang xử lý...'
                  : confirmText
              }}
            </button>

          </div>

        </div>
      </div>

    </Transition>
  </Teleport>
</template>

<style scoped>
.confirm-mask {
  position: fixed;
  inset: 0;

  z-index: 999998;

  display: flex;
  align-items: center;
  justify-content: center;

  padding: 20px;

  background:
    rgba(15, 23, 42, 0.46);

  backdrop-filter: blur(1px);
}

.confirm-card {
  width: min(390px, 100%);

  padding: 28px 28px 22px;

  text-align: center;

  background: #fff;

  border-radius: 18px;

  box-shadow:
    0 25px 70px rgba(15, 23, 42, 0.25);
}

.confirm-icon {
  width: 68px;
  height: 68px;

  margin: 0 auto 18px;

  display: flex;
  align-items: center;
  justify-content: center;

  border: 3px solid #168aad;
  border-radius: 50%;

  color: #168aad;

  font-size: 34px;
  font-weight: 500;
}

.confirm-card h2 {
  margin: 0 0 10px;

  color: #1e293b;

  font-size: 22px;
  font-weight: 750;
}

.confirm-card p {
  margin: 0 auto;

  max-width: 310px;

  color: #64748b;

  font-size: 14px;
  line-height: 1.6;
}

.confirm-actions {
  display: flex;

  gap: 12px;

  margin: 25px -28px -22px;

  padding: 20px 28px;

  border-top: 1px solid #e2e8f0;
}

.confirm-btn {
  height: 46px;

  flex: 1;

  border-radius: 8px;

  font-size: 15px;

  cursor: pointer;
}

.confirm-btn.cancel {
  border: 1px solid #cbd5e1;

  color: #64748b;
  background: #fff;
}

.confirm-btn.cancel:hover {
  background: #f8fafc;
}

.confirm-btn.primary {
  border: 1px solid #0ea5e9;

  color: #fff;
  background: #0ea5e9;
}

.confirm-btn.primary:hover {
  background: #0284c7;
}

.confirm-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>