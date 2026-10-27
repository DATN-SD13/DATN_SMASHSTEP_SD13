<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { chatService } from '../services/chatService'
import { money } from '../utils/money'
import { imageUrl } from '../utils/image'

const auth = useAuthStore()
const router = useRouter()

const open = ref(false)
const loading = ref(false)
const sending = ref(false)
const detail = ref(null)
const text = ref('')
const error = ref('')
const messageBox = ref(null)
const failedImages = ref(new Set())
let timer = null

const session = computed(() => detail.value?.session || null)
const messages = computed(() => detail.value?.messages || [])
const status = computed(() => session.value?.status || '')
const canType = computed(() => ['AI_ACTIVE', 'WAITING_STAFF', 'STAFF_ACTIVE'].includes(status.value))

const headerSub = computed(() => ({
  AI_ACTIVE: 'Tư vấn sản phẩm và mua sắm',
  WAITING_STAFF: 'Đang chờ nhân viên tiếp nhận',
  STAFF_ACTIVE: session.value?.employeeName
    ? `Đang hỗ trợ bởi ${session.value.employeeName}`
    : 'Nhân viên đang hỗ trợ',
  CLOSED: 'Phiên tư vấn đã kết thúc'
}[status.value] || 'Hỗ trợ khách hàng'))

const latestAiMessage = computed(() => {
  for (let i = messages.value.length - 1; i >= 0; i--) {
    if (messages.value[i]?.senderType === 'AI') return messages.value[i]
  }
  return null
})

const latestSuggestions = computed(() => {
  if (status.value !== 'AI_ACTIVE') return []
  const values = latestAiMessage.value?.suggestions
  return Array.isArray(values) ? values.slice(0, 4) : []
})

function errorText(e) {
  return e?.response?.data?.message || e?.message || 'Không thể kết nối hệ thống tư vấn.'
}

async function scrollBottom() {
  await nextTick()
  if (messageBox.value) messageBox.value.scrollTop = messageBox.value.scrollHeight
}

async function load(silent = false) {
  auth.syncSession()
  if (!auth.isAuthenticated || !auth.isCustomer) return
  if (!silent) loading.value = true
  try {
    const oldLast = messages.value.at(-1)?.id
    detail.value = await chatService.current()
    error.value = ''
    if (oldLast !== messages.value.at(-1)?.id) await scrollBottom()
  } catch (e) {
    if (!silent) error.value = errorText(e)
  } finally {
    if (!silent) loading.value = false
  }
}

async function toggle() {
  open.value = !open.value
  if (!open.value) {
    stopPolling()
    return
  }
  if (auth.isAuthenticated && auth.isCustomer) {
    await load()
    startPolling()
  }
}

function startPolling() {
  stopPolling()
  timer = window.setInterval(() => open.value && load(true), 1500)
}

function stopPolling() {
  if (timer) window.clearInterval(timer)
  timer = null
}

async function send(contentOverride = null) {
  const content = String(contentOverride ?? text.value).trim()
  if (!content || sending.value || !canType.value) return

  sending.value = true
  try {
    detail.value = await chatService.send(content)
    text.value = ''
    error.value = ''
    await scrollBottom()
  } catch (e) {
    error.value = errorText(e)
  } finally {
    sending.value = false
  }
}

async function handleSuggestion(value) {
  if (!value || sending.value) return
  if (value === 'Gặp nhân viên') {
    await requestStaff()
    return
  }
  await send(value)
}

async function requestStaff() {
  if (sending.value) return
  sending.value = true
  try {
    detail.value = await chatService.requestStaff()
    error.value = ''
    await scrollBottom()
  } catch (e) {
    error.value = errorText(e)
  } finally {
    sending.value = false
  }
}

async function startNew() {
  loading.value = true
  try {
    detail.value = await chatService.startNew()
    error.value = ''
    failedImages.value = new Set()
    await scrollBottom()
  } catch (e) {
    error.value = errorText(e)
  } finally {
    loading.value = false
  }
}

function login() {
  router.push({ path: '/dang-nhap', query: { redirect: router.currentRoute.value.fullPath } })
  open.value = false
}

function formatTime(value) {
  if (!value) return ''
  return new Date(value).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })
}

function productImage(product) {
  if (!product?.id || failedImages.value.has(product.id)) return ''
  return imageUrl(product.imageUrl)
}

function markImageFailed(id) {
  const next = new Set(failedImages.value)
  next.add(id)
  failedImages.value = next
}

function discountText(value) {
  const n = Number(value)
  if (!Number.isFinite(n) || n <= 0) return ''
  return `${Number(n.toFixed(0))}%`
}

function visibleSizes(product) {
  return Array.isArray(product?.availableSizes) ? product.availableSizes.slice(0, 5) : []
}

function hideAfterNavigate() {
  open.value = false
  stopPolling()
}

watch(() => auth.isAuthenticated, value => {
  if (!value) {
    detail.value = null
    stopPolling()
  }
})

onBeforeUnmount(stopPolling)
</script>

<template>
  <div class="ss-chat-widget">
    <transition name="chat-pop">
      <section v-if="open" class="chat-panel" aria-label="Tư vấn SmashStep">
        <header class="chat-head">
          <div class="chat-title-wrap">
            <div class="chat-title-line">
              <strong>Tư vấn SmashStep</strong>
              <span class="status-dot" aria-hidden="true"></span>
            </div>
            <span class="chat-subtitle">{{ headerSub }}</span>
          </div>
          <button class="head-action" type="button" @click="toggle">Thu gọn</button>
        </header>

        <div v-if="!auth.isAuthenticated || !auth.isCustomer" class="login-state">
          <p class="eyebrow">SMASHSTEP SUPPORT</p>
          <h3>Tư vấn mua sắm theo nhu cầu của bạn</h3>
          <p>Đăng nhập để nhận tư vấn sản phẩm, kiểm tra giá và khuyến mãi, chọn size hoặc trò chuyện trực tiếp với nhân viên.</p>
          <button type="button" @click="login">Đăng nhập để bắt đầu</button>
        </div>

        <template v-else>
          <div ref="messageBox" class="chat-messages">
            <div v-if="loading" class="chat-loading">
              <span class="loading-line"></span>
              <span class="loading-line short"></span>
            </div>

            <template v-else>
              <div
                v-for="m in messages"
                :key="m.id"
                class="message-row"
                :class="{
                  mine: m.senderType === 'CUSTOMER',
                  system: m.senderType === 'SYSTEM'
                }"
              >
                <div v-if="m.senderType !== 'SYSTEM'" class="sender">
                  <span>{{ m.senderType === 'CUSTOMER' ? 'Bạn' : (m.senderName || 'SmashStep') }}</span>
                  <time>{{ formatTime(m.createdAt) }}</time>
                </div>

                <div class="bubble">{{ m.content }}</div>

                <div v-if="Array.isArray(m.products) && m.products.length" class="product-strip">
                  <RouterLink
                    v-for="product in m.products"
                    :key="product.id"
                    :to="{ name: 'product-detail', params: { id: product.id } }"
                    class="chat-product-card"
                    @click="hideAfterNavigate"
                  >
                    <div class="product-media">
                      <img
                        v-if="productImage(product)"
                        :src="productImage(product)"
                        :alt="product.name"
                        loading="lazy"
                        @error="markImageFailed(product.id)"
                      >
                      <div v-else class="product-fallback">SMASHSTEP</div>

                      <span v-if="product.discounted" class="sale-label">
                        -{{ discountText(product.discountPercent) }}
                      </span>
                    </div>

                    <div class="product-info">
                      <p v-if="product.brand" class="product-brand">{{ product.brand }}</p>
                      <h4>{{ product.name }}</h4>

                      <div class="product-price-row">
                        <strong>{{ money(product.salePrice) }}</strong>
                        <del v-if="product.discounted">{{ money(product.originalPrice) }}</del>
                      </div>

                      <div class="product-meta">
                        <span v-if="product.soldCount">Đã bán {{ product.soldCount }}</span>
                        <span>{{ product.stock > 0 ? `Còn ${product.stock} sản phẩm` : 'Tạm hết hàng' }}</span>
                      </div>

                      <div v-if="visibleSizes(product).length" class="size-row">
                        <span class="size-label">Size</span>
                        <span v-for="size in visibleSizes(product)" :key="size" class="size-value">{{ size }}</span>
                        <span v-if="product.availableSizes.length > 5" class="size-more">+{{ product.availableSizes.length - 5 }}</span>
                      </div>

                      <div class="product-link">Xem thông tin sản phẩm</div>
                    </div>
                  </RouterLink>
                </div>
              </div>
            </template>
          </div>

          <div v-if="error" class="chat-error">{{ error }}</div>

          <div v-if="latestSuggestions.length" class="suggestion-area">
            <button
              v-for="suggestion in latestSuggestions"
              :key="suggestion"
              type="button"
              :disabled="sending"
              @click="handleSuggestion(suggestion)"
            >
              {{ suggestion }}
            </button>
          </div>

          <div v-else-if="status === 'WAITING_STAFF'" class="state-note">
            <strong>Đã chuyển yêu cầu</strong>
            <span>Bạn có thể tiếp tục gửi thông tin trong khi chờ nhân viên tiếp nhận.</span>
          </div>

          <div v-else-if="status === 'CLOSED'" class="closed-note">
            <div>
              <strong>Phiên tư vấn đã kết thúc</strong>
              <span>Bạn có thể bắt đầu một cuộc trò chuyện mới khi cần.</span>
            </div>
            <button type="button" @click="startNew">Bắt đầu phiên mới</button>
          </div>

          <form v-if="status !== 'CLOSED'" class="chat-compose" @submit.prevent="send()">
            <textarea
              v-model="text"
              maxlength="2000"
              rows="1"
              :disabled="!canType || sending"
              placeholder="Nhập câu hỏi về sản phẩm, size hoặc khuyến mãi..."
              @keydown.enter.exact.prevent="send()"
            ></textarea>
            <button type="submit" :disabled="!text.trim() || sending">Gửi</button>
          </form>
        </template>
      </section>
    </transition>

    <button
      class="chat-fab"
      type="button"
      :class="{ opened: open }"
      :aria-label="open ? 'Đóng tư vấn' : 'Mở tư vấn'"
      @click="toggle"
    >
      {{ open ? 'Đóng' : 'Tư vấn' }}
    </button>
  </div>
</template>

<style scoped>
.ss-chat-widget {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 1200;
  font-family: var(--customer-font);
}

.chat-fab {
  min-width: 86px;
  height: 46px;
  padding: 0 20px;
  border: 0;
  border-radius: 999px;
  background: var(--customer-primary);
  color: #fff;
  font: inherit;
  font-size: 13px;
  font-weight: 750;
  letter-spacing: .01em;
  box-shadow: 0 12px 30px rgba(40, 121, 227, .24);
  cursor: pointer;
  transition: transform .18s ease, background .18s ease, box-shadow .18s ease;
}

.chat-fab:hover {
  transform: translateY(-2px);
  background: var(--customer-primary-hover);
  box-shadow: 0 15px 34px rgba(40, 121, 227, .30);
}

.chat-fab.opened {
  min-width: 76px;
  background: #172033;
  box-shadow: 0 12px 28px rgba(15, 23, 42, .18);
}

.chat-panel {
  position: absolute;
  right: 0;
  bottom: 60px;
  width: min(440px, calc(100vw - 32px));
  height: 650px;
  max-height: calc(100vh - 104px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  border: 1px solid rgba(15, 23, 42, .09);
  border-radius: 22px;
  box-shadow: 0 28px 80px rgba(15, 23, 42, .20);
}

.chat-head {
  min-height: 76px;
  padding: 17px 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  background: #fff;
  border-bottom: 1px solid #e8eef6;
}

.chat-title-wrap {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.chat-title-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-title-line strong {
  color: #12233e;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: -.01em;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #24a66a;
  box-shadow: 0 0 0 4px rgba(36, 166, 106, .10);
}

.chat-subtitle {
  color: #70819a;
  font-size: 11px;
  line-height: 1.35;
}

.head-action {
  border: 0;
  background: transparent;
  color: #65748b;
  font: inherit;
  font-size: 11px;
  font-weight: 700;
  cursor: pointer;
}

.head-action:hover { color: var(--customer-primary); }

.chat-messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 16px 18px;
  background:
    linear-gradient(180deg, #fbfdff 0%, #f8fbff 55%, #fff 100%);
  scroll-behavior: smooth;
}

.chat-messages::-webkit-scrollbar { width: 7px; }
.chat-messages::-webkit-scrollbar-thumb { background: #d7e0eb; border-radius: 999px; }

.message-row {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 18px;
}

.message-row.mine { align-items: flex-end; }
.message-row.system { align-items: center; margin: 12px 0 18px; }

.sender {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 0 7px 5px;
  color: #556781;
  font-size: 10px;
}

.sender > span { font-weight: 750; }
.sender time { color: #9aa8b9; font-size: 9px; }

.bubble {
  max-width: 84%;
  padding: 11px 13px;
  border: 1px solid #e1e8f0;
  border-radius: 5px 15px 15px 15px;
  background: #fff;
  color: #27364d;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  box-shadow: 0 3px 12px rgba(15, 23, 42, .035);
}

.mine .bubble {
  border-color: var(--customer-primary);
  border-radius: 15px 5px 15px 15px;
  background: var(--customer-primary);
  color: #fff;
  box-shadow: 0 6px 18px rgba(40, 121, 227, .15);
}

.system .bubble {
  max-width: 94%;
  padding: 8px 11px;
  border: 0;
  border-radius: 10px;
  background: #edf4fc;
  color: #60748f;
  font-size: 10px;
  text-align: center;
  box-shadow: none;
}

.product-strip {
  width: min(100%, 392px);
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: 232px;
  gap: 10px;
  margin-top: 10px;
  padding-bottom: 4px;
  overflow-x: auto;
  scroll-snap-type: x proximity;
}

.product-strip::-webkit-scrollbar { height: 5px; }
.product-strip::-webkit-scrollbar-thumb { background: #d8e1ec; border-radius: 999px; }

.chat-product-card {
  scroll-snap-align: start;
  display: block;
  overflow: hidden;
  border: 1px solid #e0e7ef;
  border-radius: 14px;
  background: #fff;
  color: inherit;
  text-decoration: none;
  box-shadow: 0 8px 22px rgba(15, 23, 42, .055);
  transition: transform .16s ease, border-color .16s ease, box-shadow .16s ease;
}

.chat-product-card:hover {
  transform: translateY(-2px);
  border-color: #b8d3f7;
  box-shadow: 0 12px 28px rgba(15, 23, 42, .09);
}

.product-media {
  position: relative;
  height: 138px;
  display: grid;
  place-items: center;
  overflow: hidden;
  background: #f6f8fb;
}

.product-media img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: transform .22s ease;
}

.chat-product-card:hover .product-media img { transform: scale(1.035); }

.product-fallback {
  color: #9ba8b8;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: .12em;
}

.sale-label {
  position: absolute;
  top: 9px;
  left: 9px;
  padding: 4px 7px;
  border-radius: 7px;
  background: #e9f2ff;
  color: var(--customer-primary);
  font-size: 10px;
  font-weight: 800;
}

.product-info { padding: 12px; }
.product-brand {
  margin: 0 0 4px;
  color: #8b99aa;
  font-size: 9px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: .08em;
}

.product-info h4 {
  min-height: 35px;
  margin: 0;
  color: #15243a;
  font-size: 12px;
  line-height: 1.45;
  font-weight: 800;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-price-row {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.product-price-row strong { color: var(--customer-primary); font-size: 13px; }
.product-price-row del { color: #9ba7b6; font-size: 9px; }

.product-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  color: #718198;
  font-size: 9px;
}

.size-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 9px;
}

.size-label { margin-right: 2px; color: #8390a3; font-size: 9px; }
.size-value, .size-more {
  min-width: 24px;
  height: 22px;
  padding: 0 5px;
  display: inline-grid;
  place-items: center;
  border: 1px solid #e0e7ef;
  border-radius: 6px;
  color: #4c5d73;
  font-size: 9px;
  background: #fbfcfe;
}

.product-link {
  margin-top: 11px;
  padding-top: 9px;
  border-top: 1px solid #edf1f5;
  color: var(--customer-primary);
  font-size: 10px;
  font-weight: 800;
}

.suggestion-area {
  flex: 0 0 auto;
  display: flex;
  gap: 7px;
  overflow-x: auto;
  padding: 10px 13px;
  border-top: 1px solid #e8eef5;
  background: #fff;
}

.suggestion-area::-webkit-scrollbar { display: none; }

.suggestion-area button {
  flex: 0 0 auto;
  height: 32px;
  padding: 0 11px;
  border: 1px solid #d9e3ee;
  border-radius: 999px;
  background: #fff;
  color: #465970;
  font: inherit;
  font-size: 10px;
  font-weight: 700;
  white-space: nowrap;
  cursor: pointer;
  transition: .15s ease;
}

.suggestion-area button:hover {
  border-color: #a9c9f3;
  background: #f5f9ff;
  color: var(--customer-primary);
}

.state-note,
.closed-note {
  flex: 0 0 auto;
  padding: 11px 14px;
  border-top: 1px solid #e7edf4;
  background: #f8fbff;
}

.state-note {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.state-note strong,
.closed-note strong { color: #2b3d55; font-size: 10px; }
.state-note span,
.closed-note span { color: #7b899a; font-size: 9px; line-height: 1.45; }

.closed-note {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.closed-note > div { display: flex; flex-direction: column; gap: 3px; }
.closed-note button {
  flex: 0 0 auto;
  height: 32px;
  padding: 0 11px;
  border: 1px solid var(--customer-primary);
  border-radius: 8px;
  background: #fff;
  color: var(--customer-primary);
  font: inherit;
  font-size: 10px;
  font-weight: 750;
  cursor: pointer;
}

.chat-compose {
  flex: 0 0 auto;
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 10px 12px 12px;
  border-top: 1px solid #e7edf4;
  background: #fff;
}

.chat-compose textarea {
  flex: 1;
  min-width: 0;
  min-height: 42px;
  max-height: 96px;
  resize: none;
  padding: 11px 12px;
  border: 1px solid #dce5ef;
  border-radius: 12px;
  outline: none;
  background: #f8fafc;
  color: #26374f;
  font: inherit;
  font-size: 11px;
  line-height: 1.45;
}

.chat-compose textarea:focus {
  border-color: #9fc4f4;
  background: #fff;
  box-shadow: 0 0 0 3px rgba(40, 121, 227, .07);
}

.chat-compose button {
  height: 42px;
  padding: 0 16px;
  border: 0;
  border-radius: 10px;
  background: var(--customer-primary);
  color: #fff;
  font: inherit;
  font-size: 11px;
  font-weight: 800;
  cursor: pointer;
}

.chat-compose button:disabled { opacity: .38; cursor: not-allowed; }
.chat-error { padding: 8px 13px; background: #fff3f3; color: #b4232a; font-size: 10px; }

.login-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding: 34px;
  background: linear-gradient(160deg, #fff, #f6faff);
}

.login-state .eyebrow {
  margin: 0 0 10px;
  color: var(--customer-primary);
  font-size: 9px;
  font-weight: 850;
  letter-spacing: .13em;
}

.login-state h3 {
  margin: 0;
  color: #15243a;
  font-size: 22px;
  line-height: 1.25;
  letter-spacing: -.025em;
}

.login-state > p:not(.eyebrow) {
  margin: 13px 0 0;
  color: #738197;
  font-size: 11px;
  line-height: 1.7;
}

.login-state button {
  margin-top: 22px;
  height: 40px;
  padding: 0 15px;
  border: 0;
  border-radius: 9px;
  background: var(--customer-primary);
  color: #fff;
  font: inherit;
  font-size: 11px;
  font-weight: 800;
  cursor: pointer;
}

.chat-loading { padding: 14px 2px 24px; }
.loading-line {
  display: block;
  width: 75%;
  height: 12px;
  margin: 7px 0;
  border-radius: 999px;
  background: linear-gradient(90deg, #eef2f6, #f8fafc, #eef2f6);
  background-size: 200% 100%;
  animation: loading 1.2s linear infinite;
}
.loading-line.short { width: 52%; }

.chat-pop-enter-active,
.chat-pop-leave-active { transition: .2s ease; }
.chat-pop-enter-from,
.chat-pop-leave-to { opacity: 0; transform: translateY(10px) scale(.985); }

@keyframes loading { to { background-position: -200% 0; } }

@media (max-width: 600px) {
  .ss-chat-widget { right: 12px; bottom: 12px; }
  .chat-panel {
    position: fixed;
    left: 10px;
    right: 10px;
    bottom: 70px;
    width: auto;
    height: calc(100vh - 86px);
    max-height: 720px;
    border-radius: 18px;
  }
  .chat-fab { height: 42px; min-width: 76px; }
}
</style>
