<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import api from '../../../../utils/api'
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import DiscountFilter from '../components/DiscountFilter.vue'
import DiscountTable from '../components/DiscountTable.vue'
import ConfirmModal from '../components/ConfirmModal.vue'

import {
  getDiscounts,
  deactivateDiscount,
  activateDiscount,
  deleteDiscount
} from '../services/discountService'

const router = useRouter()

const filters = ref({
  keyword: '',
  type: 'all',
  start: '',
  end: '',
  discount: 'all',
  status: 'all'
})

const rows = ref([])
const formSupported = ref(false)

const pageSize = ref(5)
const page = ref(1)

const totalElements = ref(0)
const totalPages = ref(1)

const loading = ref(false)
let requestId = 0

/* xac nhan */

const showConfirm = ref(false)
const saving = ref(false)

const confirmAction = ref('')
const selectedVoucher = ref(null)

/*tbao popup*/

const toast = ref({
  visible: false,
  type: 'success',
  title: '',
  message: ''
})

let toastTimer = null

function showLocalToast(type, title, message) {
  toast.value = {
    visible: true,
    type,
    title,
    message
  }

  if (toastTimer) {
    window.clearTimeout(toastTimer)
  }

  toastTimer = window.setTimeout(() => {
    toast.value.visible = false
  }, 3000)
}

/*  bo loc */

function typeToValue(type) {
  if (type === 'Công khai') return 1
  if (type === 'Cá nhân') return 2

  return undefined
}

function discountToValue(type) {
  if (type === 'Phần trăm (%)') return 1
  if (type === 'Tiền mặt (VNĐ)') return 2

  return undefined
}

function statusToValue(status) {
  if (status === 'Đang hoạt động') return 1
  if (status === 'Ngừng hoạt động') return 0

  return undefined
}

/*  format  */

function formatDateTime(value) {
  if (!value) return '-'

  const [datePart, timePart = ''] =
    String(value).split('T')

  const [y, m, d] =
    datePart.split('-')

  if (!y || !m || !d) {
    return value
  }

  const time =
    timePart.substring(0, 5)

  return `${d}/${m}/${y} ${time}`
}

function formatDiscount(item) {
  if (item.discountType === 1) {
    return `${item.discountValue ?? 0}%`
  }

  return `${Number(
    item.discountValue ?? 0
  ).toLocaleString('vi-VN')}đ`
}

function mapRow(item) {
  return {
    ...item,
    type: item.formLabel,
    value: formatDiscount(item),

    start: formatDateTime(item.startDate),
    end: formatDateTime(item.endDate),

    timeStatus: item.timeStatus,
    timeStatusLabel: item.timeStatusLabel
  }
}

/* view du lieu */

async function loadData() {
  const currentRequest = ++requestId
  loading.value = true

  try {
    const response = await getDiscounts({
      ma: filters.value.keyword.trim() || undefined,

      hinhThuc:
        filters.value.type === 'all'
          ? undefined
          : Number(filters.value.type),

      tuNgay:
        filters.value.start || undefined,

      denNgay:
        filters.value.end || undefined,

      loaiGiam:
        filters.value.discount === 'all'
          ? undefined
          : Number(filters.value.discount),

      trangThai:
        filters.value.status === 'all'
          ? undefined
          : Number(filters.value.status),

      page: page.value,
      size: pageSize.value
    })

    if (currentRequest !== requestId) return
    const data = response.data?.data

    let content = data?.content || []

    if (filters.value.type !== 'all') {
      content = content.filter(item =>
        item.form === Number(filters.value.type)
      )
    }

    if (filters.value.discount !== 'all') {
      content = content.filter(item =>
        item.discountType === Number(
          filters.value.discount
        )
      )
    }

    rows.value = content.map(mapRow)

    totalElements.value =
      data?.totalElements ?? 0

    totalPages.value =
      Math.max(1, data?.totalPages ?? 1)
    if (page.value > totalPages.value) {
      page.value = totalPages.value
      await loadData()
    }

  } catch (error) {
    if (currentRequest !== requestId) return
    console.error(error)

    showLocalToast(
      'error',
      'Tải dữ liệu thất bại',
      error.response?.data?.message ||
      'Không thể tải danh sách phiếu giảm giá!'
    )

  } finally {
    if (currentRequest === requestId) loading.value = false
  }
}

/* bo loc+ */

function search(value) {
  filters.value = {
    ...value
  }

  page.value = 1

  loadData()
}

function reset() {
  filters.value = {
    keyword: '',
    type: 'all',
    start: '',
    end: '',
    discount: 'all',
    status: 'all'
  }

  page.value = 1

  loadData()
}


function viewVoucher(id) {
  router.push(`/giam-gia/chi-tiet/${id}`)
}

function editVoucher(id) {
  router.push(`/giam-gia/sua/${id}`)
}

/* mo tbao xac nhan*/

function toggleVoucher(voucher) {
  selectedVoucher.value = voucher

  if (voucher.status === 1) {
    confirmAction.value = 'deactivate'
  } else {
    confirmAction.value = 'activate'
  }

  showConfirm.value = true
}

function removeVoucher(voucher) {
  selectedVoucher.value = voucher
  confirmAction.value = 'delete'
  showConfirm.value = true
}

/* xac nhan hanh dong*/

async function handleConfirm() {
  if (saving.value) return
  if (!selectedVoucher.value) {
    return
  }

  saving.value = true

  const voucher = selectedVoucher.value

  try {

    /* tat hoat dong */
    if (confirmAction.value === 'deactivate') {

      await deactivateDiscount(voucher.id)

      showLocalToast(
        'success',
        'Thành công',
        `Đã ngừng hoạt động phiếu "${voucher.code}"!`
      )
    }

    /* bat hoat dong */
    else if (confirmAction.value === 'activate') {

      await activateDiscount(voucher.id)

      showLocalToast(
        'success',
        'Thành công',
        `Đã bật hoạt động phiếu "${voucher.code}"!`
      )
    }

    /* xoa */
    else if (confirmAction.value === 'delete') {

      await deleteDiscount(voucher.id)

      showLocalToast(
        'success',
        'Xóa thành công',
        `Đã xóa phiếu "${voucher.code}"!`
      )
    }

    showConfirm.value = false
    selectedVoucher.value = null

    await loadData()

  } catch (error) {

    console.error(error)

    showConfirm.value = false

    showLocalToast(
      'error',
      'Thao tác thất bại',
      error.response?.data?.message ||
      'Không thể thực hiện thao tác!'
    )

  } finally {
    saving.value = false
  }
}

/* xac nhan  */

function confirmTitle() {
  if (confirmAction.value === 'deactivate') {
    return 'Xác nhận ngừng hoạt động'
  }

  if (confirmAction.value === 'activate') {
    return 'Xác nhận bật hoạt động'
  }

  if (confirmAction.value === 'delete') {
    return 'Xác nhận xóa'
  }

  return 'Xác nhận'
}

function confirmMessage() {
  const code =
    selectedVoucher.value?.code || ''

  if (confirmAction.value === 'deactivate') {
    return `Bạn có chắc chắn muốn ngừng hoạt động phiếu "${code}" không?`
  }

  if (confirmAction.value === 'activate') {
    return `Bạn có chắc chắn muốn bật hoạt động phiếu "${code}" không?`
  }

  if (confirmAction.value === 'delete') {
    return `Bạn có chắc chắn muốn xóa phiếu "${code}" không? Thao tác này không thể hoàn tác.`
  }

  return 'Bạn có chắc chắn muốn thực hiện thao tác này không?'
}

function confirmText() {
  if (confirmAction.value === 'deactivate') {
    return 'Ngừng hoạt động'
  }

  if (confirmAction.value === 'activate') {
    return 'Bật hoạt động'
  }

  if (confirmAction.value === 'delete') {
    return 'Xóa'
  }

  return 'Đồng ý'
}


function changePage(nextPage) {
  if (
    nextPage < 1 ||
    nextPage > totalPages.value
  ) {
    return
  }

  page.value = nextPage

  loadData()
}

onMounted(async () => {
  try { const response = await api.get('/phieu-giam-gia/capabilities'); formSupported.value = response.data?.data?.formSupported === true }
  catch { showLocalToast('error', 'Không tải được cấu hình phiếu', 'Vui lòng tải lại trang.'); }
  await loadData()
})
onUnmounted(() => { ++requestId; window.clearTimeout(toastTimer) })
</script>

<template>

  <AdminLayout>

    <main class="ss-page">

      <!-- bo loc -->
      <DiscountFilter
        :form-supported="formSupported"
        v-model="filters"
        @search="search"
        @reset="reset"
        @create="router.push('/giam-gia/them')"
      />

      <!-- danh sach -->
      <section class="ss-card">

        <div class="ss-head">

          <h2>
            Danh sách phiếu giảm giá
          </h2>

          <span class="ss-spacer"></span>

          <span class="ss-count">
            {{ totalElements }}
            bản ghi hiển thị.
          </span>

        </div>

        <div
          v-if="loading"
          class="ss-empty"
        >
          <i class="bi bi-arrow-repeat"></i>
          Đang tải dữ liệu...
        </div>

        <div
          v-else
          class="ss-table-wrap"
        >

          <table class="ss-table">

            <thead>

              <tr>

                <th class="w-stt c">
                  STT
                </th>

                <th>Mã</th>

                <th>Tên phiếu</th>

                <th>Hình thức</th>

                <th>Giá trị giảm</th>

                <th>Ngày bắt đầu</th>

                <th>Ngày kết thúc</th>

                <th>Trạng thái</th>

                <th>Hành động</th>

              </tr>

            </thead>

            <DiscountTable
              :rows="rows"
              :page="page"
              :page-size="pageSize"
              @view="viewVoucher"
              @edit="editVoucher"
              @toggle="toggleVoucher"
              @delete="removeVoucher"
            />

          </table>

        </div>

        <!-- phan trang -->
        <div class="ss-foot">

          <select
            class="ss-select"
            v-model.number="pageSize"
            @change="page = 1; loadData()"
          >

            <option :value="5">
              5
            </option>

            <option :value="10">
              10
            </option>

            <option :value="20">
              20
            </option>

          </select>

          <div class="ss-pages">

            <button
              :disabled="page === 1"
              @click="changePage(page - 1)"
            >
              <i class="bi bi-chevron-left"></i>
            </button>

            <button
              v-for="n in totalPages"
              :key="n"
              :class="{ active: n === page }"
              @click="changePage(n)"
            >
              {{ n }}
            </button>

            <button
              :disabled="page === totalPages"
              @click="changePage(page + 1)"
            >
              <i class="bi bi-chevron-right"></i>
            </button>

          </div>

        </div>

      </section>

    </main>

    <!-- tbao xac nhan -->

    <ConfirmModal
      :visible="showConfirm"
      :title="confirmTitle()"
      :message="confirmMessage()"
      :confirm-text="confirmText()"
      :loading="saving"
      @confirm="handleConfirm"
      @cancel="showConfirm = false"
    />

    <!-- tbao popup -->

    <div
      v-if="toast.visible"
      class="ss-toast"
      :class="`ss-toast-${toast.type}`"
    >

      <div class="ss-toast-icon">

        <i
          v-if="toast.type === 'success'"
          class="bi bi-check-circle-fill"
        ></i>

        <i
          v-else
          class="bi bi-x-circle-fill"
        ></i>

      </div>

      <div class="ss-toast-content">

        <strong>
          {{ toast.title }}
        </strong>

        <span>
          {{ toast.message }}
        </span>

      </div>

      <button
        type="button"
        class="ss-toast-close"
        @click="toast.visible = false"
      >
        ×
      </button>

    </div>

  </AdminLayout>

</template>
