import { ref, watch } from 'vue'

const toast = ref(null)
let timer, sequence = 0, lastShownAt = 0
const titles = { success: 'Thành công', error: 'Thao tác thất bại', warning: 'Vui lòng kiểm tra', info: 'Thông tin' }
function close() { clearTimeout(timer); toast.value = null }
function show(type, message, title = titles[type]) {
  if (!message) return
  if (toast.value?.type === type && toast.value?.message === message && toast.value?.title === title && Date.now() - lastShownAt < 250) return
  lastShownAt = Date.now()
  clearTimeout(timer)
  toast.value = { id: ++sequence, type, title, message }
  timer = setTimeout(close, 3000)
}
export function useProductToast() {
  return { toast, close, show, ...Object.fromEntries(Object.keys(titles).map(type => [type, (message, title) => show(type, message, title)])) }
}
export function useProductFeedback(error, success) {
  if (error) watch(error, value => { if (value) show('error', value) }, { immediate: true, flush: 'sync' })
  if (success) watch(success, value => { if (value) show('success', value) }, { immediate: true, flush: 'sync' })
}
