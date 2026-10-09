<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import $ from 'jquery'
import 'select2/dist/css/select2.css'

const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  options: { type: Array, default: () => [] },
  placeholder: { type: String, default: 'Chọn...' },
  disabled: { type: Boolean, default: false },
  allowClear: { type: Boolean, default: true },
  width: { type: String, default: '100%' },
  searchPlaceholder: { type: String, default: 'Nhập để tìm kiếm...' }
})
const emit = defineEmits(['update:modelValue', 'change'])
const selectEl = ref(null)
let $select = null
const normalizedOptions = computed(() => props.options.map(option => ({
  id: String(option.value ?? option.id ?? ''),
  text: String(option.label ?? option.text ?? '')
})))

function syncOptions() {
  if (!$select) return
  const current = props.modelValue == null ? '' : String(props.modelValue)
  $select.empty()
  $select.append(new Option('', '', false, current === ''))
  normalizedOptions.value.forEach(option => {
    $select.append(new Option(option.text, option.id, false, option.id === current))
  })
  $select.val(current).trigger('change.select2')
}

async function init() {
  if (!selectEl.value) return

  // Dùng chung một instance jQuery cho Select2
  window.$ = window.jQuery = $

  const select2Module = await import('select2')

  // Một số cấu hình module cần gọi factory với jQuery
  if (typeof $.fn.select2 !== 'function') {
    const factory =
      typeof select2Module.default === 'function'
        ? select2Module.default
        : null

    if (factory) {
      try {
        factory($)
      } catch (error) {
        console.error('Không thể khởi tạo Select2:', error)
      }
    }
  }

  if (typeof $.fn.select2 !== 'function') {
    console.error(
      'Select2 chưa được gắn vào jQuery. Kiểm tra phiên bản và cách đóng gói thư viện.'
    )
    return
  }

  $select = $(selectEl.value)

  $select.select2({
    width: props.width,
    placeholder: props.placeholder,
    allowClear: props.allowClear,
    minimumResultsForSearch: 0,
    language: {
      noResults: () => 'Không tìm thấy kết quả',
      searching: () => 'Đang tìm...'
    }
  })

  $select.on('change.select2Field', () => {
    const raw = $select.val() ?? ''
    const option = props.options.find(
      item => String(item.value ?? item.id ?? '') === String(raw)
    )
    const value = option ? (option.value ?? option.id) : ''

    emit('update:modelValue', value)
    emit('change', value)
  })

  syncOptions()
  $select.prop('disabled', props.disabled).trigger('change.select2')
}
watch(() => props.options, () => nextTick(syncOptions), { deep: true })
watch(() => props.modelValue, value => {
  if ($select && String($select.val() ?? '') !== String(value ?? '')) $select.val(value == null ? '' : String(value)).trigger('change.select2')
})
watch(() => props.disabled, value => { if ($select) $select.prop('disabled', value) })
onMounted(() => { void init() })
onBeforeUnmount(() => {
  if ($select) { $select.off('.select2Field'); if ($select.hasClass('select2-hidden-accessible')) $select.select2('destroy') }
  $select = null
})
</script>

<template>
  <select ref="selectEl" class="select2-field" :disabled="disabled">
    <option value=""></option>
  </select>
</template>

<style>
.select2-container { width: 100% !important; font-size: 13px; color: #40545e !important; }
.select2-container * { box-sizing: border-box; }
.select2-container--default .select2-selection--single .select2-selection__rendered,
.select2-container--default .select2-selection--multiple .select2-selection__rendered { color: #40545e !important; }
.select2-container--default .select2-selection--single .select2-selection__placeholder { color: #7b8c96 !important; opacity: 1 !important; }
.select2-dropdown { background-color: #fff !important; color: #40545e !important; }
.select2-results, .select2-results__options { background-color: #fff !important; color: #40545e !important; }
.select2-container--default .select2-results__option { color: #40545e !important; background-color: #fff !important; }
.select2-container--default .select2-results__option--selected { background-color: #eaf4fb !important; color: #17303b !important; }
.select2-container--default .select2-results__option--highlighted.select2-results__option--selectable { color: #fff !important; background-color: #1689cf !important; }
.select2-container--default .select2-selection--single { min-height: 42px; border: 1px solid #dce6ed; border-radius: 10px; display: flex; align-items: center; background: #fff; }
.select2-container--default .select2-selection--single .select2-selection__rendered { color: #40545e; line-height: 40px; padding-left: 12px; padding-right: 30px; }
.select2-container--default .select2-selection--single .select2-selection__arrow { height: 40px; right: 7px; }
.select2-container--default.select2-container--open .select2-selection--single, .select2-container--default.select2-container--focus .select2-selection--single { border-color: #1689cf; box-shadow: 0 0 0 .2rem rgba(22,137,207,.12); }
.select2-dropdown { border-color: #dce6ed; border-radius: 8px; overflow: hidden; z-index: 2055; }
.select2-search--dropdown .select2-search__field { border: 1px solid #dce6ed; border-radius: 6px; padding: 7px 9px; outline-color: #1689cf; }

</style>
