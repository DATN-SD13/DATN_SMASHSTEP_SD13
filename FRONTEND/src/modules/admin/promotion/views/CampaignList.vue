<script setup>
import * as XLSX from "xlsx";
import AdminLayout from "../../../../layouts/AdminLayout.vue";
import Select2Input from "../components/Select2Input.vue";
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import api from "../../../../utils/api";
import {
  confirmAction,
  showSuccess,
  showError,
} from "../../../../utils/feedback";

const router = useRouter();
const keyword = ref("");
const status = ref("all");
const start = ref("");
const end = ref("");
const rows = ref([]);
const loading = ref(false);
const errorMessage = ref("");
const pageSize = ref(5);
const page = ref(1);
const totalElements = ref(0);
const totalPages = ref(1);
const saving = ref(false);
const exporting = ref(false);
let requestId = 0;
let searchTimer;

const statusOptions = [
  { value: "all", label: "Tất cả trạng thái" },
  { value: 1, label: "Đã bật" },
  { value: 0, label: "Đã tắt" },
];
const visiblePages = computed(() => {
  const pages = [];
  const maxVisible = 5;

  let from = Math.max(1, page.value - Math.floor(maxVisible / 2));

  let to = Math.min(totalPages.value, from + maxVisible - 1);

  from = Math.max(1, to - maxVisible + 1);

  for (let i = from; i <= to; i++) {
    pages.push(i);
  }

  return pages;
});

function formatDate(value) {
  if (!value) return "—";
  const [year, month, day] = String(value).substring(0, 10).split("-");
  return year && month && day ? `${day}/${month}/${year}` : "—";
}

async function loadCampaigns() {
  const currentRequest = ++requestId;

  // Kiểm tra khoảng ngày lọc
  if (start.value && end.value && start.value > end.value) {
    rows.value = [];
    totalElements.value = 0;
    totalPages.value = 1;
    errorMessage.value = "Ngày bắt đầu không được lớn hơn ngày kết thúc";
    loading.value = false;
    return;
  }

  loading.value = true;
  errorMessage.value = "";

  try {
    const response = await api.get("/dot-giam-gia", {
      params: {
        ma: keyword.value.trim() || undefined,
        trangThai: status.value === "all" ? undefined : Number(status.value),
        tuNgay: start.value || undefined,
        denNgay: end.value || undefined,
        page: page.value,
        size: pageSize.value,
      },
    });

    // Bỏ qua response cũ nếu đã có request mới
    if (currentRequest !== requestId) return;

    const data = response.data?.data;

    totalElements.value = data?.totalElements ?? 0;
    totalPages.value = Math.max(1, data?.totalPages ?? 1);

    // Nếu trang hiện tại vượt tổng số trang
    if (page.value > totalPages.value) {
      page.value = totalPages.value;
      return;
    }

    rows.value = (data?.content || []).map((item) => ({
      id: item.id,
      code: item.code || "—",
      name: item.name || "—",
      value: item.discountValue == null ? "—" : `${item.discountValue}%`,
      start: formatDate(item.startDate),
      end: formatDate(item.endDate),
      status: item.statusLabel || "—",
      statusValue: item.status,
      timeStatus: item.timeStatus,
    }));
  } catch (error) {
    if (currentRequest !== requestId) return;

    rows.value = [];
    totalElements.value = 0;
    totalPages.value = 1;

    errorMessage.value =
      error.response?.data?.message || "Không thể tải danh sách đợt giảm giá.";
  } finally {
    if (currentRequest === requestId) {
      loading.value = false;
    }
  }
}

watch([keyword, status, start, end, pageSize], () => {
  window.clearTimeout(searchTimer);

  // Vô hiệu hóa dữ liệu từ request cũ
  ++requestId;

  // Đảm bảo không bị kẹt loading
  loading.value = false;

  if (page.value !== 1) {
    page.value = 1;
    return;
  }

  searchTimer = window.setTimeout(() => {
    loadCampaigns();
  }, 250);
});

watch(page, () => {
  window.clearTimeout(searchTimer);
  loadCampaigns();
});

const isActive = (x) => Number(x.statusValue) === 1;
function getDisplayStatus(x) {
  if (!isActive(x)) return "Ngừng hoạt động";

  switch (x.timeStatus) {
    case "SAP_DIEN_RA":
      return "Sắp diễn ra";

    case "DANG_DIEN_RA":
      return "Đang diễn ra";

    case "DA_KET_THUC":
      return "Đã kết thúc";

    default:
      return x.status || "—";
  }
}

function getStatusClass(x) {
  if (!isActive(x)) return "danger";

  switch (x.timeStatus) {
    case "DANG_DIEN_RA":
      return "success";

    case "SAP_DIEN_RA":
      return "warning";

    case "DA_KET_THUC":
      return "secondary";

    default:
      return "secondary";
  }
}
async function toggle(x) {
  if (saving.value) return;
  const newStatus = isActive(x) ? 0 : 1;
  confirmAction(
    newStatus === 1
      ? `Bạn có chắc muốn kích hoạt ${x.code}?`
      : `Bạn có chắc muốn ngừng hoạt động ${x.code}?`,
    async () => {
      if (saving.value) return;
      saving.value = true;
      try {
        await api.patch(
          `/dot-giam-gia/${encodeURIComponent(x.code)}/trang-thai`,
          { status: newStatus },
        );
        await loadCampaigns();
        showSuccess(
          newStatus === 1
            ? "Kích hoạt thành công!"
            : "Ngừng hoạt động thành công!",
        );
      } catch (error) {
        showError(
          error.response?.data?.message || "Không thể cập nhật trạng thái",
        );
      } finally {
        saving.value = false;
      }
    },
  );
}

function reset() {
  window.clearTimeout(searchTimer);

  const changed =
    keyword.value !== "" ||
    status.value !== "all" ||
    start.value !== "" ||
    end.value !== "";

  keyword.value = "";
  status.value = "all";
  start.value = "";
  end.value = "";

  if (page.value !== 1) {
    page.value = 1;
  } else if (!changed) {
    loadCampaigns();
  }
}

async function exportExcel() {
  if (exporting.value) return;

  if (start.value && end.value && start.value > end.value) {
    showError("Ngày bắt đầu không được lớn hơn ngày kết thúc");
    return;
  }

  try {
    exporting.value = true;

    const allCampaigns = [];
    const exportPageSize = 100;
    let currentPage = 1;
    let lastPage = 1;

    // Lấy tất cả trang phù hợp với bộ lọc hiện tại
    do {
      const response = await api.get("/dot-giam-gia", {
        params: {
          ma: keyword.value.trim() || undefined,
          trangThai: status.value === "all" ? undefined : Number(status.value),
          tuNgay: start.value || undefined,
          denNgay: end.value || undefined,
          page: currentPage,
          size: exportPageSize,
        },
      });

      const result = response.data?.data;

      if (response.data?.success === false || !result) {
        throw new Error(
          response.data?.message || "Không lấy được dữ liệu xuất Excel",
        );
      }

      allCampaigns.push(...(result.content || []));

      lastPage = Math.max(1, Number(result.totalPages || 1));
      currentPage++;
    } while (currentPage <= lastPage);

    if (allCampaigns.length === 0) {
      showError("Không có đợt giảm giá nào để xuất Excel");
      return;
    }

    // Tạo dữ liệu Excel
    const excelData = allCampaigns.map((item, index) => ({
      STT: index + 1,
      "Mã đợt giảm giá": item.code || "",
      "Tên đợt giảm giá": item.name || "",
      "Giá trị giảm (%)": Number(item.discountValue || 0),
      "Ngày bắt đầu": formatDate(item.startDate),
      "Ngày kết thúc": formatDate(item.endDate),
      "Trạng thái": getDisplayStatus({
        statusValue: item.status,
        timeStatus: item.timeStatus,
        status: item.statusLabel,
      }),
    }));

    const worksheet = XLSX.utils.json_to_sheet(excelData);

    // Độ rộng cột
    worksheet["!cols"] = [
      { wch: 7 },
      { wch: 20 },
      { wch: 40 },
      { wch: 20 },
      { wch: 18 },
      { wch: 18 },
      { wch: 22 },
    ];

    const workbook = XLSX.utils.book_new();

    XLSX.utils.book_append_sheet(workbook, worksheet, "Dot giam gia");

    // Ngày giờ tạo file
    const now = new Date();
    const timestamp = [
      now.getFullYear(),
      String(now.getMonth() + 1).padStart(2, "0"),
      String(now.getDate()).padStart(2, "0"),
    ].join("");

    XLSX.writeFile(workbook, `Danh_sach_dot_giam_gia_${timestamp}.xlsx`);

    showSuccess(`Xuất Excel thành công ${allCampaigns.length} đợt giảm giá!`);
  } catch (error) {
    console.error("Lỗi xuất Excel:", error);

    showError(
      error.response?.data?.message ||
        error.message ||
        "Không thể xuất file Excel",
    );
  } finally {
    exporting.value = false;
  }
}

function viewDetail(x) {
  router.push(`/dot-giam-gia/chi-tiet/${encodeURIComponent(x.code)}`);
}
onMounted(loadCampaigns);
onUnmounted(() => {
  ++requestId;
  window.clearTimeout(searchTimer);
});
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
            <span class="ss-label"> Tìm kiếm </span>

            <div class="ss-search">
              <i class="bi bi-search"></i>

              <input
                class="ss-input"
                v-model="keyword"
                placeholder="Tìm theo mã đợt giảm giá..."
              />
            </div>
          </div>

          <!-- NGÀY BẮT ĐẦU -->
          <div class="ss-field">
            <span class="ss-label"> Ngày bắt đầu </span>

            <input class="ss-input" type="date" v-model="start" />
          </div>

          <!-- NGÀY KẾT THÚC -->
          <div class="ss-field">
            <span class="ss-label"> Ngày kết thúc </span>

            <input class="ss-input" type="date" v-model="end" />
          </div>

          <!-- TRẠNG THÁI -->
          <div class="ss-field">
            <span class="ss-label"> Trạng thái </span>

            <Select2Input
              v-model="status"
              :options="statusOptions"
              placeholder="Chọn trạng thái"
            />
          </div>
        </div>

        <!-- BUTTON -->
        <div class="ss-actions">
          <button class="ss-btn" @click="reset">Đặt lại bộ lọc</button>

          <button
            type="button"
            class="ss-btn"
            :disabled="exporting || loading"
            @click="exportExcel"
          >
            <i
              class="bi"
              :class="exporting ? 'bi-arrow-repeat' : 'bi-file-earmark-excel'"
            ></i>

            {{ exporting ? "Đang xuất Excel..." : "Xuất Excel" }}
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
          <h2>Danh sách các đợt giảm giá</h2>

          <span class="ss-spacer"></span>

          <span class="ss-count"> {{ totalElements }} bản ghi hiển thị. </span>
        </div>

        <p v-if="errorMessage" class="ss-empty" role="alert">
          {{ errorMessage }}
        </p>
        <!-- LOADING -->
        <div v-if="loading" class="ss-empty">Đang tải dữ liệu...</div>

        <!-- TABLE -->
        <div v-else class="ss-table-wrap">
          <table class="ss-table">
            <thead>
              <tr>
                <th class="w-stt c">STT</th>

                <th>Mã</th>

                <th>Tên</th>

                <th>Giá trị</th>

                <th>Ngày bắt đầu</th>

                <th>Ngày kết thúc</th>

                <th>Trạng thái</th>

                <th>Hành động</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="(x, i) in rows" :key="x.code">
                <!-- STT -->
                <td class="c">
                  {{ (page - 1) * pageSize + i + 1 }}
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
                  <span class="ss-pill dot" :class="getStatusClass(x)">
                    {{ getDisplayStatus(x) }}
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
                      :title="isActive(x) ? 'Ngừng hoạt động' : 'Kích hoạt'"
                      :disabled="saving"
                      @click="toggle(x)"
                    >
                      <i class="bi bi-power"></i>
                    </button>
                    <!-- XÓA -->
                  </div>
                </td>
              </tr>

              <!-- KHÔNG CÓ DỮ LIỆU -->
              <tr v-if="!rows.length">
                <td colspan="8" class="ss-empty">
                  <i class="bi bi-inbox"></i>

                  Không có đợt giảm giá phù hợp.
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- PHÂN TRANG -->
        <div class="ss-foot">
          <select class="ss-select" v-model.number="pageSize">
            <option :value="5">5</option>

            <option :value="10">10</option>

            <option :value="20">20</option>
          </select>

          <div class="ss-pages">
            <!-- PREVIOUS -->
            <button :disabled="page === 1" @click="page--">
              <i class="bi bi-chevron-left"></i>
            </button>

            <!-- PAGE -->
            <button
              v-for="n in visiblePages"
              :key="n"
              :class="{ active: n === page }"
              @click="page = n"
            >
              {{ n }}
            </button>

            <!-- NEXT -->
            <button :disabled="page === totalPages" @click="page++">
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

@media (max-width: 1000px) {
  .filter-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
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
.ss-pill.warning {
  background: #fff7ed;
  color: #c2410c;
}

.ss-pill.secondary {
  background: #f1f5f9;
  color: #475569;
}
</style>
