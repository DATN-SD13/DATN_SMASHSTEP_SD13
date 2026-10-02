<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import EmployeeForm from '../components/EmployeeForm.vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { employees } from '../services/employeeData'

const route = useRoute()
const router = useRouter()
const emp = computed(() => employees.find(e => e.code === route.params.code) || employees[1])
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <div class="back">
        <button class="ss-back" aria-label="Quay lại" @click="router.push('/nhan-vien')"><i class="bi bi-arrow-left"></i></button>
        <strong>Mã nhân viên: {{ emp.code }}</strong>
      </div>
      <EmployeeForm :key="emp.code" mode="edit" :employee="emp" />
    </main>
  </AdminLayout>
</template>

<style scoped>
.back { display: flex; align-items: center; gap: 12px; font-size: 12px; color: var(--ss-ink); }
</style>
