package com.smashstep.datn.sales.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.PageResponse;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.invoice.entity.*;
import com.smashstep.datn.payment.entity.PhuongThucThanhToan;
import com.smashstep.datn.product.entity.SanPhamChiTiet;
import com.smashstep.datn.promotion.entity.*;
import com.smashstep.datn.sales.dto.SalesRequest;
import com.smashstep.datn.sales.dto.SalesResponse.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
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
    private final EntityManager em;
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final String SELLABLE = " where v.trangThai = 1 and v.kichHoat = true and v.soLuong > 0"
            + " and v.giaBan > 0 and v.idSanPham.trangThai = 1";

    public PageResponse<CatalogItem> catalog(int page, int size, String keyword) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw AppException.badRequest("Phân trang không hợp lệ");
        }
        String filter = SELLABLE + " and (lower(v.maChiTietSanPham) like :keyword or lower(v.sku) like :keyword"
                + " or lower(v.idSanPham.tenSanPham) like :keyword or lower(v.idMauSac.tenMauSac) like :keyword)";
        String search = "%" + (keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT)) + "%";
        long count = em.createQuery("select count(v) from SanPhamChiTiet v" + filter, Long.class)
                .setParameter("keyword", search).getSingleResult();
        List<SanPhamChiTiet> rows = em.createQuery("select v from SanPhamChiTiet v join fetch v.idSanPham"
                + " left join fetch v.idMauSac left join fetch v.idKichThuoc" + filter + " order by v.id", SanPhamChiTiet.class)
                .setParameter("keyword", search).setFirstResult(page * size).setMaxResults(size).getResultList();
        LocalDateTime now = LocalDateTime.now();
        List<CatalogItem> content = rows.stream().map(v -> new CatalogItem(v.getId(), v.getMaChiTietSanPham(),
                v.getIdSanPham().getMaSanPham(), v.getIdSanPham().getTenSanPham(),
                v.getIdMauSac() == null ? "—" : v.getIdMauSac().getTenMauSac(),
                v.getIdKichThuoc() == null ? "—" : v.getIdKichThuoc().getGiaTri(),
                sellingPrice(v, now), v.getGiaBan(), v.getSoLuong())).toList();
        return new PageResponse<>(content, page, size, count, (int) ((count + size - 1) / size));
    }

    public List<PaymentMethod> paymentMethods() {
        return em.createQuery("select p from PhuongThucThanhToan p where p.trangThai = 1"
                + " and p.maPhuongThuc in ('TIEN_MAT','CHUYEN_KHOAN','THE') order by p.id", PhuongThucThanhToan.class)
                .getResultList().stream().map(p -> new PaymentMethod(p.getId(), p.getMaPhuongThuc(), p.getTenPhuongThuc())).toList();
    }

    public CatalogItem catalogItem(Long id) {
        SanPhamChiTiet v = em.find(SanPhamChiTiet.class, id);
        if (v == null || !Integer.valueOf(1).equals(v.getTrangThai()) || !Boolean.TRUE.equals(v.getKichHoat())
                || v.getSoLuong() == null || v.getSoLuong() <= 0 || v.getIdSanPham() == null
                || !Integer.valueOf(1).equals(v.getIdSanPham().getTrangThai()) || amount(v.getGiaBan()).signum() <= 0) {
            throw AppException.conflict("Biến thể không còn được bán. Hãy bỏ sản phẩm này khỏi giỏ hàng");
        }
        return new CatalogItem(v.getId(), v.getMaChiTietSanPham(), v.getIdSanPham().getMaSanPham(),
                v.getIdSanPham().getTenSanPham(), v.getIdMauSac() == null ? "—" : v.getIdMauSac().getTenMauSac(),
                v.getIdKichThuoc() == null ? "—" : v.getIdKichThuoc().getGiaTri(), sellingPrice(v, LocalDateTime.now()), v.getGiaBan(), v.getSoLuong());
    }

    public Quote quote(SalesRequest request) { return prepare(request, false).quote(); }

    @Transactional
    public Receipt checkout(SalesRequest request) {
        if (request.getPaidAmount() == null || request.getPaidAmount().signum() < 0) {
            throw AppException.badRequest("Số tiền thanh toán phải được cung cấp và không âm");
        }
        // Serialize request IDs without adding a table or relying on optional DB unique indexes.
        lockCheckout();
        String invoiceCode = "POS-" + request.getRequestId().toLowerCase(Locale.ROOT);
        List<HoaDon> existing = em.createQuery("select h from HoaDon h where h.maHoaDon = :code", HoaDon.class)
                .setParameter("code", invoiceCode).setMaxResults(1).getResultList();
        if (!existing.isEmpty()) {
            HoaDon invoice = existing.get(0);
            if (!Integer.valueOf(5).equals(invoice.getTrangThai())) throw AppException.conflict("Hóa đơn đã được xử lý");
            verifyRetry(invoice, request);
            BigDecimal discount = amount(invoice.getTongTien()).subtract(amount(invoice.getThanhTien())).max(ZERO);
            return new Receipt(invoice.getId(), invoiceCode, invoice.getTongTien(), discount,
                    invoice.getThanhTien(), amount(request.getPaidAmount()).subtract(invoice.getThanhTien()).max(ZERO));
        }
        Prepared data = prepare(request, true);
        BigDecimal paid = amount(request.getPaidAmount());
        if (paid.compareTo(data.quote().total()) < 0) throw AppException.badRequest("Số tiền thanh toán chưa đủ");
        LocalDateTime now = LocalDateTime.now();
        HoaDon invoice = new HoaDon();
        invoice.setMaHoaDon(invoiceCode); invoice.setLoaiHoaDon(0); invoice.setTrangThai(5);
        invoice.setIdKhachHang(data.customer()); invoice.setIdNhanVien(data.employee());
        invoice.setIdPhuongThucThanhToan(data.payment()); invoice.setIdPhieuGiamGia(data.voucher());
        invoice.setTongTien(data.quote().subtotal()); invoice.setPhiVanChuyen(ZERO);
        invoice.setTienGiamGia(data.quote().discount()); invoice.setThanhTien(data.quote().total());
        invoice.setGhiChu(request.getNote()); invoice.setNgayTao(now); invoice.setNgayCapNhat(now); invoice.setNgayThanhToan(now);
        em.persist(invoice);
        for (SalesRequest.Item item : request.getItems()) {
            SanPhamChiTiet variant = data.variants().get(item.getVariantId());
            HoaDonChiTiet detail = new HoaDonChiTiet();
            detail.setIdHoaDon(invoice); detail.setIdSanPhamChiTiet(variant); detail.setSoLuong(item.getQuantity());
            detail.setDonGia(item.getUnitPrice()); detail.setThanhTien(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            detail.setTrangThai(1); em.persist(detail);
            variant.setSoLuong(variant.getSoLuong() - item.getQuantity()); variant.setNgayCapNhat(now);
        }
        if (data.voucher() != null) {
            data.voucher().setSoLuongDaDung(Optional.ofNullable(data.voucher().getSoLuongDaDung()).orElse(0) + 1);
            data.voucher().setNgayCapNhat(now);
            if (data.assignment() != null) data.assignment().setNgaySuDung(now);
        }
        LichSuThanhToan payment = new LichSuThanhToan(); payment.setIdHoaDon(invoice);
        payment.setIdPhuongThucThanhToan(data.payment()); payment.setSoTien(data.quote().total());
        payment.setMaGiaoDich(invoiceCode); payment.setThoiGian(now); payment.setTrangThai(1);
        payment.setMoTa("Thanh toán tại quầy - " + data.payment().getTenPhuongThuc()); em.persist(payment);
        LichSuHoaDon history = new LichSuHoaDon(); history.setIdHoaDon(invoice); history.setNguoiTao(data.employee().getId());
        history.setTrangThai(5); history.setNgayTao(now); history.setGhiChu("Hoàn thành thanh toán tại quầy"); em.persist(history);
        em.flush();
        return new Receipt(invoice.getId(), invoiceCode, data.quote().subtotal(), data.quote().discount(),
                data.quote().total(), paid.subtract(data.quote().total()));
    }

    private Prepared prepare(SalesRequest request, boolean lock) {
        NhanVien employee = em.find(NhanVien.class, request.getEmployeeId());
        if (employee == null || !Integer.valueOf(1).equals(employee.getTrangThai())) throw AppException.badRequest("Nhân viên không hoạt động");
        KhachHang customer = request.getCustomerId() == null ? null : em.find(KhachHang.class, request.getCustomerId());
        if (request.getCustomerId() != null && (customer == null || !Integer.valueOf(1).equals(customer.getTrangThai()))) {
            throw AppException.badRequest("Khách hàng không hoạt động");
        }
        PhuongThucThanhToan payment = em.find(PhuongThucThanhToan.class, request.getPaymentMethodId());
        if (payment == null || !Integer.valueOf(1).equals(payment.getTrangThai())
                || !Set.of("TIEN_MAT", "CHUYEN_KHOAN", "THE").contains(payment.getMaPhuongThuc())) {
            throw AppException.badRequest("Phương thức thanh toán không hợp lệ tại quầy");
        }
        Map<Long, SanPhamChiTiet> variants = new LinkedHashMap<>();
        BigDecimal subtotal = ZERO;
        LocalDateTime now = LocalDateTime.now();
        for (SalesRequest.Item item : request.getItems().stream().sorted(Comparator.comparing(SalesRequest.Item::getVariantId)).toList()) {
            if (variants.containsKey(item.getVariantId())) throw AppException.badRequest("Biến thể bị trùng trong giỏ hàng");
            SanPhamChiTiet variant = em.find(SanPhamChiTiet.class, item.getVariantId());
            if (variant == null) throw AppException.notFound("Không tìm thấy biến thể");
            if (variant.getIdSanPham() == null) throw AppException.badRequest("Biến thể thiếu sản phẩm");
            if (lock) {
                // Product edits already lock the parent; use the same order, then reload stock.
                em.lock(variant.getIdSanPham(), LockModeType.PESSIMISTIC_WRITE);
                em.refresh(variant, LockModeType.PESSIMISTIC_WRITE);
            }
            if (!Integer.valueOf(1).equals(variant.getTrangThai()) || !Boolean.TRUE.equals(variant.getKichHoat())
                    || variant.getIdSanPham() == null || !Integer.valueOf(1).equals(variant.getIdSanPham().getTrangThai())) {
                throw AppException.badRequest("Biến thể không còn được bán");
            }
            if (variant.getSoLuong() == null || variant.getSoLuong() < item.getQuantity()) throw AppException.conflict("Tồn kho không đủ");
            BigDecimal price = sellingPrice(variant, now);
            if (amount(variant.getGiaBan()).signum() <= 0 || price.compareTo(item.getUnitPrice()) != 0) {
                throw AppException.conflict("Giá bán đã thay đổi. Hãy tải lại sản phẩm và xác nhận giá mới");
            }
            variants.put(variant.getId(), variant);
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        PhieuGiamGia voucher = null;
        PhieuGiamGiaKhachHang assignment = null;
        BigDecimal discount = ZERO;
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            var query = em.createQuery("select p from PhieuGiamGia p where lower(p.maPhieuGiamGia) = :code", PhieuGiamGia.class)
                    .setParameter("code", request.getVoucherCode().trim().toLowerCase(Locale.ROOT));
            if (lock) query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
            List<PhieuGiamGia> matches = query.getResultList();
            if (matches.size() != 1) throw AppException.badRequest("Mã phiếu giảm giá không hợp lệ");
            voucher = matches.get(0);
            if ((voucher.getSoLuongDaDung() != null && voucher.getSoLuongDaDung() < 0)
                    || (voucher.getSoLuong() != null && voucher.getSoLuong() <= 0)
                    || (voucher.getGiaTriToiThieu() != null && voucher.getGiaTriToiThieu().signum() < 0)
                    || (voucher.getGiamToiDa() != null && voucher.getGiamToiDa().signum() < 0)) {
                throw AppException.badRequest("Phiếu có số lượng hoặc giới hạn giảm không hợp lệ");
            }
            if (!Integer.valueOf(1).equals(voucher.getTrangThai()) || voucher.getNgayBatDau() == null || voucher.getNgayKetThuc() == null
                    || now.isBefore(voucher.getNgayBatDau()) || now.isAfter(voucher.getNgayKetThuc())) {
                throw AppException.badRequest("Phiếu giảm giá chưa có hiệu lực hoặc đã hết hạn");
            }
            if (voucher.getSoLuong() != null
                    && Optional.ofNullable(voucher.getSoLuongDaDung()).orElse(0) >= voucher.getSoLuong()) {
                throw AppException.conflict("Phiếu giảm giá đã hết lượt sử dụng");
            }
            List<PhieuGiamGiaKhachHang> assignments = em.createQuery("select a from PhieuGiamGiaKhachHang a"
                    + " where a.idPhieuGiamGia.id = :id and a.trangThai = 1", PhieuGiamGiaKhachHang.class)
                    .setParameter("id", voucher.getId()).getResultList();
            if (!Set.of(1, 2).contains(voucher.getHinhThucPhieu() == null ? 0 : voucher.getHinhThucPhieu())) {
                throw AppException.badRequest("Phiếu chưa có hình thức hợp lệ theo schema chuẩn");
            }
            if (Integer.valueOf(2).equals(voucher.getHinhThucPhieu())) {
                assignment = assignments.stream().filter(a -> customer != null && a.getIdKhachHang().getId().equals(customer.getId()))
                        .findFirst().orElseThrow(() -> AppException.badRequest("Phiếu cá nhân không thuộc khách hàng này"));
                if (assignment.getNgaySuDung() != null) throw AppException.conflict("Khách hàng đã sử dụng phiếu này");
            }
            if (subtotal.compareTo(amount(voucher.getGiaTriToiThieu())) < 0) throw AppException.badRequest("Đơn hàng chưa đạt giá trị tối thiểu của phiếu");
            if (voucher.getGiaTriGiam() == null || voucher.getGiaTriGiam().signum() <= 0
                    || (Integer.valueOf(1).equals(voucher.getLoaiGiamGia()) && voucher.getGiaTriGiam().compareTo(new BigDecimal("100")) > 0)) {
                throw AppException.badRequest("Phiếu giảm giá không hợp lệ");
            }
            if (Integer.valueOf(1).equals(voucher.getLoaiGiamGia())) {
                discount = subtotal.multiply(voucher.getGiaTriGiam().min(new BigDecimal("100"))).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                if (voucher.getGiamToiDa() != null && voucher.getGiamToiDa().signum() > 0) discount = discount.min(voucher.getGiamToiDa());
            } else if (Integer.valueOf(2).equals(voucher.getLoaiGiamGia())) discount = voucher.getGiaTriGiam();
            else throw AppException.badRequest("Loại giảm giá không hợp lệ");
            discount = discount.min(subtotal).setScale(2, RoundingMode.HALF_UP);
        }
        return new Prepared(customer, employee, payment, voucher, assignment, variants, new Quote(subtotal, discount, subtotal.subtract(discount)));
    }

    private BigDecimal sellingPrice(SanPhamChiTiet variant, LocalDateTime now) {
        BigDecimal original = amount(variant.getGiaBan());
        List<BigDecimal> discounts = em.createQuery("select coalesce(c.phanTramGiamBienThe,c.idDotGiamGia.phanTramGiamDot) from ChiTietDotGiamGia c"
                + " where c.idSanPhamChiTiet.id = :id and c.trangThai = 1 and c.idDotGiamGia.trangThai = 1"
                + " and c.idDotGiamGia.kichHoat = true and c.idDotGiamGia.ngayBatDau <= :now and c.idDotGiamGia.ngayKetThuc >= :now", BigDecimal.class)
                .setParameter("id", variant.getId()).setParameter("now", now).getResultList();
        BigDecimal percent = discounts.stream().filter(Objects::nonNull).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO)
                .max(BigDecimal.ZERO).min(new BigDecimal("100"));
        return original.multiply(BigDecimal.ONE.subtract(percent.divide(new BigDecimal("100"))))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void lockCheckout() {
        int result = em.unwrap(Session.class).doReturningWork(connection -> {
            if (connection.getAutoCommit()) throw new java.sql.SQLException("Checkout requires a managed transaction");
            // JDBC disables autocommit through SQL Server's implicit transaction mode.
            // EXEC alone does not start that transaction, so read a system table first.
            // Spring commits/rolls back the same connection and releases the application lock.
            try (var start = connection.prepareStatement("SELECT TOP (1) object_id FROM sys.objects");
                 var ignored = start.executeQuery()) {
                // No business row is changed or locked by this metadata read.
            }
            try (var statement = connection.prepareStatement("DECLARE @result int; EXEC @result = sys.sp_getapplock "
                    + "@Resource=N'smashstep:pos:checkout', @LockMode='Exclusive', @LockOwner='Transaction', @LockTimeout=10000; SELECT @result")) {
                try (var rows = statement.executeQuery()) { rows.next(); return rows.getInt(1); }
            }
        });
        if (result < 0) throw AppException.conflict("Đang xử lý thanh toán khác, hãy thử lại");
    }

    private void verifyRetry(HoaDon invoice, SalesRequest request) {
        List<HoaDonChiTiet> details = em.createQuery("select d from HoaDonChiTiet d where d.idHoaDon.id = :id", HoaDonChiTiet.class)
                .setParameter("id", invoice.getId()).getResultList();
        boolean same = Objects.equals(invoice.getIdKhachHang() == null ? null : invoice.getIdKhachHang().getId(), request.getCustomerId())
                && Objects.equals(invoice.getIdNhanVien().getId(), request.getEmployeeId())
                && Objects.equals(invoice.getIdPhuongThucThanhToan().getId(), request.getPaymentMethodId())
                && Objects.equals(invoice.getIdPhieuGiamGia() == null ? "" : invoice.getIdPhieuGiamGia().getMaPhieuGiamGia().toLowerCase(Locale.ROOT),
                    request.getVoucherCode() == null ? "" : request.getVoucherCode().trim().toLowerCase(Locale.ROOT))
                && Objects.equals(invoice.getGhiChu(), request.getNote()) && details.size() == request.getItems().size()
                && request.getItems().stream().map(SalesRequest.Item::getVariantId).distinct().count() == details.size();
        for (SalesRequest.Item item : request.getItems()) {
            same &= details.stream().anyMatch(d -> Objects.equals(d.getIdSanPhamChiTiet().getId(), item.getVariantId())
                    && Objects.equals(d.getSoLuong(), item.getQuantity()) && d.getDonGia().compareTo(item.getUnitPrice()) == 0);
        }
        if (!same || amount(request.getPaidAmount()).compareTo(invoice.getThanhTien()) < 0) {
            throw AppException.conflict("Phiên thanh toán đã dùng cho hóa đơn " + invoice.getMaHoaDon()
                    + ". Hãy kiểm tra hóa đơn trước khi tạo giỏ hàng mới");
        }
    }

    private static BigDecimal amount(BigDecimal value) { return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP); }
    private static final class Prepared {
        private final KhachHang customer;
        private final NhanVien employee;
        private final PhuongThucThanhToan payment;
        private final PhieuGiamGia voucher;
        private final PhieuGiamGiaKhachHang assignment;
        private final Map<Long, SanPhamChiTiet> variants;
        private final Quote quote;

        private Prepared(KhachHang customer, NhanVien employee, PhuongThucThanhToan payment, PhieuGiamGia voucher, PhieuGiamGiaKhachHang assignment, Map<Long, SanPhamChiTiet> variants, Quote quote) {
            this.customer = customer;
            this.employee = employee;
            this.payment = payment;
            this.voucher = voucher;
            this.assignment = assignment;
            this.variants = variants;
            this.quote = quote;
        }

        public KhachHang customer() { return customer; }
        public NhanVien employee() { return employee; }
        public PhuongThucThanhToan payment() { return payment; }
        public PhieuGiamGia voucher() { return voucher; }
        public PhieuGiamGiaKhachHang assignment() { return assignment; }
        public Map<Long, SanPhamChiTiet> variants() { return variants; }
        public Quote quote() { return quote; }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Prepared)) return false;
            Prepared that = (Prepared) other;
            return Objects.equals(customer, that.customer)
                    && Objects.equals(employee, that.employee)
                    && Objects.equals(payment, that.payment)
                    && Objects.equals(voucher, that.voucher)
                    && Objects.equals(assignment, that.assignment)
                    && Objects.equals(variants, that.variants)
                    && Objects.equals(quote, that.quote);
        }

        @Override
        public int hashCode() {
            return Objects.hash(customer, employee, payment, voucher, assignment, variants, quote);
        }

        @Override
        public String toString() {
            return "Prepared[customer=" + customer
                    + ", employee=" + employee
                    + ", payment=" + payment
                    + ", voucher=" + voucher
                    + ", assignment=" + assignment
                    + ", variants=" + variants
                    + ", quote=" + quote
                    + "]";
        }
    }
}
