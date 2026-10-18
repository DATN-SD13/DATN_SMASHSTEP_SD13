import api from './api'

export const avatarTypes = 'image/jpeg,image/png,image/webp'
export const avatarMaxSize = 5 * 1024 * 1024

export function avatarUrl(value) {
  if (typeof value !== 'string' || !value) return ''
  if (/^data:image\/(jpeg|png|webp);base64,/.test(value)) return value
  if (value.startsWith('/') && !value.startsWith('//')) {
    return /^https?:\/\//i.test(api.defaults.baseURL) ? new URL(value, api.defaults.baseURL).href : value
  }
  try { const url = new URL(value); return ['http:', 'https:'].includes(url.protocol) ? url.href : '' } catch { return '' }
}

export async function persistAvatar(value) {
  if (!value) return null
  if (!value.startsWith('data:')) return value
  const match = /^data:(image\/(?:jpeg|png|webp));base64,([A-Za-z0-9+/=]+)$/.exec(value)
  if (!match) throw new Error('Chỉ hỗ trợ ảnh JPG, PNG hoặc WEBP.')
  const content = atob(match[2])
  if (!content.length || content.length > avatarMaxSize) throw new Error('Ảnh phải có dung lượng từ 1 byte đến 5 MB.')
  const bytes = Uint8Array.from(content, char => char.charCodeAt(0))
  const form = new FormData()
  form.append('file', new Blob([bytes], { type: match[1] }), 'avatar')
  const response = await api.post('/uploads/avatar', form, { headers: { 'Content-Type': undefined }, timeout: 30000 })
  const url = response.data?.data?.url
  if (!response.data?.success || typeof url !== 'string' || !url.startsWith('/uploads/avatars/')) {
    throw new Error('Máy chủ chưa trả đường dẫn ảnh hợp lệ.')
  }
  return url
}
