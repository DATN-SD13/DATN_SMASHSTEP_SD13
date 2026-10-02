import { computed, ref, watch } from 'vue'

// Phân trang phía client: usePaging(listRef, 5) -> { page, size, pages, paged, offset }
export function usePaging(list, initialSize = 5) {
  const page = ref(1)
  const size = ref(initialSize)
  const pages = computed(() => Math.max(1, Math.ceil(list.value.length / size.value)))
  const paged = computed(() => list.value.slice((page.value - 1) * size.value, page.value * size.value))
  const offset = computed(() => (page.value - 1) * size.value)
  watch([() => list.value.length, size], () => { if (page.value > pages.value) page.value = 1 })
  return { page, size, pages, paged, offset }
}

export const initials = (name = '') => {
  const w = String(name).trim().split(/\s+/).filter(Boolean)
  if (!w.length) return ''
  return (w.length === 1 ? w[0].slice(0, 2) : w[w.length - 2][0] + w[w.length - 1][0]).toUpperCase()
}
export const money = (n) => `${Number(n || 0).toLocaleString('vi-VN')} đ`
