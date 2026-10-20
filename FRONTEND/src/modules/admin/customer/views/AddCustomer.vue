<script setup>
import Select2KhachHang from '../components/Select2KhachHang.vue'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import AvatarCard from '../../../../components/AvatarCard.vue'
import { persistAvatar } from '../../../../utils/avatar'
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAddressApi } from '../../../../composables/useAddressApi'
import customerService from '../services/customerService'
import { confirmAction, showSuccess, showError } from '../../../../utils/feedback'

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

const { provinces, wards, loadingProvinces, loadingWards, addressError, changeProvince, resetAddress } = useAddressApi(f)
// Use customer details for the default recipient until the user enters a different recipient.
watch(() => f.value.name, (name, previous) => {
  if (!f.value.rName || f.value.rName === previous) f.value.rName = name
})
watch(() => f.value.phone, (phone, previous) => {
  if (!f.value.rPhone || f.value.rPhone === previous) f.value.rPhone = phone
})


const err = ref('')
const saving = ref(false)

const errorMessage = (e) =>
  e?.response?.data?.message || e?.message ||
  'Không thể tạo khách hàng, vui lòng thử lại.'

function getMaxDob() {
  const today = new Date()

  const date = new Date(
    today.getFullYear() - 15,
    today.getMonth(),
    today.getDate()
  )

  const year = date.getFullYear()

  const month = String(
    date.getMonth() + 1
  ).padStart(2, '0')

  const day = String(
    date.getDate()
  ).padStart(2, '0')

  return `${year}-${month}-${day}`
}

const maxDob = getMaxDob()

function validateAge(dobValue) {
  if (!dobValue) {
    return false
  }

  const dob = new Date(
    `${dobValue}T00:00:00`
  )

  if (
    Number.isNaN(
      dob.getTime()
    )
  ) {
    return false
  }

  const today = new Date()

  const minimumDob = new Date(
    today.getFullYear() - 15,
    today.getMonth(),
    today.getDate()
  )

  dob.setHours(
    0,
    0,
    0,
    0
  )

  minimumDob.setHours(
    0,
    0,
    0,
    0
  )

  return dob <= minimumDob
}

/* ================================
   ERROR
-------------------------------- */

async function save() {
  if (saving.value) return
  const v = f.value

  if (
    !v.name.trim() ||
    !v.email.trim() ||
    !v.phone.trim() ||
    !v.dob ||
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

  if (!validateAge(v.dob)) {
    err.value = 'Khách hàng phải đủ 15 tuổi trở lên.'
    showError(err.value)
    return
  }
  if (v.gender !== '' && ![0, 1, 2].includes(Number(v.gender))) {
    err.value = 'Giới tính không hợp lệ.'; return
  }
  if (![1, 2].includes(Number(v.addressType))) {
    err.value = 'Loại địa chỉ không hợp lệ.'; return
  }
  confirmAction('Xác nhận thêm khách hàng?', async () => {
  if (saving.value) return
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
  })
}
</script>

<template>
  <AdminLayout>
    <main class="ss-page">

      <div>
    <p v-if="addressError" class="ss-alert error" role="alert">{{ addressError }}</p>
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

                <input maxlength="255"
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

                <input maxlength="254"
                  class="ss-input"
                  type="email"
                  v-model="f.email"
                  placeholder="Nhập email"
                />
              </div>

              <div class="ss-field">
                <label class="ss-label">
                  Số điện thoại <span class="req">*</span>
                </label>

                <input maxlength="10"
                  class="ss-input"
                  v-model="f.phone"
                  placeholder="VD: 0901234567"
                />
              </div>

              <div class="ss-field">

                <label class="ss-label">
                  Giới tính
                </label>

                <span class="khach-hang-select-field"><Select2KhachHang :allow-clear="false" aria-label="Giới tính" v-model="f.gender">
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

                </Select2KhachHang></span>

              </div>

              <div class="ss-field">

                <label class="ss-label">
                  Ngày sinh <span class="req">*</span>
                </label>

                <input
                  class="ss-input"
                  type="date"
                  v-model="f.dob" :max="maxDob"
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

                <input maxlength="255"
                  class="ss-input"
                  v-model="f.rName"
                  placeholder="Họ tên người nhận"
                />

              </div>

              <div class="ss-field">

                <label class="ss-label">
                  Số điện thoại <span class="req">*</span>
                  <span class="req">*</span>
                </label>

                <input maxlength="10"
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

                <span class="khach-hang-select-field"><Select2KhachHang :allow-clear="false" aria-label="Tỉnh/Thành phố" v-model="f.province" :disabled="loadingProvinces" @change="changeProvince">
              <option value="">Chọn tỉnh/thành</option>
              <option v-for="p in provinces" :key="p.code" :value="p.name">{{ p.name }}</option>
            </Select2KhachHang></span>

              </div>

              <div class="ss-field">

                <label class="ss-label">
                  Phường/Xã
                  <span class="req">*</span>
                </label>

                <span class="khach-hang-select-field"><Select2KhachHang :allow-clear="false" aria-label="Phường/Xã" v-model="f.ward" :disabled="loadingWards || !f.province">
              <option value="">Chọn phường/xã</option>
              <option v-for="w in wards" :key="w.code" :value="w.name">{{ w.name }}</option>
            </Select2KhachHang></span>

              </div>

              <div class="ss-field">

                <label class="ss-label">
                  Loại địa chỉ
                </label>

                <span class="khach-hang-select-field"><Select2KhachHang :allow-clear="false" aria-label="Loại địa chỉ" v-model="f.addressType">

                  <option :value="1">
                    Nhà riêng
                  </option>

                  <option :value="2">
                    Văn phòng
                  </option>

                </Select2KhachHang></span>

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
