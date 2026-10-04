<script setup>
import { reactive, ref } from 'vue'
import { attributeTypes } from '../services/productAttributeService'
const props = defineProps({ initial: Object, options: { type: Object, default: () => ({}) }, saving: Boolean, editing: Boolean })
const emit = defineEmits(['save'])
const fields = attributeTypes.filter(t => t.field)
const error = ref('')
const form = reactive({
  maSanPham: '', tenSanPham: '', danhMucId: '', thuongHieuId: '', chatLieuId: '',
  kieuDangId: '', coGiayId: '', xuatXuId: '', moTaChiTiet: '', trangThai: 1,
  ...props.initial
})
function choices(type) {
  return (props.options[type.key] || []).filter(a => a.trangThai === 1 || a.id === form[type.field])
}
function submit() {
  error.value = ''
  if (!/^[A-Za-z0-9][A-Za-z0-9._-]*$/.test(form.maSanPham.trim())) {
    error.value = 'Mã sản phẩm chỉ gồm chữ Latin, số, dấu chấm, gạch dưới hoặc gạch ngang.'; return
  }
  if (!form.tenSanPham.trim()) { error.value = 'Tên sản phẩm không được để trống.'; return }
  if (props.editing && form.trangThai !== props.initial.trangThai && !window.confirm('Lưu thay đổi trạng thái sản phẩm?')) return
  emit('save', { maSanPham: form.maSanPham.trim(), tenSanPham: form.tenSanPham.trim(),
    ...Object.fromEntries(fields.map(t => [t.field, Number(form[t.field])])),
    moTaChiTiet: (form.moTaChiTiet || '').trim(), trangThai: Number(form.trangThai) })
}
</script>
<template>
  <form class="p-card" @submit.prevent="submit">
    <h2><i class="bi bi-box-seam"></i> Thông tin sản phẩm</h2>
    <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
    <div class="p-form-grid">
      <label>Mã sản phẩm <b>*</b><input v-model="form.maSanPham" required maxlength="50" :readonly="editing" placeholder="Ví dụ: SP0001" /><small>{{ editing ? 'Mã được giữ nguyên khi sửa.' : 'Chữ Latin, số, dấu chấm, gạch dưới hoặc gạch ngang.' }}</small></label>
      <label>Tên sản phẩm <b>*</b><input v-model="form.tenSanPham" required maxlength="255" pattern=".*\S.*" placeholder="Nhập tên sản phẩm" /></label>
      <label v-for="type in fields" :key="type.key">{{ type.label }} <b>*</b><select v-model="form[type.field]" required><option value="">Chọn {{ type.label.toLowerCase() }}</option><option v-for="a in choices(type)" :key="a.id" :value="a.id">{{ a.ten }}{{ a.trangThai === 1 ? '' : ' (ngừng hoạt động)' }}</option></select><small v-if="!choices(type).length">Chưa có dữ liệu. Thêm trong <RouterLink to="/thuoc-tinh">Quản lý thuộc tính</RouterLink>.</small></label>
      <label>Trạng thái<select v-model.number="form.trangThai"><option :value="1">Hoạt động</option><option :value="0">Ngừng hoạt động</option></select></label>
      <label class="wide">Mô tả chi tiết<textarea v-model="form.moTaChiTiet" rows="5" placeholder="Mô tả sản phẩm..." /></label>
    </div>
    <div class="p-form-actions"><button class="p-btn primary" type="submit" :disabled="saving"><i class="bi bi-check2"></i>{{ saving ? 'Đang lưu...' : editing ? 'Lưu thay đổi' : 'Tạo sản phẩm' }}</button><RouterLink class="p-btn" :to="editing ? '/san-pham/' + initial.id : '/san-pham'">Hủy</RouterLink></div>
  </form>
</template>
