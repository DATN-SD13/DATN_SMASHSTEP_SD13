<script setup>
import { ref, reactive, onMounted } from 'vue'
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import api from '../../../../utils/api'
import { useAuthStore } from '../../../../stores/auth'
import { useAddressApi } from '../../../../composables/useAddressApi'
import Select2KhachHang from '../../customer/components/Select2KhachHang.vue'
import { showSuccess, showError } from '../../../../utils/feedback'

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

// ── Lỗi validate inline ────────────────────────────────────
const errors = reactive({
  name: '', email: '', phone: '', street: '',
  province: '', ward: '',
  matKhauCu: '', matKhauMoi: '', xacNhan: ''
})

function clearErrors() {
  Object.keys(errors).forEach(k => errors[k] = '')
}

function validateField(field) {
  switch (field) {
    case 'name': {
      const v = form.value.name?.trim()
      errors.name = !v                  ? 'Vui lòng nhập họ tên.'
                  : v.length > 255      ? 'Họ tên không được vượt quá 255 ký tự.'
                  : ''
      break
    }
    case 'email': {
      const v = form.value.email?.trim()
      errors.email = !v                                          ? 'Vui lòng nhập email.'
                   : !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)     ? 'Email không hợp lệ.'
                   : v.length > 255                             ? 'Email không được vượt quá 255 ký tự.'
                   : ''
      break
    }
    case 'phone': {
      const v = form.value.phone?.trim()
      errors.phone = !v                    ? 'Vui lòng nhập số điện thoại.'
                   : !/^0\d{9}$/.test(v)  ? 'Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.'
                   : ''
      break
    }
    case 'street': {
      const v = form.value.street?.trim() || ''
      errors.street = v.length > 500 ? 'Địa chỉ cụ thể không được vượt quá 500 ký tự.' : ''
      break
    }
    case 'province':
      errors.province = !form.value.province ? 'Vui lòng chọn tỉnh / thành phố.' : ''
      break
    case 'ward':
      errors.ward = !form.value.ward ? 'Vui lòng chọn phường / xã.' : ''
      break
    case 'matKhauCu':
      errors.matKhauCu = !passForm.matKhauCu?.trim()
        ? 'Vui lòng nhập mật khẩu hiện tại.' : ''
      break
    case 'matKhauMoi':
      errors.matKhauMoi = !passForm.matKhauMoi        ? 'Vui lòng nhập mật khẩu mới.'
                        : passForm.matKhauMoi.length < 6 ? 'Mật khẩu mới phải có ít nhất 6 ký tự.'
                        : ''
      break
    case 'xacNhan':
      errors.xacNhan = !passForm.xacNhan                          ? 'Vui lòng xác nhận mật khẩu mới.'
                     : passForm.xacNhan !== passForm.matKhauMoi    ? 'Mật khẩu xác nhận không khớp.'
                     : ''
      break
  }
}

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
    showSuccess('Tải ảnh lên thành công!')
  } catch (e) {
    showError(e.response?.data?.message || 'Tải ảnh thất bại.')
  } finally {
    avatarUploading.value = false
    if (avatarInput.value) avatarInput.value.value = ''
  }
}

// ── Lưu thông tin ─────────────────────────────────────────
async function saveInfo() {
  if (saving.value) return
  const err = validateInfo()
  if (err) return

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
    showSuccess('Cập nhật thông tin thành công!')
  } catch (e) {
    showError(e.response?.data?.message || 'Lưu thất bại.')
  } finally {
    saving.value = false
  }
}

function validateInfo() {
  validateField('name')
  validateField('email')
  validateField('phone')
  validateField('province')
  validateField('ward')
  validateField('street')
  return errors.name || errors.email || errors.phone || errors.province || errors.ward || errors.street || null
}

// ── Đổi mật khẩu ──────────────────────────────────────────
async function savePassword() {
  if (savingPass.value) return
  // Validate tất cả 3 field rồi mới dừng nếu có lỗi
  validateField('matKhauCu')
  validateField('matKhauMoi')
  validateField('xacNhan')
  if (errors.matKhauCu || errors.matKhauMoi || errors.xacNhan) return

  savingPass.value = true
  try {
    await api.patch('/nhan-vien/me/doi-mat-khau', {
      matKhauCu:  passForm.matKhauCu,
      matKhauMoi: passForm.matKhauMoi
    })
    passForm.matKhauCu = passForm.matKhauMoi = passForm.xacNhan = ''
    errors.matKhauCu = errors.matKhauMoi = errors.xacNhan = ''
    showSuccess('Đổi mật khẩu thành công!')
  } catch (e) {
    showError(e.response?.data?.message || 'Đổi mật khẩu thất bại.')
  } finally {
    savingPass.value = false
  }
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
                  <input class="ss-input" :class="{ 'input-error': errors.name }"
                    v-model="form.name" placeholder="Nhập họ tên"
                    maxlength="255"
                    @blur="validateField('name')" @input="errors.name = ''" />
                  <div class="field-bottom">
                    <p v-if="errors.name" class="field-error">{{ errors.name }}</p>
                    <span v-else-if="(form.name?.length || 0) > 200" class="char-count"
                      :class="{ warn: (form.name?.length || 0) > 230 }">
                      {{ form.name?.length || 0 }}/255
                    </span>
                  </div>
                </div>

                <div class="ss-field">
                  <label class="ss-label">Email <span class="req">*</span></label>
                  <input class="ss-input" :class="{ 'input-error': errors.email }"
                    v-model="form.email" type="email" placeholder="Nhập email"
                    maxlength="255"
                    @blur="validateField('email')" @input="errors.email = ''" />
                  <p v-if="errors.email" class="field-error">{{ errors.email }}</p>
                </div>

                <div class="ss-field">
                  <label class="ss-label">Số điện thoại <span class="req">*</span></label>
                  <input class="ss-input" :class="{ 'input-error': errors.phone }"
                    v-model="form.phone" type="tel" placeholder="0xxxxxxxxx"
                    maxlength="10"
                    @blur="validateField('phone')" @input="errors.phone = ''" />
                  <p v-if="errors.phone" class="field-error">{{ errors.phone }}</p>
                </div>

                <div class="ss-field">
                  <label class="ss-label">Ngày sinh</label>
                  <input class="ss-input input-readonly" :value="fmtDate(form.dob)" readonly />
                </div>

                <div class="ss-field">
                  <label class="ss-label">Tỉnh / Thành phố <span class="req">*</span></label>
                  <Select2KhachHang
                    v-model="form.province"
                    :options="provinces.map(p => ({ id: p.name, text: p.name }))"
                    placeholder="Chọn tỉnh / thành phố"
                    :disabled="loadingProvinces"
                    :search="true"
                    :allow-clear="true"
                    dropdown-parent-selector=""
                    @change="() => { changeProvince(); validateField('province'); errors.ward = '' }"
                  />
                  <p v-if="errors.province" class="field-error">{{ errors.province }}</p>
                  <p v-else-if="addressError" class="field-error">{{ addressError }}</p>
                </div>

                <div class="ss-field">
                  <label class="ss-label">Phường / Xã <span class="req">*</span></label>
                  <Select2KhachHang
                    v-model="form.ward"
                    :options="wards.map(w => ({ id: w.name, text: w.name }))"
                    placeholder="Chọn phường / xã"
                    :disabled="loadingWards || !form.province"
                    :search="true"
                    :allow-clear="true"
                    dropdown-parent-selector=""
                    @change="validateField('ward')"
                  />
                  <p v-if="errors.ward" class="field-error">{{ errors.ward }}</p>
                </div>

                <div class="ss-field full">
                  <label class="ss-label">Địa chỉ cụ thể</label>
                  <input class="ss-input" :class="{ 'input-error': errors.street }"
                    v-model="form.street" placeholder="Số nhà, tên đường..."
                    maxlength="500"
                    @blur="validateField('street')" @input="errors.street = ''" />
                  <div class="field-bottom">
                    <p v-if="errors.street" class="field-error">{{ errors.street }}</p>
                    <span v-else-if="(form.street?.length || 0) > 0" class="char-count"
                      :class="{ warn: (form.street?.length || 0) > 450 }">
                      {{ form.street?.length || 0 }}/500
                    </span>
                  </div>
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
                    <input class="ss-input" :class="{ 'input-error': errors.matKhauCu }"
                      :type="showOld ? 'text' : 'password'"
                      v-model="passForm.matKhauCu" placeholder="Nhập mật khẩu hiện tại"
                      autocomplete="current-password"
                      @blur="validateField('matKhauCu')" @input="errors.matKhauCu = ''" />
                    <button type="button" class="toggle-pass" @click="showOld = !showOld">
                      <i :class="showOld ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                    </button>
                  </div>
                  <p v-if="errors.matKhauCu" class="field-error">{{ errors.matKhauCu }}</p>
                </div>

                <div class="ss-field">
                  <label class="ss-label">Mật khẩu mới <span class="req">*</span></label>
                  <div class="pass-wrap">
                    <input class="ss-input" :class="{ 'input-error': errors.matKhauMoi }"
                      :type="showNew ? 'text' : 'password'"
                      v-model="passForm.matKhauMoi" placeholder="Ít nhất 6 ký tự"
                      autocomplete="new-password"
                      @blur="validateField('matKhauMoi')"
                      @input="errors.matKhauMoi = ''; if (passForm.xacNhan) validateField('xacNhan')" />
                    <button type="button" class="toggle-pass" @click="showNew = !showNew">
                      <i :class="showNew ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                    </button>
                  </div>
                  <p v-if="errors.matKhauMoi" class="field-error">{{ errors.matKhauMoi }}</p>
                </div>

                <div class="ss-field">
                  <label class="ss-label">Xác nhận mật khẩu mới <span class="req">*</span></label>
                  <div class="pass-wrap">
                    <input class="ss-input" :class="{ 'input-error': errors.xacNhan }"
                      :type="showCfm ? 'text' : 'password'"
                      v-model="passForm.xacNhan" placeholder="Nhập lại mật khẩu mới"
                      autocomplete="new-password"
                      @blur="validateField('xacNhan')" @input="errors.xacNhan = ''" />
                    <button type="button" class="toggle-pass" @click="showCfm = !showCfm">
                      <i :class="showCfm ? 'bi bi-eye-slash' : 'bi bi-eye'"></i>
                    </button>
                  </div>
                  <p v-if="errors.xacNhan" class="field-error">{{ errors.xacNhan }}</p>
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

.spin { display: inline-block; animation: spin .8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.req { color: #e53e3e; }

/* ── Validate inline ───────────────────────────────────── */
.field-error {
  margin: 4px 0 0;
  font-size: 12px; color: #c62828;
  display: flex; align-items: center; gap: 4px;
}
.field-error::before {
  content: '\F332';
  font-family: 'Bootstrap Icons';
  font-size: 11px;
}

.input-error {
  border-color: #e53e3e !important;
  background: #fff8f8 !important;
}

/* ── Field bottom (lỗi + counter) ──────────────────────── */
.field-bottom {
  display: flex; justify-content: space-between;
  align-items: flex-start; min-height: 18px;
  margin-top: 4px;
}

.char-count {
  font-size: 11px; color: var(--ss-muted);
  margin-left: auto;
}
.char-count.warn { color: #d97706; font-weight: 600; }

.ss-field-error {
  margin: 4px 0 0;
  font-size: 12px;
  color: #c62828;
}
</style>
