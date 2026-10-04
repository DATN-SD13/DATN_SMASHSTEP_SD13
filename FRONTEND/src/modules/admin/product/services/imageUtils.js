import api from '../../../../utils/api'

export const imageTypes = 'image/jpeg,image/png,image/webp'
export const maxImageSize = 5 * 1024 * 1024

export function validateImageFile(file) {
  if (!file || !file.size) return 'Tệp ảnh không được để trống.'
  if (!imageTypes.split(',').includes(file.type)) return 'Chỉ hỗ trợ ảnh JPG, PNG hoặc WEBP.'
  if (file.size > maxImageSize) return 'Ảnh không được vượt quá 5 MB.'
  return ''
}

export function imageUrl(value) {
  const url = typeof value === 'string' ? value.trim() : ''
  if (!url) return ''
  if (url.startsWith('/') && !url.startsWith('//')) {
    return /^https?:\/\//i.test(api.defaults.baseURL) ? new URL(url, api.defaults.baseURL).href : url
  }
  try {
    const parsed = new URL(url)
    return ['http:', 'https:'].includes(parsed.protocol) ? parsed.href : ''
  } catch { return '' }
}
