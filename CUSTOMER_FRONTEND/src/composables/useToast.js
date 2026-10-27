import { ref } from 'vue'

const notification = ref(null)
let timer, sequence = 0
function close() { clearTimeout(timer); notification.value = null }
function show(type, message) {
  if (!message) return
  clearTimeout(timer)
  notification.value = { id: ++sequence, type, message }
  timer = setTimeout(close, type === 'error' ? 7000 : 5000)
}
export function useToast() {
  return { notification, close, success: message => show('success', message),
    error: message => show('error', message), info: message => show('info', message) }
}
