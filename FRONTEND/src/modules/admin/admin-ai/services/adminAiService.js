import api from '../../../../utils/api'

function unwrap(response) {
  const body = response?.data
  if (!body?.success) throw new Error(body?.message || 'Không thể lấy dữ liệu trợ lý quản trị')
  return body.data
}

export const adminAiService = {
  async ask(message) {
    return unwrap(await api.post('/admin-ai/ask', { message }))
  }
}
