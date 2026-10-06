<script setup>
import AvatarCard from '../../../../components/AvatarCard.vue'
import { persistAvatar } from '../../../../utils/avatar'
import employeeService from '../services/employeeService'
import { confirmAction, showSuccess, showError } from '../../../../utils/feedback'

import {
  computed,
  onMounted,
  ref
} from 'vue'

import { useRouter } from 'vue-router'

import {
  provinceNames,
  wardsOf
} from '../../../../utils/address'

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
const changingStatus = ref(false)

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

const wards = computed(
  () => wardsOf(f.value.province)
)

const err = ref('')
const msg = ref('')

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

    image: f.value.image || null
  }
}

async function submit() {
  if (saving.value || changingStatus.value) return
  err.value = ''
  msg.value = ''

  const validationError =
    validate()

  if (validationError) {
    err.value = validationError
    return
  }

  saving.value = true

  try {
    f.value.image = await persistAvatar(f.value.image) || ''
    const payload =
      buildPayload()

    if (edit.value) {
      await employeeService.updateEmployee(
        props.employee.code,
        payload
      )

      msg.value =
        'Đã lưu thay đổi thành công.'
      showSuccess(msg.value)

      setTimeout(() => {
        router.push('/nhan-vien')
      }, 700)
    } else {
      await employeeService.createEmployee(
        payload
      )

      showSuccess(
        'Thêm nhân viên thành công.'
      )

      router.push('/nhan-vien')
    }
  } catch (error) {
    console.error(error)

    err.value =
      getErrorMessage(error)
    showError(err.value)
  } finally {
    saving.value = false
  }
}

async function toggleStatus() {
  if (!edit.value || saving.value || changingStatus.value) {
    return
  }

  const nextStatus =
    active.value ? 0 : 1

  const action =
    active.value
      ? 'khóa tài khoản'
      : 'mở khóa tài khoản'

  confirmAction(`Bạn có chắc muốn ${action}?`, async () => {
    if (saving.value || changingStatus.value) return
    err.value = ''
    msg.value = ''
    changingStatus.value = true
    try {
      const data = await employeeService.updateStatus(props.employee.code, nextStatus)
      active.value = data?.active ?? nextStatus === 1
      msg.value = nextStatus === 1 ? 'Đã mở khóa tài khoản.' : 'Đã khóa tài khoản.'
      showSuccess(msg.value)
    } catch (error) {
      err.value = getErrorMessage(error)
      showError(err.value)
    } finally {
      changingStatus.value = false
    }
  })
}

function scanCccd() {
  showError(
    'Chức năng quét mã CCCD chưa được tích hợp.'
  )
}

function changePassword() {
  showError(
    'Chức năng đổi mật khẩu chưa được tích hợp.'
  )
}

onMounted(() => {
  loadRoles()
})
</script>

<template>
  <div class="ss-split">

    <div class="ss-stack">

      <AvatarCard
        v-model:image="f.image"
        :disabled="saving || changingStatus"
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
            :disabled="saving || changingStatus"
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
                Chọn tỉnh thành
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
              Xã/Phường/Thị trấn
              <span class="req">*</span>
            </label>

            <select
              class="ss-select"
              v-model="f.ward"
              :disabled="!f.province"
            >
              <option value="">
                Chọn xã phường
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
            :disabled="saving || changingStatus"
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
            :disabled="saving || changingStatus"
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
