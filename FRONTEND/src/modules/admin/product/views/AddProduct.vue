<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { computed, nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'
import { brands, shoeTypes, colorOptions, sizeOptions } from '../services/productData'

const router = useRouter()
const f = ref({ code: 'G66748', name: '', brand: '', type: '', gender: 'all', material: '', sole: '', collar: '', cushion: '', weight: '', colors: [], sizes: [] })
const opts = {
  gender: ['Nam', 'Nữ', 'Unisex'], material: ['Vải lưới', 'Da tổng hợp', 'Da thật', 'Knit'], sole: ['Cao su', 'EVA', 'TPU'],
  collar: ['Cổ thấp', 'Cổ trung', 'Cổ cao'], cushion: ['Gel', 'Boost', 'Air', 'Fresh Foam'], weight: ['Dưới 250g', '250–300g', 'Trên 300g']
}
const pickColor = ref('')
const pickSize = ref('')
const err = ref('')

function addColor() { if (pickColor.value && !f.value.colors.includes(pickColor.value)) f.value.colors.push(pickColor.value); pickColor.value = '' }
function addSize() { if (pickSize.value && !f.value.sizes.includes(pickSize.value)) f.value.sizes.push(pickSize.value); pickSize.value = '' }

const groups = ref([]) // [{ color, hex, images: [], rows: [{ size, qty, price, checked }] }]
const defQty = ref(100)
const defPrice = ref(0)
const priceErr = ref(false)
const saveErr = ref('')
const variantsEl = ref(null)

const hexOf = (name) => colorOptions.find(c => c.name === name)?.hex || '#999'
const fmt = (n) => (Number(n) || 0).toLocaleString('vi-VN')
function onMoney(e, target, key) {
  const n = Number(String(e.target.value).replace(/\D/g, '')) || 0
  target[key] = n
  e.target.value = fmt(n)
}

function onDefPrice(e) {
  const n = Number(String(e.target.value).replace(/\D/g, '')) || 0
  defPrice.value = n
  e.target.value = fmt(n)
  priceErr.value = false
}

const allRows = computed(() => groups.value.flatMap(g => g.rows))
const allChecked = computed({
  get: () => allRows.value.length > 0 && allRows.value.every(r => r.checked),
  set: (v) => allRows.value.forEach(r => { r.checked = v })
})
const groupChecked = (g) => g.rows.length > 0 && g.rows.every(r => r.checked)
const toggleGroup = (g, v) => g.rows.forEach(r => { r.checked = v })
function removeRow(g, size) {
  g.rows = g.rows.filter(r => r.size !== size)
  if (!g.rows.length) groups.value = groups.value.filter(x => x !== g)
}

async function generate() {
  const v = f.value
  if (!v.name || !v.brand || !v.type || !v.material || !v.sole || !v.collar || !v.cushion || !v.weight) {
    err.value = 'Vui lòng nhập đầy đủ các trường bắt buộc (*).'; return
  }
  if (!v.colors.length || !v.sizes.length) {
    err.value = 'Chọn ít nhất 1 màu sắc và 1 kích cỡ để tạo biến thể.'; return
  }
  err.value = ''
  // giữ lại dữ liệu đã nhập nếu bấm tạo lại
  const oldRows = new Map(groups.value.flatMap(g => g.rows.map(r => [`${g.color}|${r.size}`, r])))
  const oldImgs = new Map(groups.value.map(g => [g.color, g.images]))
  const sizes = [...v.sizes].sort((a, b) => a - b)
  groups.value = v.colors.map(color => ({
    color, hex: hexOf(color), images: oldImgs.get(color) || [],
    rows: sizes.map(size => oldRows.get(`${color}|${size}`) || { size, qty: 0, price: 0, checked: true })
  }))
  await nextTick()
  variantsEl.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function applyDefaults() {
  if (!defPrice.value) { priceErr.value = true; return }
  priceErr.value = false
  groups.value.forEach(g => g.rows.forEach(r => { if (r.checked) { r.qty = Number(defQty.value) || 0; r.price = defPrice.value } }))
}

function addImages(g, e) {
  for (const file of e.target.files) {
    const r = new FileReader()
    r.onload = () => g.images.push(r.result)
    r.readAsDataURL(file)
  }
  e.target.value = ''
}

function save() {
  const sel = allRows.value.filter(r => r.checked)
  if (!sel.length) { saveErr.value = 'Chọn ít nhất 1 biến thể để lưu.'; return }
  if (sel.some(r => !r.price)) { saveErr.value = 'Mỗi biến thể được chọn phải có giá bán lớn hơn 0.'; return }
  saveErr.value = ''
  alert(`Đã lưu sản phẩm "${f.value.name}" và ${sel.length} chi tiết sản phẩm`)
  router.push('/bien-the-san-pham')
}
</script>

<template>
  <AdminLayout>
    <main class="ss-page">
      <div class="ss-actions"><button class="ss-btn" @click="router.push('/san-pham')"><i class="bi bi-arrow-left"></i> Quay lại danh sách</button></div>

      <section class="ss-card ss-form">
        <div class="top">
          <div class="ss-field"><label class="ss-label muted">Mã sản phẩm</label><input class="ss-input" v-model="f.code" /></div>
          <div class="ss-field"><label class="ss-label">Sản phẩm <span class="req">*</span></label><input class="ss-input" v-model="f.name" placeholder="Nhập tên sản phẩm..." /></div>
        </div>
        <div class="ss-grid2">
          <div class="ss-field"><label class="ss-label">Thương hiệu <span class="req">*</span></label>
            <select class="ss-select" v-model="f.brand"><option value="" disabled>Chọn thương hiệu...</option><option v-for="b in brands" :key="b">{{ b }}</option></select></div>
          <div class="ss-field"><label class="ss-label">Loại giày <span class="req">*</span></label>
            <select class="ss-select" v-model="f.type"><option value="" disabled>Chọn loại giày...</option><option v-for="b in shoeTypes" :key="b">{{ b }}</option></select></div>
          <div class="ss-field"><label class="ss-label">Giới tính <span class="req">*</span></label>
            <select class="ss-select" v-model="f.gender"><option value="all">Tất cả</option><option v-for="b in opts.gender" :key="b">{{ b }}</option></select></div>
          <div class="ss-field"><label class="ss-label">Chất liệu <span class="req">*</span></label>
            <select class="ss-select" v-model="f.material"><option value="" disabled>Chọn chất liệu giày...</option><option v-for="b in opts.material" :key="b">{{ b }}</option></select></div>
          <div class="ss-field"><label class="ss-label">Đế giày <span class="req">*</span></label>
            <select class="ss-select" v-model="f.sole"><option value="" disabled>Chọn đế giày...</option><option v-for="b in opts.sole" :key="b">{{ b }}</option></select></div>
          <div class="ss-field"><label class="ss-label">Cổ giày <span class="req">*</span></label>
            <select class="ss-select" v-model="f.collar"><option value="" disabled>Chọn cổ giày...</option><option v-for="b in opts.collar" :key="b">{{ b }}</option></select></div>
          <div class="ss-field"><label class="ss-label">Công nghệ đệm <span class="req">*</span></label>
            <select class="ss-select" v-model="f.cushion"><option value="" disabled>Chọn công nghệ đệm...</option><option v-for="b in opts.cushion" :key="b">{{ b }}</option></select></div>
          <div class="ss-field"><label class="ss-label">Trọng lượng <span class="req">*</span></label>
            <select class="ss-select" v-model="f.weight"><option value="" disabled>Chọn trọng lượng...</option><option v-for="b in opts.weight" :key="b">{{ b }}</option></select></div>
        </div>
      </section>

      <section class="ss-card ss-form">
        <div class="line">
          <label class="ss-label">Màu sắc <span class="req">*</span></label>
          <select class="ss-select" v-model="pickColor" @change="addColor"><option value="">Chọn màu</option><option v-for="c in colorOptions" :key="c.name" :value="c.name">{{ c.name }}</option></select>
          <button class="ss-btn" type="button" @click="addColor">Thêm nhanh</button>
        </div>
        <div v-if="f.colors.length" class="ss-chips pad"><span v-for="c in f.colors" :key="c" class="ss-chip">{{ c }}<button type="button" @click="f.colors = f.colors.filter(x => x !== c)"><i class="bi bi-x"></i></button></span></div>

        <div class="line">
          <label class="ss-label">Kích cỡ <span class="req">*</span></label>
          <select class="ss-select" v-model="pickSize" @change="addSize"><option value="">Chọn size</option><option v-for="s in sizeOptions" :key="s">{{ s }}</option></select>
          <button class="ss-btn" type="button" @click="addSize">Thêm nhanh</button>
        </div>
        <div v-if="f.sizes.length" class="ss-chips pad"><span v-for="c in f.sizes" :key="c" class="ss-chip">{{ c }}<button type="button" @click="f.sizes = f.sizes.filter(x => x !== c)"><i class="bi bi-x"></i></button></span></div>

        <p v-if="err" class="ss-hint warn"><i class="bi bi-exclamation-triangle"></i> {{ err }}</p>
        <div class="ss-actions"><button class="ss-btn primary" @click="generate"><i class="bi bi-magic"></i> Tạo biến thể tự động</button></div>
      </section>

      <template v-if="groups.length">
        <!-- Thiết lập biến thể -->
        <section ref="variantsEl" class="ss-card ss-form">
          <label class="ss-check strong"><input class="ss-cb" type="checkbox" v-model="allChecked" /> Chọn tất cả biến thể</label>

          <div class="defaults">
            <div class="ss-field"><label class="ss-label muted">Số lượng mặc định</label><input class="ss-input" type="number" min="0" v-model.number="defQty" /></div>
            <div class="ss-field"><label class="ss-label muted">Giá bán mặc định <span class="req">*</span></label>
              <input class="ss-input" :class="{ err: priceErr }" inputmode="numeric" :value="fmt(defPrice)" @input="onDefPrice" /></div>
            <button type="button" class="ss-btn" @click="applyDefaults">Áp dụng</button>
          </div>
          <p v-if="priceErr" class="ss-hint warn"><i class="bi bi-exclamation-triangle"></i> Nhập giá bán mặc định lớn hơn 0 trước khi áp dụng.</p>

          <div v-for="g in groups" :key="g.color" class="group">
            <div class="g-head">
              <span class="ss-dot big" :style="{ background: g.hex }"></span><strong>{{ g.color }}</strong>
              <span class="sp"></span>
              <span class="ss-pill gray">{{ g.rows.map(r => `Size ${r.size}`).join(' • ') }}</span>
            </div>
            <div class="ss-table-wrap">
              <table class="ss-table" style="min-width:560px">
                <thead><tr>
                  <th style="width:44px" class="c"><input class="ss-cb" type="checkbox" :checked="groupChecked(g)" @change="toggleGroup(g, $event.target.checked)" /></th>
                  <th class="w-stt c">STT</th><th>Kích cỡ</th><th>Số lượng</th><th>Giá bán</th><th class="c" style="width:70px">Xóa</th>
                </tr></thead>
                <tbody>
                  <tr v-for="(r, i) in g.rows" :key="r.size">
                    <td class="c"><input class="ss-cb" type="checkbox" v-model="r.checked" /></td>
                    <td class="c">{{ i + 1 }}</td>
                    <td class="ss-strong">Size {{ r.size }}</td>
                    <td><input class="ss-input cell" type="number" min="0" v-model.number="r.qty" /></td>
                    <td><input class="ss-input cell wide" inputmode="numeric" :value="fmt(r.price)" @input="onMoney($event, r, 'price')" /></td>
                    <td class="c"><button type="button" class="ss-icon-btn danger" title="Xóa biến thể" @click="removeRow(g, r.size)"><i class="bi bi-trash3"></i></button></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>

        <!-- Ảnh theo màu -->
        <section class="ss-card ss-form">
          <div>
            <h2 class="h">Ảnh sản phẩm chi tiết</h2>
            <p class="ss-hint" style="margin-top:3px">Thêm ảnh cho từng màu sắc (biến thể đại diện) để tự động đồng bộ cho toàn bộ kích cỡ.</p>
          </div>
          <div class="img-grid">
            <div v-for="g in groups" :key="g.color" class="img-card">
              <div class="img-head">
                <div><strong>Ảnh sản phẩm màu {{ g.color.toLowerCase() }}</strong>
                  <span class="ss-sub">Áp dụng cho {{ g.rows.length }} kích cỡ cùng màu • Size {{ g.rows[0].size }}</span></div>
                <span class="sp"></span>
                <button type="button" class="ss-icon-btn" title="Xóa tất cả ảnh" @click="g.images = []"><i class="bi bi-arrow-repeat"></i></button>
                <label class="ss-btn primary sm"><i class="bi bi-plus-lg"></i> Thêm ảnh<input type="file" accept="image/*" multiple hidden @change="addImages(g, $event)" /></label>
              </div>
              <div v-if="!g.images.length" class="img-empty">
                <i class="bi bi-image"></i><strong>Nhóm màu này chưa có ảnh</strong>
                <span>Thêm một bộ ảnh để áp dụng cho toàn bộ kích cỡ cùng màu.</span>
              </div>
              <div v-else class="img-list">
                <div v-for="(src, i) in g.images" :key="i" class="img-item"><img :src="src" alt="" /><button type="button" title="Xóa ảnh" @click="g.images.splice(i, 1)"><i class="bi bi-x"></i></button></div>
              </div>
            </div>
          </div>
        </section>

        <div class="ss-actions">
          <p v-if="saveErr" class="ss-hint warn"><i class="bi bi-exclamation-triangle"></i> {{ saveErr }}</p>
          <button type="button" class="ss-btn primary" @click="save"><i class="bi bi-save"></i> Lưu sản phẩm và CTSP</button>
        </div>
      </template>
    </main>
  </AdminLayout>
</template>

<style scoped>
.top { display: grid; grid-template-columns: 220px minmax(0, 1fr); gap: 16px; }
.line { display: grid; grid-template-columns: 90px minmax(0, 1fr) auto; gap: 14px; align-items: center; }
.pad { padding-left: 104px; }
.ss-label.muted { font-weight: 500; color: var(--ss-muted); }
.defaults { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) auto; gap: 14px; align-items: end; }
.ss-input.err { border-color: var(--ss-danger); background: #fff; box-shadow: 0 0 0 3px rgba(184, 38, 43, .1); }
.ss-check.strong { font-size: 12px; font-weight: 600; color: var(--ss-text); }
.group { border: 1px solid var(--ss-line); border-radius: 12px; padding: 14px; display: flex; flex-direction: column; gap: 10px; background: #fff; }
.g-head { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--ss-ink); }
.sp { flex: 1; }
.ss-dot.big { width: 11px; height: 11px; margin: 0; }
.ss-input.cell { width: 110px; height: 34px; }
.ss-input.cell.wide { width: 150px; }
.h { font-size: 13px; font-weight: 600; color: var(--ss-text); }
.img-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.img-card { border: 1px solid var(--ss-line); border-radius: 12px; padding: 14px; display: flex; flex-direction: column; gap: 12px; }
.img-head { display: flex; align-items: center; gap: 8px; }
.img-head strong { font-size: 12px; color: var(--ss-ink); }
.img-empty { min-height: 150px; border: 1px dashed var(--ss-border); border-radius: 10px; background: var(--ss-surface); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; text-align: center; padding: 14px; color: var(--ss-faint); }
.img-empty i { font-size: 22px; }
.img-empty strong { font-size: 12px; color: var(--ss-muted); }
.img-empty span { font-size: 10.5px; }
.img-list { display: grid; grid-template-columns: repeat(auto-fill, minmax(88px, 1fr)); gap: 10px; }
.img-item { position: relative; aspect-ratio: 1; border-radius: 10px; overflow: hidden; border: 1px solid var(--ss-line); }
.img-item img { width: 100%; height: 100%; object-fit: cover; display: block; }
.img-item button { position: absolute; top: 4px; right: 4px; width: 20px; height: 20px; border: 0; border-radius: 50%; background: rgba(15, 23, 42, .65); color: #fff; display: grid; place-items: center; cursor: pointer; padding: 0; }
.img-item button:hover { background: var(--ss-danger); }
@media (max-width: 900px) { .img-grid { grid-template-columns: 1fr; } .defaults { grid-template-columns: 1fr; } }
@media (max-width: 760px) { .top, .line { grid-template-columns: 1fr; } .pad { padding-left: 0; } }
</style>
