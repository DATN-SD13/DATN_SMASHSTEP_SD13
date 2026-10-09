<script setup>
import { ref, reactive, computed } from 'vue'
import { productService, variantService, validateVariant, money } from '../services/productService'
import ConfirmModal from './ConfirmModal.vue'
import Select2Control from './Select2Control.vue'
import ProductImagePicker from './ProductImagePicker.vue'
import { validateImageFile } from '../services/imageUtils'
import { errorMessage } from '../services/productService'
import { useConfirmation } from '../composables/useConfirmation'
const props = defineProps({ product: { type: Object, required: true }, existing: { type: Array, default: () => [] }, options: Object })
const emit = defineEmits(['created'])
const colorIds = ref([]), sizeIds = ref([]), drafts = ref([]), error = ref(''), pendingVariants = ref([])
const { confirmation, confirming: saving, confirmError, askConfirmation, cancelConfirmation, confirmAction } = useConfirmation()
const createdBatch = ref(false), progress = ref('')
const defaults = reactive({ price: '', quantity: 0 })
const colors = computed(() => (props.options?.colors || []).filter(a => a.trangThai === 1))
const sizes = computed(() => (props.options?.sizes || []).filter(a => a.trangThai === 1))
function generate() {
  if (saving.value || confirmation.value || createdBatch.value) return
  error.value = ''
  if (!colorIds.value.length || !sizeIds.value.length) { error.value = 'Chọn ít nhất một màu và một kích thước.'; return }
  if (colorIds.value.length * sizeIds.value.length > 200) { error.value = 'Mỗi lần tạo tối đa 200 tổ hợp màu × kích thước.'; return }
  if (drafts.value.length) {
    askConfirmation({ title: 'Tạo lại danh sách tổ hợp', message: 'Danh sách mới sẽ thay thế các dòng chưa lưu. Bạn có muốn tiếp tục?' }, replaceDrafts)
    return
  }
  replaceDrafts()
}
function replaceDrafts() {
  const existing = new Set(props.existing.map(v => `${v.mauSacId}:${v.kichThuocId}`))
  drafts.value = colorIds.value.flatMap(colorId => sizeIds.value.filter(sizeId => !existing.has(`${colorId}:${sizeId}`)).map(sizeId => {
    const code = `${props.product.maSanPham}-${colorId}-${sizeId}`
    return { sanPhamId: props.product.id, mauSacId: colorId, kichThuocId: sizeId,
      maChiTietSanPham: code, sku: code, soLuong: defaults.quantity, giaBan: defaults.price, kichHoat: true, trangThai: 1, images: [], imagesOpen: false, savedId: null }
  }))
  if (!drafts.value.length) error.value = 'Tất cả tổ hợp đã có biến thể. Hãy chọn màu hoặc kích thước khác.'
}
function label(type, id) { return props.options?.[type]?.find(a => a.id === id)?.ten || id }
async function persist(data) {
  // The created IDs survive an upload failure. Retrying never posts this batch again.
  if (!createdBatch.value) {
    const created = await productService.createVariants(props.product.id, data)
    createdBatch.value = true
    for (const row of drafts.value) {
      const variant = created.find(item => `${item.mauSacId}:${item.kichThuocId}` === `${row.mauSacId}:${row.kichThuocId}`)
        || created.find(item => item.maChiTietSanPham === row.maChiTietSanPham.trim())
      row.savedId = variant?.id
    }
    emit('created')
  }
  try {
    for (const row of drafts.value) {
      if (!row.savedId) throw new Error('Không xác định được ID biến thể đã tạo. Vui lòng tải lại chi tiết sản phẩm.')
      for (const image of [...row.images]) {
        progress.value = `Đang tải ảnh ${label('colors', row.mauSacId)} / ${label('sizes', row.kichThuocId)}: ${image.file.name}`
        await variantService.uploadImage(row.savedId, image)
        row.images = row.images.filter(item => item.key !== image.key)
      }
    }
  } catch (e) {
    error.value = 'Biến thể đã được tạo nhưng tải ảnh thất bại. Các ảnh chưa lưu được giữ để thử lại. ' + (e.response ? errorMessage(e) : e.message)
    if (confirmation.value) confirmation.value = { ...confirmation.value, title: 'Thử lại ảnh biến thể', confirmText: 'Thử lại ảnh',
      message: 'Các biến thể đã được lưu. Chỉ tải tiếp những ảnh chưa thành công.' }
    throw e
  } finally { progress.value = ''; emit('created') }
  drafts.value = []; colorIds.value = []; sizeIds.value = []; createdBatch.value = false; error.value = ''
}
function save() {
  if (saving.value || !drafts.value.length) return
  error.value = ''
  const payload = drafts.value.map(({ images, imagesOpen, savedId, ...row }) => ({ ...row, maChiTietSanPham: row.maChiTietSanPham.trim(), sku: row.sku.trim() }))
  for (const row of drafts.value) for (const image of row.images) {
    const message = validateImageFile(image.file)
    if (message) { error.value = message; return }
  }
  const codes = new Set(), skus = new Set()
  for (const row of payload) {
    error.value = validateVariant(row)
    if (error.value) return
    const code = row.maChiTietSanPham.toLowerCase(), sku = row.sku.toLowerCase()
    if (codes.has(code) || skus.has(sku)) { error.value = 'Mã biến thể hoặc SKU bị trùng trong danh sách.'; return }
    codes.add(code); skus.add(sku)
  }
  const data = payload.map(row => ({ ...row, giaBan: Number(row.giaBan), soLuong: Number(row.soLuong) }))
  pendingVariants.value = data
  askConfirmation({ title: createdBatch.value ? 'Thử lại ảnh biến thể' : 'Xác nhận thêm biến thể', message: createdBatch.value ? 'Các biến thể đã được lưu. Chỉ tải tiếp những ảnh chưa thành công.' : 'Bạn có chắc muốn thêm các biến thể dưới đây?', confirmText: createdBatch.value ? 'Thử lại ảnh' : 'Xác nhận thêm', details: [
    { label: 'Sản phẩm', value: `${props.product.maSanPham} - ${props.product.tenSanPham}` },
    { label: 'Số biến thể', value: data.length }
  ] }, () => persist(data))
}
</script>
<template>
  <section class="p-card">
    <h2><i class="bi bi-plus-square"></i> Thêm biến thể màu sắc × kích thước</h2>
    <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
    <form @submit.prevent="generate">
      <fieldset :disabled="saving || createdBatch">
        <div class="p-form-grid"><div><label>Màu sắc</label><Select2Control v-model="colorIds" :options="colors" multiple placeholder="Chọn màu sắc" :disabled="saving || createdBatch" /><small v-if="!colors.length">Chưa có màu sắc hoạt động. Thêm tại trang thuộc tính.</small></div><div><label>Kích thước</label><Select2Control v-model="sizeIds" :options="sizes" multiple placeholder="Chọn kích thước" :disabled="saving || createdBatch" /><small v-if="!sizes.length">Chưa có kích thước hoạt động. Thêm tại trang thuộc tính.</small></div><label>Giá bán mặc định (VNĐ)<input v-model.number="defaults.price" type="number" required min="0.01" step="0.01" /></label><label>Số lượng mặc định<input v-model.number="defaults.quantity" type="number" required min="0" max="2147483647" step="1" /></label></div>
        <div class="p-form-actions"><button class="p-btn" :disabled="saving || createdBatch || !colors.length || !sizes.length"><i class="bi bi-layers"></i> Tạo danh sách tổ hợp</button><small>Tổ hợp đã tồn tại sẽ được bỏ qua. Mã/SKU gợi ý có thể sửa trước khi lưu.</small></div>
      </fieldset>
    </form>
    <form v-if="drafts.length" @submit.prevent="save">
      <div class="p-table-wrap" style="margin-top:18px"><table><thead><tr><th>Màu / Size</th><th>Mã chi tiết</th><th>SKU</th><th>Số lượng</th><th>Giá bán</th><th>Kích hoạt</th><th>Trạng thái</th><th>Ảnh</th><th></th></tr></thead><tbody><template v-for="(row, index) in drafts" :key="row.mauSacId + ':' + row.kichThuocId"><tr><td class="nowrap">{{ label('colors', row.mauSacId) }} / {{ label('sizes', row.kichThuocId) }}</td><td><input v-model="row.maChiTietSanPham" required maxlength="100" aria-label="Mã chi tiết" style="min-width:170px" :disabled="saving || createdBatch" /></td><td><input v-model="row.sku" required maxlength="100" aria-label="SKU" style="min-width:170px" :disabled="saving || createdBatch" /></td><td><input v-model.number="row.soLuong" type="number" required min="0" max="2147483647" step="1" aria-label="Số lượng" style="min-width:95px" :disabled="saving || createdBatch" /></td><td><input v-model.number="row.giaBan" type="number" required min="0.01" step="0.01" aria-label="Giá bán" style="min-width:130px" :disabled="saving || createdBatch" /></td><td><input v-model="row.kichHoat" type="checkbox" aria-label="Kích hoạt" :disabled="saving || createdBatch" /></td><td><Select2Control v-model="row.trangThai" aria-label="Trạng thái" :disabled="saving || createdBatch"><option :value="1">Hoạt động</option><option :value="0">Ngừng</option></Select2Control></td><td><button class="p-btn" type="button" :disabled="saving" @click="row.imagesOpen = !row.imagesOpen"><i class="bi bi-images"></i> {{ row.images.length ? row.images.length + ' ảnh' : 'Thêm ảnh' }}</button></td><td><button class="p-btn danger" type="button" :disabled="saving || createdBatch" aria-label="Bỏ dòng chưa lưu" @click="drafts.splice(index, 1)"><i class="bi bi-x-lg"></i></button></td></tr><tr v-show="row.imagesOpen" class="p-variant-images"><td colspan="9"><strong>Ảnh {{ label('colors', row.mauSacId) }} / {{ label('sizes', row.kichThuocId) }}</strong><ProductImagePicker v-model="row.images" :disabled="saving" /><small>Ảnh chỉ được tải lên khi lưu biến thể này.</small></td></tr></template></tbody></table></div>
      <div class="p-form-actions"><button class="p-btn primary" :disabled="saving">{{ saving ? 'Đang lưu...' : createdBatch ? 'Thử lại ảnh / Hoàn tất' : 'Lưu ' + drafts.length + ' biến thể' }}</button></div>
    </form>
    <ConfirmModal v-bind="confirmation || {}" :show="!!confirmation" :loading="saving" :error="createdBatch && error ? error : confirmError" @confirm="confirmAction" @cancel="cancelConfirmation">
      <p v-if="progress" role="status">{{ progress }}</p>
      <div v-if="pendingVariants.length && !confirmation?.title.includes('Tạo lại')" class="p-table-wrap">
        <table><thead><tr><th>Màu sắc</th><th>Kích thước</th><th>Số lượng</th><th>Giá bán</th><th>SKU</th></tr></thead>
          <tbody><tr v-for="row in pendingVariants" :key="row.mauSacId + ':' + row.kichThuocId"><td>{{ label('colors', row.mauSacId) }}</td><td>{{ label('sizes', row.kichThuocId) }}</td><td>{{ row.soLuong }}</td><td class="nowrap">{{ money(row.giaBan) }}</td><td>{{ row.sku }}</td></tr></tbody>
        </table>
      </div>
    </ConfirmModal>
  </section>
</template>
