import { QR_LIFETIME_MS } from './qrPayment.js'
export const DEMO_PREFIX = 'smashstep_payment_demo_v1:'
export function validDemoAmount(value) { return Number.isSafeInteger(value) && value > 0 && value <= 9999999999999 }
export function createDemo(amount, now, uuid) {
  if (!validDemoAmount(amount) || !Number.isFinite(now) || !/^[0-9a-f-]{36}$/i.test(uuid)) throw new Error('Phiên thử không hợp lệ.')
  return { version: 1, id: 'DEMO-' + uuid, amount, createdAt: now, expiresAt: now + QR_LIFETIME_MS, status: 'PENDING', paidAt: null }
}
export function readDemo(value, now) {
  if (!value || value.version !== 1 || !/^DEMO-[0-9a-f-]{36}$/i.test(value.id)
    || !validDemoAmount(value.amount) || !Number.isSafeInteger(value.createdAt) || !Number.isSafeInteger(value.expiresAt)
    || value.createdAt > now || value.expiresAt - value.createdAt !== QR_LIFETIME_MS
    || !['PENDING', 'PAID_DEMO', 'FAILED', 'EXPIRED'].includes(value.status)) return null
  if (value.status === 'PAID_DEMO' && (!Number.isSafeInteger(value.paidAt) || value.paidAt < value.createdAt || value.paidAt >= value.expiresAt)) return null
  return { version: 1, id: value.id, amount: value.amount, createdAt: value.createdAt, expiresAt: value.expiresAt,
    status: value.status === 'PENDING' && now >= value.expiresAt ? 'EXPIRED' : value.status,
    paidAt: value.status === 'PAID_DEMO' && Number.isFinite(value.paidAt) ? value.paidAt : null }
}
export function transitionDemo(session, action, now) {
  const current = readDemo(session, now)
  if (!current) throw new Error('Phiên thử không hợp lệ. Hãy tạo phiên mới.')
  if (current.status !== 'PENDING') return current
  if (action === 'success') return { ...current, status: 'PAID_DEMO', paidAt: now }
  if (action === 'failure') return { ...current, status: 'FAILED' }
  if (action === 'expire') return { ...current, createdAt: now - QR_LIFETIME_MS, expiresAt: now, status: 'EXPIRED' }
  throw new Error('Tình huống thử không hợp lệ.')
}
export function demoPayload(session) {
  return `SMASHSTEP DEMO ONLY | ${session.id} | VND ${session.amount} | EXPIRES ${session.expiresAt} | NO BANK TRANSFER`
}
