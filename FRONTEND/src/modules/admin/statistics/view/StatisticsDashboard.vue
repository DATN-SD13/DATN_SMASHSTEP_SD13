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
  return brandChartData.value.slice().sort(
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

onMounted(() => {
  loadBrands();
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
      <!-- TIÊU ĐỀ THỐNG KÊ -->
      <header class="dashboard-hero">
        <div>
          <div class="dashboard-eyebrow">Báo cáo kinh doanh</div>

          <h1>Thống kê tổng quan</h1>

          <p>Theo dõi doanh thu, hiệu suất bán hàng và hoạt động kinh doanh.</p>
        </div>

        <div class="dashboard-hero-mark">
          <i class="bi bi-bar-chart-line"></i>
          Tổng quan cửa hàng
        </div>
      </header>

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
            <label for="stats-from-date">Từ ngày</label>
            <input id="stats-from-date" v-model="fromDate" type="date" :aria-invalid="!!dateValidationMessage" />
          </div>

          <div class="filter-field">
            <label for="stats-to-date">Đến ngày</label>
            <input id="stats-to-date" v-model="toDate" type="date" :aria-invalid="!!dateValidationMessage" />
          </div>

          <div class="filter-field">
            <label>Nhóm thời gian biểu đồ</label>
            <div class="stats-select"><StatisticsSelect v-model="groupBy" :allow-clear="false" aria-label="Nhóm thời gian biểu đồ">
              <option value="day">Theo ngày</option>
              <option value="month">Theo tháng</option>
              <option value="year">Theo năm</option>
            </StatisticsSelect></div>
          </div>

          <div class="filter-field">
            <label>Thương hiệu biểu đồ</label>
            <div class="stats-select"><StatisticsSelect v-model="selectedBrand" :allow-clear="false" aria-label="Thương hiệu biểu đồ">
              <option value="">Tất cả thương hiệu</option>

              <option
                v-for="brand in brandOptions"
                :key="brand.id"
                :value="brand.id"
              >
                {{ brand.name }}
              </option>
            </StatisticsSelect></div>
          </div>


          <button type="button" class="reset-button" @click="resetFilters">
            <i class="bi bi-arrow-repeat"></i>
            Đặt lại
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

      </section>

      <p v-if="dateValidationMessage" class="date-error" role="alert">
        <i class="bi bi-exclamation-triangle"></i>
        {{ dateValidationMessage }}
      </p>

      <!-- 7 Ô DOANH THU -->
      <section class="summary-section">
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

              <div class="stats-select"><StatisticsSelect v-model="brandChartSort" :allow-clear="false" aria-label="Sắp xếp thương hiệu">
                <option value="revenue">Theo doanh thu</option>
                <option value="quantity">Theo số lượng</option>
              </StatisticsSelect></div>
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
              aria-label="Tìm tên hoặc mã nhân viên"
              placeholder="Tìm tên hoặc mã nhân viên..."
            />

            <div class="stats-select"><StatisticsSelect v-model="employeeSort" :allow-clear="false" aria-label="Sắp xếp nhân viên">
              <option value="revenue">Doanh thu cao nhất</option>
              <option value="totalOrders">Số đơn nhiều nhất</option>
              <option value="productsSold">Sản phẩm bán nhiều nhất</option>
              <option value="name">Tên nhân viên A-Z</option>
            </StatisticsSelect></div>

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

            <div class="stats-select"><StatisticsSelect v-model="productStockFilter" :allow-clear="false" aria-label="Lọc tồn kho">
              <option value="all">Tất cả tồn kho</option>
              <option value="inStock">Còn hàng</option>
              <option value="outOfStock">Hết hàng</option>
            </StatisticsSelect></div>

            <div class="stats-select"><StatisticsSelect v-model="productSort" :allow-clear="false" aria-label="Sắp xếp sản phẩm">
              <option value="sold">Bán nhiều nhất</option>
              <option value="revenue">Doanh thu cao nhất</option>
              <option value="stock">Tồn kho nhiều nhất</option>
              <option value="name">Tên sản phẩm A-Z</option>
            </StatisticsSelect></div>

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
.statistics-page {
  --text: var(--ss-text, #17365f);
  --muted: var(--ss-muted, #718096);
  --border: var(--ss-border, #dde6f0);
  --primary: var(--ss-primary, #2879e3);
  padding: 22px 24px 40px;
  background: var(--ss-bg, #f6f9fd);
  color: var(--text);
  font-family: var(--ss-font, "Inter", "Segoe UI", sans-serif);
  min-width: 0;
}
.statistics-page *, .statistics-page *::before, .statistics-page *::after { box-sizing: border-box; }
.dashboard-hero { display:flex; align-items:center; justify-content:space-between; gap:16px; margin-bottom:20px; }
.dashboard-eyebrow { color:var(--primary); font-size:11px; font-weight:600; margin-bottom:5px; }
.dashboard-hero h1 { color:var(--ss-ink, #0f172a); font-size:22px; font-weight:700; margin:0; line-height:1.35; }
.dashboard-hero p { color:var(--muted); font-size:13px; line-height:1.6; margin:6px 0 0; }
.dashboard-hero-mark { display:flex; align-items:center; gap:8px; color:var(--primary); border:1px solid #d2e5fc; background:var(--ss-primary-soft, #eaf4ff); border-radius:999px; padding:9px 14px; font-size:12px; white-space:nowrap; }
.dashboard-hero-mark i { font-size:16px; }
.filter-card,.chart-card,.period-card,.order-status-card,.employee-card,.stat-card { min-width:0; background:#fff; border:1px solid var(--border); border-radius:14px; box-shadow:0 3px 12px rgba(23,54,95,.025); }
.filter-card,.chart-card,.period-card,.order-status-card,.employee-card { padding:20px; }
.filter-header,.chart-header,.period-header,.employee-header { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px; padding-bottom:16px; margin-bottom:16px; border-bottom:1px solid var(--ss-line, #eaf0f6); }
.filter-header h3,.chart-header h3,.period-header h3,.employee-header h3 { display:flex; align-items:center; gap:10px; font-size:15px; font-weight:600; color:var(--text); line-height:1.4; margin:0; }
.filter-header h3 i,.chart-header h3 i,.period-header h3 i,.employee-header h3 i { display:grid; place-items:center; flex:0 0 32px; width:32px; height:32px; border-radius:8px; background:var(--ss-primary-soft, #eaf4ff); color:var(--primary); font-size:16px; }
.quick-filters,.chart-right,.chart-actions { display:flex; align-items:center; flex-wrap:wrap; gap:6px; }
.statistics-page button { font-family:inherit; font-size:12px; font-weight:600; cursor:pointer; transition:background-color .15s,border-color .15s; }
.quick-filters button,.reset-button,.employee-reset-btn,.employee-page-buttons button { min-height:38px; border:1px solid var(--border); border-radius:8px; padding:0 13px; background:#fff; color:var(--text); }
.quick-filters button:hover,.reset-button:hover,.employee-reset-btn:hover,.employee-page-buttons button:hover:not(:disabled) { color:var(--primary); background:var(--ss-primary-soft, #eaf4ff); border-color:#a7cdfa; }
.statistics-page button:focus-visible { outline:2px solid var(--primary); outline-offset:3px; }
.statistics-page button:disabled { opacity:.45; cursor:not-allowed; }
.filter-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)) auto; gap:14px; align-items:end; }
.filter-field { display:flex; flex-direction:column; gap:7px; min-width:0; }
.filter-field label { color:var(--muted); font-size:12px; font-weight:500; }
.filter-field input,.employee-filters input { width:100%; min-width:0; height:42px; padding:0 12px; background:var(--ss-surface, #f8fafc); color:var(--text); border:1px solid var(--border); border-radius:8px; font:inherit; font-size:13px; outline:none; }
.filter-field input:focus,.employee-filters input:focus { border-color:var(--primary); background:#fff; box-shadow:0 0 0 3px rgba(40,121,227,.1); }
.employee-filters input::placeholder { color:var(--ss-faint, #9aa8ba); }
.reset-button,.employee-reset-btn { height:42px; white-space:nowrap; }
.reset-button i,.employee-reset-btn i { margin-right:5px; }
.stats-select { min-width:0; width:100%; }
.stats-select :deep(.select2-container) { width:100% !important; min-width:0; font-family:inherit; font-size:13px; }
.stats-select :deep(.select2-selection--single) { height:42px; min-height:42px; border:1px solid var(--border); border-radius:8px; background:var(--ss-surface, #f8fafc); }
.stats-select :deep(.select2-selection--single .select2-selection__rendered) { display:block; line-height:40px; padding:0 32px 0 12px; color:var(--text) !important; text-overflow:ellipsis; overflow:hidden; white-space:nowrap; }
.stats-select :deep(.select2-selection--single .select2-selection__arrow) { height:40px; right:8px; }
.stats-select :deep(.select2-container--open .select2-selection),.stats-select :deep(.select2-container--focus .select2-selection) { border-color:var(--primary); box-shadow:0 0 0 3px rgba(40,121,227,.1); }
.information-bar { display:flex; align-items:center; padding:12px 16px; margin:16px 0; color:var(--muted); background:#eef5fe; border:1px solid #dceafb; border-radius:10px; font-size:12px; }
.information-left { display:flex; align-items:center; gap:10px; line-height:1.6; }
.information-left strong { color:var(--text); font-weight:600; }
.blue-dot { width:7px; height:7px; flex:0 0 7px; border-radius:50%; background:var(--primary); box-shadow:0 0 0 4px #dceafb; }
.date-error { margin:12px 0; padding:12px 16px; border:1px solid #ffd1d7; color:#b8262b; background:#fff3f4; border-radius:8px; font-size:13px; }
.summary-section { display:flex; flex-direction:column; gap:14px; }
.summary-grid { display:grid; gap:14px; }
.top-grid { grid-template-columns:repeat(3,minmax(0,1fr)); }
.bottom-grid { grid-template-columns:repeat(4,minmax(0,1fr)); }
.stat-card { display:grid; grid-template-columns:42px minmax(0,1fr); align-content:center; column-gap:14px; row-gap:5px; padding:20px; min-height:112px; }
.stat-icon { grid-column:1; grid-row:1 / 3; align-self:center; width:42px; height:42px; border-radius:10px; display:grid; place-items:center; font-size:20px; color:var(--primary); background:var(--ss-primary-soft, #eaf4ff); }
.stat-card strong { grid-column:2; font-size:clamp(18px,1.55vw,26px); color:var(--ss-ink, #0f172a); font-weight:700; line-height:1.3; overflow-wrap:anywhere; font-variant-numeric:tabular-nums; }
.stat-card > span { grid-column:2; color:var(--muted); font-size:12px; line-height:1.5; }
.top-grid .stat-card:first-child { background:#f1f7ff; border-color:#bdd9fb; }
.top-grid .stat-card:first-child strong { color:var(--primary); }
.green { color:#148c4d; background:#edfcf2; }
.purple { color:#7754b6; background:#f2edfc; }
.yellow { color:#b77b16; background:#fff6e5; }
.red { color:#bf5268; background:#fff0f3; }
.charts-grid,.period-section { display:grid; grid-template-columns:minmax(0,1.5fr) minmax(0,1fr); gap:16px; margin-top:18px; align-items:stretch; }
.chart-right { gap:10px; }
.chart-right .stats-select { width:165px; }
.chart-actions { padding:3px; background:var(--ss-surface, #f8fafc); border:1px solid var(--border); border-radius:8px; gap:3px; }
.chart-actions button { padding:7px 12px; color:var(--muted); border:0; background:transparent; border-radius:5px; }
.chart-actions button.active { background:var(--ss-primary-soft, #eaf4ff); color:var(--primary); }
.chart-content { min-height:335px; display:flex; align-items:center; justify-content:center; }
.chart-canvas { height:320px; width:100%; position:relative; }
.chart-canvas canvas { height:100% !important; width:100% !important; }
.brand-chart-content { flex-direction:column; gap:16px; align-items:stretch; }
.brand-chart-canvas { width:100%; height:220px; position:relative; }
.chart-empty,.period-message { padding:28px 12px; width:100%; text-align:center; color:var(--muted); font-size:13px; }
.period-error { color:#b8262b; }
.brand-list,.order-status-list { display:flex; flex-direction:column; gap:8px; }
.brand-list-item,.order-status-item { display:flex; align-items:center; justify-content:space-between; gap:10px; padding:10px 12px; background:var(--ss-surface, #f8fafc); border:1px solid var(--ss-line, #eaf0f6); border-radius:8px; font-size:12px; min-width:0; }
.brand-name,.order-status-name { display:flex; align-items:center; gap:8px; min-width:0; line-height:1.5; }
.brand-name > span:last-child { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.brand-dot,.order-status-dot { flex:0 0 8px; width:8px; height:8px; border-radius:50%; }
.brand-list-item strong,.order-status-item strong { font-weight:600; white-space:nowrap; font-variant-numeric:tabular-nums; }
.order-status-total { text-align:center; font-size:12px; color:var(--muted); margin:0 0 14px; }
.order-status-total strong { color:var(--text); font-weight:700; font-size:20px; margin-left:6px; }
.order-status-canvas { width:100%; height:210px; position:relative; margin:0 auto 18px; }
.order-status-list { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); }
.period-table-wrapper,.employee-table-wrapper { overflow-x:auto; width:100%; border:1px solid var(--border); border-radius:9px; }
.period-table,.employee-table { width:100%; border-collapse:collapse; font-size:13px; white-space:nowrap; }
.period-table th,.employee-table th { background:#f5f8fc; color:var(--muted); text-align:left; font-size:12px; font-weight:600; padding:13px 14px; border-bottom:1px solid var(--border); }
.period-table td,.employee-table td { padding:15px 14px; color:var(--text); border-bottom:1px solid var(--ss-line, #eaf0f6); }
.period-table tr:last-child td,.employee-table tr:last-child td { border-bottom:0; }
.period-table tbody tr:hover,.employee-table tbody tr:hover { background:#fafcff; }
.period-name,.employee-name { font-weight:600; }
.growth-positive,.growth-negative { display:inline-block; padding:4px 8px; border-radius:6px; font-size:12px; font-weight:600; }
.growth-positive { color:#148c4d; background:#edfcf2; }
.growth-negative { color:#b8262b; background:#ffeded; }
.employee-section { margin-top:18px; }
.employee-filters { display:flex; align-items:center; gap:12px; flex-wrap:wrap; margin-bottom:16px; }
.employee-filters input { flex:1 1 220px; }
.employee-filters .stats-select { flex:0 1 210px; }
.employee-export-btn,.period-export-btn { display:inline-flex; align-items:center; justify-content:center; gap:7px; min-height:38px; padding:0 13px; color:var(--primary); background:#fff; border:1px solid #cfe0f7; border-radius:8px; white-space:nowrap; }
.employee-export-btn:hover,.period-export-btn:hover { background:var(--ss-primary-soft, #eaf4ff); border-color:var(--primary); }
.employee-revenue { color:#148c4d !important; font-weight:600; }
.employee-pagination { display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap; gap:12px; padding-top:16px; font-size:12px; color:var(--muted); }
.employee-page-buttons { display:flex; align-items:center; gap:12px; }
.summary-message { display:flex; align-items:center; justify-content:center; gap:10px; padding:25px; border:1px solid var(--border); border-radius:12px; color:var(--muted); background:#fff; font-size:13px; }
.summary-message-error { background:#fff3f4; border-color:#ffd1d7; color:#b8262b; }
.product-revenue-note { margin:0 0 16px; padding:11px 13px; background:#f2f7fe; border:1px solid #dceafb; border-radius:8px; color:var(--text); font-size:12px; line-height:1.65; }
.product-revenue-note i { color:var(--primary); margin-right:6px; }
@media (max-width:1280px) {
  .filter-grid { grid-template-columns:repeat(2,minmax(0,1fr)); }
  .reset-button { justify-self:start; }
  .bottom-grid { grid-template-columns:repeat(2,minmax(0,1fr)); }
  .charts-grid,.period-section { grid-template-columns:1fr; }
}
@media (max-width:820px) {
  .statistics-page { padding:18px 16px 32px; }
  .dashboard-hero-mark { display:none; }
  .top-grid { grid-template-columns:1fr; }
  .stat-card strong { font-size:23px; }
}
@media (max-width:540px) {
  .statistics-page { padding:16px 12px 28px; }
  .dashboard-hero h1 { font-size:20px; }
  .filter-card,.chart-card,.period-card,.order-status-card,.employee-card { padding:16px; }
  .filter-grid,.bottom-grid { grid-template-columns:1fr; }
  .reset-button { justify-self:stretch; }
  .quick-filters { width:100%; }
  .quick-filters button { flex:1 1 auto; }
  .employee-filters input,.employee-filters .stats-select { flex:1 1 100%; width:100%; }
  .employee-reset-btn { width:100%; }
  .chart-right { width:100%; justify-content:space-between; }
  .order-status-list { grid-template-columns:1fr; }
  .chart-canvas { height:260px; }
  .employee-pagination { align-items:flex-start; flex-direction:column; }
}
@media (prefers-reduced-motion:reduce) { .statistics-page button { transition:none; } }
</style>
