<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { invoiceService } from '../services/invoiceService'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const invoice = ref(null)
const loadError = ref('')
const selectedStatus = ref('')
const showStatusModal = ref(false)
const showHistoryModal = ref(false)
const updatingStatus = ref(false)
const loadingHistory = ref(false)
const historyItems = ref([])
const showConfirmModal = ref(false)
const confirmSubmitting = ref(false)
const showCancelModal = ref(false)
const toast = ref({ visible: false, type: 'success', title: '', message: '' })
let toastTimer = null

const steps = [
  { key: 'waiting', label: 'Chờ xác nhận', icon: 'bi-hourglass-split', value: 0 },
  { key: 'confirmed', label: 'Đã xác nhận', icon: 'bi-check2-circle', value: 1 },
  { key: 'ready', label: 'Chờ giao hàng', icon: 'bi-box-seam', value: 2 },
  { key: 'shipping', label: 'Đang giao hàng', icon: 'bi-truck', value: 3 },
  { key: 'delivered', label: 'Đã giao hàng', icon: 'bi-bag-check', value: 4 },
  { key: 'done', label: 'Hoàn thành', icon: 'bi-flag', value: 5 }
]

const statusOptions = steps.map(step => ({
  value: step.value,
  key: step.key,
  label: step.label
}))

const progress = computed(() => {
  const code = Number(invoice.value?.statusCode)
  return steps.some(step => step.value === code) ? code : -1
})

const nextStatus = computed(() => {
  if (!invoice.value?.nextStatusCode && invoice.value?.nextStatusCode !== 0) return null
  return {
    value: Number(invoice.value.nextStatusCode),
    label: invoice.value.nextStatusLabel || 'Trạng thái kế tiếp'
  }
})

const canUpdateStatus = computed(() => Boolean(nextStatus.value))

const canCancelOrder = computed(() => {
  const code = Number(invoice.value?.statusCode)
  return Number.isInteger(code) && code >= 0 && code <= 4
})

const hasShippingInfo = computed(() => {
  const current = invoice.value
  if (!current) return false
  return Boolean(
    current.shippingCarrier ||
    current.shippingRecipient ||
    current.shippingRecipientPhone ||
    current.shippingAddress ||
    current.shippingNote ||
    Number(current.shippingFee || 0) !== 0
  )
})

const subtotal = computed(() => Number(invoice.value?.totalBeforeAdjustments ?? 0))

function money(value) {
  return `${Number(value || 0).toLocaleString('vi-VN')} đ`
}

function formatDate(value) {
  if (!value) return '—'
  return String(value)
}

function formatHistoryDate(value) {
  return value || '—'
}

function mapInvoice(data) {
  const statusCode = Number(data?.maTrangThai)
  const status = statusOptions.find(item => item.value === statusCode)

  return {
    id: data?.id,
    code: data?.maHoaDon || route.params.code,
    employee: data?.tenNhanVien || 'Chưa cập nhật',
    employeeId: data?.idNhanVien,
    employeeCode: data?.maNhanVien || '—',
    customer: data?.tenKhachHang || data?.hoTenNguoiNhan || 'Khách lẻ',
    customerCode: data?.maKhachHang || '—',
    phone: data?.soDienThoai || '—',
    recipient: data?.hoTenNguoiNhan || data?.tenKhachHang || '—',
    recipientPhone: data?.soDienThoaiNguoiNhan || data?.soDienThoai || '—',
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
    shippingFee: Number(data?.phiVanChuyen ?? 0),
    status: data?.trangThai || status?.label || 'Không xác định',
    statusClass: data?.lopTrangThai || status?.key || 'unknown',
    statusCode: statusCode,
    nextStatusCode: data?.maTrangThaiTiepTheo,
    nextStatusLabel: data?.trangThaiTiepTheo,
    payment: data?.phuongThucThanhToan || 'Chưa cập nhật',
    paymentStatus: data?.trangThaiThanhToan || 'Chưa cập nhật',
    paymentStatusCode: Number.isFinite(Number(data?.maTrangThaiThanhToan)) ? Number(data?.maTrangThaiThanhToan) : null,
    paymentDate: data?.ngayThanhToan || null,
    address: data?.diaChiGiaoHang || data?.diaChi || 'Chưa cập nhật',
    shippingCarrier: data?.donViVanChuyen || '—',
    note: data?.ghiChu || '—',
    items: (data?.chiTietHoaDon || []).map(item => ({
      id: item?.id,
      productDetailId: item?.idSanPhamChiTiet,
      productDetailCode: item?.maSanPhamChiTiet || '—',
      sku: item?.maSku || '—',
      name: item?.tenSanPham || 'Sản phẩm',
      variant: [item?.mauSac, item?.kichThuoc].filter(Boolean).join(' / ') || '—',
      quantity: Number(item?.soLuong || 0),
      price: Number(item?.donGia || 0),
      lineTotal: Number(item?.thanhTien ?? (Number(item?.donGia || 0) * Number(item?.soLuong || 0)))
    }))
  }
}

async function loadInvoice() {
  loading.value = true
  loadError.value = ''
  try {
    const response = await invoiceService.getById(route.params.code)
    if (!response?.success || !response?.data) {
      throw new Error(response?.message || 'Không thể tải chi tiết hóa đơn')
    }
    invoice.value = mapInvoice(response.data)
  } catch (error) {
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
    loading.value = false
  }
}

function openStatusModal() {
  if (!canUpdateStatus.value || updatingStatus.value) return
  selectedStatus.value = nextStatus.value.value
  showStatusModal.value = true
}

function closeStatusModal() {
  if (updatingStatus.value) return
  showStatusModal.value = false
  selectedStatus.value = ''
}

function openConfirmModal() {
  if (!canUpdateStatus.value || !selectedStatus.value || updatingStatus.value) return
  showStatusModal.value = false
  showConfirmModal.value = true
}

function closeConfirmModal() {
  if (confirmSubmitting.value) return
  showConfirmModal.value = false
  if (canUpdateStatus.value) {
    showStatusModal.value = true
  }
}

function openCancelModal() {
  if (!canCancelOrder.value || confirmSubmitting.value) return
  showCancelModal.value = true
}

function closeCancelModal() {
  if (confirmSubmitting.value) return
  showCancelModal.value = false
}

async function cancelOrder() {
  if (!canCancelOrder.value || confirmSubmitting.value) return

  confirmSubmitting.value = true
  try {
    const response = await invoiceService.updateStatus(invoice.value.code, {
      trangThai: 6,
      ghiChu: 'Hủy đơn hàng'
    })

    if (!response?.success) {
      throw new Error(response?.message || 'Không thể hủy đơn hàng')
    }

    if (response.data) {
      invoice.value = mapInvoice(response.data)
    } else {
      await loadInvoice()
    }

    showCancelModal.value = false
    showLocalToast('success', 'Hủy đơn hàng thành công', response.message || 'Đơn hàng đã được chuyển sang trạng thái Đã hủy.')
  } catch (error) {
    showCancelModal.value = false
    showLocalToast(
      'error',
      'Hủy đơn hàng thất bại',
      error?.response?.data?.message || error?.message || 'Không thể hủy đơn hàng.'
    )
  } finally {
    confirmSubmitting.value = false
  }
}

function showLocalToast(type, title, message) {
  toast.value = { visible: true, type, title, message }
  if (toastTimer) window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => {
    toast.value.visible = false
  }, 4500)
}

async function saveStatus() {
  const newCode = Number(nextStatus.value?.value)
  if (!Number.isInteger(newCode)) {
    showLocalToast('error', 'Cập nhật thất bại', 'BE chưa cung cấp trạng thái kế tiếp hợp lệ. Vui lòng tải lại dữ liệu.')
    return
  }

  confirmSubmitting.value = true
  updatingStatus.value = true
  try {
    const response = await invoiceService.updateStatus(invoice.value.code, {
      trangThai: newCode
    })

    if (!response?.success) {
      throw new Error(response?.message || 'Không thể cập nhật trạng thái hóa đơn')
    }

    if (response.data) {
      invoice.value = mapInvoice(response.data)
    } else {
      await loadInvoice()
    }

    showConfirmModal.value = false
    showStatusModal.value = false
    selectedStatus.value = ''
    const message = response.message || 'Cập nhật trạng thái hóa đơn thành công'
    showLocalToast('success', 'Cập nhật thành công', message)
  } catch (error) {
    const message = error?.response?.data?.message || error?.message || 'Không thể cập nhật trạng thái hóa đơn.'
    showConfirmModal.value = false
    showLocalToast('error', 'Cập nhật thất bại', message)
  } finally {
    confirmSubmitting.value = false
    updatingStatus.value = false
  }
}

async function openHistoryModal() {
  showHistoryModal.value = true
  loadingHistory.value = true

  try {
    const response = await invoiceService.getHistory(invoice.value.code)
    if (!response?.success) {
      throw new Error(response?.message || 'Không thể tải lịch sử hóa đơn')
    }

    // BE đã trả theo ngayTao DESC; giữ nguyên thứ tự để item mới nhất ở trên.
    historyItems.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
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

function historyActor(item) {
  return item?.tenNhanVien || (item?.idNhanVien != null ? `Nhân viên #${item.idNhanVien}` : 'Hệ thống / chưa xác định')
}

function historyAction(item) {
  if (item?.ghiChu) return item.ghiChu
  return item?.trangThai ? `Chuyển sang trạng thái: ${item.trangThai}` : 'Cập nhật hóa đơn'
}

function printInvoice() {
  window.print()
}

onMounted(loadInvoice)
</script>

<template>
  <AdminLayout>
    <main class="detail-page container-fluid">
      <div class="page-title-row">
        <div>
          <div class="breadcrumb">
            <span>Trang chủ</span><b>/</b>
            <RouterLink to="/hoa-don">Hóa đơn</RouterLink><b>/</b>
            <strong>Chi tiết</strong>
          </div>
          <h1>Quản Lý Hóa Đơn</h1>
        </div>
      </div>

      <button class="back-link" @click="router.push('/hoa-don')">
        <i class="bi bi-chevron-left"></i>
        <span>Chi tiết hóa đơn: <b>{{ invoice?.code || route.params.code }}</b></span>
      </button>

      <div v-if="loading" class="panel card shadow-sm loading-panel">
        <div class="spinner-border spinner-border-sm text-primary me-2"></div>
        Đang tải chi tiết hóa đơn...
      </div>

      <div v-else-if="invoice" class="detail-layout">
        <div class="main-column">
          <section class="panel card shadow-sm timeline-panel">
            <h2><i class="bi bi-clipboard2-check"></i> Trạng thái đơn hàng</h2>

            <div v-if="progress >= 0" class="timeline">
              <div
                v-for="(step, index) in steps"
                :key="step.key"
                class="timeline-step"
                :class="{ reached: index <= progress, current: index === progress }"
              >
                <div class="timeline-node">
                  <i :class="['bi', step.icon]"></i>
                </div>
                <div class="step-label">{{ step.label }}</div>
                <small v-if="index <= progress">{{ invoice.date }}</small>
              </div>
            </div>

            <div v-else class="cancel-state">
              <span class="cancel-icon"><i class="bi bi-x-circle"></i></span>
              <div>
                <strong>{{ invoice.status }}</strong>
                <p>Đơn hàng đã dừng ở trạng thái này.</p>
              </div>
            </div>

            <div class="timeline-actions">
              <button
                class="soft-button btn btn-outline-primary btn-sm"
                type="button"
                @click="openHistoryModal"
              >
                <i class="bi bi-clock-history"></i>
                Lịch sử thao tác
              </button>

              <button
                v-if="canUpdateStatus"
                class="status-update-button btn btn-primary"
                type="button"
                @click="openStatusModal"
              >
                <i class="bi bi-arrow-repeat"></i>
                Cập nhật trạng thái
              </button>

              <button
                v-if="canCancelOrder"
                class="cancel-order-button btn btn-danger"
                type="button"
                @click="openCancelModal"
              >
                <i class="bi bi-x-circle"></i>
                Hủy đơn hàng
              </button>
            </div>
          </section>

          <div class="info-grid">
            <section class="panel card shadow-sm info-panel">
              <h2><i class="bi bi-person-lock"></i> Thông tin khách hàng</h2>
              <div class="info-row"><span>Tên khách hàng</span><strong>{{ invoice.customer }}</strong></div>
              <div class="info-row"><span>Mã khách hàng</span><strong>{{ invoice.customerCode }}</strong></div>
              <div class="info-row"><span>Số điện thoại</span><strong>{{ invoice.phone }}</strong></div>
              <div class="info-row"><span>Người nhận</span><strong>{{ invoice.recipient }}</strong></div>
              <div class="info-row"><span>SĐT nhận hàng</span><strong>{{ invoice.recipientPhone }}</strong></div>
            </section>

            <section v-if="hasShippingInfo" class="panel card shadow-sm info-panel">
              <h2><i class="bi bi-geo-alt"></i> Thông tin giao hàng</h2>
              <div v-if="invoice.shippingCarrier" class="info-row">
                <span>Đơn vị vận chuyển</span><strong>{{ invoice.shippingCarrier }}</strong>
              </div>
              <div class="info-row">
                <span>Phí vận chuyển</span><strong>{{ money(invoice.shippingFee) }}</strong>
              </div>
              <div v-if="invoice.shippingRecipient" class="info-row">
                <span>Người nhận</span><strong>{{ invoice.shippingRecipient }}</strong>
              </div>
              <div v-if="invoice.shippingRecipientPhone" class="info-row">
                <span>Số điện thoại</span><strong>{{ invoice.shippingRecipientPhone }}</strong>
              </div>
              <div v-if="invoice.shippingAddress" class="info-row address-row">
                <span>Địa chỉ giao hàng</span><strong>{{ invoice.shippingAddress }}</strong>
              </div>
              <div v-if="invoice.shippingNote" class="info-row">
                <span>Ghi chú</span><strong>{{ invoice.shippingNote }}</strong>
              </div>
            </section>
          </div>

          <section class="panel card shadow-sm products-panel">
            <h2><i class="bi bi-box-seam"></i> Danh sách sản phẩm</h2>
            <div class="product-table-wrap">
              <table class="product-table table table-hover align-middle mb-0">
                <thead>
                  <tr>
                    <th>Sản phẩm</th><th>Phân loại</th><th>Đơn giá</th>
                    <th>Số lượng</th><th>Thành tiền</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(item, index) in invoice.items" :key="index">
                    <td>
                      <div class="product-name">
                        <span class="product-thumb"><i class="bi bi-bag"></i></span>
                        <strong>{{ item.name }}</strong>
                      </div>
                    </td>
                    <td>{{ item.variant }}</td>
                    <td>{{ money(item.price) }}</td>
                    <td>{{ item.quantity }}</td>
                    <td class="product-total">{{ money(item.lineTotal) }}</td>
                  </tr>
                  <tr v-if="!invoice.items?.length">
                    <td colspan="5" class="empty-products">Không có sản phẩm.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>
        </div>

        <aside class="side-column">
          <section class="panel card shadow-sm payment-panel">
            <h2><i class="bi bi-calendar2-check"></i> Tổng kết thanh toán</h2>
            <div class="amount-row"><span>Tổng tiền hàng</span><strong>{{ money(subtotal) }}</strong></div>
            <div class="amount-row"><span>Giảm giá</span><strong class="discount">− {{ money(invoice.discount) }}</strong></div>
            <div class="amount-row"><span>Phí vận chuyển</span><strong>+ {{ money(invoice.shippingFee) }}</strong></div>
            <div class="grand-total"><span>TỔNG TIỀN</span><strong>{{ money(invoice.total) }}</strong></div>
          </section>

          <section class="panel card shadow-sm payment-history">
            <h2><i class="bi bi-credit-card"></i> Thông tin thanh toán</h2>
            <div class="payment-method">
              <div>
                <span>Phương thức thanh toán</span>
                <strong>{{ invoice.payment }}</strong>
              </div>
            </div>
            <div class="payment-amount-row"><span>Thành tiền</span><strong>{{ money(invoice.total) }}</strong></div>
            <div class="payment-status-row">
              <span>Trạng thái thanh toán</span>
              <strong :class="invoice.paymentStatusCode === 1 ? 'payment-status paid' : 'payment-status unpaid'">
                <i :class="invoice.paymentStatusCode === 1 ? 'bi bi-check-circle-fill' : 'bi bi-clock-fill'"></i>
                {{ invoice.paymentStatus }}
              </strong>
            </div>
            <div v-if="invoice.paymentDate" class="payment-status-row">
              <span>Ngày thanh toán</span><strong>{{ invoice.paymentDate }}</strong>
            </div>
            <button class="print-button btn btn-primary" type="button" @click="printInvoice">
              <i class="bi bi-printer"></i> In hóa đơn
            </button>
          </section>

          <section class="panel card shadow-sm invoice-meta">
            <h2><i class="bi bi-info-circle"></i> Thông tin hóa đơn</h2>
            <div class="info-row"><span>Mã hóa đơn</span><strong>{{ invoice.code }}</strong></div>
            <div class="info-row"><span>Nhân viên</span><strong>{{ invoice.employee }}</strong></div>
            <div class="info-row"><span>Mã nhân viên</span><strong>{{ invoice.employeeCode }}</strong></div>
            <div class="info-row"><span>Ngày tạo</span><strong>{{ invoice.date }}</strong></div>
            <div class="info-row"><span>Ngày cập nhật</span><strong>{{ invoice.updatedDate }}</strong></div>
            <div class="info-row">
              <span>Trạng thái</span>
              <strong><span :class="['status-pill', invoice.statusClass]">{{ invoice.status }}</span></strong>
            </div>
          </section>
        </aside>
      </div>

      <section v-if="invoice" class="print-invoice">
        <div class="print-header">
          <div>
            <div class="print-brand">SMASHSTEP</div>
            <div class="print-subtitle">SPORTS SHOES</div>
          </div>
          <div class="print-title">HÓA ĐƠN BÁN HÀNG</div>
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
              <td>{{ money(item.price) }}</td>
              <td>{{ item.quantity }}</td>
              <td>{{ money(item.lineTotal) }}</td>
            </tr>
          </tbody>
        </table>

        <div class="print-summary">
          <div><span>Tổng tiền hàng</span><strong>{{ money(subtotal) }}</strong></div>
          <div><span>Giảm giá</span><strong>{{ money(invoice.discount) }}</strong></div>
          <div><span>Phí vận chuyển</span><strong>{{ money(invoice.shippingFee) }}</strong></div>
          <div class="print-grand-total"><span>TỔNG THANH TOÁN</span><strong>{{ money(invoice.total) }}</strong></div>
        </div>

        <div class="print-payment">
          <div><span>Phương thức thanh toán:</span> {{ invoice.payment }}</div>
          <div><span>Trạng thái thanh toán:</span> {{ invoice.paymentStatus }}</div>
          <div v-if="invoice.paymentDate"><span>Ngày thanh toán:</span> {{ invoice.paymentDate }}</div>
        </div>

        <div v-if="hasShippingInfo" class="print-shipping">
          <h3>THÔNG TIN GIAO HÀNG</h3>
          <div v-if="invoice.shippingCarrier"><span>Đơn vị vận chuyển:</span> {{ invoice.shippingCarrier }}</div>
          <div><span>Phí vận chuyển:</span> {{ money(invoice.shippingFee) }}</div>
          <div v-if="invoice.shippingRecipient"><span>Người nhận:</span> {{ invoice.shippingRecipient }}</div>
          <div v-if="invoice.shippingRecipientPhone"><span>Số điện thoại:</span> {{ invoice.shippingRecipientPhone }}</div>
          <div v-if="invoice.shippingAddress"><span>Địa chỉ giao hàng:</span> {{ invoice.shippingAddress }}</div>
          <div v-if="invoice.shippingNote"><span>Ghi chú:</span> {{ invoice.shippingNote }}</div>
        </div>

        <div class="print-footer">Cảm ơn quý khách đã mua hàng tại SMASHSTEP!</div>
      </section>

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
      <div v-if="toast.visible" class="invoice-toast toast show" role="alert" aria-live="assertive">
        <div class="toast-icon" :class="toast.type === 'success' ? 'success' : 'error'">
          <i :class="toast.type === 'success' ? 'bi bi-check-lg' : 'bi bi-x-lg'"></i>
        </div>
        <div class="toast-body">
          <strong>{{ toast.title }}</strong>
          <span>{{ toast.message }}</span>
        </div>
        <button type="button" class="btn-close ms-auto" aria-label="Đóng" @click="toast.visible = false"></button>
      </div>

      <!-- Modal xác nhận thao tác có ghi dữ liệu -->
      <div
        v-if="showConfirmModal"
        class="modal fade show d-block status-modal-backdrop confirm-layer"
        tabindex="-1"
        role="dialog"
        aria-modal="true"
        @click.self="closeConfirmModal"
      >
        <div class="modal-dialog modal-dialog-centered modal-sm">
          <div class="modal-content status-modal confirm-modal">
            <div class="confirm-icon"><i class="bi bi-question-lg"></i></div>
            <div class="modal-body text-center pt-0">
              <h5 class="confirm-title">Xác nhận cập nhật</h5>
              <p class="confirm-message">
                Bạn có chắc chắn muốn chuyển đơn <strong>{{ invoice.code }}</strong>
                từ <strong>{{ invoice.status }}</strong> sang <strong>{{ nextStatus?.label }}</strong> không?
              </p>
            </div>
            <div class="modal-footer justify-content-center">
              <button type="button" class="btn btn-light modal-cancel-button" :disabled="confirmSubmitting" @click="closeConfirmModal">Hủy</button>
              <button type="button" class="btn btn-primary modal-save-button" :disabled="confirmSubmitting" @click="saveStatus">
                <span v-if="confirmSubmitting" class="spinner-border spinner-border-sm me-1"></span>
                <span v-else>Đồng ý</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Modal xác nhận hủy đơn hàng -->
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
      </div>

      <!-- Modal cập nhật trạng thái -->
      <div
        v-if="showStatusModal"
        class="modal fade show d-block status-modal-backdrop"
        tabindex="-1"
        role="dialog"
        aria-modal="true"
        @click.self="closeStatusModal"
      >
        <div class="modal-dialog modal-dialog-centered modal-md">
          <div class="modal-content status-modal">
            <div class="modal-header">
              <h5 class="modal-title">Cập nhật trạng thái đơn hàng</h5>
              <button
                type="button"
                class="btn-close"
                aria-label="Đóng"
                :disabled="updatingStatus"
                @click="closeStatusModal"
              ></button>
            </div>

            <div class="modal-body">
              <div class="status-info-grid">
                <div>
                  <span>Mã đơn hàng</span>
                  <strong>{{ invoice.code }}</strong>
                </div>
                <div>
                  <span>Ngày tạo</span>
                  <strong>{{ invoice.date }}</strong>
                </div>
                <div>
                  <span>Trạng thái hiện tại</span>
                  <strong>{{ invoice.status }}</strong>
                </div>
                <div>
                  <span>Trạng thái mới</span>
                  <strong class="next-status-text">{{ nextStatus?.label }}</strong>
                </div>
              </div>

              <label class="form-label mt-3 mb-2">Chọn trạng thái mới</label>
              <select
                v-model="selectedStatus"
                class="form-select"
                :disabled="updatingStatus"
              >
                <option value="">Chọn trạng thái mới</option>
                <option
                  v-if="nextStatus"
                  :value="nextStatus.value"
                >
                  {{ nextStatus.label }}
                </option>
              </select>
              <div class="form-text">
                Chỉ được chuyển sang trạng thái kế tiếp, không được cập nhật ngược hoặc bỏ qua trạng thái.
              </div>
            </div>

            <div class="modal-footer">
              <button
                type="button"
                class="btn btn-light modal-cancel-button"
                :disabled="updatingStatus"
                @click="closeStatusModal"
              >
                Hủy
              </button>
              <button
                type="button"
                class="btn btn-primary modal-save-button"
                :disabled="!selectedStatus || updatingStatus"
                @click="openConfirmModal"
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
                <small class="text-muted">Đơn hàng {{ invoice.code }}</small>
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
                >
                  <div class="history-dot">
                    <i class="bi bi-check2"></i>
                  </div>
                  <div class="history-card">
                    <div class="history-card-top">
                      <strong>{{ historyActor(item) }}</strong>
                      <span>{{ formatHistoryDate(item.ngayTao) }}</span>
                    </div>
                    <div class="history-meta">
                      <span><i class="bi bi-person-badge"></i> Vai trò: {{ item.vaiTro || 'Chưa có dữ liệu' }}</span>
                      <span><i class="bi bi-arrow-right-circle"></i> Trạng thái: {{ item.trangThai || '—' }}</span>
                    </div>
                    <p>{{ historyAction(item) }}</p>
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
.detail-page{max-width:1700px;margin:0 auto;padding:24px 24px 36px;color:#263943}
.page-title-row{margin-bottom:20px}.breadcrumb{font-size:11px;color:#8c9ba5;margin-bottom:5px}.breadcrumb b{padding:0 9px;color:#c4d0d7}.breadcrumb a{color:#536c79;text-decoration:none}.breadcrumb strong{color:#1689cf;font-weight:600}.page-title-row h1{margin:0;font-size:23px;color:#203744;font-weight:750}
.back-link{display:flex;align-items:center;gap:10px;border:0;background:transparent;color:#435b67;padding:0 0 16px 3px;font-size:15px;font-weight:650;cursor:pointer}.back-link i{font-size:15px;color:#1689cf}.back-link b{color:#1689cf}
.detail-layout{display:grid;grid-template-columns:minmax(0,1.85fr) minmax(285px,.9fr);gap:18px;align-items:start}.main-column,.side-column{display:grid;gap:16px;min-width:0}.panel{background:#fff;border:1px solid #e5edf1!important;border-radius:14px;box-shadow:0 3px 12px rgba(35,73,91,.045)!important;padding:19px}
.panel h2{display:flex;align-items:center;gap:9px;margin:0 0 20px;color:#263f4b;font-size:13px;font-weight:700}.panel h2 i{color:#1689cf;font-size:14px}
.timeline-panel{padding-bottom:16px}.timeline{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));position:relative;padding:7px 0 0;gap:0}.timeline:before{content:"";position:absolute;left:8.2%;right:8.2%;height:2px;background:#dce9ef;top:22px}.timeline-step{position:relative;display:flex;align-items:center;flex-direction:column;text-align:center;min-width:0}.timeline-node{width:34px;height:34px;border-radius:50%;display:grid;place-items:center;background:#eaf1f5;color:#91a4ae;border:2px solid #fff;box-shadow:0 0 0 1px #dbe7ed;z-index:1;font-size:14px}.timeline-step.reached .timeline-node{background:#1689cf;color:white;box-shadow:0 0 0 1px #1689cf}.timeline-step.current .timeline-node{box-shadow:0 0 0 4px #d9f0fb,0 0 0 5px #1689cf}.step-label{font-size:11px;font-weight:650;color:#91a0a8;margin-top:11px;line-height:1.4}.timeline-step.reached .step-label{color:#147fb9}.timeline-step small{font-size:9px;color:#98a7ae;margin-top:5px}
.timeline-actions{display:flex;flex-direction:column;align-items:flex-end;gap:9px;margin-top:18px}.soft-button,.status-update-button{height:36px;border-radius:9px;padding:0 15px;font-size:11px;font-weight:700}.soft-button{border:1px solid #d6e7ef;background:#f7fbfd;color:#477182}.soft-button:hover{background:#e9f6fc;color:#0878bd}.status-update-button{min-width:180px;background:#1689cf;border-color:#1689cf}.status-update-button:hover{background:#0d72b0;border-color:#0d72b0}
.info-grid{display:grid;grid-template-columns:1fr 1fr;gap:16px}.info-panel{padding:18px 19px 10px}.info-panel h2{margin-bottom:13px}.info-row{display:flex;justify-content:space-between;align-items:flex-start;gap:15px;padding:11px 0;border-bottom:1px solid #edf2f5;font-size:11px}.info-row:last-child{border-bottom:0}.info-row>span{color:#82949e;flex:0 0 auto}.info-row>strong{color:#334d59;font-weight:650;text-align:right;line-height:1.55;min-width:0}.address-row strong{max-width:65%}
.products-panel{padding:19px 0 0}.products-panel h2{padding:0 19px}.product-table-wrap{overflow-x:auto}.product-table{width:100%;border-collapse:collapse;min-width:650px}.product-table thead{background:#edf5fa}.product-table th{height:39px;padding:0 13px;text-align:left;font-size:10px;color:#607b8a;font-weight:700;white-space:nowrap}.product-table td{padding:12px 13px;border-bottom:1px solid #edf2f5;color:#5a707b;font-size:11px;white-space:nowrap}.product-name{display:flex;align-items:center;gap:9px}.product-name strong{color:#344e5a;font-size:11px}.product-thumb{width:36px;height:36px;border-radius:7px;background:#eaf5fb;color:#1689cf;display:grid;place-items:center;font-size:16px}.product-total{color:#1681c3!important;font-weight:700}.empty-products{text-align:center!important;color:#91a0a8!important}
.payment-panel h2,.payment-history h2,.invoice-meta h2{margin-bottom:18px}.amount-row{display:flex;justify-content:space-between;gap:12px;margin:0 0 13px;font-size:11px}.amount-row span{color:#7e909a}.amount-row strong{color:#334c58;font-weight:650;white-space:nowrap}.amount-row .discount{color:#16966f}.grand-total{border-top:1px solid #e5edf1;margin-top:17px;padding-top:15px;display:flex;justify-content:space-between;gap:10px;align-items:center}.grand-total span{font-size:12px;color:#55707d;font-weight:650}.grand-total strong{font-size:18px;color:#1689cf}
.payment-history{min-height:245px;display:flex;flex-direction:column}.payment-method{display:flex;justify-content:space-between;gap:12px;align-items:flex-start}.payment-method strong{display:block;color:#3b5662;font-size:11px;font-weight:650;line-height:1.5}.payment-method small{display:block;color:#91a0a8;font-size:10px;margin-top:7px}.paid-tag{color:#168b68;font-size:10px;font-weight:650;white-space:nowrap}.paid-amount{text-align:right;color:#344e5a;font-size:13px;margin-top:10px}.print-button{height:42px;width:100%;margin-top:auto;border:1px solid #1689cf;border-radius:10px;background:#1689cf;color:#fff;font-size:12px;font-weight:700;cursor:pointer;--bs-btn-bg:#1689cf;--bs-btn-border-color:#1689cf;--bs-btn-hover-bg:#0d72b0;--bs-btn-hover-border-color:#0d72b0}.print-button:hover{background:#d9effa}.invoice-meta{padding-bottom:10px}.status-pill{display:inline-block;border-radius:13px;padding:5px 9px;background:#e7f3fc;color:#1679b5;font-size:10px}.status-pill.done,.status-pill.delivered{background:#e8f7ef;color:#168455}.status-pill.waiting{background:#fff4dd;color:#a86a08}.status-pill.cancel{background:#ffebed;color:#c64f5b}.status-pill.shipping{background:#e6f4ff;color:#1678b8}
.cancel-state{display:flex;align-items:center;gap:13px;padding:8px 0 18px}.cancel-icon{width:40px;height:40px;border-radius:50%;background:#ffebed;color:#c64f5b;display:grid;place-items:center;font-size:19px}.cancel-state strong{color:#c64f5b;font-size:13px}.cancel-state p{margin:5px 0 0;color:#84949d;font-size:11px}
.loading-panel{display:flex;align-items:center;min-height:110px;color:#667c87;font-size:12px}

.invoice-toast{position:fixed;top:88px;right:24px;z-index:1200;min-width:340px;max-width:430px;display:flex;align-items:center;gap:11px;padding:13px 14px;border:1px solid #e3eaee;border-radius:12px;background:#fff;box-shadow:0 12px 32px rgba(30,55,68,.18)}.toast-icon{width:34px;height:34px;flex:0 0 34px;border-radius:50%;display:grid;place-items:center;font-size:15px}.toast-icon.success{background:#e7f7ee;color:#16935f}.toast-icon.error{background:#ffebed;color:#d14958}.invoice-toast .toast-body{display:flex;flex-direction:column;gap:3px;min-width:0}.invoice-toast .toast-body strong{font-size:12px;color:#263f4b}.invoice-toast .toast-body span{font-size:11px;color:#687f8a;line-height:1.4}.confirm-modal{padding-top:18px}.confirm-icon{width:56px;height:56px;margin:2px auto 13px;border:3px solid #1689cf;border-radius:50%;display:grid;place-items:center;color:#1689cf;font-size:25px}.confirm-title{font-size:18px;font-weight:750;color:#263f4b;margin-bottom:8px}.confirm-message{font-size:12px;color:#667c87;line-height:1.55;margin:0}.confirm-message strong{color:#334d59}.payment-method span,.payment-amount-row span{display:block;color:#8497a0;font-size:10px;margin-bottom:5px}.payment-amount-row{display:flex;justify-content:space-between;gap:12px;margin-top:18px;padding-top:14px;border-top:1px solid #edf2f5;font-size:11px}.payment-amount-row strong{color:#344e5a;font-size:13px}.payment-note{margin-top:12px;color:#93a1a8;font-size:9.5px;line-height:1.45}.status-modal-backdrop{background:rgba(26,39,48,.52);z-index:1060}.status-modal-backdrop.confirm-layer{z-index:1075}.history-modal-backdrop{background:rgba(26,39,48,.52);z-index:1060}.status-modal,.history-modal{border:0;border-radius:14px;box-shadow:0 16px 45px rgba(25,46,58,.22)}.status-modal .modal-header,.history-modal .modal-header{border-bottom:1px solid #edf2f5;padding:18px 20px}.status-modal .modal-title,.history-modal .modal-title{color:#263f4b;font-size:16px;font-weight:750}.status-modal .modal-body,.history-modal .modal-body{padding:20px}.status-modal .modal-footer,.history-modal .modal-footer{border-top:1px solid #edf2f5;padding:14px 20px}
.status-info-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}.status-info-grid>div{padding:11px 12px;border:1px solid #e6eef2;border-radius:9px;background:#f9fbfc}.status-info-grid span{display:block;color:#8497a0;font-size:10px;margin-bottom:5px}.status-info-grid strong{display:block;color:#334d59;font-size:12px}.next-status-text{color:#1689cf!important}.status-modal .form-select{height:40px;border-radius:9px;font-size:12px;box-shadow:none;border-color:#d8e5eb}.status-modal .form-select:focus{border-color:#1689cf;box-shadow:0 0 0 .2rem rgba(22,137,207,.12)}.status-modal .form-text{font-size:10px;color:#8a9aa2;margin-top:7px}.modal-cancel-button{min-width:82px;border:1px solid #dbe5e9;color:#536b76;background:#f7fafb}.modal-save-button{min-width:82px;background:#1689cf;border-color:#1689cf}.modal-save-button:hover{background:#0d72b0;border-color:#0d72b0}
.history-modal-dialog{max-width:760px}.history-modal{max-height:85vh}.history-body{overflow-y:auto;max-height:65vh}.history-loading,.history-empty{min-height:180px;display:flex;align-items:center;justify-content:center;color:#8798a1;font-size:12px}.history-empty{flex-direction:column;gap:8px}.history-empty i{font-size:30px;color:#c3d0d6}.history-empty p{margin:0}.history-timeline{position:relative;padding:5px 4px 5px 42px}.history-timeline:before{content:"";position:absolute;left:13px;top:12px;bottom:12px;width:2px;background:#dce9ef}.history-item{position:relative;padding-bottom:16px}.history-item:last-child{padding-bottom:0}.history-dot{position:absolute;left:-42px;top:2px;width:28px;height:28px;border-radius:50%;display:grid;place-items:center;background:#e8f5fb;color:#1689cf;border:1px solid #cde8f5;z-index:1;font-size:12px}.history-card{border:1px solid #e5edf1;border-radius:11px;padding:13px 14px;background:#fff}.history-card-top{display:flex;align-items:center;justify-content:space-between;gap:12px}.history-card-top strong{font-size:12px;color:#334d59}.history-card-top span{font-size:10px;color:#93a1a8;white-space:nowrap}.history-meta{display:flex;flex-wrap:wrap;gap:8px 16px;margin-top:7px}.history-meta span{font-size:10px;color:#7c909a}.history-meta i{color:#1689cf;margin-right:3px}.history-card p{margin:8px 0 0;color:#526a76;font-size:11px;line-height:1.5}
@media(max-width:1050px){.detail-layout{grid-template-columns:minmax(0,1.4fr) minmax(260px,.9fr)}.info-grid{grid-template-columns:1fr}.timeline{grid-template-columns:repeat(3,minmax(0,1fr));row-gap:20px}.timeline:before{display:none}}
@media(max-width:760px){.invoice-toast{left:12px;right:12px;top:78px;min-width:0;max-width:none}.detail-page{padding:15px}.detail-layout{grid-template-columns:1fr}.info-grid{grid-template-columns:1fr}.panel{padding:16px}.timeline{grid-template-columns:repeat(3,minmax(0,1fr));row-gap:18px}.step-label{font-size:10px}.back-link{font-size:13px}.status-info-grid{grid-template-columns:1fr}.history-modal-dialog{margin:10px}.history-body{max-height:65vh}.timeline-actions{align-items:stretch}.soft-button,.status-update-button{width:100%}}
.cancel-order-button{min-width:180px;height:36px;border-radius:9px;padding:0 15px;font-size:11px;font-weight:700;background:#dc3545;border-color:#dc3545}.cancel-order-button:hover{background:#bb2d3b;border-color:#bb2d3b}.cancel-confirm-icon{border-color:#dc3545;color:#dc3545}.cancel-warning{margin:8px 0 0;color:#c64f5b;font-size:11px}.cancel-confirm-button{min-width:120px}.print-invoice{display:none}.print-header{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:2px solid #1689cf;padding-bottom:14px;margin-bottom:18px}.print-brand{font-size:22px;font-weight:800;letter-spacing:1px}.print-subtitle{font-size:9px;letter-spacing:2px;margin-top:3px}.print-title{font-size:18px;font-weight:800}.print-meta-grid{display:grid;grid-template-columns:1fr 1fr;gap:9px 28px;margin-bottom:20px}.print-meta-grid div{display:flex;justify-content:space-between;gap:12px;border-bottom:1px solid #ddd;padding-bottom:5px;font-size:10px}.print-meta-grid span{color:#666}.print-product-table{width:100%;border-collapse:collapse;font-size:10px}.print-product-table th,.print-product-table td{border:1px solid #ccc;padding:7px 6px;text-align:left}.print-product-table th{font-weight:700;background:#f5f5f5}.print-summary{width:300px;margin:18px 0 0 auto;font-size:10px}.print-summary div{display:flex;justify-content:space-between;padding:5px 0}.print-grand-total{border-top:2px solid #222;margin-top:5px;padding-top:9px!important;font-size:13px;font-weight:800}.print-payment{margin-top:20px;padding-top:10px;border-top:1px solid #ddd;font-size:10px}.print-payment div{margin:4px 0}.print-payment span{font-weight:700}.print-shipping{margin-top:20px;padding-top:10px;border-top:1px solid #ddd;font-size:10px}.print-shipping h3{font-size:12px;margin:0 0 8px;font-weight:800}.print-shipping div{margin:4px 0}.print-shipping span{font-weight:700}.print-footer{text-align:center;margin-top:28px;font-size:10px;font-style:italic}
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

@media print{.detail-page{padding:0}.back-link,.timeline-actions,.print-button{display:none}.detail-layout{grid-template-columns:1fr 1fr}.panel{box-shadow:none;break-inside:avoid}}

.payment-status-row { display:flex; justify-content:space-between; gap:16px; align-items:center; padding:12px 0; border-top:1px solid #edf2f7; }
.payment-status { display:inline-flex; align-items:center; gap:6px; }
.payment-status.paid { color:#16a34a; }
.payment-status.unpaid { color:#d97706; }
@media print{html,body{margin:0!important;padding:0!important;background:#fff!important}.detail-page{padding:0!important}.detail-page>.page-title-row,.detail-page>.back-link,.detail-page>.detail-layout,.detail-page>.invoice-toast,.detail-page>.modal{display:none!important}.print-invoice{display:block!important;padding:12mm 10mm;color:#222;background:#fff;font-family:Arial,sans-serif}.print-invoice *{box-sizing:border-box}}
</style>
