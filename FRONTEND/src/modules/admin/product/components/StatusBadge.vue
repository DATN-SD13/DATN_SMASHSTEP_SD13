<script setup>
defineProps({ status: Number })
</script>
<template><span class="p-badge" :class="status === 1 ? 'active' : 'inactive'"><i class="bi" :class="status === 1 ? 'bi-check-circle-fill' : 'bi-pause-circle-fill'"></i>{{ status === 1 ? 'Hoạt động' : status === 0 ? 'Ngừng hoạt động' : 'Chưa xác định' }}</span></template>

