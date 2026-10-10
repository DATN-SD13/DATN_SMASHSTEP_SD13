
package com.smashstep.datn.promotion.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

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
        System.out.println("MAIL_USERNAME = " + System.getenv("MAIL_USERNAME"));

        String password = System.getenv("MAIL_PASSWORD");
        System.out.println("MAIL_PASSWORD tồn tại = "
                + (password != null && !password.isBlank()));
        System.out.println("Độ dài mật khẩu = "
                + (password == null ? 0 : password.trim().length()));
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail, "SmashStep");
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

            System.out.println("Gửi email thành công tới: " + to);

        } catch (Exception e) {
            System.err.println("Gửi email thất bại tới: " + to + ". Lý do: " + e.getMessage());
            e.printStackTrace();
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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail, "SmashStep");
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

            System.out.println("Gửi email cập nhật voucher thành công tới: " + to);

        } catch (Exception e) {
            System.err.println("Gửi email cập nhật voucher thất bại tới: " + to + ". Lý do: " + e.getMessage());
            e.printStackTrace();
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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail, "SmashStep");
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

            System.out.println("Gửi email hủy voucher thành công tới: " + to);

        } catch (Exception e) {
            System.err.println("Gửi email hủy voucher thất bại tới: " + to + ". Lý do: " + e.getMessage());
            e.printStackTrace();
            throw new IllegalStateException(
                    "Không thể gửi email thông báo hủy phiếu giảm giá", e);
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
