import axios from 'axios'
const api = axios.create({ baseURL: '/api', timeout: 30000, paramsSerializer(params) { const query = new URLSearchParams(); for (const [key, value] of Object.entries(params || {})) for (const entry of Array.isArray(value) ? value : [value]) if (entry !== '' && entry !== null && entry !== undefined) query.append(key, entry); return query.toString() } })
api.interceptors.response.use(response => { if (response.data?.success === false) return Promise.reject(new Error('Không thể tải dữ liệu.')); return response.data?.data ?? response.data })
export default api
