<script setup>
import { ref, watch, onBeforeUnmount } from 'vue'
import { invoiceService } from '../services/invoiceService'
const props = defineProps({ code: { type: String, required: true } })
const emit = defineEmits(['confirmed'])
const payment = ref(null), loading = ref(false), saving = ref(false), reference = ref(''), received = ref(false), error = ref(''), notice = ref('')
let version = 0
const money = value => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(Number(value || 0))
async function load() {
  if (saving.value) return
  const current = ++version; loading.value = true; error.value = ''
  try {
    const result = await invoiceService.qrPayment(props.code)
    if (current === version) payment.value = result.data
  } catch (e) { if (current === version) { payment.value = null; error.value = e.response?.data?.message || 'Chưa tải được thông tin đối soát QR.' } }
  finally { if (current === version) loading.value = false }
}
async function confirm() {
  if (saving.value || loading.value || !payment.value || !['PENDING', 'UNAVAILABLE', 'EXPIRED'].includes(payment.value.status)) return
  const refCode = reference.value.trim()
  if (!/^[A-Za-z0-9][A-Za-z0-9._/\-]{3,99}$/.test(refCode)) { error.value = 'Nhập mã giao dịch ngân hàng từ 4 đến 100 ký tự; chỉ gồm chữ, số, dấu chấm, gạch nối, gạch chéo hoặc gạch dưới.'; return }
  if (!received.value) { error.value = 'Cần kiểm tra giao dịch thực tế trên ngân hàng và xác nhận đã nhận đủ tiền.'; return }
  const current = ++version; saving.value = true; error.value = ''
  try {
    const result = await invoiceService.confirmQrPayment(props.code, { amount: payment.value.amount, reference: refCode, received: true })
    if (current !== version) return
    payment.value = result.data; notice.value = 'Đã ghi nhận nhận đủ tiền. Có thể tiếp tục xử lý đơn hàng.'; emit('confirmed')
  } catch (e) { if (current === version) error.value = e.response?.data?.message || 'Chưa xác định được kết quả. Tải lại trạng thái trước khi thử lại với cùng mã giao dịch.' }
  finally { if (current === version) saving.value = false }
}
watch(() => props.code, () => { version++; payment.value = null; saving.value = false; reference.value = ''; received.value = false; notice.value = ''; load() }, { immediate: true })
onBeforeUnmount(() => { version++ })
</script>
<template>
  <section class="bank-confirmation" aria-label="Đối soát chuyển khoản QR">
    <h3><i class="bi bi-qr-code"></i> Đối soát chuyển khoản QR</h3>
    <p v-if="loading" role="status">Đang tải trạng thái thanh toán…</p>
    <p v-if="error" class="bank-error" role="alert">{{ error }}</p>
    <p v-if="notice" class="bank-success" role="status">{{ notice }}</p>
    <template v-if="payment && payment.status !== 'OTHER'">
      <p>{{ payment.message }}</p>
      <form v-if="['PENDING', 'UNAVAILABLE', 'EXPIRED'].includes(payment.status)" @submit.prevent="confirm">
        <div class="bank-due"><span>Phải nhận đủ</span><strong>{{ money(payment.amount) }}</strong></div>
        <p v-if="payment.accountNumber" class="bank-receiver">{{ payment.bankLabel }} · {{ payment.accountNumber }}<br>{{ payment.accountName }}<br>Nội dung: <strong>{{ payment.memo }}</strong></p>
        <label class="bank-field">Mã giao dịch ngân hàng <span aria-hidden="true">*</span><input v-model="reference" class="form-control" autocomplete="off" maxlength="100" :disabled="saving || loading" required placeholder="Mã giao dịch từ sao kê ngân hàng"></label>
        <label class="bank-checkbox"><input v-model="received" type="checkbox" :disabled="saving || loading" required><span>Tôi đã kiểm tra tài khoản ngân hàng, nhận đủ tiền và đối chiếu đúng nội dung đơn {{ code }}.</span></label>
        <p class="bank-caution">Chỉ xác nhận sau khi tiền thực tế đã vào tài khoản; ảnh chụp của khách hoặc mã QR không chứng minh đã nhận tiền.</p>
        <button class="btn btn-primary bank-submit" type="submit" :disabled="saving || loading || !received || !reference.trim()">{{ saving ? 'Đang ghi nhận…' : 'Xác nhận đã nhận đủ tiền' }}</button>
      </form>
    </template>
    <button class="bank-refresh" type="button" :disabled="loading || saving" @click="load">Tải lại trạng thái</button>
  </section>
</template>
<style scoped>
.bank-confirmation{padding:16px;margin:14px 0;border:1px solid #cbe4f4;background:#f5fbff;border-radius:10px;font-size:12px;color:#3b5662;line-height:1.6}.bank-confirmation h3{font-size:14px;font-weight:700;margin-bottom:12px}.bank-due{display:flex;justify-content:space-between;gap:12px;padding:12px 0}.bank-due strong{font-size:18px;color:#087bb4}.bank-receiver{padding:10px;background:#fff;border-radius:8px;overflow-wrap:anywhere}.bank-field{display:block;font-weight:650;margin:14px 0}.bank-field>span{color:#c23f49}.bank-field input{margin-top:7px;font-size:12px}.bank-checkbox{display:flex;align-items:flex-start;gap:9px;margin-bottom:12px}.bank-checkbox input{margin-top:4px;flex-shrink:0}.bank-caution{font-size:11px;color:#8b6615}.bank-submit{width:100%;font-size:12px}.bank-refresh{border:0;background:none;padding:10px 0 0;color:#087bb4;font-size:11px;text-decoration:underline}.bank-error{color:#b33039}.bank-success{color:#157247}button:disabled{opacity:.6}
</style>
