import api from '../../../../utils/api'

const unwrap = r => r?.data?.data

const scheduleService = {
  async capabilities() { return unwrap(await api.get('/lich-lam-viec/capabilities')) },
  // ── Ca làm việc ──────────────────────────────────────────
  async getCaLamViec() {
    return unwrap(await api.get('/ca-lam-viec'))
  },

  async getCaHomNay() {
    return unwrap(await api.get('/lich-lam-viec/ca-hom-nay'))
  },

  async getCaSapBatDau() {
    return unwrap(await api.get('/lich-lam-viec/ca-sap-bat-dau'))
  },

  async xacNhanNhanCa(id) {
    return unwrap(await api.post(`/lich-lam-viec/${id}/xac-nhan-nhan-ca`))
  },

  async chuaNhanCa(id) {
    return unwrap(await api.post(`/lich-lam-viec/${id}/chua-nhan-ca`))
  },
  async getLichNhanVienBiKhoa() {
    return unwrap(await api.get('/lich-lam-viec/nhan-vien-bi-khoa'))
  },
  async getNhanVienTrongCa(caId, ngayLam) {
    return unwrap(await api.get('/lich-lam-viec/nhan-vien-trong-ca', { params: { caId, ngayLam } }))
  },
  async getCaLamViecActive() {
    return unwrap(await api.get('/ca-lam-viec/active'))
  },

  async createCaLamViec(payload) {
    return unwrap(await api.post('/ca-lam-viec', payload))
  },

  async updateCaLamViec(id, payload) {
    return unwrap(await api.put(`/ca-lam-viec/${id}`, payload))
  },

  async toggleTrangThaiCa(id) {
    return unwrap(await api.patch(`/ca-lam-viec/${id}/trang-thai`))
  },

  // ── Lịch làm việc ────────────────────────────────────────
  async getLichLamViec(params = {}) {
    return unwrap(await api.get('/lich-lam-viec', { params }))
  },

  async getLichSuHoatDong(params = {}) {
    return unwrap(await api.get('/lich-lam-viec/lich-su-hoat-dong', { params }))
  },

  async getVaiTro() {
    return unwrap(await api.get('/lich-lam-viec/vai-tro'))
  },

  async getLichTuan(tuNgay) {
    return unwrap(await api.get('/lich-lam-viec/tuan', { params: { tuNgay } }))
  },

  async phanCa(payload) {
    return unwrap(await api.post('/lich-lam-viec', payload))
  },

  async updateTrangThai(id, trangThai, ghiChu = '') {
    return unwrap(await api.patch(`/lich-lam-viec/${id}/trang-thai`, { trangThai, ghiChu }))
  },

  async deleteLich(id) {
    return unwrap(await api.delete(`/lich-lam-viec/${id}`))
  },

  async xepCaTuDong(tuNgay, denNgay) {
    return unwrap(await api.post('/lich-lam-viec/xep-ca-tu-dong', null, { params: { tuNgay, denNgay } }))
  },

  async downloadTemplate() {
    const res = await api.get('/lich-lam-viec/template', { responseType: 'blob' })
    const url = URL.createObjectURL(new Blob([res.data]))
    const a   = document.createElement('a')
    a.href = url
    a.download = 'template-lich-lam-viec.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  },

  async importExcel(file) {
    const fd = new FormData()
    fd.append('file', file)
    return unwrap(await api.post('/lich-lam-viec/import-excel', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }))
  },

  async xuatExcelLichSu(params = {}) {
    const res = await api.get('/lich-lam-viec/xuat-excel-lich-su', {
      params,
      responseType: 'blob'
    })
    const tuNgay  = params.tuNgay  || ''
    const denNgay = params.denNgay || ''
    const filename = `lich-su-hoat-dong${tuNgay ? '-' + tuNgay : ''}${denNgay ? '-' + denNgay : ''}.xlsx`
    const url = URL.createObjectURL(new Blob([res.data]))
    const a = document.createElement('a')
    a.href = url; a.download = filename; a.click()
    URL.revokeObjectURL(url)
  }
}

export default scheduleService
