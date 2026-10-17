<script setup>
import { useProductFeedback } from '../composables/useProductToast'
import FormattedNumberInput from './FormattedNumberInput.vue'
import { validatePriceValue, priceValidationNotice } from '../services/priceValidation'
import { validateVariantFields, apiFieldErrors } from '../services/productValidation'
import { useProductToast } from '../composables/useProductToast'
import Select2Control from './Select2Control.vue'
import { reactive, ref, onMounted } from 'vue'
import ProductImages from './ProductImages.vue'
import { variantService, money, errorMessage } from '../services/productService'
import ConfirmModal from './ConfirmModal.vue'
import { useConfirmation } from '../composables/useConfirmation'
const props = defineProps({ variant: { type: Object, required: true }, options: { type: Object, default: () => ({}) } })
const emit = defineEmits(['close', 'saved', 'images-changed'])
const form = reactive({ ...props.variant }), error = ref('')
const fieldErrors = reactive({}), checking = ref(false), toast = useProductToast()
const priceControl = ref(null), quantityControl = ref(null)
const images = ref([]), imageBusy = ref(false), imagesLoading = ref(false)
async function loadImages(changed = false) {
  imagesLoading.value = true
  try { images.value = await variantService.images(props.variant.id); if (changed) emit('images-changed') }
  catch (e) { error.value = errorMessage(e) }
  finally { imagesLoading.value = false }
}
onMounted(() => loadImages())
const { confirmation, confirming: saving, confirmError, askConfirmation, cancelConfirmation, confirmAction } = useConfirmation()
async function save() {
  if (saving.value || imageBusy.value || checking.value || confirmation.value) return
  error.value = ''
  Object.keys(fieldErrors).forEach(key => delete fieldErrors[key])
  const price = priceControl.value.validate(), quantity = quantityControl.value.validate()
  const identity = { sanPhamId: props.variant.sanPhamId,
    maChiTietSanPham: props.variant.maChiTietSanPham, sku: props.variant.sku,
    mauSacId: props.variant.mauSacId, kichThuocId: props.variant.kichThuocId }
  Object.assign(fieldErrors, validateVariantFields({ ...form, ...identity }))
  if (!price.valid) fieldErrors.giaBan = price.message
  if (!quantity.valid) fieldErrors.soLuong = quantity.message
  if (Object.keys(fieldErrors).length) { const notice = priceValidationNotice(fieldErrors); toast.warning(notice.message, notice.title); return }
  const id = props.variant.id, data = { ...identity,
    soLuong: Number(form.soLuong), giaBan: validatePriceValue(form.giaBan).value,
    kichHoat: form.kichHoat, trangThai: Number(form.trangThai) }
  askConfirmation({ title: 'Xác nhận cập nhật biến thể', message: 'Bạn có chắc muốn lưu các thay đổi của biến thể này?', details: [
    { label: 'SKU', value: data.sku },
    { label: 'Màu sắc', value: props.options.colors?.find(item => item.id === data.mauSacId)?.ten || props.variant.tenMauSac },
    { label: 'Kích thước', value: props.options.sizes?.find(item => item.id === data.kichThuocId)?.ten || props.variant.giaTriKichThuoc },
    { label: 'Số lượng mới', value: data.soLuong }, { label: 'Giá mới', value: money(data.giaBan) },
    { label: 'Kích hoạt', value: data.kichHoat ? 'Có' : 'Không' },
    { label: 'Trạng thái', value: data.trangThai === 1 ? 'Kinh doanh' : 'Ngừng kinh doanh' }
  ] }, async () => {
    try { await variantService.update(id, data) } catch (e) { Object.assign(fieldErrors, apiFieldErrors(e)); throw e }
    toast.success('Cập nhật biến thể thành công.')
    emit('saved')
  })
}
useProductFeedback(error)
</script>
<template>
  <div class="p-overlay" role="dialog" aria-modal="true" aria-labelledby="variant-dialog-title" @keydown.esc="!saving && !imageBusy && !confirmation && emit('close')">
    <div class="p-card p-dialog">
    <form novalidate @submit.prevent="save">
      <h2 id="variant-dialog-title">Sửa biến thể {{ variant.maChiTietSanPham }}</h2>
      <p>{{ variant.tenSanPham }} — {{ variant.tenMauSac }} / {{ variant.giaTriKichThuoc }}</p>
      <p>Mã chi tiết, SKU, màu sắc và kích thước được giữ nguyên sau khi tạo.</p>
      <fieldset :disabled="saving || imageBusy || checking"><div class="p-form-grid">
        <label><span class="p-field-label">Mã chi tiết</span><input :value="variant.maChiTietSanPham" readonly :aria-invalid="!!fieldErrors.maChiTietSanPham" /><small v-if="fieldErrors.maChiTietSanPham" class="p-field-error">{{ fieldErrors.maChiTietSanPham }}</small></label>
        <label><span class="p-field-label">SKU</span><input :value="variant.sku" readonly :aria-invalid="!!fieldErrors.sku" /><small v-if="fieldErrors.sku" class="p-field-error">{{ fieldErrors.sku }}</small></label>
        <label><span class="p-field-label">Màu sắc</span><input :value="variant.tenMauSac || '—'" readonly :aria-invalid="!!fieldErrors.mauSacId" /><small v-if="fieldErrors.mauSacId" class="p-field-error">{{ fieldErrors.mauSacId }}</small></label>
        <label><span class="p-field-label">Kích thước</span><input :value="variant.giaTriKichThuoc || '—'" readonly :aria-invalid="!!fieldErrors.kichThuocId" /><small v-if="fieldErrors.kichThuocId" class="p-field-error">{{ fieldErrors.kichThuocId }}</small></label>
        <label><span class="p-field-label">Số lượng <b class="p-required">*</b></span><FormattedNumberInput ref="quantityControl" v-model="form.soLuong" required :min="0" :max="2147483647" placeholder="Ví dụ: 100" :disabled="saving || imageBusy || checking" :validation-error="fieldErrors.soLuong" @editing="delete fieldErrors.soLuong" /></label>
        <label><span class="p-field-label">Giá bán (VNĐ) <b class="p-required">*</b></span><FormattedNumberInput ref="priceControl" v-model="form.giaBan" required currency placeholder="Ví dụ: 1.250.000" :disabled="saving || imageBusy || checking" :validation-error="fieldErrors.giaBan" @editing="delete fieldErrors.giaBan" /></label>
        <label><span class="p-field-label">Kích hoạt <b class="p-required">*</b></span><Select2Control required v-model="form.kichHoat" :aria-invalid="!!fieldErrors.kichHoat" :disabled="saving || imageBusy || checking"><option :value="true">Có</option><option :value="false">Không</option></Select2Control><small v-if="fieldErrors.kichHoat" class="p-field-error">{{ fieldErrors.kichHoat }}</small></label>
        <label><span class="p-field-label">Trạng thái <b class="p-required">*</b></span><Select2Control required v-model="form.trangThai" :aria-invalid="!!fieldErrors.trangThai" :disabled="saving || imageBusy || checking"><option :value="1">Kinh doanh</option><option :value="0">Ngừng kinh doanh</option></Select2Control><small v-if="fieldErrors.trangThai" class="p-field-error">{{ fieldErrors.trangThai }}</small></label>
      </div></fieldset>
      <div class="p-form-actions"><button class="p-btn primary" :disabled="saving || imageBusy || checking">{{ saving ? 'Đang lưu...' : 'Lưu thay đổi' }}</button><button class="p-btn" type="button" :disabled="saving || imageBusy || checking" @click="emit('close')">Hủy</button></div>
    </form>
    <ProductImages :variant-id="variant.id" :product-code="variant.maChiTietSanPham" :images="images" :loading="saving || imagesLoading" @busy="imageBusy = $event" @changed="loadImages(true)" />
    </div>
    <ConfirmModal v-bind="confirmation || {}" :show="!!confirmation" :loading="saving" :error="confirmError" @confirm="confirmAction" @cancel="cancelConfirmation" />
  </div>
</template>
