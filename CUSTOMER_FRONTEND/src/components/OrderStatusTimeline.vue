<script setup>
import { computed } from 'vue'
const props = defineProps({ status: Number, history: { type: Array, default: () => [] } })
const steps = ['Chờ xác nhận', 'Đã xác nhận', 'Chờ lấy hàng', 'Đang giao hàng', 'Đã giao hàng', 'Hoàn thành']
const exceptional = computed(() => props.status === 6 ? 'Đơn hàng đã hủy' : props.status === 7 ? 'Đơn hàng đang xử lý hoàn tiền' : props.status === 9 ? 'Giao hàng thất bại — cửa hàng đang xử lý giao lại hoặc hoàn / hủy đơn.' : '')
function at(index) { return props.history.find(e => e.status === index)?.at }
</script>
<template><section class="shop-card"><h2>Trạng thái đơn hàng</h2><p v-if="exceptional" class="shop-alert">{{ exceptional }}</p><ol v-else class="order-timeline"><li v-for="(step, index) in steps" :key="step" :class="{ done: status >= index, current: status === index }"><span>{{ status > index ? '✓' : index + 1 }}</span><strong>{{ step }}</strong><time v-if="at(index)">{{ new Date(at(index)).toLocaleString('vi-VN') }}</time></li></ol></section></template>
