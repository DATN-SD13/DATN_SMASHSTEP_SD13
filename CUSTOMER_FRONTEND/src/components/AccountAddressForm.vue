<script setup>
import { ref } from 'vue'
import { accountService } from '../services/accountService'
import { useAddressApi } from '../composables/useAddressApi'

const props = defineProps({ address: { type: Object, default: () => ({}) }, customer: Object })
const emit = defineEmits(['saved', 'cancel'])
const form = ref({
  receiverName: props.address.receiverName || props.customer?.name || '',
  receiverPhone: props.address.receiverPhone || props.customer?.phone || '',
  province: props.address.province || '', district: props.address.district || '',
  ward: props.address.ward || '', street: props.address.street || '',
  addressType: props.address.addressType || 1, isDefault: !!props.address.isDefault
})
const { provinces, wards, loadingProvinces, loadingWards, addressError, changeProvince, loadProvinces } = useAddressApi(form)
const saving = ref(false), error = ref('')
async function save() {
  if (saving.value) return
  saving.value = true; error.value = ''
  try {
    const body = Object.fromEntries(Object.entries(form.value).map(([k, v]) => [k, typeof v === 'string' ? v.trim() : v]))
    if (props.address.id) await accountService.updateAddress(props.address.id, body)
    else await accountService.createAddress(body)
    emit('saved')
  } catch (e) {
    error.value = e.response?.data?.message || e.message || 'Chưa lưu được địa chỉ. Vui lòng thử lại.'
  } finally { saving.value = false }
}
</script>

<template>
  <form class="account-address-form" @submit.prevent="save" aria-label="Thông tin địa chỉ">
    <div class="account-section-heading"><div><h3>{{ address.id ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ mới' }}</h3><p>Thông tin người nhận và địa chỉ giao hàng của bạn.</p></div></div>
    <p v-if="error" class="account-alert account-alert--error" role="alert">{{ error }}</p>
    <fieldset :disabled="saving" class="account-form-grid">
      <label>Họ tên người nhận <span>*</span><input v-model="form.receiverName" required maxlength="255" autocomplete="shipping name" placeholder="Nhập tên người nhận"></label>
      <label>Số điện thoại <span>*</span><input v-model="form.receiverPhone" type="tel" required pattern="0[0-9]{9}" maxlength="10" title="10 chữ số, bắt đầu bằng 0" autocomplete="shipping tel" placeholder="Nhập số điện thoại"></label>
      <label>Tỉnh / Thành phố <span>*</span><select v-model="form.province" required :disabled="loadingProvinces" autocomplete="shipping address-level1" @change="changeProvince"><option value="">{{ loadingProvinces ? 'Đang tải tỉnh/thành...' : 'Chọn tỉnh/thành phố' }}</option><option v-for="p in provinces" :key="p.code" :value="p.name">{{ p.name }}</option></select></label>
      <label>Phường / Xã <span>*</span><select v-model="form.ward" required :disabled="!form.province || loadingWards" autocomplete="shipping address-level3"><option value="">{{ loadingWards ? 'Đang tải phường/xã...' : 'Chọn phường/xã' }}</option><option v-for="w in wards" :key="w.code" :value="w.name">{{ w.name }}</option></select></label>
      <div v-if="addressError" class="account-form-wide account-inline-error" role="alert">{{ addressError }} <button type="button" class="account-text-button" @click="loadProvinces">Thử lại</button></div>
      <label class="account-form-wide">Địa chỉ cụ thể <span>*</span><input v-model="form.street" required maxlength="500" autocomplete="shipping street-address" placeholder="Số nhà, tên đường, tòa nhà..."></label>
      <div class="account-form-wide"><span class="account-field-label">Loại địa chỉ</span><div class="account-radio-group"><label><input v-model="form.addressType" type="radio" :value="1"> Nhà riêng</label><label><input v-model="form.addressType" type="radio" :value="2"> Văn phòng</label></div></div>
      <label class="account-checkbox account-form-wide"><input v-model="form.isDefault" type="checkbox" :disabled="!!address.isDefault"> Đặt làm địa chỉ mặc định</label>
    </fieldset>
    <div class="account-form-actions"><button type="button" class="button account-button-secondary" :disabled="saving" @click="emit('cancel')">Hủy</button><button type="submit" class="button button-primary" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu địa chỉ' }}</button></div>
  </form>
</template>
