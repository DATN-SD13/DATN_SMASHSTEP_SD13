<script setup>
import { ref, onMounted, onUpdated, onBeforeUnmount, watch, nextTick } from 'vue'
import $ from 'jquery'
import installSelect2 from 'select2'
import 'select2/dist/css/select2.css'

defineOptions({ inheritAttrs: false })

// Select2 and the page must use the same jQuery instance.
window.$ = window.jQuery = $
if (typeof $.fn.select2 !== 'function') installSelect2(window, $)
const props = defineProps({
  modelValue: { default: '' }, options: { type: Array, default: () => [] },
  placeholder: String, disabled: Boolean, multiple: Boolean,
  dropdownParentSelector: { type: String, default: '.modal-mask, .ss-modal-bg, .ss-modal, .p-dialog' },
  allowClear: { type: Boolean, default: true }, search: { type: Boolean, default: true }
})
const emit = defineEmits(['update:modelValue', 'change', 'blur'])
const select = ref(null)
let control, signature = '', disposed = false, lastValue, keyTargets = []
let openAtPointerDown = false
function rememberOpenState() {
  openAtPointerDown = Boolean(control?.data('select2')?.isOpen())
}
function openOnClick(event) {
  const wasOpen = openAtPointerDown
  openAtPointerDown = false
  if (props.disabled || wasOpen || !event.target.closest('.select2-selection') ||
      event.target.closest('.select2-selection__clear')) return
  // Keep normal Select2 mousedown behavior; also accept a click/touch activation.
  const instance = control?.data('select2')
  if (instance && !instance.isOpen()) control.select2('open')
}
function closeDropdownOnEscape(event) {
  if (event.key === 'Escape' && control?.data('select2')?.isOpen()) {
    event.preventDefault(); event.stopPropagation()
    control.select2('close')
  }
}
const normalized = value => JSON.stringify(props.multiple ? (value || []).map(String).sort() : String(value ?? ''))
function typed(value) {
  const option = [...select.value.options].find(item => item.value === String(value))
  return option && '_value' in option ? option._value : props.options.find(item => String(item.id ?? item.value) === String(value))?.value ?? props.options.find(item => String(item.id) === String(value))?.id ?? value
}
function sync() {
  if (!control) return
  control.prop('disabled', props.disabled)
  const value = props.multiple ? (props.modelValue || []).map(String) : String(props.modelValue ?? '')
  control.val(value).trigger('change.select2')
  lastValue = normalized(props.modelValue)
}
function destroy() {
  for (const target of keyTargets) target.removeEventListener('keydown', closeDropdownOnEscape, true)
  keyTargets = []
  if (!control) return
  control.off('.Select2KhachHang')
  if (control.data('select2')) control.select2('destroy')
  control = null
}
function initialize() {
  if (disposed || !select.value) return
  destroy()
  control = $(select.value)
  const parent = props.dropdownParentSelector ? select.value.closest(props.dropdownParentSelector) : null
  const placeholder = props.placeholder ?? (!props.multiple ? select.value.options[0]?.value === '' ? select.value.options[0].text : undefined : undefined)
  control.select2({ width: '100%', placeholder, allowClear: props.allowClear && Boolean(placeholder),
    minimumResultsForSearch: props.search ? 0 : Infinity, dropdownParent: parent ? $(parent) : $(document.body),
    closeOnSelect: !props.multiple, language: { noResults: () => 'Không có kết quả' } })
  const instance = control.data('select2')
  // Select2 4.0.x exposes an outer positioning span as $dropdown.
  // Only the inner .select2-dropdown may clip its contents for rounded corners.
  instance.$dropdown.addClass('khach-hang-select2-portal')
  instance.$dropdown.find('.select2-dropdown').addClass('khach-hang-select2-dropdown')
  keyTargets = [instance.$container[0], instance.$dropdown[0]]
  for (const target of keyTargets) target.addEventListener('keydown', closeDropdownOnEscape, true)
  control.on('change.Select2KhachHang', () => {
    const raw = control.val()
    const value = props.multiple ? (raw || []).map(typed) : raw == null ? '' : typed(raw)
    const key = normalized(value)
    if (key === lastValue) return
    lastValue = key
    emit('update:modelValue', value)
    emit('change', value)
  })
  control.on('select2:close.Select2KhachHang', () => emit('blur'))
  sync()
}
function refresh() {
  if (!select.value || disposed) return
  const current = JSON.stringify([...select.value.options].map(option => [option.value, option.text, option.disabled])) + JSON.stringify([props.multiple, props.placeholder, props.search, props.allowClear, props.dropdownParentSelector])
  if (current !== signature || !control) { signature = current; initialize() }
  else sync()
}
onMounted(refresh)
onUpdated(refresh)
watch(() => props.modelValue, async () => { await nextTick(); if (!disposed) sync() }, { deep: true })
watch(() => [props.options, props.disabled, props.multiple, props.placeholder, props.search, props.allowClear, props.dropdownParentSelector], async () => {
  await nextTick(); refresh()
}, { deep: true })
onBeforeUnmount(() => { disposed = true; destroy() })
</script>
<template>
  <span class="khach-hang-select2" @pointerdown.capture="rememberOpenState" @mousedown.capture="rememberOpenState" @click="openOnClick">
  <select v-bind="$attrs" ref="select" :multiple="multiple" :disabled="disabled">
    <slot>
      <option v-if="!multiple" value="">{{ placeholder || 'Chọn...' }}</option>
      <option v-for="option in options" :key="String(option.id ?? option.value)" :value="option.value ?? option.id" :disabled="option.disabled">{{ option.text ?? option.ten ?? option.label }}</option>
    </slot>
  </select>
  </span>
</template>

<style>

.khach-hang-select-field { display: block; width: 100%; min-width: 0; }
.ss-toolbar > .khach-hang-select-field { flex: 0 1 auto; }
.ss-foot > .khach-hang-select-field.pager-select-field { flex: 0 0 72px; width: 72px; }
.khach-hang-select2 { display:block; min-width:0; width:100%; }
.khach-hang-select2 .select2-container { width:100% !important; min-width:0; font-family:inherit; font-size:12px; }
.khach-hang-select2 .select2-selection--single { min-height:40px; height:40px; display:flex; align-items:center; border:1px solid var(--ss-border); border-radius:8px; background:var(--ss-surface); }
.khach-hang-select2 .select2-selection--single .select2-selection__rendered { display:block; width:100%; line-height:38px; padding:0 30px 0 12px; color:var(--ss-text) !important; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.khach-hang-select2 .select2-selection--single .select2-selection__placeholder { color:var(--ss-text) !important; }
.khach-hang-select2 .select2-selection--single .select2-selection__arrow { height:38px; right:7px; }
.khach-hang-select2 .select2-container--open .select2-selection,.khach-hang-select2 .select2-container--focus .select2-selection { border-color:var(--ss-primary); box-shadow:0 0 0 3px rgba(40,121,227,.1); }
.khach-hang-select2 .select2-container--disabled .select2-selection { opacity:.6; cursor:not-allowed; }
.khach-hang-select2-portal { overflow:visible !important; z-index:2056; }
.khach-hang-select2-dropdown { z-index:2056; border-color:var(--ss-border); border-radius:8px; background:#fff; color:var(--ss-text); font-family:var(--ss-font); font-size:12px; overflow:hidden; }
.khach-hang-select2-dropdown .select2-search__field { border:1px solid var(--ss-border); padding:8px; border-radius:6px; outline-color:var(--ss-primary); }
.khach-hang-select2-dropdown .select2-results__option--highlighted[aria-selected] { background:var(--ss-primary); color:#fff; }
@media(max-width:600px) { .ss-toolbar > .khach-hang-select-field { width:100% !important; flex:1 1 100%; } }
</style>
