import api from '../../../../utils/api'

const unwrap = (response) => response?.data?.data

const employeeService = {
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