import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { commerceService, commerceError } from '../services/commerceService'
const key = 'smashstep_cart_v1'
export function readCart(value) {
  if (!Array.isArray(value)) return []
  const found = new Set()
  return value.filter(i => i && Number.isSafeInteger(i.variantId) && i.variantId > 0 && Number.isInteger(i.quantity) && i.quantity >= 1 && i.quantity <= 10000 && !found.has(i.variantId) && found.add(i.variantId)).slice(0, 50).map(({ variantId, quantity }) => ({ variantId, quantity }))
}
export const useCartStore = defineStore('customer-cart', () => {
  let saved = []
  try { saved = readCart(JSON.parse(localStorage.getItem(key) || '[]')) } catch { /* Invalid storage is never trusted. */ }
  const items = ref(saved), variants = ref([]), loading = ref(false), error = ref(''), verified = ref(false)
  let pending, revision = 0
  const lines = computed(() => items.value.map(item => ({ ...item, variant: variants.value.find(v => v.variantId === item.variantId) })))
  const count = computed(() => items.value.reduce((sum, i) => sum + i.quantity, 0))
  const subtotal = computed(() => lines.value.reduce((sum, i) => sum + Number(i.variant?.giaSauGiam || 0) * i.quantity, 0))
  const invalid = computed(() => !verified.value || !!error.value || lines.value.some(i => !i.variant?.available || i.quantity > i.variant.stock))
  function persist() { try { localStorage.setItem(key, JSON.stringify(items.value)) } catch { error.value = 'Trình duyệt không lưu được giỏ hàng. Vui lòng kiểm tra dung lượng lưu trữ.' } }
  async function refresh() {
    if (pending) return pending
    const version = revision
    if (!items.value.length) { variants.value = []; verified.value = true; return }
    loading.value = true
    pending = commerceService.variants(items.value.map(i => i.variantId)).then(rows => {
      if (version === revision) { variants.value = rows; verified.value = true; error.value = '' }
    }).catch(e => { verified.value = false; error.value = commerceError(e) }).finally(() => { loading.value = false; pending = null })
    await pending
    if (version !== revision) return refresh()
  }
  async function add(variantId, quantity) {
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > 10000) throw new Error('Số lượng không hợp lệ.')
    const rows = await commerceService.variants([variantId]), v = rows[0]
    const existing = items.value.find(i => i.variantId === variantId), total = (existing?.quantity || 0) + quantity
    if (!v?.available || total > v.stock || total > 10000) throw new Error('Sản phẩm không đủ tồn kho cho số lượng trong giỏ.')
    if (!existing && items.value.length >= 50) throw new Error('Giỏ hàng tối đa 50 biến thể.')
    if (existing) existing.quantity = total
    else items.value.push({ variantId, quantity })
    revision++; persist(); await refresh()
  }
  function setQuantity(id, quantity) {
    const item = items.value.find(i => i.variantId === id), v = variants.value.find(i => i.variantId === id)
    if (!item || !Number.isInteger(quantity) || quantity < 1 || quantity > Math.min(v?.stock ?? 0, 10000)) return
    item.quantity = quantity; revision++; persist()
  }
  function remove(id) { items.value = items.value.filter(i => i.variantId !== id); revision++; persist() }
  function purchased(bought) {
    items.value = items.value.flatMap(i => { const used = bought.find(b => b.variantId === i.variantId)?.quantity || 0; return i.quantity > used ? [{ variantId: i.variantId, quantity: i.quantity - used }] : [] })
    revision++; persist()
  }
  function syncStorage(event) { if (event.key === key) { try { items.value = readCart(JSON.parse(event.newValue || '[]')); revision++; verified.value = false; refresh() } catch { /* Ignore malformed cross-tab values. */ } } }
  return { items, lines, count, subtotal, invalid, loading, error, verified, refresh, add, setQuantity, remove, purchased, syncStorage }
})
