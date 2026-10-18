<script setup>
const props = defineProps({ modelValue: Number, max: { type: Number, default: 10000 }, disabled: Boolean })
const emit = defineEmits(['update:modelValue'])
function change(value) { const n = Number(value); if (Number.isInteger(n) && n >= 1 && n <= Math.min(props.max, 10000)) emit('update:modelValue', n) }
</script>
<template><div class="quantity-stepper"><button type="button" :disabled="disabled || modelValue <= 1" aria-label="Giảm số lượng" @click="change(modelValue - 1)">−</button><input :value="modelValue" type="number" inputmode="numeric" aria-label="Số lượng" min="1" :max="Math.min(max, 10000)" :disabled="disabled" @change="change($event.target.value); $event.target.value = modelValue"><button type="button" :disabled="disabled || modelValue >= Math.min(max, 10000)" aria-label="Tăng số lượng" @click="change(modelValue + 1)">+</button></div></template>
