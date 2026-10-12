import api from '../../../../utils/api'

export const attributeTypes = [
  { key: 'categories', label: 'Danh mục', field: 'danhMucId', filter: 'categoryId' },
  { key: 'brands', label: 'Thương hiệu', field: 'thuongHieuId', filter: 'brandId' },
  { key: 'materials', label: 'Chất liệu', field: 'chatLieuId', filter: 'materialId' },
  { key: 'styles', label: 'Kiểu dáng', field: 'kieuDangId', filter: 'styleId' },
  { key: 'collars', label: 'Cổ giày', field: 'coGiayId', filter: 'collarId' },
  { key: 'origins', label: 'Xuất xứ', field: 'xuatXuId', filter: 'originId' },
  { key: 'colors', label: 'Màu sắc' },
  { key: 'sizes', label: 'Kích thước' }
]
async function allAttributes(type) {
  const first = (await api.get(`/product-attributes/${type}`, { params: { page: 0, size: 100 } })).data
  const rows = [...first.content]
  for (let page = 1; page < first.totalPages; page++) {
    const next = (await api.get(`/product-attributes/${type}`, { params: { page, size: 100 } })).data
    rows.push(...next.content)
  }
  return [...new Map(rows.map(row => [row.id, row])).values()]
}

export const productAttributeService = {
  options: async (keys = attributeTypes.map(type => type.key)) =>
    Object.fromEntries(await Promise.all(keys.map(async key => [key, await allAttributes(key)]))),
  list: (type, params) => api.get(`/product-attributes/${type}`, { params }).then(r => r.data),
  get: (type, id) => api.get(`/product-attributes/${type}/${id}`).then(r => r.data),
  checkDuplicate: (type, data) => api.post(`/product-attributes/${type}/check-duplicate`, data).then(r => r.data),
  nextCode: type => api.get(`/product-attributes/${type}/next-code`).then(r => r.data),
  create: (type, data) => api.post(`/product-attributes/${type}`, data).then(r => r.data),
  update: (type, id, data) => api.put(`/product-attributes/${type}/${id}`, data).then(r => r.data),
  status: (type, id, trangThai) => api.patch(`/product-attributes/${type}/${id}/status`, { trangThai }).then(r => r.data)
}
