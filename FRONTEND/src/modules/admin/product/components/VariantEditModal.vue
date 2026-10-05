<script setup>
import { reactive, ref } from 'vue'
import { variantService, validateVariant, money } from '../services/productService'
import ConfirmModal from './ConfirmModal.vue'
import { useConfirmation } from '../composables/useConfirmation'
const props = defineProps({ variant: { type: Object, required: true }, options: { type: Object, default: () => ({}) } })
const emit = defineEmits(['close', 'saved'])
const form = reactive({ ...props.variant }), error = ref('')
const { confirmation, confirming: saving, confirmError, askConfirmation, cancelConfirmation, confirmAction } = useConfirmation()
function choices(type, field) {
  return (props.options[type] || []).filter(a => a.trangThai === 1 || a.id === props.variant[field])
}
function save() {
  if (saving.value) return
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
  <div class="p-overlay" role="dialog" aria-modal="true" aria-labelledby="variant-dialog-title" @keydown.esc="!saving && !confirmation && emit('close')">
    <form class="p-card p-dialog" @submit.prevent="save">
      <h2 id="variant-dialog-title">Sửa biến thể {{ variant.maChiTietSanPham }}</h2>
      <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
      <p>{{ variant.tenSanPham }} — {{ variant.tenMauSac }} / {{ variant.giaTriKichThuoc }}</p>
      <fieldset :disabled="saving"><div class="p-form-grid">
        <label>Mã chi tiết <b>*</b><input v-model="form.maChiTietSanPham" required maxlength="100" /></label>
        <label>SKU <b>*</b><input v-model="form.sku" required maxlength="100" /></label>
        <label>Màu sắc <b>*</b><select v-model="form.mauSacId" required><option v-for="a in choices('colors', 'mauSacId')" :key="a.id" :value="a.id">{{ a.ten }}{{ a.trangThai === 1 ? '' : ' (ngừng hoạt động)' }}</option></select></label>
        <label>Kích thước <b>*</b><select v-model="form.kichThuocId" required><option v-for="a in choices('sizes', 'kichThuocId')" :key="a.id" :value="a.id">{{ a.ten }}{{ a.trangThai === 1 ? '' : ' (ngừng hoạt động)' }}</option></select></label>
        <label>Số lượng <b>*</b><input v-model.number="form.soLuong" type="number" required min="0" max="2147483647" step="1" /></label>
        <label>Giá bán (VNĐ) <b>*</b><input v-model.number="form.giaBan" type="number" required min="0.01" max="9999999999999999" step="0.01" /></label>
        <label>Kích hoạt<select v-model="form.kichHoat"><option :value="true">Có</option><option :value="false">Không</option></select></label>
        <label>Trạng thái<select v-model.number="form.trangThai"><option :value="1">Hoạt động</option><option :value="0">Ngừng hoạt động</option></select></label>
      </div></fieldset>
      <div class="p-form-actions"><button class="p-btn primary" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu thay đổi' }}</button><button class="p-btn" type="button" :disabled="saving" @click="emit('close')">Hủy</button></div>
    </form>
    <ConfirmModal v-bind="confirmation || {}" :show="!!confirmation" :loading="saving" :error="confirmError" @confirm="confirmAction" @cancel="cancelConfirmation" />
  </div>
</template>
