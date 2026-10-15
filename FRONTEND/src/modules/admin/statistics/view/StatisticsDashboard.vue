<script setup>
import { computed, ref, onMounted, watch } from "vue";
import AdminLayout from "../../../../layouts/AdminLayout.vue";

import {
  getStatisticsChart,
  getStatisticsSummary,
  getStatisticsPeriod,
  getStatisticsOrderStatus,
  getStatisticsEmployee,
  getStatisticsProduct,
} from "../service/statisticsService";

import { Bar, Line, Pie } from "vue-chartjs";

import * as XLSX from "xlsx";

import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  LineElement,
  PointElement,
  ArcElement,
  Tooltip,
  Legend,
  Title,
} from "chart.js";

ChartJS.register(
  CategoryScale,
  LinearScale,
  BarElement,
  LineElement,
  PointElement,
  ArcElement,
  Tooltip,
  Legend,
  Title,
);

function formatDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

const today = new Date();

const fromDate = ref(
  formatDate(new Date(today.getFullYear(), today.getMonth(), 1)),
);
const toDate = ref(formatDate(today));

const groupBy = ref("day");
const selectedBrand = ref("");
const brandSearch = ref("");

// Sẽ lấy dữ liệu thật từ API ở bước kết nối backend
const brandOptions = ref([]);
const dashboardSummary = ref(null);

const orderStatusData = ref([]);

const orderStatusColors = {
  waiting: "#f59e0b",
  confirmed: "#3b82f6",
  ready: "#8b5cf6",
  shipping: "#06b6d4",
  delivered: "#10b981",
  done: "#22c55e",
  cancel: "#ef4444",
  refund: "#a855f7",
  pending: "#64748b",
};

// Chỉ lấy trạng thái có hóa đơn
const activeOrderStatuses = computed(() => {
  return orderStatusData.value.filter((item) => Number(item.soLuong) > 0);
});

// Tổng số hóa đơn
const totalOrderStatuses = computed(() => {
  return orderStatusData.value.reduce(
    (total, item) => total + Number(item.soLuong || 0),
    0,
  );
});

// Dữ liệu biểu đồ tròn
const orderStatusChart = computed(() => ({
  labels: activeOrderStatuses.value.map((item) => item.ten),
  datasets: [
    {
      label: "Số đơn hàng",
      data: activeOrderStatuses.value.map((item) => Number(item.soLuong)),
      backgroundColor: activeOrderStatuses.value.map(
        (item) => orderStatusColors[item.khoa] || "#94a3b8",
      ),
      borderColor: "#ffffff",
      borderWidth: 3,
      hoverOffset: 8,
    },
  ],
}));

const orderStatusChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      enabled: true,
    },
  },
};

// Phía dưới phải là 2 dòng này

const loadingOrderStatus = ref(false);
const orderStatusError = ref("");

async function loadOrderStatus() {
  if (!fromDate.value || !toDate.value || fromDate.value > toDate.value) {
    orderStatusData.value = [];
    return;
  }

  loadingOrderStatus.value = true;
  orderStatusError.value = "";

  try {
    const response = await getStatisticsOrderStatus({
      tuNgay: fromDate.value,
      denNgay: toDate.value,
    });

    if (!response.data?.success) {
      throw new Error(
        response.data?.message || "Không thể tải trạng thái đơn hàng",
      );
    }

    orderStatusData.value = response.data.data?.theoTrangThai || [];

    console.log("TRẠNG THÁI ĐƠN HÀNG:", orderStatusData.value);
  } catch (error) {
    console.error("Lỗi thống kê trạng thái:", error);

    orderStatusError.value =
      error.response?.data?.message ||
      error.message ||
      "Không thể tải dữ liệu trạng thái đơn hàng";

    orderStatusData.value = [];
  } finally {
    loadingOrderStatus.value = false;
  }
}

const productData = ref([]);
const loadingProduct = ref(false);

const productError = ref("");

const productSearch = ref("");
const productStockFilter = ref("all");
const productSort = ref("sold");
const productPage = ref(1);
const productPageSize = 5;

// Tìm kiếm, lọc tồn kho và sắp xếp sản phẩm
const filteredProducts = computed(() => {
  const keyword = productSearch.value.trim().toLowerCase();

  const result = productData.value.filter((product) => {
    const name = String(product.name || "").toLowerCase();
    const code = String(product.code || "").toLowerCase();
    const stock = Number(product.stock || 0);

    const matchesSearch = name.includes(keyword) || code.includes(keyword);

    if (productStockFilter.value === "inStock") {
      return matchesSearch && stock > 0;
    }

    if (productStockFilter.value === "outOfStock") {
      return matchesSearch && stock <= 0;
    }

    return matchesSearch;
  });

  return result.sort((a, b) => {
    if (productSort.value === "name") {
      return String(a.name || "").localeCompare(String(b.name || ""), "vi");
    }

    return (
      Number(b[productSort.value] || 0) - Number(a[productSort.value] || 0)
    );
  });
});

// Phân trang 5 sản phẩm
const productTotalPages = computed(() =>
  Math.max(1, Math.ceil(filteredProducts.value.length / productPageSize)),
);

const paginatedProducts = computed(() => {
  const start = (productPage.value - 1) * productPageSize;

  return filteredProducts.value.slice(start, start + productPageSize);
});

// Quay lại trang đầu khi thay đổi bộ lọc
watch([productSearch, productStockFilter, productSort, productData], () => {
  productPage.value = 1;
});

function resetProductFilters() {
  function exportProductExcel() {
    if (!filteredProducts.value.length) {
      alert("Không có dữ liệu sản phẩm để xuất Excel");
      return;
    }

    const rows = filteredProducts.value.map((product, index) => ({
      STT: index + 1,
      "Mã sản phẩm": product.code,
      "Tên sản phẩm": product.name,
      "Số lượng đã bán": Number(product.sold || 0),
      "Số lượng hoàn tiền": Number(product.returned || 0),
      "Doanh thu (VNĐ)": Number(product.revenue || 0),
      "Tồn kho": Number(product.stock || 0),
    }));

    const worksheet = XLSX.utils.json_to_sheet(rows);

    worksheet["!cols"] = [
      { wch: 8 },
      { wch: 18 },
      { wch: 35 },
      { wch: 20 },
      { wch: 22 },
      { wch: 22 },
      { wch: 15 },
    ];

    const workbook = XLSX.utils.book_new();

    XLSX.utils.book_append_sheet(workbook, worksheet, "Thong ke san pham");

    XLSX.writeFile(
      workbook,
      `thong-ke-san-pham_${fromDate.value}_${toDate.value}.xlsx`,
    );
  }

  productSearch.value = "";
  productStockFilter.value = "all";
  productSort.value = "sold";
  productPage.value = 1;
}

async function loadProduct() {
  if (!fromDate.value || !toDate.value || fromDate.value > toDate.value) {
    productData.value = [];
    return;
  }

  loadingProduct.value = true;
  productError.value = "";

  try {
    const response = await getStatisticsProduct({
      tuNgay: fromDate.value,
      denNgay: toDate.value,
    });

    if (!response.data?.success) {
      throw new Error(
        response.data?.message || "Không thể tải thống kê sản phẩm",
      );
    }

    productData.value = response.data.data || [];

    console.log("TỔNG QUAN SẢN PHẨM:", productData.value);
  } catch (error) {
    console.error("Lỗi thống kê sản phẩm:", error);

    productError.value =
      error.response?.data?.message ||
      error.message ||
      "Không thể tải dữ liệu sản phẩm";

    productData.value = [];
  } finally {
    loadingProduct.value = false;
  }
}

const employeeData = ref([]);

const employeeSearch = ref("");
const employeeSort = ref("revenue");
const employeePage = ref(1);
const employeePageSize = 5;

// Tìm kiếm và sắp xếp nhân viên
const filteredEmployees = computed(() => {
  const keyword = employeeSearch.value.trim().toLowerCase();

  const result = employeeData.value.filter((employee) => {
    const name = String(employee.name || "").toLowerCase();
    const code = String(employee.code || "").toLowerCase();

    return name.includes(keyword) || code.includes(keyword);
  });

  return result.sort((a, b) => {
    if (employeeSort.value === "name") {
      return String(a.name || "").localeCompare(String(b.name || ""), "vi");
    }

    return (
      Number(b[employeeSort.value] || 0) - Number(a[employeeSort.value] || 0)
    );
  });
});

// Tổng số trang
const employeeTotalPages = computed(() =>
  Math.max(1, Math.ceil(filteredEmployees.value.length / employeePageSize)),
);

// Dữ liệu hiển thị trên trang hiện tại
const paginatedEmployees = computed(() => {
  const start = (employeePage.value - 1) * employeePageSize;

  return filteredEmployees.value.slice(start, start + employeePageSize);
});

// Đưa về trang đầu khi tìm kiếm hoặc sắp xếp
watch([employeeSearch, employeeSort], () => {
  employeePage.value = 1;
});

// Reset bộ lọc nhân viên
function resetEmployeeFilters() {
  employeeSearch.value = "";
  employeeSort.value = "revenue";
  employeePage.value = 1;
}

// Xuất danh sách nhân viên ra Excel
function exportEmployeeExcel() {
  if (!filteredEmployees.value.length) {
    alert("Không có dữ liệu nhân viên để xuất Excel");
    return;
  }

  const rows = filteredEmployees.value.map((employee, index) => ({
    STT: index + 1,
    "Mã nhân viên": employee.code,
    "Tên nhân viên": employee.name,
    "Tổng đơn hàng": Number(employee.totalOrders || 0),
    "Sản phẩm đã bán": Number(employee.productsSold || 0),
    "Doanh thu (VNĐ)": Number(employee.revenue || 0),
  }));

  const sheet = XLSX.utils.json_to_sheet(rows);

  sheet["!cols"] = [
    { wch: 8 },
    { wch: 18 },
    { wch: 28 },
    { wch: 18 },
    { wch: 20 },
    { wch: 22 },
  ];

  const workbook = XLSX.utils.book_new();

  XLSX.utils.book_append_sheet(workbook, sheet, "Doanh thu nhan vien");

  XLSX.writeFile(
    workbook,
    `thong-ke-nhan-vien_${fromDate.value}_${toDate.value}.xlsx`,
  );
}

const loadingEmployee = ref(false);
const employeeError = ref("");

async function loadEmployee() {
  if (!fromDate.value || !toDate.value || fromDate.value > toDate.value) {
    employeeData.value = [];
    return;
  }

  loadingEmployee.value = true;
  employeeError.value = "";

  try {
    const response = await getStatisticsEmployee({
      tuNgay: fromDate.value,
      denNgay: toDate.value,
    });

    if (!response.data?.success) {
      throw new Error(
        response.data?.message || "Không thể tải thống kê nhân viên",
      );
    }

    employeeData.value = response.data.data || [];

    console.log("DOANH THU NHÂN VIÊN:", employeeData.value);
  } catch (error) {
    console.error("Lỗi thống kê nhân viên:", error);

    employeeError.value =
      error.response?.data?.message ||
      error.message ||
      "Không thể tải dữ liệu nhân viên";

    employeeData.value = [];
  } finally {
    loadingEmployee.value = false;
  }
}

const periodData = ref([]);
const loadingPeriod = ref(false);
const periodError = ref("");

async function loadPeriod() {
  loadingPeriod.value = true;
  periodError.value = "";

  try {
    const response = await getStatisticsPeriod();

    if (!response.data?.success) {
      throw new Error(
        response.data?.message || "Không thể tải thống kê chu kỳ",
      );
    }

    periodData.value = response.data.data || [];
  } catch (error) {
    console.error("Lỗi thống kê chu kỳ:", error);
    periodError.value =
      error.response?.data?.message ||
      error.message ||
      "Không thể tải dữ liệu thống kê chu kỳ";

    periodData.value = [];
  } finally {
    loadingPeriod.value = false;
  }
}

function growthText(value) {
  if (value == null) return "—";

  const growth = Number(value);

  return `${growth > 0 ? "+" : ""}${growth.toLocaleString("vi-VN", {
    maximumFractionDigits: 2,
  })}%`;
}

const loadingSummary = ref(false);
const summaryError = ref("");

async function loadSummary() {
  if (!fromDate.value || !toDate.value || fromDate.value > toDate.value) {
    dashboardSummary.value = null;
    return;
  }

  loadingSummary.value = true;
  summaryError.value = "";

  try {
    const response = await getStatisticsSummary({
      tuNgay: fromDate.value,
      denNgay: toDate.value,
    });

    if (!response.data?.success) {
      throw new Error(response.data?.message || "Không thể tải doanh thu");
    }

    dashboardSummary.value = response.data.data;

    console.log("TỔNG QUAN DOANH THU:", dashboardSummary.value);
  } catch (error) {
    console.error("Lỗi thống kê doanh thu:", error);

    summaryError.value =
      error.response?.data?.message ||
      error.message ||
      "Không thể tải tổng quan doanh thu";

    dashboardSummary.value = null;
  } finally {
    loadingSummary.value = false;
  }
}

const salesChartType = ref("bar");
const brandChartType = ref("pie");
const brandChartSort = ref("revenue");

const salesChartData = ref([]);
const brandChartData = ref([]);

const brandColors = [
  "#ef4444",
  "#f97316",
  "#f59e0b",
  "#10b981",
  "#06b6d4",
  "#64748b",
  "#8b5cf6",
];

const sortedBrands = computed(() => {
  return [...brandChartData.value].sort(
    (a, b) =>
      Number(b[brandChartSort.value] || 0) -
      Number(a[brandChartSort.value] || 0),
  );
});

const brandChart = computed(() => ({
  labels: sortedBrands.value.map((item) => item.name),
  datasets: [
    {
      label: brandChartSort.value === "revenue" ? "Doanh thu" : "Số lượng bán",
      data: sortedBrands.value.map((item) =>
        Number(item[brandChartSort.value] || 0),
      ),
      backgroundColor: sortedBrands.value.map(
        (_, index) => brandColors[index % brandColors.length],
      ),
      borderColor: "#ffffff",
      borderWidth: 2,
    },
  ],
}));

const brandLineChart = computed(() => ({
  labels: sortedBrands.value.map((item) => item.name),
  datasets: [
    {
      label: brandChartSort.value === "revenue" ? "Doanh thu" : "Số lượng bán",
      data: sortedBrands.value.map((item) =>
        Number(item[brandChartSort.value] || 0),
      ),
      borderColor: "#ef4444",
      backgroundColor: "#ef4444",
      borderWidth: 2,
      tension: 0.35,
      pointRadius: 4,
      fill: false,
    },
  ],
}));

const brandPieOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: true,
      position: "bottom",
      labels: {
        boxWidth: 10,
        padding: 12,
        font: { size: 10 },
      },
    },
  },
};

const brandLineOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
  },
  scales: {
    y: {
      beginAtZero: true,
      ticks: { precision: 0 },
      grid: { color: "#edf0f5" },
    },
    x: {
      grid: { display: false },
    },
  },
};

const salesChart = computed(() => ({
  labels: salesChartData.value.map((item) => {
    const date = item.label || "";
    return date.length === 10
      ? `${date.slice(8, 10)}/${date.slice(5, 7)}`
      : date;
  }),

  datasets: [
    {
      label: "Sản phẩm bán được",
      data: salesChartData.value.map((item) => Number(item.quantity || 0)),
      backgroundColor: "#dc172c",
      borderColor: "#dc172c",
      borderWidth: 2,
      borderRadius: 8,
      maxBarThickness: 42,
      tension: 0.35,
      pointRadius: 4,
      fill: false,
    },
  ],
}));

const salesChartOptions = {
  responsive: true,
  maintainAspectRatio: false,

  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      enabled: true,
    },
  },

  scales: {
    x: {
      grid: {
        display: false,
      },
      ticks: {
        color: "#64748b",
      },
    },

    y: {
      beginAtZero: true,
      ticks: {
        precision: 0,
        stepSize: 1,
        color: "#64748b",
      },
      grid: {
        color: "#edf0f5",
      },
    },
  },
};

const loadingCharts = ref(false);
const chartError = ref("");

async function loadCharts() {
  if (!fromDate.value || !toDate.value || fromDate.value > toDate.value) {
    chartError.value = "Khoảng ngày không hợp lệ";
    return;
  }

  loadingCharts.value = true;
  chartError.value = "";

  try {
    const response = await getStatisticsChart({
      tuNgay: fromDate.value,
      denNgay: toDate.value,
      nhomTheo: groupBy.value,
      thuongHieuId: selectedBrand.value || undefined,
    });

    if (!response.data?.success) {
      throw new Error(
        response.data?.message || "Không thể lấy dữ liệu biểu đồ",
      );
    }

    salesChartData.value = response.data.data?.sales || [];
    brandChartData.value = response.data.data?.brands || [];

    console.log("DỮ LIỆU BÁN HÀNG:", salesChartData.value);
    console.log("DỮ LIỆU THƯƠNG HIỆU:", brandChartData.value);
  } catch (error) {
    console.error("LỖI THỐNG KÊ:", error);

    chartError.value =
      error.response?.data?.message ||
      error.message ||
      "Không thể tải dữ liệu biểu đồ";

    salesChartData.value = [];
    brandChartData.value = [];
  } finally {
    loadingCharts.value = false;
  }
}

onMounted(() => {
  loadCharts();
  loadSummary();
  loadPeriod();
  loadOrderStatus();
  loadEmployee();
  loadProduct();
});

watch([fromDate, toDate, groupBy, selectedBrand], () => {
  loadCharts();
});

watch([fromDate, toDate], () => {
  loadSummary();
  loadOrderStatus();
  loadEmployee();
  loadProduct();
});

const periodLabel = computed(() => {
  const display = (value) => {
    if (!value) return "";
    const [year, month, day] = value.split("-");
    return `${day}/${month}/${year}`;
  };

  return `${display(fromDate.value)} đến ${display(toDate.value)}`;
});

function quickFilter(type) {
  const now = new Date();

  let start = new Date(now.getFullYear(), now.getMonth(), now.getDate());

  let end = new Date(start);

  if (type === "week") {
    start.setDate(start.getDate() - 6);
  }

  if (type === "month") {
    start = new Date(now.getFullYear(), now.getMonth(), 1);
  }

  if (type === "lastMonth") {
    start = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    end = new Date(now.getFullYear(), now.getMonth(), 0);
  }

  if (type === "year") {
    start = new Date(now.getFullYear(), 0, 1);
  }

  fromDate.value = formatDate(start);
  toDate.value = formatDate(end);
}

function resetFilters() {
  quickFilter("month");
  groupBy.value = "day";
  selectedBrand.value = "";
  brandSearch.value = "";
}

function money(value) {
  if (value == null) return "—";
  return `${Number(value).toLocaleString("vi-VN")} đ`;
}

function number(value) {
  if (value == null) return "—";
  return Number(value).toLocaleString("vi-VN");
}
</script>

<template>
  <AdminLayout>
    <main class="statistics-page">
      <!-- BỘ LỌC TỔNG QUAN -->
      <section class="filter-card">
        <div class="filter-header">
          <h3>
            <i class="bi bi-funnel"></i>
            Bộ lọc tổng quan
          </h3>

          <div class="quick-filters">
            <button type="button" @click="quickFilter('today')">Hôm nay</button>

            <button type="button" @click="quickFilter('week')">
              7 ngày qua
            </button>

            <button type="button" @click="quickFilter('month')">
              Tháng này
            </button>

            <button type="button" @click="quickFilter('lastMonth')">
              Tháng trước
            </button>

            <button type="button" @click="quickFilter('year')">Năm nay</button>
          </div>
        </div>

        <div class="filter-grid">
          <div class="filter-field">
            <label>Từ ngày</label>
            <input v-model="fromDate" type="date" />
          </div>

          <div class="filter-field">
            <label>Đến ngày</label>
            <input v-model="toDate" type="date" />
          </div>

          <div class="filter-field">
            <label>Tổng quan</label>
            <select v-model="groupBy">
              <option value="day">Theo ngày</option>
              <option value="month">Theo tháng</option>
              <option value="year">Theo năm</option>
            </select>
          </div>

          <div class="filter-field">
            <label>Thương hiệu</label>
            <select v-model="selectedBrand">
              <option value="">Tất cả thương hiệu</option>

              <option
                v-for="brand in brandOptions"
                :key="brand.id"
                :value="brand.id"
              >
                {{ brand.name }}
              </option>
            </select>
          </div>

          <div class="filter-field">
            <label>Tìm theo thương hiệu</label>
            <input
              v-model="brandSearch"
              placeholder="Nhập tên thương hiệu..."
            />
          </div>

          <button type="button" class="reset-button" @click="resetFilters">
            <i class="bi bi-arrow-repeat"></i>
            Reset
          </button>
        </div>
      </section>

      <!-- THANH THÔNG TIN THỜI GIAN -->
      <section class="information-bar">
        <div class="information-left">
          <span class="blue-dot"></span>
          <span>
            Đang hiển thị dữ liệu tổng quan từ ngày
            <strong>{{ periodLabel }}</strong>
          </span>
        </div>

        <div class="information-right">
          <span>Cập nhật lúc: — · Tự động lúc 22:00</span>

          <button type="button" class="refresh-button" disabled>
            <i class="bi bi-arrow-repeat"></i>
            Cập nhật doanh thu
          </button>
        </div>
      </section>

      <p v-if="fromDate && toDate && fromDate > toDate" class="date-error">
        Từ ngày không được lớn hơn đến ngày.
      </p>

      <!-- 7 Ô DOANH THU -->
      <section class="summary-section">
        <!-- HÀNG TRÊN -->
        <div class="summary-grid top-grid">
          <div class="stat-card">
            <div class="stat-icon mint">
              <i class="bi bi-graph-up-arrow"></i>
            </div>
            <strong>{{ money(dashboardSummary?.totalRevenue) }}</strong>
            <span>Tổng doanh thu</span>
          </div>

          <div class="stat-card">
            <div class="stat-icon green">
              <i class="bi bi-graph-up-arrow"></i>
            </div>
            <strong>{{ money(dashboardSummary?.cashRevenue) }}</strong>
            <span>Tổng tiền mặt</span>
          </div>

          <div class="stat-card">
            <div class="stat-icon purple">
              <i class="bi bi-credit-card"></i>
            </div>
            <strong>{{ money(dashboardSummary?.transferRevenue) }}</strong>
            <span>Tổng tiền chuyển khoản</span>
          </div>
        </div>

        <!-- HÀNG DƯỚI -->
        <div class="summary-grid bottom-grid">
          <div class="stat-card">
            <div class="stat-icon blue">
              <i class="bi bi-cart3"></i>
            </div>
            <strong>{{ number(dashboardSummary?.totalOrders) }}</strong>
            <span>Tổng đơn hàng</span>
          </div>

          <div class="stat-card">
            <div class="stat-icon purple">
              <i class="bi bi-credit-card"></i>
            </div>
            <strong>{{ money(dashboardSummary?.averageOrderValue) }}</strong>
            <span>Giá trị trung bình đơn (AOV)</span>
          </div>

          <div class="stat-card">
            <div class="stat-icon yellow">
              <i class="bi bi-box-seam"></i>
            </div>
            <strong>{{ number(dashboardSummary?.productsSold) }}</strong>
            <span>Sản phẩm đã bán</span>
          </div>

          <div class="stat-card">
            <div class="stat-icon red">
              <i class="bi bi-people"></i>
            </div>
            <strong>{{ number(dashboardSummary?.newCustomers) }}</strong>
            <span>Khách mới</span>
          </div>
        </div>
      </section>

      <!-- HAI BIỂU ĐỒ THỐNG KÊ -->
      <section class="charts-grid">
        <!-- BIỂU ĐỒ SẢN PHẨM BÁN ĐƯỢC -->
        <div class="chart-card">
          <div class="chart-header">
            <h3>
              <i class="bi bi-bar-chart-line"></i>
              Sản phẩm bán được theo ngày
            </h3>

            <div class="chart-actions">
              <button
                type="button"
                :class="{ active: salesChartType === 'bar' }"
                @click="salesChartType = 'bar'"
              >
                Cột
              </button>
              <button
                type="button"
                :class="{ active: salesChartType === 'line' }"
                @click="salesChartType = 'line'"
              >
                Đường
              </button>
            </div>
          </div>

          <div class="chart-content">
            <div v-if="loadingCharts" class="chart-empty">
              Đang tải dữ liệu biểu đồ...
            </div>

            <div v-else-if="chartError" class="chart-empty">
              {{ chartError }}
            </div>

            <div v-else-if="salesChartData.length" class="chart-canvas">
              <Bar
                v-if="salesChartType === 'bar'"
                :data="salesChart"
                :options="salesChartOptions"
              />

              <Line v-else :data="salesChart" :options="salesChartOptions" />
            </div>

            <div v-else class="chart-empty">
              Chưa có dữ liệu sản phẩm bán được
            </div>
          </div>
        </div>
        <!-- Đóng chart-card biểu đồ bên trái -->

        <!-- BIỂU ĐỒ THƯƠNG HIỆU -->
        <div class="chart-card">
          <div class="chart-header">
            <h3>
              <i class="bi bi-pie-chart"></i>
              Thương hiệu
            </h3>

            <div class="chart-right">
              <div class="chart-actions">
                <button
                  type="button"
                  :class="{ active: brandChartType === 'pie' }"
                  @click="brandChartType = 'pie'"
                >
                  Tròn
                </button>
                <button
                  type="button"
                  :class="{ active: brandChartType === 'line' }"
                  @click="brandChartType = 'line'"
                >
                  Đường
                </button>
              </div>

              <select v-model="brandChartSort">
                <option value="revenue">Theo doanh thu</option>
                <option value="quantity">Theo số lượng</option>
              </select>
            </div>
          </div>

          <div class="chart-content brand-chart-content">
            <div v-if="loadingCharts" class="chart-empty">
              Đang tải dữ liệu thương hiệu...
            </div>

            <div v-else-if="chartError" class="chart-empty">
              {{ chartError }}
            </div>

            <template v-else-if="brandChartData.length">
              <div class="brand-chart-canvas">
                <Pie
                  v-if="brandChartType === 'pie'"
                  :data="brandChart"
                  :options="brandPieOptions"
                />

                <Line
                  v-else
                  :data="brandLineChart"
                  :options="brandLineOptions"
                />
              </div>

              <div class="brand-list">
                <div
                  v-for="(brand, index) in sortedBrands"
                  :key="brand.name"
                  class="brand-list-item"
                >
                  <div class="brand-name">
                    <span
                      class="brand-dot"
                      :style="{
                        backgroundColor:
                          brandColors[index % brandColors.length],
                      }"
                    ></span>

                    <span>{{ brand.name }}</span>
                  </div>

                  <strong>
                    {{
                      brandChartSort === "revenue"
                        ? money(brand.revenue)
                        : number(brand.quantity)
                    }}
                  </strong>
                </div>
              </div>
            </template>

            <div v-else class="chart-empty">Chưa có dữ liệu thương hiệu</div>
          </div>
        </div>
      </section>

      <!-- TỔNG QUAN THEO CHU KỲ THỜI GIAN -->
      <section class="period-section">
        <div class="period-card">
          <div class="period-header">
            <h3>
              <i class="bi bi-calendar3"></i>
              Tổng quan theo chu kỳ thời gian
            </h3>
          </div>

          <div v-if="loadingPeriod" class="period-message">
            Đang tải dữ liệu...
          </div>

          <div v-else-if="periodError" class="period-message period-error">
            {{ periodError }}
          </div>

          <div v-else class="period-table-wrapper">
            <table class="period-table">
              <thead>
                <tr>
                  <th>Thời gian</th>
                  <th>Doanh thu</th>
                  <th>Số đơn</th>
                  <th>AOV</th>
                  <th>Tăng trưởng</th>
                </tr>
              </thead>

              <tbody>
                <tr v-for="row in periodData" :key="row.period">
                  <td class="period-name">
                    {{ row.period }}
                  </td>

                  <td>{{ money(row.revenue) }}</td>

                  <td>{{ number(row.orders) }}</td>

                  <td>{{ money(row.aov) }}</td>

                  <td>
                    <span
                      :class="{
                        'growth-positive': Number(row.growth) > 0,
                        'growth-negative':
                          row.growth != null && Number(row.growth) < 0,
                      }"
                    >
                      {{ growthText(row.growth) }}
                    </span>
                  </td>
                </tr>

                <tr v-if="!periodData.length">
                  <td colspan="5" class="period-message">
                    Chưa có dữ liệu thống kê
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- CƠ CẤU TRẠNG THÁI ĐƠN HÀNG -->
        <div class="order-status-card">
          <div class="period-header">
            <h3>
              <i class="bi bi-pie-chart"></i>
              Cơ cấu trạng thái đơn hàng
            </h3>
          </div>

          <div v-if="loadingOrderStatus" class="period-message">
            Đang tải dữ liệu...
          </div>

          <div v-else-if="orderStatusError" class="period-message period-error">
            {{ orderStatusError }}
          </div>

          <template v-else-if="activeOrderStatuses.length">
            <p class="order-status-total">
              Tổng số đơn hàng:
              <strong>{{ number(totalOrderStatuses) }}</strong>
            </p>

            <div class="order-status-canvas">
              <Pie
                :data="orderStatusChart"
                :options="orderStatusChartOptions"
              />
            </div>

            <div class="order-status-list">
              <div
                v-for="status in orderStatusData"
                :key="status.ma"
                class="order-status-item"
              >
                <span class="order-status-name">
                  <span
                    class="order-status-dot"
                    :style="{
                      backgroundColor:
                        orderStatusColors[status.khoa] || '#94a3b8',
                    }"
                  ></span>
                  {{ status.ten }}
                </span>

                <strong>{{ number(status.soLuong) }}</strong>
              </div>
            </div>
          </template>

          <div v-else class="period-message">
            Chưa có dữ liệu trạng thái đơn hàng
          </div>
        </div>
      </section>

      <!-- TỔNG QUAN DOANH THU NHÂN VIÊN -->
      <section class="employee-section">
        <div class="employee-card">
          <!-- TIÊU ĐỀ -->
          <div class="employee-header">
            <h3>
              <i class="bi bi-people"></i>
              Tổng quan doanh thu nhân viên
            </h3>

            <button
              type="button"
              class="employee-export-btn"
              @click="exportEmployeeExcel"
            >
              <i class="bi bi-file-earmark-excel"></i>
              Xuất Excel
            </button>
          </div>

          <!-- BỘ LỌC NHÂN VIÊN -->
          <div class="employee-filters">
            <input
              v-model="employeeSearch"
              type="text"
              placeholder="Tìm tên hoặc mã nhân viên..."
            />

            <select v-model="employeeSort">
              <option value="revenue">Doanh thu cao nhất</option>
              <option value="totalOrders">Số đơn nhiều nhất</option>
              <option value="productsSold">Sản phẩm bán nhiều nhất</option>
              <option value="name">Tên nhân viên A-Z</option>
            </select>

            <button
              type="button"
              class="employee-reset-btn"
              @click="resetEmployeeFilters"
            >
              <i class="bi bi-arrow-repeat"></i>
              Reset
            </button>
          </div>

          <!-- TRẠNG THÁI TẢI DỮ LIỆU -->
          <div v-if="loadingEmployee" class="period-message">
            Đang tải dữ liệu nhân viên...
          </div>

          <div v-else-if="employeeError" class="period-message period-error">
            {{ employeeError }}
          </div>

          <!-- BẢNG NHÂN VIÊN -->
          <div v-else class="employee-table-wrapper">
            <table class="employee-table">
              <thead>
                <tr>
                  <th>STT</th>
                  <th>Mã nhân viên</th>
                  <th>Tên nhân viên</th>
                  <th>Tổng đơn hàng</th>
                  <th>Sản phẩm đã bán</th>
                  <th>Doanh thu</th>
                </tr>
              </thead>

              <tbody>
                <tr
                  v-for="(employee, index) in paginatedEmployees"
                  :key="employee.id"
                >
                  <td>
                    {{ (employeePage - 1) * employeePageSize + index + 1 }}
                  </td>

                  <td>{{ employee.code }}</td>

                  <td class="employee-name">
                    {{ employee.name }}
                  </td>

                  <td>{{ number(employee.totalOrders) }}</td>

                  <td>{{ number(employee.productsSold) }}</td>

                  <td class="employee-revenue">
                    {{ money(employee.revenue) }}
                  </td>
                </tr>

                <tr v-if="!paginatedEmployees.length">
                  <td colspan="6" class="period-message">
                    Không tìm thấy nhân viên phù hợp
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- PHÂN TRANG -->
          <div
            v-if="
              !loadingEmployee && !employeeError && filteredEmployees.length
            "
            class="employee-pagination"
          >
            <span>
              Hiển thị {{ paginatedEmployees.length }} /
              {{ filteredEmployees.length }} nhân viên
            </span>

            <div class="employee-page-buttons">
              <button
                type="button"
                :disabled="employeePage <= 1"
                @click="employeePage--"
              >
                Trước
              </button>

              <span> Trang {{ employeePage }} / {{ employeeTotalPages }} </span>

              <button
                type="button"
                :disabled="employeePage >= employeeTotalPages"
                @click="employeePage++"
              >
                Sau
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- ===== TỔNG QUAN SẢN PHẨM ===== -->
      <section class="employee-section">
        <div class="employee-card">
          <!-- TIÊU ĐỀ -->

          <div class="employee-header">
            <h3>
              <i class="bi bi-box-seam"></i>
              Tổng quan sản phẩm
            </h3>

            <button
              type="button"
              class="employee-export-btn"
              @click="exportProductExcel"
            >
              <i class="bi bi-file-earmark-excel"></i>
              Xuất Excel
            </button>
          </div>

          <!-- BỘ LỌC -->
          <div class="employee-filters">
            <input
              v-model="productSearch"
              type="text"
              placeholder="Tìm mã hoặc tên sản phẩm..."
            />

            <select v-model="productStockFilter">
              <option value="all">Tất cả tồn kho</option>
              <option value="inStock">Còn hàng</option>
              <option value="outOfStock">Hết hàng</option>
            </select>

            <select v-model="productSort">
              <option value="sold">Bán nhiều nhất</option>
              <option value="revenue">Doanh thu cao nhất</option>
              <option value="stock">Tồn kho nhiều nhất</option>
              <option value="name">Tên sản phẩm A-Z</option>
            </select>

            <button
              type="button"
              class="employee-reset-btn"
              @click="resetProductFilters"
            >
              <i class="bi bi-arrow-repeat"></i>
              Reset
            </button>
          </div>

          <!-- TRẠNG THÁI API -->
          <div v-if="loadingProduct" class="period-message">
            Đang tải dữ liệu sản phẩm...
          </div>

          <div v-else-if="productError" class="period-message period-error">
            {{ productError }}
          </div>

          <!-- BẢNG SẢN PHẨM -->
          <div v-else class="employee-table-wrapper">
            <table class="employee-table">
              <thead>
                <tr>
                  <th>STT</th>
                  <th>Mã sản phẩm</th>
                  <th>Tên sản phẩm</th>
                  <th>Đã bán</th>
                  <th>Hoàn tiền</th>
                  <th>Doanh thu</th>
                  <th>Tồn kho</th>
                </tr>
              </thead>

              <tbody>
                <tr
                  v-for="(product, index) in paginatedProducts"
                  :key="product.id"
                >
                  <td>
                    {{ (productPage - 1) * productPageSize + index + 1 }}
                  </td>

                  <td>{{ product.code }}</td>

                  <td class="employee-name">
                    {{ product.name }}
                  </td>

                  <td>{{ number(product.sold) }}</td>

                  <td>{{ number(product.returned) }}</td>

                  <td class="employee-revenue">
                    {{ money(product.revenue) }}
                  </td>

                  <td>
                    <span
                      :style="{
                        color:
                          Number(product.stock) > 0 ? '#16a34a' : '#dc2626',
                        fontWeight: '600',
                      }"
                    >
                      {{ number(product.stock) }}
                    </span>
                  </td>
                </tr>

                <tr v-if="!paginatedProducts.length">
                  <td colspan="7" class="period-message">
                    Không tìm thấy sản phẩm phù hợp
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- PHÂN TRANG -->
          <div
            v-if="!loadingProduct && !productError && filteredProducts.length"
            class="employee-pagination"
          >
            <span>
              Hiển thị {{ paginatedProducts.length }} /
              {{ filteredProducts.length }} sản phẩm
            </span>

            <div class="employee-page-buttons">
              <button
                type="button"
                :disabled="productPage <= 1"
                @click="productPage--"
              >
                Trước
              </button>

              <span> Trang {{ productPage }} / {{ productTotalPages }} </span>

              <button
                type="button"
                :disabled="productPage >= productTotalPages"
                @click="productPage++"
              >
                Sau
              </button>
            </div>
          </div>
        </div>
      </section>
    </main>
  </AdminLayout>
</template>

<style scoped>
/* ===== TỔNG QUAN DOANH THU NHÂN VIÊN ===== */

.employee-section {
  margin-top: 20px;
  margin-bottom: 20px;
}

.employee-card {
  background: #ffffff;
  border: 1px solid #e7ebf2;
  border-radius: 9px;
  padding: 18px 20px;
  min-width: 0;
}

/* Tiêu đề và nút xuất Excel */

.employee-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf0f5;
  margin-bottom: 18px;
}

.employee-header h3 {
  font-size: 14px;
  font-weight: 700;
  color: #202b3d;
  margin: 0;
}

.employee-header h3 i {
  color: #dc3545;
  margin-right: 7px;
}

.employee-export-btn {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 9px 14px;
  border: 1px solid #16a34a;
  border-radius: 6px;
  background: #16a34a;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.employee-export-btn:hover {
  background: #15803d;
}

/* Bộ lọc tìm kiếm và sắp xếp */

.employee-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.employee-filters input {
  flex: 1;
  min-width: 200px;
  height: 39px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  color: #334155;
  font-size: 13px;
  outline: none;
}

.employee-filters select {
  min-width: 190px;
  height: 39px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #f8fafc;
  color: #334155;
  font-size: 13px;
  cursor: pointer;
}

.employee-filters input:focus,
.employee-filters select:focus {
  border-color: #dc3545;
}

.employee-reset-btn {
  height: 39px;
  padding: 0 15px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #ffffff;
  color: #334155;
  font-size: 13px;
  cursor: pointer;
}

.employee-reset-btn:hover {
  border-color: #dc3545;
  color: #dc3545;
}

/* Bảng nhân viên */

.employee-table-wrapper {
  width: 100%;
  overflow-x: auto;
}

.employee-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  white-space: nowrap;
}

.employee-table thead {
  background: #f8fafc;
}

.employee-table th {
  padding: 15px 14px;
  text-align: left;
  color: #64748b;
  font-weight: 600;
  border-bottom: 1px solid #e7ebf2;
}

.employee-table td {
  padding: 16px 14px;
  color: #334155;
  border-bottom: 1px solid #edf0f5;
}

.employee-table tbody tr:hover {
  background: #f8fafc;
}

.employee-name {
  font-weight: 600;
  color: #172033 !important;
}

.employee-revenue {
  font-weight: 700;
  color: #16a34a !important;
}

/* Phân trang */

.employee-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 18px;
  color: #64748b;
  font-size: 12px;
}

.employee-page-buttons {
  display: flex;
  align-items: center;
  gap: 12px;
}

.employee-page-buttons button {
  padding: 8px 13px;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  background: #ffffff;
  color: #334155;
  font-size: 12px;
  cursor: pointer;
}

.employee-page-buttons button:hover:not(:disabled) {
  border-color: #dc3545;
  color: #dc3545;
}

.employee-page-buttons button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* Responsive */

@media (max-width: 700px) {
  .employee-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .employee-filters {
    flex-direction: column;
    align-items: stretch;
  }

  .employee-filters input,
  .employee-filters select,
  .employee-reset-btn {
    width: 100%;
    min-width: 0;
  }

  .employee-pagination {
    flex-direction: column;
    align-items: flex-start;
  }
}

.statistics-page {
  padding: 20px 22px;
  min-height: 100vh;
  background: #f5f7fb;
  color: #172033;
}

/* BỘ LỌC */

.filter-card {
  background: white;
  border: 1px solid #e9edf4;
  border-radius: 9px;
  padding: 18px 20px;
}

.filter-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #edf0f5;
  padding-bottom: 15px;
  margin-bottom: 16px;
}

.filter-header h3 {
  font-size: 14px;
  font-weight: 700;
  margin: 0;
}

.filter-header h3 i {
  color: #dc3545;
  margin-right: 6px;
}

.quick-filters {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.quick-filters button {
  border: 1px solid #e4e8ef;
  border-radius: 5px;
  background: white;
  padding: 7px 10px;
  font-size: 12px;
  color: #334155;
  cursor: pointer;
}

.quick-filters button:hover {
  color: #dc3545;
  border-color: #dc3545;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr)) auto;
  gap: 12px;
  align-items: end;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-width: 0;
}

.filter-field label {
  font-size: 12px;
  color: #64748b;
}

.filter-field input,
.filter-field select {
  width: 100%;
  height: 40px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
  border-radius: 5px;
  padding: 0 10px;
  font-size: 13px;
  color: #334155;
}

.reset-button {
  height: 40px;
  padding: 0 15px;
  border: 1px solid #e2e8f0;
  background: white;
  border-radius: 5px;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
}

/* THANH THÔNG TIN */

.information-bar {
  background: white;
  border: 1px solid #e9edf4;
  border-radius: 7px;
  padding: 11px 14px;
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #64748b;
}

.information-left,
.information-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.information-left strong {
  color: #172033;
}

.blue-dot {
  width: 7px;
  height: 7px;
  background: #60a5fa;
  border-radius: 50%;
  flex-shrink: 0;
}

.refresh-button {
  padding: 8px 12px;
  border: 1px solid #f2c5cb;
  border-radius: 5px;
  background: white;
  color: #c7283e;
  font-size: 12px;
}

.refresh-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.date-error {
  margin-top: 10px;
  color: #dc2626;
  font-size: 13px;
}

/* CÁC Ô THỐNG KÊ */

.summary-section {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.summary-grid {
  display: grid;
  gap: 14px;
}

.top-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.bottom-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.stat-card {
  min-height: 116px;
  padding: 17px;
  background: #fff;
  border: 1px solid #e6eaf0;
  border-radius: 9px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 5px;
}

.stat-icon {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  margin-bottom: 7px;
  font-size: 18px;
}

.mint {
  background: #e8fcf4;
  color: #16b981;
}

.green {
  background: #ebfcf8;
  color: #10bfa6;
}

.purple {
  background: #eef0ff;
  color: #6b61e8;
}

.blue {
  background: #edf8ff;
  color: #249edb;
}

.yellow {
  background: #fff7df;
  color: #eda319;
}

.red {
  background: #fff0f2;
  color: #d94f61;
}

.stat-card strong {
  font-size: 22px;
  font-weight: 750;
  color: #172033;
  line-height: 1.3;
}

.stat-card span {
  color: #758092;
  font-size: 12px;
}

/* RESPONSIVE */

@media (max-width: 1250px) {
  .filter-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .filter-header,
  .information-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .bottom-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 700px) {
  .filter-grid,
  .top-grid,
  .bottom-grid {
    grid-template-columns: 1fr;
  }

  .statistics-page {
    padding: 14px;
  }
}

/* ===== HAI BIỂU ĐỒ THỐNG KÊ ===== */

.charts-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 18px;
  margin-top: 20px;
  margin-bottom: 20px;
}

.chart-card {
  min-width: 0;
  min-height: 500px;
  background: #fff;
  border: 1px solid #e7ebf2;
  border-radius: 9px;
  padding: 18px 20px;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
  padding-bottom: 15px;
  border-bottom: 1px solid #edf0f5;
}

.chart-header h3 {
  font-size: 14px;
  font-weight: 700;
  color: #202b3d;
  margin: 0;
}

.chart-header h3 i {
  color: #dc3545;
  margin-right: 7px;
}

.chart-right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.chart-actions {
  display: inline-flex;
  border: 1px solid #e5e9f0;
  border-radius: 5px;
  overflow: hidden;
}

.chart-actions button {
  padding: 7px 12px;
  background: #f8fafc;
  border: none;
  color: #64748b;
  font-size: 12px;
  cursor: pointer;
}

.chart-actions button.active {
  color: #172033;
  background: #fff;
  font-weight: 700;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.12);
}

.chart-right select {
  height: 33px;
  max-width: 135px;
  border: 1px solid #e5e9f0;
  border-radius: 5px;
  padding: 0 7px;
  background: #fff;
  font-size: 12px;
}

.chart-canvas {
  width: 100%;
  height: 365px;
  position: relative;
}

.chart-canvas canvas {
  width: 100% !important;
  height: 100% !important;
}

.chart-content {
  min-height: 410px;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* BIỂU ĐỒ THƯƠNG HIỆU */

.brand-chart-content {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
  gap: 16px;
  padding-top: 20px;
}

.brand-chart-canvas {
  width: 100%;
  height: 250px;
  position: relative;
}

.brand-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.brand-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 12px;
  border-radius: 7px;
  background: #f8fafc;
  font-size: 12px;
}

.brand-name {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: #334155;
  font-weight: 500;
}

.brand-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}

.brand-list-item strong {
  color: #172033;
  font-weight: 700;
  white-space: nowrap;
}

.chart-empty {
  font-size: 13px;
  color: #94a3b8;
  text-align: center;
}

@media (max-width: 1100px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}

/* TỔNG QUAN THEO CHU KỲ THỜI GIAN */

.period-section {
  margin-top: 20px;
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.period-card {
  background: #ffffff;
  border: 1px solid #e7ebf2;
  border-radius: 9px;
  padding: 18px 20px;
}

.period-header {
  padding-bottom: 16px;
  border-bottom: 1px solid #edf0f5;
  margin-bottom: 12px;
}

.period-header h3 {
  font-size: 14px;
  font-weight: 700;
  margin: 0;
  color: #202b3d;
}

.period-header i {
  color: #dc3545;
  margin-right: 7px;
}

.period-table-wrapper {
  width: 100%;
  overflow-x: auto;
}

.period-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  white-space: nowrap;
}

.period-table th {
  background: #f8fafc;
  color: #64748b;
  font-weight: 600;
  padding: 14px 12px;
  text-align: left;
}

.period-table td {
  padding: 16px 12px;
  border-bottom: 1px solid #edf0f5;
  color: #334155;
}

.period-name {
  font-weight: 600;
}

.growth-positive {
  color: #16a34a;
  font-weight: 700;
}

.growth-negative {
  color: #dc2626;
  font-weight: 700;
}

.period-message {
  padding: 24px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}

.period-error {
  color: #dc2626;
}

/* CƠ CẤU TRẠNG THÁI ĐƠN HÀNG */

.period-card,
.order-status-card {
  min-width: 0;
}

.order-status-card {
  background: white;
  border: 1px solid #e7ebf2;
  border-radius: 9px;
  padding: 18px 20px;
}

.order-status-total {
  text-align: center;
  font-size: 13px;
  color: #64748b;
  margin: 15px 0;
}

.order-status-total strong {
  font-size: 18px;
  color: #172033;
  margin-left: 5px;
}

.order-status-canvas {
  width: 100%;
  height: 245px;
  position: relative;
  margin-bottom: 20px;
}

.order-status-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-status-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 12px;
}

.order-status-name {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #334155;
}

.order-status-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}

.order-status-item strong {
  color: #172033;
}

@media (max-width: 1200px) {
  .period-section {
    grid-template-columns: 1fr;
  }
}
</style>
