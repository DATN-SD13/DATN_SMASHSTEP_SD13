<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import AvatarCard from '../../../../components/AvatarCard.vue'
import { persistAvatar } from '../../../../utils/avatar'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  provinceNames,
  wardsOf
} from '../../../../utils/address'
import customerService from '../services/customerService'
import { showSuccess, showError } from '../../../../utils/feedback'

const router = useRouter()

const f = ref({
  name: '',
  email: '',
  phone: '',
  gender: '',
  dob: '',
  image: '',

  rName: '',
  rPhone: '',

  province: '',
  ward: '',
  street: '',

  addressType: 1
})

const wards = computed(
  () => wardsOf(f.value.province)
)

const err = ref('')
const saving = ref(false)

const errorMessage = (e) =>
  e?.response?.data?.message || e?.message ||
  'Không thể tạo khách hàng, vui lòng thử lại.'

async function save() {
  if (saving.value) return
  const v = f.value

  if (
    !v.name.trim() ||
    !v.email.trim() ||
    !v.rName.trim() ||
    !v.rPhone.trim() ||
    !v.province ||
    !v.ward ||
    !v.street.trim()
  ) {
    err.value =
      'Vui lòng nhập đầy đủ các trường bắt buộc (*).'
    return
  }

  if (
    !/^\S+@\S+\.\S+$/.test(v.email.trim())
  ) {
    err.value =
      'Email không hợp lệ.'
    return
  }

  if (
    v.phone &&
    !/^0\d{9}$/.test(v.phone.trim())
  ) {
    err.value =
      'Số điện thoại khách hàng phải gồm 10 số và bắt đầu bằng 0.'
    return
  }

  if (
    !/^0\d{9}$/.test(v.rPhone.trim())
  ) {
    err.value =
      'Số điện thoại người nhận phải gồm 10 số và bắt đầu bằng 0.'
    return
  }

  if (
    v.dob &&
    new Date(v.dob) > new Date()
  ) {
    err.value =
      'Ngày sinh không được lớn hơn ngày hiện tại.'
    return
  }

  saving.value = true
  err.value = ''

  try {
    const image = await persistAvatar(v.image)
    v.image = image || ''

    await customerService.createCustomer({
      name: v.name.trim(),

      email: v.email.trim(),

      phone:
        v.phone.trim() || null,

      gender:
        v.gender === ''
          ? null
          : Number(v.gender),

      dob:
        v.dob || null,

      image,

      defaultAddress: {
        receiverName:
          v.rName.trim(),

        receiverPhone:
          v.rPhone.trim(),

        province:
          v.province,

        district:
          null,

        ward:
          v.ward,

        street:
          v.street.trim(),

        addressType:
          Number(v.addressType),

        isDefault:
          true
      }
    })

    showSuccess(
      'Đã tạo khách hàng thành công'
    )

    router.push('/khach-hang')

  } catch (e) {
    err.value = errorMessage(e)
    showError(err.value)

  } finally {
    saving.value = false
  }
}
</script>

<template>
  <AdminLayout>
    <main class="ss-page">

      <div>
        <button
          class="ss-back"
          aria-label="Quay lại"
          @click="router.push('/khach-hang')"
        >
          <i class="bi bi-arrow-left"></i>
        </button>
      </div>

      <div class="ss-split">

        <AvatarCard
          v-model:image="f.image"
          :disabled="saving"
          :name="f.name"
          :email="f.email"
          fallback="KH"
        />

        <div class="ss-stack">

          <section class="ss-card ss-form">

            <div class="ss-head">

              <div class="ss-head-icon">
                <i class="bi bi-person"></i>
              </div>

              <div>
                <h2>
                  Thông tin cơ bản
                </h2>

                <p>
                  Họ tên, email, liên hệ
                  và tài khoản.
                </p>
              </div>

            </div>

            <div class="ss-grid2">

              <div class="ss-field">
                <label class="ss-label">
                  Họ và tên
                  <span class="req">*</span>
                </label>

                <input
                  class="ss-input"
                  v-model="f.name"
                  placeholder="Nhập họ tên"
                />
              </div>

              <div class="ss-field">
                <label class="ss-label">
                  Email
                  <span class="req">*</span>
                </label>

                <input
                  class="ss-input"
                  type="email"
                  v-model="f.email"
                  placeholder="Nhập email"
                />
              </div>

              <div class="ss-field">
                <label class="ss-label">
                  Số điện thoại
                </label>

                <input
                  class="ss-input"
                  v-model="f.phone"
                  placeholder="VD: 0901234567"
                />
              </div>

              <div class="ss-field">

                <label class="ss-label">
                  Giới tính
                </label>

                <select
                  class="ss-select"
                  v-model="f.gender"
                >
                  <option value="">
                    -- Chọn giới tính --
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

                </select>

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

          </section>

          <section class="ss-card ss-form">

            <div class="ss-head">

              <div class="ss-head-icon">
                <i class="bi bi-geo-alt"></i>
              </div>

              <div>
                <h2>
                  Địa chỉ giao hàng
                </h2>

                <p>
                  Địa chỉ mặc định của
                  khách hàng mới.
                </p>
              </div>

            </div>

            <div class="ss-grid2">

              <div class="ss-field">

                <label class="ss-label">
                  Họ tên người nhận
                  <span class="req">*</span>
                </label>

                <input
                  class="ss-input"
                  v-model="f.rName"
                  placeholder="Họ tên người nhận"
                />

              </div>

              <div class="ss-field">

                <label class="ss-label">
                  Số điện thoại
                  <span class="req">*</span>
                </label>

                <input
                  class="ss-input"
                  v-model="f.rPhone"
                  placeholder="VD: 0901234567"
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
                    ? 'Đang tạo...'
                    : 'Tạo khách hàng'
                }}
              </button>

              <button
                class="ss-btn"
                @click="router.push('/khach-hang')"
              >
                Hủy
              </button>

            </div>

          </section>

        </div>
      </div>
    </main>
  </AdminLayout>
</template>
