<script setup>
import { ref, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import ProductShell from '../components/ProductShell.vue'
import StatusBadge from '../components/StatusBadge.vue'
import VariantTable from '../components/VariantTable.vue'
import VariantCreate from '../components/VariantCreate.vue'
import VariantEditModal from '../components/VariantEditModal.vue'
import ProductImages from '../components/ProductImages.vue'
import { productService, variantService, errorMessage, priceRange, dateTime } from '../services/productService'
import { productAttributeService } from '../services/productAttributeService'
const route = useRoute()
const detail = ref(null), options = ref({}), error = ref(''), success = ref(''), busy = ref(false), editing = ref(null)
const product = computed(() => detail.value?.product)
let version = 0
async function load(initial = false) {
  const request = ++version
  if (initial) { detail.value = null; editing.value = null; success.value = route.query.created ? 'Đã tạo sản phẩm. Bạn có thể thêm biến thể và ảnh bên dưới.' : '' }
  busy.value = true; error.value = ''
  try {
    const [data, attributes] = await Promise.all([productService.get(route.params.id), productAttributeService.options()])
    if (request !== version) return
    detail.value = data; options.value = attributes
  } catch (e) { if (request === version) error.value = errorMessage(e) }
  finally { if (request === version) busy.value = false }
}
async function productStatus() {
  if (busy.value || !window.confirm('Đổi trạng thái sản phẩm này?')) return
  busy.value = true; error.value = ''
  try { await productService.status(product.value.id, product.value.trangThai === 1 ? 0 : 1); success.value = 'Đã cập nhật trạng thái sản phẩm.'; await load() }
  catch (e) { error.value = errorMessage(e) } finally { busy.value = false }
}
async function variantStatus(v) {
  if (busy.value || !window.confirm('Đổi trạng thái biến thể ' + v.maChiTietSanPham + '?')) return
  busy.value = true; error.value = ''
  try { await variantService.status(v.id, v.trangThai === 1 ? 0 : 1); success.value = 'Đã cập nhật trạng thái biến thể.'; await load() }
  catch (e) { error.value = errorMessage(e) } finally { busy.value = false }
}
function variantSaved() { editing.value = null; success.value = 'Đã lưu biến thể.'; load() }
watch(() => route.params.id, () => load(true), { immediate: true })
</script>
<template>
  <ProductShell title="Chi tiết sản phẩm" :description="product?.tenSanPham" :error="error" :success="success">
    <template #actions><RouterLink to="/san-pham" class="p-btn"><i class="bi bi-arrow-left"></i> Danh sách</RouterLink><RouterLink v-if="product" :to="'/san-pham/' + product.id + '/sua'" class="p-btn primary"><i class="bi bi-pencil"></i> Sửa sản phẩm</RouterLink></template>
    <template v-if="detail">
      <div class="p-summary"><span>Tổng biến thể: <strong>{{ product.tongSoBienThe }}</strong></span><span>Tổng tồn kho: <strong>{{ product.tongSoLuong }}</strong></span><span>Giá bán: <strong>{{ priceRange(product) }}</strong></span></div>
      <section class="p-card"><h2><i class="bi bi-box-seam"></i> Thông tin sản phẩm</h2><dl class="p-info"><div><dt>Mã sản phẩm</dt><dd>{{ product.maSanPham }}</dd></div><div><dt>Tên sản phẩm</dt><dd>{{ product.tenSanPham }}</dd></div><div><dt>Trạng thái</dt><dd><StatusBadge :status="product.trangThai" /> <button class="p-btn" :disabled="busy" @click="productStatus"><i class="bi bi-power"></i> Đổi trạng thái</button></dd></div><div><dt>Danh mục</dt><dd>{{ product.tenDanhMuc || '—' }}</dd></div><div><dt>Thương hiệu</dt><dd>{{ product.tenThuongHieu || '—' }}</dd></div><div><dt>Chất liệu</dt><dd>{{ product.tenChatLieu || '—' }}</dd></div><div><dt>Kiểu dáng</dt><dd>{{ product.tenKieuDang || '—' }}</dd></div><div><dt>Cổ giày</dt><dd>{{ product.tenCoGiay || '—' }}</dd></div><div><dt>Xuất xứ</dt><dd>{{ product.tenXuatXu || '—' }}</dd></div><div><dt>Ngày tạo</dt><dd>{{ dateTime(product.ngayTao) }}</dd></div><div><dt>Cập nhật gần nhất</dt><dd>{{ dateTime(product.ngayCapNhat) }}</dd></div></dl><h2>Mô tả chi tiết</h2><p class="p-description">{{ product.moTaChiTiet || 'Chưa có mô tả.' }}</p></section>
      <section class="p-card"><h2><i class="bi bi-layers"></i> Biến thể của sản phẩm ({{ detail.variants.length }})</h2><VariantTable :rows="detail.variants" :busy="busy" @edit="editing = $event" @status="variantStatus" /></section>
      <VariantCreate :key="product.id" :product="product" :existing="detail.variants" :options="options" @created="success = 'Đã tạo biến thể.'; load()" />
      <ProductImages :key="'images-' + product.id" :product-id="product.id" :images="detail.images" @changed="load()" />
      <VariantEditModal v-if="editing" :key="editing.id" :variant="editing" @close="editing = null" @saved="variantSaved" />
    </template>
    <div v-else class="p-card p-empty">{{ error ? 'Không tải được sản phẩm.' : 'Đang tải...' }} <button v-if="error" class="p-btn" @click="load(true)">Thử lại</button></div>
  </ProductShell>
</template>

