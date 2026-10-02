<script setup>
import AdminLayout from './AdminLayout.vue'
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <section class="ss-card soon">
        <div class="ss-head-icon"><i class="bi bi-tools"></i></div>
        <h2>Màn hình đang được phát triển</h2>
        <p>Chức năng này sẽ sớm có mặt trong các phiên bản tiếp theo.</p>
        <RouterLink to="/hoa-don" class="ss-btn primary"><i class="bi bi-arrow-left"></i> Về trang hóa đơn</RouterLink>
      </section>
    </main>
  </AdminLayout>
</template>

<style scoped>
.soon { align-items: center; text-align: center; padding: 64px 16px; gap: 10px; }
.soon .ss-head-icon { width: 48px; height: 48px; font-size: 22px; border-radius: 12px; }
.soon h2 { font-size: 16px; font-weight: 600; color: var(--ss-text); }
.soon p { color: var(--ss-faint); margin-bottom: 8px; }
</style>
