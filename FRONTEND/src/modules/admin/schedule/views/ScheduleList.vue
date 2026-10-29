<script setup>
import { ref, reactive, onMounted, onUnmounted, watch, computed } from 'vue'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import SsPager from '../../../../components/SsPager.vue'
import Select2 from '../../promotion/components/Select2.vue'
import scheduleService from '../services/scheduleService'
import { canSetShiftStatus, canDeleteShift, requiresReopenReason, lateShiftInfo } from '../services/shiftStatusRules'
import LichLamViecImportModal from '../components/LichLamViecImportModal.vue'
import { confirmAction, showSuccess, showError } from '../../../../utils/feedback'
import api from '../../../../utils/api'
import { avatarUrl } from '../../../../utils/avatar'

// ── State ─────────────────────────────────────────────────
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
const failedAvatarUrls = ref(new Set())

// ── Cảnh báo nhân viên bị khóa ────────────────────────────
const lichBiKhoa        = ref([])   // lịch tương lai của NV bị khóa
const showKhoaWarning   = ref(true) // có thể ẩn đi tạm thời

async function loadLichBiKhoa() {
  try {
    lichBiKhoa.value = await scheduleService.getLichNhanVienBiKhoa() || []
  } catch { lichBiKhoa.value = [] }
}

// Nhóm lịch theo nhân viên để hiển thị gọn
const lichBiKhoaTheoNV = computed(() => {
  const map = new Map()
  for (const l of lichBiKhoa.value) {
    if (!map.has(l.maNhanVien)) {
      map.set(l.maNhanVien, { maNhanVien: l.maNhanVien, tenNhanVien: l.tenNhanVien, lich: [] })
    }
    map.get(l.maNhanVien).lich.push(l)
  }
  return [...map.values()]
})

const toYMDToday = () => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

const f = reactive({ tuKhoa: '', caId: '', ngayLam: toYMDToday() })

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
  4: { label: 'Chưa nhận ca', cls: 'warn' },
  0: { label: 'Chờ giao ca', cls: 'warn' },
  1: { label: 'Đang làm',   cls: 'success' },
  2: { label: 'Vắng mặt',   cls: 'danger' },
  3: { label: 'Đã kết thúc', cls: 'gray' }
}

// ── Load bảng ─────────────────────────────────────────────
async function load() {
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

function reset() { f.tuKhoa = ''; f.caId = ''; f.ngayLam = toYMDToday(); page.value = 1; load() }

// Format ngày yyyy-MM-dd → dd/MM/yyyy để hiển thị
function fmtNgay(val) {
  if (!val) return ''
  const [y, m, d] = String(val).substring(0, 10).split('-')
  return d && m && y ? `${d}/${m}/${y}` : val
}
watch(page, load)
watch(size, () => { page.value = 1; load() })
onMounted(() => { loadShifts(); load(); loadLichBiKhoa(); statusTimer = setInterval(() => { statusNow.value = Date.now() }, 1000) })
onUnmounted(() => clearInterval(statusTimer))

// ── Thay nhân viên vào ca (từ banner cảnh báo) ────────────
const thayTheModal    = ref(false)
const thayTheLich     = ref(null)   // lịch đang cần thay { id, maNhanVien, tenNhanVien, tenCa, thoiGian, ngayLam, caLamViecId }
const thayTheSearch   = ref('')
const thayTheSaving   = ref(false)
const thayTheError    = ref('')
const thayTheMaNV     = ref('')     // mã NV được chọn để thay vào
const nvTrongCa       = ref([])     // mã NV đã có lịch trong ca đó (để disable)

const thayTheEmps = computed(() => {
  const kw = thayTheSearch.value.trim().toLowerCase()
  // Loại bỏ chính NV đang bị khóa
  const list = employees.value.filter(e => e.code !== thayTheLich.value?.maNhanVien)
  if (!kw) return list
  return list.filter(e =>
    (e.code||'').toLowerCase().includes(kw) || (e.name||'').toLowerCase().includes(kw)
  )
})

async function openThayThe(lich) {
  thayTheLich.value   = lich
  thayTheMaNV.value   = ''
  thayTheSearch.value = ''
  thayTheError.value  = ''
  nvTrongCa.value     = []
  thayTheModal.value  = true
  // Load song song: danh sách NV active + NV đã có lịch trong ca đó
  await Promise.all([
    loadEmployees(),
    scheduleService.getNhanVienTrongCa(lich.caLamViecId, lich.ngayLam)
      .then(list => { nvTrongCa.value = list || [] })
      .catch(() => {})
  ])
}

async function submitThayThe() {
  if (!thayTheMaNV.value) { thayTheError.value = 'Vui lòng chọn nhân viên thay thế'; return }
  // Kiểm tra trùng ca ngay tại frontend
  if (nvTrongCa.value.includes(thayTheMaNV.value)) {
    thayTheError.value = 'Nhân viên này đã có lịch trong ca đó, không thể thay thế.'
    return
  }
  thayTheSaving.value = true
  thayTheError.value  = ''
  try {
    await scheduleService.thayNhanVien(thayTheLich.value.id, thayTheMaNV.value)
    thayTheModal.value = false
    showSuccess('Đã xếp lịch thành công.')
    await Promise.all([loadLichBiKhoa(), viewMode.value === 'lich' ? loadCalendar() : load()])
  } catch (e) {
    thayTheError.value = e?.response?.data?.message || 'Thay thế thất bại'
  } finally { thayTheSaving.value = false }
}

// ── Actions bảng ──────────────────────────────────────────
function handleDelete(row) {
  if (!canDeleteShift(row.trangThai)) return
  confirmAction(`Xóa lịch làm của ${row.tenNhanVien} ngày ${fmtNgay(row.ngayLam)}?`, async () => {
    try { await scheduleService.deleteLich(row.id); showSuccess('Đã xóa.'); load() }
    catch (e) { showError(e?.response?.data?.message || 'Xóa thất bại') }
  })
}

// ── Calendar ──────────────────────────────────────────────
const calMode   = ref('tuan')
const weekStart = ref(getMonday(new Date()))
const calRows   = ref([])
let calendarRequest = 0
const calendarShifts = computed(() => shifts.value.filter(s => s.trangThai === 1 || calRows.value.some(r => r.caLamViecId === s.id)))

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


// ── Lịch ngày ────────────────────────────────────────────
const currentDay = ref((() => { const d = new Date(); d.setHours(0,0,0,0); return d })())
const dayLabel = computed(() =>
  currentDay.value.toLocaleDateString('vi-VN', { day:'2-digit', month:'long', year:'numeric' })
)
function prevDay() { const d=new Date(currentDay.value); d.setDate(d.getDate()-1); currentDay.value=d; loadCalendar() }
function nextDay() { const d=new Date(currentDay.value); d.setDate(d.getDate()+1); currentDay.value=d; loadCalendar() }
const dayRows = computed(() =>
  calendarShifts.value.map(shift => ({
    shift, list: calCell(shift.id, toYMD(currentDay.value)), ymd: toYMD(currentDay.value)
  }))
)

// ── Lịch tháng ───────────────────────────────────────────
const currentMonth = ref(new Date())
const monthLabel = computed(() => {
  const s = currentMonth.value.toLocaleDateString('vi-VN', { month:'long', year:'numeric' })
  return s.charAt(0).toUpperCase() + s.slice(1)
})
function prevMonth() { const d=new Date(currentMonth.value); d.setDate(1); d.setMonth(d.getMonth()-1); currentMonth.value=d; loadCalendar() }
function nextMonth() { const d=new Date(currentMonth.value); d.setDate(1); d.setMonth(d.getMonth()+1); currentMonth.value=d; loadCalendar() }

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
  return calendarShifts.value.reduce((sum,shift)=>sum+calCell(shift.id,ymd).length,0)
}

// ── Load calendar (chung cho ngày/tuần/tháng) ─────────────
async function loadCalendar() {
  const request = ++calendarRequest
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
    const collected = []
    let currentPage = 1, lastPage = 1
    do {
      const data = await scheduleService.getLichLamViec({ tuNgay: from, denNgay: to, page: currentPage, size: 100 })
      if (request !== calendarRequest) return
      collected.push(...(data?.content || []))
      lastPage = Math.max(1, Number(data?.totalPages || 1))
      currentPage++
    } while (currentPage <= lastPage)
    calRows.value = collected
  } catch (e) {
    if (request !== calendarRequest) return
    calRows.value = []
    showError(e?.response?.data?.message || 'Không thể tải lịch. Vui lòng thử lại.')
  }
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

// ── Xếp ca tự động ────────────────────────────────────────
const showAutoModal   = ref(false)
const autoSaving      = ref(false)
const autoError       = ref('')
const autoResult      = ref(null)
const autoForm = reactive({
  tuNgay:  toYMD(new Date()),
  denNgay: (() => { const d = new Date(); d.setDate(d.getDate() + 6); return toYMD(d) })()
})

function openAutoModal() {
  autoError.value = ''; autoResult.value = null
  showAutoModal.value = true
}

async function submitAuto() {
  autoError.value = ''; autoResult.value = null
  if (!autoForm.tuNgay || !autoForm.denNgay) return (autoError.value = 'Vui lòng chọn khoảng ngày')
  if (autoForm.tuNgay > autoForm.denNgay)    return (autoError.value = 'Từ ngày không được lớn hơn đến ngày')
  autoSaving.value = true
  try {
    autoResult.value = await scheduleService.xepCaTuDong(autoForm.tuNgay, autoForm.denNgay)
    showSuccess(autoResult.value.thongBao)
    if (viewMode.value === 'lich') loadCalendar(); else load()
  } catch (e) {
    autoError.value = e?.response?.data?.message || 'Xếp ca thất bại'
  } finally { autoSaving.value = false }
}

// ── Import Excel ───────────────────────────────────────────
const showImportModal = ref(false)
function openImportModal() { showImportModal.value = true }
function importedSchedules() {
  if (viewMode.value === 'lich') loadCalendar(); else load()
  loadLichBiKhoa()
}

// ── Tải template ──────────────────────────────────────────
const downloadingTpl = ref(false)
async function handleDownloadTemplate() {
  downloadingTpl.value = true
  try { await scheduleService.downloadTemplate() }
  catch { showError('Không thể tải template') }
  finally { downloadingTpl.value = false }
}

// ── Chỉnh sửa trạng thái ──────────────────────────────────
const showEditModal = ref(false)
const editRow       = ref(null)
const editSaving    = ref(false)
const editError     = ref('')
const editForm      = reactive({ trangThai: 0, ghiChu: '', lyDoDieuChinh: '' })
const statusNow = ref(Date.now())
let statusTimer
const canChangeStatus = next => canSetShiftStatus(editRow.value, next, statusNow.value)

const TRANG_THAI_OPTIONS = [
  { value: 0, label: 'Chờ giao ca'  },
  { value: 1, label: 'Đang làm — nhận từ ca cá nhân' },
  { value: 3, label: 'Đã kết thúc — chốt từ ca cá nhân' },
  { value: 2, label: 'Vắng mặt'    },
  { value: 4, label: 'Chưa nhận ca' }
]

function openEditModal(row) {
  editRow.value        = row
  editForm.trangThai   = row.trangThai
  editForm.ghiChu      = row.ghiChu || ''
  editForm.lyDoDieuChinh = ''
  statusNow.value = Date.now()
  editError.value      = ''
  showEditModal.value  = true
}

async function submitEdit() {
  editError.value = ''
  if (!canSetShiftStatus(editRow.value, editForm.trangThai, Date.now())) {
    editError.value = 'Đã hết hạn nhận ca hoặc trạng thái không được phép đổi. Ca đang làm/đã kết thúc chỉ được sửa ghi chú.'
    return
  }
  if (requiresReopenReason(editRow.value, editForm.trangThai) && !editForm.lyDoDieuChinh.trim()) {
    editError.value = 'Nhập lý do điều chỉnh vắng mặt trước khi mở lại lịch.'
    return
  }
  editSaving.value = true
  try {
    const updated = await scheduleService.updateTrangThai(editRow.value.id, editForm.trangThai, editForm.ghiChu, editForm.lyDoDieuChinh)
    const idx = rows.value.findIndex(r => r.id === editRow.value.id)
    if (idx !== -1) {
      rows.value[idx] = { ...rows.value[idx], ...updated }
    }
    showEditModal.value = false
    showSuccess('Đã cập nhật trạng thái.')
    if (viewMode.value === 'lich') loadCalendar()
  } catch (e) {
    editError.value = e?.response?.data?.message || 'Cập nhật thất bại'
  } finally { editSaving.value = false }
}
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
    <main class="ss-page">

      <!-- ── Cảnh báo nhân viên bị khóa ── -->
      <Transition name="warn-fade">
        <section
          v-if="showKhoaWarning && lichBiKhoa.length"
          class="ss-card warn-banner"
        >
          <div class="warn-banner-head">
            <div class="warn-banner-title">
              <i class="bi bi-exclamation-triangle-fill"></i>
              <span>
                Cần xếp lại lịch —
                <strong>{{ lichBiKhoa.length }}</strong> ca sắp tới
                của <strong>{{ lichBiKhoaTheoNV.length }}</strong> nhân viên bị khóa chưa được sắp xếp lại
              </span>
            </div>
            <button class="ss-icon-btn warn-close" title="Ẩn cảnh báo" @click="showKhoaWarning = false">
              <i class="bi bi-x-lg"></i>
            </button>
          </div>

          <div class="warn-nv-list">
            <div
              v-for="nv in lichBiKhoaTheoNV"
              :key="nv.maNhanVien"
              class="warn-nv-item"
            >
              <div class="warn-nv-info">
                <i class="bi bi-person-fill-lock"></i>
                <span class="ss-code">{{ nv.maNhanVien }}</span>
                <span class="warn-nv-name">{{ nv.tenNhanVien }}</span>
                <span class="warn-nv-count">{{ nv.lich.length }} ca</span>
              </div>
              <div class="warn-lich-chips">
                <span
                  v-for="l in nv.lich"
                  :key="l.id"
                  class="warn-chip"
                  :title="l.tenCa + ' · ' + l.thoiGian"
                >
                  <i class="bi bi-calendar3"></i>
                  {{ fmtNgay(l.ngayLam) }}
                  <span class="warn-chip-ca">{{ l.tenCa }}</span>
                  <button
                    class="warn-chip-btn"
                    title="Thay nhân viên vào ca này"
                    @click="openThayThe(l)"
                  >
                    <i class="bi bi-person-fill-up"></i> Thay thế
                  </button>
                </span>
              </div>
            </div>
          </div>
        </section>
      </Transition>

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
          <button class="ss-btn sm" @click="openAutoModal">
            <i class="bi bi-arrow-left-right"></i> Xếp ca tự động
          </button>
          <button class="ss-btn sm" @click="reset">
            <i class="bi bi-arrow-counterclockwise"></i> Đặt lại bộ lọc
          </button>
          <button class="ss-btn sm" :disabled="downloadingTpl" @click="handleDownloadTemplate">
            <i :class="downloadingTpl ? 'bi bi-arrow-repeat spin' : 'bi bi-download'"></i>
            {{ downloadingTpl ? 'Đang tải...' : 'Tải template' }}
          </button>
          <button class="ss-btn sm" @click="openImportModal">
            <i class="bi bi-file-earmark-excel"></i> Import Excel
          </button>
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
            <table class="ss-table" style="min-width:1020px">
              <thead>
                <tr>
                  <th class="w-stt c">STT</th>
                  <th>Mã nhân viên</th>
                  <th>Nhân viên</th>
                  <th>Ca làm</th>
                  <th>Thời gian</th>
                  <th>Ngày làm</th>
                  <th>Trạng thái</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loading">
                  <td colspan="8" class="ss-empty">
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
                    <td class="nowrap">{{ fmtNgay(row.ngayLam) }}</td>
                    <td><span class="ss-pill dot" :class="STATUS[row.trangThai]?.cls">{{ STATUS[row.trangThai]?.label || 'Chưa xác định' }}</span><small v-if="lateShiftInfo(row)" class="shift-late">{{ lateShiftInfo(row) }}</small></td>
                    <td>
                      <div class="ss-row-actions">
                        <button class="ss-icon-btn" :title="[1,3].includes(row.trangThai) ? 'Sửa ghi chú' : 'Sửa trạng thái và ghi chú'" @click="openEditModal(row)">
                          <i class="bi bi-pencil-square"></i>
                        </button>
                        <button class="ss-icon-btn danger" :disabled="!canDeleteShift(row.trangThai)" :title="canDeleteShift(row.trangThai) ? 'Xóa lịch' : 'Không xóa ca đang làm hoặc đã kết thúc'" @click="handleDelete(row)">
                          <i class="bi bi-trash"></i>
                        </button>
                      </div>
                    </td>
                  </tr>
                  <tr v-if="!rows.length">
                    <td colspan="8" class="ss-empty"><i class="bi bi-inbox"></i> Không có dữ liệu.</td>
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
                  <tr v-for="item in dayRows" :key="item.shift.id">
                    <td class="cal-td-shift">
                      <div class="cal-shift-name">{{ item.shift.tenCa }}</div>
                      <div class="cal-shift-time">{{ item.shift.thoiGian }}</div>
                    </td>
                    <td class="cal-td-cell">
                      <button v-if="item.list.length" class="cal-assignment"
                        :aria-label="`${item.list.length} nhân viên, ${item.shift.tenCa}, ${fmtNgay(item.ymd)}. Xem danh sách`"
                        @click="openCalDetail(item.shift, { ymd: item.ymd, display: currentDay.toLocaleDateString('vi-VN',{day:'2-digit',month:'2-digit',year:'numeric'}) })">
                        <span><i class="bi bi-people" aria-hidden="true"></i> {{ item.list.length }} nhân viên</span>
                      </button><span v-else class="cal-empty" aria-label="Chưa phân lịch">—</span>
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
                      <div class="cal-day-date" :title="day.display">{{ day.display.slice(0,5) }}</div>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="shift in calendarShifts" :key="shift.id">
                    <td class="cal-td-shift">
                      <div class="cal-shift-name">{{ shift.tenCa }}</div>
                      <div class="cal-shift-time">{{ shift.thoiGian }}</div>
                    </td>
                    <td v-for="day in weekDays" :key="day.ymd"
                      class="cal-td-cell">
                      <button v-if="calCell(shift.id, day.ymd).length" class="cal-assignment"
                        :aria-label="`${calCell(shift.id, day.ymd).length} nhân viên, ${shift.tenCa}, ${day.display}. Xem danh sách`"
                        @click="openCalDetail(shift, day)"><span><i class="bi bi-people" aria-hidden="true"></i> {{ calCell(shift.id, day.ymd).length }} nhân viên</span></button>
                      <span v-else class="cal-empty" aria-label="Chưa phân lịch">—</span>
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
          <p v-if="calMode!=='thang'" class="cal-help">Chọn ô có nhân viên để xem danh sách và trạng thái.</p>
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

    <!-- Modal thay thế nhân viên bị khóa -->
    <Teleport to="body">
      <div v-if="thayTheModal" class="ss-modal-bg" @click.self="thayTheModal=false">
        <div class="ss-modal" style="width:min(560px,100%)">

          <div class="ss-head">
            <div class="ss-head-icon warn-icon"><i class="bi bi-person-fill-up"></i></div>
            <h2>Thay nhân viên vào ca</h2>
            <span class="ss-spacer"></span>
            <button class="ss-icon-btn" @click="thayTheModal=false"><i class="bi bi-x-lg"></i></button>
          </div>

          <!-- Thông tin ca cần thay -->
          <div v-if="thayTheLich" class="thayt-info">
            <div class="thayt-info-row">
              <span class="thayt-lbl"><i class="bi bi-person-fill-lock"></i> NV bị khóa</span>
              <span>
                <span class="ss-code">{{ thayTheLich.maNhanVien }}</span>
                <strong style="margin-left:6px">{{ thayTheLich.tenNhanVien }}</strong>
              </span>
            </div>
            <div class="thayt-info-row">
              <span class="thayt-lbl"><i class="bi bi-arrow-left-right"></i> Ca làm việc</span>
              <span><strong>{{ thayTheLich.tenCa }}</strong> · {{ thayTheLich.thoiGian }}</span>
            </div>
            <div class="thayt-info-row">
              <span class="thayt-lbl"><i class="bi bi-calendar3"></i> Ngày làm</span>
              <span><strong>{{ fmtNgay(thayTheLich.ngayLam) }}</strong></span>
            </div>
          </div>

          <!-- Chọn NV thay thế -->
          <div class="ss-field" style="margin-top:16px">
            <label class="ss-label">
              Chọn nhân viên thay thế <span class="req">*</span>
              <span v-if="thayTheMaNV" class="badge-count">1 đã chọn</span>
            </label>
            <div class="ss-search" style="margin-bottom:8px">
              <i class="bi bi-search"></i>
              <input class="ss-input" v-model="thayTheSearch" placeholder="Tìm theo tên hoặc mã..." />
            </div>
            <div class="emp-list">
              <label
                v-for="emp in thayTheEmps"
                :key="emp.id"
                class="emp-item"
                :class="{ selected: thayTheMaNV === emp.code, 'emp-item-disabled': nvTrongCa.includes(emp.code) }"
              >
                <input type="radio" class="ss-cb" :value="emp.code" v-model="thayTheMaNV"
                  :disabled="nvTrongCa.includes(emp.code)" />
                <span class="ss-code" style="min-width:72px;font-size:11px">{{ emp.code }}</span>
                <span class="ss-strong" style="font-size:12px">{{ emp.name }}</span>
                <span v-if="nvTrongCa.includes(emp.code)" class="emp-trung-ca">
                  <i class="bi bi-calendar-x"></i> Đã có ca
                </span>
              </label>
              <div v-if="!thayTheEmps.length" class="ss-empty" style="height:60px">Không tìm thấy</div>
            </div>
          </div>

          <p v-if="thayTheError" class="ss-hint warn"><i class="bi bi-exclamation-circle"></i> {{ thayTheError }}</p>

          <div class="ss-actions">
            <button class="ss-btn" @click="thayTheModal=false" :disabled="thayTheSaving">Hủy</button>
            <button class="ss-btn primary" @click="submitThayThe" :disabled="thayTheSaving || !thayTheMaNV">
              <i :class="thayTheSaving ? 'bi bi-arrow-repeat spin' : 'bi bi-check2'"></i>
              {{ thayTheSaving ? 'Đang xử lý...' : 'Xác nhận thay thế' }}
            </button>
          </div>

        </div>
      </div>
    </Teleport>

    <!-- Modal chi tiết ô lịch -->
    <Teleport to="body">
      <div v-if="calDetail" class="ss-modal-bg" @click.self="calDetail=null" @keydown.esc="calDetail=null">
        <section class="calendar-detail" role="dialog" aria-modal="true" aria-labelledby="calendar-detail-title">
          <header class="calendar-detail-head">
            <div class="calendar-detail-icon"><i class="bi bi-calendar3" aria-hidden="true"></i></div>
            <div class="calendar-detail-heading">
              <h2 id="calendar-detail-title">{{ calDetail.shift.tenCa }}</h2>
              <p>{{ calDetail.day.display }}<span aria-hidden="true"> · </span>{{ calDetail.shift.thoiGian }}</p>
            </div>
            <button class="ss-icon-btn" aria-label="Đóng danh sách nhân viên" autofocus @click="calDetail=null"><i class="bi bi-x-lg" aria-hidden="true"></i></button>
          </header>
          <div class="calendar-detail-summary"><span>Nhân viên được phân ca</span><span class="calendar-detail-count"><i class="bi bi-people" aria-hidden="true"></i> {{ calDetail.list.length }} nhân viên</span></div>
          <div v-if="!calDetail.list.length" class="calendar-detail-empty">Chưa có nhân viên được phân vào ca này.</div>
          <ul v-else class="calendar-detail-list" aria-label="Danh sách nhân viên trong ca">
            <li v-for="r in calDetail.list" :key="r.id" class="calendar-detail-person">
              <div class="calendar-person-avatar" aria-hidden="true">
                <img v-if="avatarUrl(r.hinhAnh) && !failedAvatarUrls.has(avatarUrl(r.hinhAnh))"
                     :src="avatarUrl(r.hinhAnh)" alt="" @error="failedAvatarUrls.add(avatarUrl(r.hinhAnh))" />
                <i v-else class="bi bi-person"></i>
              </div>
              <div class="calendar-person-info"><strong>{{ r.tenNhanVien }}</strong><span>{{ r.maNhanVien }}</span><small v-if="lateShiftInfo(r)" class="shift-late">{{ lateShiftInfo(r) }}</small></div>
              <span class="ss-pill dot calendar-person-status" :class="STATUS[r.trangThai]?.cls">{{ STATUS[r.trangThai]?.label || 'Chưa xác định' }}</span>
            </li>
          </ul>
          <footer class="calendar-detail-footer"><button class="ss-btn" @click="calDetail=null">Đóng</button></footer>
        </section>
      </div>
    </Teleport>

    <!-- Modal chỉnh sửa trạng thái -->
    <Teleport to="body">
      <div v-if="showEditModal" class="ss-modal-bg" @click.self="showEditModal=false">
        <div class="ss-modal shift-status-modal" role="dialog" aria-modal="true" aria-labelledby="shift-status-title" style="width:min(420px,100%)">
          <div class="ss-head">
            <div class="ss-head-icon"><i class="bi bi-pencil-square"></i></div>
            <h2 id="shift-status-title">Cập nhật trạng thái</h2>
            <span class="ss-spacer"></span>
            <button class="ss-icon-btn" @click="showEditModal=false"><i class="bi bi-x-lg"></i></button>
          </div>

          <div class="edit-info" v-if="editRow">
            <div class="edit-info-row">
              <span class="edit-info-label">Nhân viên</span>
              <span class="edit-info-val ss-strong">{{ editRow.tenNhanVien }}</span>
            </div>
            <div class="edit-info-row">
              <span class="edit-info-label">Ca làm</span>
              <span class="edit-info-val">{{ editRow.tenCa }}</span>
            </div>
            <div class="edit-info-row">
              <span class="edit-info-label">Ngày làm</span>
              <span class="edit-info-val">{{ fmtNgay(editRow.ngayLam) }}</span>
            </div>
          </div>

          <div class="ss-field" style="margin-top:14px">
            <label class="ss-label">Trạng thái <span class="req">*</span></label>
            <div class="trang-thai-options">
              <label
                v-for="opt in TRANG_THAI_OPTIONS" :key="opt.value"
                class="tt-option"
                :class="{ active: editForm.trangThai === opt.value, [`cls-${opt.value}`]: true }"
                :style="!canChangeStatus(opt.value) ? { opacity: .45, cursor: 'not-allowed' } : undefined"
              >
                <input type="radio" :value="opt.value" v-model="editForm.trangThai" :disabled="!canChangeStatus(opt.value)" style="display:none" />
                <span class="tt-dot"></span>
                {{ opt.label }}
              </label>
            </div>
            <p class="ss-hint">Nhận ca: sớm tối đa 15 phút, muộn tối đa 15 phút. Quá hạn không được mở lại lịch vắng. Lịch đang làm/đã kết thúc chỉ được sửa ghi chú.</p>
          </div>

          <div class="ss-field" style="margin-top:12px">
            <label class="ss-label">Ghi chú</label>
            <input class="ss-input" v-model="editForm.ghiChu" placeholder="Nhập ghi chú..." />
          </div>

          <div v-if="requiresReopenReason(editRow, editForm.trangThai)" class="ss-field" style="margin-top:12px">
            <label class="ss-label">Lý do điều chỉnh vắng mặt <span class="req">*</span></label>
            <input class="ss-input" v-model="editForm.lyDoDieuChinh" maxlength="200" placeholder="Nhập lý do mở lại lịch (tối đa 200 ký tự)" />
          </div>
          <p v-if="editError" class="ss-hint warn" style="margin-top:10px">
            <i class="bi bi-exclamation-circle"></i> {{ editError }}
          </p>

          <div class="ss-actions" style="margin-top:16px">
            <button class="ss-btn" @click="showEditModal=false" :disabled="editSaving">Hủy</button>
            <button class="ss-btn primary" @click="submitEdit" :disabled="editSaving">
              <i :class="editSaving ? 'bi bi-arrow-repeat spin' : 'bi bi-check2'"></i>
              {{ editSaving ? 'Đang lưu...' : 'Lưu' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Modal xếp ca tự động -->    <Teleport to="body">
      <div v-if="showAutoModal" class="ss-modal-bg" @click.self="showAutoModal=false">
        <div class="ss-modal" style="width:min(440px,100%)">
          <div class="ss-head">
            <div class="ss-head-icon"><i class="bi bi-arrow-left-right"></i></div>
            <h2>Xếp ca tự động</h2>
            <span class="ss-spacer"></span>
            <button class="ss-icon-btn" @click="showAutoModal=false"><i class="bi bi-x-lg"></i></button>
          </div>

          <p class="ss-hint" style="margin-bottom:14px">
            <i class="bi bi-info-circle"></i>
            Hệ thống sẽ tự động phân ca luân phiên cho tất cả nhân viên đang hoạt động
            trong khoảng thời gian đã chọn. Những lịch đã tồn tại sẽ được bỏ qua.
          </p>

          <div class="ss-grid2">
            <div class="ss-field">
              <label class="ss-label">Từ ngày <span class="req">*</span></label>
              <input class="ss-input" type="date" v-model="autoForm.tuNgay" />
            </div>
            <div class="ss-field">
              <label class="ss-label">Đến ngày <span class="req">*</span></label>
              <input class="ss-input" type="date" v-model="autoForm.denNgay" />
            </div>
          </div>

          <!-- Kết quả -->
          <div v-if="autoResult" class="auto-result">
            <div class="auto-stat success"><i class="bi bi-check-circle"></i> Đã tạo: <strong>{{ autoResult.daTao }}</strong></div>
            <div class="auto-stat warn"><i class="bi bi-skip-forward"></i> Bỏ qua: <strong>{{ autoResult.boBua }}</strong></div>
          </div>

          <p v-if="autoError" class="ss-hint warn"><i class="bi bi-exclamation-circle"></i> {{ autoError }}</p>

          <div class="ss-actions">
            <button class="ss-btn" @click="showAutoModal=false" :disabled="autoSaving">Đóng</button>
            <button class="ss-btn primary" @click="submitAuto" :disabled="autoSaving">
              <i :class="autoSaving ? 'bi bi-arrow-repeat spin' : 'bi bi-arrow-left-right'"></i>
              {{ autoSaving ? 'Đang xếp...' : 'Bắt đầu xếp ca' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <LichLamViecImportModal v-if="showImportModal" @close="showImportModal=false" @imported="importedSchedules" />

  </AdminLayout>
</template>

<style scoped>
.shift-status-modal{overflow-y:auto;max-height:calc(100dvh - 40px);overscroll-behavior:contain}.shift-status-modal>*{flex-shrink:0}
.shift-late{display:block;font-size:11px;color:#a56514;margin-top:5px;line-height:1.5}
.calendar-detail{width:min(560px,calc(100vw - 32px));max-height:calc(100dvh - 48px);display:flex;flex-direction:column;overflow:hidden;background:#fff;border:1px solid var(--ss-border);border-radius:14px;box-shadow:0 20px 60px #19364b26;color:var(--ss-text)}
.calendar-detail-head{display:flex;align-items:center;gap:12px;padding:22px 22px 18px;flex-shrink:0}
.calendar-detail-icon{width:44px;height:44px;display:grid;place-items:center;flex-shrink:0;background:var(--ss-primary-soft);border-radius:10px;color:var(--ss-primary);font-size:20px}
.calendar-detail-heading{min-width:0;flex:1}.calendar-detail-heading h2{font-size:17px;font-weight:650;margin:0 0 5px;overflow-wrap:anywhere}.calendar-detail-heading p{font-size:12px;color:var(--ss-muted);margin:0;line-height:1.5}
.calendar-detail-summary{display:flex;justify-content:space-between;align-items:center;gap:12px;background:var(--ss-surface);padding:12px 22px;border-block:1px solid var(--ss-border);font-size:12px;flex-shrink:0}
.calendar-detail-count{display:inline-flex;align-items:center;gap:6px;color:var(--ss-muted);white-space:nowrap}
.calendar-detail-list{list-style:none;padding:0 22px;margin:0;overflow-y:auto;min-height:0;overscroll-behavior:contain}
.calendar-detail-person{display:flex;align-items:center;gap:12px;padding:16px 0;border-bottom:1px solid var(--ss-border)}.calendar-detail-person:last-child{border-bottom:0}
.calendar-person-avatar{display:grid;place-items:center;width:36px;height:36px;border-radius:50%;background:#edf3f8;color:#658399;font-size:18px;flex-shrink:0;overflow:hidden}
.calendar-person-avatar img{display:block;width:100%;height:100%;object-fit:cover}
.calendar-person-info{display:flex;flex-direction:column;gap:3px;flex:1;min-width:0}.calendar-person-info strong{font-size:13px;font-weight:600;overflow-wrap:anywhere}.calendar-person-info>span{font-size:11px;color:var(--ss-muted)}
.calendar-person-status{flex-shrink:0;max-width:45%;white-space:normal;line-height:1.4}.calendar-detail-footer{display:flex;justify-content:flex-end;padding:14px 22px;border-top:1px solid var(--ss-border);background:#fff;flex-shrink:0}.calendar-detail-empty{padding:30px 22px;text-align:center;color:var(--ss-muted);font-size:13px}
@media(max-width:440px){.calendar-detail-head{padding:18px 16px}.calendar-detail-summary{padding:10px 16px}.calendar-detail-list{padding:0 16px}.calendar-detail-person{gap:9px;flex-wrap:wrap}.calendar-person-info{flex-basis:calc(100% - 48px)}.calendar-person-status{margin-left:45px;max-width:calc(100% - 45px)}.calendar-detail-footer{padding:12px 16px}}
.ss-icon-btn:disabled{opacity:.35;cursor:not-allowed;pointer-events:auto}.cal-empty{display:block;padding:20px 0;color:var(--ss-muted);font-size:11px}.cal-person{font-size:11px;color:var(--ss-text);line-height:1.6;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;max-width:150px}.cal-cell-count i{margin-right:3px}

/* ── Banner cảnh báo NV bị khóa ───────────────────────── */
.warn-banner {
  border: 1.5px solid #f6ad55;
  background: #fffbf0;
  padding: 16px 20px;
  display: flex; flex-direction: column; gap: 14px;
}

.warn-banner-head {
  display: flex; align-items: flex-start;
  justify-content: space-between; gap: 12px;
}

.warn-banner-title {
  display: flex; align-items: flex-start; gap: 10px;
  font-size: 13.5px; color: #7b4f00;
  line-height: 1.5;
}
.warn-banner-title i { font-size: 16px; color: #d97706; flex-shrink: 0; margin-top: 2px; }

.warn-close { color: #d97706; }
.warn-close:hover { background: #fde68a; }

.warn-nv-list {
  display: flex; flex-direction: column; gap: 10px;
}

.warn-nv-item {
  background: #fff; border: 1px solid #fde68a;
  border-radius: 8px; padding: 10px 14px;
  display: flex; flex-direction: column; gap: 8px;
}

.warn-nv-info {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px;
}
.warn-nv-info i { color: #d97706; font-size: 14px; }
.warn-nv-name { font-weight: 600; color: var(--ss-text); }
.warn-nv-count {
  margin-left: auto;
  background: #fef3c7; color: #92400e;
  font-size: 11px; font-weight: 700;
  padding: 2px 8px; border-radius: 20px;
}

.warn-lich-chips {
  display: flex; flex-wrap: wrap; gap: 6px;
}

.warn-chip {
  display: inline-flex; align-items: center; gap: 5px;
  background: #fff7ed; border: 1px solid #fed7aa;
  color: #9a3412;
  font-size: 11.5px; font-weight: 600;
  padding: 3px 10px; border-radius: 6px;
}
.warn-chip i { font-size: 10px; color: #c2410c; }
.warn-chip-ca {
  font-weight: 400; color: #78350f;
  margin-left: 2px;
}

.warn-chip-btn {
  display: inline-flex; align-items: center; gap: 4px;
  margin-left: 6px; padding: 2px 8px;
  background: #fff; border: 1px solid #c2410c;
  color: #c2410c; border-radius: 4px;
  font-size: 11px; font-weight: 600;
  cursor: pointer; transition: background .15s, color .15s;
}
.warn-chip-btn:hover { background: #c2410c; color: #fff; }
.warn-chip-btn i { font-size: 11px; }

/* ── Modal thay thế ────────────────────────────────────── */
.warn-icon { background: #fff7ed !important; color: #d97706 !important; }

.thayt-info {
  background: #fffbf0; border: 1px solid #fde68a;
  border-radius: 8px; padding: 12px 16px;
  display: flex; flex-direction: column; gap: 8px;
  margin-top: 4px;
}
.thayt-info-row {
  display: flex; align-items: center; gap: 12px;
  font-size: 13px;
}
.thayt-lbl {
  min-width: 130px; font-size: 11.5px; font-weight: 600;
  color: var(--ss-muted);
  display: flex; align-items: center; gap: 6px;
}

/* NV đã có lịch trong ca — disabled */
.emp-item-disabled {
  opacity: .55; cursor: not-allowed;
}
.emp-item-disabled input { cursor: not-allowed; }

.emp-trung-ca {
  margin-left: auto;
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11px; font-weight: 600;
  color: #c62828; background: #fff5f5;
  border: 1px solid #ef9a9a;
  padding: 2px 8px; border-radius: 20px;
}

.warn-fade-enter-active, .warn-fade-leave-active { transition: all .3s ease; }
.warn-fade-enter-from, .warn-fade-leave-to { opacity: 0; transform: translateY(-8px); }

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
.cal-table { width:100%; border-collapse:collapse; min-width:900px; table-layout:fixed; }
.cal-th-shift { width:140px; background:var(--ss-surface); padding:10px 12px; font-size:11px; font-weight:700; color:var(--ss-muted); text-align:left; border:1px solid var(--ss-border); }
.cal-table thead th { background:var(--ss-surface); padding:10px 8px; text-align:center; border:1px solid var(--ss-border); }
.cal-th-today { background:var(--ss-primary-soft) !important; box-shadow:inset 0 -2px var(--ss-primary); }
.cal-day-lbl  { font-size:11px; font-weight:700; color:var(--ss-muted); }
.cal-day-date { font-size:12px; font-weight:600; color:var(--ss-text); margin-top:2px; }
.cal-td-shift { padding:12px; text-align:left; border:1px solid var(--ss-border); vertical-align:middle; background:var(--ss-surface); }
.cal-shift-name { font-size:12px; font-weight:600; color:var(--ss-text); margin-bottom:3px; }
.cal-shift-time { font-size:11px; color:var(--ss-muted); }
.cal-td-cell { padding:0; border:1px solid var(--ss-border); vertical-align:middle; background:#fff; height:72px; }
.cal-td-cell .cal-empty { padding:0; text-align:center; color:#b1bdc9; font-size:16px; }
.cal-assignment { display:flex; align-items:center; justify-content:center; width:100%; min-height:72px; height:100%; padding:12px 6px; border:0; background:transparent; color:var(--ss-text); cursor:pointer; font:inherit; font-size:12px; }
.cal-assignment span { display:inline-flex; align-items:center; gap:6px; padding:6px 9px; border-radius:6px; background:var(--ss-primary-soft); color:var(--ss-primary); white-space:nowrap; }
.cal-assignment:hover { background:#f5f9fd; }
.cal-assignment:focus-visible { outline:2px solid var(--ss-primary); outline-offset:-3px; }
.cal-help { margin:10px 0 0; font-size:11px; color:var(--ss-muted); }
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

/* ── Auto & Import result ─────────────────────────────────── */
.auto-result, .import-result {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px;
  background: var(--ss-surface);
  border-radius: var(--ss-radius);
  margin-bottom: 12px;
}
.auto-stat {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 20px;
}
.auto-stat.success { background: #e8f5e9; color: #2e7d32; }
.auto-stat.warn    { background: #fff8e1; color: #f57f17; }
.auto-stat.danger  { background: #fff5f5; color: #c62828; }
.import-result { flex-direction: column; }
.error-list {
  margin: 6px 0 0 0;
  padding-left: 18px;
  font-size: 12px;
  color: #c62828;
  max-height: 100px;
  overflow-y: auto;
}

/* ── Upload zone ─────────────────────────────────────────── */
.upload-zone {
  border: 2px dashed #cbd5e1;
  border-radius: 10px;
  padding: 28px 16px;
  text-align: center;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
  margin-bottom: 14px;
}
.upload-zone:hover       { border-color: #079fc9; background: rgba(7,159,201,0.04); }
.upload-zone.has-file    { border-color: #2e7d32; background: #f1f8e9; border-style: solid; }
.upload-hint { font-size: 13px; font-weight: 600; color: var(--ss-text); }
.upload-filename { font-size: 13px; font-weight: 700; color: #2e7d32; }
.upload-sub  { font-size: 11px; color: var(--ss-muted); }

/* ── Modal chỉnh sửa trạng thái ─────────────────────────── */
.edit-info {
  background: var(--ss-surface);
  border-radius: var(--ss-radius);
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.edit-info-row { display: flex; align-items: center; gap: 10px; font-size: 13px; }
.edit-info-label { width: 80px; font-size: 11px; font-weight: 600; color: var(--ss-muted); flex-shrink: 0; }
.edit-info-val { color: var(--ss-text); }

.trang-thai-options { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 6px; }
.tt-option {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 14px; border-radius: 20px; font-size: 12px; font-weight: 600;
  cursor: pointer; border: 2px solid transparent;
  background: var(--ss-surface); color: var(--ss-muted);
  transition: all 0.15s; user-select: none;
}
.tt-option:hover { border-color: #cbd5e1; color: var(--ss-text); }
.tt-dot { width: 8px; height: 8px; border-radius: 50%; background: currentColor; flex-shrink: 0; }
.tt-option.active.cls-0 { background: #fff8e1; color: #f57f17; border-color: #f57f17; }
.tt-option.active.cls-1 { background: #e3f2fd; color: #1565c0; border-color: #1565c0; }
.tt-option.active.cls-2 { background: #fff5f5; color: #c62828; border-color: #c62828; }
.tt-option.active.cls-3 { background: #e8f5e9; color: #2e7d32; border-color: #2e7d32; }
</style>
