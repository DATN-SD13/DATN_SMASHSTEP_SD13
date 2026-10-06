<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { computed, reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../../../../utils/api'

const route = useRoute()
const router = useRouter()

// =====================================
// TRẠNG THÁI
// =====================================

const loading = ref(true)
const loadingProducts = ref(false)
const saving = ref(false)

// =====================================
// FORM
// =====================================

const form = ref({
  code: '',
  name: '',
  value: 0,
  start: '',
  end: '',
  desc: ''
})

// =====================================
// DANH SÁCH SẢN PHẨM
// =====================================

const products = ref([])

// =====================================
// LOAD SẢN PHẨM TỪ BACKEND
// =====================================

async function loadProducts() {
  try {
    loadingProducts.value = true

    const response = await api.get(
      '/dot-giam-gia/san-pham-chi-tiet'
    )

    const data = response.data?.data || []

    const productMap = new Map()

    data.forEach(item => {
      const productCode =
        item.productCode || ''

      const productName =
        item.productName || ''

      if (!productMap.has(productCode)) {
        productMap.set(productCode, {
          code: productCode,
          name: productName,
          variants: []
        })
      }

      productMap
        .get(productCode)
        .variants
        .push({
          id: item.id,

          code:
            item.productDetailCode ||
            item.sku ||
            `CTSP-${item.id}`,

          sku:
            item.sku || '',

          color:
            item.color || '',

          colorHex:
            item.colorHex || '',

          size:
            item.size || '',

          price:
            Number(item.price || 0),

          qty:
            Number(item.quantity || 0),

          status:
            item.status
        })
    })

    products.value =
      Array.from(productMap.values())

  } catch (error) {
    console.error(
      'Lỗi tải sản phẩm:',
      error
    )

    products.value = []

    alert(
      error.response?.data?.message ||
      'Không thể tải danh sách sản phẩm'
    )

  } finally {
    loadingProducts.value = false
  }
}

// =====================================
// TẤT CẢ BIẾN THỂ
// =====================================

const allVariants = computed(() => {
  return products.value.flatMap(product =>
    product.variants.map(variant => ({
      ...variant,

      productCode:
        product.code,

      productName:
        product.name
    }))
  )
})

// =====================================
// DANH SÁCH MÀU
// =====================================

const colors = computed(() => {
  return [
    ...new Set(
      allVariants.value
        .map(x => x.color)
        .filter(Boolean)
    )
  ]
})

// =====================================
// DANH SÁCH SIZE
// =====================================

const sizes = computed(() => {
  return [
    ...new Set(
      allVariants.value
        .map(x => x.size)
        .filter(Boolean)
    )
  ]
})

// =====================================
// BỘ LỌC
// =====================================

const draft = reactive({
  keyword: '',
  color: '',
  size: ''
})

const applied = reactive({
  keyword: '',
  color: '',
  size: ''
})

function applyFilter() {
  applied.keyword =
    draft.keyword.trim()

  applied.color =
    draft.color

  applied.size =
    draft.size
}

function resetFilter() {
  draft.keyword = ''
  draft.color = ''
  draft.size = ''

  applied.keyword = ''
  applied.color = ''
  applied.size = ''
}

// =====================================
// SẢN PHẨM SAU KHI LỌC
// =====================================

const filteredProducts = computed(() => {
  const keyword =
    applied.keyword
      .trim()
      .toLowerCase()

  return products.value
    .map(product => {
      const variants =
        product.variants.filter(variant => {

          const matchKeyword =
            !keyword ||
            product.code
              .toLowerCase()
              .includes(keyword) ||
            product.name
              .toLowerCase()
              .includes(keyword) ||
            variant.code
              .toLowerCase()
              .includes(keyword) ||
            variant.sku
              .toLowerCase()
              .includes(keyword)

          const matchColor =
            !applied.color ||
            variant.color === applied.color

          const matchSize =
            !applied.size ||
            variant.size === applied.size

          return (
            matchKeyword &&
            matchColor &&
            matchSize
          )
        })

      return {
        ...product,
        variants
      }
    })
    .filter(product =>
      product.variants.length > 0
    )
})

// =====================================
// BIẾN THỂ ĐÃ CHỌN
// LƯU ID THẬT TỪ DATABASE
// =====================================

const selected = ref([])

// =====================================
// KIỂM TRA SẢN PHẨM ĐÃ CHỌN
// =====================================

const isChosen = product => {
  return product.variants.some(
    variant =>
      selected.value.includes(
        variant.id
      )
  )
}

// =====================================
// KIỂM TRA ĐÃ CHỌN HẾT BIẾN THỂ
// =====================================

const isFull = product => {
  return (
    product.variants.length > 0 &&
    product.variants.every(
      variant =>
        selected.value.includes(
          variant.id
        )
    )
  )
}

// =====================================
// CHỌN / BỎ CHỌN MỘT SẢN PHẨM
// =====================================

function toggleProduct(product) {
  const ids =
    product.variants.map(
      variant => variant.id
    )

  if (isFull(product)) {

    selected.value =
      selected.value.filter(
        id => !ids.includes(id)
      )

  } else {

    selected.value = [
      ...new Set([
        ...selected.value,
        ...ids
      ])
    ]
  }
}

// =====================================
// CHECKBOX CHỌN TẤT CẢ
// =====================================

const allChecked = computed(() => {
  if (!allVariants.value.length) {
    return false
  }

  return allVariants.value.every(
    variant =>
      selected.value.includes(
        variant.id
      )
  )
})

function toggleAll() {
  const ids =
    allVariants.value.map(
      variant => variant.id
    )

  if (allChecked.value) {
    selected.value = []
  } else {
    selected.value = [...ids]
  }
}

// =====================================
// XÓA MỘT BIẾN THỂ ĐÃ CHỌN
// =====================================

function removeVariant(id) {
  selected.value =
    selected.value.filter(
      selectedId =>
        selectedId !== id
    )
}

// =====================================
// DANH SÁCH BIẾN THỂ ĐÃ CHỌN
// =====================================

const chosenRows = computed(() => {
  return allVariants.value.filter(
    variant =>
      selected.value.includes(
        variant.id
      )
  )
})

// =====================================
// FORMAT TIỀN
// =====================================

function money(value) {
  return `${Number(
    value || 0
  ).toLocaleString('vi-VN')} đ`
}

// =====================================
// GIÁ SAU KHI GIẢM
// =====================================

function afterDiscount(price) {
  const discount =
    Number(form.value.value) || 0

  return Math.round(
    Number(price || 0) *
    (1 - discount / 100)
  )
}

// =====================================
// LOAD THÔNG TIN ĐỢT GIẢM GIÁ
// =====================================

async function loadEditData() {
  try {
    loading.value = true

    const response =
      await api.get(
        `/dot-giam-gia/${route.params.code}`
      )

    const data =
      response.data?.data

    if (!data) {
      alert(
        'Không tìm thấy đợt giảm giá'
      )

      router.push(
        '/dot-giam-gia'
      )

      return
    }

    // Đổ dữ liệu cũ vào form
    form.value = {
      code:
        data.code || '',

      name:
        data.name || '',

      value:
        Number(
          data.discountValue || 0
        ),

      start:
        data.startDate || '',

      end:
        data.endDate || '',

      desc:
        data.description || ''
    }

    // Load sản phẩm trước
    await loadProducts()

    // Sau đó tick lại sản phẩm cũ
    selected.value = [
      ...(data.productDetailIds || [])
    ]

  } catch (error) {
    console.error(
      'Lỗi tải đợt giảm giá:',
      error
    )

    alert(
      error.response?.data?.message ||
      'Không thể tải đợt giảm giá'
    )

    router.push(
      '/dot-giam-gia'
    )

  } finally {
    loading.value = false
  }
}

// =====================================
// VALIDATE
// =====================================

function validate() {

  if (!form.value.name.trim()) {
    return 'Vui lòng nhập tên đợt giảm giá'
  }

  const discountValue =
    Number(form.value.value)

  if (
    !discountValue ||
    discountValue <= 0
  ) {
    return 'Giá trị giảm phải lớn hơn 0'
  }

  if (discountValue > 100) {
    return 'Giá trị giảm không được vượt quá 100%'
  }

  if (!form.value.start) {
    return 'Vui lòng chọn ngày bắt đầu'
  }

  if (!form.value.end) {
    return 'Vui lòng chọn ngày kết thúc'
  }

  if (
    form.value.end <
    form.value.start
  ) {
    return 'Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu'
  }

  if (
    selected.value.length === 0
  ) {
    return 'Vui lòng chọn ít nhất một biến thể sản phẩm'
  }

  return null
}

// =====================================
// LƯU THAY ĐỔI
// =====================================

async function save() {

  const message = validate()

  if (message) {
    alert(message)
    return
  }

  const confirmed =
    window.confirm(
      'Bạn có chắc chắn muốn lưu thay đổi đợt giảm giá này không?'
    )

  if (!confirmed) {
    return
  }

  const data = {
    code:
      form.value.code.trim(),

    name:
      form.value.name.trim(),

    discountValue:
      Number(form.value.value),

    startDate:
      form.value.start,

    endDate:
      form.value.end,

    description:
      form.value.desc.trim(),

    productDetailIds:
      selected.value
  }

  try {
    saving.value = true

    await api.put(
      `/dot-giam-gia/${route.params.code}`,
      data
    )

    alert(
      'Cập nhật đợt giảm giá thành công!'
    )

    router.push(
      `/dot-giam-gia/chi-tiet/${form.value.code}`
    )

  } catch (error) {
    console.error(
      'Lỗi cập nhật đợt giảm giá:',
      error
    )

    alert(
      error.response?.data?.message ||
      'Không thể cập nhật đợt giảm giá'
    )

  } finally {
    saving.value = false
  }
}

// =====================================
// LOAD KHI MỞ TRANG
// =====================================

onMounted(() => {
  loadEditData()
})
</script>

<template>

  <AdminLayout>

    <main class="ss-page">

      <!-- ========================= -->
      <!-- LOADING -->
      <!-- ========================= -->

      <div
        v-if="loading"
        class="ss-card ss-empty"
      >
        <i class="bi bi-arrow-repeat"></i>
        Đang tải dữ liệu...
      </div>

      <template v-else>

        <!-- ========================= -->
        <!-- PHẦN TRÊN -->
        <!-- ========================= -->

        <div class="top-grid">

          <!-- ======================= -->
          <!-- FORM THÔNG TIN -->
          <!-- ======================= -->

          <section class="ss-card ss-form">

            <div class="ss-head">

              <div class="ss-head-icon">
                <i class="bi bi-tag"></i>
              </div>

              <div>

                <h2>
                  Sửa đợt giảm giá
                </h2>

                <p>
                  Cập nhật thông tin đợt giảm giá.
                </p>

              </div>

            </div>

            <!-- MÃ ĐỢT -->

            <div class="ss-field">

              <label class="ss-label">
                Mã đợt
              </label>

              <input
                class="ss-input"
                v-model="form.code"
                disabled
              />

            </div>

            <!-- TÊN ĐỢT -->

            <div class="ss-field">

              <label class="ss-label">
                Tên đợt
              </label>

              <input
                class="ss-input"
                v-model="form.name"
                placeholder="Nhập tên đợt giảm giá"
              />

            </div>

            <!-- GIÁ TRỊ GIẢM -->

            <div class="ss-field">

              <label class="ss-label">
                Giá trị giảm (%)
              </label>

              <input
                class="ss-input"
                type="number"
                min="0"
                max="100"
                v-model.number="form.value"
                placeholder="Nhập % giảm"
              />

            </div>

            <!-- NGÀY -->

            <div class="two">

              <div class="ss-field">

                <label class="ss-label">
                  Từ ngày
                </label>

                <input
                  class="ss-input"
                  type="date"
                  v-model="form.start"
                />

              </div>

              <div class="ss-field">

                <label class="ss-label">
                  Đến ngày
                </label>

                <input
                  class="ss-input"
                  type="date"
                  v-model="form.end"
                />

              </div>

            </div>

            <!-- MÔ TẢ -->

            <div class="ss-field">

              <label class="ss-label">
                Mô tả
              </label>

              <textarea
                class="ss-textarea"
                v-model="form.desc"
                rows="4"
                placeholder="Nhập mô tả"
              ></textarea>

            </div>

            <!-- BUTTON -->

            <div class="ss-actions">

              <button
                type="button"
                class="ss-btn"
                @click="router.back()"
              >
                <i class="bi bi-arrow-left"></i>
                Quay lại
              </button>

              <button
                type="button"
                class="ss-btn primary"
                :disabled="saving"
                @click="save"
              >
                <i class="bi bi-floppy"></i>

                {{
                  saving
                    ? 'Đang lưu...'
                    : 'Lưu thay đổi'
                }}

              </button>

            </div>

          </section>

          <!-- ======================= -->
          <!-- CHỌN SẢN PHẨM -->
          <!-- ======================= -->

          <section class="ss-card">

            <div class="ss-head">

              <div class="ss-head-icon">
                <i class="bi bi-box-seam"></i>
              </div>

              <div>

                <h2>
                  Chọn sản phẩm
                </h2>

                <p>
                  Chọn các biến thể áp dụng đợt giảm giá.
                </p>

              </div>

            </div>

            <!-- FILTER -->

            <div class="pick-filter">

              <div class="ss-field">

                <span class="ss-label">
                  Tìm kiếm
                </span>

                <div class="ss-search">

                  <i class="bi bi-search"></i>

                  <input
                    class="ss-input"
                    v-model="draft.keyword"
                    placeholder="Mã, tên sản phẩm..."
                    @keyup.enter="applyFilter"
                  />

                </div>

              </div>

              <div class="ss-field">

                <span class="ss-label">
                  Màu sắc
                </span>

                <select
                  class="ss-select"
                  v-model="draft.color"
                >

                  <option value="">
                    Tất cả màu
                  </option>

                  <option
                    v-for="color in colors"
                    :key="color"
                    :value="color"
                  >
                    {{ color }}
                  </option>

                </select>

              </div>

              <div class="ss-field">

                <span class="ss-label">
                  Kích thước
                </span>

                <select
                  class="ss-select"
                  v-model="draft.size"
                >

                  <option value="">
                    Tất cả kích thước
                  </option>

                  <option
                    v-for="size in sizes"
                    :key="size"
                    :value="size"
                  >
                    {{ size }}
                  </option>

                </select>

              </div>

            </div>

            <!-- FILTER BUTTON -->

            <div class="ss-actions">

              <button
                type="button"
                class="ss-btn"
                @click="resetFilter"
              >
                Đặt lại
              </button>

              <button
                type="button"
                class="ss-btn primary"
                @click="applyFilter"
              >
                <i class="bi bi-search"></i>
                Tìm kiếm
              </button>

            </div>

            <!-- LOADING PRODUCTS -->

            <div
              v-if="loadingProducts"
              class="ss-empty"
            >
              Đang tải sản phẩm...
            </div>

            <!-- PRODUCT LIST -->

            <div
              v-else
              class="list-filter"
            >

              <!-- CHỌN TẤT CẢ -->

              <label
                v-if="allVariants.length"
                class="product-row select-all"
              >

                <input
                  type="checkbox"
                  :checked="allChecked"
                  @change="toggleAll"
                />

                <div>

                  <strong>
                    Chọn tất cả
                  </strong>

                  <span class="ss-sub">
                    {{ allVariants.length }}
                    biến thể
                  </span>

                </div>

              </label>

              <!-- DANH SÁCH PRODUCT -->

              <div
                v-for="product in filteredProducts"
                :key="product.code"
                class="product-box"
              >

                <label class="product-row">

                  <input
                    type="checkbox"
                    :checked="isFull(product)"
                    @change="toggleProduct(product)"
                  />

                  <div>

                    <strong>
                      {{ product.name }}
                    </strong>

                    <span class="ss-sub">
                      {{ product.code }}
                      ·
                      {{ product.variants.length }}
                      biến thể
                    </span>

                  </div>

                </label>

                <!-- VARIANTS -->

                <label
                  v-for="variant in product.variants"
                  :key="variant.id"
                  class="variant-row"
                >

                  <input
                    type="checkbox"
                    :value="variant.id"
                    v-model="selected"
                  />

                  <div class="variant-info">

                    <span class="ss-code">
                      {{ variant.code }}
                    </span>

                    <span class="ss-sub">

                      {{ variant.color || '-' }}

                      ·

                      {{ variant.size || '-' }}

                      ·

                      {{ money(variant.price) }}

                    </span>

                  </div>

                </label>

              </div>

              <!-- EMPTY -->

              <div
                v-if="!filteredProducts.length"
                class="ss-empty"
              >
                <i class="bi bi-inbox"></i>
                Không có sản phẩm phù hợp.
              </div>

            </div>

          </section>

        </div>

        <!-- ========================= -->
        <!-- DANH SÁCH ĐÃ CHỌN -->
        <!-- ========================= -->

        <section class="ss-card chosen">

          <div class="ss-head">

            <div class="ss-head-icon">
              <i class="bi bi-check2-square"></i>
            </div>

            <h2>
              Biến thể đã chọn
            </h2>

            <span class="ss-spacer"></span>

            <span class="ss-count">
              {{ chosenRows.length }} biến thể
            </span>

          </div>

          <div class="ss-table-wrap">

            <table class="ss-table">

              <thead>

                <tr>

                  <th class="w-stt c">
                    STT
                  </th>

                  <th>
                    Sản phẩm
                  </th>

                  <th>
                    Mã biến thể
                  </th>

                  <th>
                    Màu sắc
                  </th>

                  <th>
                    Kích thước
                  </th>

                  <th>
                    Giá bán
                  </th>

                  <th>
                    Giá sau giảm
                  </th>

                  <th class="r">
                    Số lượng
                  </th>

                  <th class="c">
                    Hành động
                  </th>

                </tr>

              </thead>

              <tbody>

                <tr
                  v-for="(x, i) in chosenRows"
                  :key="x.id"
                >

                  <td class="c">
                    {{ i + 1 }}
                  </td>

                  <td>

                    <strong>
                      {{ x.productName }}
                    </strong>

                    <span class="ss-sub">
                      {{ x.productCode }}
                    </span>

                  </td>

                  <td>
                    <span class="ss-code">
                      {{ x.code }}
                    </span>
                  </td>

                  <td>
                    {{ x.color || '-' }}
                  </td>

                  <td>
                    {{ x.size || '-' }}
                  </td>

                  <td class="nowrap">
                    {{ money(x.price) }}
                  </td>

                  <td class="nowrap">

                    <strong>
                      {{
                        money(
                          afterDiscount(x.price)
                        )
                      }}
                    </strong>

                  </td>

                  <td class="r">
                    {{ x.qty }}
                  </td>

                  <td class="c">

                    <button
                      type="button"
                      class="ss-icon-btn danger"
                      title="Bỏ chọn"
                      @click="removeVariant(x.id)"
                    >
                      <i class="bi bi-trash"></i>
                    </button>

                  </td>

                </tr>

                <tr
                  v-if="!chosenRows.length"
                >

                  <td
                    colspan="9"
                    class="ss-empty"
                  >

                    <i class="bi bi-inbox"></i>

                    Chưa chọn biến thể sản phẩm.

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

.top-grid {
  display: grid;
  grid-template-columns:
    360px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.two {
  display: grid;
  grid-template-columns:
    repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.pick-filter {
  display: grid;
  grid-template-columns:
    minmax(0, 2fr)
    minmax(130px, 1fr)
    minmax(130px, 1fr);
  gap: 10px;
}

.list-filter {
  margin-top: 14px;
  max-height: 430px;
  overflow-y: auto;
  border-top:
    1px solid
    var(--ss-border, #e8edf2);
}

.product-box {
  border-bottom:
    1px solid
    var(--ss-border, #e8edf2);
}

.product-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 8px;
  cursor: pointer;
}

.product-row > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.product-row.select-all {
  border-bottom:
    1px solid
    var(--ss-border, #e8edf2);
}

.variant-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 8px 9px 34px;
  cursor: pointer;
}

.variant-row:hover,
.product-row:hover {
  background: #f8fafc;
}

.variant-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.chosen {
  margin-top: 16px;
}

.ss-sub {
  display: block;
  margin-top: 3px;
  color: var(--ss-muted);
  font-size: 12px;
}

input[type='checkbox'] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

@media (max-width: 1000px) {

  .top-grid {
    grid-template-columns: 1fr;
  }

}

@media (max-width: 760px) {

  .pick-filter {
    grid-template-columns: 1fr;
  }

  .two {
    grid-template-columns: 1fr;
  }

}

</style>