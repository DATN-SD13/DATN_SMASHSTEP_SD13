import api from '../../../../utils/api'

const unwrap = r => r?.data?.data

const scheduleService = {
  async capabilities() { return unwrap(await api.get('/lich-lam-viec/capabilities')) },
  // ── Ca làm việc ──────────────────────────────────────────
  async getCaLamViec() {
    return unwrap(await api.get('/ca-lam-viec'))
  },

  async getCaLamViecActive() {
    return unwrap(await api.get('/ca-lam-viec/active'))
  },

  // ── Lịch làm việc ────────────────────────────────────────
  async getLichLamViec(params = {}) {
    return unwrap(await api.get('/lich-lam-viec', { params }))
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
  }
}

export default scheduleService
