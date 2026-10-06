<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  formSupported: { type: Boolean, default: true },
  modelValue: {
    type: Object,
    default: () => ({
      keyword: '',
      type: 'all',
      start: '',
      end: '',
      discount: 'all',
      status: 'all'
    })
  }
})

const emit = defineEmits([
  'update:modelValue',
  'search',
  'reset',
  'create'
])

const local = ref({ ...props.modelValue })

watch(
  () => props.modelValue,
  value => {
    local.value = { ...value }
  },
  { deep: true }
)

function search() {
  emit('update:modelValue', { ...local.value })
  emit('search', { ...local.value })
}

function reset() {
  local.value = {
    keyword: '',
    type: 'all',
    start: '',
    end: '',
    discount: 'all',
    status: 'all'
  }

  emit('update:modelValue', { ...local.value })
  emit('reset')
}
</script>

<template>
  <section class="ss-card">

    <div class="ss-head">
      <div class="ss-head-icon">
        <i class="bi bi-funnel"></i>
      </div>

      <div>
        <h2>Bộ lọc</h2>
        <p>Tra cứu nhanh dữ liệu.</p>
      </div>
    </div>

    <div class="filter-row">

      <!-- MÃ PHIẾU -->
      <div class="ss-field">
        <span class="ss-label">Mã hoặc tên phiếu</span>

        <div class="ss-search">
          <i class="bi bi-search"></i>

          <input
            v-model="local.keyword"
            class="ss-input"
            placeholder="Nhập mã hoặc tên phiếu..."
            @keyup.enter="search"
          />
        </div>
      </div>

      <!-- HÌNH THỨC -->
      <div class="ss-field">
        <span class="ss-label">Hình thức</span>

        <select
          v-model="local.type"
          :disabled="!formSupported"
          class="ss-select"
        >
          <option value="all">
            Tất cả hình thức
          </option>

          <option :value="1">
            Công khai
          </option>

          <option :value="2">
            Cá nhân
          </option>
        </select>
        <small v-if="!formSupported" class="ss-hint warn">Hình thức phiếu chưa khả dụng.</small>
      </div>

      <!-- TỪ NGÀY -->
      <div class="ss-field">
        <span class="ss-label">Từ ngày</span>

        <input
          v-model="local.start"
          class="ss-input"
          type="date"
        />
      </div>

      <!-- ĐẾN NGÀY -->
      <div class="ss-field">
        <span class="ss-label">Đến ngày</span>

        <input
          v-model="local.end"
          class="ss-input"
          type="date"
        />
      </div>

      <!-- LOẠI GIẢM -->
      <div class="ss-field">
        <span class="ss-label">Loại giảm</span>

        <select
          v-model="local.discount"
          class="ss-select"
        >
          <option value="all">
            Tất cả loại giảm
          </option>

          <option :value="1">
            Phần trăm
          </option>

          <option :value="2">
            Tiền mặt
          </option>
        </select>
      </div>

      <!-- TRẠNG THÁI -->
      <div class="ss-field">
        <span class="ss-label">Trạng thái</span>

        <select
          v-model="local.status"
          class="ss-select"
        >
          <option value="all">
            Tất cả trạng thái
          </option>

          <option :value="1">
            Hoạt động
          </option>

          <option :value="0">
            Ngừng hoạt động
          </option>
        </select>
      </div>

    </div>

    <div class="ss-actions">

      <button
        class="ss-btn"
        type="button"
        @click="reset"
      >
        <i class="bi bi-arrow-clockwise"></i>
        Đặt lại bộ lọc
      </button>

      <button
        class="ss-btn primary"
        type="button"
        @click="search"
      >
        <i class="bi bi-search"></i>
        Tìm kiếm
      </button>

      <button
        class="ss-btn primary"
        type="button"
        @click="emit('create')"
      >
        <i class="bi bi-plus-lg"></i>
        Tạo phiếu mới
      </button>

    </div>

  </section>
</template>

<style scoped>
.filter-row {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
}

@media (max-width: 1100px) {
  .filter-row {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 700px) {
  .filter-row {
    grid-template-columns: 1fr;
  }
}
</style>
