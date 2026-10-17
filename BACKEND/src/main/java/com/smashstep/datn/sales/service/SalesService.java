package com.smashstep.datn.sales.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.pricing.CurrentPrice;
import com.smashstep.datn.common.pricing.CurrentPriceService;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.invoice.entity.*;
import com.smashstep.datn.invoice.rules.PosDraftRules;
import com.smashstep.datn.invoice.rules.PosPriceSnapshot;
import com.smashstep.datn.invoice.service.InvoiceCodeService;
import com.smashstep.datn.payment.entity.PhuongThucThanhToan;
import com.smashstep.datn.product.entity.SanPhamChiTiet;
import com.smashstep.datn.product.service.KhoaGhiSanPham;
import com.smashstep.datn.promotion.entity.*;
import com.smashstep.datn.sales.dto.SalesRequest;
import com.smashstep.datn.sales.dto.SalesResponse.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalesService {
    private final EntityManager entityManager;
    private final CurrentPriceService dichVuGiaHienTai;
    private final InvoiceCodeService dichVuMaHoaDon;
    @Autowired private KhoaGhiSanPham khoaGhiSanPham;
    private static final BigDecimal SO_KHONG = new BigDecimal("0.00");
    private static final String DIEU_KIEN_DUOC_BAN =
            " where v.trangThai = 1 and v.kichHoat = true and v.soLuong > 0"
                    + " and v.giaBan > 0 and v.idSanPham.trangThai = 1";

    public PageResponse<CatalogItem> danhSachSanPhamBan(
            int trang, int kichThuocTrang, String tuKhoa) {
        return danhSachSanPhamBan(trang, kichThuocTrang, tuKhoa, "", "");
    }

    public PageResponse<CatalogItem> danhSachSanPhamBan(
            int trang, int kichThuocTrang, String tuKhoa, String mauSac, String kichThuoc) {
        return danhSachSanPhamBan(trang, kichThuocTrang, tuKhoa, mauSac, kichThuoc, null, null);
    }

    public PageResponse<CatalogItem> danhSachSanPhamBan(
            int trang,
            int kichThuocTrang,
            String tuKhoa,
            String mauSac,
            String kichThuoc,
            Long idDanhMuc,
            Long idThuongHieu) {
        if (trang < 0
                || kichThuocTrang < 1
                || kichThuocTrang > 100
                || (long) trang * kichThuocTrang > Integer.MAX_VALUE) {
            throw AppException.badRequest("Phân trang không hợp lệ");
        }
        String dieuKien =
                DIEU_KIEN_DUOC_BAN
                        + " and (lower(v.maChiTietSanPham) like :keyword or lower(v.sku) like"
                        + " :keyword or lower(v.idSanPham.maSanPham) like :keyword or"
                        + " lower(v.idSanPham.tenSanPham) like :keyword or"
                        + " lower(v.idMauSac.tenMauSac) like :keyword or"
                        + " lower(v.idKichThuoc.giaTri) like :keyword)";
        dieuKien +=
                " and (:color = '' or lower(v.idMauSac.tenMauSac) = :color)"
                        + " and (:variantSize = '' or v.idKichThuoc.giaTri = :variantSize)";
        if (idDanhMuc != null) dieuKien += " and v.idSanPham.idDanhMuc.id = :categoryId";
        if (idThuongHieu != null) dieuKien += " and v.idSanPham.idThuongHieu.id = :brandId";
        mauSac = mauSac == null ? "" : mauSac.trim().toLowerCase(Locale.ROOT);
        kichThuoc = kichThuoc == null ? "" : kichThuoc.trim();
        String tuKhoaTimKiem =
                "%" + (tuKhoa == null ? "" : tuKhoa.trim().toLowerCase(Locale.ROOT)) + "%";
        var countQuery =
                entityManager.createQuery(
                        "select count(v) from SanPhamChiTiet v" + dieuKien, Long.class);
        var rowQuery =
                entityManager.createQuery(
                        "select v from SanPhamChiTiet v join fetch v.idSanPham"
                                + " left join fetch v.idMauSac left join fetch v.idKichThuoc"
                                + dieuKien
                                + " order by v.id",
                        SanPhamChiTiet.class);
        for (var query : List.of(countQuery, rowQuery)) {
            query.setParameter("keyword", tuKhoaTimKiem)
                    .setParameter("color", mauSac)
                    .setParameter("variantSize", kichThuoc);
            if (idDanhMuc != null) query.setParameter("categoryId", idDanhMuc);
            if (idThuongHieu != null) query.setParameter("brandId", idThuongHieu);
        }
        long soLuongKetQua = countQuery.getSingleResult();
        List<SanPhamChiTiet> danhSach =
                rowQuery.setFirstResult(trang * kichThuocTrang)
                        .setMaxResults(kichThuocTrang)
                        .getResultList();
        LocalDateTime thoiDiem = LocalDateTime.now();
        var bangGia = dichVuGiaHienTai.getPrices(danhSach, thoiDiem);
        var giuHang =
                GiuHangTaiQuay.daGiu(
                        entityManager, danhSach.stream().map(SanPhamChiTiet::getId).toList(), null);
        List<CatalogItem> content =
                danhSach.stream()
                        .map(
                                v ->
                                        chuyenSanPhamBan(v, bangGia.get(v.getId()))
                                                .tonKhaDung(
                                                        GiuHangTaiQuay.khaDung(
                                                                v.getSoLuong(),
                                                                giuHang.getOrDefault(
                                                                        v.getId(), 0L))))
                        .toList();
        return new PageResponse<>(
                content,
                trang,
                kichThuocTrang,
                soLuongKetQua,
                (int) ((soLuongKetQua + kichThuocTrang - 1) / kichThuocTrang));
    }

    public List<PaymentMethod> danhSachPhuongThucThanhToan() {
        return entityManager
                .createQuery(
                        "select p from PhuongThucThanhToan p where p.trangThai = 1 and"
                                + " p.maPhuongThuc in ('TIEN_MAT','CHUYEN_KHOAN','THE') order by p.id",
                        PhuongThucThanhToan.class)
                .getResultList()
                .stream()
                .map(p -> new PaymentMethod(p.getId(), p.getMaPhuongThuc(), p.getTenPhuongThuc()))
                .toList();
    }

    public CatalogItem chiTietSanPhamBan(Long id) {
        SanPhamChiTiet v = entityManager.find(SanPhamChiTiet.class, id);
        if (v == null
                || !Integer.valueOf(1).equals(v.getTrangThai())
                || !Boolean.TRUE.equals(v.getKichHoat())
                || v.getSoLuong() == null
                || v.getSoLuong() <= 0
                || v.getIdSanPham() == null
                || !Integer.valueOf(1).equals(v.getIdSanPham().getTrangThai())
                || soTien(v.getGiaBan()).signum() <= 0) {
            throw AppException.conflict(
                    "Biến thể không còn được bán. Hãy bỏ sản phẩm này khỏi giỏ hàng");
        }
        var giuHang = GiuHangTaiQuay.daGiu(entityManager, List.of(v.getId()), null);
        return chuyenSanPhamBan(v, dichVuGiaHienTai.getPrice(v, LocalDateTime.now()))
                .tonKhaDung(
                        GiuHangTaiQuay.khaDung(
                                v.getSoLuong(), giuHang.getOrDefault(v.getId(), 0L)));
    }

    public Quote tinhTien(SalesRequest yeuCau) {
        if (yeuCau.getIdHoaDon() != null) hoaDonThuocNhanVien(yeuCau.getIdHoaDon(), false);
        LocalDateTime thoiDiem = LocalDateTime.now();
        Prepared duLieu;
        String loiPhieuDaChon = null;
        try {
            duLieu = chuanBiDuLieu(yeuCau, false, thoiDiem);
        } catch (AppException invalid) {
            if (yeuCau.getIdHoaDon() == null
                    || yeuCau.getMaPhieuGiamGia() == null
                    || yeuCau.getMaPhieuGiamGia().isBlank()) throw invalid;
            String ma = yeuCau.getMaPhieuGiamGia();
            yeuCau.setMaPhieuGiamGia(null);
            try {
                duLieu = chuanBiDuLieu(yeuCau, false, thoiDiem);
                loiPhieuDaChon = invalid.getMessage();
            } finally {
                yeuCau.setMaPhieuGiamGia(ma);
            }
        }
        var giaoHang =
                DeliveryRules.chuanHoa(
                        entityManager, yeuCau.getIdKhachHang(), yeuCau.getGiaoHang());
        List<VoucherOption> cacPhieuHopLe = new ArrayList<>();
        List<VoucherOption> phieuChuaDu = new ArrayList<>();
        Long idKhachDuocChon = duLieu.khachHang() == null ? null : duLieu.khachHang().getId();
        for (var phieuUngVien :
                entityManager
                        .createQuery(
                                "select p from PhieuGiamGia p where p.trangThai = 1 and"
                                        + " p.ngayBatDau <= :now and p.ngayKetThuc >= :now order by"
                                        + " p.id",
                                PhieuGiamGia.class)
                        .setParameter("now", thoiDiem)
                        .getResultList()) {
            if (phieuUngVien.getHinhThucPhieu() == null
                    || !Set.of(1, 2).contains(phieuUngVien.getHinhThucPhieu())) continue;
            if (Integer.valueOf(2).equals(phieuUngVien.getHinhThucPhieu())) {
                if (duLieu.khachHang() == null) continue;
                var links =
                        entityManager
                                .createQuery(
                                        "select a from PhieuGiamGiaKhachHang a where"
                                                + " a.idPhieuGiamGia.id = :id and a.trangThai = 1",
                                        PhieuGiamGiaKhachHang.class)
                                .setParameter("id", phieuUngVien.getId())
                                .getResultList();
                if (links.stream()
                        .noneMatch(
                                a ->
                                        a.getIdKhachHang() != null
                                                && Objects.equals(
                                                a.getIdKhachHang().getId(),
                                                idKhachDuocChon))) continue;
            }
            try {
                var valid =
                        kiemTraPhieu(
                                phieuUngVien.getMaPhieuGiamGia(),
                                duLieu.khachHang(),
                                duLieu.ketQuaTinhTien().getTongTienHang(),
                                false,
                                thoiDiem);
                if (valid.tienGiam().signum() > 0)
                    cacPhieuHopLe.add(
                            new VoucherOption(
                                    phieuUngVien.getMaPhieuGiamGia(),
                                    phieuUngVien.getTenPhieuGiamGia(),
                                    valid.tienGiam(),
                                    phieuUngVien.getNgayKetThuc(),
                                    phieuUngVien.getLoaiGiamGia(),
                                    phieuUngVien.getGiaTriGiam(),
                                    phieuUngVien.getHinhThucPhieu(),
                                    null));
            } catch (AppException invalidForCart) {
                phieuChuaDu.add(
                        new VoucherOption(
                                phieuUngVien.getMaPhieuGiamGia(),
                                phieuUngVien.getTenPhieuGiamGia(),
                                SO_KHONG,
                                phieuUngVien.getNgayKetThuc(),
                                phieuUngVien.getLoaiGiamGia(),
                                phieuUngVien.getGiaTriGiam(),
                                phieuUngVien.getHinhThucPhieu(),
                                invalidForCart.getMessage()));
            }
        }
        cacPhieuHopLe.sort(
                Comparator.comparing(VoucherOption::tienGiam)
                        .reversed()
                        .thenComparing(VoucherOption::maPhieuGiamGia));
        return new Quote(
                duLieu.ketQuaTinhTien().getTongTienHang(),
                duLieu.ketQuaTinhTien().getTienGiam(),
                duLieu.ketQuaTinhTien().getThanhTien().add(giaoHang.phiVanChuyen()),
                giaoHang.phiVanChuyen(),
                cacPhieuHopLe,
                loiPhieuDaChon)
                .phieuChuaDuDieuKien(phieuChuaDu);
    }

    @Transactional
    public Receipt thanhToan(SalesRequest yeuCau) {
        if (yeuCau.getIdHoaDon() != null) return thanhToanHoaDonCho(yeuCau);
        if (yeuCau.getDanhSachThanhToan() != null
                || yeuCau.getGiaoHang() != null
                || yeuCau.getPhienBan() != null)
            throw AppException.badRequest("Thanh toán giỏ chờ cần invoiceId");
        if (yeuCau.getTienKhachDua() == null || yeuCau.getTienKhachDua().signum() < 0) {
            throw AppException.badRequest("Số tiền thanh toán phải được cung cấp và không âm");
        }
        // Serialize request IDs without adding a table or relying on optional DB unique indexes.
        khoaThanhToan();
        String maGiaoDich = "POS-REQ-" + yeuCau.getMaYeuCau().toLowerCase(Locale.ROOT);
        List<LichSuThanhToan> existing =
                entityManager
                        .createQuery(
                                "select p from LichSuThanhToan p join fetch p.idHoaDon where"
                                        + " p.maGiaoDich = :code or p.maGiaoDich = :first",
                                LichSuThanhToan.class)
                        .setParameter("code", maGiaoDich)
                        .setParameter("first", maGiaoDich + "-1")
                        .setMaxResults(1)
                        .getResultList();
        if (!existing.isEmpty()) {
            HoaDon hoaDon = existing.get(0).getIdHoaDon();
            if (PosDraftRules.managed(hoaDon))
                throw AppException.conflict("requestId đã dùng cho thanh toán hóa đơn chờ");
            if (!List.of(0, 5, 8).contains(hoaDon.getTrangThai()))
                throw AppException.conflict("Hóa đơn đã được xử lý");
            kiemTraYeuCauLap(hoaDon, yeuCau);
            // Existing payment description stores tendered cash without changing the database
            // schema.
            String paidMarker = " | Khách đưa: " + soTien(yeuCau.getTienKhachDua()).toPlainString();
            if (existing.get(0).getMoTa() == null
                    || !existing.get(0).getMoTa().endsWith(paidMarker))
                throw AppException.conflict("Phiên thanh toán đã dùng với số tiền khách đưa khác");
            return chuyenKetQuaThanhToan(hoaDon, soTien(yeuCau.getTienKhachDua()));
        }
        LocalDateTime thoiDiem = LocalDateTime.now();
        Prepared duLieu = chuanBiDuLieu(yeuCau, true, thoiDiem);
        BigDecimal paid = soTien(yeuCau.getTienKhachDua());
        if (paid.compareTo(duLieu.ketQuaTinhTien().getThanhTien()) < 0)
            throw AppException.badRequest("Số tiền thanh toán chưa đủ");
        String maHoaDon = dichVuMaHoaDon.generateNextInvoiceCode();
        HoaDon hoaDon = new HoaDon();
        boolean walkInCustomer = duLieu.khachHang() == null;
        hoaDon.setMaHoaDon(maHoaDon);
        hoaDon.setLoaiHoaDon(0);
        hoaDon.setTrangThai(walkInCustomer ? 8 : 0);
        hoaDon.setIdKhachHang(duLieu.khachHang());
        hoaDon.setIdNhanVien(duLieu.nhanVien());
        hoaDon.setIdPhuongThucThanhToan(duLieu.phuongThuc());
        hoaDon.setIdPhieuGiamGia(duLieu.phieuGiamGia());
        hoaDon.setTongTien(duLieu.ketQuaTinhTien().getTongTienHang());
        hoaDon.setPhiVanChuyen(SO_KHONG);
        hoaDon.setTienGiamGia(duLieu.ketQuaTinhTien().getTienGiam());
        hoaDon.setThanhTien(duLieu.ketQuaTinhTien().getThanhTien());
        hoaDon.setHoTenNguoiNhan(
                duLieu.khachHang() == null ? "Khách lẻ" : duLieu.khachHang().getTenKhachHang());
        hoaDon.setSoDienThoaiNguoiNhan(
                duLieu.khachHang() == null ? null : duLieu.khachHang().getSoDienThoai());
        hoaDon.setGhiChu(yeuCau.getGhiChu());
        hoaDon.setNgayTao(thoiDiem);
        hoaDon.setNgayCapNhat(thoiDiem);
        hoaDon.setNgayThanhToan(thoiDiem);
        entityManager.persist(hoaDon);
        for (SalesRequest.Item item : yeuCau.getDanhSachSanPham()) {
            SanPhamChiTiet bienThe = duLieu.danhSachBienThe().get(item.getIdSanPhamChiTiet());
            HoaDonChiTiet chiTiet = new HoaDonChiTiet();
            chiTiet.setIdHoaDon(hoaDon);
            chiTiet.setIdSanPhamChiTiet(bienThe);
            chiTiet.setSoLuong(item.getSoLuong());
            BigDecimal soldPrice =
                    duLieu.bangGia().get(item.getIdSanPhamChiTiet()).getEffectivePrice();
            chiTiet.setDonGia(soldPrice);
            chiTiet.setThanhTien(soldPrice.multiply(BigDecimal.valueOf(item.getSoLuong())));
            chiTiet.setTrangThai(1);
            entityManager.persist(chiTiet);
            bienThe.setSoLuong(bienThe.getSoLuong() - item.getSoLuong());
            bienThe.setNgayCapNhat(thoiDiem);
        }
        if (duLieu.phieuGiamGia() != null) {
            duLieu.phieuGiamGia()
                    .setSoLuongDaDung(
                            Optional.ofNullable(duLieu.phieuGiamGia().getSoLuongDaDung()).orElse(0)
                                    + 1);
            duLieu.phieuGiamGia().setNgayCapNhat(thoiDiem);
            if (duLieu.phieuKhachHang() != null) duLieu.phieuKhachHang().setNgaySuDung(thoiDiem);
        }
        LichSuThanhToan phuongThuc = new LichSuThanhToan();
        phuongThuc.setIdHoaDon(hoaDon);
        phuongThuc.setIdPhuongThucThanhToan(duLieu.phuongThuc());
        phuongThuc.setSoTien(duLieu.ketQuaTinhTien().getThanhTien());
        phuongThuc.setMaGiaoDich(maGiaoDich);
        phuongThuc.setThoiGian(thoiDiem);
        phuongThuc.setTrangThai(1);
        phuongThuc.setMoTa(
                "Thanh toán tại quầy - "
                        + duLieu.phuongThuc().getTenPhuongThuc()
                        + " | Khách đưa: "
                        + paid.toPlainString());
        entityManager.persist(phuongThuc);
        LichSuHoaDon ghiLichSu = new LichSuHoaDon();
        ghiLichSu.setIdHoaDon(hoaDon);
        ghiLichSu.setNguoiTao(duLieu.nhanVien().getId());
        ghiLichSu.setTrangThai(walkInCustomer ? 8 : 0);
        ghiLichSu.setNgayTao(thoiDiem);
        ghiLichSu.setGhiChu(
                walkInCustomer
                        ? "Khách lẻ nhận tại quầy - hóa đơn chờ hoàn tất"
                        : "Khách hàng đã chọn - tạo đơn giao hàng tại quầy");
        entityManager.persist(ghiLichSu);
        entityManager.flush();
        return chuyenKetQuaThanhToan(hoaDon, paid);
    }

    private HoaDon hoaDonThuocNhanVien(Long id, boolean write) {
        Long nguoiThaoTac = PosActor.idNhanVien();
        var nhanVien = entityManager.find(NhanVien.class, nguoiThaoTac);
        if (nhanVien == null || !Integer.valueOf(1).equals(nhanVien.getTrangThai()))
            throw AppException.forbidden("Nhân viên không hoạt động");
        var h =
                entityManager.find(
                        HoaDon.class,
                        id,
                        write ? LockModeType.PESSIMISTIC_WRITE : LockModeType.PESSIMISTIC_READ);
        if (!PosDraftRules.managed(h)
                || h.getIdNhanVien() == null
                || !nguoiThaoTac.equals(h.getIdNhanVien().getId()))
            throw AppException.notFound("Không tìm thấy hóa đơn chờ của bạn");
        if (!Integer.valueOf(8).equals(h.getTrangThai()) || h.getNgayThanhToan() != null)
            throw AppException.conflict("Hóa đơn đã được xử lý; hãy kiểm tra chi tiết hóa đơn");
        return h;
    }

    private Receipt thanhToanHoaDonCho(SalesRequest yeuCau) {
        yeuCau.setIdNhanVien(PosActor.idNhanVien());
        if (yeuCau.getMaYeuCau() == null
                || !yeuCau.getMaYeuCau()
                .matches(
                        "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
            throw AppException.badRequest("requestId không hợp lệ");
        if (yeuCau.getPhienBan() == null
                || yeuCau.getGhiChu() != null && yeuCau.getGhiChu().length() > 980)
            throw AppException.badRequest("Cần revision hợp lệ và ghi chú tối đa 980 ký tự");
        if (yeuCau.getDanhSachThanhToan() == null
                || yeuCau.getDanhSachThanhToan().isEmpty()
                || yeuCau.getDanhSachThanhToan().size() > 2)
            throw AppException.badRequest("Thiếu các khoản thanh toán");
        if (yeuCau.getDanhSachSanPham() == null || yeuCau.getDanhSachSanPham().isEmpty())
            throw AppException.badRequest("Giỏ hàng trống");
        // Preserve the existing cross-instance transaction lock, then lock the invoice itself.
        khoaThanhToan();
        String base = "POS-REQ-" + yeuCau.getMaYeuCau().toLowerCase(Locale.ROOT);
        String maDoiChieu = "POS2:" + CheckoutFingerprint.taoMa(yeuCau);
        List<LichSuThanhToan> giaoDichTruoc =
                entityManager
                        .createQuery(
                                "select p from LichSuThanhToan p join fetch p.idHoaDon"
                                        + " where p.maGiaoDich in :codes order by p.id",
                                LichSuThanhToan.class)
                        .setParameter("codes", List.of(base, base + "-1", base + "-2"))
                        .getResultList();
        if (!giaoDichTruoc.isEmpty()) {
            var h = giaoDichTruoc.get(0).getIdHoaDon();
            var nguoiThaoTac = entityManager.find(NhanVien.class, yeuCau.getIdNhanVien());
            if (nguoiThaoTac == null
                    || !Integer.valueOf(1).equals(nguoiThaoTac.getTrangThai())
                    || h.getIdNhanVien() == null
                    || !Objects.equals(h.getIdNhanVien().getId(), yeuCau.getIdNhanVien())
                    || !Objects.equals(h.getId(), yeuCau.getIdHoaDon())
                    || giaoDichTruoc.size() != yeuCau.getDanhSachThanhToan().size()
                    || giaoDichTruoc.stream()
                    .anyMatch(
                            p ->
                                    !Objects.equals(p.getIdHoaDon().getId(), h.getId())
                                            || p.getMoTa() == null
                                            || !p.getMoTa().startsWith(maDoiChieu + " |")))
                throw AppException.conflict(
                        "requestId đã dùng cho một yêu cầu khác. Kiểm tra hóa đơn trước khi bán"
                                + " tiếp.");
            return ketQuaThanhToanHoaDonCho(h, giaoDichTruoc, yeuCau.getTienKhachDua());
        }
        HoaDon h = hoaDonThuocNhanVien(yeuCau.getIdHoaDon(), true);
        if (entityManager
                .createQuery(
                        "select count(p) from LichSuThanhToan p where p.idHoaDon.id = :id",
                        Long.class)
                .setParameter("id", h.getId())
                .getSingleResult()
                > 0) throw AppException.conflict("Hóa đơn đã có thanh toán");
        Long phienBan =
                entityManager
                        .createQuery(
                                "select max(l.id) from LichSuHoaDon l where l.idHoaDon.id = :id",
                                Long.class)
                        .setParameter("id", h.getId())
                        .getSingleResult();
        if (!Objects.equals(phienBan, yeuCau.getPhienBan()))
            throw AppException.conflict("Hóa đơn đã thay đổi; tải lại và xác nhận lại");
        var chiTietHoaDon =
                entityManager
                        .createQuery(
                                "select d from HoaDonChiTiet d where d.idHoaDon.id = :id order by"
                                        + " d.id",
                                HoaDonChiTiet.class)
                        .setParameter("id", h.getId())
                        .getResultList();
        var giaoHang =
                DeliveryRules.chuanHoa(
                        entityManager, yeuCau.getIdKhachHang(), yeuCau.getGiaoHang());
        boolean khopDuLieu =
                Objects.equals(
                        h.getIdKhachHang() == null ? null : h.getIdKhachHang().getId(),
                        yeuCau.getIdKhachHang())
                        && Objects.equals(
                        PosDraftRules.note(h), Objects.toString(yeuCau.getGhiChu(), ""))
                        && DeliveryRules.khopHoaDon(h, giaoHang)
                        && chiTietHoaDon.size() == yeuCau.getDanhSachSanPham().size()
                        && yeuCau.getDanhSachSanPham().stream()
                        .map(SalesRequest.Item::getIdSanPhamChiTiet)
                        .distinct()
                        .count()
                        == chiTietHoaDon.size();
        for (var item : yeuCau.getDanhSachSanPham())
            khopDuLieu &=
                    chiTietHoaDon.stream()
                            .anyMatch(
                                    d ->
                                            Objects.equals(
                                                    d.getIdSanPhamChiTiet().getId(),
                                                    item.getIdSanPhamChiTiet())
                                                    && Objects.equals(
                                                    d.getSoLuong(), item.getSoLuong())
                                                    && d.getDonGia().compareTo(item.getDonGia())
                                                    == 0);
        if (!khopDuLieu)
            throw AppException.conflict(
                    "Giỏ gửi lên khác hóa đơn đã lưu; hãy lưu và xác nhận lại trước thanh toán");
        yeuCau.setIdPhuongThucThanhToan(
                yeuCau.getDanhSachThanhToan().get(0).getIdPhuongThucThanhToan());
        LocalDateTime thoiDiem = LocalDateTime.now();
        Prepared duLieu = chuanBiDuLieu(yeuCau, true, thoiDiem);
        BigDecimal thanhTien = duLieu.ketQuaTinhTien().getThanhTien().add(giaoHang.phiVanChuyen());
        if (thanhTien.precision() - thanhTien.scale() > 16)
            throw AppException.badRequest("Giá trị hóa đơn vượt giới hạn lưu trữ");
        if (Integer.valueOf(2).equals(yeuCau.getPhienBanThanhToan())
                && yeuCau.getDanhSachThanhToan().size() != 1)
            throw AppException.badRequest(
                    "POS chỉ hỗ trợ một phương thức: tiền mặt hoặc chuyển khoản");
        PaymentPlan plan = PaymentPlan.lapKeHoach(entityManager, yeuCau, thanhTien);
        // Locks may have waited: a campaign/voucher can expire while waiting even without a data
        // edit.
        thoiDiem = LocalDateTime.now();
        if (duLieu.phieuGiamGia() != null
                && (thoiDiem.isBefore(duLieu.phieuGiamGia().getNgayBatDau())
                || thoiDiem.isAfter(duLieu.phieuGiamGia().getNgayKetThuc())))
            throw AppException.conflict(
                    "Phiếu giảm giá vừa hết hiệu lực; hãy xác nhận lại tổng tiền");
        var finalPrices = dichVuGiaHienTai.getPrices(duLieu.danhSachBienThe().values(), thoiDiem);
        for (var item : yeuCau.getDanhSachSanPham())
            if (finalPrices
                    .get(item.getIdSanPhamChiTiet())
                    .getEffectivePrice()
                    .compareTo(item.getDonGia())
                    != 0)
                throw AppException.conflict(
                        "Đợt giảm giá vừa thay đổi hiệu lực; hãy kiểm tra lại giá");
        for (var chiTiet : chiTietHoaDon) {
            var v = duLieu.danhSachBienThe().get(chiTiet.getIdSanPhamChiTiet().getId());
            BigDecimal giaBan = duLieu.bangGia().get(v.getId()).getEffectivePrice();
            chiTiet.setDonGia(giaBan);
            chiTiet.setThanhTien(giaBan.multiply(BigDecimal.valueOf(chiTiet.getSoLuong())));
            if (!PosDraftRules.daTruKho(h)) v.setSoLuong(v.getSoLuong() - chiTiet.getSoLuong());
            v.setNgayCapNhat(thoiDiem);
        }
        if (duLieu.phieuGiamGia() != null) {
            duLieu.phieuGiamGia()
                    .setSoLuongDaDung(
                            Optional.ofNullable(duLieu.phieuGiamGia().getSoLuongDaDung()).orElse(0)
                                    + 1);
            duLieu.phieuGiamGia().setNgayCapNhat(thoiDiem);
            if (duLieu.phieuKhachHang() != null) duLieu.phieuKhachHang().setNgaySuDung(thoiDiem);
        }
        h.setIdPhieuGiamGia(duLieu.phieuGiamGia());
        h.setIdPhuongThucThanhToan(
                plan.cacKhoanThu().size() == 1 ? plan.cacKhoanThu().get(0).phuongThuc() : null);
        h.setTongTien(duLieu.ketQuaTinhTien().getTongTienHang());
        h.setTienGiamGia(duLieu.ketQuaTinhTien().getTienGiam());
        h.setThanhTien(thanhTien);
        DeliveryRules.apDung(h, giaoHang, yeuCau.getGhiChu());
        h.setTrangThai(giaoHang.apDung() ? 0 : 5);
        h.setNgayThanhToan(thoiDiem);
        h.setNgayCapNhat(thoiDiem);
        List<LichSuThanhToan> danhSachThanhToan = new ArrayList<>();
        int index = 0;
        for (var part : plan.cacKhoanThu()) {
            var phuongThuc = new LichSuThanhToan();
            phuongThuc.setIdHoaDon(h);
            phuongThuc.setIdPhuongThucThanhToan(part.phuongThuc());
            phuongThuc.setSoTien(part.soTien());
            phuongThuc.setMaGiaoDich(base + "-" + (++index));
            phuongThuc.setThoiGian(thoiDiem);
            phuongThuc.setTrangThai(1);
            phuongThuc.setMoTa(
                    maDoiChieu
                            + " | Khách đưa: "
                            + soTien(yeuCau.getTienKhachDua()).toPlainString()
                            + ("CHUYEN_KHOAN".equals(part.phuongThuc().getMaPhuongThuc())
                            ? " | Nhân viên xác nhận chuyển khoản thủ công"
                            : ""));
            entityManager.persist(phuongThuc);
            danhSachThanhToan.add(phuongThuc);
        }
        // Persist immutable prices in existing audit rows in this same checkout transaction.
        for (var chiTiet : chiTietHoaDon) {
            var giaBan = finalPrices.get(chiTiet.getIdSanPhamChiTiet().getId());
            luuDuLieuGiaLichSu(
                    h,
                    duLieu.nhanVien().getId(),
                    thoiDiem,
                    PosPriceSnapshot.line(
                            chiTiet.getIdSanPhamChiTiet().getId(),
                            giaBan.getOriginalPrice(),
                            chiTiet.getDonGia(),
                            giaBan.getDiscountPercent() == null
                                    ? BigDecimal.ZERO
                                    : giaBan.getDiscountPercent()));
        }
        if (duLieu.phieuGiamGia() != null)
            luuDuLieuGiaLichSu(
                    h,
                    duLieu.nhanVien().getId(),
                    thoiDiem,
                    PosPriceSnapshot.voucher(
                            duLieu.phieuGiamGia().getMaPhieuGiamGia(),
                            duLieu.phieuGiamGia().getLoaiGiamGia(),
                            duLieu.phieuGiamGia().getGiaTriGiam()));
        var ghiLichSu = new LichSuHoaDon();
        ghiLichSu.setIdHoaDon(h);
        ghiLichSu.setNguoiTao(duLieu.nhanVien().getId());
        ghiLichSu.setTrangThai(h.getTrangThai());
        ghiLichSu.setNgayTao(thoiDiem);
        ghiLichSu.setGhiChu(
                com.smashstep.datn.invoice.rules.PosHistoryRules.HOAN_TAT
                        + (giaoHang.apDung()
                        ? "Đã thanh toán tại POS - giao hàng chờ xác nhận"
                        : "Đã thanh toán và nhận hàng tại quầy"));
        entityManager.persist(ghiLichSu);
        entityManager.flush();
        return ketQuaThanhToanHoaDonCho(h, danhSachThanhToan, yeuCau.getTienKhachDua());
    }

    private void luuDuLieuGiaLichSu(
            HoaDon hoaDon, Long nguoiThaoTac, LocalDateTime time, String ghiChu) {
        if (ghiChu.length() > 1000)
            throw AppException.badRequest("Thông tin giá vượt giới hạn lưu trữ");
        var dong = new LichSuHoaDon();
        dong.setIdHoaDon(hoaDon);
        dong.setNguoiTao(nguoiThaoTac);
        dong.setTrangThai(hoaDon.getTrangThai());
        dong.setNgayTao(time);
        dong.setGhiChu(ghiChu);
        entityManager.persist(dong);
    }

    private Receipt ketQuaThanhToanHoaDonCho(
            HoaDon h, List<LichSuThanhToan> danhSachThanhToan, BigDecimal tienMatKhachDua) {
        BigDecimal tienDaChuyen =
                danhSachThanhToan.stream()
                        .filter(
                                p ->
                                        "CHUYEN_KHOAN"
                                                .equals(
                                                        p.getIdPhuongThucThanhToan()
                                                                .getMaPhuongThuc()))
                        .map(LichSuThanhToan::getSoTien)
                        .reduce(SO_KHONG, BigDecimal::add);
        var cacKhoanThu =
                danhSachThanhToan.stream()
                        .map(
                                p ->
                                        new ReceiptPayment(
                                                p.getIdPhuongThucThanhToan().getId(),
                                                p.getIdPhuongThucThanhToan().getMaPhuongThuc(),
                                                p.getIdPhuongThucThanhToan().getTenPhuongThuc(),
                                                p.getSoTien(),
                                                p.getMaGiaoDich()))
                        .toList();
        return chuyenKetQuaThanhToan(h, soTien(tienMatKhachDua).add(tienDaChuyen))
                .ghiNhanThanhToan(cacKhoanThu, h.getPhiVanChuyen(), h.getTrangThai());
    }

    private Prepared chuanBiDuLieu(SalesRequest yeuCau, boolean khoa, LocalDateTime thoiDiem) {
        // Share the campaign module's existing transaction lock before reading final selling
        // prices.
        if (khoa && khoaGhiSanPham != null) khoaGhiSanPham.khoa("campaigns");
        NhanVien nhanVien = entityManager.find(NhanVien.class, yeuCau.getIdNhanVien());
        if (nhanVien == null || !Integer.valueOf(1).equals(nhanVien.getTrangThai()))
            throw AppException.badRequest("Nhân viên không hoạt động");
        KhachHang khachHang =
                yeuCau.getIdKhachHang() == null
                        ? null
                        : entityManager.find(KhachHang.class, yeuCau.getIdKhachHang());
        if (yeuCau.getIdKhachHang() != null
                && (khachHang == null || !Integer.valueOf(1).equals(khachHang.getTrangThai()))) {
            throw AppException.badRequest("Khách hàng không hoạt động");
        }
        PhuongThucThanhToan phuongThuc =
                yeuCau.getIdPhuongThucThanhToan() == null
                        ? null
                        : entityManager.find(
                        PhuongThucThanhToan.class, yeuCau.getIdPhuongThucThanhToan());
        if (phuongThuc == null
                || !Integer.valueOf(1).equals(phuongThuc.getTrangThai())
                || !Set.of("TIEN_MAT", "CHUYEN_KHOAN", "THE")
                .contains(phuongThuc.getMaPhuongThuc())) {
            throw AppException.badRequest("Phương thức thanh toán không hợp lệ tại quầy");
        }
        Map<Long, Integer> daTruCuaDon = Map.of();
        if (yeuCau.getIdHoaDon() != null) {
            var draft = hoaDonThuocNhanVien(yeuCau.getIdHoaDon(), false);
            if (PosDraftRules.daTruKho(draft)) daTruCuaDon = TonKhoHoaDonCho.soLuong(entityManager.createQuery(
                            "select d from HoaDonChiTiet d where d.idHoaDon.id = :id", HoaDonChiTiet.class)
                    .setParameter("id", draft.getId()).getResultList());
        }
        Map<Long, SanPhamChiTiet> danhSachBienThe = new LinkedHashMap<>();
        BigDecimal tongTienHang = SO_KHONG;
        for (SalesRequest.Item item :
                yeuCau.getDanhSachSanPham().stream()
                        .sorted(Comparator.comparing(SalesRequest.Item::getIdSanPhamChiTiet))
                        .toList()) {
            if (danhSachBienThe.containsKey(item.getIdSanPhamChiTiet()))
                throw AppException.badRequest("Biến thể bị trùng trong giỏ hàng");
            SanPhamChiTiet bienThe =
                    entityManager.find(SanPhamChiTiet.class, item.getIdSanPhamChiTiet());
            if (bienThe == null) throw AppException.notFound("Không tìm thấy biến thể");
            if (bienThe.getIdSanPham() == null)
                throw AppException.badRequest("Biến thể thiếu sản phẩm");
            if (khoa) {
                // Product edits already lock the parent; use the same order, then reload stock.
                entityManager.lock(bienThe.getIdSanPham(), LockModeType.PESSIMISTIC_WRITE);
                entityManager.refresh(bienThe.getIdSanPham(), LockModeType.PESSIMISTIC_WRITE);
                entityManager.refresh(bienThe, LockModeType.PESSIMISTIC_WRITE);
            }
            if (!Integer.valueOf(1).equals(bienThe.getTrangThai())
                    || !Boolean.TRUE.equals(bienThe.getKichHoat())
                    || bienThe.getIdSanPham() == null
                    || !Integer.valueOf(1).equals(bienThe.getIdSanPham().getTrangThai())) {
                throw AppException.badRequest("Biến thể không còn được bán");
            }
            if (bienThe.getSoLuong() == null || ((long) bienThe.getSoLuong() + daTruCuaDon.getOrDefault(bienThe.getId(), 0)) < item.getSoLuong())
                throw AppException.conflict("Tồn kho không đủ");
            danhSachBienThe.put(bienThe.getId(), bienThe);
        }
        var giuKhac =
                GiuHangTaiQuay.daGiu(entityManager, danhSachBienThe.keySet(), yeuCau.getIdHoaDon());
        for (var item : yeuCau.getDanhSachSanPham()) {
            if (item.getSoLuong()
                    > GiuHangTaiQuay.khaDung(
                    danhSachBienThe.get(item.getIdSanPhamChiTiet()).getSoLuong() + daTruCuaDon.getOrDefault(item.getIdSanPhamChiTiet(), 0),
                    giuKhac.getOrDefault(item.getIdSanPhamChiTiet(), 0L)))
                throw AppException.conflict(
                        "Không đủ tồn khả dụng; sản phẩm đang được giữ bởi hóa đơn khác");
        }
        if (khoa) thoiDiem = LocalDateTime.now();
        var bangGia = dichVuGiaHienTai.getPrices(danhSachBienThe.values(), thoiDiem);
        for (SalesRequest.Item item : yeuCau.getDanhSachSanPham()) {
            BigDecimal giaBan = bangGia.get(item.getIdSanPhamChiTiet()).getEffectivePrice();
            if (soTien(danhSachBienThe.get(item.getIdSanPhamChiTiet()).getGiaBan()).signum() <= 0
                    || giaBan.compareTo(item.getDonGia()) != 0)
                throw AppException.conflict(
                        "Giá bán đã thay đổi. Hãy tải lại sản phẩm và xác nhận giá mới.");
            tongTienHang = tongTienHang.add(giaBan.multiply(BigDecimal.valueOf(item.getSoLuong())));
        }
        if (tongTienHang.precision() - tongTienHang.scale() > 16)
            throw AppException.badRequest("Tổng tiền hàng vượt giới hạn lưu trữ");
        var ketQuaPhieu =
                kiemTraPhieu(yeuCau.getMaPhieuGiamGia(), khachHang, tongTienHang, khoa, thoiDiem);
        return new Prepared(
                khachHang,
                nhanVien,
                phuongThuc,
                ketQuaPhieu.phieuGiamGia(),
                ketQuaPhieu.phieuKhachHang(),
                danhSachBienThe,
                bangGia,
                new Quote(
                        tongTienHang,
                        ketQuaPhieu.tienGiam(),
                        tongTienHang.subtract(ketQuaPhieu.tienGiam())));
    }

    private record VoucherEvaluation(
            PhieuGiamGia phieuGiamGia, PhieuGiamGiaKhachHang phieuKhachHang, BigDecimal tienGiam) {}

    public record DraftVoucher(PhieuGiamGia phieuGiamGia, BigDecimal tienGiam) {}

    public DraftVoucher tinhThuPhieu(KhachHang khachHang, BigDecimal tongTienHang, String ma) {
        var ketQua = kiemTraPhieu(ma, khachHang, tongTienHang, false, LocalDateTime.now());
        return new DraftVoucher(ketQua.phieuGiamGia(), ketQua.tienGiam());
    }

    private VoucherEvaluation kiemTraPhieu(
            String ma,
            KhachHang khachHang,
            BigDecimal tongTienHang,
            boolean khoa,
            LocalDateTime thoiDiem) {
        PhieuGiamGia phieuGiamGia = null;
        PhieuGiamGiaKhachHang phieuKhachHang = null;
        BigDecimal tienGiam = SO_KHONG;
        if (ma != null && !ma.isBlank()) {
            var query =
                    entityManager
                            .createQuery(
                                    "select p from PhieuGiamGia p where lower(p.maPhieuGiamGia) ="
                                            + " :code",
                                    PhieuGiamGia.class)
                            .setParameter("code", ma.trim().toLowerCase(Locale.ROOT));
            if (khoa) query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
            List<PhieuGiamGia> khopDuLieu = query.getResultList();
            if (khopDuLieu.size() != 1)
                throw AppException.badRequest("Mã phiếu giảm giá không hợp lệ");
            phieuGiamGia = khopDuLieu.get(0);
            if (khoa) entityManager.refresh(phieuGiamGia, LockModeType.PESSIMISTIC_WRITE);
            if (khoa) thoiDiem = LocalDateTime.now();
            if ((phieuGiamGia.getSoLuongDaDung() != null && phieuGiamGia.getSoLuongDaDung() < 0)
                    || (phieuGiamGia.getSoLuong() != null && phieuGiamGia.getSoLuong() <= 0)
                    || (phieuGiamGia.getGiaTriToiThieu() != null
                    && phieuGiamGia.getGiaTriToiThieu().signum() < 0)
                    || (phieuGiamGia.getGiamToiDa() != null
                    && phieuGiamGia.getGiamToiDa().signum() < 0)) {
                throw AppException.badRequest("Phiếu có số lượng hoặc giới hạn giảm không hợp lệ");
            }
            if (!Integer.valueOf(1).equals(phieuGiamGia.getTrangThai())
                    || phieuGiamGia.getNgayBatDau() == null
                    || phieuGiamGia.getNgayKetThuc() == null
                    || thoiDiem.isBefore(phieuGiamGia.getNgayBatDau())
                    || thoiDiem.isAfter(phieuGiamGia.getNgayKetThuc())) {
                throw AppException.badRequest("Phiếu giảm giá chưa có hiệu lực hoặc đã hết hạn");
            }
            if (phieuGiamGia.getSoLuong() != null
                    && Optional.ofNullable(phieuGiamGia.getSoLuongDaDung()).orElse(0)
                    >= phieuGiamGia.getSoLuong()) {
                throw AppException.conflict("Phiếu giảm giá đã hết lượt sử dụng");
            }
            List<PhieuGiamGiaKhachHang> danhSachPhieuKhachHang =
                    entityManager
                            .createQuery(
                                    "select a from PhieuGiamGiaKhachHang a where"
                                            + " a.idPhieuGiamGia.id = :id and a.trangThai = 1",
                                    PhieuGiamGiaKhachHang.class)
                            .setParameter("id", phieuGiamGia.getId())
                            .getResultList();
            if (!Set.of(1, 2)
                    .contains(
                            phieuGiamGia.getHinhThucPhieu() == null
                                    ? 0
                                    : phieuGiamGia.getHinhThucPhieu())) {
                throw AppException.badRequest("Phiếu chưa được cấu hình hình thức áp dụng hợp lệ");
            }
            if (Integer.valueOf(2).equals(phieuGiamGia.getHinhThucPhieu())) {
                phieuKhachHang =
                        danhSachPhieuKhachHang.stream()
                                .filter(
                                        a ->
                                                khachHang != null
                                                        && a.getIdKhachHang()
                                                        .getId()
                                                        .equals(khachHang.getId()))
                                .findFirst()
                                .orElseThrow(
                                        () ->
                                                AppException.badRequest(
                                                        "Phiếu cá nhân không thuộc khách hàng"
                                                                + " này"));
                if (khoa) entityManager.refresh(phieuKhachHang, LockModeType.PESSIMISTIC_WRITE);
                if (!Integer.valueOf(1).equals(phieuKhachHang.getTrangThai())
                        || phieuKhachHang.getIdKhachHang() == null
                        || !Objects.equals(
                        phieuKhachHang.getIdKhachHang().getId(), khachHang.getId())
                        || phieuKhachHang.getIdPhieuGiamGia() == null
                        || !Objects.equals(
                        phieuKhachHang.getIdPhieuGiamGia().getId(), phieuGiamGia.getId()))
                    throw AppException.conflict("Quyền sử dụng phiếu cá nhân đã thay đổi");
                if (phieuKhachHang.getNgaySuDung() != null)
                    throw AppException.conflict("Khách hàng đã sử dụng phiếu này");
            }
            if (tongTienHang.compareTo(soTien(phieuGiamGia.getGiaTriToiThieu())) < 0)
                throw AppException.badRequest("Đơn hàng chưa đạt giá trị tối thiểu của phiếu");
            if (phieuGiamGia.getGiaTriGiam() == null
                    || phieuGiamGia.getGiaTriGiam().signum() <= 0
                    || (Integer.valueOf(1).equals(phieuGiamGia.getLoaiGiamGia())
                    && phieuGiamGia.getGiaTriGiam().compareTo(new BigDecimal("100")) > 0)) {
                throw AppException.badRequest("Phiếu giảm giá không hợp lệ");
            }
            if (Integer.valueOf(1).equals(phieuGiamGia.getLoaiGiamGia())) {
                tienGiam =
                        tongTienHang
                                .multiply(phieuGiamGia.getGiaTriGiam().min(new BigDecimal("100")))
                                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                if (phieuGiamGia.getGiamToiDa() != null && phieuGiamGia.getGiamToiDa().signum() > 0)
                    tienGiam = tienGiam.min(phieuGiamGia.getGiamToiDa());
            } else if (Integer.valueOf(2).equals(phieuGiamGia.getLoaiGiamGia()))
                tienGiam = phieuGiamGia.getGiaTriGiam();
            else throw AppException.badRequest("Loại giảm giá không hợp lệ");
            tienGiam = tienGiam.min(tongTienHang).setScale(2, RoundingMode.HALF_UP);
        }
        return new VoucherEvaluation(phieuGiamGia, phieuKhachHang, tienGiam);
    }

    private CatalogItem chuyenSanPhamBan(SanPhamChiTiet v, CurrentPrice giaBan) {
        return new CatalogItem(
                v.getId(),
                v.getMaChiTietSanPham(),
                v.getIdSanPham().getMaSanPham(),
                v.getIdSanPham().getTenSanPham(),
                v.getIdMauSac() == null ? "—" : v.getIdMauSac().getTenMauSac(),
                v.getIdKichThuoc() == null ? "—" : v.getIdKichThuoc().getGiaTri(),
                giaBan.getEffectivePrice(),
                giaBan.getOriginalPrice(),
                v.getSoLuong(),
                giaBan.isDiscounted(),
                giaBan.getDiscountPercent(),
                giaBan.getCampaignCode(),
                giaBan.getCampaignName())
                .ganIdSanPham(v.getIdSanPham().getId());
    }

    private Receipt chuyenKetQuaThanhToan(HoaDon hoaDon, BigDecimal paid) {
        var nhanVien = hoaDon.getIdNhanVien();
        var khachHang = hoaDon.getIdKhachHang();
        var method = hoaDon.getIdPhuongThucThanhToan();
        return new Receipt(
                hoaDon.getId(),
                hoaDon.getMaHoaDon(),
                hoaDon.getTongTien(),
                hoaDon.getTienGiamGia(),
                hoaDon.getThanhTien(),
                paid.subtract(hoaDon.getThanhTien()),
                nhanVien.getId(),
                nhanVien.getMaNhanVien(),
                nhanVien.getTenNhanVien(),
                khachHang == null ? null : khachHang.getId(),
                khachHang == null ? null : khachHang.getMaKhachHang(),
                hoaDon.getHoTenNguoiNhan(),
                hoaDon.getSoDienThoaiNguoiNhan(),
                method == null ? "KET_HOP" : method.getMaPhuongThuc(),
                method == null ? "Kết hợp" : method.getTenPhuongThuc(),
                hoaDon.getIdPhieuGiamGia() == null
                        ? null
                        : hoaDon.getIdPhieuGiamGia().getMaPhieuGiamGia(),
                hoaDon.getNgayTao(),
                paid);
    }

    private void khoaThanhToan() {
        dichVuMaHoaDon.lockInvoiceCreation();
    }

    private void kiemTraYeuCauLap(HoaDon hoaDon, SalesRequest yeuCau) {
        List<HoaDonChiTiet> chiTietHoaDon =
                entityManager
                        .createQuery(
                                "select d from HoaDonChiTiet d where d.idHoaDon.id = :id",
                                HoaDonChiTiet.class)
                        .setParameter("id", hoaDon.getId())
                        .getResultList();
        boolean same =
                Objects.equals(
                        hoaDon.getIdKhachHang() == null
                                ? null
                                : hoaDon.getIdKhachHang().getId(),
                        yeuCau.getIdKhachHang())
                        && Objects.equals(hoaDon.getIdNhanVien().getId(), yeuCau.getIdNhanVien())
                        && Objects.equals(
                        hoaDon.getIdPhuongThucThanhToan().getId(),
                        yeuCau.getIdPhuongThucThanhToan())
                        && Objects.equals(
                        hoaDon.getIdPhieuGiamGia() == null
                                ? ""
                                : hoaDon.getIdPhieuGiamGia()
                                .getMaPhieuGiamGia()
                                .toLowerCase(Locale.ROOT),
                        yeuCau.getMaPhieuGiamGia() == null
                                ? ""
                                : yeuCau.getMaPhieuGiamGia()
                                .trim()
                                .toLowerCase(Locale.ROOT))
                        && Objects.equals(hoaDon.getGhiChu(), yeuCau.getGhiChu())
                        && chiTietHoaDon.size() == yeuCau.getDanhSachSanPham().size()
                        && yeuCau.getDanhSachSanPham().stream()
                        .map(SalesRequest.Item::getIdSanPhamChiTiet)
                        .distinct()
                        .count()
                        == chiTietHoaDon.size();
        for (SalesRequest.Item item : yeuCau.getDanhSachSanPham()) {
            same &=
                    chiTietHoaDon.stream()
                            .anyMatch(
                                    d ->
                                            Objects.equals(
                                                    d.getIdSanPhamChiTiet().getId(),
                                                    item.getIdSanPhamChiTiet())
                                                    && Objects.equals(
                                                    d.getSoLuong(), item.getSoLuong())
                                                    && d.getDonGia().compareTo(item.getDonGia())
                                                    == 0);
        }
        if (!same || soTien(yeuCau.getTienKhachDua()).compareTo(hoaDon.getThanhTien()) < 0) {
            throw AppException.conflict(
                    "Phiên thanh toán đã dùng cho hóa đơn "
                            + hoaDon.getMaHoaDon()
                            + ". Hãy kiểm tra hóa đơn trước khi tạo giỏ hàng mới");
        }
    }

    private static BigDecimal soTien(BigDecimal value) {
        return value == null ? SO_KHONG : value.setScale(2, RoundingMode.HALF_UP);
    }

    private record Prepared(
            KhachHang khachHang,
            NhanVien nhanVien,
            PhuongThucThanhToan phuongThuc,
            PhieuGiamGia phieuGiamGia,
            PhieuGiamGiaKhachHang phieuKhachHang,
            Map<Long, SanPhamChiTiet> danhSachBienThe,
            Map<Long, CurrentPrice> bangGia,
            Quote ketQuaTinhTien) {}
}
