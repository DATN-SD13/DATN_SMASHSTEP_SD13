export const QR_LIFETIME_MS = 5 * 60 * 1000
// Anchor to the server's clock and advance with a monotonic browser clock, not the user's wall clock.
export function countdownAnchor(payment, monotonicNow) {
  const expires = Date.parse(payment?.expiresAt), server = Date.parse(payment?.serverTime)
  if (!Number.isFinite(expires) || !Number.isFinite(server) || !Number.isFinite(monotonicNow)) return null
  return monotonicNow + Math.max(0, Math.min(QR_LIFETIME_MS, expires - server))
}
export function remainingSeconds(anchor, monotonicNow) {
  if (!Number.isFinite(anchor) || !Number.isFinite(monotonicNow)) return 0
  return Math.max(0, Math.min(300, Math.ceil((anchor - monotonicNow) / 1000)))
}
export function countdownLabel(seconds) {
  const safe = Number.isFinite(seconds) ? Math.min(300, Math.max(0, Math.floor(seconds))) : 0
  return String(Math.floor(safe / 60)).padStart(2, '0') + ':' + String(safe % 60).padStart(2, '0')
}
