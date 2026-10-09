<script setup>
import { ref, onMounted, onUpdated, onBeforeUnmount, watch, nextTick } from 'vue'
import $ from 'jquery'
import installSelect2 from 'select2'
import 'select2/dist/css/select2.css'

if (!$.fn.select2) installSelect2(window, $)
const props = defineProps({
  modelValue: { default: '' }, options: { type: Array, default: () => [] },
  placeholder: String, disabled: Boolean, multiple: Boolean,
  allowClear: { type: Boolean, default: true }, search: { type: Boolean, default: true }
})
const emit = defineEmits(['update:modelValue', 'change'])
const select = ref(null)
let control, signature = '', disposed = false, lastValue, keyTargets = []
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
  control.off('.productSelect2')
  if (control.data('select2')) control.select2('destroy')
  control = null
}
function initialize() {
  if (disposed || !select.value) return
  destroy()
  control = $(select.value)
  const parent = select.value.closest('.p-dialog') || select.value.closest('.product-page')
  const placeholder = props.placeholder ?? (!props.multiple ? select.value.options[0]?.value === '' ? select.value.options[0].text : undefined : undefined)
  control.select2({ width: '100%', placeholder, allowClear: props.allowClear && Boolean(placeholder),
    minimumResultsForSearch: props.search ? 0 : Infinity, dropdownParent: parent ? $(parent) : $(document.body),
    closeOnSelect: !props.multiple, language: { noResults: () => 'Không có kết quả' } })
  const instance = control.data('select2')
  keyTargets = [instance.$container[0], instance.$dropdown[0]]
  for (const target of keyTargets) target.addEventListener('keydown', closeDropdownOnEscape, true)
  control.on('change.productSelect2', () => {
    const raw = control.val()
    const value = props.multiple ? (raw || []).map(typed) : raw == null ? '' : typed(raw)
    const key = normalized(value)
    if (key === lastValue) return
    lastValue = key
    emit('update:modelValue', value)
    emit('change', value)
  })
  sync()
}
function refresh() {
  if (!select.value || disposed) return
  const current = JSON.stringify([...select.value.options].map(option => [option.value, option.text, option.disabled])) + JSON.stringify([props.multiple, props.placeholder, props.search, props.allowClear])
  if (current !== signature || !control) { signature = current; initialize() }
  else sync()
}
onMounted(refresh)
onUpdated(refresh)
watch(() => props.modelValue, async () => { await nextTick(); if (!disposed) sync() }, { deep: true })
watch(() => [props.options, props.disabled, props.multiple, props.placeholder, props.search, props.allowClear], async () => {
  await nextTick(); refresh()
}, { deep: true })
onBeforeUnmount(() => { disposed = true; destroy() })
</script>
<template>
  <select ref="select" :multiple="multiple" :disabled="disabled">
    <slot>
      <option v-if="!multiple" value="">{{ placeholder || 'Chọn...' }}</option>
      <option v-for="option in options" :key="String(option.id ?? option.value)" :value="option.value ?? option.id" :disabled="option.disabled">{{ option.text ?? option.ten ?? option.label }}</option>
    </slot>
  </select>
</template>
