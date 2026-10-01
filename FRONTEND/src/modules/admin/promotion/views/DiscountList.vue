<script setup>
import AdminLayout from '../../../../layouts/AdminLayout.vue'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const keyword = ref('')
const form = ref({ type: 'all', start: '', end: '', discount: 'all', status: 'all' })
const rows = ref([
  { code:'VCH65FFTO', name:'Giảm sốc cho giày New Balance', type:'Công khai', value:'60%', start:'16/08/2026', end:'01/09/2026', discount:'Phần trăm (%)', status:'Đang hoạt động' },
  { code:'VCH49OSEQJ', name:'New Balance giảm 25%', type:'Công khai', value:'25%', start:'16/08/2026', end:'01/09/2026', discount:'Phần trăm (%)', status:'Đang hoạt động' },
  { code:'VCH4FNGLO', name:'Puma Running giảm 20%', type:'Công khai', value:'50%', start:'13/08/2026', end:'03/09/2026', discount:'Phần trăm (%)', status:'Đang hoạt động' },
  { code:'VCHWS9DFI', name:'Giảm 50% Air Jordan 1 Low G', type:'Công khai', value:'50%', start:'12/08/2026', end:'01/09/2026', discount:'Phần trăm (%)', status:'Đang hoạt động' },
  { code:'VOUCHER5', name:'Giảm 500K', type:'Cá nhân', value:'500.000đ', start:'01/01/2025', end:'02/01/2036', discount:'Tiền mặt (VNĐ)', status:'Đang hoạt động' }
])
const filtered = computed(() => rows.value.filter(x => {
  const q = keyword.value.trim().toLowerCase()
  return (!q || x.code.toLowerCase().includes(q) || x.name.toLowerCase().includes(q)) &&
    (form.value.type === 'all' || x.type === form.value.type) &&
    (form.value.discount === 'all' || x.discount === form.value.discount) &&
    (form.value.status === 'all' || x.status === form.value.status)
}))
function reset(){ keyword.value=''; form.value={type:'all',start:'',end:'',discount:'all',status:'all'} }
</script>

<template>
  <AdminLayout>
    <main class="page">
      <div class="breadcrumb"><span>Quản lý giảm giá</span><i class="bi bi-chevron-right"></i><strong>Phiếu giảm giá</strong></div>
      <div class="page-heading">
        <div><h1>Phiếu giảm giá</h1><p>Quản lý mã giảm giá và chương trình ưu đãi cho khách hàng.</p></div>
        <button class="primary" @click="router.push('/giam-gia/them')"><i class="bi bi-plus-lg"></i> Tạo phiếu mới</button>
      </div>

      <section class="card filter-card">
        <div class="section-title"><div class="section-icon"><i class="bi bi-funnel"></i></div><div><h2>Bộ lọc</h2><p>Tra cứu nhanh dữ liệu phiếu giảm giá.</p></div></div>
        <div class="filter-grid">
          <label>Tìm kiếm<div class="input-wrap"><i class="bi bi-search"></i><input v-model="keyword" placeholder="Mã, tên phiếu..." /></div></label>
          <label>Hình thức<select v-model="form.type"><option value="all">Tất cả hình thức</option><option>Công khai</option><option>Cá nhân</option></select></label>
          <label>Ngày bắt đầu<input type="date" v-model="form.start" /></label>
          <label>Ngày kết thúc<input type="date" v-model="form.end" /></label>
          <label>Loại giảm<select v-model="form.discount"><option value="all">Tất cả loại giảm</option><option>Phần trăm (%)</option><option>Tiền mặt (VNĐ)</option></select></label>
          <label>Trạng thái<select v-model="form.status"><option value="all">Tất cả trạng thái</option><option>Đang hoạt động</option><option>Ngừng hoạt động</option></select></label>
        </div>
        <div class="filter-actions"><button class="secondary" @click="reset"><i class="bi bi-arrow-counterclockwise"></i> Đặt lại</button><button class="secondary"><i class="bi bi-file-earmark-excel"></i> Xuất Excel</button></div>
      </section>

      <section class="card table-card">
        <div class="table-heading"><div><h2>Danh sách phiếu giảm giá</h2><p>{{ filtered.length }} phiếu đang hiển thị</p></div><span class="count-badge">{{ rows.length }} phiếu</span></div>
        <div class="table-wrap"><table><thead><tr><th>STT</th><th>Mã phiếu</th><th>Tên phiếu</th><th>Hình thức</th><th>Giá trị giảm</th><th>Ngày bắt đầu</th><th>Ngày kết thúc</th><th>Trạng thái</th><th>Hành động</th></tr></thead>
          <tbody><tr v-for="(x,i) in filtered" :key="x.code"><td class="index">{{ i+1 }}</td><td><strong class="code">{{ x.code }}</strong></td><td class="name">{{ x.name }}</td><td><span class="type-pill" :class="x.type==='Cá nhân'?'personal':''"><i class="bi" :class="x.type==='Cá nhân'?'bi-person':'bi-globe2'"></i>{{ x.type }}</span></td><td><strong>{{ x.value }}</strong></td><td>{{ x.start }}</td><td>{{ x.end }}</td><td><span class="status active"><i class="bi bi-check-circle-fill"></i>{{ x.status }}</span></td><td><button class="icon-btn danger"><i class="bi bi-power"></i></button><button class="icon-btn"><i class="bi bi-eye"></i></button></td></tr></tbody>
        </table></div>
        <div class="table-footer"><select><option>5 / trang</option><option>10 / trang</option></select><div class="pagination"><button disabled><i class="bi bi-chevron-left"></i></button><button class="current">1</button><button disabled><i class="bi bi-chevron-right"></i></button></div></div>
      </section>
    </main>
  </AdminLayout>
</template>

<style scoped>

.page{min-height:calc(100vh - 72px);padding:22px 28px 44px;background:#eef4f7;color:#17303b}.breadcrumb{display:flex;align-items:center;gap:8px;font-size:11px;color:#607782;margin-bottom:8px}.breadcrumb strong{color:#17303b;font-weight:850}.breadcrumb i{font-size:9px;color:#91a4ad}.page-heading{display:flex;align-items:flex-end;justify-content:space-between;gap:18px;margin-bottom:20px}.page-heading h1{font-size:26px;line-height:1.15;margin:0;font-weight:900;color:#102c38;letter-spacing:-.35px}.page-heading p{font-size:12px;color:#637b86;margin:7px 0 0}.card{background:#fff;border:1px solid #d6e2e8;border-radius:16px;box-shadow:0 5px 18px rgba(21,54,70,.055);margin-bottom:18px}.filter-card{padding:21px 23px}.section-title{display:flex;gap:12px;align-items:center;margin-bottom:18px}.section-icon{width:40px;height:40px;border-radius:10px;display:grid;place-items:center;background:#e0f1f8;color:#087fb8;font-size:17px;border:1px solid #cbe5ef}.section-title h2,.table-heading h2{margin:0;font-size:16px;color:#102c38;font-weight:850}.section-title p,.table-heading p{margin:4px 0 0;color:#69808b;font-size:11px}.filter-grid{display:grid;grid-template-columns:1.35fr 1fr 1fr 1fr 1fr 1fr;gap:13px}.filter-grid label{display:flex;flex-direction:column;gap:7px;font-size:11px;font-weight:800;color:#27414d}.filter-grid input,.filter-grid select{height:43px;border:1px solid #ccdbe2;border-radius:10px;padding:0 11px;background:#fff;color:#17303b;font-size:12px;outline:none}.filter-grid input:focus,.filter-grid select:focus{border-color:#087fb8;box-shadow:0 0 0 3px rgba(8,127,184,.1)}.input-wrap{position:relative}.input-wrap i{position:absolute;left:12px;top:13px;color:#66808c;font-size:12px}.input-wrap input{width:100%;padding-left:34px}.filter-actions{display:flex;justify-content:flex-end;gap:9px;margin-top:16px}.primary,.secondary{height:42px;border-radius:10px;padding:0 16px;font-size:12px;font-weight:850;display:inline-flex;align-items:center;gap:7px;cursor:pointer}.primary{border:0;background:#087fb8;color:#fff;box-shadow:0 5px 12px rgba(8,127,184,.2)}.primary:hover{background:#066d9f}.secondary{border:1px solid #b9d5e1;background:#fff;color:#17586f}.secondary:hover{background:#eef8fc;border-color:#7eb9d1}.table-card{overflow:hidden}.table-heading{min-height:78px;display:flex;align-items:center;justify-content:space-between;padding:0 23px}.count-badge{font-size:11px;color:#086f9f;background:#e1f2f9;border:1px solid #c9e7f2;padding:7px 11px;border-radius:20px;font-weight:850}.table-wrap{overflow:auto;border-top:1px solid #dce8ed}.table-wrap table{width:100%;min-width:1080px;border-collapse:collapse}.table-wrap th{background:#e5f1f7;padding:13px;text-align:left;color:#173744;font-size:11px;font-weight:850;white-space:nowrap;border-bottom:1px solid #cadde5}.table-wrap td{padding:14px 13px;border-top:1px solid #e8eef1;font-size:12px;color:#203944;white-space:nowrap}.table-wrap tbody tr:hover{background:#f5fafc}.index{color:#55717d;width:50px;text-align:center}.code{font-size:12px;color:#102c38}.name{min-width:240px;white-space:normal!important;font-weight:650}.type-pill,.status{display:inline-flex;align-items:center;gap:5px;padding:6px 9px;border-radius:18px;font-size:10px;font-weight:850}.type-pill{background:#e0f1f8;color:#086f9f}.type-pill.personal{background:#fff0d8;color:#9a5e00}.status.active{background:#dcf5e9;color:#147c58}.status.inactive{background:#ffe1e5;color:#b73d4c}.status i{font-size:8px}.icon-btn{width:33px;height:33px;border:1px solid #cbdde5;background:#f6fafc;color:#526d79;border-radius:9px;margin-right:5px;cursor:pointer}.icon-btn.danger{color:#b73d4c}.icon-btn:hover{background:#e6f5fa;color:#087fb8;border-color:#9bc8db}.table-footer{height:64px;display:flex;align-items:center;justify-content:space-between;padding:0 23px;color:#55717d;font-size:11px}.table-footer select{height:35px;border:1px solid #cbdde5;border-radius:8px;padding:0 9px;color:#304b57;background:#fff;font-size:11px}.pagination{display:flex;gap:5px}.pagination button{width:32px;height:32px;border:1px solid #d1e0e6;background:#fff;border-radius:8px;color:#304b57}.pagination .current{background:#087fb8;border-color:#087fb8;color:#fff;font-weight:800}
@media(max-width:1050px){.filter-grid{grid-template-columns:1fr 1fr 1fr}.page{padding:20px}}@media(max-width:650px){.page{padding:17px 14px}.page-heading{align-items:flex-start;flex-direction:column}.filter-grid{grid-template-columns:1fr}.filter-actions{justify-content:stretch}.filter-actions>*{flex:1}}

</style>
