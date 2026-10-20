<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { storefrontService } from '../services/storefrontService'
import { commerceError } from '../services/commerceService'
import { priceError } from '../utils/query'
const router = useRouter(), options = ref(null), loading = ref(true), error = ref(''), step = ref(0)
const answers = ref({ categoryId: '', styleId: '', colorId: '', sizeId: '', minPrice: '', maxPrice: '', brandId: '' })
const steps = [
  { key: 'categoryId', source: 'categories', title: 'Bạn đang tìm loại giày nào?', description: 'Chọn từ những dòng giày đang có trong bộ sưu tập.' },
  { key: 'styleId', source: 'styles', title: 'Kiểu dáng nào phù hợp với bạn?', description: 'Tìm một thiết kế đồng hành với phong cách của bạn.' },
  { key: 'colorId', source: 'colors', title: 'Bạn thích màu sắc nào?', description: 'Chỉ gợi ý những màu thực sự có trong danh mục.' },
  { key: 'sizeId', source: 'sizes', title: 'Cỡ giày bạn thường mang?', description: 'Giúp thu hẹp những lựa chọn có biến thể phù hợp.' },
  { key: 'budget', title: 'Khoảng giá bạn mong muốn?', description: 'Kết quả sử dụng giá bán sau campaign đang có hiệu lực.' },
  { key: 'brandId', source: 'brands', title: 'Thương hiệu yêu thích của bạn?', description: 'Bạn có thể bỏ qua để khám phá tất cả thương hiệu.' }
]
const current = computed(() => steps[step.value]), choices = computed(() => options.value?.[current.value.source] || [])
function next() {
  error.value = ''
  if (current.value.key === 'budget') { error.value = priceError(answers.value.minPrice, answers.value.maxPrice); if (error.value) return }
  if (step.value < steps.length - 1) step.value++
  else router.push({ path: '/san-pham', query: Object.fromEntries(Object.entries(answers.value).filter(([,v]) => v !== '' && v != null)) })
}
async function load() { loading.value = true; error.value = ''; try { options.value = await storefrontService.filters() } catch (e) { error.value = commerceError(e) } finally { loading.value = false } }
onMounted(load)
</script>
<template><div class="container shop-page recommendation-page"><header class="shop-heading"><span class="eyebrow">FIND YOUR NEXT PAIR</span><h1>Gợi ý giày cho bạn</h1><p>Vài lựa chọn nhỏ. Một đôi giày phù hợp hơn.</p></header><p v-if="loading" class="shop-card" role="status">Đang tải bộ sưu tập…</p><template v-else-if="options"><div class="recommendation-progress"><span>Bước {{ step + 1 }} / {{ steps.length }}</span><span>{{ Math.round((step + 1) / steps.length * 100) }}%</span><progress :value="step + 1" :max="steps.length" aria-label="Tiến trình gợi ý giày"></progress></div><form @submit.prevent="next"><section class="shop-card recommendation-question"><h2>{{ current.title }}</h2><p class="shop-muted">{{ current.description }}</p><div v-if="current.key === 'budget'" class="shop-form-grid"><label>Giá từ (VNĐ)<input v-model="answers.minPrice" inputmode="numeric" type="number" min="0" step="1" placeholder="Không giới hạn"></label><label>Giá đến (VNĐ)<input v-model="answers.maxPrice" inputmode="numeric" type="number" min="0" step="1" placeholder="Không giới hạn"></label></div><div v-else class="recommendation-choices"><label :class="{ selected: answers[current.key] === '' }"><input v-model="answers[current.key]" type="radio" :name="current.key" value="">Không ưu tiên</label><label v-for="option in choices" :key="option.id" :class="{ selected: answers[current.key] === option.id }"><input v-model="answers[current.key]" type="radio" :name="current.key" :value="option.id">{{ option.name }} <small v-if="option.productCount">({{ option.productCount }})</small></label></div><p v-if="current.source && !choices.length" class="shop-muted">Danh mục này hiện chưa có lựa chọn. Bạn có thể tiếp tục.</p></section><p v-if="error" class="shop-alert" role="alert">{{ error }}</p><div class="recommendation-actions"><button v-if="step" type="button" class="button button-outline" @click="step--; error = ''">← Quay lại</button><button class="button button-primary">{{ step === steps.length - 1 ? 'Xem giày phù hợp' : 'Tiếp tục' }} →</button></div></form></template><section v-else class="shop-card shop-empty"><p role="alert">{{ error }}</p><button type="button" class="button button-primary" @click="load">Thử lại</button></section></div></template>
