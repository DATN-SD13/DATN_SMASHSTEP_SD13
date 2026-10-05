<script setup>
import { useRoute } from 'vue-router'
import AdminLayout from '../layouts/AdminLayout.vue'

const route = useRoute()
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <section class="ss-card">
        <h2>{{ route.meta.title }}</h2>
        <p>Mục này hiện chưa có nội dung.</p>
      </section>
    </main>
  </AdminLayout>
</template>
