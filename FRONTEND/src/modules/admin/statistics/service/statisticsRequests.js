/** Keep only the latest read per endpoint and bound UI waiting independently of interceptors. */
export function createStatisticsRequests(api, timeoutMs = 15000) {
  const pending = new Map()
  function cancelAll() {
    for (const request of pending.values()) request.cancel()
    pending.clear()
  }
  function get(url, config = {}) {
    pending.get(url)?.cancel()
    const controller = new AbortController()
    let timer, rejectPending, settled = false
    const request = {
      cancel() {
        if (settled) return
        settled = true
        clearTimeout(timer)
        controller.abort()
        const error = new Error('Đã chuyển bộ lọc thống kê')
        error.code = 'ERR_CANCELED'
        rejectPending(error)
      },
    }
    const result = new Promise((resolve, reject) => {
      rejectPending = reject
      const finish = (callback, value) => {
        if (settled) return
        settled = true
        clearTimeout(timer)
        callback(value)
      }
      timer = setTimeout(() => {
        const error = new Error('Tải thống kê quá thời gian chờ. Kiểm tra backend và thử lại bộ lọc.')
        error.code = 'ECONNABORTED'
        finish(reject, error)
        controller.abort()
      }, timeoutMs)
      Promise.resolve().then(() => controller.signal.aborted ? undefined : api.get(url, { ...config, timeout: timeoutMs, signal: controller.signal }))
        .then(value => finish(resolve, value), error => finish(reject, error))
    })
    pending.set(url, request)
    return result.finally(() => {
      if (pending.get(url) === request) pending.delete(url)
    })
  }
  return { get, cancelAll }
}
