<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import SsPager from '../../../../components/SsPager.vue'
import Select2 from '../../promotion/components/Select2.vue'
import scheduleService from '../services/scheduleService'
import { confirmAction, showSuccess, showError } from '../../../../utils/feedback'
import api from '../../../../utils/api'

// ── State ─────────────────────────────────────────────────
const configured = ref(false), checking = ref(true)
const configurationMessage = 'Lịch làm việc chưa khả dụng trong cấu hình hiện tại.'
const rows          = ref([])
const shifts        = ref([])
const employees     = ref([])
const loading       = ref(false)
const error         = ref('')
const page          = ref(1)
const size          = ref(10)
const pages         = ref(1)
const totalElements = ref(0)
const viewMode      = ref('bang')

const f = reactive({ tuKhoa: '', caId: '', ngayLam: '' })

// ── Select2 options ───────────────────────────────────────
const shiftOptions = computed(() => [
  { value: '', label: 'Tất cả ca' },
  ...shifts.value.map(s => ({ value: s.id, label: `${s.tenCa} (${s.thoiGian})` }))
])
const shiftOptionsActive = computed(() =>
  shifts.value.filter(s => s.trangThai === 1)
    .map(s => ({ value: s.id, label: `${s.tenCa} (${s.thoiGian})` }))
)

// ── Trạng thái ────────────────────────────────────────────
const STATUS = {
  0: { label: 'Chờ xác nhận', cls: 'warn' },
  1: { label: 'Đã xác nhận',  cls: 'success' },
  2: { label: 'Vắng mặt',     cls: 'danger' },
  3: { label: 'Hoàn thành',   cls: '' }
}

// ── Load bảng ─────────────────────────────────────────────
async function load() {
  if (!configured.value) return
  loading.value = true; error.value = ''
  try {
    const params = { page: page.value, size: size.value }
    if (f.tuKhoa)  params.tuKhoa  = f.tuKhoa
    if (f.caId)    params.caId    = f.caId
    if (f.ngayLam) { params.tuNgay = f.ngayLam; params.denNgay = f.ngayLam }
    const data      = await scheduleService.getLichLamViec(params)
    rows.value      = data?.content || []
    pages.value     = Math.max(data?.totalPages || 1, 1)
    totalElements.value = data?.totalElements || 0
  } catch (e) {
    error.value = e?.response?.data?.message || 'Không thể tải dữ liệu'
  } finally { loading.value = false }
}

async function loadShifts() {
  shifts.value = (await scheduleService.getCaLamViec()) || []
}

async function loadEmployees() {
  if (employees.value.length) return
  try {
    let result = [], p = 1
    while (true) {
      const res = await api.get('/nhan-vien', { params: { trangThai: 1, page: p, size: 100 } })
      const d = res.data?.data
      result.push(...(d?.content || []))
      if (p >= (d?.totalPages || 1)) break
      p++
    }
    employees.value = result
  } catch { /* ignore */ }
}

function reset() { f.tuKhoa = ''; f.caId = ''; f.ngayLam = ''; page.value = 1; load() }
watch(page, load)
watch(size, () => { page.value = 1; load() })
onMounted(async () => {
  try {
    const result = await scheduleService.capabilities()
    configured.value = result?.available === true
    if (configured.value) await Promise.all([loadShifts(), load()])
  } catch (e) {
    configured.value = false
    error.value = e?.response?.data?.message || configurationMessage
  } finally { checking.value = false }
})

// ── Actions bảng ──────────────────────────────────────────
function handleDelete(row) {
  confirmAction(`Xóa lịch làm của ${row.tenNhanVien} ngày ${row.ngayLam}?`, async () => {
    try { await scheduleService.deleteLich(row.id); showSuccess('Đã xóa.'); load() }
    catch (e) { showError(e?.response?.data?.message || 'Xóa thất bại') }
  })
}

// ── Calendar ──────────────────────────────────────────────
const calMode   = ref('tuan')
const weekStart = ref(getMonday(new Date()))
const calRows   = ref([])

function getMonday(d) {
  const date = new Date(d); const day = date.getDay()
  date.setDate(date.getDate() + (day === 0 ? -6 : 1 - day))
  date.setHours(0,0,0,0); return date
}
function toYMD(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
function fmtDate(d) {
  return d.toLocaleDateString('vi-VN', { day:'2-digit', month:'long', year:'numeric' })
}

const weekDays = computed(() =>
  Array.from({ length: 7 }, (_, i) => {
    const d = new Date(weekStart.value); d.setDate(d.getDate() + i)
    return {
      ymd:     toYMD(d),
      label:   ['CN','T2','T3','T4','T5','T6','T7'][d.getDay()],
      display: d.toLocaleDateString('vi-VN', { day:'2-digit', month:'2-digit', year:'numeric' }),
      isToday: toYMD(d) === toYMD(new Date())
    }
  })
)
const weekLabel = computed(() =>
  `${fmtDate(weekStart.value)} - ${fmtDate((() => { const d=new Date(weekStart.value); d.setDate(d.getDate()+6); return d })())}`
)

const calMap = computed(() => {
  const map = {}
  for (const r of calRows.value) {
    const key = `${r.caLamViecId}|${r.ngayLam}`
    if (!map[key]) map[key] = []
    map[key].push(r)
  }
  return map
})
function calCell(shiftId, ymd) { return calMap.value[`${shiftId}|${ymd}`] || [] }

const SHIFT_COLORS = ['#fff0f0','#fff8e1','#f0f7ff']

// ── Lịch ngày ────────────────────────────────────────────
const currentDay = ref((() => { const d = new Date(); d.setHours(0,0,0,0); return d })())
const dayLabel = computed(() =>
  currentDay.value.toLocaleDateString('vi-VN', { day:'2-digit', month:'long', year:'numeric' })
)
function prevDay() { const d=new Date(currentDay.value); d.setDate(d.getDate()-1); currentDay.value=d; loadCalendar() }
function nextDay() { const d=new Date(currentDay.value); d.setDate(d.getDate()+1); currentDay.value=d; loadCalendar() }
const dayRows = computed(() =>
  shifts.value.filter(s=>s.trangThai===1).map(shift => ({
    shift, list: calCell(shift.id, toYMD(currentDay.value)), ymd: toYMD(currentDay.value)
  }))
)

// ── Lịch tháng ───────────────────────────────────────────
const currentMonth = ref(new Date())
const monthLabel = computed(() => {
  const s = currentMonth.value.toLocaleDateString('vi-VN', { month:'long', year:'numeric' })
  return s.charAt(0).toUpperCase() + s.slice(1)
})
function prevMonth() { const d=new Date(currentMonth.value); d.setMonth(d.getMonth()-1); currentMonth.value=d; loadCalendar() }
function nextMonth() { const d=new Date(currentMonth.value); d.setMonth(d.getMonth()+1); currentMonth.value=d; loadCalendar() }

const monthGrid = computed(() => {
  const year=currentMonth.value.getFullYear(), month=currentMonth.value.getMonth()
  const first=new Date(year,month,1), last=new Date(year,month+1,0)
  let startDow=first.getDay(); if(startDow===0) startDow=7
  const cells=[]
  for(let i=startDow-1;i>0;i--){ const d=new Date(first); d.setDate(d.getDate()-i); cells.push({date:d,ymd:toYMD(d),cur:false}) }
  for(let d=1;d<=last.getDate();d++){ const date=new Date(year,month,d); cells.push({date,ymd:toYMD(date),cur:true}) }
  const rem=cells.length%7; if(rem>0){ for(let i=1;i<=7-rem;i++){ const date=new Date(last); date.setDate(date.getDate()+i); cells.push({date,ymd:toYMD(date),cur:false}) } }
  const rows=[]; for(let i=0;i<cells.length;i+=7) rows.push(cells.slice(i,i+7))
  return rows
})

function monthCellCount(ymd) {
  return shifts.value.filter(s=>s.trangThai===1).reduce((sum,shift)=>sum+calCell(shift.id,ymd).length,0)
}

// ── Load calendar (chung cho ngày/tuần/tháng) ─────────────
async function loadCalendar() {
  if (!configured.value) return
  let from, to
  if (calMode.value==='ngay') {
    from=to=toYMD(currentDay.value)
  } else if (calMode.value==='tuan') {
    from=toYMD(weekStart.value)
    const e=new Date(weekStart.value); e.setDate(e.getDate()+6); to=toYMD(e)
  } else {
    const y=currentMonth.value.getFullYear(), m=currentMonth.value.getMonth()
    from=toYMD(new Date(y,m,1)); to=toYMD(new Date(y,m+1,0))
  }
  try {
    let page = 1, totalPages = 1
    const result = []
    do {
      const data = await scheduleService.getLichLamViec({ tuNgay:from, denNgay:to, page, size:100 })
      result.push(...(data?.content || []))
      totalPages = data?.totalPages || 1
      page++
    } while (page <= totalPages)
    calRows.value = result
  } catch { /* ignore */ }
}

function prevNav() { if(calMode.value==='ngay') prevDay(); else if(calMode.value==='tuan') prevWeek(); else prevMonth() }
function nextNav() { if(calMode.value==='ngay') nextDay(); else if(calMode.value==='tuan') nextWeek(); else nextMonth() }

const navLabel = computed(() => {
  if(calMode.value==='ngay') return dayLabel.value
  if(calMode.value==='tuan') return weekLabel.value
  return monthLabel.value
})

function prevWeek() { weekStart.value=new Date(weekStart.value.getTime()-7*86400000); loadCalendar() }
function nextWeek() { weekStart.value=new Date(weekStart.value.getTime()+7*86400000); loadCalendar() }
watch(viewMode, v => { if(v==='lich') loadCalendar() })
watch(calMode, () => loadCalendar())

// ── Modal chi tiết ô lịch ─────────────────────────────────
const calDetail = ref(null)
function openCalDetail(shift, day) {
  calDetail.value = { shift, day, list: calCell(shift.id, day.ymd) }
}

// ── Modal phân ca ─────────────────────────────────────────
const showModal = ref(false)
const saving    = ref(false)
const formError = ref('')
const empSearch = ref('')
const form = reactive({ maNhanViens: [], caLamViecId: '', ngayLam: '', ghiChu: '' })

const filteredEmps = computed(() => {
  const kw = empSearch.value.trim().toLowerCase()
  if (!kw) return employees.value
  return employees.value.filter(e =>
    (e.code||'').toLowerCase().includes(kw) || (e.name||'').toLowerCase().includes(kw)
  )
})

function openModal() {
  if (!configured.value) return
  form.maNhanViens = []; form.caLamViecId = ''
  form.ngayLam = f.ngayLam || ''; form.ghiChu = ''
  formError.value = ''; empSearch.value = ''
  loadEmployees(); showModal.value = true
}

async function submitModal() {
  formError.value = ''
  if (!form.maNhanViens.length) return (formError.value = 'Chọn ít nhất 1 nhân viên')
  if (!form.caLamViecId)        return (formError.value = 'Chọn ca làm việc')
  if (!form.ngayLam)            return (formError.value = 'Chọn ngày làm')
  saving.value = true
  try {
    await scheduleService.phanCa({
      maNhanViens: form.maNhanViens,
      caLamViecId: Number(form.caLamViecId),
      ngayLam: form.ngayLam, ghiChu: form.ghiChu
    })
    showSuccess(`Đã phân ca cho ${form.maNhanViens.length} nhân viên.`)
    showModal.value = false
    if (viewMode.value === 'lich') loadCalendar(); else load()
  } catch (e) {
    formError.value = e?.response?.data?.message || 'Phân ca thất bại'
  } finally { saving.value = false }
}
</script>

<template>
  <AdminLayout>
    <section v-if="checking" class="ss-page" role="status">Đang tải lịch làm việc...</section>
    <section v-else-if="!configured" class="ss-page"><p class="ss-hint warn" role="status">{{ error || configurationMessage }}</p></section>
    <main v-else class="ss-page">

      <!-- Bộ lọc -->
      <section class="ss-card">
        <div class="ss-head">
          <div class="ss-head-icon"><i class="bi bi-funnel"></i></div>
          <h2>Bộ lọc</h2>
        </div>
        <div class="filter-row">
          <label class="filter-label">Nhân viên</label>
          <input class="ss-input filter-input" v-model="f.tuKhoa"
            placeholder="Tìm kiếm nhân viên..." @keyup.enter="load" />
          <label class="filter-label">Ca làm:</label>
          <div style="width:200px">
            <Select2 v-model="f.caId" :options="shiftOptions"
              placeholder="Tất cả ca" search-placeholder="Tìm ca..." @change="load" />
          </div>
          <label class="filter-label">Ngày làm:</label>
          <input class="ss-input filter-date" type="date" v-model="f.ngayLam" @change="load" />
        </div>
        <div class="ss-actions left" style="gap:8px">
          <button class="ss-btn sm" disabled title="Chưa khả dụng"><i class="bi bi-arrow-left-right"></i> Xếp ca tự động</button>
          <button class="ss-btn sm" @click="reset"><i class="bi bi-arrow-counterclockwise"></i> Đặt lại lịch</button>
          <button class="ss-btn sm" disabled title="Chưa khả dụng"><i class="bi bi-download"></i> Tải template</button>
          <button class="ss-btn sm" disabled title="Chưa khả dụng"><i class="bi bi-file-earmark-excel"></i> Import Excel</button>
          <button class="ss-btn sm primary" @click="openModal">
            <i class="bi bi-plus-lg"></i> Thêm mới lịch làm việc
          </button>
        </div>
      </section>

      <!-- Danh sách / Lịch -->
      <section class="ss-card">
        <div class="ss-head">
          <i class="bi bi-table" style="color:var(--ss-muted);font-size:14px"></i>
          <h2>Danh sách lịch làm việc</h2>
          <span class="ss-spacer"></span>
          <div class="view-toggle">
            <button class="vt-btn" :class="{ active: viewMode==='bang' }" @click="viewMode='bang'">
              <i class="bi bi-table"></i> Bảng
            </button>
            <button class="vt-btn" :class="{ active: viewMode==='lich' }" @click="viewMode='lich'">
              <i class="bi bi-calendar3"></i> Lịch
            </button>
          </div>
        </div>

        <p v-if="error" class="ss-hint warn"><i class="bi bi-exclamation-triangle"></i> {{ error }}</p>

        <!-- Bảng -->
        <template v-if="viewMode==='bang'">
          <div class="ss-table-wrap">
            <table class="ss-table" style="min-width:860px">
              <thead>
                <tr>
                  <th class="w-stt c">STT</th>
                  <th>Mã nhân viên</th>
                  <th>Nhân viên</th>
                  <th>Ca làm</th>
                  <th>Thời gian</th>
                  <th>Ngày làm</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loading">
                  <td colspan="7" class="ss-empty">
                    <i class="bi bi-arrow-repeat spin"></i> Đang tải dữ liệu...
                  </td>
                </tr>
                <template v-else>
                  <tr v-for="(row,i) in rows" :key="row.id">
                    <td class="c">{{ (page-1)*size+i+1 }}</td>
                    <td><span class="ss-code">{{ row.maNhanVien }}</span></td>
                    <td class="ss-strong">{{ row.tenNhanVien }}</td>
                    <td>{{ row.tenCa }}</td>
                    <td class="nowrap">{{ row.thoiGian }}</td>
                    <td class="nowrap">{{ row.ngayLam }}</td>
                    <td>
                      <div class="ss-row-actions">
                        <button class="ss-icon-btn" title="Xem"><i class="bi bi-eye"></i></button>
                        <button class="ss-icon-btn danger" title="Xóa" @click="handleDelete(row)">
                          <i class="bi bi-trash"></i>
                        </button>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="!rows.length">
                    <td colspan="7" class="ss-empty"><i class="bi bi-inbox"></i> Không có dữ liệu.</td>
                  </tr>
                </template>
              </tbody>
            </table>
          </div>
          <SsPager v-model:page="page" v-model:size="size" :pages="pages" />
        </template>

        <!-- Lịch -->
        <template v-else>
          <!-- Nav chung -->
          <div class="cal-nav">
            <button class="ss-icon-btn" @click="prevNav"><i class="bi bi-chevron-left"></i></button>
            <span class="cal-week-label">{{ navLabel }}</span>
            <button class="ss-icon-btn" @click="nextNav"><i class="bi bi-chevron-right"></i></button>
            <span class="ss-spacer"></span>
            <div class="view-toggle">
              <button class="vt-btn" :class="{ active: calMode==='ngay' }"  @click="calMode='ngay'">Ngày</button>
              <button class="vt-btn" :class="{ active: calMode==='tuan' }"  @click="calMode='tuan'">Tuần</button>
              <button class="vt-btn" :class="{ active: calMode==='thang' }" @click="calMode='thang'">Tháng</button>
            </div>
          </div>

          <!-- ── LỊCH NGÀY ── -->
          <template v-if="calMode==='ngay'">
            <div class="cal-wrap">
              <table class="cal-table">
                <thead>
                  <tr>
                    <th class="cal-th-shift">CA LÀM<br>VIỆC</th>
                    <th :class="{ 'cal-th-today': toYMD(currentDay)===toYMD(new Date()) }">
                      <div class="cal-day-lbl">{{ ['CN','T2','T3','T4','T5','T6','T7'][currentDay.getDay()] }}</div>
                      <div class="cal-day-date">{{ currentDay.toLocaleDateString('vi-VN',{day:'2-digit',month:'2-digit',year:'numeric'}) }}</div>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, si) in dayRows" :key="item.shift.id">
                    <td class="cal-td-shift" :style="{ background: SHIFT_COLORS[si%3] }">
                      <div class="cal-shift-name">{{ item.shift.tenCa }}</div>
                      <div class="cal-shift-time">{{ item.shift.thoiGian }}</div>
                    </td>
                    <td class="cal-td-cell" :style="{ background: toYMD(currentDay)===toYMD(new Date()) ? '#fff8f0':'#fff' }">
                      <div class="cal-cell-name">{{ item.shift.tenCa }}</div>
                      <div class="cal-cell-time">{{ item.shift.thoiGian }}</div>
                      <div class="cal-cell-count">{{ item.list.length }} Nhân viên</div>
                      <button class="cal-more-btn"
                        :class="{ 'cal-more-red': toYMD(currentDay)===toYMD(new Date()) }"
                        @click="openCalDetail(item.shift, { ymd: item.ymd, display: currentDay.toLocaleDateString('vi-VN',{day:'2-digit',month:'2-digit',year:'numeric'}) })">
                        Xem thêm
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>

          <!-- ── LỊCH TUẦN ── -->
          <template v-else-if="calMode==='tuan'">
            <div class="cal-wrap">
              <table class="cal-table">
                <thead>
                  <tr>
                    <th class="cal-th-shift">CA LÀM<br>VIỆC</th>
                    <th v-for="day in weekDays" :key="day.ymd" :class="{ 'cal-th-today': day.isToday }">
                      <div class="cal-day-lbl">{{ day.label }}</div>
                      <div class="cal-day-date">{{ day.display }}</div>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(shift, si) in shifts.filter(s=>s.trangThai===1)" :key="shift.id">
                    <td class="cal-td-shift" :style="{ background: SHIFT_COLORS[si%3] }">
                      <div class="cal-shift-name">{{ shift.tenCa }}</div>
                      <div class="cal-shift-time">{{ shift.thoiGian }}</div>
                    </td>
                    <td v-for="day in weekDays" :key="day.ymd"
                      class="cal-td-cell"
                      :style="{ background: day.isToday ? '#fff8f0':'#fff' }">
                      <div class="cal-cell-name">{{ shift.tenCa }}</div>
                      <div class="cal-cell-time">{{ shift.thoiGian }}</div>
                      <div class="cal-cell-count">{{ calCell(shift.id, day.ymd).length }} Nhân viên</div>
                      <button class="cal-more-btn" :class="{ 'cal-more-red': day.isToday }"
                        @click="openCalDetail(shift, day)">Xem thêm</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>

          <!-- ── LỊCH THÁNG ── -->
          <template v-else>
            <div class="cal-wrap">
              <table class="cal-table-month">
                <thead>
                  <tr>
                    <th v-for="lbl in ['T2','T3','T4','T5','T6','T7','CN']" :key="lbl">{{ lbl }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(week, wi) in monthGrid" :key="wi">
                    <td v-for="cell in week" :key="cell.ymd"
                      class="cal-month-cell"
                      :class="{ 'cal-month-other': !cell.cur, 'cal-month-today': cell.ymd===toYMD(new Date()) }">
                      <div class="cal-month-day">
                        <span :class="{ 'today-badge': cell.ymd===toYMD(new Date()) }">
                          {{ cell.date.getDate() }}
                        </span>
                      </div>
                      <div v-if="monthCellCount(cell.ymd) > 0 && cell.cur" class="cal-month-count">
                        {{ monthCellCount(cell.ymd) }} lịch
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </template>
        </template>
      </section>

    </main>

    <!-- Modal phân ca -->
    <Teleport to="body">
      <div v-if="showModal" class="ss-modal-bg" @click.self="showModal=false">
        <div class="ss-modal" style="width:min(600px,100%)">
          <div class="ss-head">
            <div class="ss-head-icon"><i class="bi bi-calendar-plus"></i></div>
            <h2>Thêm mới lịch làm việc</h2>
            <span class="ss-spacer"></span>
            <button class="ss-icon-btn" @click="showModal=false"><i class="bi bi-x-lg"></i></button>
          </div>
          <div class="ss-grid2">
            <div class="ss-field">
              <label class="ss-label">Ca làm việc <span class="req">*</span></label>
              <Select2 v-model="form.caLamViecId" :options="shiftOptionsActive"
                placeholder="-- Chọn ca --" search-placeholder="Tìm ca..." />
            </div>
            <div class="ss-field">
              <label class="ss-label">Ngày làm <span class="req">*</span></label>
              <input class="ss-input" type="date" v-model="form.ngayLam" />
            </div>
            <div class="ss-field full">
              <label class="ss-label">Ghi chú</label>
              <input class="ss-input" v-model="form.ghiChu" placeholder="Tuỳ chọn" />
            </div>
          </div>
          <div class="ss-field">
            <label class="ss-label">
              Nhân viên <span class="req">*</span>
              <span v-if="form.maNhanViens.length" class="badge-count">{{ form.maNhanViens.length }} đã chọn</span>
            </label>
            <div class="ss-search" style="margin-bottom:8px">
              <i class="bi bi-search"></i>
              <input class="ss-input" v-model="empSearch" placeholder="Tìm nhân viên..." />
            </div>
            <div class="emp-list">
              <label v-for="emp in filteredEmps" :key="emp.id" class="emp-item"
                :class="{ selected: form.maNhanViens.includes(emp.code) }">
                <input type="checkbox" class="ss-cb" :value="emp.code" v-model="form.maNhanViens" />
                <span class="ss-code" style="min-width:72px;font-size:11px">{{ emp.code }}</span>
                <span class="ss-strong" style="font-size:12px">{{ emp.name }}</span>
              </label>
              <div v-if="!filteredEmps.length" class="ss-empty" style="height:60px">Không tìm thấy</div>
            </div>
          </div>
          <p v-if="formError" class="ss-hint warn"><i class="bi bi-exclamation-circle"></i> {{ formError }}</p>
          <div class="ss-actions">
            <button class="ss-btn" @click="showModal=false" :disabled="saving">Hủy</button>
            <button class="ss-btn primary" @click="submitModal" :disabled="saving">
              <i :class="saving ? 'bi bi-arrow-repeat spin' : 'bi bi-check2'"></i>
              {{ saving ? 'Đang lưu...' : 'Lưu lịch' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Modal chi tiết ô lịch -->
    <Teleport to="body">
      <div v-if="calDetail" class="ss-modal-bg" @click.self="calDetail=null">
        <div class="ss-modal" style="width:min(480px,100%)">
          <div class="ss-head">
            <div class="ss-head-icon"><i class="bi bi-calendar-day"></i></div>
            <h2>{{ calDetail.shift.tenCa }} — {{ calDetail.day.display }}</h2>
            <span class="ss-spacer"></span>
            <button class="ss-icon-btn" @click="calDetail=null"><i class="bi bi-x-lg"></i></button>
          </div>
          <div v-if="!calDetail.list.length" class="ss-empty" style="height:80px">
            Chưa có nhân viên nào trong ca này.
          </div>
          <div v-else class="ss-table-wrap">
            <table class="ss-table">
              <thead><tr><th>Mã NV</th><th>Họ tên</th><th>Trạng thái</th></tr></thead>
              <tbody>
                <tr v-for="r in calDetail.list" :key="r.id">
                  <td><span class="ss-code">{{ r.maNhanVien }}</span></td>
                  <td class="ss-strong">{{ r.tenNhanVien }}</td>
                  <td><span class="ss-pill dot" :class="STATUS[r.trangThai]?.cls">{{ STATUS[r.trangThai]?.label }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="ss-actions">
            <button class="ss-btn" @click="calDetail=null">Đóng</button>
          </div>
        </div>
      </div>
    </Teleport>

  </AdminLayout>
</template>

<style scoped>
.filter-row { display:flex; align-items:center; gap:10px; flex-wrap:wrap; }
.filter-label { font-size:12px; font-weight:600; color:var(--ss-text); white-space:nowrap; }
.filter-input { width:200px; }
.filter-date  { width:160px; }

.view-toggle { display:flex; border:1px solid var(--ss-border); border-radius:var(--ss-radius); overflow:hidden; }
.vt-btn { height:32px; padding:0 14px; border:none; background:#fff; font-size:12px; font-weight:600; color:var(--ss-muted); cursor:pointer; display:inline-flex; align-items:center; gap:5px; transition:.15s; }
.vt-btn + .vt-btn { border-left:1px solid var(--ss-border); }
.vt-btn.active { background:var(--ss-primary); color:#fff; }

/* Calendar */
.cal-nav { display:flex; align-items:center; gap:10px; margin-bottom:14px; }
.cal-week-label { font-size:13px; font-weight:600; color:var(--ss-text); }
.cal-wrap { overflow-x:auto; }
.cal-table { width:100%; border-collapse:collapse; min-width:900px; }
.cal-th-shift { width:100px; background:var(--ss-surface); padding:10px 12px; font-size:11px; font-weight:700; color:var(--ss-muted); text-align:center; border:1px solid var(--ss-border); }
.cal-table thead th { background:var(--ss-surface); padding:10px 8px; text-align:center; border:1px solid var(--ss-border); }
.cal-th-today { background:#fff3e0 !important; }
.cal-day-lbl  { font-size:11px; font-weight:700; color:var(--ss-muted); }
.cal-day-date { font-size:12px; font-weight:600; color:var(--ss-text); margin-top:2px; }
.cal-td-shift { padding:14px 10px; text-align:center; border:1px solid var(--ss-border); vertical-align:middle; }
.cal-shift-name { font-size:12px; font-weight:700; color:#e53935; margin-bottom:2px; }
.cal-shift-time { font-size:11px; color:var(--ss-muted); }
.cal-td-cell { padding:10px 8px; border:1px solid var(--ss-border); vertical-align:top; min-width:110px; }
.cal-cell-name  { font-size:11px; font-weight:600; color:var(--ss-text); }
.cal-cell-time  { font-size:11px; color:var(--ss-muted); margin-bottom:4px; }
.cal-cell-count { font-size:11px; color:var(--ss-muted); margin-bottom:6px; }
.cal-more-btn {
  display:inline-block; padding:2px 10px; border-radius:20px; font-size:11px; font-weight:600;
  border:1px solid var(--ss-border); background:#fff; color:var(--ss-text); cursor:pointer; transition:.15s;
}
.cal-more-btn:hover { border-color:var(--ss-primary); color:var(--ss-primary); }
.cal-more-red { border-color:#e53935 !important; color:#e53935 !important; }

/* Modal */
.full { grid-column:1/-1; }
.badge-count { display:inline-block; margin-left:8px; padding:1px 8px; border-radius:999px; background:var(--ss-primary-soft); color:var(--ss-primary); font-size:11px; font-weight:600; }
.emp-list { max-height:200px; overflow-y:auto; border:1px solid var(--ss-border); border-radius:var(--ss-radius); }
.emp-item { display:flex; align-items:center; gap:10px; padding:8px 12px; cursor:pointer; border-bottom:1px solid var(--ss-line); transition:background .12s; }
.emp-item:last-child { border-bottom:none; }
.emp-item:hover { background:var(--ss-surface); }
.emp-item.selected { background:var(--ss-primary-soft); }

.spin { display:inline-block; animation:spin .7s linear infinite; }
@keyframes spin { to { transform:rotate(360deg); } }

/* ── Lịch tháng ──────────────────────────────────────────── */
.cal-table-month {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.cal-table-month thead th {
  padding: 10px 0;
  text-align: center;
  font-size: 12px;
  font-weight: 700;
  color: var(--ss-muted);
  border: 1px solid var(--ss-border);
  background: var(--ss-surface);
}

.cal-month-cell {
  border: 1px solid var(--ss-border);
  vertical-align: top;
  height: 100px;
  padding: 6px 8px;
  background: #fff;
  cursor: default;
  transition: background .12s;
}

.cal-month-cell:hover { background: #fafcff; }

.cal-month-other {
  background: var(--ss-surface);
  color: var(--ss-faint);
}

.cal-month-day {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 4px;
}

.cal-month-day span {
  font-size: 13px;
  font-weight: 600;
  color: var(--ss-text);
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.today-badge {
  background: var(--ss-primary) !important;
  color: #fff !important;
}

.cal-month-other .cal-month-day span {
  color: var(--ss-faint);
}

.cal-month-count {
  font-size: 11px;
  background: var(--ss-primary-soft);
  color: var(--ss-primary);
  border-radius: 4px;
  padding: 2px 6px;
  display: inline-block;
  font-weight: 600;
}
</style>
