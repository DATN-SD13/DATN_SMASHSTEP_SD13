import { discountPercent, hasDiscount } from '../src/modules/admin/product/services/discountBadge.js'
import fs from 'node:fs'
import vm from 'node:vm'
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
const require = createRequire(new URL('../package.json', import.meta.url))
const vue = require('vue')
const { parse, compileScript, compileTemplate } = require('@vue/compiler-sfc')
const root = new URL('../src/', import.meta.url)
let count = 0
function walk(dir) {
  for (const item of fs.readdirSync(dir, { withFileTypes: true })) {
    const path = new URL(item.name + (item.isDirectory() ? '/' : ''), dir)
    if (item.isDirectory()) walk(path)
    else if (item.name.endsWith('.vue')) {
      const { descriptor, errors } = parse(fs.readFileSync(path, 'utf8'))
      assert.equal(errors.length, 0, path.pathname)
      if (descriptor.scriptSetup) compileScript(descriptor, { id: String(count) })
      if (descriptor.template) assert.equal(compileTemplate({ source: descriptor.template.content, filename: path.pathname, id: String(count) }).errors.length, 0, path.pathname)
      count++
    }
  }
}
walk(root)
let calls = 0, fail = false
const notices = []
const invoiceDescriptor = parse(fs.readFileSync(new URL('modules/admin/invoice/views/InvoiceDetail.vue', root), 'utf8')).descriptor
const script = invoiceDescriptor.scriptSetup.content.replace(/^import .*\r?\n/gm, '')
const context = vm.createContext({
  ...vue, onMounted: () => {}, onUnmounted: () => {}, watch: () => {},
  useRoute: () => ({ params: { code: 'HD01' } }), useRouter: () => ({ push: async () => {} }),
  useProductToast: () => ({ show: (...args) => notices.push(args) }),
  invoiceService: { updateStatus: async (code, body) => {
    calls++; await new Promise(resolve => setTimeout(resolve, 10))
    if (fail) throw new Error('Database rejected transition')
    return { success: true, data: { maHoaDon: code, maTrangThai: body.trangThai, maHinhThucNhan: 1, lichSuHoaDon: [{ maTrangThai: body.trangThai, ngayTao: '11/10/2026 12:00:00' }], chiTietHoaDon: [] } }
  } },
  api: {}, imageUrl: x => x, window: { setTimeout, clearTimeout, addEventListener: () => {}, removeEventListener: () => {} }, console
})
vm.runInContext(script + '\nglobalThis.state = { invoice, selectedStatus, updatingStatus, saveStatus, steps, notices: null };', context)
const state = context.state
state.invoice.value = { code: 'HD01', statusCode: 1, nextStatusCode: 2, receiveMethodCode: 1, items: [] }
state.selectedStatus.value = 1
await state.saveStatus(); assert.equal(calls, 0)
state.selectedStatus.value = 2
await Promise.all([state.saveStatus(), state.saveStatus()]); assert.equal(calls, 1)
assert.equal(state.invoice.value.statusCode, 2)
assert.equal(state.invoice.value.history[0].maTrangThai, 2)
assert.equal(notices.at(-1)[0], 'success')
state.invoice.value = { code: 'HD01', statusCode: 2, nextStatusCode: 3, receiveMethodCode: 1, items: [] }
state.selectedStatus.value = 3; fail = true
await state.saveStatus(); assert.equal(state.invoice.value.statusCode, 2); assert.equal(notices.at(-1)[0], 'error')
fail = false; await state.saveStatus(); assert.equal(state.invoice.value.statusCode, 3)
// Latest main removed the cancellation UI in eec1937; its backend transition remains tested separately.
const activeTemplate = invoiceDescriptor.template.content.replace(/<!--[\s\S]*?-->/g, '')
assert.equal(activeTemplate.includes('@click="cancelOrder"'), false)
assert.equal(activeTemplate.includes('@click="openCancelModal"'), false)
state.invoice.value = { code: 'HD01', statusCode: 0, nextStatusCode: 1, receiveMethodCode: 0, items: [] }
assert.deepEqual(Array.from(state.steps.value, s => s.value), [0, 1, 5])
state.invoice.value = { code: 'HD01', statusCode: 8, nextStatusCode: 5, receiveMethodCode: 0, items: [] }
assert.deepEqual(Array.from(state.steps.value, s => s.value), [8, 5])
console.log(`PASS: ${count} Vue SFCs compiled; invoice unchanged-state, duplicate-submit, failure preservation, returned history, recovery after failure, current cancellation UI contract and pickup timeline regressions.`)

const { useProductToast } = await import('../src/modules/admin/product/composables/useProductToast.js')
const first = useProductToast(), afterNavigation = useProductToast()
first.success('Lưu thành công')
const id = first.toast.value.id
assert.equal(first.toast, afterNavigation.toast)
assert.equal(afterNavigation.toast.value.message, 'Lưu thành công')
first.success('Lưu thành công'); assert.equal(first.toast.value.id, id)
first.error('Không lưu được'); assert.equal(afterNavigation.toast.value.type, 'error')
first.close(); assert.equal(afterNavigation.toast.value, null)
console.log('PASS: shared toast persists across consumers, suppresses duplicate notices and displays errors.')

for (const [value, expected] of [[20, 20], ['20.0', 20], ['20.00', 20], [12.5, 12.5], ['12.50', 12.5]]) {
  const product = { dangGiamGia: true, phanTramGiamCaoNhat: value }
  assert.equal(hasDiscount(product, true), true)
  assert.equal(discountPercent(product, true), expected)
}
for (const value of [null, undefined, 0, -1, 'NaN', Infinity]) {
  assert.equal(hasDiscount({ dangGiamGia: true, phanTramGiamCaoNhat: value }, true), false)
}
for (const flag of [false, null, undefined, 1, 'true']) {
  assert.equal(hasDiscount({ dangGiamGia: flag, phanTramGiamCaoNhat: 20 }, true), false)
}
const variant = { dangGiamGia: true, phanTramGiamCaoNhat: 20, phanTramGiamHienTai: 10 }
assert.equal(discountPercent(variant), 10)
assert.equal(discountPercent(variant, true), 20)
assert.equal(hasDiscount({ ...variant, phanTramGiamHienTai: 0 }), false)
console.log('PASS: sale image badge canonical percentage, decimal formatting, invalid/inactive hiding, product maximum and variant own percentage regressions.')