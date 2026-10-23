<script setup>
import { computed, ref } from 'vue'
import scheduleService from '../services/scheduleService'
const emit = defineEmits(['close', 'imported'])
const file = ref(null), preview = ref(null), result = ref(null), error = ref('')
const reading = ref(false), saving = ref(false), downloading = ref(false), onlyErrors = ref(false)
const busy = computed(() => reading.value || saving.value || downloading.value)
const rows = computed(() => (preview.value?.dong || []).filter(r => !onlyErrors.value || r.loi.length))
const canConfirm = computed(() => !busy.value && preview.value?.tongDong > 0 && preview.value.coLoi === 0)
const message = e => e?.response?.data?.message || e?.message || 'Không thực hiện được, vui lòng thử lại.'
function choose(value) {
  if (busy.value) return
  file.value = null; preview.value = null; result.value = null; error.value = ''; onlyErrors.value = false
  if (!value) return
  if (!/\.(xlsx|xls)$/i.test(value.name)) { error.value = 'Chỉ nhận file .xlsx hoặc .xls.'; return }
  if (value.size > 5 * 1024 * 1024) { error.value = 'File Excel tối đa 5 MB.'; return }
  file.value = value
}
async function download() {
  if (busy.value) return
  downloading.value = true; error.value = ''
  try { await scheduleService.downloadTemplate() } catch (e) { error.value = message(e) }
  finally { downloading.value = false }
}
async function read() {
  if (!file.value || busy.value) return
  reading.value = true; preview.value = null; result.value = null; error.value = ''; onlyErrors.value = false
  try { preview.value = await scheduleService.previewImport(file.value) }
  catch (e) { error.value = message(e) } finally { reading.value = false }
}
async function confirm() {
  if (!canConfirm.value) return
  saving.value = true; error.value = ''
  try {
    result.value = await scheduleService.confirmImport({ dong: preview.value.dong.map(r => r.duLieu) })
    preview.value = null; emit('imported', result.value)
  } catch (e) {
    const changed = e?.response?.data?.data
    if (changed?.dong) preview.value = changed
    error.value = message(e)
    if (!e?.response?.status || e.response.status >= 500) error.value += ' Kiểm tra danh sách lịch hoặc xác nhận lại; lịch đã có sẽ được bỏ qua.'
  } finally { saving.value = false }
}
</script>
<template>
  <Teleport to="body">
    <div class="llv-import-overlay" @click.self="!busy && emit('close')">
      <section class="llv-import-modal" role="dialog" aria-modal="true" aria-labelledby="llv-import-title" :aria-busy="busy">
        <header class="llv-import-head"><div><h2 id="llv-import-title"><i class="bi bi-file-earmark-excel"></i> Nhập Excel lịch làm việc</h2><p>Kiểm tra toàn bộ dữ liệu trước khi lưu lịch.</p></div><button type="button" class="llv-import-close" aria-label="Đóng nhập Excel" :disabled="busy" @click="emit('close')"><i class="bi bi-x-lg"></i></button></header>
        <div class="llv-import-body">
          <p v-if="error" class="llv-import-alert danger" role="alert">{{ error }}</p>
          <div class="llv-import-file" @dragover.prevent @drop.prevent="choose($event.dataTransfer.files?.[0])"><label for="llv-import-file">Chọn file Excel (.xlsx, .xls · tối đa 5 MB / 500 dòng)</label><div class="llv-import-file-actions"><input id="llv-import-file" type="file" accept=".xlsx,.xls" :disabled="busy" @change="choose($event.target.files?.[0])"><button type="button" class="llv-import-btn" :disabled="busy" @click="download">{{ downloading ? 'Đang tải…' : 'Tải file mẫu' }}</button><button type="button" class="llv-import-btn primary" :disabled="busy || !file" @click="read">{{ reading ? 'Đang kiểm tra…' : 'Xem trước dữ liệu' }}</button></div><small>Nhập mã nhân viên, mã ca, ngày và ghi chú tối đa 500 ký tự. Tên nhân viên/tên ca trong Excel là cột tự động. Lịch đã có sẽ được bỏ qua.</small></div>
          <p v-if="result" class="llv-import-alert success" role="status">{{ result.thongBao }}</p>
          <template v-if="preview">
            <div class="llv-import-counts"><span>Tổng dòng <b>{{ preview.tongDong }}</b></span><span class="ok">Có thể tạo <b>{{ preview.hopLe }}</b></span><span>Đã tồn tại <b>{{ preview.daTonTai }}</b></span><span class="bad">Có lỗi <b>{{ preview.coLoi }}</b></span></div>
            <p class="llv-import-alert" :class="preview.coLoi ? 'danger' : 'success'">{{ preview.coLoi ? 'Chưa lưu dữ liệu. Sửa hết lỗi trong Excel rồi chọn lại file và xem trước.' : 'Dữ liệu hợp lệ. Khi xác nhận, hệ thống kiểm tra lại; lịch đã tồn tại sẽ được bỏ qua.' }}</p>
            <div class="llv-import-table-head"><h3>Thông tin từng dòng</h3><label><input v-model="onlyErrors" type="checkbox"> Chỉ hiện dòng lỗi</label></div>
            <div class="llv-import-table" tabindex="0" role="region" aria-label="Bảng xem trước lịch làm việc"><table><thead><tr><th>Dòng</th><th>Nhân viên</th><th>Ca làm việc</th><th>Ngày làm</th><th>Ghi chú</th><th>Kết quả kiểm tra</th></tr></thead><tbody><tr v-for="row in rows" :key="row.duLieu.soDong" :class="{ invalid: row.loi.length }"><td>{{ row.duLieu.soDong }}</td><td><b>{{ row.duLieu.maNhanVien || '—' }}</b><small>{{ row.tenNhanVien || '' }}</small></td><td><b>{{ row.duLieu.maCa || '—' }}</b><small>{{ row.tenCa || '' }}</small></td><td class="nowrap">{{ row.duLieu.ngayLam || '—' }}</td><td class="note">{{ row.duLieu.ghiChu || '—' }}</td><td class="validation"><ul v-if="row.loi.length"><li v-for="item in row.loi" :key="item">{{ item }}</li></ul><span v-else-if="row.daTonTai">Đã tồn tại · sẽ bỏ qua</span><span v-else class="ok">Hợp lệ</span></td></tr><tr v-if="!rows.length"><td colspan="6">Không có dòng phù hợp.</td></tr></tbody></table></div>
          </template>
        </div>
        <footer class="llv-import-footer"><span v-if="saving" role="status">Đang lưu toàn bộ lô…</span><button type="button" class="llv-import-btn" :disabled="busy" @click="emit('close')">Đóng</button><button v-if="preview" type="button" class="llv-import-btn primary" :disabled="!canConfirm" @click="confirm">{{ saving ? 'Đang nhập…' : 'Xác nhận nhập' }}</button></footer>
      </section>
    </div>
  </Teleport>
</template>
<style scoped>
.llv-import-overlay{position:fixed;inset:0;z-index:1300;display:grid;place-items:center;padding:20px;background:#0f172a75;color:#17365f;font:13px var(--ss-font,Arial,sans-serif)}
.llv-import-modal{display:flex;flex-direction:column;width:min(1200px,100%);max-height:calc(100dvh - 40px);min-height:0;overflow:hidden;border:1px solid #dde6f0;border-radius:16px;background:white;box-shadow:0 24px 80px #0f172a30}
.llv-import-head,.llv-import-footer{display:flex;align-items:center;justify-content:space-between;gap:12px;flex:0 0 auto;padding:18px 22px;border-bottom:1px solid #dde6f0}.llv-import-head h2{margin:0;font-size:18px}.llv-import-head h2 i{margin-right:8px;color:#2879e3}.llv-import-head p{margin:6px 0 0;color:#64748b}.llv-import-close{width:34px;height:34px;flex:0 0 auto;border:1px solid #dde6f0;border-radius:50%;background:#f8fafc;color:#64748b;cursor:pointer}
.llv-import-body{display:flex;flex-direction:column;gap:14px;flex:1 1 auto;min-height:0;overflow:auto;padding:20px 22px}.llv-import-body>*{flex-shrink:0}.llv-import-file{display:grid;gap:12px;padding:16px;border:1px dashed #b9d3f3;border-radius:12px;background:#f8fbff}.llv-import-file>label{font-weight:600}.llv-import-file small{color:#64748b;line-height:1.6}.llv-import-file-actions{display:flex;align-items:center;gap:10px;flex-wrap:wrap}.llv-import-file-actions input{flex:1;min-width:220px;max-width:100%}
.llv-import-btn{display:inline-flex;align-items:center;justify-content:center;gap:8px;min-height:40px;padding:0 16px;border:1px solid #dde6f0;border-radius:8px;background:#fff;color:#2879e3;font:inherit;font-weight:600;cursor:pointer}.llv-import-btn.primary{background:#2879e3;border-color:#2879e3;color:#fff}.llv-import-btn:disabled,.llv-import-close:disabled{opacity:.5;cursor:not-allowed}.llv-import-btn:focus-visible,.llv-import-table:focus-visible{outline:2px solid #2879e3;outline-offset:2px}
.llv-import-alert{margin:0;padding:12px 14px;border:1px solid #d5e6fc;border-radius:9px;background:#eff6ff;line-height:1.6}.llv-import-alert.danger{background:#fff3f4;color:#b8262b;border-color:#ffd1d7}.llv-import-alert.success{background:#effbf6;color:#087b54;border-color:#c6efdf}.llv-import-counts{display:flex;gap:10px;flex-wrap:wrap}.llv-import-counts>span{padding:10px 14px;border:1px solid #dde6f0;border-radius:9px;background:#f8fafc}.llv-import-counts b{margin-left:8px;font-size:18px}.ok{color:#087b54}.bad,.validation ul{color:#b8262b}
.llv-import-table-head{display:flex;align-items:center;justify-content:space-between;gap:10px}.llv-import-table-head h3{margin:0;font-size:14px}.llv-import-table{flex:0 0 auto;min-height:120px;max-height:min(420px,48vh);overflow:auto;border:1px solid #dde6f0;border-radius:10px}.llv-import-table table{width:100%;border-collapse:separate;border-spacing:0;font-size:12px}.llv-import-table th,.llv-import-table td{padding:12px;min-width:110px;border-bottom:1px solid #eaf0f6;text-align:left;vertical-align:top}.llv-import-table th{position:sticky;top:0;z-index:1;background:#f3f7fc;white-space:nowrap}.llv-import-table th:first-child,.llv-import-table td:first-child{min-width:48px}.llv-import-table .note{min-width:240px;max-width:340px;white-space:normal;overflow-wrap:break-word;line-height:1.6}.llv-import-table .validation{min-width:260px}.llv-import-table small{display:block;margin-top:5px;color:#64748b}.llv-import-table ul{margin:0;padding-left:16px;line-height:1.7}.invalid{background:#fff8f8}.nowrap{white-space:nowrap}.llv-import-footer{justify-content:flex-end;border-top:1px solid #dde6f0;border-bottom:0;background:#f8fafc}.llv-import-footer>span{margin-right:auto}
@media(max-width:640px){.llv-import-overlay{padding:10px}.llv-import-modal{max-height:calc(100dvh - 20px)}.llv-import-head,.llv-import-body,.llv-import-footer{padding:14px}.llv-import-head p{display:none}.llv-import-file-actions input{flex-basis:100%}.llv-import-table-head{align-items:flex-start;flex-direction:column}}
</style>
