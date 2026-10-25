<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import EmployeeForm from '../components/EmployeeForm.vue'
import employeeService from '../services/employeeService'

import { onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const employee = ref(null)
const loading = ref(true)
const error = ref('')
let loadVersion = 0

function getErrorMessage(err) {
  return (
    err?.response?.data?.message ||
    err?.message ||
    'Không thể tải thông tin nhân viên.'
  )
}

async function loadEmployee() {
  const version = ++loadVersion
  const code = route.params.code
  loading.value = true
  error.value = ''
  employee.value = null

  try {
    const data = await employeeService.getEmployee(code)
    if (version === loadVersion) employee.value = data
  } catch (err) {
    if (version === loadVersion) error.value = getErrorMessage(err)
  } finally {
    if (version === loadVersion) loading.value = false
  }
}

watch(() => route.params.code, loadEmployee, { immediate: true })
onBeforeUnmount(() => { loadVersion++ })
</script>

<template>
  <AdminLayout>
    <main class="ss-page">

      <div class="back">
        <button
          class="ss-back"
          aria-label="Quay lại"
          @click="router.push('/nhan-vien')"
        >
          <i class="bi bi-arrow-left"></i>
        </button>

        <strong v-if="employee">
          Mã nhân viên: {{ employee.code || '—' }}
        </strong>
      </div>

      <section
        v-if="loading"
        class="ss-card"
      >
        <div class="ss-empty">
          <i class="bi bi-arrow-repeat"></i>
          Đang tải thông tin nhân viên...
        </div>
      </section>

      <section
        v-else-if="error"
        class="ss-card"
      >
        <p class="ss-hint warn">
          <i class="bi bi-exclamation-triangle"></i>
          {{ error }}
        </p>

        <button
          class="ss-btn"
          @click="router.push('/nhan-vien')"
        >
          Quay lại danh sách
        </button>
      </section>

      <EmployeeForm
        v-else-if="employee"
        :key="employee.code"
        mode="edit"
        :employee="employee"
      />

    </main>
  </AdminLayout>
</template>

<style scoped>
.back {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--ss-ink);
}
</style>
