<script setup>
import ProductPrice from '../components/ProductPrice.vue'
import { useCampaignPriceRefresh } from '../composables/useCampaignPriceRefresh'
import { ref, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import ProductShell from '../components/ProductShell.vue'
import StatusBadge from '../components/StatusBadge.vue'
import VariantTable from '../components/VariantTable.vue'
import VariantCreate from '../components/VariantCreate.vue'
import VariantEditModal from '../components/VariantEditModal.vue'
import ProductImages from '../components/ProductImages.vue'
import ConfirmModal from '../components/ConfirmModal.vue'
import { useConfirmation, statusConfirmation } from '../composables/useConfirmation'
import { productService, variantService, errorMessage, mergeCurrentPriceFields, dateTime } from '../services/productService'
import { productAttributeService } from '../services/productAttributeService'
const route = useRoute()
const detail = ref(null), options = ref({}), error = ref(''), success = ref(''), busy = ref(false), editing = ref(null)
const { confirmation, confirming, confirmError, askConfirmation, cancelConfirmation, confirmAction } = useConfirmation()
const product = computed(() => detail.value?.product)
let version = 0
async function load(initial = false) {
  const request = ++version
  if (initial) { detail.value = null; editing.value = null; success.value = '' }
  busy.value = true; error.value = ''
  try {
    const [data, attributes] = await Promise.all([productService.get(route.params.id), productAttributeService.options()])
    if (request !== version) return
    detail.value = data; options.value = attributes
  } catch (e) { if (request === version) error.value = errorMessage(e) }
  finally { if (request === version) busy.value = false }
}
function productStatus() {
  if (busy.value || confirming.value || !product.value) return
  const id = product.value.id, status = product.value.trangThai === 1 ? 0 : 1
  askConfirmation(statusConfirmation('sản phẩm', `${product.value.maSanPham} - ${product.value.tenSanPham}`, product.value.trangThai), async () => {
    error.value = ''; success.value = ''
    await productService.status(id, status)
    success.value = 'Thay đổi trạng thái sản phẩm thành công.'
    await load()
  })
}
function variantStatus(v) {
  if (busy.value || confirming.value) return
  const id = v.id, status = v.trangThai === 1 ? 0 : 1
  askConfirmation(statusConfirmation('biến thể', `${v.sku} - ${v.tenSanPham}`, v.trangThai), async () => {
    error.value = ''; success.value = ''
    await variantService.status(id, status)
    success.value = 'Thay đổi trạng thái biến thể thành công.'
    await load()
  })
}
function variantSaved() { editing.value = null; success.value = 'Cập nhật biến thể thành công.'; load() }
useCampaignPriceRefresh(async () => {
  if (busy.value || !detail.value) return
  const current = detail.value, id = route.params.id
  const fresh = await productService.get(id)
  if (current !== detail.value || id !== route.params.id) return
  mergeCurrentPriceFields([current.product], [fresh.product])
  mergeCurrentPriceFields(current.variants, fresh.variants)
})
watch(() => route.params.id, () => load(true), { immediate: true })
</script>
<template>
  <ProductShell title="Chi tiết sản phẩm" :description="product?.tenSanPham" :error="error" :success="success">
    <template #actions><RouterLink to="/san-pham" class="p-btn"><i class="bi bi-arrow-left"></i> Danh sách</RouterLink><RouterLink v-if="product" :to="'/san-pham/' + product.id + '/sua'" class="p-btn primary"><i class="bi bi-pencil"></i> Sửa sản phẩm</RouterLink></template>
    <template v-if="detail">
      <div class="p-summary"><span>Tổng biến thể: <strong>{{ product.tongSoBienThe }}</strong></span><span>Số màu: <strong>{{ product.soMau }}</strong></span><span>Số kích thước: <strong>{{ product.soKichThuoc }}</strong></span><span>Tổng tồn kho: <strong>{{ product.tongSoLuong }}</strong></span><span>Giá bán: <strong><ProductPrice :item="product" range /></strong></span></div>
      <section class="p-card"><h2><i class="bi bi-box-seam"></i> Thông tin sản phẩm</h2>
        <div class="p-form-grid p-detail-grid">
          <div class="p-detail-field"><span class="p-field-label">Mã sản phẩm</span><div class="p-readonly-control">{{ product.maSanPham }}</div></div>
          <div class="p-detail-field"><span class="p-field-label">Tên sản phẩm</span><div class="p-readonly-control">{{ product.tenSanPham }}</div></div>
          <div class="p-detail-field"><span class="p-field-label">Danh mục</span><div class="p-readonly-control">{{ product.tenDanhMuc || '—' }}</div></div>
          <div class="p-detail-field"><span class="p-field-label">Thương hiệu</span><div class="p-readonly-control">{{ product.tenThuongHieu || '—' }}</div></div>
          <div class="p-detail-field"><span class="p-field-label">Chất liệu</span><div class="p-readonly-control">{{ product.tenChatLieu || '—' }}</div></div>
          <div class="p-detail-field"><span class="p-field-label">Kiểu dáng</span><div class="p-readonly-control">{{ product.tenKieuDang || '—' }}</div></div>
          <div class="p-detail-field"><span class="p-field-label">Cổ giày</span><div class="p-readonly-control">{{ product.tenCoGiay || '—' }}</div></div>
          <div class="p-detail-field"><span class="p-field-label">Xuất xứ</span><div class="p-readonly-control">{{ product.tenXuatXu || '—' }}</div></div>
          <div class="p-detail-field"><span class="p-field-label">Trạng thái</span><div class="p-readonly-control"><StatusBadge :status="product.trangThai" /><button type="button" class="p-btn" :disabled="busy" @click="productStatus"><i class="bi bi-power"></i> Đổi trạng thái</button></div></div>
          <div class="p-detail-field"><span class="p-field-label">Giá bán</span><div class="p-readonly-control"><ProductPrice :item="product" range /></div></div>
          <div class="p-detail-field"><span class="p-field-label">Ngày tạo</span><div class="p-readonly-control">{{ dateTime(product.ngayTao) }}</div></div>
          <div class="p-detail-field"><span class="p-field-label">Ngày cập nhật</span><div class="p-readonly-control">{{ dateTime(product.ngayCapNhat) }}</div></div>
          <div class="p-detail-field wide"><span class="p-field-label">Mô tả chi tiết</span><div class="p-readonly-control p-description">{{ product.moTaChiTiet?.trim() || 'Chưa có mô tả.' }}</div></div>
        </div>
      </section>
      <section class="p-card"><h2><i class="bi bi-layers"></i> Biến thể của sản phẩm ({{ detail.variants.length }})</h2><VariantTable :rows="detail.variants" :busy="busy" @edit="editing = $event" @status="variantStatus" /></section>
      <VariantCreate :key="product.id" :product="product" :existing="detail.variants" :options="options" @created="success = 'Đã tạo biến thể.'; load()" />
      <ProductImages :key="'images-' + product.id" :product-id="product.id" :product-code="product.maSanPham" :images="detail.images" @changed="load()" />
      <VariantEditModal v-if="editing" :key="editing.id" :variant="editing" :options="options" @close="editing = null" @saved="variantSaved" @images-changed="load()" />
    </template>
    <div v-else class="p-card p-empty">{{ error ? 'Không tải được sản phẩm.' : 'Đang tải...' }} <button v-if="error" class="p-btn" @click="load(true)">Thử lại</button></div>
    <ConfirmModal v-bind="confirmation || {}" :show="!!confirmation" :loading="confirming" :error="confirmError" @confirm="confirmAction" @cancel="cancelConfirmation" />
  </ProductShell>
</template>
