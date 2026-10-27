<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import SsPager from '../../../../components/SsPager.vue'
import Select2 from '../../promotion/components/Select2.vue'
import scheduleService from '../services/scheduleService'
import api from '../../../../utils/api'

// ── State ─────────────────────────────────────────────────
const rows    = ref([])
const shifts  = ref([])
const roles   = ref([])
const loading = ref(false)
const error   = ref('')
const page    = ref(1)
const size    = ref(10)
const pages   = ref(1)
const total   = ref(0)

// ── Helpers ngày ──────────────────────────────────────────
const toYMD = d => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${dd}`
}

const today = new Date()

const f = reactive({
  tuKhoa:  '',
  caId:    '',
  vaiTroId:'',
  tuNgay:  toYMD(today),
  denNgay: toYMD(today)
})

// ── Trạng thái ────────────────────────────────────────────
const STATUS = {
  4: { label: 'Chưa nhận ca', cls: 'warn' },
  0: { label: 'Chờ giao ca',  cls: 'warn'    },
  1: { label: 'Giao ca',      cls: 'info'    },
  2: { label: 'Vắng mặt',    cls: 'danger'  },
  3: { label: 'Đã kết thúc', cls: 'success' },
  99:{ label: 'Đang làm',    cls: 'active'  }
}

function statusInfo(row) {
  // Nếu trangThai = 1 và ngày làm = hôm nay → đang trong ca
  if (row.trangThai === 1 && row.ngayLam === toYMD(today)) return STATUS[99]
  return STATUS[row.trangThai] || { label: 'Không xác định', cls: '' }
}

// ── Load ──────────────────────────────────────────────────
async function load() {
  loading.value = true; error.value = ''
  try {
    const params = { page: page.value, size: size.value }
    if (f.tuKhoa)   params.tuKhoa   = f.tuKhoa
    if (f.caId)     params.caId     = f.caId
    if (f.vaiTroId) params.vaiTroId = f.vaiTroId
    if (f.tuNgay)   params.tuNgay   = f.tuNgay
    if (f.denNgay)  params.denNgay  = f.denNgay

    const data = await scheduleService.getLichSuHoatDong(params)
    rows.value  = data?.content       || []
    pages.value = Math.max(data?.totalPages    || 1, 1)
    total.value = data?.totalElements || 0
  } catch (e) {
    error.value = e?.response?.data?.message || 'Không thể tải dữ liệu'
  } finally { loading.value = false }
}

async function loadOptions() {
  const [s, r] = await Promise.all([
    scheduleService.getCaLamViec(),
    scheduleService.getVaiTro()
  ])
  shifts.value = s || []
  roles.value  = r || []
}

// ── Select2 options ───────────────────────────────────────
const shiftOptions = computed(() => [
  { value: '', label: 'Tất cả' },
  ...shifts.value.map(s => ({ value: s.id, label: `${s.tenCa} (${s.thoiGian})` }))
])

const roleOptions = computed(() => [
  { value: '', label: 'Tất cả' },
  ...roles.value.map(r => ({ value: r.id, label: r.tenVaiTro }))
])

function reset() {
  f.tuKhoa = ''; f.caId = ''; f.vaiTroId = ''
  f.tuNgay = toYMD(today); f.denNgay = toYMD(today)
  page.value = 1; load()
}

watch(page, load)
watch(size, () => { page.value = 1; load() })
onMounted(() => { loadOptions(); load() })

// ── Xuất Excel ────────────────────────────────────────────
const exportingExcel = ref(false)

async function xuatExcel() {
  exportingExcel.value = true
  try {
    await scheduleService.xuatExcelLichSu({
      tuKhoa:   f.tuKhoa   || undefined,
      caId:     f.caId     || undefined,
      vaiTroId: f.vaiTroId || undefined,
      tuNgay:   f.tuNgay   || undefined,
      denNgay:  f.denNgay  || undefined
    })
  } catch {
    // showError nếu cần
  } finally { exportingExcel.value = false }
}

// ── Format tiền ───────────────────────────────────────────
function fmtMoney(val) {
  if (!val && val !== 0) return '—'
  return Number(val).toLocaleString('vi-VN') + 'đ'
}

// ── Avatar initials ───────────────────────────────────────
function initials(name) {
  if (!name) return '?'
  return name.split(' ').slice(-2).map(w => w[0]).join('').toUpperCase()
}
</script>

<template>
  <AdminLayout>
    <main class="ss-page">

      <!-- Bộ lọc -->
      <section class="ss-card">
        <div class="ss-head">
          <div class="ss-head-icon"><i class="bi bi-funnel"></i></div>
          <h2>Bộ lọc</h2>
        </div>
        <div class="filter-grid">
          <!-- Tìm kiếm -->
          <div class="filter-field wide">
            <label class="filter-label">Tìm kiếm</label>
            <div class="ss-search">
              <i class="bi bi-search"></i>
              <input class="ss-input"
                v-model="f.tuKhoa"
                placeholder="Tìm theo tài khoản / mã ca / mã hóa đơn..."
                @keyup.enter="load" />
            </div>
          </div>

          <!-- Ca làm việc -->
          <div class="filter-field">
            <label class="filter-label">Ca làm việc</label>
            <Select2
              v-model="f.caId"
              :options="shiftOptions"
              placeholder="Tất cả"
              search-placeholder="Tìm ca..."
              @change="load"
            />
          </div>

          <!-- Vai trò -->
          <div class="filter-field">
            <label class="filter-label">Vai trò</label>
            <Select2
              v-model="f.vaiTroId"
              :options="roleOptions"
              placeholder="Tất cả"
              search-placeholder="Tìm vai trò..."
              @change="load"
            />
          </div>

          <!-- Từ ngày -->
          <div class="filter-field">
            <label class="filter-label">Từ ngày:</label>
            <input class="ss-input" type="date" v-model="f.tuNgay" @change="load" />
          </div>

          <!-- Đến ngày -->
          <div class="filter-field">
            <label class="filter-label">Đến ngày:</label>
            <input class="ss-input" type="date" v-model="f.denNgay" @change="load" />
          </div>

          <!-- Actions -->
          <div class="filter-actions">
            <button class="ss-btn sm danger-outline" @click="reset">
              <i class="bi bi-arrow-counterclockwise"></i> Đặt lại bộ lọc
            </button>
            <button class="ss-btn sm success-outline" @click="xuatExcel" :disabled="exportingExcel">
              <i :class="exportingExcel ? 'bi bi-arrow-repeat spin' : 'bi bi-file-earmark-excel'"></i>
              {{ exportingExcel ? 'Đang xuất...' : 'Xuất Excel' }}
            </button>
          </div>
        </div>
      </section>

      <!-- Bảng -->
      <section class="ss-card">
        <div class="ss-head">
          <i class="bi bi-clock-history" style="color:var(--ss-muted);font-size:14px"></i>
          <h2>Danh sách hoạt động</h2>
          <span class="ss-spacer"></span>
          <button class="ss-icon-btn" title="Làm mới" @click="load">
            <i class="bi bi-arrow-clockwise" :class="{ spin: loading }"></i>
          </button>
        </div>

        <p v-if="error" class="ss-hint warn">
          <i class="bi bi-exclamation-triangle"></i> {{ error }}
        </p>

        <div class="ss-table-wrap">
          <table class="ss-table activity-table">
            <thead>
              <tr>
                <th class="w-stt c">STT</th>
                <th>Mã nhân viên</th>
                <th>Tên nhân viên</th>
                <th>Vai trò</th>
                <th>Ca làm việc</th>
                <th>Thời gian</th>
                <th class="c">Thu/hoàn thuần</th>
                <th class="c">Trạng thái</th>
                <th>Ghi chú</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="9" class="ss-empty">
                  <i class="bi bi-arrow-repeat spin"></i> Đang tải dữ liệu...
                </td>
              </tr>
              <template v-else>
                <tr v-for="(row, i) in rows" :key="row.id">
                  <td class="c">{{ (page - 1) * size + i + 1 }}</td>

                  <!-- Mã NV -->
                  <td><span class="ss-code">{{ row.maNhanVien }}</span></td>

                  <!-- Tên NV + avatar -->
                  <td>
                    <div class="nv-info">
                      <div class="nv-avatar">
                        <img v-if="row.hinhAnh" :src="row.hinhAnh" :alt="row.tenNhanVien" />
                        <span v-else>{{ initials(row.tenNhanVien) }}</span>
                      </div>
                      <span class="ss-strong">{{ row.tenNhanVien }}</span>
                    </div>
                  </td>

                  <!-- Vai trò -->
                  <td>{{ row.vaiTro }}</td>

                  <!-- Ca làm việc -->
                  <td>{{ row.tenCa }}</td>

                  <!-- Thời gian (2 dòng: giờ bắt đầu + kết thúc) -->
                  <td>
                    <div class="time-col">
                      <div v-if="row.thoiGianBatDau" class="time-line">
                        <span v-for="(line, li) in row.thoiGianBatDau.split('\n')" :key="li"
                          :class="li === 0 ? 'time-hour' : 'time-date'">{{ line }}</span>
                      </div>
                      <div v-if="row.thoiGianKetThuc" class="time-line">
                        <span v-for="(line, li) in row.thoiGianKetThuc.split('\n')" :key="li"
                          :class="li === 0 ? 'time-hour' : 'time-date'">{{ line }}</span>
                      </div>
                    </div>
                  </td>

                  <!-- Doanh thu -->
                  <td class="c revenue">{{ fmtMoney(row.doanhThu) }}<details v-if="row.daChot" class="settlement-detail"><summary>Biên bản kết ca</summary><div>Đầu ca: {{ fmtMoney(row.tienMatDauCa) }}</div><div>Tiền mặt dự kiến: {{ fmtMoney(row.tienMatDuKien) }}</div><div>Đã kiểm đếm: {{ row.tienMatThucTe == null ? 'Chưa xác nhận' : fmtMoney(row.tienMatThucTe) }}</div><div>Chênh lệch: {{ row.chenhLech == null ? 'Chưa đối soát' : fmtMoney(row.chenhLech) }}</div><small v-if="row.tuDongKetThuc">Tự đóng khi hết giờ; chưa kiểm đếm tiền thực tế</small></details></td>

                  <!-- Trạng thái -->
                  <td class="c">
                    <span class="ss-pill dot" :class="statusInfo(row).cls">
                      {{ statusInfo(row).label }}
                    </span>
                  </td>

                  <!-- Ghi chú -->
                  <td class="note-cell" :title="row.ghiChu">{{ row.ghiChu || '' }}</td>
                </tr>

                <tr v-if="!rows.length">
                  <td colspan="9" class="ss-empty">
                    <i class="bi bi-inbox"></i> Không có dữ liệu.
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>

        <SsPager v-model:page="page" v-model:size="size" :pages="pages" />
      </section>

    </main>
  </AdminLayout>
</template>

<style scoped>
/* Bộ lọc */
.filter-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 16px;
  align-items: flex-end;
}
.filter-field { display: flex; flex-direction: column; gap: 4px; }
.filter-field.wide { flex: 1 1 260px; }
.filter-field { flex: 0 0 160px; }
.filter-label { font-size: 12px; font-weight: 600; color: var(--ss-muted); }
.filter-actions { display: flex; gap: 8px; align-items: flex-end; margin-left: auto; }

/* Nút outline */
.ss-btn.danger-outline {
  background: #fff;
  color: #e53935;
  border: 1.5px solid #e53935;
}
.ss-btn.danger-outline:hover { background: #fff5f5; }
.ss-btn.success-outline {
  background: #fff;
  color: #2e7d32;
  border: 1.5px solid #2e7d32;
}
.ss-btn.success-outline:hover { background: #f1f8e9; }

/* Bảng */
.activity-table th, .activity-table td { vertical-align: middle; }

/* Avatar + tên nhân viên */
.nv-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.nv-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--ss-primary-soft);
  color: var(--ss-primary);
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}
.nv-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Cột thời gian */
.time-col { display: flex; flex-direction: column; gap: 4px; }
.time-line { display: flex; flex-direction: column; line-height: 1.3; }
.time-hour { font-size: 13px; font-weight: 700; color: var(--ss-text); }
.time-date { font-size: 11px; color: var(--ss-muted); }

/* Doanh thu */
.revenue { font-weight: 700; color: var(--ss-text); white-space: nowrap; }

/* Trạng thái pill */
.ss-pill.dot { display: inline-flex; align-items: center; gap: 5px; padding: 3px 10px; border-radius: 20px; font-size: 12px; font-weight: 600; }
.ss-pill.dot::before { content: ''; width: 7px; height: 7px; border-radius: 50%; background: currentColor; }
.ss-pill.warn    { background: #fff8e1; color: #f57f17; }
.ss-pill.info    { background: #e3f2fd; color: #1565c0; }
.ss-pill.danger  { background: #fff5f5; color: #c62828; }
.ss-pill.success { background: #e8f5e9; color: #2e7d32; }
.ss-pill.active  { background: #c8e6c9; color: #1b5e20; font-weight: 700; }

/* Ghi chú */
.note-cell {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--ss-muted);
}

.spin { animation: spin .7s linear infinite; display: inline-block; }
@keyframes spin { to { transform: rotate(360deg); } }

</style>

<style scoped>.settlement-detail{margin-top:8px;text-align:left;font-size:12px;font-weight:400;line-height:1.8;min-width:190px}.settlement-detail summary{color:#168fce;cursor:pointer;font-weight:600}.settlement-detail small{color:#9b6b26;display:block;white-space:normal;max-width:230px}</style>
