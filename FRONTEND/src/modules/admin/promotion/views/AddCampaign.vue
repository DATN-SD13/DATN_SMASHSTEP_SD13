<script setup>
import AdminLayout from "../../../../layouts/AdminLayout.vue";
import Select2Input from "../components/Select2Input.vue";
import { computed, reactive, ref, onMounted } from "vue";
import { useRouter, onBeforeRouteLeave } from "vue-router";
import api from "../../../../utils/api";
import { showSuccess, showError } from "../../../../utils/feedback";

const router = useRouter();

// =====================================================
// FORM ĐỢT GIẢM GIÁ
// =====================================================
const form = ref({
  name: "",
  value: 0,
  start: "",
  end: "",
  desc: "",
});

// =====================================================
// DỮ LIỆU SẢN PHẨM THẬT TỪ BACKEND
// =====================================================
const products = ref([]);
const loadingProducts = ref(false);
const loadProductsError = ref(false);
const saving = ref(false);
const descriptionSupported = ref(false);

// Lấy sản phẩm + biến thể từ backend

async function loadProducts() {
  try {
    loadingProducts.value = true;
    loadProductsError.value = false;

    // Kiểm tra backend có hỗ trợ mô tả hay không
    const capabilities = await api.get("/dot-giam-gia/capabilities");

    descriptionSupported.value =
      capabilities.data?.data?.descriptionSupported === true;

    // Lấy danh sách biến thể sản phẩm
    const response = await api.get("/dot-giam-gia/san-pham-chi-tiet");

    const data = response.data?.data || [];

    // Gom các biến thể theo sản phẩm
    const productMap = new Map();

    data.forEach((item) => {
      const productCode = item.productCode || "";
      const productName = item.productName || "";

      if (!productMap.has(productCode)) {
        productMap.set(productCode, {
          code: productCode,
          name: productName,
          variants: [],
        });
      }

      productMap.get(productCode).variants.push({
        id: item.id,
        code: item.productDetailCode || item.sku || `CTSP-${item.id}`,
        sku: item.sku || "",
        color: item.color || "",
        colorHex: item.colorHex || "",
        size: item.size || "",
        price: Number(item.price || 0),
        qty: Number(item.quantity || 0),
        status: item.status,
      });
    });

    products.value = Array.from(productMap.values());
  } catch (error) {
    console.error("Lỗi tải sản phẩm:", error);

    products.value = [];
    loadProductsError.value = true;

    showError(
      error.response?.data?.message || "Không thể tải danh sách sản phẩm",
    );
  } finally {
    loadingProducts.value = false;
  }
}

// =====================================================
// TẤT CẢ BIẾN THỂ
// =====================================================
const allVariants = computed(() =>
  products.value.flatMap((p) =>
    p.variants.map((x) => ({
      ...x,
      product: p.name,
      productCode: p.code,
    })),
  ),
);

// =====================================================
// DANH SÁCH MÀU
// =====================================================
const colors = computed(() => [
  ...new Set(allVariants.value.map((x) => x.color).filter(Boolean)),
]);

// =====================================================
// DANH SÁCH SIZE
// =====================================================
const sizes = computed(() =>
  [...new Set(allVariants.value.map((x) => x.size).filter(Boolean))].sort(),
);
const colorOptions = computed(() => [
  { value: "all", label: "Tất cả màu sắc" },
  ...colors.value.map((c) => ({
    value: c,
    label: c,
  })),
]);

const sizeOptions = computed(() => [
  { value: "all", label: "Tất cả kích cỡ" },
  ...sizes.value.map((s) => ({
    value: s,
    label: String(s),
  })),
]);
// =====================================================
// BỘ LỌC CHỌN SẢN PHẨM
// =====================================================
const draft = reactive({
  q: "",
  color: "all",
  size: "all",
});

// Đặt lại toàn bộ bộ lọc
function resetSearch() {
  Object.assign(draft, {
    q: "",
    color: "all",
    size: "all",
  });
}

// =====================================================
// SẢN PHẨM SAU KHI LỌC
// =====================================================

const filteredProducts = computed(() => {
  const q = draft.q.trim().toLowerCase();

  return products.value
    .map((p) => {
      const matchKeyword =
        !q ||
        p.code.toLowerCase().includes(q) ||
        p.name.toLowerCase().includes(q);

      const matchingVariants = p.variants.filter(
        (x) =>
          (draft.color === "all" || x.color === draft.color) &&
          (draft.size === "all" || String(x.size) === String(draft.size)),
      );

      return {
        ...p,
        variants: matchKeyword ? matchingVariants : [],
      };
    })
    .filter((p) => p.variants.length > 0);
});

// =====================================================
// BIẾN THỂ ĐÃ CHỌN
// Lưu ID thật của san_pham_chi_tiet
// =====================================================
const selected = ref([]);
const hasUnsavedChanges = computed(() => {
  return (
    form.value.name.trim() !== "" ||
    Number(form.value.value) !== 0 ||
    form.value.start !== "" ||
    form.value.end !== "" ||
    form.value.desc.trim() !== "" ||
    selected.value.length > 0
  );
});

const allowLeave = ref(false);
const isChosen = (p) => p.variants.some((x) => selected.value.includes(x.id));

const isFull = (p) =>
  p.variants.length > 0 &&
  p.variants.every((x) => selected.value.includes(x.id));

// Kiểm tra sản phẩm được chọn một phần biến thể
function isPartiallySelected(p) {
  const selectedCount = p.variants.filter((x) =>
    selected.value.includes(x.id),
  ).length;

  return selectedCount > 0 && selectedCount < p.variants.length;
}

// Hiển thị dấu gạch ngang khi chọn một phần
const vIndeterminate = {
  mounted(el, binding) {
    el.indeterminate = binding.value;
  },
  updated(el, binding) {
    el.indeterminate = binding.value;
  },
};

// Chọn / bỏ chọn một sản phẩm
function toggleProduct(p) {
  const ids = p.variants.map((x) => x.id);

  if (isFull(p)) {
    selected.value = selected.value.filter((id) => !ids.includes(id));
  } else {
    selected.value = [...new Set([...selected.value, ...ids])];
  }
}

// =====================================================
// CHỌN TẤT CẢ
// =====================================================
const allChecked = computed(
  () =>
    filteredProducts.value.length > 0 && filteredProducts.value.every(isFull),
);

function toggleAll() {
  const ids = filteredProducts.value.flatMap((p) =>
    p.variants.map((x) => x.id),
  );

  if (allChecked.value) {
    selected.value = selected.value.filter((id) => !ids.includes(id));
  } else {
    selected.value = [...new Set([...selected.value, ...ids])];
  }
}

// =====================================================
// XÓA BIẾN THỂ KHỎI DANH SÁCH CHỌN
// =====================================================
function removeVariant(id) {
  selected.value = selected.value.filter((x) => x !== id);
}

// =====================================================
// DANH SÁCH BIẾN THỂ ĐÃ CHỌN
// =====================================================
const listFilter = reactive({
  q: "",
  color: "all",
  size: "all",
});

const chosenRows = computed(() =>
  allVariants.value.filter((x) => selected.value.includes(x.id)),
);

const shownRows = computed(() => {
  const q = listFilter.q.trim().toLowerCase();

  return chosenRows.value.filter(
    (x) =>
      (!q ||
        x.code.toLowerCase().includes(q) ||
        x.product.toLowerCase().includes(q)) &&
      (listFilter.color === "all" || x.color === listFilter.color) &&
      (listFilter.size === "all" || String(x.size) === String(listFilter.size)),
  );
});

// =====================================================
// TÍNH GIÁ SAU GIẢM
// =====================================================
const afterDiscount = (price) => {
  const discount = Math.min(100, Math.max(0, Number(form.value.value) || 0));

  return Math.round(price * (1 - discount / 100));
};

// =====================================================
// FORMAT TIỀN
// =====================================================
const money = (n) => `${Number(n || 0).toLocaleString("vi-VN")} đ`;

// =====================================================
// TẠO ĐỢT GIẢM GIÁ
// Bước này CHƯA gọi POST.
// Test sản phẩm trước.
// =====================================================
async function save() {
  if (saving.value || loadingProducts.value) return;
  // Kiểm tra dữ liệu

  if (!form.value.name.trim()) {
    showError("Vui lòng nhập tên đợt giảm giá");
    return;
  }

  const discountValue = Number(form.value.value);

  if (
    !Number.isFinite(discountValue) ||
    discountValue <= 0 ||
    discountValue > 100
  ) {
    showError("Giá trị giảm phải lớn hơn 0 và không vượt quá 100%");
    return;
  }

  // Chỉ cho phép tối đa 2 chữ số sau dấu thập phân
  const decimalPart = String(form.value.value).split(".")[1];

  if (decimalPart && decimalPart.length > 2) {
    showError("Phần trăm giảm chỉ được tối đa 2 chữ số thập phân");
    return;
  }

  if (!form.value.start) {
    showError("Vui lòng chọn ngày bắt đầu");
    return;
  }

  if (!form.value.end) {
    showError("Vui lòng chọn ngày kết thúc");
    return;
  }

  if (form.value.end < form.value.start) {
    showError("Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu");
    return;
  }

  if (selected.value.length === 0) {
    showError("Vui lòng chọn ít nhất một biến thể sản phẩm");
    return;
  }

  const data = {
    name: form.value.name.trim(),
    discountValue: discountValue,
    startDate: form.value.start,
    endDate: form.value.end,
    description: descriptionSupported.value ? form.value.desc.trim() : null,
    productDetailIds: selected.value,
  };

  try {
    saving.value = true;
    await api.post("/dot-giam-gia", data);
    showSuccess("Tạo đợt giảm giá thành công!");

    allowLeave.value = true;
    router.push("/dot-giam-gia");
  } catch (error) {
    console.error("Lỗi tạo đợt giảm giá:", error);

    showError(error.response?.data?.message || "Không thể tạo đợt giảm giá");
  } finally {
    saving.value = false;
  }
}

// =====================================================
// LOAD KHI MỞ TRANG
// =====================================================
onMounted(() => {
  loadProducts();
});

onBeforeRouteLeave(() => {
  if (allowLeave.value || !hasUnsavedChanges.value) {
    return true;
  }

  return window.confirm(
    "Bạn có thông tin chưa lưu. Bạn có chắc chắn muốn rời khỏi trang không?",
  );
});
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <div class="top-grid">
        <!-- ========================= -->
        <!-- THÔNG TIN ĐỢT GIẢM -->
        <!-- ========================= -->

        <section class="ss-card ss-form">
          <div class="ss-head">
            <div class="ss-head-icon">
              <i class="bi bi-tag"></i>
            </div>

            <h2>Thông tin đợt giảm</h2>
          </div>

          <div class="ss-field">
            <label class="ss-label">
              Tên đợt
              <span class="req">*</span>
            </label>

            <input
              class="ss-input"
              v-model="form.name"
              placeholder="Ví dụ: Siêu giảm giá mùa hè"
            />
          </div>

          <div class="ss-field">
            <label class="ss-label">
              Giá trị giảm (%)
              <span class="req">*</span>
            </label>

            <input
              class="ss-input"
              type="number"
              min="0.01"
              max="100"
              step="0.01"
              v-model="form.value"
            />
          </div>

          <div class="two">
            <div class="ss-field">
              <label class="ss-label">
                Từ ngày
                <span class="req">*</span>
              </label>

              <input class="ss-input" type="date" v-model="form.start" />
            </div>

            <div class="ss-field">
              <label class="ss-label">
                Đến ngày
                <span class="req">*</span>
              </label>

              <input class="ss-input" type="date" v-model="form.end" />
            </div>
          </div>

          <div v-if="descriptionSupported" class="ss-field">
            <label class="ss-label"> Mô tả </label>

            <textarea
              class="ss-textarea"
              v-model="form.desc"
              placeholder="Nhập mô tả..."
            ></textarea>
          </div>

          <div class="ss-summary">
            <h3 class="ss-summary-title">
              <i class="bi bi-clipboard-check"></i>
              Tổng kết đợt giảm giá
            </h3>

            <div class="ss-summary-row">
              <span>Mức giảm:</span>
              <strong>{{ form.value || 0 }}%</strong>
            </div>

            <div class="ss-summary-row">
              <span>Số biến thể:</span>
              <strong>{{ selected.length }}</strong>
            </div>

            <div class="ss-summary-row">
              <span>Ngày bắt đầu:</span>
              <strong>{{ form.start || "Chưa chọn" }}</strong>
            </div>

            <div class="ss-summary-row">
              <span>Ngày kết thúc:</span>
              <strong>{{ form.end || "Chưa chọn" }}</strong>
            </div>
          </div>

          <button
            type="button"
            class="ss-btn primary block"
            @click="save"
            :disabled="saving || loadingProducts"
          >
            <i
              class="bi"
              :class="saving ? 'bi-arrow-repeat spin' : 'bi-check2'"
            ></i>

            {{ saving ? "Đang tạo đợt giảm giá..." : "Tạo đợt giảm giá" }}
          </button>

          <button
            type="button"
            class="ss-btn block"
            :disabled="saving"
            @click="router.back()"
          >
            Hủy
          </button>
        </section>

        <!-- ========================= -->
        <!-- CHỌN SẢN PHẨM -->
        <!-- ========================= -->

        <section class="ss-card">
          <div class="ss-head">
            <div class="ss-head-icon">
              <i class="bi bi-search"></i>
            </div>

            <div>
              <h2>Chọn sản phẩm áp dụng</h2>

              <p>
                Đã chọn
                {{ selected.length }}
                biến thể
              </p>
            </div>
          </div>

          <div class="pick-filter">
            <!-- Hàng 1: Tìm kiếm theo tên hoặc mã -->
            <div class="pick-search-row">
              <label class="ss-label strong"> Tìm kiếm sản phẩm </label>

              <div class="ss-search">
                <i class="bi bi-search"></i>

                <input
                  class="ss-input"
                  v-model="draft.q"
                  placeholder="Nhập mã hoặc tên sản phẩm..."
                />
              </div>
            </div>

            <!-- Hàng 2: Lọc màu, kích cỡ và nút thao tác -->
            <div class="pick-filter-row">
              <div class="ss-field">
                <span class="ss-label strong">Màu sắc</span>

                <Select2Input
                  v-model="draft.color"
                  :options="colorOptions"
                  placeholder="Chọn màu sắc"
                />
              </div>

              <div class="ss-field">
                <span class="ss-label strong">Kích cỡ</span>

                <Select2Input
                  v-model="draft.size"
                  :options="sizeOptions"
                  placeholder="Chọn kích cỡ"
                />
              </div>

              <button
                type="button"
                class="ss-btn pick-reset-btn"
                @click="resetSearch"
              >
                <i class="bi bi-arrow-counterclockwise"></i>
                Đặt lại
              </button>
            </div>

            <!-- Số lượng sản phẩm tìm được -->
            <div class="pick-result">
              <i class="bi bi-list-check"></i>
              Tìm thấy
              <strong>{{ filteredProducts.length }}</strong>
              sản phẩm
            </div>
          </div>

          <div v-if="loadingProducts" class="ss-empty">
            Đang tải sản phẩm...
          </div>

          <div v-else-if="loadProductsError" class="ss-empty">
            <p>Không thể tải danh sách sản phẩm.</p>

            <button type="button" class="ss-btn primary" @click="loadProducts">
              <i class="bi bi-arrow-clockwise"></i>
              Thử tải lại
            </button>
          </div>

          <div v-else class="ss-table-wrap">
            <table class="ss-table" style="min-width: 520px">
              <thead>
                <tr>
                  <th style="width: 44px" class="c">
                    <input
                      type="checkbox"
                      :checked="allChecked"
                      @change="toggleAll"
                    />
                  </th>

                  <th class="w-stt c">STT</th>

                  <th>Mã SP</th>

                  <th>Tên sản phẩm</th>
                </tr>
              </thead>

              <tbody>
                <tr v-for="(p, i) in filteredProducts" :key="p.code">
                  <td class="c">
                    <input
                      type="checkbox"
                      :checked="isFull(p)"
                      v-indeterminate="isPartiallySelected(p)"
                      @change="toggleProduct(p)"
                    />
                  </td>

                  <td class="c">
                    {{ i + 1 }}
                  </td>

                  <td>
                    {{ p.code }}
                  </td>

                  <td>
                    {{ p.name }}
                  </td>
                </tr>

                <tr v-if="!filteredProducts.length">
                  <td colspan="4" class="ss-empty">
                    <i class="bi bi-inbox"></i>

                    Không tìm thấy sản phẩm.
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>

      <!-- ========================= -->
      <!-- BIẾN THỂ ĐÃ CHỌN -->
      <!-- ========================= -->

      <section class="ss-card">
        <div class="ss-head">
          <div class="ss-head-icon">
            <i class="bi bi-check2-square"></i>
          </div>

          <div>
            <h2>Sản phẩm & biến thể đã chọn áp dụng</h2>

            <p>
              Danh sách chi tiết gồm
              {{ chosenRows.length }}
              biến thể đã chọn
            </p>
          </div>
        </div>

        <div class="list-filter">
          <Select2Input
            v-model="listFilter.color"
            :options="colorOptions"
            placeholder="Chọn màu sắc"
          />

          <Select2Input
            v-model="listFilter.size"
            :options="sizeOptions"
            placeholder="Chọn kích cỡ"
          />

          <input
            class="ss-input"
            v-model="listFilter.q"
            placeholder="Tìm trong danh sách..."
          />
        </div>

        <div class="ss-table-wrap">
          <table class="ss-table">
            <thead>
              <tr>
                <th class="w-stt c">STT</th>

                <th>Sản phẩm</th>

                <th>Biến thể</th>

                <th>Giá bán</th>

                <th>Giá sau giảm</th>

                <th class="r">Số lượng</th>

                <th class="c" style="width: 60px">Xóa</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="(x, i) in shownRows" :key="x.id">
                <td class="c">
                  {{ i + 1 }}
                </td>

                <td>
                  <span class="ss-strong">
                    {{ x.product }}
                  </span>

                  <span class="ss-sub">
                    {{ x.productCode }}
                  </span>
                </td>

                <td>
                  {{ x.code }}

                  <span class="ss-sub"> {{ x.color }} · {{ x.size }} </span>
                </td>

                <td class="nowrap">
                  {{ money(x.price) }}
                </td>

                <td class="nowrap">
                  <span class="ss-code">
                    {{ money(afterDiscount(x.price)) }}
                  </span>
                </td>

                <td class="r">
                  {{ x.qty }}
                </td>

                <td class="c">
                  <button
                    type="button"
                    class="ss-icon-btn danger"
                    title="Bỏ chọn biến thể"
                    aria-label="Bỏ chọn biến thể"
                    :disabled="saving"
                    @click="removeVariant(x.id)"
                  >
                    <i class="bi bi-x-lg"></i>
                  </button>
                </td>
              </tr>

              <tr v-if="!shownRows.length">
                <td colspan="7" class="ss-empty">
                  <i class="bi bi-inbox"></i>

                  Chưa có biến thể nào được chọn.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  </AdminLayout>
</template>

<style scoped>
.top-grid {
  display: grid;
  grid-template-columns: 365px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.two {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

/* ============================= */
/* BỘ LỌC TÌM KIẾM SẢN PHẨM */
/* ============================= */

.pick-filter {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

/* Hàng tìm kiếm */
.pick-search-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.pick-search-row .ss-search {
  width: 100%;
}

/* Hàng lọc màu, kích cỡ và nút */
.pick-filter-row {
  display: grid;
  grid-template-columns:
    minmax(0, 1fr)
    minmax(0, 1fr)
    auto
    auto;
  gap: 10px;
  align-items: end;
}

.pick-filter-row .ss-field {
  min-width: 0;
}

/* Nút Đặt lại và Tìm kiếm */
.pick-reset-btn,
.pick-search-btn {
  height: 40px;
  white-space: nowrap;
}

.pick-reset-btn {
  background: #fff;
  border: 1px solid #dbe4f0;
  color: #475569;
}

.pick-reset-btn:hover {
  background: #f1f5f9;
}

/* Hiển thị số sản phẩm tìm thấy */
.pick-result {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: #64748b;
}

.pick-result strong {
  color: #2563eb;
  font-weight: 700;
}

/* ============================= */
/* BỘ LỌC BẢNG BIẾN THỂ ĐÃ CHỌN */
/* ============================= */

.list-filter {
  display: grid;
  grid-template-columns: 170px 170px 240px;
  justify-content: end;
  gap: 10px;
}

.ss-icon-btn.chosen {
  background: var(--ss-primary);
  border-color: var(--ss-primary);
  color: #fff;
}

input[type="checkbox"] {
  width: 15px;
  height: 15px;
  accent-color: var(--ss-primary);
  cursor: pointer;
}

/* ============================= */
/* RESPONSIVE */
/* ============================= */

@media (max-width: 1100px) {
  .top-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .pick-filter-row {
    grid-template-columns: 1fr 1fr;
  }

  .list-filter {
    grid-template-columns: 1fr;
  }

  .pick-reset-btn,
  .pick-search-btn {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .pick-filter-row {
    grid-template-columns: 1fr;
  }
}

/* ============================= */
/* HIỆU ỨNG NÚT LƯU */
/* ============================= */

.spin {
  display: inline-block;
  animation: loading-spin 1s linear infinite;
}

@keyframes loading-spin {
  to {
    transform: rotate(360deg);
  }
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

/* ============================= */
/* TỔNG KẾT ĐỢT GIẢM GIÁ */
/* ============================= */

.ss-summary {
  padding: 14px;
  margin: 16px 0;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

.ss-summary-title {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 700;
}

.ss-summary-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
  font-size: 13px;
}

.ss-summary-row strong {
  text-align: right;
}
</style>
