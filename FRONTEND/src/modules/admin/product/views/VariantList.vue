<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import SsPager from '../../../../components/SsPager.vue'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePaging, money } from '../../../../utils/paging'
import { products } from '../services/productData'

const route = useRoute()
const router = useRouter()
const product = computed(() => products.find(p => p.code === route.query.sp) || products[products.length - 1])
const rows = ref(product.value.variants.map(v => ({ ...v })))
const f = ref({ q: '', color: 'all', size: 'all', status: 'all' })

const colors = computed(() => [...new Set(rows.value.map(v => v.color))])
const sizes = computed(() => [...new Set(rows.value.map(v => v.size))])
const filtered = computed(() => rows.value.filter(v => {
  const q = f.value.q.trim().toLowerCase()
  return (!q || v.code.toLowerCase().includes(q) || v.color.toLowerCase().includes(q)) &&
    (f.value.color === 'all' || v.color === f.value.color) &&
    (f.value.size === 'all' || v.size === f.value.size) &&
    (f.value.status === 'all' || v.status === f.value.status)
}))
const { page, size, pages, paged, offset } = usePaging(filtered, 10)

const selected = ref([])
const allOn = computed(() => paged.value.length > 0 && paged.value.every(v => selected.value.includes(v.code)))
const toggleAll = () => { selected.value = allOn.value ? [] : paged.value.map(v => v.code) }
const after = (v) => Math.round(v.price * (1 - v.discount / 100))
const on = (v) => v.status === 'Đang bán'
const toggle = (v) => { v.status = on(v) ? 'Ngừng bán' : 'Đang bán' }
const reset = () => { f.value = { q: '', color: 'all', size: 'all', status: 'all' } }
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <section class="ss-card">
        <div class="ss-head">
          <div><h2 class="blue">Biến thể của: {{ product.name }}</h2><p>Mã sản phẩm: {{ rows[0]?.pc }}</p></div>
          <span class="ss-spacer"></span>
          <button class="ss-btn primary" @click="router.push('/san-pham/them')"><i class="bi bi-plus-lg"></i> Tạo thêm biến thể</button>
        </div>
        <div class="ss-toolbar">
          <div class="ss-search grow"><i class="bi bi-search"></i><input class="ss-input" v-model="f.q" placeholder="Nhập mã sản phẩm, phân loại, màu sắc..." /></div>
          <button class="ss-btn" @click="reset">Đặt lại bộ lọc</button>
          <button class="ss-btn"><i class="bi bi-qr-code"></i> Tải QR</button>
          <button class="ss-btn"><i class="bi bi-file-earmark-excel"></i> Xuất Excel</button>
        </div>
        <div class="row3">
          <select class="ss-select" v-model="f.color"><option value="all">Tất cả màu sắc</option><option v-for="c in colors" :key="c">{{ c }}</option></select>
          <select class="ss-select" v-model="f.size"><option value="all">Tất cả kích cỡ</option><option v-for="c in sizes" :key="c">{{ c }}</option></select>
          <select class="ss-select" v-model="f.status"><option value="all">Tất cả trạng thái</option><option>Đang bán</option><option>Ngừng bán</option></select>
        </div>
      </section>

      <section class="ss-card">
        <div class="ss-table-wrap">
          <table class="ss-table">
            <thead><tr><th style="width:40px" class="c"><input class="ss-cb" type="checkbox" :checked="allOn" @change="toggleAll" /></th><th class="w-stt c">STT</th><th>Mã SP</th><th>Mã CTSP</th><th>Ảnh</th><th>Màu sắc</th><th class="c">Kích cỡ</th><th class="c">Số lượng</th><th>Giá bán</th><th>Giảm</th><th>Trạng thái</th><th>Hành động</th></tr></thead>
            <tbody>
              <tr v-for="(v, i) in paged" :key="v.code">
                <td class="c"><input class="ss-cb" type="checkbox" :value="v.code" v-model="selected" /></td>
                <td class="c">{{ offset + i + 1 }}</td>
                <td class="ss-strong">{{ v.pc }}</td>
                <td class="ss-strong nowrap">{{ v.code }}</td>
                <td><span class="ss-thumb"><i class="bi bi-image"></i></span></td>
                <td class="nowrap"><span class="ss-dot" :style="{ background: v.hex }"></span>{{ v.color }}</td>
                <td class="c">{{ v.size }}</td>
                <td class="c">{{ v.qty }}</td>
                <td class="nowrap"><span class="ss-code">{{ money(after(v)) }}</span><span class="ss-old">{{ money(v.price) }}</span></td>
                <td><span class="ss-pill success">{{ v.discount }}%</span></td>
                <td><span class="ss-pill dot" :class="on(v) ? 'success' : 'danger'">{{ v.status }}</span></td>
                <td><div class="ss-row-actions">
                  <button class="ss-icon-btn" title="Xem"><i class="bi bi-eye"></i></button>
                  <button class="ss-icon-btn" title="Sửa"><i class="bi bi-pencil"></i></button>
                  <button class="ss-icon-btn danger" :title="on(v) ? 'Ngừng bán' : 'Bán lại'" @click="toggle(v)"><i class="bi bi-power"></i></button>
                </div></td>
              </tr>
              <tr v-if="!paged.length"><td colspan="12" class="ss-empty"><i class="bi bi-inbox"></i>Không có biến thể phù hợp.</td></tr>
            </tbody>
          </table>
        </div>
        <SsPager v-model:page="page" v-model:size="size" :pages="pages" />
      </section>
    </main>
  </AdminLayout>
</template>

<style scoped>
.blue { color: var(--ss-primary) !important; }
.row3 { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
@media (max-width: 760px) { .row3 { grid-template-columns: 1fr; } }
</style>
