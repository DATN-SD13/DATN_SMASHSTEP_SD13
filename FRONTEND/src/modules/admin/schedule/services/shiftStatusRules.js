// Receiving/closing a shift belongs to its employee's dedicated workflow.
export function canSetShiftStatus(current, next) {
    if (!Number.isInteger(current) || !Number.isInteger(next) || next < 0 || next > 4) return false
    if (current === next) return true
    return ![1, 3].includes(current) && ![1, 3].includes(next)
  }
  
  export function canDeleteShift(status) { return [0, 2, 4].includes(status) }
  