<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import BaseIcon from '../components/BaseIcon.vue'
import AccountAddressForm from '../components/AccountAddressForm.vue'
import { accountService } from '../services/accountService'
import { useAuthStore } from '../stores/auth'
import { imageUrl } from '../utils/image'
import '../assets/styles/account.css'
import { nameError, phoneError, birthDateError, passwordError } from '../utils/accountValidation'

const auth = useAuthStore()
const profile = ref(null), loading = ref(true), loadError = ref(''), tab = ref('profile')
const notice = ref(''), actionError = ref(''), saving = ref(false), addressBusy = ref(false)
const editingAddress = ref(null), avatarFailed = ref(false), showPasswords = ref(false)
const form = reactive({ name: '', phone: '', gender: '', dob: '' })
const password = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const fieldErrors = reactive({})
const tabs = [
  { id: 'profile', label: 'Thông tin cá nhân', icon: 'user', description: 'Hồ sơ & thông tin tài khoản' },
  { id: 'addresses', label: 'Sổ địa chỉ', icon: 'pin', description: 'Địa chỉ nhận hàng của bạn' },
  { id: 'security', label: 'Đăng nhập & bảo mật', icon: 'shield', description: 'Quản lý mật khẩu' }
]
const addresses = computed(() => profile.value?.addresses || [])
const initials = computed(() => (profile.value?.name || auth.user?.name || 'KH').trim().split(/\s+/).slice(-2).map(x => x[0]).join('').toUpperCase())
const avatar = computed(() => imageUrl(profile.value?.image))
const normalized = data => ({ name: data?.name || '', phone: data?.phone || '', gender: data?.gender ?? '', dob: data?.dob || '' })
const isDirty = computed(() => JSON.stringify(form) !== JSON.stringify(normalized(profile.value)))
const completed = computed(() => {
  const p = profile.value
  return p ? [p.name, p.email, p.phone, p.dob, p.gender != null, addresses.value.length > 0].filter(Boolean).length : 0
})
const completion = computed(() => Math.round(completed.value / 6 * 100))
const dateText = (value, withTime = false) => {
  if (!value) return 'Chưa cập nhật'
  const date = new Date(value.length === 10 ? `${value}T00:00:00` : value)
  return Number.isNaN(date.getTime()) ? 'Chưa cập nhật' : new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', ...(withTime ? { hour: '2-digit', minute: '2-digit' } : {}) }).format(date)
}
const today = new Date()
const maxDob = `${today.getFullYear() - 15}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
const errorMessage = e => e.response?.data?.message || e.message || 'Thao tác chưa hoàn tất. Vui lòng thử lại.'

function clearMessages() { notice.value = ''; actionError.value = ''; Object.keys(fieldErrors).forEach(k => delete fieldErrors[k]) }
function chooseTab(id) { tab.value = id; editingAddress.value = null; showPasswords.value = false; clearMessages(); Object.keys(password).forEach(k => password[k] = '') }
function applyProfile(data, resetForm = true) {
  profile.value = data
  if (resetForm) Object.assign(form, normalized(data))
  auth.updateProfile(data)
  avatarFailed.value = false
}
async function loadProfile() {
  loading.value = true; loadError.value = ''
  try { applyProfile(await accountService.profile()) }
  catch (e) { loadError.value = errorMessage(e) }
  finally { loading.value = false }
}
function restoreForm() { Object.assign(form, normalized(profile.value)); clearMessages() }
async function saveProfile() {
  if (saving.value) return
  clearMessages()
  const invalidName = nameError(form.name), invalidPhone = phoneError(form.phone, false), invalidDob = birthDateError(form.dob, 15)
  if (invalidName) fieldErrors.name = invalidName
  if (invalidPhone) fieldErrors.phone = invalidPhone
  if (invalidDob) fieldErrors.dob = invalidDob
  if (form.phone && !/^0\d{9}$/.test(form.phone.trim())) fieldErrors.phone = 'Số điện thoại gồm 10 số, bắt đầu bằng 0.'
  if (form.dob && form.dob > maxDob) fieldErrors.dob = 'Bạn phải đủ 15 tuổi trở lên.'
  if (Object.keys(fieldErrors).length) return
  saving.value = true
  try {
    applyProfile(await accountService.update({ name: form.name.trim(), phone: form.phone.trim(), gender: form.gender === '' ? null : Number(form.gender), dob: form.dob || null }))
    notice.value = 'Đã lưu thông tin cá nhân của bạn.'
  } catch (e) { actionError.value = errorMessage(e) }
  finally { saving.value = false }
}
async function refreshAddresses(message) {
  const keepForm = isDirty.value
  applyProfile(await accountService.profile(), !keepForm)
  notice.value = message
}
async function addressSaved() {
  editingAddress.value = null; clearMessages(); addressBusy.value = true
  try { await refreshAddresses('Đã lưu địa chỉ giao hàng.') }
  catch (e) { actionError.value = `Địa chỉ đã lưu nhưng chưa tải lại được danh sách. ${errorMessage(e)}` }
  finally { addressBusy.value = false }
}
async function setDefault(address) {
  if (addressBusy.value) return
  clearMessages(); addressBusy.value = true
  try { await accountService.setDefaultAddress(address.id); await refreshAddresses('Đã cập nhật địa chỉ mặc định.') }
  catch (e) { actionError.value = errorMessage(e) }
  finally { addressBusy.value = false }
}
async function changePassword() {
  if (saving.value) return
  clearMessages()
  const invalidPassword = passwordError(password.newPassword)
  if (invalidPassword) fieldErrors.newPassword = invalidPassword
  if (!password.currentPassword) fieldErrors.currentPassword = 'Vui lòng nhập mật khẩu hiện tại.'
  if (password.newPassword.length < 6 || new TextEncoder().encode(password.newPassword).length > 72) fieldErrors.newPassword = 'Mật khẩu cần ít nhất 6 ký tự và không quá dài (tối đa 72 ký tự không dấu).'
  if (password.newPassword === password.currentPassword) fieldErrors.newPassword = 'Hãy chọn mật khẩu khác mật khẩu hiện tại.'
  if (password.confirmPassword !== password.newPassword) fieldErrors.confirmPassword = 'Mật khẩu xác nhận không khớp.'
  if (Object.keys(fieldErrors).length) return
  saving.value = true
  try {
    await accountService.changePassword({ ...password })
    Object.keys(password).forEach(k => password[k] = '')
    await auth.logout()
  } catch (e) { actionError.value = errorMessage(e) }
  finally { saving.value = false }
}
onMounted(loadProfile)
</script>

<template>
  <div class="account-page container">
    <nav class="breadcrumbs" aria-label="Đường dẫn"><RouterLink to="/">Trang chủ</RouterLink><span>/</span><span>Tài khoản của tôi</span></nav>
    <header class="account-page-heading"><div><span class="account-eyebrow">KHÔNG GIAN CỦA BẠN</span><h1>Tài khoản của tôi<span>.</span></h1><p>Quản lý thông tin để mỗi trải nghiệm mua sắm luôn trọn vẹn.</p></div><RouterLink to="/san-pham" class="account-shop-link">Tiếp tục mua sắm <BaseIcon name="arrow" /></RouterLink></header>

    <div v-if="loading" class="account-loading" role="status"><span class="account-spinner"></span> Đang tải thông tin tài khoản...</div>
    <div v-else-if="loadError" class="account-card account-load-error" role="alert"><BaseIcon name="user" /><h2>Chưa tải được thông tin tài khoản</h2><p>{{ loadError }}</p><button class="button button-primary" @click="loadProfile">Thử lại</button></div>

    <div v-else-if="profile" class="account-layout">
      <aside class="account-sidebar">
        <div class="account-card account-member-card">
          <div class="account-member-cover"><span>SMASHSTEP</span><span>MOVE YOUR WAY</span></div>
          <div class="account-avatar"><img v-if="avatar && !avatarFailed" :src="avatar" alt="Ảnh đại diện của bạn" @error="avatarFailed = true"><span v-else>{{ initials }}</span><span class="account-avatar-check"><BaseIcon name="check" /></span></div>
          <h2>{{ profile.name }}</h2><p class="account-member-code">{{ profile.code || 'Khách hàng SmashStep' }}</p>
          <span class="account-status"><span></span> {{ profile.statusLabel || 'Đang hoạt động' }}</span>
          <div class="account-member-since"><BaseIcon name="calendar" /> Tham gia {{ dateText(profile.createdAt) }}</div>
        </div>
        <nav class="account-card account-navigation" aria-label="Quản lý tài khoản"><button v-for="item in tabs" :key="item.id" type="button" :class="{ selected: tab === item.id }" :aria-current="tab === item.id ? 'page' : undefined" @click="chooseTab(item.id)"><BaseIcon :name="item.icon" /><span><strong>{{ item.label }}</strong><small>{{ item.description }}</small></span><span v-if="item.id === 'addresses'" class="account-nav-count">{{ addresses.length }}</span><BaseIcon v-else name="right" /></button></nav>
        <div class="account-completion"><div><strong>Hoàn thiện hồ sơ</strong><span>{{ completion }}%</span></div><progress :value="completion" max="100" aria-label="Mức độ hoàn thiện hồ sơ"></progress><p>{{ completion === 100 ? 'Hồ sơ của bạn đã đầy đủ. Cảm ơn bạn đồng hành cùng SmashStep!' : 'Bổ sung thông tin cá nhân và địa chỉ để cửa hàng phục vụ bạn tốt hơn.' }}</p></div>
      </aside>

      <div class="account-content" :aria-busy="saving || addressBusy">
        <p v-if="notice" class="account-alert account-alert--success" role="status"><BaseIcon name="check" />{{ notice }}</p>
        <p v-if="actionError" class="account-alert account-alert--error" role="alert">{{ actionError }}</p>

        <template v-if="tab === 'profile'">
          <section class="account-card account-panel">
            <div class="account-section-heading"><div class="account-section-title"><span class="account-section-icon"><BaseIcon name="user" /></span><div><h2>Thông tin cá nhân</h2><p>Thông tin giúp SmashStep hiểu bạn hơn.</p></div></div><span class="account-subtle-tag">Hồ sơ của bạn</span></div>
            <form @submit.prevent="saveProfile">
              <fieldset class="account-form-grid" :disabled="saving">
                <label class="account-form-wide" for="account-name">Họ và tên <span>*</span><input id="account-name" v-model="form.name" required maxlength="255" autocomplete="name" placeholder="Nhập họ và tên" :aria-invalid="!!fieldErrors.name" :aria-describedby="fieldErrors.name ? 'account-name-error' : undefined"><small v-if="fieldErrors.name" id="account-name-error" class="account-field-error">{{ fieldErrors.name }}</small></label>
                <label for="account-phone">Số điện thoại<input id="account-phone" v-model="form.phone" type="tel" maxlength="10" pattern="0[0-9]{9}" autocomplete="tel" placeholder="Nhập số điện thoại" :aria-invalid="!!fieldErrors.phone" :aria-describedby="fieldErrors.phone ? 'account-phone-error' : undefined"><small v-if="fieldErrors.phone" id="account-phone-error" class="account-field-error">{{ fieldErrors.phone }}</small></label>
                <label for="account-dob">Ngày sinh<input id="account-dob" v-model="form.dob" type="date" :max="maxDob" min="1900-01-01" autocomplete="bday" :aria-invalid="!!fieldErrors.dob" :aria-describedby="fieldErrors.dob ? 'account-dob-error' : undefined"><small v-if="fieldErrors.dob" id="account-dob-error" class="account-field-error">{{ fieldErrors.dob }}</small></label>
                <div class="account-form-wide"><span class="account-field-label" id="account-gender-label">Giới tính</span><div class="account-radio-group" role="group" aria-labelledby="account-gender-label"><label><input v-model="form.gender" type="radio" :value="1"> Nam</label><label><input v-model="form.gender" type="radio" :value="2"> Nữ</label><label><input v-model="form.gender" type="radio" :value="0"> Khác</label><label><input v-model="form.gender" type="radio" value=""> Chưa cập nhật</label></div></div>
              </fieldset>
              <div class="account-form-footer"><span><BaseIcon name="shield" /> Thông tin chỉ dành cho tài khoản của bạn.</span><div class="account-form-actions"><button class="button account-button-secondary" type="button" :disabled="saving || !isDirty" @click="restoreForm">Khôi phục</button><button class="button button-primary" type="submit" :disabled="saving || !isDirty"><BaseIcon name="check" />{{ saving ? 'Đang lưu...' : 'Lưu thay đổi' }}</button></div></div>
            </form>
          </section>
          <section class="account-card account-panel">
            <div class="account-section-heading"><div class="account-section-title"><span class="account-section-icon"><BaseIcon name="lock" /></span><div><h2>Thông tin tài khoản</h2><p>Thông tin đăng nhập và thời gian tham gia của bạn.</p></div></div></div>
            <dl class="account-details"><div><dt>Mã khách hàng</dt><dd>{{ profile.code || 'Chưa cập nhật' }}</dd></div><div><dt>Tên đăng nhập</dt><dd>{{ profile.username || 'Chưa cập nhật' }}</dd></div><div class="account-form-wide"><dt>Email đăng nhập</dt><dd><BaseIcon name="mail" />{{ profile.email || 'Chưa cập nhật' }}</dd></div><div><dt>Ngày tham gia</dt><dd>{{ dateText(profile.createdAt, true) }}</dd></div><div><dt>Cập nhật gần nhất</dt><dd>{{ dateText(profile.updatedAt, true) }}</dd></div></dl>
          </section>
          <section class="account-address-preview"><div><BaseIcon name="pin" /><div><h3>Địa chỉ nhận hàng mặc định</h3><p v-if="profile.defaultAddress"><strong>{{ profile.defaultAddress.receiverName }}</strong> · {{ profile.defaultAddress.receiverPhone }}<br>{{ profile.defaultAddress.fullAddress }}</p><p v-else>Bạn chưa có địa chỉ nhận hàng. Thêm địa chỉ để tiện mua sắm.</p></div></div><button type="button" class="account-text-button" @click="chooseTab('addresses')">{{ profile.defaultAddress ? 'Quản lý địa chỉ' : 'Thêm địa chỉ' }} <BaseIcon name="arrow" /></button></section>
        </template>

        <section v-else-if="tab === 'addresses'" class="account-card account-panel">
          <div class="account-section-heading"><div class="account-section-title"><span class="account-section-icon"><BaseIcon name="pin" /></span><div><h2>Sổ địa chỉ <span class="account-heading-count">{{ addresses.length }}</span></h2><p>Lưu địa chỉ để nhận hàng thuận tiện hơn.</p></div></div><button v-if="!editingAddress" class="button button-primary account-add-address" :disabled="addressBusy" @click="clearMessages(); editingAddress = {}">+ Thêm địa chỉ</button></div>
          <AccountAddressForm v-if="editingAddress" :key="editingAddress.id || 'new'" :address="editingAddress" :customer="profile" @saved="addressSaved" @cancel="editingAddress = null" />
          <div v-else-if="!addresses.length" class="account-empty"><span class="account-empty-icon"><BaseIcon name="pin" /></span><h3>Chưa có địa chỉ nào</h3><p>Thêm địa chỉ nhà hoặc văn phòng để SmashStep giao hàng đến đúng nơi bạn muốn.</p><button class="button button-primary" @click="editingAddress = {}">Thêm địa chỉ đầu tiên <BaseIcon name="arrow" /></button></div>
          <div v-else class="account-address-list"><article v-for="address in addresses" :key="address.id" class="account-address-card" :class="{ 'is-default': address.isDefault }"><div class="account-address-top"><span class="account-address-type"><BaseIcon :name="address.addressType === 2 ? 'office' : 'home'" />{{ address.addressTypeLabel || (address.addressType === 2 ? 'Văn phòng' : 'Nhà riêng') }}</span><span v-if="address.isDefault" class="account-default-badge"><BaseIcon name="check" />Mặc định</span></div><h3>{{ address.receiverName }}<span>{{ address.receiverPhone }}</span></h3><p>{{ address.fullAddress || [address.street, address.ward, address.district, address.province].filter(Boolean).join(', ') }}</p><div class="account-address-actions"><button class="account-text-button" :disabled="addressBusy" @click="clearMessages(); editingAddress = { ...address }"><BaseIcon name="edit" />Chỉnh sửa</button><button v-if="!address.isDefault" class="account-text-button" :disabled="addressBusy" @click="setDefault(address)">Đặt làm mặc định</button></div></article></div>
        </section>

        <template v-else-if="tab === 'security'">
          <section class="account-card account-panel">
            <div class="account-section-heading"><div class="account-section-title"><span class="account-section-icon"><BaseIcon name="shield" /></span><div><h2>Đăng nhập & bảo mật</h2><p>Chủ động bảo vệ tài khoản của bạn.</p></div></div></div>
            <div class="account-security-email"><BaseIcon name="mail" /><div><small>Email đăng nhập</small><strong>{{ profile.email }}</strong></div><span class="account-subtle-tag">Tài khoản khách hàng</span></div>
            <div class="account-password-heading"><h3>Đổi mật khẩu</h3><p>Sau khi thay đổi thành công, bạn sẽ đăng nhập lại bằng mật khẩu mới.</p></div>
            <form class="account-password-form" @submit.prevent="changePassword">
              <fieldset :disabled="saving" class="account-form-grid">
                <label v-for="(label, key) in { currentPassword: 'Mật khẩu hiện tại', newPassword: 'Mật khẩu mới', confirmPassword: 'Xác nhận mật khẩu mới' }" :key="key" class="account-form-wide" :for="`account-${key}`">{{ label }} <span>*</span><input :id="`account-${key}`" v-model="password[key]" :type="showPasswords ? 'text' : 'password'" required :minlength="key === 'currentPassword' ? 1 : 6" :maxlength="key === 'currentPassword' ? 100 : 72" :autocomplete="key === 'currentPassword' ? 'current-password' : 'new-password'" :placeholder="label" :aria-invalid="!!fieldErrors[key]" :aria-describedby="fieldErrors[key] ? `error-${key}` : undefined"><small v-if="fieldErrors[key]" :id="`error-${key}`" class="account-field-error">{{ fieldErrors[key] }}</small></label>
                <label class="account-checkbox account-form-wide"><input v-model="showPasswords" type="checkbox"> Hiển thị mật khẩu</label>
              </fieldset>
              <div class="account-form-actions"><button class="button button-primary" type="submit" :disabled="saving"><BaseIcon name="lock" />{{ saving ? 'Đang cập nhật...' : 'Cập nhật mật khẩu' }}</button></div>
            </form>
          </section>
          <div class="account-security-note"><BaseIcon name="shield" /><div><strong>Một vài lưu ý nhỏ cho tài khoản an toàn</strong><p>Dùng mật khẩu riêng cho SmashStep, kết hợp chữ, số và ký tự đặc biệt. Không chia sẻ mật khẩu với người khác.</p></div></div>
        </template>
      </div>
    </div>
  </div>
</template>
