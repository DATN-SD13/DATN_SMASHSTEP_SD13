import api from './api'
export const storefrontService = {
  products: (params, signal) => api.get('/store/products', { params, signal }),
  product: (id, signal) => api.get('/store/products/' + encodeURIComponent(id), { signal }),
  filters: signal => api.get('/store/filters', { signal }),
  brands: signal => api.get('/store/brands', { signal })
}
