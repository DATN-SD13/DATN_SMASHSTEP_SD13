package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.pricing.CurrentPriceService;
import com.smashstep.datn.customer.entity.KhachHang;
import com.smashstep.datn.invoice.entity.*;
import com.smashstep.datn.invoice.rules.PosPriceSnapshot;
import com.smashstep.datn.invoice.enums.TrangThaiHoaDon;
import com.smashstep.datn.invoice.service.InvoiceCodeService;
import com.smashstep.datn.payment.entity.PhuongThucThanhToan;
import com.smashstep.datn.product.entity.*;
import com.smashstep.datn.product.service.KhoaGhiSanPham;
import com.smashstep.datn.promotion.entity.PhieuGiamGia;
import com.smashstep.datn.sales.service.GiuHangTaiQuay;
import com.smashstep.datn.sales.service.SalesService;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import static com.smashstep.datn.storefront.checkout.CommerceDtos.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class CustomerCommerceService {
    private final EntityManager em;
    private final CurrentPriceService prices;
    private final SalesService sales;
    private final InvoiceCodeService codes;
    private final KhoaGhiSanPham productLock;
    private final GhnShippingService shipping;
    private final com.smashstep.datn.customer.service.KhachHangService addresses;
    private final OnlineBankPayment bankPayment;
    private final OrderEmailNotifications orderEmail;
    private static final String SHIPPING_CHANGED="Khách hàng cập nhật thông tin nhận hàng";

    public KhachHang customer(Long id) {
        var c=em.find(KhachHang.class,id);
        if(c==null || !Integer.valueOf(1).equals(c.getTrangThai())) throw AppException.unauthorized("Tài khoản không còn hoạt động");
        return c;
    }
    public List<Variant> variants(List<Long> ids) {
        if(ids==null || ids.isEmpty() || ids.size()>50 || ids.stream().anyMatch(id->id==null || id<=0))
            throw AppException.badRequest("Giỏ hàng cần từ 1 đến 50 biến thể hợp lệ");
        var rows=em.createQuery("select v from SanPhamChiTiet v join fetch v.idSanPham p left join fetch v.idMauSac left join fetch v.idKichThuoc where v.id in :ids",SanPhamChiTiet.class)
                .setParameter("ids",ids).getResultList();
        var current=prices.getPrices(rows,LocalDateTime.now());
        var reserved=GiuHangTaiQuay.daGiu(em,ids,null);
        return rows.stream().map(v->{
            var p=current.get(v.getId()); int stock=GiuHangTaiQuay.khaDung(v.getSoLuong(),reserved.getOrDefault(v.getId(),0L));
            boolean eligible=Integer.valueOf(1).equals(v.getTrangThai()) && Boolean.TRUE.equals(v.getKichHoat())
                    && Integer.valueOf(1).equals(v.getIdSanPham().getTrangThai()) && v.getGiaBan()!=null && v.getGiaBan().signum()>0
                    && v.getIdMauSac()!=null && v.getIdKichThuoc()!=null;
            return new Variant(v.getId(),v.getIdSanPham().getId(),v.getIdSanPham().getTenSanPham(),v.getSku(),
                    v.getIdMauSac()==null?null:v.getIdMauSac().getTenMauSac(),v.getIdKichThuoc()==null?null:v.getIdKichThuoc().getGiaTri(),
                    image(v),stock,p.getOriginalPrice(),p.getEffectivePrice(),p.getDiscountPercent(),p.isDiscounted(),eligible && stock>0);
        }).toList();
    }
    private List<Line> lines(List<Item> items) {
        if(items==null || items.isEmpty() || items.size()>50) throw AppException.badRequest("Giỏ hàng trống hoặc quá nhiều sản phẩm");
        if(items.stream().anyMatch(i->i==null || i.variantId()==null || i.quantity()==null || i.quantity()<1 || i.quantity()>10000))
            throw AppException.badRequest("Số lượng sản phẩm không hợp lệ");
        if(items.stream().map(Item::variantId).distinct().count()!=items.size()) throw AppException.badRequest("Biến thể bị trùng trong giỏ hàng");
        var indexed=new HashMap<Long,Variant>(); variants(items.stream().map(Item::variantId).toList()).forEach(v->indexed.put(v.variantId(),v));
        return items.stream().map(i->{
            var v=indexed.get(i.variantId());
            if(v==null || !v.available()) throw AppException.conflict("Một sản phẩm đã ngừng bán hoặc hết hàng. Vui lòng kiểm tra giỏ hàng.");
            if(i.quantity()>v.stock()) throw AppException.conflict(v.name()+" chỉ còn "+v.stock()+" sản phẩm khả dụng");
            return new Line(v,i.quantity(),v.giaSauGiam().multiply(BigDecimal.valueOf(i.quantity())));
        }).toList();
    }
    public void validateQuotedOffer(Long customerId,List<Item> items,Quote offer) {
        var quoted=offer.lines();
        var current=lines(items);
        if(current.size()!=quoted.size()) throw AppException.conflict("Sản phẩm trong đơn hàng vừa thay đổi");
        for(int i=0;i<current.size();i++)
            if(!Objects.equals(current.get(i).variant().variantId(),quoted.get(i).variant().variantId())
                    || current.get(i).quantity()!=quoted.get(i).quantity()
                    || current.get(i).variant().giaSauGiam().compareTo(quoted.get(i).variant().giaSauGiam())!=0)
                throw AppException.conflict("Giá sản phẩm vừa thay đổi. Vui lòng kiểm tra lại đơn hàng.");
        var selected=offer.voucherCode()==null?null:vouchers(customerId,offer.subtotal()).stream()
                .filter(v->Objects.equals(v.code(),offer.voucherCode())).findFirst().orElse(null);
        if((offer.voucherCode()!=null && selected==null)
                || (selected==null?BigDecimal.ZERO:selected.discount()).compareTo(offer.discount())!=0)
            throw AppException.conflict("Phiếu giảm giá vừa thay đổi hoặc không còn đủ điều kiện. Vui lòng kiểm tra lại đơn hàng.");
    }
    public List<Method> methods() {
        return em.createQuery("select p from PhuongThucThanhToan p where p.trangThai=1 and (p.idHinhThucThanhToan is null or p.idHinhThucThanhToan.trangThai=1) order by p.id",PhuongThucThanhToan.class)
                .getResultList().stream().map(p->new Method(p.getId(),p.getMaPhuongThuc(),p.getTenPhuongThuc(),
                        "COD".equals(p.getMaPhuongThuc()) || ("CHUYEN_KHOAN".equals(p.getMaPhuongThuc()) && bankPayment.configured()),
                        "COD".equals(p.getMaPhuongThuc()) ? "Thanh toán khi nhận hàng" : "CHUYEN_KHOAN".equals(p.getMaPhuongThuc())
                                ? bankPayment.configured() ? "Quét QR chuyển khoản sau khi tạo đơn; cửa hàng đối soát trước khi xác nhận đơn" : "Thanh toán QR đang tạm ngưng"
                                : "Chưa hỗ trợ thanh toán online bằng phương thức này")).toList();
    }
    public Quote quote(Long customerId,Request r) {
        return calculateQuote(customerId,r,false);
    }
    /** Read-only quotation for the isolated local payment simulator. */
    public Quote demoQuote(Long customerId,Request r) {
        return calculateQuote(customerId,r,true);
    }
    private Quote calculateQuote(Long customerId,Request r,boolean demo) {
        var c=customer(customerId); var lines=lines(r.items());
        if(c.getEmail()==null || c.getEmail().isBlank() || com.smashstep.datn.common.validation.AccountRules.error(com.smashstep.datn.common.validation.AccountRules.Kind.EMAIL,c.getEmail())!=null)
            throw AppException.badRequest("Tài khoản cần email hợp lệ để nhận xác nhận đơn hàng. Vui lòng liên hệ cửa hàng cập nhật email.");
        BigDecimal subtotal=lines.stream().map(Line::total).reduce(BigDecimal.ZERO,BigDecimal::add);
        // No voucher is applied until the customer explicitly chooses one.
        String requestedCode=r.voucherCode()==null?"":r.voucherCode().trim();
        String selectedCode=requestedCode.isEmpty() ? null
                : vouchers(customerId,subtotal).stream().filter(v->v.code().equalsIgnoreCase(requestedCode)).map(Voucher::code).findFirst()
                .orElseThrow(()->AppException.badRequest("Phiếu giảm giá đã chọn không còn đủ điều kiện cho đơn hàng này. Vui lòng chọn phiếu khác hoặc bỏ phiếu."));
        var voucher=sales.tinhThuPhieu(c,subtotal,selectedCode);
        BigDecimal fee=null; String message="";
        try { fee=shipping.fee(r.receiver(),r.items().stream().mapToInt(Item::quantity).sum(),subtotal.subtract(voucher.tienGiam())); }
        catch(AppException e) { message=e.getMessage(); }
        var methods=methods();
        boolean methodValid=demo || methods.stream().anyMatch(m->m.supported() && Objects.equals(m.id(),r.paymentMethodId()));
        if(message.isEmpty() && !methodValid) message="Vui lòng chọn phương thức thanh toán được hỗ trợ";
        BigDecimal total=fee==null?null:subtotal.subtract(voucher.tienGiam()).add(fee);
        boolean bank=methods.stream().anyMatch(m->Objects.equals(m.id(),r.paymentMethodId()) && "CHUYEN_KHOAN".equals(m.code()));
        if((bank || demo) && total!=null && !OnlineBankPayment.validAmount(total)) {
            methodValid=false; message="Thanh toán QR cần tổng tiền VND nguyên dương, tối đa 13 chữ số. Vui lòng chọn COD hoặc liên hệ cửa hàng.";
        }
        return new Quote(lines,subtotal,voucher.tienGiam(),fee,total,selectedCode,methods,fee!=null && methodValid,message);
    }
    public List<Voucher> vouchers(Long customerId,BigDecimal subtotal) {
        var c=customer(customerId);
        if(subtotal==null || subtotal.signum()<0 || subtotal.precision()>20) throw AppException.badRequest("Tạm tính không hợp lệ");
        var now=LocalDateTime.now(); List<Voucher> result=new ArrayList<>();
        // Exclude private vouchers not assigned to this customer before evaluating the shared rules.
        var rows=em.createQuery("select p from PhieuGiamGia p where p.trangThai=1 and p.ngayBatDau<=:now and p.ngayKetThuc>=:now and (p.hinhThucPhieu=1 or (p.hinhThucPhieu=2 and exists (select a.id from PhieuGiamGiaKhachHang a where a.idPhieuGiamGia=p and a.idKhachHang.id=:c and a.trangThai=1 and a.ngaySuDung is null))) order by p.ngayTao desc",PhieuGiamGia.class)
                .setParameter("now",now).setParameter("c",customerId).getResultList();
        for(var p:rows) try {
            var evaluation=sales.tinhThuPhieu(c,subtotal,p.getMaPhieuGiamGia());
            if(evaluation.tienGiam().signum()>0)
                result.add(new Voucher(p.getMaPhieuGiamGia(),p.getTenPhieuGiamGia(),p.getLoaiGiamGia(),p.getGiaTriGiam(),p.getGiaTriToiThieu(),p.getGiamToiDa(),evaluation.tienGiam(),p.getNgayKetThuc()));
        } catch(AppException ineligible) { /* Not a usable voucher for this customer and subtotal. */ }
        result.sort(Comparator.comparing(Voucher::discount).reversed()
                .thenComparing(Voucher::expiresAt).thenComparing(Voucher::code,String.CASE_INSENSITIVE_ORDER));
        var unique=new LinkedHashMap<String,Voucher>();
        for(var voucher:result) unique.putIfAbsent(voucher.code().trim().toLowerCase(Locale.ROOT),voucher);
        return List.copyOf(unique.values());
    }
    @Transactional
    public Order checkout(Long customerId,Request r) {
        if(r.requestId()==null || !r.requestId().matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw AppException.badRequest("Thiếu mã phiên đặt hàng hợp lệ");
        var c=customer(customerId); codes.lockInvoiceCreation();
        String prefix=OnlineOrderInventory.PREFIX+r.requestId()+"|", fingerprint=fingerprint(r);
        var prior=em.createQuery("select l from LichSuHoaDon l join fetch l.idHoaDon h where h.idKhachHang.id=:c and locate(:prefix,l.ghiChu)=1",LichSuHoaDon.class)
                .setParameter("c",customerId).setParameter("prefix",prefix).getResultList();
        if(!prior.isEmpty()) {
            if(!prior.get(0).getGhiChu().equals(prefix+fingerprint)) throw AppException.conflict("Phiên đặt hàng đã dùng cho một giỏ hàng khác");
            return order(customerId,prior.get(0).getIdHoaDon().getMaHoaDon());
        }
        if(r.receiver()==null || r.receiver().address().length()>500) throw AppException.badRequest("Cần thông tin nhận hàng đầy đủ, địa chỉ tối đa 500 ký tự");
        productLock.khoa("variants");
        var ids=r.items().stream().map(Item::variantId).sorted().toList();
        var locked=com.smashstep.datn.sales.service.TonKhoHoaDonCho.khoaBienThe(em,ids);
        if(locked.size()!=ids.size()) throw AppException.conflict("Một biến thể không còn tồn tại. Vui lòng kiểm tra giỏ hàng.");
        var q=quote(customerId,r);
        if(!q.canCheckout()) throw AppException.badRequest(q.message());
        // The carrier request can span a campaign boundary. Reprice again after the network call.
        validateQuotedOffer(customerId,r.items(),q);
        if(r.expectedTotal()==null || r.expectedTotal().compareTo(q.total())!=0) throw AppException.conflict("Giá hoặc phí vận chuyển đã thay đổi. Vui lòng kiểm tra tổng mới và đặt lại.");
        var voucher=sales.kiemTraPhieuDatHang(c,q.subtotal(),q.voucherCode());
        if(voucher.discount().compareTo(q.discount())!=0) throw AppException.conflict("Ưu đãi vừa thay đổi. Vui lòng kiểm tra lại đơn hàng.");
        var method=em.find(PhuongThucThanhToan.class,r.paymentMethodId(),LockModeType.PESSIMISTIC_READ);
        if(method==null || !Integer.valueOf(1).equals(method.getTrangThai())
                || (method.getIdHinhThucThanhToan()!=null && !Integer.valueOf(1).equals(method.getIdHinhThucThanhToan().getTrangThai()))
                || !("COD".equals(method.getMaPhuongThuc()) || ("CHUYEN_KHOAN".equals(method.getMaPhuongThuc()) && bankPayment.configured())))
            throw AppException.badRequest("Phương thức thanh toán không còn khả dụng");
        var now=LocalDateTime.now(); var h=new HoaDon();
        h.setMaHoaDon(codes.generateNextInvoiceCode()); h.setIdKhachHang(c); h.setIdNhanVien(null);
        h.setIdPhuongThucThanhToan(method); h.setIdPhieuGiamGia(voucher.voucher()); h.setLoaiHoaDon(1);
        h.setTongTien(q.subtotal()); h.setTienGiamGia(q.discount()); h.setPhiVanChuyen(q.shipping()); h.setThanhTien(q.total());
        h.setDonViVanChuyen("GHN"); h.setHoTenNguoiNhan(r.receiver().name().trim()); h.setSoDienThoaiNguoiNhan(r.receiver().phone().trim());
        h.setDiaChiGiaoHang(r.receiver().address()); h.setGhiChu(r.note()); h.setNgayTao(now); h.setNgayCapNhat(now); h.setTrangThai(0); em.persist(h);
        for(var line:q.lines()) {
            var v=em.find(SanPhamChiTiet.class,line.variant().variantId());
            v.setSoLuong(v.getSoLuong()-line.quantity());
            var d=new HoaDonChiTiet(); d.setIdHoaDon(h); d.setIdSanPhamChiTiet(v); d.setSoLuong(line.quantity());
            d.setDonGia(line.variant().giaSauGiam()); d.setThanhTien(line.total()); d.setTrangThai(1); em.persist(d);
            String priceRecord=PosPriceSnapshot.onlineLine(line.variant().variantId(),line.variant().giaGoc(),
                    line.variant().giaSauGiam(),line.variant().phanTramGiam());
            if(priceRecord!=null)history(h,priceRecord);
        }
        if(voucher.voucher()!=null) voucher.voucher().setSoLuongDaDung(Objects.requireNonNullElse(voucher.voucher().getSoLuongDaDung(),0)+1);
        if(voucher.assignment()!=null) voucher.assignment().setNgaySuDung(now);
        if(Boolean.TRUE.equals(r.saveAddress())) saveReceiverAddress(c,r);
        history(h,prefix+fingerprint); history(h,"CHUYEN_KHOAN".equals(method.getMaPhuongThuc())
                ? "Khách hàng đặt hàng online; chờ chuyển khoản QR và cửa hàng đối soát" : "Khách hàng đặt hàng online; chờ xác nhận và thu COD");
        OrderLocation.save(em,h,r.deliveryPoint());
        orderEmail.configure(h,true);
        em.flush(); return order(customerId,h.getMaHoaDon());
    }
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
    public void savePaidReceiverAddress(Long customerId,Request request) {
        saveReceiverAddress(customer(customerId),request);
    }
    private void saveReceiverAddress(KhachHang customer,Request r) {
        var receiver=r.receiver();
        var existing=addresses.getAddresses(customer.getMaKhachHang()).stream().filter(a ->
                same(a.getReceiverName(),receiver.name()) && same(a.getReceiverPhone(),receiver.phone())
                        && same(a.getProvince(),receiver.province()) && same(a.getWard(),receiver.ward())
                        && same(a.getDistrict(),receiver.district()) && same(a.getStreet(),receiver.street())).findFirst();
        if(existing.isPresent()) {
            if(Boolean.TRUE.equals(r.makeDefaultAddress())) addresses.setDefaultAddress(customer.getMaKhachHang(),existing.get().getId());
            return;
        }
        var address=new com.smashstep.datn.customer.dto.DiaChiKhachHangRequest();
        address.setReceiverName(receiver.name()); address.setReceiverPhone(receiver.phone());
        address.setProvince(receiver.province()); address.setWard(receiver.ward()); address.setDistrict(receiver.district());
        address.setStreet(receiver.street()); address.setAddressType(1); address.setIsDefault(Boolean.TRUE.equals(r.makeDefaultAddress()));
        addresses.createAddress(customer.getMaKhachHang(),address);
    }
    private boolean same(String left,String right) {
        return Objects.toString(left,"").trim().replaceAll("\\s+"," ")
                .equalsIgnoreCase(Objects.toString(right,"").trim().replaceAll("\\s+"," "));
    }
    public List<Order> orders(Long customerId,int page) {
        customer(customerId); if(page<0 || page>10000) throw AppException.badRequest("Trang không hợp lệ");
        return em.createQuery("select h from HoaDon h where h.idKhachHang.id=:c and h.trangThai<>8 order by h.ngayTao desc,h.id desc",HoaDon.class)
                .setParameter("c",customerId).setFirstResult(page*20).setMaxResults(20).getResultList().stream().map(this::response).toList();
    }
    public Order order(Long customerId,String code) { customer(customerId); return response(owned(customerId,code,false)); }
    public OrderEmailNotifications.Status emailStatus(Long customerId,String code) {customer(customerId);return orderEmail.status(owned(customerId,code,false));}
    public OnlineBankPayment.Payment payment(Long customerId,String code) {
        customer(customerId); return bankPayment.details(owned(customerId,code,false));
    }
    @Transactional
    public Order editShipping(Long customerId,String code,Shipping r) {
        customer(customerId); codes.lockInvoiceCreation(); var h=owned(customerId,code,true);
        if(!Integer.valueOf(0).equals(h.getTrangThai()) || !OnlineOrderInventory.managed(em,h))
            throw AppException.conflict("Chỉ được đổi thông tin nhận hàng khi đơn còn chờ xác nhận.");
        if(same(h.getHoTenNguoiNhan(),r.receiver().name()) && same(h.getSoDienThoaiNguoiNhan(),r.receiver().phone())
                && same(h.getDiaChiGiaoHang(),r.receiver().address()) && same(h.getGhiChu(),r.note())
                && Objects.equals(OrderLocation.get(em,h),r.deliveryPoint()))
            return response(h);
        if(shippingChanges(h)>0)throw AppException.conflict("Bạn đã dùng hết một lần đổi thông tin nhận hàng của đơn này. Vui lòng liên hệ cửa hàng.");
        var rows=em.createQuery("select d from HoaDonChiTiet d where d.idHoaDon.id=:id",HoaDonChiTiet.class).setParameter("id",h.getId()).getResultList();
        if(r.receiver().address().length()>500) throw AppException.badRequest("Địa chỉ tối đa 500 ký tự");
        BigDecimal newFee=shipping.fee(r.receiver(),rows.stream().mapToInt(HoaDonChiTiet::getSoLuong).sum(),h.getTongTien().subtract(h.getTienGiamGia()));
        if((OnlineBankPayment.hasReceipt(em,h) || OnlineBankPayment.bankOrder(em,h))
                && newFee.compareTo(Objects.requireNonNullElse(h.getPhiVanChuyen(),BigDecimal.ZERO))!=0)
            throw AppException.conflict("Đơn đã chốt thanh toán chỉ đổi được địa chỉ có cùng phí giao hàng. Vui lòng liên hệ cửa hàng nếu phí thay đổi.");
        // Editing shipping never reprices products or changes historical voucher amounts.
        h.setPhiVanChuyen(newFee); h.setThanhTien(h.getTongTien().subtract(h.getTienGiamGia()).add(newFee));
        h.setHoTenNguoiNhan(r.receiver().name().trim()); h.setSoDienThoaiNguoiNhan(r.receiver().phone().trim());
        h.setDiaChiGiaoHang(r.receiver().address()); h.setGhiChu(r.note()); h.setNgayCapNhat(LocalDateTime.now());
        if(r.deliveryPoint()==null)OrderLocation.clear(em,h);else OrderLocation.save(em,h,r.deliveryPoint());
        history(h,SHIPPING_CHANGED+" (1/1)"); em.flush(); return response(h);
    }
    @Transactional
    public Order cancel(Long customerId,String code) {
        customer(customerId); codes.lockInvoiceCreation(); var h=owned(customerId,code,true); editable(h);
        OnlineOrderInventory.restore(em,h); h.setTrangThai(6); h.setNgayCapNhat(LocalDateTime.now());
        history(h,"Khách hàng hủy đơn khi đang chờ xác nhận"); orderEmail.queueStatus(h); return response(h);
    }
    public List<Notice> notices(Long customerId) {
        customer(customerId); var from=LocalDateTime.now().minusDays(3); List<Notice> notices=new ArrayList<>();
        var orders=em.createQuery("select h from HoaDon h where h.idKhachHang.id=:c and h.trangThai<>8 and coalesce(h.ngayCapNhat,h.ngayTao)>=:from order by h.ngayCapNhat desc",HoaDon.class)
                .setParameter("c",customerId).setParameter("from",from).setMaxResults(20).getResultList();
        for(var h:orders) notices.add(new Notice("order:"+h.getId()+":"+h.getTrangThai()+":"+h.getNgayCapNhat(),"Cập nhật đơn "+h.getMaHoaDon(),label(h.getTrangThai()),"/don-hang/"+h.getMaHoaDon(),h.getNgayCapNhat()==null?h.getNgayTao():h.getNgayCapNhat()));
        // With no cart subtotal, only immediately usable vouchers are announced (no fabricated eligibility).
        for(var v:vouchers(customerId,BigDecimal.ZERO)) notices.add(new Notice("voucher:"+v.code(),"Ưu đãi: "+v.name(),"Mã "+v.code()+" · Hết hạn "+v.expiresAt().toLocalDate(),"/gio-hang",null));
        return notices;
    }
    private HoaDon owned(Long id,String code,boolean lock) {
        var query=em.createQuery("select h from HoaDon h where h.idKhachHang.id=:c and h.maHoaDon=:code and h.trangThai<>8",HoaDon.class)
                .setParameter("c",id).setParameter("code",code);
        if(lock) query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        return query.getResultStream().findFirst().orElseThrow(()->AppException.notFound("Không tìm thấy đơn hàng của bạn"));
    }
    private boolean isEditable(HoaDon h) { return Integer.valueOf(0).equals(h.getTrangThai()) && OnlineOrderInventory.managed(em,h) && !OnlineBankPayment.hasReceipt(em,h); }
    private long shippingChanges(HoaDon h) {
        return em.createQuery("select count(l) from LichSuHoaDon l where l.idHoaDon.id=:id and locate(:prefix,l.ghiChu)=1",Long.class)
                .setParameter("id",h.getId()).setParameter("prefix",SHIPPING_CHANGED).getSingleResult();
    }
    private boolean canChangeShipping(HoaDon h) {
        return Integer.valueOf(0).equals(h.getTrangThai()) && OnlineOrderInventory.managed(em,h) && shippingChanges(h)==0;
    }
    private void editable(HoaDon h) { if(!isEditable(h)) throw AppException.conflict("Chỉ đơn online đang chờ xác nhận mới được sửa hoặc hủy"); }
    private String image(SanPhamChiTiet v) {
        return em.createQuery("select h.urlAnh from HinhAnhSanPham h where h.idSanPham.id=:p and (h.idSanPhamChiTiet.id=:v or h.idSanPhamChiTiet is null) order by case when h.idSanPhamChiTiet.id=:v then 0 else 1 end,h.isAnhChinh desc,h.id",String.class)
                .setParameter("p",v.getIdSanPham().getId()).setParameter("v",v.getId()).setMaxResults(1).getResultStream().findFirst().orElse(null);
    }
    private Order response(HoaDon h) {
        var lines=em.createQuery("select d from HoaDonChiTiet d join fetch d.idSanPhamChiTiet v join fetch v.idSanPham left join fetch v.idMauSac left join fetch v.idKichThuoc where d.idHoaDon.id=:id order by d.id",HoaDonChiTiet.class)
                .setParameter("id",h.getId()).getResultList().stream().map(d->{var v=d.getIdSanPhamChiTiet();return new OrderItem(v.getId(),v.getIdSanPham().getId(),v.getIdSanPham().getTenSanPham(),v.getSku(),v.getIdMauSac()==null?null:v.getIdMauSac().getTenMauSac(),v.getIdKichThuoc()==null?null:v.getIdKichThuoc().getGiaTri(),image(v),Objects.requireNonNullElse(d.getSoLuong(),0),d.getDonGia(),d.getThanhTien());}).toList();
        var history=em.createQuery("select l from LichSuHoaDon l where l.idHoaDon.id=:id order by l.ngayTao,l.id",LichSuHoaDon.class)
                .setParameter("id",h.getId()).getResultList().stream().filter(l->!com.smashstep.datn.invoice.rules.PosHistoryRules.danhRieng(l.getGhiChu()) && !com.smashstep.datn.invoice.rules.PosPriceSnapshot.metadata(l.getGhiChu()))
                .map(l->new Event(l.getTrangThai(),label(l.getTrangThai()),l.getGhiChu(),l.getNgayTao())).toList();
        return new Order(h.getMaHoaDon(),h.getTrangThai(),label(h.getTrangThai()),h.getNgayTao(),h.getNgayCapNhat(),h.getHoTenNguoiNhan(),h.getSoDienThoaiNguoiNhan(),h.getDiaChiGiaoHang(),h.getIdPhuongThucThanhToan()==null?null:h.getIdPhuongThucThanhToan().getTenPhuongThuc(),com.smashstep.datn.invoice.rules.PosDraftRules.note(h),h.getTongTien(),h.getTienGiamGia(),h.getPhiVanChuyen(),h.getThanhTien(),isEditable(h),lines,history,
                h.getIdPhuongThucThanhToan()==null?null:h.getIdPhuongThucThanhToan().getMaPhuongThuc(),canChangeShipping(h),OrderLocation.get(em,h),
                h.getIdPhieuGiamGia()==null?null:h.getIdPhieuGiamGia().getMaPhieuGiamGia(),
                h.getIdPhieuGiamGia()==null?null:h.getIdPhieuGiamGia().getTenPhieuGiamGia(),h.getDonViVanChuyen());
    }
    private static String label(Integer status) {var value=TrangThaiHoaDon.tuMa(status);return value==null?"Chưa xác định":value.getNhan();}
    private void history(HoaDon h,String message) {var l=new LichSuHoaDon();l.setIdHoaDon(h);l.setTrangThai(h.getTrangThai());l.setNgayTao(LocalDateTime.now());l.setGhiChu(message);em.persist(l);}
    private String fingerprint(Request r) {
        try {
            String value=r.items().stream().sorted(Comparator.comparing(Item::variantId)).map(i->i.variantId()+":"+i.quantity()).reduce("",(a,b)->a+"|"+b)
                    +"|"+Objects.toString(r.voucherCode(),"")+"|"+r.paymentMethodId()+"|"+r.receiver()+"|"+r.note()+"|"+r.expectedTotal()
                    +(Boolean.TRUE.equals(r.saveAddress()) || Boolean.TRUE.equals(r.makeDefaultAddress())
                    ? "|save:"+Boolean.TRUE.equals(r.saveAddress())+"|default:"+Boolean.TRUE.equals(r.makeDefaultAddress()) : "")
                    +(r.deliveryPoint()==null?"":"|point:"+r.deliveryPoint())
                    +(Boolean.TRUE.equals(r.receiveOrderEmails())?"|order-email:true":"");
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch(java.security.NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
    }
}

