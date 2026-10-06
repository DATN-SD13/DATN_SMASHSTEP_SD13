<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../../../utils/api'

const router = useRouter()

// ==========================
// BỘ LỌC
// ==========================
const keyword = ref('')
const status = ref('all')
const start = ref('')
const end = ref('')

// ==========================
// DỮ LIỆU ĐỢT GIẢM GIÁ
// ==========================
const rows = ref([])
const loading = ref(false)

// Chuyển yyyy-MM-dd -> dd/MM/yyyy
function formatDate(date) {
  if (!date) return ''

  const [year, month, day] = date.split('-')
  return `${day}/${month}/${year}`
}

// ==========================
// LẤY DỮ LIỆU TỪ BACKEND
// ==========================
async function loadCampaigns() {
  try {
    loading.value = true

    const response = await api.get('/dot-giam-gia', {
      params: {
        page: 1,
        size: 100
      }
    })

    const content = response.data?.data?.content || []

    rows.value = content.map(item => ({
      id: item.id,
      code: item.code,
      name: item.name,
      value: `${item.discountValue}%`,
      start: formatDate(item.startDate),
      end: formatDate(item.endDate),
      status: item.statusLabel,
      statusValue: item.status,
      timeStatus: item.timeStatus
    }))

  } catch (error) {
    console.error(
      'Lỗi lấy danh sách đợt giảm giá:',
      error
    )

    rows.value = []
  } finally {
    loading.value = false
  }
}

// ==========================
// LỌC DỮ LIỆU
// Tạm thời giữ cách lọc của giao diện gốc
// ==========================
const iso = (d) => {
  if (!d) return ''
  return d.split('/').reverse().join('-')
}

const filtered = computed(() => {
  return rows.value.filter(x => {
    const q = keyword.value
      .trim()
      .toLowerCase()

    return (
      (
        !q ||
        x.code.toLowerCase().includes(q) ||
        x.name.toLowerCase().includes(q) ||
        x.value.includes(q)
      ) &&
      (
        status.value === 'all' ||
        x.status === status.value
      ) &&
      (
        !start.value ||
        iso(x.start) >= start.value
      ) &&
      (
        !end.value ||
        iso(x.end) <= end.value
      )
    )
  })
})

// ==========================
// PHÂN TRANG
// ==========================
const pageSize = ref(5)
const page = ref(1)

const totalPages = computed(() =>
  Math.max(
    1,
    Math.ceil(
      filtered.value.length / pageSize.value
    )
  )
)

const paged = computed(() =>
  filtered.value.slice(
    (page.value - 1) * pageSize.value,
    page.value * pageSize.value
  )
)

watch([filtered, pageSize], () => {
  if (page.value > totalPages.value) {
    page.value = 1
  }
})

// ==========================
// TRẠNG THÁI
// ==========================
const isActive = (x) =>
  x.status === 'Đang hoạt động'

// Tạm thời giữ chức năng cũ.
// Bước sau mới nối PATCH backend.
// ==========================
// BẬT / TẮT TRẠNG THÁI
// ==========================

async function toggle(x) {
  const newStatus = isActive(x) ? 0 : 1

  const message = newStatus === 1
    ? `Bạn có chắc muốn kích hoạt ${x.code}?`
    : `Bạn có chắc muốn ngừng hoạt động ${x.code}?`

  if (!confirm(message)) {
    return
  }

  try {
    await api.patch(
      `/dot-giam-gia/${x.code}/trang-thai`,
      {
        status: newStatus
      }
    )

    await loadCampaigns()

    alert(
      newStatus === 1
        ? 'Kích hoạt thành công!'
        : 'Ngừng hoạt động thành công!'
    )

  } catch (error) {
    console.error(
      'Lỗi cập nhật trạng thái:',
      error
    )

    alert(
      error.response?.data?.message ||
      'Không thể cập nhật trạng thái'
    )
  }
}


// ==========================
// XÓA ĐỢT GIẢM GIÁ
// ==========================

async function removeCampaign(x) {
  const confirmed = window.confirm(
    `Bạn có chắc muốn xóa đợt giảm giá ${x.code} không?`
  )

  if (!confirmed) {
    return
  }

  try {
    await api.delete(
      `/dot-giam-gia/${x.code}`
    )

    alert(
      'Xóa đợt giảm giá thành công!'
    )

    await loadCampaigns()

  } catch (error) {
    console.error(
      'Lỗi xóa đợt giảm giá:',
      error
    )

    alert(
      error.response?.data?.message ||
      'Không thể xóa đợt giảm giá'
    )
  }
}

// ==========================
// RESET BỘ LỌC
// ==========================
function reset() {
  keyword.value = ''
  status.value = 'all'
  start.value = ''
  end.value = ''
  page.value = 1
}
function viewDetail(x) {
  router.push(`/dot-giam-gia/chi-tiet/${x.code}`)
}
// ==========================
// KHI MỞ TRANG
// ==========================
onMounted(() => {
  loadCampaigns()
})
</script>

<template>
  <AdminLayout>
    <main class="ss-page">

      <!-- BỘ LỌC -->
      <section class="ss-card">

        <div class="ss-head">
          <div class="ss-head-icon">
            <i class="bi bi-funnel"></i>
          </div>

          <h2>Bộ lọc</h2>
        </div>

        <div class="filter-row">

          <!-- TÌM KIẾM -->
          <div class="ss-field">
            <span class="ss-label">
              Tìm kiếm
            </span>

            <div class="ss-search">
              <i class="bi bi-search"></i>

              <input
                class="ss-input"
                v-model="keyword"
                placeholder="Mã, tên, giá trị..."
              />
            </div>
          </div>

          <!-- NGÀY BẮT ĐẦU -->
          <div class="ss-field">
            <span class="ss-label">
              Ngày bắt đầu
            </span>

            <input
              class="ss-input"
              type="date"
              v-model="start"
            />
          </div>

          <!-- NGÀY KẾT THÚC -->
          <div class="ss-field">
            <span class="ss-label">
              Ngày kết thúc
            </span>

            <input
              class="ss-input"
              type="date"
              v-model="end"
            />
          </div>

          <!-- TRẠNG THÁI -->
          <div class="ss-field">
            <span class="ss-label">
              Trạng thái
            </span>

            <select
              class="ss-select"
              v-model="status"
            >
              <option value="all">
                Tất cả trạng thái
              </option>

              <option value="Đang hoạt động">
                Đang hoạt động
              </option>

              <option value="Ngừng hoạt động">
                Ngừng hoạt động
              </option>
            </select>
          </div>

        </div>

        <!-- BUTTON -->
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
            @click="router.push('/dot-giam-gia/them')"
          >
            <i class="bi bi-plus-lg"></i>
            Tạo đợt giảm giá
          </button>

        </div>

      </section>

      <!-- DANH SÁCH -->
      <section class="ss-card">

        <div class="ss-head">

          <h2>
            Danh sách các đợt giảm giá
          </h2>

          <span class="ss-spacer"></span>

          <span class="ss-count">
            {{ filtered.length }} bản ghi hiển thị.
          </span>

        </div>

        <!-- LOADING -->
        <div
          v-if="loading"
          class="ss-empty"
        >
          Đang tải dữ liệu...
        </div>

        <!-- TABLE -->
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

                <th>
                  Mã
                </th>

                <th>
                  Tên
                </th>

                <th>
                  Giá trị
                </th>

                <th>
                  Ngày bắt đầu
                </th>

                <th>
                  Ngày kết thúc
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

              <tr
                v-for="(x, i) in paged"
                :key="x.code"
              >

                <!-- STT -->
                <td class="c">
                  {{
                    (page - 1) * pageSize
                    + i
                    + 1
                  }}
                </td>

                <!-- MÃ -->
                <td>
                  <span class="ss-code">
                    {{ x.code }}
                  </span>
                </td>

                <!-- TÊN -->
                <td>
                  {{ x.name }}
                </td>

                <!-- GIÁ TRỊ -->
                <td>
                  {{ x.value }}
                </td>

                <!-- NGÀY BẮT ĐẦU -->
                <td class="nowrap">
                  {{ x.start }}
                </td>

                <!-- NGÀY KẾT THÚC -->
                <td class="nowrap">
                  {{ x.end }}
                </td>

                <!-- TRẠNG THÁI -->
                <td>
                  <span
                    class="ss-pill dot"
                    :class="
                      isActive(x)
                        ? 'success'
                        : 'danger'
                    "
                  >
                    {{ x.status }}
                  </span>
                </td>

                <!-- HÀNH ĐỘNG -->
                <td>

                  <div class="ss-row-actions">

                    <!-- XEM CHI TIẾT -->
                                        <button
                      class="ss-icon-btn"
                      title="Xem chi tiết"
                      @click="viewDetail(x)"
                    >
                      <i class="bi bi-eye"></i>
                    </button>

                    <!-- BẬT / TẮT -->
                    <button
                      class="ss-icon-btn danger"
                      :title="
                        isActive(x)
                          ? 'Ngừng hoạt động'
                          : 'Kích hoạt'
                      "
                      @click="toggle(x)"
                    >
                      <i class="bi bi-power"></i>

                    </button>
                                        <!-- XÓA -->
                    <button
                      class="ss-icon-btn danger"
                      title="Xóa đợt giảm giá"
                      @click="removeCampaign(x)"
                    >
                      <i class="bi bi-trash"></i>
                    </button>

                  </div>

                </td>

              </tr>

              <!-- KHÔNG CÓ DỮ LIỆU -->
              <tr v-if="!paged.length">
                <td
                  colspan="8"
                  class="ss-empty"
                >
                  <i class="bi bi-inbox"></i>

                  Không có đợt giảm giá phù hợp.
                </td>
              </tr>

            </tbody>

          </table>

        </div>

        <!-- PHÂN TRANG -->
        <div class="ss-foot">

          <select
            class="ss-select"
            v-model.number="pageSize"
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

            <!-- PREVIOUS -->
            <button
              :disabled="page === 1"
              @click="page--"
            >
              <i class="bi bi-chevron-left"></i>
            </button>

            <!-- PAGE -->
            <button
              v-for="n in totalPages"
              :key="n"
              :class="{ active: n === page }"
              @click="page = n"
            >
              {{ n }}
            </button>

            <!-- NEXT -->
            <button
              :disabled="page === totalPages"
              @click="page++"
            >
              <i class="bi bi-chevron-right"></i>
            </button>

          </div>

        </div>

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

@media (max-width: 1000px) {
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
</style>