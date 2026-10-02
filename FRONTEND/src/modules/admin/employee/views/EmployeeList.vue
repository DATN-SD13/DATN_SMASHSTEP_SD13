<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import SsPager from '../../../../components/SsPager.vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { usePaging, initials } from '../../../../utils/paging'
import { employees, roles } from '../services/employeeData'

const router = useRouter()
const rows = ref(employees.map(e => ({ ...e })))
const f = ref({ q: '', role: 'all', status: 'all' })

const filtered = computed(() => rows.value.filter(e => {
  const k = f.value.q.trim().toLowerCase()
  return (!k || e.code.toLowerCase().includes(k) || e.name.toLowerCase().includes(k) || e.email.toLowerCase().includes(k) || e.phone.includes(k)) &&
    (f.value.role === 'all' || e.role === f.value.role) &&
    (f.value.status === 'all' || (f.value.status === 'on') === e.active)
}))
const { page, size, pages, paged, offset } = usePaging(filtered, 5)
const reset = () => { f.value = { q: '', role: 'all', status: 'all' } }
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <section class="ss-card">
        <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-funnel"></i></div><h2>Bộ lọc</h2></div>
        <div class="ss-toolbar">
          <div class="ss-search grow"><i class="bi bi-search"></i><input class="ss-input" v-model="f.q" placeholder="Tìm theo mã, họ tên, tài khoản, SĐT..." /></div>
          <select class="ss-select" v-model="f.role" style="width:180px"><option value="all">Tất cả vai trò</option><option v-for="r in roles" :key="r">{{ r }}</option></select>
          <select class="ss-select" v-model="f.status" style="width:150px"><option value="all">Tất cả</option><option value="on">Hoạt động</option><option value="off">Đã khóa</option></select>
        </div>
        <div class="ss-actions">
          <button class="ss-btn" @click="reset">Đặt lại bộ lọc</button>
          <button class="ss-btn"><i class="bi bi-file-earmark-excel"></i> Xuất Excel</button>
          <button class="ss-btn primary" @click="router.push('/nhan-vien/them')"><i class="bi bi-plus-lg"></i> Thêm nhân viên</button>
        </div>
      </section>

      <section class="ss-card">
        <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-person-badge"></i></div><h2>Danh sách nhân viên</h2><span class="ss-spacer"></span><span class="ss-count">{{ filtered.length }} bản ghi hiển thị.</span></div>
        <div class="ss-table-wrap">
          <table class="ss-table" style="min-width:1000px">
            <thead><tr><th class="w-stt c">STT</th><th>Ảnh</th><th>Mã NV</th><th>Họ tên</th><th>Email</th><th>Giới tính</th><th>SĐT</th><th>Địa chỉ</th><th>Vai trò</th><th>Trạng thái</th><th>Hành động</th></tr></thead>
            <tbody>
              <tr v-for="(e, i) in paged" :key="e.code + i">
                <td class="c">{{ offset + i + 1 }}</td>
                <td><span class="ss-avatar" style="width:40px;height:40px;flex-basis:40px">{{ initials(e.name) }}</span></td>
                <td><span class="ss-code">{{ e.code }}</span></td>
                <td class="ss-strong">{{ e.name }}</td>
                <td>{{ e.email }}</td>
                <td>{{ e.gender }}</td>
                <td class="nowrap">{{ e.phone }}</td>
                <td style="min-width:220px">{{ e.address }}</td>
                <td class="nowrap">{{ e.role }}</td>
                <td><span class="ss-pill" :class="e.active ? 'success' : 'danger'">{{ e.active ? 'Hoạt động' : 'Đã khóa' }}</span></td>
                <td><div class="ss-row-actions">
                  <button class="ss-icon-btn" title="Xem chi tiết" @click="router.push(`/nhan-vien/${e.code}`)"><i class="bi bi-eye"></i></button>
                  <button class="ss-icon-btn danger" :title="e.active ? 'Khóa tài khoản' : 'Mở khóa'" @click="e.active = !e.active"><i class="bi" :class="e.active ? 'bi-lock' : 'bi-unlock'"></i></button>
                </div></td>
              </tr>
              <tr v-if="!paged.length"><td colspan="11" class="ss-empty"><i class="bi bi-inbox"></i>Không tìm thấy nhân viên.</td></tr>
            </tbody>
          </table>
        </div>
        <SsPager v-model:page="page" v-model:size="size" :pages="pages" />
      </section>
    </main>
  </AdminLayout>
</template>
