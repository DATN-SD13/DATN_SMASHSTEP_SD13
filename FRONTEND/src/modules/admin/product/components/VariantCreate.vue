<script setup>
import { ref, reactive, computed } from 'vue'
import { productService, errorMessage } from '../services/productService'
const props = defineProps({ product: { type: Object, required: true }, existing: { type: Array, default: () => [] }, options: Object })
const emit = defineEmits(['created'])
const colorIds = ref([]), sizeIds = ref([]), drafts = ref([]), error = ref(''), saving = ref(false)
const defaults = reactive({ price: '', quantity: 0 })
const colors = computed(() => (props.options?.colors || []).filter(a => a.trangThai === 1))
const sizes = computed(() => (props.options?.sizes || []).filter(a => a.trangThai === 1))
function generate() {
  error.value = ''
  if (!colorIds.value.length || !sizeIds.value.length) { error.value = 'Chọn ít nhất một màu và một kích thước.'; return }
  if (colorIds.value.length * sizeIds.value.length > 200) { error.value = 'Mỗi lần tạo tối đa 200 tổ hợp màu × kích thước.'; return }
  if (drafts.value.length && !window.confirm('Tạo lại danh sách sẽ thay thế các dòng chưa lưu. Tiếp tục?')) return
  const existing = new Set(props.existing.map(v => `${v.mauSacId}:${v.kichThuocId}`))
  drafts.value = colorIds.value.flatMap(colorId => sizeIds.value.filter(sizeId => !existing.has(`${colorId}:${sizeId}`)).map(sizeId => {
    const code = `${props.product.maSanPham}-${colorId}-${sizeId}`
    return { sanPhamId: props.product.id, mauSacId: colorId, kichThuocId: sizeId,
      maChiTietSanPham: code, sku: code, soLuong: defaults.quantity, giaBan: defaults.price, kichHoat: true, trangThai: 1 }
  }))
  if (!drafts.value.length) error.value = 'Tất cả tổ hợp đã có biến thể. Hãy chọn màu hoặc kích thước khác.'
}
function label(type, id) { return props.options?.[type]?.find(a => a.id === id)?.ten || id }
async function save() {
  if (saving.value || !drafts.value.length) return
  error.value = ''
  const codes = new Set(), skus = new Set()
  for (const row of drafts.value) {
    row.maChiTietSanPham = row.maChiTietSanPham.trim(); row.sku = row.sku.trim()
    if (![row.maChiTietSanPham, row.sku].every(v => /^[A-Za-z0-9][A-Za-z0-9._-]*$/.test(v))) {
      error.value = 'Mã biến thể và SKU chỉ gồm chữ Latin, số, dấu chấm, gạch dưới hoặc gạch ngang.'; return
    }
    const code = row.maChiTietSanPham.toLowerCase(), sku = row.sku.toLowerCase()
    if (codes.has(code) || skus.has(sku)) { error.value = 'Mã biến thể hoặc SKU bị trùng trong danh sách.'; return }
    codes.add(code); skus.add(sku)
  }
  saving.value = true
  try {
    await productService.createVariants(props.product.id, drafts.value.map(r => ({ ...r, giaBan: Number(r.giaBan), soLuong: Number(r.soLuong) })))
    drafts.value = []; colorIds.value = []; sizeIds.value = []
    emit('created')
  } catch (e) { error.value = errorMessage(e) } finally { saving.value = false }
}
</script>
<template>
  <section class="p-card">
    <h2><i class="bi bi-plus-square"></i> Thêm biến thể màu sắc × kích thước</h2>
    <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
    <form @submit.prevent="generate">
      <fieldset :disabled="saving">
        <div class="p-form-grid"><div><label>Màu sắc</label><div class="p-checks"><label v-for="c in colors" :key="c.id"><input v-model="colorIds" type="checkbox" :value="c.id" />{{ c.ten }}</label></div><small v-if="!colors.length">Chưa có màu sắc hoạt động. Thêm tại trang thuộc tính.</small></div><div><label>Kích thước</label><div class="p-checks"><label v-for="s in sizes" :key="s.id"><input v-model="sizeIds" type="checkbox" :value="s.id" />{{ s.ten }}</label></div><small v-if="!sizes.length">Chưa có kích thước hoạt động. Thêm tại trang thuộc tính.</small></div><label>Giá bán mặc định (VNĐ)<input v-model.number="defaults.price" type="number" required min="0.01" step="0.01" /></label><label>Số lượng mặc định<input v-model.number="defaults.quantity" type="number" required min="0" max="2147483647" step="1" /></label></div>
        <div class="p-form-actions"><button class="p-btn" :disabled="saving || !colors.length || !sizes.length"><i class="bi bi-layers"></i> Tạo danh sách tổ hợp</button><small>Tổ hợp đã tồn tại sẽ được bỏ qua. Mã/SKU gợi ý có thể sửa trước khi lưu.</small></div>
      </fieldset>
    </form>
    <form v-if="drafts.length" @submit.prevent="save">
      <div class="p-table-wrap" style="margin-top:18px"><table><thead><tr><th>Màu / Size</th><th>Mã chi tiết</th><th>SKU</th><th>Số lượng</th><th>Giá bán</th><th>Kích hoạt</th><th>Trạng thái</th><th></th></tr></thead><tbody><tr v-for="(row, index) in drafts" :key="row.mauSacId + ':' + row.kichThuocId"><td class="nowrap">{{ label('colors', row.mauSacId) }} / {{ label('sizes', row.kichThuocId) }}</td><td><input v-model="row.maChiTietSanPham" required maxlength="100" aria-label="Mã chi tiết" style="min-width:170px" :disabled="saving" /></td><td><input v-model="row.sku" required maxlength="100" aria-label="SKU" style="min-width:170px" :disabled="saving" /></td><td><input v-model.number="row.soLuong" type="number" required min="0" max="2147483647" step="1" aria-label="Số lượng" style="min-width:95px" :disabled="saving" /></td><td><input v-model.number="row.giaBan" type="number" required min="0.01" step="0.01" aria-label="Giá bán" style="min-width:130px" :disabled="saving" /></td><td><input v-model="row.kichHoat" type="checkbox" aria-label="Kích hoạt" :disabled="saving" /></td><td><select v-model.number="row.trangThai" aria-label="Trạng thái" :disabled="saving"><option :value="1">Hoạt động</option><option :value="0">Ngừng</option></select></td><td><button class="p-btn danger" type="button" :disabled="saving" aria-label="Bỏ dòng chưa lưu" @click="drafts.splice(index, 1)"><i class="bi bi-x-lg"></i></button></td></tr></tbody></table></div>
      <div class="p-form-actions"><button class="p-btn primary" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu ' + drafts.length + ' biến thể' }}</button></div>
    </form>
  </section>
</template>

