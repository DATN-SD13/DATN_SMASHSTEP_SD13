<script setup>
defineProps({
  customer: {
    type: Object,
    required: true
  }
})

defineEmits([
  'close'
])

const genderText = (value) => ({
  0: 'Khác',
  1: 'Nam',
  2: 'Nữ'
}[value] || 'Chưa cập nhật')

const formatDate = (value) =>
  value
    ? new Date(
        `${value}T00:00:00`
      ).toLocaleDateString('vi-VN')
    : 'Chưa cập nhật'
</script>

<template>

  <div
    class="modal-mask"
    @click.self="$emit('close')"
  >

    <section class="modal-card">

      <div class="modal-head">

        <div>
          <h2>
            Chi tiết khách hàng
          </h2>

          <p>
            {{ customer.code || '—' }}
            ·
            {{ customer.username || '—' }}
          </p>
        </div>

        <button
          class="ss-icon-btn"
          @click="$emit('close')"
        >
          <i class="bi bi-x-lg"></i>
        </button>

      </div>

      <div class="detail-grid">

        <div>
          <span>Họ tên</span>
          <strong>
            {{ customer.name || '—' }}
          </strong>
        </div>

        <div>
          <span>Email</span>
          <strong>
            {{ customer.email || '—' }}
          </strong>
        </div>

        <div>
          <span>Số điện thoại</span>
          <strong>
            {{
              customer.phone
              || 'Chưa cập nhật'
            }}
          </strong>
        </div>

        <div>
          <span>Giới tính</span>
          <strong>
            {{
              genderText(
                customer.gender
              )
            }}
          </strong>
        </div>

        <div>
          <span>Ngày sinh</span>
          <strong>
            {{
              formatDate(
                customer.dob
              )
            }}
          </strong>
        </div>

        <div>
          <span>Trạng thái</span>
          <strong>
            {{ customer.statusLabel || '—' }}
          </strong>
        </div>

      </div>

      <div class="address-box">

        <div class="address-title">

          <i class="bi bi-geo-alt"></i>

          Địa chỉ mặc định

        </div>

        <template
          v-if="customer.defaultAddress"
        >

          <strong>
            {{
              customer.defaultAddress
                .receiverName || '—'
            }}
            ·
            {{
              customer.defaultAddress
                .receiverPhone || '—'
            }}
          </strong>

          <p>
            {{
              customer.defaultAddress
                .fullAddress || '—'
            }}
          </p>

        </template>

        <p v-else>
          Chưa có địa chỉ mặc định.
        </p>

      </div>

      <div class="ss-actions">

        <button
          class="ss-btn"
          @click="$emit('close')"
        >
          Đóng
        </button>

      </div>

    </section>

  </div>

</template>

<style scoped>
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, .48);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 1050;
}

.modal-card {
  width: min(760px, 100%);
  max-height: 90vh;
  overflow: auto;
  background: #fff;
  border-radius: 18px;
  padding: 24px;
  box-shadow: 0 24px 70px rgba(15, 23, 42, .24);
}

.modal-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 20px;
}

.modal-head h2 {
  margin: 0;
  font-size: 22px;
}

.modal-head p {
  margin: 5px 0 0;
  color: #64748b;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.detail-grid > div {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 13px;
}

.detail-grid span {
  display: block;
  color: #64748b;
  font-size: 13px;
  margin-bottom: 5px;
}

.address-box {
  margin-top: 16px;
  padding: 16px;
  border-radius: 12px;
  background: #f8fafc;
}

.address-title {
  font-weight: 700;
  margin-bottom: 9px;
}

.address-box p {
  margin: 5px 0 0;
  color: #475569;
}

@media(max-width: 640px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
