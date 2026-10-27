import api from './api'

export const chatService = {
  current: () => api.get('/chat/customer/session'),
  startNew: () => api.post('/chat/customer/session/new'),
  send: content => api.post('/chat/customer/message', { content }),
  requestStaff: () => api.post('/chat/customer/request-staff')
}
