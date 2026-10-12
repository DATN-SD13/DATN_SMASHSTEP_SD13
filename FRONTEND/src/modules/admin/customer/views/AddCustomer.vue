<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import AvatarCard from '../../../../components/AvatarCard.vue'

import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { useAddressApi } from '../../../../composables/useAddressApi'

import customerService from '../services/customerService'

import AppToast from '../../../../components/AppToast.vue'
import ConfirmModal from '../../../../components/ConfirmModal.vue'
import { useToast } from '../../../../composables/useToast'

const router = useRouter()

/* ================================
   TOAST
================================ */

const {
  toast,
  success,
  error: showError,
  closeToast
} = useToast()

/* ================================
   CONFIRM
================================ */

const confirmModal = ref({
  show: false,
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

/* ================================
   FORM
================================ */

const f = ref({
  name: '',
  email: '',
  phone: '',
  gender: '',
  dob: '',
  image: '',

  province: '',
  ward: '',
  street: '',

  /*
   * 1 = Nhà riêng
   * 2 = Văn phòng
   */
  addressType: 1
})

/* ================================
   ADDRESS API
================================ */

const {
  provinces,
  wards,

  provinceCode,
  wardCode,

  loadingProvinces,
  loadingWards,

  addressError,

  changeProvince,
  changeWard
} = useAddressApi(f)

/* ================================
   STATE
================================ */

const err = ref('')
const saving = ref(false)

/* ================================
   GIỚI HẠN TUỔI
================================ */

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
================================ */

function errorMessage(e) {
  const response =
    e?.response?.data

  if (
    response?.data &&
    typeof response.data === 'object' &&
    !Array.isArray(response.data)
  ) {
    const messages =
      Object.values(
        response.data
      ).filter(Boolean)

    if (messages.length) {
      return messages.join(', ')
    }
  }

  return (
    response?.message ||
    e?.message ||
    'Không thể tạo khách hàng, vui lòng thử lại.'
  )
}

/* ================================
   SAVE
================================ */

async function save() {
  const v = f.value

  err.value = ''

  /*
   * Trường bắt buộc
   */
  if (
    !v.name.trim() ||
    !v.email.trim() ||
    !v.phone.trim() ||
    !v.dob ||
    !v.province ||
    !v.ward ||
    !v.street.trim()
  ) {
    const message =
      'Vui lòng nhập đầy đủ các trường bắt buộc (*).'

    err.value = message

    showError(
      message,
      'Dữ liệu không hợp lệ'
    )

    return
  }

  /*
   * Email
   */
  if (
    !/^\S+@\S+\.\S+$/.test(
      v.email.trim()
    )
  ) {
    const message =
      'Email không hợp lệ.'

    err.value = message

    showError(
      message,
      'Dữ liệu không hợp lệ'
    )

    return
  }

  /*
   * Số điện thoại
   */
  if (
    !/^0\d{9}$/.test(
      v.phone.trim()
    )
  ) {
    const message =
      'Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0.'

    err.value = message

    showError(
      message,
      'Dữ liệu không hợp lệ'
    )

    return
  }

  /*
   * Giới tính
   */
  if (
    v.gender !== '' &&
    ![0, 1, 2].includes(
      Number(v.gender)
    )
  ) {
    const message =
      'Giới tính không hợp lệ.'

    err.value = message

    showError(
      message,
      'Dữ liệu không hợp lệ'
    )

    return
  }

  /*
   * Tuổi >= 15
   */
  if (
    !validateAge(
      v.dob
    )
  ) {
    const message =
      'Khách hàng phải đủ 15 tuổi trở lên.'

    err.value = message

    showError(
      message,
      'Không đủ tuổi'
    )

    return
  }

  /*
   * Loại địa chỉ
   */
  if (
    ![1, 2].includes(
      Number(v.addressType)
    )
  ) {
    const message =
      'Loại địa chỉ không hợp lệ.'

    err.value = message

    showError(
      message,
      'Dữ liệu không hợp lệ'
    )

    return
  }

  /*
   * Hiện popup xác nhận
   */
  confirmModal.value = {
    show: true,

    action: async () => {
      saving.value = true

      try {
        /*
         * Backend hiện không lưu base64
         */
        const image =
          v.image?.startsWith(
            'data:image/'
          )
            ? null
            : v.image || null

        const payload = {
          name:
            v.name.trim(),

          email:
            v.email.trim(),

          phone:
            v.phone.trim(),

          gender:
            v.gender === ''
              ? null
              : Number(
                  v.gender
                ),

          dob:
            v.dob,

          image,

          /*
           * Địa chỉ mặc định
           */
          defaultAddress: {
            /*
             * Người nhận luôn chính là khách hàng
             */
            receiverName:
              v.name.trim(),

            /*
             * SĐT nhận hàng luôn là SĐT khách hàng
             */
            receiverPhone:
              v.phone.trim(),

            province:
              v.province,

            /*
             * Địa chỉ hành chính 2 cấp
             */
            district: null,

            ward:
              v.ward,

            street:
              v.street.trim(),

            addressType:
              Number(
                v.addressType
              ),

            isDefault: true
          }
        }

        await customerService
          .createCustomer(
            payload
          )

        success(
          'Thêm khách hàng thành công',
          'Thêm thành công'
        )

        setTimeout(
          () => {
            router.push(
              '/khach-hang'
            )
          },
          1200
        )

      } catch (e) {
        console.error(e)

        const message =
          errorMessage(e)

        err.value = message

        showError(
          message,
          'Thêm thất bại'
        )

      } finally {
        saving.value = false
      }
    }
  }
}
</script>

<template>

  <!-- ============================
       TOAST
  ============================= -->

  <AppToast
    :show="toast.show"
    :type="toast.type"
    :title="toast.title"
    :message="toast.message"
    @close="closeToast"
  />

  <!-- ============================
       CONFIRM
  ============================= -->

  <ConfirmModal
    :show="confirmModal.show"
    title="Xác nhận thêm khách hàng"
    :message="
      `Bạn có chắc chắn muốn thêm khách hàng ${f.name || ''} không?`
    "
    :loading="confirmLoading"
    confirm-text="Đồng ý"
    cancel-text="Hủy"
    @confirm="executeConfirm"
    @cancel="closeConfirm"
  />

  <AdminLayout>

    <main class="ss-page">

      <!-- ======================
           BACK
      ======================= -->

      <div>
        <button
          class="ss-back"
          type="button"
          aria-label="Quay lại"
          @click="
            router.push(
              '/khach-hang'
            )
          "
        >
          <i
            class="bi bi-arrow-left"
          ></i>
        </button>
      </div>

      <div class="ss-split">

        <!-- ======================
             AVATAR
        ======================= -->

        <AvatarCard
          v-model:image="f.image"
          :name="f.name"
          :email="f.email"
          fallback="KH"
        />

        <div class="ss-stack">

          <!-- ======================
               THÔNG TIN CƠ BẢN
          ======================= -->

          <section
            class="ss-card ss-form"
          >

            <div class="ss-head">

              <div
                class="ss-head-icon"
              >
                <i
                  class="bi bi-person"
                ></i>
              </div>

              <div>
                <h2>
                  Thông tin cơ bản
                </h2>

                <p>
                  Họ tên, email, liên hệ
                  và thông tin khách hàng.
                </p>
              </div>

            </div>

            <div class="ss-grid2">

              <!-- HỌ TÊN -->

              <div class="ss-field">

                <label
                  class="ss-label"
                >
                  Họ và tên

                  <span
                    class="req"
                  >
                    *
                  </span>
                </label>

                <input
                  class="ss-input"
                  v-model="f.name"
                  placeholder="Nhập họ tên"
                />

              </div>

              <!-- EMAIL -->

              <div class="ss-field">

                <label
                  class="ss-label"
                >
                  Email

                  <span
                    class="req"
                  >
                    *
                  </span>
                </label>

                <input
                  class="ss-input"
                  type="email"
                  v-model="f.email"
                  placeholder="Nhập email"
                />

              </div>

              <!-- SỐ ĐIỆN THOẠI -->

              <div class="ss-field">

                <label
                  class="ss-label"
                >
                  Số điện thoại

                  <span
                    class="req"
                  >
                    *
                  </span>
                </label>

                <input
                  class="ss-input"
                  v-model="f.phone"
                  maxlength="10"
                  inputmode="numeric"
                  placeholder="VD: 0901234567"
                />

                <small
                  class="ss-hint"
                >
                  Số điện thoại này cũng
                  được sử dụng để nhận hàng.
                </small>

              </div>

              <!-- GIỚI TÍNH -->

              <div class="ss-field">

                <label
                  class="ss-label"
                >
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

              <!-- NGÀY SINH -->

              <div class="ss-field">

                <label
                  class="ss-label"
                >
                  Ngày sinh

                  <span
                    class="req"
                  >
                    *
                  </span>
                </label>

                <input
                  class="ss-input"
                  type="date"
                  v-model="f.dob"
                  :max="maxDob"
                />

                <small
                  class="ss-hint"
                >
                  Khách hàng phải đủ
                  15 tuổi trở lên.
                </small>

              </div>

            </div>

          </section>

          <!-- ======================
               ĐỊA CHỈ GIAO HÀNG
          ======================= -->

          <section
            class="ss-card ss-form"
          >

            <div class="ss-head">

              <div
                class="ss-head-icon"
              >
                <i
                  class="bi bi-geo-alt"
                ></i>
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

              <!-- ======================
                   HỌ TÊN NGƯỜI NHẬN
              ======================= -->

              <!-- HỌ TÊN NGƯỜI NHẬN -->
<div class="ss-field">
  <label class="ss-label">
    Họ tên người nhận
  </label>

  <input
    class="ss-input"
    :value="f.name"
    :placeholder="
      f.name
        ? ''
        : 'Họ tên người nhận'
    "
    disabled
  />
</div>

<!-- SỐ ĐIỆN THOẠI NHẬN HÀNG -->
<div class="ss-field">
  <label class="ss-label">
    Số điện thoại nhận hàng
  </label>

  <input
    class="ss-input"
    :value="f.phone"
    :placeholder="
      f.phone
        ? ''
        : 'VD: 0901234567'
    "
    disabled
  />
</div>

              <!-- ======================
                   TỈNH / THÀNH PHỐ
              ======================= -->

              <div class="ss-field">

                <label
                  class="ss-label"
                >
                  Tỉnh/Thành phố

                  <span
                    class="req"
                  >
                    *
                  </span>
                </label>

                <select
                  class="ss-select"
                  v-model="provinceCode"
                  :disabled="
                    loadingProvinces
                  "
                  @change="
                    changeProvince
                  "
                >

                  <option value="">
                    {{
                      loadingProvinces
                        ? 'Đang tải...'
                        : '-- Chọn tỉnh/thành --'
                    }}
                  </option>

                  <option
                    v-for="p in provinces"
                    :key="p.code"
                    :value="
                      String(
                        p.code
                      )
                    "
                  >
                    {{ p.name }}
                  </option>

                </select>

              </div>

              <!-- ======================
                   PHƯỜNG / XÃ
              ======================= -->

              <div class="ss-field">

                <label
                  class="ss-label"
                >
                  Phường/Xã

                  <span
                    class="req"
                  >
                    *
                  </span>
                </label>

                <select
                  class="ss-select"
                  v-model="wardCode"
                  :disabled="
                    !provinceCode ||
                    loadingWards
                  "
                  @change="
                    changeWard
                  "
                >

                  <option value="">
                    {{
                      loadingWards
                        ? 'Đang tải...'
                        : '-- Chọn phường/xã --'
                    }}
                  </option>

                  <option
                    v-for="w in wards"
                    :key="w.code"
                    :value="
                      String(
                        w.code
                      )
                    "
                  >
                    {{ w.name }}
                  </option>

                </select>

              </div>

              <!-- ======================
                   LỖI ADDRESS API
              ======================= -->

              <div
                v-if="addressError"
                class="ss-field full"
              >

                <p
                  class="ss-hint warn"
                >
                  <i
                    class="
                      bi
                      bi-exclamation-triangle
                    "
                  ></i>

                  {{ addressError }}
                </p>

              </div>

              <!-- ======================
                   LOẠI ĐỊA CHỈ
              ======================= -->

              <div class="ss-field">

                <label
                  class="ss-label"
                >
                  Loại địa chỉ
                </label>

                <select
                  class="ss-select"
                  v-model="
                    f.addressType
                  "
                >

                  <option :value="1">
                    Nhà riêng
                  </option>

                  <option :value="2">
                    Văn phòng
                  </option>

                </select>

              </div>

              <!-- ======================
                   ĐỊA CHỈ CỤ THỂ
              ======================= -->

              <div
                class="ss-field full"
              >

                <label
                  class="ss-label"
                >
                  Địa chỉ cụ thể

                  <span
                    class="req"
                  >
                    *
                  </span>
                </label>

                <input
                  class="ss-input"
                  v-model="f.street"
                  placeholder="
                    Số nhà, tên đường...
                  "
                />

              </div>

            </div>

            <!-- ======================
                 ERROR
            ======================= -->

            <p
              v-if="err"
              class="ss-hint warn"
            >
              <i
                class="
                  bi
                  bi-exclamation-triangle
                "
              ></i>

              {{ err }}
            </p>

            <!-- ======================
                 ACTIONS
            ======================= -->

            <div
              class="ss-actions left"
            >

              <button
                type="button"
                class="ss-btn primary"
                :disabled="saving"
                @click="save"
              >

                <span
                  v-if="saving"
                  class="
                    spinner-border
                    spinner-border-sm
                  "
                ></span>

                {{
                  saving
                    ? 'Đang tạo...'
                    : 'Tạo khách hàng'
                }}
              </button>

              <button
                type="button"
                class="ss-btn"
                :disabled="saving"
                @click="
                  router.push(
                    '/khach-hang'
                  )
                "
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