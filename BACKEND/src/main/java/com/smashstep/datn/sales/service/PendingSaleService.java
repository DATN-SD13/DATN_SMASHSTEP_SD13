package com.smashstep.datn.sales.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.pricing.CurrentPriceService;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.invoice.entity.*;
import com.smashstep.datn.invoice.rules.PosDraftRules;
import com.smashstep.datn.invoice.service.InvoiceCodeService;
import com.smashstep.datn.product.entity.SanPhamChiTiet;
import com.smashstep.datn.sales.dto.PendingSaleRequest;
import com.smashstep.datn.sales.dto.SalesDelivery;

import jakarta.persistence.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PendingSaleService {
    @org.springframework.beans.factory.annotation.Autowired private GhnPhiVanChuyenService phiGhn;
    private final EntityManager entityManager;
    private final InvoiceCodeService dichVuMaHoaDon;
    private final CurrentPriceService dichVuGiaHienTai;
    private final SalesService salesService;
    private static final BigDecimal SO_KHONG = new BigDecimal("0.00");
    private static final String DIEU_KIEN_HOA_DON_CHO =
            " from HoaDon h where h.idNhanVien.id = :employee and h.loaiHoaDon = 0 and h.trangThai"
                    + " = 8 and h.ngayThanhToan is null and (locate(:marker, h.ghiChu) = 1 or"
                    + " locate(:deliveryMarker, h.ghiChu) = 1) and not exists (select p.id from"
                    + " LichSuThanhToan p where p.idHoaDon = h)";

    public List<Draft> danhSachHoaDonCho() {
        var nhanVien = nhanVien();
        return entityManager
                .createQuery("select h" + DIEU_KIEN_HOA_DON_CHO + " order by h.id", HoaDon.class)
                .setParameter("employee", nhanVien.getId())
                .setParameter("marker", PosDraftRules.PREFIX)
                .setParameter("deliveryMarker", PosDraftRules.DELIVERY_PREFIX)
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .getResultList()
                .stream()
                .map(this::chuyenDuLieuHoaDonCho)
                .toList();
    }

    @Transactional
    public List<Draft> dongBoHoaDonCho() {
        var employee = nhanVien();
        dichVuMaHoaDon.lockInvoiceCreation();
        var drafts = entityManager.createQuery("select h" + DIEU_KIEN_HOA_DON_CHO + " order by h.id", HoaDon.class)
                .setParameter("employee", employee.getId()).setParameter("marker", PosDraftRules.PREFIX)
                .setParameter("deliveryMarker", PosDraftRules.DELIVERY_PREFIX)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
        for (var h : drafts) {
            var rows = chiTietHoaDon(h);
            var cu = TonKhoHoaDonCho.soLuong(rows);
            var variants = TonKhoHoaDonCho.khoaBienThe(entityManager, cu.keySet());
            var removed = rows.stream().filter(d -> d.getIdSanPhamChiTiet() == null
                    || !duocPhepBan(variants.get(d.getIdSanPhamChiTiet().getId()))).toList();
            var remaining = new ArrayList<>(rows); remaining.removeAll(removed);
            boolean migrated = false;
            if (!PosDraftRules.daTruKho(h)) {
                var wanted = TonKhoHoaDonCho.soLuong(remaining);
                var legacyOthers = GiuHangTaiQuay.daGiu(entityManager, wanted.keySet(), h.getId());
                boolean enough = wanted.entrySet().stream().allMatch(e -> variants.containsKey(e.getKey())
                        && e.getValue() <= GiuHangTaiQuay.khaDung(variants.get(e.getKey()).getSoLuong(), legacyOthers.getOrDefault(e.getKey(), 0L)));
                if (enough) { TonKhoHoaDonCho.capNhat(h, cu, wanted, variants); migrated = true; }
            } else if (!removed.isEmpty()) TonKhoHoaDonCho.capNhat(h, cu, TonKhoHoaDonCho.soLuong(remaining), variants);
            if (removed.isEmpty()) {
                if (migrated) { h.setNgayCapNhat(LocalDateTime.now()); ghiLichSu(h, "Chuyển giỏ cũ sang trừ tồn kho thực tế"); }
                continue;
            }
            removed.forEach(entityManager::remove);
            BigDecimal total = remaining.stream().map(d -> d.getDonGia().multiply(BigDecimal.valueOf(d.getSoLuong())))
                    .reduce(SO_KHONG, BigDecimal::add);
            // Selection is recalculated by POS after synchronization; never reuse a voucher on a changed cart.
            h.setIdPhieuGiamGia(null); h.setTienGiamGia(SO_KHONG); h.setTongTien(total);
            h.setThanhTien(total.add(h.getPhiVanChuyen() == null ? SO_KHONG : h.getPhiVanChuyen()));
            h.setNgayCapNhat(LocalDateTime.now());
            ghiLichSu(h, "Tự loại sản phẩm ngừng kinh doanh khỏi giỏ và hoàn tồn đã trừ");
        }
        entityManager.flush();
        return drafts.stream().map(this::chuyenDuLieuHoaDonCho).toList();
    }

    public Draft layHoaDonCho(Long id) {
        return chuyenDuLieuHoaDonCho(hoaDonThuocNhanVien(id, false));
    }

    @Transactional
    public Draft taoHoaDonCho() {
        var nhanVien = nhanVien();
        // Same transaction-scoped SQL Server lock as all existing invoice creation paths.
        // Count happens AFTER acquiring it, so concurrent requests cannot both take the fifth slot.
        dichVuMaHoaDon.lockInvoiceCreation();
        long soLuongKetQua =
                entityManager
                        .createQuery("select count(h)" + DIEU_KIEN_HOA_DON_CHO, Long.class)
                        .setParameter("employee", nhanVien.getId())
                        .setParameter("marker", PosDraftRules.PREFIX)
                        .setParameter("deliveryMarker", PosDraftRules.DELIVERY_PREFIX)
                        .getSingleResult();
        if (soLuongKetQua >= 5)
            throw AppException.conflict("Mỗi nhân viên chỉ được mở tối đa 5 hóa đơn chờ");
        var h = new HoaDon();
        h.setMaHoaDon(dichVuMaHoaDon.generateNextInvoiceCode());
        h.setLoaiHoaDon(0);
        h.setTrangThai(8);
        h.setIdNhanVien(nhanVien);
        h.setHoTenNguoiNhan("Khách lẻ");
        h.setGhiChu(PosDraftRules.PREFIX + PosDraftRules.TON_KHO_THUC);
        h.setTongTien(SO_KHONG);
        h.setThanhTien(SO_KHONG);
        h.setTienGiamGia(SO_KHONG);
        h.setPhiVanChuyen(SO_KHONG);
        h.setNgayTao(LocalDateTime.now());
        h.setNgayCapNhat(h.getNgayTao());
        entityManager.persist(h);
        ghiLichSu(h, "Tạo hóa đơn chờ tại quầy (chưa thanh toán)");
        entityManager.flush();
        return chuyenDuLieuHoaDonCho(h);
    }

    @Transactional
    public Draft capNhatHoaDonCho(Long id, PendingSaleRequest yeuCau) {
        dichVuMaHoaDon.lockInvoiceCreation();
        var h = hoaDonThuocNhanVien(id, true);
        if (!Objects.equals(phienBan(h), yeuCau.phienBan()))
            throw AppException.conflict(
                    "Hóa đơn đã thay đổi ở phiên khác. Tải lại hóa đơn trước khi sửa.");
        KhachHang khachHang =
                yeuCau.idKhachHang() == null
                        ? null
                        : entityManager.find(KhachHang.class, yeuCau.idKhachHang());
        if (yeuCau.idKhachHang() != null
                && (khachHang == null || !Integer.valueOf(1).equals(khachHang.getTrangThai())))
            throw AppException.badRequest("Khách hàng không hoạt động");
        var giaoHang =
                DeliveryRules.chuanHoa(entityManager, yeuCau.idKhachHang(), yeuCau.giaoHang());
        if (giaoHang.apDung() && !yeuCau.danhSachSanPham().isEmpty()) phiGhn.xacThuc(giaoHang, yeuCau.danhSachSanPham().stream()
                .mapToInt(i -> i.soLuong().intValueExact()).sum());
        if (giaoHang.apDung() && yeuCau.danhSachSanPham().isEmpty())
            giaoHang = new SalesDelivery(true, null, giaoHang.tenNguoiNhan(),
                    giaoHang.soDienThoaiNguoiNhan(), giaoHang.diaChi(), BigDecimal.ZERO);
        var chiTietCu = chiTietHoaDon(h);
        Map<Long, Integer> soLuongCu = new HashMap<>();
        for (var dongCu : chiTietCu) {
            if (dongCu.getIdSanPhamChiTiet() != null && dongCu.getSoLuong() != null)
                soLuongCu.merge(
                        dongCu.getIdSanPhamChiTiet().getId(), dongCu.getSoLuong(), Integer::sum);
        }
        var idsCanKhoa = new TreeSet<Long>(soLuongCu.keySet());
        yeuCau.danhSachSanPham().forEach(i -> idsCanKhoa.add(i.idSanPhamChiTiet()));
        var tatCaBienThe = TonKhoHoaDonCho.khoaBienThe(entityManager, idsCanKhoa);
        boolean daTruKho = PosDraftRules.daTruKho(h);
        Map<Long, SanPhamChiTiet> danhSachBienThe = new LinkedHashMap<>();
        for (var item :
                yeuCau.danhSachSanPham().stream()
                        .sorted(Comparator.comparing(PendingSaleRequest.Item::idSanPhamChiTiet))
                        .toList()) {
            int soLuong;
            try {
                soLuong = item.soLuong().intValueExact();
            } catch (ArithmeticException ex) {
                throw AppException.badRequest("Số lượng phải là số nguyên");
            }
            if (soLuong < 1 || soLuong > 10000)
                throw AppException.badRequest("Số lượng phải từ 1 đến 10000");
            if (danhSachBienThe.containsKey(item.idSanPhamChiTiet()))
                throw AppException.badRequest("Biến thể bị trùng");
            var v = tatCaBienThe.get(item.idSanPhamChiTiet());
            if (!duocPhepBan(v)) throw AppException.conflict("Biến thể không còn được bán; hãy đồng bộ lại giỏ");
            danhSachBienThe.put(v.getId(), v);
        }
        var giuKhac = GiuHangTaiQuay.daGiu(entityManager, danhSachBienThe.keySet(), h.getId());
        for (var item : yeuCau.danhSachSanPham()) {
            if (!GiuHangTaiQuay.choPhepLuu(
                    item.soLuong().intValueExact(),
                    0,
                    GiuHangTaiQuay.khaDung(
                            danhSachBienThe.get(item.idSanPhamChiTiet()).getSoLuong()
                                    + (daTruKho ? soLuongCu.getOrDefault(item.idSanPhamChiTiet(), 0) : 0),
                            giuKhac.getOrDefault(item.idSanPhamChiTiet(), 0L))))
                throw AppException.conflict(
                        "Không đủ tồn khả dụng; sản phẩm đang được giữ bởi hóa đơn khác");
        }
        var hienTai = dichVuGiaHienTai.getPrices(danhSachBienThe.values(), LocalDateTime.now());
        BigDecimal thanhTien = SO_KHONG;
        for (var item : yeuCau.danhSachSanPham()) {
            var giaBan = hienTai.get(item.idSanPhamChiTiet()).getEffectivePrice();
            if (giaBan.compareTo(item.donGia()) != 0)
                throw AppException.conflict(
                        "Giá đã thay đổi; kiểm tra lại giá và tồn kho trước khi lưu");
            thanhTien = thanhTien.add(giaBan.multiply(item.soLuong()));
        }
        if (thanhTien.precision() - thanhTien.scale() > 16)
            throw AppException.badRequest("Tổng tiền hàng vượt giới hạn lưu trữ");
        var phieuGiamGia =
                yeuCau.maPhieuGiamGia() == null || yeuCau.maPhieuGiamGia().isBlank()
                        ? null
                        : salesService.tinhThuPhieu(khachHang, thanhTien, yeuCau.maPhieuGiamGia());
        BigDecimal tienGiam = phieuGiamGia == null ? SO_KHONG : phieuGiamGia.tienGiam();
        BigDecimal canThanhToan = thanhTien.subtract(tienGiam).add(giaoHang.phiVanChuyen());
        if (canThanhToan.precision() - canThanhToan.scale() > 16)
            throw AppException.badRequest("Giá trị hóa đơn vượt giới hạn lưu trữ");
        // Validate prices, voucher and all quantities before applying the stock delta.
        var soLuongMoi = new TreeMap<Long, Integer>();
        yeuCau.danhSachSanPham().forEach(i -> soLuongMoi.put(i.idSanPhamChiTiet(), i.soLuong().intValueExact()));
        TonKhoHoaDonCho.capNhat(h, soLuongCu, soLuongMoi, tatCaBienThe);
        for (var line : chiTietCu) entityManager.remove(line);
        entityManager.flush();
        for (var item : yeuCau.danhSachSanPham()) {
            var d = new HoaDonChiTiet();
            d.setIdHoaDon(h);
            d.setIdSanPhamChiTiet(danhSachBienThe.get(item.idSanPhamChiTiet()));
            d.setSoLuong(item.soLuong().intValueExact());
            d.setDonGia(hienTai.get(item.idSanPhamChiTiet()).getEffectivePrice());
            d.setThanhTien(d.getDonGia().multiply(item.soLuong()));
            d.setTrangThai(1);
            entityManager.persist(d);
        }
        h.setIdKhachHang(khachHang);
        h.setHoTenNguoiNhan(khachHang == null ? "Khách lẻ" : khachHang.getTenKhachHang());
        h.setSoDienThoaiNguoiNhan(khachHang == null ? null : khachHang.getSoDienThoai());
        DeliveryRules.apDung(h, giaoHang, yeuCau.ghiChu());
        h.setIdPhieuGiamGia(phieuGiamGia == null ? null : phieuGiamGia.phieuGiamGia());
        h.setTienGiamGia(tienGiam);
        h.setTongTien(thanhTien);
        h.setThanhTien(canThanhToan);
        h.setNgayCapNhat(LocalDateTime.now());
        ghiLichSu(h, "Cập nhật giỏ hàng chờ tại quầy");
        entityManager.flush();
        return chuyenDuLieuHoaDonCho(h);
    }

    private NhanVien nhanVien() {
        var nhanVien = entityManager.find(NhanVien.class, PosActor.idNhanVien());
        if (nhanVien == null || !Integer.valueOf(1).equals(nhanVien.getTrangThai()))
            throw AppException.forbidden("Nhân viên không hoạt động");
        return nhanVien;
    }

    private HoaDon hoaDonThuocNhanVien(Long id, boolean khoa) {
        var nguoiThaoTac = nhanVien();
        var h =
                entityManager.find(
                        HoaDon.class,
                        id,
                        khoa ? LockModeType.PESSIMISTIC_WRITE : LockModeType.PESSIMISTIC_READ);
        if (!PosDraftRules.managed(h)
                || h.getIdNhanVien() == null
                || !Objects.equals(nguoiThaoTac.getId(), h.getIdNhanVien().getId()))
            throw AppException.notFound("Không tìm thấy hóa đơn chờ của bạn");
        if (!Integer.valueOf(8).equals(h.getTrangThai())
                || h.getNgayThanhToan() != null
                || entityManager
                .createQuery(
                        "select count(p) from LichSuThanhToan p where p.idHoaDon.id"
                                + " = :id",
                        Long.class)
                .setParameter("id", id)
                .getSingleResult()
                > 0)
            throw AppException.conflict("Hóa đơn không còn là đơn chờ chưa thanh toán");
        return h;
    }

    private List<HoaDonChiTiet> chiTietHoaDon(HoaDon h) {
        return entityManager
                .createQuery(
                        "select d from HoaDonChiTiet d where d.idHoaDon.id = :id order by d.id",
                        HoaDonChiTiet.class)
                .setParameter("id", h.getId())
                .getResultList();
    }

    private Long phienBan(HoaDon h) {
        return entityManager
                .createQuery(
                        "select max(l.id) from LichSuHoaDon l where l.idHoaDon.id = :id",
                        Long.class)
                .setParameter("id", h.getId())
                .getSingleResult();
    }

    private void ghiLichSu(HoaDon h, String ghiChu) {
        var l = new LichSuHoaDon();
        l.setIdHoaDon(h);
        l.setNguoiTao(h.getIdNhanVien().getId());
        l.setTrangThai(h.getTrangThai());
        l.setGhiChu(com.smashstep.datn.invoice.rules.PosHistoryRules.KY_THUAT + ghiChu);
        l.setNgayTao(LocalDateTime.now());
        entityManager.persist(l);
    }

    private static boolean duocPhepBan(SanPhamChiTiet v) {
        return v != null
                && Integer.valueOf(1).equals(v.getTrangThai())
                && Boolean.TRUE.equals(v.getKichHoat())
                && v.getIdSanPham() != null
                && Integer.valueOf(1).equals(v.getIdSanPham().getTrangThai())
                && v.getGiaBan() != null
                && v.getGiaBan().signum() > 0
                && v.getSoLuong() != null
                && v.getSoLuong() >= 0;
    }

    private Draft chuyenDuLieuHoaDonCho(HoaDon h) {
        var cacDong = chiTietHoaDon(h);
        var giuKhac =
                GiuHangTaiQuay.daGiu(
                        entityManager,
                        cacDong.stream().map(d -> d.getIdSanPhamChiTiet().getId()).toList(),
                        h.getId());
        var hienTai =
                dichVuGiaHienTai.getPrices(
                        cacDong.stream().map(HoaDonChiTiet::getIdSanPhamChiTiet).toList(),
                        LocalDateTime.now());
        var danhSachSanPham =
                cacDong.stream()
                        .map(
                                d -> {
                                    var v = d.getIdSanPhamChiTiet();
                                    var p = hienTai.get(v.getId());
                                    return new Line(
                                            v.getId(),
                                            v.getMaChiTietSanPham(),
                                            v.getIdSanPham().getTenSanPham(),
                                            v.getIdMauSac() == null
                                                    ? "—"
                                                    : v.getIdMauSac().getTenMauSac(),
                                            v.getIdKichThuoc() == null
                                                    ? "—"
                                                    : v.getIdKichThuoc().getGiaTri(),
                                            d.getSoLuong(),
                                            d.getDonGia(),
                                            p.getOriginalPrice(),
                                            p.getEffectivePrice(),
                                            GiuHangTaiQuay.khaDung(
                                                    v.getSoLuong() + (PosDraftRules.daTruKho(h) ? d.getSoLuong() : 0),
                                                    giuKhac.getOrDefault(v.getId(), 0L)),
                                            duocPhepBan(v),
                                            v.getIdSanPham().getId(),
                                            v.getIdSanPham().getMaSanPham(),
                                            p.getEffectivePrice().compareTo(d.getDonGia()) == 0
                                                    && p.isDiscounted(),
                                            p.getEffectivePrice().compareTo(d.getDonGia()) == 0
                                                    ? p.getDiscountPercent()
                                                    : null);
                                })
                        .toList();
        var c = h.getIdKhachHang();
        return new Draft(
                h.getId(),
                h.getMaHoaDon(),
                phienBan(h),
                c == null ? null : c.getId(),
                c == null ? null : c.getMaKhachHang(),
                c == null ? null : c.getTenKhachHang(),
                c == null ? null : c.getSoDienThoai(),
                PosDraftRules.note(h),
                danhSachSanPham,
                DeliveryRules.tuHoaDon(h),
                h.getIdPhieuGiamGia() == null ? "" : h.getIdPhieuGiamGia().getMaPhieuGiamGia(),
                h.getTongTien(),
                h.getTienGiamGia(),
                h.getThanhTien());
    }

    public record Draft(
            Long id,
            String maHoaDon,
            Long phienBan,
            Long idKhachHang,
            String maKhachHang,
            String tenKhachHang,
            String soDienThoaiKhachHang,
            String ghiChu,
            List<Line> danhSachSanPham,
            SalesDelivery giaoHang,
            String maPhieuGiamGia,
            BigDecimal tongTienHang,
            BigDecimal tienGiam,
            BigDecimal thanhTien) {}

    public record Line(
            Long id,
            String maSanPhamChiTiet,
            String tenSanPham,
            String mauSac,
            String kichThuoc,
            Integer soLuong,
            BigDecimal giaBan,
            BigDecimal giaGoc,
            BigDecimal giaHienTai,
            Integer tonKho,
            boolean conKinhDoanh,
            Long idSanPham,
            String maSanPham,
            boolean dangGiamGia,
            BigDecimal phanTramGiam) {}
}
