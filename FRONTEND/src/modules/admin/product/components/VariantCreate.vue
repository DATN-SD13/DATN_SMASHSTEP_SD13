<script setup>
import { useProductToast, useProductFeedback } from '../composables/useProductToast'
import { ref, reactive, computed } from 'vue'
import { productService, variantService, money } from '../services/productService'
import FormattedNumberInput from './FormattedNumberInput.vue'
import { validatePriceValue, priceValidationNotice } from '../services/priceValidation'
import { validateVariantFields, duplicateVariantFields, apiFieldErrors } from '../services/productValidation'
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
const defaults = reactive({ price: null, quantity: 0 })
const fieldErrors = reactive({}), rowErrors = ref([]), checking = ref(false), toast = useProductToast()
const defaultPrice = ref(null), defaultQuantity = ref(null), rowControls = new Map()
function setRowControl(row, field, control) {
  if (control) rowControls.set(row, { ...rowControls.get(row), [field]: control })
  else { const controls = rowControls.get(row); if (controls) { delete controls[field]; if (!Object.keys(controls).length) rowControls.delete(row) } }
}
const colors = computed(() => (props.options?.colors || []).filter(a => a.trangThai === 1))
const sizes = computed(() => (props.options?.sizes || []).filter(a => a.trangThai === 1))
function generate() {
  if (saving.value || confirmation.value || createdBatch.value) return
  error.value = ''
  Object.keys(fieldErrors).forEach(key => delete fieldErrors[key])
  if (!colorIds.value.length || colorIds.value.some(id => !colors.value.some(item => item.id === Number(id)))) fieldErrors.colors = 'Vui lòng chọn màu sắc kinh doanh.'
  if (!sizeIds.value.length || sizeIds.value.some(id => !sizes.value.some(item => item.id === Number(id)))) fieldErrors.sizes = 'Vui lòng chọn kích thước kinh doanh.'
  const price = defaultPrice.value.validate(), quantity = defaultQuantity.value.validate()
  if (!quantity.valid) fieldErrors.quantity = quantity.message
  if (!price.valid) fieldErrors.price = price.message
  if (Object.keys(fieldErrors).length) { const notice = priceValidationNotice(fieldErrors, 'price'); toast.warning(notice.message, notice.title); return }
  if (colorIds.value.length * sizeIds.value.length > 200) { error.value = 'Mỗi lần tạo tối đa 200 tổ hợp màu × kích thước.'; return }
  if (drafts.value.length) {
    askConfirmation({ title: 'Tạo lại danh sách tổ hợp', message: 'Danh sách mới sẽ thay thế các dòng chưa lưu. Bạn có muốn tiếp tục?' }, replaceDrafts)
    return
  }
  replaceDrafts()
}
function replaceDrafts() {
  rowErrors.value = []
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
    let created
    try { created = await productService.createVariants(props.product.id, data) }
    catch (e) {
      const errors = apiFieldErrors(e)
      rowErrors.value = drafts.value.map((row, index) => Object.fromEntries(Object.entries(errors).filter(([key]) => !key.includes('[') || key.startsWith(`variants[${index}].`)).map(([key, value]) => [key.replace(/^variants\[\d+\]\./, ''), value])))
      throw e
    }
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
  toast.success('Thêm biến thể và lưu ảnh thành công.')
  drafts.value = []; rowErrors.value = []; colorIds.value = []; sizeIds.value = []; createdBatch.value = false; error.value = ''
}
async function save() {
  if (saving.value || checking.value || confirmation.value || !drafts.value.length) return
  error.value = ''
  const numericErrors = drafts.value.map(row => {
    const controls = rowControls.get(row), errors = {}
    for (const field of ['giaBan', 'soLuong']) {
      const result = controls?.[field]?.validate()
      if (result && !result.valid) errors[field] = result.message
    }
    return errors
  })
  const payload = drafts.value.map(({ images, imagesOpen, savedId, ...row }) => ({ ...row, maChiTietSanPham: row.maChiTietSanPham.trim(), sku: row.sku.trim() }))
  for (const row of drafts.value) for (const image of row.images) {
    const message = validateImageFile(image.file)
    if (message) { error.value = message; return }
  }
  rowErrors.value = payload.map((row, index) => ({ ...validateVariantFields(row), ...numericErrors[index] }))
  if (rowErrors.value.some(errors => Object.keys(errors).length)) {
    toast.warning(rowErrors.value.some(errors => errors.giaBan) ? 'Vui lòng kiểm tra giá bán của các biến thể.' : 'Vui lòng kiểm tra các trường bắt buộc.', rowErrors.value.some(errors => errors.giaBan) ? 'Giá bán không hợp lệ' : 'Vui lòng kiểm tra dữ liệu')
    return
  }
  if (!createdBatch.value) {
    checking.value = true
    try {
      const existing = await variantService.all()
      rowErrors.value = payload.map((row, index) => ({
        ...duplicateVariantFields(row, existing),
        ...duplicateVariantFields(row, payload.filter((_, other) => other !== index), null)
      }))
    } catch (e) { error.value = errorMessage(e); return }
    finally { checking.value = false }
    if (rowErrors.value.some(errors => Object.keys(errors).length)) { toast.warning('Mã biến thể, SKU hoặc tổ hợp màu sắc/kích thước đã tồn tại.'); return }
  }
  const data = payload.map(row => ({ ...row, giaBan: validatePriceValue(row.giaBan).value, soLuong: Number(row.soLuong) }))
  pendingVariants.value = data
  askConfirmation({ title: createdBatch.value ? 'Thử lại ảnh biến thể' : 'Xác nhận thêm biến thể', message: createdBatch.value ? 'Các biến thể đã được lưu. Chỉ tải tiếp những ảnh chưa thành công.' : 'Bạn có chắc muốn thêm các biến thể dưới đây?', confirmText: createdBatch.value ? 'Thử lại ảnh' : 'Xác nhận thêm', details: [
    { label: 'Sản phẩm', value: `${props.product.maSanPham} - ${props.product.tenSanPham}` },
    { label: 'Số biến thể', value: data.length }
  ] }, () => persist(data))
}
useProductFeedback(error)
</script>
<template>
  <section class="p-card">
    <h2><i class="bi bi-plus-square"></i> Thêm biến thể màu sắc × kích thước</h2>
    <form novalidate @submit.prevent="generate">
      <fieldset :disabled="saving || createdBatch || checking">
        <div class="p-form-grid"><div><label><span class="p-field-label">Màu sắc <b class="p-required">*</b></span></label><Select2Control v-model="colorIds" :aria-invalid="!!fieldErrors.colors" :options="colors" multiple placeholder="Chọn màu sắc" :disabled="saving || createdBatch || checking" /><small v-if="fieldErrors.colors" class="p-field-error">{{ fieldErrors.colors }}</small><small v-if="!colors.length">Chưa có màu sắc kinh doanh. Thêm tại trang thuộc tính.</small></div><div><label><span class="p-field-label">Kích thước <b class="p-required">*</b></span></label><Select2Control v-model="sizeIds" :aria-invalid="!!fieldErrors.sizes" :options="sizes" multiple placeholder="Chọn kích thước" :disabled="saving || createdBatch || checking" /><small v-if="fieldErrors.sizes" class="p-field-error">{{ fieldErrors.sizes }}</small><small v-if="!sizes.length">Chưa có kích thước kinh doanh. Thêm tại trang thuộc tính.</small></div><label><span class="p-field-label">Giá bán mặc định (VNĐ) <b class="p-required">*</b></span><FormattedNumberInput ref="defaultPrice" v-model="defaults.price" required currency placeholder="Ví dụ: 1.250.000" :validation-error="fieldErrors.price" @editing="delete fieldErrors.price" /></label><label><span class="p-field-label">Số lượng mặc định <b class="p-required">*</b></span><FormattedNumberInput ref="defaultQuantity" v-model="defaults.quantity" required :min="0" :max="2147483647" placeholder="Ví dụ: 100" :validation-error="fieldErrors.quantity" @editing="delete fieldErrors.quantity" /></label></div>
        <div class="p-form-actions"><button class="p-btn" :disabled="saving || createdBatch || !colors.length || !sizes.length"><i class="bi bi-layers"></i> Tạo danh sách tổ hợp</button><small>Tổ hợp đã tồn tại sẽ được bỏ qua. Mã/SKU gợi ý có thể sửa trước khi lưu.</small></div>
      </fieldset>
    </form>
    <form v-if="drafts.length" novalidate @submit.prevent="save">
      <div class="p-table-wrap" style="margin-top:18px"><table><thead><tr><th>Màu / Size</th><th><span class="p-field-label">Mã chi tiết <b class="p-required">*</b></span></th><th><span class="p-field-label">SKU <b class="p-required">*</b></span></th><th><span class="p-field-label">Số lượng <b class="p-required">*</b></span></th><th><span class="p-field-label">Giá bán <b class="p-required">*</b></span></th><th><span class="p-field-label">Kích hoạt <b class="p-required">*</b></span></th><th><span class="p-field-label">Trạng thái <b class="p-required">*</b></span></th><th>Ảnh</th><th></th></tr></thead><tbody><template v-for="(row, index) in drafts" :key="row.mauSacId + ':' + row.kichThuocId"><tr><td class="nowrap">{{ label('colors', row.mauSacId) }} / {{ label('sizes', row.kichThuocId) }}<small v-if="rowErrors[index]?.mauSacId || rowErrors[index]?.kichThuocId" class="p-field-error">{{ rowErrors[index].mauSacId || rowErrors[index].kichThuocId }}</small></td><td><input v-model="row.maChiTietSanPham" :aria-invalid="!!rowErrors[index]?.maChiTietSanPham" placeholder="Nhập Mã chi tiết" required maxlength="100" aria-label="Mã chi tiết" style="min-width:170px" :disabled="saving || createdBatch || checking" /><small v-if="rowErrors[index]?.maChiTietSanPham" class="p-field-error">{{ rowErrors[index].maChiTietSanPham }}</small></td><td><input v-model="row.sku" :aria-invalid="!!rowErrors[index]?.sku" placeholder="Nhập SKU" required maxlength="100" aria-label="SKU" style="min-width:170px" :disabled="saving || createdBatch || checking" /><small v-if="rowErrors[index]?.sku" class="p-field-error">{{ rowErrors[index].sku }}</small></td><td><FormattedNumberInput :ref="control => setRowControl(row, 'soLuong', control)" v-model="row.soLuong" :validation-error="rowErrors[index]?.soLuong" @editing="rowErrors[index] && delete rowErrors[index].soLuong" required :min="0" :max="2147483647" placeholder="Ví dụ: 100" aria-label="Số lượng" style="min-width:130px" :disabled="saving || createdBatch || checking" /></td><td><FormattedNumberInput :ref="control => setRowControl(row, 'giaBan', control)" v-model="row.giaBan" :validation-error="rowErrors[index]?.giaBan" @editing="rowErrors[index] && delete rowErrors[index].giaBan" required currency placeholder="Ví dụ: 1.250.000" aria-label="Giá bán" style="min-width:130px" :disabled="saving || createdBatch || checking" /></td><td><input v-model="row.kichHoat" :aria-invalid="!!rowErrors[index]?.kichHoat" type="checkbox" aria-label="Kích hoạt" :disabled="saving || createdBatch || checking" /><small v-if="rowErrors[index]?.kichHoat" class="p-field-error">{{ rowErrors[index].kichHoat }}</small></td><td><Select2Control v-model="row.trangThai" :aria-invalid="!!rowErrors[index]?.trangThai" aria-label="Trạng thái" :disabled="saving || createdBatch || checking"><option :value="1">Kinh doanh</option><option :value="0">Ngừng kinh doanh</option></Select2Control><small v-if="rowErrors[index]?.trangThai" class="p-field-error">{{ rowErrors[index].trangThai }}</small></td><td><button class="p-btn" type="button" :disabled="saving || checking" @click="row.imagesOpen = !row.imagesOpen"><i class="bi bi-images"></i> {{ row.images.length ? row.images.length + ' ảnh' : 'Thêm ảnh' }}</button></td><td><button class="p-btn danger" type="button" :disabled="saving || createdBatch || checking" aria-label="Bỏ dòng chưa lưu" @click="drafts.splice(index, 1)"><i class="bi bi-x-lg"></i></button></td></tr><tr v-show="row.imagesOpen" class="p-variant-images"><td colspan="9"><strong>Ảnh {{ label('colors', row.mauSacId) }} / {{ label('sizes', row.kichThuocId) }}</strong><ProductImagePicker v-model="row.images" :disabled="saving || checking" /><small>Ảnh chỉ được tải lên khi lưu biến thể này.</small></td></tr></template></tbody></table></div>
      <div class="p-form-actions"><button class="p-btn primary" :disabled="saving || checking">{{ saving ? 'Đang lưu...' : createdBatch ? 'Thử lại ảnh / Hoàn tất' : 'Lưu ' + drafts.length + ' biến thể' }}</button></div>
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
