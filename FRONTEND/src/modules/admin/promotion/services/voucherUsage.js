// quantity is the issuance cap; usedQuantity is incremented by successful checkout.
export function voucherRemaining(voucher) {
    if (voucher?.unlimited === true || voucher?.quantity === null) return 'Không giới hạn'
    if (voucher?.quantity === undefined || !Number.isFinite(Number(voucher.quantity))) return '—'
    return Math.max(0, Number(voucher.quantity) - Number(voucher.usedQuantity || 0))
  }
