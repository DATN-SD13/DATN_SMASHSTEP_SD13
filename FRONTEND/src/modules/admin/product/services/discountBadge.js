// Display the canonical campaign percentage returned by the backend; never infer it from prices.
export function discountPercent(item, range = false) {
  const percent = Number(range ? item?.phanTramGiamCaoNhat : item?.phanTramGiamHienTai)
  return Number.isFinite(percent) ? Number(percent.toFixed(2)) : null
}

export function hasDiscount(item, range = false) {
  const percent = discountPercent(item, range)
  return item?.dangGiamGia === true && percent != null && percent > 0
}
