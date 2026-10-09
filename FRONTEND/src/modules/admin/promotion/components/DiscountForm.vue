<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import api from '../../../../utils/api'


const props = defineProps({
  form: {
    type: Object,
    required: true
  },

  editMode: {
    type: Boolean,
    default: false
  },

  saving: {
    type: Boolean,
    default: false
  }
})

/* emit */

const emit = defineEmits([
  'submit',
  'cancel',
  'generate-code'
])

/* form */

const isPercent = computed(() => {
  return props.form.discountType === 1
})

/* khach hang */

const customers = ref([])

const customerSearch = ref('')

const loadingCustomers = ref(false)

const customerError = ref('')

/* so kh da chon */
const selectedCustomerCount = computed(() => {
  return props.form.customerIds?.length || 0

})

/* dsach sau khi tiem kiem */
const filteredCustomers = computed(() => {

  const keyword = customerSearch.value
    .trim()
    .toLowerCase()

  if (!keyword) {
    return customers.value
  }

  return customers.value.filter(customer => {

    const code =
      String(
        customer.code ||
        customer.maKhachHang ||
        ''
      ).toLowerCase()

    const name =
      String(
        customer.name ||
        customer.tenKhachHang ||
        ''
      ).toLowerCase()

    const phone =
      String(
        customer.phone ||
        customer.soDienThoai ||
        ''
      ).toLowerCase()

    const email =
      String(
        customer.email || ''
      ).toLowerCase()

    return (
      code.includes(keyword) ||
      name.includes(keyword) ||
      phone.includes(keyword) ||
      email.includes(keyword)
    )
  })
})

/* kiem tra ng dc chon */
function isCustomerSelected(id) {
  return props.form.customerIds?.includes(id)
}

/* chon/bo chon */
function toggleCustomer(id) {

  if (!Array.isArray(props.form.customerIds)) {
    props.form.customerIds = []
  }

  const index =
    props.form.customerIds.indexOf(id)

  if (index >= 0) {

    props.form.customerIds.splice(index, 1)

  } else {

    props.form.customerIds.push(id)

  }
  if (props.form.form === 2) {
    props.form.quantity = props.form.customerIds.length
  }
}

/* chon all kh hien thi */
function toggleAllVisibleCustomers() {

  const ids = filteredCustomers.value.map(
    customer => customer.id
  )

  if (!ids.length) {
    return
  }

  const current =
    props.form.customerIds || []

  const allSelected =
    ids.every(id =>
      current.includes(id)
    )

  if (allSelected) {

    props.form.customerIds =
      current.filter(
        id => !ids.includes(id)
      )

  } else {

    props.form.customerIds = [
      ...new Set([
        ...current,
        ...ids
      ])
    ]

  }
}

/* kiem tra chon tat ca */
const allVisibleSelected = computed(() => {

  const ids =
    filteredCustomers.value.map(
      customer => customer.id
    )

  if (!ids.length) {
    return false
  }

  return ids.every(id =>
    props.form.customerIds?.includes(id)
  )
})

/* kiem tra */

const formSupported = ref(false)

const capabilityError = ref('')

onMounted(async () => {

  try {

    const response =
      await api.get(
        '/phieu-giam-gia/capabilities'
      )

    formSupported.value =
      response.data?.data?.formSupported === true

    if (!formSupported.value) {

      capabilityError.value =
        'Cấu hình hiện tại chưa hỗ trợ lưu hình thức phiếu. Không thể tạo hoặc sửa phiếu.'

    }

  } catch {

    capabilityError.value =
      'Không thể kiểm tra khả năng lưu phiếu. Vui lòng tải lại trang.'

  }

})

/* format cho ngay sinh*/
function formatCustomerDate(value) {
  if (!value) return '—'

  const date = String(value).substring(0, 10)
  const [y, m, d] = date.split('-')

  if (!y || !m || !d) {
    return date
  }

  return `${d}/${m}/${y}`
}

/* load kh */

watch(
  () => props.form.form,

  async value => {

    /* chi load form khi co 2 nguoi*/
    if (
      value !== 2 ||
      customers.value.length ||
      loadingCustomers.value
    ) {
      return
    }

    loadingCustomers.value = true

    customerError.value = ''

    try {

      const result = []

      let nextPage = 1

      let pages = 1

      do {

        const response =
          await api.get(
            '/khach-hang',
            {
              params: {
                trangThai: 1,
                page: nextPage,
                size: 100
              }
            }
          )

        const data =
          response.data?.data

        result.push(
          ...(data?.content || [])
        )

        pages =
          data?.totalPages ?? 1

        nextPage++

      } while (
        nextPage <= pages
      )

      customers.value = result

    } catch (error) {

      console.error(error)

      customerError.value =
        error.response?.data?.message ||
        'Không thể tải khách hàng nhận phiếu.'

    } finally {

      loadingCustomers.value = false

    }

  },

  {
    immediate: true
  }
)

const minOrderDisplay = computed({
  get() {
    const value = props.form.minOrderValue
    if (value == null || value === '') return ''
    return Number(value) / 1000
  },
  set(value) {
    if (value === '' || value == null) {
      props.form.minOrderValue = ''
      return
    }

    const number = Number(value)
    if (Number.isFinite(number)) {
      props.form.minOrderValue = number * 1000
    }
  }
})

const maxDiscountDisplay = computed({
  get() {
    const value = props.form.maxDiscount
    if (value == null || value === '') return ''
    return Number(value) / 1000
  },
  set(value) {
    if (value === '' || value == null) {
      props.form.maxDiscount = ''
      return
    }

    const number = Number(value)
    if (Number.isFinite(number)) {
      props.form.maxDiscount = number * 1000
    }
  }
})


</script>

<template>
  <section class="ss-card ss-form">
    <p v-if="capabilityError" class="ss-hint warn" role="alert">{{ capabilityError }}</p>

    <div class="ss-head">

      <div class="ss-head-icon">
        <i class="bi bi-ticket-perforated"></i>
      </div>

      <h2>Thông tin phiếu</h2>

    </div>

    <div class="form-grid">

      <div class="ss-field">

        <label class="ss-label">
          Mã phiếu
          <span class="req">*</span>
        </label>

        <div class="code-input-wrapper">

          <input
            class="ss-input"
            v-model="form.code"
            readonly
            placeholder="Tự động tạo"
          />

          <button
            v-if="!editMode"
            type="button"
            class="generate-code-btn"
            title="Tạo mã mới"
            @click="emit('generate-code')"
          >
            <i class="bi bi-arrow-clockwise"></i>
          </button>
          
        </div>
        <span class="ss-hint">
          <i class="bi bi-info-circle"></i>
          Mã phiếu giảm giá được hệ thống tự động tạo và không thể chỉnh sửa.
        </span>
      </div>

      <div class="ss-field">

        <label class="ss-label">
          Tên phiếu
          <span class="req">*</span>
        </label>

        <input
          class="ss-input"
          v-model="form.name"
          placeholder="Ví dụ: Giảm tháng 10 2026"
        />

      </div>

      <div class="ss-field">

        <span class="ss-label">
          Loại giảm
        </span>

        <div class="ss-radios">

          <label>
            <input
              type="radio"
              :value="1"
              v-model="form.discountType"
            />
            Phần trăm (%)
          </label>

          <label>
            <input
              type="radio"
              :value="2"
              v-model="form.discountType"
            />
            Tiền mặt (VNĐ)
          </label>

        </div>

      </div>

      <div class="ss-field">

        <label class="ss-label">
          Giá trị giảm
          ({{ isPercent ? '%' : 'VNĐ' }})

          <span class="req">*</span>
        </label>

        <input
          class="ss-input"
          type="number"
          min="0.01"
          step="0.01"
          v-model.number="form.discountValue"
        />

      </div>

      <div class="ss-field">

        <label class="ss-label muted">
          Giá trị đơn tối thiểu (VNĐ)
        </label>

        <input
          class="ss-input"
          type="number"
          min="0"
          step="1"
          v-model.number="minOrderDisplay"
        />

      </div>

      <div class="ss-field">

        <label class="ss-label muted">
          Giảm tối đa (VNĐ)
        </label>

        <input
          class="ss-input"
          type="number"
          min="0"
          step="1"
          v-model.number="maxDiscountDisplay"
        />

        <span class="ss-hint warn">
          <i class="bi bi-exclamation-triangle"></i>
          Nếu không giới hạn, để trống hoặc 0.
        </span>

      </div>

      <div class="ss-field">
  <label class="ss-label">
    Số lượng
    <span class="req">*</span>
  </label>

  <!-- hien thi so kh da chon -->
  <input
    v-if="form.form === 2"
    class="ss-input"
    type="number"
    :value="selectedCustomerCount"
    readonly
  />

  <!-- Công khai -->
  <template v-else>
    <input
      v-if="!form.unlimited"
      class="ss-input"
      type="number"
      min="1"
      v-model.number="form.quantity"
    />

    <div
      v-else
      class="ss-input"
      style="background: #f5f5f5;"
    >
      Không giới hạn
    </div>

    <label style="margin-top: 8px;">
      <input
        type="checkbox"
        v-model="form.unlimited"
      />
      Không giới hạn số lượng
    </label>
  </template>
</div>

      <div class="ss-field">

        <label class="ss-label">
          Ngày bắt đầu
          <span class="req">*</span>
        </label>

        <input
          class="ss-input"
          type="datetime-local"
          v-model="form.startDate"
        />

      </div>

      <div class="ss-field">

        <label class="ss-label">
          Ngày kết thúc
          <span class="req">*</span>
        </label>

        <input
          class="ss-input"
          type="datetime-local"
          v-model="form.endDate"
        />

      </div>

      <div class="ss-field full-width">

        <label class="ss-label">
          Mô tả phiếu giảm giá
        </label>

        <textarea
          class="ss-input ss-textarea"
          v-model="form.description"
          rows="4"
          placeholder="Nhập mô tả cho phiếu giảm giá..."
        ></textarea>

      </div>
    </div>

    <div class="ss-field">

        <span class="ss-label">
          Hình thức phiếu
        </span>

        <div class="ss-radios">

          <label>
            <input
              type="radio"
              :value="1"
              v-model="form.form"
            />
            Công khai
          </label>

          <label>
            <input
              type="radio"
              :value="2"
              v-model="form.form"
            />
            Cá nhân
          </label>

        </div>

      </div>

      <div
  v-if="form.form === 2"
  class="ss-field full-width customer-section"
>
  <div class="customer-section-head">
    <label class="ss-label">
      <i class="bi bi-people-fill"></i>
      Đối tượng áp dụng
    </label>
  </div>

  <div class="customer-list-card">

    <!-- title -->
    <div class="customer-list-head">

      <strong>
        Danh sách khách hàng nhận phiếu
      </strong>

      <span class="customer-selected-count">
        Đã chọn {{ selectedCustomerCount }}
      </span>

    </div>

    <!-- tim kiem -->
    <div class="customer-search">

      <i class="bi bi-search"></i>

      <input
        v-model="customerSearch"
        type="text"
        placeholder="Tìm kiếm theo mã, tên, SĐT, email..."
        :disabled="loadingCustomers || saving"
      />

    </div>

    <!-- loading -->
    <div
      v-if="loadingCustomers"
      class="customer-empty"
    >
      <i class="bi bi-arrow-repeat"></i>
      Đang tải danh sách khách hàng...
    </div>

    <!-- error -->
    <div
      v-else-if="customerError"
      class="customer-error"
    >
      {{ customerError }}
    </div>

    <!-- bang -->
    <div
      v-else
      class="customer-table-wrap"
    >

      <table class="customer-table">

        <thead>
          <tr>

            <th class="customer-check">
              <input
                type="checkbox"
                :checked="allVisibleSelected"
                @change="toggleAllVisibleCustomers"
              />
            </th>

            <th>Mã KH</th>

            <th>Tên khách hàng</th>

            <th>Ngày sinh</th>

            <th>Số điện thoại</th>

            <th>Email</th>

            <th>Đã mua</th>

            <th>Gần nhất</th>

          </tr>
        </thead>

        <tbody>

          <tr
            v-for="customer in filteredCustomers"
            :key="customer.id"
          >

            <td class="customer-check">
              <input
                type="checkbox"
                :checked="isCustomerSelected(customer.id)"
                :disabled="saving"
                @change="toggleCustomer(customer.id)"
              />
            </td>

            <td>
              {{ customer.code || '—' }}
            </td>

            <td class="customer-name">
              {{ customer.name || '—' }}
            </td>

            <td>
              {{ formatCustomerDate(customer.dob) }}
            </td>

            <td>
              {{ customer.phone || '—' }}
            </td>

            <td>
              {{ customer.email || '—' }}
            </td>

            <td>
              {{ customer.totalOrders ?? customer.soDon ?? 0 }}
              đơn
            </td>

            <td>
              {{
                customer.lastPurchase ||
                customer.ganNhat ||
                '—'
              }}
            </td>

          </tr>

          <tr v-if="!filteredCustomers.length">
            <td
              colspan="8"
              class="customer-empty"
            >
              Không tìm thấy khách hàng phù hợp.
            </td>
          </tr>

        </tbody>

      </table>

    </div>

  </div>

  <span class="ss-hint">
    Chọn ít nhất một khách hàng để cấp phiếu cá nhân.
  </span>

</div>

    <div class="ss-actions left">

      <button
        class="ss-btn primary"
        type="button"
        :disabled="saving || !formSupported || (form.form === 2 && (loadingCustomers || !!customerError))"
        @click="emit('submit')"
      >
        <i class="bi bi-check2"></i>

        {{ editMode
          ? 'Lưu thay đổi'
          : 'Tạo phiếu giảm giá'
        }}
      </button>

      <button
        class="ss-btn"
        type="button"
        :disabled="saving"
        @click="emit('cancel')"
      >
        Hủy
      </button>

    </div>

  </section>
</template>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 24px;
}

.ss-label.muted {
  font-weight: 500;
  color: var(--ss-muted);
}

.ss-hint i {
  margin-right: 3px;
}

@media (max-width: 760px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}

.customer-section {
  margin-top: 4px;
}

.customer-section-head {
  margin-bottom: 10px;
}

.customer-section-head .ss-label {
  display: flex;
  align-items: center;
  gap: 7px;
  font-weight: 600;
}

.customer-list-card {
  border: 1px solid #dce4eb;
  border-radius: 10px;
  background: #fff;
  overflow: hidden;
}

.customer-list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;

  padding: 14px 16px;

  border-bottom: 1px solid #e8edf2;
}

.customer-selected-count {
  padding: 4px 11px;

  border-radius: 20px;

  background: #737b80;
  color: #fff;

  font-size: 12px;
  font-weight: 600;
}

.customer-search {
  position: relative;

  margin: 12px 16px;

  width: 50%;
}

.customer-search i {
  position: absolute;

  left: 12px;
  top: 50%;

  transform: translateY(-50%);

  color: #8a98a8;
}

.customer-search input {
  width: 100%;
  height: 38px;

  padding: 0 12px 0 36px;

  border: 1px solid #d7e0e8;
  border-radius: 20px;

  outline: none;

  font-size: 13px;

  box-sizing: border-box;
}

.customer-search input:focus {
  border-color: #168dcc;
}

.customer-table-wrap {
  overflow-x: auto;

  margin: 0 16px 16px;

  border: 1px solid #e2e7ec;
  border-radius: 8px;
}

.customer-table {
  width: 100%;

  border-collapse: collapse;

  font-size: 13px;
}

.customer-table th {
  height: 42px;

  padding: 8px 10px;

  background: #f8fafc;

  border-bottom: 1px solid #dfe5ea;

  color: #596979;

  font-weight: 600;

  white-space: nowrap;

  text-align: left;
}

.customer-table td {
  height: 44px;

  padding: 8px 10px;

  border-bottom: 1px solid #edf0f3;

  color: #4f5e6d;

  white-space: nowrap;
}

.customer-table tbody tr:last-child td {
  border-bottom: none;
}

.customer-table tbody tr:hover {
  background: #f8fbfd;
}

.customer-table input[type='checkbox'] {
  width: 15px;
  height: 15px;

  cursor: pointer;
}

.customer-check {
  width: 38px;

  text-align: center !important;
}

.customer-name {
  color: #1e2f40 !important;

  font-weight: 600;
}

.customer-empty {
  padding: 30px !important;

  text-align: center !important;

  color: #8996a3 !important;
}

.customer-error {
  margin: 12px 16px;

  padding: 10px 12px;

  border-radius: 7px;

  background: #fff1f1;

  color: #c62828;

  font-size: 13px;
}

@media (max-width: 1000px) {
  .customer-search {
    width: 100%;
  }
}
</style>
