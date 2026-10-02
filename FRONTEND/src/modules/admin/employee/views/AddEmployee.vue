<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import EmployeeForm from '../components/EmployeeForm.vue'
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <div><button class="ss-back" aria-label="Quay lại" @click="router.push('/nhan-vien')"><i class="bi bi-arrow-left"></i></button></div>
      <EmployeeForm mode="create" />
    </main>
  </AdminLayout>
</template>
