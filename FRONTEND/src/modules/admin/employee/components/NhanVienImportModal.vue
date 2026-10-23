<script setup>
import { computed, ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import employeeService from '../services/employeeService'
import { useAuthStore } from '../../../../stores/auth'
const emit = defineEmits(['close', 'imported'])
const auth = useAuthStore()
const file = ref(null), input = ref(null), preview = ref(null), result = ref(null), history = ref([])
const historyPanel = ref(null), bodyPanel = ref(null)
const error = ref(''), previewBusy = ref(false), confirmBusy = ref(false), retryBusy = ref(false), historyBusy = ref(false)
const busy = computed(() => previewBusy.value || confirmBusy.value || retryBusy.value)
const canConfirm = computed(() => preview.value?.totalRows > 0 && preview.value.invalidRows === 0 && !busy.value && !result.value)
const recoveryKey = `ss_employee_import_${auth.user?.id || auth.user?.username || 'admin'}`
let alive = true, pollTimer, refreshVersion = 0, reportedBatch = ''
const message = e => e?.response?.data?.message || e?.message || 'Không thực hiện được. Vui lòng thử lại.'
const stateLabel = value => ({ SENT: 'Đã gửi', FAILED: 'Gửi thất bại', PENDING: 'Đang chờ gửi' }[value] || value)
function dateLabel(value) { return value ? new Date(value).toLocaleString('vi-VN') : '' }
function remember(id) { try { localStorage.setItem(recoveryKey, id) } catch {} }
function notifyCreated(value) { if (reportedBatch !== value.batchId) { reportedBatch = value.batchId; emit('imported') } }
function setResult(value) {
  if (!alive) return
  result.value = value; preview.value = null; remember(value.batchId); notifyCreated(value)
  history.value = history.value.map(batch => batch.batchId === value.batchId ? value : batch)
  clearTimeout(pollTimer)
  if (value.emailPending > 0) pollTimer = setTimeout(() => refreshResult(value.batchId, true), 3000)
}
async function refreshResult(id = result.value?.batchId, quiet = false) {
  if (!id || !alive) return
  const version = ++refreshVersion
  try { const value = await employeeService.getImportResult(id); if (alive && version === refreshVersion) setResult(value) }
  catch (e) {
    if (!alive || version !== refreshVersion) return
    if (!quiet) error.value = message(e)
    if (result.value?.emailPending > 0) pollTimer = setTimeout(() => refreshResult(id, true), 5000)
  }
}
async function viewHistory(id) {
  await refreshResult(id)
  if (!alive || result.value?.batchId !== id) return
  await nextTick()
  if (historyPanel.value) historyPanel.value.open = false
  bodyPanel.value?.scrollTo({ top: 0, behavior: 'smooth' })
}
async function loadHistory() {
  historyBusy.value = true
  try { const values = await employeeService.getImportHistory(); if (alive) history.value = values || [] }
  catch (e) { if (alive) error.value = message(e) }
  finally { if (alive) historyBusy.value = false }
}
function chooseFile(event) { file.value = event.target.files?.[0] || null; preview.value = null; result.value = null; error.value = ''; clearTimeout(pollTimer); ++refreshVersion }
async function readFile() {
  if (!file.value || busy.value) return
  error.value = ''; preview.value = null; result.value = null
  if (!/\.(xlsx|xls)$/i.test(file.value.name)) { error.value = 'Chỉ nhận file .xlsx hoặc .xls.'; return }
  if (file.value.size > 5 * 1024 * 1024) { error.value = 'File Excel tối đa 5 MB.'; return }
  previewBusy.value = true
  try { const value = await employeeService.previewImport(file.value); if (alive) preview.value = value }
  catch (e) { if (alive) error.value = message(e) }
  finally { if (alive) previewBusy.value = false }
}
async function confirmImport() {
  if (!canConfirm.value) return
  const value = preview.value
  confirmBusy.value = true; error.value = ''; remember(value.requestId)
  try {
    const created = await employeeService.confirmImport({ requestId: value.requestId, rows: value.rows.map(row => ({ rowNumber: row.rowNumber, employee: row.employee })) })
    if (alive) { setResult(created); await loadHistory() }
  } catch (e) {
    if (!alive) return
    const invalid = e?.response?.data?.data
    if (invalid?.rows && invalid?.requestId) { preview.value = invalid; error.value = message(e) }
    else if (e?.response?.status && e.response.status < 500) { error.value = message(e) }
    else {
      // A lost HTTP response may follow a committed batch. Recover by the same ID, never create a new batch.
      try { const recovered = await employeeService.getImportResult(value.requestId); if (alive) { setResult(recovered); await loadHistory() } }
      catch { if (alive) error.value = `${message(e)} Nếu mất kết nối, bấm xác nhận lại để dùng cùng mã lô; hoặc tải lại lịch sử nhập để kiểm tra.` }
    }
  } finally { if (alive) confirmBusy.value = false }
}
async function retryEmails() {
  if (!result.value || busy.value) return
  const id = result.value.batchId; retryBusy.value = true; error.value = ''; clearTimeout(pollTimer)
  try { const value = await employeeService.retryImportEmails(id); if (alive) { setResult(value); await loadHistory() } }
  catch (e) { if (alive) { error.value = message(e); await refreshResult(id, true) } }
  finally { if (alive) retryBusy.value = false }
}
function newFile() { ++refreshVersion; clearTimeout(pollTimer); preview.value = null; result.value = null; file.value = null; if (input.value) input.value.value = ''; error.value = ''; loadHistory() }
onMounted(async () => {
  await loadHistory()
  let id; try { id = localStorage.getItem(recoveryKey) } catch {}
  if (id && alive && !file.value && !preview.value) await refreshResult(id, true)
})
onBeforeUnmount(() => { alive = false; clearTimeout(pollTimer); ++refreshVersion })
</script>
<template>
  <div class="import-overlay" @click.self="!busy && emit('close')">
    <section class="import-modal" role="dialog" aria-modal="true" aria-labelledby="employee-import-title">
      <header class="import-head"><div class="ss-head"><span class="ss-head-icon"><i class="bi bi-file-earmark-excel"></i></span><div><h2 id="employee-import-title">Nhập Excel nhân viên</h2><p>Xem trước và kiểm tra toàn bộ dữ liệu trước khi tạo tài khoản.</p></div></div><button type="button" class="ss-icon-btn" aria-label="Đóng nhập Excel" :disabled="busy" @click="emit('close')"><i class="bi bi-x-lg"></i></button></header>
      <div ref="bodyPanel" class="import-body">
        <p v-if="error" class="import-alert error" role="alert">{{ error }}</p>
        <div v-if="!result" class="import-file"><label for="employee-import-file">Chọn file Excel (.xlsx, .xls · tối đa 5 MB / 500 dòng)</label><div><input id="employee-import-file" ref="input" type="file" accept=".xlsx,.xls" :disabled="busy" @change="chooseFile"/><button type="button" class="ss-btn" :disabled="!file || busy" @click="readFile">{{ previewBusy ? 'Đang kiểm tra…' : 'Xem trước dữ liệu' }}</button></div><small>Dùng file mẫu. Email không được trùng; SĐT lưu dạng Text để giữ số 0 đầu. Ngày sinh dùng dd/MM/yyyy.</small></div>
        <template v-if="preview">
          <div class="import-counts"><span>Tổng số dòng <b>{{ preview.totalRows }}</b></span><span class="valid">Hợp lệ <b>{{ preview.validRows }}</b></span><span class="invalid">Có lỗi <b>{{ preview.invalidRows }}</b></span></div>
          <p class="import-alert" :class="preview.invalidRows ? 'error' : 'info'">{{ preview.invalidRows ? 'Chưa thể nhập. Sửa tất cả dòng có lỗi trong Excel rồi chọn lại file và xem trước.' : 'Tất cả dòng hợp lệ. Hệ thống sẽ kiểm tra lại khi xác nhận; mã nhân viên và tài khoản được tạo tự động.' }}</p>
          <div class="import-table"><table class="ss-table"><thead><tr><th>Dòng</th><th>Họ tên / Email</th><th>SĐT</th><th>Giới tính</th><th>Ngày sinh</th><th>Vai trò ID</th><th>Địa chỉ</th><th>Kết quả kiểm tra</th></tr></thead><tbody><tr v-for="row in preview.rows" :key="row.rowNumber" :class="{ 'invalid-row': row.errors.length }"><td>{{ row.rowNumber }}</td><td><b>{{ row.employee?.name }}</b><small>{{ row.employee?.email }}</small></td><td>{{ row.employee?.phone }}</td><td>{{ ({ 0: 'Khác', 1: 'Nam', 2: 'Nữ' })[row.employee?.gender] || '—' }}</td><td>{{ row.employee?.dob }}</td><td>{{ row.employee?.roleId }}</td><td>{{ [row.employee?.street, row.employee?.ward, row.employee?.province].filter(Boolean).join(', ') }}</td><td><ul v-if="row.errors.length"><li v-for="item in row.errors" :key="item">{{ item }}</li></ul><span v-else class="valid">Hợp lệ</span></td></tr></tbody></table></div>
        </template>
        <template v-if="result">
          <div class="import-alert info"><b>Đã tạo {{ result.createdCount }} nhân viên và tài khoản.</b> Email được gửi riêng sau khi dữ liệu đã lưu; gửi lại email không tạo lại nhân viên.</div>
          <div class="import-counts" aria-live="polite"><span class="valid">Email đã gửi <b>{{ result.emailSent }}</b></span><span class="invalid">Email lỗi <b>{{ result.emailFailed }}</b></span><span>Đang chờ <b>{{ result.emailPending }}</b></span></div>
          <p v-if="result.emailPending" class="ss-hint" role="status">Đang gửi email, kết quả tự cập nhật. Có thể đóng cửa sổ và xem lại trong lịch sử nhập.</p>
          <div class="import-result-actions"><button type="button" class="ss-btn" :disabled="busy" @click="refreshResult()"><i class="bi bi-arrow-clockwise"></i> Cập nhật kết quả</button><button v-if="result.emailFailed || result.emailPending" type="button" class="ss-btn primary" :disabled="busy" @click="retryEmails">{{ retryBusy ? 'Đang yêu cầu…' : 'Gửi tiếp / thử lại email lỗi' }}</button></div>
          <div class="import-table"><table class="ss-table"><thead><tr><th>Dòng</th><th>Mã NV</th><th>Họ tên</th><th>Email</th><th>Kết quả email</th><th>Chi tiết</th></tr></thead><tbody><tr v-for="row in result.rows" :key="row.mailId"><td>{{ row.rowNumber }}</td><td class="ss-code">{{ row.employeeCode }}</td><td>{{ row.name }}</td><td>{{ row.email }}</td><td><span :class="row.state === 'SENT' ? 'valid' : row.state === 'FAILED' ? 'invalid' : ''">{{ stateLabel(row.state) }}</span></td><td>{{ row.error || '—' }}</td></tr></tbody></table></div>
        </template>
        <details ref="historyPanel" class="import-history">
          <summary class="import-history-summary">
            <span class="import-history-title"><i class="bi bi-clock-history" aria-hidden="true"></i> Lịch sử nhập của bạn <span class="import-history-count">{{ history.length }} lô gần nhất</span></span>
            <span class="import-history-toggle">Xem lịch sử <i class="bi bi-chevron-down" aria-hidden="true"></i></span>
          </summary>
          <div class="import-history-content">
            <div class="import-history-toolbar"><p class="ss-hint">Chọn một lô để xem chi tiết và kết quả gửi email.</p><button type="button" class="ss-btn sm" :disabled="busy || historyBusy" @click="loadHistory">{{ historyBusy ? 'Đang tải…' : 'Tải lại lịch sử' }}</button></div>
            <p v-if="historyBusy" class="ss-hint" role="status">Đang tải lịch sử…</p>
            <p v-else-if="!history.length" class="ss-hint">Chưa có lô nhập nào.</p>
            <div v-else class="import-history-list" role="region" aria-label="Các lô nhập gần đây" tabindex="0">
              <button v-for="batch in history" :key="batch.batchId" type="button" class="import-history-item" :disabled="busy" @click="viewHistory(batch.batchId)"><span><b>{{ dateLabel(batch.createdAt) }}</b><small>{{ batch.createdCount }} nhân viên</small></span><span class="import-history-stats"><span class="valid">Đã gửi {{ batch.emailSent }}</span><span :class="{ invalid: batch.emailFailed }">Lỗi {{ batch.emailFailed }}</span><span>Chờ {{ batch.emailPending }}</span><i class="bi bi-chevron-right" aria-hidden="true"></i></span></button>
            </div>
          </div>
        </details>
      </div>
      <footer class="import-footer"><button type="button" class="ss-btn" :disabled="busy" @click="emit('close')">Đóng</button><button v-if="result" type="button" class="ss-btn" :disabled="busy" @click="newFile">Chọn file khác</button><button v-else type="button" class="ss-btn primary" :disabled="!canConfirm" @click="confirmImport">{{ confirmBusy ? 'Đang lưu toàn bộ lô…' : 'Xác nhận nhập' }}</button></footer>
    </section>
  </div>
</template>
<style scoped>
.import-overlay { position:fixed; inset:0; display:grid; place-items:center; padding:20px; background:rgba(15,23,42,.45); z-index:1150; }
.import-modal { width:min(1240px,100%); min-height:0; max-height:calc(100vh - 40px); max-height:calc(100dvh - 40px); background:#fff; border:1px solid var(--ss-border); border-radius:16px; display:flex; flex-direction:column; overflow:hidden; box-shadow:0 20px 70px rgba(15,23,42,.2); }
.import-head,.import-footer { flex:0 0 auto; display:flex; align-items:center; justify-content:space-between; gap:16px; padding:18px 22px; border-bottom:1px solid var(--ss-border); }
.import-head h2 { font-size:17px; font-weight:600; margin:0; color:var(--ss-text); }
.import-head p { margin:4px 0 0; font-size:12px; color:var(--ss-muted); }
.import-body { flex:1 1 auto; min-height:0; padding:18px 22px; overflow:auto; display:flex; flex-direction:column; gap:16px; scrollbar-gutter:stable; }
.import-body > * { flex-shrink:0; }
.import-file { display:grid; gap:10px; border:1px dashed #b9d3f3; border-radius:12px; padding:16px; background:#f8fbff; }
.import-file>div { display:flex; justify-content:space-between; align-items:center; gap:12px; flex-wrap:wrap; }
.import-file small,.import-table small { display:block; color:var(--ss-muted); margin-top:4px; }
.import-alert { padding:12px 14px; border:1px solid var(--ss-border); border-radius:9px; line-height:1.6; font-size:13px; margin:0; }
.import-alert.info { background:#eff6ff; border-color:#d5e6fc; color:var(--ss-text); }
.import-alert.error { background:#fff3f4; border-color:#ffd1d7; color:#b8262b; }
.import-counts { display:flex; gap:12px; flex-wrap:wrap; }
.import-counts>span { padding:12px 16px; background:var(--ss-surface); border:1px solid var(--ss-border); border-radius:9px; font-size:13px; }
.import-counts b { margin-left:10px; font-size:18px; }
.valid { color:#148c4d; }.invalid { color:#b8262b; }
.import-table { flex:0 0 auto; min-height:120px; max-height:360px; max-height:min(360px,45dvh); border:1px solid var(--ss-border); border-radius:10px; overflow:auto; scrollbar-gutter:stable; }
.import-table thead th { position:sticky; top:0; z-index:1; background:#f3f7fc; }
.import-table table { min-width:920px; }.import-table td { vertical-align:top; font-size:12px; max-width:300px; white-space:normal; overflow-wrap:anywhere; }
.import-table ul { color:#b8262b; margin:0; padding-left:16px; line-height:1.7; }.invalid-row { background:#fff8f8; }
.import-footer { border-top:1px solid var(--ss-border); border-bottom:0; justify-content:flex-end; background:#f8fafc; }
.import-result-actions { display:flex; gap:10px; flex-wrap:wrap; }
.import-history { border:1px solid var(--ss-border); border-radius:10px; background:#fff; }
.import-history-summary { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:13px 14px; list-style:none; cursor:pointer; color:var(--ss-text); }
.import-history-summary::-webkit-details-marker { display:none; }
.import-history-summary:focus-visible,.import-history-list:focus-visible { outline:2px solid var(--ss-primary); outline-offset:2px; }
.import-history-title { display:flex; align-items:center; gap:8px; flex-wrap:wrap; font-size:13px; font-weight:600; }
.import-history-title > i { color:var(--ss-primary); }
.import-history-count { font-size:11px; font-weight:500; color:var(--ss-muted); background:var(--ss-surface); padding:3px 8px; border-radius:999px; }
.import-history-toggle { display:flex; align-items:center; gap:8px; flex-shrink:0; font-size:12px; color:var(--ss-primary); }
.import-history[open] .import-history-toggle > i { transform:rotate(180deg); }
.import-history-content { padding:0 14px 14px; }
.import-history-toolbar { display:flex; align-items:center; justify-content:space-between; gap:12px; padding:0 0 10px; }
.import-history-list { display:grid; gap:8px; max-height:180px; overflow:auto; overscroll-behavior:contain; padding-right:4px; scrollbar-gutter:stable; }
.import-history-item { display:flex; justify-content:space-between; align-items:center; gap:12px; width:100%; border:1px solid var(--ss-border); border-radius:8px; background:#fff; color:var(--ss-text); padding:10px 12px; text-align:left; font:inherit; cursor:pointer; }
.import-history-item b { font-size:12px; font-weight:600; }
.import-history-item small { display:block; color:var(--ss-muted); font-size:11px; margin-top:3px; }
.import-history-stats { display:flex; align-items:center; justify-content:flex-end; flex-wrap:wrap; gap:10px; font-size:11px; }
.import-history-item:hover { background:var(--ss-primary-soft); border-color:#b9d3f3; }
.import-history-item:disabled { opacity:.6; cursor:wait; }
@media(max-width:640px) { .import-history-summary,.import-history-toolbar { align-items:flex-start; }.import-history-toggle { font-size:0; }.import-history-toggle i { font-size:13px; }.import-history-item { align-items:flex-start; flex-direction:column; }.import-history-stats { justify-content:flex-start; }.import-modal { max-height:calc(100dvh - 20px); }.import-table { max-height:300px; } }
@media(max-width:640px) { .import-overlay { padding:10px; }.import-body,.import-head,.import-footer { padding:14px; }.import-head p { display:none; }.import-counts>span { flex:1; }.import-file input { width:100%; } }
</style>
