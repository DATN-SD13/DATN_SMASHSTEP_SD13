<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import Select2 from '../../promotion/components/Select2.vue'
import scheduleService from '../services/scheduleService'
import { confirmAction, showSuccess, showError } from '../../../../utils/feedback'

// ── State ─────────────────────────────────────────────────
const rows    = ref([])
const loading = ref(false)
const error   = ref('')

// ── Bộ lọc (client-side) ──────────────────────────────────
const f = reactive({ tuKhoa: '', trangThai: '' })

const trangThaiOptions = [
  { value: '', label: 'Tất cả' },
  { value: '1', label: 'Hoạt động' },
  { value: '0', label: 'Ngừng' }
]

const filtered = computed(() => {
  let list = rows.value
  if (f.tuKhoa.trim()) {
    const kw = f.tuKhoa.trim().toLowerCase()
    list = list.filter(r =>
      r.maCa?.toLowerCase().includes(kw) ||
      r.tenCa?.toLowerCase().includes(kw) ||
      r.moTa?.toLowerCase().includes(kw)
    )
  }
  if (f.trangThai !== '') {
    list = list.filter(r => r.trangThai === Number(f.trangThai))
  }
  return list
})

function reset() { f.tuKhoa = ''; f.trangThai = '' }

// ── Load ──────────────────────────────────────────────────
async function load() {
  loading.value = true; error.value = ''
  try { rows.value = await scheduleService.getCaLamViec() || [] }
  catch (e) { error.value = e?.response?.data?.message || 'Không thể tải dữ liệu' }
  finally { loading.value = false }
}

onMounted(load)

// ── Toggle trạng thái ─────────────────────────────────────
async function handleToggle(row) {
  const action = row.trangThai === 1 ? 'ngừng hoạt động' : 'kích hoạt'
  confirmAction(`${action.charAt(0).toUpperCase() + action.slice(1)} ca "${row.tenCa}"?`, async () => {
    try {
      const updated = await scheduleService.toggleTrangThaiCa(row.id)
      const idx = rows.value.findIndex(r => r.id === row.id)
      if (idx !== -1) rows.value[idx] = updated
      showSuccess('Đã cập nhật trạng thái.')
    } catch (e) { showError(e?.response?.data?.message || 'Cập nhật thất bại') }
  })
}

// ── Modal thêm / sửa ──────────────────────────────────────
const showModal = ref(false)
const saving    = ref(false)
const formError = ref('')
const editId    = ref(null)
const form = reactive({ tenCa: '', gioBatDau: '', gioKetThuc: '', moTa: '' })

const modalTitle = computed(() => editId.value ? 'Chỉnh sửa ca làm' : 'Thêm ca làm mới')

function openAdd() {
  editId.value = null
  form.tenCa = ''; form.gioBatDau = ''; form.gioKetThuc = ''; form.moTa = ''
  formError.value = ''; showModal.value = true
}

function openEdit(row) {
  editId.value    = row.id
  form.tenCa      = row.tenCa      || ''
  form.gioBatDau  = row.gioBatDau  || ''
  form.gioKetThuc = row.gioKetThuc || ''
  form.moTa       = row.moTa       || ''
  formError.value = ''; showModal.value = true
}

function validate() {
  if (!form.tenCa.trim())   { formError.value = 'Tên ca không được để trống'; return false }
  if (!form.gioBatDau)      { formError.value = 'Giờ bắt đầu không được để trống'; return false }
  if (!form.gioKetThuc)     { formError.value = 'Giờ kết thúc không được để trống'; return false }
  return true
}

async function submitModal() {
  formError.value = ''
  if (!validate()) return
  saving.value = true
  try {
    const payload = {
      tenCa: form.tenCa.trim(),
      gioBatDau: form.gioBatDau,
      gioKetThuc: form.gioKetThuc,
      moTa: form.moTa || null
    }
    if (editId.value) {
      const updated = await scheduleService.updateCaLamViec(editId.value, payload)
      const idx = rows.value.findIndex(r => r.id === editId.value)
      if (idx !== -1) rows.value[idx] = updated
      showSuccess('Đã cập nhật ca làm việc.')
    } else {
      const created = await scheduleService.createCaLamViec(payload)
      rows.value.push(created)
      showSuccess('Đã thêm ca làm việc mới.')
    }
    showModal.value = false
  } catch (e) {
    formError.value = e?.response?.data?.message || 'Lưu thất bại'
  } finally { saving.value = false }
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
        <div class="filter-row">
          <input
            class="ss-input filter-input"
            v-model="f.tuKhoa"
            placeholder="Nhập mã hoặc tên ca..."
          />
          <div style="width:180px">
            <Select2
              v-model="f.trangThai"
              :options="trangThaiOptions"
              placeholder="Tất cả"
              search-placeholder="Tìm trạng thái..."
            />
          </div>
          <button class="ss-btn sm" @click="reset">
            <i class="bi bi-arrow-counterclockwise"></i> Đặt lại bộ lọc
          </button>
          <span class="ss-spacer"></span>
          <button class="ss-btn primary sm" @click="openAdd">
            <i class="bi bi-plus-lg"></i> Thêm lịch ca làm
          </button>
        </div>
      </section>

      <!-- Bảng -->
      <section class="ss-card">
        <div class="ss-head">
          <i class="bi bi-clock" style="color:var(--ss-muted);font-size:14px"></i>
          <h2>Lịch ca làm</h2>
        </div>

        <p v-if="error" class="ss-hint warn">
          <i class="bi bi-exclamation-triangle"></i> {{ error }}
        </p>

        <div class="ss-table-wrap">
          <table class="ss-table">
            <thead>
              <tr>
                <th class="w-stt c">STT</th>
                <th>Thông tin ca</th>
                <th class="c">Giờ bắt đầu</th>
                <th class="c">Giờ kết thúc</th>
                <th class="c">Trạng thái</th>
                <th class="c">Thao tác</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="loading">
                <td colspan="6" class="ss-empty">
                  <i class="bi bi-arrow-repeat spin"></i> Đang tải...
                </td>
              </tr>
              <template v-else>
                <tr v-for="(row, i) in filtered" :key="row.id">
                  <td class="c">{{ i + 1 }}</td>
                  <td>
                    <div class="shift-info">
                      <span class="shift-icon">
                        <i class="bi bi-clock-history"></i>
                      </span>
                      <div>
                        <div class="ss-strong">{{ row.tenCa }}</div>
                        <div class="ss-muted" style="font-size:11px">{{ row.maCa }}</div>
                      </div>
                    </div>
                  </td>
                  <td class="c">
                    <span class="time-badge start">{{ row.gioBatDau }}</span>
                  </td>
                  <td class="c">
                    <span class="time-badge end">{{ row.gioKetThuc }}</span>
                  </td>
                  <td class="c">
                    <button
                      class="toggle-btn"
                      :class="row.trangThai === 1 ? 'on' : 'off'"
                      @click="handleToggle(row)"
                      :title="row.trangThai === 1 ? 'Nhấn để ngừng' : 'Nhấn để kích hoạt'"
                      :aria-label="row.trangThai === 1 ? 'Đang hoạt động' : 'Đang ngừng'"
                    >
                      <span class="toggle-dot"></span>
                    </button>
                  </td>
                  <td class="c">
                    <div class="ss-row-actions">
                      <button class="ss-icon-btn" title="Chỉnh sửa" @click="openEdit(row)">
                        <i class="bi bi-eye"></i>
                      </button>
                    </div>
                  </td>
                </tr>
                <tr v-if="!filtered.length">
                  <td colspan="6" class="ss-empty">
                    <i class="bi bi-inbox"></i> Không có dữ liệu.
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </section>

    </main>

    <!-- Modal thêm / sửa -->
    <Teleport to="body">
      <div v-if="showModal" class="ss-modal-bg" @click.self="showModal = false">
        <div class="ss-modal" style="width:min(460px,100%)">
          <div class="ss-head">
            <div class="ss-head-icon"><i class="bi bi-clock-history"></i></div>
            <h2>{{ modalTitle }}</h2>
            <span class="ss-spacer"></span>
            <button class="ss-icon-btn" @click="showModal = false">
              <i class="bi bi-x-lg"></i>
            </button>
          </div>

          <div class="ss-grid2">
            <div class="ss-field full">
              <label class="ss-label">Tên ca <span class="req">*</span></label>
              <input
                class="ss-input"
                v-model="form.tenCa"
                placeholder="VD: Ca sáng, Ca chiều, Ca tối..."
              />
            </div>
            <div class="ss-field">
              <label class="ss-label">Giờ bắt đầu <span class="req">*</span></label>
              <input class="ss-input" type="time" v-model="form.gioBatDau" />
            </div>
            <div class="ss-field">
              <label class="ss-label">Giờ kết thúc <span class="req">*</span></label>
              <input class="ss-input" type="time" v-model="form.gioKetThuc" />
            </div>
            <div class="ss-field full">
              <label class="ss-label">Mô tả</label>
              <input class="ss-input" v-model="form.moTa" placeholder="Tuỳ chọn" />
            </div>
          </div>

          <p v-if="formError" class="ss-hint warn">
            <i class="bi bi-exclamation-circle"></i> {{ formError }}
          </p>

          <div class="ss-actions">
            <button class="ss-btn" @click="showModal = false" :disabled="saving">Hủy</button>
            <button class="ss-btn primary" @click="submitModal" :disabled="saving">
              <i :class="saving ? 'bi bi-arrow-repeat spin' : 'bi bi-check2'"></i>
              {{ saving ? 'Đang lưu...' : 'Lưu' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>

  </AdminLayout>
</template>

<style scoped>
.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.filter-input  { width: 220px; }
.filter-select { width: 150px; }

/* Shift info cell */
.shift-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.shift-icon {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #e3f2fd;
  color: #1565c0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  flex-shrink: 0;
}

/* Time badges */
.time-badge {
  display: inline-block;
  padding: 4px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: .3px;
}
.time-badge.start { background: #e8f5e9; color: #2e7d32; }
.time-badge.end   { background: #fce4ec; color: #c62828; }

/* Toggle switch */
.toggle-btn {
  width: 44px;
  height: 24px;
  border-radius: 12px;
  border: none;
  cursor: pointer;
  position: relative;
  transition: background .2s;
  padding: 0;
}
.toggle-btn.on  { background: #e53935; }
.toggle-btn.off { background: #bdbdbd; }
.toggle-dot {
  position: absolute;
  top: 3px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  transition: left .2s;
  box-shadow: 0 1px 3px rgba(0,0,0,.25);
}
.toggle-btn.on  .toggle-dot { left: 23px; }
.toggle-btn.off .toggle-dot { left: 3px; }

/* Modal */
.full { grid-column: 1 / -1; }

.spin {
  display: inline-block;
  animation: spin .7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
