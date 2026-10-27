import api from '../../../../utils/api'

const unwrap = response => response?.data?.data ?? response?.data

export default {
  async sessions(includeClosed = false) {
    return unwrap(await api.get('/chat/staff/sessions', { params: { includeClosed } })) || []
  },
  async detail(id) {
    return unwrap(await api.get(`/chat/staff/sessions/${id}`))
  },
  async accept(id) {
    return unwrap(await api.post(`/chat/staff/sessions/${id}/accept`))
  },
  async send(id, content) {
    return unwrap(await api.post(`/chat/staff/sessions/${id}/message`, { content }))
  },
  async close(id) {
    return unwrap(await api.post(`/chat/staff/sessions/${id}/close`))
  }
}
