
package com.smashstep.datn.promotion.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final org.springframework.beans.factory.ObjectProvider<JavaMailSender> mailSenderProvider;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:}")
    private String from;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.host:}")
    private String host;
    @org.springframework.beans.factory.annotation.Value("${spring.mail.password:}")
    private String password;

    public record OrderEmailLine(String name,String variant,int quantity,String unitPrice,String total) {}
    public record OrderEmail(String code,String customer,String receiver,String phone,String address,String method,String status,
            boolean paid,String subtotal,String discount,String shipping,String total,
            java.util.List<OrderEmailLine> lines,String url) {}

    public void sendOrderConfirmationEmail(String to,OrderEmail order) {sendOrderEmail(to,order,false);}
    public void sendOrderStatusEmail(String to,OrderEmail order) {sendOrderEmail(to,order,true);}
    private void sendOrderEmail(String to,OrderEmail order,boolean update) {
        var content=OrderEmailTemplate.render(order,update);
        sendBranded(to,"SmashStep - "+(update?"Cập nhật đơn hàng ":"Xác nhận đơn hàng ")+order.code(),content.html(),content.text());
    }

    private void sendBranded(String to,String subject,String html,String text) {
        var sender=senderFor(to);
        try {
            var message=sender.createMimeMessage();var helper=new MimeMessageHelper(message,true,"UTF-8");
            helper.setFrom(from,"SmashStep");helper.setReplyTo(from,"SmashStep");helper.setTo(to);helper.setSubject(subject);
            helper.setSentDate(new java.util.Date());
            helper.setText(text,html);sender.send(message);
        } catch(Exception e){throw new IllegalStateException("Không gửi được email SmashStep",e);}
    }
    public String configurationError() {
        if (host == null || host.isBlank() || "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) {
            return "SMTP chưa hợp lệ: cấu hình MAIL_HOST=smtp.gmail.com.";
        }
        if (from == null || from.isBlank() || password == null || password.isBlank()) {
            return "Đã lưu phiếu nhưng chưa gửi email: thiếu MAIL_USERNAME hoặc MAIL_PASSWORD (Google App Password).";
        }
        return mailSenderProvider.getIfAvailable() == null ? "SMTP chưa được cấu hình." : null;
    }

    private JavaMailSender senderFor(String to) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) throw new IllegalStateException("SMTP chưa được cấu hình");
        // Mock senders remain usable in unit tests; enforce credentials for the real transport.
        if (sender instanceof org.springframework.mail.javamail.JavaMailSenderImpl) {
            String error = configurationError();
            if (error != null) throw new IllegalStateException(error);
        }
        try {
            var address = new jakarta.mail.internet.InternetAddress(to, true);
            address.validate();
            if (!address.getAddress().contains("@")) throw new IllegalArgumentException();
        } catch (Exception ex) { throw new IllegalArgumentException("Email khách hàng không hợp lệ"); }
        return sender;
    }

    public void sendVoucherEmail(
            String to,
            String customerName,
            String voucherName,
            String voucherCode,
            String discount,
            String maxDiscount,
            String minOrderValue,
            String startDate,
            String endDate,
            String websiteUrl
    ) {
        JavaMailSender mailSender = senderFor(to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            if (from != null && !from.isBlank()) {
                helper.setFrom(from, "SmashStep");
            }
            helper.setTo(to);
            helper.setSubject("SmashStep - Ưu đãi dành riêng cho bạn!");

            String safeWebsiteUrl = isSafeHttpUrl(websiteUrl)
                    ? escape(websiteUrl)
                    : "";

            String html = """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:24px 10px;background:#f1f5f9;
                             font-family:Arial,Helvetica,sans-serif;color:#10243a;">
                  <div style="max-width:520px;margin:0 auto;background:#ffffff;
                              border:1px solid #dce5ed;border-radius:12px;overflow:hidden;">

                    <div style="padding:20px;text-align:center;
                                background:#071b32;">
                      <div style="font-size:25px;font-weight:900;letter-spacing:1px;
                                  color:#ffffff;">
                        SMASH<span style="color:#20d5f5;">STEP</span>
                      </div>
                      <div style="margin-top:5px;font-size:10px;letter-spacing:3px;
                                  color:#b9eefa;">
                        STEP INTO YOUR STYLE
                      </div>
                    </div>

                    <div style="padding:24px 22px;">
                      <h2 style="margin:0 0 14px;text-align:center;font-size:22px;
                                 color:#0b2540;">
                        Chào %s,
                      </h2>

                      <p style="font-size:14px;line-height:1.6;margin:0 0 18px;
                                color:#40546a;">
                        Cảm ơn bạn đã đồng hành cùng
                        <b style="color:#087fa8;">SmashStep</b>.
                        Chúng tôi gửi tặng bạn một ưu đãi đặc biệt!
                      </p>

                      <div style="padding:16px;background:#f0faff;
                                  border:1px solid #c8edf8;
                                  border-left:4px solid #08bfe5;
                                  border-radius:8px;font-size:13px;line-height:2;">
                        <div>Voucher: <b style="color:#0b2540;">%s</b></div>

                        <div>
                          <b>Mã sử dụng:</b>
                          <span style="display:inline-block;background:#d7f5fc;
                                       color:#075477;padding:3px 8px;border-radius:5px;
                                       font-weight:bold;letter-spacing:1px;">%s</span>
                        </div>

                        <div><b>Giá trị giảm:</b>
                          <span style="color:#008db8;font-weight:bold;">%s</span>
                        </div>
                        <div><b>Giảm tối đa:</b> %s</div>
                        <div><b>Áp dụng cho đơn từ:</b> %s</div>
                        <div><b>Thời gian áp dụng:</b> %s đến %s</div>
                      </div>

                      <p style="font-size:13px;line-height:1.6;margin:18px 0;
                                color:#526579;">
                        Hãy nhanh tay sử dụng mã giảm giá này cho lần mua sắm
                        tiếp theo nhé!
                      </p>

                      %s
                    </div>

                    <div style="height:5px;background:#20d5f5;"></div>
                  </div>
                </body>
                </html>
                """.formatted(
                    escape(customerName),
                    escape(voucherName),
                    escape(voucherCode),
                    escape(discount),
                    escape(formatCurrency(maxDiscount)),
                    escape(formatCurrency(minOrderValue)),
                    escape(formatDateTime(startDate)),
                    escape(formatDateTime(endDate)),
                    safeWebsiteUrl.isEmpty()
                            ? ""
                            : """
                          <div style="text-align:center;">
                            <a href="%s"
                               style="display:inline-block;padding:12px 28px;
                                      background:#079fc9;color:#ffffff;
                                      text-decoration:none;border-radius:25px;
                                      font-size:13px;font-weight:bold;">
                              Mua sắm ngay &rarr;
                            </a>
                          </div>
                          """.formatted(safeWebsiteUrl)
            );

            String plainText = """
                Chào %s!

                SmashStep gửi bạn ưu đãi:
                Voucher: %s
                Mã sử dụng: %s
                Giá trị giảm: %s
                Giảm tối đa: %s
                Áp dụng cho đơn từ: %s
                Thời gian: %s đến %s

                Cảm ơn bạn đã đồng hành cùng SmashStep.
                """.formatted(
                    safeText(customerName),
                    safeText(voucherName),
                    safeText(voucherCode),
                    safeText(discount),
                    safeText(formatCurrency(maxDiscount)),
                    safeText(formatCurrency(minOrderValue)),
                    safeText(formatDateTime(startDate)),
                    safeText(formatDateTime(endDate))
            );

            helper.setText(plainText, html);
            mailSender.send(message);


        } catch (Exception e) {

            throw new IllegalStateException(
                    "Không thể gửi email phiếu giảm giá", e);
        }
    }

    public void sendVoucherUpdatedEmail(
            String to,
            String customerName,
            String voucherName,
            String voucherCode,
            String discount,
            String maxDiscount,
            String minOrderValue,
            String startDate,
            String endDate,
            String websiteUrl
    ) {
        JavaMailSender mailSender = senderFor(to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            if (from != null && !from.isBlank()) {
                helper.setFrom(from, "SmashStep");
            }
            helper.setTo(to);
            helper.setSubject("SmashStep - Cập nhật thông tin phiếu giảm giá!");

            String safeWebsiteUrl = isSafeHttpUrl(websiteUrl)
                    ? escape(websiteUrl)
                    : "";

            String html = """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:24px 10px;background:#f1f5f9;
                             font-family:Arial,Helvetica,sans-serif;color:#10243a;">
                  <div style="max-width:520px;margin:0 auto;background:#ffffff;
                              border:1px solid #dce5ed;border-radius:12px;overflow:hidden;">

                    <div style="padding:20px;text-align:center;
                                background:#071b32;">
                      <div style="font-size:25px;font-weight:900;letter-spacing:1px;
                                  color:#ffffff;">
                        SMASH<span style="color:#20d5f5;">STEP</span>
                      </div>
                      <div style="margin-top:5px;font-size:10px;letter-spacing:3px;
                                  color:#b9eefa;">
                        STEP INTO YOUR STYLE
                      </div>
                    </div>

                    <div style="padding:24px 22px;">
                      <h2 style="margin:0 0 14px;text-align:center;font-size:22px;
                                 color:#0b2540;">
                        Chào %s,
                      </h2>

                      <p style="font-size:14px;line-height:1.6;margin:0 0 18px;
                                color:#40546a;">
                        SmashStep xin thông báo phiếu giảm giá dành cho bạn vừa được
                        <b style="color:#e67e22;">cập nhật điều kiện ưu đãi mới</b>!
                      </p>

                      <div style="padding:16px;background:#fffaf0;
                                  border:1px solid #feebc8;
                                  border-left:4px solid #dd6b20;
                                  border-radius:8px;font-size:13px;line-height:2;">
                        <div>Voucher: <b style="color:#0b2540;">%s</b></div>

                        <div>
                          <b>Mã sử dụng:</b>
                          <span style="display:inline-block;background:#feebc8;
                                       color:#7b341e;padding:3px 8px;border-radius:5px;
                                       font-weight:bold;letter-spacing:1px;">%s</span>
                        </div>

                        <div><b>Giá trị giảm mới:</b>
                          <span style="color:#c05621;font-weight:bold;">%s</span>
                        </div>
                        <div><b>Giảm tối đa:</b> %s</div>
                        <div><b>Áp dụng cho đơn từ:</b> %s</div>
                        <div><b>Thời gian áp dụng:</b> %s đến %s</div>
                      </div>

                      <p style="font-size:13px;line-height:1.6;margin:18px 0;
                                color:#526579;">
                        Ưu đãi mới đã sẵn sàng. Hãy nhanh tay sử dụng cho lần mua sắm tiếp theo nhé!
                      </p>

                      %s
                    </div>

                    <div style="height:5px;background:#dd6b20;"></div>
                  </div>
                </body>
                </html>
                """.formatted(
                    escape(customerName),
                    escape(voucherName),
                    escape(voucherCode),
                    escape(discount),
                    escape(formatCurrency(maxDiscount)),
                    escape(formatCurrency(minOrderValue)),
                    escape(formatDateTime(startDate)),
                    escape(formatDateTime(endDate)),
                    safeWebsiteUrl.isEmpty()
                            ? ""
                            : """
                          <div style="text-align:center;">
                            <a href="%s"
                               style="display:inline-block;padding:12px 28px;
                                      background:#dd6b20;color:#ffffff;
                                      text-decoration:none;border-radius:25px;
                                      font-size:13px;font-weight:bold;">
                              Mua sắm ngay &rarr;
                            </a>
                          </div>
                          """.formatted(safeWebsiteUrl)
            );

            String plainText = """
                Chào %s!

                SmashStep cập nhật ưu đãi cho bạn:
                Voucher: %s
                Mã sử dụng: %s
                Giá trị giảm mới: %s
                Giảm tối đa: %s
                Áp dụng cho đơn từ: %s
                Thời gian: %s đến %s

                Cảm ơn bạn đã đồng hành cùng SmashStep.
                """.formatted(
                    safeText(customerName),
                    safeText(voucherName),
                    safeText(voucherCode),
                    safeText(discount),
                    safeText(formatCurrency(maxDiscount)),
                    safeText(formatCurrency(minOrderValue)),
                    safeText(formatDateTime(startDate)),
                    safeText(formatDateTime(endDate))
            );

            helper.setText(plainText, html);
            mailSender.send(message);


        } catch (Exception e) {

            throw new IllegalStateException(
                    "Không thể gửi email cập nhật phiếu giảm giá", e);
        }
    }

    public void sendVoucherCancelledEmail(
            String to,
            String customerName,
            String voucherName,
            String voucherCode
    ) {
        JavaMailSender mailSender = senderFor(to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            if (from != null && !from.isBlank()) {
                helper.setFrom(from, "SmashStep");
            }
            helper.setTo(to);
            helper.setSubject("SmashStep - Thông báo về phiếu giảm giá");

            String html = """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:24px 10px;background:#f1f5f9;
                             font-family:Arial,Helvetica,sans-serif;color:#10243a;">
                  <div style="max-width:520px;margin:0 auto;background:#ffffff;
                              border:1px solid #dce5ed;border-radius:12px;overflow:hidden;">

                    <div style="padding:20px;text-align:center;
                                background:#071b32;">
                      <div style="font-size:25px;font-weight:900;letter-spacing:1px;
                                  color:#ffffff;">
                        SMASH<span style="color:#20d5f5;">STEP</span>
                      </div>
                      <div style="margin-top:5px;font-size:10px;letter-spacing:3px;
                                  color:#b9eefa;">
                        STEP INTO YOUR STYLE
                      </div>
                    </div>

                    <div style="padding:24px 22px;">
                      <h2 style="margin:0 0 14px;text-align:center;font-size:22px;
                                 color:#0b2540;">
                        Chào %s,
                      </h2>

                      <p style="font-size:14px;line-height:1.6;margin:0 0 18px;
                                color:#40546a;">
                        SmashStep xin thông báo phiếu giảm giá sau đã
                        <b style="color:#e53e3e;">ngừng áp dụng</b> cho tài khoản của bạn:
                      </p>

                      <div style="padding:16px;background:#fff5f5;
                                  border:1px solid #fed7d7;
                                  border-left:4px solid #e53e3e;
                                  border-radius:8px;font-size:13px;line-height:2;">
                        <div>Voucher: <b style="color:#0b2540;">%s</b></div>

                        <div>
                          <b>Mã phiếu:</b>
                          <span style="display:inline-block;background:#fed7d7;
                                       color:#9b2c2c;padding:3px 8px;border-radius:5px;
                                       font-weight:bold;letter-spacing:1px;">%s</span>
                        </div>

                        <div><b>Trạng thái:</b>
                          <span style="color:#e53e3e;font-weight:bold;">Đã hủy áp dụng</span>
                        </div>
                      </div>

                      <p style="font-size:13px;line-height:1.6;margin:18px 0;
                                color:#526579;">
                        SmashStep rất tiếc về sự thay đổi này và hy vọng sẽ mang đến cho bạn các chương trình ưu đãi hấp dẫn khác trong thời gian tới!
                      </p>
                    </div>

                    <div style="height:5px;background:#e53e3e;"></div>
                  </div>
                </body>
                </html>
                """.formatted(
                    escape(customerName),
                    escape(voucherName),
                    escape(voucherCode)
            );

            String plainText = """
                Chào %s!

                SmashStep xin thông báo phiếu giảm giá sau đã ngừng áp dụng cho tài khoản của bạn:
                Voucher: %s
                Mã phiếu: %s
                Trạng thái: Đã hủy áp dụng

                SmashStep rất tiếc về sự thay đổi này. Cảm ơn bạn đã luôn đồng hành cùng SmashStep!
                """.formatted(
                    safeText(customerName),
                    safeText(voucherName),
                    safeText(voucherCode)
            );

            helper.setText(plainText, html);
            mailSender.send(message);


        } catch (Exception e) {

            throw new IllegalStateException(
                    "Không thể gửi email thông báo hủy phiếu giảm giá", e);
        }
    }

    /**
     * Gửi email chứa link đặt lại mật khẩu cho khách hàng.
     */
    public void sendPasswordResetEmail(
            String to,
            String customerName,
            String resetLink,
            int expiryMinutes
    ) {
        JavaMailSender mailSender = senderFor(to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            if (from != null && !from.isBlank()) {
                helper.setFrom(from, "SmashStep");
            }
            helper.setTo(to);
            helper.setSubject("SmashStep - Yêu cầu đặt lại mật khẩu");

            String safeLink = isSafeHttpUrl(resetLink) ? escape(resetLink) : "";

            String html = """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="margin:0;padding:24px 10px;background:#f1f5f9;
                             font-family:Arial,Helvetica,sans-serif;color:#10243a;">
                  <div style="max-width:520px;margin:0 auto;background:#ffffff;
                              border:1px solid #dce5ed;border-radius:12px;overflow:hidden;">

                    <div style="padding:20px;text-align:center;background:#071b32;">
                      <div style="font-size:25px;font-weight:900;letter-spacing:1px;color:#ffffff;">
                        SMASH<span style="color:#20d5f5;">STEP</span>
                      </div>
                      <div style="margin-top:5px;font-size:10px;letter-spacing:3px;color:#b9eefa;">
                        STEP INTO YOUR STYLE
                      </div>
                    </div>

                    <div style="padding:32px 28px;">
                      <div style="text-align:center;margin-bottom:20px;">
                        <div style="width:60px;height:60px;border-radius:50%%;background:#e8f4fd;
                                    display:inline-flex;align-items:center;justify-content:center;
                                    font-size:28px;">&#128272;</div>
                      </div>

                      <h2 style="margin:0 0 10px;text-align:center;font-size:20px;color:#0b2540;">
                        &#272;&#7863;t l&#7841;i m&#7853;t kh&#7849;u
                      </h2>

                      <p style="font-size:14px;line-height:1.7;color:#40546a;margin:0 0 8px;">
                        Ch&#224;o <b>%s</b>,
                      </p>
                      <p style="font-size:14px;line-height:1.7;color:#40546a;margin:0 0 20px;">
                        Ch&#250;ng t&#244;i nh&#7853;n &#273;&#432;&#7907;c y&#234;u c&#7847;u &#273;&#7863;t l&#7841;i m&#7853;t kh&#7849;u cho t&#224;i kho&#7843;n c&#7911;a b&#7841;n.
                        Nh&#7845;n v&#224;o n&#250;t b&#234;n d&#432;&#7899;i &#273;&#7875; ti&#7871;p t&#7909;c. Link c&#243; hi&#7879;u l&#7921;c trong
                        <b style="color:#e67e22;">%d ph&#250;t</b>.
                      </p>

                      <div style="text-align:center;margin:24px 0;">
                        <a href="%s"
                           style="display:inline-block;padding:14px 36px;background:#079fc9;
                                  color:#ffffff;text-decoration:none;border-radius:25px;
                                  font-size:14px;font-weight:bold;">
                          &#272;&#7863;t l&#7841;i m&#7853;t kh&#7849;u &rarr;
                        </a>
                      </div>

                      <p style="font-size:12px;line-height:1.6;color:#8a9db0;
                                border-top:1px solid #edf2f7;padding-top:16px;margin:0;">
                        N&#7871;u b&#7841;n kh&#244;ng y&#234;u c&#7847;u &#273;&#7863;t l&#7841;i m&#7853;t kh&#7849;u, h&#227;y b&#7887; qua email n&#224;y.
                        T&#224;i kho&#7843;n c&#7911;a b&#7841;n v&#7851;n an to&#224;n.
                      </p>
                    </div>

                    <div style="height:5px;background:#20d5f5;"></div>
                  </div>
                </body>
                </html>
                """.formatted(escape(customerName), expiryMinutes, safeLink);

            String plainText = """
                Chào %s!

                Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.
                Truy cập link sau để đặt lại (hiệu lực %d phút):

                %s

                Nếu bạn không yêu cầu, hãy bỏ qua email này.

                SmashStep
                """.formatted(safeText(customerName), expiryMinutes, safeText(resetLink));

            helper.setText(plainText, html);
            mailSender.send(message);

        } catch (Exception e) {
            throw new IllegalStateException("Không thể gửi email đặt lại mật khẩu", e);
        }
    }

    private boolean isSafeHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String url = value.trim().toLowerCase();
        return url.startsWith("https://") || url.startsWith("http://");
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatCurrency(String value) {
        try {
            NumberFormat formatter = NumberFormat.getNumberInstance(
                    Locale.forLanguageTag("vi-VN")
            );
            formatter.setMaximumFractionDigits(0);
            return formatter.format(new java.math.BigDecimal(value)) + "đ";
        } catch (Exception e) {
            return value + "đ";
        }
    }

    private String formatDateTime(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        try {
            java.time.LocalDateTime dt = java.time.LocalDateTime.parse(value);
            return dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        } catch (Exception e) {
            return value.replace("T", " ");
        }
    }
}
