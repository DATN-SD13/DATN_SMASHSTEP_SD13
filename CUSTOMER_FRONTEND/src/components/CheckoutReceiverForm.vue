<script setup>
import { ref, onMounted, watch } from 'vue'
import { commerceService, commerceError } from '../services/commerceService'
const form = defineModel({ required: true })
const provinces = ref([]), districts = ref([]), wards = ref([]), busy = ref(false), error = ref('')
let version = 0
async function loadProvinces() {
  busy.value = true; error.value = ''
  try { provinces.value = await commerceService.areas('provinces') }
  catch (e) { error.value = commerceError(e) }
  finally { busy.value = false }
}
async function provinceChanged() {
  const current = ++version
  form.value.province = provinces.value.find(p => Number(p.id) === Number(form.value.provinceId))?.name || ''
  form.value.districtId = null; form.value.wardCode = ''; form.value.district = ''; form.value.ward = ''; districts.value = []; wards.value = []
  if (!form.value.provinceId) return
  busy.value = true; error.value = ''
  try { const rows = await commerceService.areas('districts', form.value.provinceId); if (current === version) districts.value = rows }
  catch (e) { if (current === version) error.value = commerceError(e) }
  finally { if (current === version) busy.value = false }
}
async function districtChanged() {
  const current = ++version
  form.value.district = districts.value.find(p => Number(p.id) === Number(form.value.districtId))?.name || ''
  form.value.wardCode = ''; form.value.ward = ''; wards.value = []
  if (!form.value.districtId) return
  busy.value = true; error.value = ''
  try { const rows = await commerceService.areas('wards', form.value.districtId); if (current === version) wards.value = rows }
  catch (e) { if (current === version) error.value = commerceError(e) }
  finally { if (current === version) busy.value = false }
}
watch(() => form.value, () => { version++; districts.value = []; wards.value = [] })
onMounted(loadProvinces)
</script>
<template><div class="receiver-form"><div class="shop-form-grid">
  <label>Họ tên người nhận <b>*</b><input v-model="form.name" required maxlength="255" autocomplete="shipping name" placeholder="Họ và tên"></label>
  <label>Số điện thoại <b>*</b><input v-model="form.phone" required type="tel" inputmode="numeric" pattern="0[35789][0-9]{8}" maxlength="10" autocomplete="shipping tel" placeholder="Số điện thoại nhận hàng"></label>
  <label class="form-wide">Email tài khoản<input v-model="form.email" readonly type="email" maxlength="254" autocomplete="email" placeholder="Chưa có email"></label>
  <p v-if="form.province && !form.provinceId" class="form-wide shop-muted">Địa chỉ đã lưu: {{ [form.street, form.ward, form.district, form.province].filter(Boolean).join(', ') }}. Chọn lại địa bàn GHN bên dưới để tính cước chính xác.</p>
  <label>Tỉnh / Thành phố <b>*</b><select v-model.number="form.provinceId" :disabled="busy" @change="provinceChanged"><option :value="null">Chọn tỉnh / thành phố</option><option v-for="p in provinces" :key="p.id" :value="Number(p.id)">{{ p.name }}</option></select></label>
  <label>Quận / Huyện theo GHN <b>*</b><select v-model.number="form.districtId" :disabled="busy || !form.provinceId" @change="districtChanged"><option :value="null">Chọn quận / huyện</option><option v-for="d in districts" :key="d.id" :value="Number(d.id)">{{ d.name }}</option></select></label>
  <label class="form-wide">Phường / Xã theo GHN <b>*</b><select v-model="form.wardCode" :disabled="busy || !form.districtId" @change="form.ward = wards.find(w => w.id === form.wardCode)?.name || ''"><option value="">Chọn phường / xã</option><option v-for="w in wards" :key="w.id" :value="w.id">{{ w.name }}</option></select></label>
  <label class="form-wide">Địa chỉ cụ thể <b>*</b><input v-model="form.street" required maxlength="200" autocomplete="shipping street-address" placeholder="Số nhà, đường, tòa nhà…"></label>
  <p v-if="busy" class="form-wide shop-muted" role="status">Đang lấy danh sách địa chỉ GHN…</p><p v-if="error" class="form-wide shop-alert" role="alert">{{ error }} <button class="text-button" type="button" @click="loadProvinces">Thử lại</button></p>
  <p v-if="form.ward" class="form-wide receiver-preview">Giao đến: {{ [form.street, form.ward, form.district, form.province].filter(Boolean).join(', ') }}</p>
</div></div></template>
