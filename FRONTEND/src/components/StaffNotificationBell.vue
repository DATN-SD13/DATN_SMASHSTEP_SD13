<script setup>
import {ref,computed,onMounted,onBeforeUnmount,watch} from 'vue'
import {useRouter} from 'vue-router'
import {useAuthStore} from '../stores/auth'
import api from '../utils/api'
const router=useRouter(),auth=useAuthStore(),root=ref(null),open=ref(false),items=ref([]),unread=ref(0),loading=ref(false),error=ref(''),saving=ref(false)
const filter=ref('all'),readFilter=ref('all')
const visible=computed(()=>items.value.filter(i=>(filter.value==='all'||i.kind===filter.value)&&(readFilter.value==='all'||readFilter.value==='read'&&i.read||readFilter.value==='unread'&&!i.read)))
let timer,disposed=false,version=0
async function refresh(){
  if(loading.value||saving.value||!auth.isStaff||auth.loggingOut||document.hidden)return
  const request=++version;loading.value=true
  try{const response=await api.get('/staff-notifications',{timeout:12000});if(disposed||request!==version)return
    if(!response.data?.success)throw Error(response.data?.message||'Không tải được thông báo')
    items.value=response.data.data.items||[];unread.value=response.data.data.unreadCount||0;error.value=''
  }catch(e){if(!disposed&&request===version)error.value=e.response?.data?.message||'Chưa tải được thông báo. Bấm thử lại.'}
  finally{if(request===version)loading.value=false}
}
async function mark(ids){
  if(!ids.length||saving.value)return false
  saving.value=true;++version;loading.value=false
  try{const response=await api.post('/staff-notifications/read',{ids},{timeout:12000});if(disposed)return false
    if(!response.data?.success)throw Error('Không lưu được trạng thái đã đọc')
    items.value=response.data.data.items||[];unread.value=response.data.data.unreadCount||0;error.value='';return true
  }catch(e){error.value=e.response?.data?.message||'Không đánh dấu đã đọc được. Vui lòng thử lại.';return false}
  finally{saving.value=false}
}
async function visit(item){if(saving.value)return;if(!item.read&&!await mark([item.id]))return;open.value=false;await router.push(item.url)}
function toggle(){open.value=!open.value;if(open.value)refresh()}
function outside(event){if(root.value&&!root.value.contains(event.target))open.value=false}
function key(event){if(event.key==='Escape')open.value=false}
function date(value){return value?new Date(value).toLocaleString('vi-VN',{hour:'2-digit',minute:'2-digit',day:'2-digit',month:'2-digit'}):''}
watch(()=>auth.user?.id,()=>{++version;loading.value=false;items.value=[];unread.value=0;open.value=false;refresh()})
onMounted(()=>{refresh();timer=setInterval(refresh,10000);document.addEventListener('click',outside);document.addEventListener('keydown',key);window.addEventListener('focus',refresh)})
onBeforeUnmount(()=>{disposed=true;++version;clearInterval(timer);document.removeEventListener('click',outside);document.removeEventListener('keydown',key);window.removeEventListener('focus',refresh)})
</script>
<template>
 <div ref="root" class="staff-notifications">
  <button class="notification-bell" type="button" :aria-label="`Thông báo, ${unread} chưa đọc`" :aria-expanded="open" aria-controls="staff-notification-panel" @click="toggle"><i class="bi bi-bell" aria-hidden="true"></i><span v-if="unread" class="notification-count">{{unread>99?'99+':unread}}</span></button>
  <section v-if="open" id="staff-notification-panel" class="notification-panel" aria-label="Danh sách thông báo">
   <header><strong>Thông báo <small v-if="unread">{{unread}} chưa đọc</small></strong><button type="button" :disabled="saving||!unread" @click="mark(items.filter(i=>!i.read).map(i=>i.id))">Đọc tất cả</button></header>
   <nav aria-label="Loại thông báo"><button v-for="tab in [{key:'all',label:'Tất cả'},{key:'order',label:'Đơn hàng'},{key:'chat',label:'Tin nhắn'}]" :key="tab.key" type="button" :class="{active:filter===tab.key}" @click="filter=tab.key">{{tab.label}}</button></nav>
   <p v-if="error" class="notification-error" role="alert">{{error}} <button type="button" @click="refresh">Thử lại</button></p>
   <p v-else-if="loading&&!items.length" class="notification-empty" role="status">Đang tải thông báo…</p>
   <div class="notification-read-filters" aria-label="Lọc trạng thái đọc"><button v-for="tab in [{key:'all',label:'Tất cả trạng thái'},{key:'unread',label:'Chưa đọc'},{key:'read',label:'Đã đọc'}]" :key="tab.key" type="button" :aria-pressed="readFilter===tab.key" :class="{active:readFilter===tab.key}" @click="readFilter=tab.key">{{tab.label}}</button></div>
   <div class="notification-list"><article v-for="item in visible" :key="item.id" class="notification-item" :class="{unread:!item.read}">
    <button type="button" class="notification-open" :disabled="saving" @click="visit(item)"><span class="notification-icon" :class="item.kind"><i :class="item.kind==='order'?'bi bi-bag-check':'bi bi-chat-dots'" aria-hidden="true"></i></span><span class="notification-content"><strong>{{item.title}}</strong><span>{{item.description}}</span><time>{{date(item.createdAt)}}</time></span></button>
    <div class="notification-read-actions"><span class="read-state" :class="{read:item.read}"><i :class="item.read?'bi bi-check2-all':'bi bi-circle-fill'" aria-hidden="true"></i>{{item.read?'Đã đọc':'Chưa đọc'}}</span><button v-if="!item.read" type="button" class="mark-one-read" :disabled="saving" :aria-label="'Đánh dấu đã đọc: '+item.title" @click="mark([item.id])">Đánh dấu đã đọc</button></div>
   </article></div>
   <p v-if="!loading&&!error&&!visible.length" class="notification-empty">Chưa có thông báo trong mục này.</p>
   <footer>Đơn trực tuyến mới và yêu cầu hỗ trợ · Tối đa 50 thông báo gần nhất</footer>
  </section>
 </div>
</template>
<style scoped>
.staff-notifications{position:relative;flex-shrink:0}.notification-bell{position:relative;width:44px;height:44px;border:1px solid #dbe6f0;background:#f8fbff;border-radius:50%;color:#60778d;font-size:19px;display:grid;place-items:center;cursor:pointer}.notification-bell:hover{background:#eaf4ff;border-color:#99c7f8}.notification-count{position:absolute;right:-3px;top:-3px;background:#d12c3c;color:white;border:2px solid white;border-radius:20px;padding:1px 5px;min-width:19px;height:19px;font-size:10px;box-sizing:content-box;display:grid;place-items:center;font-weight:700}.notification-panel{position:absolute;right:0;top:55px;width:390px;max-width:calc(100vw - 24px);background:white;border:1px solid #dce7f0;border-radius:15px;box-shadow:0 14px 50px #16354d25;z-index:5000;overflow:hidden;color:#24455d;text-align:left}.notification-panel header{padding:18px;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #edf2f6;gap:10px}.notification-panel header strong{font-size:16px}.notification-panel small{font-size:11px;font-weight:400;color:#8b9fb0;display:block;margin-top:5px}.notification-panel header button,.notification-error button{background:none;border:0;color:#168fce;font-size:12px;cursor:pointer}.notification-panel nav{display:flex;gap:8px;padding:12px 16px}.notification-panel nav button{border:0;background:#f4f7fa;border-radius:20px;padding:7px 12px;color:#6f8799;cursor:pointer}.notification-panel nav button.active{background:#e4f2ff;color:#168fce}.notification-list{max-height:min(430px,55vh);overflow:auto}.notification-item{border:0;border-top:1px solid #f0f4f7;background:white;width:100%;display:flex;text-align:left;align-items:flex-start;gap:12px;padding:15px 17px;cursor:pointer;color:inherit}.notification-item.unread{background:#f1f8ff}.notification-item:hover{background:#eaf4fc}.notification-icon{flex-shrink:0;width:36px;height:36px;border-radius:11px;background:#e1efff;color:#2675c7;display:grid;place-items:center}.notification-icon.chat{background:#e4f7ee;color:#29966b}.notification-content{display:flex;flex:1;min-width:0;flex-direction:column;gap:5px;font-size:12px;line-height:1.5}.notification-content strong{font-size:13px}.notification-content time{color:#94a6b4;font-size:11px}.unread-dot{width:7px;height:7px;flex-shrink:0;border-radius:50%;background:#2383e8;margin-top:6px}.notification-empty{padding:30px 20px;text-align:center;color:#869cad;font-size:13px}.notification-error{padding:12px 16px;color:#b34e59;font-size:12px}.notification-panel footer{font-size:10px;color:#91a2af;padding:12px 16px;border-top:1px solid #edf2f6}.notification-panel button:disabled{opacity:.6;cursor:wait}@media(max-width:600px){.notification-panel{position:fixed;right:12px;top:110px}.notification-bell{width:38px;height:38px}}
.notification-item{display:block;padding:0;cursor:default}.notification-open{width:100%;background:none;border:0;display:flex;align-items:flex-start;gap:12px;padding:15px 17px 8px;text-align:left;color:inherit;cursor:pointer}.notification-read-actions{display:flex;align-items:center;justify-content:space-between;gap:10px;padding:0 17px 12px 65px}.read-state{display:flex;align-items:center;gap:5px;color:#2483c7;font-size:10px;white-space:nowrap}.read-state i{font-size:6px}.read-state.read{color:#7e95a5}.read-state.read i{font-size:13px}.mark-one-read{border:0;background:none;padding:3px 0;font-size:10px;color:#168fce;cursor:pointer}.notification-read-filters{display:flex;gap:5px;flex-wrap:wrap;padding:0 16px 12px}.notification-read-filters button{border:1px solid #e2eaf1;background:white;color:#8a9fae;font-size:10px;padding:5px 8px;border-radius:6px;cursor:pointer}.notification-read-filters button.active{background:#edf6ff;border-color:#afd1ed;color:#168fce}
</style>
