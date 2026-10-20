<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { commerceService, commerceError } from '../services/commerceService'
const form = defineModel({ required: true })
const emit = defineEmits(['busy'])
const provinces = ref([]), wards = ref([]), busy = ref(false), error = ref(''), message = ref('')
let version = 0, disposed = false
watch(busy, value => emit('busy', value), { immediate: true })
function current(id) { return !disposed && id === version }
async function restoreAddress() {
  const id = ++version
  wards.value = []; message.value = ''; error.value = ''
  if (!form.value.province) { busy.value = false; return }
  busy.value = true
  try {
    if (!form.value.provinceId && form.value.ward) {
      const location = await commerceService.resolveAddress({ province: form.value.province, district: form.value.district || '', ward: form.value.ward })
      if (!current(id)) return
      if (!location.resolved) { message.value = location.message; return }
      Object.assign(form.value, { provinceId: location.provinceId, province: location.province,
        districtId: null, district: '', wardCode: location.wardCode, ward: location.ward })
      message.value = location.message
    }
    if (!form.value.provinceId) return
    const rows = await commerceService.areas('wards', form.value.provinceId)
    if (current(id)) wards.value = rows
  } catch (e) { if (current(id)) error.value = commerceError(e) }
  finally { if (current(id)) busy.value = false }
}
async function loadProvinces() {
  error.value = ''; busy.value = true
  try {
    const rows = await commerceService.areas('provinces')
    if (disposed) return
    provinces.value = rows
    await restoreAddress()
  } catch (e) { if (!disposed) { error.value = commerceError(e); busy.value = false } }
}
async function provinceChanged() {
  const id = ++version
  form.value.province = provinces.value.find(p => Number(p.id) === Number(form.value.provinceId))?.name || ''
  form.value.districtId = null; form.value.district = ''; form.value.wardCode = ''; form.value.ward = ''
  wards.value = []; message.value = ''; error.value = ''
  if (!form.value.provinceId) { busy.value = false; return }
  busy.value = true
  try { const rows = await commerceService.areas('wards', form.value.provinceId); if (current(id)) wards.value = rows }
  catch (e) { if (current(id)) error.value = commerceError(e) }
  finally { if (current(id)) busy.value = false }
}
watch(() => form.value, () => { if (provinces.value.length) restoreAddress() })
onMounted(loadProvinces)
onBeforeUnmount(() => { disposed = true; version++; emit('busy', false) })
</script>
<template><div class="receiver-form"><div class="shop-form-grid">
  <label>Họ tên người nhận <b>*</b><input v-model="form.name" required maxlength="255" autocomplete="shipping name" placeholder="Họ và tên"></label>
  <label>Số điện thoại <b>*</b><input v-model="form.phone" required type="tel" inputmode="numeric" pattern="0[35789][0-9]{8}" maxlength="10" autocomplete="shipping tel" placeholder="Số điện thoại nhận hàng"></label>
  <label class="form-wide">Email tài khoản<input :value="form.email || ''" readonly type="text" autocomplete="email" placeholder="Chưa có email"></label>
  <p v-if="form.province && !form.provinceId" class="form-wide shop-muted">Địa chỉ đã lưu: {{ [form.street, form.ward, form.district, form.province].filter(Boolean).join(', ') }}</p>
  <label>Tỉnh / Thành phố <b>*</b><select v-model.number="form.provinceId" required :disabled="busy" @change="provinceChanged"><option :value="null">Chọn tỉnh / thành phố</option><option v-for="p in provinces" :key="p.id" :value="Number(p.id)">{{ p.name }}</option></select></label>
  <label>Phường / Xã <b>*</b><select v-model="form.wardCode" required :disabled="busy || !form.provinceId" @change="form.ward = wards.find(w => w.id === form.wardCode)?.name || ''"><option value="">Chọn phường / xã</option><option v-for="w in wards" :key="w.id" :value="w.id">{{ w.name }}</option></select></label>
  <label class="form-wide">Địa chỉ cụ thể <b>*</b><input v-model="form.street" required maxlength="500" autocomplete="shipping street-address" placeholder="Số nhà, đường, tòa nhà…"></label>
  <p v-if="busy" class="form-wide shop-muted" role="status">Đang lấy địa chỉ và đối chiếu với GHN…</p>
  <p v-if="message" class="form-wide shop-muted" role="status">{{ message }}</p>
  <p v-if="error" class="form-wide shop-alert" role="alert">{{ error }} <button class="text-button" type="button" @click="loadProvinces">Thử lại</button></p>
  <p class="form-wide shop-muted">Tên người nhận, số điện thoại và địa chỉ được gửi đến GHN để tính phí; chưa tạo vận đơn.</p>
  <p v-if="form.ward" class="form-wide receiver-preview">Giao đến: {{ [form.street, form.ward, form.district, form.province].filter(Boolean).join(', ') }}</p>
</div></div></template>
