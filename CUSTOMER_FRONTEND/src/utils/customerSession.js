export const TOKEN_KEY = 'ss_token'
export const USER_KEY = 'ss_user'
export function readCustomerSession(storage, now = Date.now()) {
  try {
    const token = storage.getItem(TOKEN_KEY), user = JSON.parse(storage.getItem(USER_KEY) || 'null')
    if (!token && !user) return { token: null, user: null }
    const claims = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')))
    // Client expiry detection is UI state only; the server still verifies signature and ownership.
    if (!token || !user?.id || user.userType !== 'KHACH_HANG' || claims.userType !== 'KHACH_HANG'
      || String(claims.userId) !== String(user.id) || !Number.isFinite(claims.exp) || claims.exp * 1000 <= now) throw new Error('Invalid session')
    return { token, user }
  } catch {
    try { storage.removeItem(TOKEN_KEY); storage.removeItem(USER_KEY) } catch { /* Storage may be disabled. */ }
    return { token: null, user: null }
  }
}
