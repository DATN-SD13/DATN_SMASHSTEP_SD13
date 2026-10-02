<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const keyword = ref('')
const status = ref('all')
const start = ref('')
const end = ref('')
const rows = ref([
  { code:'DGGRYCXHG', name:'New Balance Back To School', value:'20%', start:'16/08/2026', end:'31/08/2026', status:'Đang hoạt động' },
  { code:'DGGFQ10AF', name:'Puma Running giảm 20%', value:'20%', start:'13/08/2026', end:'31/08/2026', status:'Đang hoạt động' },
  { code:'DGGAG86WH', name:'Air Jordan 1 Low G - Summer Sale', value:'30%', start:'12/08/2026', end:'31/08/2026', status:'Ngừng hoạt động' },
  { code:'DGG_WINTER', name:'Sale mùa đông', value:'20%', start:'01/11/2025', end:'31/12/2026', status:'Đang hoạt động' },
  { code:'DGG_SUMMER', name:'Siêu sale mùa hè', value:'15%', start:'01/06/2025', end:'31/12/2026', status:'Đang hoạt động' }
])

const iso = (d) => d.split('/').reverse().join('-')
const filtered = computed(() => rows.value.filter(x => {
  const q = keyword.value.trim().toLowerCase()
  return (!q || x.code.toLowerCase().includes(q) || x.name.toLowerCase().includes(q) || x.value.includes(q)) &&
    (status.value === 'all' || x.status === status.value) &&
    (!start.value || iso(x.start) >= start.value) &&
    (!end.value || iso(x.end) <= end.value)
}))

const pageSize = ref(5)
const page = ref(1)
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize.value)))
const paged = computed(() => filtered.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))
watch([filtered, pageSize], () => { if (page.value > totalPages.value) page.value = 1 })

const isActive = (x) => x.status === 'Đang hoạt động'
function toggle(x) { x.status = isActive(x) ? 'Ngừng hoạt động' : 'Đang hoạt động' }
function reset() { keyword.value = ''; status.value = 'all'; start.value = ''; end.value = '' }
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <section class="ss-card">
        <div class="ss-head">
          <div class="ss-head-icon"><i class="bi bi-funnel"></i></div>
          <h2>Bộ lọc</h2>
        </div>

        <div class="filter-row">
          <div class="ss-field"><span class="ss-label">Tìm kiếm</span>
            <div class="ss-search"><i class="bi bi-search"></i><input class="ss-input" v-model="keyword" placeholder="Mã, tên, giá trị..." /></div>
          </div>
          <div class="ss-field"><span class="ss-label">Ngày bắt đầu</span><input class="ss-input" type="date" v-model="start" /></div>
          <div class="ss-field"><span class="ss-label">Ngày kết thúc</span><input class="ss-input" type="date" v-model="end" /></div>
          <div class="ss-field"><span class="ss-label">Trạng thái</span>
            <select class="ss-select" v-model="status"><option value="all">Tất cả trạng thái</option><option>Đang hoạt động</option><option>Ngừng hoạt động</option></select>
          </div>
        </div>

        <div class="ss-actions">
          <button class="ss-btn" @click="reset">Đặt lại bộ lọc</button>
          <button class="ss-btn"><i class="bi bi-file-earmark-excel"></i> Xuất Excel</button>
          <button class="ss-btn primary" @click="router.push('/dot-giam-gia/them')"><i class="bi bi-plus-lg"></i> Tạo đợt giảm giá</button>
        </div>
      </section>

      <section class="ss-card">
        <div class="ss-head">
          <h2>Danh sách các đợt giảm giá</h2>
          <span class="ss-spacer"></span>
          <span class="ss-count">{{ filtered.length }} bản ghi hiển thị.</span>
        </div>

        <div class="ss-table-wrap">
          <table class="ss-table">
            <thead>
              <tr><th class="w-stt c">STT</th><th>Mã</th><th>Tên</th><th>Giá trị</th><th>Ngày bắt đầu</th><th>Ngày kết thúc</th><th>Trạng thái</th><th>Hành động</th></tr>
            </thead>
            <tbody>
              <tr v-for="(x, i) in paged" :key="x.code">
                <td class="c">{{ (page - 1) * pageSize + i + 1 }}</td>
                <td><span class="ss-code">{{ x.code }}</span></td>
                <td>{{ x.name }}</td>
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
              <tr v-if="!paged.length"><td colspan="8" class="ss-empty"><i class="bi bi-inbox"></i>Không có đợt giảm giá phù hợp.</td></tr>
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
.filter-row { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; }
@media (max-width: 1000px) { .filter-row { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) { .filter-row { grid-template-columns: 1fr; } .ss-actions > * { flex: 1; } }
</style>
