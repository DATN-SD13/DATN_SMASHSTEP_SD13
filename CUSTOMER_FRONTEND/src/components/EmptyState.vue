<script setup>
import BaseIcon from './BaseIcon.vue'
defineProps({ title: { type: String, default: 'Không tìm thấy sản phẩm phù hợp.' }, description: String, action: String, error: Boolean })
defineEmits(['action'])
</script>
<template><div class="empty-state" :role="error ? 'alert' : 'status'"><span class="empty-icon"><BaseIcon :name="error ? 'close' : 'search'" /></span><h2>{{ title }}</h2><p v-if="description">{{ description }}</p><button v-if="action" class="button button-primary" @click="$emit('action')">{{ action }}</button></div></template>
