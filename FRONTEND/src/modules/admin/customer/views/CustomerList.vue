<script setup>
import { avatarUrl } from '../../../../utils/avatar'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import SsPager from '../../../../components/SsPager.vue'
import { initials } from '../../../../utils/paging'
import { confirmAction, showSuccess, showError } from '../../../../utils/feedback'

import {
  onMounted,
  onBeforeUnmount,
  ref,
  watch
} from 'vue'

import { useRouter } from 'vue-router'

import customerService
  from '../services/customerService'

import CustomerDetailModal
  from '../components/CustomerDetailModal.vue'

import CustomerEditModal
  from '../components/CustomerEditModal.vue'

import CustomerAddressModal
  from '../components/CustomerAddressModal.vue'

const router = useRouter()

const rows = ref([])

const q = ref('')

const status = ref('')

const page = ref(1)

const size = ref(5)

const pages = ref(1)

const totalElements = ref(0)

const loading = ref(false)

const error = ref('')

const selected = ref(null)

const modal = ref('')
const updatingStatus = ref('')
const openingModal = ref(false)

let searchTimer
let loadVersion = 0

const errorMessage = (e) =>
  e?.response?.data?.message
  || 'Không thể tải dữ liệu khách hàng.'

async function loadCustomers() {
  const version = ++loadVersion
  loading.value = true
  error.value = ''

  try {

    const params = {
      page: page.value,
      size: size.value
    }

    if (q.value.trim()) {
      params.tuKhoa =
        q.value.trim()
    }

    if (status.value !== '') {
      params.trangThai =
        Number(status.value)
    }

    const data =
      await customerService
        .getCustomers(params)

    if (version !== loadVersion) return

    rows.value =
      data?.content || []

    pages.value =
      Math.max(
        data?.totalPages || 1,
        1
      )

    totalElements.value =
      data?.totalElements || 0

    if (
      page.value > pages.value
    ) {
      page.value =
        pages.value
    }

  } catch (e) {

    if (version !== loadVersion) return

    error.value =
      errorMessage(e)

    rows.value = []
    pages.value = 1
    totalElements.value = 0

  } finally {
    if (version === loadVersion) loading.value = false
  }
}

async function openModal(
  type,
  customer
) {
  if (openingModal.value) return
  openingModal.value = true
  try {

    selected.value =
      await customerService
        .getCustomer(
          customer.code
        )

    modal.value =
      type

  } catch (e) {

    showError(
      errorMessage(e)
    )
  } finally {
    openingModal.value = false
  }
}

async function toggleStatus(
  customer
) {
  if (updatingStatus.value) return
  const next =
    customer.active
      ? 0
      : 1

  const text =
    customer.active
      ? 'khóa'
      : 'mở khóa'

  confirmAction(`Bạn có chắc muốn ${text} khách hàng ${customer.name || customer.code}?`, async () => {
    if (updatingStatus.value) return
    try {
      updatingStatus.value = customer.code
      await customerService.updateStatus(customer.code, next)
      showSuccess(next === 1 ? 'Đã mở khóa khách hàng.' : 'Đã khóa khách hàng.')
      await loadCustomers()
    } catch (e) {
      showError(errorMessage(e))
    } finally {
      updatingStatus.value = ''
    }
  })
}

function reset() {

  q.value = ''

  status.value = ''

  page.value = 1

  loadCustomers()
}

function closeModal() {

  modal.value = ''

  selected.value = null
}

async function saved() {

  closeModal()

  await loadCustomers()
}

async function addressChanged() {
  const code = selected.value?.code
  await loadCustomers()

  if (!code || selected.value?.code !== code) return
  try {
    const customer = await customerService.getCustomer(code)
    if (selected.value?.code === code) selected.value = customer
  } catch (e) {
    error.value = errorMessage(e)
  }
}

/*
 * Search debounce 400ms.
 */
watch(
  q,
  () => {

    ++loadVersion

    clearTimeout(
      searchTimer
    )

    searchTimer =
      setTimeout(
        () => {
          page.value = 1
          loadCustomers()
        },
        400
      )
  }
)

watch(
  status,
  () => {
    page.value = 1
    loadCustomers()
  }
)

watch(
  page,
  loadCustomers
)

watch(
  size,
  () => {
    page.value = 1
    loadCustomers()
  }
)

onMounted(
  loadCustomers
)

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

          <h2>
            Bộ lọc
          </h2>

        </div>

        <div class="ss-toolbar">

          <div class="ss-search grow">

            <i class="bi bi-search"></i>

            <input
              class="ss-input"
              v-model="q"
              placeholder="Tìm theo mã, tên đăng nhập, họ tên, email, SĐT..."
            />

          </div>

          <select
            class="ss-select"
            v-model="status"
            style="width:150px"
          >

            <option value="">
              Tất cả
            </option>

            <option value="1">
              Hoạt động
            </option>

            <option value="0">
              Đã khóa
            </option>

          </select>

          <button
            class="ss-btn"
            @click="reset"
          >
            Đặt lại bộ lọc
          </button>

          <button
            class="ss-btn"
            title="Chức năng xuất Excel sẽ làm riêng"
          >
            <i class="bi bi-file-earmark-excel"></i>
            Xuất Excel
          </button>

          <button
            class="ss-btn primary"
            @click="
              router.push(
                '/khach-hang/them'
              )
            "
          >
            <i class="bi bi-plus-lg"></i>
            Thêm khách hàng
          </button>

        </div>

      </section>

      <section class="ss-card">

        <div class="ss-head">

          <div class="ss-head-icon">
            <i class="bi bi-people"></i>
          </div>

          <h2>
            Danh sách khách hàng
          </h2>

          <span class="ss-spacer"></span>

          <span class="ss-count">
            {{ totalElements }}
            bản ghi.
          </span>

        </div>

        <p
          v-if="error"
          class="ss-hint warn"
        >
          <i
            class="bi bi-exclamation-triangle"
          ></i>

          {{ error }}
        </p>

        <div class="ss-table-wrap">

          <table class="ss-table">

            <thead>
              <tr>
                <th class="w-stt c">
                  STT
                </th>

                <th>
                  Ảnh
                </th>

                <th>
                  Mã
                </th>

                <th>
                  Họ tên
                </th>

                <th>
                  Email
                </th>

                <th>
                  Địa chỉ mặc định
                </th>

                <th>
                  Số điện thoại
                </th>

                <th>
                  Trạng thái
                </th>

                <th>
                  Hành động
                </th>
              </tr>
            </thead>

            <tbody>

              <tr v-if="loading">

                <td
                  colspan="9"
                  class="ss-empty"
                >
                  Đang tải dữ liệu...
                </td>

              </tr>

              <tr
                v-for="(c, i) in rows"
                v-else
                :key="c.id ?? c.code"
              >

                <td class="c">
                  {{
                    (page - 1)
                    * size
                    + i
                    + 1
                  }}
                </td>

                <td>

                  <img
                    v-if="c.image"
                    :src="avatarUrl(c.image)"
                    class="customer-avatar-img"
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
                    {{ initials(c.name) }}
                  </span>

                </td>

                <td class="ss-strong nowrap">
                  {{ c.code || '—' }}
                </td>

                <td class="ss-strong">
                  {{ c.name || '—' }}
                </td>

                <td>
                  {{ c.email || '—' }}
                </td>

                <td style="min-width:240px">
                  {{
                    c.defaultAddress?.fullAddress
                    || 'Chưa có địa chỉ'
                  }}
                </td>

                <td class="nowrap">
                  {{ c.phone || '—' }}
                </td>

                <td>

                  <span
                    class="ss-pill"
                    :class="
                      c.active
                        ? 'success'
                        : 'danger'
                    "
                  >
                    {{ c.statusLabel || '—' }}
                  </span>

                </td>

                <td>

                  <div class="ss-row-actions">

                    <button
                      class="ss-icon-btn"
                      title="Xem chi tiết"
                      :disabled="openingModal"
                      @click="
                        openModal(
                          'detail',
                          c
                        )
                      "
                    >
                      <i class="bi bi-eye"></i>
                    </button>

                    <button
                      class="ss-icon-btn"
                      title="Sửa"
                      :disabled="openingModal"
                      @click="
                        openModal(
                          'edit',
                          c
                        )
                      "
                    >
                      <i class="bi bi-pencil"></i>
                    </button>

                    <button
                      class="ss-icon-btn"
                      title="Địa chỉ"
                      :disabled="openingModal"
                      @click="
                        openModal(
                          'address',
                          c
                        )
                      "
                    >
                      <i class="bi bi-geo-alt"></i>
                    </button>

                    <button
                      class="ss-icon-btn danger"
                      :disabled="!!updatingStatus"
                      :title="
                        c.active
                          ? 'Khóa tài khoản'
                          : 'Mở khóa'
                      "
                      @click="
                        toggleStatus(c)
                      "
                    >

                      <i
                        class="bi"
                        :class="
                          c.active
                            ? 'bi-lock'
                            : 'bi-unlock'
                        "
                      ></i>

                    </button>

                  </div>

                </td>

              </tr>

              <tr
                v-if="
                  !loading
                  && !rows.length
                "
              >

                <td
                  colspan="9"
                  class="ss-empty"
                >

                  <i class="bi bi-inbox"></i>

                  Không tìm thấy khách hàng.

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

    <CustomerDetailModal
      v-if="
        modal === 'detail'
        && selected
      "
      :customer="selected"
      @close="closeModal"
    />

    <CustomerEditModal
      v-if="
        modal === 'edit'
        && selected
      "
      :customer="selected"
      @close="closeModal"
      @saved="saved"
    />

    <CustomerAddressModal
      v-if="
        modal === 'address'
        && selected
      "
      :customer="selected"
      @close="closeModal"
      @changed="addressChanged"
    />

  </AdminLayout>

</template>

<style scoped>
.grow {
  flex: 1;
}

.customer-avatar-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}
</style>
