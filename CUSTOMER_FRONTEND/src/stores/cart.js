import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { commerceService, commerceError } from '../services/commerceService'
import { readCustomerSession, TOKEN_KEY, USER_KEY } from '../utils/customerSession'
const prefix = 'smashstep_cart_v2:'
export function cartStorageKey(owner = 'guest') {
  if (owner !== 'guest' && !/^customer:[1-9][0-9]*$/.test(owner)) throw new Error('Giỏ hàng không hợp lệ.')
  return prefix + owner
}
function selectionKey(owner) { return cartStorageKey(owner) + ':selected' }
function storedSelection(owner, items) {
  try {
    const raw = localStorage.getItem(selectionKey(owner))
    if (raw === null) return items.map(i => i.variantId)
    const saved = JSON.parse(raw)
    return Array.isArray(saved) ? items.filter(i => saved.includes(i.variantId)).map(i => i.variantId) : []
  } catch { return [] }
}
export function readCart(value) {
  if (!Array.isArray(value)) return []
  const found = new Set()
  return value.filter(i => i && Number.isSafeInteger(i.variantId) && i.variantId > 0 && Number.isInteger(i.quantity) && i.quantity >= 1 && i.quantity <= 10000 && !found.has(i.variantId) && found.add(i.variantId)).slice(0, 50).map(({ variantId, quantity }) => ({ variantId, quantity }))
}
function sessionOwner() {
  const { user, token } = readCustomerSession(localStorage)
  return token && /^[1-9][0-9]*$/.test(String(user?.id)) ? 'customer:' + user.id : 'guest'
}
function storedItems(owner) {
  try { return readCart(JSON.parse(localStorage.getItem(cartStorageKey(owner)) || '[]')) }
  catch { return [] }
}
export const useCartStore = defineStore('customer-cart', () => {
  const owner = ref(sessionOwner()), items = ref(storedItems(owner.value)), variants = ref([])
  const selectedIds = ref(storedSelection(owner.value, items.value))
  const loading = ref(false), verified = ref(false), fetchError = ref(''), storageError = ref(''), selectionError = ref('')
  const error = computed(() => storageError.value || selectionError.value || fetchError.value)
  let pending = null, revision = 0, generation = 0
  const lines = computed(() => items.value.map(item => ({ ...item, variant: variants.value.find(v => v.variantId === item.variantId) })))
  const count = computed(() => items.value.reduce((sum, i) => sum + i.quantity, 0))
  const subtotal = computed(() => lines.value.reduce((sum, i) => sum + Number(i.variant?.giaSauGiam || 0) * i.quantity, 0))
  const invalid = computed(() => !verified.value || !!error.value || lines.value.some(i => !i.variant?.available || i.quantity > i.variant.stock))
  const selectedItems = computed(() => items.value.filter(i => selectedIds.value.includes(i.variantId)))
  const selectedLines = computed(() => lines.value.filter(i => selectedIds.value.includes(i.variantId)))
  const selectedCount = computed(() => selectedItems.value.reduce((sum, i) => sum + i.quantity, 0))
  const selectedSubtotal = computed(() => selectedLines.value.reduce((sum, i) => sum + Number(i.variant?.giaSauGiam || 0) * i.quantity, 0))
  const selectedInvalid = computed(() => !selectedItems.value.length || !verified.value || !!error.value || selectedLines.value.some(i => !i.variant?.available || i.quantity > i.variant.stock))
  const allSelected = computed(() => items.value.length > 0 && selectedItems.value.length === items.value.length)
  const partlySelected = computed(() => selectedItems.value.length > 0 && !allSelected.value)
  function ensureOwner() {
    const next = sessionOwner()
    if (next === owner.value) return false
    generation++; revision++; pending = null
    owner.value = next; items.value = storedItems(next); variants.value = []
    selectedIds.value = storedSelection(next, items.value)
    verified.value = false; loading.value = false; fetchError.value = ''; storageError.value = ''; selectionError.value = ''
    return true
  }
  function writeItems(target, value) {
    try {
      localStorage.setItem(cartStorageKey(target), JSON.stringify(readCart(value)))
      if (target === owner.value) storageError.value = ''
    } catch {
      if (target === owner.value) storageError.value = 'Trình duyệt không lưu được giỏ hàng. Vui lòng kiểm tra dung lượng lưu trữ.'
    }
  }
  function writeSelection(target, ids) {
    try { localStorage.setItem(selectionKey(target), JSON.stringify(ids)); if (target === owner.value) selectionError.value = '' }
    catch { if (target === owner.value) selectionError.value = 'Trình duyệt không lưu được sản phẩm đã chọn. Vui lòng thử lại.' }
  }
  function persist() { writeItems(owner.value, items.value); writeSelection(owner.value, selectedIds.value) }
  function isSelected(id) { return selectedIds.value.includes(id) }
  function setSelected(id, checked) {
    if (ensureOwner()) { refresh(); return }
    if (!items.value.some(i => i.variantId === id)) return
    selectedIds.value = checked ? [...new Set([...selectedIds.value, id])] : selectedIds.value.filter(i => i !== id)
    writeSelection(owner.value, selectedIds.value)
  }
  function selectAll(checked) {
    if (ensureOwner()) { refresh(); return }
    selectedIds.value = checked ? items.value.map(i => i.variantId) : []; writeSelection(owner.value, selectedIds.value)
  }
  function syncOwner() { return ensureOwner() ? refresh() : Promise.resolve() }
  async function refresh() {
    ensureOwner()
    if (pending) return pending.promise
    if (!items.value.length) { variants.value = []; verified.value = true; fetchError.value = ''; loading.value = false; return }
    const request = { generation, revision, owner: owner.value }
    loading.value = true
    request.promise = commerceService.variants(items.value.map(i => i.variantId)).then(rows => {
      if (ensureOwner()) { refresh(); return }
      if (request.generation === generation && request.revision === revision) {
        variants.value = rows; verified.value = true; fetchError.value = ''
      }
    }).catch(e => {
      if (ensureOwner()) { refresh(); return }
      if (request.generation === generation && request.revision === revision) {
        verified.value = false; fetchError.value = commerceError(e)
      }
    }).finally(() => { if (pending === request) { loading.value = false; pending = null } })
    pending = request
    await request.promise
    if (request.generation === generation && request.revision !== revision) return refresh()
  }
  async function add(variantId, quantity) {
    ensureOwner()
    if (!Number.isSafeInteger(variantId) || variantId < 1 || !Number.isInteger(quantity) || quantity < 1 || quantity > 10000)
      throw new Error('Sản phẩm hoặc số lượng không hợp lệ.')
    const start = generation
    // Read-only catalogue lookup. Adding a cart item never creates an invoice or reserves stock.
    const rows = await commerceService.variants([variantId])
    ensureOwner()
    if (start !== generation) throw new Error('Tài khoản vừa thay đổi. Vui lòng thêm lại sản phẩm vào giỏ hiện tại.')
    const v = rows.find(row => row.variantId === variantId)
    const existing = items.value.find(i => i.variantId === variantId), total = (existing?.quantity || 0) + quantity
    if (!v?.available || total > v.stock || total > 10000) throw new Error('Sản phẩm không đủ tồn kho cho số lượng trong giỏ.')
    if (!existing && items.value.length >= 50) throw new Error('Giỏ hàng tối đa 50 biến thể.')
    if (existing) existing.quantity = total
    else items.value.push({ variantId, quantity })
    if (!selectedIds.value.includes(variantId)) selectedIds.value.push(variantId)
    revision++; persist(); await refresh()
  }
  function setQuantity(id, quantity) {
    if (ensureOwner()) { refresh(); return }
    const item = items.value.find(i => i.variantId === id), v = variants.value.find(i => i.variantId === id)
    if (!item || !Number.isInteger(quantity) || quantity < 1 || quantity > Math.min(v?.stock ?? 0, 10000)) return
    item.quantity = quantity; revision++; persist()
  }
  function remove(id) {
    if (ensureOwner()) { refresh(); return }
    items.value = items.value.filter(i => i.variantId !== id)
    selectedIds.value = selectedIds.value.filter(i => i !== id); revision++; persist()
  }
  function purchased(bought, target = owner.value) {
    ensureOwner()
    // A late checkout response must only update the cart belonging to that checkout's customer.
    const current = target === owner.value ? items.value : storedItems(target)
    const selected = target === owner.value ? selectedIds.value : storedSelection(target, current)
    const next = current.flatMap(i => {
      const used = bought.find(b => b.variantId === i.variantId)?.quantity || 0
      return i.quantity > used ? [{ variantId: i.variantId, quantity: i.quantity - used }] : []
    })
    writeItems(target, next)
    const nextSelected = selected.filter(id => next.some(i => i.variantId === id)); writeSelection(target, nextSelected)
    if (target === owner.value) { items.value = next; selectedIds.value = nextSelected; revision++ }
  }
  function syncStorage(event) {
    if (event.storageArea && event.storageArea !== localStorage) return
    if (ensureOwner()) { refresh(); return }
    if (event.key === TOKEN_KEY || event.key === USER_KEY) return
    if (event.key === null || event.key === cartStorageKey(owner.value) || event.key === selectionKey(owner.value)) {
      items.value = storedItems(owner.value); selectedIds.value = storedSelection(owner.value, items.value)
      revision++; verified.value = false; refresh()
    }
  }
  return { owner: computed(() => owner.value), items, lines, count, subtotal, invalid, loading, error, verified,
    selectedItems, selectedLines, selectedCount, selectedSubtotal, selectedInvalid, allSelected, partlySelected,
    isSelected, setSelected, selectAll, refresh, syncOwner, add, setQuantity, remove, purchased, syncStorage }
})
