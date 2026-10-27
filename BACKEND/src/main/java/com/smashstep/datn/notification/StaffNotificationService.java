package com.smashstep.datn.notification;

import com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails;
import com.smashstep.datn.common.exception.AppException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

@Service
public class StaffNotificationService {
    @PersistenceContext private EntityManager em;
    @Value("${smashstep.notifications.directory:private/staff-notifications}")
    private String directory="private/staff-notifications";
    public record Item(String id,String kind,String title,String description,String url,LocalDateTime createdAt,boolean read){}
    public record Feed(List<Item> items,long unreadCount){}
    static Long employee(){
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null || !auth.isAuthenticated() || !(auth.getDetails() instanceof AuthDetails d)
            || !"NHAN_VIEN".equals(d.userType()) || d.userId()==null)throw AppException.forbidden("Cần đăng nhập nhân viên");
        return ((AuthDetails)auth.getDetails()).userId();
    }
    @Transactional(readOnly=true)
    public Feed list(){return feed(employee());}
    private Feed feed(Long employee){
        var read=read(employee);var items=new ArrayList<Item>();
        var since=LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(30);
        var orders=em.createQuery("select h.id,h.maHoaDon,h.hoTenNguoiNhan,h.ngayTao from HoaDon h where h.loaiHoaDon=1 and h.trangThai<>8 and h.ngayTao>=:since order by h.ngayTao desc,h.id desc")
            .setParameter("since",since).setMaxResults(30).getResultList();
        for(Object value:orders){var row=(Object[])value;String id="order-"+row[0];
            items.add(new Item(id,"order","Đơn hàng mới "+row[1],Objects.toString(row[2],"Khách hàng"),
                "/hoa-don/"+row[1],(LocalDateTime)row[3],read.containsKey(id)));}
        var chats=em.createQuery("select s.id,c.tenKhachHang,s.requestedAt from ChatSession s join s.customer c where s.status='WAITING_STAFF' order by s.requestedAt desc,s.id desc")
            .setMaxResults(30).getResultList();
        for(Object value:chats){var row=(Object[])value;String id="waiting-"+row[0];
            items.add(new Item(id,"chat","Khách hàng cần hỗ trợ",Objects.toString(row[1],"Khách hàng")+" đang chờ tiếp nhận",
                "/ho-tro-truc-tuyen?session="+row[0],(LocalDateTime)row[2],read.containsKey(id)));}
        var messages=em.createQuery("select m.id,s.id,c.tenKhachHang,m.createdAt from ChatMessage m join m.session s join s.customer c where s.status='STAFF_ACTIVE' and s.assignedEmployee.id=:employee and m.senderType='CUSTOMER' and m.createdAt>=:since and m.id=(select max(m2.id) from ChatMessage m2 where m2.session=s and m2.senderType='CUSTOMER') order by m.createdAt desc")
            .setParameter("employee",employee).setParameter("since",since).setMaxResults(30).getResultList();
        for(Object value:messages){var row=(Object[])value;String id="message-"+row[0];
            items.add(new Item(id,"chat","Tin nhắn mới từ khách hàng",Objects.toString(row[2],"Khách hàng"),
                "/ho-tro-truc-tuyen?session="+row[1],(LocalDateTime)row[3],read.containsKey(id)));}
        items.sort(Comparator.comparing(Item::createdAt,Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(Item::id));
        var recent=items.stream().limit(50).toList();return new Feed(recent,recent.stream().filter(i->!i.read()).count());
    }
    @Transactional(readOnly=true)
    public synchronized Feed markRead(List<String> ids){
        Long employee=employee();
        if(ids==null || ids.size()>100 || ids.stream().anyMatch(id->id==null || !id.matches("(order|waiting|message)-[0-9]+")))
            throw AppException.badRequest("Danh sách thông báo không hợp lệ");
        var visible=new HashSet<String>();feed(employee).items().forEach(i->visible.add(i.id()));
        var props=read(employee);for(String id:ids)if(visible.contains(id))props.setProperty(id,Instant.now().toString());
        while(props.size()>2000){String oldest=props.stringPropertyNames().stream().min(Comparator.comparing(props::getProperty)).orElseThrow();props.remove(oldest);}
        write(employee,props);return feed(employee);
    }
    private Path file(Long employee)throws IOException{var root=Path.of(directory).toAbsolutePath().normalize();Files.createDirectories(root);return root.resolve(employee+".properties");}
    private Properties read(Long employee){
        try{var props=new Properties();var file=file(employee);if(Files.exists(file))try(var input=Files.newInputStream(file)){props.load(input);}return props;}
        catch(IOException e){throw new IllegalStateException("Không đọc được trạng thái thông báo",e);}
    }
    private void write(Long employee,Properties props){
        try{var target=file(employee);var temporary=Files.createTempFile(target.getParent(),"notice-",".tmp");
            try{try(var out=Files.newOutputStream(temporary)){props.store(out,"SmashStep staff notification receipts");}
                try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException e){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}}
            finally{Files.deleteIfExists(temporary);}}
        catch(IOException e){throw new IllegalStateException("Không lưu được trạng thái thông báo",e);}
    }
}
