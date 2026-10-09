<script setup>
import Select2Control from '../components/Select2Control.vue'
import { ref, reactive, watch, computed, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import ProductShell from '../components/ProductShell.vue'
import StatusBadge from '../components/StatusBadge.vue'
import ProductPagination from '../components/ProductPagination.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { useConfirmation, statusConfirmation } from '../composables/useConfirmation'
import { productAttributeService, attributeTypes } from '../services/productAttributeService'
import { attributeDuplicatePrompt } from '../services/duplicateConfirmation'
import { cleanParams, errorMessage } from '../services/productService'
const route = useRoute()
const type = computed(() => route.meta.attributeType || 'brands')
const keyword = ref(''), status = ref('')
const busy = ref(false), error = ref(''), success = ref(''), showForm = ref(false), editingId = ref(null)
const dangLayMa = ref(false), duplicateBusy = ref(false)
const { confirmation, confirming: saving, confirmError, askConfirmation, replaceConfirmation, cancelConfirmation, confirmAction } = useConfirmation()
const label = computed(() => attributeTypes.find(t => t.key === type.value)?.label)
const isSize = computed(() => type.value === 'sizes'), isColor = computed(() => type.value === 'colors')
const hasNotes = computed(() => ['categories', 'sizes'].includes(type.value))
const form = reactive({ ma: '', ten: '', ghiChu: '', maMauHex: '#000000', trangThai: 1 })
const page = ref({ content: [], number: 0, size: 10, totalElements: 0, totalPages: 0 })
let applied = {}, maRequest = 0, loadVersion = 0, duplicateVersion = 0
onBeforeUnmount(() => { loadVersion++; maRequest++; duplicateVersion++ })
async function load(number = 0, size = page.value.size) {
  const currentType = type.value, request = ++loadVersion
  busy.value = true; error.value = ''
  try {
    const result = await productAttributeService.list(currentType, cleanParams({ ...applied, page: number, size }))
    if (request !== loadVersion || currentType !== type.value) return
    page.value = result
    if (number > 0 && !result.content.length && number >= result.totalPages) {
      const fallback = await productAttributeService.list(currentType, cleanParams({ ...applied, page: Math.max(0, result.totalPages - 1), size }))
      if (request === loadVersion && currentType === type.value) page.value = fallback
    }
  } catch (e) { if (request === loadVersion) error.value = errorMessage(e) } finally { if (request === loadVersion) busy.value = false }
}
function search() { applied = { keyword: keyword.value, status: status.value }; load(0) }
function reset() { keyword.value = ''; status.value = ''; search() }
async function open(item = null) {
  if (duplicateBusy.value || busy.value || saving.value || dangLayMa.value || confirmation.value) return
  const currentType = type.value, request = ++maRequest
  showForm.value = false
  editingId.value = item?.id || null
  Object.assign(form, { ma: item?.ma || '', ten: item?.ten || '', ghiChu: item?.ghiChu || '',
    maMauHex: item?.maMauHex || '#000000', trangThai: item?.trangThai ?? 1 })
  error.value = ''; success.value = ''
  if (!item && currentType !== 'sizes') {
    dangLayMa.value = true
    try {
      const result = await productAttributeService.nextCode(currentType)
      if (request !== maRequest || currentType !== type.value) return
      if (typeof result.ma !== 'string' || !result.ma.trim()) throw new Error('Thiếu mã thuộc tính')
      form.ma = result.ma
    } catch {
      if (request === maRequest && currentType === type.value)
        error.value = 'Không thể tạo mã thuộc tính. Vui lòng thử lại.'
      return
    } finally {
      if (request === maRequest) dangLayMa.value = false
    }
  }
  if (request === maRequest && currentType === type.value) showForm.value = true
}
async function save() {
  if (duplicateBusy.value || saving.value || busy.value || dangLayMa.value || confirmation.value) return
  error.value = ''; success.value = ''
  if (!form.ten.trim()) { error.value = 'Tên/giá trị thuộc tính không được để trống.'; return }
  if (form.ten.trim().length > (isSize.value ? 50 : 255)) { error.value = 'Tên/giá trị thuộc tính quá dài.'; return }
  if (![0, 1].includes(Number(form.trangThai))) { error.value = 'Trạng thái không hợp lệ.'; return }
  if (!isSize.value && !editingId.value && !form.ma.trim()) {
    error.value = 'Không thể tạo mã thuộc tính. Vui lòng thử lại.'; return
  }
  if (isColor.value && !/^#[0-9a-f]{6}$/i.test(form.maMauHex.trim())) { error.value = 'Mã HEX phải có dạng #RRGGBB.'; return }
  const data = { ma: isSize.value ? null : form.ma.trim(), ten: form.ten.trim(),
    ghiChu: hasNotes.value ? form.ghiChu.trim() : null, maMauHex: isColor.value ? form.maMauHex.trim() : null,
    trangThai: Number(form.trangThai) }
  const currentType = type.value, id = editingId.value, currentLabel = label.value.toLowerCase()
  const currentTitle = label.value, version = ++duplicateVersion
  duplicateBusy.value = true
  try {
    const result = await productAttributeService.checkDuplicate(currentType, { ...data, excludeId: id })
    if (version !== duplicateVersion || currentType !== type.value) return
    if (result.duplicate) {
      askConfirmation(attributeDuplicatePrompt(currentTitle, result.attribute, result.reason),
        () => editExisting(currentType, result.attribute.id))
      return
    }
  } catch {
    if (version === duplicateVersion) error.value = 'Không thể kiểm tra thuộc tính trùng. Vui lòng thử lại.'
    return
  } finally { if (version === duplicateVersion) duplicateBusy.value = false }

  const details = [
    ...(isSize.value ? [] : [{ label: 'Mã', value: data.ma }]),
    { label: isSize.value ? 'Giá trị size' : 'Tên', value: data.ten },
    ...(isColor.value ? [{ label: 'Mã HEX', value: data.maMauHex }] : []),
    { label: 'Trạng thái', value: data.trangThai === 1 ? 'Hoạt động' : 'Ngừng hoạt động' }
  ]
  askConfirmation({ title: `Xác nhận ${id ? 'cập nhật' : 'thêm'} ${currentLabel}`,
    message: `Bạn có chắc muốn ${id ? 'cập nhật' : 'thêm'} ${currentLabel} này?`,
    confirmText: id ? 'Xác nhận cập nhật' : 'Xác nhận thêm', details }, async () => {
    let result
    try {
      result = id ? await productAttributeService.update(currentType, id, data)
        : await productAttributeService.create(currentType, data)
    } catch (e) {
      if (e?.response?.status !== 409) throw e
      let duplicate
      try { duplicate = await productAttributeService.checkDuplicate(currentType, { ...data, excludeId: id }) }
      catch { error.value = 'Không thể kiểm tra thuộc tính trùng. Vui lòng thử lại.'; throw e }
      if (!duplicate.duplicate) throw e
      replaceConfirmation(attributeDuplicatePrompt(currentTitle, duplicate.attribute, duplicate.reason),
        () => editExisting(currentType, duplicate.attribute.id))
      return
    }
    if (currentType !== type.value) return
    form.ma = result.ma || ''
    showForm.value = false
    success.value = `${id ? 'Cập nhật' : 'Thêm'} ${currentLabel} thành công${result.ma ? ' (' + result.ma + ')' : ''}.`
    await load(id ? page.value.number : 0)
  })
}
async function editExisting(currentType, id) {
  const existing = await productAttributeService.get(currentType, id)
  if (currentType !== type.value) return
  editingId.value = existing.id
  Object.assign(form, { ma: existing.ma || '', ten: existing.ten || '', ghiChu: existing.ghiChu || '',
    maMauHex: existing.maMauHex || '#000000', trangThai: existing.trangThai ?? 1 })
  showForm.value = true; error.value = ''; success.value = ''
}
function changeStatus(item) {
  if (duplicateBusy.value || busy.value || saving.value || dangLayMa.value || confirmation.value) return
  const currentType = type.value, id = item.id, nextStatus = item.trangThai === 1 ? 0 : 1
  askConfirmation(statusConfirmation(label.value.toLowerCase(), [item.ma, item.ten].filter(Boolean).join(' - '), item.trangThai), async () => {
    error.value = ''; success.value = ''
    await productAttributeService.status(currentType, id, nextStatus)
    success.value = 'Thay đổi trạng thái thuộc tính thành công.'
    await load(page.value.number)
  })
}
watch(type, () => {
  duplicateVersion++; duplicateBusy.value = false
  maRequest++; cancelConfirmation(); dangLayMa.value = false; showForm.value = false; editingId.value = null
  page.value = { content: [], number: 0, size: 10, totalElements: 0, totalPages: 0 }
  Object.assign(form, { ma: '', ten: '', ghiChu: '', maMauHex: '#000000', trangThai: 1 })
  keyword.value = ''; status.value = ''; applied = {}; success.value = ''; load(0)
}, { immediate: true })
</script>
<template>
  <ProductShell :title="label" description="Quản lý các thuộc tính dùng chung của sản phẩm và biến thể." :error="error" :success="success">
    <template #actions><button class="p-btn primary" :disabled="busy || saving || duplicateBusy || dangLayMa || !!confirmation" @click="open()"><i class="bi bi-plus-lg"></i> {{ dangLayMa ? 'Đang tạo mã...' : 'Thêm ' + label?.toLowerCase() }}</button></template>
    <form class="p-card" @submit.prevent="search"><h2><i class="bi bi-sliders2-vertical"></i> Tìm kiếm {{ label?.toLowerCase() }}</h2><div class="p-filter-grid"><label>Tìm kiếm<input v-model="keyword" :placeholder="isSize ? 'Giá trị size...' : 'Mã hoặc tên...'" /></label><label>Trạng thái<Select2Control v-model="status" :disabled="busy || saving || duplicateBusy || !!confirmation"><option value="">Tất cả</option><option :value="1">Hoạt động</option><option :value="0">Ngừng hoạt động</option></Select2Control></label></div><div class="p-filter-actions"><button class="p-btn" type="button" :disabled="busy || saving || duplicateBusy || !!confirmation" @click="reset">Đặt lại</button><button class="p-btn primary" :disabled="busy || saving || duplicateBusy || !!confirmation">Tìm kiếm</button></div></form>
    <form v-if="showForm" class="p-card" @submit.prevent="save"><h2>{{ editingId ? 'Sửa' : 'Thêm' }} {{ label?.toLowerCase() }}</h2><fieldset :disabled="saving || duplicateBusy || !!confirmation"><div class="p-form-grid"><label v-if="!isSize">Mã {{ isColor ? 'màu' : 'thuộc tính' }} <b>*</b><input v-model="form.ma" readonly /><small>Mã được tạo tự động</small></label><label>{{ isSize ? 'Giá trị size' : 'Tên ' + label?.toLowerCase() }} <b>*</b><input v-model="form.ten" required :maxlength="isSize ? 50 : 255" /></label><label v-if="isColor">Mã HEX <b>*</b><input v-model="form.maMauHex" required maxlength="7" placeholder="#RRGGBB" /><span><span class="p-swatch" :style="{ background: /^#[0-9a-f]{6}$/i.test(form.maMauHex) ? form.maMauHex : 'transparent' }"></span>{{ form.maMauHex }}</span></label><label>Trạng thái<Select2Control v-model="form.trangThai" :disabled="busy || saving || duplicateBusy || !!confirmation"><option :value="1">Hoạt động</option><option :value="0">Ngừng hoạt động</option></Select2Control></label><label v-if="hasNotes" class="wide">{{ isSize ? 'Ghi chú' : 'Mô tả' }}<textarea v-model="form.ghiChu" rows="3" maxlength="1000" /></label></div><div class="p-form-actions"><button class="p-btn primary" :disabled="saving || busy || duplicateBusy || !!confirmation">{{ duplicateBusy ? 'Đang kiểm tra...' : saving ? 'Đang lưu...' : 'Lưu thuộc tính' }}</button><button class="p-btn" type="button" @click="showForm = false">Hủy</button></div></fieldset></form>
    <section class="p-card"><h2>{{ label }}</h2><div class="p-table-wrap"><table><thead><tr><th>STT</th><th v-if="!isSize">Mã {{ isColor ? 'màu' : 'thuộc tính' }}</th><th>{{ isSize ? 'Giá trị size' : 'Tên' }}</th><th v-if="isColor">HEX / Preview</th><th v-if="hasNotes">{{ isSize ? 'Ghi chú' : 'Mô tả' }}</th><th>Trạng thái</th><th>Hành động</th></tr></thead><tbody><tr v-for="(item, i) in page.content" :key="item.id"><td>{{ page.number * page.size + i + 1 }}</td><td v-if="!isSize">{{ item.ma }}</td><td>{{ item.ten }}</td><td v-if="isColor"><span class="p-swatch" :style="{ background: /^#[0-9a-f]{6}$/i.test(item.maMauHex || '') ? item.maMauHex : 'transparent' }"></span>{{ item.maMauHex }}</td><td v-if="hasNotes">{{ item.ghiChu || '—' }}</td><td><StatusBadge :status="item.trangThai" /></td><td><div class="p-actions"><button class="p-btn" :disabled="busy || saving || duplicateBusy || dangLayMa || !!confirmation" aria-label="Sửa thuộc tính" @click="open(item)"><i class="bi bi-pencil"></i></button><button class="p-btn" :disabled="busy || saving || duplicateBusy || !!confirmation" :class="{ danger: item.trangThai === 1 }" aria-label="Đổi trạng thái thuộc tính" @click="changeStatus(item)"><i class="bi bi-power"></i></button></div></td></tr><tr v-if="!page.content.length"><td colspan="7" class="p-empty">{{ busy ? 'Đang tải...' : 'Chưa có thuộc tính phù hợp. Bạn có thể thêm mới.' }}</td></tr></tbody></table></div><ProductPagination :page="page" :busy="busy || saving || duplicateBusy || !!confirmation" @change="load($event)" @size="load(0, $event)" /></section>
    <ConfirmModal v-bind="confirmation || {}" :show="!!confirmation" :loading="saving" :error="confirmError" @confirm="confirmAction" @cancel="cancelConfirmation" />
  </ProductShell>
</template>
