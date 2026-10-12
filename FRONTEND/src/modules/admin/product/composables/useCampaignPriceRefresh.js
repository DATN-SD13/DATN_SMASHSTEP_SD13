import { onMounted, onBeforeUnmount } from 'vue'

// Refresh display prices only; preserve drafts, edit inputs, filters and confirmations.
export function useCampaignPriceRefresh(readPrices) {
  let timer, pending = false, disposed = false
  async function refresh() {
    if (disposed || pending || document.visibilityState === 'hidden') return
    pending = true
    try { await readPrices() } catch { /* Retry on the next refresh; checkout rechecks server prices. */ }
    finally { pending = false }
  }
  onMounted(() => {
    timer = window.setInterval(refresh, 30000)
    window.addEventListener('focus', refresh)
    document.addEventListener('visibilitychange', refresh)
  })
  onBeforeUnmount(() => {
    disposed = true; window.clearInterval(timer)
    window.removeEventListener('focus', refresh)
    document.removeEventListener('visibilitychange', refresh)
  })
}
