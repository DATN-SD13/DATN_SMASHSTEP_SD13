package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.invoice.entity.*;
import com.smashstep.datn.promotion.service.EmailService;
import com.smashstep.datn.common.validation.AccountRules;
import jakarta.annotation.PreDestroy;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.*;
import org.springframework.transaction.PlatformTransactionManager;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class OrderEmailNotifications {
    public static final String PREFIX="[WEB-ORDER-MAIL:2]|";
    private static final String EVENT=PREFIX+"EVENT|", QUEUED=EVENT+"QUEUED|", SENT=EVENT+"SENT|";
    private final EntityManager em;private final EmailService mail;private final TransactionTemplate tx;
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"customer-order-mail");t.setDaemon(true);return t;});
    @Value("${app.customer-website-url:http://localhost:5174}") private String site;
    public OrderEmailNotifications(EntityManager em,EmailService mail,PlatformTransactionManager manager){this.em=em;this.mail=mail;this.tx=new TransactionTemplate(manager);}
    public static boolean technical(String note){return note!=null && (note.startsWith(PREFIX) || note.startsWith("[WEB-ORDER-MAIL:1]|"));}
    public void configure(HoaDon invoice,boolean receive){
        requireTransaction();
        if(!em.createQuery("select l.id from LichSuHoaDon l where l.idHoaDon.id=:id and locate(:prefix,l.ghiChu)=1",Long.class)
                .setParameter("id",invoice.getId()).setParameter("prefix",PREFIX).setMaxResults(1).getResultList().isEmpty())return;
        if(!receive){note(invoice,PREFIX+"OPT_OUT");return;}
        String email=invoice.getIdKhachHang()==null?null:invoice.getIdKhachHang().getEmail();
        if(email==null || email.isBlank() || AccountRules.error(AccountRules.Kind.EMAIL,email)!=null){note(invoice,PREFIX+"UNDELIVERABLE");return;}
        note(invoice,PREFIX+"OPT_IN|"+email.trim().toLowerCase(Locale.ROOT));enqueue(invoice,"CREATED");
    }
    public void queueStatus(HoaDon invoice){
        requireTransaction();if(recipient(invoice)==null)return;
        var previous=em.createQuery("select l.trangThai from LichSuHoaDon l where l.idHoaDon.id=:id and locate(:prefix,l.ghiChu)=1 order by l.id desc",Integer.class)
                .setParameter("id",invoice.getId()).setParameter("prefix",EVENT).setMaxResults(1).getResultList();
        if(!previous.isEmpty() && Objects.equals(previous.get(0),invoice.getTrangThai()))return;
        enqueue(invoice,"STATUS");
    }
    private void enqueue(HoaDon invoice,String kind){
        note(invoice,QUEUED+UUID.randomUUID()+"|"+kind);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCommit(){try{worker.execute(OrderEmailNotifications.this::poll);}catch(RejectedExecutionException ignored){/* Recovered after restart. */}}
        });
    }
    private void note(HoaDon invoice,String value){
        var row=new LichSuHoaDon();row.setIdHoaDon(invoice);row.setTrangThai(invoice.getTrangThai());row.setNgayTao(LocalDateTime.now());row.setGhiChu(value);em.persist(row);
    }
    private String recipient(HoaDon invoice){
        return em.createQuery("select l.ghiChu from LichSuHoaDon l where l.idHoaDon.id=:id and locate(:prefix,l.ghiChu)=1 order by l.id",String.class)
                .setParameter("id",invoice.getId()).setParameter("prefix",PREFIX+"OPT_IN|").setMaxResults(1)
                .getResultStream().map(n->n.substring((PREFIX+"OPT_IN|").length())).findFirst().orElse(null);
    }
    @EventListener(ApplicationReadyEvent.class) public void start(){worker.scheduleWithFixedDelay(this::poll,10,60,TimeUnit.SECONDS);}
    public record Status(String email,String state,String message) {}
    public Status status(HoaDon invoice){
        var rows=em.createQuery("select l.ghiChu from LichSuHoaDon l where l.idHoaDon.id=:id and locate(:prefix,l.ghiChu)=1 order by l.id",String.class)
                .setParameter("id",invoice.getId()).setParameter("prefix",PREFIX).getResultList();
        if(rows.contains(PREFIX+"OPT_OUT"))return new Status(null,"OPTED_OUT","Bạn đã chọn không nhận email thông báo cho đơn hàng này.");
        if(rows.contains(PREFIX+"UNDELIVERABLE"))return new Status(null,"UNDELIVERABLE","Email tài khoản không hợp lệ nên chưa gửi được thông báo. Đơn hàng vẫn được lưu; vui lòng liên hệ cửa hàng cập nhật email.");
        String email=recipient(invoice);if(email==null)return new Status(null,"NOT_REQUESTED","Đơn này không đăng ký nhận email cập nhật trạng thái.");
        boolean queued=rows.stream().anyMatch(n->n.startsWith(QUEUED));
        return new Status(email,queued?"QUEUED":"SENT",queued?"Thông báo đơn hàng đang được tự động gửi đến "+email+"."
                :"Đã gửi email thông báo đến "+email+". Vui lòng kiểm tra cả thư mục Spam / Thư rác.");
    }
    private void poll(){
        if(mail.configurationError()!=null)return;
        try{
            var ids=tx.execute(status->em.createQuery("select q.id from LichSuHoaDon q where locate(:queued,q.ghiChu)=1 order by q.id",Long.class)
                    .setParameter("queued",QUEUED).setMaxResults(20).getResultList());
            if(ids!=null)for(var id:ids)deliver(id);
        }catch(RuntimeException e){log.warn("Chưa xử lý được hàng chờ email đơn hàng; sẽ thử lại.");}
    }
    private record Delivery(String email,EmailService.OrderEmail message,String marker,boolean update) {}
    private void deliver(Long eventId){
        try{
            var delivery=tx.execute(status->{
                var event=em.find(LichSuHoaDon.class,eventId);
                if(event==null || event.getGhiChu()==null || !event.getGhiChu().startsWith(QUEUED))return null;
                var invoice=event.getIdHoaDon();String email=recipient(invoice);if(email==null)return null;
                var lines=em.createQuery("select d from HoaDonChiTiet d join fetch d.idSanPhamChiTiet v join fetch v.idSanPham left join fetch v.idMauSac left join fetch v.idKichThuoc where d.idHoaDon.id=:id order by d.id",HoaDonChiTiet.class)
                        .setParameter("id",invoice.getId()).getResultList().stream().map(d->{var v=d.getIdSanPhamChiTiet();return new EmailService.OrderEmailLine(v.getIdSanPham().getTenSanPham(),
                                (v.getIdMauSac()==null?"":v.getIdMauSac().getTenMauSac())+" · Size "+(v.getIdKichThuoc()==null?"":v.getIdKichThuoc().getGiaTri()),
                                d.getSoLuong(),money(d.getDonGia()),money(d.getThanhTien()));}).toList();
                String url=site!=null && site.matches("https?://(?:localhost|127\\.0\\.0\\.1):5174/?")?site.replaceAll("/+$","")+"/don-hang/"+invoice.getMaHoaDon():"";
                var state=com.smashstep.datn.invoice.enums.TrangThaiHoaDon.tuMa(event.getTrangThai());
                return new Delivery(email,new EmailService.OrderEmail(invoice.getMaHoaDon(),invoice.getIdKhachHang().getTenKhachHang(),invoice.getHoTenNguoiNhan(),
                        invoice.getSoDienThoaiNguoiNhan(),invoice.getDiaChiGiaoHang(),invoice.getIdPhuongThucThanhToan()==null?"":invoice.getIdPhuongThucThanhToan().getTenPhuongThuc(),
                        state==null?"Đã ghi nhận":state.getNhan(),OnlineBankPayment.hasReceipt(em,invoice),
                        money(invoice.getTongTien()),money(invoice.getTienGiamGia()),money(invoice.getPhiVanChuyen()),money(invoice.getThanhTien()),lines,url),
                        event.getGhiChu(),event.getGhiChu().endsWith("|STATUS"));
            });
            if(delivery==null)return;
            if(delivery.update())mail.sendOrderStatusEmail(delivery.email(),delivery.message());else mail.sendOrderConfirmationEmail(delivery.email(),delivery.message());
            tx.executeWithoutResult(status->{var event=em.find(LichSuHoaDon.class,eventId,LockModeType.PESSIMISTIC_WRITE);
                if(event!=null && delivery.marker().equals(event.getGhiChu()))event.setGhiChu(SENT+delivery.marker().substring(QUEUED.length()));});
        }catch(RuntimeException e){log.warn("Chưa gửi được email đơn hàng event={}; đơn đã lưu, email sẽ được thử lại.",eventId);}
    }
    private static void requireTransaction(){if(!TransactionSynchronizationManager.isSynchronizationActive())throw new IllegalStateException("Order email requires the order transaction");}
    private static String money(java.math.BigDecimal value){return java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value==null?java.math.BigDecimal.ZERO:value)+" đ";}
    @PreDestroy public void close(){worker.shutdown();}
}
