<script setup>
import { reactive, ref } from 'vue'
import { variantService, errorMessage } from '../services/productService'
const props = defineProps({ variant: { type: Object, required: true } })
const emit = defineEmits(['close', 'saved'])
const form = reactive({ ...props.variant }), saving = ref(false), error = ref('')
async function save() {
  if (saving.value) return
  if (form.trangThai !== props.variant.trangThai && !window.confirm('Lưu thay đổi trạng thái biến thể?')) return
  saving.value = true; error.value = ''
  try {
    await variantService.update(form.id, { sanPhamId: form.sanPhamId,
      maChiTietSanPham: form.maChiTietSanPham, sku: form.sku,
      mauSacId: form.mauSacId, kichThuocId: form.kichThuocId,
      soLuong: Number(form.soLuong), giaBan: Number(form.giaBan),
      kichHoat: form.kichHoat, trangThai: Number(form.trangThai) })
    emit('saved')
  } catch (e) { error.value = errorMessage(e) } finally { saving.value = false }
}
</script>
<template>
  <div class="p-overlay" role="dialog" aria-modal="true" aria-labelledby="variant-dialog-title" @keydown.esc="!saving && emit('close')">
    <form class="p-card p-dialog" @submit.prevent="save">
      <h2 id="variant-dialog-title">Sửa biến thể {{ variant.maChiTietSanPham }}</h2>
      <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
      <p>{{ variant.tenSanPham }} — {{ variant.tenMauSac }} / {{ variant.giaTriKichThuoc }}</p>
      <div class="p-form-grid"><label>Mã chi tiết<input :value="form.maChiTietSanPham" readonly /></label><label>SKU<input :value="form.sku" readonly /></label><label>Số lượng <b>*</b><input v-model.number="form.soLuong" type="number" required min="0" max="2147483647" step="1" /></label><label>Giá bán (VNĐ) <b>*</b><input v-model.number="form.giaBan" type="number" required min="0.01" max="9999999999999999" step="0.01" /></label><label>Kích hoạt<select v-model="form.kichHoat"><option :value="true">Có</option><option :value="false">Không</option></select></label><label>Trạng thái<select v-model.number="form.trangThai"><option :value="1">Hoạt động</option><option :value="0">Ngừng hoạt động</option></select></label></div>
      <div class="p-form-actions"><button class="p-btn primary" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu thay đổi' }}</button><button class="p-btn" type="button" :disabled="saving" @click="emit('close')">Hủy</button></div>
    </form>
  </div>
</template>
