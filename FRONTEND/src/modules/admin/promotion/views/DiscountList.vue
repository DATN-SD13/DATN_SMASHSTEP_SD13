<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const keyword = ref('')
const form = ref({ type: 'all', start: '', end: '', discount: 'all', status: 'all' })
const rows = ref([
  { code:'VCH65FFTO', name:'Giảm sốc cho giày New Balance', type:'Công khai', value:'60%', start:'16/08/2026', end:'01/09/2026', discount:'Phần trăm (%)', status:'Đang hoạt động' },
  { code:'VCH49OSEQJ', name:'New Balance giảm 25%', type:'Công khai', value:'25%', start:'16/08/2026', end:'01/09/2026', discount:'Phần trăm (%)', status:'Đang hoạt động' },
  { code:'VCH4FNGLO', name:'Puma Running giảm 20%', type:'Công khai', value:'50%', start:'13/08/2026', end:'03/09/2026', discount:'Phần trăm (%)', status:'Đang hoạt động' },
  { code:'VCHWS9DFI', name:'Giảm 50% Air Jordan 1 Low G', type:'Công khai', value:'50%', start:'12/08/2026', end:'01/09/2026', discount:'Phần trăm (%)', status:'Đang hoạt động' },
  { code:'VOUCHER5', name:'Giảm 500K', type:'Cá nhân', value:'500.000đ', start:'01/01/2025', end:'02/01/2036', discount:'Tiền mặt (VNĐ)', status:'Đang hoạt động' }
])

// dd/mm/yyyy -> yyyy-mm-dd để so sánh với <input type="date">
const iso = (d) => d.split('/').reverse().join('-')

const filtered = computed(() => rows.value.filter(x => {
  const q = keyword.value.trim().toLowerCase()
  const f = form.value
  return (!q || x.code.toLowerCase().includes(q) || x.name.toLowerCase().includes(q)) &&
    (f.type === 'all' || x.type === f.type) &&
    (f.discount === 'all' || x.discount === f.discount) &&
    (f.status === 'all' || x.status === f.status) &&
    (!f.start || iso(x.start) >= f.start) &&
    (!f.end || iso(x.end) <= f.end)
}))

const pageSize = ref(5)
const page = ref(1)
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize.value)))
const paged = computed(() => filtered.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
watch([filtered, pageSize], () => { if (page.value > totalPages.value) page.value = 1 })

const isActive = (x) => x.status === 'Đang hoạt động'
function toggle(x) { x.status = isActive(x) ? 'Ngừng hoạt động' : 'Đang hoạt động' }
function reset() { keyword.value = ''; form.value = { type: 'all', start: '', end: '', discount: 'all', status: 'all' } }
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <!-- Bộ lọc -->
      <section class="ss-card">
        <div class="ss-head">
          <div class="ss-head-icon"><i class="bi bi-funnel"></i></div>
          <div><h2>Bộ lọc</h2><p>Tra cứu nhanh dữ liệu.</p></div>
        </div>

        <div class="filter-row">
          <div class="ss-field"><span class="ss-label">Tìm kiếm</span>
            <div class="ss-search"><i class="bi bi-search"></i><input class="ss-input" v-model="keyword" placeholder="Mã, tên phiếu..." /></div>
          </div>
          <div class="ss-field"><span class="ss-label">Hình thức</span>
            <select class="ss-select" v-model="form.type"><option value="all">Tất cả hình thức</option><option>Công khai</option><option>Cá nhân</option></select>
          </div>
          <div class="ss-field"><span class="ss-label">Ngày bắt đầu</span><input class="ss-input" type="date" v-model="form.start" /></div>
          <div class="ss-field"><span class="ss-label">Ngày kết thúc</span><input class="ss-input" type="date" v-model="form.end" /></div>
          <div class="ss-field"><span class="ss-label">Loại giảm</span>
            <select class="ss-select" v-model="form.discount"><option value="all">Tất cả loại giảm</option><option>Phần trăm (%)</option><option>Tiền mặt (VNĐ)</option></select>
          </div>
          <div class="ss-field"><span class="ss-label">Trạng thái</span>
            <select class="ss-select" v-model="form.status"><option value="all">Tất cả trạng thái</option><option>Đang hoạt động</option><option>Ngừng hoạt động</option></select>
          </div>
        </div>

        <div class="ss-actions">
          <button class="ss-btn" @click="reset">Đặt lại bộ lọc</button>
          <button class="ss-btn"><i class="bi bi-file-earmark-excel"></i> Xuất Excel</button>
          <button class="ss-btn primary" @click="router.push('/giam-gia/them')"><i class="bi bi-plus-lg"></i> Tạo phiếu mới</button>
        </div>
      </section>

      <!-- Danh sách -->
      <section class="ss-card">
        <div class="ss-head">
          <h2>Danh sách phiếu giảm giá</h2>
          <span class="ss-spacer"></span>
          <span class="ss-count">{{ filtered.length }} bản ghi hiển thị.</span>
        </div>

        <div class="ss-table-wrap">
          <table class="ss-table">
            <thead>
              <tr>
                <th class="w-stt c">STT</th><th>Mã</th><th>Tên phiếu</th><th>Hình thức</th><th>Giá trị giảm</th>
                <th>Ngày bắt đầu</th><th>Ngày kết thúc</th><th>Trạng thái</th><th>Hành động</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(x, i) in paged" :key="x.code">
                <td class="c">{{ (page - 1) * pageSize + i + 1 }}</td>
                <td><span class="ss-code">{{ x.code }}</span></td>
                <td>{{ x.name }}</td>
                <td><span class="ss-pill" :class="{ warn: x.type === 'Cá nhân' }">{{ x.type }}</span></td>
                <td>{{ x.value }}</td>
                <td class="nowrap">{{ x.start }}</td>
                <td class="nowrap">{{ x.end }}</td>
                <td><span class="ss-pill dot" :class="isActive(x) ? 'success' : 'danger'">{{ x.status }}</span></td>
                <td>
                  <div class="ss-row-actions">
                    <button class="ss-icon-btn" title="Xem chi tiết"><i class="bi bi-eye"></i></button>
                    <button class="ss-icon-btn danger" :title="isActive(x) ? 'Ngừng hoạt động' : 'Kích hoạt'" @click="toggle(x)"><i class="bi bi-power"></i></button>
                  </div>
                </td>
              </tr>
              <tr v-if="!paged.length"><td colspan="9" class="ss-empty"><i class="bi bi-inbox"></i>Không có phiếu giảm giá phù hợp.</td></tr>
            </tbody>
          </table>
        </div>

        <div class="ss-foot">
          <select class="ss-select" v-model.number="pageSize"><option :value="5">5</option><option :value="10">10</option><option :value="20">20</option></select>
          <div class="ss-pages">
            <button :disabled="page === 1" @click="page--"><i class="bi bi-chevron-left"></i></button>
            <button v-for="n in totalPages" :key="n" :class="{ active: n === page }" @click="page = n">{{ n }}</button>
            <button :disabled="page === totalPages" @click="page++"><i class="bi bi-chevron-right"></i></button>
          </div>
        </div>
      </section>
    </main>
  </AdminLayout>
</template>

<style scoped>
.filter-row { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 10px; }
@media (max-width: 1100px) { .filter-row { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 640px) { .filter-row { grid-template-columns: 1fr; } .ss-actions > * { flex: 1; } }
</style>
