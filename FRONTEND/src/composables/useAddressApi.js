import { computed, ref, watch, onMounted, onBeforeUnmount } from 'vue'
import axios from 'axios'

const api = axios.create({ baseURL: 'https://provinces.open-api.vn/api/v2', timeout: 15000 })
let provinceCache
const wardCache = new Map()
const normalize = value => String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '')
  .replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase()
  .replace(/^(thanh pho|tinh|tp\.?|phuong|xa|thi tran)\s+/, '').trim()
function checkedList(data) {
  if (!Array.isArray(data) || !data.length || data.some(item => !item?.name || item.code == null)) {
    throw new Error('Dữ liệu địa chỉ không hợp lệ')
  }
  return data
}
async function fetchProvinces() {
  if (!provinceCache) provinceCache = checkedList((await api.get('/p/')).data)
  return provinceCache
}
async function fetchWards(code) {
  const key = String(code)
  if (!wardCache.has(key)) {
    const { data } = await api.get('/w/', { params: { province: code } })
    const wards = checkedList(data)
    if (wards.some(ward => String(ward.province_code) !== key)) throw new Error('Phường/xã không thuộc tỉnh đã chọn')
    wardCache.set(key, wards)
  }
  return wardCache.get(key)
}
// Keep saved legacy names until the user chooses a new address. They are data, not fallback API lists.
function withStoredValue(items, name) {
  return name && !items.some(item => item.name === name)
    ? [{ code: `stored:${name}`, name }, ...items] : items
}
export function useAddressApi(form) {
  const provinceItems = ref([]), wardItems = ref([])
  const provinces = computed(() => withStoredValue(provinceItems.value, form.value.province))
  const wards = computed(() => withStoredValue(wardItems.value, form.value.ward))
  const loadingProvinces = ref(false), loadingWards = ref(false), addressError = ref('')
  let requestId = 0, disposed = false
  async function restoreAddress() {
    const current = ++requestId
    const name = form.value.province
    wardItems.value = []
    const province = provinceItems.value.find(p => normalize(p.name) === normalize(name))
    if (!province || !name) { loadingWards.value = false; return }
    loadingWards.value = true
    addressError.value = ''
    try {
      const result = await fetchWards(province.code)
      if (disposed || current !== requestId || form.value.province !== name) return
      wardItems.value = result
    } catch (error) {
      if (!disposed && current === requestId) addressError.value = 'Không tải được danh sách phường/xã. Vui lòng thử lại.'
    } finally {
      if (!disposed && current === requestId) loadingWards.value = false
    }
  }
  async function loadProvinces() {
    loadingProvinces.value = true
    addressError.value = ''
    try {
      const result = await fetchProvinces()
      if (disposed) return
      provinceItems.value = result
      await restoreAddress()
    } catch (error) {
      if (!disposed) addressError.value = 'Không tải được danh sách tỉnh/thành. Vui lòng thử lại.'
    } finally {
      if (!disposed) loadingProvinces.value = false
    }
  }
  function changeProvince() {
    form.value.ward = ''
    form.value.district = ''
    restoreAddress()
  }
  function resetAddress() {
    requestId++
    wardItems.value = []
    loadingWards.value = false
    addressError.value = ''
  }
  watch(() => form.value.province, restoreAddress)
  onMounted(loadProvinces)
  onBeforeUnmount(() => { disposed = true; requestId++ })
  return { provinces, wards, loadingProvinces, loadingWards, addressError, changeProvince, restoreAddress, resetAddress, loadProvinces }
}