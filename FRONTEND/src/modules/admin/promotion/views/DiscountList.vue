<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import DiscountFilter from '../components/DiscountFilter.vue'
import DiscountTable from '../components/DiscountTable.vue'

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

const pageSize = ref(5)
const page = ref(1)

const totalElements = ref(0)
const totalPages = ref(1)

const loading = ref(false)

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

function formatDate(value) {
  if (!value) return '-'

  const date = String(value).substring(0, 10)
  const [y, m, d] = date.split('-')

  return y && m && d
    ? `${d}/${m}/${y}`
    : date
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

    start: formatDate(item.startDate),

    end: formatDate(item.endDate),

    statusLabel: item.statusLabel
  }
}

async function loadData() {
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
    const data = response.data.data

    let content = data?.content || []

    // =========================
    // loc theo hinh thuc
    // =========================
    if (filters.value.type !== 'all') {
      content = content.filter(item =>
        item.form === Number(filters.value.type)
      )
    }

    // =========================
    // loc theo loai
    // =========================
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

  } catch (error) {

    console.error(error)

    alert(
      error.response?.data?.message ||
      'Không thể tải danh sách phiếu giảm giá!'
    )

  } finally {
    loading.value = false
  }
}

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

async function toggleVoucher(voucher) {

  const active = voucher.status === 1

  const action = active
    ? 'ngừng hoạt động'
    : 'bật hoạt động'

  const confirmed = window.confirm(
    `Bạn có chắc chắn muốn ${action} phiếu "${voucher.code}" không?`
  )

  if (!confirmed) return

  try {

    if (active) {
      await deactivateDiscount(voucher.id)
    } else {
      await activateDiscount(voucher.id)
    }

    alert(
      active
        ? 'Ngừng hoạt động phiếu giảm giá thành công!'
        : 'Bật hoạt động phiếu giảm giá thành công!'
    )

    await loadData()

  } catch (error) {

    console.error(error)

    alert(
      error.response?.data?.message ||
      `${active
        ? 'Ngừng hoạt động'
        : 'Bật hoạt động'} thất bại!`
    )
  }
}

async function removeVoucher(voucher) {

  const confirmed = window.confirm(
    `Bạn có chắc chắn muốn XÓA phiếu "${voucher.code}" không?\n\nThao tác này không thể hoàn tác.`
  )

  if (!confirmed) return

  try {

    await deleteDiscount(voucher.id)

    alert('Xóa phiếu giảm giá thành công!')

    await loadData()

  } catch (error) {

    console.error(error)

    alert(
      error.response?.data?.message ||
      'Xóa phiếu giảm giá thất bại!'
    )
  }
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

onMounted(loadData)
</script>

<template>

  <AdminLayout>

    <main class="ss-page">

      <!-- BỘ LỌC -->
      <DiscountFilter
        v-model="filters"
        @search="search"
        @reset="reset"
        @create="router.push('/giam-gia/them')"
      />

      <!-- DANH SÁCH -->
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

        <!-- PHÂN TRANG -->
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

  </AdminLayout>

</template>