<script setup>
import { avatarUrl } from '../../../../utils/avatar'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import SsPager from '../../../../components/SsPager.vue'
import { initials } from '../../../../utils/paging'
import employeeService from '../services/employeeService'
import { confirmAction, showSuccess, showError } from '../../../../utils/feedback'

import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const rows = ref([])
const roles = ref([])

const loading = ref(false)
const error = ref('')
const updatingStatus = ref('')

const f = ref({
  q: '',
  roleId: 'all',
  status: 'all'
})

const page = ref(1)
const size = ref(5)
const pages = ref(1)
const totalElements = ref(0)

let searchTimer = null
let loadVersion = 0

function getErrorMessage(err) {
  return (
    err?.response?.data?.message ||
    err?.message ||
    'Có lỗi xảy ra. Vui lòng thử lại.'
  )
}

async function loadRoles() {
  try {
    const data = await employeeService.getRoles()
    roles.value = Array.isArray(data) ? data : []
  } catch (err) {
    console.error('Không tải được vai trò:', err)
  }
}

async function loadEmployees() {
  const version = ++loadVersion
  loading.value = true
  error.value = ''

  try {
    const params = {
      page: page.value,
      size: size.value
    }

    const keyword = f.value.q.trim()

    if (keyword) {
      params.tuKhoa = keyword
    }

    if (f.value.roleId !== 'all') {
      params.vaiTroId = Number(f.value.roleId)
    }

    if (f.value.status !== 'all') {
      params.trangThai = Number(f.value.status)
    }

    const data = await employeeService.getEmployees(params)

    if (version !== loadVersion) return

    rows.value = data?.content ?? []
    pages.value = Math.max(data?.totalPages ?? 0, 1)
    totalElements.value = data?.totalElements ?? 0

    if (page.value > pages.value) {
      page.value = pages.value
    }
  } catch (err) {
    if (version !== loadVersion) return
    console.error(err)
    rows.value = []
    pages.value = 1
    totalElements.value = 0
    error.value = getErrorMessage(err)
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

async function toggleStatus(employee) {
  if (updatingStatus.value) return
  const nextStatus = employee.active ? 0 : 1

  const action = employee.active
    ? 'khóa tài khoản'
    : 'mở khóa tài khoản'

  confirmAction(`Bạn có chắc muốn ${action} ${employee.name || employee.code}?`, async () => {
    if (updatingStatus.value) return
    try {
      updatingStatus.value = employee.code
      await employeeService.updateStatus(employee.code, nextStatus)
      showSuccess(nextStatus === 1 ? 'Đã mở khóa nhân viên.' : 'Đã khóa nhân viên.')
      await loadEmployees()
    } catch (err) {
      showError(getErrorMessage(err))
    } finally {
      updatingStatus.value = ''
    }
  })
}

function reset() {
  f.value = {
    q: '',
    roleId: 'all',
    status: 'all'
  }

  page.value = 1
  loadEmployees()
}

watch(
  () => f.value.q,
  () => {
    ++loadVersion
    clearTimeout(searchTimer)

    searchTimer = setTimeout(() => {
      page.value = 1
      loadEmployees()
    }, 400)
  }
)

watch(
  () => f.value.roleId,
  () => {
    page.value = 1
    loadEmployees()
  }
)

watch(
  () => f.value.status,
  () => {
    page.value = 1
    loadEmployees()
  }
)

watch(page, () => {
  loadEmployees()
})

watch(size, () => {
  if (page.value !== 1) {
    page.value = 1
  } else {
    loadEmployees()
  }
})

onMounted(async () => {
  await Promise.all([
    loadRoles(),
    loadEmployees()
  ])
})

onBeforeUnmount(() => {
  clearTimeout(searchTimer)
  ++loadVersion
})
</script>

<template>
  <AdminLayout>
    <main class="ss-page">

      <section class="ss-card">
        <div class="ss-head">
          <div class="ss-head-icon">
            <i class="bi bi-funnel"></i>
          </div>

          <h2>Bộ lọc</h2>
        </div>

        <div class="ss-toolbar">

          <div class="ss-search grow">
            <i class="bi bi-search"></i>

            <input
              class="ss-input"
              v-model="f.q"
              placeholder="Tìm theo mã, họ tên, tài khoản, SĐT..."
            />
          </div>

          <select
            class="ss-select"
            v-model="f.roleId"
            style="width:180px"
          >
            <option value="all">
              Tất cả vai trò
            </option>

            <option
              v-for="r in roles"
              :key="r.id"
              :value="String(r.id)"
            >
              {{ r.name }}
            </option>
          </select>

          <select
            class="ss-select"
            v-model="f.status"
            style="width:150px"
          >
            <option value="all">
              Tất cả
            </option>

            <option value="1">
              Hoạt động
            </option>

            <option value="0">
              Đã khóa
            </option>
          </select>
        </div>

        <div class="ss-actions">
          <button
            class="ss-btn"
            @click="reset"
          >
            Đặt lại bộ lọc
          </button>

          <button class="ss-btn">
            <i class="bi bi-file-earmark-excel"></i>
            Xuất Excel
          </button>

          <button
            class="ss-btn primary"
            @click="router.push('/nhan-vien/them')"
          >
            <i class="bi bi-plus-lg"></i>
            Thêm nhân viên
          </button>
        </div>
      </section>

      <section class="ss-card">

        <div class="ss-head">

          <div class="ss-head-icon">
            <i class="bi bi-person-badge"></i>
          </div>

          <h2>Danh sách nhân viên</h2>

          <span class="ss-spacer"></span>

          <span class="ss-count">
            {{ totalElements }} bản ghi.
          </span>
        </div>

        <p
          v-if="error"
          class="ss-hint warn"
        >
          <i class="bi bi-exclamation-triangle"></i>
          {{ error }}
        </p>

        <div class="ss-table-wrap">

          <table
            class="ss-table"
            style="min-width:1000px"
          >
            <thead>
              <tr>
                <th class="w-stt c">STT</th>
                <th>Ảnh</th>
                <th>Mã NV</th>
                <th>Họ tên</th>
                <th>Email</th>
                <th>Giới tính</th>
                <th>SĐT</th>
                <th>Địa chỉ</th>
                <th>Vai trò</th>
                <th>Trạng thái</th>
                <th>Hành động</th>
              </tr>
            </thead>

            <tbody>

              <tr v-if="loading">
                <td
                  colspan="11"
                  class="ss-empty"
                >
                  <i class="bi bi-arrow-repeat"></i>
                  Đang tải dữ liệu...
                </td>
              </tr>

              <tr
                v-for="(e, i) in rows"
                v-else
                :key="e.id ?? e.code"
              >

                <td class="c">
                  {{ (page - 1) * size + i + 1 }}
                </td>

                <td>
                  <img
                    v-if="e.image"
                    :src="avatarUrl(e.image)"
                    :alt="e.name"
                    class="employee-avatar"
                  />

                  <span
                    v-else
                    class="ss-avatar"
                    style="
                      width:40px;
                      height:40px;
                      flex-basis:40px
                    "
                  >
                    {{ initials(e.name) }}
                  </span>
                </td>

                <td>
                  <span class="ss-code">
                    {{ e.code || '—' }}
                  </span>
                </td>

                <td class="ss-strong">
                  {{ e.name || '—' }}
                </td>

                <td>
                  {{ e.email || '—' }}
                </td>

                <td>
                  {{ e.genderLabel || '—' }}
                </td>

                <td class="nowrap">
                  {{ e.phone || '—' }}
                </td>

                <td style="min-width:220px">
                  {{ e.address || '—' }}
                </td>

                <td class="nowrap">
                  {{ e.role || '—' }}
                </td>

                <td>
                  <span
                    class="ss-pill"
                    :class="e.active ? 'success' : 'danger'"
                  >
                    {{
                      e.active
                        ? 'Hoạt động'
                        : 'Đã khóa'
                    }}
                  </span>
                </td>

                <td>
                  <div class="ss-row-actions">

                    <button
                      class="ss-icon-btn"
                      title="Xem chi tiết"
                      @click="
                        router.push(
                          `/nhan-vien/${e.code}`
                        )
                      "
                    >
                      <i class="bi bi-eye"></i>
                    </button>

                    <button
                      class="ss-icon-btn danger"
                      :disabled="!!updatingStatus"
                      :title="
                        e.active
                          ? 'Khóa tài khoản'
                          : 'Mở khóa'
                      "
                      @click="toggleStatus(e)"
                    >
                      <i
                        class="bi"
                        :class="
                          e.active
                            ? 'bi-lock'
                            : 'bi-unlock'
                        "
                      ></i>
                    </button>

                  </div>
                </td>
              </tr>

              <tr
                v-if="!loading && !rows.length"
              >
                <td
                  colspan="11"
                  class="ss-empty"
                >
                  <i class="bi bi-inbox"></i>
                  Không tìm thấy nhân viên.
                </td>
              </tr>

            </tbody>
          </table>
        </div>

        <SsPager
          v-model:page="page"
          v-model:size="size"
          :pages="pages"
        />

      </section>

    </main>
  </AdminLayout>
</template>

<style scoped>
.employee-avatar {
  width: 40px;
  height: 40px;
  display: block;
  border-radius: 50%;
  object-fit: cover;
}
</style>
