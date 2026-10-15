package com.smashstep.datn.sales.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class SalesResponse {
    private SalesResponse() {}

    @Getter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @EqualsAndHashCode(exclude = "idSanPham")
    public static final class CatalogItem {
        private Long idSanPham;
        private final Long id;
        private final String maSanPhamChiTiet;
        private final String maSanPham;
        private final String tenSanPham;
        private final String mauSac;
        private final String kichThuoc;
        private final BigDecimal giaBan;
        private final BigDecimal giaGoc;
        private Integer tonKho;
        private final Boolean dangGiamGia;
        private final BigDecimal phanTramGiam;
        private final String maDotGiamGia;
        private final String tenDotGiamGia;

        public CatalogItem(
                Long id,
                String maSanPhamChiTiet,
                String maSanPham,
                String tenSanPham,
                String mauSac,
                String kichThuoc,
                BigDecimal giaBan,
                BigDecimal giaGoc,
                Integer tonKho) {
            this(
                    id,
                    maSanPhamChiTiet,
                    maSanPham,
                    tenSanPham,
                    mauSac,
                    kichThuoc,
                    giaBan,
                    giaGoc,
                    tonKho,
                    null,
                    null,
                    null,
                    null);
        }

        public CatalogItem(
                Long id,
                String maSanPhamChiTiet,
                String maSanPham,
                String tenSanPham,
                String mauSac,
                String kichThuoc,
                BigDecimal giaBan,
                BigDecimal giaGoc,
                Integer tonKho,
                Boolean dangGiamGia,
                BigDecimal phanTramGiam,
                String maDotGiamGia,
                String tenDotGiamGia) {
            this.id = id;
            this.maSanPhamChiTiet = maSanPhamChiTiet;
            this.maSanPham = maSanPham;
            this.tenSanPham = tenSanPham;
            this.mauSac = mauSac;
            this.kichThuoc = kichThuoc;
            this.giaBan = giaBan;
            this.giaGoc = giaGoc;
            this.tonKho = tonKho;
            this.dangGiamGia = dangGiamGia;
            this.phanTramGiam = phanTramGiam;
            this.maDotGiamGia = maDotGiamGia;
            this.tenDotGiamGia = tenDotGiamGia;
        }

        public CatalogItem tonKhaDung(int soLuong) {
            this.tonKho = soLuong;
            return this;
        }

        public CatalogItem ganIdSanPham(Long idSanPham) {
            this.idSanPham = idSanPham;
            return this;
        }
    }

    public record PaymentMethod(
            Long id, String maPhuongThucThanhToan, String tenPhuongThucThanhToan) {}

    public record VoucherOption(
            String maPhieuGiamGia,
            String tenPhieuGiamGia,
            BigDecimal tienGiam,
            LocalDateTime ngayHetHan,
            Integer loaiGiamGia,
            BigDecimal giaTriGiam,
            Integer hinhThucPhieu,
            String lyDoKhongApDung) {
        public VoucherOption(
                String ma,
                String ten,
                BigDecimal giam,
                LocalDateTime hetHan,
                Integer loai,
                BigDecimal giaTri) {
            this(ma, ten, giam, hetHan, loai, giaTri, null, null);
        }

        public VoucherOption(
                String maPhieuGiamGia,
                String tenPhieuGiamGia,
                BigDecimal tienGiam,
                LocalDateTime ngayHetHan) {
            this(maPhieuGiamGia, tenPhieuGiamGia, tienGiam, ngayHetHan, null, null, null, null);
        }
    }

    public record ReceiptPayment(
            Long idPhuongThucThanhToan,
            String maPhuongThucThanhToan,
            String tenPhuongThucThanhToan,
            BigDecimal soTien,
            String maGiaoDich) {}

    @Getter
    @EqualsAndHashCode(of = {"tongTienHang", "tienGiam", "thanhTien"})
    @ToString
    public static final class Quote {
        private final BigDecimal tongTienHang;
        private final BigDecimal tienGiam;
        private final BigDecimal thanhTien;
        private final BigDecimal phiVanChuyen;
        private final List<VoucherOption> danhSachPhieuGiamGia;
        private final String loiPhieuDaChon;
        private List<VoucherOption> phieuChuaDuDieuKien = List.of();

        public Quote phieuChuaDuDieuKien(List<VoucherOption> phieu) {
            this.phieuChuaDuDieuKien = List.copyOf(phieu);
            return this;
        }

        // Điểm gọi Java cũ còn được tham chiếu ngoài module POS; không tạo trường JSON tiếng Anh.
        @Deprecated
        public BigDecimal subtotal() {
            return tongTienHang;
        }

        @Deprecated
        public BigDecimal discount() {
            return tienGiam;
        }

        @Deprecated
        public BigDecimal total() {
            return thanhTien;
        }

        public Quote(BigDecimal tongTienHang, BigDecimal tienGiam, BigDecimal thanhTien) {
            this(tongTienHang, tienGiam, thanhTien, BigDecimal.ZERO, List.of(), null);
        }

        public Quote(
                BigDecimal tongTienHang,
                BigDecimal tienGiam,
                BigDecimal thanhTien,
                BigDecimal phiVanChuyen,
                List<VoucherOption> danhSachPhieuGiamGia,
                String loiPhieuDaChon) {
            this.tongTienHang = tongTienHang;
            this.tienGiam = tienGiam;
            this.thanhTien = thanhTien;
            this.phiVanChuyen = phiVanChuyen;
            this.danhSachPhieuGiamGia = List.copyOf(danhSachPhieuGiamGia);
            this.loiPhieuDaChon = loiPhieuDaChon;
        }
    }

    @Getter
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @EqualsAndHashCode(exclude = {"danhSachThanhToan", "phiVanChuyen", "trangThai"})
    public static final class Receipt {
        private List<ReceiptPayment> danhSachThanhToan = List.of();
        private BigDecimal phiVanChuyen = BigDecimal.ZERO;
        private Integer trangThai;
        private final Long idHoaDon;
        private final String maHoaDon;
        private final BigDecimal tongTienHang;
        private final BigDecimal tienGiam;
        private final BigDecimal thanhTien;
        private final BigDecimal tienThua;
        private final Long idNhanVien;
        private final String maNhanVien;
        private final String tenNhanVien;
        private final Long idKhachHang;
        private final String maKhachHang;
        private final String tenKhachHang;
        private final String soDienThoaiKhachHang;
        private final String maPhuongThucThanhToan;
        private final String tenPhuongThucThanhToan;
        private final String maPhieuGiamGia;
        private final LocalDateTime ngayTao;
        private final BigDecimal tienKhachDua;

        public Receipt(
                Long idHoaDon,
                String maHoaDon,
                BigDecimal tongTienHang,
                BigDecimal tienGiam,
                BigDecimal thanhTien,
                BigDecimal tienThua) {
            this(
                    idHoaDon,
                    maHoaDon,
                    tongTienHang,
                    tienGiam,
                    thanhTien,
                    tienThua,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        public Receipt(
                Long idHoaDon,
                String maHoaDon,
                BigDecimal tongTienHang,
                BigDecimal tienGiam,
                BigDecimal thanhTien,
                BigDecimal tienThua,
                Long idNhanVien,
                String maNhanVien,
                String tenNhanVien,
                Long idKhachHang,
                String maKhachHang,
                String tenKhachHang,
                String soDienThoaiKhachHang,
                String maPhuongThucThanhToan,
                String tenPhuongThucThanhToan,
                String maPhieuGiamGia,
                LocalDateTime ngayTao,
                BigDecimal tienKhachDua) {
            this.idHoaDon = idHoaDon;
            this.maHoaDon = maHoaDon;
            this.tongTienHang = tongTienHang;
            this.tienGiam = tienGiam;
            this.thanhTien = thanhTien;
            this.tienThua = tienThua;
            this.idNhanVien = idNhanVien;
            this.maNhanVien = maNhanVien;
            this.tenNhanVien = tenNhanVien;
            this.idKhachHang = idKhachHang;
            this.maKhachHang = maKhachHang;
            this.tenKhachHang = tenKhachHang;
            this.soDienThoaiKhachHang = soDienThoaiKhachHang;
            this.maPhuongThucThanhToan = maPhuongThucThanhToan;
            this.tenPhuongThucThanhToan = tenPhuongThucThanhToan;
            this.maPhieuGiamGia = maPhieuGiamGia;
            this.ngayTao = ngayTao;
            this.tienKhachDua = tienKhachDua;
        }

        public Receipt ghiNhanThanhToan(
                List<ReceiptPayment> danhSachThanhToan,
                BigDecimal phiVanChuyen,
                Integer trangThai) {
            this.danhSachThanhToan = List.copyOf(danhSachThanhToan);
            this.phiVanChuyen = phiVanChuyen;
            this.trangThai = trangThai;
            return this;
        }
    }
}
