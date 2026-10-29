<script setup>
import { computed, ref, watch } from 'vue'
import { formatCashValue, updateCashInput } from '../services/shiftMoneyInput'
const props=defineProps({show:Boolean,caData:Object,loading:Boolean})
const emit=defineEmits(['close','confirm'])
const cash=ref(''),note=ref(''),error=ref('')
watch(()=>props.show,()=>{cash.value='';note.value='';error.value=''})
const difference=computed(()=>cash.value===''||!Number.isFinite(Number(cash.value))?null:Number(cash.value)-Number(props.caData?.tienMatDuKien||0))
const money=value=>Number(value||0).toLocaleString('vi-VN')+' đ'
function confirm(){
  if(props.loading)return
  error.value=''
  if(cash.value===''||!Number.isFinite(Number(cash.value))||Number(cash.value)<0){error.value='Nhập số tiền mặt thực tế đã kiểm đếm.';return}
  emit('confirm',{ghiChu:note.value.trim(),tienMatThucTe:Number(cash.value)})
}
watch(cash,()=>{error.value=''})
const formattedCash=computed(()=>formatCashValue(cash.value))
function onCashInput(event){cash.value=updateCashInput(event,cash.value)}
</script>
<template>
  <Teleport to="body"><div v-if="show" class="close-overlay" role="dialog" aria-modal="true" aria-labelledby="close-shift-title">
    <section class="close-card"><header class="close-heading"><div class="close-icon"><i class="bi bi-clipboard-check" aria-hidden="true"></i></div><div><h2 id="close-shift-title">Kết thúc ca làm việc</h2><p class="subtitle">{{ caData?.tenCa }} · {{ caData?.gioBatDau }} – {{ caData?.gioKetThuc }}</p></div></header>
      <dl><div><dt><i class="bi bi-cash-stack" aria-hidden="true"></i> Tiền mặt đầu ca</dt><dd>{{ money(caData?.tienMatDauCa) }}</dd></div>
        <div><dt>Thu/hoàn tiền mặt trong ca</dt><dd>{{ money(caData?.tienMatCuoiCa) }}</dd></div>
        <div><dt><i class="bi bi-bank" aria-hidden="true"></i> Thu/hoàn chuyển khoản</dt><dd>{{ money(caData?.chuyenKhoanCuoiCa) }}</dd></div>
        <div><dt>Thẻ / phương thức khác</dt><dd>{{ money(caData?.thanhToanKhac) }}</dd></div>
        <div class="total"><dt>Tổng thu/hoàn thuần trong ca</dt><dd>{{ money(caData?.doanhThuCa) }}</dd></div>
        <div class="expected"><dt>Tiền mặt dự kiến bàn giao</dt><dd>{{ money(caData?.tienMatDuKien) }}</dd></div>
      </dl>
      <label for="closing-cash">Tiền mặt thực tế đã kiểm đếm (đ)</label><input id="closing-cash" :value="formattedCash" @input="onCashInput" type="text" inputmode="decimal" autocomplete="off" placeholder="Nhập tiền thực tế, có thể bằng 0" :disabled="loading">
      <p v-if="difference!==null" class="difference" :class="{mismatch:difference!==0}">Chênh lệch: <strong>{{ money(difference) }}</strong></p>
      <label for="closing-note">Ghi chú kết ca</label><textarea id="closing-note" v-model="note" rows="2" maxlength="500" placeholder="Giải thích nếu có chênh lệch tiền" :disabled="loading"></textarea>
      <p v-if="error" class="error" role="alert">{{ error }}</p><footer><button :disabled="loading" @click="emit('close')">Hủy</button><button class="primary" :disabled="loading" @click="confirm"><i class="bi bi-check2" aria-hidden="true"></i> {{ loading?'Đang chốt ca…':'Xác nhận kết ca' }}</button></footer>
    </section>
  </div></Teleport>
</template>
<style scoped>
.close-overlay{position:fixed;inset:0;background:#122e4b70;z-index:10000;display:flex;align-items:center;justify-content:center;padding:20px}.close-card{box-sizing:border-box;width:100%;max-width:520px;padding:26px;background:white;border-radius:16px;color:#17364b;max-height:90vh;overflow:auto;box-shadow:0 20px 60px #122e4b30}.close-card h2{font-size:20px;margin:0 0 8px}.subtitle{color:#708695;font-size:14px}.close-card dl{background:#f6f9fc;border-radius:10px;padding:14px;margin:20px 0}.close-card dl>div{display:flex;justify-content:space-between;gap:16px;font-size:13px;padding:8px 0}.close-card dt{font-weight:400}.close-card dd{margin:0;font-weight:600;white-space:nowrap}.total{border-top:1px solid #dae6ee;margin-top:7px}.expected{color:#168fce;font-weight:600}.close-card label{display:block;font-size:13px;font-weight:600;margin:16px 0 8px}.close-card input,.close-card textarea{box-sizing:border-box;width:100%;border:1px solid #d3e2ec;border-radius:8px;padding:12px;font:inherit;color:#17364b}.difference{font-size:13px;color:#168d72}.difference.mismatch,.error{color:#c54444}.error{font-size:13px}footer{display:flex;justify-content:flex-end;gap:10px;margin-top:22px}footer button{border:1px solid #d4e3ee;padding:11px 14px;border-radius:8px;background:white;color:#42647a;cursor:pointer}footer .primary{background:#168fce;border-color:#168fce;color:white}button:disabled{opacity:.55;cursor:wait}
.close-heading{display:flex;align-items:center;gap:14px}.close-icon{width:48px;height:48px;border-radius:12px;display:grid;place-items:center;background:#eaf5fc;color:#168fce;font-size:23px;flex-shrink:0}.close-heading h2{margin:0 0 6px}.close-heading .subtitle{margin:0}.close-card dt i{margin-right:5px;color:#6b8799}.close-card footer .primary i{margin-right:5px}
</style>
