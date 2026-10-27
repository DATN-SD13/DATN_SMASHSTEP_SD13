<script setup>
import { useToast } from '../composables/useToast'
const { notification, close } = useToast()
const titles = { success: 'Thành công', error: 'Chưa thể thực hiện', info: 'Thông báo' }
</script>
<template>
  <Teleport to="body">
    <div v-if="notification" :key="notification.id" class="shop-toast" :class="notification.type"
      :role="notification.type === 'error' ? 'alert' : 'status'" aria-atomic="true">
      <div><strong>{{ titles[notification.type] }}</strong><p>{{ notification.message }}</p></div>
      <button type="button" aria-label="Đóng thông báo" @click="close">×</button>
    </div>
  </Teleport>
</template>
<style scoped>
.shop-toast{position:fixed;z-index:12000;top:90px;right:20px;display:flex;gap:16px;max-width:min(420px,calc(100vw - 32px));padding:16px 18px;border:1px solid #b9d6ed;border-left:4px solid #2492ce;border-radius:12px;background:#fff;color:#17324f;box-shadow:0 8px 30px #102a4326}.shop-toast.success{border-left-color:#168357}.shop-toast.error{border-left-color:#cf3f42}.shop-toast strong{font-size:14px}.shop-toast p{margin:5px 0 0;font-size:13px;line-height:1.6}.shop-toast button{align-self:flex-start;border:0;background:transparent;cursor:pointer;color:#64748b;font-size:24px;line-height:1;padding:0 2px}
@media(max-width:600px){.shop-toast{top:78px;right:16px}}
</style>
