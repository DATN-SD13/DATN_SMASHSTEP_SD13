<script setup>
import Select2Control from './Select2Control.vue'
import { reactive, ref, onMounted } from 'vue'
import ProductImages from './ProductImages.vue'
import { variantService, validateVariant, money, errorMessage } from '../services/productService'
import ConfirmModal from './ConfirmModal.vue'
import { useConfirmation } from '../composables/useConfirmation'
const props = defineProps({ variant: { type: Object, required: true }, options: { type: Object, default: () => ({}) } })
const emit = defineEmits(['close', 'saved', 'images-changed'])
const form = reactive({ ...props.variant }), error = ref('')
const images = ref([]), imageBusy = ref(false), imagesLoading = ref(false)
async function loadImages(changed = false) {
  imagesLoading.value = true
  try { images.value = await variantService.images(props.variant.id); if (changed) emit('images-changed') }
  catch (e) { error.value = errorMessage(e) }
  finally { imagesLoading.value = false }
}
onMounted(() => loadImages())
const { confirmation, confirming: saving, confirmError, askConfirmation, cancelConfirmation, confirmAction } = useConfirmation()
function choices(type, field) {
  return (props.options[type] || []).filter(a => a.trangThai === 1 || a.id === props.variant[field])
}
function save() {
  if (saving.value || imageBusy.value) return
  error.value = ''
  error.value = validateVariant(form)
  if (error.value) return
  const id = form.id, data = { sanPhamId: form.sanPhamId,
    maChiTietSanPham: form.maChiTietSanPham.trim(), sku: form.sku.trim(),
    mauSacId: Number(form.mauSacId), kichThuocId: Number(form.kichThuocId),
    soLuong: Number(form.soLuong), giaBan: Number(form.giaBan),
    kichHoat: form.kichHoat, trangThai: Number(form.trangThai) }
  askConfirmation({ title: 'Xác nhận cập nhật biến thể', message: 'Bạn có chắc muốn lưu các thay đổi của biến thể này?', details: [
    { label: 'SKU', value: data.sku },
    { label: 'Màu sắc', value: props.options.colors?.find(item => item.id === data.mauSacId)?.ten || props.variant.tenMauSac },
    { label: 'Kích thước', value: props.options.sizes?.find(item => item.id === data.kichThuocId)?.ten || props.variant.giaTriKichThuoc },
    { label: 'Số lượng mới', value: data.soLuong }, { label: 'Giá mới', value: money(data.giaBan) },
    { label: 'Kích hoạt', value: data.kichHoat ? 'Có' : 'Không' },
    { label: 'Trạng thái', value: data.trangThai === 1 ? 'Hoạt động' : 'Ngừng hoạt động' }
  ] }, async () => {
    await variantService.update(id, data)
    emit('saved')
  })
}
</script>
<template>
  <div class="p-overlay" role="dialog" aria-modal="true" aria-labelledby="variant-dialog-title" @keydown.esc="!saving && !imageBusy && !confirmation && emit('close')">
    <div class="p-card p-dialog">
    <form @submit.prevent="save">
      <h2 id="variant-dialog-title">Sửa biến thể {{ variant.maChiTietSanPham }}</h2>
      <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
      <p>{{ variant.tenSanPham }} — {{ variant.tenMauSac }} / {{ variant.giaTriKichThuoc }}</p>
      <fieldset :disabled="saving || imageBusy"><div class="p-form-grid">
        <label>Mã chi tiết <b>*</b><input v-model="form.maChiTietSanPham" required maxlength="100" /></label>
        <label>SKU <b>*</b><input v-model="form.sku" required maxlength="100" /></label>
        <label>Màu sắc <b>*</b><Select2Control v-model="form.mauSacId" required :disabled="saving || imageBusy"><option v-for="a in choices('colors', 'mauSacId')" :key="a.id" :value="a.id">{{ a.ten }}{{ a.trangThai === 1 ? '' : ' (ngừng hoạt động)' }}</option></Select2Control></label>
        <label>Kích thước <b>*</b><Select2Control v-model="form.kichThuocId" required :disabled="saving || imageBusy"><option v-for="a in choices('sizes', 'kichThuocId')" :key="a.id" :value="a.id">{{ a.ten }}{{ a.trangThai === 1 ? '' : ' (ngừng hoạt động)' }}</option></Select2Control></label>
        <label>Số lượng <b>*</b><input v-model.number="form.soLuong" type="number" required min="0" max="2147483647" step="1" /></label>
        <label>Giá bán (VNĐ) <b>*</b><input v-model.number="form.giaBan" type="number" required min="0.01" max="9999999999999999" step="0.01" /></label>
        <label>Kích hoạt<Select2Control v-model="form.kichHoat" :disabled="saving || imageBusy"><option :value="true">Có</option><option :value="false">Không</option></Select2Control></label>
        <label>Trạng thái<Select2Control v-model="form.trangThai" :disabled="saving || imageBusy"><option :value="1">Hoạt động</option><option :value="0">Ngừng hoạt động</option></Select2Control></label>
      </div></fieldset>
      <div class="p-form-actions"><button class="p-btn primary" :disabled="saving || imageBusy">{{ saving ? 'Đang lưu...' : 'Lưu thay đổi' }}</button><button class="p-btn" type="button" :disabled="saving || imageBusy" @click="emit('close')">Hủy</button></div>
    </form>
    <ProductImages :variant-id="variant.id" :product-code="variant.maChiTietSanPham" :images="images" :loading="saving || imagesLoading" @busy="imageBusy = $event" @changed="loadImages(true)" />
    </div>
    <ConfirmModal v-bind="confirmation || {}" :show="!!confirmation" :loading="saving" :error="confirmError" @confirm="confirmAction" @cancel="cancelConfirmation" />
  </div>
</template>
