// Public receiving-account configuration. Never put API keys in VITE_* variables.
import defaults from './pos-bank.config.json'
const env = { ...defaults, ...(import.meta.env || {}) }
export function receivingAccount(config = env) {
  const bank = String(config.VITE_POS_BANK_BIN || '').trim()
  const account = String(config.VITE_POS_BANK_ACCOUNT || '').trim()
  const name = String(config.VITE_POS_BANK_NAME || '').trim()
  return /^\d{6}$/.test(bank) && /^[A-Za-z0-9]{1,19}$/.test(account) && name
    ? { bank, account, name, label: String(config.VITE_POS_BANK_LABEL || bank) } : null
}
export const bankConfig = receivingAccount()
export function qrLink(account, amount, invoiceCode) {
  if (!account || !Number.isSafeInteger(amount) || amount <= 0) return null
  const memo = `SMASHSTEP ${invoiceCode}`.normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[^A-Za-z0-9 ]/g, '').slice(0, 50)
  const accountName = account.name.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/gi, 'D').toUpperCase()
  const params = new URLSearchParams({ amount: String(amount), addInfo: memo, accountName })
  return { url: `https://img.vietqr.io/image/${encodeURIComponent(account.bank)}-${encodeURIComponent(account.account)}-compact2.png?${params}`, memo, amount }
}
