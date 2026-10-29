// Server remains authoritative. All schedule date/times are Vietnam local times.
function shiftRange(row) {
  const match = String(row?.thoiGian || '').match(/^(\d{2}:\d{2})\s*[-–]\s*(\d{2}:\d{2})$/)
  if (!match || !/^\d{4}-\d{2}-\d{2}$/.test(row?.ngayLam || '')) return null
  const start = Date.parse(`${row.ngayLam}T${match[1]}:00+07:00`)
  let end = Date.parse(`${row.ngayLam}T${match[2]}:00+07:00`)
  if (!Number.isFinite(start) || !Number.isFinite(end) || start === end) return null
  if (end < start) end += 86400000
  return { start, end }
}
export function canSetShiftStatus(row, next, now = Date.now()) {
  const current = row?.trangThai
  if (!Number.isInteger(current) || !Number.isInteger(next) || next < 0 || next > 4) return false
  if (current === next) return true
  if ([1, 3].includes(current) || [1, 3].includes(next)) return false
  const range = shiftRange(row)
  if (!range) return false
  if (next !== 2 && (Number(now) > range.start + 15 * 60000 || Number(now) >= range.end)) return false
  return true
}
export function requiresReopenReason(row, next) { return row?.trangThai === 2 && next !== 2 }
export function lateShiftInfo(row) {
  if (![1, 3].includes(row?.trangThai) || !row.nhanCaLuc) return ''
  const range = shiftRange(row)
  const value = String(row.nhanCaLuc)
  const received = Date.parse(/[zZ]$|[+-]\d{2}:\d{2}$/.test(value) ? value : value + '+07:00')
  if (!range || !Number.isFinite(received) || received <= range.start) return ''
  const time = new Intl.DateTimeFormat('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh', hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(received))
  return `Nhận ca muộn lúc ${time}`
}
export function canDeleteShift(status) { return [0, 2, 4].includes(status) }
  
