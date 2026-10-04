<script setup>
import StatusBadge from './StatusBadge.vue'
import { priceRange, dateTime } from '../services/productService'
defineProps({ rows: { type: Array, default: () => [] }, offset: Number, busy: Boolean })
defineEmits(['status'])
</script>
<template>
  <div class="p-table-wrap">
    <table><thead><tr><th>STT</th><th>Mã sản phẩm</th><th>Tên sản phẩm</th><th>Danh mục</th><th>Thương hiệu</th><th>Chất liệu</th><th>Kiểu dáng</th><th>Tồn kho</th><th>Giá bán</th><th>Trạng thái</th><th>Ngày tạo</th><th>Hành động</th></tr></thead>
      <tbody><tr v-for="(p, i) in rows" :key="p.id"><td>{{ (offset || 0) + i + 1 }}</td><td><strong>{{ p.maSanPham }}</strong></td><td class="p-name">{{ p.tenSanPham }}</td><td>{{ p.tenDanhMuc || '—' }}</td><td>{{ p.tenThuongHieu || '—' }}</td><td>{{ p.tenChatLieu || '—' }}</td><td>{{ p.tenKieuDang || '—' }}</td><td>{{ p.tongSoLuong }}</td><td class="nowrap">{{ priceRange(p) }}</td><td><StatusBadge :status="p.trangThai" /></td><td class="nowrap">{{ dateTime(p.ngayTao) }}</td><td><div class="p-actions nowrap"><RouterLink class="p-btn" :to="'/san-pham/' + p.id" title="Xem chi tiết" aria-label="Xem chi tiết sản phẩm"><i class="bi bi-eye"></i></RouterLink><RouterLink class="p-btn" :to="'/san-pham/' + p.id + '/sua'" title="Sửa" aria-label="Sửa sản phẩm"><i class="bi bi-pencil"></i></RouterLink><button class="p-btn" :class="{ danger: p.trangThai === 1 }" :disabled="busy" :title="p.trangThai === 1 ? 'Ngừng hoạt động' : 'Bật hoạt động'" aria-label="Đổi trạng thái sản phẩm" @click="$emit('status', p)"><i class="bi bi-power"></i></button></div></td></tr><tr v-if="!rows.length"><td colspan="12" class="p-empty">{{ busy ? 'Đang tải sản phẩm...' : 'Không tìm thấy sản phẩm phù hợp.' }}</td></tr></tbody>
    </table>
  </div>
</template>

