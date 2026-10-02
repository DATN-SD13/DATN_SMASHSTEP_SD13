<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const form = ref({ code: 'DGGD5OY17', name: '', value: 0, start: '2026-08-16', end: '', desc: '' })

/* ---------- Dữ liệu mẫu: sản phẩm + biến thể ---------- */
const v = (code, color, size, price) => ({ code, color, size, price, qty: 100 })
const products = ref([
  { code: 'G66748', name: 'ASICS GEL-Kayano 31', variants: [v('G66748-MS05-37', 'Xám', '37', 3790000), v('G66748-MS05-36', 'Xám', '36', 3790000), v('G66748-MS01-37', 'Đen', '37', 3790000), v('G66748-MS01-36', 'Đen', '36', 3790000)] },
  { code: 'G57664', name: 'New Balance 530', variants: [v('G57664-WH-40', 'Trắng', '40', 2490000), v('G57664-WH-41', 'Trắng', '41', 2490000)] },
  { code: 'G86428', name: 'Air Jordan 1 Low G', variants: [v('G86428-RD-42', 'Đỏ', '42', 3250000), v('G86428-RD-41', 'Đỏ', '41', 3250000)] },
  { code: 'SP20', name: 'Brooks Ghost 14', variants: [v('SP20-GR-41', 'Xám', '41', 2890000), v('SP20-GR-42', 'Xám', '42', 2890000)] },
  { code: 'SP19', name: 'Hoka Clifton 8', variants: [v('SP19-BL-40', 'Xanh', '40', 3190000), v('SP19-BL-41', 'Xanh', '41', 3190000)] }
])
const allVariants = computed(() => products.value.flatMap(p => p.variants.map(x => ({ ...x, product: p.name, productCode: p.code }))))
const colors = computed(() => [...new Set(allVariants.value.map(x => x.color))])
const sizes = computed(() => [...new Set(allVariants.value.map(x => x.size))].sort())

/* ---------- Bảng chọn sản phẩm ---------- */
const draft = reactive({ q: '', color: 'all', size: 'all' })
const applied = reactive({ q: '', color: 'all', size: 'all' })
function applySearch() { Object.assign(applied, draft) }

const filteredProducts = computed(() => {
  const q = applied.q.trim().toLowerCase()
  return products.value.filter(p =>
    (!q || p.code.toLowerCase().includes(q) || p.name.toLowerCase().includes(q)) &&
    p.variants.some(x => (applied.color === 'all' || x.color === applied.color) && (applied.size === 'all' || x.size === applied.size))
  )
})

const selected = ref([]) // danh sách mã biến thể đã chọn
const isChosen = (p) => p.variants.some(x => selected.value.includes(x.code))
const isFull = (p) => p.variants.every(x => selected.value.includes(x.code))
function toggleProduct(p) {
  const codes = p.variants.map(x => x.code)
  selected.value = isFull(p)
    ? selected.value.filter(c => !codes.includes(c))
    : [...new Set([...selected.value, ...codes])]
}
const allChecked = computed(() => filteredProducts.value.length > 0 && filteredProducts.value.every(isFull))
function toggleAll() {
  const codes = filteredProducts.value.flatMap(p => p.variants.map(x => x.code))
  selected.value = allChecked.value
    ? selected.value.filter(c => !codes.includes(c))
    : [...new Set([...selected.value, ...codes])]
}
function removeVariant(code) { selected.value = selected.value.filter(c => c !== code) }

/* ---------- Bảng biến thể đã chọn ---------- */
const listFilter = reactive({ q: '', color: 'all', size: 'all' })
const chosenRows = computed(() => allVariants.value.filter(x => selected.value.includes(x.code)))
const shownRows = computed(() => {
  const q = listFilter.q.trim().toLowerCase()
  return chosenRows.value.filter(x =>
    (!q || x.code.toLowerCase().includes(q) || x.product.toLowerCase().includes(q)) &&
    (listFilter.color === 'all' || x.color === listFilter.color) &&
    (listFilter.size === 'all' || x.size === listFilter.size))
})
const afterDiscount = (price) => Math.round(price * (1 - Math.min(100, Math.max(0, Number(form.value.value) || 0)) / 100))
const money = (n) => `${Number(n || 0).toLocaleString('vi-VN')} đ`

function save() { alert('Đã tạo đợt giảm giá'); router.push('/dot-giam-gia') }
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <div class="top-grid">
        <!-- Thông tin đợt giảm -->
        <section class="ss-card ss-form">
          <div class="ss-head">
            <div class="ss-head-icon"><i class="bi bi-tag"></i></div>
            <h2>Thông tin đợt giảm</h2>
          </div>

          <div class="ss-field"><label class="ss-label">Mã đợt <span class="req">*</span></label><input class="ss-input" v-model="form.code" /></div>
          <div class="ss-field"><label class="ss-label">Tên đợt <span class="req">*</span></label><input class="ss-input" v-model="form.name" placeholder="Ví dụ: Siêu giảm giá mùa hè" /></div>
          <div class="ss-field"><label class="ss-label">Giá trị giảm (%) <span class="req">*</span></label><input class="ss-input" type="number" min="0" max="100" v-model="form.value" /></div>
          <div class="two">
            <div class="ss-field"><label class="ss-label">Từ ngày <span class="req">*</span></label><input class="ss-input" type="date" v-model="form.start" /></div>
            <div class="ss-field"><label class="ss-label">Đến ngày <span class="req">*</span></label><input class="ss-input" type="date" v-model="form.end" /></div>
          </div>
          <div class="ss-field"><label class="ss-label">Mô tả</label><textarea class="ss-textarea" v-model="form.desc" placeholder="Nhập mô tả..."></textarea></div>

          <button class="ss-btn primary block" @click="save"><i class="bi bi-check2"></i> Tạo đợt giảm giá</button>
          <button class="ss-btn block" @click="router.back()">Hủy</button>
        </section>

        <!-- Chọn sản phẩm áp dụng -->
        <section class="ss-card">
          <div class="ss-head">
            <div class="ss-head-icon"><i class="bi bi-search"></i></div>
            <div><h2>Chọn sản phẩm áp dụng</h2><p>Đã chọn {{ selected.length }} biến thể</p></div>
          </div>

          <div class="pick-filter">
            <div class="ss-search"><i class="bi bi-search"></i><input class="ss-input" v-model="draft.q" placeholder="Tìm theo tên hoặc mã sản phẩm..." @keyup.enter="applySearch" /></div>
            <div class="ss-field"><span class="ss-label strong">Màu sắc</span>
              <select class="ss-select" v-model="draft.color"><option value="all">Tất cả màu sắc</option><option v-for="c in colors" :key="c">{{ c }}</option></select>
            </div>
            <div class="ss-field"><span class="ss-label strong">Kích cỡ</span>
              <select class="ss-select" v-model="draft.size"><option value="all">Tất cả kích cỡ</option><option v-for="s in sizes" :key="s">{{ s }}</option></select>
            </div>
            <button class="ss-btn primary" @click="applySearch"><i class="bi bi-search"></i> Tìm kiếm</button>
          </div>

          <div class="ss-table-wrap">
            <table class="ss-table" style="min-width:520px">
              <thead>
                <tr><th style="width:44px" class="c"><input type="checkbox" :checked="allChecked" @change="toggleAll" /></th><th class="w-stt c">STT</th><th>Mã SP</th><th>Tên sản phẩm</th><th class="r" style="width:70px"></th></tr>
              </thead>
              <tbody>
                <tr v-for="(p, i) in filteredProducts" :key="p.code">
                  <td class="c"><input type="checkbox" :checked="isFull(p)" @change="toggleProduct(p)" /></td>
                  <td class="c">{{ i + 1 }}</td>
                  <td>{{ p.code }}</td>
                  <td>{{ p.name }}</td>
                  <td class="r"><button class="ss-icon-btn" :class="{ chosen: isChosen(p) }" :title="isFull(p) ? 'Bỏ chọn' : 'Chọn'" @click="toggleProduct(p)"><i class="bi" :class="isFull(p) ? 'bi-check2' : 'bi-plus-lg'"></i></button></td>
                </tr>
                <tr v-if="!filteredProducts.length"><td colspan="5" class="ss-empty"><i class="bi bi-inbox"></i>Không tìm thấy sản phẩm.</td></tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>

      <!-- Biến thể đã chọn -->
      <section class="ss-card">
        <div class="ss-head">
          <div class="ss-head-icon"><i class="bi bi-check2-square"></i></div>
          <div><h2>Sản phẩm &amp; biến thể đã chọn áp dụng</h2><p>Danh sách chi tiết gồm {{ chosenRows.length }} biến thể đã chọn</p></div>
        </div>

        <div class="list-filter">
          <select class="ss-select" v-model="listFilter.color"><option value="all">Tất cả màu sắc</option><option v-for="c in colors" :key="c">{{ c }}</option></select>
          <select class="ss-select" v-model="listFilter.size"><option value="all">Tất cả kích cỡ</option><option v-for="s in sizes" :key="s">{{ s }}</option></select>
          <input class="ss-input" v-model="listFilter.q" placeholder="Tìm trong danh sách..." />
        </div>

        <div class="ss-table-wrap">
          <table class="ss-table">
            <thead>
              <tr><th class="w-stt c">STT</th><th>Sản phẩm</th><th>Biến thể</th><th>Giá bán</th><th>Giá sau giảm</th><th class="r">Số lượng</th><th class="c" style="width:60px">Xóa</th></tr>
            </thead>
            <tbody>
              <tr v-for="(x, i) in shownRows" :key="x.code">
                <td class="c">{{ i + 1 }}</td>
                <td><span class="ss-strong">{{ x.product }}</span><span class="ss-sub">{{ x.productCode }}</span></td>
                <td>{{ x.code }}<span class="ss-sub">{{ x.color }} · {{ x.size }}</span></td>
                <td class="nowrap">{{ money(x.price) }}</td>
                <td class="nowrap"><span class="ss-code">{{ money(afterDiscount(x.price)) }}</span></td>
                <td class="r">{{ x.qty }}</td>
                <td class="c"><button class="ss-icon-btn danger" title="Xóa" @click="removeVariant(x.code)"><i class="bi bi-x-lg"></i></button></td>
              </tr>
              <tr v-if="!shownRows.length"><td colspan="7" class="ss-empty"><i class="bi bi-inbox"></i>Chưa có biến thể nào được chọn.</td></tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  </AdminLayout>
</template>

<style scoped>
.top-grid { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 16px; align-items: start; }
.two { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.pick-filter { display: grid; grid-template-columns: minmax(0, 1fr) 150px 150px auto; gap: 10px; align-items: end; }
.pick-filter > .ss-btn { height: 40px; }
.list-filter { display: grid; grid-template-columns: 170px 170px 240px; justify-content: end; gap: 10px; }
.ss-icon-btn.chosen { background: var(--ss-primary); border-color: var(--ss-primary); color: #fff; }
input[type="checkbox"] { width: 15px; height: 15px; accent-color: var(--ss-primary); cursor: pointer; }
@media (max-width: 1100px) { .top-grid { grid-template-columns: 1fr; } }
@media (max-width: 760px) { .pick-filter, .list-filter { grid-template-columns: 1fr; } }
</style>
