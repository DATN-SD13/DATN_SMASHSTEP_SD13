<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useAuthStore } from '../stores/auth'
import { commerceService, commerceError } from '../services/commerceService'
import { useLiveRefresh } from '../composables/useLiveRefresh'
import BaseIcon from './BaseIcon.vue'
const auth = useAuthStore(), root = ref(null), open = ref(false), notices = ref([]), read = ref([]), error = ref(''), loading = ref(false)
let version = 0, disposed = false
const key = computed(() => 'ss_notice_read_' + auth.user?.id)
const unread = computed(() => notices.value.filter(n => !read.value.includes(n.id)).length)
async function load() {
  if (!auth.isCustomer || !auth.isAuthenticated || loading.value) return
  const current = ++version; loading.value = true
  try { const rows = await commerceService.notifications(); if (!disposed && current === version) { notices.value = rows; error.value = '' } }
  catch (e) { if (current === version) error.value = commerceError(e) }
  finally { loading.value = false }
}
function seen(id) { read.value = [...new Set([...read.value, id])].slice(-100); try { localStorage.setItem(key.value, JSON.stringify(read.value)) } catch { /* Read markers are optional local state. */ } open.value = false }
function outside(e) { if (!root.value?.contains(e.target)) open.value = false }
function toggle() { open.value = !open.value; if (open.value) load() }
watch(() => auth.user?.id, () => { version++; notices.value = []; read.value = []; error.value = ''; try { const value = JSON.parse(localStorage.getItem(key.value) || '[]'); if (Array.isArray(value)) read.value = value.filter(v => typeof v === 'string').slice(-100) } catch { /* Invalid local markers are ignored. */ } load() }, { immediate: true })
onMounted(() => document.addEventListener('pointerdown', outside))
onBeforeUnmount(() => { disposed = true; version++; document.removeEventListener('pointerdown', outside) })
useLiveRefresh(load, 60000)
</script>
<template><div ref="root" class="notification-wrap" @keydown.esc="open = false"><button class="icon-button header-action" type="button" aria-label="Thông báo" :aria-expanded="open" aria-controls="customer-notifications" @click="toggle"><BaseIcon name="bell" /><span v-if="unread" class="header-badge">{{ unread > 99 ? '99+' : unread }}</span></button><section v-if="open" id="customer-notifications" class="notification-panel" aria-label="Thông báo"><header><h2>Thông báo</h2><span>Đơn hàng & ưu đãi của bạn</span></header><div v-if="!auth.isCustomer" class="notification-empty"><p>Đăng nhập để theo dõi đơn hàng và ưu đãi của bạn.</p><RouterLink to="/dang-nhap" class="button button-primary" @click="open = false">Đăng nhập</RouterLink></div><p v-else-if="loading && !notices.length" class="notification-empty" role="status">Đang tải thông báo…</p><p v-else-if="error" class="notification-empty shop-error" role="alert">{{ error }}</p><p v-else-if="!notices.length" class="notification-empty">Chưa có thông báo mới.</p><div v-else class="notification-items"><RouterLink v-for="n in notices" :key="n.id" :to="n.href" class="notification-item" :class="{ unread: !read.includes(n.id) }" @click="seen(n.id)"><span class="notification-icon"><BaseIcon :name="n.id.startsWith('order:') ? 'box' : 'ticket'" /></span><div><strong>{{ n.title }}</strong><p>{{ n.message }}</p><time v-if="n.at">{{ new Date(n.at).toLocaleString('vi-VN') }}</time></div><span v-if="!read.includes(n.id)" class="notification-dot"></span></RouterLink></div></section></div></template>
