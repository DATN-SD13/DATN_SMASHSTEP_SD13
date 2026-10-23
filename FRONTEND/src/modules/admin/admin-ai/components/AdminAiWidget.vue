<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminAiService } from '../services/adminAiService'

const router = useRouter()
const open = ref(false)
const minimized = ref(false)
const loading = ref(false)
const input = ref('')
const messages = ref([])
const bodyRef = ref(null)

const STORAGE_KEY = 'smashstep_admin_ai_history_v1'
const initialSuggestions = [
  'Doanh thu hôm nay',
  'Sản phẩm bán chạy',
  'Sản phẩm đang hot',
  'Sản phẩm sắp hết hàng',
  'Đơn cần xử lý',
  'Nhân viên hiệu quả nhất'
]

const latestSuggestions = computed(() => {
  const last = [...messages.value].reverse().find(item => item.role === 'assistant' && item.data?.suggestions?.length)
  return last?.data?.suggestions || initialSuggestions
})

function persist() {
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(messages.value.slice(-24)))
  } catch {}
}

function restore() {
  try {
    const saved = JSON.parse(sessionStorage.getItem(STORAGE_KEY) || '[]')
    if (Array.isArray(saved)) messages.value = saved
  } catch {}
}

function resetConversation() {
  messages.value = []
  sessionStorage.removeItem(STORAGE_KEY)
  welcome()
}

async function scrollBottom() {
  await nextTick()
  if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight
}

async function welcome() {
  if (messages.value.length) return
  messages.value.push({
    id: crypto.randomUUID(),
    role: 'assistant',
    text: 'Xin chào quản trị viên. Tôi có thể tổng hợp nhanh dữ liệu vận hành của SmashStep và đưa ra các điểm cần chú ý.',
    data: { suggestions: initialSuggestions },
    createdAt: new Date().toISOString()
  })
  persist()
}

async function ask(value = input.value) {
  const message = String(value || '').trim()
  if (!message || loading.value) return

  messages.value.push({
    id: crypto.randomUUID(),
    role: 'user',
    text: message,
    createdAt: new Date().toISOString()
  })
  input.value = ''
  loading.value = true
  persist()
  await scrollBottom()

  try {
    const data = await adminAiService.ask(message)
    messages.value.push({
      id: crypto.randomUUID(),
      role: 'assistant',
      text: data.content,
      data,
      createdAt: data.generatedAt || new Date().toISOString()
    })
  } catch (error) {
    messages.value.push({
      id: crypto.randomUUID(),
      role: 'assistant',
      text: error.response?.data?.message || error.message || 'Không thể truy vấn dữ liệu quản trị lúc này.',
      error: true,
      createdAt: new Date().toISOString()
    })
  } finally {
    loading.value = false
    persist()
    await scrollBottom()
  }
}

function openProduct(product) {
  if (!product?.id) return
  router.push(`/san-pham/${product.id}`)
  open.value = false
}

function openEmployee(employee) {
  if (!employee?.id) return
  router.push('/nhan-vien')
  open.value = false
}

function time(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

function money(value) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }).format(Number(value || 0))
}

function imageUrl(url) {
  if (!url) return null
  if (/^https?:\/\//i.test(url)) return url
  const base = String(import.meta.env.VITE_API_URL || '/api').replace(/\/api\/?$/, '')
  return `${base}${url.startsWith('/') ? '' : '/'}${url}`
}

function keydown(event) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    ask()
  }
}

onMounted(() => {
  restore()
  welcome()
})

onBeforeUnmount(persist)
</script>

<template>
  <Teleport to="body">
    <button v-if="!open" type="button" class="admin-ai-launch" @click="open = true; minimized = false">
      <span class="launch-mark">AI</span>
      <span>Trợ lý quản trị</span>
    </button>

    <section v-else class="admin-ai-shell" :class="{ minimized }" aria-label="Trợ lý quản trị SmashStep">
      <header class="admin-ai-header">
        <div>
          <strong>Trợ lý quản trị</strong>
          <small>Phân tích dữ liệu SmashStep</small>
        </div>
        <div class="header-actions">
          <button type="button" title="Làm mới cuộc trò chuyện" @click="resetConversation">Làm mới</button>
          <button type="button" title="Thu gọn" @click="minimized = !minimized">{{ minimized ? 'Mở' : 'Thu gọn' }}</button>
          <button type="button" title="Đóng" @click="open = false">Đóng</button>
        </div>
      </header>

      <template v-if="!minimized">
        <div ref="bodyRef" class="admin-ai-body">
          <article v-for="message in messages" :key="message.id" class="ai-message" :class="[message.role, { error: message.error }]">
            <div class="message-meta">
              <span>{{ message.role === 'assistant' ? 'SmashStep AI' : 'Bạn' }}</span>
              <time>{{ time(message.createdAt) }}</time>
            </div>
            <div class="message-bubble">{{ message.text }}</div>

            <div v-if="message.data?.metrics?.length" class="metric-grid">
              <div v-for="metric in message.data.metrics" :key="metric.key" class="metric-card" :class="metric.tone">
                <span>{{ metric.label }}</span>
                <strong>{{ metric.value }}</strong>
                <small>{{ metric.helper }}</small>
              </div>
            </div>

            <div v-if="message.data?.products?.length" class="insight-list">
              <button v-for="product in message.data.products" :key="product.id" type="button" class="product-card" @click="openProduct(product)">
                <div class="product-image">
                  <img v-if="imageUrl(product.imageUrl)" :src="imageUrl(product.imageUrl)" :alt="product.name" />
                  <span v-else>Không có ảnh</span>
                </div>
                <div class="product-copy">
                  <small>{{ product.code }}</small>
                  <strong>{{ product.name }}</strong>
                  <div class="product-stats">
                    <span v-if="product.sold > 0">Đã bán <b>{{ product.sold }}</b></span>
                    <span v-if="Number(product.revenue) > 0">Doanh thu <b>{{ money(product.revenue) }}</b></span>
                    <span>Tồn <b>{{ product.stock }}</b></span>
                  </div>
                  <p v-if="product.note">{{ product.note }}</p>
                  <span class="detail-link">Xem chi tiết sản phẩm</span>
                </div>
              </button>
            </div>

            <div v-if="message.data?.employees?.length" class="employee-list">
              <button v-for="(employee, index) in message.data.employees" :key="employee.id" type="button" class="employee-row" @click="openEmployee(employee)">
                <span class="rank">{{ index + 1 }}</span>
                <span class="employee-main"><strong>{{ employee.name }}</strong><small>{{ employee.code }}</small></span>
                <span class="employee-number"><strong>{{ money(employee.revenue) }}</strong><small>{{ employee.orders }} đơn · {{ employee.productsSold }} SP</small></span>
              </button>
            </div>

            <div v-if="message.data?.orderStatuses?.length" class="status-list">
              <div v-for="status in message.data.orderStatuses" :key="status.status" class="status-row">
                <span>{{ status.label }}</span>
                <strong>{{ status.count }}</strong>
              </div>
            </div>
          </article>

          <div v-if="loading" class="typing-row">
            <span></span><span></span><span></span>
            <small>Đang phân tích dữ liệu</small>
          </div>
        </div>

        <div class="suggestions">
          <button v-for="item in latestSuggestions" :key="item" type="button" :disabled="loading" @click="ask(item)">{{ item }}</button>
        </div>

        <form class="admin-ai-compose" @submit.prevent="ask()">
          <textarea v-model="input" rows="1" maxlength="500" placeholder="Hỏi về doanh thu, sản phẩm, tồn kho, đơn hàng..." :disabled="loading" @keydown="keydown"></textarea>
          <button type="submit" :disabled="loading || !input.trim()">Gửi</button>
        </form>
      </template>
    </section>
  </Teleport>
</template>

<style scoped>
.admin-ai-launch {
  position: fixed; right: 24px; bottom: 24px; z-index: 1080;
  display: flex; align-items: center; gap: 10px; border: 0; border-radius: 16px;
  padding: 11px 16px 11px 10px; background: var(--ss-primary); color: #fff;
  font-weight: 700; box-shadow: 0 16px 40px rgba(15, 48, 78, .22); cursor: pointer;
}
.launch-mark { display:grid; place-items:center; width:32px; height:32px; border-radius:10px; background:rgba(255,255,255,.16); font-size:12px; letter-spacing:.04em; }
.admin-ai-shell {
  position: fixed; right: 24px; bottom: 24px; z-index: 1080; width: min(430px, calc(100vw - 32px)); height: min(720px, calc(100vh - 48px));
  display: flex; flex-direction: column; overflow: hidden; border: 1px solid var(--ss-border); border-radius: 22px;
  background: #fff; box-shadow: 0 24px 70px rgba(15, 48, 78, .22);
}
.admin-ai-shell.minimized { height: auto; }
.admin-ai-header { display:flex; justify-content:space-between; align-items:center; gap:12px; padding:16px 17px; background:linear-gradient(135deg,var(--ss-primary),#174f7c); color:#fff; }
.admin-ai-header strong { display:block; font-size:15px; }
.admin-ai-header small { display:block; margin-top:2px; opacity:.78; font-size:11px; }
.header-actions { display:flex; gap:4px; }
.header-actions button { border:0; background:transparent; color:#fff; opacity:.8; padding:6px; font-size:11px; cursor:pointer; }
.header-actions button:hover { opacity:1; text-decoration:underline; }
.admin-ai-body { flex:1; overflow-y:auto; padding:18px 16px; background:#f6f9fc; scroll-behavior:smooth; }
.ai-message { margin-bottom:18px; }
.message-meta { display:flex; align-items:center; gap:7px; margin:0 4px 6px; color:#728195; font-size:10px; }
.message-meta time { margin-left:auto; }
.user .message-meta { justify-content:flex-end; }
.user .message-meta time { margin-left:0; }
.message-bubble { width:fit-content; max-width:92%; padding:11px 13px; border:1px solid #dfe7ef; border-radius:14px; background:#fff; color:#243444; font-size:13px; line-height:1.55; white-space:pre-line; }
.user .message-bubble { margin-left:auto; border-color:var(--ss-primary); background:var(--ss-primary); color:#fff; border-bottom-right-radius:5px; }
.assistant .message-bubble { border-bottom-left-radius:5px; }
.error .message-bubble { border-color:#efc7c7; background:#fff7f7; color:#9c3030; }
.metric-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:8px; margin-top:10px; }
.metric-card { min-width:0; padding:11px; border:1px solid #dfe7ef; border-radius:13px; background:#fff; }
.metric-card span,.metric-card small { display:block; color:#728195; font-size:10px; line-height:1.4; }
.metric-card strong { display:block; margin:4px 0; color:#172a3d; font-size:16px; }
.metric-card.positive strong { color:#147a55; }
.metric-card.negative strong { color:#b24444; }
.metric-card.warning strong { color:#aa6b12; }
.insight-list { display:grid; gap:8px; margin-top:10px; }
.product-card { display:grid; grid-template-columns:82px 1fr; gap:11px; width:100%; padding:9px; text-align:left; border:1px solid #dfe7ef; border-radius:14px; background:#fff; cursor:pointer; transition:.18s ease; }
.product-card:hover { transform:translateY(-1px); border-color:#a9bfd3; box-shadow:0 8px 20px rgba(31,64,91,.08); }
.product-image { display:grid; place-items:center; height:78px; overflow:hidden; border-radius:10px; background:#f2f5f8; color:#91a0af; font-size:9px; }
.product-image img { width:100%; height:100%; object-fit:contain; }
.product-copy small { display:block; color:#8391a0; font-size:9px; }
.product-copy strong { display:block; margin:2px 0 6px; color:#172a3d; font-size:12px; }
.product-stats { display:flex; flex-wrap:wrap; gap:5px 9px; color:#667789; font-size:9px; }
.product-stats b { color:#243444; }
.product-copy p { margin:6px 0 0; color:#5e7081; font-size:9px; }
.detail-link { display:inline-block; margin-top:6px; color:var(--ss-primary); font-size:9px; font-weight:700; }
.employee-list,.status-list { display:grid; gap:7px; margin-top:10px; }
.employee-row { display:grid; grid-template-columns:28px 1fr auto; align-items:center; gap:9px; width:100%; padding:10px; border:1px solid #dfe7ef; border-radius:12px; background:#fff; text-align:left; cursor:pointer; }
.rank { display:grid; place-items:center; width:26px; height:26px; border-radius:8px; background:var(--ss-primary-soft); color:var(--ss-primary); font-weight:800; font-size:11px; }
.employee-main strong,.employee-main small,.employee-number strong,.employee-number small { display:block; }
.employee-main strong { color:#233547; font-size:11px; }
.employee-main small,.employee-number small { color:#7c8996; font-size:9px; }
.employee-number { text-align:right; }
.employee-number strong { color:#233547; font-size:10px; }
.status-row { display:flex; justify-content:space-between; align-items:center; padding:10px 12px; border:1px solid #dfe7ef; border-radius:11px; background:#fff; color:#435568; font-size:11px; }
.status-row strong { min-width:30px; text-align:center; color:var(--ss-primary); font-size:13px; }
.typing-row { display:flex; align-items:center; gap:4px; color:#788898; }
.typing-row span { width:5px; height:5px; border-radius:50%; background:#8ca0b3; animation:pulse 1s infinite alternate; }
.typing-row span:nth-child(2){animation-delay:.15s}.typing-row span:nth-child(3){animation-delay:.3s}
.typing-row small { margin-left:5px; font-size:10px; }
.suggestions { display:flex; gap:6px; overflow-x:auto; padding:9px 12px; border-top:1px solid #e4ebf1; background:#fff; scrollbar-width:none; }
.suggestions::-webkit-scrollbar { display:none; }
.suggestions button { flex:0 0 auto; border:1px solid #d7e1ea; border-radius:999px; background:#f8fafc; color:#40566b; padding:7px 10px; font-size:10px; cursor:pointer; }
.suggestions button:hover { border-color:var(--ss-primary); color:var(--ss-primary); background:var(--ss-primary-soft); }
.admin-ai-compose { display:grid; grid-template-columns:1fr auto; gap:8px; padding:11px 12px 12px; border-top:1px solid #e4ebf1; background:#fff; }
.admin-ai-compose textarea { resize:none; min-height:39px; max-height:90px; padding:10px 12px; border:1px solid #d7e1ea; border-radius:12px; outline:none; color:#243444; font:inherit; font-size:12px; }
.admin-ai-compose textarea:focus { border-color:var(--ss-primary); box-shadow:0 0 0 3px var(--ss-primary-soft); }
.admin-ai-compose button { min-width:58px; border:0; border-radius:11px; background:var(--ss-primary); color:#fff; font-weight:700; cursor:pointer; }
.admin-ai-compose button:disabled,.suggestions button:disabled { opacity:.55; cursor:not-allowed; }
@keyframes pulse { from{opacity:.25; transform:translateY(0)} to{opacity:1; transform:translateY(-2px)} }
@media (max-width: 600px) {
  .admin-ai-shell { right:8px; bottom:8px; width:calc(100vw - 16px); height:calc(100vh - 16px); border-radius:17px; }
  .admin-ai-launch { right:14px; bottom:14px; }
}
</style>
