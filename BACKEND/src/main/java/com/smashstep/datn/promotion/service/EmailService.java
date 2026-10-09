
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
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) throw new IllegalStateException("SMTP chưa được cấu hình");
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            if (from != null && !from.isBlank()) helper.setFrom(from);
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
                        <div><b>Thời gian áp dụng:</b><br>%s đến %s</div>
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
                    escape(startDate),
                    escape(endDate),
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
                    safeText(startDate),
                    safeText(endDate)
            );

            helper.setText(plainText, html);
            mailSender.send(message);

            System.out.println("Gửi email thành công tới: " + to);

        } catch (MessagingException | MailException e) {
            System.err.println("Gửi email thất bại tới: " + to);
            throw new IllegalStateException(
                    "Không thể gửi email phiếu giảm giá", e);
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
}
