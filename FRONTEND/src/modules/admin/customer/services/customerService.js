import api from '../../../../utils/api'

const unwrap = (response) => response?.data?.data

const customerService = {

  async getCustomers(params = {}) {
    const response = await api.get(
      '/khach-hang',
      { params }
    )

    return unwrap(response)
  },

  async getCustomer(code) {
    const response = await api.get(
      `/khach-hang/${encodeURIComponent(code)}`
    )

    return unwrap(response)
  },

  async createCustomer(payload) {
    const response = await api.post(
      '/khach-hang',
      payload
    )

    return unwrap(response)
  },

  async updateCustomer(code, payload) {
    const response = await api.put(
      `/khach-hang/${encodeURIComponent(code)}`,
      payload
    )

    return unwrap(response)
  },

  async updateStatus(code, status) {
    const response = await api.patch(
      `/khach-hang/${encodeURIComponent(code)}/trang-thai`,
      { status }
    )

    return unwrap(response)
  },

  async getAddresses(code) {
    const response = await api.get(
      `/khach-hang/${encodeURIComponent(code)}/dia-chi`
    )

    return unwrap(response)
  },

  async createAddress(code, payload) {
    const response = await api.post(
      `/khach-hang/${encodeURIComponent(code)}/dia-chi`,
      payload
    )

    return unwrap(response)
  },

  async updateAddress(code, id, payload) {
    const response = await api.put(
      `/khach-hang/${encodeURIComponent(code)}/dia-chi/${id}`,
      payload
    )

    return unwrap(response)
  },

  async setDefaultAddress(code, id) {
    const response = await api.patch(
      `/khach-hang/${encodeURIComponent(code)}/dia-chi/${id}/mac-dinh`
    )

    return unwrap(response)
  },

  async stopAddress(code, id) {
    const response = await api.patch(
      `/khach-hang/${encodeURIComponent(code)}/dia-chi/${id}/ngung-hoat-dong`
    )

    return unwrap(response)
  }
}

export default customerService