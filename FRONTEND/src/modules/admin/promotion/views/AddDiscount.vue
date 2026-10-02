<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const form = ref({ code: 'VCH8H5TG9', name: '', type: 'Công khai', discountType: 'Phần trăm (%)', value: 0, min: 0, max: 0, quantity: '', unlimited: false, start: '2026-08-16', end: '' })
const isPercent = computed(() => form.value.discountType === 'Phần trăm (%)')

function save() { alert('Đã tạo phiếu giảm giá'); router.push('/giam-gia') }
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <div><button class="ss-back" aria-label="Quay lại" @click="router.back()"><i class="bi bi-arrow-left"></i></button></div>

      <section class="ss-card ss-form">
        <div class="ss-head">
          <div class="ss-head-icon"><i class="bi bi-ticket-perforated"></i></div>
          <h2>Thông tin phiếu</h2>
        </div>

        <div class="form-grid">
          <div class="ss-field">
            <label class="ss-label">Mã phiếu <span class="req">*</span></label>
            <input class="ss-input" v-model="form.code" />
          </div>
          <div class="ss-field">
            <label class="ss-label">Tên phiếu <span class="req">*</span></label>
            <input class="ss-input" v-model="form.name" placeholder="Ví dụ: Giảm giá hè 2024" />
          </div>

          <div class="ss-field">
            <span class="ss-label">Hình thức phiếu</span>
            <div class="ss-radios">
              <label><input type="radio" value="Công khai" v-model="form.type" /> Công khai</label>
              <label><input type="radio" value="Cá nhân" v-model="form.type" /> Cá nhân</label>
            </div>
          </div>
          <div class="ss-field">
            <span class="ss-label">Loại giảm</span>
            <div class="ss-radios">
              <label><input type="radio" value="Phần trăm (%)" v-model="form.discountType" /> Phần trăm (%)</label>
              <label><input type="radio" value="Tiền mặt (VNĐ)" v-model="form.discountType" /> Tiền mặt (VNĐ)</label>
            </div>
          </div>

          <div class="ss-field">
            <label class="ss-label">Giá trị giảm ({{ isPercent ? '%' : 'VNĐ' }}) <span class="req">*</span></label>
            <input class="ss-input" type="number" min="0" v-model="form.value" />
          </div>
          <div class="ss-field">
            <label class="ss-label muted">Giá trị đơn tối thiểu (VNĐ)</label>
            <input class="ss-input" type="number" min="0" v-model="form.min" />
          </div>

          <div class="ss-field">
            <label class="ss-label muted">Giảm tối đa (VNĐ)</label>
            <input class="ss-input" type="number" min="0" v-model="form.max" />
            <span class="ss-hint warn"><i class="bi bi-exclamation-triangle"></i> Lưu ý: Voucher này sẽ không giới hạn số tiền giảm tối đa.</span>
          </div>
          <div class="ss-field">
            <label class="ss-label">Số lượng còn lại <span class="req">*</span></label>
            <input class="ss-input" v-model="form.quantity" :disabled="form.unlimited" placeholder="Nhập số lượng còn lại" />
            <label class="ss-check"><input type="checkbox" v-model="form.unlimited" /> Vô hạn số lượng</label>
          </div>

          <div class="ss-field">
            <label class="ss-label">Ngày bắt đầu <span class="req">*</span></label>
            <input class="ss-input" type="date" v-model="form.start" />
          </div>
          <div class="ss-field">
            <label class="ss-label">Ngày kết thúc <span class="req">*</span></label>
            <input class="ss-input" type="date" v-model="form.end" />
          </div>
        </div>

        <div class="ss-actions left">
          <button class="ss-btn primary" @click="save"><i class="bi bi-check2"></i> Tạo phiếu giảm giá</button>
          <button class="ss-btn" @click="router.back()">Hủy</button>
        </div>
      </section>
    </main>
  </AdminLayout>
</template>

<style scoped>
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px 24px; }
.ss-label.muted { font-weight: 500; color: var(--ss-muted); }
.ss-hint i { margin-right: 3px; }
@media (max-width: 760px) { .form-grid { grid-template-columns: 1fr; } }
</style>
