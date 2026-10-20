import { onMounted, onBeforeUnmount } from 'vue'
export function useLiveRefresh(refresh, interval = 30000) {
  let timer
  const update = () => { if (!document.hidden) Promise.resolve(refresh()).catch(() => {}) }
  onMounted(() => { timer = setInterval(update, interval); window.addEventListener('focus', update); document.addEventListener('visibilitychange', update) })
  onBeforeUnmount(() => { clearInterval(timer); window.removeEventListener('focus', update); document.removeEventListener('visibilitychange', update) })
}
