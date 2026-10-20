import test from 'node:test'
import assert from 'node:assert/strict'
import { countdownAnchor, countdownLabel, remainingSeconds, QR_LIFETIME_MS } from '../src/utils/qrPayment.js'
import { createDemo, readDemo, transitionDemo, demoPayload, validDemoAmount } from '../src/utils/paymentDemo.js'
const now = Date.parse('2026-10-20T10:00:00Z'), uuid = '12345678-1234-4234-8234-123456789abc'
const make = () => createDemo(999000, now, uuid)
test('QR window is exactly 300 seconds and uses server time instead of browser wall clock', () => {
  const anchor = countdownAnchor({ serverTime: '2026-10-20T10:00:00Z', expiresAt: '2026-10-20T10:05:00Z' }, 123)
  assert.equal(remainingSeconds(anchor, 123), 300)
  assert.equal(remainingSeconds(anchor, 299123), 1)
  assert.equal(remainingSeconds(anchor, 300123), 0)
  assert.equal(remainingSeconds(anchor, 999999), 0)
  assert.equal(countdownLabel(300), '05:00'); assert.equal(countdownLabel(59), '00:59')
})
test('reloading or polling does not mint a new five minute validity', () => {
  const anchor = countdownAnchor({ serverTime: '2026-10-20T10:04:30Z', expiresAt: '2026-10-20T10:05:00Z' }, 700)
  assert.equal(remainingSeconds(anchor, 700), 30)
  assert.equal(remainingSeconds(countdownAnchor({ serverTime: 'bad', expiresAt: 'bad' }, 0), 0), 0)
  assert.equal(remainingSeconds(countdownAnchor({ serverTime: '2026-10-20T10:06:00Z', expiresAt: '2026-10-20T10:05:00Z' }, 10), 10), 0)
})
test('demo expires exactly at five minutes and success after expiry is rejected', () => {
  const session = make(); assert.equal(session.expiresAt - session.createdAt, QR_LIFETIME_MS)
  assert.equal(readDemo(session, now + QR_LIFETIME_MS - 1).status, 'PENDING')
  assert.equal(readDemo(session, now + QR_LIFETIME_MS).status, 'EXPIRED')
  assert.equal(transitionDemo(session, 'success', now + QR_LIFETIME_MS).status, 'EXPIRED')
})
test('demo success remains explicitly simulated and a terminal result cannot be charged twice', () => {
  const paid = transitionDemo(make(), 'success', now + 1000)
  assert.equal(paid.status, 'PAID_DEMO'); assert.equal(paid.paidAt, now + 1000)
  assert.deepEqual(transitionDemo(paid, 'success', now + 2000), paid)
  assert.deepEqual(transitionDemo(paid, 'failure', now + 2000), paid)
  assert.equal(readDemo(paid, now + QR_LIFETIME_MS + 1000).status, 'PAID_DEMO')
})
test('failed and forced-expired scenarios never become paid', () => {
  const failed = transitionDemo(make(), 'failure', now + 1000), expired = transitionDemo(make(), 'expire', now + 1000)
  assert.equal(failed.status, 'FAILED'); assert.equal(expired.status, 'EXPIRED')
  assert.equal(transitionDemo(failed, 'success', now + 2000).status, 'FAILED')
  assert.equal(transitionDemo(expired, 'success', now + 2000).status, 'EXPIRED')
  assert.equal(readDemo(expired, now + 2000).status, 'EXPIRED')
})
test('stored demo session cannot extend its original deadline', () => {
  const original = make(); assert.equal(readDemo(JSON.parse(JSON.stringify(original)), now + 120000).expiresAt, original.expiresAt)
  assert.equal(readDemo({ ...original, expiresAt: original.expiresAt + 1 }, now), null)
  assert.equal(readDemo({ ...original, createdAt: now + 1 }, now), null)
  assert.equal(readDemo({ ...original, status: 'PAID_DEMO', paidAt: original.expiresAt }, now), null)
})
test('invalid amounts and session data are rejected', () => {
  for (const amount of [0, -1, 1.5, NaN, Infinity, '1000', 10000000000000]) assert.equal(validDemoAmount(amount), false)
  assert.equal(readDemo(null, now), null); assert.equal(readDemo({ ...make(), id: 'HD000001' }, now), null)
  assert.throws(() => createDemo(-1, now, uuid)); assert.throws(() => transitionDemo(make(), 'real-payment', now))
})
test('demo QR has no receiving bank account or payment URL', () => {
  const payload = demoPayload(make())
  assert.match(payload, /SMASHSTEP DEMO ONLY/); assert.match(payload, /NO BANK TRANSFER/)
  assert.doesNotMatch(payload, /https?:|970422|0966026145|accountNo|000201/)
})
