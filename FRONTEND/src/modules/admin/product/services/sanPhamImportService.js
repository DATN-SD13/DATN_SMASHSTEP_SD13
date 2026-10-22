import api from '../../../../utils/api'
const data = response => response.data.data
export const sanPhamImportService = {
  download: () => api.get('/products/excel/mau', { responseType: 'blob', timeout: 60000 }).then(r => r.data),
  preview: file => { const form = new FormData(); form.append('file', file); return api.post('/products/excel/xem-truoc', form, { headers: { 'Content-Type': undefined }, timeout: 120000 }).then(data) },
  confirm: payload => api.post('/products/excel/xac-nhan', payload, { timeout: 180000 }).then(data),
  result: id => api.get(`/products/excel/${encodeURIComponent(id)}`, { timeout: 60000 }).then(data)
}
export async function taiMauSanPham() {
  const blob = await sanPhamImportService.download(), url = URL.createObjectURL(blob)
  const link = document.createElement('a'); link.href = url; link.download = 'san-pham-mau.xlsx'; link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}
