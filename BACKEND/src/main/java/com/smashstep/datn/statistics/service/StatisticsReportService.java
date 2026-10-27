package com.smashstep.datn.statistics.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.statistics.controller.ThongKeTongQuanController;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

@Service
public class StatisticsReportService {
    public static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private final JavaMailSender sender;
    private final ThongKeTongQuanController summary;
    @Value("${spring.mail.username:}") private String from;
    public StatisticsReportService(JavaMailSender sender,ThongKeTongQuanController summary){this.sender=sender;this.summary=summary;}

    public void send(String email,LocalDate start,LocalDate end){
        validateRange(start,end);
        if(email==null || !email.matches("^[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+$") || email.length()>254)
            throw AppException.badRequest("Email người nhận không hợp lệ");
        if(from==null || from.isBlank())throw new AppException(HttpStatus.SERVICE_UNAVAILABLE,"Chưa cấu hình tài khoản gửi email");
        var data=summary.tongQuan(start,end).getData();
        String title=periodTitle(start,end);
        var content=render(data,start,end,LocalDate.now(ZONE));
        try{
            var message=sender.createMimeMessage();var helper=new MimeMessageHelper(message,false,"UTF-8");
            helper.setFrom(from);helper.setTo(email.trim());helper.setSubject("SmashStep - Báo cáo tổng kết "+title);
            helper.setText(content,true);sender.send(message);
        }catch(MailException|MessagingException e){throw new AppException(HttpStatus.BAD_GATEWAY,"Không gửi được báo cáo. Vui lòng kiểm tra kết nối và cấu hình SMTP.");}
    }
    public static void validateRange(LocalDate start,LocalDate end){
        if(start==null || end==null || start.isAfter(end))throw AppException.badRequest("Khoảng báo cáo không hợp lệ");
        if(ChronoUnit.DAYS.between(start,end)>366)throw AppException.badRequest("Chỉ hỗ trợ báo cáo tối đa 367 ngày");
        if(start.isAfter(LocalDate.now(ZONE)))throw AppException.badRequest("Không gửi báo cáo cho kỳ chưa bắt đầu");
    }
    public static String periodTitle(LocalDate start,LocalDate end){
        if(start.equals(LocalDate.of(start.getYear(),1,1)) && end.equals(LocalDate.of(start.getYear(),12,31)))return "năm "+start.getYear();
        var month=YearMonth.from(start);
        if(start.equals(month.atDay(1)) && end.equals(month.atEndOfMonth()))return "tháng "+month.format(DateTimeFormatter.ofPattern("MM/yyyy"));
        return "từ "+start+" đến "+end;
    }
    public static String render(Map<String,Object> data,LocalDate start,LocalDate end,LocalDate today){
        var labels=new LinkedHashMap<String,String>();
        labels.put("totalRevenue","Doanh thu thực tế (đơn hoàn thành)");labels.put("expectedRevenue","Giá trị đơn dự kiến");
        labels.put("cashRevenue","Tiền mặt");labels.put("transferRevenue","Chuyển khoản");labels.put("totalOrders","Đơn hoàn thành");
        labels.put("averageOrderValue","Giá trị đơn trung bình");labels.put("productsSold","Số lượng đã bán");labels.put("newCustomers","Khách đăng ký mới");
        var rows=new StringBuilder();
        labels.forEach((key,label)->{var value=data.get(key);String n=NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value==null?BigDecimal.ZERO:new BigDecimal(value.toString()));
            if(Set.of("totalRevenue","expectedRevenue","cashRevenue","transferRevenue","averageOrderValue").contains(key))n+=" đ";
            rows.append("<tr><td style='padding:13px;border-bottom:1px solid #e5edf3;color:#607d8e'>").append(label).append("</td><td align='right' style='padding:13px;border-bottom:1px solid #e5edf3;font-weight:bold;color:#244c65'>").append(n).append("</td></tr>");});
        var formatter=DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String temporary=end.isAfter(today)?"<p style='padding:12px;background:#fff6e3;color:#936d28'>Kỳ chưa kết thúc: số liệu tạm tính đến "+today.format(formatter)+".</p>":"";
        return """
            <!doctype html><html lang="vi"><body style="margin:0;background:#f2f7fb;font-family:Arial,sans-serif;color:#25485f"><table role="presentation" width="100%%"><tr><td align="center" style="padding:25px 12px"><table role="presentation" width="620" style="width:100%%;max-width:620px;background:#fff;border:1px solid #dce8f0;border-radius:12px"><tr><td style="padding:25px;border-bottom:3px solid #168fce"><strong style="font-size:22px">SMASHSTEP</strong><h1 style="font-size:21px;margin:15px 0 7px">Báo cáo tổng kết %s</h1><span style="font-size:13px;color:#7a91a0">%s – %s · Múi giờ Việt Nam</span></td></tr><tr><td style="padding:16px 25px">%s<table width="100%%" style="border-collapse:collapse;font-size:13px">%s</table><p style="font-size:12px;line-height:1.8;color:#849ba9">Doanh thu và đơn hoàn thành theo ngày thanh toán. Giá trị dự kiến từ đơn đã xác nhận/chờ lấy hàng theo ngày tạo; chưa cộng vào doanh thu thực tế.</p><p style="font-size:11px;color:#95a8b4">Báo cáo tự động của hệ thống SmashStep.</p></td></tr></table></td></tr></table></body></html>
            """.formatted(periodTitle(start,end),start.format(formatter),end.format(formatter),temporary,rows);
    }
}
