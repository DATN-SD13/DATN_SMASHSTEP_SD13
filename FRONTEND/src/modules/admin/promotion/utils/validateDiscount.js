/**
 * Validate form phiếu giảm giá dùng chung cho AddDiscount và EditDiscount.
 * @param {Object} f       - form.value
 * @param {boolean} isAdd  - true nếu đang tạo mới (cần check mã phiếu)
 * @returns {string|null}  - thông báo lỗi hoặc null nếu hợp lệ
 */
export function validateDiscount(f, isAdd = false) {
  const isPercent     = Number(f.discountType) === 1
  const discountValue = Number(f.discountValue)
  const minOrderValue = Number(f.minOrderValue)
  const maxDiscount   = Number(f.maxDiscount)

  // ── Hình thức & loại ──────────────────────────────────
  if (![1, 2].includes(Number(f.form))) {
    return 'Hình thức phiếu giảm giá không hợp lệ!'
  }
  if (![1, 2].includes(Number(f.discountType))) {
    return 'Loại giảm giá không hợp lệ!'
  }

  // ── Tên phiếu ────────────────────────────────────────
  if (!f.name?.trim()) {
    return 'Vui lòng nhập tên phiếu!'
  }

  // ── Mã phiếu (chỉ khi tạo mới) ───────────────────────
  if (isAdd && !f.code?.trim()) {
    return 'Mã phiếu chưa được tạo!'
  }

  // ── Giá trị đơn tối thiểu ────────────────────────────
  if (
    f.minOrderValue == null ||
    f.minOrderValue === '' ||
    !Number.isFinite(minOrderValue) ||
    minOrderValue < 0
  ) {
    return 'Giá trị đơn tối thiểu phải lớn hơn hoặc bằng 0!'
  }
  if (!isPercent && minOrderValue <= 0) {
    return 'Giảm tiền mặt yêu cầu giá trị đơn tối thiểu lớn hơn 0!'
  }

  // ── Giá trị giảm ─────────────────────────────────────
  if (
    f.discountValue == null ||
    f.discountValue === '' ||
    !Number.isFinite(discountValue) ||
    discountValue <= 0
  ) {
    return 'Giá trị giảm phải lớn hơn 0!'
  }
  if (isPercent && discountValue > 100) {
    return 'Phần trăm giảm không được vượt quá 100%!'
  }
  if (!isPercent && discountValue > minOrderValue) {
    return 'Giá trị giảm không được lớn hơn giá trị đơn tối thiểu!'
  }

  // ── Giảm tối đa (chỉ %) ──────────────────────────────
  if (isPercent) {
    if (!Number.isFinite(maxDiscount) || maxDiscount <= 0) {
      return 'Giảm theo phần trăm phải có mức giảm tối đa lớn hơn 0!'
    }
    if (minOrderValue > 0 && maxDiscount > minOrderValue) {
      return 'Giảm tối đa không được lớn hơn giá trị đơn tối thiểu!'
    }
  }

  // ── Số lượng ─────────────────────────────────────────
  if (Number(f.form) === 2) {
    if (!Array.isArray(f.customerIds) || f.customerIds.length === 0) {
      return 'Vui lòng chọn khách hàng nhận phiếu!'
    }
  } else if (!f.unlimited) {
    const quantity = Number(f.quantity)
    if (
      f.quantity == null ||
      f.quantity === '' ||
      !Number.isInteger(quantity) ||
      quantity <= 0
    ) {
      return 'Số lượng phải là số nguyên lớn hơn 0!'
    }
  }

  // ── Ngày giờ ─────────────────────────────────────────
  if (!f.startDate || !f.endDate) {
    return 'Vui lòng nhập đầy đủ ngày bắt đầu và ngày kết thúc!'
  }
  const start = new Date(f.startDate)
  const end   = new Date(f.endDate)
  if (Number.isNaN(start.getTime())) return 'Ngày giờ bắt đầu không hợp lệ!'
  if (Number.isNaN(end.getTime()))   return 'Ngày giờ kết thúc không hợp lệ!'
  if (end <= start)                  return 'Ngày giờ kết thúc phải sau ngày giờ bắt đầu!'

  return null
}
