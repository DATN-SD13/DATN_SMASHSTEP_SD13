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
export const productAttributeService = {
  options: () => api.get('/product-attributes/options').then(r => r.data),
  list: (type, params) => api.get(`/product-attributes/${type}`, { params }).then(r => r.data),
  create: (type, data) => api.post(`/product-attributes/${type}`, data).then(r => r.data),
  update: (type, id, data) => api.put(`/product-attributes/${type}/${id}`, data).then(r => r.data),
  status: (type, id, trangThai) => api.patch(`/product-attributes/${type}/${id}/status`, { trangThai }).then(r => r.data)
}

