<script setup>
import { computed, ref, watch } from 'vue'
import { formatCashValue, updateCashInput } from '../services/shiftMoneyInput'
import api from '../../../../utils/api'
const props=defineProps({data:{type:Object,required:true}})
const emit=defineEmits(['confirmed','rejected','close'])
const amount=ref(''),loading=ref(false),error=ref('')
watch(()=>`${props.data.lichLamViecId}:${props.data.lichLamViecCaTruocId}`,()=>{
  amount.value=props.data.tienMatCaTruocDaKiemDem && props.data.tienMatDauCa!=null
    ?String(props.data.tienMatDauCa):'';error.value=''
},{immediate:true})
const money=value=>new Intl.NumberFormat('vi-VN',{maximumFractionDigits:2}).format(Number(value||0))+' đ'
const previousDate=computed(()=>props.data.ngayCaTruoc?.split('-').reverse().join('/')||'')
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
    <section class="shift-card"><header class="shift-heading"><div class="shift-icon"><i class="bi bi-person-check" aria-hidden="true"></i></div>
      <div><h2 id="receive-title">Nhận ca làm việc</h2><p class="shift-sub">{{ data.tenCa }} · {{ data.gioBatDau }} – {{ data.gioKetThuc }}</p></div></header>
      <div v-if="data.lichLamViecCaTruocId" class="previous-shift">
        <strong>Bàn giao từ ca trước</strong>
        <p>{{ data.tenCaTruoc }} · {{ previousDate }}<br>{{ data.nhanVienCaTruoc }}</p>
        <div><span><i class="bi bi-cash-stack" aria-hidden="true"></i> Tiền mặt {{ data.tienMatCaTruocDaKiemDem?'đã kiểm đếm':'dự kiến' }}</span><b>{{ money(data.tienMatCaTruoc) }}</b></div>
        <div><span><i class="bi bi-bank" aria-hidden="true"></i> Chuyển khoản trong ca trước</span><b>{{ money(data.chuyenKhoanCaTruoc) }}</b></div>
        <div class="previous-total"><span>Tổng tiền tham chiếu</span><b>{{ money(Number(data.tienMatCaTruoc||0)+Number(data.chuyenKhoanCaTruoc||0)) }}</b></div>
        <p v-if="!data.tienMatCaTruocDaKiemDem" class="uncounted">Ca trước chưa lưu số tiền kiểm đếm. Hãy nhập số tiền mặt thực nhận.</p>
      </div>
      <p v-else class="shift-hint">Chưa có ca trước đã kết thúc để bàn giao.</p>
      <p class="shift-hint">Kiểm đếm tiền mặt thực tế được giao trước khi xác nhận. Tiền chuyển khoản được theo dõi riêng, không cộng vào tiền mặt đầu ca.</p>
      <p class="shift-hint">Nhận ca từ 15 phút trước giờ bắt đầu đến muộn tối đa 15 phút. Xác nhận sau giờ bắt đầu sẽ được ghi nhận đi muộn; chọn “Chưa nhận ca” không gia hạn thời gian.</p>
      <label for="opening-cash">Tiền mặt đầu ca (đ)</label><input id="opening-cash" :value="formattedAmount" @input="onAmountInput" type="text" inputmode="decimal" autocomplete="off" placeholder="Nhập số đã kiểm đếm, có thể bằng 0" :disabled="loading">
      <p v-if="error" class="shift-error" role="alert">{{ error }}</p><div class="shift-actions">
        <button type="button" :disabled="loading" @click="submit(false)">Chưa nhận ca</button><button class="primary" type="button" :disabled="loading" @click="submit(true)"><i class="bi bi-check2" aria-hidden="true"></i> {{ loading?'Đang xử lý…':'Xác nhận nhận ca' }}</button>
      </div>
    </section>
  </div></Teleport>
</template>
<style scoped>
.shift-overlay{position:fixed;inset:0;background:#122e4b70;z-index:10000;display:flex;align-items:center;justify-content:center;padding:20px}.shift-card{box-sizing:border-box;background:#fff;border-radius:16px;padding:28px;max-width:460px;width:100%;box-shadow:0 20px 60px #122e4b30;color:#17364b;max-height:90vh;overflow:auto}.shift-icon{width:48px;height:48px;border-radius:12px;background:#eaf5fc;color:#168fce;display:grid;place-items:center;font-size:23px}.shift-card h2{font-size:20px;margin:18px 0 8px}.shift-sub{color:#627e90;font-size:14px}.shift-hint{background:#f1f7fb;border:1px solid #dfebf3;border-radius:10px;padding:14px;font-size:13px;line-height:1.65}.shift-card label{display:block;font-size:14px;font-weight:600;margin:18px 0 9px}.shift-card input{box-sizing:border-box;width:100%;padding:12px;border:1px solid #cedde8;border-radius:8px;color:#17364b;font:inherit}.shift-actions{display:flex;justify-content:flex-end;gap:10px;margin-top:24px}.shift-actions button{border:1px solid #d4e3ee;padding:11px 14px;border-radius:8px;background:#fff;color:#42647a;cursor:pointer}.shift-actions .primary{background:#168fce;border-color:#168fce;color:white}.shift-actions button:disabled{opacity:.55;cursor:wait}.shift-error{color:#c43c42;font-size:13px;line-height:1.6}
.previous-shift{background:#fff9e8;border:1px solid #f4e4ae;border-radius:10px;padding:14px;font-size:13px;line-height:1.6}.previous-shift p{margin:5px 0 10px;color:#637383}.previous-shift>div{display:flex;justify-content:space-between;gap:12px;margin:8px 0}.previous-shift b{text-align:right;white-space:nowrap}.previous-total{border-top:1px solid #eadfbf;padding-top:8px}.previous-shift .uncounted{color:#a36400;margin-bottom:0}
.shift-heading{display:flex;align-items:center;gap:14px;margin-bottom:20px}.shift-heading .shift-icon{flex-shrink:0}.shift-heading h2{margin:0 0 6px}.shift-heading .shift-sub{margin:0}.previous-shift span i{color:#887032;margin-right:4px}.shift-actions .primary i{margin-right:5px}
</style>
