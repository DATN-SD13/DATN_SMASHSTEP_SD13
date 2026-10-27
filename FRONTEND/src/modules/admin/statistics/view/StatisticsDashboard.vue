<script setup>
import { computed, ref, onMounted, onBeforeUnmount, watch } from "vue";
import AdminLayout from "../../../../layouts/AdminLayout.vue";
import StatisticsPager from "../components/StatisticsPager.vue";
import { imageUrl } from "../../product/services/imageUtils";

import {
  cancelStatisticsRequests,
  getStatisticsChart,
  getStatisticsSummary,
  getStatisticsPeriod,
  getStatisticsOrderStatus,
  getStatisticsEmployee,
  getStatisticsProduct,
  getStatisticsRevenueComparison,
  sendStatisticsReportEmail,
  getStatisticsEmailSchedule,
  saveStatisticsEmailSchedule,
} from "../service/statisticsService";

import { Bar, Line, Doughnut } from "vue-chartjs";

import * as XLSX from "xlsx";

import {
  Chart as ChartJS,
  DoughnutController,
  BarController,
  LineController,
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
  BarController,
  LineController,
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

const dateValidationMessage = computed(() => {
  if (!fromDate.value || !toDate.value) {
    return "Vui lòng chọn đầy đủ ngày bắt đầu và ngày kết thúc.";
  }

  if (fromDate.value > toDate.value) {
    return "Ngày bắt đầu không được lớn hơn ngày kết thúc.";
  }

  return "";
});

const groupBy = ref("day");
// ===== CHUYEN GIAO DIEN THONG KE =====
// overview: Thống kê tổng quan
// comparison: So sánh doanh thu
// email: Gửi báo cáo tổng kết

const activeStatisticsView = ref("overview");

function changeStatisticsView(view) {
  activeStatisticsView.value = view;
}

const dashboardSummary = ref(null);

const orderStatusData = ref([]);

const orderStatusColors = {
  waiting: "#e5ac4b", // Chờ xác nhận
  confirmed: "#4c9cde", // Đã xác nhận
  ready: "#818CF8", // Chờ giao hàng
  shipping: "#06B6D4", // Đang giao hàng
  delivered: "#0EA5E9", // Đã giao hàng
  done: "#33ad91", // Hoàn thành
  cancel: "#dd7f8a", // Đã hủy - xám xanh
  refund: "#a28ac9", // Hoàn tiền
  pending: "#94A3B8",
  failed: "#e88b4b", // Hóa đơn chờ
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
  cutout: "74%",
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

let loadOrderStatusVersion = 0;
async function loadOrderStatus() {
  const request = ++loadOrderStatusVersion;
  if (!fromDate.value || !toDate.value || fromDate.value > toDate.value) {
    orderStatusData.value = [];
    loadingOrderStatus.value = false;
    orderStatusError.value = "";
    return;
  }

  loadingOrderStatus.value = true;
  orderStatusError.value = "";

  try {
    const response = await getStatisticsOrderStatus({
      tuNgay: fromDate.value,
      denNgay: toDate.value,
    });

    if (request !== loadOrderStatusVersion) return;
    if (!response.data?.success) {
      throw new Error(
        response.data?.message || "Không thể tải trạng thái đơn hàng",
      );
    }

    orderStatusData.value = response.data.data?.theoTrangThai || [];

    console.log("TRẠNG THÁI ĐƠN HÀNG:", orderStatusData.value);
  } catch (error) {
    if (request !== loadOrderStatusVersion) return;
    console.error("Lỗi thống kê trạng thái:", error);

    orderStatusError.value =
      error.response?.data?.message ||
      error.message ||
      "Không thể tải dữ liệu trạng thái đơn hàng";

    orderStatusData.value = [];
  } finally {
    if (request === loadOrderStatusVersion) loadingOrderStatus.value = false;
  }
}

const productData = ref([]);
const loadingProduct = ref(false);

let productRequestId = 0;

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

function exportProductExcel() {
  // Kiểm tra API đang tải
  if (loadingProduct.value) {
    showFeedback("Dữ liệu sản phẩm đang tải. Vui lòng đợi!");
    return;
  }

  // Kiểm tra API gặp lỗi
  if (productError.value) {
    showFeedback("Không thể xuất Excel vì dữ liệu sản phẩm đang bị lỗi.");
    return;
  }

  // Kiểm tra khoảng ngày
  if (!fromDate.value || !toDate.value || fromDate.value > toDate.value) {
    showFeedback("Vui lòng chọn khoảng ngày hợp lệ trước khi xuất Excel.");
    return;
  }

  // Kiểm tra dữ liệu rỗng
  if (!filteredProducts.value.length) {
    showFeedback("Không có dữ liệu sản phẩm để xuất Excel.");
    return;
  }

  const rows = filteredProducts.value.map((product, index) => ({
    STT: index + 1,
    "Mã sản phẩm": product.code,
    "Tên sản phẩm": product.name,
    "Số lượng đã bán": Number(product.sold || 0),
    "SL từ đơn hoàn tiền": Number(product.returned || 0),

    "Doanh số sản phẩm (VNĐ)": Number(product.revenue || 0),
    "Tồn kho hiện tại": Number(product.stock || 0),
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

function resetProductFilters() {
  productSearch.value = "";
  productStockFilter.value = "all";
  productSort.value = "sold";
  productPage.value = 1;
}

async function loadProduct() {
  // Đánh dấu lần gọi API mới nhất
  const currentRequestId = ++productRequestId;

  // Validate khoảng ngày
  if (!fromDate.value || !toDate.value || fromDate.value > toDate.value) {
    productData.value = [];
    productError.value = "";
    loadingProduct.value = false;
    return;
  }

  loadingProduct.value = true;
  productError.value = "";

  try {
    const response = await getStatisticsProduct({
      tuNgay: fromDate.value,
      denNgay: toDate.value,
    });

    // Bỏ qua kết quả của API cũ
    if (currentRequestId !== productRequestId) {
      return;
    }

    if (!response.data?.success) {
      throw new Error(
        response.data?.message || "Không thể tải thống kê sản phẩm",
      );
    }

    // Kiểm tra dữ liệu trả về phải là mảng
    productData.value = Array.isArray(response.data.data)
      ? response.data.data
      : [];
  } catch (error) {
    // Không xử lý lỗi từ API cũ
    if (currentRequestId !== productRequestId) {
      return;
    }

    console.error("Lỗi thống kê sản phẩm:", error);

    productError.value =
      error.response?.data?.message ||
      error.message ||
      "Không thể tải dữ liệu sản phẩm";

    productData.value = [];
  } finally {
    // Chỉ API mới nhất được tắt loading
    if (currentRequestId === productRequestId) {
      loadingProduct.value = false;
    }
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
  // Kiểm tra API đang tải
  if (loadingEmployee.value) {
    showFeedback("Dữ liệu nhân viên đang tải. Vui lòng đợi!");
    return;
  }

  // Kiểm tra API gặp lỗi
  if (employeeError.value) {
    showFeedback("Không thể xuất Excel vì dữ liệu nhân viên đang bị lỗi.");
    return;
  }

  // Kiểm tra khoảng ngày
  if (!fromDate.value || !toDate.value || fromDate.value > toDate.value) {
    showFeedback("Vui lòng chọn khoảng ngày hợp lệ trước khi xuất Excel.");
    return;
  }

  // Kiểm tra dữ liệu rỗng
  if (!filteredEmployees.value.length) {
    showFeedback("Không có dữ liệu nhân viên để xuất Excel.");
    return;
  }

  const rows = filteredEmployees.value.map((employee, index) => ({
    STT: index + 1,
    "Mã nhân viên": employee.code,
    "Tên nhân viên": employee.name,
    "Đơn hoàn thành": Number(employee.totalOrders || 0),
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

let employeeRequestId = 0;

async function loadEmployee() {
  const currentRequestId = ++employeeRequestId;

  // Validate khoảng ngày
  if (!fromDate.value || !toDate.value || fromDate.value > toDate.value) {
    employeeData.value = [];
    employeeError.value = "";
    loadingEmployee.value = false;
    return;
  }

  loadingEmployee.value = true;
  employeeError.value = "";

  try {
    const response = await getStatisticsEmployee({
      tuNgay: fromDate.value,
      denNgay: toDate.value,
    });

    // Bỏ qua kết quả của API cũ
    if (currentRequestId !== employeeRequestId) {
      return;
    }

    if (!response.data?.success) {
      throw new Error(
        response.data?.message || "Không thể tải thống kê nhân viên",
      );
    }

    // Kiểm tra dữ liệu trả về
    employeeData.value = Array.isArray(response.data.data)
      ? response.data.data
      : [];
  } catch (error) {
    // Bỏ qua lỗi của API cũ
    if (currentRequestId !== employeeRequestId) {
      return;
    }

    console.error("Lỗi thống kê nhân viên:", error);

    employeeError.value =
      error.response?.data?.message ||
      error.message ||
      "Không thể tải dữ liệu nhân viên";

    employeeData.value = [];
  } finally {
    // Chỉ API mới nhất được cập nhật trạng thái loading
    if (currentRequestId === employeeRequestId) {
      loadingEmployee.value = false;
    }
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

// Xuất Excel thống kê theo chu kỳ thời gian
function exportPeriodExcel() {
  // Kiểm tra API đang tải
  if (loadingPeriod.value) {
    showFeedback("Dữ liệu đang được tải. Vui lòng đợi!");
    return;
  }

  // Kiểm tra API gặp lỗi
  if (periodError.value) {
    showFeedback("Không thể xuất Excel vì dữ liệu thống kê đang bị lỗi.");
    return;
  }

  // Kiểm tra dữ liệu rỗng
  if (!periodData.value.length) {
    showFeedback("Không có dữ liệu thống kê để xuất Excel.");
    return;
  }

  const rows = periodData.value.map((item, index) => ({
    STT: index + 1,
    "Thời gian": item.period,
    "Doanh thu (VNĐ)": Number(item.revenue || 0),
    "Số đơn hàng": Number(item.orders || 0),
    "Giá trị đơn trung bình (VNĐ)": Number(item.aov || 0),
    "Tăng trưởng (%)": item.growth == null ? "—" : Number(item.growth),
  }));

  const worksheet = XLSX.utils.json_to_sheet(rows);

  worksheet["!cols"] = [
    { wch: 8 },
    { wch: 20 },
    { wch: 24 },
    { wch: 18 },
    { wch: 24 },
    { wch: 20 },
  ];

  const workbook = XLSX.utils.book_new();

  XLSX.utils.book_append_sheet(workbook, worksheet, "Thong ke chu ky");

  XLSX.writeFile(workbook, "tong-quan-theo-chu-ky.xlsx");
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

let loadSummaryVersion = 0;
async function loadSummary() {
  const request = ++loadSummaryVersion;
  if (!fromDate.value || !toDate.value || fromDate.value > toDate.value) {
    dashboardSummary.value = null;
    loadingSummary.value = false;
    summaryError.value = "";
    return;
  }

  loadingSummary.value = true;
  summaryError.value = "";

  try {
    const response = await getStatisticsSummary({
      tuNgay: fromDate.value,
      denNgay: toDate.value,
    });

    if (request !== loadSummaryVersion) return;
    if (!response.data?.success) {
      throw new Error(response.data?.message || "Không thể tải doanh thu");
    }

    dashboardSummary.value = response.data.data;

    console.log("TỔNG QUAN DOANH THU:", dashboardSummary.value);
  } catch (error) {
    if (request !== loadSummaryVersion) return;
    console.error("Lỗi thống kê doanh thu:", error);

    summaryError.value =
      error.response?.data?.message ||
      error.message ||
      "Không thể tải tổng quan doanh thu";

    dashboardSummary.value = null;
  } finally {
    if (request === loadSummaryVersion) loadingSummary.value = false;
  }
}

const salesChartType = ref("bar");
const brandChartSort = ref("revenue");

const salesChartData = ref([]);
const brandChartData = ref([]);

const brandColors = [
  "#2563EB",
  "#60A5FA",
  "#F59E0B",
  "#10B981",
  "#06B6D4",
  "#8B5CF6",
  "#64748B",
];

const sortedBrands = computed(() => {
  return brandChartData.value
    .slice()
    .sort(
      (a, b) =>
        Number(b[brandChartSort.value] || 0) -
        Number(a[brandChartSort.value] || 0),
    );
});

const brandChart = computed(() => ({
  labels: sortedBrands.value.map((item) => item.name),
  datasets: [
    {
      label: brandChartSort.value === "revenue" ? "Doanh số sản phẩm" : "Số lượng bán",
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

      borderColor: "#2563EB",
      backgroundColor: "#2563EB",

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

let chartRequestId = 0;

async function loadCharts() {
  // Đánh dấu lần gọi API mới nhất
  const currentRequestId = ++chartRequestId;

  // Validate khoảng ngày
  if (!fromDate.value || !toDate.value || fromDate.value > toDate.value) {
    chartError.value = "Khoảng ngày không hợp lệ";
    salesChartData.value = [];
    brandChartData.value = [];
    loadingCharts.value = false;
    return;
  }

  loadingCharts.value = true;
  chartError.value = "";

  try {
    const response = await getStatisticsChart({
      tuNgay: fromDate.value,
      denNgay: toDate.value,
      nhomTheo: groupBy.value,
    });

    // Bỏ qua kết quả từ yêu cầu cũ
    if (currentRequestId !== chartRequestId) {
      return;
    }

    if (!response.data?.success) {
      throw new Error(
        response.data?.message || "Không thể lấy dữ liệu biểu đồ",
      );
    }

    salesChartData.value = response.data.data?.sales || [];
    brandChartData.value = response.data.data?.brands || [];
  } catch (error) {
    // Không xử lý lỗi của yêu cầu cũ
    if (currentRequestId !== chartRequestId) {
      return;
    }

    console.error("Lỗi thống kê:", error);

    chartError.value =
      error.response?.data?.message ||
      error.message ||
      "Không thể tải dữ liệu biểu đồ";

    salesChartData.value = [];
    brandChartData.value = [];
  } finally {
    // Chỉ yêu cầu mới nhất được tắt loading
    if (currentRequestId === chartRequestId) {
      loadingCharts.value = false;
    }
  }
}

// ===== SO SÁNH DOANH THU THEO THÁNG / QUÝ / NĂM =====

// Chế độ so sánh
const comparisonMode = ref("month");
const comparisonYear = ref(new Date().getFullYear());

// Dữ liệu từ Backend
const comparisonData = ref([]);
const loadingComparison = ref(false);
const comparisonError = ref("");

let comparisonRequestId = 0;

// Kiểm tra kỳ thống kê nằm trong tương lai
function isFutureComparisonPeriod(period) {
  const now = new Date();
  const selectedYear = Number(comparisonYear.value);

  if (comparisonMode.value === "month") {
    const month = Number(String(period).match(/\d+/)?.[0]);

    return (
      selectedYear > now.getFullYear() ||
      (selectedYear === now.getFullYear() && month > now.getMonth() + 1)
    );
  }

  if (comparisonMode.value === "quarter") {
    const quarter = Number(String(period).match(/\d+/)?.[0]);
    const currentQuarter = Math.floor(now.getMonth() / 3) + 1;

    return (
      selectedYear > now.getFullYear() ||
      (selectedYear === now.getFullYear() && quarter > currentQuarter)
    );
  }

  const year = Number(String(period).match(/\d{4}/)?.[0]);

  return year > now.getFullYear();
}

// Không hiển thị những kỳ chưa diễn ra
const visibleComparisonData = computed(() =>
  comparisonData.value.filter((item) => !isFutureComparisonPeriod(item.period)),
);

// Dữ liệu biểu đồ: cột doanh thu + đường tăng trưởng
const revenueComparisonChart = computed(() => ({
  labels: visibleComparisonData.value.map((item) => item.period),

  datasets: [
    {
      type: "bar",
      label: "Doanh thu thực tế",
      data: visibleComparisonData.value.map((item) =>
        Number(item.revenue || 0),
      ),
      backgroundColor: "#2563EB",
      borderColor: "#1D4ED8",
      borderWidth: 1,
      borderRadius: 7,
      maxBarThickness: 48,
      yAxisID: "y",
    },
    {
      type: "line",
      label: "Tăng trưởng (%)",
      data: visibleComparisonData.value.map((item) =>
        item.growth == null ? null : Number(item.growth),
      ),
      borderColor: "#F59E0B",
      backgroundColor: "#F59E0B",
      tension: 0.3,
      borderWidth: 3,
      pointRadius: 4,
      spanGaps: false,
      yAxisID: "y1",
    },
  ],
}));

// Cấu hình 2 trục của biểu đồ
const revenueComparisonOptions = {
  responsive: true,
  maintainAspectRatio: false,

  interaction: {
    mode: "index",
    intersect: false,
  },

  plugins: {
    legend: {
      position: "bottom",
    },
    tooltip: {
      enabled: true,
    },
  },

  scales: {
    y: {
      position: "left",
      beginAtZero: true,
      title: {
        display: true,
        text: "Doanh thu (VNĐ)",
      },
      ticks: {
        callback: (value) => Number(value).toLocaleString("vi-VN") + " đ",
      },
    },

    y1: {
      position: "right",
      title: {
        display: true,
        text: "Tăng trưởng (%)",
      },
      ticks: {
        callback: (value) => value + "%",
      },
      grid: {
        drawOnChartArea: false,
      },
    },
  },
};

// Gọi API so sánh doanh thu
async function loadRevenueComparison() {
  const requestId = ++comparisonRequestId;
  const year = Number(comparisonYear.value);

  if (!Number.isInteger(year) || year < 2000 || year > 2100) {
    comparisonError.value = "Vui lòng nhập năm từ 2000 đến 2100.";
    comparisonData.value = [];
    loadingComparison.value = false;
    return;
  }

  loadingComparison.value = true;
  comparisonError.value = "";

  try {
    const response = await getStatisticsRevenueComparison({
      nhomTheo: comparisonMode.value,
      nam: year,
    });

    // Không cho API cũ ghi đè dữ liệu mới
    if (requestId !== comparisonRequestId) return;

    if (!response.data?.success) {
      throw new Error(
        response.data?.message || "Không thể tải so sánh doanh thu",
      );
    }

    comparisonData.value = Array.isArray(response.data.data)
      ? response.data.data
      : [];
  } catch (error) {
    if (requestId !== comparisonRequestId) return;

    comparisonError.value =
      error.response?.data?.message ||
      error.message ||
      "Không thể tải dữ liệu so sánh doanh thu";

    comparisonData.value = [];
  } finally {
    if (requestId === comparisonRequestId) {
      loadingComparison.value = false;
    }
  }
}

// Tự tải lại khi thay đổi tháng / quý / năm
watch([comparisonMode, comparisonYear], loadRevenueComparison);

// ===== BẢNG ĐỐI CHIẾU DOANH THU =====

// Chỉ lấy các kỳ đã diễn ra và kỳ hiện tại

/* ===== XUAT EXCEL SO SANH DOANH THU ===== */

function exportRevenueComparisonExcel() {
  // Kiểm tra dữ liệu đang tải
  if (loadingComparison.value) {
    showFeedback("Dữ liệu đang tải, vui lòng thử lại.");
    return;
  }

  // Kiểm tra lỗi API
  if (comparisonError.value) {
    showFeedback("Không thể xuất Excel vì dữ liệu đang bị lỗi.");
    return;
  }

  // Kiểm tra dữ liệu rỗng
  if (!revenueComparisonRows.value.length) {
    showFeedback("Không có dữ liệu doanh thu để xuất Excel.");
    return;
  }

  // Chuyển dữ liệu sang Excel
  const rows = revenueComparisonRows.value.map((item) => ({
    STT: item.stt,
    "Thời gian": item.period,
    "Doanh thu thực tế (VNĐ)": item.revenue,
    "Số đơn hàng": item.orders,
    "Giá trị đơn trung bình (VNĐ)": item.aov,
    "Tăng trưởng (%)": item.growth == null ? "—" : item.growth,
  }));

  const worksheet = XLSX.utils.json_to_sheet(rows);

  // Định dạng độ rộng cột
  worksheet["!cols"] = [
    { wch: 8 },
    { wch: 18 },
    { wch: 28 },
    { wch: 18 },
    { wch: 38 },
    { wch: 20 },
  ];

  const workbook = XLSX.utils.book_new();

  XLSX.utils.book_append_sheet(workbook, worksheet, "So sanh doanh thu");

  // Tên file theo chế độ thống kê
  const fileName = `so-sanh-doanh-thu-${comparisonMode.value}-${comparisonYear.value}.xlsx`;

  XLSX.writeFile(workbook, fileName);
}

const revenueComparisonRows = computed(() => {
  return visibleComparisonData.value.map((item, index) => ({
    stt: index + 1,
    period: item.period,
    revenue: Number(item.revenue || 0),
    orders: Number(item.orders || 0),
    aov: Number(item.aov || 0),
    growth: item.growth == null ? null : Number(item.growth),
  }));
});

/* ===== GUI EMAIL BAO CAO TONG KET ===== */

// Email người nhận
const reportEmail = ref("");

// Ngày bắt đầu và kết thúc báo cáo
const reportType = ref('month');
const reportMonth = ref(formatDate(new Date()).slice(0, 7));
const reportYear = ref(new Date().getFullYear());
const currentReportMonth = formatDate(new Date()).slice(0, 7);
const currentReportYear = new Date().getFullYear();
const reportRange = computed(() => {
  if (reportType.value === 'month') {
    const match = String(reportMonth.value || '').match(/^(\d{4})-(\d{2})$/);
    if (!match || Number(match[2]) < 1 || Number(match[2]) > 12) return { from: '', to: '' };
    const year=Number(match[1]),month=Number(match[2]);
    return { from: formatDate(new Date(year,month-1,1)), to: formatDate(new Date(year,month,0)) };
  }
  const year=Number(reportYear.value);
  if(!Number.isInteger(year) || year<2000 || year>2100)return {from:'',to:''};
  return {from: year+'-01-01',to:year+'-12-31'};
});
const reportFromDate = computed(() => reportRange.value.from);
const reportToDate = computed(() => reportRange.value.to);
const reportPeriodError = computed(() => !reportFromDate.value ? 'Chọn tháng hoặc năm hợp lệ.' : reportFromDate.value > formatDate(new Date()) ? 'Không gửi báo cáo cho kỳ chưa bắt đầu.' : '');
const reportIsTemporary = computed(() => reportToDate.value > formatDate(new Date()));

// Trạng thái gửi email
const sendingReportEmail = ref(false);
const reportEmailError = ref("");
const reportEmailSuccess = ref("");

// Gửi báo cáo doanh thu qua email
async function sendRevenueReport() {
  reportEmailError.value = "";
  reportEmailSuccess.value = "";

  const email = reportEmail.value.trim();

  // Kiểm tra email
  if (!email) {
    reportEmailError.value = "Vui lòng nhập email người nhận.";
    return;
  }

  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    reportEmailError.value = "Địa chỉ email không hợp lệ.";
    return;
  }

  if(reportPeriodError.value){reportEmailError.value=reportPeriodError.value;return;}

  // Kiểm tra thời gian
  if (!reportFromDate.value || !reportToDate.value) {
    reportEmailError.value = "Vui lòng chọn đầy đủ khoảng ngày.";
    return;
  }

  if (reportFromDate.value > reportToDate.value) {
    reportEmailError.value = "Ngày bắt đầu không được lớn hơn ngày kết thúc.";
    return;
  }

  // Giới hạn khoảng thời gian giống Backend
  const from = new Date(reportFromDate.value + "T00:00:00");
  const to = new Date(reportToDate.value + "T00:00:00");

  const days = Math.round((to.getTime() - from.getTime()) / 86400000);

  if (days > 366) {
    reportEmailError.value =
      "Khoảng thời gian báo cáo không được vượt quá 367 ngày.";
    return;
  }

  if (sendingReportEmail.value) return;

  sendingReportEmail.value = true;

  try {
    const response = await sendStatisticsReportEmail({
      email: email,
      tuNgay: reportFromDate.value,
      denNgay: reportToDate.value,
    });

    if (!response.data?.success) {
      throw new Error(response.data?.message || "Không thể gửi báo cáo.");
    }

    reportEmailSuccess.value =
      "Đã gửi báo cáo " + (reportType.value === 'month' ? 'tháng ' + reportMonth.value : 'năm ' + reportYear.value) + " đến " + email;
  } catch (error) {
    console.error("Lỗi gửi báo cáo email:", error);

    reportEmailError.value =
      error.response?.data?.message ||
      error.message ||
      "Gửi email thất bại. Vui lòng thử lại.";
  } finally {
    sendingReportEmail.value = false;
  }
}

function loadCurrentView() {
  if (activeStatisticsView.value === 'overview') { loadSummary(); loadOrderStatus(); loadCharts(); loadProduct(); loadEmployee(); }
  else if (activeStatisticsView.value === 'products') { loadProduct(); loadCharts(); }
  else if (activeStatisticsView.value === 'employees') loadEmployee();
  else if (activeStatisticsView.value === 'comparison') { loadRevenueComparison(); loadPeriod(); }
  else if (activeStatisticsView.value === 'email') loadEmailSchedule();
}
let filterReloadTimer;
function invalidateStatisticsRequests() {
  chartRequestId++; productRequestId++; employeeRequestId++; comparisonRequestId++;
  loadSummaryVersion++; loadOrderStatusVersion++;
  cancelStatisticsRequests();
}
function scheduleFilterReload() {
  clearTimeout(filterReloadTimer);
  invalidateStatisticsRequests();
  if (activeStatisticsView.value === 'overview') {
    loadingSummary.value = loadingOrderStatus.value = loadingCharts.value = loadingProduct.value = loadingEmployee.value = true;
  }
  filterReloadTimer = setTimeout(() => { filterReloadTimer = null; loadCurrentView(); }, 180);
}
onMounted(loadCurrentView);
watch([activeStatisticsView, fromDate, toDate], scheduleFilterReload);
watch(groupBy, () => {
  if (filterReloadTimer) { clearTimeout(filterReloadTimer); filterReloadTimer = setTimeout(() => { filterReloadTimer = null; loadCurrentView(); }, 180); }
  else if (activeStatisticsView.value === 'overview') loadCharts();
});
onBeforeUnmount(() => {
  clearTimeout(filterReloadTimer); cancelStatisticsRequests();
  chartRequestId++; productRequestId++; employeeRequestId++; comparisonRequestId++;
  loadSummaryVersion++; loadOrderStatusVersion++; clearTimeout(feedbackTimer);
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
}

function money(value) {
  if (value == null) return "—";
  return `${Number(value).toLocaleString("vi-VN")} đ`;
}

function number(value) {
  if (value == null) return "—";
  return Number(value).toLocaleString("vi-VN");
}

const statisticsTabs = [
  {key:'overview',label:'Tổng quan',icon:'bi-grid'},
  {key:'comparison',label:'So sánh doanh thu',icon:'bi-bar-chart-line'},
  {key:'email',label:'Gửi báo cáo',icon:'bi-envelope'}
];
const quickPeriods = [{key:'today',label:'Hôm nay'},{key:'week',label:'7 ngày qua'},{key:'month',label:'Tháng này'},{key:'lastMonth',label:'Tháng trước'},{key:'year',label:'Năm nay'}];
function rangeFor(type) {
  const now=new Date(); let start=new Date(now.getFullYear(),now.getMonth(),now.getDate()),end=new Date(start);
  if(type==='week') start.setDate(start.getDate()-6);
  if(type==='month') start=new Date(now.getFullYear(),now.getMonth(),1);
  if(type==='lastMonth'){start=new Date(now.getFullYear(),now.getMonth()-1,1);end=new Date(now.getFullYear(),now.getMonth(),0);}
  if(type==='year') start=new Date(now.getFullYear(),0,1);
  return [formatDate(start),formatDate(end)];
}
const selectedQuickPeriod=computed(()=>quickPeriods.find(p=>{const [a,b]=rangeFor(p.key);return a===fromDate.value && b===toDate.value;})?.key);
const compactDonutOptions={responsive:true,maintainAspectRatio:false,cutout:'72%',plugins:{legend:{display:false}}};
const otherPaymentRevenue=computed(()=>Math.max(0,Number(dashboardSummary.value?.totalRevenue||0)-Number(dashboardSummary.value?.cashRevenue||0)-Number(dashboardSummary.value?.transferRevenue||0)));
function statusShare(count){return totalOrderStatuses.value?(Number(count)*100/totalOrderStatuses.value).toLocaleString('vi-VN',{maximumFractionDigits:1}):'0';}
function growthClass(value){return value==null?'':Number(value)>0?'growth-positive':Number(value)<0?'growth-negative':'';}
const feedback=ref(''); let feedbackTimer;
function showFeedback(message){feedback.value=message;clearTimeout(feedbackTimer);feedbackTimer=setTimeout(()=>feedback.value='',6000);}
watch(filteredEmployees,()=>{employeePage.value=Math.min(employeePage.value,employeeTotalPages.value);});
function exportOverviewExcel(){
  if(loadingSummary.value || summaryError.value || !dashboardSummary.value || dateValidationMessage.value)return;
  const labels={totalRevenue:'Doanh thu đơn hoàn thành',expectedRevenue:'Giá trị đơn dự kiến',expectedOrders:'Đơn dự kiến',totalOrders:'Đơn hoàn thành',productsSold:'Số lượng bán',cashRevenue:'Tiền mặt',transferRevenue:'Chuyển khoản',averageOrderValue:'Giá trị đơn trung bình',newCustomers:'Khách đăng ký mới'};
  const rows=Object.entries(labels).map(([key,label])=>({'Chỉ số':label,'Giá trị':Number(dashboardSummary.value[key]||0),'Từ ngày':fromDate.value,'Đến ngày':toDate.value}));
  rows.push({'Chỉ số':'Khác / chưa phân loại (chênh lệch)','Giá trị':otherPaymentRevenue.value,'Từ ngày':fromDate.value,'Đến ngày':toDate.value});
  const sheet=XLSX.utils.json_to_sheet(rows);sheet['!cols']=[{wch:42},{wch:24},{wch:16},{wch:16}];const book=XLSX.utils.book_new();XLSX.utils.book_append_sheet(book,sheet,'Tong quan');XLSX.writeFile(book,'thong-ke-tong-quan-'+fromDate.value+'-'+toDate.value+'.xlsx');
}

const failedProductImages = ref(new Set());
watch(productData,()=>failedProductImages.value=new Set());
function productThumbnail(product){return failedProductImages.value.has(product.id)?'':imageUrl(product.imageUrl);}
const scheduleEmail = ref(''), monthlyEnabled=ref(false), yearlyEnabled=ref(false);
const scheduleLoading=ref(false), scheduleSaving=ref(false), scheduleLoaded=ref(false), scheduleError=ref(''), scheduleSuccess=ref('');
const lastMonthlySent=ref(null), lastYearlySent=ref(null);
function applySchedule(data){scheduleEmail.value=data.email||'';monthlyEnabled.value=!!data.monthlyEnabled;yearlyEnabled.value=!!data.yearlyEnabled;lastMonthlySent.value=data.lastMonthlySent;lastYearlySent.value=data.lastYearlySent;}
async function loadEmailSchedule(){
  if(scheduleSaving.value)return;
  scheduleLoading.value=true;scheduleError.value='';
  try{const response=await getStatisticsEmailSchedule();if(!response.data?.success)throw new Error(response.data?.message||'Không tải được lịch gửi.');applySchedule(response.data.data);scheduleLoaded.value=true;}
  catch(e){scheduleLoaded.value=false;scheduleError.value=e.response?.data?.message||e.message||'Không tải được lịch gửi.';}
  finally{scheduleLoading.value=false;}
}
async function saveEmailSchedule(){
  if(scheduleSaving.value || !scheduleLoaded.value)return;
  scheduleError.value='';scheduleSuccess.value='';
  if((monthlyEnabled.value||yearlyEnabled.value) && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(scheduleEmail.value.trim())){scheduleError.value='Nhập email hợp lệ để bật lịch tự động.';return;}
  scheduleSaving.value=true;
  try{const response=await saveStatisticsEmailSchedule({email:scheduleEmail.value.trim(),monthlyEnabled:monthlyEnabled.value,yearlyEnabled:yearlyEnabled.value});if(!response.data?.success)throw new Error(response.data?.message||'Không lưu được lịch gửi.');applySchedule(response.data.data);scheduleSuccess.value='Đã lưu lịch gửi tự động.';}
  catch(e){scheduleError.value=e.response?.data?.message||e.message||'Không lưu được lịch gửi.';}
  finally{scheduleSaving.value=false;}
}
function sentTime(value){return value?new Date(value).toLocaleString('vi-VN',{timeZone:'Asia/Ho_Chi_Minh'}):'Chưa gửi';}
watch([reportType,reportMonth,reportYear],()=>{reportEmailError.value='';reportEmailSuccess.value='';});

</script>

<template>
  <AdminLayout>
    <main class="statistics-page">
      <header class="page-heading">
        <div class="breadcrumb"><span>Trang chủ</span><b>/</b><strong>Thống kê</strong></div>
        <h1>Thống kê tổng hợp</h1>
        <p>Theo dõi kết quả kinh doanh và đối chiếu số liệu cửa hàng.</p>
      </header>
      <nav class="view-tabs" aria-label="Chức năng thống kê">
        <button v-for="tab in statisticsTabs" :key="tab.key" type="button" :class="{ active: activeStatisticsView === tab.key }" :aria-pressed="activeStatisticsView === tab.key" @click="changeStatisticsView(tab.key)"><i aria-hidden="true" :class="'bi ' + tab.icon"></i>{{ tab.label }}</button>
      </nav>
      <div v-if="feedback" class="notice warning" role="status"><i aria-hidden="true" class="bi bi-info-circle"></i>{{ feedback }}<button type="button" @click="feedback = ''" aria-label="Đóng thông báo">×</button></div>

      <section v-if="activeStatisticsView === 'overview'" class="panel filter-panel">
        <div class="panel-heading"><h2>Bộ lọc thời gian</h2><button type="button" class="outline-button" @click="resetFilters"><i aria-hidden="true" class="bi bi-arrow-counterclockwise"></i>Đặt lại bộ lọc</button></div>
        <div class="filter-row">
          <label>Từ ngày<input v-model="fromDate" type="date" :aria-invalid="!!dateValidationMessage"></label>
          <label>Đến ngày<input v-model="toDate" type="date" :aria-invalid="!!dateValidationMessage"></label>
          <div class="quick-periods" aria-label="Khoảng thời gian nhanh"><button v-for="item in quickPeriods" :key="item.key" type="button" :class="{ active: selectedQuickPeriod === item.key }" @click="quickFilter(item.key)">{{ item.label }}</button></div>
        </div>
        <p v-if="dateValidationMessage" class="error-text" role="alert">{{ dateValidationMessage }}</p>
        <p v-else class="filter-caption">Khoảng thống kê: <strong>{{ periodLabel }}</strong></p>
      </section>

      <template v-if="activeStatisticsView === 'overview'">
        <div class="section-heading"><div><h2>Kết quả kinh doanh</h2><p>Doanh thu và số lượng bán lấy từ đơn hoàn thành, theo ngày thanh toán.</p></div><button class="outline-button" type="button" :disabled="loadingSummary || !!summaryError || !dashboardSummary || !!dateValidationMessage" @click="exportOverviewExcel"><i aria-hidden="true" class="bi bi-file-earmark-excel"></i>Xuất Excel</button></div>
        <div v-if="loadingSummary" class="panel empty-state" role="status">Đang tải tổng quan…</div>
        <div v-else-if="summaryError" class="notice error" role="alert">{{ summaryError }}<button type="button" @click="loadSummary">Thử lại</button></div>
        <template v-else-if="dashboardSummary && !dateValidationMessage">
          <section class="metric-grid">
            <article class="metric revenue"><span class="metric-icon"><i aria-hidden="true" class="bi bi-graph-up-arrow"></i></span><span>Doanh thu thực tế</span><strong>{{ money(dashboardSummary.totalRevenue) }}</strong><small>Từ các đơn đã hoàn thành</small></article>
            <article class="metric"><span class="metric-icon amber"><i aria-hidden="true" class="bi bi-clock-history"></i></span><span>Giá trị đơn dự kiến</span><strong>{{ money(dashboardSummary.expectedRevenue) }}</strong><small>{{ number(dashboardSummary.expectedOrders) }} đơn đã xác nhận / chờ lấy hàng · theo ngày tạo</small></article>
            <article class="metric"><span class="metric-icon blue"><i aria-hidden="true" class="bi bi-bag-check"></i></span><span>Đơn hoàn thành</span><strong>{{ number(dashboardSummary.totalOrders) }}</strong><small>Đã thanh toán, trạng thái hoàn thành</small></article>
            <article class="metric"><span class="metric-icon purple"><i aria-hidden="true" class="bi bi-box-seam"></i></span><span>Số lượng đã bán</span><strong>{{ number(dashboardSummary.productsSold) }}</strong><small>Tổng số lượng sản phẩm trên các đơn hoàn thành</small></article>
          </section>
          <section class="panel secondary-metrics" aria-label="Chỉ số bổ sung">
            <div><span>Tiền mặt</span><strong>{{ money(dashboardSummary.cashRevenue) }}</strong></div><div><span>Chuyển khoản</span><strong>{{ money(dashboardSummary.transferRevenue) }}</strong></div>
            <div v-if="otherPaymentRevenue > 0" title="Chênh lệch giữa doanh thu và hai số tiền mặt, chuyển khoản trong báo cáo; không gán toàn bộ cho một phương thức."><span>Khác / chưa phân loại <i aria-hidden="true" class="bi bi-info-circle"></i></span><strong>{{ money(otherPaymentRevenue) }}</strong></div>
            <div><span>Giá trị đơn trung bình</span><strong>{{ money(dashboardSummary.averageOrderValue) }}</strong></div><div><span>Khách đăng ký mới</span><strong>{{ number(dashboardSummary.newCustomers) }}</strong></div>
          </section>
        </template>
        <section class="overview-charts">
          <article class="panel sales-panel">
            <div class="panel-heading"><div><h2>Xu hướng số lượng bán</h2><p>Đơn hoàn thành · {{ groupBy === 'day' ? 'theo ngày' : groupBy === 'month' ? 'theo tháng' : 'theo năm' }}</p></div><div class="chart-controls"><select v-model="groupBy" aria-label="Nhóm thời gian biểu đồ"><option value="day">Theo ngày</option><option value="month">Theo tháng</option><option value="year">Theo năm</option></select><div class="segmented"><button type="button" :class="{ active: salesChartType === 'bar' }" @click="salesChartType = 'bar'" aria-label="Biểu đồ cột"><i aria-hidden="true" class="bi bi-bar-chart"></i></button><button type="button" :class="{ active: salesChartType === 'line' }" @click="salesChartType = 'line'" aria-label="Biểu đồ đường"><i aria-hidden="true" class="bi bi-graph-up"></i></button></div></div></div>
            <div v-if="loadingCharts" class="empty-state" role="status">Đang tải biểu đồ…</div><div v-else-if="chartError" class="empty-state error-text" role="alert">{{ chartError }}</div>
            <div v-else-if="salesChartData.some(row => Number(row.quantity) > 0)" class="sales-canvas"><Bar v-if="salesChartType === 'bar'" :data="salesChart" :options="salesChartOptions"/><Line v-else :data="salesChart" :options="salesChartOptions"/></div>
            <div v-else class="empty-state"><i aria-hidden="true" class="bi bi-bar-chart"></i><strong>Chưa có sản phẩm được bán</strong><span>Thử chọn khoảng thời gian khác.</span></div>
          </article>
          <article class="panel status-panel">
            <div class="panel-heading"><div><h2>Trạng thái đơn hàng</h2><p>Tất cả đơn đã phát hành · theo ngày tạo đơn</p></div></div>
            <div v-if="loadingOrderStatus" class="empty-state" role="status">Đang tải trạng thái…</div><div v-else-if="orderStatusError" class="empty-state error-text" role="alert">{{ orderStatusError }}</div>
            <template v-else-if="activeOrderStatuses.length"><div class="donut-wrap"><Doughnut :data="orderStatusChart" :options="orderStatusChartOptions"/><div class="donut-center"><strong>{{ number(totalOrderStatuses) }}</strong><span>đơn hàng</span></div></div><ul class="status-legend"><li v-for="status in activeOrderStatuses" :key="status.ma"><span><i aria-hidden="true" :style="{ background: orderStatusColors[status.khoa] || '#94a3b8' }"></i>{{ status.ten }}</span><strong>{{ number(status.soLuong) }}<small>{{ statusShare(status.soLuong) }}%</small></strong></li></ul></template>
            <div v-else class="empty-state"><i aria-hidden="true" class="bi bi-inbox"></i><strong>Chưa có đơn hàng</strong><span>Không có đơn trong khoảng đã chọn.</span></div>
          </article>
        </section>
      </template>

      <template v-if="activeStatisticsView === 'overview'">
        <details class="panel brand-panel detail-table"><summary><i aria-hidden="true" class="bi bi-pie-chart"></i>Cơ cấu theo thương hiệu<span>Mở để xem chi tiết</span></summary><div class="panel-heading"><div><h2>Cơ cấu bán hàng theo thương hiệu</h2><p>Doanh số theo dòng sản phẩm; chưa phân bổ giảm phiếu và phí vận chuyển.</p></div><select v-model="brandChartSort" aria-label="Chỉ số thương hiệu"><option value="revenue">Theo doanh số</option><option value="quantity">Theo số lượng</option></select></div>
          <div v-if="loadingCharts" class="empty-state">Đang tải thương hiệu…</div><div v-else-if="chartError" class="empty-state error-text">{{ chartError }}</div>
          <div v-else-if="sortedBrands.some(row => Number(row[brandChartSort]) > 0)" class="brand-content"><div class="brand-canvas"><Doughnut :data="brandChart" :options="compactDonutOptions"/></div><ul class="brand-legend"><li v-for="(brand, index) in sortedBrands" :key="brand.name"><span><i aria-hidden="true" :style="{ background: brandColors[index % brandColors.length] }"></i>{{ brand.name }}</span><strong>{{ brandChartSort === 'revenue' ? money(brand.revenue) : number(brand.quantity) }}</strong></li></ul></div>
          <div v-else class="empty-state">Chưa có dữ liệu thương hiệu trong khoảng này.</div>
        </details>
        <article class="panel table-panel"><div class="panel-heading"><div><h2>Hiệu quả bán hàng theo sản phẩm</h2><p>Tồn kho là số lượng hiện tại; các chỉ số bán hàng áp dụng khoảng ngày đã chọn.</p></div><button class="outline-button" type="button" :disabled="loadingProduct || !!productError || !filteredProducts.length || !!dateValidationMessage" @click="exportProductExcel"><i aria-hidden="true" class="bi bi-file-earmark-excel"></i>Xuất Excel</button></div>
          <div class="table-filters"><label class="search-field"><i aria-hidden="true" class="bi bi-search"></i><input v-model="productSearch" placeholder="Tìm mã, tên sản phẩm…" aria-label="Tìm sản phẩm"></label><select v-model="productStockFilter" aria-label="Lọc tồn kho"><option value="all">Tất cả tồn kho</option><option value="inStock">Còn hàng</option><option value="outOfStock">Hết hàng</option></select><select v-model="productSort" aria-label="Sắp xếp sản phẩm"><option value="sold">Bán nhiều nhất</option><option value="revenue">Doanh số cao nhất</option><option value="stock">Tồn kho nhiều nhất</option><option value="name">Tên A–Z</option></select><button class="outline-button" type="button" @click="resetProductFilters">Đặt lại</button></div>
          <div v-if="loadingProduct" class="empty-state">Đang tải sản phẩm…</div><div v-else-if="productError" class="empty-state error-text">{{ productError }}</div>
          <div v-else class="table-scroll"><table><thead><tr><th>STT</th><th>Mã sản phẩm</th><th>Sản phẩm</th><th>Đã bán</th><th title="Số lượng trên các hóa đơn có giao dịch hoàn tiền trong kỳ">SL từ đơn hoàn tiền</th><th>Doanh số sản phẩm</th><th>Tồn kho hiện tại</th></tr></thead><tbody><tr v-for="(product, index) in paginatedProducts" :key="product.id"><td class="muted">{{ (productPage - 1) * productPageSize + index + 1 }}</td><td class="code">{{ product.code }}</td><td class="name-cell"><div class="product-identity"><img v-if="productThumbnail(product)" :src="productThumbnail(product)" :alt="product.name" loading="lazy" @error="failedProductImages.add(product.id)"><span v-else class="product-image-fallback"><i aria-hidden="true" class="bi bi-image"></i></span><span>{{ product.name }}</span></div></td><td>{{ number(product.sold) }}</td><td>{{ number(product.returned) }}</td><td class="amount">{{ money(product.revenue) }}</td><td><span class="stock-badge" :class="{ empty: Number(product.stock) <= 0 }">{{ number(product.stock) }}</span></td></tr><tr v-if="!paginatedProducts.length"><td colspan="7" class="empty-row">Không có sản phẩm phù hợp với bộ lọc.</td></tr></tbody></table></div>
          <StatisticsPager v-if="!loadingProduct && !productError" v-model="productPage" :total="filteredProducts.length" :page-size="productPageSize" label="sản phẩm"/>
          <p class="footnote">Doanh số sản phẩm đã tính giảm giá trên sản phẩm; chưa phân bổ phiếu giảm giá và phí vận chuyển của hóa đơn.</p>
        </article>
      </template>

      <article v-if="activeStatisticsView === 'overview'" class="panel table-panel"><div class="panel-heading"><div><h2>Hiệu quả bán hàng theo nhân viên</h2><p>Kết quả được ghi nhận theo nhân viên trên các đơn hoàn thành.</p></div><button class="outline-button" type="button" :disabled="loadingEmployee || !!employeeError || !filteredEmployees.length || !!dateValidationMessage" @click="exportEmployeeExcel"><i aria-hidden="true" class="bi bi-file-earmark-excel"></i>Xuất Excel</button></div>
        <div class="table-filters"><label class="search-field"><i aria-hidden="true" class="bi bi-search"></i><input v-model="employeeSearch" placeholder="Tìm mã, tên nhân viên…" aria-label="Tìm nhân viên"></label><select v-model="employeeSort" aria-label="Sắp xếp nhân viên"><option value="revenue">Doanh thu cao nhất</option><option value="totalOrders">Số đơn nhiều nhất</option><option value="productsSold">Bán nhiều sản phẩm nhất</option><option value="name">Tên A–Z</option></select><button class="outline-button" type="button" @click="resetEmployeeFilters">Đặt lại</button></div>
        <div v-if="loadingEmployee" class="empty-state">Đang tải nhân viên…</div><div v-else-if="employeeError" class="empty-state error-text">{{ employeeError }}</div>
        <div v-else class="table-scroll"><table><thead><tr><th>STT</th><th>Mã nhân viên</th><th>Nhân viên</th><th>Đơn hoàn thành</th><th>Số lượng đã bán</th><th>Doanh thu</th></tr></thead><tbody><tr v-for="(employee, index) in paginatedEmployees" :key="employee.id"><td class="muted">{{ (employeePage - 1) * employeePageSize + index + 1 }}</td><td class="code">{{ employee.code }}</td><td class="name-cell">{{ employee.name }}</td><td>{{ number(employee.totalOrders) }}</td><td>{{ number(employee.productsSold) }}</td><td class="amount">{{ money(employee.revenue) }}</td></tr><tr v-if="!paginatedEmployees.length"><td colspan="6" class="empty-row">Không có nhân viên phù hợp với bộ lọc.</td></tr></tbody></table></div>
        <StatisticsPager v-if="!loadingEmployee && !employeeError" v-model="employeePage" :total="filteredEmployees.length" :page-size="employeePageSize" label="nhân viên"/>
      </article>

      <template v-if="activeStatisticsView === 'comparison'">
        <article class="panel comparison-panel"><div class="panel-heading"><div><h2>So sánh doanh thu giữa các kỳ</h2><p>Dựa trên đơn hoàn thành. Kỳ hiện tại là số tạm tính; tăng trưởng so với toàn bộ kỳ trước.</p></div><button class="outline-button" type="button" :disabled="loadingComparison || !!comparisonError || !revenueComparisonRows.length" @click="exportRevenueComparisonExcel"><i aria-hidden="true" class="bi bi-file-earmark-excel"></i>Xuất Excel</button></div>
          <div class="table-filters"><label>Nhóm kỳ<select v-model="comparisonMode"><option value="month">Theo tháng</option><option value="quarter">Theo quý</option><option value="year">Theo năm</option></select></label><label>Năm tham chiếu<input v-model.number="comparisonYear" type="number" min="2000" max="2100"></label></div>
          <div v-if="loadingComparison" class="empty-state">Đang tải so sánh…</div><div v-else-if="comparisonError" class="empty-state error-text">{{ comparisonError }}</div>
          <template v-else-if="visibleComparisonData.length"><div class="comparison-canvas"><Bar :data="revenueComparisonChart" :options="revenueComparisonOptions"/></div>
            <details class="detail-table"><summary><i aria-hidden="true" class="bi bi-table"></i>Xem bảng số liệu đối chiếu <span>{{ revenueComparisonRows.length }} kỳ</span></summary><div class="table-scroll"><table><thead><tr><th>Kỳ</th><th>Doanh thu</th><th>Đơn hoàn thành</th><th>Giá trị đơn TB</th><th>Tăng trưởng</th></tr></thead><tbody><tr v-for="row in revenueComparisonRows" :key="row.period"><td class="code">{{ row.period }}</td><td class="amount">{{ money(row.revenue) }}</td><td>{{ number(row.orders) }}</td><td>{{ money(row.aov) }}</td><td><span :class="growthClass(row.growth)">{{ growthText(row.growth) }}</span></td></tr></tbody></table></div></details>
          </template><div v-else class="empty-state">Chưa có dữ liệu so sánh.</div>
        </article>
        <details class="panel detail-table fixed-periods"><summary><i aria-hidden="true" class="bi bi-calendar3"></i>Các mốc thời gian hiện tại<span>Hôm nay · Tuần · Tháng · Năm</span></summary><div class="panel-heading"><p>Các mốc được tính theo thời điểm hiện tại, độc lập với khoảng lọc và năm so sánh bên trên.</p><button class="outline-button" type="button" :disabled="loadingPeriod || !!periodError || !periodData.length" @click="exportPeriodExcel"><i aria-hidden="true" class="bi bi-file-earmark-excel"></i>Xuất Excel</button></div><div v-if="loadingPeriod" class="empty-state">Đang tải…</div><div v-else-if="periodError" class="empty-state error-text">{{ periodError }}</div><div v-else class="table-scroll"><table><thead><tr><th>Mốc thời gian</th><th>Doanh thu</th><th>Đơn hoàn thành</th><th>Giá trị đơn TB</th><th>Tăng trưởng</th></tr></thead><tbody><tr v-for="row in periodData" :key="row.period"><td class="code">{{ row.period }}</td><td class="amount">{{ money(row.revenue) }}</td><td>{{ number(row.orders) }}</td><td>{{ money(row.aov) }}</td><td><span :class="growthClass(row.growth)">{{ growthText(row.growth) }}</span></td></tr><tr v-if="!periodData.length"><td colspan="5" class="empty-row">Chưa có dữ liệu.</td></tr></tbody></table></div></details>
      </template>

      <section v-if="activeStatisticsView === 'email'" class="report-layout">
        <article class="panel"><div class="panel-heading"><div><h2>Gửi báo cáo tổng kết</h2><p>Chọn báo cáo theo tháng hoặc năm để gửi ngay.</p></div></div>
          <form class="report-form" @submit.prevent="sendRevenueReport">
            <div class="report-kinds" aria-label="Loại báo cáo"><button type="button" :class="{ active: reportType === 'month' }" @click="reportType = 'month'" :disabled="sendingReportEmail"><i aria-hidden="true" class="bi bi-calendar-month"></i>Tổng kết tháng</button><button type="button" :class="{ active: reportType === 'year' }" @click="reportType = 'year'" :disabled="sendingReportEmail"><i aria-hidden="true" class="bi bi-calendar3"></i>Tổng kết năm</button></div>
            <label>Email người nhận<input v-model="reportEmail" type="email" required maxlength="254" autocomplete="email" placeholder="Ví dụ: quanly@example.com" :disabled="sendingReportEmail"></label>
            <label v-if="reportType === 'month'">Tháng báo cáo<input v-model="reportMonth" type="month" min="2000-01" :max="currentReportMonth" required :disabled="sendingReportEmail"></label>
            <label v-else>Năm báo cáo<input v-model.number="reportYear" type="number" min="2000" :max="currentReportYear" required :disabled="sendingReportEmail"></label>
            <p v-if="reportPeriodError" class="error-text">{{ reportPeriodError }}</p><p v-else class="report-period-note">Khoảng báo cáo: <strong>{{ reportFromDate.split('-').reverse().join('/') }} – {{ reportToDate.split('-').reverse().join('/') }}</strong><span v-if="reportIsTemporary">Kỳ chưa kết thúc; email sẽ ghi rõ số liệu tạm tính.</span></p>
            <p v-if="reportEmailError" class="notice error" role="alert">{{ reportEmailError }}</p><p v-if="reportEmailSuccess" class="notice success" role="status">{{ reportEmailSuccess }}</p>
            <div class="report-actions"><span><i aria-hidden="true" class="bi bi-shield-check"></i>Chỉ quản lý được gửi báo cáo</span><button class="primary-button" type="submit" :disabled="sendingReportEmail || !!reportPeriodError"><i aria-hidden="true" class="bi bi-send"></i>{{ sendingReportEmail ? 'Đang gửi…' : reportType === 'month' ? 'Gửi tổng kết tháng' : 'Gửi tổng kết năm' }}</button></div>
          </form>
        </article>
        <article class="panel"><div class="panel-heading"><div><h2>Tự động gửi cuối kỳ</h2><p>Lưu email nhận và các kỳ muốn theo dõi.</p></div><span class="schedule-zone">Giờ Việt Nam</span></div>
          <div v-if="scheduleLoading" class="empty-state" role="status">Đang tải lịch gửi…</div>
          <form v-else class="report-form schedule-form" @submit.prevent="saveEmailSchedule">
            <label>Email nhận tự động<input v-model="scheduleEmail" type="email" maxlength="254" :required="monthlyEnabled || yearlyEnabled" :disabled="scheduleSaving || !scheduleLoaded" placeholder="Ví dụ: quanly@example.com"></label>
            <label class="schedule-option"><input v-model="monthlyEnabled" type="checkbox" :disabled="scheduleSaving || !scheduleLoaded"><span><strong>Tổng kết tháng</strong><small>00:05 ngày 1 tháng sau, báo cáo toàn bộ tháng vừa kết thúc.</small></span></label>
            <label class="schedule-option"><input v-model="yearlyEnabled" type="checkbox" :disabled="scheduleSaving || !scheduleLoaded"><span><strong>Tổng kết năm</strong><small>00:10 ngày 1/1, báo cáo toàn bộ năm vừa kết thúc.</small></span></label>
            <div class="schedule-last"><span>Tháng gần nhất: <strong>{{ sentTime(lastMonthlySent) }}</strong></span><span>Năm gần nhất: <strong>{{ sentTime(lastYearlySent) }}</strong></span></div>
            <p class="schedule-note"><i aria-hidden="true" class="bi bi-info-circle"></i>Backend cần chạy để gửi email. Nếu tắt lúc đến lịch, hệ thống gửi bù sau khi chạy lại. Lịch bắt đầu từ tháng/năm được bật; không tự gửi các kỳ cũ trước đó.</p>
            <p v-if="scheduleError" class="notice error" role="alert">{{ scheduleError }}<button v-if="!scheduleLoaded" type="button" @click="loadEmailSchedule">Thử lại</button></p><p v-if="scheduleSuccess" class="notice success" role="status">{{ scheduleSuccess }}</p>
            <div class="report-actions"><span>Các thay đổi có hiệu lực sau khi lưu.</span><button class="primary-button" type="submit" :disabled="scheduleSaving || !scheduleLoaded">{{ scheduleSaving ? 'Đang lưu…' : 'Lưu lịch tự động' }}</button></div>
          </form>
        </article>
      </section>

    </main>
  </AdminLayout>
</template>

<style scoped>
.statistics-page{--blue:#168fce;--ink:#243f50;--muted:#8296a3;--border:#e0eaf0;background:#f5f8fc;color:var(--ink);padding:26px 28px 36px;min-height:calc(100vh - 86px);font-size:13px}.statistics-page *{box-sizing:border-box}.page-heading{margin-bottom:22px}.breadcrumb{display:flex;gap:14px;align-items:center;color:#90a3af;font-size:12px;margin-bottom:8px}.breadcrumb b{color:#c0cdd5}.breadcrumb strong{font-weight:500;color:#4b6b80}.page-heading h1{font-size:26px;font-weight:750;line-height:1.3;margin:0 0 7px}.page-heading p{font-size:13px;color:var(--muted);margin:0}.view-tabs{display:flex;gap:5px;margin-bottom:24px;border-bottom:1px solid var(--border);overflow-x:auto}.view-tabs button{display:flex;align-items:center;justify-content:center;gap:9px;padding:14px 18px;border:0;border-bottom:3px solid transparent;border-radius:8px 8px 0 0;background:transparent;color:#7c929f;white-space:nowrap;font:inherit;font-weight:600;cursor:pointer}.view-tabs button.active{border-bottom-color:var(--blue);color:var(--blue);background:#eaf5fc}.view-tabs button:hover{color:var(--blue)}
.panel{background:#fff;border:1px solid var(--border);border-radius:15px;box-shadow:0 2px 4px #234a6410;min-width:0;margin-bottom:22px;overflow:hidden}.panel-heading{display:flex;justify-content:space-between;align-items:center;gap:16px;padding:22px 24px}.panel-heading h2,.section-heading h2{font-size:16px;font-weight:700;margin:0;line-height:1.4}.panel-heading p,.section-heading p{font-size:12px;line-height:1.7;color:var(--muted);margin:6px 0 0}.section-heading{display:flex;align-items:center;justify-content:space-between;gap:16px;margin:25px 0 16px}.statistics-page button{font:inherit}.outline-button,.primary-button{display:inline-flex;justify-content:center;align-items:center;gap:8px;padding:0 15px;height:38px;border:1px solid #b9dbef;border-radius:9px;background:#fff;color:#168bc5;white-space:nowrap;font-weight:600!important;cursor:pointer}.outline-button:hover{background:#eff8fd}.primary-button{color:white;background:var(--blue);border-color:var(--blue)}.statistics-page button:disabled{opacity:.45;cursor:default}.statistics-page button:focus-visible,.statistics-page summary:focus-visible{outline:2px solid #168fce;outline-offset:3px}
.statistics-page input,.statistics-page select{min-width:0;height:42px;border:1px solid #d7e5ed;border-radius:9px;padding:0 12px;color:#4c687b;background:#fff;font:inherit;outline:none}.statistics-page input:focus,.statistics-page select:focus{border-color:var(--blue);box-shadow:0 0 0 3px #168fce10}.statistics-page input[aria-invalid=true]{border-color:#df7373}.statistics-page label{display:flex;flex-direction:column;gap:8px;color:#657f8e;font-size:12px;font-weight:500}.filter-panel .panel-heading{padding-bottom:18px}.filter-row{display:flex;align-items:flex-end;gap:18px;padding:0 24px}.filter-row>label{flex:0 1 220px}.filter-row input{width:100%}.quick-periods{display:flex;flex-wrap:wrap;gap:6px;min-height:42px;align-items:center;margin-left:auto}.quick-periods button{background:#f6f9fb;border:1px solid #e3ebf0;border-radius:7px;padding:8px 11px;color:#728b9b;cursor:pointer;font-size:12px}.quick-periods button.active{background:#e9f5fc;border-color:#a4d6f1;color:#1489c4}.filter-caption{padding:16px 24px 18px;margin:0;color:#91a2af;font-size:12px}.filter-caption strong{font-weight:500;color:#54778d}.filter-panel>.error-text{padding:0 24px 16px}
.metric-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:16px;margin-bottom:16px}.metric{background:#fff;border:1px solid var(--border);box-shadow:0 2px 4px #234a6408;border-radius:14px;padding:21px 20px;display:flex;flex-direction:column;align-items:flex-start;min-width:0}.metric>span:not(.metric-icon){font-size:12px;color:#718998;margin-top:17px}.metric>strong{font-size:25px;font-weight:750;margin-top:8px;line-height:1.35;overflow-wrap:anywhere}.metric>small{font-size:11px;color:#9aabb7;line-height:1.65;margin-top:9px}.metric-icon{display:flex;align-items:center;justify-content:center;width:39px;height:39px;background:#e7f7f1;color:#24916d;border-radius:11px;font-size:18px}.metric-icon.amber{background:#fff5e4;color:#cc972c}.metric-icon.blue{background:#eaf4ff;color:#3489d1}.metric-icon.purple{background:#f0edff;color:#8c72d5}.metric.revenue{border-top:3px solid #21a59a;padding-top:19px}.secondary-metrics{display:grid;grid-template-columns:repeat(auto-fit,minmax(145px,1fr));padding:21px 8px}.secondary-metrics>div{display:flex;flex-direction:column;gap:8px;padding:0 18px;border-right:1px solid #e7eef3}.secondary-metrics>div:last-child{border:0}.secondary-metrics span{font-size:11px;color:#8ba0ae}.secondary-metrics strong{font-size:15px;font-weight:650;color:#466477}
.overview-charts{display:grid;grid-template-columns:minmax(0,1.6fr) minmax(340px,1fr);gap:20px;align-items:start}.chart-controls{display:flex;align-items:center;gap:10px}.chart-controls select{height:34px;max-width:130px;font-size:12px}.segmented{display:flex;background:#f3f7fa;padding:3px;border-radius:7px}.segmented button{height:28px;width:32px;border:0;border-radius:5px;background:transparent;color:#8aa1b0;cursor:pointer}.segmented button.active{background:#fff;box-shadow:0 1px 3px #1f4d7320;color:var(--blue)}.sales-canvas{height:400px;padding:10px 24px 24px;min-width:0}.donut-wrap{height:210px;width:210px;margin:6px auto 20px;position:relative}.donut-center{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;flex-direction:column;pointer-events:none}.donut-center strong{font-size:29px;font-weight:750}.donut-center span{font-size:11px;color:#91a3af;margin-top:3px}.status-legend,.brand-legend{list-style:none;padding:0 24px 20px;margin:0}.status-legend{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));column-gap:18px}.status-legend li,.brand-legend li{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:10px 0;border-bottom:1px solid #edf2f6;font-size:12px}.status-legend li:last-child,.brand-legend li:last-child{border:0}.status-legend li>span,.brand-legend li>span{display:flex;align-items:center;gap:9px;color:#627f91}.status-legend i,.brand-legend i{display:inline-block;width:8px;height:8px;border-radius:3px;flex-shrink:0}.status-legend strong{font-size:12px}.status-legend small{display:inline-block;min-width:43px;text-align:right;font-size:10px;font-weight:400;color:#9aadb9;margin-left:10px}.brand-content{display:grid;grid-template-columns:280px minmax(0,1fr);align-items:center;gap:25px;padding:4px 24px 24px}.brand-canvas{height:240px;max-width:240px;margin:auto}.brand-legend{padding:0}.brand-legend strong{white-space:nowrap;font-size:12px}.brand-panel .panel-heading select{font-size:12px;max-width:170px}
.table-filters{display:flex;align-items:center;gap:12px;flex-wrap:wrap;padding:0 24px 22px}.table-filters>label:not(.search-field){min-width:160px}.table-filters select{min-width:170px}.search-field{position:relative;flex:1;min-width:200px;max-width:440px}.search-field input{width:100%;padding-left:36px}.search-field>i{position:absolute;left:13px;top:14px;color:#9bb0bd}.table-filters>.outline-button{margin-left:auto}.table-scroll{width:100%;overflow-x:auto}.statistics-page table{width:100%;border-collapse:collapse;font-size:12px;text-align:left;white-space:nowrap}.statistics-page th{padding:17px 20px;background:#eaf4fa;font-size:11px;font-weight:600;color:#668396;border-top:1px solid #e1ebf2;border-bottom:1px solid #dce9f1}.statistics-page td{padding:18px 20px;border-bottom:1px solid #edf2f6;vertical-align:middle;color:#526f82}.statistics-page th:first-child,.statistics-page td:first-child{padding-left:24px}.statistics-page tr:last-child td{border-bottom:0}.statistics-page tbody tr:hover td{background:#fafdff}.statistics-page .code{color:#35596f;font-weight:600}.statistics-page .amount{color:#168fc9;font-weight:700}.statistics-page .name-cell{white-space:normal;min-width:160px;max-width:280px;font-weight:500;line-height:1.6}.statistics-page .muted{color:#98aab6}.stock-badge{display:inline-block;border-radius:6px;padding:5px 9px;background:#eaf8f0;color:#27936c;font-size:11px}.stock-badge.empty{background:#fff0f1;color:#ce6670}.footnote{margin:0;padding:0 24px 20px;color:#96a8b4;font-size:11px;line-height:1.7}.empty-state{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:12px;min-height:240px;padding:30px 24px;color:#8da1ae;text-align:center;font-size:13px}.empty-state>i{font-size:30px;color:#c2d4e0}.empty-state strong{font-size:14px;font-weight:550;color:#6a8698}.empty-state span{font-size:12px}.empty-row{text-align:center!important;height:150px;color:#91a5b3!important}.notice{display:flex;align-items:center;gap:10px;padding:13px 16px;border-radius:9px;margin:0 0 18px;font-size:12px;line-height:1.6}.notice button{margin-left:auto;background:none;border:0;color:inherit;cursor:pointer}.warning{background:#fff7e8;color:#9b7b37;border:1px solid #f1dfb7}.error{background:#fff2f2;border:1px solid #f4d9dd;color:#b7616a}.success{background:#edf9f3;color:#30886a;border:1px solid #d5eddf}.error-text{color:#bf6973!important}
.comparison-canvas{height:340px;padding:12px 24px 25px}.detail-table summary{display:flex;align-items:center;gap:10px;list-style:none;cursor:pointer;padding:20px 24px;font-size:13px;font-weight:600;color:#45677c;border-top:1px solid #e6eef4}.detail-table summary::-webkit-details-marker{display:none}.detail-table summary:after{content:'⌄';margin-left:auto;color:#87a1b1}.detail-table[open]>summary:after{content:'⌃'}.detail-table summary span{font-weight:400;color:#95a8b5;font-size:11px}.fixed-periods summary{border-top:0}.fixed-periods .panel-heading{padding-top:0}.fixed-periods .panel-heading p{margin:0;max-width:70%;font-size:12px}.growth-positive,.growth-negative{font-size:11px;border-radius:20px;padding:5px 8px;font-weight:600}.growth-positive{background:#eaf8f0;color:#25936d}.growth-negative{background:#fff0f1;color:#cc6672}.report-layout{display:grid;grid-template-columns:minmax(0,1.5fr) minmax(260px,1fr);gap:22px;align-items:start}.report-form{padding:0 24px 24px;display:flex;flex-direction:column;gap:21px}.report-dates{display:grid;grid-template-columns:1fr 1fr;gap:16px}.report-actions{display:flex;align-items:center;justify-content:space-between;gap:14px;border-top:1px solid #e8eef3;padding-top:20px}.report-actions>span{color:#90a5b3;font-size:11px;display:flex;gap:7px;align-items:center}.report-aside{padding:26px}.report-aside h2{font-size:16px;margin:20px 0 14px}.aside-icon{display:flex;align-items:center;justify-content:center;width:44px;height:44px;border-radius:12px;background:#eaf6fc;color:#1695ce;font-size:21px}.report-aside ul{margin:0;padding:0 0 0 18px;font-size:12px;line-height:2.5;color:#6d899c}.report-aside p{font-size:11px;color:#95a7b4;line-height:1.8;margin:20px 0 0}
@media(min-width:1700px){.metric>strong{font-size:28px}.sales-canvas{height:370px}}@media(max-width:1250px){.metric-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.overview-charts{grid-template-columns:1fr}.status-panel{display:grid;grid-template-columns:260px minmax(0,1fr)}.status-panel .panel-heading{grid-column:1/-1}.status-panel .donut-wrap{grid-column:1;margin-top:0}.status-panel .status-legend{grid-column:2;grid-row:2}.status-panel .empty-state{grid-column:1/-1}.quick-periods{margin-left:0}.filter-row{flex-wrap:wrap}.filter-row>label{flex:1}.quick-periods{flex-basis:100%}.sales-panel .panel-heading{flex-wrap:wrap}}@media(max-width:768px){.statistics-page{padding:20px 16px}.view-tabs button{padding:12px}.panel-heading{padding:19px 18px;flex-wrap:wrap}.metric{padding:18px 16px}.metric>strong{font-size:22px}.secondary-metrics{grid-template-columns:repeat(2,minmax(0,1fr));row-gap:20px}.secondary-metrics>div{border:0}.filter-row{padding:0 18px;gap:12px}.filter-caption{padding-left:18px}.filter-panel .panel-heading{align-items:center}.panel-heading h2,.section-heading h2{font-size:15px}.report-layout{grid-template-columns:1fr}.brand-content{grid-template-columns:1fr;gap:20px}.table-filters{padding-left:18px;padding-right:18px}.status-panel{display:block}.status-panel .status-legend{padding:0 22px 20px;grid-template-columns:1fr}.sales-canvas{height:300px}.chart-controls{flex-wrap:wrap}.detail-table summary{padding:18px}.detail-table summary span{display:none}.report-actions{flex-direction:column;align-items:flex-start}.report-actions button{align-self:flex-end}.report-form{padding:0 18px 20px}.comparison-canvas{padding-left:12px;padding-right:12px}.fixed-periods .panel-heading p{max-width:100%}.filter-row>label{flex-basis:calc(50% - 12px)}}@media(max-width:420px){.metric-grid{gap:10px}.metric>strong{font-size:20px}.metric>small{font-size:10px}.page-heading h1{font-size:23px}.outline-button{padding:0 10px;font-size:11px!important}.quick-periods button{font-size:11px;padding:7px 9px}.report-dates{grid-template-columns:1fr}.table-filters .search-field{flex-basis:100%;max-width:none}.table-filters select{width:100%}}
.product-identity{display:flex;align-items:center;gap:13px;min-width:200px}.product-identity img,.product-image-fallback{width:44px;height:44px;flex:0 0 44px;border-radius:8px;border:1px solid #e3edf3;background:#f7fafc;object-fit:contain}.product-image-fallback{display:flex;align-items:center;justify-content:center;color:#a5b9c6;font-size:18px}.product-identity>span:last-child{line-height:1.6}.brand-panel>summary{border-top:0}.report-layout{grid-template-columns:repeat(2,minmax(0,1fr))}.report-kinds{display:flex;gap:9px}.report-kinds button{display:flex;align-items:center;gap:8px;flex:1;justify-content:center;background:#f7fafc;border:1px solid #dfeaf1;border-radius:9px;padding:14px 10px;color:#7992a3;font:inherit;cursor:pointer}.report-kinds button.active{color:#168fce;border-color:#a4d7ef;background:#edf8fe}.report-period-note{margin:0;font-size:12px;color:#8099aa;line-height:1.9}.report-period-note strong{color:#4d7188;font-weight:500}.report-period-note span{display:block;color:#b28b40}.schedule-zone{font-size:10px;color:#819bad;background:#f1f7fb;padding:7px 9px;border-radius:7px;white-space:nowrap}.statistics-page .schedule-option{display:flex;flex-direction:row;align-items:flex-start;gap:12px;background:#f8fbfd;border:1px solid #e3edf4;border-radius:10px;padding:16px;cursor:pointer}.statistics-page .schedule-option input{height:17px;width:17px;min-width:17px;margin:2px 0 0;accent-color:#168fce}.schedule-option span{display:flex;flex-direction:column;gap:7px}.schedule-option strong{font-size:13px;color:#466b83}.schedule-option small{font-size:11px;color:#8aa0b0;font-weight:400;line-height:1.7}.schedule-note{display:flex;gap:9px;align-items:flex-start;font-size:11px;color:#91a6b5;line-height:1.8;margin:0}.schedule-note i{padding-top:3px}.schedule-last{display:flex;flex-direction:column;gap:8px;padding:13px 0;border-bottom:1px solid #e5eef4;font-size:11px;color:#91a6b5}.schedule-last strong{font-weight:500;color:#66879d}@media(max-width:1000px){.report-layout{grid-template-columns:1fr}}@media(max-width:500px){.product-identity{min-width:180px}.product-identity img,.product-image-fallback{width:38px;height:38px;flex-basis:38px}.report-kinds button{font-size:12px}.report-layout .panel-heading{gap:10px}}

</style>
