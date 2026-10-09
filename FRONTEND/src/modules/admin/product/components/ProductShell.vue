<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import '../product.css'
const route = useRoute()
const breadcrumb = computed(() => route.meta.attributeType ? { label: 'Thuộc tính', to: '/thuoc-tinh' } : { label: 'Quản lý sản phẩm', to: '/san-pham' })
defineProps({ title: String, description: String, error: String, success: String })
</script>
<template>
  <AdminLayout>
    <main class="product-page">
      <div class="p-breadcrumb"><RouterLink :to="breadcrumb.to">{{ breadcrumb.label }}</RouterLink><span>/</span><strong>{{ title }}</strong></div>
      <div class="p-heading"><div><h1>{{ title }}</h1><p v-if="description">{{ description }}</p></div><div class="p-actions"><slot name="actions" /></div></div>
      <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
      <div v-if="success" class="alert alert-success" role="status">{{ success }}</div>
      <slot />
    </main>
  </AdminLayout>
</template>
