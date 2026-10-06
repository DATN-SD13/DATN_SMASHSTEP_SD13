<script setup>
import {
  computed,
  onMounted,
  ref
} from 'vue'

import {
  provinceNames,
  wardsOf
} from '../../../../utils/address'

import customerService
  from '../services/customerService'

const props = defineProps({
  customer: {
    type: Object,
    required: true
  }
})

const emit = defineEmits([
  'close',
  'changed'
])

const addresses = ref([])
const loading = ref(false)
const saving = ref(false)
const err = ref('')
const editingId = ref(null)

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

const wards = computed(
  () => wardsOf(
    f.value.province
  )
)

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
  err.value = ''
}

function edit(a) {
  editingId.value = a.id

  f.value = {
    receiverName:
      a.receiverName || '',

    receiverPhone:
      a.receiverPhone || '',

    province:
      a.province || '',

    district:
      a.district || '',

    ward:
      a.ward || '',

    street:
      a.street || '',

    addressType:
      a.addressType ?? 1,

    isDefault:
      !!a.isDefault
  }
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
    err.value =
      'Vui lòng nhập đầy đủ các trường bắt buộc.'
    return
  }

  if (
    !/^0\d{9}$/.test(
      v.receiverPhone.trim()
    )
  ) {
    err.value =
      'Số điện thoại phải gồm 10 số và bắt đầu bằng 0.'
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

      district:
        v.district?.trim() || null,

      ward:
        v.ward,

      street:
        v.street.trim(),

      addressType:
        Number(v.addressType),

      isDefault:
        !!v.isDefault
    }

    if (editingId.value) {
      await customerService.updateAddress(
        props.customer.code,
        editingId.value,
        payload
      )
    } else {
      await customerService.createAddress(
        props.customer.code,
        payload
      )
    }

    resetForm()

    await load()

    emit('changed')
  } catch (e) {
    err.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}

async function makeDefault(a) {
  if (a.isDefault) {
    return
  }

  try {
    await customerService.setDefaultAddress(
      props.customer.code,
      a.id
    )

    await load()

    emit('changed')
  } catch (e) {
    err.value = errorMessage(e)
  }
}

async function stop(a) {
  if (
    !confirm(
      'Ngừng sử dụng địa chỉ này?'
    )
  ) {
    return
  }

  try {
    await customerService.stopAddress(
      props.customer.code,
      a.id
    )

    if (
      editingId.value === a.id
    ) {
      resetForm()
    }

    await load()

    emit('changed')
  } catch (e) {
    err.value = errorMessage(e)
  }
}

onMounted(load)
</script>

<template>
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
              v-model="f.province"
              @change="f.ward = ''"
            >
              <option value="">
                -- Chọn tỉnh/thành --
              </option>

              <option
                v-if="
                  f.province
                  && !provinceNames.includes(
                    f.province
                  )
                "
                :value="f.province"
              >
                {{ f.province }}
              </option>

              <option
                v-for="p in provinceNames"
                :key="p"
                :value="p"
              >
                {{ p }}
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
              v-model="f.ward"
              :disabled="!f.province"
            >
              <option value="">
                -- Chọn phường/xã --
              </option>

              <option
                v-if="
                  f.ward
                  && !wards.includes(
                    f.ward
                  )
                "
                :value="f.ward"
              >
                {{ f.ward }}
              </option>

              <option
                v-for="w in wards"
                :key="w"
                :value="w"
              >
                {{ w }}
              </option>
            </select>
          </div>

          <div class="ss-field">

            <label class="ss-label">
              Quận/Huyện
            </label>

            <input
              class="ss-input"
              v-model="f.district"
              placeholder="Không bắt buộc"
            />
          </div>

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
</style>