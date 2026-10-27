<script setup>
import { computed, ref, watch } from 'vue'
import { formatCashValue, updateCashInput } from '../services/shiftMoneyInput'
import api from '../../../../utils/api'
const props=defineProps({data:{type:Object,required:true}})
const emit=defineEmits(['confirmed','rejected','close'])
const amount=ref(''),loading=ref(false),error=ref('')
watch(()=>props.data.lichLamViecId,()=>{amount.value='';error.value=''})
async function submit(receive){
  if(loading.value)return
  error.value=''
  if(receive && (amount.value==='' || !Number.isFinite(Number(amount.value)) || Number(amount.value)<0)){
    error.value='Nhập tiền mặt đã kiểm đếm; nhập 0 nếu không có tiền đầu ca.';return
  }
  loading.value=true
  try {
    await api.post(`/lich-lam-viec/${props.data.lichLamViecId}/${receive?'xac-nhan-nhan-ca':'chua-nhan-ca'}`,
      receive?{tienMatDauCa:Number(amount.value)}:undefined)
    emit(receive?'confirmed':'rejected')
  } catch(e){error.value=e.response?.data?.message||'Không thể cập nhật ca. Vui lòng thử lại.'}
  finally{loading.value=false}
}
watch(amount,()=>{error.value=''})
const formattedAmount=computed(()=>formatCashValue(amount.value))
function onAmountInput(event){amount.value=updateCashInput(event,amount.value)}
</script>
<template>
  <Teleport to="body"><div class="shift-overlay" role="dialog" aria-modal="true" aria-labelledby="receive-title">
    <section class="shift-card"><div class="shift-icon"><i class="bi bi-arrow-left-right"></i></div>
      <h2 id="receive-title">Nhận ca làm việc</h2><p class="shift-sub">{{ data.tenCa }} · {{ data.gioBatDau }} – {{ data.gioKetThuc }}</p>
      <p class="shift-hint">Kiểm đếm tiền mặt thực tế được giao trước khi xác nhận. Tiền chuyển khoản được theo dõi riêng, không cộng vào tiền mặt đầu ca.</p>
      <label for="opening-cash">Tiền mặt đầu ca (đ)</label><input id="opening-cash" :value="formattedAmount" @input="onAmountInput" type="text" inputmode="decimal" autocomplete="off" placeholder="Nhập số đã kiểm đếm, có thể bằng 0" :disabled="loading">
      <p v-if="error" class="shift-error" role="alert">{{ error }}</p><div class="shift-actions">
        <button type="button" :disabled="loading" @click="submit(false)">Chưa nhận ca</button><button class="primary" type="button" :disabled="loading" @click="submit(true)">{{ loading?'Đang xử lý…':'Xác nhận nhận ca' }}</button>
      </div>
    </section>
  </div></Teleport>
</template>
<style scoped>
.shift-overlay{position:fixed;inset:0;background:#122e4b70;z-index:10000;display:flex;align-items:center;justify-content:center;padding:20px}.shift-card{box-sizing:border-box;background:#fff;border-radius:16px;padding:28px;max-width:460px;width:100%;box-shadow:0 20px 60px #122e4b30;color:#17364b;max-height:90vh;overflow:auto}.shift-icon{width:48px;height:48px;border-radius:12px;background:#eaf5fc;color:#168fce;display:grid;place-items:center;font-size:23px}.shift-card h2{font-size:20px;margin:18px 0 8px}.shift-sub{color:#627e90;font-size:14px}.shift-hint{background:#f1f7fb;border:1px solid #dfebf3;border-radius:10px;padding:14px;font-size:13px;line-height:1.65}.shift-card label{display:block;font-size:14px;font-weight:600;margin:18px 0 9px}.shift-card input{box-sizing:border-box;width:100%;padding:12px;border:1px solid #cedde8;border-radius:8px;color:#17364b;font:inherit}.shift-actions{display:flex;justify-content:flex-end;gap:10px;margin-top:24px}.shift-actions button{border:1px solid #d4e3ee;padding:11px 14px;border-radius:8px;background:#fff;color:#42647a;cursor:pointer}.shift-actions .primary{background:#168fce;border-color:#168fce;color:white}.shift-actions button:disabled{opacity:.55;cursor:wait}.shift-error{color:#c43c42;font-size:13px;line-height:1.6}
</style>
