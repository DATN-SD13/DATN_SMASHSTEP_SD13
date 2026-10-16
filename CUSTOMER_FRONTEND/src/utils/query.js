export const filterKeys = ['brandId', 'categoryId', 'colorId', 'sizeId', 'styleId']
export function ids(value) { return [...new Set((Array.isArray(value) ? value : value == null ? [] : [value]).filter(v => /^\d+$/.test(String(v)) && Number.isSafeInteger(Number(v)) && Number(v) > 0).map(Number))] }
export function readQuery(query) {
  const page = Number(query.page)
  const result = { page: Number.isInteger(page) && page > 0 && page < 1000000 ? page - 1 : 0, size: [6, 12, 24].includes(Number(query.size)) ? Number(query.size) : 12, keyword: typeof query.keyword === 'string' ? query.keyword.trim().slice(0, 120) : '', sort: ['newest', 'priceAsc', 'priceDesc'].includes(query.sort) ? query.sort : 'newest' }
  for (const key of filterKeys) result[key] = ids(query[key])
  for (const key of ['minPrice', 'maxPrice']) if (typeof query[key] === 'string' && /^\d+(\.\d{1,2})?$/.test(query[key]) && Number.isFinite(Number(query[key]))) result[key] = query[key]
  return result
}
export function priceError(min, max) {
  if ((min !== '' && (!Number.isFinite(Number(min)) || Number(min) < 0)) || (max !== '' && (!Number.isFinite(Number(max)) || Number(max) < 0))) return 'Giá phải là số không âm.'
  if (min !== '' && max !== '' && Number(min) > Number(max)) return 'Giá từ không được lớn hơn giá đến.'
  return ''
}
