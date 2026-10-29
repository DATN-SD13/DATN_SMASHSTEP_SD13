<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import api from '../services/api'
import RatingStars from './RatingStars.vue'
const props = defineProps({ productId: { type: [Number, String], required: true } })
const emit = defineEmits(['updated'])
const auth = useAuthStore(), route = useRoute()
const result=ref(null), mine=ref(null), page=ref(0), filter=ref(null), stars=ref(0), comment=ref('')
const loading=ref(false), saving=ref(false), error=ref(''), formError=ref(''), success=ref('')
let controller, version=0
const summary=computed(() => result.value?.summary || { average:0,count:0 })
const labels=['','Rất không hài lòng','Không hài lòng','Bình thường','Hài lòng','Rất hài lòng']
async function load() {
  controller?.abort(); controller=new AbortController(); const signal=controller.signal, current=++version
  loading.value=true; error.value=''; mine.value=null
  try {
    const [list, own]=await Promise.all([
      api.get(`/store/products/${props.productId}/reviews`, { params:{page:page.value,stars:filter.value},signal }),
      auth.isAuthenticated && auth.isCustomer ? api.get(`/customer/products/${props.productId}/review`,{signal}) : Promise.resolve(null)
    ])
    if(current!==version)return
    result.value=list; mine.value=own
    stars.value=own?.review?.stars || 0; comment.value=own?.review?.comment || ''
    emit('updated',list.summary)
  } catch(e) { if(current===version && !signal.aborted)error.value=e.response?.data?.message || 'Chưa tải được đánh giá. Vui lòng thử lại.' }
  finally { if(current===version)loading.value=false }
}
watch(() => [props.productId,auth.token], () => { page.value=0; filter.value=null; result.value=null; formError.value=''; success.value=''; load() },{immediate:true})
function selectFilter(value) { if(loading.value || saving.value)return;filter.value=value;page.value=0;load() }
function changePage(value) { page.value=value;load() }
async function submit() {
  if(saving.value)return
  formError.value='';success.value=''
  const text=comment.value.trim()
  if(!Number.isInteger(stars.value) || stars.value<1 || stars.value>5){formError.value='Vui lòng chọn từ 1 đến 5 sao.';return}
  if(!text || text.length>2000){formError.value='Vui lòng nhập bình luận từ 1 đến 2.000 ký tự.';return}
  saving.value=true; const productId=props.productId, token=auth.token
  try {
    await api.put(`/customer/products/${productId}/review`,{stars:stars.value,comment:text})
    if(productId!==props.productId || token!==auth.token)return
    page.value=0;filter.value=null;await load();success.value='Đã lưu đánh giá của bạn. Cảm ơn bạn đã chia sẻ!'
  } catch(e){if(productId===props.productId && token===auth.token)formError.value=e.response?.data?.message || 'Chưa lưu được đánh giá. Vui lòng thử lại.'}
  finally {saving.value=false}
}
function date(value){return new Date(value).toLocaleString('vi-VN',{day:'2-digit',month:'2-digit',year:'numeric',hour:'2-digit',minute:'2-digit'})}
onBeforeUnmount(() => {version++;controller?.abort()})
</script>
<template>
  <section id="product-reviews" class="reviews-panel" aria-labelledby="review-heading">
    <div class="review-heading"><div><span class="eyebrow">TRẢI NGHIỆM TỪ KHÁCH HÀNG</span><h2 id="review-heading">Đánh giá sản phẩm</h2></div><span>{{ summary.count }} đánh giá</span></div>
    <p v-if="error" class="shop-error" role="alert">{{ error }} <button class="review-link" @click="load">Thử lại</button></p>
    <div class="review-overview" :aria-busy="loading">
      <div class="review-score"><strong>{{ summary.count ? summary.average.toFixed(1) : '—' }}<small> / 5</small></strong><RatingStars :value="summary.average"/><span>{{ summary.count ? 'Điểm trung bình từ khách hàng' : 'Chưa có đánh giá' }}</span></div>
      <div class="review-filters" aria-label="Lọc đánh giá"><button :class="{active:filter===null}" :disabled="saving || loading" @click="selectFilter(null)">Tất cả ({{summary.count}})</button><button v-for="n in [5,4,3,2,1]" :key="n" :class="{active:filter===n}" :disabled="saving || loading" @click="selectFilter(n)">{{n}} sao ({{result?.distribution?.[n] || 0}})</button></div>
    </div>
    <p v-if="!auth.isAuthenticated" class="review-notice"><RouterLink :to="{path:'/dang-nhap',query:{redirect:route.fullPath}}">Đăng nhập</RouterLink> để đánh giá sản phẩm bạn đã mua.</p>
    <p v-else-if="mine && !mine.eligible" class="review-notice">Bạn có thể đánh giá sau khi đơn chứa sản phẩm này đã giao hoặc hoàn thành.</p>
    <form v-if="mine?.eligible" class="review-form" @submit.prevent="submit">
      <h3>{{mine.review ? 'Chỉnh sửa đánh giá của bạn' : 'Chia sẻ đánh giá của bạn'}}</h3>
      <fieldset :disabled="saving || loading"><legend>Mức độ hài lòng <span aria-hidden="true">*</span></legend><div class="star-picker" role="radiogroup" aria-label="Chọn số sao"><label v-for="n in 5" :key="n" :class="{chosen:n<=stars}"><input v-model="stars" type="radio" name="review-stars" :value="n" :aria-label="`${n} sao`" required/><span aria-hidden="true">★</span></label><span class="star-caption">{{labels[stars] || 'Chọn từ 1 đến 5 sao'}}</span></div><label for="review-comment">Bình luận <span aria-hidden="true">*</span></label><textarea id="review-comment" v-model="comment" rows="4" maxlength="2000" required placeholder="Chia sẻ cảm nhận về chất lượng, độ vừa chân và trải nghiệm sử dụng…"></textarea><small>{{comment.length}} / 2.000 ký tự · Mỗi tài khoản có một đánh giá cho sản phẩm.</small></fieldset>
      <p v-if="formError" class="shop-error" role="alert">{{formError}}</p><p v-if="success" class="shop-success" role="status">{{success}}</p><button class="button button-primary" :disabled="saving || loading" type="submit">{{saving ? 'Đang lưu…' : mine.review ? 'Cập nhật đánh giá' : 'Gửi đánh giá'}}</button>
    </form>
    <p v-if="loading" role="status" class="review-notice">Đang tải đánh giá…</p>
    <template v-else-if="result"><article v-for="item in result.items" :key="item.id" class="review-item"><div class="review-avatar" aria-hidden="true">{{item.customerName[0]}}</div><div class="review-content"><strong>{{item.customerName}}</strong><div><RatingStars :value="item.stars"/><span class="review-verified">✓ Đã mua hàng</span></div><time :datetime="item.createdAt">{{date(item.createdAt)}}<span v-if="item.createdAt!==item.updatedAt"> · Đã chỉnh sửa</span></time><p>{{item.comment}}</p></div></article><p v-if="!result.items.length" class="review-notice">{{filter ? 'Chưa có đánh giá '+filter+' sao.' : 'Chưa có đánh giá. Hãy chia sẻ trải nghiệm sau khi nhận hàng nhé!'}}</p><nav v-if="result.total>10" class="review-pagination" aria-label="Trang đánh giá"><button class="button button-outline" :disabled="page===0 || saving" @click="changePage(page-1)">← Trước</button><span>Trang {{page+1}} / {{Math.ceil(result.total/10)}}</span><button class="button button-outline" :disabled="(page+1)*10>=result.total || saving" @click="changePage(page+1)">Sau →</button></nav></template>
  </section>
</template>
<style scoped>
.reviews-panel{background:#fff;border:1px solid #e0e7ef;border-radius:20px;margin:32px 0 56px;padding:32px;scroll-margin-top:100px}.review-heading{display:flex;align-items:center;justify-content:space-between;gap:16px}.review-heading h2{margin:8px 0 24px}.review-heading>span,.review-score>span{color:#64748b;font-size:13px}.review-overview{display:flex;gap:32px;align-items:center;padding:24px;background:#f5f9ff;border:1px solid #dce9f8;border-radius:12px}.review-score{display:flex;flex-direction:column;gap:8px;min-width:190px}.review-score strong{font-size:36px;color:#1868bd}.review-score small{font-size:18px;font-weight:400}.review-filters{display:flex;flex-wrap:wrap;gap:8px}.review-filters button{background:#fff;padding:10px 16px;border:1px solid #dce3ed;border-radius:7px;cursor:pointer}.review-filters button.active{color:#1868bd;border-color:#1868bd;background:#eaf3ff}.review-notice{padding:20px 0;color:#64748b;font-size:14px}.review-notice a,.review-link{color:#1868bd;text-decoration:underline}.review-form{margin:24px 0;padding:24px;border:1px solid #dce3ed;border-radius:12px}.review-form h3{margin-top:0}.review-form fieldset{border:0;padding:0;margin:0}.review-form legend,.review-form label[for]{font-size:14px;font-weight:600}.star-picker{display:flex;align-items:center;gap:6px;margin:8px 0 18px;flex-wrap:wrap}.star-picker label{position:relative;color:#cbd5e1;cursor:pointer;font-size:34px;line-height:1.3}.star-picker label.chosen{color:#ed9b12}.star-picker input{position:absolute;opacity:0;width:100%;height:100%;cursor:pointer;inset:0;margin:0}.star-picker label:focus-within{outline:2px solid #1868bd;border-radius:4px}.star-caption{font-size:13px;color:#64748b;margin-left:12px}.review-form textarea{display:block;width:100%;box-sizing:border-box;margin:10px 0 8px;padding:14px;font:inherit;border:1px solid #cbd5e1;border-radius:8px;resize:vertical;min-height:110px}.review-form small{display:block;color:#64748b;margin-bottom:16px}.review-item{display:flex;gap:14px;padding:24px 0;border-bottom:1px solid #e8edf3}.review-avatar{display:grid;place-items:center;border-radius:50%;width:40px;height:40px;flex-shrink:0;background:#eaf3ff;color:#1868bd;font-weight:700}.review-content{min-width:0}.review-content>strong{display:block;margin-bottom:6px}.review-verified{font-size:12px;color:#14865a;margin-left:12px}.review-content time{display:block;font-size:12px;color:#8793a4;margin-top:7px}.review-content p{white-space:pre-wrap;overflow-wrap:anywhere;line-height:1.8;margin-bottom:0}.review-pagination{display:flex;align-items:center;justify-content:center;gap:16px;margin-top:24px}.review-pagination span{font-size:13px}@media(max-width:640px){.reviews-panel{padding:18px}.review-overview{flex-direction:column;align-items:flex-start;padding:18px;gap:18px}.review-heading{align-items:flex-start}.review-form{padding:16px}.review-verified{display:block;margin:4px 0}}
</style>
