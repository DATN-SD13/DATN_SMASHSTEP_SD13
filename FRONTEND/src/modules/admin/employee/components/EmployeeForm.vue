<script setup>
import AvatarCard from '../../../../components/AvatarCard.vue'
import employeeService from '../services/employeeService'
import CccdScanner from './CccdScanner.vue'
import AppToast from '../../../../components/AppToast.vue'
import ConfirmModal from '../../../../components/ConfirmModal.vue'
import { useToast } from '../../../../composables/useToast'

import {
  computed,
  onMounted,
  ref
} from 'vue'

import { useRouter } from 'vue-router'

import { useAddressApi } from '../../../../composables/useAddressApi'

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

function askConfirm({
  title = 'Xác nhận',
  message,
  action
}) {
  confirmModal.value = {
    show: true,
    title,
    message,
    action
  }
}

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

    confirmModal.value.show = false
    confirmModal.value.action = null
  } finally {
    confirmLoading.value = false
  }
}

const props = defineProps({
  mode: {
    type: String,
    default: 'create'
  },

  employee: {
    type: Object,
    default: null
  }
})

const router = useRouter()

const edit = computed(
  () => props.mode === 'edit'
)

const roles = ref([])
const saving = ref(false)

const maxDob = computed(() => {
  const date = new Date()
  date.setFullYear(date.getFullYear() - 18)

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
})

const active = ref(
  props.employee?.active ?? true
)

const f = ref({
  name: props.employee?.name ?? '',

  email: props.employee?.email ?? '',

  phone: props.employee?.phone ?? '',

  roleId:
    props.employee?.roleId != null
      ? String(props.employee.roleId)
      : '',

  gender:
    props.employee?.gender != null
      ? String(props.employee.gender)
      : '1',

  dob: props.employee?.dob ?? '',

  province:
    props.employee?.province ?? '',

  ward:
    props.employee?.ward ?? '',

  street:
    props.employee?.street ?? '',

  image:
    props.employee?.image ?? ''
})

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

const err = ref('')
const msg = ref('')
const showCccdScanner = ref(false)

function getErrorMessage(error) {
  const response = error?.response?.data

  if (!response) {
    return (
      error?.message ||
      'Có lỗi xảy ra. Vui lòng thử lại.'
    )
  }

  /*
   * GlobalExceptionHandler có thể trả data
   * là object chứa lỗi validation.
   */
  if (
    response.data &&
    typeof response.data === 'object' &&
    !Array.isArray(response.data)
  ) {
    const messages =
      Object.values(response.data)
        .filter(Boolean)

    if (messages.length) {
      return messages.join(', ')
    }
  }

  return (
    response.message ||
    'Có lỗi xảy ra. Vui lòng thử lại.'
  )
}

async function loadRoles() {
  try {
    const data =
      await employeeService.getRoles()

    roles.value =
      Array.isArray(data)
        ? data
        : []

    /*
     * Nếu đang tạo mới thì mặc định chọn
     * vai trò "Nhân viên" nếu có.
     */
    if (!edit.value && !f.value.roleId) {
      const employeeRole =
        roles.value.find(
          role =>
            role.name
              ?.trim()
              .toLowerCase() ===
            'nhân viên'
        )

      if (employeeRole) {
        f.value.roleId =
          String(employeeRole.id)
      } else if (roles.value.length) {
        f.value.roleId =
          String(roles.value[0].id)
      }
    }
  } catch (error) {
    console.error(
      'Không tải được vai trò:',
      error
    )

    err.value =
      'Không thể tải danh sách vai trò.'
  }
}

function validate() {
  const v = f.value

  if (
    !v.name.trim() ||
    !v.email.trim() ||
    !v.phone.trim() ||
    !v.roleId ||
    v.gender === '' ||
    !v.dob ||
    !v.province ||
    !v.ward
  ) {
    return 'Vui lòng nhập đầy đủ các trường bắt buộc (*).'
  }

  if (
    !/^\S+@\S+\.\S+$/.test(
      v.email.trim()
    )
  ) {
    return 'Email không hợp lệ.'
  }

  if (
    !/^0\d{9}$/.test(
      v.phone.trim()
    )
  ) {
    return 'Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0.'
  }

  const gender = Number(v.gender)

  if (![0, 1, 2].includes(gender)) {
    return 'Giới tính không hợp lệ.'
  }

  const dob = new Date(
    `${v.dob}T00:00:00`
  )

  const today = new Date()

  today.setHours(0, 0, 0, 0)

  if (dob > today) {
    return 'Ngày sinh không được lớn hơn ngày hiện tại.'
  }

  const minimumAgeDate = new Date()
  minimumAgeDate.setFullYear(
    minimumAgeDate.getFullYear() - 18
  )
  minimumAgeDate.setHours(0, 0, 0, 0)

  if (dob > minimumAgeDate) {
    return 'Nhân viên phải đủ 18 tuổi trở lên.'
  }

  return ''
}

function buildPayload() {
  return {
    name: f.value.name.trim(),

    email: f.value.email.trim(),

    phone: f.value.phone.trim(),

    roleId: Number(f.value.roleId),

    gender: Number(f.value.gender),

    dob: f.value.dob,

    province:
      f.value.province || null,

    ward:
      f.value.ward || null,

    street:
      f.value.street?.trim() || null,

    /*
     * Backend hiện không lưu base64.
     * Nếu là URL/path cũ thì vẫn gửi.
     */
    image:
      f.value.image?.startsWith(
        'data:image/'
      )
        ? null
        : f.value.image || null
  }
}

async function submit() {
  err.value = ''
  msg.value = ''

  const validationError = validate()

  if (validationError) {
    err.value = validationError
    showError(
      validationError,
      'Dữ liệu không hợp lệ'
    )
    return
  }

  askConfirm({
    title: edit.value
      ? 'Xác nhận cập nhật'
      : 'Xác nhận thêm nhân viên',

    message: edit.value
      ? `Bạn có chắc chắn muốn cập nhật thông tin nhân viên ${props.employee?.code || ''} - ${f.value.name} không?`
      : `Bạn có chắc chắn muốn thêm nhân viên ${f.value.name} không?`,

    action: async () => {
      saving.value = true

      try {
        const payload = buildPayload()

        if (edit.value) {
          await employeeService.updateEmployee(
            props.employee.code,
            payload
          )

          success(
            'Cập nhật thông tin nhân viên thành công',
            'Cập nhật thành công'
          )
        } else {
          await employeeService.createEmployee(
            payload
          )

          success(
            `Thêm nhân viên thành công. Thông tin tài khoản đã được gửi tới ${f.value.email}.`,
            'Tạo tài khoản thành công'
          )
        }

        setTimeout(() => {
          router.push('/nhan-vien')
        }, 1200)

      } catch (error) {
        console.error(error)

        const message =
          getErrorMessage(error)

        err.value = message

        showError(
          message,
          edit.value
            ? 'Cập nhật thất bại'
            : 'Thêm thất bại'
        )

        throw error
      } finally {
        saving.value = false
      }
    }
  })
}

function toggleStatus() {
  if (!edit.value) {
    return
  }

  const nextStatus =
    active.value ? 0 : 1

  const actionText =
    active.value
      ? 'khóa tài khoản'
      : 'mở khóa tài khoản'

  askConfirm({
    title: 'Xác nhận cập nhật',

    message:
      `Bạn có chắc chắn muốn ${actionText} ` +
      `nhân viên ${props.employee?.code || ''} - ${f.value.name} không?`,

    action: async () => {
      err.value = ''
      msg.value = ''

      try {
        const data =
          await employeeService.updateStatus(
            props.employee.code,
            nextStatus
          )

        active.value =
          data?.active ??
          nextStatus === 1

        success(
          nextStatus === 1
            ? 'Mở khóa tài khoản nhân viên thành công'
            : 'Khóa tài khoản nhân viên thành công',
          'Cập nhật thành công'
        )

      } catch (error) {
        console.error(error)

        const message =
          getErrorMessage(error)

        err.value = message

        showError(
          message,
          'Cập nhật thất bại'
        )

        throw error
      }
    }
  })
}

function scanCccd() {
  showCccdScanner.value = true
}

function handleCccdScanned(data) {
  showCccdScanner.value = false

  if (data.name) {
    f.value.name = data.name
  }

  if (data.dob) {
    f.value.dob = data.dob
  }

  if (
    data.gender === 0 ||
    data.gender === 1 ||
    data.gender === 2
  ) {
    f.value.gender = data.gender
  }

  msg.value =
    'Quét CCCD thành công. Vui lòng kiểm tra lại thông tin.'
}

function changePassword() {
  window.alert(
    'Chức năng đổi mật khẩu chưa được tích hợp.'
  )
}

onMounted(() => {
  loadRoles()
})
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

  <CccdScanner
    v-if="showCccdScanner"
    @close="showCccdScanner = false"
    @scanned="handleCccdScanned"
  />

  <div class="ss-split">

    <div class="ss-stack">

      <AvatarCard
        v-model:image="f.image"
        :name="f.name"
        :email="f.email"
        fallback="NV"
        :hint="!edit"
      >

        <span
          v-if="edit"
          class="ss-pill"
          :class="
            active
              ? 'success'
              : 'danger'
          "
          style="margin-top:2px"
        >
          {{
            active
              ? 'Hoạt động'
              : 'Đã khóa'
          }}
        </span>

      </AvatarCard>

      <template v-if="edit">

        <section class="ss-card">

          <h3 class="side-title">
            Đổi mật khẩu
          </h3>

          <button
            class="ss-btn block"
            type="button"
            @click="changePassword"
          >
            Đổi mật khẩu
          </button>

        </section>

        <section class="ss-card">

          <h3 class="side-title">
            Trạng thái tài khoản
          </h3>

          <button
            class="ss-btn block"
            type="button"
            :class="
              active
                ? 'danger-outline'
                : ''
            "
            @click="toggleStatus"
          >
            {{
              active
                ? 'Khóa tài khoản'
                : 'Mở khóa tài khoản'
            }}
          </button>

        </section>

      </template>

    </div>

    <div class="ss-stack">

      <section class="ss-card ss-form">

        <div class="ss-head">

          <div class="ss-head-icon">
            <i class="bi bi-person-vcard"></i>
          </div>

          <div>
            <h2>Thông tin cơ bản</h2>

            <p>
              Họ tên, email, liên hệ và tài khoản.
            </p>
          </div>

        </div>

        <div>
          <button
            type="button"
            class="ss-btn sm"
            @click="scanCccd"
          >
            <i class="bi bi-upc-scan"></i>
            Quét mã CCCD
          </button>
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
              <span class="req">*</span>
            </label>

            <input
              class="ss-input"
              v-model="f.phone"
              maxlength="10"
              placeholder="Nhập số điện thoại"
            />
          </div>

          <div class="ss-field">
            <label class="ss-label">
              Vai trò
              <span class="req">*</span>
            </label>

            <select
              class="ss-select"
              v-model="f.roleId"
            >
              <option value="">
                Chọn vai trò
              </option>

              <option
                v-for="role in roles"
                :key="role.id"
                :value="String(role.id)"
              >
                {{ role.name }}
              </option>
            </select>
          </div>

          <div class="ss-field">
            <label class="ss-label">
              Giới tính
              <span class="req">*</span>
            </label>

            <select
              class="ss-select"
              v-model="f.gender"
            >
              <option value="1">
                Nam
              </option>

              <option value="2">
                Nữ
              </option>

              <option value="0">
                Khác
              </option>
            </select>
          </div>

          <div class="ss-field">
            <label class="ss-label">
              Ngày sinh
              <span class="req">*</span>
            </label>

            <input
              class="ss-input"
              type="date"
              v-model="f.dob"
              :max="maxDob"
            />
          </div>

        </div>
      </section>

      <section class="ss-card ss-form">

        <div class="ss-head">

          <div class="ss-head-icon">
            <i class="bi bi-geo-alt"></i>
          </div>

          <h2>Địa chỉ</h2>

        </div>

        <div class="ss-grid2">

          <!-- TỈNH THÀNH -->
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
        {{
          loadingProvinces
            ? 'Đang tải tỉnh/thành...'
            : '-- Chọn tỉnh/thành --'
        }}
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

          <!-- PHƯỜNG XÃ -->
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
        {{
          loadingWards
            ? 'Đang tải phường/xã...'
            : '-- Chọn phường/xã --'
        }}
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

  <div
    v-if="addressError"
    class="ss-field full"
    style="color: #dc3545"
  >
    {{ addressError }}
  </div>

          <div class="ss-field full">

            <label class="ss-label">
              Địa chỉ cụ thể
            </label>

            <input
              class="ss-input"
              v-model="f.street"
              placeholder="Nhập địa chỉ cụ thể"
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

        <p
          v-if="msg"
          class="ss-hint ok"
        >
          <i class="bi bi-check-circle"></i>
          {{ msg }}
        </p>

        <div class="ss-actions left">

          <button
            class="ss-btn primary"
            type="button"
            :disabled="saving"
            @click="submit"
          >
            {{
              saving
                ? 'Đang lưu...'
                : edit
                  ? 'Lưu thay đổi'
                  : 'Tạo nhân viên'
            }}
          </button>

          <button
            class="ss-btn"
            type="button"
            :disabled="saving"
            @click="
              router.push('/nhan-vien')
            "
          >
            Hủy
          </button>

        </div>

      </section>

    </div>

  </div>
</template>

<style scoped>
.side-title {
  font-size: 12px;
  font-weight: 700;
  color: var(--ss-text);
}

.ss-hint.ok {
  color: var(--ss-success);
}
</style>