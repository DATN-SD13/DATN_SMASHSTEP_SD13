import { ref, onMounted, onBeforeUnmount } from 'vue'
import axios from 'axios'
export function useResource(loader, refreshMs = 30000) {
  const data = ref(null), loading = ref(false), error = ref('')
  let controller, version = 0, timer, disposed = false
  async function reload() {
    const current = ++version
    controller?.abort(); controller = new AbortController()
    loading.value = true; error.value = ''
    try { const value = await loader(controller.signal); if (!disposed && current === version) data.value = value }
    catch (err) { if (!disposed && current === version && !axios.isCancel(err)) error.value = err.response?.status === 404 ? 'Sản phẩm không tồn tại hoặc hiện không còn kinh doanh.' : 'Không thể tải sản phẩm. Vui lòng thử lại.' }
    finally { if (!disposed && current === version) loading.value = false }
  }
  function onVisible() { if (document.visibilityState === 'visible') reload() }
  onMounted(() => { if (refreshMs) { timer = setInterval(() => { if (!document.hidden) reload() }, refreshMs); document.addEventListener('visibilitychange', onVisible) } })
  onBeforeUnmount(() => { disposed = true; ++version; controller?.abort(); clearInterval(timer); document.removeEventListener('visibilitychange', onVisible) })
  return { data, loading, error, reload }
}
