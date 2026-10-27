<script setup>
import Select2KhachHang from './Select2KhachHang.vue'
import { ref } from 'vue'
import AvatarCard from '../../../../components/AvatarCard.vue'
import { persistAvatar } from '../../../../utils/avatar'
import customerService from '../services/customerService'
import { confirmAction, showSuccess, showError } from '../../../../utils/feedback'
import { accountRequestError } from '../../../../utils/accountValidation'

const props = defineProps({
  customer: {
    type: Object,
    required: true
  }
})

const emit = defineEmits([
  'close',
  'saved'
])

const f = ref({
  name: props.customer.name || '',
  email: props.customer.email || '',
  phone: props.customer.phone || '',
  gender: props.customer.gender ?? '',
  dob: props.customer.dob || '',
  image: props.customer.image || ''
})

const err = ref('')
const saving = ref(false)

const errorMessage = (e) =>
  e?.response?.data?.message || e?.message ||
  'Có lỗi xảy ra, vui lòng thử lại.'

async function save() {
  if (saving.value) return
  const v = f.value
  const validate = () => accountRequestError({ method: 'put', url: '/khach-hang/' + encodeURIComponent(props.customer.code), data: { ...v, gender: v.gender === '' ? null : Number(v.gender) } })
  const validationError = validate()
  if (validationError) { err.value = validationError; showError(validationError); return }

  if (!v.name.trim() || !v.email.trim()) {
    err.value = 'Vui lòng nhập họ tên và email.'
    return
  }

  if (!/^\S+@\S+\.\S+$/.test(v.email.trim())) {
    err.value = 'Email không hợp lệ.'
    return
  }

  if (
    v.phone &&
    !/^0\d{9}$/.test(v.phone.trim())
  ) {
    err.value =
      'Số điện thoại phải gồm 10 số và bắt đầu bằng 0.'
    return
  }

  if (
    v.dob &&
    new Date(`${v.dob}T00:00:00`) > new Date()
  ) {
    err.value =
      'Ngày sinh không được lớn hơn ngày hiện tại.'
    return
  }

  confirmAction('Xác nhận lưu thay đổi khách hàng?', async () => {
  if (saving.value) return
  const latestError = validate()
  if (latestError) { err.value = latestError; showError(latestError); return }
  saving.value = true
  err.value = ''

  try {
    const image = await persistAvatar(v.image)
    v.image = image || ''

    const data =
      await customerService.updateCustomer(
        props.customer.code,
        {
          name: v.name.trim(),
          email: v.email.trim(),
          phone: v.phone.trim() || null,
          gender:
            v.gender === ''
              ? null
              : Number(v.gender),
          dob: v.dob || null,
          image
        }
      )

    showSuccess('Đã cập nhật khách hàng thành công.')
    emit('saved', data)
  } catch (e) {
    err.value = errorMessage(e)
    showError(err.value)
  } finally {
    saving.value = false
  }
  })
}
</script>

<template>
  <div
    class="modal-mask"
    @click.self="!saving && $emit('close')"
  >
    <section class="modal-card">
      <div class="modal-head">
        <div>
          <h2>Cập nhật khách hàng</h2>
          <p>{{ customer.code }}</p>
        </div>

        <button
          class="ss-icon-btn"
          :disabled="saving"
          @click="$emit('close')"
        >
          <i class="bi bi-x-lg"></i>
        </button>
      </div>

      <fieldset class="edit-layout" :disabled="saving">
        <AvatarCard
          v-model:image="f.image"
          :disabled="saving"
          :name="f.name"
          :email="f.email"
          fallback="KH"
        />

        <div class="ss-form grow">
          <div class="ss-grid2">
            <div class="ss-field">
              <label class="ss-label">
                Họ và tên
                <span class="req">*</span>
              </label>

              <input maxlength="255"
                class="ss-input"
                v-model="f.name"
              />
            </div>

            <div class="ss-field">
              <label class="ss-label">
                Email
                <span class="req">*</span>
              </label>

              <input maxlength="254"
                class="ss-input"
                type="email"
                v-model="f.email"
              />
            </div>

            <div class="ss-field">
              <label class="ss-label">
                Số điện thoại
              </label>

              <input maxlength="10"
                class="ss-input"
                v-model="f.phone"
              />
            </div>

            <div class="ss-field">
              <label class="ss-label">
                Giới tính
              </label>

              <span class="khach-hang-select-field"><Select2KhachHang :disabled="saving" :allow-clear="false" aria-label="Giới tính" v-model="f.gender">
                <option value="">
                  -- Chọn --
                </option>

                <option :value="1">
                  Nam
                </option>

                <option :value="2">
                  Nữ
                </option>

                <option :value="0">
                  Khác
                </option>
              </Select2KhachHang></span>
            </div>

            <div class="ss-field">
              <label class="ss-label">
                Ngày sinh
              </label>

              <input
                class="ss-input"
                type="date"
                v-model="f.dob"
              />
            </div>
          </div>

          <p
            v-if="err"
            class="ss-hint warn"
          >
            <i class="bi bi-exclamation-triangle"></i>
            {{ err }}
          </p>

          <div class="ss-actions left">
            <button
              class="ss-btn primary"
              :disabled="saving"
              @click="save"
            >
              {{
                saving
                  ? 'Đang lưu...'
                  : 'Lưu thay đổi'
              }}
            </button>

            <button
              class="ss-btn"
              @click="$emit('close')"
            >
              Hủy
            </button>
          </div>
        </div>
      </fieldset>
    </section>
  </div>
</template>

<style scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, .48);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 1050;
}

.modal-card {
  width: min(980px, 100%);
  max-height: 92vh;
  overflow: auto;
  background: #fff;
  border-radius: 18px;
  padding: 24px;
  box-shadow: 0 24px 70px rgba(15, 23, 42, .24);
}

.modal-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.modal-head h2 {
  margin: 0;
}

.modal-head p {
  margin: 5px 0 0;
  color: #64748b;
}

.edit-layout {
  border: 0;
  padding: 0;
  margin: 0;
  min-width: 0;
  display: flex;
  gap: 22px;
  align-items: flex-start;
}

.grow {
  flex: 1;
}

@media(max-width: 760px) {
  .edit-layout {
    flex-direction: column;
  }
}
</style>
