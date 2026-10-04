<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import '../product.css'
defineProps({ title: String, description: String, error: String, success: String })
</script>
<template>
  <AdminLayout>
    <main class="product-page">
      <div class="p-breadcrumb"><RouterLink to="/san-pham">Quản lý sản phẩm</RouterLink><span>/</span><strong>{{ title }}</strong></div>
      <div class="p-heading"><div><h1>{{ title }}</h1><p v-if="description">{{ description }}</p></div><div class="p-actions"><slot name="actions" /></div></div>
      <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
      <div v-if="success" class="alert alert-success" role="status">{{ success }}</div>
      <slot />
    </main>
  </AdminLayout>
</template>

