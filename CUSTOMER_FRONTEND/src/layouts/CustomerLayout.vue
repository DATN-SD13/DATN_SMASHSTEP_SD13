<script setup>
import CustomerHeader from '../components/CustomerHeader.vue'
import CustomerFooter from '../components/CustomerFooter.vue'
</script>
<template><a class="skip-link" href="#main-content">Đến nội dung chính</a><CustomerHeader /><main id="main-content"><RouterView /></main><CustomerFooter /></template>
