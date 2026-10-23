package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.invoice.entity.*;
import com.smashstep.datn.invoice.rules.PosPriceSnapshot;
import com.smashstep.datn.invoice.service.InvoiceCodeService;
import com.smashstep.datn.payment.entity.*;
import com.smashstep.datn.product.service.KhoaGhiSanPham;
import com.smashstep.datn.sales.service.GiuHangTaiQuay;
import com.smashstep.datn.sales.service.SalesService;
import com.smashstep.datn.sales.service.TonKhoHoaDonCho;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** Publishes a verified gateway payment into the existing invoice/inventory workflow exactly once. */
@Service
@RequiredArgsConstructor
public class VnpayOrderService {
    public static final String PREFIX = "[VNPAY-ORDER:1]|";
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final EntityManager em;
    private final DemoPaymentLedger ledger;
    private final InvoiceCodeService codes;
    private final KhoaGhiSanPham productLock;
    private final SalesService sales;
    private final CustomerCommerceService commerce;
    private final Validator validator;

    public record Posted(String code, Long customerId) {}

    @Transactional
    public Posted post(String gatewayCode) {
        var entry = ledger.entry(gatewayCode);
        if (entry == null || !"VNPAY_SANDBOX".equals(entry.payment().channel())
                || !"PAID_SANDBOX".equals(entry.payment().status())
                || entry.owner() == null || entry.owner() <= 0)
            throw AppException.conflict("Giao dịch chưa được VNPAY xác nhận");
        var payment = entry.payment();
        if (payment.confirmationMethod() != null && !Set.of("IPN", "QUERYDR").contains(payment.confirmationMethod()))
            throw AppException.conflict("Giao dịch không có xác nhận hợp lệ từ VNPAY");
        codes.lockInvoiceCreation();
        String marker = PREFIX + payment.code() + "|";
        var previous = em.createQuery("select l from LichSuHoaDon l join fetch l.idHoaDon h where locate(:marker,l.ghiChu)=1", LichSuHoaDon.class)
                .setParameter("marker", marker).getResultList();
        if (!previous.isEmpty()) {
            if (previous.size() != 1 || !previous.get(0).getGhiChu().equals(marker + entry.fingerprint()))
                throw AppException.conflict("Liên kết giao dịch VNPAY bị trùng hoặc đã thay đổi");
            var invoice = previous.get(0).getIdHoaDon();
            if (invoice.getIdKhachHang() == null || !entry.owner().equals(invoice.getIdKhachHang().getId()))
                throw AppException.conflict("Giao dịch đã được liên kết với khách hàng khác");
            return new Posted(invoice.getMaHoaDon(), entry.owner());
        }
        validate(payment);
        if (em.createQuery("select count(p) from LichSuThanhToan p where upper(p.maGiaoDich)=:ref", Long.class)
                .setParameter("ref", payment.reference().toUpperCase(Locale.ROOT)).getSingleResult() != 0)
            throw AppException.conflict("Mã giao dịch VNPAY đã có trên một hóa đơn khác");
        var customer = em.find(KhachHang.class, entry.owner());
        if (customer == null) throw AppException.conflict("Không còn khách hàng của giao dịch VNPAY");

        productLock.khoa("variants");
        var ids = payment.lines().stream().map(line -> line.variant().variantId()).sorted().toList();
        var variants = TonKhoHoaDonCho.khoaBienThe(em, ids);
        var reserved = GiuHangTaiQuay.daGiu(em, ids, null);
        for (var line : payment.lines()) {
            var variant = variants.get(line.variant().variantId());
            if (variant == null) throw AppException.conflict("Sản phẩm đã thanh toán không còn tồn tại");
            if (GiuHangTaiQuay.khaDung(variant.getSoLuong(), reserved.getOrDefault(variant.getId(), 0L)) < line.quantity())
                throw AppException.conflict("Sản phẩm đã thanh toán không đủ tồn kho; cần cửa hàng xử lý, không thanh toán lại");
        }
        var request = entry.request();
        if (request == null && payment.discount().signum() != 0)
            throw AppException.conflict("Giao dịch cũ thiếu mã voucher gốc; cần bổ sung trước khi ghi hóa đơn");
        var voucher = sales.kiemTraPhieuDaThanhToan(customer, payment.subtotal(),
                request == null ? null : request.voucherCode(), LocalDateTime.ofInstant(payment.createdAt(), ZONE));
        if (voucher.discount().compareTo(payment.discount()) != 0)
            throw AppException.conflict("Voucher không khớp số tiền VNPAY đã xác nhận; cần cửa hàng xử lý");

        var method = paymentMethod();
        var now = LocalDateTime.now(ZONE);
        var placedAt = LocalDateTime.ofInstant(payment.createdAt(), ZONE);
        var paidAt = LocalDateTime.ofInstant(payment.paidAt(), ZONE);
        var receiver = payment.receiver();
        var invoice = new HoaDon();
        invoice.setMaHoaDon(codes.generateNextInvoiceCode()); invoice.setIdKhachHang(customer);
        invoice.setIdPhuongThucThanhToan(method); invoice.setIdPhieuGiamGia(voucher.voucher());
        invoice.setLoaiHoaDon(1); invoice.setTrangThai(0);
        invoice.setTongTien(payment.subtotal()); invoice.setTienGiamGia(payment.discount());
        invoice.setPhiVanChuyen(payment.shipping()); invoice.setThanhTien(payment.amount());
        invoice.setDonViVanChuyen("GHN"); invoice.setHoTenNguoiNhan(receiver.name().trim());
        invoice.setSoDienThoaiNguoiNhan(receiver.phone().trim()); invoice.setDiaChiGiaoHang(receiver.address());
        invoice.setGhiChu(request == null ? null : request.note());
        invoice.setNgayTao(placedAt); invoice.setNgayCapNhat(now); invoice.setNgayThanhToan(paidAt);
        em.persist(invoice);
        for (var line : payment.lines()) {
            var variant = variants.get(line.variant().variantId());
            variant.setSoLuong(variant.getSoLuong() - line.quantity());
            var detail = new HoaDonChiTiet(); detail.setIdHoaDon(invoice); detail.setIdSanPhamChiTiet(variant);
            detail.setSoLuong(line.quantity()); detail.setDonGia(line.variant().giaSauGiam());
            detail.setThanhTien(line.total()); detail.setTrangThai(1); em.persist(detail);
            String priceRecord=PosPriceSnapshot.onlineLine(line.variant().variantId(),line.variant().giaGoc(),
                    line.variant().giaSauGiam(),line.variant().phanTramGiam());
            if(priceRecord!=null)history(invoice,priceRecord,now);
        }
        if (voucher.voucher() != null)
            voucher.voucher().setSoLuongDaDung(Math.addExact(Objects.requireNonNullElse(voucher.voucher().getSoLuongDaDung(), 0), 1));
        if (voucher.assignment() != null) voucher.assignment().setNgaySuDung(placedAt);
        if (request != null && Boolean.TRUE.equals(request.saveAddress())) commerce.savePaidReceiverAddress(entry.owner(), request);

        var receipt = new LichSuThanhToan(); receipt.setIdHoaDon(invoice); receipt.setIdPhuongThucThanhToan(method);
        receipt.setSoTien(payment.amount()); receipt.setMaGiaoDich(payment.reference()); receipt.setTrangThai(1);
        receipt.setThoiGian(paidAt); receipt.setMoTa("VNPAY Sandbox xác nhận qua "
                + Objects.requireNonNullElse(payment.confirmationMethod(), "IPN") + "; môi trường kiểm thử, không thu tiền thật");
        em.persist(receipt);
        history(invoice, marker + entry.fingerprint(), now);
        history(invoice, OnlineOrderInventory.PREFIX + entry.requestId() + "|" + entry.fingerprint(), now);
        history(invoice, "Đặt hàng online, đã thanh toán bằng VNPAY (Sandbox); chờ cửa hàng xác nhận", now);
        em.flush();
        return new Posted(invoice.getMaHoaDon(), entry.owner());
    }

    private void validate(DemoCardPayment.View payment) {
        if (!payment.code().matches("SB[A-F0-9]{32}") || payment.reference() == null
                || !payment.reference().matches("VNPAY-SANDBOX-[0-9]{1,30}")
                || payment.createdAt() == null || payment.expiresAt() == null || payment.paidAt() == null
                || payment.paidAt().isBefore(payment.createdAt().minusSeconds(1)) || !payment.paidAt().isBefore(payment.expiresAt())
                || payment.receiver() == null || !validator.validate(payment.receiver()).isEmpty()
                || payment.receiver().address().length() > 500 || payment.lines() == null
                || payment.lines().isEmpty() || payment.lines().size() > 50
                || !OnlineBankPayment.validAmount(payment.amount()) || payment.subtotal() == null
                || payment.discount() == null || payment.shipping() == null || payment.discount().signum() < 0
                || payment.shipping().signum() < 0 || payment.discount().compareTo(payment.subtotal()) > 0)
            throw AppException.conflict("Biên nhận VNPAY không đủ dữ liệu để lập hóa đơn");
        BigDecimal subtotal = BigDecimal.ZERO;
        Set<Long> ids = new HashSet<>();
        for (var line : payment.lines()) {
            if (line == null || line.variant() == null || line.variant().variantId() == null
                    || line.variant().variantId() <= 0 || !ids.add(line.variant().variantId())
                    || line.quantity() < 1 || line.quantity() > 10000 || line.variant().giaSauGiam() == null
                    || line.variant().giaSauGiam().signum() < 0 || line.total() == null
                    || line.variant().giaSauGiam().multiply(BigDecimal.valueOf(line.quantity())).compareTo(line.total()) != 0)
                throw AppException.conflict("Chi tiết sản phẩm không khớp biên nhận VNPAY");
            subtotal = subtotal.add(line.total());
        }
        if (subtotal.compareTo(payment.subtotal()) != 0
                || subtotal.subtract(payment.discount()).add(payment.shipping()).compareTo(payment.amount()) != 0)
            throw AppException.conflict("Tổng tiền hóa đơn không khớp VNPAY");
    }

    private PhuongThucThanhToan paymentMethod() {
        var matches = em.createQuery("select p from PhuongThucThanhToan p where upper(p.maPhuongThuc)=:code", PhuongThucThanhToan.class)
                .setParameter("code", "VNPAY").getResultList();
        if (matches.size() > 1) throw AppException.conflict("Có nhiều cấu hình phương thức VNPAY, cần kiểm tra lại");
        if (!matches.isEmpty()) return matches.get(0);
        var parent = em.createQuery("select p.idHinhThucThanhToan from PhuongThucThanhToan p where p.maPhuongThuc=:code and p.idHinhThucThanhToan is not null", HinhThucThanhToan.class)
                .setParameter("code", "CHUYEN_KHOAN").setMaxResults(1).getResultStream().findFirst().orElse(null);
        if (parent == null) {
            parent = em.createQuery("select h from HinhThucThanhToan h where h.maHinhThuc=:code", HinhThucThanhToan.class)
                    .setParameter("code", "TRUC_TUYEN").setMaxResults(1).getResultStream().findFirst().orElse(null);
            if (parent == null) {
                parent = new HinhThucThanhToan(); parent.setMaHinhThuc("TRUC_TUYEN");
                parent.setTenHinhThuc("Thanh toán trực tuyến"); parent.setTrangThai(1); em.persist(parent);
            }
        }
        var method = new PhuongThucThanhToan(); method.setMaPhuongThuc("VNPAY"); method.setTenPhuongThuc("VNPAY");
        method.setIdHinhThucThanhToan(parent); method.setTrangThai(1); em.persist(method); return method;
    }
    private void history(HoaDon invoice, String note, LocalDateTime at) {
        var event = new LichSuHoaDon(); event.setIdHoaDon(invoice); event.setTrangThai(invoice.getTrangThai());
        event.setGhiChu(note); event.setNgayTao(at); em.persist(event);
    }
}
