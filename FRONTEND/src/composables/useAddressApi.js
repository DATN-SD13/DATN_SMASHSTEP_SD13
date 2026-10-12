import { ref, watch, onMounted } from 'vue'
import axios from 'axios'

const api = axios.create({
  baseURL: 'https://provinces.open-api.vn/api/v2',
  timeout: 15000
})

// Cache để không gọi API nhiều lần
let provinceCache = null
const wardCache = new Map()

async function fetchProvinces() {
  if (provinceCache) return provinceCache

  const { data } = await api.get('/p/')

  provinceCache = Array.isArray(data) ? data : []

  return provinceCache
}

async function fetchWards(provinceCode) {
  if (!provinceCode) return []

  const key = String(provinceCode)

  if (wardCache.has(key)) {
    return wardCache.get(key)
  }

  const { data } = await api.get('/w/', {
    params: {
      province: provinceCode
    }
  })

  const result = Array.isArray(data) ? data : []

  wardCache.set(key, result)

  return result
}

export function useAddressApi(form) {
  const provinces = ref([])
  const wards = ref([])

  const loadingProvinces = ref(false)
  const loadingWards = ref(false)

  const addressError = ref('')

  // Dùng tên tỉnh/phường để tương thích backend
  const provinceCode = ref('')
  const wardCode = ref('')

  let requestId = 0

  async function loadProvinces() {
    loadingProvinces.value = true
    addressError.value = ''

    try {
      provinces.value = await fetchProvinces()

      // Khi sửa dữ liệu cũ
      await restoreAddress()
    } catch (error) {
      console.error('Lỗi tải tỉnh/thành:', error)

      addressError.value =
        'Không tải được danh sách tỉnh/thành.'
    } finally {
      loadingProvinces.value = false
    }
  }

  async function loadWards(code) {
    const currentRequest = ++requestId

    wards.value = []

    if (!code) {
      loadingWards.value = false
      return
    }

    loadingWards.value = true

    try {
      const result = await fetchWards(code)

      // Tránh API cũ ghi đè dữ liệu tỉnh mới
      if (currentRequest !== requestId) return

      wards.value = result
    } catch (error) {
      if (currentRequest !== requestId) return

      console.error('Lỗi tải phường/xã:', error)

      addressError.value =
        'Không tải được danh sách phường/xã.'
    } finally {
      if (currentRequest === requestId) {
        loadingWards.value = false
      }
    }
  }

  async function changeProvince() {
    const province = provinces.value.find(
      p => String(p.code) === String(provinceCode.value)
    )

    form.value.province = province?.name || ''
    form.value.ward = ''

    wardCode.value = ''

    await loadWards(provinceCode.value)
  }

  function changeWard() {
    const ward = wards.value.find(
      w => String(w.code) === String(wardCode.value)
    )

    form.value.ward = ward?.name || ''
  }

  async function restoreAddress() {
    if (!provinces.value.length) return

    const province = provinces.value.find(
      p => p.name === form.value.province
    )

    if (!province) {
      provinceCode.value = ''
      wardCode.value = ''
      wards.value = []
      return
    }

    provinceCode.value = String(province.code)

    await loadWards(province.code)

    const ward = wards.value.find(
      w => w.name === form.value.ward
    )

    wardCode.value = ward
      ? String(ward.code)
      : ''
  }

  function resetAddress() {
    requestId++

    provinceCode.value = ''
    wardCode.value = ''

    wards.value = []
  }

  onMounted(loadProvinces)

  return {
    provinces,
    wards,

    provinceCode,
    wardCode,

    loadingProvinces,
    loadingWards,

    addressError,

    loadProvinces,
    changeProvince,
    changeWard,
    restoreAddress,
    resetAddress
  }
}