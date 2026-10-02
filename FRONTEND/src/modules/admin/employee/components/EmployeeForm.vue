<script setup>
import AvatarCard from '../../../../components/AvatarCard.vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { provinceNames, wardsOf } from '../../../../utils/address'
import { roles } from '../services/employeeData'

// mode = 'create' (frame 12) | 'edit' (frame 11)
const props = defineProps({ mode: { type: String, default: 'create' }, employee: { type: Object, default: null } })
const router = useRouter()
const edit = computed(() => props.mode === 'edit')

const f = ref({
  name: props.employee?.name ?? '', email: props.employee?.email ?? '', phone: props.employee?.phone ?? '',
  role: props.employee?.role ?? 'Nhân viên', gender: props.employee?.gender ?? 'Nam', dob: props.employee?.dob ?? '',
  province: props.employee?.province ?? '', ward: props.employee?.ward ?? '', street: props.employee?.street ?? '', image: ''
})
const active = ref(props.employee?.active ?? true)
const wards = computed(() => wardsOf(f.value.province))
const err = ref('')
const msg = ref('')

function submit() {
  const v = f.value
  if (!v.name || !v.email || !v.phone || !v.dob || !v.province || !v.ward) { err.value = 'Vui lòng nhập đầy đủ các trường bắt buộc (*).'; return }
  if (!/^\S+@\S+\.\S+$/.test(v.email)) { err.value = 'Email không hợp lệ.'; return }
  if (!/^0\d{9}$/.test(v.phone)) { err.value = 'Số điện thoại phải gồm 10 chữ số, bắt đầu bằng 0.'; return }
  err.value = ''
  if (edit.value) { msg.value = 'Đã lưu thay đổi'; setTimeout(() => (msg.value = ''), 2200) }
  else { alert('Đã tạo nhân viên'); router.push('/nhan-vien') }
}
function scanCccd() { alert('Chức năng quét mã CCCD sẽ kết nối thiết bị quét khi có backend.') }
function changePassword() { alert('Đã gửi yêu cầu đổi mật khẩu.') }
</script>

<template>
  <div class="ss-split">
    <div class="ss-stack">
      <AvatarCard v-model:image="f.image" :name="f.name" :email="f.email" fallback="NV" :hint="!edit">
        <span v-if="edit" class="ss-pill" :class="active ? 'success' : 'danger'" style="margin-top:2px">{{ active ? 'Hoạt động' : 'Đã khóa' }}</span>
      </AvatarCard>

      <template v-if="edit">
        <section class="ss-card">
          <h3 class="side-title">Đổi mật khẩu</h3>
          <button class="ss-btn block" @click="changePassword">Đổi mật khẩu</button>
        </section>
        <section class="ss-card">
          <h3 class="side-title">Trạng thái tài khoản</h3>
          <button class="ss-btn block" :class="active ? 'danger-outline' : ''" @click="active = !active">{{ active ? 'Khóa tài khoản' : 'Mở khóa tài khoản' }}</button>
        </section>
      </template>
    </div>

    <div class="ss-stack">
      <section class="ss-card ss-form">
        <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-person-vcard"></i></div><div><h2>Thông tin cơ bản</h2><p>Họ tên, email, liên hệ và tài khoản.</p></div></div>
        <div><button type="button" class="ss-btn sm" @click="scanCccd"><i class="bi bi-upc-scan"></i> Quét mã CCCD</button></div>
        <div class="ss-grid2">
          <div class="ss-field"><label class="ss-label">Họ và tên <span class="req">*</span></label><input class="ss-input" v-model="f.name" placeholder="Nhập họ tên" /></div>
          <div class="ss-field"><label class="ss-label">Email <span class="req">*</span></label><input class="ss-input" type="email" v-model="f.email" placeholder="Nhập email" /></div>
          <div class="ss-field"><label class="ss-label">Số điện thoại <span class="req">*</span></label><input class="ss-input" v-model="f.phone" placeholder="Nhập số điện thoại" /></div>
          <div class="ss-field"><label class="ss-label">Vai trò <span class="req">*</span></label>
            <select class="ss-select" v-model="f.role"><option v-for="r in roles" :key="r">{{ r }}</option></select></div>
          <div class="ss-field"><label class="ss-label">Giới tính <span class="req">*</span></label>
            <select class="ss-select" v-model="f.gender"><option>Nam</option><option>Nữ</option><option>Khác</option></select></div>
          <div class="ss-field"><label class="ss-label">Ngày sinh <span class="req">*</span></label><input class="ss-input" type="date" v-model="f.dob" /></div>
        </div>
      </section>

      <section class="ss-card ss-form">
        <div class="ss-head"><div class="ss-head-icon"><i class="bi bi-geo-alt"></i></div><h2>Địa chỉ</h2></div>
        <div class="ss-grid2">
          <div class="ss-field"><label class="ss-label">Tỉnh/Thành phố <span class="req">*</span></label>
            <select class="ss-select" v-model="f.province" @change="f.ward = ''"><option value="">Chọn tỉnh thành</option><option v-for="p in provinceNames" :key="p">{{ p }}</option></select></div>
          <div class="ss-field"><label class="ss-label">Xã/Phường/Thị trấn <span class="req">*</span></label>
            <select class="ss-select" v-model="f.ward" :disabled="!f.province"><option value="">Chọn xã phường</option><option v-for="w in wards" :key="w">{{ w }}</option></select></div>
          <div class="ss-field full"><label class="ss-label">Địa chỉ cụ thể</label><input class="ss-input" v-model="f.street" placeholder="Nhập địa chỉ cụ thể" /></div>
        </div>
        <p v-if="err" class="ss-hint warn"><i class="bi bi-exclamation-triangle"></i> {{ err }}</p>
        <p v-if="msg" class="ss-hint ok"><i class="bi bi-check-circle"></i> {{ msg }}</p>
        <div class="ss-actions left">
          <button class="ss-btn primary" @click="submit">{{ edit ? 'Lưu thay đổi' : 'Tạo nhân viên' }}</button>
          <button class="ss-btn" @click="router.push('/nhan-vien')">Hủy</button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.side-title { font-size: 12px; font-weight: 700; color: var(--ss-text); }
.ss-hint.ok { color: var(--ss-success); }
</style>
