<script setup>
import { computed, ref, onMounted, watch } from "vue";
import AdminLayout from "../../../../layouts/AdminLayout.vue";
import StatisticsSelect from "../../product/components/Select2Control.vue";

import {
  getStatisticsChart,
  getStatisticsSummary,
  getStatisticsPeriod,
  getStatisticsOrderStatus,
  getStatisticsEmployee,
  getStatisticsProduct,
  getStatisticsBrands,
  getStatisticsRevenueComparison,
  sendStatisticsReportEmail,
} from "../service/statisticsService";

import { Bar, Line, Pie } from "vue-chartjs";

import * as XLSX from "xlsx";

import {
  Chart as ChartJS,
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
const selectedBrand = ref("");

// Sẽ lấy dữ liệu thật từ API ở bước kết nối backend
const brandOptions = ref([]);

async function loadBrands() {
  try {
    const response = await getStatisticsBrands();

    // Hỗ trợ API trả về mảng trực tiếp hoặc ApiResponse
    const data = Array.isArray(response)
      ? response
      : Array.isArray(response?.data?.data)
        ? response.data.data
        : Array.isArray(response?.data)
          ? response.data
          : [];

    // Chỉ giữ thương hiệu hợp lệ
    brandOptions.value = data.filter(
      (brand) => brand && brand.id != null && typeof brand.name === "string",
    );
  } catch (error) {
    console.error("Lỗi tải thương hiệu:", error);

    brandOptions.value = [];
  }
}

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
  waiting: "#60A5FA", // Chờ xác nhận
  confirmed: "#2563EB", // Đã xác nhận
  ready: "#818CF8", // Chờ giao hàng
  shipping: "#06B6D4", // Đang giao hàng
  delivered: "#0EA5E9", // Đã giao hàng
  done: "#1D4ED8", // Hoàn thành
  cancel: "#64748B", // Đã hủy - xám xanh
  refund: "#A5B4FC", // Hoàn tiền
  pending: "#94A3B8", // Hóa đơn chờ
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
    alert("Dữ liệu sản phẩm đang tải. Vui lòng đợi!");
    return;
  }

  // Kiểm tra API gặp lỗi
  if (productError.value) {
    alert("Không thể xuất Excel vì dữ liệu sản phẩm đang bị lỗi.");
    return;
  }

  // Kiểm tra khoảng ngày
  if (!fromDate.value || !toDate.value || fromDate.value > toDate.value) {
    alert("Vui lòng chọn khoảng ngày hợp lệ trước khi xuất Excel.");
    return;
  }

  // Kiểm tra dữ liệu rỗng
  if (!filteredProducts.value.length) {
    alert("Không có dữ liệu sản phẩm để xuất Excel.");
    return;
  }

  const rows = filteredProducts.value.map((product, index) => ({
    STT: index + 1,
    "Mã sản phẩm": product.code,
    "Tên sản phẩm": product.name,
    "Số lượng đã bán": Number(product.sold || 0),
    "Số lượng hoàn tiền": Number(product.returned || 0),

    "Doanh số SP trước giảm giá (VNĐ)": Number(product.revenue || 0),
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
    alert("Dữ liệu nhân viên đang tải. Vui lòng đợi!");
    return;
  }

  // Kiểm tra API gặp lỗi
  if (employeeError.value) {
    alert("Không thể xuất Excel vì dữ liệu nhân viên đang bị lỗi.");
    return;
  }

  // Kiểm tra khoảng ngày
  if (!fromDate.value || !toDate.value || fromDate.value > toDate.value) {
    alert("Vui lòng chọn khoảng ngày hợp lệ trước khi xuất Excel.");
    return;
  }

  // Kiểm tra dữ liệu rỗng
  if (!filteredEmployees.value.length) {
    alert("Không có dữ liệu nhân viên để xuất Excel.");
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
    alert("Dữ liệu đang được tải. Vui lòng đợi!");
    return;
  }

  // Kiểm tra API gặp lỗi
  if (periodError.value) {
    alert("Không thể xuất Excel vì dữ liệu thống kê đang bị lỗi.");
    return;
  }

  // Kiểm tra dữ liệu rỗng
  if (!periodData.value.length) {
    alert("Không có dữ liệu thống kê để xuất Excel.");
    return;
  }

  const rows = periodData.value.map((item, index) => ({
    STT: index + 1,
    "Thời gian": item.period,
    "Doanh thu (VNĐ)": Number(item.revenue || 0),
    "Số đơn hàng": Number(item.orders || 0),
    "AOV (VNĐ)": Number(item.aov || 0),
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
      borderColor: "#2563EB",
      backgroundColor: "#2563EB",
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
      thuongHieuId: selectedBrand.value || undefined,
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
    alert("Dữ liệu đang tải, vui lòng thử lại.");
    return;
  }

  // Kiểm tra lỗi API
  if (comparisonError.value) {
    alert("Không thể xuất Excel vì dữ liệu đang bị lỗi.");
    return;
  }

  // Kiểm tra dữ liệu rỗng
  if (!revenueComparisonRows.value.length) {
    alert("Không có dữ liệu doanh thu để xuất Excel.");
    return;
  }

  // Chuyển dữ liệu sang Excel
  const rows = revenueComparisonRows.value.map((item) => ({
    STT: item.stt,
    "Thời gian": item.period,
    "Doanh thu thực tế (VNĐ)": item.revenue,
    "Số đơn hàng": item.orders,
    "Giá trị trung bình đơn - AOV (VNĐ)": item.aov,
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
const reportFromDate = ref(fromDate.value);
const reportToDate = ref(toDate.value);

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
      "Đã gửi báo cáo tổng kết thành công đến " + email;
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

onMounted(() => {
  loadBrands();
  loadCharts();
  loadSummary();
  loadPeriod();
  loadOrderStatus();
  loadEmployee();
  loadProduct();

  // Tải biểu đồ so sánh doanh thu
  loadRevenueComparison();
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
      <!-- TIÊU ĐỀ THỐNG KÊ - GIAO DIEN NEN SANG -->
      <header class="dashboard-hero">
        <div class="dashboard-hero-copy">
          <div class="dashboard-eyebrow">Báo cáo kinh doanh</div>
          <h1>Thống kê tổng hợp</h1>
          <p>Theo dõi doanh thu, hiệu suất bán hàng và hoạt động kinh doanh.</p>
        </div>

        <div class="dashboard-hero-mark">
          <i class="bi bi-bar-chart-line"></i>
          <span>{{
            activeStatisticsView === 'overview'
              ? 'Tổng quan cửa hàng'
              : activeStatisticsView === 'comparison'
                ? 'So sánh doanh thu'
                : 'Gửi mail tổng kết'
          }}</span>
        </div>
      </header>

      <!-- Chuyen 3 chuc nang - giu cac API va noi dung da co -->
      <nav class="statistics-view-navigation" aria-label="Chức năng thống kê">
        <button
          type="button"
          :class="{ active: activeStatisticsView === 'overview' }"
          :aria-pressed="activeStatisticsView === 'overview'"
          @click="changeStatisticsView('overview')"
        >
          <i class="bi bi-speedometer2"></i>
          Tổng quan
        </button>
        <button
          type="button"
          :class="{ active: activeStatisticsView === 'comparison' }"
          :aria-pressed="activeStatisticsView === 'comparison'"
          @click="changeStatisticsView('comparison')"
        >
          <i class="bi bi-bar-chart-line"></i>
          So sánh doanh thu
        </button>
        <button
          type="button"
          :class="{ active: activeStatisticsView === 'email' }"
          :aria-pressed="activeStatisticsView === 'email'"
          @click="changeStatisticsView('email')"
        >
          <i class="bi bi-envelope"></i>
          Gửi mail tổng kết
        </button>
      </nav>

      <!-- ===== GUI EMAIL BAO CAO TONG KET ===== -->
      <section
        v-if="activeStatisticsView === 'email'"
        class="report-email-section"
      >
        <div class="report-email-card">
          <!-- TIEU DE -->
          <div class="report-email-header">
            <div class="report-email-heading">
              <div class="report-email-icon">
                <i class="bi bi-envelope-paper"></i>
              </div>

              <div>
                <h3>Gửi mail tổng kết doanh thu</h3>
                <p>
                  Gửi báo cáo kết quả kinh doanh SmashStep trực tiếp đến email
                  người nhận.
                </p>
              </div>
            </div>
          </div>

          <!-- FORM GUI EMAIL -->
          <form class="report-email-form" @submit.prevent="sendRevenueReport">
            <div class="report-email-field">
              <label for="report-email">
                <i class="bi bi-envelope"></i>
                Email người nhận
              </label>

              <input
                id="report-email"
                v-model.trim="reportEmail"
                type="email"
                placeholder="Nhập địa chỉ email nhận báo cáo..."
                autocomplete="email"
                maxlength="254"
                required
                :disabled="sendingReportEmail"
              />
            </div>

            <!-- CHON THOI GIAN -->
            <div class="report-email-date-grid">
              <div class="report-email-field">
                <label for="report-from-date">
                  <i class="bi bi-calendar3"></i>
                  Từ ngày
                </label>

                <input
                  id="report-from-date"
                  v-model="reportFromDate"
                  type="date"
                  required
                  :disabled="sendingReportEmail"
                />
              </div>

              <div class="report-email-field">
                <label for="report-to-date">
                  <i class="bi bi-calendar-check"></i>
                  Đến ngày
                </label>

                <input
                  id="report-to-date"
                  v-model="reportToDate"
                  type="date"
                  required
                  :min="reportFromDate || undefined"
                  :disabled="sendingReportEmail"
                />
              </div>
            </div>

            <!-- NOI DUNG BAO CAO -->
            <div class="report-email-preview">
              <h4>
                <i class="bi bi-file-earmark-bar-graph"></i>
                Nội dung báo cáo sẽ gửi
              </h4>

              <div class="report-email-preview-grid">
                <span>
                  <i class="bi bi-check-circle"></i>
                  Doanh thu thực tế
                </span>

                <span>
                  <i class="bi bi-check-circle"></i>
                  Doanh thu dự kiến
                </span>

                <span>
                  <i class="bi bi-check-circle"></i>
                  Tiền mặt / chuyển khoản
                </span>

                <span>
                  <i class="bi bi-check-circle"></i>
                  Tổng số đơn hàng
                </span>

                <span>
                  <i class="bi bi-check-circle"></i>
                  Giá trị trung bình đơn (AOV)
                </span>

                <span>
                  <i class="bi bi-check-circle"></i>
                  Sản phẩm bán được và khách mới
                </span>
              </div>
            </div>

            <!-- THONG BAO LOI -->
            <div
              v-if="reportEmailError"
              class="report-email-alert report-email-error"
              role="alert"
            >
              <i class="bi bi-exclamation-triangle"></i>
              {{ reportEmailError }}
            </div>

            <!-- THONG BAO THANH CONG -->
            <div
              v-if="reportEmailSuccess"
              class="report-email-alert report-email-success"
              role="status"
            >
              <i class="bi bi-check-circle"></i>
              {{ reportEmailSuccess }}
            </div>

            <!-- NUT GUI -->
            <div class="report-email-actions">
              <span class="report-email-note">
                <i class="bi bi-shield-check"></i>
                Chỉ tài khoản Admin được phép gửi báo cáo.
              </span>

              <button
                type="submit"
                class="report-email-submit"
                :disabled="sendingReportEmail"
              >
                <i
                  :class="
                    sendingReportEmail ? 'bi bi-hourglass-split' : 'bi bi-send'
                  "
                ></i>

                {{
                  sendingReportEmail
                    ? "Đang gửi báo cáo..."
                    : "Gửi mail tổng kết"
                }}
              </button>
            </div>
          </form>
        </div>
      </section>

      <!-- BỘ LỌC TỔNG QUAN -->
      <section v-if="activeStatisticsView === 'overview'" class="filter-card">
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
            <label for="stats-from-date">Từ ngày</label>
            <input
              id="stats-from-date"
              v-model="fromDate"
              type="date"
              :aria-invalid="!!dateValidationMessage"
            />
          </div>

          <div class="filter-field">
            <label for="stats-to-date">Đến ngày</label>
            <input
              id="stats-to-date"
              v-model="toDate"
              type="date"
              :aria-invalid="!!dateValidationMessage"
            />
          </div>

          <div class="filter-field">
            <label>Nhóm thời gian biểu đồ</label>
            <div class="stats-select">
              <StatisticsSelect
                v-model="groupBy"
                :allow-clear="false"
                aria-label="Nhóm thời gian biểu đồ"
              >
                <option value="day">Theo ngày</option>
                <option value="month">Theo tháng</option>
                <option value="year">Theo năm</option>
              </StatisticsSelect>
            </div>
          </div>

          <div class="filter-field">
            <label>Thương hiệu biểu đồ</label>
            <div class="stats-select">
              <StatisticsSelect
                v-model="selectedBrand"
                :allow-clear="false"
                aria-label="Thương hiệu biểu đồ"
              >
                <option value="">Tất cả thương hiệu</option>

                <option
                  v-for="brand in brandOptions"
                  :key="brand.id"
                  :value="brand.id"
                >
                  {{ brand.name }}
                </option>
              </StatisticsSelect>
            </div>
          </div>

          <button type="button" class="reset-button" @click="resetFilters">
            <i class="bi bi-arrow-repeat"></i>
            Đặt lại
          </button>
        </div>
      </section>

      <!-- THANH THÔNG TIN THỜI GIAN -->
      <section
        v-if="activeStatisticsView === 'overview'"
        class="information-bar"
      >
        <div class="information-left">
          <span class="blue-dot"></span>
          <span>
            Đang hiển thị dữ liệu tổng quan từ ngày
            <strong>{{ periodLabel }}</strong>
          </span>
        </div>
      </section>

      <p
        v-if="activeStatisticsView === 'overview' && dateValidationMessage"
        class="date-error"
        role="alert"
      >
        <i class="bi bi-exclamation-triangle"></i>
        {{ dateValidationMessage }}
      </p>

      <!-- 7 Ô DOANH THU -->
      <section
        v-if="activeStatisticsView === 'overview'"
        class="summary-section"
      >
        <!-- HÀNG TRÊN -->

        <!-- Trạng thái đang tải -->
        <div v-if="loadingSummary" class="summary-message">
          <i class="bi bi-hourglass-split"></i>
          Đang tải dữ liệu tổng quan doanh thu...
        </div>

        <!-- Trạng thái API lỗi -->
        <div
          v-else-if="summaryError"
          class="summary-message summary-message-error"
          role="alert"
        >
          <i class="bi bi-exclamation-triangle"></i>
          {{ summaryError }}
        </div>

        <div
          v-if="!loadingSummary && !summaryError"
          class="summary-grid top-grid"
        >
          <div class="stat-card">
            <div class="stat-icon mint">
              <i class="bi bi-graph-up-arrow"></i>
            </div>
            <strong>{{ money(dashboardSummary?.totalRevenue) }}</strong>

            <span>Doanh thu thực tế (đơn hoàn thành)</span>
          </div>

          <!-- DOANH THU DỰ KIẾN -->
          <div class="stat-card stat-card-expected">
            <div class="stat-icon blue">
              <i class="bi bi-clock-history"></i>
            </div>

            <strong>
              {{ money(dashboardSummary?.expectedRevenue) }}
            </strong>

            <span> Doanh thu dự kiến (đã xác nhận / chờ lấy hàng) </span>

            <small>
              {{ number(dashboardSummary?.expectedOrders) }}
              đơn • theo ngày tạo
            </small>
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
        <div
          v-if="!loadingSummary && !summaryError"
          class="summary-grid bottom-grid"
        >
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
      <section v-if="activeStatisticsView === 'overview'" class="charts-grid">
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

              <div class="stats-select">
                <StatisticsSelect
                  v-model="brandChartSort"
                  :allow-clear="false"
                  aria-label="Sắp xếp thương hiệu"
                >
                  <option value="revenue">Theo doanh thu</option>
                  <option value="quantity">Theo số lượng</option>
                </StatisticsSelect>
              </div>
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

      <!-- ===== BIỂU ĐỒ SO SÁNH DOANH THU ===== -->

      <section
        v-if="activeStatisticsView === 'comparison'"
        class="revenue-comparison-card"
      >
        <div class="revenue-comparison-header">
          <h3>
            <i class="bi bi-bar-chart-line"></i>
            So sánh doanh thu và tăng trưởng
          </h3>

          <div class="revenue-comparison-filters">
            <select v-model="comparisonMode">
              <option value="month">Theo tháng</option>
              <option value="quarter">Theo quý</option>
              <option value="year">Theo năm</option>
            </select>

            <input
              v-model.number="comparisonYear"
              type="number"
              min="2000"
              max="2100"
              aria-label="Năm thống kê"
            />
          </div>
        </div>

        <div v-if="loadingComparison" class="comparison-message">
          Đang tải dữ liệu so sánh doanh thu...
        </div>

        <div
          v-else-if="comparisonError"
          class="comparison-message"
          role="alert"
        >
          {{ comparisonError }}
        </div>

        <template v-else-if="visibleComparisonData.length">
          <div class="revenue-comparison-chart">
            <Bar
              :data="revenueComparisonChart"
              :options="revenueComparisonOptions"
            />
          </div>

          <p class="comparison-note">
            Doanh thu tính từ đơn hoàn thành. Kỳ hiện tại là số liệu tạm tính.
            Các kỳ tương lai không hiển thị.
          </p>
        </template>

        <div v-else class="comparison-message">
          Chưa có dữ liệu so sánh doanh thu.
        </div>
      </section>

      <!-- ===== BANG DOI CHIEU DOANH THU ===== -->

      <section
        v-if="activeStatisticsView === 'comparison'"
        class="revenue-comparison-card"
      >
        <div class="revenue-comparison-header">
          <h3>
            <i class="bi bi-table"></i>
            Bảng đối chiếu doanh thu
          </h3>

          <button
            type="button"
            class="period-export-btn"
            :disabled="
              loadingComparison ||
              !!comparisonError ||
              !revenueComparisonRows.length
            "
            @click="exportRevenueComparisonExcel"
          >
            <i class="bi bi-file-earmark-excel"></i>
            Xuất Excel
          </button>

          <span class="comparison-note">
            Dữ liệu theo
            {{
              comparisonMode === "month"
                ? "tháng"
                : comparisonMode === "quarter"
                  ? "quý"
                  : "năm"
            }}
          </span>
        </div>

        <!-- Đang tải -->
        <div v-if="loadingComparison" class="comparison-message">
          Đang tải bảng đối chiếu doanh thu...
        </div>

        <!-- Lỗi API -->
        <div
          v-else-if="comparisonError"
          class="comparison-message"
          role="alert"
        >
          {{ comparisonError }}
        </div>

        <!-- Bảng dữ liệu -->
        <div v-else class="period-table-wrapper">
          <table class="period-table">
            <thead>
              <tr>
                <th>STT</th>
                <th>Thời gian</th>
                <th>Doanh thu thực tế</th>
                <th>Số đơn</th>
                <th>AOV</th>
                <th>Tăng trưởng</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="row in revenueComparisonRows" :key="row.period">
                <td>{{ row.stt }}</td>

                <td class="period-name">
                  {{ row.period }}
                </td>

                <td>
                  {{ money(row.revenue) }}
                </td>

                <td>
                  {{ number(row.orders) }}
                </td>

                <td>
                  {{ money(row.aov) }}
                </td>

                <td>
                  <span
                    :class="{
                      'growth-positive': row.growth != null && row.growth > 0,
                      'growth-negative': row.growth != null && row.growth < 0,
                    }"
                  >
                    {{ growthText(row.growth) }}
                  </span>
                </td>
              </tr>

              <tr v-if="!revenueComparisonRows.length">
                <td colspan="6" class="period-message">
                  Chưa có dữ liệu đối chiếu doanh thu.
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <p class="comparison-note">
          Doanh thu được tính từ đơn hoàn thành. Kỳ hiện tại là số liệu tạm
          tính. Tăng trưởng được so với toàn bộ kỳ trước.
        </p>
      </section>

      <!-- TỔNG QUAN THEO CHU KỲ THỜI GIAN -->
      <section
        v-if="activeStatisticsView === 'overview'"
        class="period-section"
      >
        <div class="period-card">
          <div class="period-header">
            <h3>
              <i class="bi bi-calendar3"></i>
              Tổng quan theo chu kỳ thời gian
            </h3>

            <button
              type="button"
              class="period-export-btn"
              @click="exportPeriodExcel"
            >
              <i class="bi bi-file-earmark-excel"></i>
              Xuất Excel
            </button>
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

      <section
        v-if="activeStatisticsView === 'overview'"
        class="employee-section"
      >
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
              aria-label="Tìm tên hoặc mã nhân viên"
              placeholder="Tìm tên hoặc mã nhân viên..."
            />

            <div class="stats-select">
              <StatisticsSelect
                v-model="employeeSort"
                :allow-clear="false"
                aria-label="Sắp xếp nhân viên"
              >
                <option value="revenue">Doanh thu cao nhất</option>
                <option value="totalOrders">Số đơn nhiều nhất</option>
                <option value="productsSold">Sản phẩm bán nhiều nhất</option>
                <option value="name">Tên nhân viên A-Z</option>
              </StatisticsSelect>
            </div>

            <button
              type="button"
              class="employee-reset-btn"
              @click="resetEmployeeFilters"
            >
              <i class="bi bi-arrow-repeat"></i>
              Đặt lại
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

      <section
        v-if="activeStatisticsView === 'overview'"
        class="employee-section"
      >
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

          <p class="product-revenue-note">
            <i class="bi bi-info-circle"></i>
            Doanh số sản phẩm được tính từ chi tiết hóa đơn, chưa phân bổ giảm
            giá và phí vận chuyển ở cấp hóa đơn nên có thể khác tổng doanh thu.
          </p>

          <!-- BỘ LỌC -->
          <div class="employee-filters">
            <input
              v-model="productSearch"
              type="text"
              aria-label="Tìm mã hoặc tên sản phẩm"
              placeholder="Tìm mã hoặc tên sản phẩm..."
            />

            <div class="stats-select">
              <StatisticsSelect
                v-model="productStockFilter"
                :allow-clear="false"
                aria-label="Lọc tồn kho"
              >
                <option value="all">Tất cả tồn kho</option>
                <option value="inStock">Còn hàng</option>
                <option value="outOfStock">Hết hàng</option>
              </StatisticsSelect>
            </div>

            <div class="stats-select">
              <StatisticsSelect
                v-model="productSort"
                :allow-clear="false"
                aria-label="Sắp xếp sản phẩm"
              >
                <option value="sold">Bán nhiều nhất</option>
                <option value="revenue">Doanh thu cao nhất</option>
                <option value="stock">Tồn kho nhiều nhất</option>
                <option value="name">Tên sản phẩm A-Z</option>
              </StatisticsSelect>
            </div>

            <button
              type="button"
              class="employee-reset-btn"
              @click="resetProductFilters"
            >
              <i class="bi bi-arrow-repeat"></i>
              Đặt lại
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
                  <th>Doanh số SP (trước giảm giá)</th>
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
/* ===== GIAO DIEN THONG KE SMASHSTEP ===== */

.statistics-page {
  --primary: #2563eb;
  --primary-dark: #1d4ed8;
  --text: #1e293b;
  --muted: #64748b;
  --border: #e7ebf2;
  --shadow: 0 5px 22px rgba(31, 41, 70, 0.05);

  min-height: 100vh;
  padding: 26px;
  background: #f4f6fa;
  color: var(--text);
  font-family: "Segoe UI", Arial, sans-serif;
}

.statistics-page * {
  box-sizing: border-box;
}

/* ===== HEADER ===== */

.dashboard-hero {
  position: relative;
  overflow: hidden;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 32px;
  margin-bottom: 22px;
  border-radius: 18px;

  background: linear-gradient(110deg, #102a56 0%, #1d4ed8 65%, #3b82f6 100%);
  color: white;
  box-shadow: 0 10px 30px rgba(22, 32, 56, 0.13);
}

.dashboard-hero::after {
  content: "";
  position: absolute;
  width: 250px;
  height: 250px;
  right: -60px;
  top: -120px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow:
    0 0 0 45px rgba(255, 255, 255, 0.025),
    0 0 0 90px rgba(255, 255, 255, 0.02);
  pointer-events: none;
}

.dashboard-eyebrow {
  color: #bfdbfe;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 2px;
  margin-bottom: 8px;
}

.dashboard-hero h1 {
  font-size: 29px;
  font-weight: 800;
  color: white;
  margin: 0 0 8px;
}

.dashboard-hero p {
  color: #cbd5e1;
  font-size: 13px;
  margin: 0;
}

.dashboard-hero-mark {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
  padding: 12px 17px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  color: #f1f5f9;
  font-size: 12px;
  font-weight: 700;
}

.dashboard-hero-mark i {
  color: #ff859b;
  font-size: 19px;
}

/* ===== CARD CHUNG ===== */

.filter-card,
.chart-card,
.period-card,
.order-status-card,
.employee-card {
  min-width: 0;
  background: white;
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: var(--shadow);
}

/* ===== TIEU DE CAC KHU VUC ===== */

.filter-header,
.chart-header,
.period-header,
.employee-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  padding-bottom: 17px;
  margin-bottom: 18px;
  border-bottom: 1px solid #edf0f5;
}

.filter-header h3,
.chart-header h3,
.period-header h3,
.employee-header h3 {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 15px;
  color: #1e293b;
  font-weight: 800;
}

.filter-header h3 i,
.chart-header h3 i,
.period-header h3 i,
.employee-header h3 i {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  width: 33px;
  height: 33px;
  border-radius: 10px;
  background: #fff0f3;
  color: #df2849;
  font-size: 16px;
}

/* ===== BO LOC TONG QUAN ===== */

.filter-card {
  padding: 23px 25px;
}

.filter-header {
  flex-wrap: wrap;
}

.quick-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.quick-filters button {
  border: 1px solid #e5eaf1;
  border-radius: 9px;
  background: #f8fafc;
  color: #64748b;
  padding: 9px 13px;
  font-size: 12px;
  font-weight: 650;
  cursor: pointer;
  transition: 0.2s;
}

.quick-filters button:hover {
  background: #fff0f3;
  color: #df2849;
  border-color: #f4b8c6;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr)) auto;
  gap: 13px;
  align-items: end;
}

.filter-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.filter-field label {
  font-size: 12px;
  color: #64748b;
  font-weight: 700;
}

.filter-field input,
.filter-field select,
.employee-filters input,
.employee-filters select {
  width: 100%;
  min-width: 0;
  height: 43px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fbfcfe;
  outline: none;
  color: #334155;
  font-size: 13px;
  transition: 0.2s;
}

.filter-field input:focus,
.filter-field select:focus,
.employee-filters input:focus,
.employee-filters select:focus {
  border-color: #e8758c;
  background: white;
  box-shadow: 0 0 0 3px rgba(223, 40, 73, 0.07);
}

.reset-button,
.employee-reset-btn {
  height: 43px;
  padding: 0 16px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: white;
  color: #475569;
  font-weight: 700;
  font-size: 12px;
  cursor: pointer;
  transition: 0.2s;
}

.reset-button:hover,
.employee-reset-btn:hover {
  color: #df2849;
  background: #fff5f7;
  border-color: #f4b8c6;
}

/* ===== THANH THONG TIN ===== */

.information-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 17px;
  padding: 14px 19px;
  background: white;
  border: 1px solid var(--border);
  border-radius: 12px;
  color: #64748b;
  font-size: 12px;
}

.information-left,
.information-right {
  display: flex;
  align-items: center;
  gap: 11px;
  flex-wrap: wrap;
}

.information-left strong {
  color: #1e293b;
  font-weight: 800;
}

.blue-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #22c55e;
  box-shadow: 0 0 0 4px #dcfce7;
  flex-shrink: 0;
}

.refresh-button {
  padding: 9px 14px;
  border: 1px solid #f4c3cd;
  border-radius: 9px;
  background: #fff6f7;
  color: #d32948;
  font-size: 12px;
}

.refresh-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.date-error {
  margin: 12px 0;
  padding: 11px 15px;
  background: #fff1f2;
  color: #be123c;
  border-radius: 9px;
  font-size: 13px;
}

/* ===== 7 THE DOANH THU ===== */

.summary-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 20px;
}

.summary-grid {
  display: grid;
  gap: 16px;
}

.top-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.bottom-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.stat-card {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  min-width: 0;
  min-height: 156px;
  gap: 7px;
  padding: 22px;
  overflow: hidden;
  background: white;
  border: 1px solid var(--border);
  border-radius: 16px;
  box-shadow: var(--shadow);
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 30px rgba(31, 41, 70, 0.1);
}

.stat-card::before {
  content: "";
  position: absolute;
  top: 0;
  left: 21px;
  right: 21px;
  height: 4px;
  background: #e2e8f0;
  border-radius: 0 0 4px 4px;
}

.top-grid .stat-card:first-child {
  background: linear-gradient(120deg, #fff, #fff6f8);
  border-color: #f4d7df;
}

.top-grid .stat-card:nth-child(1)::before {
  background: #df2849;
}

.top-grid .stat-card:nth-child(2)::before {
  background: #10b981;
}

.top-grid .stat-card:nth-child(3)::before {
  background: #8b5cf6;
}

.bottom-grid .stat-card:nth-child(1)::before {
  background: #0ea5e9;
}

.bottom-grid .stat-card:nth-child(2)::before {
  background: #a78bfa;
}

.bottom-grid .stat-card:nth-child(3)::before {
  background: #f59e0b;
}

.bottom-grid .stat-card:nth-child(4)::before {
  background: #fb7185;
}

.stat-icon {
  width: 42px;
  height: 42px;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
  border-radius: 12px;
  font-size: 20px;
  margin-bottom: 6px;
}

.mint {
  background: #ffe4ea;
  color: #df2849;
}

.green {
  background: #dcfce7;
  color: #16a34a;
}

.purple {
  background: #f0eaff;
  color: #7c3aed;
}

.blue {
  background: #e0f2fe;
  color: #0284c7;
}

.yellow {
  background: #fef3c7;
  color: #d97706;
}

.red {
  background: #ffe4e6;
  color: #e11d48;
}

.stat-card strong {
  width: 100%;
  font-size: clamp(20px, 1.8vw, 27px);
  font-weight: 800;
  line-height: 1.25;
  color: #172033;
  letter-spacing: -0.5px;
  overflow-wrap: anywhere;
}

.stat-card span {
  font-size: 12px;
  color: #64748b;
  font-weight: 650;
}

/* ===== BIEU DO ===== */

.charts-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.65fr) minmax(0, 1fr);
  gap: 19px;
  margin-top: 20px;
}

.chart-card {
  min-height: 490px;
  padding: 23px 24px;
}

.chart-header {
  flex-wrap: wrap;
}

.chart-right {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-wrap: wrap;
}

.chart-actions {
  display: inline-flex;
  padding: 4px;
  gap: 4px;
  border: 1px solid #e9edf4;
  border-radius: 10px;
  background: #f5f7fb;
}

.chart-actions button {
  padding: 8px 13px;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: 0.2s;
}

.chart-actions button.active {
  color: #df2849;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

.chart-actions button:hover {
  color: #df2849;
}

.chart-right select {
  height: 38px;
  padding: 0 11px;
  max-width: 165px;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  background: white;
  font-size: 12px;
  color: #475569;
}

.chart-content {
  min-height: 403px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-canvas {
  width: 100%;
  height: 363px;
  position: relative;
}

.chart-canvas canvas {
  width: 100% !important;
  height: 100% !important;
}

.chart-empty {
  width: 100%;
  padding: 30px 12px;
  color: #94a3b8;
  font-size: 13px;
  text-align: center;
}

.brand-chart-content {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  justify-content: flex-start;
  gap: 17px;
  padding-top: 8px;
}

.brand-chart-canvas {
  width: 100%;
  height: 242px;
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
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 11px 13px;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  background: #f9fafc;
  font-size: 12px;
}

.brand-name {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 9px;
  color: #475569;
  font-weight: 650;
}

.brand-name > span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-dot {
  width: 10px;
  height: 10px;
  flex-shrink: 0;
  border-radius: 4px;
}

.brand-list-item strong {
  color: #1e293b;
  font-weight: 800;
  white-space: nowrap;
}

/* ===== CHU KY VA TRANG THAI DON ===== */

.period-section {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(0, 1fr);
  align-items: stretch;
  gap: 19px;
  margin-top: 20px;
}

.period-card,
.order-status-card {
  padding: 23px 24px;
}

.period-table-wrapper,
.employee-table-wrapper {
  width: 100%;
  overflow-x: auto;
  border: 1px solid #e9edf4;
  border-radius: 11px;
}

.period-table,
.employee-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  white-space: nowrap;
}

.period-table th,
.employee-table th {
  background: #f5f7fb;
  color: #64748b;
  font-weight: 800;
  font-size: 12px;
  padding: 15px;
  text-align: left;
  border-bottom: 1px solid #e9edf4;
}

.period-table td,
.employee-table td {
  padding: 16px 15px;
  border-bottom: 1px solid #edf0f5;
  color: #475569;
  background: white;
}

.period-table tbody tr:last-child td,
.employee-table tbody tr:last-child td {
  border-bottom: none;
}

.period-table tbody tr:hover td,
.employee-table tbody tr:hover td {
  background: #fafbfe;
}

.period-name,
.employee-name {
  font-weight: 750 !important;
  color: #1e293b !important;
}

.growth-positive {
  display: inline-block;
  padding: 5px 9px;
  border-radius: 7px;
  background: #dcfce7;
  color: #15803d;
  font-weight: 800;
}

.growth-negative {
  display: inline-block;
  padding: 5px 9px;
  border-radius: 7px;
  background: #ffe4e6;
  color: #be123c;
  font-weight: 800;
}

.period-message {
  padding: 28px 15px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}

.period-error {
  color: #dc2626;
}

/* ===== CO CAU TRANG THAI DON HANG ===== */

.order-status-total {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #64748b;
  margin: 0 0 15px;
  font-size: 13px;
}

.order-status-total strong {
  color: #1e293b;
  font-size: 21px;
  font-weight: 850;
}

.order-status-canvas {
  width: 100%;
  height: 230px;
  position: relative;
  margin-bottom: 19px;
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
  min-height: 39px;
  padding: 10px 13px;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  background: #f9fafc;
  font-size: 12px;
}

.order-status-name {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #475569;
  font-weight: 650;
}

.order-status-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}

.order-status-item strong {
  color: #1e293b;
  font-weight: 800;
}

/* ===== BANG NHAN VIEN VA SAN PHAM ===== */

.employee-section {
  margin-top: 20px;
  margin-bottom: 0;
}

.employee-card {
  padding: 23px 24px;
}

.employee-header {
  flex-wrap: wrap;
}

.employee-filters {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 11px;
  margin-bottom: 19px;
}

.employee-filters input {
  flex: 1 1 230px;
}

.employee-filters select {
  width: auto;
  flex: 0 1 200px;
  min-width: 175px;
  cursor: pointer;
}

.employee-export-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 40px;
  padding: 10px 16px;
  border: 1px solid #16a34a;
  border-radius: 10px;
  background: #16a34a;
  color: white;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
  transition: 0.2s;
}

.employee-export-btn:hover {
  background: #15803d;
  border-color: #15803d;
  box-shadow: 0 5px 14px rgba(22, 163, 74, 0.2);
}

.employee-revenue {
  color: #16a34a !important;
  font-weight: 800 !important;
}

/* ===== PHAN TRANG ===== */

.employee-pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 19px;
  color: #64748b;
  font-size: 12px;
}

.employee-page-buttons {
  display: flex;
  align-items: center;
  gap: 12px;
  font-weight: 700;
}

.employee-page-buttons button {
  min-height: 35px;
  padding: 8px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 9px;
  background: white;
  color: #475569;
  font-size: 12px;
  font-weight: 750;
  cursor: pointer;
  transition: 0.2s;
}

.employee-page-buttons button:hover:not(:disabled) {
  border-color: #f4b8c6;
  color: #df2849;
  background: #fff5f7;
}

.employee-page-buttons button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* ===== RESPONSIVE ===== */

@media (max-width: 1440px) {
  .filter-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .reset-button {
    width: 100%;
  }
}

@media (max-width: 1190px) {
  .charts-grid,
  .period-section {
    grid-template-columns: 1fr;
  }

  .chart-card {
    min-height: 0;
  }
}

@media (max-width: 980px) {
  .top-grid,
  .bottom-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .top-grid .stat-card:first-child {
    grid-column: span 2;
  }

  .dashboard-hero-mark {
    display: none;
  }
}

@media (max-width: 680px) {
  .statistics-page {
    padding: 14px 12px 28px;
  }

  .dashboard-hero {
    padding: 23px 20px;
    border-radius: 14px;
  }

  .dashboard-hero h1 {
    font-size: 23px;
  }

  .filter-card,
  .chart-card,
  .period-card,
  .order-status-card,
  .employee-card {
    padding: 18px 15px;
    border-radius: 12px;
  }

  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-grid > .filter-field:nth-child(4),
  .filter-grid > .filter-field:nth-child(5) {
    grid-column: span 2;
  }

  .top-grid {
    grid-template-columns: 1fr;
  }

  .top-grid .stat-card:first-child {
    grid-column: auto;
  }

  .bottom-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .stat-card {
    min-height: 135px;
    padding: 16px;
  }

  .stat-card strong {
    font-size: 19px;
  }

  .employee-filters {
    flex-direction: column;
    align-items: stretch;
  }

  .employee-filters input,
  .employee-filters select,
  .employee-reset-btn {
    flex: none;
    width: 100%;
    min-width: 0;
  }

  .employee-pagination {
    flex-direction: column;
    align-items: flex-start;
  }

  .chart-header,
  .employee-header {
    align-items: flex-start;
  }

  .chart-canvas {
    height: 280px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .stat-card,
  .statistics-page button {
    transition: none;
  }

  .stat-card:hover {
    transform: none;
  }
}

/* ===== GIAO DIEN CHU KY VA TRANG THAI - ANH MAU ===== */

.period-section {
  grid-template-columns: minmax(0, 1.75fr) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.period-card,
.order-status-card {
  padding: 20px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #edf0f5;
  box-shadow: 0 3px 16px rgba(15, 23, 42, 0.04);
  min-height: 0;
}

.period-card .period-header,
.order-status-card .period-header {
  margin-bottom: 16px;
  padding-bottom: 15px;
}

.period-card .period-header h3,
.order-status-card .period-header h3 {
  font-size: 14px;
  font-weight: 800;
}

/* BANG CHU KY GIONG ANH 1 */

.period-card .period-table-wrapper {
  border: none;
  border-radius: 0;
  overflow-x: auto;
}

.period-card .period-table {
  border-collapse: separate;
  border-spacing: 0 9px;
}

.period-card .period-table th {
  background: transparent;
  border: none;
  padding: 11px 13px;
  font-size: 12px;
  color: #697386;
  white-space: nowrap;
}

.period-card .period-table td {
  background: #f7f9fc;
  border: none;
  padding: 18px 13px;
  font-size: 13px;
  white-space: nowrap;
}

.period-card .period-table tbody tr:hover td {
  background: #eff3f9;
}

.period-card .period-table tbody td:first-child {
  border-radius: 12px 0 0 12px;
  font-weight: 800;
}

.period-card .period-table tbody td:last-child {
  border-radius: 0 12px 12px 0;
}

.period-card .growth-positive {
  background: #eafbf1;
  color: #16a34a;
  padding: 7px 12px;
  border-radius: 20px;
  font-weight: 800;
}

.period-card .growth-negative {
  background: #fff0f2;
  color: #e11d48;
  padding: 7px 12px;
  border-radius: 20px;
  font-weight: 800;
}

/* BIEU DO TRANG THAI */

.order-status-card .order-status-total {
  font-size: 12px;
  margin-bottom: 8px;
}

.order-status-card .order-status-canvas {
  width: 100%;
  max-width: 260px;
  height: 205px;
  margin: 0 auto 20px;
}

/* DANH SACH TRANG THAI CHIA 2 COT */

.order-status-card .order-status-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px;
}

.order-status-card .order-status-item {
  min-width: 0;
  min-height: 42px;
  padding: 10px 12px;
  border: 1px solid #edf1f6;
  background: #f7f9fc;
  border-radius: 9px;
  gap: 7px;
}

.order-status-card .order-status-name {
  min-width: 0;
  font-size: 12px;
  overflow: hidden;
}

.order-status-card .order-status-name {
  white-space: nowrap;
  text-overflow: ellipsis;
}

.order-status-card .order-status-item strong {
  flex-shrink: 0;
  font-size: 12px;
  color: #172033;
}

@media (max-width: 1200px) {
  .period-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 600px) {
  .order-status-card .order-status-list {
    grid-template-columns: 1fr;
  }
}

/* ===== NÚT XUẤT EXCEL CHU KỲ ===== */

.period-card .period-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.period-export-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 9px 14px;
  border: 1px solid #fbd5df;
  border-radius: 9px;
  background: #fff6f8;
  color: #e54870;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
  transition: 0.2s ease;
}

.period-export-btn:hover {
  background: #ffe9ef;
  border-color: #f4a9bc;
}

.period-export-btn i {
  font-size: 14px;
}

/* TRẠNG THÁI TẢI DỮ LIỆU DOANH THU */

.summary-message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 95px;
  padding: 20px;
  border: 1px solid #e2e8f0;
  border-radius: 13px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
}

.summary-message i {
  font-size: 18px;
}

.summary-message-error {
  background: #fff1f2;
  border-color: #fecdd3;
  color: #be123c;
}

.product-revenue-note {
  margin: 0 0 16px;
  padding: 12px 15px;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: #eff6ff;
  color: #475569;
  font-size: 12px;
  line-height: 1.6;
}

.product-revenue-note i {
  margin-right: 7px;
  color: #2563eb;
}

/* ===== DONG BO MAU XANH DUONG ===== */

/* Icon tiêu đề các khối */
.statistics-page .filter-header h3 i,
.statistics-page .chart-header h3 i,
.statistics-page .period-header h3 i,
.statistics-page .employee-header h3 i {
  background: #dbeafe;
  color: #2563eb;
}

/* Nút Xuất Excel chu kỳ */
.statistics-page .period-export-btn {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

.statistics-page .period-export-btn:hover {
  background: #dbeafe;
  border-color: #60a5fa;
}

/* Nút biểu đồ đang được chọn */
.statistics-page .chart-actions button.active {
  color: #2563eb;
}

/* Màu viền ô lọc khi chọn */
.statistics-page .filter-field input:focus,
.statistics-page .filter-field select:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

/* Hover bộ lọc nhanh */
.statistics-page .quick-filters button:hover {
  color: #2563eb;
  border-color: #93c5fd;
  background: #eff6ff;
}

/* ===== KPI THONG KE - TONG XANH DUONG ===== */

/* Ô tổng doanh thu */
.statistics-page .top-grid .stat-card:first-child {
  background: linear-gradient(120deg, #ffffff, #eff6ff);
  border-color: #bfdbfe;
}

/* Thanh màu phía trên 7 ô */
.statistics-page .top-grid .stat-card:nth-child(1)::before {
  background: #2563eb;
}

.statistics-page .top-grid .stat-card:nth-child(2)::before {
  background: #3b82f6;
}

.statistics-page .top-grid .stat-card:nth-child(3)::before {
  background: #60a5fa;
}

.statistics-page .bottom-grid .stat-card:nth-child(1)::before {
  background: #1d4ed8;
}

.statistics-page .bottom-grid .stat-card:nth-child(2)::before {
  background: #2563eb;
}

.statistics-page .bottom-grid .stat-card:nth-child(3)::before {
  background: #0ea5e9;
}

.statistics-page .bottom-grid .stat-card:nth-child(4)::before {
  background: #38bdf8;
}

/* Icon KPI */
.statistics-page .stat-icon {
  background: #dbeafe;
  color: #2563eb;
}

/* Hiệu ứng khi rê chuột */
.statistics-page .stat-card:hover {
  border-color: #93c5fd;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.12);
}

/* ===== DONG BO BO LOC SANG XANH DUONG ===== */

/* Nút lọc nhanh */
.statistics-page .quick-filters button:hover {
  color: #2563eb;
  background: #eff6ff;
  border-color: #93c5fd;
}

/* Nút Reset */
.statistics-page .reset-button:hover,
.statistics-page .employee-reset-btn:hover {
  color: #2563eb;
  background: #eff6ff;
  border-color: #93c5fd;
}

/* Viền input và select khi chọn */
.statistics-page .filter-field input:focus,
.statistics-page .filter-field select:focus,
.statistics-page .employee-filters input:focus,
.statistics-page .employee-filters select:focus {
  outline: none;
  border-color: #2563eb;
  background: #ffffff;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

/* Nút chuyển đổi biểu đồ */
.statistics-page .chart-actions button.active {
  color: #2563eb;
  background: #eff6ff;
}

/* Nút phân trang */
.statistics-page .employee-page-buttons button:hover:not(:disabled) {
  color: #2563eb;
  border-color: #93c5fd;
  background: #eff6ff;
}

/* Màu chủ đạo */
.statistics-page {
  --primary: #2563eb;
  --primary-dark: #1d4ed8;
}

/* ===== SELECT2 TU MAIN - GIAO DIEN XANH DONG BO ===== */
.statistics-page .stats-select {
  min-width: 0;
  width: 100%;
}
.statistics-page .stats-select :deep(.select2-container) {
  width: 100% !important;
  min-width: 0;
  font-family: inherit;
  font-size: 13px;
}
.statistics-page .stats-select :deep(.select2-selection--single) {
  height: 43px;
  min-height: 43px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fbfcfe;
}
.statistics-page
  .stats-select
  :deep(.select2-selection--single .select2-selection__rendered) {
  display: block;
  line-height: 41px;
  padding: 0 32px 0 12px;
  color: #334155 !important;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
}
.statistics-page
  .stats-select
  :deep(.select2-selection--single .select2-selection__arrow) {
  height: 41px;
  right: 8px;
}
.statistics-page
  .stats-select
  :deep(.select2-container--open .select2-selection),
.statistics-page
  .stats-select
  :deep(.select2-container--focus .select2-selection) {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}
.statistics-page .employee-filters .stats-select {
  flex: 0 1 200px;
  min-width: 175px;
}
.statistics-page .chart-right .stats-select {
  width: 165px;
}
.statistics-page .dashboard-hero-mark i {
  color: #bfdbfe;
}

@media (max-width: 680px) {
  .statistics-page .employee-filters .stats-select {
    flex: none;
    width: 100%;
    min-width: 0;
  }
}

/* ===== DOANH THU DU KIEN ===== */

/* 4 ô KPI hàng trên */
.statistics-page .top-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

/* Thẻ doanh thu dự kiến */
.statistics-page .top-grid .stat-card-expected {
  background: linear-gradient(120deg, #f8fbff, #eaf3ff);

  border: 1px solid #bfdbfe;
}

/* Thanh màu trên thẻ */
.statistics-page .top-grid .stat-card-expected::before {
  background: #0ea5e9;
}

/* Ghi chú số đơn */
.statistics-page .stat-card-expected small {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
}

/* Laptop và tablet */
@media (max-width: 1100px) {
  .statistics-page .top-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .statistics-page .top-grid .stat-card:first-child {
    grid-column: auto;
  }
}

/* Điện thoại */
@media (max-width: 680px) {
  .statistics-page .top-grid {
    grid-template-columns: 1fr;
  }
}

/* ===== BIEU DO SO SANH DOANH THU ===== */

.statistics-page .revenue-comparison-card {
  margin: 22px 0;
  padding: 24px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 5px 22px rgba(31, 41, 70, 0.05);
}

.statistics-page .revenue-comparison-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 1px solid #e2e8f0;
}

.statistics-page .revenue-comparison-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1e293b;
}

.statistics-page .revenue-comparison-header h3 i {
  margin-right: 8px;
  color: #2563eb;
}

.statistics-page .revenue-comparison-filters {
  display: flex;
  align-items: center;
  gap: 10px;
}

.statistics-page .revenue-comparison-filters select,
.statistics-page .revenue-comparison-filters input {
  height: 38px;
  padding: 8px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 9px;
  background: #ffffff;
  color: #334155;
  font-size: 13px;
  outline: none;
}

.statistics-page .revenue-comparison-filters input {
  width: 105px;
}

.statistics-page .revenue-comparison-filters select:focus,
.statistics-page .revenue-comparison-filters input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.statistics-page .revenue-comparison-chart {
  width: 100%;
  height: 350px;
  margin-top: 22px;
  position: relative;
}

.statistics-page .comparison-note {
  margin: 16px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.statistics-page .comparison-message {
  padding: 40px 16px;
  text-align: center;
  font-size: 14px;
  color: #64748b;
}

@media (max-width: 680px) {
  .statistics-page .revenue-comparison-card {
    padding: 16px;
  }

  .statistics-page .revenue-comparison-filters {
    width: 100%;
    flex-wrap: wrap;
  }

  .statistics-page .revenue-comparison-chart {
    height: 280px;
  }
}

/* ===== CHUYEN CHE DO THONG KE ===== */

.statistics-view-navigation {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
}

.statistics-view-navigation button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 11px 15px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.statistics-view-navigation button:hover {
  background: rgba(255, 255, 255, 0.24);
}

.statistics-view-navigation button.active {
  background: #fff;
  color: #2563eb;
  border-color: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

@media (max-width: 900px) {
  .statistics-view-navigation {
    width: 100%;
    justify-content: flex-start;
  }

  .statistics-view-navigation button {
    flex: 1;
    min-width: 145px;
  }
}

/* ===== GIAO DIEN GUI MAIL TONG KET ===== */

.statistics-page .report-email-section {
  margin-top: 22px;
}

.statistics-page .report-email-card {
  max-width: 900px;
  margin: 0 auto;
  padding: 28px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  box-shadow: 0 5px 22px rgba(31, 41, 70, 0.06);
}

.statistics-page .report-email-header {
  padding-bottom: 20px;
  margin-bottom: 24px;
  border-bottom: 1px solid #e2e8f0;
}

.statistics-page .report-email-heading {
  display: flex;
  align-items: center;
  gap: 16px;
}

.statistics-page .report-email-icon {
  width: 54px;
  height: 54px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eff6ff;
  color: #2563eb;
  font-size: 25px;
  border-radius: 14px;
}

.statistics-page .report-email-heading h3 {
  margin: 0 0 8px;
  color: #1e293b;
  font-size: 20px;
  font-weight: 700;
}

.statistics-page .report-email-heading p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.statistics-page .report-email-form {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.statistics-page .report-email-field {
  display: flex;
  flex-direction: column;
  gap: 9px;
}

.statistics-page .report-email-field label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.statistics-page .report-email-field label i {
  color: #2563eb;
  margin-right: 5px;
}

.statistics-page .report-email-field input {
  width: 100%;
  height: 44px;
  padding: 10px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  background: #ffffff;
  color: #1e293b;
  font-size: 14px;
  outline: none;
}

.statistics-page .report-email-field input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.statistics-page .report-email-date-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.statistics-page .report-email-preview {
  padding: 20px;
  background: #f8fbff;
  border: 1px solid #dbeafe;
  border-radius: 12px;
}

.statistics-page .report-email-preview h4 {
  margin: 0 0 16px;
  font-size: 14px;
  font-weight: 700;
  color: #1e40af;
}

.statistics-page .report-email-preview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.statistics-page .report-email-preview-grid span {
  font-size: 13px;
  color: #475569;
}

.statistics-page .report-email-preview-grid i {
  color: #2563eb;
  margin-right: 7px;
}

.statistics-page .report-email-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 15px;
}

.statistics-page .report-email-note {
  color: #64748b;
  font-size: 12px;
}

.statistics-page .report-email-submit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  padding: 12px 24px;
  border: none;
  border-radius: 10px;
  background: #2563eb;
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.statistics-page .report-email-submit:hover:not(:disabled) {
  background: #1d4ed8;
}

.statistics-page .report-email-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.statistics-page .report-email-alert {
  padding: 13px 16px;
  border-radius: 10px;
  font-size: 13px;
}

.statistics-page .report-email-error {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
}

.statistics-page .report-email-success {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #15803d;
}

@media (max-width: 680px) {
  .statistics-page .report-email-card {
    padding: 18px;
  }

  .statistics-page .report-email-date-grid,
  .statistics-page .report-email-preview-grid {
    grid-template-columns: 1fr;
  }

  .statistics-page .report-email-submit {
    width: 100%;
  }
}

/* ===== HEADER NEN SANG THEO GIAO DIEN THAM KHAO ===== */
.statistics-page .dashboard-hero {
  background: transparent;
  box-shadow: none;
  border: 0;
  border-radius: 0;
  color: #172b4d;
  padding: 0;
  margin-bottom: 20px;
  overflow: visible;
  min-height: 100px;
}

.statistics-page .dashboard-hero::after {
  content: none;
  display: none;
}

.statistics-page .dashboard-eyebrow {
  color: #5a82ad;
  font-size: 12px;
  letter-spacing: 0;
  text-transform: none;
  font-weight: 600;
  margin-bottom: 7px;
}

.statistics-page .dashboard-hero h1 {
  color: #111827;
  font-size: 26px;
  font-weight: 750;
  margin-bottom: 10px;
}

.statistics-page .dashboard-hero p {
  color: #64748b;
  font-size: 14px;
}

.statistics-page .dashboard-hero-mark {
  display: inline-flex;
  background: #eff8ff;
  border: 1px solid #d7e8fa;
  border-radius: 24px;
  color: #5680a8;
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
}

.statistics-page .dashboard-hero-mark i {
  color: #5680a8;
  font-size: 15px;
}

.statistics-page .statistics-view-navigation {
  justify-content: flex-start;
  margin-bottom: 20px;
  padding-bottom: 0;
  gap: 10px;
}

.statistics-page .statistics-view-navigation button {
  background: #ffffff;
  border: 1px solid #dce4ee;
  border-radius: 10px;
  color: #52657a;
  font-size: 13px;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(30, 60, 90, 0.03);
}

.statistics-page .statistics-view-navigation button:hover {
  color: #2563eb;
  border-color: #b7d4f9;
  background: #f5faff;
}

.statistics-page .statistics-view-navigation button.active {
  color: #2563eb;
  background: #edf5ff;
  border-color: #b8d6ff;
  box-shadow: none;
}

@media (max-width: 680px) {
  .statistics-page .dashboard-hero {
    min-height: 0;
    padding: 0;
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .statistics-page .dashboard-hero h1 {
    font-size: 23px;
  }

  .statistics-page .statistics-view-navigation button {
    flex: 1 1 145px;
    min-width: 0;
  }
}
</style>
