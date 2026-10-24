<script setup>
import GhnLogo from '../../../../components/GhnLogo.vue'
import { useProductToast } from '../../product/composables/useProductToast'
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { invoiceService } from '../services/invoiceService'
import api from '../../../../utils/api'
import { imageUrl } from '../../product/services/imageUtils'
import Select2Field from '../components/Select2Field.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const invoice = ref(null)
const loadError = ref('')
const selectedStatus = ref('')
const failureReason = ref('')
const latestDeliveryFailure = computed(() => invoice.value?.history?.find(row => Number(row.maTrangThai) === 9))
function deliveryFailureReason(note) {
  return String(note || '').replace(/^Giao hàng thất bại(?: lần \d+\/3)?:\s*/i, '').trim() || 'Chưa ghi nhận lý do.'
}
const returnDialog = ref(null)
const refundReferenceError = ref('')
const refundReferenceTouched = ref(false)
function refundReferenceMessage(value) {
  const raw = String(value ?? '')
  if (/[\p{Cc}\p{Cf}\p{Zl}\p{Zp}]/u.test(raw)) return 'Không được chứa xuống dòng hoặc ký tự điều khiển.'
  const normalized = raw.trim()
  if (!normalized) return 'Nhập mã giao dịch hoặc số chứng từ hoàn tiền.'
  if (normalized.length > 100) return 'Mã giao dịch/chứng từ tối đa 100 ký tự.'
  return ''
}
function validateRefundReference(normalize = false) {
  refundReferenceTouched.value = true
  refundReferenceError.value = refundReferenceMessage(returnDialog.value?.maHoanTien)
  if (!refundReferenceError.value && normalize && returnDialog.value) returnDialog.value.maHoanTien = returnDialog.value.maHoanTien.trim()
  return !refundReferenceError.value
}
function inputRefundReference() {
  if (refundReferenceTouched.value) validateRefundReference()
}
function pasteRefundReference(event) {
  const pasted = event.clipboardData?.getData('text')
  if (pasted == null) return
  const input = event.target, current = String(returnDialog.value?.maHoanTien || '')
  const candidate = current.slice(0, input.selectionStart ?? current.length) + pasted + current.slice(input.selectionEnd ?? current.length)
  const error = refundReferenceMessage(candidate)
  if (error) { event.preventDefault(); refundReferenceTouched.value = true; refundReferenceError.value = error }
}

const returnDialogTitle = computed(() => ({ HANG_TRA: 'Xử lý hàng trả', HOAN_TIEN: 'Xác nhận hoàn tiền', HUY: 'Hủy hóa đơn' }[returnDialog.value?.thaoTac] || 'Xử lý đơn hàng'))
const returnButtonLabel = computed(() => ({ HANG_TRA: 'Lưu xử lý hàng trả', HOAN_TIEN: 'Xác nhận đã hoàn tiền', HUY: 'Xác nhận hủy hóa đơn' }[returnDialog.value?.thaoTac] || 'Xác nhận'))
const failureCount = computed(() => Number(invoice.value?.failureCount || 0))
const canRetryDelivery = computed(() => invoice.value?.statusCode === 9 && !invoice.value?.goodsHandled && invoice.value?.paymentStatusCode !== 2 && failureCount.value < 3)
const canFailDelivery = computed(() => invoice.value?.statusCode === 3 && invoice.value?.receiveMethodCode === 1 && failureCount.value < 3)
async function resolveFailure() {
  const form = returnDialog.value
  if (!form || updatingStatus.value) return
  if (form.thaoTac === 'HOAN_TIEN' && !validateRefundReference(true)) return
  if ((form.thaoTac === 'HANG_TRA' && !form.daNhanLaiHang) || !form.ghiChu.trim()) {
    showLocalToast('error', 'Chưa đủ thông tin', 'Nhập ghi chú xử lý và xác nhận đã nhận hàng nếu đang xử lý hàng trả.'); return
  }
  if (form.thaoTac === 'HOAN_TIEN' && Number(invoice.value.refundDue) > 0 && (!form.daHoanTien || !form.maHoanTien.trim())) {
    showLocalToast('error', 'Chưa xác nhận hoàn tiền', 'Hoàn tiền thực tế và nhập mã/chứng từ hoàn tiền trước khi hủy.'); return
  }
  updatingStatus.value = true
  try {
    const response = await invoiceService.resolveFailedDelivery(invoice.value.code, { ...form, soTienHoan: invoice.value.refundDue, soLanThatBai: failureCount.value })
    if (!response?.success) throw new Error(response?.message || 'Chưa xử lý được hoàn/hủy')
    invoice.value = mapInvoice(response.data)
    returnDialog.value = null
    await loadProductImages()
    showLocalToast('success', 'Đã lưu', { HANG_TRA: 'Đã xử lý hàng trả và chuyển hóa đơn sang bước Hoàn tiền.', HOAN_TIEN: 'Đã ghi nhận khoản hoàn tiền. Bạn có thể hủy hóa đơn.', HUY: 'Đã hủy hóa đơn.' }[form.thaoTac])
  } catch (e) { showLocalToast('error', 'Chưa xử lý được', e?.response?.data?.message || e.message) }
  finally { updatingStatus.value = false }
}
function openReturnDialog(thaoTac = 'HANG_TRA') {
  refundReferenceError.value = ''; refundReferenceTouched.value = false
  returnDialog.value = { thaoTac, daNhanLaiHang: false, nhapLaiKho: false, daHoanTien: false, maHoanTien: '', ghiChu: '' }
}
const showEditOrderModal = ref(false)
const editOrderTab = ref('order')
const showHistoryModal = ref(false)
const updatingStatus = ref(false)
const loadingHistory = ref(false)
const historyItems = ref([])
const productSearch = ref('')
const productColor = ref('')
const productSize = ref('')
const productCategory = ref('')
const productSort = ref('default')
const showConfirmModal = ref(false)
const confirmSubmitting = ref(false)
const showCancelModal = ref(false)
const { show: showToast } = useProductToast()
let invoiceLoadVersion = 0

const deliverySteps = [
  { key: 'waiting', label: 'Chờ xác nhận', icon: 'bi-hourglass-split', value: 0 },
  { key: 'confirmed', label: 'Đã xác nhận', icon: 'bi-check2-circle', value: 1 },
  { key: 'ready', label: 'Chờ lấy hàng', icon: 'bi-box-seam', value: 2 },
  { key: 'shipping', label: 'Đang giao hàng', icon: 'bi-truck', value: 3 },
  { key: 'delivered', label: 'Đã giao hàng', icon: 'bi-bag-check', value: 4 },
  { key: 'done', label: 'Hoàn thành', icon: 'bi-flag', value: 5 }
]

const pickupSteps = [
  { key: 'waiting', label: 'Chờ xác nhận', icon: 'bi-hourglass-split', value: 0 },
  { key: 'confirmed', label: 'Đã xác nhận', icon: 'bi-check2-circle', value: 1 },
  { key: 'done', label: 'Hoàn thành', icon: 'bi-flag', value: 5 }
]

const steps = computed(() => {
  if (invoice.value?.posManaged) {
    const pending = { key: 'pending', label: 'Hóa đơn chờ', icon: 'bi-hourglass-split', value: 8 }
    return invoice.value.receiveMethodCode === 1 ? [pending, ...deliverySteps] : [pending, pickupSteps.at(-1)]
  }
  if (invoice.value?.statusCode === 8 || invoice.value?.history?.some(item => Number(item.maTrangThai) === 8)) {
    return [{ key: 'pending', label: 'Hóa đơn chờ', icon: 'bi-hourglass-split', value: 8 }, pickupSteps.at(-1)]
  }
  return invoice.value?.receiveMethodCode === 0 ? pickupSteps : deliverySteps
})
const statusOptions = computed(() => steps.value.map(step => ({
  value: step.value, key: step.key, label: step.label
})))

const progress = computed(() => {
  const code = invoice.value?.statusCode
  return steps.value.findIndex(step => step.value === code)
})

// Chỉ hiển thị các bước đã đi qua, bước hiện tại và tối đa một bước kế tiếp.
const visibleSteps = computed(() => {
  if (progress.value < 0) return []
  return steps.value.slice(0, Math.min(progress.value + 2, steps.value.length))
})

// Đơn đã hủy có nhánh kết thúc riêng; không đưa mốc chưa xảy ra vào lịch sử.
const cancelledSteps = computed(() => {
  const current = invoice.value
  if (![6, 7].includes(current?.statusCode)) return []
  const recorded = new Set((current.history || [])
    .filter(row => row.maTrangThai != null).map(row => Number(row.maTrangThai)))
  // Lịch sử kỹ thuật POS được BE ẩn; ngày tạo vẫn là mốc hóa đơn chờ đã có thật.
  if (current.posManaged) recorded.add(8)
  if (current.paymentHistory?.some(row => row.statusCode === 2)) recorded.add(7)
  recorded.add(current.statusCode)
  return [
    { key: 'pending', label: 'Hóa đơn chờ', icon: 'bi-hourglass-split', value: 8 },
    ...deliverySteps.filter(step => step.value <= 3),
    { key: 'failed', label: 'Giao hàng thất bại', icon: 'bi-exclamation-circle', value: 9 },
    { key: 'refund', label: 'Hoàn tiền', icon: 'bi-arrow-return-left', value: 7 },
    { key: 'cancel', label: 'Đã hủy', icon: 'bi-x-circle', value: 6 }
  ].filter(step => recorded.has(step.value) && (step.value !== 6 || current.statusCode === 6))
})
function cancelledStepDate(status) {
  const recordedDate = stepDate(status)
  if (recordedDate !== '—') return recordedDate
  if (status === 7) return invoice.value?.paymentHistory?.find(row => row.statusCode === 2)?.time || '—'
  if (status === 6) return invoice.value?.updatedDate || '—'
  return '—'
}

const nextStatus = computed(() => {
  if (!invoice.value?.nextStatusCode && invoice.value?.nextStatusCode !== 0) return null
  return {
    value: Number(invoice.value.nextStatusCode),
    label: invoice.value.nextStatusLabel || 'Trạng thái kế tiếp'
  }
})

const canUpdateStatus = computed(() => !(invoice.value?.statusCode === 7 || (invoice.value?.statusCode === 9 && (invoice.value?.goodsHandled || invoice.value?.paymentStatusCode === 2))) && (Boolean(nextStatus.value) || canFailDelivery.value || canRetryDelivery.value))

const canCancelOrder = computed(() => {
  if (invoice.value?.posManaged && invoice.value?.paymentStatusCode === 1) return false
  const code = invoice.value?.statusCode
  if (!Number.isInteger(code)) return false
  return invoice.value?.receiveMethodCode === 0 ? (code === 8 || code <= 1) : code <= 4
})

const canPrintInvoice = computed(() => {
  const status = invoice.value?.statusCode
  return status === 0 || status === 1 || status === 5
})

const paymentHistory = computed(() => {
  return Array.isArray(invoice.value?.paymentHistory) ? invoice.value.paymentHistory : []
})

const isDeliveryOrder = computed(() => invoice.value?.receiveMethodCode === 1)

const hasShippingInfo = computed(() => {
  const current = invoice.value
  if (!current) return false
  if (current.receiveMethodCode !== 1) return false
  return Boolean(
    current.shippingCarrier ||
    current.shippingRecipient ||
    current.shippingRecipientPhone ||
    current.shippingAddress ||
    current.shippingNote ||
    Number(current.shippingFee || 0) !== 0
  )
})

const productCategories = computed(() => {
  const values = (invoice.value?.items || []).map(item => item.category).filter(Boolean)
  return [...new Set(values)].sort((a, b) => a.localeCompare(b, 'vi'))
})

const productColors = computed(() => {
  const values = (invoice.value?.items || []).map(item => item.color).filter(Boolean)
  return [...new Set(values)].sort((a, b) => a.localeCompare(b, 'vi'))
})

const productSizes = computed(() => {
  const values = (invoice.value?.items || []).map(item => item.size).filter(Boolean)
  return [...new Set(values)].sort((a, b) => a.localeCompare(b, 'vi', { numeric: true }))
})

const productCategoryOptions = computed(() => [
  { value: '', label: 'Tất cả loại' },
  ...productCategories.value.map(value => ({ value, label: value }))
])

const productColorOptions = computed(() => [
  { value: '', label: 'Tất cả màu sắc' },
  ...productColors.value.map(value => ({ value, label: value }))
])

const productSizeOptions = computed(() => [
  { value: '', label: 'Tất cả kích cỡ' },
  ...productSizes.value.map(value => ({ value, label: value }))
])

const productSortOptions = [
  { value: 'default', label: 'Mặc định' },
  { value: 'name-asc', label: 'Tên A → Z' },
  { value: 'price-asc', label: 'Giá thấp → cao' },
  { value: 'price-desc', label: 'Giá cao → thấp' }
]

const filteredItems = computed(() => {
  let items = [...(invoice.value?.items || [])]
  const keyword = productSearch.value.trim().toLowerCase()

  if (keyword) {
    items = items.filter(item =>
      [item.productDetailCode, item.sku, item.name, item.category, item.brand, item.color, item.size]
        .some(value => String(value || '').toLowerCase().includes(keyword))
    )
  }
  if (productCategory.value) items = items.filter(item => item.category === productCategory.value)
  if (productColor.value) items = items.filter(item => item.color === productColor.value)
  if (productSize.value) items = items.filter(item => item.size === productSize.value)

  if (productSort.value === 'price-asc') items.sort((a, b) => a.price - b.price)
  else if (productSort.value === 'price-desc') items.sort((a, b) => b.price - a.price)
  else if (productSort.value === 'name-asc') items.sort((a, b) => a.name.localeCompare(b.name, 'vi'))

  return items
})

const productFilterActive = computed(() =>
  Boolean(productSearch.value || productCategory.value || productColor.value || productSize.value || productSort.value !== 'default')
)

function resetProductFilters() {
  productSearch.value = ''
  productCategory.value = ''
  productColor.value = ''
  productSize.value = ''
  productSort.value = 'default'
}

const subtotal = computed(() => Number(invoice.value?.totalBeforeAdjustments ?? 0))

function money(value) {
  return `${Number(value || 0).toLocaleString('vi-VN')} đ`
}

const hasVoucher = computed(() => Boolean(invoice.value?.voucherCode))

const voucherLabel = computed(() => {
  if (!invoice.value?.voucherCode || !invoice.value?.voucherSnapshot) return ''
  if (Number(invoice.value?.voucherType) === 1 && Number(invoice.value?.voucherValue) > 0) {
    return `${Number(invoice.value.voucherValue).toLocaleString('vi-VN')}%`
  }
  return Number(invoice.value?.voucherValue) > 0 ? money(invoice.value.voucherValue) : ''
})

const isGHN = computed(() => String(invoice.value?.shippingCarrier || '').trim().toUpperCase() === 'GHN' || String(invoice.value?.shippingCarrier || '').toUpperCase().includes('GIAO HÀNG NHANH'))

const qrCodeUrl = computed(() => {
  const code = invoice.value?.code || route.params.code || ''
  const payload = `SMASHSTEP|${code}`
  return `https://quickchart.io/qr?text=${encodeURIComponent(payload)}&size=140&margin=1`
})

function formatDate(value) {
  if (!value) return '—'
  return String(value)
}

function formatHistoryDate(value) {
  return value || '—'
}

function mapInvoice(data) {
  const statusCode = data?.maTrangThai == null ? null : Number(data.maTrangThai)
  const status = (statusOptions.value || []).find(item => item.value === statusCode)

  return {
    goodsHandled: data?.daXuLyHangTra === true,
    failureCount: Number(data?.soLanGiaoThatBai || 0),
    refundDue: data?.soTienCanHoan == null ? null : Number(data.soTienCanHoan),
    reconciliationError: data?.loiDoiSoat || '',
    id: data?.id,
    code: data?.maHoaDon || route.params.code,
    employee: data?.tenNhanVien || 'Chưa cập nhật',
    creator: Number(data?.maLoaiHoaDon) === 1 ? 'Khách hàng' : (data?.tenNguoiTao || data?.maNhanVien || data?.tenNhanVien || 'Chưa cập nhật'),
    employeeId: data?.idNhanVien,
    employeeCode: data?.maNhanVien || '—',
    customer: data?.tenKhachHang || data?.hoTenNguoiNhan || 'Khách lẻ',
    customerCode: data?.maKhachHang || '—',
    phone: data?.soDienThoai || '—',
    email: data?.email || data?.emailKhachHang || data?.emailNguoiNhan || '—',
    recipient: data?.hoTenNguoiNhan || data?.tenKhachHang || '—',
    recipientPhone: data?.soDienThoaiNguoiNhan || data?.soDienThoai || '—',
    orderType: data?.loaiHoaDon || (Number(data?.maLoaiHoaDon) === 1 ? 'Trực tuyến' : 'Tại quầy'),
    orderTypeCode: data?.maLoaiHoaDon == null ? null : Number(data.maLoaiHoaDon),
    receiveMethod: data?.hinhThucNhan || (Number(data?.maHinhThucNhan) === 0 ? 'Nhận tại quầy' : 'Giao hàng'),
    receiveMethodCode: data?.maHinhThucNhan == null ? (Number(data?.maLoaiHoaDon) === 0 ? 0 : 1) : Number(data.maHinhThucNhan),
    shippingRecipient: data?.hoTenNguoiNhan || '',
    shippingRecipientPhone: data?.soDienThoaiNguoiNhan || '',
    shippingAddress: data?.diaChiGiaoHang || '',
    shippingNote: data?.ghiChu || '',
    date: formatDate(data?.ngayTao),
    rawDate: data?.ngayTao,
    updatedDate: formatDate(data?.ngayCapNhat),
    totalBeforeAdjustments: Number(data?.tongTien ?? 0),
    total: Number(data?.thanhTien ?? 0),
    discount: Number(data?.tienGiamGia ?? 0),
    voucherCode: data?.maPhieuGiamGia || '',
    voucherSnapshot: data?.voucherSnapshot === true,
    voucherType: data?.loaiGiamGia == null ? null : Number(data.loaiGiamGia),
    voucherValue: Number(data?.giaTriGiam ?? 0),
    shippingFee: Number(data?.phiVanChuyen ?? 0),
    status: data?.trangThai || status?.label || 'Không xác định',
    statusClass: data?.lopTrangThai || status?.key || 'unknown',
    statusCode: statusCode,
    posManaged: data?.posManaged === true,
    nextStatusCode: data?.maTrangThaiTiepTheo,
    nextStatusLabel: data?.trangThaiTiepTheo,
    payment: data?.phuongThucThanhToan || 'Chưa cập nhật',
    paymentMethodCode: data?.maPhuongThucThanhToan || '',
    paymentStatus: data?.trangThaiThanhToan || 'Chưa cập nhật',
    paymentStatusCode: Number.isFinite(Number(data?.maTrangThaiThanhToan)) ? Number(data?.maTrangThaiThanhToan) : null,
    paymentDate: data?.ngayThanhToan || null,
    paymentHistory: (data?.lichSuThanhToan || []).map(item => ({
      id: item?.id,
      method: item?.phuongThucThanhToan || data?.phuongThucThanhToan || 'Chưa cập nhật',
      status: item?.trangThaiThanhToan || item?.trangThai || 'Chưa cập nhật',
      statusCode: item?.maTrangThai == null ? null : Number(item.maTrangThai),
      time: item?.thoiGian || null,
      amount: Number(item?.soTien ?? 0),
      transactionCode: item?.maGiaoDich || null,
    })),
    address: data?.diaChiGiaoHang || data?.diaChi || 'Chưa cập nhật',
    shippingCarrier: data?.donViVanChuyen || '',
    note: data?.ghiChu || '—',
    history: Array.isArray(data?.lichSuHoaDon) ? data.lichSuHoaDon : [],
    items: (data?.chiTietHoaDon || []).map(item => ({
      id: item?.id,
      productDetailId: item?.idSanPhamChiTiet,
      productDetailCode: item?.maSanPhamChiTiet || '—',
      sku: item?.maSku || '—',
      name: item?.tenSanPham || 'Sản phẩm',
      category: item?.tenDanhMuc || '',
      brand: item?.tenThuongHieu || '',
      material: item?.tenChatLieu || '',
      style: item?.tenKieuDang || '',
      collar: item?.tenCoGiay || '',
      origin: item?.tenXuatXu || '',
      color: item?.mauSac || '—',
      size: item?.kichThuoc || '—',
      variant: [item?.mauSac, item?.kichThuoc].filter(Boolean).join(' / ') || '—',
      quantity: Number(item?.soLuong || 0),
      price: Number(item?.donGia || 0),
      originalPrice: item?.giaGoc == null ? null : Number(item.giaGoc),
      discountPercent: item?.phanTramGiam == null ? null : Number(item.phanTramGiam),
      lineTotal: Number(item?.thanhTien ?? (Number(item?.donGia || 0) * Number(item?.soLuong || 0)))
    }))
  }
}

async function loadProductImages(target = invoice.value) {
  if (!target?.items?.length) return

  const results = await Promise.all(
    target.items.map(async item => {
      if (!item.productDetailId) return { id: item.id, url: '' }
      try {
        const response = await api.get(`/product-details/${item.productDetailId}`)
        return { id: item.id, url: response?.data?.anhChinh || '' }
      } catch {
        return { id: item.id, url: '' }
      }
    })
  )

  const imageMap = new Map(results.map(item => [item.id, item.url]))
  if (invoice.value !== target) return
  target.items = target.items.map(item => ({
    ...item,
    image: imageMap.get(item.id) || ''
  }))
}

async function loadInvoice() {
  const version = ++invoiceLoadVersion
  const code = route.params.code
  loading.value = true
  invoice.value = null
  loadError.value = ''
  try {
    const response = await invoiceService.getById(code)
    if (version !== invoiceLoadVersion) return
    if (!response?.success || !response?.data) {
      throw new Error(response?.message || 'Không thể tải chi tiết hóa đơn')
    }
    invoice.value = mapInvoice(response.data)
    selectedStatus.value = Number(invoice.value.statusCode)
    await loadProductImages()
  } catch (error) {
    if (version !== invoiceLoadVersion) return
    invoice.value = null
    loadError.value =
      error?.response?.data?.message ||
      error?.message ||
      'Không thể tải chi tiết hóa đơn.'

    showLocalToast(
      'error',
      'Không thể tải dữ liệu',
      loadError.value
    )
  } finally {
    if (version === invoiceLoadVersion) loading.value = false
  }
}

function openEditOrderModal() {
  if (!invoice.value || updatingStatus.value || confirmSubmitting.value) return
  editOrderTab.value = 'order'
  failureReason.value = ''
  selectedStatus.value = Number.isInteger(Number(invoice.value.statusCode))
    ? Number(invoice.value.statusCode)
    : ''
  showEditOrderModal.value = true
}

function closeEditOrderModal() {
  if (updatingStatus.value) return
  showEditOrderModal.value = false
  selectedStatus.value = ''
  editOrderTab.value = 'order'
}

const editableStatusOptions = computed(() => {
  if (!invoice.value) return []
  const currentCode = invoice.value.statusCode
  const options = []
  const current = statusOptions.value.find(item => item.value === currentCode)
  if (current) options.push(current)
  else options.push({ value: currentCode, label: invoice.value.status })
  if (canFailDelivery.value) options.push({ value: 9, key: 'failed', label: 'Giao hàng thất bại' })
  if (nextStatus.value && nextStatus.value.value !== currentCode) {
    const next = statusOptions.value.find(item => item.value === nextStatus.value.value) || {
      value: nextStatus.value.value,
      key: 'next',
      label: nextStatus.value.label
    }
    options.push(next)
  }
  return options
})

function openCancelModal() {
  if (!canCancelOrder.value || confirmSubmitting.value || updatingStatus.value) return
  showCancelModal.value = true
}

function closeCancelModal() {
  if (confirmSubmitting.value) return
  showCancelModal.value = false
}

// async function cancelOrder() {
//   if (!canCancelOrder.value || confirmSubmitting.value || updatingStatus.value) return

//   confirmSubmitting.value = true
//   try {
//     const response = await invoiceService.updateStatus(invoice.value.code, {
//       trangThai: 6,
//       ghiChu: 'Hủy đơn hàng'
//     })

//     if (!response?.success) {
//       throw new Error(response?.message || 'Không thể hủy đơn hàng')
//     }

//     if (response.data) {
//       invoice.value = mapInvoice(response.data)
//     await loadProductImages()
//     } else {
//       await loadInvoice()
//     }

//     showCancelModal.value = false
//     showLocalToast('success', 'Hủy đơn hàng thành công', response.message || 'Đơn hàng đã được chuyển sang trạng thái Đã hủy.')
//   } catch (error) {
//     showCancelModal.value = false
//     showLocalToast(
//       'error',
//       'Hủy đơn hàng thất bại',
//       error?.response?.data?.message || error?.message || 'Không thể hủy đơn hàng.'
//     )
//   } finally {
//     confirmSubmitting.value = false
//   }
// }
function showLocalToast(type, title, message) { showToast(type, message, title) }

async function saveStatus() {
  if (updatingStatus.value || confirmSubmitting.value || !invoice.value) return
  const newCode = Number(selectedStatus.value)
  const currentCode = Number(invoice.value?.statusCode)

  if (selectedStatus.value === '' || selectedStatus.value == null || !Number.isInteger(newCode)) {
    showLocalToast('error', 'Cập nhật thất bại', 'Vui lòng chọn trạng thái hợp lệ.')
    return
  }

  if (newCode === currentCode) {
    showLocalToast('error', 'Chưa có thay đổi', 'Vui lòng chọn trạng thái kế tiếp trước khi lưu.')
    return
  }

  if (newCode === 9 && !failureReason.value.trim()) {
    showLocalToast('error', 'Thiếu lý do', 'Nhập lý do giao hàng thất bại.'); return
  }
  if (!(canFailDelivery.value && newCode === 9) && (!nextStatus.value || newCode !== Number(nextStatus.value.value))) {
    showLocalToast(
      'error',
      'Cập nhật thất bại',
      'Chỉ được chuyển sang trạng thái kế tiếp theo đúng quy trình của đơn hàng.'
    )
    return
  }

  updatingStatus.value = true
  try {
    const response = await invoiceService.updateStatus(invoice.value.code, {
      trangThai: newCode,
      soLanThatBai: failureCount.value,
      ghiChu: newCode === 9 ? failureReason.value.trim() : undefined
    })

    if (!response?.success) {
      throw new Error(response?.message || 'Không thể cập nhật trạng thái hóa đơn')
    }

    if (response.data) {
      invoice.value = mapInvoice(response.data)
      await loadProductImages()
    } else {
      await loadInvoice()
    }

    showEditOrderModal.value = false
    selectedStatus.value = ''
    editOrderTab.value = 'order'
    const message = response.message || 'Cập nhật trạng thái hóa đơn thành công'
    showLocalToast('success', 'Cập nhật thành công', message)

    // Giữ yêu cầu cũ: khi chuyển sang Đã xác nhận thì mở hộp thoại in.
    if (newCode === 1 && currentCode !== 1) {
      await new Promise(resolve => window.setTimeout(resolve, 150))
      printInvoice()
    }
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || 'Không thể cập nhật trạng thái hóa đơn.'
    showLocalToast('error', 'Cập nhật thất bại', message)
  } finally {
    updatingStatus.value = false
  }
}


async function openHistoryModal() {
  if (loadingHistory.value || !invoice.value) return
  const code = invoice.value.code
  showHistoryModal.value = true
  loadingHistory.value = true

  try {
    const response = await invoiceService.getHistory(code)
    if (invoice.value?.code !== code) return
    if (!response?.success) {
      throw new Error(response?.message || 'Không thể tải lịch sử hóa đơn')
    }

    // Hiển thị theo thời gian tăng dần; id phân định các thao tác trong cùng một giây.
    // Đơn nhận tại quầy không có các trạng thái giao hàng (2/3/4),
    // nên loại các bản ghi giao hàng nếu dữ liệu lịch sử cũ còn tồn tại.
    const rawHistory = Array.isArray(response.data) ? response.data : []
    const isPickupOrder = Number(invoice.value?.receiveMethodCode) === 0
    const filteredHistory = isPickupOrder
      ? rawHistory.filter(item => ![2, 3, 4].includes(Number(item?.maTrangThai)))
      : rawHistory
    historyItems.value = chronologicalHistory(filteredHistory)
  } catch (error) {
    if (invoice.value?.code !== code) return
    historyItems.value = []
    showLocalToast(
      'error',
      'Không thể tải lịch sử',
      error?.response?.data?.message || error?.message || 'Không thể tải lịch sử hóa đơn.'
    )
  } finally {
    loadingHistory.value = false
  }
}

function closeHistoryModal() {
  showHistoryModal.value = false
}

function chronologicalHistory(rows) {
  const timestamp = value => {
    const match = String(value || '').match(/^(\d{2})\/(\d{2})\/(\d{4}) (\d{2}):(\d{2}):(\d{2})$/)
    if (!match) return null
    return Date.UTC(Number(match[3]), Number(match[2]) - 1, Number(match[1]), Number(match[4]), Number(match[5]), Number(match[6]))
  }
  return [...rows].sort((a, b) => {
    const first = timestamp(a.ngayTao), second = timestamp(b.ngayTao)
    if (first != null && second != null && first !== second) return first - second
    return Number(a.id || 0) - Number(b.id || 0)
  })
}
function historyStatusLabel(item) {
  if (Number(item?.maTrangThai) === 7) {
    const note = String(item?.ghiChu || '')
    if (note.startsWith('Đã xử lý hàng trả;')) return 'Chuyển sang bước Hoàn tiền'
    if (note.startsWith('Đã xác nhận hoàn tiền thực tế:')) return 'Đã hoàn tiền thực tế'
  }
  return item?.trangThai || '—'
}

function historyActor(item) {
  return item?.tenKhachHang || item?.tenNhanVien || (item?.idNhanVien != null ? `Nhân viên #${item.idNhanVien}` : 'Hệ thống / chưa xác định')
}

function historyAction(item) {
  if (item?.ghiChu) return String(item.ghiChu).replace(/(Đã xác nhận hoàn tiền thực tế: )([0-9]+(?:\.[0-9]+)?) đ/, (_, label, amount) => label + money(amount))
  return item?.trangThai ? `Chuyển sang trạng thái: ${item.trangThai}` : 'Cập nhật hóa đơn'
}

function printInvoice() {
  if (!canPrintInvoice.value) return
  document.body.classList.add('invoice-print-mode')
  window.setTimeout(() => window.print(), 50)
}

function clearPrintMode() {
  document.body.classList.remove('invoice-print-mode')
}

window.addEventListener('afterprint', clearPrintMode)

function stepDate(status) {
  const entry = invoice.value?.history?.find(item => Number(item?.maTrangThai) === status)
  if (entry?.ngayTao) return entry.ngayTao
  if (status === 8 && invoice.value?.posManaged) return invoice.value.date || '—'
  return '—'
}

onMounted(async () => {
  await loadInvoice()
  if (route.query?.print === '1' && canPrintInvoice.value) { await nextTick(); printInvoice() }
})
watch(() => route.params.code, () => {
  showEditOrderModal.value = false
  showCancelModal.value = false
  showHistoryModal.value = false
  resetProductFilters()
  loadInvoice()
})
onUnmounted(() => {
  invoiceLoadVersion += 1
  window.removeEventListener('afterprint', clearPrintMode)
  clearPrintMode()
})
</script>

<template>
  <AdminLayout>
    <main class="detail-page container-fluid">
      <div class="page-title-row">
        <div class="breadcrumb">
          <RouterLink to="/hoa-don">Hóa đơn</RouterLink><b>/</b>
          <strong>Chi tiết hóa đơn</strong>
        </div>
        <div class="page-title-actions">
          <button class="back-list-button" type="button" @click="router.push('/ban-hang')">
            <i class="bi bi-arrow-left"></i>
            Quay lại bán hàng tại quầy
          </button>
          <button class="back-list-button" type="button" @click="router.push('/hoa-don')">
            <i class="bi bi-arrow-left"></i>
            Danh sách hóa đơn
          </button>
        </div>
      </div>

      <section v-if="invoice" class="invoice-summary-strip">
        <div><span>Mã Đơn Hàng:</span><strong>{{ invoice.code }}</strong></div>
        <div><span>Ngày Tạo:</span><strong>{{ invoice.date }}</strong></div>
        <div><span>Tạo bởi:</span><strong>{{ invoice.creator }}</strong></div>
        <div><span>Cập Nhật Gần Nhất:</span><strong>{{ invoice.updatedDate }}</strong></div>
        <div><span>Loại đơn:</span><strong>{{ invoice.orderType }}</strong></div>
        <div><span>Hình thức nhận:</span><strong>{{ invoice.receiveMethod }}</strong></div>
      </section>

      <div v-if="loading" class="panel card shadow-sm loading-panel">
        <div class="spinner-border spinner-border-sm text-primary me-2"></div>
        Đang tải chi tiết hóa đơn...
      </div>

      <template v-else-if="invoice">
        <div class="detail-layout">
        <div class="main-column">
          <section class="panel card shadow-sm timeline-panel">
            <h2><i class="bi bi-clipboard2-check"></i> Trạng thái đơn hàng</h2>

            <div v-if="[6, 7].includes(invoice.statusCode)" class="cancelled-timeline-scroll invoice-timeline-scroll" role="region" aria-label="Tiến trình xử lý hoàn và hủy hóa đơn" tabindex="0">
              <div class="timeline timeline-cancelled" :style="{ '--cancel-step-count': cancelledSteps.length, '--timeline-step-count': cancelledSteps.length }">
                <div v-for="step in cancelledSteps" :key="step.key" class="timeline-step reached"
                  :class="[{ current: step.value === invoice.statusCode }, 'cancelled-step-' + step.key]">
                  <div class="timeline-node"><i :class="['bi', step.icon]"></i></div>
                  <div class="step-label">{{ step.label }}</div>
                  <small>{{ cancelledStepDate(step.value) }}</small>
                </div>
              </div>
            </div>

            <div v-else-if="progress >= 0" class="invoice-timeline-scroll" role="region" aria-label="Tiến trình hóa đơn" tabindex="0">
            <div
              class="timeline"
              :class="{ 'timeline-pickup': invoice.receiveMethodCode === 0 }"
              :style="{ '--timeline-step-count': visibleSteps.length }"
            >
              <div
                v-for="(step, index) in visibleSteps"
                :key="step.key"
                class="timeline-step"
                :class="{ reached: index <= progress, current: index === progress }"
              >
                <div class="timeline-node">
                  <i :class="['bi', step.icon]"></i>
                </div>
                <div class="step-label">{{ step.label }}</div>
                <small v-if="index <= progress">{{ stepDate(step.value) }}</small>
              </div>
            </div>

            </div>

            <div v-else class="cancel-state">
              <span class="cancel-icon"><i class="bi bi-x-circle"></i></span>
              <div>
                <strong>{{ invoice.status }}</strong>
                <p v-if="invoice.statusCode === 9">{{ failureCount >= 3 ? 'Đã thất bại 3 lần. Xử lý hàng trả, hoàn tiền nếu có khoản thu rồi hủy hóa đơn.' : invoice.goodsHandled ? 'Đã xử lý hàng trả. Hoàn tiền nếu có khoản thu rồi hủy hóa đơn.' : 'Có thể giao lại hoặc xử lý hàng trả để kết thúc đơn hàng.' }}</p>
                <div v-if="invoice.statusCode === 9" class="delivery-failure-reason">
                  <span class="failure-reason-label"><i class="bi bi-chat-left-text"></i> Lý do giao hàng thất bại</span>
                  <p>{{ deliveryFailureReason(latestDeliveryFailure?.ghiChu) }}</p>
                  <span v-if="latestDeliveryFailure?.ngayTao" class="failure-reason-date"><i class="bi bi-clock"></i> {{ latestDeliveryFailure.ngayTao }}</span>
                </div>
                <p v-else>Đơn hàng đã dừng ở trạng thái này.</p>
              </div>
            </div>

            <div class="timeline-actions">
              <span v-if="invoice.statusCode === 7 && invoice.goodsHandled" class="goods-handled-tag"><i class="bi bi-check-circle"></i> Đã xử lý hàng trả</span>
              <button v-if="invoice.statusCode === 9 && !invoice.goodsHandled" type="button" class="invoice-flow-action invoice-flow-action--return" :disabled="updatingStatus" @click="openReturnDialog('HANG_TRA')"><i class="bi bi-box-seam" aria-hidden="true"></i><span>Xử lý hàng trả</span></button>
              <button v-if="invoice.statusCode === 7 && invoice.goodsHandled && invoice.refundDue === 0 && !invoice.reconciliationError" type="button" class="invoice-flow-action invoice-flow-action--cancel" :disabled="updatingStatus || invoice.refundDue !== 0 || !!invoice.reconciliationError" @click="openReturnDialog('HUY')"><i class="bi bi-x-circle" aria-hidden="true"></i><span>Hủy hóa đơn</span></button>
              <!-- <button v-if="canCancelOrder" type="button" class="btn btn-outline-danger btn-sm cancel-order-button"
                :disabled="confirmSubmitting || updatingStatus" @click="openCancelModal">
                <i class="bi bi-x-circle"></i> Hủy đơn hàng
              </button> -->
              <button
                class="soft-button btn btn-outline-primary btn-sm"
                type="button"
                :disabled="loadingHistory"
                @click="openHistoryModal"
              >
                <i class="bi bi-clock-history"></i>
                Lịch sử thao tác
              </button>
            </div>
          </section>

          <div class="info-grid">
            <section class="panel card shadow-sm info-panel">
              <h2><i class="bi bi-person-lock"></i> Thông tin khách hàng</h2>
              <div class="info-row"><span>Tên khách hàng</span><strong>{{ invoice.customer }}</strong></div>
              <div class="info-row"><span>Mã khách hàng</span><strong>{{ invoice.customerCode }}</strong></div>
              <div class="info-row"><span>Số điện thoại</span><strong>{{ invoice.phone }}</strong></div>
              <div class="info-row"><span>Email</span><strong>{{ invoice.email || '—' }}</strong></div>
            </section>

            <section class="panel card shadow-sm info-panel shipping-panel">
              <h2><i class="bi bi-geo-alt"></i> Thông tin giao hàng</h2>
              <div class="info-row address-row">
                <span>Địa chỉ giao hàng</span><strong>{{ invoice.shippingAddress || '—' }}</strong>
              </div>
              <!-- <div class="info-row">
                <span>Đơn vị vận chuyển</span><strong>{{ invoice.shippingCarrier || '—' }}</strong>
              </div> -->
              <div class="info-row">
                <span>Ghi chú</span><strong>{{ invoice.shippingNote || '—' }}</strong>
              </div>
            </section>
          </div>

        </div>

        <aside class="side-column">
          <section class="panel card shadow-sm payment-panel">
            <h2><i class="bi bi-calendar2-check"></i> Tổng kết thanh toán</h2>
            <div class="amount-row"><span>Tổng tiền hàng</span><strong>{{ money(subtotal) }}</strong></div>
            <div class="amount-row discount-row">
              <span class="voucher-summary-label">Phiếu giảm giá <b v-if="hasVoucher">{{ invoice.voucherCode }}</b></span>
              <strong class="discount voucher-summary-value"><template v-if="voucherLabel">{{ voucherLabel }} </template>(− {{ money(invoice.discount) }})</strong>
            </div>
            <div v-if="isDeliveryOrder" class="amount-row">
              <span class="shipping-provider-label"><GhnLogo v-if="isGHN" /> Phí vận chuyển</span>
              <strong class="shipping-fee-value">
                + {{ money(invoice.shippingFee) }}
                <span v-if="!isGHN && invoice.shippingCarrier" class="carrier-badge">{{ invoice.shippingCarrier }}</span>
              </strong>
            </div>
            <div class="grand-total"><span>TỔNG TIỀN</span><strong>{{ money(invoice.total) }}</strong></div>
          </section>

          <section class="panel card shadow-sm payment-history">
            <h2><i class="bi bi-credit-card"></i> Lịch sử thanh toán</h2>

            <div v-if="paymentHistory.length" class="payment-history-list">
              <div v-for="payment in paymentHistory" :key="payment.id" class="payment-history-item">
                <div class="payment-history-top">
                  <div>
                    <span class="payment-history-label">Phương thức thanh toán</span>
                    <strong>{{ payment.method }}</strong>
                  </div>
                  <strong :class="payment.statusCode === 1 ? 'payment-status paid' : 'payment-status unpaid'">
                    <i :class="payment.statusCode === 1 ? 'bi bi-check-circle-fill' : 'bi bi-clock-fill'"></i>
                    {{ payment.status }}
                  </strong>
                </div>
                <div class="payment-history-meta">
                  <span v-if="payment.time"><i class="bi bi-clock"></i>{{ payment.time }}</span>
                  <span v-if="payment.transactionCode"><i class="bi bi-upc"></i>{{ payment.transactionCode }}</span>
                </div>
                <div class="payment-history-amount">
                  <span>Số tiền</span><strong>{{ money(payment.amount) }}</strong>
                </div>
              </div>
            </div>
            <div v-else class="payment-history-empty">
              <i class="bi bi-credit-card-2-front"></i>
              <span>Chưa có lịch sử thanh toán.</span>
            </div>

            <div v-if="invoice.statusCode === 7" class="refund-workflow">
              <div class="refund-workflow-heading"><strong>Hoàn tiền</strong><span>{{ invoice.reconciliationError ? 'Cần đối soát' : invoice.refundDue > 0 ? 'Chờ hoàn tiền' : invoice.paymentStatusCode === 2 ? 'Đã hoàn tiền' : 'Không cần hoàn tiền' }}</span></div>
              <p v-if="invoice.reconciliationError">{{ invoice.reconciliationError }}</p>
              <template v-else-if="invoice.refundDue > 0">
                <div class="refund-workflow-amount"><span>Cần hoàn cho khách</span><strong>{{ money(invoice.refundDue) }}</strong></div>
                <p>{{ invoice.goodsHandled ? 'Hoàn tiền thực tế rồi xác nhận và lưu chứng từ tại đây.' : 'Hoàn tất xử lý hàng trả trước khi xác nhận hoàn tiền.' }}</p>
                <button class="ss-refund-button" :disabled="updatingStatus || !invoice.goodsHandled" @click="openReturnDialog('HOAN_TIEN')"><i class="bi bi-arrow-return-left"></i> Hoàn tiền</button>
              </template>
              <p v-else>{{ invoice.paymentStatusCode === 2 ? 'Khoản hoàn đã được ghi nhận riêng. Bạn có thể hủy hóa đơn sau khi xử lý hàng trả.' : 'Đơn không có khoản tiền đã thu còn phải hoàn.' }}</p>
            </div>
            <div class="payment-history-actions" :class="{ 'single-action': !canPrintInvoice }">
              <button
                v-if="canPrintInvoice"
                class="payment-action-button print-button btn btn-primary"
                type="button"
                @click="printInvoice"
              >
                <i class="bi bi-printer"></i> In hóa đơn
              </button>
              <button
                v-if="canUpdateStatus"
                class="payment-action-button edit-order-button btn btn-primary"
                type="button"
                :disabled="updatingStatus || confirmSubmitting"
                @click="openEditOrderModal"
              >
                <i class="bi bi-pencil-square"></i> Chỉnh sửa đơn hàng
              </button>
            </div>
          </section>
        </aside>
        </div>

        <section class="panel card shadow-sm products-panel">
        <div class="products-heading">
          <h2><i class="bi bi-box-seam"></i> Danh sách sản phẩm</h2>
          <span class="products-count">{{ filteredItems.length }}/{{ invoice.items?.length || 0 }} sản phẩm</span>
        </div>

        <div class="product-filter-box">
          <div class="product-filter-field product-search-field">
            <label>Tìm kiếm</label>
            <div class="product-search-input">
              <i class="bi bi-search"></i>
              <input v-model="productSearch" type="text" placeholder="Mã SPCT, SKU hoặc tên sản phẩm..." />
            </div>
          </div>
          <div class="product-filter-field">
            <label>Loại sản phẩm</label>
            <Select2Field
              v-model="productCategory"
              :options="productCategoryOptions"
              placeholder="Tất cả loại"
              :allow-clear="false"
            />
          </div>
          <div class="product-filter-field">
            <label>Màu sắc</label>
            <Select2Field
              v-model="productColor"
              :options="productColorOptions"
              placeholder="Tất cả màu sắc"
              :allow-clear="false"
            />
          </div>
          <div class="product-filter-field">
            <label>Kích cỡ</label>
            <Select2Field
              v-model="productSize"
              :options="productSizeOptions"
              placeholder="Tất cả kích cỡ"
              :allow-clear="false"
            />
          </div>
          <div class="product-filter-field product-sort-field">
            <label>Sắp xếp</label>
            <Select2Field
              v-model="productSort"
              :options="productSortOptions"
              placeholder="Sắp xếp"
              :allow-clear="false"
            />
          </div>
          <button v-if="productFilterActive" type="button" class="product-reset-button" @click="resetProductFilters">
            <i class="bi bi-arrow-counterclockwise"></i> Đặt lại
          </button>
        </div>

        <div class="product-table-wrap">
          <table class="product-table table table-hover align-middle mb-0">
            <thead>
              <tr>
                <th>STT</th>
                <th>Mã SPCT</th>
                <th>Ảnh</th>
                <th>Sản phẩm</th>
                <th>Màu sắc</th>
                <th>Size</th>
                <th>Số lượng</th>
                <th>Thời gian</th>
                <th>Giảm giá</th>
                <th>Đơn giá</th>
                <th>Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in filteredItems" :key="item.id || index">
                <td>{{ index + 1 }}</td>
                <td class="product-code">{{ item.productDetailCode }}</td>
                <td>
                  <span class="product-thumb">
                    <img
                      v-if="imageUrl(item.image)"
                      :src="imageUrl(item.image)"
                      :alt="item.name"
                      loading="lazy"
                      referrerpolicy="no-referrer"
                    />
                    <i v-else class="bi bi-image"></i>
                  </span>
                </td>
                <td class="product-info-cell">
                  <strong>{{ item.name }}</strong>
                  <small v-if="item.category || item.brand">
                    {{ [item.category, item.brand].filter(Boolean).join(' • ') }}
                  </small>
                  <small class="product-subline">
                    {{ [item.material, item.style, item.collar, item.origin].filter(Boolean).join(' • ') || ('SKU: ' + item.sku) }}
                  </small>
                </td>
                <td>{{ item.color }}</td>
                <td>{{ item.size }}</td>
                <td>{{ item.quantity }}</td>
                <td class="product-time">
                  {{ invoice.date?.split(' ')[1] || invoice.date }}
                  <small>{{ invoice.date?.split(' ')[0] || '' }}</small>
                </td>
                <td><div v-if="item.discountPercent != null" class="line-discount"><span>{{ item.discountPercent }}%</span><small v-if="item.originalPrice > item.price">−{{ money((item.originalPrice - item.price) * item.quantity) }}</small></div><span v-else class="discount-empty">—</span></td>
                <td class="nowrap"><del v-if="item.originalPrice > item.price" class="historical-price">{{ money(item.originalPrice) }}</del><strong :class="{ 'historical-sale': item.originalPrice > item.price }">{{ money(item.price) }}</strong></td>
                <td class="product-total">{{ money(item.lineTotal) }}</td>
              </tr>
              <tr v-if="!filteredItems.length">
                <td colspan="11" class="empty-products">
                  {{ invoice.items?.length ? 'Không có sản phẩm phù hợp với bộ lọc.' : 'Không có sản phẩm.' }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

        <section class="print-invoice">
        <div class="print-header">
          <div>
            <div class="print-brand">SMASHSTEP</div>
            <div class="print-subtitle">SPORTS SHOES</div>
          </div>
          <div class="print-header-right">
            <div class="print-title">HÓA ĐƠN BÁN HÀNG</div>
            <div class="print-qr">
              <img :src="qrCodeUrl" :alt="`Mã QR ${invoice.code}`" />
              <span>Quét mã: {{ invoice.code }}</span>
            </div>
          </div>
        </div>

        <div class="print-meta-grid">
          <div><span>Mã hóa đơn</span><strong>{{ invoice.code }}</strong></div>
          <div><span>Ngày tạo</span><strong>{{ invoice.date }}</strong></div>
          <div><span>Khách hàng</span><strong>{{ invoice.customer }}</strong></div>
          <div><span>Số điện thoại</span><strong>{{ invoice.phone }}</strong></div>
          <div><span>Người nhận</span><strong>{{ invoice.recipient }}</strong></div>
          <div><span>Phương thức thanh toán</span><strong>{{ invoice.payment }}</strong></div>
        </div>

        <table class="print-product-table">
          <thead>
            <tr><th>#</th><th>Sản phẩm</th><th>Phân loại</th><th>Đơn giá</th><th>SL</th><th>Thành tiền</th></tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in invoice.items" :key="`print-${index}`">
              <td>{{ index + 1 }}</td>
              <td>{{ item.name }}</td>
              <td>{{ item.variant }}</td>
              <td><del v-if="item.originalPrice > item.price" class="historical-price">{{ money(item.originalPrice) }}</del>{{ money(item.price) }}<small v-if="item.discountPercent > 0"> (−{{ item.discountPercent }}%)</small></td>
              <td>{{ item.quantity }}</td>
              <td>{{ money(item.lineTotal) }}</td>
            </tr>
          </tbody>
        </table>

        <div class="print-summary">
          <div><span>Tổng tiền hàng</span><strong>{{ money(subtotal) }}</strong></div>
          <div>
            <span>Giảm giá</span>
            <strong>
              {{ money(invoice.discount) }}
              <small v-if="hasVoucher">({{ invoice.voucherCode }}<template v-if="voucherLabel"> · {{ voucherLabel }}</template>)</small>
            </strong>
          </div>
          <div v-if="isDeliveryOrder"><span class="shipping-provider-label"><GhnLogo v-if="isGHN" /> Phí vận chuyển</span><strong>{{ money(invoice.shippingFee) }}</strong></div>
          <div class="print-grand-total"><span>TỔNG THANH TOÁN</span><strong>{{ money(invoice.total) }}</strong></div>
        </div>

        <div class="print-payment">
          <div><span>Trạng thái thanh toán:</span> {{ invoice.paymentStatus }}</div>
          <div v-if="invoice.paymentDate"><span>Ngày thanh toán:</span> {{ invoice.paymentDate }}</div>
          <div v-for="payment in paymentHistory" :key="payment.id"><span>{{ payment.method }}:</span> {{ money(payment.amount) }} <small>{{ payment.transactionCode }}</small></div>
        </div>

        <div v-if="isDeliveryOrder && hasShippingInfo" class="print-shipping">
          <h3>THÔNG TIN GIAO HÀNG</h3>
          <div v-if="invoice.shippingCarrier" class="print-carrier"><span>Đơn vị vận chuyển:</span><GhnLogo v-if="isGHN" /><template v-else>{{ invoice.shippingCarrier }}</template></div>
          <!-- <div><span>Phí vận chuyển:</span> {{ money(invoice.shippingFee) }}</div> -->
          <!-- <div v-if="invoice.shippingRecipient"><span>Người nhận:</span> {{ invoice.shippingRecipient }}</div> -->
          <!-- <div v-if="invoice.shippingRecipientPhone"><span>Số điện thoại:</span> {{ invoice.shippingRecipientPhone }}</div> -->
          <div v-if="invoice.shippingAddress"><span>Địa chỉ giao hàng:</span> {{ invoice.shippingAddress }}</div>
          <div v-if="invoice.shippingNote"><span>Ghi chú:</span> {{ invoice.shippingNote }}</div>
        </div>

        </section>
      </template>

      <div v-else-if="loadError" class="panel card shadow-sm error-panel">
        <div class="error-panel-icon">
          <i class="bi bi-exclamation-triangle"></i>
        </div>
        <div>
          <h2>Không thể tải chi tiết hóa đơn</h2>
          <p>{{ loadError }}</p>
          <button type="button" class="btn btn-primary btn-sm" @click="loadInvoice">
            <i class="bi bi-arrow-clockwise"></i>
            Thử tải lại
          </button>
        </div>
      </div>


      <!-- Toast cục bộ cho thao tác cập nhật DB -->
      

      <!-- Modal xác nhận hủy đơn hàng
      <div
        v-if="showCancelModal"
        class="modal fade show d-block status-modal-backdrop confirm-layer"
        tabindex="-1"
        role="dialog"
        aria-modal="true"
        @click.self="closeCancelModal"
      >
        <div class="modal-dialog modal-dialog-centered modal-sm">
          <div class="modal-content status-modal confirm-modal">
            <div class="confirm-icon cancel-confirm-icon"><i class="bi bi-x-lg"></i></div>
            <div class="modal-body text-center pt-0">
              <h5 class="confirm-title">Xác nhận hủy đơn hàng</h5>
              <p class="confirm-message">
                Bạn có chắc chắn muốn hủy đơn <strong>{{ invoice.code }}</strong> không?
              </p>
              <p class="cancel-warning">Đơn hàng sẽ chuyển sang trạng thái <strong>Đã hủy</strong>.</p>
            </div>
            <div class="modal-footer justify-content-center">
              <button type="button" class="btn btn-light modal-cancel-button" :disabled="confirmSubmitting" @click="closeCancelModal">Không</button>
              <button type="button" class="btn btn-danger cancel-confirm-button" :disabled="confirmSubmitting" @click="cancelOrder">
                <span v-if="confirmSubmitting" class="spinner-border spinner-border-sm me-1"></span>
                <span v-else><i class="bi bi-x-circle me-1"></i>Hủy đơn hàng</span>
              </button>
            </div>
          </div>
        </div>
      </div> -->

      <!-- Modal chỉnh sửa đơn hàng -->
      <div v-if="returnDialog" class="modal fade show return-modal-backdrop" role="dialog" aria-modal="true" aria-labelledby="return-modal-title" aria-describedby="return-modal-description">
        <div class="modal-dialog modal-dialog-centered return-modal-dialog">
          <div class="modal-content return-modal">
            <div class="modal-header">
              <div class="return-modal-heading">
                <span class="return-heading-icon"><i class="bi bi-arrow-return-left"></i></span>
                <div><h5 id="return-modal-title">{{ returnDialogTitle }}</h5><p>Hóa đơn <strong>{{ invoice.code }}</strong></p></div>
              </div>
              <button type="button" class="return-close" aria-label="Đóng" :disabled="updatingStatus" @click="returnDialog = null"><i class="bi bi-x-lg"></i></button>
            </div>
            <div class="modal-body">
              <div class="return-summary" id="return-modal-description">
                <span class="return-attempt"><i class="bi bi-exclamation-circle"></i> Giao thất bại {{ failureCount }}/3</span>
                <p>{{ returnDialog.thaoTac === 'HANG_TRA' ? 'Lưu kết quả nhận và kiểm tra hàng, sau đó chuyển hóa đơn sang bước Hoàn tiền. Chưa ghi nhận tiền hoàn ở bước này.' : returnDialog.thaoTac === 'HOAN_TIEN' ? 'Ghi nhận khoản đã hoàn thực tế cho khách. Sau khi hoàn đủ tiền, bạn có thể hủy hóa đơn.' : 'Hàng trả và khoản thu đã được xử lý. Xác nhận để kết thúc hóa đơn.' }}</p>
              </div>
              <p v-if="invoice.reconciliationError" class="alert alert-warning" role="alert">{{ invoice.reconciliationError }}</p>

              <section v-if="returnDialog.thaoTac === 'HANG_TRA'" class="return-section">
                <h6><span class="return-step">1</span> Nhận hàng và kiểm tra tồn kho</h6>
                <label class="return-check" :class="{ checked: returnDialog.daNhanLaiHang }">
                  <input v-model="returnDialog.daNhanLaiHang" type="checkbox" :disabled="updatingStatus" />
                  <span><strong>Đã nhận lại đầy đủ hàng</strong><small>Hàng đã được đơn vị vận chuyển bàn giao lại.</small></span>
                </label>
                <label class="return-check" :class="{ checked: returnDialog.nhapLaiKho }">
                  <input v-model="returnDialog.nhapLaiKho" type="checkbox" :disabled="updatingStatus" />
                  <span><strong>Nhập lại toàn bộ hàng vào kho</strong><small>Chỉ chọn khi đã kiểm tra và toàn bộ hàng đủ điều kiện bán.</small></span>
                </label>
                <p class="return-hint"><i class="bi bi-info-circle"></i> Bỏ chọn nhập lại kho nếu hàng trả không đủ điều kiện.</p>
              </section>

              <section v-if="returnDialog.thaoTac === 'HOAN_TIEN'" class="return-section">
                <h6><span class="return-step">2</span> Xác nhận hoàn tiền</h6>
                <template v-if="invoice.refundDue > 0">
                  <div class="return-refund-amount"><span>Số tiền cần hoàn cho khách</span><strong>{{ money(invoice.refundDue) }}</strong></div>
                  <label class="return-check" :class="{ checked: returnDialog.daHoanTien }">
                    <input v-model="returnDialog.daHoanTien" type="checkbox" :disabled="updatingStatus" />
                    <span><strong>Đã hoàn đủ tiền thực tế cho khách</strong><small>Kiểm tra giao dịch trước khi xác nhận.</small></span>
                  </label>
                  <label class="return-field">Mã giao dịch / chứng từ hoàn tiền <span>*</span>
                    <input v-model="returnDialog.maHoanTien" id="refund-reference" maxlength="100" class="form-control"
                      :class="{ 'refund-reference-invalid': refundReferenceError }" :aria-invalid="!!refundReferenceError"
                      aria-describedby="refund-reference-error refund-reference-hint" :disabled="updatingStatus" autocomplete="off"
                      placeholder="VD: FT26101812345678 hoặc PC-20261018-001"
                      @blur="validateRefundReference(true)" @input="inputRefundReference" @paste="pasteRefundReference" />
                    <small v-if="refundReferenceError" id="refund-reference-error" class="refund-reference-error" role="alert">{{ refundReferenceError }}</small>
                    <small id="refund-reference-hint" class="refund-reference-hint">Nhập mã giao dịch ngân hàng hoặc số phiếu chi thực tế, tối đa 100 ký tự.</small>
                  </label>
                  <p class="return-hint"><i class="bi bi-info-circle"></i> Hệ thống ghi nhận khoản đã hoàn, không tự chuyển tiền ngân hàng.</p>
                </template>
                <div v-else-if="invoice.refundDue === 0" class="return-no-refund"><i class="bi bi-check-circle"></i> Không có khoản tiền đã thu cần hoàn.</div>
              </section>

              <section class="return-section return-note-section">
                <h6><span class="return-step">3</span> Ghi chú xử lý</h6>
                <label class="return-field">Nội dung xử lý <span>*</span>
                  <textarea v-model="returnDialog.ghiChu" class="form-control" maxlength="700" rows="3" :disabled="updatingStatus" placeholder="Ghi rõ tình trạng hàng trả và kết quả xử lý..." />
                </label>
                <div class="return-note-count">{{ returnDialog.ghiChu.length }}/700 ký tự</div>
              </section>
            </div>
            <div class="modal-footer">
              <button type="button" class="return-secondary" :disabled="updatingStatus" @click="returnDialog = null">Đóng</button>
              <button type="button" class="return-submit" :disabled="updatingStatus || (returnDialog.thaoTac !== 'HANG_TRA' && !!invoice.reconciliationError) || (returnDialog.thaoTac === 'HANG_TRA' && !returnDialog.daNhanLaiHang)" @click="resolveFailure">
                <i :class="updatingStatus ? 'bi bi-hourglass-split' : 'bi bi-check2-circle'"></i>
                {{ updatingStatus ? 'Đang xử lý...' : returnButtonLabel }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div
        v-if="showEditOrderModal"
        class="modal fade show d-block status-modal-backdrop"
        tabindex="-1"
        role="dialog"
        aria-modal="true"
        @click.self="closeEditOrderModal"
      >
        <div class="modal-dialog modal-dialog-centered modal-md">
          <div class="modal-content status-modal edit-order-modal">
            <div class="modal-header">
              <h5 class="modal-title">Chỉnh sửa đơn hàng</h5>
              <button
                type="button"
                class="btn-close"
                aria-label="Đóng"
                :disabled="updatingStatus"
                @click="closeEditOrderModal"
              ></button>
            </div>

            <div class="edit-order-tabs">
              <button
                type="button"
                class="edit-order-tab"
                :class="{ active: editOrderTab === 'order' }"
                @click="editOrderTab = 'order'"
              >
                Thông tin đơn hàng
              </button>
              <button
                type="button"
                class="edit-order-tab"
                :class="{ active: editOrderTab === 'customer' }"
                @click="editOrderTab = 'customer'"
              >
                Thông tin khách hàng
              </button>
            </div>

            <div class="modal-body">
              <div v-if="editOrderTab === 'order'">
                <div class="status-info-grid">
                  <div>
                    <span>Mã đơn hàng</span>
                    <strong>{{ invoice.code }}</strong>
                  </div>
                  <div>
                    <span>Ngày tạo</span>
                    <strong>{{ invoice.date }}</strong>
                  </div>
                </div>

                <label class="form-label mt-3 mb-2">Trạng thái</label>
                <select
                  v-model.number="selectedStatus"
                  class="form-select"
                  aria-label="Trạng thái đơn hàng"
                  :disabled="updatingStatus || !canUpdateStatus"
                >
                  <option v-for="option in editableStatusOptions" :key="option.value" :value="option.value">
                    {{ option.label }}{{ option.value === Number(invoice.statusCode) ? ' (hiện tại)' : '' }}
                  </option>
                </select>
                <label v-if="Number(selectedStatus) === 9" class="d-block mt-3">
                  Lý do giao hàng thất bại <span class="text-danger">*</span>
                  <textarea v-model="failureReason" class="form-control mt-2" maxlength="800" rows="3" :disabled="updatingStatus" placeholder="Ví dụ: khách không nghe máy, hẹn giao lại..." />
                </label>
                <div class="form-text">
                  Chỉ được chuyển sang trạng thái kế tiếp theo đúng quy trình của đơn hàng.
                  <p v-if="Number(selectedStatus) === 5 && invoice.orderTypeCode === 1 && String(invoice.payment).toUpperCase().includes('COD')" class="mt-2 text-primary fw-semibold">Chỉ lưu Hoàn thành sau khi đã giao hàng và thực thu đủ COD. Thao tác này sẽ ghi nhận số tiền COD đã thu vào lịch sử thanh toán.</p>
                </div>

                <div v-if="!canUpdateStatus" class="edit-order-note">
                  <i class="bi bi-info-circle"></i>
                  Đơn hàng hiện không còn trạng thái kế tiếp để cập nhật.
                </div>
              </div>

              <div v-else class="customer-readonly">
                <div class="customer-readonly-head">
                  <div class="customer-avatar"><i class="bi bi-person"></i></div>
                  <div>
                    <strong>{{ invoice.customer }}</strong>
                    <span>{{ invoice.customerCode }}</span>
                  </div>
                </div>
                <div class="customer-fields">
                  <div class="customer-field">
                    <span>Họ tên</span>
                    <strong>{{ invoice.customer }}</strong>
                  </div>
                  <div class="customer-field">
                    <span>Số điện thoại</span>
                    <strong>{{ invoice.phone }}</strong>
                  </div>
                  <div class="customer-field">
                    <span>Email</span>
                    <strong>{{ invoice.email }}</strong>
                  </div>
                  <div class="customer-field">
                    <span>Người nhận</span>
                    <strong>{{ invoice.recipient }}</strong>
                  </div>
                </div>
                <div class="edit-order-note">
                  <i class="bi bi-info-circle"></i>
                  Thông tin khách hàng đang ở chế độ chỉ xem vì project hiện tại chưa có API cập nhật khách hàng trong module hóa đơn.
                </div>
              </div>
            </div>

            <div class="modal-footer">
              <button
                type="button"
                class="btn btn-light modal-cancel-button"
                :disabled="updatingStatus"
                @click="closeEditOrderModal"
              >
                Hủy
              </button>
              <button
                v-if="editOrderTab === 'order'"
                type="button"
                class="btn btn-primary modal-save-button"
                :disabled="!canUpdateStatus || Number(selectedStatus) === Number(invoice.statusCode) || updatingStatus"
                @click="saveStatus"
              >
                <span v-if="updatingStatus" class="spinner-border spinner-border-sm me-1"></span>
                <i v-else class="bi bi-check2 me-1"></i>
                Lưu
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Modal lịch sử thao tác -->
      <div
        v-if="showHistoryModal"
        class="modal fade show d-block history-modal-backdrop"
        tabindex="-1"
        role="dialog"
        aria-modal="true"
        @click.self="closeHistoryModal"
      >
        <div class="modal-dialog modal-dialog-centered modal-lg history-modal-dialog">
          <div class="modal-content history-modal">
            <div class="modal-header">
              <div>
                <h5 class="modal-title">Lịch sử thao tác</h5>
                <small class="text-muted">Đơn hàng {{ invoice.code }} · Thứ tự từ cũ đến mới</small>
              </div>
              <button
                type="button"
                class="btn-close"
                aria-label="Đóng"
                @click="closeHistoryModal"
              ></button>
            </div>

            <div class="modal-body history-body">
              <div v-if="loadingHistory" class="history-loading">
                <div class="spinner-border spinner-border-sm text-primary me-2"></div>
                Đang tải lịch sử...
              </div>

              <div v-else-if="!historyItems.length" class="history-empty">
                <i class="bi bi-clock-history"></i>
                <p>Chưa có lịch sử thao tác.</p>
              </div>

              <div v-else class="history-timeline">
                <div
                  v-for="item in historyItems"
                  :key="item.id"
                  class="history-item"
                  :class="{ 'history-failure': Number(item.maTrangThai) === 9, 'history-refund': Number(item.maTrangThai) === 7 }"
                >
                  <div class="history-dot">
                    <i :class="Number(item.maTrangThai) === 9 ? 'bi bi-exclamation-lg' : Number(item.maTrangThai) === 7 ? 'bi bi-arrow-return-left' : 'bi bi-check2'"></i>
                  </div>
                  <div class="history-card">
                    <div class="history-card-top">
                      <strong>{{ historyActor(item) }}</strong>
                      <span>{{ formatHistoryDate(item.ngayTao) }}</span>
                    </div>
                    <div class="history-meta">
                      <span><i class="bi bi-person-badge"></i> Vai trò: {{ item.vaiTro || 'Chưa có dữ liệu' }}</span>
                      <span class="history-status"><i class="bi bi-arrow-right-circle"></i> {{ historyStatusLabel(item) }}</span>
                    </div>
                    <div v-if="Number(item.maTrangThai) === 9" class="history-failure-reason">
                      <span>Lý do giao thất bại</span><p>{{ deliveryFailureReason(item.ghiChu) }}</p>
                    </div>
                    <p v-else>{{ historyAction(item) }}</p>
                  </div>
                </div>
              </div>
            </div>

            <div class="modal-footer">
              <button type="button" class="btn btn-light modal-cancel-button" @click="closeHistoryModal">
                Đóng
              </button>
            </div>
          </div>
        </div>
      </div>
    </main>
  </AdminLayout>
</template>

<style scoped>
.voucher-summary-label{display:flex;gap:7px;align-items:baseline;flex-wrap:wrap}.voucher-summary-label b{color:#344e5b;font-size:11px;font-weight:650}.voucher-summary-value{color:#1a9a73!important;font-size:12px;white-space:normal!important;text-align:right}.line-discount{display:grid;justify-items:start;gap:5px}.line-discount>span{display:inline-flex;padding:4px 8px;background:#e5f7ef;color:#18865e;border-radius:6px;font-weight:700;font-size:11px}.line-discount small{font-size:10px;color:#18865e;white-space:nowrap}.payment-panel .grand-total{margin-top:12px;padding-top:16px}.payment-panel .grand-total strong{font-size:19px;color:var(--ss-primary,#1689cf)}

.historical-price{display:block;color:#8b9aa2;font-size:11px}.historical-sale{color:#d32c50}
.detail-page{max-width:1700px;margin:0 auto;padding:24px 24px 36px;color:#263943}
.page-title-row{margin-bottom:20px}.breadcrumb{font-size:11px;color:#8c9ba5;margin-bottom:5px}.breadcrumb b{padding:0 9px;color:#c4d0d7}.breadcrumb a{color:#536c79;text-decoration:none}.breadcrumb strong{color:#1689cf;font-weight:600}.page-title-row h1{margin:0;font-size:23px;color:#203744;font-weight:750}
.back-link{display:flex;align-items:center;gap:10px;border:0;background:transparent;color:#435b67;padding:0 0 16px 3px;font-size:15px;font-weight:650;cursor:pointer}.back-link i{font-size:15px;color:#1689cf}.back-link b{color:#1689cf}
.detail-layout{display:grid;grid-template-columns:minmax(0,1.85fr) minmax(285px,.9fr);gap:18px;align-items:start}.main-column,.side-column{display:grid;gap:16px;min-width:0}.panel{background:#fff;border:1px solid #e5edf1!important;border-radius:14px;box-shadow:0 3px 12px rgba(35,73,91,.045)!important;padding:19px}
.panel h2{display:flex;align-items:center;gap:9px;margin:0 0 20px;color:#263f4b;font-size:13px;font-weight:700}.panel h2 i{color:#1689cf;font-size:14px}
.timeline-panel{padding-bottom:16px}.timeline{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));position:relative;padding:7px 0 0;gap:0}.timeline.timeline-pickup{grid-template-columns:repeat(3,minmax(0,1fr))}.timeline:before{content:"";position:absolute;left:8.2%;right:8.2%;height:2px;background:#dce9ef;top:24px}.timeline.timeline-pickup:before{left:16.67%;right:16.67%}.timeline-step{position:relative;display:flex;align-items:center;flex-direction:column;text-align:center;min-width:0}.timeline-node{width:38px;height:38px;border-radius:50%;display:grid;place-items:center;background:#fff;color:#a2afb6;border:2px solid #d9e5ea;box-shadow:0 0 0 4px #fff;z-index:1;font-size:14px}.timeline-step.reached .timeline-node{background:#1689cf;color:#fff;border-color:#1689cf;box-shadow:0 0 0 4px #d9f0fb}.timeline-step.current .timeline-node{box-shadow:0 0 0 4px #d9f0fb,0 0 0 6px #1689cf}.step-label{font-size:11px;font-weight:650;color:#91a0a8;margin-top:11px;line-height:1.4}.timeline-step.reached .step-label{color:#147fb9}.timeline-step small{font-size:9px;color:#98a7ae;margin-top:5px}
.timeline-actions{display:flex;align-items:center;justify-content:flex-end;gap:8px;margin-top:18px}.soft-button,.edit-order-button,.cancel-order-button{height:34px;border-radius:8px;padding:0 11px;font-size:11px;font-weight:700;display:inline-flex;align-items:center;justify-content:center;gap:6px;white-space:nowrap}.soft-button{border:1px solid #d6e7ef;background:#f7fbfd;color:#477182;min-width:0}.soft-button:hover{background:#e9f6fc;color:#0878bd}
.info-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}.info-panel{padding:18px 19px 10px}.info-panel h2{margin-bottom:13px}.info-row{display:flex;justify-content:space-between;align-items:flex-start;gap:15px;padding:11px 0;border-bottom:1px solid #edf2f5;font-size:11px}.info-row:last-child{border-bottom:0}.info-row>span{color:#82949e;flex:0 0 auto}.info-row>strong{color:#334d59;font-weight:650;text-align:right;line-height:1.55;min-width:0}.address-row strong{max-width:65%}
.shipping-panel .info-row:last-child{border-bottom:0}.shipping-fee-zero{color:#1689cf!important}

.products-panel{padding:19px 0 0}.products-panel h2{padding:0 19px}.product-table-wrap{overflow-x:auto}.product-table{width:100%;border-collapse:collapse;min-width:650px}.product-table thead{background:#edf5fa}.product-table th{height:39px;padding:0 13px;text-align:left;font-size:10px;color:#607b8a;font-weight:700;white-space:nowrap}.product-table td{padding:12px 13px;border-bottom:1px solid #edf2f5;color:#5a707b;font-size:11px;white-space:nowrap}.product-name{display:flex;align-items:center;gap:9px}.product-name strong{color:#344e5a;font-size:11px}.product-thumb{width:46px;height:46px;border-radius:8px;background:#f3f8fb;color:#1689cf;display:grid;place-items:center;font-size:16px;overflow:hidden;border:1px solid #e3edf2}.product-thumb img{width:100%;height:100%;display:block;object-fit:cover}.product-total{color:#1681c3!important;font-weight:700}.empty-products{text-align:center!important;color:#91a0a8!important}
.payment-panel h2,.payment-history h2,.invoice-meta h2{margin-bottom:18px}.amount-row{display:flex;justify-content:space-between;gap:12px;margin:0 0 13px;font-size:11px}.amount-row span{color:#7e909a}.amount-row strong{color:#334c58;font-weight:650;white-space:nowrap}.amount-row .discount{color:#16966f}.discount-row{align-items:center}.discount-value{display:flex;align-items:center;justify-content:flex-end;gap:5px;flex-wrap:wrap}.voucher-code{display:inline-flex;align-items:center;padding:2px 6px;border-radius:4px;background:#fff0df;color:#c96a16;font-size:9px;font-weight:800;line-height:1.2}.voucher-percent{display:inline-flex;align-items:center;padding:2px 5px;border-radius:4px;background:#f07c22;color:#fff;font-size:9px;font-weight:800;line-height:1.2}.shipping-fee-value{display:inline-flex;align-items:center;gap:7px}.ghn-badge{display:inline-flex;align-items:center;gap:2px;color:#ef6c20;font-size:10px;font-weight:900;letter-spacing:.2px;white-space:nowrap}.ghn-badge i{font-size:10px;color:#1675c1}.carrier-badge{display:inline-flex;align-items:center;padding:2px 5px;border-radius:4px;background:#eef5f8;color:#486673;font-size:9px;font-weight:700;white-space:nowrap}.grand-total{border-top:1px solid #e5edf1;margin-top:17px;padding-top:15px;display:flex;justify-content:space-between;gap:10px;align-items:center}.grand-total span{font-size:12px;color:#55707d;font-weight:650}.grand-total strong{font-size:18px;color:#1689cf}
.payment-history{min-height:245px;display:flex;flex-direction:column}.payment-method{display:flex;justify-content:space-between;gap:12px;align-items:flex-start}.payment-method strong{display:block;color:#3b5662;font-size:11px;font-weight:650;line-height:1.5}.payment-method small{display:block;color:#91a0a8;font-size:10px;margin-top:7px}.paid-tag{color:#168b68;font-size:10px;font-weight:650;white-space:nowrap}.paid-amount{text-align:right;color:#344e5a;font-size:13px;margin-top:10px}.payment-history-actions{display:flex;flex-direction:column;gap:8px;margin-top:auto}.payment-history-actions.single-action{display:flex;flex-direction:column}.payment-action-button{height:42px!important;width:100%!important;min-height:42px!important;box-sizing:border-box!important;display:flex!important;align-items:center!important;justify-content:center!important;padding:0 12px!important}.print-button{height:42px;width:100%;margin-top:0;border:1px solid #1689cf;border-radius:10px;background:#1689cf;color:#fff;font-size:12px;font-weight:700;cursor:pointer;--bs-btn-bg:#1689cf;--bs-btn-border-color:#1689cf;--bs-btn-hover-bg:#0d72b0;--bs-btn-hover-border-color:#0d72b0}.print-button:hover{background:#0d72b0;color:#fff}
.edit-order-button{height:42px!important;width:100%!important;min-height:42px!important;margin-top:0;box-sizing:border-box!important;background:#1689cf;border-color:#1689cf;color:#fff;font-size:12px;font-weight:700;border-radius:10px}
.edit-order-button:hover{background:#0d72b0;border-color:#0d72b0;color:#fff}.invoice-meta{padding-bottom:10px}.status-pill{display:inline-block;border-radius:13px;padding:5px 9px;background:#e7f3fc;color:#1679b5;font-size:10px}.status-pill.done,.status-pill.delivered{background:#e8f7ef;color:#168455}.status-pill.waiting{background:#fff4dd;color:#a86a08}.status-pill.cancel{background:#ffebed;color:#c64f5b}.status-pill.shipping{background:#e6f4ff;color:#1678b8}
.cancel-state{display:flex;align-items:center;gap:13px;padding:8px 0 18px}.cancel-icon{width:40px;height:40px;border-radius:50%;background:#ffebed;color:#c64f5b;display:grid;place-items:center;font-size:19px}.cancel-state strong{color:#c64f5b;font-size:13px}.cancel-state p{margin:5px 0 0;color:#84949d;font-size:11px}
.loading-panel{display:flex;align-items:center;min-height:110px;color:#667c87;font-size:12px}

.invoice-toast{position:fixed;top:88px;right:24px;z-index:1200;min-width:340px;max-width:430px;display:flex;align-items:center;gap:11px;padding:13px 14px;border:1px solid #e3eaee;border-radius:12px;background:#fff;box-shadow:0 12px 32px rgba(30,55,68,.18)}.toast-icon{width:34px;height:34px;flex:0 0 34px;border-radius:50%;display:grid;place-items:center;font-size:15px}.toast-icon.success{background:#e7f7ee;color:#16935f}.toast-icon.error{background:#ffebed;color:#d14958}.invoice-toast .toast-body{display:flex;flex-direction:column;gap:3px;min-width:0}.invoice-toast .toast-body strong{font-size:12px;color:#263f4b}.invoice-toast .toast-body span{font-size:11px;color:#687f8a;line-height:1.4}.confirm-modal{padding-top:18px}.confirm-icon{width:56px;height:56px;margin:2px auto 13px;border:3px solid #1689cf;border-radius:50%;display:grid;place-items:center;color:#1689cf;font-size:25px}.confirm-title{font-size:18px;font-weight:750;color:#263f4b;margin-bottom:8px}.confirm-message{font-size:12px;color:#667c87;line-height:1.55;margin:0}.confirm-message strong{color:#334d59}.payment-method span,.payment-amount-row span{display:block;color:#8497a0;font-size:10px;margin-bottom:5px}.payment-amount-row{display:flex;justify-content:space-between;gap:12px;margin-top:18px;padding-top:14px;border-top:1px solid #edf2f5;font-size:11px}.payment-amount-row strong{color:#344e5a;font-size:13px}.payment-note{margin-top:12px;color:#93a1a8;font-size:9.5px;line-height:1.45}.status-modal-backdrop{background:rgba(26,39,48,.52);z-index:1060}.status-modal-backdrop.confirm-layer{z-index:1075}.history-modal-backdrop{background:rgba(26,39,48,.52);z-index:1060}.status-modal,.history-modal{border:0;border-radius:14px;box-shadow:0 16px 45px rgba(25,46,58,.22)}.status-modal .modal-header,.history-modal .modal-header{border-bottom:1px solid #edf2f5;padding:18px 20px}.status-modal .modal-title,.history-modal .modal-title{color:#263f4b;font-size:16px;font-weight:750}.status-modal .modal-body,.history-modal .modal-body{padding:20px}.status-modal .modal-footer,.history-modal .modal-footer{border-top:1px solid #edf2f5;padding:14px 20px}
.status-info-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}.status-info-grid>div{padding:11px 12px;border:1px solid #e6eef2;border-radius:9px;background:#f9fbfc}.status-info-grid span{display:block;color:#8497a0;font-size:10px;margin-bottom:5px}.status-info-grid strong{display:block;color:#334d59;font-size:12px}.next-status-text{color:#1689cf!important}.status-modal .form-select{height:40px;border-radius:9px;font-size:12px;box-shadow:none;border-color:#d8e5eb}.status-modal .form-select:focus{border-color:#1689cf;box-shadow:0 0 0 .2rem rgba(22,137,207,.12)}.status-modal .form-text{font-size:10px;color:#8a9aa2;margin-top:7px}.modal-cancel-button{min-width:82px;border:1px solid #dbe5e9;color:#536b76;background:#f7fafb}.modal-save-button{min-width:82px;background:#1689cf;border-color:#1689cf}.modal-save-button:hover{background:#0d72b0;border-color:#0d72b0}
.edit-order-tabs{display:flex;gap:24px;padding:0 20px;border-bottom:1px solid #edf2f5}.edit-order-tab{position:relative;border:0;background:transparent;padding:13px 0 11px;color:#1689cf;font-size:11px;font-weight:700;cursor:pointer}.edit-order-tab:not(.active){color:#8497a0;font-weight:600}.edit-order-tab.active:after{content:"";position:absolute;left:0;right:0;bottom:-1px;height:2px;background:#1689cf;border-radius:2px}.edit-order-note{display:flex;align-items:flex-start;gap:8px;margin-top:14px;padding:10px 12px;border-radius:9px;background:#f6fafc;color:#718792;font-size:10px;line-height:1.5}.edit-order-note i{color:#1689cf;margin-top:1px}.customer-readonly-head{display:flex;align-items:center;gap:12px;padding:12px;border:1px solid #e6eef2;border-radius:10px;background:#f9fbfc;margin-bottom:14px}.customer-avatar{width:40px;height:40px;border-radius:50%;display:grid;place-items:center;background:#e7f5fb;color:#1689cf;font-size:18px}.customer-readonly-head strong{display:block;color:#334d59;font-size:12px}.customer-readonly-head span{display:block;color:#8a9ba3;font-size:10px;margin-top:3px}.customer-fields{display:grid;grid-template-columns:1fr 1fr;gap:10px}.customer-field{padding:11px 12px;border:1px solid #e6eef2;border-radius:9px;background:#fff}.customer-field span{display:block;color:#8497a0;font-size:10px;margin-bottom:5px}.customer-field strong{display:block;color:#334d59;font-size:11px;line-height:1.45;word-break:break-word}
.history-modal-dialog{max-width:760px}.history-modal{max-height:85vh}.history-body{overflow-y:auto;max-height:65vh}.history-loading,.history-empty{min-height:180px;display:flex;align-items:center;justify-content:center;color:#8798a1;font-size:12px}.history-empty{flex-direction:column;gap:8px}.history-empty i{font-size:30px;color:#c3d0d6}.history-empty p{margin:0}.history-timeline{position:relative;padding:5px 4px 5px 42px}.history-timeline:before{content:"";position:absolute;left:13px;top:12px;bottom:12px;width:2px;background:#dce9ef}.history-item{position:relative;padding-bottom:16px}.history-item:last-child{padding-bottom:0}.history-dot{position:absolute;left:-42px;top:2px;width:28px;height:28px;border-radius:50%;display:grid;place-items:center;background:#e8f5fb;color:#1689cf;border:1px solid #cde8f5;z-index:1;font-size:12px}.history-card{border:1px solid #e5edf1;border-radius:11px;padding:13px 14px;background:#fff}.history-card-top{display:flex;align-items:center;justify-content:space-between;gap:12px}.history-card-top strong{font-size:12px;color:#334d59}.history-card-top span{font-size:10px;color:#93a1a8;white-space:nowrap}.history-meta{display:flex;flex-wrap:wrap;gap:8px 16px;margin-top:7px}.history-meta span{font-size:10px;color:#7c909a}.history-meta i{color:#1689cf;margin-right:3px}.history-card p{margin:8px 0 0;color:#526a76;font-size:11px;line-height:1.5}
@media(max-width:1050px){.detail-layout{grid-template-columns:minmax(0,1.4fr) minmax(260px,.9fr)}.info-grid{grid-template-columns:1fr}.timeline{grid-template-columns:repeat(3,minmax(0,1fr));row-gap:20px}.timeline:before{display:none}}
@media(max-width:760px){.page-title-actions{width:100%;justify-content:flex-end}.page-title-actions .back-list-button{flex:1}.invoice-toast{left:12px;right:12px;top:78px;min-width:0;max-width:none}.detail-page{padding:15px}.detail-layout{grid-template-columns:1fr}.info-grid{grid-template-columns:1fr}.panel{padding:16px}.timeline{grid-template-columns:repeat(3,minmax(0,1fr));row-gap:18px}.step-label{font-size:10px}.back-link{font-size:13px}.status-info-grid{grid-template-columns:1fr}.customer-fields{grid-template-columns:1fr}.history-modal-dialog{margin:10px}.history-body{max-height:65vh}.timeline-actions{align-items:center;justify-content:flex-end}.soft-button,.edit-order-button,.cancel-order-button{width:auto}.payment-history-actions{display:flex;flex-direction:column}}
.cancel-order-button{min-width:0;height:34px;border-radius:8px;padding:0 11px;font-size:11px;font-weight:700;background:#dc3545;border-color:#dc3545}.cancel-order-button:hover{background:#bb2d3b;border-color:#bb2d3b}.cancel-confirm-icon{border-color:#dc3545;color:#dc3545}.cancel-warning{margin:8px 0 0;color:#c64f5b;font-size:11px}.cancel-confirm-button{min-width:120px}.print-invoice{display:none}.print-header{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:2px solid #1689cf;padding-bottom:14px;margin-bottom:18px}.print-brand{font-size:22px;font-weight:800;letter-spacing:1px}.print-subtitle{font-size:9px;letter-spacing:2px;margin-top:3px}.print-header-right{display:flex;align-items:flex-start;gap:18px}.print-title{font-size:18px;font-weight:800;padding-top:4px}.print-qr{display:flex;flex-direction:column;align-items:center;gap:3px}.print-qr img{width:82px;height:82px;display:block;object-fit:contain}.print-qr span{font-size:7px;color:#666;white-space:nowrap}.print-meta-grid{display:grid;grid-template-columns:1fr 1fr;gap:9px 28px;margin-bottom:20px}.print-meta-grid div{display:flex;justify-content:space-between;gap:12px;border-bottom:1px solid #ddd;padding-bottom:5px;font-size:10px}.print-meta-grid span{color:#666}.print-product-table{width:100%;border-collapse:collapse;font-size:10px}.print-product-table th,.print-product-table td{border:1px solid #ccc;padding:7px 6px;text-align:left}.print-product-table th{font-weight:700;background:#f5f5f5}.print-summary{width:300px;margin:18px 0 0 auto;font-size:10px}.print-summary div{display:flex;justify-content:space-between;padding:5px 0}.print-grand-total{border-top:2px solid #222;margin-top:5px;padding-top:9px!important;font-size:13px;font-weight:800}.print-payment{margin-top:20px;padding-top:10px;border-top:1px solid #ddd;font-size:10px}.print-payment div{margin:4px 0}.print-payment span{font-weight:700}.print-shipping{margin-top:20px;padding-top:10px;border-top:1px solid #ddd;font-size:10px}.print-shipping h3{font-size:12px;margin:0 0 8px;font-weight:800}.print-shipping div{margin:4px 0}.print-shipping span{font-weight:700}.print-footer{text-align:center;margin-top:28px;font-size:10px;font-style:italic}
.error-panel {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 28px;
  margin-top: 16px;
}

.error-panel-icon {
  font-size: 28px;
  color: #dc3545;
}

.error-panel h2 {
  margin: 0 0 8px;
  font-size: 18px;
}

.error-panel p {
  margin: 0 0 16px;
  color: #555;
}

@media print{.detail-page{padding:0}.back-list-button,.timeline-actions,.print-button{display:none}.detail-layout{grid-template-columns:1fr 1fr}.panel{box-shadow:none;break-inside:avoid}}

 .status-selector{display:flex;align-items:flex-end;gap:8px;margin-left:auto}
.status-selector label{display:block;margin-bottom:6px;color:#718690;font-size:10px;font-weight:700;white-space:nowrap}
.status-selector .form-select{width:190px;min-height:34px;border-color:#d7e5ec;border-radius:8px;font-size:11px;box-shadow:none}
.status-selector .form-select:focus{border-color:#1689cf;box-shadow:0 0 0 .2rem rgba(22,137,207,.12)}
.payment-status-row { display:flex; justify-content:space-between; gap:16px; align-items:center; padding:12px 0; border-top:1px solid #edf2f7; }
.payment-status { display:inline-flex; align-items:center; gap:6px; }
.payment-status.paid { color:#16a34a; }
.payment-status.unpaid { color:#d97706; }
@media print{html,body{margin:0!important;padding:0!important;background:#fff!important}.detail-page{padding:0!important}.detail-page>.page-title-row,.detail-page>.back-link,.detail-page>.detail-layout,.detail-page>.invoice-toast,.detail-page>.modal{display:none!important}.print-invoice{display:block!important;padding:12mm 10mm;color:#222;background:#fff;font-family:Arial,sans-serif}.print-invoice *{box-sizing:border-box}}

.payment-history-list{display:flex;flex-direction:column;gap:10px;min-height:0}.payment-history-item{border:1px solid #e6eef3;border-radius:10px;padding:12px;background:#fbfdff}.payment-history-top{display:flex;justify-content:space-between;gap:12px;align-items:flex-start}.payment-history-label{display:block;color:#91a0a8;font-size:10px;margin-bottom:4px}.payment-history-top>div>strong{display:block;color:#3b5662;font-size:12px}.payment-history-meta{display:flex;flex-wrap:wrap;gap:8px 14px;margin-top:8px;color:#7c909a;font-size:10px}.payment-history-meta i{margin-right:4px;color:#1689cf}.payment-history-amount{display:flex;justify-content:space-between;align-items:center;margin-top:9px;padding-top:9px;border-top:1px solid #edf2f7;font-size:10px}.payment-history-amount strong{font-size:13px;color:#344e5a}.payment-history-item p{margin:7px 0 0;color:#7c909a;font-size:10px}.payment-history-empty{min-height:145px;display:flex;align-items:center;justify-content:center;gap:8px;color:#93a1a8;font-size:11px}.payment-history-empty i{font-size:18px;color:#b7c7cf}
:global(body.invoice-print-mode > *){visibility:hidden!important}.invoice-print{visibility:hidden}.invoice-print *{visibility:hidden}
:global(body.invoice-print-mode .print-invoice){visibility:visible!important;display:block!important;position:fixed!important;inset:0!important;width:100%!important;min-height:100vh!important;margin:0!important;padding:12mm 14mm!important;background:#fff!important;z-index:2147483647!important;overflow:visible!important;color:#222!important;font-family:Arial,Helvetica,sans-serif!important;box-sizing:border-box!important}
:global(body.invoice-print-mode .print-invoice *){visibility:visible!important}
:global(body.invoice-print-mode .print-header){display:flex!important}.print-header{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:2px solid #1689cf;padding-bottom:14px;margin-bottom:18px}.print-brand{font-size:24px;font-weight:800;letter-spacing:1px}.print-subtitle{font-size:9px;letter-spacing:2px;margin-top:3px}.print-title{font-size:18px;font-weight:800}.print-meta-grid{display:grid;grid-template-columns:1fr 1fr;gap:9px 28px;margin-bottom:20px}.print-meta-grid div{display:flex;justify-content:space-between;gap:12px;border-bottom:1px solid #ddd;padding-bottom:5px;font-size:10px}.print-meta-grid span{color:#666}.print-product-table{width:100%;border-collapse:collapse;font-size:10px}.print-product-table th,.print-product-table td{border:1px solid #ccc;padding:7px 6px;text-align:left}.print-product-table th{font-weight:700;background:#f5f5f5}.print-summary{width:300px;margin:18px 0 0 auto;font-size:10px}.print-summary div{display:flex;justify-content:space-between;padding:5px 0}.print-grand-total{border-top:2px solid #222;margin-top:5px;padding-top:9px!important;font-size:13px;font-weight:800}.print-payment{margin-top:20px;padding-top:10px;border-top:1px solid #ddd;font-size:10px}.print-payment div{margin:4px 0}.print-payment span{font-weight:700}.print-shipping{margin-top:20px;padding-top:10px;border-top:1px solid #ddd;font-size:10px}.print-shipping h3{font-size:12px;margin:0 0 8px;font-weight:800}.print-shipping div{margin:4px 0}.print-shipping span{font-weight:700}.print-footer{text-align:center;margin-top:28px;font-size:10px;font-style:italic}
@media print{html,body{margin:0!important;padding:0!important;background:#fff!important}.detail-page{padding:0!important}.detail-page>.page-title-row,.detail-page>.back-link,.detail-page>.detail-layout,.detail-page>.invoice-toast,.detail-page>.modal{display:none!important}.print-invoice{display:block!important;visibility:visible!important;position:static!important;width:auto!important;min-height:0!important;padding:0!important;color:#222!important;background:#fff!important}.print-invoice *{visibility:visible!important}.payment-history{min-height:0}.print-button{display:block!important}}

/* ===== Giao diện chi tiết hóa đơn theo mẫu ===== */
.page-title-row{
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:16px;
  margin-bottom:10px;
}
.page-title-row .breadcrumb{margin:0;font-size:14px}
.page-title-actions{display:flex;align-items:center;gap:8px;flex-wrap:wrap}
.page-title-row .breadcrumb a{font-weight:600;color:#7f98a6}
.page-title-row .breadcrumb strong{font-weight:750;color:#263943}
.back-list-button{
  display:inline-flex;
  align-items:center;
  gap:7px;
  height:34px;
  padding:0 12px;
  border:1px solid #dbe8ee;
  border-radius:9px;
  background:#fff;
  color:#5d7480;
  font-size:11px;
  font-weight:650;
  cursor:pointer;
}
.back-list-button:hover{border-color:#b9d8e7;color:#1689cf;background:#f7fbfd}

.invoice-summary-strip{
  display:grid;
  grid-template-columns:repeat(3,minmax(0,1fr));
  align-items:center;
  gap:0;
  min-height:72px;
  margin-bottom:16px;
  padding:8px 16px;
  border:1px solid #e8eef2;
  border-radius:10px;
  background:#f8fafb;
}
.invoice-summary-strip>div{
  display:flex;
  align-items:center;
  gap:7px;
  min-width:0;
  min-height:27px;
  padding:0 14px;
  border-right:1px solid #e6edf1;
  font-size:11px;
  white-space:nowrap;
}
.invoice-summary-strip>div:nth-child(3n){border-right:0}
.invoice-summary-strip>div:nth-child(n+4){border-top:1px solid #e6edf1}
.invoice-summary-strip span{
  color:#7b8d96;
  flex:0 0 auto;
}
.invoice-summary-strip strong{
  color:#263943;
  font-weight:700;
  min-width:0;
  overflow:hidden;
  text-overflow:ellipsis;
}
.invoice-summary-strip>div:first-child strong{color:#1689cf}

.products-panel{
  width:100%;
  margin-top:18px;
  padding:18px 0 0;
  overflow:hidden;
}
.products-heading{
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:12px;
  padding:0 20px;
}
.products-heading h2{margin-bottom:16px;padding:0}
.products-count{font-size:10px;color:#8a9aa3;white-space:nowrap}

.product-filter-box{
  display:grid;
  grid-template-columns:minmax(260px,1.6fr) minmax(145px,.8fr) minmax(135px,.7fr) minmax(120px,.65fr) minmax(145px,.8fr) auto;
  gap:10px;
  align-items:end;
  margin:0 20px 16px;
  padding:14px;
  border:1px solid #e4edf2;
  border-radius:10px;
  background:#fbfdfe;
}
.product-filter-field{min-width:0}
.product-filter-field label{
  display:block;
  margin:0 0 6px;
  color:#718690;
  font-size:9px;
  font-weight:650;
}
.product-filter-field select,
.product-search-input{
  width:100%;
  height:38px;
  border:1px solid #d8e5eb;
  border-radius:8px;
  background:#fff;
  color:#3f5965;
  font-size:10px;
  outline:none;
}
.product-filter-field select{padding:0 10px}
.product-search-input{display:flex;align-items:center;padding:0 10px}
.product-search-input i{color:#91a3ad;margin-right:7px}
.product-search-input input{
  width:100%;
  border:0;
  outline:0;
  background:transparent;
  color:#3f5965;
  font-size:10px;
}
.product-filter-field select:focus,
.product-search-input:focus-within{
  border-color:#1689cf;
  box-shadow:0 0 0 2px rgba(22,137,207,.08);
}
.product-reset-button{
  height:38px;
  padding:0 11px;
  border:1px solid #bcddeb;
  border-radius:8px;
  background:#fff;
  color:#1689cf;
  font-size:10px;
  font-weight:700;
  white-space:nowrap;
  cursor:pointer;
}
.product-reset-button:hover{background:#edf8fd}

.product-table-wrap{
  width:100%;
  overflow-x:auto;
  overflow-y:hidden;
  scrollbar-width:thin;
}
.product-table{
  width:100%;
  min-width:1180px;
  border-collapse:separate;
  border-spacing:0;
}
.product-table thead{background:#edf4f8}
.product-table th{
  height:40px;
  padding:0 13px;
  text-align:left;
  font-size:10px;
  color:#58717e;
  font-weight:750;
  white-space:nowrap;
  border-bottom:1px solid #e0e9ee;
}
.product-table td{
  min-height:62px;
  padding:11px 13px;
  border-bottom:1px solid #edf2f5;
  color:#566e79;
  font-size:10.5px;
  white-space:nowrap;
  vertical-align:middle;
}
.product-table tbody tr:hover{background:#fbfdfe}
.product-code{font-weight:650;color:#566e79}
.product-info-cell{min-width:270px;white-space:normal!important}
.product-info-cell strong{
  display:block;
  color:#344e5a;
  font-size:11px;
  font-weight:700;
  line-height:1.35;
}
.product-info-cell small{
  display:block;
  margin-top:3px;
  color:#8a9ba4;
  font-size:9px;
  line-height:1.35;
}
.product-info-cell .product-subline{color:#a0adb3}
.product-thumb{
  width:42px;
  height:42px;
  border-radius:7px;
  background:#edf7fb;
  color:#1689cf;
  display:grid;
  place-items:center;
  font-size:17px;
}
.product-time{line-height:1.35}
.product-time small{display:block;color:#8d9da5;font-size:9px;margin-top:2px}
.discount-empty{color:#a2adb2}
.product-total{color:#1681c3!important;font-weight:750}
.nowrap{white-space:nowrap!important}
.empty-products{text-align:center!important;color:#91a0a8!important;padding:22px!important}

@media(max-width:1100px){
  .product-filter-box{grid-template-columns:1fr 1fr 1fr}
  .product-search-field{grid-column:1 / -1}
}
@media(max-width:760px){
  .page-title-row{align-items:flex-start;flex-direction:column}
  .invoice-summary-strip{
    grid-template-columns:repeat(2,minmax(0,1fr));
    gap:6px 18px;
  }
  .product-filter-box{grid-template-columns:1fr 1fr}
  .product-search-field{grid-column:1 / -1}
  .product-reset-button{width:100%}
}

</style>

<style scoped>
.shipping-provider-label { display: inline-flex; align-items: center; gap: 6px; flex-wrap: wrap; }
</style>

<style scoped>
.print-carrier { display: flex; align-items: center; gap: 6px; }
.print-invoice :deep(.ghn-logo) { width: 46px; height: 14px; }
</style>

<style scoped>
/* Return handling uses the same blue palette as the invoice, with a distinct final action. */
.return-modal-backdrop{display:block;background:rgba(20,35,52,.5);z-index:1070;padding:16px;overflow-y:auto}
.return-modal-dialog{max-width:640px;margin:16px auto;min-height:calc(100% - 32px)}
.return-modal{border:0;border-radius:18px;overflow:hidden;box-shadow:0 24px 80px rgba(15,33,55,.24);max-height:calc(100dvh - 64px);color:#263d52}
.return-modal .modal-header{padding:20px 24px;border-bottom:1px solid #e4ebf3;align-items:flex-start;background:#fff}
.return-modal-heading{display:flex;align-items:center;gap:12px}
.return-heading-icon{display:grid;place-items:center;width:44px;height:44px;border-radius:12px;background:#eaf3ff;color:#2476de;font-size:21px;flex-shrink:0}
.return-modal h5{margin:0;font-size:20px;font-weight:750;color:#193650;line-height:1.4}
.return-modal-heading p{margin:3px 0 0;font-size:13px;color:#52677c}.return-modal-heading p strong{color:#2466ac}
.return-close{display:grid;place-items:center;width:32px;height:32px;border:0;border-radius:8px;background:#f3f6fa;color:#53697b;font-size:14px;cursor:pointer}
.return-close:hover{background:#e6edf5;color:#18364c}
.return-modal .modal-body{padding:20px 24px;overflow-y:auto;min-height:0;overscroll-behavior:contain}
.return-summary{padding:12px 14px;border:1px solid #f3d5c6;border-radius:11px;background:#fff8f3;margin-bottom:18px}
.return-attempt{display:inline-flex;align-items:center;gap:7px;font-size:13px;font-weight:750;color:#a14319}
.return-summary p{font-size:13px;line-height:1.55;color:#6b4b3b;margin:5px 0 0}
.return-section{padding:0 0 18px;margin-bottom:18px;border-bottom:1px solid #e6edf4}
.return-section h6{display:flex;align-items:center;gap:9px;margin:0 0 12px;font-size:14px;font-weight:750;color:#243e56}
.return-step{display:grid;place-items:center;width:24px;height:24px;border-radius:7px;background:#edf4fd;color:#246ec2;font-size:12px}
.return-check{display:flex;align-items:flex-start;gap:11px;padding:12px;margin:0 0 9px;border:1px solid #dce5ef;border-radius:10px;background:#fff;cursor:pointer;transition:background .15s,border-color .15s}
.return-check.checked{background:#f2f7ff;border-color:#8bb7f0}.return-check input{width:18px;height:18px;flex:0 0 18px;margin:2px 0 0;accent-color:#2878dc;cursor:pointer}
.return-check span{min-width:0}.return-check strong{display:block;color:#243c52;font-size:13px;font-weight:650;line-height:1.5}
.return-check small{display:block;margin-top:3px;color:#536b80;font-size:12px;line-height:1.5}
.return-hint{display:flex;gap:7px;margin:9px 0 0;color:#566d80;font-size:12px;line-height:1.6}.return-hint i{flex-shrink:0;color:#3978b9}
.return-refund-amount{display:flex;justify-content:space-between;align-items:center;gap:12px;padding:13px 14px;border-radius:10px;background:#eef8f4;margin-bottom:12px}
.return-refund-amount span{font-size:13px;color:#365b50}.return-refund-amount strong{font-size:20px;color:#087957;white-space:nowrap}
.return-field{display:block;font-size:13px;font-weight:650;color:#314a60;margin-top:12px}.return-field>span{color:#c73d4b}
.return-field .form-control{margin-top:7px;border:1px solid #cddbe8;border-radius:9px;padding:10px 12px;font-size:13px;color:#243d53;background:#fff;line-height:1.6;box-shadow:none}
.return-field .form-control::placeholder{color:#6d8091;font-weight:400}.return-field .form-control:focus{border-color:#4e94e7;box-shadow:0 0 0 3px rgba(49,125,220,.12)}
.return-field textarea{min-height:86px;resize:vertical}.return-note-section{padding:0;margin:0;border:0}.return-note-count{text-align:right;margin-top:5px;font-size:11px;color:#5c7084}
.return-no-refund{display:flex;gap:8px;padding:12px;border-radius:9px;background:#f0f8f4;color:#27654d;font-size:13px}
.return-modal .modal-footer{padding:16px 24px;gap:10px;background:#f8fafc;border-top:1px solid #e4ebf3;flex-shrink:0}
.return-modal .modal-footer>button{margin:0;min-height:40px;padding:10px 16px;border-radius:9px;font-size:13px;font-weight:700;display:inline-flex;align-items:center;justify-content:center;gap:8px;cursor:pointer}
.return-secondary{border:1px solid #cbd8e5;background:#fff;color:#425d74}.return-submit{border:1px solid #cb3d4c;background:#d94151;color:#fff}.return-submit:hover:not(:disabled){background:#bf3040}
.return-modal button:disabled{opacity:.55;cursor:not-allowed}.return-modal :is(button,input):focus-visible{outline:3px solid #80b5f5;outline-offset:3px}
.cancel-state{align-items:flex-start}.cancel-state>div{min-width:0;flex:1}.cancel-state strong{font-size:15px;line-height:1.5;color:#b63845}.cancel-state>div>p{font-size:13px;line-height:1.65;color:#4f6578;margin-top:6px}.cancel-icon{flex-shrink:0;margin-top:2px}
.delivery-failure-reason{background:#fff5f4;border:1px solid #f0cbc8;border-left:3px solid #d64f55;border-radius:9px;padding:12px 14px;margin-top:13px;overflow-wrap:anywhere}
.failure-reason-label{display:flex;align-items:center;gap:7px;font-size:12px;font-weight:750;color:#a83740}
.cancel-state .delivery-failure-reason p{font-size:15px;font-weight:600;line-height:1.65;color:#593338;margin:7px 0 0;white-space:pre-wrap}
.failure-reason-date{display:block;color:#765e63;font-size:11px;margin-top:8px}
.history-modal .modal-title{font-size:18px;color:#20384d}.history-modal .modal-header small{color:#52677a!important;font-size:13px}
.history-card{padding:16px 18px;border-color:#d6e2ed;background:#fff}.history-card-top strong{font-size:14px;font-weight:750;color:#203b51}.history-card-top span{font-size:12px;color:#52677b}
.history-meta{align-items:center;margin-top:9px;gap:8px 14px}.history-meta span{font-size:12px;color:#4a6277}.history-meta .history-status{padding:4px 8px;border-radius:6px;background:#ecf4ff;color:#215f9f;font-weight:650}.history-meta i{color:inherit}
.history-card p{margin-top:10px;font-size:14px;color:#304b61;line-height:1.7;white-space:pre-wrap;overflow-wrap:anywhere}
.history-failure .history-dot{color:#bd3c46;background:#fff0ee;border-color:#efc5c5}.history-failure .history-card{border-left:3px solid #d65b62}.history-failure .history-status{color:#a52f3b;background:#fff0ef}
.history-refund .history-dot{color:#7353af;background:#f4efff;border-color:#dcd0ef}.history-refund .history-status{color:#67439c;background:#f2ecfb}
.history-failure-reason{padding:10px 12px;background:#fff6f5;border-radius:8px;margin-top:12px}.history-failure-reason>span{font-size:11px;font-weight:750;color:#a33741}.history-card .history-failure-reason p{margin:4px 0 0;color:#593338;font-weight:600}
@media(max-width:600px){.return-modal-backdrop{padding:8px}.return-modal-dialog{margin:8px auto;min-height:calc(100% - 16px)}.return-modal{max-height:calc(100dvh - 32px);border-radius:14px}.return-modal .modal-header,.return-modal .modal-body{padding:16px}.return-modal h5{font-size:18px}.return-modal .modal-footer{padding:12px 16px}.return-refund-amount{align-items:flex-start;flex-direction:column;gap:5px}.history-card{padding:13px}.history-card-top{align-items:flex-start;flex-direction:column;gap:5px}.history-timeline{padding-left:34px}.history-dot{left:-34px}.history-timeline:before{left:13px}.history-meta{gap:7px}.delivery-failure-reason{padding:10px 12px}}
</style>

<style scoped>
.cancelled-timeline-scroll{overflow-x:auto;padding:8px 6px 12px;scrollbar-width:thin;scrollbar-color:#bbd5e7 transparent}
.timeline.timeline-cancelled{grid-template-columns:repeat(var(--cancel-step-count),minmax(112px,1fr));min-width:calc(var(--cancel-step-count) * 112px);padding-top:7px;gap:0}
.timeline.timeline-cancelled:before{display:block;left:calc(50% / var(--cancel-step-count));right:calc(50% / var(--cancel-step-count));top:24px;background:#cce3f3}
.timeline-cancelled .step-label{font-size:12px;line-height:1.5;padding:0 5px}
.timeline-cancelled .timeline-step small{font-size:10px;color:#526b7e;line-height:1.5;white-space:nowrap}
.timeline-cancelled .cancelled-step-failed .timeline-node{background:#c66a23;border-color:#c66a23;box-shadow:0 0 0 4px #fff0e3}
.timeline-cancelled .cancelled-step-failed .step-label{color:#a34f18}
.timeline-cancelled .cancelled-step-refund .timeline-node{background:#7853b3;border-color:#7853b3;box-shadow:0 0 0 4px #f0e9fb}
.timeline-cancelled .cancelled-step-refund .step-label{color:#6b449f}
.timeline-cancelled .cancelled-step-cancel.current .timeline-node{background:#d44756;border-color:#d44756;box-shadow:0 0 0 4px #ffe9ed,0 0 0 6px #d44756}
.timeline-cancelled .cancelled-step-cancel .step-label{color:#bc3343;font-weight:750}
.cancelled-timeline-scroll:focus-visible{outline:2px solid #76aee5;outline-offset:3px;border-radius:8px}
</style>

<style scoped>
.goods-handled-tag{display:inline-flex;align-items:center;gap:6px;color:#167755;font-size:12px;font-weight:650;margin-right:auto}
.refund-workflow{border:1px solid #dbe7f3;background:#f6faff;border-radius:11px;padding:14px;margin:14px 0}
.refund-workflow-heading,.refund-workflow-amount{display:flex;justify-content:space-between;align-items:center;gap:10px}
.refund-workflow-heading strong{font-size:14px;color:#263f54}.refund-workflow-heading>span{font-size:11px;font-weight:700;color:#71529f;background:#f0eafb;padding:4px 7px;border-radius:6px}
.refund-workflow p{font-size:12px;color:#50687c;line-height:1.6;margin:10px 0 0}.refund-workflow-amount{margin-top:12px;font-size:12px;color:#476278}.refund-workflow-amount strong{font-size:17px;color:#137d60}
.ss-refund-button{display:flex;justify-content:center;align-items:center;gap:7px;border:1px solid #337cd5;border-radius:8px;color:#fff;background:#337cd5;width:100%;padding:10px;margin-top:12px;font-weight:700;font-size:13px}
.ss-refund-button:disabled{opacity:.5;cursor:not-allowed}
</style>

<style scoped>
/* One row at every zoom level; overflow belongs to the timeline, not the page. */
.timeline-panel .invoice-timeline-scroll{container-type:normal;min-width:0;max-width:100%;overflow-x:auto;overflow-y:hidden;padding:10px 8px 14px;scrollbar-width:thin;scrollbar-color:#9fbfd5 #f0f5f9;overscroll-behavior-x:contain;-webkit-overflow-scrolling:touch}
.timeline-panel .invoice-timeline-scroll::-webkit-scrollbar{height:8px}
.timeline-panel .invoice-timeline-scroll::-webkit-scrollbar-track{background:#f0f5f9;border-radius:8px}
.timeline-panel .invoice-timeline-scroll::-webkit-scrollbar-thumb{background:#9fbfd5;border-radius:8px}
.timeline-panel .invoice-timeline-scroll>.timeline{display:grid;grid-template-columns:repeat(var(--timeline-step-count),minmax(124px,1fr));min-width:calc(var(--timeline-step-count) * 124px);width:100%;padding-top:7px;gap:0;row-gap:0}
.timeline-panel .invoice-timeline-scroll>.timeline:before{display:block;left:calc(50% / var(--timeline-step-count));right:calc(50% / var(--timeline-step-count));top:24px}
.timeline-panel .invoice-timeline-scroll .timeline-step{min-width:0;padding:0 4px}
.timeline-panel .invoice-timeline-scroll .timeline-step small{white-space:nowrap;text-align:center;font-size:10px;color:#526b7e;line-height:1.5}
.timeline-panel .invoice-timeline-scroll .step-label{font-size:12px;line-height:1.5}
.timeline-cancelled .cancelled-step-refund.current .timeline-node{box-shadow:0 0 0 4px #f0e9fb,0 0 0 6px #7853b3}
.return-modal>.modal-header{display:flex;flex-direction:row;justify-content:space-between;align-items:flex-start;gap:16px}
.return-modal>.modal-header>.return-modal-heading{flex:1 1 auto;min-width:0}
.return-modal>.modal-header>.return-close{flex:0 0 32px;margin-left:auto;align-self:flex-start}
</style>

<style scoped>
.timeline-actions{gap:10px;flex-wrap:wrap;align-items:center}
.timeline-actions .invoice-flow-action{display:inline-flex;align-items:center;justify-content:center;gap:8px;min-height:38px;box-sizing:border-box;padding:9px 14px;border:1px solid transparent;border-radius:9px;font-family:inherit;font-size:12px;font-weight:700;line-height:18px;white-space:nowrap;cursor:pointer;transition:background-color .15s,border-color .15s,box-shadow .15s}
.invoice-flow-action i{font-size:15px;line-height:1;flex-shrink:0}
.timeline-actions .invoice-flow-action--return{background:#2f7bea;border-color:#2f7bea;color:#fff;box-shadow:0 2px 5px rgba(47,123,234,.12)}
.timeline-actions .invoice-flow-action--return:hover:not(:disabled){background:#2268ce;border-color:#2268ce;box-shadow:0 3px 9px rgba(47,123,234,.2)}
.timeline-actions .invoice-flow-action--cancel{background:#fff1f3;border-color:#efbcc4;color:#bb3345}
.timeline-actions .invoice-flow-action--cancel:hover:not(:disabled){background:#ffe4e9;border-color:#db7e8e;color:#a42536}
.timeline-actions .invoice-flow-action:focus-visible{outline:3px solid #9fc7ff;outline-offset:3px}
.timeline-actions .invoice-flow-action--cancel:focus-visible{outline-color:#efabb7}
.timeline-actions .invoice-flow-action:disabled{opacity:.55;cursor:not-allowed;box-shadow:none}
.timeline-actions .soft-button{height:38px;min-height:38px;padding:9px 13px;border-radius:9px;font-size:12px;line-height:18px}
@media(max-width:540px){.timeline-actions{gap:8px}.timeline-actions .invoice-flow-action,.timeline-actions .soft-button{flex:1 1 auto}.timeline-actions .goods-handled-tag{flex-basis:100%;margin-bottom:3px}}
@media(prefers-reduced-motion:reduce){.timeline-actions .invoice-flow-action{transition:none}}
</style>

<style scoped>
.return-field .form-control.refund-reference-invalid{border-color:#d43c50;background:#fffafb}
.return-field .form-control.refund-reference-invalid:focus{box-shadow:0 0 0 3px rgba(212,60,80,.12)}
.refund-reference-error{display:block;margin-top:6px;color:#b52d41;font-size:12px;font-weight:600;line-height:1.5}
.refund-reference-hint{display:block;margin-top:6px;color:#52697d;font-size:11px;font-weight:400;line-height:1.6}
</style>
