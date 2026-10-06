<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="confirm-overlay"
      @click.self="cancel"
    >
      <div class="confirm-modal">

        <!-- Icon -->
        <div class="confirm-icon">
          <i class="bi bi-question-lg"></i>
        </div>

        <!-- Nội dung -->
        <div class="confirm-content">
          <h3>{{ title }}</h3>

          <p v-html="message"></p>
        </div>

        <!-- Nút -->
        <div class="confirm-actions">
          <button
            type="button"
            class="btn-cancel"
            @click="cancel"
          >
            Hủy
          </button>

          <button
            type="button"
            class="btn-confirm"
            :disabled="loading"
            @click="confirm"
          >
            <span v-if="loading">
              Đang xử lý...
            </span>

            <span v-else>
              {{ confirmText }}
            </span>
          </button>
        </div>

      </div>
    </div>
  </Teleport>
</template>

<script setup>
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },

  title: {
    type: String,
    default: 'Xác nhận'
  },

  message: {
    type: String,
    default: 'Bạn có chắc chắn muốn thực hiện thao tác này không?'
  },

  confirmText: {
    type: String,
    default: 'Đồng ý'
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

function confirm() {
  if (props.loading) return

  emit('confirm')
}

function cancel() {
  if (props.loading) return

  emit('cancel')
}
</script>

<style scoped>
.confirm-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;

  display: flex;
  align-items: center;
  justify-content: center;

  background: rgba(15, 23, 42, 0.48);
  backdrop-filter: blur(2px);
}

.confirm-modal {
  width: 360px;
  max-width: calc(100vw - 32px);

  background: #ffffff;
  border-radius: 18px;

  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.2);

  overflow: hidden;

  animation: confirmShow 0.2s ease-out;
}

/* Icon */
.confirm-icon {
  width: 64px;
  height: 64px;

  margin: 24px auto 14px;

  border: 3px solid #2296d2;
  border-radius: 50%;

  display: flex;
  align-items: center;
  justify-content: center;

  color: #2296d2;
  font-size: 30px;
  font-weight: 600;
}

/* Nội dung */
.confirm-content {
  padding: 0 28px 22px;
  text-align: center;
}

.confirm-content h3 {
  margin: 0 0 10px;

  color: #263746;
  font-size: 20px;
  font-weight: 700;
}

.confirm-content p {
  margin: 0;

  color: #687784;
  font-size: 14px;
  line-height: 1.6;
}

/* Buttons */
.confirm-actions {
  display: flex;
  justify-content: center;
  gap: 10px;

  padding: 22px 28px;

  border-top: 1px solid #eeeeee;
}

.confirm-actions button {
  min-width: 98px;
  height: 46px;

  padding: 0 18px;

  border-radius: 7px;

  font-size: 15px;
  font-weight: 600;

  cursor: pointer;

  transition: all 0.2s ease;
}

/* Hủy */
.btn-cancel {
  border: 1px solid #d9e0e5;
  background: #ffffff;
  color: #6c757d;
}

.btn-cancel:hover {
  background: #f5f7f9;
}

/* Đồng ý */
.btn-confirm {
  border: none;
  background: #2296d2;
  color: #ffffff;
}

.btn-confirm:hover {
  background: #1684bd;
}

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Animation */
@keyframes confirmShow {
  from {
    opacity: 0;
    transform: scale(0.95);
  }

  to {
    opacity: 1;
    transform: scale(1);
  }
}
</style>