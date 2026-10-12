<script setup>
import {
  computed,
  onMounted,
  ref
} from 'vue'

import { useAddressApi } from '../../../../composables/useAddressApi'
import AppToast
  from '../../../../components/AppToast.vue'

import ConfirmModal
  from '../../../../components/ConfirmModal.vue'

import {
  useToast
} from '../../../../composables/useToast'

import customerService
  from '../services/customerService'
  const props = defineProps({
  customer: {
    type: Object,
    required: true
  }
})

const {
  toast,
  success,
  error: showError,
  closeToast
} = useToast()

const confirmModal = ref({
  show: false,
  title: '',
  message: '',
  action: null
})

const confirmLoading = ref(false)

function closeConfirm() {
  if (confirmLoading.value) {
    return
  }

  confirmModal.value.show = false
  confirmModal.value.action = null
}

async function executeConfirm() {
  if (!confirmModal.value.action) {
    return
  }

  confirmLoading.value = true

  try {
    await confirmModal.value.action()
  } finally {
    confirmLoading.value = false
    confirmModal.value.show = false
    confirmModal.value.action = null
  }
}

const emit = defineEmits([
  'close',
  'changed'
])

const addresses = ref([])
const loading = ref(false)
const saving = ref(false)
const err = ref('')
const editingId = ref(null)


let toastTimer = null

function showToast(message, type = 'success') {
  if (toastTimer) {
    clearTimeout(toastTimer)
  }

  toast.value = {
    show: true,
    message,
    type
  }

  toastTimer = setTimeout(() => {
    toast.value.show = false
  }, 3000)
}

const emptyForm = () => ({
  receiverName: '',
  receiverPhone: '',
  province: '',
  district: '',
  ward: '',
  street: '',
  addressType: 1,
  isDefault: false
})

const f = ref(
  emptyForm()
)

const {
  provinces,
  wards,

  provinceCode,
  wardCode,

  loadingProvinces,
  loadingWards,

  addressError,

  changeProvince,
  changeWard,
  restoreAddress,
  resetAddress
} = useAddressApi(f)

const errorMessage = (e) =>
  e?.response?.data?.message ||
  'Có lỗi xảy ra, vui lòng thử lại.'

async function load() {
  loading.value = true
  err.value = ''

  try {
    addresses.value =
      await customerService.getAddresses(
        props.customer.code
      ) || []
  } catch (e) {
    err.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null

  f.value = emptyForm()

  resetAddress()

  err.value = ''
}

function edit(a) {
  editingId.value = a.id

  f.value = {
    receiverName: a.receiverName || '',
    receiverPhone: a.receiverPhone || '',

    province: a.province || '',
    district: a.district || '',
    ward: a.ward || '',

    street: a.street || '',

    addressType: a.addressType ?? 1,
    isDefault: !!a.isDefault
  }

  restoreAddress()
}

async function save() {
  const v = f.value

  if (
    !v.receiverName.trim() ||
    !v.receiverPhone.trim() ||
    !v.province ||
    !v.ward ||
    !v.street.trim()
  ) {
    const message =
      'Vui lòng nhập đầy đủ các trường bắt buộc.'

    err.value = message

    showError(
      message,
      'Dữ liệu không hợp lệ'
    )

    return
  }

  if (
    !/^0\d{9}$/.test(
      v.receiverPhone.trim()
    )
  ) {
    const message =
      'Số điện thoại phải gồm 10 số và bắt đầu bằng 0.'

    err.value = message

    showError(
      message,
      'Dữ liệu không hợp lệ'
    )

    return
  }

  saving.value = true
  err.value = ''

  try {
    const payload = {
      receiverName:
        v.receiverName.trim(),

      receiverPhone:
        v.receiverPhone.trim(),

      province:
        v.province,

      district: null,

      ward:
        v.ward,

      street:
        v.street.trim(),

      addressType:
        Number(
          v.addressType
        ),

      isDefault:
        !!v.isDefault
    }

    const isEditing =
      !!editingId.value

    if (isEditing) {
      await customerService
        .updateAddress(
          props.customer.code,
          editingId.value,
          payload
        )
    } else {
      await customerService
        .createAddress(
          props.customer.code,
          payload
        )
    }

    resetForm()

    await load()

    success(
      isEditing
        ? 'Cập nhật địa chỉ thành công'
        : 'Thêm địa chỉ thành công',

      isEditing
        ? 'Cập nhật thành công'
        : 'Thêm thành công'
    )

    emit('changed')

  } catch (e) {
    const message =
      errorMessage(e)

    err.value = message

    showError(
      message,
      'Thao tác thất bại'
    )

  } finally {
    saving.value = false
  }
}

async function makeDefault(a) {
  if (a.isDefault) {
    return
  }

  try {
    await customerService
      .setDefaultAddress(
        props.customer.code,
        a.id
      )

    await load()

    success(
      'Đặt địa chỉ mặc định thành công',
      'Cập nhật thành công'
    )

    emit('changed')

  } catch (e) {
    const message =
      errorMessage(e)

    err.value = message

    showError(
      message,
      'Cập nhật thất bại'
    )
  }
}

function stop(a) {
  confirmModal.value = {
    show: true,

    title:
      'Xác nhận cập nhật',

    message:
      `Bạn có chắc chắn muốn ngừng sử dụng địa chỉ của ${a.receiverName || 'khách hàng'} không?`,

    action: async () => {
      try {
        await customerService
          .stopAddress(
            props.customer.code,
            a.id
          )

        if (
          editingId.value ===
          a.id
        ) {
          resetForm()
        }

        await load()

        success(
          'Ngừng sử dụng địa chỉ thành công',
          'Cập nhật thành công'
        )

        emit('changed')

      } catch (e) {
        const message =
          errorMessage(e)

        err.value = message

        showError(
          message,
          'Cập nhật thất bại'
        )
      }
    }
  }
}

onMounted(load)
</script>

<template>

  <AppToast
  :show="toast.show"
  :type="toast.type"
  :title="toast.title"
  :message="toast.message"
  @close="closeToast"
/>

<ConfirmModal
  :show="confirmModal.show"
  :title="confirmModal.title"
  :message="confirmModal.message"
  :loading="confirmLoading"
  confirm-text="Đồng ý"
  cancel-text="Hủy"
  @confirm="executeConfirm"
  @cancel="closeConfirm"
/>

  
  <div
    class="modal-mask"
    @click.self="$emit('close')"
  >
    <section class="modal-card">

      <div class="modal-head">
        <div>
          <h2>
            Địa chỉ khách hàng
          </h2>

          <p>
            {{ customer.code }}
            ·
            {{ customer.name }}
          </p>
        </div>

        <button
          class="ss-icon-btn"
          @click="$emit('close')"
        >
          <i class="bi bi-x-lg"></i>
        </button>
      </div>

      <div class="address-list">

        <div
          v-if="loading"
          class="ss-empty"
        >
          Đang tải địa chỉ...
        </div>

        <div
          v-for="a in addresses"
          :key="a.id"
          class="address-item"
          :class="{
            selected:
              editingId === a.id
          }"
        >
          <div class="grow">

            <div class="address-line">
              <strong>
                {{ a.receiverName }}
              </strong>

              <span>
                · {{ a.receiverPhone }}
              </span>

              <span
                v-if="a.isDefault"
                class="ss-pill success"
              >
                Mặc định
              </span>

              <span class="ss-pill">
                {{ a.addressTypeLabel }}
              </span>
            </div>

            <p>
              {{ a.fullAddress }}
            </p>
          </div>

          <div class="ss-row-actions">

            <button
              class="ss-icon-btn"
              title="Sửa"
              @click="edit(a)"
            >
              <i class="bi bi-pencil"></i>
            </button>

            <button
              class="ss-icon-btn"
              title="Đặt mặc định"
              :disabled="a.isDefault"
              @click="makeDefault(a)"
            >
              <i class="bi bi-star"></i>
            </button>

            <button
              class="ss-icon-btn danger"
              title="Ngừng sử dụng"
              @click="stop(a)"
            >
              <i class="bi bi-trash"></i>
            </button>

          </div>
        </div>

        <div
          v-if="
            !loading
            && !addresses.length
          "
          class="ss-empty"
        >
          Khách hàng chưa có địa chỉ.
        </div>

      </div>

      <div class="form-box ss-form">

        <div class="form-title">
          {{
            editingId
              ? 'Cập nhật địa chỉ'
              : 'Thêm địa chỉ mới'
          }}
        </div>

        <div class="ss-grid2">

          <div class="ss-field">

            <label class="ss-label">
              Người nhận
              <span class="req">*</span>
            </label>

            <input
              class="ss-input"
              v-model="f.receiverName"
              placeholder="Nhập tên người nhận"
            />
          </div>

          <div class="ss-field">

            <label class="ss-label">
              Số điện thoại
              <span class="req">*</span>
            </label>

            <input
              class="ss-input"
              v-model="f.receiverPhone"
              placeholder="0901234567"
            />
          </div>

          <div class="ss-field">

  <label class="ss-label">
    Tỉnh/Thành phố
    <span class="req">*</span>
  </label>

  <select
    class="ss-select"
    v-model="provinceCode"
    :disabled="loadingProvinces"
    @change="changeProvince"
  >

    <option value="">
      -- Chọn tỉnh/thành --
    </option>

    <option
      v-for="p in provinces"
      :key="p.code"
      :value="String(p.code)"
    >
      {{ p.name }}
    </option>

  </select>

</div>

          <div class="ss-field">

  <label class="ss-label">
    Phường/Xã
    <span class="req">*</span>
  </label>

  <select
    class="ss-select"
    v-model="wardCode"
    :disabled="!provinceCode || loadingWards"
    @change="changeWard"
  >

    <option value="">
      -- Chọn phường/xã --
    </option>

    <option
      v-for="w in wards"
      :key="w.code"
      :value="String(w.code)"
    >
      {{ w.name }}
    </option>

  </select>

</div>

<p v-if="addressError" class="text-danger">
  {{ addressError }}
</p>

          <!-- <div class="ss-field">

            <label class="ss-label">
              Quận/Huyện
            </label>

            <input
              class="ss-input"
              v-model="f.district"
              placeholder="Không bắt buộc"
            />
          </div> -->

          <div class="ss-field">

            <label class="ss-label">
              Loại địa chỉ
            </label>

            <select
              class="ss-select"
              v-model="f.addressType"
            >
              <option :value="1">
                Nhà riêng
              </option>

              <option :value="2">
                Văn phòng
              </option>
            </select>
          </div>

          <div class="ss-field full">

            <label class="ss-label">
              Địa chỉ cụ thể
              <span class="req">*</span>
            </label>

            <input
              class="ss-input"
              v-model="f.street"
              placeholder="Số nhà, tên đường..."
            />
          </div>
          

          <div class="ss-field check-field">

            <label>
              <input
                type="checkbox"
                v-model="f.isDefault"
              />

              Đặt làm địa chỉ mặc định
            </label>
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
                : (
                  editingId
                    ? 'Cập nhật'
                    : 'Thêm địa chỉ'
                )
            }}
          </button>

          <button
            v-if="editingId"
            class="ss-btn"
            @click="resetForm"
          >
            Hủy sửa
          </button>

        </div>

      </div>

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
  width: min(920px, 100%);
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
  margin-bottom: 18px;
}

.modal-head h2 {
  margin: 0;
}

.modal-head p {
  margin: 5px 0 0;
  color: #64748b;
}

.address-list {
  display: grid;
  gap: 10px;
}

.address-item {
  display: flex;
  gap: 14px;
  align-items: center;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 14px;
}

.address-item.selected {
  border-color: #94a3b8;
  background: #f8fafc;
}

.grow {
  flex: 1;
}

.address-line {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.address-item p {
  margin: 5px 0 0;
  color: #64748b;
}

.form-box {
  margin-top: 18px;
  border-top: 1px solid #e2e8f0;
  padding-top: 18px;
}

.form-title {
  font-weight: 800;
  margin-bottom: 14px;
}

.check-field {
  display: flex;
  align-items: end;
  padding-bottom: 9px;
}

.check-field label {
  display: flex;
  gap: 8px;
  align-items: center;
}

@media(max-width: 640px) {
  .address-item {
    align-items: flex-start;
    flex-direction: column;
  }
}

/* ================================
   TOAST NOTIFICATION
================================ */

.app-toast {
  position: fixed;
  top: 24px;
  right: 24px;

  z-index: 99999;

  min-width: 330px;
  max-width: 430px;

  display: flex;
  align-items: center;
  gap: 12px;

  padding: 14px 16px;

  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;

  box-shadow:
    0 12px 35px rgba(15, 23, 42, 0.16);

  color: #0f172a;
}

.app-toast.success {
  border-left: 5px solid #22c55e;
}

.app-toast.error {
  border-left: 5px solid #ef4444;
}

.toast-icon {
  width: 38px;
  height: 38px;

  flex-shrink: 0;

  display: flex;
  align-items: center;
  justify-content: center;

  border-radius: 50%;

  font-size: 18px;
}

.success .toast-icon {
  color: #15803d;
  background: #dcfce7;
}

.error .toast-icon {
  color: #dc2626;
  background: #fee2e2;
}

.toast-content {
  flex: 1;

  display: flex;
  flex-direction: column;

  gap: 2px;
}

.toast-content strong {
  font-size: 14px;
  font-weight: 700;
}

.toast-content span {
  font-size: 13px;
  color: #64748b;
}

.toast-close {
  width: 30px;
  height: 30px;

  display: flex;
  align-items: center;
  justify-content: center;

  border: 0;
  border-radius: 8px;

  background: transparent;
  color: #94a3b8;

  cursor: pointer;
}

.toast-close:hover {
  background: #f1f5f9;
  color: #475569;
}


/* Animation */

.toast-enter-active,
.toast-leave-active {
  transition:
    opacity 0.25s ease,
    transform 0.25s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(30px);
}


/* Mobile */

@media (max-width: 640px) {
  .app-toast {
    top: 16px;
    left: 16px;
    right: 16px;

    min-width: 0;
    max-width: none;
  }
}
</style>