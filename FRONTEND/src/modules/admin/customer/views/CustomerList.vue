<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import SsPager from '../../../../components/SsPager.vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { usePaging, initials } from '../../../../utils/paging'

const router = useRouter()
const rows = ref([
  { name: 'Vũ Chí Tuấn Anh', email: 'vcta27102007@gmail.com', address: 'Chung cư Thăng Long, đường quốc lộ, Phường An Khánh, Tỉnh Hoài Đức', phone: '0383854485', active: true },
  { name: 'Lê Minh Quang', email: 'kh1@gmail.com', address: 'Số 1 Trần Thái Tông, Phường Cầu Giấy, Hà Nội', phone: '0934000001', active: true },
  { name: 'Đinh Doãn Hùng', email: 'kh2@gmail.com', address: 'Số 2 Trần Thái Tông, Phường Cầu Giấy, Hà Nội', phone: '0934000002', active: true },
  { name: 'Lý Hoàng Trung', email: 'kh3@gmail.com', address: 'Số 3 Trần Thái Tông, Phường Cầu Giấy, Hà Nội', phone: '0934000003', active: true },
  { name: 'Đặng Bá Khôi', email: 'kh4@gmail.com', address: 'Số 4 Trần Thái Tông, Phường Cầu Giấy, Hà Nội', phone: '0934000004', active: true }
])
const q = ref('')
const status = ref('all')

const filtered = computed(() => rows.value.filter(c => {
  const k = q.value.trim().toLowerCase()
  return (!k || c.name.toLowerCase().includes(k) || c.email.toLowerCase().includes(k) || c.phone.includes(k)) &&
    (status.value === 'all' || (status.value === 'on') === c.active)
}))
const { page, size, pages, paged, offset } = usePaging(filtered, 5)
const reset = () => { q.value = ''; status.value = 'all' }
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <section class="ss-card">
        <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-funnel"></i></div><h2>Bộ lọc</h2></div>
        <div class="ss-toolbar">
          <div class="ss-search grow"><i class="bi bi-search"></i><input class="ss-input" v-model="q" placeholder="Tìm theo tên đăng nhập, họ tên, email, SĐT..." /></div>
          <select class="ss-select" v-model="status" style="width:150px"><option value="all">Tất cả</option><option value="on">Hoạt động</option><option value="off">Đã khóa</option></select>
          <button class="ss-btn" @click="reset">Đặt lại bộ lọc</button>
          <button class="ss-btn"><i class="bi bi-file-earmark-excel"></i> Xuất Excel</button>
          <button class="ss-btn primary" @click="router.push('/khach-hang/them')"><i class="bi bi-plus-lg"></i> Thêm khách hàng</button>
        </div>
      </section>

      <section class="ss-card">
        <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-people"></i></div><h2>Danh sách khách hàng</h2><span class="ss-spacer"></span><span class="ss-count">{{ filtered.length }} bản ghi hiển thị.</span></div>
        <div class="ss-table-wrap">
          <table class="ss-table">
            <thead><tr><th class="w-stt c">STT</th><th>Ảnh</th><th>Họ tên</th><th>Email</th><th>Địa chỉ</th><th>Số điện thoại</th><th>Trạng thái</th><th>Hành động</th></tr></thead>
            <tbody>
              <tr v-for="(c, i) in paged" :key="c.email">
                <td class="c">{{ offset + i + 1 }}</td>
                <td><span class="ss-avatar" style="width:40px;height:40px;flex-basis:40px">{{ initials(c.name) }}</span></td>
                <td class="ss-strong">{{ c.name }}</td>
                <td>{{ c.email }}</td>
                <td style="min-width:240px">{{ c.address }}</td>
                <td class="nowrap">{{ c.phone }}</td>
                <td><span class="ss-pill" :class="c.active ? 'success' : 'danger'">{{ c.active ? 'Hoạt động' : 'Đã khóa' }}</span></td>
                <td><div class="ss-row-actions">
                  <button class="ss-icon-btn" title="Xem chi tiết"><i class="bi bi-eye"></i></button>
                  <button class="ss-icon-btn" title="Sửa"><i class="bi bi-pencil"></i></button>
                  <button class="ss-icon-btn" title="Địa chỉ"><i class="bi bi-geo-alt"></i></button>
                  <button class="ss-icon-btn danger" :title="c.active ? 'Khóa tài khoản' : 'Mở khóa'" @click="c.active = !c.active"><i class="bi" :class="c.active ? 'bi-lock' : 'bi-unlock'"></i></button>
                </div></td>
              </tr>
              <tr v-if="!paged.length"><td colspan="8" class="ss-empty"><i class="bi bi-inbox"></i>Không tìm thấy khách hàng.</td></tr>
            </tbody>
          </table>
        </div>
        <SsPager v-model:page="page" v-model:size="size" :pages="pages" />
      </section>
    </main>
  </AdminLayout>
</template>
