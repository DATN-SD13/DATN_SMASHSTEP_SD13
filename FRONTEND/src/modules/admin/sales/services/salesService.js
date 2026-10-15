import api from '../../../../utils/api'

const layDuLieu = phanHoi => phanHoi.data.data

// Chuyển hợp đồng API tại một nơi; giữ mô hình giao diện và phiên thanh toán đã lưu.
const doiTenTruong = (duLieu, bangTen) => duLieu == null ? duLieu : Object.fromEntries(
  Object.entries(duLieu).map(([ten, giaTri]) => [bangTen[ten] || ten, giaTri])
)
const tenYeuCau = {
  paymentVersion: 'phienBanThanhToan', requestId: 'maYeuCau', invoiceId: 'idHoaDon', revision: 'phienBan', customerId: 'idKhachHang',
  employeeId: 'idNhanVien', paymentMethodId: 'idPhuongThucThanhToan', voucherCode: 'maPhieuGiamGia',
  paidAmount: 'tienKhachDua', note: 'ghiChu', transferConfirmed: 'daXacNhanChuyenKhoan'
}
const tenGiaoHang = {
  enabled: 'apDung', addressId: 'idDiaChi', recipientName: 'tenNguoiNhan', recipientPhone: 'soDienThoaiNguoiNhan',
  address: 'diaChi', shippingFee: 'phiVanChuyen'
}
const tenSanPham = {
  productId: 'idSanPham', code: 'maSanPhamChiTiet', productCode: 'maSanPham', name: 'tenSanPham',
  color: 'mauSac', size: 'kichThuoc', price: 'giaBan', originalPrice: 'giaGoc', currentPrice: 'giaHienTai',
  stock: 'tonKho', qty: 'soLuong', available: 'conKinhDoanh', discounted: 'dangGiamGia',
  discountPercent: 'phanTramGiam', campaignCode: 'maDotGiamGia', campaignName: 'tenDotGiamGia'
}
const tenKetQua = {
  invoiceId: 'idHoaDon', invoiceCode: 'maHoaDon', revision: 'phienBan', customerId: 'idKhachHang',
  customerCode: 'maKhachHang', customerName: 'tenKhachHang', customerPhone: 'soDienThoaiKhachHang',
  employeeId: 'idNhanVien', employeeCode: 'maNhanVien', employeeName: 'tenNhanVien',
  note: 'ghiChu', voucherCode: 'maPhieuGiamGia', subtotal: 'tongTienHang', discount: 'tienGiam',
  total: 'thanhTien', shippingFee: 'phiVanChuyen', change: 'tienThua', status: 'trangThai',
  paymentMethodCode: 'maPhuongThucThanhToan', paymentMethodName: 'tenPhuongThucThanhToan',
  createdAt: 'ngayTao', paidAmount: 'tienKhachDua', selectedVoucherError: 'loiPhieuDaChon'
}
const tenKhoanThu = {
  methodId: 'idPhuongThucThanhToan', methodCode: 'maPhuongThucThanhToan', methodName: 'tenPhuongThucThanhToan',
  amount: 'soTien', transactionCode: 'maGiaoDich'
}
const daoBangTen = bangTen => Object.fromEntries(Object.entries(bangTen).map(([cu, moi]) => [moi, cu]))
const docSanPham = duLieu => doiTenTruong(duLieu, daoBangTen(tenSanPham))
function taoYeuCau(duLieu) {
  const { items, delivery, payments, ...thongTin } = duLieu
  const yeuCau = doiTenTruong(thongTin, tenYeuCau)
  if (items !== undefined) yeuCau.danhSachSanPham = items.map(sanPham => doiTenTruong(sanPham, {
    variantId: 'idSanPhamChiTiet', quantity: 'soLuong', unitPrice: 'donGia'
  }))
  if (delivery !== undefined) yeuCau.giaoHang = doiTenTruong(delivery, tenGiaoHang)
  if (payments !== undefined) yeuCau.danhSachThanhToan = payments.map(khoanThu => doiTenTruong(khoanThu, tenKhoanThu))
  return yeuCau
}
function docKetQua(duLieu) {
  const { danhSachSanPham, giaoHang, danhSachThanhToan, danhSachPhieuGiamGia, phieuChuaDuDieuKien, ...thongTin } = duLieu
  const ketQua = doiTenTruong(thongTin, daoBangTen(tenKetQua))
  if (danhSachSanPham !== undefined) ketQua.items = danhSachSanPham.map(docSanPham)
  if (giaoHang !== undefined) ketQua.delivery = doiTenTruong(giaoHang, daoBangTen(tenGiaoHang))
  if (danhSachThanhToan !== undefined) ketQua.payments = danhSachThanhToan.map(p => doiTenTruong(p, daoBangTen(tenKhoanThu)))
  if (danhSachPhieuGiamGia !== undefined) ketQua.vouchers = danhSachPhieuGiamGia.map(p => doiTenTruong(p, {
    maPhieuGiamGia: 'code', tenPhieuGiamGia: 'name', tienGiam: 'discount', ngayHetHan: 'expiresAt',
    loaiGiamGia: 'type', giaTriGiam: 'value', hinhThucPhieu: 'form', lyDoKhongApDung: 'reason'
  }))
  if (phieuChuaDuDieuKien !== undefined) ketQua.ineligibleVouchers = phieuChuaDuDieuKien.map(p => doiTenTruong(p, {
    maPhieuGiamGia: 'code', tenPhieuGiamGia: 'name', tienGiam: 'discount', hinhThucPhieu: 'form', lyDoKhongApDung: 'reason'
  }))
  return ketQua
}

export default {
  async danhSachHoaDonCho() { return layDuLieu(await api.get('/ban-hang/hoa-don-cho')).map(docKetQua) },
  async chiTietHoaDonCho(id) { return docKetQua(layDuLieu(await api.get(`/ban-hang/hoa-don-cho/${id}`))) },
  async taoHoaDonCho() { return docKetQua(layDuLieu(await api.post('/ban-hang/hoa-don-cho'))) },
  async luuHoaDonCho(id, duLieu) { return docKetQua(layDuLieu(await api.put(`/ban-hang/hoa-don-cho/${id}`, taoYeuCau(duLieu)))) },
  async huyHoaDonCho(ma) {
    return layDuLieu(await api.put(`/hoa-don/${encodeURIComponent(ma)}/trang-thai`, {
      trangThai: 6, nguonThaoTac: 'POS', ghiChu: 'Hủy hóa đơn chờ tại POS (chưa thanh toán)'
    }))
  },
  async danhSachSanPhamBan(boLoc) {
    const params = doiTenTruong(boLoc, {
      page: 'trang', size: 'kichThuocTrang', keyword: 'tuKhoa', color: 'mauSac', variantSize: 'kichThuoc',
      categoryId: 'idDanhMuc', brandId: 'idThuongHieu'
    })
    const duLieu = layDuLieu(await api.get('/ban-hang/san-pham', { params }))
    return { ...duLieu, content: duLieu.content.map(docSanPham) }
  },
  async chiTietSanPhamBan(id) { return docSanPham(layDuLieu(await api.get(`/ban-hang/san-pham/${id}`))) },
  async danhSachPhuongThucThanhToan() {
    return layDuLieu(await api.get('/ban-hang/phuong-thuc-thanh-toan')).map(p => doiTenTruong(p, {
      maPhuongThucThanhToan: 'code', tenPhuongThucThanhToan: 'name'
    }))
  },
  async tinhTien(duLieu) { return docKetQua(layDuLieu(await api.post('/ban-hang/tinh-tien', taoYeuCau(duLieu)))) },
  async thanhToan(duLieu) { return docKetQua(layDuLieu(await api.post('/ban-hang/thanh-toan', taoYeuCau(duLieu)))) }
}
