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
    private final com.smashstep.datn.common.pricing.CurrentPriceService currentPrices;
    private final com.smashstep.datn.invoice.service.InvoiceCodeService invoiceCodes;
    @org.springframework.beans.factory.annotation.Autowired
    private com.smashstep.datn.product.service.KhoaGhiSanPham campaignWriteLock;
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final String SELLABLE = " where v.trangThai = 1 and v.kichHoat = true and v.soLuong > 0"
            + " and v.giaBan > 0 and v.idSanPham.trangThai = 1";

    public PageResponse<CatalogItem> catalog(int page, int size, String keyword) {
        return catalog(page, size, keyword, "", "");
    }
    public PageResponse<CatalogItem> catalog(int page, int size, String keyword, String color, String variantSize) {
        return catalog(page, size, keyword, color, variantSize, null, null);
    }
    public PageResponse<CatalogItem> catalog(int page, int size, String keyword, String color, String variantSize, Long categoryId, Long brandId) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw AppException.badRequest("Phân trang không hợp lệ");
        }
        String filter = SELLABLE + " and (lower(v.maChiTietSanPham) like :keyword or lower(v.sku) like :keyword"
                + " or lower(v.idSanPham.maSanPham) like :keyword or lower(v.idSanPham.tenSanPham) like :keyword or lower(v.idMauSac.tenMauSac) like :keyword or lower(v.idKichThuoc.giaTri) like :keyword)";
        filter += " and (:color = '' or lower(v.idMauSac.tenMauSac) = :color)"
                + " and (:variantSize = '' or v.idKichThuoc.giaTri = :variantSize)";
        if (categoryId != null) filter += " and v.idSanPham.idDanhMuc.id = :categoryId";
        if (brandId != null) filter += " and v.idSanPham.idThuongHieu.id = :brandId";
        color = color == null ? "" : color.trim().toLowerCase(Locale.ROOT);
        variantSize = variantSize == null ? "" : variantSize.trim();
        String search = "%" + (keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT)) + "%";
        var countQuery = em.createQuery("select count(v) from SanPhamChiTiet v" + filter, Long.class);
        var rowQuery = em.createQuery("select v from SanPhamChiTiet v join fetch v.idSanPham"
                + " left join fetch v.idMauSac left join fetch v.idKichThuoc" + filter + " order by v.id", SanPhamChiTiet.class);
        for (var query : List.of(countQuery, rowQuery)) {
            query.setParameter("keyword", search).setParameter("color", color).setParameter("variantSize", variantSize);
            if (categoryId != null) query.setParameter("categoryId", categoryId);
            if (brandId != null) query.setParameter("brandId", brandId);
        }
        long count = countQuery.getSingleResult();
        List<SanPhamChiTiet> rows = rowQuery.setFirstResult(page * size).setMaxResults(size).getResultList();
        LocalDateTime now = LocalDateTime.now();
        var prices = currentPrices.getPrices(rows, now);
        List<CatalogItem> content = rows.stream().map(v -> catalogResponse(v, prices.get(v.getId()))).toList();
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
        return catalogResponse(v, currentPrices.getPrice(v, LocalDateTime.now()));
    }

    public Quote quote(SalesRequest request) {
        if (request.getInvoiceId() != null) ownedInvoice(request.getInvoiceId(), false);
        LocalDateTime now = LocalDateTime.now();
        Prepared data;
        String selectedVoucherError = null;
        try { data = prepare(request, false, now); }
        catch (AppException invalid) {
            if (request.getInvoiceId() == null || request.getVoucherCode() == null || request.getVoucherCode().isBlank()) throw invalid;
            String code = request.getVoucherCode(); request.setVoucherCode(null);
            try { data = prepare(request, false, now); selectedVoucherError = invalid.getMessage(); }
            finally { request.setVoucherCode(code); }
        }
        var delivery = DeliveryRules.resolve(em, request.getCustomerId(), request.getDelivery());
        List<VoucherOption> options = new ArrayList<>();
        for (var candidate : em.createQuery("select p from PhieuGiamGia p where p.trangThai = 1"
                        + " and p.ngayBatDau <= :now and p.ngayKetThuc >= :now order by p.id", PhieuGiamGia.class)
                .setParameter("now", now).getResultList()) {
            try {
                var valid = evaluateVoucher(candidate.getMaPhieuGiamGia(), data.customer(), data.quote().subtotal(), false, now);
                if (valid.discount().signum() > 0) options.add(new VoucherOption(candidate.getMaPhieuGiamGia(),
                        candidate.getTenPhieuGiamGia(), valid.discount(), candidate.getNgayKetThuc(), candidate.getLoaiGiamGia(), candidate.getGiaTriGiam()));
            } catch (AppException invalidForCart) { /* Ineligible vouchers are not advertised. */ }
        }
        options.sort(Comparator.comparing(VoucherOption::discount).reversed().thenComparing(VoucherOption::code));
        return new Quote(data.quote().subtotal(), data.quote().discount(), data.quote().total().add(delivery.shippingFee()),
                delivery.shippingFee(), options, selectedVoucherError);
    }

    @Transactional
    public Receipt checkout(SalesRequest request) {
        if (request.getInvoiceId() != null) return checkoutPending(request);
        if (request.getPayments() != null || request.getDelivery() != null || request.getRevision() != null)
            throw AppException.badRequest("Thanh toán giỏ chờ cần invoiceId");
        if (request.getPaidAmount() == null || request.getPaidAmount().signum() < 0) {
            throw AppException.badRequest("Số tiền thanh toán phải được cung cấp và không âm");
        }
        // Serialize request IDs without adding a table or relying on optional DB unique indexes.
        lockCheckout();
        String transactionCode = "POS-REQ-" + request.getRequestId().toLowerCase(Locale.ROOT);
        List<LichSuThanhToan> existing = em.createQuery("select p from LichSuThanhToan p join fetch p.idHoaDon where p.maGiaoDich = :code or p.maGiaoDich = :first", LichSuThanhToan.class)
                .setParameter("code", transactionCode).setParameter("first", transactionCode + "-1").setMaxResults(1).getResultList();
        if (!existing.isEmpty()) {
            HoaDon invoice = existing.get(0).getIdHoaDon();
            if (com.smashstep.datn.invoice.rules.PosDraftRules.managed(invoice))
                throw AppException.conflict("requestId đã dùng cho thanh toán hóa đơn chờ");
            if (!List.of(0, 5, 8).contains(invoice.getTrangThai())) throw AppException.conflict("Hóa đơn đã được xử lý");
            verifyRetry(invoice, request);
            // Existing payment description stores tendered cash without changing the database schema.
            String paidMarker = " | Khách đưa: " + amount(request.getPaidAmount()).toPlainString();
            if (existing.get(0).getMoTa() == null || !existing.get(0).getMoTa().endsWith(paidMarker))
                throw AppException.conflict("Phiên thanh toán đã dùng với số tiền khách đưa khác");
            return receipt(invoice, amount(request.getPaidAmount()));
        }
        LocalDateTime now = LocalDateTime.now();
        Prepared data = prepare(request, true, now);
        BigDecimal paid = amount(request.getPaidAmount());
        if (paid.compareTo(data.quote().total()) < 0) throw AppException.badRequest("Số tiền thanh toán chưa đủ");
        String invoiceCode = invoiceCodes.generateNextInvoiceCode();
        HoaDon invoice = new HoaDon();
        boolean walkInCustomer = data.customer() == null;
        invoice.setMaHoaDon(invoiceCode); invoice.setLoaiHoaDon(0);
        invoice.setTrangThai(walkInCustomer ? 8 : 0);
        invoice.setIdKhachHang(data.customer()); invoice.setIdNhanVien(data.employee());
        invoice.setIdPhuongThucThanhToan(data.payment()); invoice.setIdPhieuGiamGia(data.voucher());
        invoice.setTongTien(data.quote().subtotal()); invoice.setPhiVanChuyen(ZERO);
        invoice.setTienGiamGia(data.quote().discount()); invoice.setThanhTien(data.quote().total());
        invoice.setHoTenNguoiNhan(data.customer() == null ? "Khách lẻ" : data.customer().getTenKhachHang());
        invoice.setSoDienThoaiNguoiNhan(data.customer() == null ? null : data.customer().getSoDienThoai());
        invoice.setGhiChu(request.getNote()); invoice.setNgayTao(now); invoice.setNgayCapNhat(now); invoice.setNgayThanhToan(now);
        em.persist(invoice);
        for (SalesRequest.Item item : request.getItems()) {
            SanPhamChiTiet variant = data.variants().get(item.getVariantId());
            HoaDonChiTiet detail = new HoaDonChiTiet();
            detail.setIdHoaDon(invoice); detail.setIdSanPhamChiTiet(variant); detail.setSoLuong(item.getQuantity());
            BigDecimal soldPrice = data.prices().get(item.getVariantId()).getEffectivePrice();
            detail.setDonGia(soldPrice); detail.setThanhTien(soldPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
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
        payment.setMaGiaoDich(transactionCode); payment.setThoiGian(now); payment.setTrangThai(1);
        payment.setMoTa("Thanh toán tại quầy - " + data.payment().getTenPhuongThuc() + " | Khách đưa: " + paid.toPlainString()); em.persist(payment);
        LichSuHoaDon history = new LichSuHoaDon(); history.setIdHoaDon(invoice); history.setNguoiTao(data.employee().getId());
        history.setTrangThai(walkInCustomer ? 8 : 0); history.setNgayTao(now); history.setGhiChu(walkInCustomer ? "Khách lẻ nhận tại quầy - hóa đơn chờ hoàn tất" : "Khách hàng đã chọn - tạo đơn giao hàng tại quầy"); em.persist(history);
        em.flush();
        return receipt(invoice, paid);
    }

    private HoaDon ownedInvoice(Long id, boolean write) {
        Long actor = PosActor.employeeId();
        var employee = em.find(NhanVien.class, actor);
        if (employee == null || !Integer.valueOf(1).equals(employee.getTrangThai())) throw AppException.forbidden("Nhân viên không hoạt động");
        var h = em.find(HoaDon.class, id, write ? LockModeType.PESSIMISTIC_WRITE : LockModeType.PESSIMISTIC_READ);
        if (!com.smashstep.datn.invoice.rules.PosDraftRules.managed(h) || h.getIdNhanVien() == null
                || !actor.equals(h.getIdNhanVien().getId())) throw AppException.notFound("Không tìm thấy hóa đơn chờ của bạn");
        if (!Integer.valueOf(8).equals(h.getTrangThai()) || h.getNgayThanhToan() != null)
            throw AppException.conflict("Hóa đơn đã được xử lý; hãy kiểm tra chi tiết hóa đơn");
        return h;
    }

    private Receipt checkoutPending(SalesRequest request) {
        request.setEmployeeId(PosActor.employeeId());
        if (request.getRequestId() == null || !request.getRequestId().matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
            throw AppException.badRequest("requestId không hợp lệ");
        if (request.getRevision() == null || request.getNote() != null && request.getNote().length() > 980)
            throw AppException.badRequest("Cần revision hợp lệ và ghi chú tối đa 980 ký tự");
        if (request.getPayments() == null || request.getPayments().isEmpty() || request.getPayments().size() > 2)
            throw AppException.badRequest("Thiếu các khoản thanh toán");
        if (request.getItems() == null || request.getItems().isEmpty()) throw AppException.badRequest("Giỏ hàng trống");
        // Preserve the existing cross-instance transaction lock, then lock the invoice itself.
        lockCheckout();
        String base = "POS-REQ-" + request.getRequestId().toLowerCase(Locale.ROOT);
        String fingerprint = "POS2:" + CheckoutFingerprint.of(request);
        List<LichSuThanhToan> previous = em.createQuery("select p from LichSuThanhToan p join fetch p.idHoaDon"
                        + " where p.maGiaoDich in :codes order by p.id", LichSuThanhToan.class)
                .setParameter("codes", List.of(base, base + "-1", base + "-2")).getResultList();
        if (!previous.isEmpty()) {
            var h = previous.get(0).getIdHoaDon();
            var actor = em.find(NhanVien.class, request.getEmployeeId());
            if (actor == null || !Integer.valueOf(1).equals(actor.getTrangThai()) || h.getIdNhanVien() == null
                    || !Objects.equals(h.getIdNhanVien().getId(), request.getEmployeeId())
                    || !Objects.equals(h.getId(), request.getInvoiceId())
                    || previous.size() != request.getPayments().size()
                    || previous.stream().anyMatch(p -> !Objects.equals(p.getIdHoaDon().getId(), h.getId())
                    || p.getMoTa() == null || !p.getMoTa().startsWith(fingerprint + " |")))
                throw AppException.conflict("requestId đã dùng cho một yêu cầu khác. Kiểm tra hóa đơn trước khi bán tiếp.");
            return pendingReceipt(h, previous, request.getPaidAmount());
        }
        HoaDon h = ownedInvoice(request.getInvoiceId(), true);
        if (em.createQuery("select count(p) from LichSuThanhToan p where p.idHoaDon.id = :id", Long.class)
                .setParameter("id", h.getId()).getSingleResult() > 0) throw AppException.conflict("Hóa đơn đã có thanh toán");
        Long revision = em.createQuery("select max(l.id) from LichSuHoaDon l where l.idHoaDon.id = :id", Long.class)
                .setParameter("id", h.getId()).getSingleResult();
        if (!Objects.equals(revision, request.getRevision())) throw AppException.conflict("Hóa đơn đã thay đổi; tải lại và xác nhận lại");
        var details = em.createQuery("select d from HoaDonChiTiet d where d.idHoaDon.id = :id order by d.id", HoaDonChiTiet.class)
                .setParameter("id", h.getId()).getResultList();
        var delivery = DeliveryRules.resolve(em, request.getCustomerId(), request.getDelivery());
        boolean matches = Objects.equals(h.getIdKhachHang() == null ? null : h.getIdKhachHang().getId(), request.getCustomerId())
                && Objects.equals(com.smashstep.datn.invoice.rules.PosDraftRules.note(h), Objects.toString(request.getNote(), ""))
                && DeliveryRules.same(h, delivery) && details.size() == request.getItems().size()
                && request.getItems().stream().map(SalesRequest.Item::getVariantId).distinct().count() == details.size();
        for (var item : request.getItems()) matches &= details.stream().anyMatch(d -> Objects.equals(d.getIdSanPhamChiTiet().getId(), item.getVariantId())
                && Objects.equals(d.getSoLuong(), item.getQuantity()) && d.getDonGia().compareTo(item.getUnitPrice()) == 0);
        if (!matches) throw AppException.conflict("Giỏ gửi lên khác hóa đơn đã lưu; hãy lưu và xác nhận lại trước thanh toán");
        request.setPaymentMethodId(request.getPayments().get(0).getMethodId());
        LocalDateTime now = LocalDateTime.now();
        Prepared data = prepare(request, true, now);
        BigDecimal total = data.quote().total().add(delivery.shippingFee());
        if (total.precision() - total.scale() > 16) throw AppException.badRequest("Giá trị hóa đơn vượt giới hạn lưu trữ");
        PaymentPlan plan = PaymentPlan.resolve(em, request, total);
        // Locks may have waited: a campaign/voucher can expire while waiting even without a data edit.
        now = LocalDateTime.now();
        if (data.voucher() != null && (now.isBefore(data.voucher().getNgayBatDau()) || now.isAfter(data.voucher().getNgayKetThuc())))
            throw AppException.conflict("Phiếu giảm giá vừa hết hiệu lực; hãy xác nhận lại tổng tiền");
        var finalPrices = currentPrices.getPrices(data.variants().values(), now);
        for (var item : request.getItems()) if (finalPrices.get(item.getVariantId()).getEffectivePrice().compareTo(item.getUnitPrice()) != 0)
            throw AppException.conflict("Đợt giảm giá vừa thay đổi hiệu lực; hãy kiểm tra lại giá");
        for (var detail : details) {
            var v = data.variants().get(detail.getIdSanPhamChiTiet().getId());
            BigDecimal price = data.prices().get(v.getId()).getEffectivePrice();
            detail.setDonGia(price); detail.setThanhTien(price.multiply(BigDecimal.valueOf(detail.getSoLuong())));
            v.setSoLuong(v.getSoLuong() - detail.getSoLuong()); v.setNgayCapNhat(now);
        }
        if (data.voucher() != null) {
            data.voucher().setSoLuongDaDung(Optional.ofNullable(data.voucher().getSoLuongDaDung()).orElse(0) + 1);
            data.voucher().setNgayCapNhat(now);
            if (data.assignment() != null) data.assignment().setNgaySuDung(now);
        }
        h.setIdPhieuGiamGia(data.voucher());
        h.setIdPhuongThucThanhToan(plan.parts().size() == 1 ? plan.parts().get(0).method() : null);
        h.setTongTien(data.quote().subtotal()); h.setTienGiamGia(data.quote().discount()); h.setThanhTien(total);
        DeliveryRules.apply(h, delivery, request.getNote());
        h.setTrangThai(delivery.enabled() ? 0 : 5); h.setNgayThanhToan(now); h.setNgayCapNhat(now);
        List<LichSuThanhToan> payments = new ArrayList<>();
        int index = 0;
        for (var part : plan.parts()) {
            var payment = new LichSuThanhToan(); payment.setIdHoaDon(h); payment.setIdPhuongThucThanhToan(part.method());
            payment.setSoTien(part.amount()); payment.setMaGiaoDich(base + "-" + (++index)); payment.setThoiGian(now); payment.setTrangThai(1);
            payment.setMoTa(fingerprint + " | Khách đưa: " + amount(request.getPaidAmount()).toPlainString()
                    + ("CHUYEN_KHOAN".equals(part.method().getMaPhuongThuc()) ? " | Nhân viên xác nhận chuyển khoản thủ công" : ""));
            em.persist(payment); payments.add(payment);
        }
        // Persist immutable prices in existing audit rows in this same checkout transaction.
        for (var detail : details) {
            var price = finalPrices.get(detail.getIdSanPhamChiTiet().getId());
            snapshotHistory(h, data.employee().getId(), now,
                    com.smashstep.datn.invoice.rules.PosPriceSnapshot.line(detail.getIdSanPhamChiTiet().getId(),
                            price.getOriginalPrice(), detail.getDonGia(), price.getDiscountPercent() == null ? BigDecimal.ZERO : price.getDiscountPercent()));
        }
        if (data.voucher() != null) snapshotHistory(h, data.employee().getId(), now,
                com.smashstep.datn.invoice.rules.PosPriceSnapshot.voucher(data.voucher().getMaPhieuGiamGia(),
                        data.voucher().getLoaiGiamGia(), data.voucher().getGiaTriGiam()));
        var history = new LichSuHoaDon(); history.setIdHoaDon(h); history.setNguoiTao(data.employee().getId());
        history.setTrangThai(h.getTrangThai()); history.setNgayTao(now);
        history.setGhiChu(delivery.enabled() ? "Đã thanh toán tại POS - giao hàng chờ xác nhận" : "Đã thanh toán và nhận hàng tại quầy");
        em.persist(history); em.flush();
        return pendingReceipt(h, payments, request.getPaidAmount());
    }

    private void snapshotHistory(HoaDon invoice, Long actor, LocalDateTime time, String note) {
        if (note.length() > 1000) throw AppException.badRequest("Thông tin giá vượt giới hạn lưu trữ");
        var row = new LichSuHoaDon(); row.setIdHoaDon(invoice); row.setNguoiTao(actor);
        row.setTrangThai(invoice.getTrangThai()); row.setNgayTao(time); row.setGhiChu(note); em.persist(row);
    }

    private Receipt pendingReceipt(HoaDon h, List<LichSuThanhToan> payments, BigDecimal cashTendered) {
        BigDecimal transferred = payments.stream().filter(p -> "CHUYEN_KHOAN".equals(p.getIdPhuongThucThanhToan().getMaPhuongThuc()))
                .map(LichSuThanhToan::getSoTien).reduce(ZERO, BigDecimal::add);
        var parts = payments.stream().map(p -> new ReceiptPayment(p.getIdPhuongThucThanhToan().getId(),
                p.getIdPhuongThucThanhToan().getMaPhuongThuc(), p.getIdPhuongThucThanhToan().getTenPhuongThuc(), p.getSoTien(), p.getMaGiaoDich())).toList();
        return receipt(h, amount(cashTendered).add(transferred)).settlement(parts, h.getPhiVanChuyen(), h.getTrangThai());
    }

    private Prepared prepare(SalesRequest request, boolean lock, LocalDateTime now) {
        // Share the campaign module's existing transaction lock before reading final selling prices.
        if (lock && campaignWriteLock != null) campaignWriteLock.khoa("campaigns");
        NhanVien employee = em.find(NhanVien.class, request.getEmployeeId());
        if (employee == null || !Integer.valueOf(1).equals(employee.getTrangThai())) throw AppException.badRequest("Nhân viên không hoạt động");
        KhachHang customer = request.getCustomerId() == null ? null : em.find(KhachHang.class, request.getCustomerId());
        if (request.getCustomerId() != null && (customer == null || !Integer.valueOf(1).equals(customer.getTrangThai()))) {
            throw AppException.badRequest("Khách hàng không hoạt động");
        }
        PhuongThucThanhToan payment = request.getPaymentMethodId() == null ? null : em.find(PhuongThucThanhToan.class, request.getPaymentMethodId());
        if (payment == null || !Integer.valueOf(1).equals(payment.getTrangThai())
                || !Set.of("TIEN_MAT", "CHUYEN_KHOAN", "THE").contains(payment.getMaPhuongThuc())) {
            throw AppException.badRequest("Phương thức thanh toán không hợp lệ tại quầy");
        }
        Map<Long, SanPhamChiTiet> variants = new LinkedHashMap<>();
        BigDecimal subtotal = ZERO;
        for (SalesRequest.Item item : request.getItems().stream().sorted(Comparator.comparing(SalesRequest.Item::getVariantId)).toList()) {
            if (variants.containsKey(item.getVariantId())) throw AppException.badRequest("Biến thể bị trùng trong giỏ hàng");
            SanPhamChiTiet variant = em.find(SanPhamChiTiet.class, item.getVariantId());
            if (variant == null) throw AppException.notFound("Không tìm thấy biến thể");
            if (variant.getIdSanPham() == null) throw AppException.badRequest("Biến thể thiếu sản phẩm");
            if (lock) {
                // Product edits already lock the parent; use the same order, then reload stock.
                em.lock(variant.getIdSanPham(), LockModeType.PESSIMISTIC_WRITE);
                em.refresh(variant.getIdSanPham(), LockModeType.PESSIMISTIC_WRITE);
                em.refresh(variant, LockModeType.PESSIMISTIC_WRITE);
            }
            if (!Integer.valueOf(1).equals(variant.getTrangThai()) || !Boolean.TRUE.equals(variant.getKichHoat())
                    || variant.getIdSanPham() == null || !Integer.valueOf(1).equals(variant.getIdSanPham().getTrangThai())) {
                throw AppException.badRequest("Biến thể không còn được bán");
            }
            if (variant.getSoLuong() == null || variant.getSoLuong() < item.getQuantity()) throw AppException.conflict("Tồn kho không đủ");
            variants.put(variant.getId(), variant);
        }
        if (lock) now = LocalDateTime.now();
        var prices = currentPrices.getPrices(variants.values(), now);
        for (SalesRequest.Item item : request.getItems()) {
            BigDecimal price = prices.get(item.getVariantId()).getEffectivePrice();
            if (amount(variants.get(item.getVariantId()).getGiaBan()).signum() <= 0 || price.compareTo(item.getUnitPrice()) != 0)
                throw AppException.conflict("Giá bán đã thay đổi. Hãy tải lại sản phẩm và xác nhận giá mới.");
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        var evaluation = evaluateVoucher(request.getVoucherCode(), customer, subtotal, lock, now);
        return new Prepared(customer, employee, payment, evaluation.voucher(), evaluation.assignment(), variants, prices,
                new Quote(subtotal, evaluation.discount(), subtotal.subtract(evaluation.discount())));
    }

    private record VoucherEvaluation(PhieuGiamGia voucher, PhieuGiamGiaKhachHang assignment, BigDecimal discount) {}
    public record DraftVoucher(PhieuGiamGia voucher, BigDecimal discount) {}
    public DraftVoucher previewVoucher(KhachHang customer, BigDecimal subtotal, String code) {
        var result = evaluateVoucher(code, customer, subtotal, false, LocalDateTime.now());
        return new DraftVoucher(result.voucher(), result.discount());
    }
    private VoucherEvaluation evaluateVoucher(String code, KhachHang customer, BigDecimal subtotal, boolean lock, LocalDateTime now) {
        PhieuGiamGia voucher = null;
        PhieuGiamGiaKhachHang assignment = null;
        BigDecimal discount = ZERO;
        if (code != null && !code.isBlank()) {
            var query = em.createQuery("select p from PhieuGiamGia p where lower(p.maPhieuGiamGia) = :code", PhieuGiamGia.class)
                    .setParameter("code", code.trim().toLowerCase(Locale.ROOT));
            if (lock) query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
            List<PhieuGiamGia> matches = query.getResultList();
            if (matches.size() != 1) throw AppException.badRequest("Mã phiếu giảm giá không hợp lệ");
            voucher = matches.get(0);
            if (lock) em.refresh(voucher, LockModeType.PESSIMISTIC_WRITE);
            if (lock) now = LocalDateTime.now();
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
                if (lock) em.refresh(assignment, LockModeType.PESSIMISTIC_WRITE);
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
        return new VoucherEvaluation(voucher, assignment, discount);
    }

    private CatalogItem catalogResponse(SanPhamChiTiet v, com.smashstep.datn.common.pricing.CurrentPrice price) {
        return new CatalogItem(v.getId(), v.getMaChiTietSanPham(), v.getIdSanPham().getMaSanPham(),
                v.getIdSanPham().getTenSanPham(), v.getIdMauSac() == null ? "—" : v.getIdMauSac().getTenMauSac(),
                v.getIdKichThuoc() == null ? "—" : v.getIdKichThuoc().getGiaTri(), price.getEffectivePrice(), price.getOriginalPrice(),
                v.getSoLuong(), price.isDiscounted(), price.getDiscountPercent(), price.getCampaignCode(), price.getCampaignName()).productId(v.getIdSanPham().getId());
    }

    private Receipt receipt(HoaDon invoice, BigDecimal paid) {
        var employee = invoice.getIdNhanVien(); var customer = invoice.getIdKhachHang(); var method = invoice.getIdPhuongThucThanhToan();
        return new Receipt(invoice.getId(), invoice.getMaHoaDon(), invoice.getTongTien(), invoice.getTienGiamGia(), invoice.getThanhTien(),
                paid.subtract(invoice.getThanhTien()), employee.getId(), employee.getMaNhanVien(), employee.getTenNhanVien(),
                customer == null ? null : customer.getId(), customer == null ? null : customer.getMaKhachHang(),
                invoice.getHoTenNguoiNhan(), invoice.getSoDienThoaiNguoiNhan(), method == null ? "KET_HOP" : method.getMaPhuongThuc(), method == null ? "Kết hợp" : method.getTenPhuongThuc(),
                invoice.getIdPhieuGiamGia() == null ? null : invoice.getIdPhieuGiamGia().getMaPhieuGiamGia(), invoice.getNgayTao(), paid);
    }

    private void lockCheckout() { invoiceCodes.lockInvoiceCreation(); }

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
        private final Map<Long, com.smashstep.datn.common.pricing.CurrentPrice> prices;
        private final Quote quote;

        private Prepared(KhachHang customer, NhanVien employee, PhuongThucThanhToan payment, PhieuGiamGia voucher, PhieuGiamGiaKhachHang assignment, Map<Long, SanPhamChiTiet> variants, Map<Long, com.smashstep.datn.common.pricing.CurrentPrice> prices, Quote quote) {
            this.customer = customer;
            this.employee = employee;
            this.payment = payment;
            this.voucher = voucher;
            this.assignment = assignment;
            this.variants = variants;
            this.prices = prices;
            this.quote = quote;
        }

        public KhachHang customer() { return customer; }
        public NhanVien employee() { return employee; }
        public PhuongThucThanhToan payment() { return payment; }
        public PhieuGiamGia voucher() { return voucher; }
        public PhieuGiamGiaKhachHang assignment() { return assignment; }
        public Map<Long, SanPhamChiTiet> variants() { return variants; }
        public Map<Long, com.smashstep.datn.common.pricing.CurrentPrice> prices() { return prices; }
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
