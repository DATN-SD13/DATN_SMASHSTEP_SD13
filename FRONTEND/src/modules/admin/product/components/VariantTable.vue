<script setup>
import StatusBadge from './StatusBadge.vue'
import ProductThumbnail from './ProductThumbnail.vue'
import { money } from '../services/productService'
defineProps({ rows: { type: Array, default: () => [] }, offset: Number, busy: Boolean })
defineEmits(['edit', 'status'])
</script>
<template>
  <div class="p-table-wrap"><table><thead><tr><th>STT</th><th>Ảnh</th><th>Mã SP</th><th>Mã chi tiết</th><th>SKU</th><th>Sản phẩm</th><th>Màu sắc</th><th>Kích thước</th><th>Số lượng</th><th>Giá bán</th><th>Kích hoạt</th><th>Trạng thái</th><th>Hành động</th></tr></thead><tbody>
    <tr v-for="(v, i) in rows" :key="v.id"><td>{{ (offset || 0) + i + 1 }}</td><td><ProductThumbnail :url="v.anhChinh" :alt="v.tenSanPham" /></td><td>{{ v.maSanPham }}</td><td>{{ v.maChiTietSanPham }}</td><td>{{ v.sku }}</td><td class="p-name"><RouterLink :to="'/san-pham/' + v.sanPhamId">{{ v.tenSanPham }}</RouterLink></td><td class="nowrap"><span class="p-swatch" :style="{ background: /^#[0-9a-f]{6}$/i.test(v.maMauHex || '') ? v.maMauHex : 'transparent' }"></span>{{ v.tenMauSac || '—' }}</td><td>{{ v.giaTriKichThuoc || '—' }}</td><td>{{ v.soLuong }}</td><td class="nowrap">{{ money(v.giaBan) }}</td><td>{{ v.kichHoat ? 'Có' : 'Không' }}</td><td><StatusBadge :status="v.trangThai" /></td><td><div class="p-actions nowrap"><button class="p-btn" :disabled="busy" title="Sửa biến thể" aria-label="Sửa biến thể" @click="$emit('edit', v)"><i class="bi bi-pencil"></i></button><button class="p-btn" :disabled="busy" :class="{ danger: v.trangThai === 1 }" title="Đổi trạng thái" aria-label="Đổi trạng thái biến thể" @click="$emit('status', v)"><i class="bi bi-power"></i></button></div></td></tr>
    <tr v-if="!rows.length"><td colspan="13" class="p-empty">{{ busy ? 'Đang tải biến thể...' : 'Chưa có biến thể phù hợp.' }}</td></tr>
  </tbody></table></div>
</template>
