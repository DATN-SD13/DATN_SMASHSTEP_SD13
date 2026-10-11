<script setup>
import { ref, watch, computed } from 'vue'
import { formatNumber, parseNumber, formatCurrencyWhileTyping } from '../services/numberFormat'
import { validatePriceValue } from '../services/priceValidation'
import { useProductToast } from '../composables/useProductToast'
defineOptions({ inheritAttrs: false })
const props = defineProps({ modelValue: { default: null }, placeholder: String, disabled: Boolean, required: Boolean, min: Number, max: Number, currency: Boolean, integer: { type: Boolean, default: true }, validationError: String })
const emit = defineEmits(['update:modelValue', 'editing'])
const decimal = computed(() => props.currency || !props.integer)
const text = ref(formatNumber(props.modelValue, decimal.value)), touched = ref(false), localError = ref('')
const toast = useProductToast()
let lastEmitted = props.modelValue, lastBlurWarning = ''
watch(() => props.modelValue, value => {
  if (!Object.is(value, lastEmitted)) { text.value = formatNumber(value, decimal.value); touched.value = false; localError.value = ''; lastBlurWarning = '' }
  lastEmitted = value
})
const error = computed(() => touched.value ? localError.value || props.validationError || '' : '')
function parsed() {
  if (decimal.value) return validatePriceValue(text.value, { raw: true, required: props.required })
  const result = parseNumber(text.value)
  let message = ''
  if (!result.valid) message = 'Số lượng phải là số nguyên không âm, ví dụ: 10.000.'
  else if (result.empty && props.required) message = 'Số lượng không được để trống.'
  else if (!result.empty && props.min != null && result.value < props.min) message = 'Số lượng không được nhỏ hơn 0.'
  else if (!result.empty && props.max != null && result.value > props.max) message = 'Giá trị tối đa là ' + formatNumber(props.max) + '.'
  return { ...result, valid: !message, message }
}
function updateModel(value) { lastEmitted = value; emit('update:modelValue', value) }
function validate() {
  touched.value = true
  const result = parsed()
  localError.value = result.message || ''
  if (result.valid) {
    text.value = formatNumber(result.canonical ?? result.value, decimal.value)
    updateModel(result.value)
  }
  return result
}
function blur(event) {
  // Leave validation to submit when clicking its button. Rendering a blur error here
  // can move the button between pointerdown and click and swallow the submission.
  const button = event.relatedTarget?.closest?.('button, input[type="submit"]')
  const submitting = button?.type === 'submit' && button.form === event.target.form
  if (submitting) return
  const result = validate()
  if (props.currency && !result.valid && lastBlurWarning !== text.value + result.message) {
    lastBlurWarning = text.value + result.message
    toast.warning(result.code === 'format' ? 'Vui lòng nhập đúng định dạng, ví dụ 1.250.000.' : result.message, 'Giá bán không hợp lệ')
  }
}
function input(event) {
  touched.value = false
  localError.value = ''; lastBlurWarning = ''
  emit('editing')
  const element = event.target, before = element.value, caret = element.selectionStart ?? before.length
  const digitsBefore = before.slice(0, caret).replace(/[^0-9,]/g, '').length
  let result
  if (decimal.value) {
    // Formatting and parsing run on every input; business errors wait for blur/submit.
    text.value = formatCurrencyWhileTyping(before)
    result = parseNumber(text.value, true)
  } else {
    // Preserve the existing quantity editing behavior.
    let candidate = before
    const editingDigits = (event.inputType === 'insertText' && /^\d$/.test(event.data || '')) || /^deleteContent/.test(event.inputType || '')
    if (editingDigits && parseNumber(text.value).valid && /^\d[\d.]*$/.test(before)) candidate = before.replaceAll('.', '')
    result = parseNumber(candidate)
    text.value = result.valid && !result.empty ? formatNumber(result.value) : before
  }
  // Retain intermediate/invalid text and the last valid model until blur or submit checks it.
  element.value = text.value
  let cursor = 0, seen = 0
  while (cursor < text.value.length && seen < digitsBefore) { if (/[0-9,]/.test(text.value[cursor])) seen++; cursor++ }
  if (text.value === before) element.setSelectionRange(caret, caret)
  else element.setSelectionRange(cursor, cursor)
  if (result.valid && !result.empty) updateModel(result.value)
}
defineExpose({ validate })
</script>
<template>
  <div class="p-number-control">
    <input v-bind="$attrs" :value="text" type="text" :inputmode="currency || !integer ? 'decimal' : 'numeric'" :placeholder="placeholder || (currency ? 'Ví dụ: 1.250.000' : 'Ví dụ: 100')" :disabled="disabled" :required="required" :aria-invalid="!!error" autocomplete="off" @input="input" @blur="blur" />
    <small v-if="error" class="p-field-error">{{ error }}</small>
  </div>
</template>
