<script setup>
import AdminLayout from "../../../../layouts/AdminLayout.vue";
import Select2Input from "../components/Select2Input.vue";
import { computed, reactive, ref, onMounted, watch } from "vue";
import { useRoute, useRouter, onBeforeRouteLeave } from "vue-router";
import api from "../../../../utils/api";
import {
  confirmAction,
  showSuccess,
  showError,
} from "../../../../utils/feedback";

const route = useRoute();
const router = useRouter();

// ======================================
// TRẠNG THÁI
// ======================================
const loading = ref(true);
const loadingProducts = ref(false);
const saving = ref(false);
const descriptionSupported = ref(false);

// ======================================
// FORM
// ======================================
const form = ref({
  code: "",
  name: "",
  value: 0,
  start: "",
  end: "",
  desc: "",
});

// ======================================
// DANH SÁCH SẢN PHẨM
// ======================================
const products = ref([]);

async function loadProducts() {
  try {
    loadingProducts.value = true;

    const response = await api.get("/dot-giam-gia/san-pham-chi-tiet");

    const data = response.data?.data || [];
    const productMap = new Map();

    data.forEach((item) => {
      const productCode = item.productCode || "";

      if (!productMap.has(productCode)) {
        productMap.set(productCode, {
          code: productCode,
          name: item.productName || "",
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

    products.value = [...productMap.values()];
  } catch (error) {
    console.error("Lỗi tải sản phẩm:", error);

    showError(
      error.response?.data?.message || "Không thể tải danh sách sản phẩm",
    );
  } finally {
    loadingProducts.value = false;
  }
}

// ======================================
// BIẾN THỂ & BỘ LỌC
// ======================================
const allVariants = computed(() =>
  products.value.flatMap((p) =>
    p.variants.map((x) => ({
      ...x,
      product: p.name,
      productCode: p.code,
    })),
  ),
);

const colors = computed(() => [
  ...new Set(allVariants.value.map((x) => x.color).filter(Boolean)),
]);

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
const draft = reactive({
  q: "",
  color: "all",
  size: "all",
});

const filteredProducts = computed(() => {
  const q = draft.q.trim().toLowerCase();

  return products.value
    .map((p) => {
      const matchKeyword =
        !q ||
        p.code.toLowerCase().includes(q) ||
        p.name.toLowerCase().includes(q);

      const variants = p.variants.filter(
        (x) =>
          (draft.color === "all" || x.color === draft.color) &&
          (draft.size === "all" || x.size === draft.size),
      );

      return {
        ...p,
        variants: matchKeyword ? variants : [],
      };
    })
    .filter((p) => p.variants.length > 0);
});

// ======================================
// CHỌN SẢN PHẨM
// ======================================
const selected = ref([]);

const originalData = ref("");
const hasUnsavedChanges = ref(false);

function getCurrentData() {
  return JSON.stringify({
    name: form.value.name,
    value: Number(form.value.value),
    start: form.value.start,
    end: form.value.end,
    desc: descriptionSupported.value ? form.value.desc : "",
    productDetailIds: [...selected.value].sort((a, b) => a - b),
  });
}

watch(
  [form, selected],
  () => {
    if (!loading.value && originalData.value) {
      hasUnsavedChanges.value = getCurrentData() !== originalData.value;
    }
  },
  { deep: true },
);

const missingProductIds = computed(() => {
  const availableIds = new Set(allVariants.value.map((x) => x.id));

  return selected.value.filter((id) => !availableIds.has(id));
});
const isChosen = (p) => p.variants.some((x) => selected.value.includes(x.id));

const isFull = (p) =>
  p.variants.length > 0 &&
  p.variants.every((x) => selected.value.includes(x.id));

// Kiểm tra sản phẩm có được chọn một phần biến thể không
function isPartiallySelected(p) {
  const selectedCount = p.variants.filter((x) =>
    selected.value.includes(x.id),
  ).length;

  return selectedCount > 0 && selectedCount < p.variants.length;
}

// Hiển thị dấu gạch ngang khi checkbox chọn một phần
const vIndeterminate = {
  mounted(el, binding) {
    el.indeterminate = binding.value;
  },

  updated(el, binding) {
    el.indeterminate = binding.value;
  },
};

// Chọn hoặc bỏ chọn biến thể của sản phẩm
function toggleProduct(p) {
  const ids = p.variants.map((x) => x.id);

  if (isFull(p)) {
    selected.value = selected.value.filter((id) => !ids.includes(id));
  } else {
    selected.value = [...new Set([...selected.value, ...ids])];
  }
}

// Kiểm tra đã chọn tất cả sản phẩm đang hiển thị
const allChecked = computed(
  () =>
    filteredProducts.value.length > 0 && filteredProducts.value.every(isFull),
);

// Chọn hoặc bỏ chọn tất cả biến thể đang hiển thị
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

// Bỏ chọn một biến thể trong bảng đã chọn
function removeVariant(id) {
  selected.value = selected.value.filter((x) => x !== id);
}

// ======================================
// BIẾN THỂ ĐÃ CHỌN
// ======================================

const chosenRows = computed(() =>
  allVariants.value.filter((x) => selected.value.includes(x.id)),
);

function afterDiscount(price) {
  const discount = Math.min(100, Math.max(0, Number(form.value.value) || 0));

  return Math.round(price * (1 - discount / 100));
}

const money = (n) => `${Number(n || 0).toLocaleString("vi-VN")} đ`;

// ======================================
// LOAD DỮ LIỆU CŨ ĐỂ EDIT
// ======================================
async function loadEditData() {
  try {
    loading.value = true;

    const [response, capabilities] = await Promise.all([
      api.get(`/dot-giam-gia/${route.params.code}`),
      api
        .get("/dot-giam-gia/capabilities")
        .catch(() => ({ data: { data: {} } })),
    ]);

    descriptionSupported.value =
      capabilities.data?.data?.descriptionSupported === true;

    const data = response.data?.data;

    if (!data) {
      showError("Không tìm thấy đợt giảm giá");
      router.push("/dot-giam-gia");
      return;
    }

    form.value = {
      code: data.code || "",
      name: data.name || "",
      value: Number(data.discountValue || 0),
      start: data.startDate || "",
      end: data.endDate || "",
      desc: data.description || "",
    };

    await loadProducts();

    selected.value = [...(data.productDetailIds || [])];
  } catch (error) {
    console.error("Lỗi tải đợt giảm giá:", error);

    showError(error.response?.data?.message || "Không thể tải đợt giảm giá");

    router.push("/dot-giam-gia");
  } finally {
    loading.value = false;
  }
  originalData.value = getCurrentData();
  hasUnsavedChanges.value = false;
}

// ======================================
// KIỂM TRA FORM
// ======================================
function validate() {
  if (missingProductIds.value.length > 0) {
    return `Có ${missingProductIds.value.length} biến thể đã áp dụng nhưng không còn trong danh sách sản phẩm. Vui lòng liên hệ quản trị viên để kiểm tra.`;
  }
  if (!form.value.name.trim()) {
    return "Vui lòng nhập tên đợt giảm giá";
  }

  const discount = Number(form.value.value);

  if (!discount || discount <= 0 || discount > 100) {
    return "Giá trị giảm phải lớn hơn 0 và không vượt quá 100%";
  }

  if (!form.value.start) {
    return "Vui lòng chọn ngày bắt đầu";
  }

  if (!form.value.end) {
    return "Vui lòng chọn ngày kết thúc";
  }

  if (form.value.end < form.value.start) {
    return "Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu";
  }

  if (!selected.value.length) {
    return "Vui lòng chọn ít nhất một biến thể sản phẩm";
  }

  return null;
}

// ======================================
// LƯU THAY ĐỔI
// ======================================

function save() {
  // Không cho lưu khi đang tải hoặc đang lưu
  if (saving.value || loading.value || loadingProducts.value) {
    return;
  }

  // Kiểm tra dữ liệu trước khi gửi
  const message = validate();

  if (message) {
    showError(message);
    return;
  }

  // Kiểm tra biến thể không còn tồn tại trong API
  if (missingProductIds.value.length > 0) {
    showError(
      `Có ${missingProductIds.value.length} biến thể không tìm thấy. Vui lòng kiểm tra trước khi lưu.`,
    );
    return;
  }

  confirmAction(
    "Bạn có chắc chắn muốn lưu thay đổi đợt giảm giá này không?",
    async () => {
      if (saving.value) return;

      const data = {
        name: form.value.name.trim(),
        discountValue: Number(form.value.value),
        startDate: form.value.start,
        endDate: form.value.end,
        description: descriptionSupported.value ? form.value.desc.trim() : null,
        productDetailIds: [...new Set(selected.value)],
      };

      try {
        saving.value = true;

        await api.put(`/dot-giam-gia/${route.params.code}`, data);

        // Đánh dấu đã lưu để không cảnh báo khi chuyển trang
        hasUnsavedChanges.value = false;

        showSuccess("Cập nhật đợt giảm giá thành công!");

        router.push(`/dot-giam-gia/chi-tiet/${form.value.code}`);
      } catch (error) {
        console.error("Lỗi cập nhật đợt giảm giá:", error);

        const status = error.response?.status;
        const backendMessage = error.response?.data?.message;

        if (status === 400) {
          showError(
            backendMessage || "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.",
          );
        } else if (status === 404) {
          showError("Không tìm thấy đợt giảm giá cần cập nhật.");
        } else if (status === 409) {
          showError(
            backendMessage ||
              "Đợt giảm giá bị trùng hoặc xung đột với chương trình khác.",
          );
        } else if (status >= 500) {
          showError("Hệ thống đang gặp lỗi. Vui lòng thử lại sau.");
        } else {
          showError(
            backendMessage || "Không thể kết nối hoặc cập nhật đợt giảm giá.",
          );
        }
      } finally {
        saving.value = false;
      }
    },
  );
}

onBeforeRouteLeave(() => {
  if (!hasUnsavedChanges.value) {
    return true;
  }

  return window.confirm(
    "Bạn có thay đổi chưa lưu. Bạn có chắc chắn muốn rời khỏi trang không?",
  );
});

onMounted(loadEditData);
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <div v-if="loading" class="ss-card ss-empty">
        Đang tải thông tin đợt giảm giá...
      </div>

      <template v-else>
        <div class="top-grid">
          <!-- THÔNG TIN ĐỢT GIẢM -->
          <section class="ss-card ss-form">
            <div class="ss-head">
              <div class="ss-head-icon">
                <i class="bi bi-tag"></i>
              </div>

              <h2>Thông tin đợt giảm</h2>
            </div>

            <div class="ss-field">
              <label class="ss-label">Mã đợt giảm giá</label>

              <div class="campaign-code">
                <i class="bi bi-tag"></i>
                <strong>{{ form.code }}</strong>
                <span>Tự động tạo</span>
              </div>
            </div>

            <div class="ss-field">
              <label class="ss-label">
                Tên đợt <span class="req">*</span>
              </label>
              <input
                class="ss-input"
                v-model="form.name"
                placeholder="Ví dụ: Siêu giảm giá mùa hè"
              />
            </div>

            <div class="ss-field">
              <label class="ss-label">
                Giá trị giảm (%) <span class="req">*</span>
              </label>
              <input
                class="ss-input"
                type="number"
                min="0.01"
                max="100"
                step="0.01"
                v-model.number="form.value"
              />
            </div>

            <div class="two">
              <div class="ss-field">
                <label class="ss-label">
                  Từ ngày <span class="req">*</span>
                </label>
                <input class="ss-input" type="date" v-model="form.start" />
              </div>

              <div class="ss-field">
                <label class="ss-label">
                  Đến ngày <span class="req">*</span>
                </label>
                <input class="ss-input" type="date" v-model="form.end" />
              </div>
            </div>

            <div v-if="descriptionSupported" class="ss-field">
              <label class="ss-label">Mô tả</label>
              <textarea
                class="ss-textarea"
                v-model="form.desc"
                placeholder="Nhập mô tả..."
              ></textarea>
            </div>

           

            <button
              class="ss-btn block"
              :disabled="saving"
              @click="router.back()"
            >
              Hủy
            </button>
          </section>

          <!-- CHỌN SẢN PHẨM -->
          <section class="ss-card">
            <div class="ss-head">
              <div class="ss-head-icon">
                <i class="bi bi-search"></i>
              </div>

              <div>
                <h2>Chọn sản phẩm áp dụng</h2>
                <p>Đã chọn {{ selected.length }} biến thể</p>
              </div>
            </div>

            <div class="pick-filter">
              <div class="ss-search">
                <i class="bi bi-search"></i>
                <input
                  class="ss-input"
                  v-model="draft.q"
                  placeholder="Tìm theo tên hoặc mã sản phẩm..."
                />
              </div>

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
            </div>

            <div v-if="loadingProducts" class="ss-empty">
              Đang tải sản phẩm...
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

                    <td class="c">{{ i + 1 }}</td>
                    <td>{{ p.code }}</td>
                    <td>{{ p.name }}</td>
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

        <!-- TỔNG KẾT TRƯỚC KHI LƯU -->
        <div class="edit-summary">
          <div class="edit-summary-info">
            <div class="edit-summary-icon">
              <i class="bi bi-check2-circle"></i>
            </div>

            <div>
              <strong>Tổng kết thay đổi</strong>

              <p>
                Đã chọn
                <b>{{ chosenRows.length }}</b>
                biến thể sản phẩm · Mức giảm
                <b>{{ form.value || 0 }}%</b>
              </p>
            </div>
          </div>

          <button
            type="button"
            class="ss-btn primary"
            :disabled="saving || loading || loadingProducts"
            @click="save"
          >
            <i class="bi" :class="saving ? 'bi-arrow-repeat' : 'bi-floppy'"></i>

            {{ saving ? "Đang lưu..." : "Lưu thay đổi" }}
          </button>
        </div>

        <!-- BIẾN THỂ ĐÃ CHỌN -->
        <div
          v-if="missingProductIds.length > 0"
          class="ss-card missing-warning"
        >
          <i class="bi bi-exclamation-triangle"></i>

          Có {{ missingProductIds.length }} biến thể đã áp dụng nhưng hiện không
          tìm thấy trong danh sách sản phẩm. Vui lòng kiểm tra trước khi cập
          nhật đợt giảm giá.
        </div>
        <section class="ss-card">
          <div class="ss-head">
            <div class="ss-head-icon">
              <i class="bi bi-check2-square"></i>
            </div>

            <div>
              <h2>Sản phẩm & biến thể đã chọn áp dụng</h2>
              <p>
                Danh sách chi tiết gồm
                {{ chosenRows.length }} biến thể đã chọn
              </p>
            </div>
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
                  <th class="c" style="width: 60px">Bỏ chọn</th>
                </tr>
              </thead>

              <tbody>
                <tr v-for="(x, i) in chosenRows" :key="x.id">
                  <td class="c">{{ i + 1 }}</td>

                  <td>
                    <span class="ss-strong">{{ x.product }}</span>
                    <span class="ss-sub">{{ x.productCode }}</span>
                  </td>

                  <td>
                    {{ x.code }}
                    <span class="ss-sub"> {{ x.color }} · {{ x.size }} </span>
                  </td>

                  <td class="nowrap">
                    {{ money(x.price) }}
                  </td>

                  <td class="nowrap">
                    <div class="discount-price">
                      <strong>{{ money(afterDiscount(x.price)) }}</strong>
                      <small>Giảm {{ form.value }}%</small>
                    </div>
                  </td>

                  <td class="r">{{ x.qty }}</td>

                  <td class="c">
                    <button
                      class="ss-icon-btn danger"
                      title="Bỏ chọn"
                      @click="removeVariant(x.id)"
                    >
                      <i class="bi bi-x-lg"></i>
                    </button>
                  </td>
                </tr>

                <tr v-if="!chosenRows.length">
                  <td colspan="7" class="ss-empty">
                    <i class="bi bi-inbox"></i>
                    Chưa có biến thể nào được chọn.
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>
    </main>
  </AdminLayout>
</template>

<style scoped>
.edit-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
  padding: 16px 20px;
  border: 1px solid var(--ss-border, #e2e8f0);
  border-radius: 10px;
  background: var(--ss-card-bg, #fff);
}

.edit-summary-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.edit-summary-icon {
  font-size: 24px;
  color: var(--ss-primary, #2563eb);
}

.edit-summary-info strong {
  font-size: 14px;
}

.edit-summary-info p {
  margin: 5px 0 0;
  font-size: 13px;
  color: var(--ss-muted, #64748b);
}

.edit-summary-info b {
  color: var(--ss-text, #1e293b);
}

@media (max-width: 760px) {
  .edit-summary {
    flex-direction: column;
    align-items: stretch;
  }

  .edit-summary > button {
    width: 100%;
  }
}

.top-grid {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.two {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.pick-filter {
  display: grid;
  grid-template-columns:
    minmax(0, 1fr)
    150px
    150px
    auto;
  gap: 10px;
  align-items: end;
}

.pick-filter > .ss-btn {
  height: 40px;
}

.list-filter {
  display: grid;
  grid-template-columns:
    170px
    170px
    240px;
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

@media (max-width: 1100px) {
  .top-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .pick-filter,
  .list-filter {
    grid-template-columns: 1fr;
  }
}
.missing-warning {
  margin-bottom: 16px;
  padding: 14px 16px;
  color: #b45309;
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-radius: 8px;
  font-size: 13px;
}

.missing-warning i {
  margin-right: 8px;
}
.campaign-code {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--ss-border, #e2e8f0);
  border-radius: 8px;
  font-size: 13px;
}

.campaign-code i {
  color: var(--ss-primary);
}

.campaign-code span {
  margin-left: auto;
  font-size: 11px;
  color: var(--ss-muted, #64748b);
}
.pick-filter {
  display: grid;
  grid-template-columns:
    minmax(0, 1fr)
    150px
    150px;
  gap: 10px;
  align-items: end;
}
.discount-price {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.discount-price strong {
  color: var(--ss-primary);
  font-size: 14px;
  font-weight: 700;
}

.discount-price small {
  color: var(--ss-muted);
  font-size: 11px;
}

.ss-table th,
.ss-table td {
  vertical-align: middle;
}

.ss-table .nowrap {
  white-space: nowrap;
}
button:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

button:disabled .bi-arrow-repeat {
  display: inline-block;
  animation: edit-spin 1s linear infinite;
}

@keyframes edit-spin {
  to {
    transform: rotate(360deg);
  }
}

/* ========================================
   RESPONSIVE MÀN EDIT ĐỢT GIẢM GIÁ
======================================== */

/* Laptop nhỏ */
@media (max-width: 1200px) {
  .top-grid {
    grid-template-columns: 320px minmax(0, 1fr);
  }

  .pick-filter {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .pick-filter .ss-search {
    grid-column: 1 / -1;
  }
}

/* Tablet */
@media (max-width: 992px) {
  .top-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .pick-filter {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .list-filter {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

/* Điện thoại */
@media (max-width: 600px) {
  .top-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 12px;
  }

  .two,
  .pick-filter,
  .list-filter {
    grid-template-columns: minmax(0, 1fr);
  }

  .ss-card {
    min-width: 0;
  }

  .ss-table-wrap {
    max-width: 100%;
    overflow-x: auto;
  }

  .ss-table {
    min-width: 650px;
  }

  .edit-summary {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .edit-summary > button {
    width: 100%;
  }

  .campaign-code {
    flex-wrap: wrap;
  }
}
</style>
