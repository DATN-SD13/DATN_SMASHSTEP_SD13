<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useAuthStore } from '../../../../stores/auth'
import { sanPhamImportService, taiMauSanPham } from '../services/sanPhamImportService'

const emit = defineEmits(['close', 'imported'])
const auth = useAuthStore()
const input = ref(null), file = ref(null), preview = ref(null), result = ref(null), error = ref('')
const reading = ref(false), saving = ref(false), downloading = ref(false), restoring = ref(false), onlyErrors = ref(false)
const busy = computed(() => reading.value || saving.value || downloading.value || restoring.value)
const canConfirm = computed(() => preview.value?.tongDong > 0 && preview.value.coLoi === 0 && !busy.value && !result.value)
const rows = computed(() => (preview.value?.dong || []).filter(row => !onlyErrors.value || row.loi.length))
const recoveryKey = `ss_product_import_${auth.user?.id || auth.user?.username || 'employee'}`
let alive = true, reported = ''
const message = e => e?.response?.data?.message || e?.message || 'Không thực hiện được. Vui lòng thử lại.'
function remember(id) { try { localStorage.setItem(recoveryKey, id) } catch {} }
function showResult(value) {
  if (!alive) return
  result.value = value; preview.value = null; remember(value.maLo)
  if (reported !== value.maLo) { reported = value.maLo; emit('imported', value) }
}
function chooseFile(event) { file.value = event.target.files?.[0] || null; preview.value = null; result.value = null; error.value = ''; onlyErrors.value = false }
async function download() {
  if (busy.value) return
  downloading.value = true; error.value = ''
  try { await taiMauSanPham() } catch (e) { if (alive) error.value = message(e) }
  finally { if (alive) downloading.value = false }
}
async function readFile() {
  if (!file.value || busy.value) return
  error.value = ''; preview.value = null; result.value = null; onlyErrors.value = false
  if (!/\.(xlsx|xls)$/i.test(file.value.name)) { error.value = 'Chỉ nhận file .xlsx hoặc .xls.'; return }
  if (file.value.size > 5 * 1024 * 1024) { error.value = 'File Excel tối đa 5 MB.'; return }
  reading.value = true
  try { const value = await sanPhamImportService.preview(file.value); if (alive) preview.value = value }
  catch (e) { if (alive) error.value = message(e) }
  finally { if (alive) reading.value = false }
}
async function confirmImport() {
  if (!canConfirm.value) return
  const current = preview.value
  saving.value = true; error.value = ''; remember(current.maLo)
  try { showResult(await sanPhamImportService.confirm({ maLo: current.maLo, dong: current.dong.map(row => ({ soDong: row.soDong, giaTri: row.giaTri })) })) }
  catch (e) {
    if (!alive) return
    const invalid = e?.response?.data?.data
    if (invalid?.dong && invalid?.maLo) { preview.value = invalid; error.value = message(e) }
    else if (e?.response?.status && e.response.status < 500) error.value = message(e)
    else {
      try { showResult(await sanPhamImportService.result(current.maLo)) }
      catch { if (alive) error.value = `${message(e)} Có thể phản hồi bị gián đoạn. Bấm Xác nhận nhập lại để dùng cùng mã lô, không tạo trùng.` }
    }
  } finally { if (alive) saving.value = false }
}
function displayCell(value, index) {
  const text = String(value ?? '').trim()
  if (index === 9 || index === 15) {
    if (!text || text === '1') return 'Hoạt động'
    if (text === '0') return 'Ngừng hoạt động'
  }
  if (index === 14) {
    if (!text || text === '1') return 'Có'
    if (text === '0') return 'Không'
  }
  return text || '—'
}
function newFile() { result.value = null; preview.value = null; file.value = null; error.value = ''; onlyErrors.value = false; if (input.value) input.value.value = '' }
onMounted(async () => {
  let id; try { id = localStorage.getItem(recoveryKey) } catch {}
  if (!id) return
  restoring.value = true
  try { showResult(await sanPhamImportService.result(id)) } catch {} finally { if (alive) restoring.value = false }
})
onBeforeUnmount(() => { alive = false })
</script>

<template>
  <Teleport to="body">
    <div class="sp-import-overlay" @click.self="!busy && emit('close')">
      <section class="sp-import-modal" role="dialog" aria-modal="true" aria-labelledby="sp-import-title" :aria-busy="busy">
        <header class="sp-import-head"><div class="sp-import-title"><span class="sp-import-icon"><i class="bi bi-file-earmark-excel"></i></span><div><h2 id="sp-import-title">Nhập Excel sản phẩm</h2><p>Tạo sản phẩm và biến thể mới sau khi kiểm tra toàn bộ dữ liệu.</p></div></div><button class="sp-import-close" type="button" aria-label="Đóng nhập Excel" :disabled="busy" @click="emit('close')"><i class="bi bi-x-lg"></i></button></header>
        <div class="sp-import-body">
          <p v-if="error" class="sp-import-alert danger" role="alert">{{ error }}</p>
          <p v-if="restoring" class="sp-import-alert">Đang kiểm tra kết quả lô nhập trước…</p>
          <template v-if="!result">
            <div class="sp-import-file"><label for="sp-import-file">Chọn file Excel (.xlsx, .xls · tối đa 5 MB / 500 dòng)</label><div class="sp-import-file-actions"><input id="sp-import-file" ref="input" type="file" accept=".xlsx,.xls" :disabled="busy" @change="chooseFile"/><button class="sp-import-btn" type="button" :disabled="busy" @click="download"><i class="bi bi-download"></i> {{ downloading ? 'Đang tải…' : 'Tải file mẫu' }}</button><button class="sp-import-btn primary" type="button" :disabled="busy || !file" @click="readFile">{{ reading ? 'Đang kiểm tra…' : 'Xem trước dữ liệu' }}</button></div><small>Mỗi dòng là một biến thể. Dùng cùng “Nhóm sản phẩm” để ghép các màu–size của một sản phẩm. Nhập tên thuộc tính như Nike, Đen và kích thước 40, 41 theo các sheet trong file mẫu; không nhập ID. File gồm 13 cột; hệ thống tự đặt trạng thái Hoạt động và kích hoạt Có.</small></div>
            <p class="sp-import-hint">Mã sản phẩm, mã biến thể và SKU được tự sinh khi lưu. Chức năng này không ghi đè hoặc cộng tồn sản phẩm đã có; thêm ảnh sau tại trang sản phẩm.</p>
          </template>
          <template v-if="preview">
            <div class="sp-import-counts"><span>Sản phẩm <b>{{ preview.soSanPham }}</b></span><span>Biến thể <b>{{ preview.tongDong }}</b></span><span class="ok">Hợp lệ <b>{{ preview.hopLe }}</b></span><span class="bad">Có lỗi <b>{{ preview.coLoi }}</b></span></div>
            <p class="sp-import-alert" :class="preview.coLoi ? 'danger' : 'success'">{{ preview.coLoi ? 'Chưa thể nhập. Sửa hết lỗi trong Excel, lưu file rồi chọn lại và xem trước.' : 'Dữ liệu hợp lệ. Khi xác nhận, hệ thống kiểm tra lại và lưu toàn bộ trong một giao dịch.' }}</p>
            <div class="sp-import-table-head"><h3>Thông tin từng dòng</h3><label><input v-model="onlyErrors" type="checkbox"/> Chỉ hiện dòng lỗi</label></div>
            <div class="sp-import-table" role="region" aria-label="Bảng xem trước sản phẩm" tabindex="0"><table><thead><tr><th>Dòng</th><th>Kết quả kiểm tra</th><th v-for="(column, index) in preview.cot" :key="column" :class="{ 'sp-import-description': index === 8 }">{{ column }}</th></tr></thead><tbody><tr v-for="row in rows" :key="row.soDong" :class="{ 'sp-import-invalid': row.loi.length }"><td>{{ row.soDong }}</td><td class="sp-import-errors"><ul v-if="row.loi.length"><li v-for="item in row.loi" :key="item">{{ item }}</li></ul><span v-else class="ok">Hợp lệ</span></td><td v-for="(value, index) in row.giaTri" :key="index" :class="{ 'sp-import-description': index === 8 }">{{ displayCell(value, index) }}</td></tr><tr v-if="!rows.length"><td :colspan="preview.cot.length + 2">Không có dòng phù hợp.</td></tr></tbody></table></div>
          </template>
          <template v-if="result">
            <div class="sp-import-alert success"><b>Nhập thành công {{ result.soSanPham }} sản phẩm và {{ result.soBienThe }} biến thể.</b><br/>Tồn kho được lưu theo số lượng của từng dòng. Có thể đóng cửa sổ để xem trên danh sách.</div>
            <p class="sp-import-hint">Mã lô: {{ result.maLo }}</p>
            <div class="sp-import-table"><table><thead><tr><th>Nhóm trong Excel</th><th>Mã sản phẩm đã tạo</th><th>Số biến thể</th></tr></thead><tbody><tr v-for="product in result.sanPham" :key="product.id"><td>{{ product.nhom }}</td><td><b>{{ product.maSanPham }}</b></td><td>{{ product.soBienThe }}</td></tr></tbody></table></div>
          </template>
        </div>
        <footer class="sp-import-footer"><span v-if="saving" role="status">Đang lưu toàn bộ lô, vui lòng chờ…</span><div><button class="sp-import-btn" type="button" :disabled="busy" @click="emit('close')">Đóng</button><button v-if="result" class="sp-import-btn primary" type="button" :disabled="busy" @click="newFile">Nhập file khác</button><button v-else class="sp-import-btn primary" type="button" :disabled="!canConfirm" @click="confirmImport">{{ saving ? 'Đang lưu…' : 'Xác nhận nhập' }}</button></div></footer>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.sp-import-overlay{position:fixed;inset:0;z-index:1200;display:grid;place-items:center;padding:20px;background:rgba(15,23,42,.46);color:#17365f;font-family:var(--ss-font,Arial,sans-serif);font-size:13px}
.sp-import-modal{width:min(1280px,100%);max-height:calc(100vh - 40px);max-height:calc(100dvh - 40px);min-height:0;display:flex;flex-direction:column;border-radius:16px;border:1px solid #dde6f0;background:#fff;overflow:hidden;box-shadow:0 24px 80px #0f172a30}
.sp-import-head,.sp-import-footer{flex:0 0 auto;display:flex;align-items:center;justify-content:space-between;gap:16px;padding:18px 22px;border-bottom:1px solid #dde6f0}
.sp-import-title{display:flex;align-items:center;gap:12px}.sp-import-title h2{margin:0;font-size:18px;font-weight:600}.sp-import-title p{margin:5px 0 0;color:#718096;font-size:12px}.sp-import-icon{width:40px;height:40px;background:#eaf4ff;color:#2879e3;border-radius:10px;display:grid;place-items:center;font-size:20px}.sp-import-close{border:1px solid #dde6f0;background:#f8fafc;color:#718096;border-radius:50%;width:34px;height:34px;cursor:pointer}
.sp-import-body{flex:1 1 auto;min-height:0;overflow:auto;display:flex;flex-direction:column;gap:14px;padding:20px 22px}.sp-import-body>*{flex-shrink:0}.sp-import-file{padding:16px;border:1px dashed #b9d3f3;background:#f8fbff;border-radius:12px;display:grid;gap:12px}.sp-import-file label{font-weight:600}.sp-import-file small,.sp-import-hint{font-size:12px;line-height:1.6;color:#718096;margin:0}.sp-import-file-actions{display:flex;align-items:center;gap:10px;flex-wrap:wrap}.sp-import-file-actions input{flex:1;min-width:220px;max-width:100%}
.sp-import-btn{display:inline-flex;align-items:center;justify-content:center;gap:8px;min-height:40px;padding:0 16px;border:1px solid #dde6f0;border-radius:8px;background:#fff;color:#2879e3;font:inherit;font-weight:600;cursor:pointer}.sp-import-btn.primary{background:#2879e3;border-color:#2879e3;color:#fff}.sp-import-btn:disabled,.sp-import-close:disabled{opacity:.5;cursor:not-allowed}.sp-import-btn:focus-visible,.sp-import-table:focus-visible{outline:2px solid #2879e3;outline-offset:2px}
.sp-import-alert{margin:0;padding:12px 14px;border-radius:9px;border:1px solid #d5e6fc;background:#eff6ff;line-height:1.6}.sp-import-alert.danger{color:#b8262b;background:#fff3f4;border-color:#ffd1d7}.sp-import-alert.success{color:#087b54;background:#effbf6;border-color:#c6efdf}.sp-import-counts{display:flex;gap:10px;flex-wrap:wrap}.sp-import-counts>span{padding:10px 14px;border:1px solid #dde6f0;border-radius:9px;background:#f8fafc}.sp-import-counts b{font-size:18px;margin-left:10px}.ok{color:#087b54}.bad{color:#b8262b}
.sp-import-table-head{display:flex;justify-content:space-between;align-items:center;gap:12px}.sp-import-table-head h3{font-size:14px;margin:0}.sp-import-table-head label{display:flex;gap:7px;align-items:center;font-size:12px}.sp-import-table{flex:0 0 auto;min-height:120px;max-height:min(400px,48vh);overflow:auto;border:1px solid #dde6f0;border-radius:10px}.sp-import-table table{width:100%;border-collapse:separate;border-spacing:0;font-size:12px}.sp-import-table th,.sp-import-table td{padding:12px;border-bottom:1px solid #eaf0f6;text-align:left;vertical-align:top;min-width:95px;max-width:300px;overflow-wrap:anywhere}.sp-import-table th{position:sticky;top:0;z-index:1;background:#f3f7fc;font-weight:600;white-space:nowrap}.sp-import-table td:nth-child(2){min-width:260px}.sp-import-table th.sp-import-description,.sp-import-table td.sp-import-description{min-width:300px;width:300px;max-width:420px;white-space:normal;overflow-wrap:break-word;line-height:1.65}.sp-import-errors ul{margin:0;padding-left:16px;line-height:1.7;color:#b8262b}.sp-import-invalid{background:#fff8f8}.sp-import-footer{border-top:1px solid #dde6f0;border-bottom:0;background:#f8fafc;justify-content:flex-end}.sp-import-footer>div{display:flex;gap:10px}.sp-import-footer>span{margin-right:auto;font-size:12px}
@media(max-width:640px){.sp-import-overlay{padding:10px}.sp-import-modal{max-height:calc(100dvh - 20px)}.sp-import-body,.sp-import-head,.sp-import-footer{padding:14px}.sp-import-title p{display:none}.sp-import-file-actions input{flex-basis:100%}.sp-import-footer{flex-wrap:wrap}.sp-import-counts>span{flex:1}.sp-import-table-head{align-items:flex-start;flex-direction:column}}
</style>
