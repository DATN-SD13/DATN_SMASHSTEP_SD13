import api from '../../../../utils/api'

const unwrap = (response) => response?.data?.data

const employeeService = {
  async sendPasswordReset(email) { return unwrap(await api.post('/auth/quen-mat-khau', { email, userType: 'NHAN_VIEN' })) },
  async downloadImportTemplate() { return (await api.get('/nhan-vien/excel/mau', { responseType: 'blob' })).data },
  async previewImport(file) { const data = new FormData(); data.append('file', file); return unwrap(await api.post('/nhan-vien/excel/xem-truoc', data, { headers: { 'Content-Type': undefined }, timeout: 60000 })) },
  async confirmImport(payload) { return unwrap(await api.post('/nhan-vien/excel/xac-nhan', payload, { timeout: 120000 })) },
  async getImportResult(id) { return unwrap(await api.get(`/nhan-vien/excel/${encodeURIComponent(id)}`, { timeout: 60000 })) },
  async getImportHistory() { return unwrap(await api.get('/nhan-vien/excel')) },
  async retryImportEmails(id) { return unwrap(await api.post(`/nhan-vien/excel/${encodeURIComponent(id)}/gui-lai-email`, {}, { timeout: 60000 })) },

  async getEmployees(params = {}) {
    const response = await api.get('/nhan-vien', { params })
    return unwrap(response)
  },

  async getEmployee(code) {
    const response = await api.get(
      `/nhan-vien/${encodeURIComponent(code)}`
    )
    return unwrap(response)
  },

  async createEmployee(payload) {
    const response = await api.post('/nhan-vien', payload)
    return unwrap(response)
  },

  async updateEmployee(code, payload) {
    const response = await api.put(
      `/nhan-vien/${encodeURIComponent(code)}`,
      payload
    )
    return unwrap(response)
  },

  async updateStatus(code, status) {
    const response = await api.patch(
      `/nhan-vien/${encodeURIComponent(code)}/trang-thai`,
      { status }
    )
    return unwrap(response)
  },

  async getRoles() {
    const response = await api.get('/nhan-vien/vai-tro')
    return unwrap(response)
  }
}

export default employeeService
