
<script setup>
import {
  ref,
  watch,
  nextTick,
  onMounted,
  onBeforeUnmount
} from 'vue'

import $ from 'jquery'
import select2 from 'select2'
import 'select2/dist/css/select2.min.css'

// Đăng ký Select2 với jQuery trong môi trường Vite
if (typeof $.fn.select2 !== 'function') {
  select2(window, $)
}

const props = defineProps({
  modelValue: {
    type: [String, Number],
    default: 'all'
  },
  options: {
    type: Array,
    default: () => []
  },
  placeholder: {
    type: String,
    default: 'Chọn giá trị'
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const selectElement = ref(null)

// ==========================================
// KHỞI TẠO SELECT2
// ==========================================
function initSelect2() {
  if (!selectElement.value) return

  const $select = $(selectElement.value)

  if (typeof $.fn.select2 !== 'function') {
    console.error('Select2 chưa được đăng ký với jQuery')
    return
  }

  if ($select.hasClass('select2-hidden-accessible')) {
    $select.select2('destroy')
  }

  $select.select2({
    width: '100%',
    minimumResultsForSearch: 0,
    placeholder: props.placeholder
  })

  $select
    .val(String(props.modelValue ?? 'all'))
    .trigger('change.select2')

  // Đồng bộ giá trị Select2 với Vue
  $select.off('change.vueSelect2')

  $select.on('change.vueSelect2', () => {
    const value = $select.val()

    const selectedOption = props.options.find(
      option => String(option.value) === String(value)
    )

    emit(
      'update:modelValue',
      selectedOption ? selectedOption.value : value
    )
  })
}

// ==========================================
// HỦY SELECT2
// ==========================================
function destroySelect2() {
  if (!selectElement.value) return

  const $select = $(selectElement.value)

  $select.off('change.vueSelect2')

  if ($select.hasClass('select2-hidden-accessible')) {
    $select.select2('destroy')
  }
}

// ==========================================
// CẬP NHẬT DANH SÁCH LỰA CHỌN
// ==========================================
watch(
  () => props.options,
  async () => {
    destroySelect2()
    await nextTick()
    initSelect2()
  },
  {
    deep: true,
    flush: 'post'
  }
)

// ==========================================
// ĐỒNG BỘ v-model
// ==========================================
watch(
  () => props.modelValue,
  value => {
    if (!selectElement.value) return

    $(selectElement.value)
      .val(String(value ?? 'all'))
      .trigger('change.select2')
  }
)

// ==========================================
// ĐỒNG BỘ TRẠNG THÁI DISABLED
// ==========================================
watch(
  () => props.disabled,
  value => {
    if (!selectElement.value) return

    $(selectElement.value)
      .prop('disabled', value)
      .trigger('change.select2')
  }
)

// ==========================================
// VÒNG ĐỜI COMPONENT
// ==========================================
onMounted(async () => {
  await nextTick()
  initSelect2()
})

onBeforeUnmount(() => {
  destroySelect2()
})
</script>

<template>
  <select
    ref="selectElement"
    class="ss-select"
    :disabled="disabled"
  >
    <option
      v-for="option in options"
      :key="option.value"
      :value="option.value"
    >
      {{ option.label }}
    </option>
  </select>
</template>

<style>
/* Giữ chiều cao tương đồng với các bộ lọc cũ */
.select2-container {
  width: 100% !important;
}

.select2-container .select2-selection--single {
  height: 40px !important;
  border: 1px solid #dbe4f0 !important;
  border-radius: 8px !important;
  background-color: #f8fafc;
}

.select2-container .select2-selection__rendered {
  line-height: 38px !important;
  padding-left: 12px !important;
}

.select2-container .select2-selection__arrow {
  height: 38px !important;
}

.select2-container--default.select2-container--focus
.select2-selection--single {
  border-color: #3b82f6 !important;
}

.select2-dropdown {
  border: 1px solid #dbe4f0;
  border-radius: 8px;
}

.select2-search__field {
  outline: none;
}
</style>
