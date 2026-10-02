<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import AvatarCard from '../../../../components/AvatarCard.vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { provinceNames, wardsOf } from '../../../../utils/address'

const router = useRouter()
const f = ref({ name: '', email: '', phone: '', gender: '', dob: '', image: '', rName: '', rPhone: '', province: '', ward: '', street: '' })
const wards = computed(() => wardsOf(f.value.province))
const err = ref('')

function save() {
  const v = f.value
  if (!v.name || !v.email || !v.rName || !v.rPhone || !v.province || !v.ward || !v.street) { err.value = 'Vui lòng nhập đầy đủ các trường bắt buộc (*).'; return }
  if (!/^\S+@\S+\.\S+$/.test(v.email)) { err.value = 'Email không hợp lệ.'; return }
  err.value = ''
  alert('Đã tạo khách hàng')
  router.push('/khach-hang')
}
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <div><button class="ss-back" aria-label="Quay lại" @click="router.push('/khach-hang')"><i class="bi bi-arrow-left"></i></button></div>

      <div class="ss-split">
        <AvatarCard v-model:image="f.image" :name="f.name" :email="f.email" fallback="KH" />
        <div class="ss-stack">
          <section class="ss-card ss-form">
            <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-person"></i></div><div><h2>Thông tin cơ bản</h2><p>Họ tên, email, liên hệ và tài khoản.</p></div></div>
            <div class="ss-grid2">
              <div class="ss-field"><label class="ss-label">Họ và tên <span class="req">*</span></label><input class="ss-input" v-model="f.name" placeholder="Nhập họ tên" /></div>
              <div class="ss-field"><label class="ss-label">Email <span class="req">*</span></label><input class="ss-input" type="email" v-model="f.email" placeholder="Nhập email" /></div>
              <div class="ss-field"><label class="ss-label">Số điện thoại</label><input class="ss-input" v-model="f.phone" placeholder="VD: 0901234567" /></div>
              <div class="ss-field"><label class="ss-label">Giới tính</label>
                <select class="ss-select" v-model="f.gender"><option value="">-- Chọn giới tính --</option><option>Nam</option><option>Nữ</option><option>Khác</option></select></div>
              <div class="ss-field"><label class="ss-label">Ngày sinh</label><input class="ss-input" type="date" v-model="f.dob" /></div>
            </div>
          </section>

          <section class="ss-card ss-form">
            <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-geo-alt"></i></div><div><h2>Địa chỉ giao hàng</h2><p>Địa chỉ mặc định của khách hàng mới.</p></div></div>
            <div class="ss-grid2">
              <div class="ss-field"><label class="ss-label">Họ tên người nhận <span class="req">*</span></label><input class="ss-input" v-model="f.rName" placeholder="Họ tên người nhận" /></div>
              <div class="ss-field"><label class="ss-label">Số điện thoại <span class="req">*</span></label><input class="ss-input" v-model="f.rPhone" placeholder="VD: 0901234567" /></div>
              <div class="ss-field"><label class="ss-label">Tỉnh/Thành phố <span class="req">*</span></label>
                <select class="ss-select" v-model="f.province" @change="f.ward = ''"><option value="">-- Chọn tỉnh/thành --</option><option v-for="p in provinceNames" :key="p">{{ p }}</option></select></div>
              <div class="ss-field"><label class="ss-label">Phường/Xã <span class="req">*</span></label>
                <select class="ss-select" v-model="f.ward" :disabled="!f.province"><option value="">-- Chọn phường/xã --</option><option v-for="w in wards" :key="w">{{ w }}</option></select></div>
              <div class="ss-field full"><label class="ss-label">Địa chỉ cụ thể <span class="req">*</span></label><input class="ss-input" v-model="f.street" placeholder="Số nhà, tên đường..." /></div>
            </div>
            <p v-if="err" class="ss-hint warn"><i class="bi bi-exclamation-triangle"></i> {{ err }}</p>
            <div class="ss-actions left"><button class="ss-btn primary" @click="save">Tạo khách hàng</button><button class="ss-btn" @click="router.push('/khach-hang')">Hủy</button></div>
          </section>
        </div>
      </div>
    </main>
  </AdminLayout>
</template>
