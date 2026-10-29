<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import chatService from '../services/chatService'
import { confirmAction, showError, showSuccess } from '../../../../utils/feedback'

const route=useRoute()
const sessions = ref([])
const selected = ref(null)
const detail = ref(null)
const loading = ref(false)
const sending = ref(false)
const search = ref('')
const tab = ref('active')
const text = ref('')
const messagesEl = ref(null)
const listEl=ref(null),detailLoading=ref(false),detailError=ref(''),newMessagesBelow=ref(false)
let timer=null,disposed=false,refreshing=false,detailVersion=0,listVersion=0
function nearBottom(){const el=messagesEl.value;return !el||el.scrollHeight-el.scrollTop-el.clientHeight<80}
function onMessagesScroll(){if(nearBottom())newMessagesBelow.value=false}

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  return sessions.value.filter(s => {
    const tabOk = tab.value === 'history' ? s.status === 'CLOSED' : s.status !== 'CLOSED'
    const qOk = !q || [s.customerName, s.customerPhone, s.customerCode]
      .some(v => String(v || '').toLowerCase().includes(q))
    return tabOk && qOk
  })
})

const current = computed(() => detail.value?.session || selected.value)
const messages = computed(() => detail.value?.messages || [])
const canReply = computed(() => current.value?.status === 'STAFF_ACTIVE')
const waiting = computed(() => current.value?.status === 'WAITING_STAFF')
const closed = computed(() => current.value?.status === 'CLOSED')

function statusInfo(status) {
  return {
    AI_ACTIVE: { label: 'AI đang hỗ trợ', cls: 'ai' },
    WAITING_STAFF: { label: 'Chờ hỗ trợ', cls: 'waiting' },
    STAFF_ACTIVE: { label: 'Đang chat', cls: 'active' },
    CLOSED: { label: 'Đã đóng', cls: 'closed' }
  }[status] || { label: status, cls: '' }
}
function fmtTime(v) {
  if (!v) return ''
  return new Date(v).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' })
}
function initials(name) {
  return String(name || 'KH').trim().split(/\s+/).slice(-2).map(x => x[0]).join('').toUpperCase()
}
function errorText(e) { return e?.response?.data?.message || 'Không thể thực hiện thao tác.' }
async function scrollBottom() { await nextTick(); if (messagesEl.value) messagesEl.value.scrollTop = messagesEl.value.scrollHeight;newMessagesBelow.value=false }

async function loadSessions(silent = false) {
  const version=++listVersion
  if (!silent) loading.value = true
  try {
    const data = await chatService.sessions(true)
    if(disposed||version!==listVersion)return
    sessions.value = data
    if (selected.value) {
      const fresh = data.find(x => x.id === selected.value.id)
      if (fresh) selected.value = fresh
    }
  } catch (e) { if (!disposed && !silent) showError(errorText(e), 'Không tải được chat') }
  finally { if (!disposed && version===listVersion) loading.value = false }
}

async function openSession(row,silent=false) {
  if(!row||disposed||sending.value)return
  const switching=selected.value?.id!==row.id,version=++detailVersion
  selected.value=row
  if(switching){detail.value=null;text.value='';newMessagesBelow.value=false}
  detailLoading.value=true;detailError.value=''
  try{
    const result=await chatService.detail(row.id)
    if(disposed||version!==detailVersion||selected.value?.id!==row.id)return
    const el=messagesEl.value,previousTop=el?.scrollTop||0,stick=nearBottom()
    const changed=messages.value.at(-1)?.id!==result.messages?.at(-1)?.id
    detail.value=result
    await nextTick()
    if(disposed||version!==detailVersion)return
    if(switching||(changed&&stick))await scrollBottom()
    else if(messagesEl.value){messagesEl.value.scrollTop=previousTop;if(changed)newMessagesBelow.value=true}
  }catch(e){if(!disposed&&version===detailVersion){detailError.value=errorText(e);if(!silent)showError(errorText(e))}}
  finally{if(!disposed&&version===detailVersion)detailLoading.value=false}
}
function clearSelection(){++detailVersion;selected.value=null;detail.value=null;detailLoading.value=false;detailError.value='';newMessagesBelow.value=false;text.value=''}
async function focusSession(row){
  if(!row||disposed)return
  tab.value=row.status==='CLOSED'?'history':'active'
  await nextTick();await openSession(row)
}
async function refresh(){
  if(refreshing||disposed||document.hidden)return
  refreshing=true
  try{await loadSessions(true);if(!disposed&&selected.value&&!detailLoading.value&&!sending.value&&current.value?.status!=='CLOSED')await openSession(selected.value,true)}
  finally{refreshing=false}
}
watch(tab,async()=>{
  await nextTick();if(disposed)return
  if(listEl.value)listEl.value.scrollTop=0
  const target=filtered.value.find(row=>row.id===selected.value?.id)||filtered.value[0]
  if(!target)clearSelection()
  else if(target.id!==selected.value?.id)await openSession(target)
})

async function acceptChat() {
  if (!selected.value || sending.value) return
  sending.value = true
  ++detailVersion;detailLoading.value=false
  const id=selected.value.id
  try {
    const result=await chatService.accept(id)
    if(disposed||selected.value?.id!==id)return
    detail.value = result
    selected.value = detail.value.session
    showSuccess('Bạn đã tiếp nhận cuộc trò chuyện.', 'Đã kết nối')
    await loadSessions(true)
    await scrollBottom()
  } catch (e) { showError(errorText(e)) }
  finally { sending.value = false }
}

async function send() {
  const content = text.value.trim()
  if (!content || !canReply.value || sending.value) return
  sending.value = true
  ++detailVersion;detailLoading.value=false
  const id=selected.value.id
  try {
    const result=await chatService.send(id, content)
    if(disposed||selected.value?.id!==id)return
    detail.value = result
    selected.value = detail.value.session
    text.value = ''
    await loadSessions(true)
    await scrollBottom()
  } catch (e) { showError(errorText(e)) }
  finally { sending.value = false }
}

function closeChat() {
  if (!selected.value || closed.value || sending.value) return
  const id=selected.value.id
  confirmAction(
    'Khách hàng sẽ quay lại trò chuyện với trợ lý AI ở phiên mới.',
    async () => {
      if(sending.value||disposed)return
      sending.value=true;++detailVersion;detailLoading.value=false
      try {
        const result=await chatService.close(id)
        if(disposed||selected.value?.id!==id)return
        detail.value = result
        selected.value = detail.value.session
        showSuccess('Phiên hỗ trợ đã được đóng.')
        await loadSessions(true)
        tab.value='history'
      } catch (e) { if(!disposed)showError(errorText(e)) }
      finally{sending.value=false}
    },
    { title: 'Đóng phiên hỗ trợ?', confirmText: 'Đóng phiên' }
  )
}

onMounted(async()=>{
  await loadSessions();if(disposed)return
  const first=sessions.value.find(s=>String(s.id)===String(route.query.session))||filtered.value[0]
  if(first)await focusSession(first)
  if(!disposed)timer=window.setInterval(refresh,1500)
})
watch(()=>route.query.session,async value=>{if(!value||disposed)return;await loadSessions();if(disposed)return;const target=sessions.value.find(s=>String(s.id)===String(value));if(target)await focusSession(target)})
onBeforeUnmount(()=>{disposed=true;++detailVersion;++listVersion;window.clearInterval(timer)})
</script>

<template>
  <AdminLayout>
    <main class="chat-page">
      <div class="chat-title">
        <div>
          <h1>Hỗ trợ khách hàng trực tuyến</h1>
          <p>Tiếp nhận yêu cầu từ trợ lý AI và tư vấn trực tiếp cho khách hàng.</p>
        </div>
        <div class="live-pill"><span></span> Trực tuyến</div>
      </div>

      <section class="chat-shell">
        <aside class="conversation-list">
          <div class="search-box"><i class="bi bi-search"></i><input v-model="search" placeholder="Tìm khách hàng, SĐT..." /></div>
          <div class="tabs">
            <button :disabled="sending" :class="{ on: tab === 'active' }" @click="tab='active'">Đang hoạt động</button>
            <button :disabled="sending" :class="{ on: tab === 'history' }" @click="tab='history'">Lịch sử chat</button>
          </div>

          <div ref="listEl" class="list-scroll" tabindex="0" aria-label="Danh sách cuộc trò chuyện">
            <div v-if="loading" class="empty-list">Đang tải...</div>
            <button
              v-for="row in filtered" :key="row.id" type="button"
              class="conversation" :class="{ selected: selected?.id === row.id }" :disabled="sending"
              @click="openSession(row)"
            >
              <div class="avatar">{{ initials(row.customerName) }}</div>
              <div class="conversation-copy">
                <div class="conversation-top"><strong>{{ row.customerName }}</strong><time>{{ fmtTime(row.updatedAt) }}</time></div>
                <div class="phone">{{ row.customerPhone || row.customerCode || 'Chưa có SĐT' }}</div>
                <p>{{ row.lastMessage }}</p>
                <span class="status" :class="statusInfo(row.status).cls">{{ statusInfo(row.status).label }}</span>
              </div>
            </button>
            <div v-if="!loading && !filtered.length" class="empty-list">Không có cuộc trò chuyện phù hợp.</div>
          </div>
        </aside>

        <section v-if="current" class="conversation-main">
          <header class="conversation-head">
            <div class="customer-head">
              <div class="avatar large">{{ initials(current.customerName) }}</div>
              <div><strong>{{ current.customerName }}</strong><span>{{ current.customerPhone || current.customerCode }}</span></div>
            </div>
            <div class="head-actions">
              <button v-if="waiting" class="accept-btn" :disabled="sending" @click="acceptChat"><i class="bi bi-headset"></i> Tiếp nhận</button>
              <button v-if="canReply" class="close-btn" @click="closeChat"><i class="bi bi-x-circle"></i> Đóng phiên chat</button>
              <span v-if="closed" class="closed-badge">Đã đóng</span>
            </div>
          </header>

          <div ref="messagesEl" class="messages" tabindex="0" aria-label="Nội dung cuộc trò chuyện" @scroll="onMessagesScroll">
            <p v-if="detailLoading && !detail" class="detail-status" role="status">Đang tải cuộc trò chuyện…</p>
            <p v-if="detailError" class="detail-error" role="alert">{{ detailError }} <button type="button" @click="openSession(selected)">Thử lại</button></p>
            <div v-for="m in messages" :key="m.id" class="message" :class="m.senderType.toLowerCase()">
              <template v-if="m.senderType === 'SYSTEM'">
                <div class="system-message">{{ m.content }}</div>
              </template>
              <template v-else>
                <div class="message-meta">{{ m.senderType === 'CUSTOMER' ? current.customerName : (m.senderName || 'Trợ lý AI') }} · {{ fmtTime(m.createdAt) }}</div>
                <div class="message-bubble">{{ m.content }}</div>
              </template>
            </div>
          </div>

          <button v-if="newMessagesBelow" class="new-messages" type="button" @click="scrollBottom">Có tin nhắn mới ↓</button>
          <div v-if="waiting" class="takeover-note"><i class="bi bi-info-circle"></i> Khách đang chờ nhân viên. Bấm <b>Tiếp nhận</b> để bắt đầu trả lời.</div>
          <form class="reply-box" @submit.prevent="send">
            <input v-model="text" maxlength="2000" :disabled="!canReply || sending" :placeholder="closed ? 'Phiên chat đã đóng' : waiting ? 'Hãy tiếp nhận phiên chat trước...' : 'Nhập nội dung phản hồi khách hàng... (Nhấn Enter để gửi)'" />
            <button type="submit" :disabled="!canReply || !text.trim() || sending"><i class="bi bi-send-fill"></i></button>
          </form>
        </section>

        <section v-else class="no-selection"><i class="bi bi-chat-square-dots"></i><h2>Chọn một cuộc trò chuyện</h2><p>Các yêu cầu cần nhân viên hỗ trợ sẽ xuất hiện ở cột bên trái.</p></section>
      </section>
    </main>
  </AdminLayout>
</template>

<style scoped>
.chat-page{height:calc(100vh - 72px);height:calc(100dvh - 72px);padding:18px 22px 22px;display:flex;flex:0 0 auto;flex-direction:column;gap:14px;min-height:0;box-sizing:border-box;overflow:hidden}.chat-title{flex-shrink:0;display:flex;align-items:center;justify-content:space-between}.chat-title h1{font-size:22px;font-weight:750;color:var(--ss-ink)}.chat-title p{margin-top:4px;color:var(--ss-muted);font-size:11px}.live-pill{display:flex;align-items:center;gap:7px;border:1px solid var(--ss-border);background:#fff;border-radius:999px;padding:8px 12px;font-weight:650}.live-pill span{width:7px;height:7px;border-radius:50%;background:var(--ss-success);box-shadow:0 0 0 4px var(--ss-success-bg)}
.chat-shell{flex:1;min-height:0;display:grid;grid-template-columns:300px minmax(0,1fr);grid-template-rows:minmax(0,1fr);background:#fff;border:1px solid var(--ss-border);border-radius:14px;overflow:hidden}.conversation-list{min-width:0;min-height:0;overflow:hidden;border-right:1px solid var(--ss-border);display:flex;flex-direction:column;background:#fbfdff}.search-box{flex-shrink:0;margin:14px 14px 10px;position:relative}.search-box i{position:absolute;left:12px;top:50%;transform:translateY(-50%);color:var(--ss-faint)}.search-box input{width:100%;height:38px;border:1px solid var(--ss-border);border-radius:9px;background:#fff;padding:0 12px 0 34px;outline:0;font-size:11px}.search-box input:focus{border-color:var(--ss-primary);box-shadow:0 0 0 3px var(--ss-primary-soft)}.tabs{flex-shrink:0;margin:0 14px 10px;display:grid;grid-template-columns:1fr 1fr;padding:3px;background:#f1f5f9;border-radius:9px}.tabs button{height:31px;border:0;border-radius:7px;background:transparent;color:var(--ss-muted);font-size:10px;font-weight:650}.tabs button.on{background:#fff;color:var(--ss-primary);box-shadow:0 1px 5px rgba(15,23,42,.08)}.list-scroll{flex:1;min-height:0;overflow:auto;overscroll-behavior:contain;scrollbar-gutter:stable;padding:0 9px 12px}.conversation{width:100%;display:flex;gap:10px;text-align:left;border:1px solid transparent;background:transparent;border-radius:11px;padding:11px 9px;margin-bottom:6px;cursor:pointer}.conversation:hover{background:#f5f9ff}.conversation.selected{border-color:#c9ddfa;background:var(--ss-primary-soft)}.avatar{width:34px;height:34px;flex:0 0 34px;border-radius:50%;display:grid;place-items:center;background:#dcecff;color:var(--ss-primary);font-weight:750;font-size:11px}.avatar.large{width:39px;height:39px;flex-basis:39px}.conversation-copy{min-width:0;flex:1}.conversation-top{display:flex;justify-content:space-between;gap:6px}.conversation-top strong{font-size:11px;color:var(--ss-ink)}.conversation-top time{font-size:8px;color:var(--ss-faint);white-space:nowrap}.phone{font-size:9px;color:var(--ss-muted);margin-top:2px}.conversation-copy p{font-size:9px;color:var(--ss-muted);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;margin:5px 0}.status{display:inline-flex;border-radius:999px;padding:3px 7px;font-size:8px;font-weight:700}.status.waiting{background:var(--ss-warn-bg);color:var(--ss-warn)}.status.active{background:var(--ss-success-bg);color:var(--ss-success)}.status.ai{background:var(--ss-primary-soft);color:var(--ss-primary)}.status.closed{background:#eef1f5;color:#6b7280}.empty-list{padding:28px 12px;text-align:center;color:var(--ss-faint);font-size:10px}
.conversation-main{min-width:0;min-height:0;display:flex;flex-direction:column}.conversation-head{height:68px;flex:0 0 68px;border-bottom:1px solid var(--ss-border);display:flex;align-items:center;justify-content:space-between;padding:0 17px}.customer-head{display:flex;align-items:center;gap:10px}.customer-head>div:last-child{display:flex;flex-direction:column;gap:3px}.customer-head strong{font-size:12px;color:var(--ss-ink)}.customer-head span{font-size:9px;color:var(--ss-muted)}.head-actions{display:flex;align-items:center;gap:8px}.head-actions button{height:34px;border-radius:8px;padding:0 11px;font-size:10px;font-weight:650;cursor:pointer}.accept-btn{border:0;background:var(--ss-primary);color:#fff}.close-btn{border:1px solid #f1c8cb;background:#fff8f8;color:var(--ss-danger)}.closed-badge{padding:6px 10px;border-radius:999px;background:#eef1f5;color:#6b7280;font-size:9px;font-weight:700}
.messages{flex:1;min-height:0;overflow:auto;overflow-anchor:none;overscroll-behavior:contain;scrollbar-gutter:stable;padding:18px 20px;background:#fff}.message{display:flex;flex-direction:column;align-items:flex-start;margin-bottom:15px}.message.staff{align-items:flex-end}.message-meta{font-size:8px;color:var(--ss-muted);margin:0 5px 4px}.message-bubble{max-width:72%;border:1px solid var(--ss-border);background:#fff;border-radius:5px 13px 13px 13px;padding:10px 12px;font-size:11px;line-height:1.55;color:var(--ss-ink);white-space:pre-wrap;overflow-wrap:anywhere}.staff .message-bubble{background:var(--ss-primary);border-color:var(--ss-primary);color:#fff;border-radius:13px 5px 13px 13px}.ai .message-bubble{background:#f6f3ff;border-color:#e5dcff;color:#57418a}.system-message{align-self:center;background:#f5f8fc;color:var(--ss-muted);border-radius:999px;padding:7px 11px;font-size:9px}.takeover-note{padding:8px 15px;border-top:1px solid #f4e4bb;background:#fffaf0;color:#956400;font-size:10px}.reply-box{height:62px;flex:0 0 62px;display:flex;align-items:center;gap:8px;border-top:1px solid var(--ss-border);padding:9px 13px}.reply-box input{flex:1;min-width:0;height:41px;border:1px solid var(--ss-border);border-radius:9px;background:var(--ss-surface);padding:0 12px;outline:0;font-size:11px}.reply-box input:focus{border-color:var(--ss-primary);background:#fff}.reply-box input:disabled{opacity:.65}.reply-box button{width:40px;height:40px;border:0;border-radius:9px;background:var(--ss-primary);color:#fff}.reply-box button:disabled{opacity:.35}.no-selection{display:flex;flex-direction:column;align-items:center;justify-content:center;color:var(--ss-muted);text-align:center}.no-selection i{font-size:40px;color:#b7c7dc}.no-selection h2{font-size:15px;color:var(--ss-text);margin-top:12px}.no-selection p{font-size:10px;margin-top:5px}
@media(max-width:900px){.chat-shell{grid-template-columns:240px minmax(0,1fr)}.chat-page{padding:12px}.conversation-head{padding:0 10px}.message-bubble{max-width:86%}}
.takeover-note{flex-shrink:0}.system-message{max-width:100%;overflow-wrap:anywhere;white-space:normal}.new-messages{align-self:center;flex-shrink:0;border:1px solid #c6e1f5;background:#edf7ff;border-radius:18px;color:#168fce;font-size:11px;padding:6px 14px;margin:5px;cursor:pointer}.detail-status,.detail-error{font-size:12px;color:#7e95a6;padding:12px;text-align:center}.detail-error{color:#bf5963}.detail-error button{border:0;background:none;color:#168fce}.list-scroll:focus-visible,.messages:focus-visible{outline:2px solid #a3d2ef;outline-offset:-2px}.conversation:disabled{cursor:wait;opacity:.65}
@media(max-width:650px){.chat-shell{grid-template-columns:minmax(0,1fr);grid-template-rows:minmax(100px,34%) minmax(0,1fr)}.conversation-list{border-right:0;border-bottom:1px solid var(--ss-border)}.chat-title h1{font-size:17px}.chat-title p{display:none}.chat-title .live-pill{font-size:10px;padding:6px 8px}.conversation-head{height:56px;flex-basis:56px}.messages{padding:12px}.reply-box{height:56px;flex-basis:56px}.reply-box input{font-size:11px}.message-bubble{max-width:95%}.search-box{margin:8px 10px 6px}.tabs{margin:0 10px 6px}.search-box input{height:32px}.tabs button{height:27px}}
</style>
