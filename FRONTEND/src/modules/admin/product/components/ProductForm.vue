<script setup>
import Select2Control from './Select2Control.vue'
import { reactive, ref, watch } from 'vue'
import { attributeTypes } from '../services/productAttributeService'
const props = defineProps({ initial: Object, code: String, options: { type: Object, default: () => ({}) }, saving: Boolean, editing: Boolean })
const emit = defineEmits(['save'])
const fields = attributeTypes.filter(t => t.field)
const error = ref('')
const fieldErrors = reactive({})
const form = reactive({
  maSanPham: '', tenSanPham: '', danhMucId: '', thuongHieuId: '', chatLieuId: '',
  kieuDangId: '', coGiayId: '', xuatXuId: '', moTaChiTiet: '', trangThai: 1,
  ...props.initial,
  maSanPham: props.initial?.maSanPham ?? '', tenSanPham: props.initial?.tenSanPham ?? ''
})
watch(() => props.code, code => { if (!props.editing) form.maSanPham = code || '' }, { immediate: true })
function choices(type) {
  return (props.options[type.key] || []).filter(a => a.trangThai === 1 || a.id === props.initial?.[type.field])
}
function submit() {
  if (props.saving) return
  error.value = ''
  Object.keys(fieldErrors).forEach(key => delete fieldErrors[key])
  if (!/^[A-Za-z0-9][A-Za-z0-9._-]*$/.test(form.maSanPham.trim()) || form.maSanPham.trim().length > 50)
    fieldErrors.maSanPham = 'Chưa có mã sản phẩm hợp lệ. Vui lòng tải lại form.'
  if (!form.tenSanPham.trim()) fieldErrors.tenSanPham = 'Tên sản phẩm không được để trống.'
  else if (form.tenSanPham.trim().length > 255) fieldErrors.tenSanPham = 'Tên sản phẩm tối đa 255 ký tự.'
  for (const type of fields) {
    if (!choices(type).some(item => item.id === Number(form[type.field]))) fieldErrors[type.field] = 'Vui lòng chọn ' + type.label.toLowerCase() + '.'
  }
  if (![0, 1].includes(Number(form.trangThai))) fieldErrors.trangThai = 'Trạng thái không hợp lệ.'
  if (Object.keys(fieldErrors).length) { error.value = 'Vui lòng kiểm tra các trường thông tin.'; return }
  emit('save', { maSanPham: form.maSanPham.trim(), tenSanPham: form.tenSanPham.trim(),
    ...Object.fromEntries(fields.map(t => [t.field, Number(form[t.field])])),
    moTaChiTiet: (form.moTaChiTiet || '').trim(), trangThai: Number(form.trangThai) })
}
</script>
<template>
  <form class="p-card" novalidate @submit.prevent="submit">
    <h2><i class="bi bi-box-seam"></i> Thông tin sản phẩm</h2>
    <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
    <fieldset :disabled="saving">
    <div class="p-form-grid">
      <label>Mã sản phẩm <b>*</b><input v-model="form.maSanPham" required maxlength="50" readonly :aria-invalid="!!fieldErrors.maSanPham" /><small>{{ editing ? 'Mã được giữ nguyên khi sửa.' : 'Mã dự kiến được hệ thống tự động tạo theo thứ tự hiện tại.' }}</small><small v-if="fieldErrors.maSanPham" class="p-field-error">{{ fieldErrors.maSanPham }}</small></label>
      <label>Tên sản phẩm <b>*</b><input v-model="form.tenSanPham" required maxlength="255" pattern=".*\S.*" placeholder="Nhập tên sản phẩm" :aria-invalid="!!fieldErrors.tenSanPham" /><small v-if="fieldErrors.tenSanPham" class="p-field-error">{{ fieldErrors.tenSanPham }}</small></label>
      <label v-for="type in fields" :key="type.key">{{ type.label }} <b>*</b><Select2Control v-model="form[type.field]" required :aria-invalid="!!fieldErrors[type.field]" :disabled="saving"><option value="">Chọn {{ type.label.toLowerCase() }}</option><option v-for="a in choices(type)" :key="a.id" :value="a.id">{{ a.ten }}{{ a.trangThai === 1 ? '' : ' (ngừng hoạt động)' }}</option></Select2Control><small v-if="fieldErrors[type.field]" class="p-field-error">{{ fieldErrors[type.field] }}</small><small v-if="!choices(type).length">Chưa có dữ liệu. Thêm trong <RouterLink to="/thuoc-tinh">Quản lý thuộc tính</RouterLink>.</small></label>
      <label>Trạng thái<Select2Control v-model="form.trangThai" :aria-invalid="!!fieldErrors.trangThai" :disabled="saving"><option :value="1">Hoạt động</option><option :value="0">Ngừng hoạt động</option></Select2Control><small v-if="fieldErrors.trangThai" class="p-field-error">{{ fieldErrors.trangThai }}</small></label>
      <label class="wide">Mô tả chi tiết<textarea v-model="form.moTaChiTiet" rows="5" placeholder="Mô tả sản phẩm..." /></label>
    </div>
    <div class="p-form-actions"><button class="p-btn primary" type="submit" :disabled="saving"><i class="bi bi-check2"></i>{{ saving ? 'Đang lưu...' : editing ? 'Lưu thay đổi' : 'Tạo sản phẩm' }}</button><RouterLink class="p-btn" :to="editing ? '/san-pham/' + initial.id : '/san-pham'">Hủy</RouterLink></div>
    </fieldset>
  </form>
</template>
