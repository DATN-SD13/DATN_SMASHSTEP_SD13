<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import SsPager from '../../../../components/SsPager.vue'
import api from '../../../../utils/api'
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// =========================
// Loc
// =========================

const keyword = ref('')

const form = ref({
  start: '',
  end: '',
  status: 'all'
})


const rows = ref([])
const loading = ref(false)
const errorMessage = ref('')

// =========================
// phan trang
// =========================

const page = ref(1)
const pageSize = ref(5)
const totalElements = ref(0)
const totalPages = ref(1)

// =========================
// du lieu
// =========================

async function loadData() {
  try {
    loading.value = true
    errorMessage.value = ''

    const params = {
      page: page.value,
      size: pageSize.value
    }

    // Mã phiếu
    if (keyword.value.trim()) {
      params.ma = keyword.value.trim()
    }

    // Từ ngày
    if (form.value.start) {
      params.tuNgay = form.value.start
    }

    // Đến ngày
    if (form.value.end) {
      params.denNgay = form.value.end
    }

    // Trạng thái
    if (form.value.status !== 'all') {
      params.trangThai = Number(form.value.status)
    }

    const response = await api.get('/phieu-giam-gia', {
      params
    })

    const data = response.data.data

    rows.value = data.content || []

    page.value = data.page
    pageSize.value = data.size
    totalElements.value = data.totalElements
    totalPages.value = data.totalPages || 1

  } catch (error) {
    console.error('Lỗi tải phiếu giảm giá:', error)

    rows.value = []
    errorMessage.value = 'Không thể tải danh sách phiếu giảm giá.'
  } finally {
    loading.value = false
  }
}

// =========================
// tim kiem
// =========================

function search() {
  page.value = 1
  loadData()
}

// =========================
// reset
// =========================

function reset() {
  keyword.value = ''

  form.value = {
    start: '',
    end: '',
    status: 'all'
  }

  page.value = 1

  loadData()
}

// =========================
// phan trang
// =========================

function changePage(newPage) {
  page.value = newPage
  loadData()
}

function changeSize(newSize) {
  pageSize.value = newSize
  page.value = 1
  loadData()
}

// =========================
// FORMAT
// =========================

function formatMoney(value) {
  if (value === null || value === undefined) {
    return '0 ₫'
  }

  return Number(value).toLocaleString('vi-VN') + ' ₫'
}

function formatDate(value) {
  if (!value) {
    return ''
  }

  const parts = value.split('-')

  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`
  }

  return value
}

function formatDiscount(item) {
  if (item.discountType === 1) {
    return `${item.discountValue}%`
  }

  return formatMoney(item.discountValue)
}

function statusClass(status) {
  return status === 1 ? 'success' : 'danger'
}

// update 
function editVoucher(id) {
  router.push(`/giam-gia/sua/${id}`)
}
// ngung hoat dong
async function deactivateVoucher(voucher) {
  const confirmed = window.confirm(
    `Bạn có chắc chắn muốn ngừng hoạt động phiếu "${voucher.code}" không?`
  )

  if (!confirmed) return

  try {
    await api.delete(`/phieu-giam-gia/${voucher.id}`)

    alert('Ngừng hoạt động phiếu giảm giá thành công!')

    await loadData()
  } catch (error) {
    console.error(error)

    alert(
      error.response?.data?.message ||
      'Ngừng hoạt động phiếu giảm giá thất bại!'
    )
  }
}
// bat lai 
async function activateVoucher(voucher) {
  const confirmed = window.confirm(
    `Bạn có chắc chắn muốn bật lại phiếu "${voucher.code}" không?`
  )

  if (!confirmed) return

  try {
    await api.put(`/phieu-giam-gia/${voucher.id}/kich-hoat`)

    alert('Bật hoạt động phiếu giảm giá thành công!')

    await loadData()
  } catch (error) {
    console.error(error)

    alert(
      error.response?.data?.message ||
      'Bật hoạt động phiếu giảm giá thất bại!'
    )
  }
}
// xoa 
async function deleteVoucher(voucher) {
  const confirmed = window.confirm(
    `Bạn có chắc chắn muốn XÓA phiếu "${voucher.code}" không?\n\nThao tác này không thể hoàn tác.`
  )

  if (!confirmed) return

  try {
    await api.delete(`/phieu-giam-gia/${voucher.id}/xoa`)

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
// xem chi tiet
function viewVoucher(id) {
  router.push(`/giam-gia/chi-tiet/${id}`)
}
// =========================
// INIT
// =========================

onMounted(() => {
  loadData()
})
</script>

<template>
  <AdminLayout>
    <main class="ss-page">

      <!-- ================= FILTER ================= -->
      <section class="ss-card">

        <div class="ss-head">
          <div class="ss-head-icon">
            <i class="bi bi-funnel"></i>
          </div>

          <div>
            <h2>Bộ lọc</h2>
            <p>Tra cứu nhanh dữ liệu.</p>
          </div>
        </div>

        <div class="filter-row">

          <!-- Mã -->
          <div class="ss-field">
            <span class="ss-label">Mã phiếu</span>

            <div class="ss-search">
              <i class="bi bi-search"></i>

              <input
                class="ss-input"
                v-model="keyword"
                placeholder="Nhập mã phiếu..."
                @keyup.enter="search"
              />
            </div>
          </div>

          <!-- Từ ngày -->
          <div class="ss-field">
            <span class="ss-label">Từ ngày</span>

            <input
              class="ss-input"
              type="date"
              v-model="form.start"
            />
          </div>

          <!-- Đến ngày -->
          <div class="ss-field">
            <span class="ss-label">Đến ngày</span>

            <input
              class="ss-input"
              type="date"
              v-model="form.end"
            />
          </div>

          <!-- Trạng thái -->
          <div class="ss-field">
            <span class="ss-label">Trạng thái</span>

            <select
              class="ss-select"
              v-model="form.status"
            >
              <option value="all">
                Tất cả trạng thái
              </option>

              <option value="1">
                Hoạt động
              </option>

              <option value="0">
                Ngừng hoạt động
              </option>
            </select>
          </div>

        </div>

        <div class="ss-actions">

          <button
            class="ss-btn"
            @click="reset"
          >
            <i class="bi bi-arrow-clockwise"></i>
            Đặt lại bộ lọc
          </button>

          <button
            class="ss-btn primary"
            @click="search"
          >
            <i class="bi bi-search"></i>
            Tìm kiếm
          </button>

          <button
            class="ss-btn primary"
            @click="router.push('/giam-gia/them')"
          >
            <i class="bi bi-plus-lg"></i>
            Tạo phiếu mới
          </button>

        </div>

      </section>


      <!-- ================= LIST ================= -->
      <section class="ss-card">

        <div class="ss-head">

          <h2>
            Danh sách phiếu giảm giá
          </h2>

          <span class="ss-spacer"></span>

          <span class="ss-count">
            {{ totalElements }} bản ghi
          </span>

        </div>


        <!-- Error -->
        <div
          v-if="errorMessage"
          class="alert alert-danger mx-3"
        >
          {{ errorMessage }}
        </div>


        <div class="ss-table-wrap">

          <table class="ss-table">

            <thead>
              <tr>

                <th class="w-stt c">
                  STT
                </th>

                <th>
                  Mã
                </th>

                <th>
                  Tên phiếu
                </th>

                <th>
                  Hình thức
                </th>

                <th>
                  Giá trị giảm
                </th>

                <th>
                  Ngày bắt đầu
                </th>

                <th>
                  Ngày kết thúc
                </th>

                <th>
                  Số lượng
                </th>

                <th>
                  Đã dùng
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

              <!-- Loading -->
              <tr v-if="loading">

                <td
                  colspan="11"
                  class="ss-empty"
                >
                  <div class="spinner-border"></div>

                  <span>
                    Đang tải dữ liệu...
                  </span>
                </td>

              </tr>


              <!-- Empty -->
              <tr v-else-if="!rows.length">

                <td
                  colspan="11"
                  class="ss-empty"
                >
                  <i class="bi bi-inbox"></i>

                  Không có phiếu giảm giá phù hợp.
                </td>

              </tr>


              <!-- Data -->
              <tr
                v-for="(x, i) in rows"
                :key="x.id"
              >

                <td class="c">
                  {{ (page - 1) * pageSize + i + 1 }}
                </td>


                <td>
                  <span class="ss-code">
                    {{ x.code }}
                  </span>
                </td>


                <td>
                  {{ x.name }}
                </td>


                <td>

                  <span
                    class="ss-pill"
                    :class="{ warn: x.form === 2 }"
                  >
                    {{ x.formLabel }}
                  </span>

                </td>


                <td>
                  {{ formatDiscount(x) }}
                </td>


                <td class="nowrap">
                  {{ formatDate(x.startDate) }}
                </td>


                <td class="nowrap">
                  {{ formatDate(x.endDate) }}
                </td>


                <td class="c">
                  {{ x.quantity }}
                </td>


                <td class="c">
                  {{ x.usedQuantity }}
                </td>


                <td>

                  <span
                    class="ss-pill dot"
                    :class="statusClass(x.status)"
                  >
                    {{ x.statusLabel }}
                  </span>

                </td>


                <td>

                  <div class="ss-row-actions">

                    <button
                      class="ss-icon-btn"
                      title="Xem chi tiết"
                      @click="viewVoucher(x.id)"
                    >
                      <i class="bi bi-eye"></i>
                    </button>

                    <button
                      class="ss-icon-btn"
                      title="Sửa"
                      @click="editVoucher(x.id)"
                    >
                      <i class="bi bi-pencil"></i>
                    </button>

                    <div class="d-flex gap-1">

                    <!-- Ngừng / Bật hoạt động -->
                    <button
                      v-if="x.status === 1"
                      class="ss-icon-btn"
                      title="Ngừng hoạt động"
                      @click="deactivateVoucher(x)"
                    >
                      <i class="bi bi-power"></i>
                    </button>

                    <button
                      v-else
                      class="ss-icon-btn"
                      title="Bật hoạt động"
                      @click="activateVoucher(x)"
                    >
                      <i class="bi bi-power"></i>
                    </button>

                    <!-- Xóa -->
                    <button
                      class="ss-icon-btn"
                      title="Xóa"
                      @click="deleteVoucher(x)"
                    >
                      <i class="bi bi-trash"></i>
                    </button>

                  </div>
                  </div>

                </td>

              </tr>

            </tbody>

          </table>

        </div>


        <!-- ================= PAGER ================= -->

        <SsPager
          :page="page"
          :size="pageSize"
          :pages="totalPages"
          @update:page="changePage"
          @update:size="changeSize"
        />

      </section>

    </main>
  </AdminLayout>
</template>


<style scoped>

.filter-row {
  display: grid;
  grid-template-columns:
    repeat(4, minmax(0, 1fr));

  gap: 10px;
}

@media (max-width: 1100px) {

  .filter-row {
    grid-template-columns:
      repeat(2, minmax(0, 1fr));
  }

}

@media (max-width: 640px) {

  .filter-row {
    grid-template-columns: 1fr;
  }

  .ss-actions > * {
    flex: 1;
  }

}

.ss-empty {
  height: 180px;
  text-align: center;
  vertical-align: middle;
  color: var(--ss-muted);
}

.ss-empty i {
  font-size: 28px;
  display: block;
  margin-bottom: 8px;
}

.ss-empty .spinner-border {
  margin-right: 8px;
}

</style>