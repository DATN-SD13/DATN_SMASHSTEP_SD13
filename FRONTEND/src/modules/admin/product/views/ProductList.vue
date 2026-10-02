<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import SsPager from '../../../../components/SsPager.vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { usePaging, money } from '../../../../utils/paging'
import { products as seed, brands, shoeTypes } from '../services/productData'

const router = useRouter()
const rows = ref(seed.map(p => ({ ...p })))
const f = ref({ q: '', brand: 'all', type: 'all', status: 'all' })

const filtered = computed(() => rows.value.filter(p => {
  const q = f.value.q.trim().toLowerCase()
  return (!q || p.code.toLowerCase().includes(q) || p.name.toLowerCase().includes(q)) &&
    (f.value.brand === 'all' || p.brand === f.value.brand) &&
    (f.value.type === 'all' || p.type === f.value.type) &&
    (f.value.status === 'all' || p.status === f.value.status)
}))
const { page, size, pages, paged, offset } = usePaging(filtered, 10)

const qty = (p) => p.variants.reduce((s, v) => s + v.qty, 0)
const colors = (p) => new Set(p.variants.map(v => v.color)).size
const sizes = (p) => new Set(p.variants.map(v => v.size)).size
const on = (p) => p.status === 'Kinh doanh'
const toggle = (p) => { p.status = on(p) ? 'Ngừng kinh doanh' : 'Kinh doanh' }
const reset = () => { f.value = { q: '', brand: 'all', type: 'all', status: 'all' } }
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <section class="ss-card">
        <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-funnel"></i></div><h2>Bộ lọc</h2></div>

        <div class="ss-toolbar">
          <div class="ss-search grow"><i class="bi bi-search"></i><input class="ss-input" v-model="f.q" placeholder="Tìm theo mã SP / tên sản phẩm..." /></div>
          <button class="ss-btn" @click="reset">Đặt lại bộ lọc</button>
          <button class="ss-btn"><i class="bi bi-file-earmark-excel"></i> Xuất Excel</button>
          <button class="ss-btn primary" @click="router.push('/san-pham/them')"><i class="bi bi-plus-lg"></i> Thêm sản phẩm</button>
        </div>

        <div class="row3">
          <div class="ss-field"><span class="ss-label">Thương hiệu</span>
            <select class="ss-select" v-model="f.brand"><option value="all">Tất cả thương hiệu</option><option v-for="b in brands" :key="b">{{ b }}</option></select></div>
          <div class="ss-field"><span class="ss-label">Loại giày</span>
            <select class="ss-select" v-model="f.type"><option value="all">Tất cả loại giày</option><option v-for="t in shoeTypes" :key="t">{{ t }}</option></select></div>
          <div class="ss-field"><span class="ss-label">Trạng thái</span>
            <select class="ss-select" v-model="f.status"><option value="all">Tất cả trạng thái</option><option>Kinh doanh</option><option>Ngừng kinh doanh</option></select></div>
        </div>
      </section>

      <section class="ss-card">
        <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-box-seam"></i></div><h2>Danh sách sản phẩm</h2><span class="ss-spacer"></span><span class="ss-count">{{ filtered.length }} bản ghi hiển thị.</span></div>

        <div class="ss-table-wrap">
          <table class="ss-table">
            <thead><tr><th class="w-stt c">STT</th><th>Mã SP</th><th>Tên SP</th><th>Thương hiệu</th><th>Loại giày</th><th class="r">Số lượng</th><th>Giá bán</th><th>Trạng thái</th><th>Hành động</th></tr></thead>
            <tbody>
              <tr v-for="(p, i) in paged" :key="p.code">
                <td class="c">{{ offset + i + 1 }}</td>
                <td><span class="ss-code">{{ p.code }}</span></td>
                <td><div class="ss-cell"><span class="ss-thumb"><i class="bi bi-image"></i></span>
                  <div><span class="ss-strong">{{ p.name }}</span><span class="ss-sub">{{ colors(p) }} màu · {{ sizes(p) }} size</span></div></div></td>
                <td>{{ p.brand }}</td>
                <td>{{ p.type }}</td>
                <td class="r">{{ qty(p) }}</td>
                <td class="nowrap"><span class="ss-code">{{ money(p.price) }}</span><span class="ss-old">{{ money(p.old) }}</span></td>
                <td><span class="ss-pill dot" :class="on(p) ? 'success' : 'danger'">{{ p.status }}</span></td>
                <td><div class="ss-row-actions">
                  <button class="ss-icon-btn" title="Xem biến thể" @click="router.push(`/bien-the-san-pham?sp=${p.code}`)"><i class="bi bi-eye"></i></button>
                  <button class="ss-icon-btn danger" :title="on(p) ? 'Ngừng kinh doanh' : 'Kinh doanh lại'" @click="toggle(p)"><i class="bi bi-power"></i></button>
                </div></td>
              </tr>
              <tr v-if="!paged.length"><td colspan="9" class="ss-empty"><i class="bi bi-inbox"></i>Không có sản phẩm phù hợp.</td></tr>
            </tbody>
          </table>
        </div>
        <SsPager v-model:page="page" v-model:size="size" :pages="pages" />
      </section>
    </main>
  </AdminLayout>
</template>

<style scoped>
.row3 { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
@media (max-width: 760px) { .row3 { grid-template-columns: 1fr; } }
</style>
