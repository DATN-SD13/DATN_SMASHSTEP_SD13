<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { computed, reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../../../utils/api'

const router = useRouter()

// =====================================================
// FORM ĐỢT GIẢM GIÁ
// =====================================================
const form = ref({
  code: '',
  name: '',
  value: 0,
  start: '',
  end: '',
  desc: ''
})

// =====================================================
// DỮ LIỆU SẢN PHẨM THẬT TỪ BACKEND
// =====================================================
const products = ref([])
const loadingProducts = ref(false)

// Lấy sản phẩm + biến thể từ backend
async function loadProducts() {
  try {
    loadingProducts.value = true

    const response = await api.get(
      '/dot-giam-gia/san-pham-chi-tiet'
    )

    const data = response.data?.data || []

    /*
      Backend trả từng biến thể.

      Ta gom lại thành:

      products = [
        {
          code: 'SP001',
          name: 'Nike Air Force 1',
          variants: [...]
        }
      ]
    */

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

      productMap.get(productCode).variants.push({
        id: item.id,

        code:
          item.productDetailCode ||
          item.sku ||
          `CTSP-${item.id}`,

        sku: item.sku || '',

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

// =====================================================
// TẤT CẢ BIẾN THỂ
// =====================================================
const allVariants = computed(() =>
  products.value.flatMap(p =>
    p.variants.map(x => ({
      ...x,
      product: p.name,
      productCode: p.code
    }))
  )
)

// =====================================================
// DANH SÁCH MÀU
// =====================================================
const colors = computed(() =>
  [
    ...new Set(
      allVariants.value
        .map(x => x.color)
        .filter(Boolean)
    )
  ]
)

// =====================================================
// DANH SÁCH SIZE
// =====================================================
const sizes = computed(() =>
  [
    ...new Set(
      allVariants.value
        .map(x => x.size)
        .filter(Boolean)
    )
  ].sort()
)

// =====================================================
// BỘ LỌC CHỌN SẢN PHẨM
// =====================================================
const draft = reactive({
  q: '',
  color: 'all',
  size: 'all'
})

const applied = reactive({
  q: '',
  color: 'all',
  size: 'all'
})

function applySearch() {
  Object.assign(applied, draft)
}

// =====================================================
// SẢN PHẨM SAU KHI LỌC
// =====================================================
const filteredProducts = computed(() => {
  const q =
    applied.q
      .trim()
      .toLowerCase()

  return products.value.filter(p => {
    const matchKeyword =
      !q ||
      p.code.toLowerCase().includes(q) ||
      p.name.toLowerCase().includes(q)

    const matchVariant =
      p.variants.some(x =>
        (
          applied.color === 'all' ||
          x.color === applied.color
        ) &&
        (
          applied.size === 'all' ||
          x.size === applied.size
        )
      )

    return matchKeyword && matchVariant
  })
})

// =====================================================
// BIẾN THỂ ĐÃ CHỌN
// Lưu ID thật của san_pham_chi_tiet
// =====================================================
const selected = ref([])

const isChosen = (p) =>
  p.variants.some(x =>
    selected.value.includes(x.id)
  )

const isFull = (p) =>
  p.variants.length > 0 &&
  p.variants.every(x =>
    selected.value.includes(x.id)
  )

// Chọn / bỏ chọn một sản phẩm
function toggleProduct(p) {
  const ids =
    p.variants.map(x => x.id)

  if (isFull(p)) {
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

// =====================================================
// CHỌN TẤT CẢ
// =====================================================
const allChecked = computed(() =>
  filteredProducts.value.length > 0 &&
  filteredProducts.value.every(isFull)
)

function toggleAll() {
  const ids =
    filteredProducts.value.flatMap(
      p => p.variants.map(x => x.id)
    )

  if (allChecked.value) {
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

// =====================================================
// XÓA BIẾN THỂ KHỎI DANH SÁCH CHỌN
// =====================================================
function removeVariant(id) {
  selected.value =
    selected.value.filter(
      x => x !== id
    )
}

// =====================================================
// DANH SÁCH BIẾN THỂ ĐÃ CHỌN
// =====================================================
const listFilter = reactive({
  q: '',
  color: 'all',
  size: 'all'
})

const chosenRows = computed(() =>
  allVariants.value.filter(x =>
    selected.value.includes(x.id)
  )
)

const shownRows = computed(() => {
  const q =
    listFilter.q
      .trim()
      .toLowerCase()

  return chosenRows.value.filter(x =>
    (
      !q ||
      x.code.toLowerCase().includes(q) ||
      x.product.toLowerCase().includes(q)
    ) &&
    (
      listFilter.color === 'all' ||
      x.color === listFilter.color
    ) &&
    (
      listFilter.size === 'all' ||
      x.size === listFilter.size
    )
  )
})

// =====================================================
// TÍNH GIÁ SAU GIẢM
// =====================================================
const afterDiscount = (price) => {
  const discount =
    Math.min(
      100,
      Math.max(
        0,
        Number(form.value.value) || 0
      )
    )

  return Math.round(
    price * (1 - discount / 100)
  )
}

// =====================================================
// FORMAT TIỀN
// =====================================================
const money = (n) =>
  `${Number(n || 0).toLocaleString('vi-VN')} đ`

// =====================================================
// TẠO ĐỢT GIẢM GIÁ
// Bước này CHƯA gọi POST.
// Test sản phẩm trước.
// =====================================================
async function save() {
  // Kiểm tra dữ liệu
  if (!form.value.code.trim()) {
    alert('Vui lòng nhập mã đợt giảm giá')
    return
  }

  if (!form.value.name.trim()) {
    alert('Vui lòng nhập tên đợt giảm giá')
    return
  }

  const discountValue = Number(form.value.value)

  if (
    !discountValue ||
    discountValue <= 0 ||
    discountValue > 100
  ) {
    alert('Giá trị giảm phải lớn hơn 0 và không vượt quá 100%')
    return
  }

  if (!form.value.start) {
    alert('Vui lòng chọn ngày bắt đầu')
    return
  }

  if (!form.value.end) {
    alert('Vui lòng chọn ngày kết thúc')
    return
  }

  if (form.value.end < form.value.start) {
    alert('Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu')
    return
  }

  if (selected.value.length === 0) {
    alert('Vui lòng chọn ít nhất một biến thể sản phẩm')
    return
  }

  const data = {
    code: form.value.code.trim(),
    name: form.value.name.trim(),
    discountValue: discountValue,
    startDate: form.value.start,
    endDate: form.value.end,
    description: form.value.desc.trim(),
    productDetailIds: selected.value
  }

  try {
    await api.post(
      '/dot-giam-gia',
      data
    )

    alert('Tạo đợt giảm giá thành công!')

    router.push('/dot-giam-gia')

  } catch (error) {
    console.error(
      'Lỗi tạo đợt giảm giá:',
      error
    )

    alert(
      error.response?.data?.message ||
      'Không thể tạo đợt giảm giá'
    )
  }
}

// =====================================================
// LOAD KHI MỞ TRANG
// =====================================================
onMounted(() => {
  loadProducts()
})
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

            <h2>
              Thông tin đợt giảm
            </h2>

          </div>

          <div class="ss-field">

            <label class="ss-label">
              Mã đợt
              <span class="req">*</span>
            </label>

            <input
              class="ss-input"
              v-model="form.code"
            />

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
              min="0"
              max="100"
              v-model="form.value"
            />

          </div>

          <div class="two">

            <div class="ss-field">

              <label class="ss-label">
                Từ ngày
                <span class="req">*</span>
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
                <span class="req">*</span>
              </label>

              <input
                class="ss-input"
                type="date"
                v-model="form.end"
              />

            </div>

          </div>

          <div class="ss-field">

            <label class="ss-label">
              Mô tả
            </label>

            <textarea
              class="ss-textarea"
              v-model="form.desc"
              placeholder="Nhập mô tả..."
            ></textarea>

          </div>

          <button
            class="ss-btn primary block"
            @click="save"
          >
            <i class="bi bi-check2"></i>
            Tạo đợt giảm giá
          </button>

          <button
            class="ss-btn block"
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

              <h2>
                Chọn sản phẩm áp dụng
              </h2>

              <p>
                Đã chọn
                {{ selected.length }}
                biến thể
              </p>

            </div>

          </div>

          <div class="pick-filter">

            <div class="ss-search">

              <i class="bi bi-search"></i>

              <input
                class="ss-input"
                v-model="draft.q"
                placeholder="Tìm theo tên hoặc mã sản phẩm..."
                @keyup.enter="applySearch"
              />

            </div>

            <div class="ss-field">

              <span class="ss-label strong">
                Màu sắc
              </span>

              <select
                class="ss-select"
                v-model="draft.color"
              >

                <option value="all">
                  Tất cả màu sắc
                </option>

                <option
                  v-for="c in colors"
                  :key="c"
                >
                  {{ c }}
                </option>

              </select>

            </div>

            <div class="ss-field">

              <span class="ss-label strong">
                Kích cỡ
              </span>

              <select
                class="ss-select"
                v-model="draft.size"
              >

                <option value="all">
                  Tất cả kích cỡ
                </option>

                <option
                  v-for="s in sizes"
                  :key="s"
                >
                  {{ s }}
                </option>

              </select>

            </div>

            <button
              class="ss-btn primary"
              @click="applySearch"
            >
              <i class="bi bi-search"></i>
              Tìm kiếm
            </button>

          </div>

          <div
            v-if="loadingProducts"
            class="ss-empty"
          >
            Đang tải sản phẩm...
          </div>

          <div
            v-else
            class="ss-table-wrap"
          >

            <table
              class="ss-table"
              style="min-width:520px"
            >

              <thead>

                <tr>

                  <th
                    style="width:44px"
                    class="c"
                  >
                    <input
                      type="checkbox"
                      :checked="allChecked"
                      @change="toggleAll"
                    />
                  </th>

                  <th class="w-stt c">
                    STT
                  </th>

                  <th>
                    Mã SP
                  </th>

                  <th>
                    Tên sản phẩm
                  </th>

                  <th
                    class="r"
                    style="width:70px"
                  ></th>

                </tr>

              </thead>

              <tbody>

                <tr
                  v-for="(p, i) in filteredProducts"
                  :key="p.code"
                >

                  <td class="c">

                    <input
                      type="checkbox"
                      :checked="isFull(p)"
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

                  <td class="r">

                    <button
                      class="ss-icon-btn"
                      :class="{
                        chosen: isChosen(p)
                      }"
                      :title="
                        isFull(p)
                          ? 'Bỏ chọn'
                          : 'Chọn'
                      "
                      @click="toggleProduct(p)"
                    >

                      <i
                        class="bi"
                        :class="
                          isFull(p)
                            ? 'bi-check2'
                            : 'bi-plus-lg'
                        "
                      ></i>

                    </button>

                  </td>

                </tr>

                <tr
                  v-if="!filteredProducts.length"
                >

                  <td
                    colspan="5"
                    class="ss-empty"
                  >

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

            <h2>
              Sản phẩm & biến thể đã chọn áp dụng
            </h2>

            <p>
              Danh sách chi tiết gồm
              {{ chosenRows.length }}
              biến thể đã chọn
            </p>

          </div>

        </div>

        <div class="list-filter">

          <select
            class="ss-select"
            v-model="listFilter.color"
          >

            <option value="all">
              Tất cả màu sắc
            </option>

            <option
              v-for="c in colors"
              :key="c"
            >
              {{ c }}
            </option>

          </select>

          <select
            class="ss-select"
            v-model="listFilter.size"
          >

            <option value="all">
              Tất cả kích cỡ
            </option>

            <option
              v-for="s in sizes"
              :key="s"
            >
              {{ s }}
            </option>

          </select>

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
                <th class="w-stt c">
                  STT
                </th>

                <th>
                  Sản phẩm
                </th>

                <th>
                  Biến thể
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

                <th
                  class="c"
                  style="width:60px"
                >
                  Xóa
                </th>
              </tr>

            </thead>

            <tbody>

              <tr
                v-for="(x, i) in shownRows"
                :key="x.id"
              >

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

                  <span class="ss-sub">
                    {{ x.color }} · {{ x.size }}
                  </span>

                </td>

                <td class="nowrap">
                  {{ money(x.price) }}
                </td>

                <td class="nowrap">

                  <span class="ss-code">
                    {{
                      money(
                        afterDiscount(x.price)
                      )
                    }}
                  </span>

                </td>

                <td class="r">
                  {{ x.qty }}
                </td>

                <td class="c">

                  <button
                    class="ss-icon-btn danger"
                    title="Xóa"
                    @click="removeVariant(x.id)"
                  >
                    <i class="bi bi-x-lg"></i>
                  </button>

                </td>

              </tr>

              <tr v-if="!shownRows.length">

                <td
                  colspan="7"
                  class="ss-empty"
                >

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
</style>