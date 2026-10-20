import { test, after } from 'node:test'
import assert from 'node:assert/strict'
import { createServer } from 'vite'
import { createPinia, setActivePinia } from 'pinia'
const storage = new Map()
globalThis.localStorage = { getItem: key => storage.get(key) ?? null, setItem: (key, value) => storage.set(key, value), removeItem: key => storage.delete(key) }
const server = await createServer({ configFile: false, root: new URL('../', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1'), server: { middlewareMode: true, hmr: false }, logLevel: 'silent' })
const { readCart, useCartStore } = await server.ssrLoadModule('/src/stores/cart.js')
const { default: api } = await server.ssrLoadModule('/src/services/api.js')
const { readQuery, priceError } = await server.ssrLoadModule('/src/utils/query.js')
const { money } = await server.ssrLoadModule('/src/utils/money.js')
after(() => server.close())
test('stored cart accepts identifiers and whole quantities only; discards stored prices', () => {
  assert.deepEqual(readCart([{ variantId: 1, quantity: 2, price: 1 }, { variantId: 1, quantity: 3 }, { variantId: 2, quantity: 1.5 }, { variantId: 3, quantity: 0 }, { variantId: '4', quantity: 1 }]), [{ variantId: 1, quantity: 2 }])
  assert.deepEqual(readCart({ items: [] }), [])
  assert.equal(readCart(Array.from({ length: 70 }, (_, i) => ({ variantId: i + 1, quantity: 1 }))).length, 50)
})
test('cart fetches authoritative price; enforces available stock and persists no display price', async () => {
  setActivePinia(createPinia()); storage.clear()
  let price = 800000, stock = 3
  api.defaults.adapter = async config => ({ data: { success: true, data: [{ variantId: 1, productId: 10, name: 'Fixture product', stock, giaSauGiam: price, available: stock > 0 }] }, status: 200, statusText: 'OK', headers: {}, config })
  const cart = useCartStore()
  await cart.add(1, 2)
  assert.equal(cart.count, 2); assert.equal(cart.subtotal, 1600000)
  assert.deepEqual(JSON.parse(storage.get('smashstep_cart_v2:guest')), [{ variantId: 1, quantity: 2 }])
  await assert.rejects(() => cart.add(1, 2), /tồn kho/)
  cart.setQuantity(1, 1.5); assert.equal(cart.count, 2)
  price = 900000; stock = 1; await cart.refresh()
  assert.equal(cart.subtotal, 1800000); assert.equal(cart.invalid, true)
  cart.setQuantity(1, 1); assert.equal(cart.invalid, false)
  cart.purchased([{ variantId: 1, quantity: 1 }]); assert.equal(cart.count, 0)
})
test('price refresh failure blocks checkout and does not discard cart items', async () => {
  setActivePinia(createPinia()); storage.set('smashstep_cart_v2:guest', '[{"variantId":1,"quantity":1}]')
  api.defaults.adapter = async () => { throw new Error('offline') }
  const cart = useCartStore(); await cart.refresh()
  assert.equal(cart.items.length, 1); assert.equal(cart.invalid, true); assert.equal(cart.verified, false)
})
test('recommendation filters map to existing query fields and reject reversed budgets', () => {
  const query = readQuery({ categoryId: '7', colorId: ['2', '2'], sizeId: '4', brandId: '6', minPrice: '100000', maxPrice: '500000' })
  assert.deepEqual(query.categoryId, [7]); assert.deepEqual(query.colorId, [2]); assert.deepEqual(query.sizeId, [4]); assert.deepEqual(query.brandId, [6])
  assert.notEqual(priceError('500000', '100000'), '')
})
test('server total strings do not lose precision through JavaScript Number', () => {
  assert.equal(money('9999999999999999.00'), '9.999.999.999.999.999\u00a0₫')
  assert.equal(money('1000.50'), '1.001\u00a0₫')
})

test('customer session restores valid identity and clears expired or malformed state', async () => {
  const { readCustomerSession } = await import('../src/utils/customerSession.js')
  const jwt = claims => 'header.' + Buffer.from(JSON.stringify(claims)).toString('base64url') + '.signature'
  const now = Date.now()
  storage.clear(); storage.set('ss_token',jwt({ userId: 4, userType: 'KHACH_HANG', exp: Math.floor(now/1000)+60 }))
  storage.set('ss_user',JSON.stringify({ id: 4, userType: 'KHACH_HANG' }))
  assert.equal(readCustomerSession(localStorage,now).user.id,4)
  assert.equal(readCustomerSession(localStorage,now+120000).token,null)
  assert.equal(storage.has('ss_token'),false)
  storage.set('ss_user','not-json'); storage.set('ss_token','undefined')
  assert.equal(readCustomerSession(localStorage,now).user,null)
  assert.equal(storage.has('ss_user'),false)
  storage.set('ss_token',jwt({ userId: 99, userType: 'KHACH_HANG', exp: Math.floor(now/1000)+60 }))
  storage.set('ss_user',JSON.stringify({ id: 4, userType: 'KHACH_HANG' }))
  assert.equal(readCustomerSession(localStorage,now).token,null)
})


test('cart selection checks out only chosen variants and keeps other products after purchase', async () => {
  setActivePinia(createPinia()); storage.clear()
  api.defaults.adapter = async config => ({ data: { success: true, data: [
    { variantId: 1, productId: 10, name: 'Nike Air Force 1', stock: 10, giaSauGiam: 1000000, available: true },
    { variantId: 2, productId: 11, name: 'Adidas Samba', stock: 10, giaSauGiam: 1500000, available: true }
  ] }, status: 200, statusText: 'OK', headers: {}, config })
  const cart = useCartStore(); await cart.add(1, 2); await cart.add(2, 1)
  assert.equal(cart.allSelected, true); cart.setSelected(2, false)
  assert.equal(cart.partlySelected, true); assert.equal(cart.selectedCount, 2)
  assert.equal(cart.selectedSubtotal, 2000000); assert.deepEqual(cart.selectedItems, [{ variantId: 1, quantity: 2 }])
  cart.purchased(cart.selectedItems.map(i => ({ ...i })))
  assert.deepEqual(cart.items, [{ variantId: 2, quantity: 1 }]); assert.equal(cart.selectedCount, 0)
  assert.equal(cart.selectedInvalid, true); assert.deepEqual(JSON.parse(storage.get('smashstep_cart_v2:guest:selected')), [])
})

test('explicitly clearing selection survives reload and unselected unavailable items do not block payment', async () => {
  storage.clear(); storage.set('smashstep_cart_v2:guest', '[{"variantId":1,"quantity":1},{"variantId":2,"quantity":1}]')
  storage.set('smashstep_cart_v2:guest:selected', '[]'); setActivePinia(createPinia())
  api.defaults.adapter = async config => ({ data: { success: true, data: [
    { variantId: 1, stock: 10, giaSauGiam: 1000000, available: true }, { variantId: 2, stock: 0, giaSauGiam: 1500000, available: false }
  ] }, status: 200, statusText: 'OK', headers: {}, config })
  const cart = useCartStore(); await cart.refresh(); assert.equal(cart.selectedCount, 0)
  cart.setSelected(1, true); assert.equal(cart.selectedInvalid, false); assert.equal(cart.invalid, true)
  cart.selectAll(true); assert.equal(cart.selectedInvalid, true)
  cart.selectAll(false); assert.equal(cart.selectedInvalid, true)
})
