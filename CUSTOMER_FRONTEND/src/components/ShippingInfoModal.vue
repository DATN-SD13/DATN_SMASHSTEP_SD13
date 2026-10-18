<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import CheckoutReceiverForm from './CheckoutReceiverForm.vue'
import { accountService } from '../services/accountService'
import { commerceService, commerceError } from '../services/commerceService'
import { nameError, phoneError, emailError } from '../utils/accountValidation'
const props = defineProps({ order: Object }), emit = defineEmits(['close', 'saved'])
const profile = ref(null), saving = ref(false), error = ref(''), selected = ref(null), note = ref(props.order.note || ''), dialog = ref(null)
const receiver = ref({ name: props.order.receiverName || '', phone: props.order.phone || '', email: '', street: '', province: '', district: '', ward: '', provinceId: null, districtId: null, wardCode: '' })
const addresses = computed(() => profile.value?.addresses || [])
function choose(a) { selected.value = a?.id || null; receiver.value = { name: a?.receiverName || props.order.receiverName || '', phone: a?.receiverPhone || props.order.phone || '', email: profile.value?.email || '', province: a?.province || '', district: a?.district || '', ward: a?.ward || '', street: a?.street || '', provinceId: null, districtId: null, wardCode: '' } }
async function save() {
  if (saving.value) return
  const r = receiver.value
  error.value = nameError(r.name) || phoneError(r.phone) || (r.email ? emailError(r.email) : '') || (!r.street.trim() || !r.province || !r.ward ? 'Vui lòng chọn đầy đủ địa chỉ nhận hàng.' : '')
  if (error.value) return
  saving.value = true
  try { emit('saved', await commerceService.shipping(props.order.code, { receiver: { ...r }, note: note.value.trim() })) }
  catch (e) { error.value = commerceError(e) }
  finally { saving.value = false }
}
let lastFocus, priorOverflow
function keydown(e) {
  if (e.key === 'Escape' && !saving.value) emit('close')
  if (e.key !== 'Tab') return
  const controls = [...dialog.value.querySelectorAll('button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), a[href]')]
  const first = controls[0], last = controls.at(-1)
  if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last?.focus() }
  else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first?.focus() }
}
onMounted(async () => { lastFocus = document.activeElement; priorOverflow = document.body.style.overflow; document.body.style.overflow = 'hidden'; dialog.value?.focus(); try { profile.value = await accountService.profile(); receiver.value.email = profile.value.email || '' } catch (e) { error.value = commerceError(e) } })
onBeforeUnmount(() => { document.body.style.overflow = priorOverflow; lastFocus?.focus() })
</script>
<template><Teleport to="body"><div class="shop-modal-backdrop" @click.self="!saving && emit('close')"><section ref="dialog" class="shop-modal" role="dialog" aria-modal="true" aria-labelledby="shipping-modal-title" tabindex="-1" @keydown="keydown"><header><div><span class="eyebrow">THÔNG TIN GIAO HÀNG</span><h2 id="shipping-modal-title">Chỉnh sửa thông tin nhận hàng</h2><p>Đơn đang chờ xác nhận. Phí giao hàng được tính lại theo địa chỉ mới.</p></div><button class="icon-button" type="button" aria-label="Đóng" :disabled="saving" @click="emit('close')">×</button></header><form @submit.prevent="save"><div class="shop-modal-body"><p v-if="error" class="shop-alert" role="alert">{{ error }}</p><fieldset :disabled="saving"><section class="shop-card"><h3>Địa chỉ giao hàng</h3><p class="shop-muted">Hiện tại: {{ order.address }}</p><label v-for="a in addresses" :key="a.id" class="address-option" :class="{ selected: selected === a.id }"><input type="radio" name="shipping-address" :checked="selected === a.id" @change="choose(a)"><span><strong>{{ a.receiverName }} · {{ a.receiverPhone }}</strong><small>{{ [a.street, a.ward, a.district, a.province].filter(Boolean).join(', ') }}</small></span></label><button type="button" class="text-button" @click="choose(null)">Sử dụng địa chỉ khác</button></section><section class="shop-card"><h3>Thông tin liên hệ và người nhận</h3><CheckoutReceiverForm v-model="receiver" /></section><section class="shop-card"><label class="shop-label">Ghi chú<textarea v-model="note" maxlength="900" rows="3" placeholder="Ghi chú giao hàng (nếu có)"></textarea></label></section></fieldset></div><footer><button type="button" class="button button-outline" :disabled="saving" @click="emit('close')">Hủy</button><button class="button button-primary" :disabled="saving">{{ saving ? 'Đang lưu…' : 'Lưu thay đổi' }}</button></footer></form></section></div></Teleport></template>
