<script setup>
import { computed, ref, watch } from 'vue'
import { imageUrl } from '../utils/image'
import BaseIcon from './BaseIcon.vue'
const props = defineProps({ src: String, alt: { type: String, default: 'Sản phẩm SmashStep' }, eager: Boolean })
const emit = defineEmits(['unavailable'])
const failed = ref(false), url = computed(() => imageUrl(props.src))
watch(() => props.src, () => { failed.value = false })
function onError() { failed.value = true; emit('unavailable') }
</script>
<template><div class="product-image"><img v-if="url && !failed" :src="url" :alt="alt" :loading="eager ? 'eager' : 'lazy'" decoding="async" @error="onError"><div v-else class="image-placeholder" role="img" :aria-label="alt + ' — chưa có ảnh'"><BaseIcon name="image" /><span>SMASHSTEP</span></div></div></template>
