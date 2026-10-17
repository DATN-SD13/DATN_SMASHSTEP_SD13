export function imageUrl(value) {
  if (typeof value !== 'string') return ''
  const url = value.trim()
  if (url.startsWith('/') && !url.startsWith('//')) return url
  try { const parsed = new URL(url); return ['http:', 'https:'].includes(parsed.protocol) && !parsed.username && !parsed.password ? parsed.href : '' } catch { return '' }
}
