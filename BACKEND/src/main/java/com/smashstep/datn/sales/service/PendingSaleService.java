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
    private final EntityManager em;
    private final InvoiceCodeService codes;
    private final CurrentPriceService prices;
    private final SalesService sales;
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final String OPEN = " from HoaDon h where h.idNhanVien.id = :employee"
            + " and h.loaiHoaDon = 0 and h.trangThai = 8 and h.ngayThanhToan is null"
            + " and (locate(:marker, h.ghiChu) = 1 or locate(:deliveryMarker, h.ghiChu) = 1)"
            + " and not exists (select p.id from LichSuThanhToan p where p.idHoaDon = h)";

    public List<Draft> list() {
        var employee = employee();
        return em.createQuery("select h" + OPEN + " order by h.id", HoaDon.class)
                .setParameter("employee", employee.getId()).setParameter("marker", PosDraftRules.PREFIX)
                .setParameter("deliveryMarker", PosDraftRules.DELIVERY_PREFIX)
                .setLockMode(LockModeType.PESSIMISTIC_READ)
                .getResultList().stream().map(this::response).toList();
    }
    public Draft get(Long id) { return response(owned(id, false)); }

    @Transactional
    public Draft create() {
        var employee = employee();
        // Same transaction-scoped SQL Server lock as all existing invoice creation paths.
        // Count happens AFTER acquiring it, so concurrent requests cannot both take the fifth slot.
        codes.lockInvoiceCreation();
        long count = em.createQuery("select count(h)" + OPEN, Long.class)
                .setParameter("employee", employee.getId()).setParameter("marker", PosDraftRules.PREFIX)
                .setParameter("deliveryMarker", PosDraftRules.DELIVERY_PREFIX).getSingleResult();
        if (count >= 5) throw AppException.conflict("Mỗi nhân viên chỉ được mở tối đa 5 hóa đơn chờ");
        var h = new HoaDon();
        h.setMaHoaDon(codes.generateNextInvoiceCode()); h.setLoaiHoaDon(0); h.setTrangThai(8);
        h.setIdNhanVien(employee); h.setHoTenNguoiNhan("Khách lẻ"); h.setGhiChu(PosDraftRules.PREFIX);
        h.setTongTien(ZERO); h.setThanhTien(ZERO); h.setTienGiamGia(ZERO); h.setPhiVanChuyen(ZERO);
        h.setNgayTao(LocalDateTime.now()); h.setNgayCapNhat(h.getNgayTao());
        em.persist(h); history(h, "Tạo hóa đơn chờ tại quầy (chưa thanh toán)"); em.flush();
        return response(h);
    }

    @Transactional
    public Draft update(Long id, PendingSaleRequest request) {
        var h = owned(id, true);
        if (!Objects.equals(revision(h), request.revision()))
            throw AppException.conflict("Hóa đơn đã thay đổi ở phiên khác. Tải lại hóa đơn trước khi sửa.");
        KhachHang customer = request.customerId() == null ? null : em.find(KhachHang.class, request.customerId());
        if (request.customerId() != null && (customer == null || !Integer.valueOf(1).equals(customer.getTrangThai())))
            throw AppException.badRequest("Khách hàng không hoạt động");
        var delivery = DeliveryRules.resolve(em, request.customerId(), request.delivery());
        Map<Long, SanPhamChiTiet> variants = new LinkedHashMap<>();
        for (var item : request.items()) {
            int qty;
            try { qty = item.quantity().intValueExact(); } catch (ArithmeticException ex) { throw AppException.badRequest("Số lượng phải là số nguyên"); }
            if (qty < 1 || qty > 10000) throw AppException.badRequest("Số lượng phải từ 1 đến 10000");
            if (variants.containsKey(item.variantId())) throw AppException.badRequest("Biến thể bị trùng");
            var v = em.find(SanPhamChiTiet.class, item.variantId());
            if (!sellable(v)) throw AppException.conflict("Biến thể không còn được bán; hãy xóa khỏi giỏ");
            if (v.getSoLuong() < qty) throw AppException.conflict("Tồn kho không đủ; kiểm tra lại giá và tồn kho");
            variants.put(v.getId(), v);
        }
        var current = prices.getPrices(variants.values(), LocalDateTime.now());
        BigDecimal total = ZERO;
        for (var item : request.items()) {
            var price = current.get(item.variantId()).getEffectivePrice();
            if (price.compareTo(item.unitPrice()) != 0) throw AppException.conflict("Giá đã thay đổi; kiểm tra lại giá và tồn kho trước khi lưu");
            total = total.add(price.multiply(item.quantity()));
        }
        var voucher = request.voucherCode() == null || request.voucherCode().isBlank() ? null
                : sales.previewVoucher(customer, total, request.voucherCode());
        BigDecimal discount = voucher == null ? ZERO : voucher.discount();
        BigDecimal payable = total.subtract(discount).add(delivery.shippingFee());
        if (payable.precision() - payable.scale() > 16) throw AppException.badRequest("Giá trị hóa đơn vượt giới hạn lưu trữ");
        // Validate the entire request before replacing lines. No stock/payment/voucher usage writes.
        for (var line : details(h)) em.remove(line);
        em.flush();
        for (var item : request.items()) {
            var d = new HoaDonChiTiet(); d.setIdHoaDon(h); d.setIdSanPhamChiTiet(variants.get(item.variantId()));
            d.setSoLuong(item.quantity().intValueExact()); d.setDonGia(current.get(item.variantId()).getEffectivePrice());
            d.setThanhTien(d.getDonGia().multiply(item.quantity())); d.setTrangThai(1); em.persist(d);
        }
        h.setIdKhachHang(customer); h.setHoTenNguoiNhan(customer == null ? "Khách lẻ" : customer.getTenKhachHang());
        h.setSoDienThoaiNguoiNhan(customer == null ? null : customer.getSoDienThoai());
        DeliveryRules.apply(h, delivery, request.note());
        h.setIdPhieuGiamGia(voucher == null ? null : voucher.voucher()); h.setTienGiamGia(discount);
        h.setTongTien(total); h.setThanhTien(payable); h.setNgayCapNhat(LocalDateTime.now());
        history(h, "Cập nhật giỏ hàng chờ tại quầy"); em.flush();
        return response(h);
    }
    private NhanVien employee() {
        var employee = em.find(NhanVien.class, PosActor.employeeId());
        if (employee == null || !Integer.valueOf(1).equals(employee.getTrangThai())) throw AppException.forbidden("Nhân viên không hoạt động");
        return employee;
    }
    private HoaDon owned(Long id, boolean lock) {
        var actor = employee();
        var h = em.find(HoaDon.class, id, lock ? LockModeType.PESSIMISTIC_WRITE : LockModeType.PESSIMISTIC_READ);
        if (!PosDraftRules.managed(h) || h.getIdNhanVien() == null || !Objects.equals(actor.getId(), h.getIdNhanVien().getId()))
            throw AppException.notFound("Không tìm thấy hóa đơn chờ của bạn");
        if (!Integer.valueOf(8).equals(h.getTrangThai()) || h.getNgayThanhToan() != null
                || em.createQuery("select count(p) from LichSuThanhToan p where p.idHoaDon.id = :id", Long.class)
                .setParameter("id", id).getSingleResult() > 0)
            throw AppException.conflict("Hóa đơn không còn là đơn chờ chưa thanh toán");
        return h;
    }
    private List<HoaDonChiTiet> details(HoaDon h) {
        return em.createQuery("select d from HoaDonChiTiet d where d.idHoaDon.id = :id order by d.id", HoaDonChiTiet.class)
                .setParameter("id", h.getId()).getResultList();
    }
    private Long revision(HoaDon h) {
        return em.createQuery("select max(l.id) from LichSuHoaDon l where l.idHoaDon.id = :id", Long.class)
                .setParameter("id", h.getId()).getSingleResult();
    }
    private void history(HoaDon h, String note) {
        var l = new LichSuHoaDon(); l.setIdHoaDon(h); l.setNguoiTao(h.getIdNhanVien().getId());
        l.setTrangThai(h.getTrangThai()); l.setGhiChu(note); l.setNgayTao(LocalDateTime.now()); em.persist(l);
    }
    private static boolean sellable(SanPhamChiTiet v) {
        return v != null && Integer.valueOf(1).equals(v.getTrangThai()) && Boolean.TRUE.equals(v.getKichHoat())
                && v.getIdSanPham() != null && Integer.valueOf(1).equals(v.getIdSanPham().getTrangThai())
                && v.getGiaBan() != null && v.getGiaBan().signum() > 0 && v.getSoLuong() != null && v.getSoLuong() > 0;
    }
    private Draft response(HoaDon h) {
        var lines = details(h);
        var current = prices.getPrices(lines.stream().map(HoaDonChiTiet::getIdSanPhamChiTiet).toList(), LocalDateTime.now());
        var items = lines.stream().map(d -> {
            var v = d.getIdSanPhamChiTiet(); var p = current.get(v.getId());
            return new Line(v.getId(), v.getMaChiTietSanPham(), v.getIdSanPham().getTenSanPham(),
                    v.getIdMauSac() == null ? "—" : v.getIdMauSac().getTenMauSac(),
                    v.getIdKichThuoc() == null ? "—" : v.getIdKichThuoc().getGiaTri(),
                    d.getSoLuong(), d.getDonGia(), p.getOriginalPrice(), p.getEffectivePrice(), v.getSoLuong(), sellable(v), v.getIdSanPham().getId(), v.getIdSanPham().getMaSanPham(),
                    p.getEffectivePrice().compareTo(d.getDonGia()) == 0 && p.isDiscounted(),
                    p.getEffectivePrice().compareTo(d.getDonGia()) == 0 ? p.getDiscountPercent() : null);
        }).toList();
        var c = h.getIdKhachHang();
        return new Draft(h.getId(), h.getMaHoaDon(), revision(h), c == null ? null : c.getId(),
                c == null ? null : c.getMaKhachHang(), c == null ? null : c.getTenKhachHang(),
                c == null ? null : c.getSoDienThoai(), PosDraftRules.note(h), items, DeliveryRules.from(h),
                h.getIdPhieuGiamGia() == null ? "" : h.getIdPhieuGiamGia().getMaPhieuGiamGia(), h.getTongTien(), h.getTienGiamGia(), h.getThanhTien());
    }
    public record Draft(Long id, String invoiceCode, Long revision, Long customerId, String customerCode,
                        String customerName, String customerPhone, String note, List<Line> items,
                        com.smashstep.datn.sales.dto.SalesDelivery delivery, String voucherCode,
                        BigDecimal subtotal, BigDecimal discount, BigDecimal total) {}
    public record Line(Long id, String code, String name, String color, String size, Integer qty,
                       BigDecimal price, BigDecimal originalPrice, BigDecimal currentPrice, Integer stock, boolean available, Long productId, String productCode, boolean discounted, BigDecimal discountPercent) {}
}
