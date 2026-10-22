<script setup>
import { ref, reactive, onMounted } from 'vue'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import api from '../../../../utils/api'
import { useAuthStore } from '../../../../stores/auth'
import { useAddressApi } from '../../../../composables/useAddressApi'
import Select2KhachHang from '../../customer/components/Select2KhachHang.vue'

const authStore = useAuthStore()

// ── State ──────────────────────────────────────────────────
const loading    = ref(true)
const saving     = ref(false)
const savingPass = ref(false)
const tab        = ref('info') // 'info' | 'password'

const profile = reactive({
  code: '', username: '', name: '', email: '', phone: '',
  gender: null, dob: '', role: '', image: '',
  province: '', ward: '', street: '', createdAt: ''
})

const form = ref({
  name: '', email: '', phone: '',
  gender: null, dob: '',
  roleId: null,
  province: '', ward: '', street: '', image: ''
})

const { provinces, wards, loadingProvinces, loadingWards, addressError, changeProvince } = useAddressApi(form)

const passForm = reactive({ matKhauCu: '', matKhauMoi: '', xacNhan: '' })
const showOld  = ref(false)
const showNew  = ref(false)
const showCfm  = ref(false)

const toast = reactive({ show: false, type: 'success', msg: '' })
const avatarUploading = ref(false)
const avatarInput = ref(null)

// ── Load ───────────────────────────────────────────────────
onMounted(async () => {
  try {
    const res = await api.get('/nhan-vien/me')
    // FRONTEND api.js trả về response gốc, data nằm ở res.data?.data
    const data = res.data?.data ?? res.data ?? res
    fillProfile(data)
  } finally {
    loading.value = false
  }
})

function fillProfile(data) {
  Object.assign(profile, {
    code:      data.code      || '',
    username:  data.username  || '',
    name:      data.name      || '',
    email:     data.email     || '',
    phone:     data.phone     || '',
    gender:    data.gender,
    dob:       data.dob       || '',
    role:      data.role      || '',
    image:     data.image     || '',
    province:  data.province  || '',
    ward:      data.ward      || '',
    street:    data.street    || '',
    createdAt: data.createdAt || '',
    roleId:    data.roleId
  })
  Object.assign(form.value, {
    name:     profile.name,
    email:    profile.email,
    phone:    profile.phone,
    gender:   profile.gender,
    dob:      profile.dob,
    roleId:   profile.roleId,
    province: profile.province,
    ward:     profile.ward,
    street:   profile.street,
    image:    profile.image
  })
}

// ── Avatar upload ──────────────────────────────────────────
async function onAvatarChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  avatarUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const res = await api.post('/uploads/avatar', fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const url = res?.data?.data?.url ?? res?.data?.url ?? res?.url ?? res
    form.value.image = url
    profile.image    = url
    showToast('success', 'Tải ảnh lên thành công!')
  } catch (e) {
    showToast('error', e.response?.data?.message || 'Tải ảnh thất bại.')
  } finally {
    avatarUploading.value = false
    if (avatarInput.value) avatarInput.value.value = ''
  }
}

// ── Lưu thông tin ─────────────────────────────────────────
async function saveInfo() {
  if (saving.value) return
  const err = validateInfo()
  if (err) { showToast('error', err); return }

  saving.value = true
  try {
    const res = await api.put('/nhan-vien/me', {
      name:     form.value.name.trim(),
      email:    form.value.email.trim(),
      phone:    form.value.phone.trim(),
      gender:   form.value.gender,
      dob:      form.value.dob,
      roleId:   form.value.roleId,
      province: form.value.province?.trim() || null,
      ward:     form.value.ward?.trim()     || null,
      street:   form.value.street?.trim()   || null,
      image:    form.value.image            || null
    })
    const data = res.data?.data ?? res.data ?? res
    fillProfile(data)
    // Cập nhật tên trong store
    authStore.user = { ...authStore.user, name: data.name, avatar: data.image }
    localStorage.setItem('ss_user', JSON.stringify(authStore.user))
    showToast('success', 'Cập nhật thông tin thành công!')
  } catch (e) {
    showToast('error', e.response?.data?.message || 'Lưu thất bại.')
  } finally {
    saving.value = false
  }
}

function validateInfo() {
  if (!form.value.name?.trim())  return 'Vui lòng nhập họ tên.'
  if (!form.value.email?.trim()) return 'Vui lòng nhập email.'
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.email.trim())) return 'Email không hợp lệ.'
  if (!form.value.phone?.trim()) return 'Vui lòng nhập số điện thoại.'
  if (!/^0\d{9}$/.test(form.value.phone.trim())) return 'Số điện thoại phải gồm 10 số và bắt đầu bằng 0.'
  return null
}

// ── Đổi mật khẩu ──────────────────────────────────────────
async function savePassword() {
  if (savingPass.value) return
  if (!passForm.matKhauCu)  { showToast('error', 'Vui lòng nhập mật khẩu hiện tại.'); return }
  if (passForm.matKhauMoi.length < 6) { showToast('error', 'Mật khẩu mới phải có ít nhất 6 ký tự.'); return }
  if (passForm.matKhauMoi !== passForm.xacNhan) { showToast('error', 'Mật khẩu xác nhận không khớp.'); return }

  savingPass.value = true
  try {
    await api.patch('/nhan-vien/me/doi-mat-khau', {
      matKhauCu:  passForm.matKhauCu,
      matKhauMoi: passForm.matKhauMoi
    })
    passForm.matKhauCu = passForm.matKhauMoi = passForm.xacNhan = ''
    showToast('success', 'Đổi mật khẩu thành công!')
  } catch (e) {
    showToast('error', e.response?.data?.message || 'Đổi mật khẩu thất bại.')
  } finally {
    savingPass.value = false
  }
}

// ── Toast ──────────────────────────────────────────────────
function showToast(type, msg) {
  toast.type = type; toast.msg = msg; toast.show = true
  setTimeout(() => { toast.show = false }, 3500)
}

function fmtDate(val) {
  if (!val) return '—'
  const s = String(val).substring(0, 10)
  const [y, m, d] = s.split('-')
  return d && m && y ? `${d}/${m}/${y}` : s
}
function fmtDatetime(val) {
  if (!val) return '—'
  return new Date(val).toLocaleString('vi-VN')
}
</script>

<template>
  <AdminLayout>
    <main class="ss-page profile-page">

      <!-- Toast -->
      <Transition name="toast">
        <div v-if="toast.show" :class="['profile-toast', toast.type]">
          <i :class="toast.type === 'success' ? 'bi bi-check-circle-fill' : 'bi bi-exclamation-circle-fill'"></i>
          {{ toast.msg }}
        </div>
      </Transition>

      <!-- Loading -->
      <div v-if="loading" class="profile-loading">
        <i class="bi bi-arrow-repeat spin"></i> Đang tải...
      </div>

      <template v-else>
        <div class="profile-layout">

          <!-- ── LEFT: Avatar card ── -->
          <aside class="profile-aside">
            <div class="avatar-card">
              <div class="avatar-wrap">
                <img v-if="profile.image" :src="profile.image" class="avatar-img" alt="Avatar" />
                <div v-else class="avatar-initials">
                  {{ (profile.name || 'NV').split(' ').slice(-2).map(w => w[0]).join('').toUpperCase() }}
                </div>
                <button
                  class="avatar-change-btn"
                  :disabled="avatarUploading"
                  @click="avatarInput.click()"
                  title="Thay ảnh đại diện"
                >
                  <i v-if="avatarUploading" class="bi bi-arrow-repeat spin"></i>
                  <i v-else class="bi bi-camera-fill"></i>
                </button>
                <input ref="avatarInput" type="file" accept="image/jpeg,image/png,image/webp" hidden @change="onAvatarChange" />
              </div>

              <h2 class="aside-name">{{ profile.name || '—' }}</h2>
              <span class="aside-role">{{ profile.role || '—' }}</span>

              <div class="aside-meta">
                <div class="aside-meta-item">
                  <i class="bi bi-person-badge"></i>
                  <span>{{ profile.code || '—' }}</span>
                </div>
                <div class="aside-meta-item">
                  <i class="bi bi-at"></i>
                  <span>{{ profile.username || '—' }}</span>
                </div>
                <div class="aside-meta-item">
                  <i class="bi bi-calendar3"></i>
                  <span>Tham gia {{ fmtDatetime(profile.createdAt) }}</span>
                </div>
              </div>
            </div>
          </aside>

          <!-- ── RIGHT: Tabs ── -->
          <section class="profile-main">

            <!-- Tab bar -->
            <div class="tab-bar">
              <button :class="['tab-btn', { active: tab === 'info' }]" @click="tab = 'info'">
                <i class="bi bi-person-lines-fill"></i> Thông tin cá nhân
              </button>
              <button :class="['tab-btn', { active: tab === 'password' }]" @click="tab = 'password'">
                <i class="bi bi-shield-lock"></i> Đổi mật khẩu
              </button>
            </div>

            <!-- ── Tab: Thông tin ── -->
            <div v-if="tab === 'info'" class="ss-card tab-content">
              <div class="form-grid">

                <div class="ss-field">
                  <label class="ss-label">Họ và tên <span class="req">*</span></label>
                  <input class="ss-input" v-model="form.name" placeholder="Nhập họ tên" />
                </div>

                <div class="ss-field">
                  <label class="ss-label">Email <span class="req">*</span></label>
                  <input class="ss-input" v-model="form.email" type="email" placeholder="Nhập email" />
                </div>

                <div class="ss-field">
                  <label class="ss-label">Số điện thoại <span class="req">*</span></label>
                  <input class="ss-input" v-model="form.phone" type="tel" placeholder="0xxxxxxxxx" />
                </div>

                <div class="ss-field">
                  <label class="ss-label">Ngày sinh</label>
                  <input class="ss-input input-readonly" :value="fmtDate(form.dob)" readonly />
                </div>

                <div class="ss-field">
                  <label class="ss-label">Tỉnh / Thành phố</label>
                  <Select2KhachHang
                    v-model="form.province"
                    :options="provinces.map(p => ({ id: p.name, text: p.name }))"
                    placeholder="Chọn tỉnh / thành phố"
                    :disabled="loadingProvinces"
                    :search="true"
                    :allow-clear="true"
                    dropdown-parent-selector=""
                    @change="changeProvince"
                  />
                  <p v-if="addressError" class="ss-field-error">{{ addressError }}</p>
                </div>

                <div class="ss-field">
                  <label class="ss-label">Phường / Xã</label>
                  <Select2KhachHang
                    v-model="form.ward"
                    :options="wards.map(w => ({ id: w.name, text: w.name }))"
                    placeholder="Chọn phường / xã"
                    :disabled="loadingWards || !form.province"
                    :search="true"
                    :allow-clear="true"
                    dropdown-parent-selector=""
                  />
                </div>

                <div class="ss-field full">
                  <label class="ss-label">Địa chỉ cụ thể</label>
                  <input class="ss-input" v-model="form.street" placeholder="Số nhà, tên đường..." />
                </div>

              </div>

              <div class="form-actions">
                <button class="ss-btn primary" :disabled="saving" @click="saveInfo">
                  <i v-if="saving" class="bi bi-arrow-repeat spin"></i>
                  <i v-else class="bi bi-check2"></i>
                  {{ saving ? 'Đang lưu…' : 'Lưu thay đổi' }}
                </button>
              </div>
            </div>

            <!-- ── Tab: Đổi mật khẩu ── -->
            <div v-if="tab === 'password'" class="ss-card tab-content pass-tab">
              <div class="pass-form">

                <div class="ss-field">
                  <label class="ss-label">Mật khẩu hiện tại <span class="req">*</span></label>
                  <div class="pass-wrap">
                    <input class="ss-input" :type="showOld ? 'text' : 'password'"
                      v-model="passForm.matKhauCu" placeholder="Nhập mật khẩu hiện tại"
                      autocomplete="current-password" />
                    <button type="button" class="toggle-pass" @click="showOld = !showOld">
                      <i :class="showOld ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                    </button>
                  </div>
                </div>

                <div class="ss-field">
                  <label class="ss-label">Mật khẩu mới <span class="req">*</span></label>
                  <div class="pass-wrap">
                    <input class="ss-input" :type="showNew ? 'text' : 'password'"
                      v-model="passForm.matKhauMoi" placeholder="Ít nhất 6 ký tự"
                      autocomplete="new-password" />
                    <button type="button" class="toggle-pass" @click="showNew = !showNew">
                      <i :class="showNew ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                    </button>
                  </div>
                </div>

                <div class="ss-field">
                  <label class="ss-label">Xác nhận mật khẩu mới <span class="req">*</span></label>
                  <div class="pass-wrap">
                    <input class="ss-input" :type="showCfm ? 'text' : 'password'"
                      v-model="passForm.xacNhan" placeholder="Nhập lại mật khẩu mới"
                      autocomplete="new-password" />
                    <button type="button" class="toggle-pass" @click="showCfm = !showCfm">
                      <i :class="showCfm ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                    </button>
                  </div>
                </div>

                <div class="form-actions">
                  <button class="ss-btn primary" :disabled="savingPass" @click="savePassword">
                    <i v-if="savingPass" class="bi bi-arrow-repeat spin"></i>
                    <i v-else class="bi bi-shield-check"></i>
                    {{ savingPass ? 'Đang lưu…' : 'Đổi mật khẩu' }}
                  </button>
                </div>

              </div>
            </div>

          </section>
        </div>
      </template>
    </main>
  </AdminLayout>
</template>

<style scoped>
.profile-page { padding: 28px; }

.profile-loading {
  display: flex; align-items: center; justify-content: center;
  min-height: 300px; font-size: 14px; color: var(--ss-muted); gap: 8px;
}

/* ── Layout ────────────────────────────────────────────── */
.profile-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 24px;
  align-items: start;
}

@media (max-width: 860px) {
  .profile-layout { grid-template-columns: 1fr; }
}

/* ── Avatar card ───────────────────────────────────────── */
.avatar-card {
  background: #fff;
  border: 1px solid var(--ss-border);
  border-radius: 14px;
  padding: 28px 20px 24px;
  text-align: center;
  display: flex; flex-direction: column; align-items: center; gap: 10px;
}

.avatar-wrap {
  position: relative;
  width: 96px; height: 96px;
  margin-bottom: 4px;
}

.avatar-img {
  width: 96px; height: 96px;
  border-radius: 50%; object-fit: cover;
  border: 3px solid var(--ss-primary-soft);
}

.avatar-initials {
  width: 96px; height: 96px; border-radius: 50%;
  background: var(--ss-primary-soft);
  color: var(--ss-primary);
  display: flex; align-items: center; justify-content: center;
  font-size: 28px; font-weight: 700;
  border: 3px solid var(--ss-primary-soft);
}

.avatar-change-btn {
  position: absolute; bottom: 0; right: 0;
  width: 28px; height: 28px; border-radius: 50%;
  background: var(--ss-primary); color: #fff;
  border: 2px solid #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; cursor: pointer;
  transition: background .15s;
}
.avatar-change-btn:hover { background: var(--ss-primary-hover, #1567c5); }
.avatar-change-btn:disabled { opacity: .6; cursor: not-allowed; }

.aside-name {
  font-size: 16px; font-weight: 700; color: var(--ss-ink);
  margin: 0; line-height: 1.3;
}

.aside-role {
  font-size: 12px; font-weight: 600;
  background: var(--ss-primary-soft); color: var(--ss-primary);
  padding: 3px 10px; border-radius: 20px;
}

.aside-meta {
  width: 100%; display: flex; flex-direction: column;
  gap: 8px; margin-top: 8px;
  border-top: 1px solid var(--ss-border); padding-top: 14px;
}

.aside-meta-item {
  display: flex; align-items: flex-start; gap: 8px;
  font-size: 12px; color: var(--ss-muted); text-align: left;
}
.aside-meta-item i { font-size: 13px; margin-top: 1px; flex-shrink: 0; }

/* ── Tab bar ───────────────────────────────────────────── */
.tab-bar {
  display: flex; gap: 4px;
  border-bottom: 2px solid var(--ss-border);
  margin-bottom: 20px;
}

.tab-btn {
  height: 40px; padding: 0 18px;
  background: none; border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  font-size: 13px; font-weight: 600; color: var(--ss-muted);
  cursor: pointer; display: flex; align-items: center; gap: 7px;
  transition: color .15s, border-color .15s;
}
.tab-btn:hover { color: var(--ss-text); }
.tab-btn.active { color: var(--ss-primary); border-bottom-color: var(--ss-primary); }

/* ── Form ──────────────────────────────────────────────── */
.tab-content { padding: 24px; }

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 24px;
}

.full { grid-column: 1 / -1; }

@media (max-width: 600px) {
  .form-grid { grid-template-columns: 1fr; }
}

.ss-select { appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 16 16'%3E%3Cpath fill='%23718096' d='M7.247 11.14L2.451 5.658C1.885 5.013 2.345 4 3.204 4h9.592a1 1 0 0 1 .753 1.659l-4.796 5.48a1 1 0 0 1-1.506 0z'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 12px center; padding-right: 32px; cursor: pointer; }

.form-actions {
  margin-top: 20px;
  display: flex; justify-content: flex-end;
}

/* ── Pass tab ──────────────────────────────────────────── */
.pass-form {
  max-width: 420px;
  display: flex; flex-direction: column; gap: 4px;
}

.pass-wrap { position: relative; }
.pass-wrap .ss-input { padding-right: 44px; }

.toggle-pass {
  position: absolute; right: 12px; top: 50%;
  transform: translateY(-50%);
  background: none; border: none;
  color: var(--ss-muted); cursor: pointer; font-size: 16px;
}

/* ── Toast ─────────────────────────────────────────────── */
.profile-toast {
  position: fixed; bottom: 28px; right: 28px; z-index: 9999;
  display: flex; align-items: center; gap: 10px;
  padding: 13px 20px; border-radius: 10px;
  font-size: 13.5px; font-weight: 600;
  box-shadow: 0 6px 20px rgba(0,0,0,.15);
  max-width: 340px;
}
.profile-toast.success { background: #e8f5e9; color: #2e7d32; border: 1px solid #a5d6a7; }
.profile-toast.error   { background: #fff1f1; color: #c62828; border: 1px solid #ef9a9a; }

.toast-enter-active, .toast-leave-active { transition: all .3s ease; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translateY(16px); }

.spin { display: inline-block; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.req { color: #e53e3e; }

.ss-field-error {
  margin: 4px 0 0;
  font-size: 12px;
  color: #c62828;
}
</style>
