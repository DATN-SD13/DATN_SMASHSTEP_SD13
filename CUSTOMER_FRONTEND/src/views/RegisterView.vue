<script setup>
import { nameError, emailError, phoneError, passwordError } from '../utils/accountValidation'
import { ref, computed, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import api from '../services/api'
import { useToast } from '../composables/useToast'
import BaseIcon from '../components/BaseIcon.vue'
const router=useRouter(), authStore=useAuthStore(), toast=useToast()
const name=ref(''), email=ref(''), phone=ref(''), password=ref(''), confirm=ref('')
const showPass=ref(false), showCfm=ref(false), loading=ref(false), error=ref(''), emailTouched=ref(false)
const emailFormatIssue=computed(()=>emailTouched.value?emailError(email.value):'')
let disposed=false
onBeforeUnmount(()=>{disposed=true})
function validate(){
  return nameError(name.value)||emailError(email.value)||phoneError(phone.value,false)||passwordError(password.value)
    ||(password.value!==confirm.value?'Mật khẩu xác nhận không khớp.':'')
}
async function handleRegister(){
  if(loading.value)return
  emailTouched.value=true;error.value=validate()
  if(error.value){toast.error(error.value);return}
  loading.value=true
  try{
    const data=await api.post('/auth/dang-ky',{name:name.value.trim(),email:email.value.trim().toLowerCase(),
      phone:phone.value.trim()||undefined,password:password.value})
    if(disposed)return
    const payload=data?.accessToken?data:data?.data
    authStore.token=payload.accessToken
    authStore.user={id:payload.id,code:payload.code,name:payload.name,email:payload.email,role:payload.role,userType:payload.userType,avatar:payload.avatar}
    localStorage.setItem('ss_token',payload.accessToken);localStorage.setItem('ss_user',JSON.stringify(authStore.user))
    toast.success('Tạo tài khoản thành công. Chào mừng bạn đến với SmashStep!');router.push('/')
  }catch(e){if(!disposed){error.value=e.response?.data?.message||e.message||'Chưa tạo được tài khoản. Vui lòng thử lại.';toast.error(error.value)}}
  finally{if(!disposed)loading.value=false}
}
</script>

<template>
  <section class="register-page">
    <div class="register-layout">
      <aside class="register-intro">
        <RouterLink to="/" aria-label="Về trang chủ SmashStep" class="register-brand"><img src="../assets/logo3D.png" alt="SmashStep" /><span>SMASHSTEP<small>STEP INTO YOUR STYLE</small></span></RouterLink>
        <div class="intro-copy"><span class="intro-eyebrow">CHÀO MỪNG BẠN</span><h1>Một tài khoản.<br />Mọi bước chân.</h1><p>Tìm đôi giày yêu thích, đặt hàng và theo dõi hành trình giao hàng của bạn.</p></div>
        <ul class="register-benefits"><li><BaseIcon name="shield" /><div><strong>Tài khoản của riêng bạn</strong><span>Lưu giỏ hàng và thông tin mua sắm thuận tiện.</span></div></li><li><BaseIcon name="mail" /><div><strong>Không bỏ lỡ đơn hàng</strong><span>Tùy chọn nhận email khi đặt hàng và đổi trạng thái.</span></div></li><li><BaseIcon name="pin" /><div><strong>Mua sắm thuận tiện</strong><span>Lưu địa chỉ và theo dõi đơn hàng mọi lúc.</span></div></li></ul>
        <p class="intro-footer">Đã có tài khoản? <RouterLink to="/dang-nhap">Đăng nhập →</RouterLink></p>
      </aside>
      <div class="register-card">
        <header class="register-heading"><span class="form-eyebrow">TÀI KHOẢN KHÁCH HÀNG</span><h2>Tạo tài khoản</h2><p>Điền thông tin bên dưới để bắt đầu mua sắm.</p></header>
        <form @submit.prevent="handleRegister" novalidate>
          <div class="register-fields">
            <div class="field"><label for="register-name">Họ và tên <span class="req">*</span></label><input id="register-name" v-model="name" maxlength="255" autocomplete="name" placeholder="Nguyễn Văn An" :disabled="loading" required /></div>
            <div class="field"><label for="register-phone">Số điện thoại <span class="optional">(tuỳ chọn)</span></label><input id="register-phone" v-model="phone" type="tel" maxlength="10" inputmode="tel" autocomplete="tel" placeholder="09xxxxxxxx" :disabled="loading" /></div>
          </div>
          <div class="field"><label for="register-email">Email <span class="req">*</span></label><input id="register-email" v-model="email" type="email" maxlength="254" autocomplete="email" placeholder="ten@gmail.com" :disabled="loading" :aria-invalid="!!emailFormatIssue" aria-describedby="register-email-help" @blur="emailTouched = true" required /><p v-if="emailFormatIssue" id="register-email-help" class="register-error" role="alert">{{ emailFormatIssue }}</p><p v-else id="register-email-help" class="email-hint">Dùng email của bạn. Bạn có thể chọn nhận thông báo email khi đặt hàng.</p></div>
          <div class="register-fields">
            <div class="field"><label for="register-password">Mật khẩu <span class="req">*</span></label><div class="pass-wrap"><input id="register-password" v-model="password" :type="showPass ? 'text' : 'password'" maxlength="72" autocomplete="new-password" placeholder="Ít nhất 6 ký tự, gồm chữ và số" :disabled="loading" required /><button class="toggle-pass" type="button" :aria-label="showPass ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'" @click="showPass = !showPass"><i :class="showPass ? 'bi bi-eye-slash' : 'bi bi-eye'"></i></button></div></div>
            <div class="field"><label for="register-confirm">Xác nhận mật khẩu <span class="req">*</span></label><div class="pass-wrap"><input id="register-confirm" v-model="confirm" :type="showCfm ? 'text' : 'password'" maxlength="72" autocomplete="new-password" placeholder="Nhập lại mật khẩu" :disabled="loading" required /><button class="toggle-pass" type="button" :aria-label="showCfm ? 'Ẩn mật khẩu xác nhận' : 'Hiện mật khẩu xác nhận'" @click="showCfm = !showCfm"><i :class="showCfm ? 'bi bi-eye-slash' : 'bi bi-eye'"></i></button></div></div>
          </div>
          <p v-if="error" class="register-error" role="alert">{{ error }}</p>
          <button class="register-submit" type="submit" :disabled="loading">{{ loading ? 'Đang tạo tài khoản…' : 'Tạo tài khoản' }}<BaseIcon v-if="!loading" name="arrow" /></button>
          <p class="submit-help">Bạn có thể cập nhật hồ sơ và địa chỉ trong mục Tài khoản.</p>
        </form>
        <p class="login-link">Đã có tài khoản? <RouterLink to="/dang-nhap">Đăng nhập ngay</RouterLink></p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.register-page{background:#f3f7fc;padding:40px 24px 56px;min-height:calc(100vh - 160px);color:#17324f}
.register-layout{display:grid;grid-template-columns:340px minmax(0,1fr);max-width:1120px;margin:auto;background:#fff;border:1px solid #dce6f0;border-radius:22px;overflow:hidden;box-shadow:0 15px 45px #1537550b}
.register-intro{display:flex;flex-direction:column;padding:34px 30px;background:radial-gradient(ellipse at top right,#19648a88,transparent 65%),#0b2540;color:#fff}
.register-brand{display:flex;align-items:center;gap:12px;color:inherit;text-decoration:none;font-size:19px;font-weight:800;letter-spacing:1px}
.register-brand img{width:58px;height:58px;object-fit:contain}.register-brand small{display:block;font-size:8px;letter-spacing:2px;margin-top:7px;color:#87cce6}
.intro-copy{margin-top:56px}.intro-eyebrow{font-size:10px;letter-spacing:2px;color:#79d6ee;font-weight:700}.intro-copy h1{font-size:33px;line-height:1.25;letter-spacing:-.8px;margin:15px 0}.intro-copy p{font-size:13px;line-height:1.85;color:#b6cbdc;margin:0}
.register-benefits{list-style:none;margin:37px 0 0;padding:0;display:grid;gap:25px}.register-benefits li{display:flex;gap:13px;align-items:flex-start}.register-benefits .icon{flex-shrink:0;width:22px;height:22px;color:#6ed3ee}.register-benefits strong{display:block;font-size:13px;margin-bottom:6px}.register-benefits span{font-size:12px;line-height:1.7;color:#b6cbdc;display:block}
.intro-footer{margin:auto 0 0;padding-top:50px;font-size:12px;color:#b6cbdc}.intro-footer a{color:white;font-weight:700;text-decoration:none;margin-left:4px}
.register-card{padding:32px 38px}.register-heading{margin-bottom:24px}.form-eyebrow{font-size:10px;letter-spacing:1.8px;color:#2680a7;font-weight:800}.register-heading h2{font-size:28px;letter-spacing:-.6px;margin:9px 0}.register-heading p{font-size:13px;color:#6d7f91;margin:0;line-height:1.6}
.register-fields{display:grid;grid-template-columns:1fr 1fr;gap:16px}.field{min-width:0;margin:0 0 18px}.field label,.email-label{display:block;font-size:12px;font-weight:600;margin-bottom:7px}.req{color:#cf4852}.optional{font-size:10px;color:#8493a3;font-weight:400}
.field input,.email-input-row input,.code-input-row input{width:100%;height:44px;min-width:0;padding:0 12px;box-sizing:border-box;border:1px solid #d8e3ef;border-radius:8px;outline:none;background:#fff;color:#16334f;font:inherit;font-size:12px}.field input:focus,.email-input-row input:focus,.code-input-row input:focus{border-color:#258dbb;box-shadow:0 0 0 3px #218dbb12}.field input:disabled,.email-input-row input:disabled{background:#f7f9fc}
.email-hint{font-size:11px;line-height:1.65;color:#7a8b9d;margin:9px 0 0}
.pass-wrap{position:relative}.pass-wrap input{padding-right:40px}.toggle-pass{position:absolute;right:10px;top:50%;transform:translateY(-50%);border:0;background:transparent;padding:4px;color:#7c8fa1;cursor:pointer}
.register-submit{display:flex;align-items:center;justify-content:center;gap:12px;width:100%;height:46px;border:0;border-radius:9px;background:#176eaa;color:#fff;font-size:13px;font-weight:700;cursor:pointer}.register-submit .icon{width:17px;height:17px}.register-submit:disabled{background:#c6d7e5;color:#f9fcff;cursor:not-allowed}.register-submit:hover:not(:disabled){background:#125b8e}
.submit-help{font-size:10px;text-align:center;line-height:1.6;color:#8292a2;margin:9px 0 0}.register-error{background:#fff0f1;color:#b82f42;border-radius:8px;padding:12px;font-size:12px;line-height:1.6;margin:0 0 14px}.login-link{text-align:center;border-top:1px solid #e8eef5;padding-top:19px;margin:23px 0 0;font-size:12px;color:#7b8da0}.login-link a{color:#176eaa;text-decoration:none;font-weight:700;margin-left:4px}
@media(max-width:950px){.register-page{padding:24px 16px 40px}.register-layout{grid-template-columns:260px minmax(0,1fr)}.register-intro{padding:28px 23px}.intro-copy h1{font-size:28px}.register-card{padding:28px 24px}.register-fields{grid-template-columns:1fr;gap:0}.verification-badge{font-size:8px}.email-card-heading p{display:none}}
@media(max-width:700px){.register-layout{display:block;max-width:540px;border-radius:16px}.register-intro{padding:22px 24px}.register-brand img{width:44px;height:44px}.register-brand{font-size:16px}.register-brand small{font-size:7px}.intro-copy{margin-top:19px}.intro-copy h1{font-size:25px;margin:7px 0}.intro-copy h1 br{display:none}.intro-copy p,.register-benefits,.intro-footer,.intro-eyebrow{display:none}.register-card{padding:24px 20px}.register-heading h2{font-size:25px}.register-fields{grid-template-columns:1fr;gap:0}.email-card{padding:14px}.register-steps{gap:16px}.send-code,.verify-code{padding:0 10px;font-size:10px}.code-input-row input{letter-spacing:5px}.verification-badge{font-size:8px}}
</style>
