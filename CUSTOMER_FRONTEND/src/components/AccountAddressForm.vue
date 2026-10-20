<script setup>
import { ref } from 'vue'
import { accountService } from '../services/accountService'
import CheckoutReceiverForm from './CheckoutReceiverForm.vue'
import { nameError, phoneError } from '../utils/accountValidation'
const props = defineProps({ address: { type: Object, default: () => ({}) }, customer: Object })
const emit = defineEmits(['saved', 'cancel'])
const receiver = ref({ name: props.address.receiverName || props.customer?.name || '', phone: props.address.receiverPhone || props.customer?.phone || '',
  email: props.customer?.email || '', province: props.address.province || '', district: props.address.district || '',
  ward: props.address.ward || '', street: props.address.street || '', provinceId: null, districtId: null, wardCode: '' })
const firstAddress = !props.customer?.addresses?.length
const addressType = ref(props.address.addressType || 1), isDefault = ref(!!props.address.isDefault || firstAddress)
const saving = ref(false), resolving = ref(false), error = ref('')
async function save() {
  if (saving.value || resolving.value) return
  const r = receiver.value
  error.value = nameError(r.name) || phoneError(r.phone) || (!r.provinceId || !r.wardCode || !r.street.trim() ? 'Vui lòng chọn đầy đủ địa chỉ nhận hàng.' : '')
  if (error.value) return
  saving.value = true
  try {
    const body = { receiverName: r.name.trim(), receiverPhone: r.phone.trim(), province: r.province, district: '', ward: r.ward,
      street: r.street.trim(), addressType: addressType.value, isDefault: isDefault.value }
    if (props.address.id) await accountService.updateAddress(props.address.id, body)
    else await accountService.createAddress(body)
    emit('saved')
  } catch (e) { error.value = e.response?.data?.message || e.message || 'Chưa lưu được địa chỉ. Vui lòng thử lại.' }
  finally { saving.value = false }
}
</script>
<template><form class="account-address-form" @submit.prevent="save" aria-label="Thông tin địa chỉ">
  <div class="account-section-heading"><div><h3>{{ address.id ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ mới' }}</h3><p>Thông tin người nhận được lấy sẵn từ tài khoản. Bạn có thể đổi người nhận cho địa chỉ này.</p></div></div>
  <p v-if="error" class="account-alert account-alert--error" role="alert">{{ error }}</p>
  <fieldset :disabled="saving"><CheckoutReceiverForm v-model="receiver" @busy="resolving = $event" />
    <div class="account-form-grid"><div class="account-form-wide"><span class="account-field-label">Loại địa chỉ</span><div class="account-radio-group"><label><input v-model="addressType" type="radio" :value="1"> Nhà riêng</label><label><input v-model="addressType" type="radio" :value="2"> Văn phòng</label></div></div>
    <label class="account-checkbox account-form-wide"><input v-model="isDefault" type="checkbox" :disabled="!!address.isDefault || firstAddress"> Đặt làm địa chỉ mặc định</label></div>
  </fieldset>
  <div class="account-form-actions"><button type="button" class="button account-button-secondary" :disabled="saving" @click="emit('cancel')">Hủy</button><button type="submit" class="button button-primary" :disabled="saving || resolving">{{ saving ? 'Đang lưu…' : 'Lưu địa chỉ' }}</button></div>
</form></template>
