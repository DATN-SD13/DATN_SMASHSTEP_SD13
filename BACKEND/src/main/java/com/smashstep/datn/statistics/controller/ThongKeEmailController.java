
package com.smashstep.datn.statistics.controller;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.ApiResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/thong-ke-tong-hop")
@RequiredArgsConstructor
public class ThongKeEmailController {

    private final JavaMailSender mailSender;

    private final ThongKeTongQuanController thongKeTongQuan;

    @Value("${spring.mail.username:}")
    private String emailGui;

    // Thông tin người nhận và thời gian báo cáo
    public record EmailRequest(
            @NotBlank(message = "Vui lòng nhập email")
            @Email(message = "Email không hợp lệ")
            String email,

            @NotNull(message = "Vui lòng chọn từ ngày")
            LocalDate tuNgay,

            @NotNull(message = "Vui lòng chọn đến ngày")
            LocalDate denNgay
    ) {}

    @PostMapping("/gui-email-tong-ket")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> guiEmailTongKet(
            @Valid @RequestBody EmailRequest request
    ) {

        LocalDate tuNgay = request.tuNgay();
        LocalDate denNgay = request.denNgay();

        // Kiểm tra khoảng ngày
        if (tuNgay.isAfter(denNgay)) {
            throw AppException.badRequest(
                    "Từ ngày không được lớn hơn đến ngày"
            );
        }

        if (ChronoUnit.DAYS.between(tuNgay, denNgay) > 366) {
            throw AppException.badRequest(
                    "Chỉ hỗ trợ báo cáo tối đa 367 ngày"
            );
        }

        if (emailGui == null || emailGui.isBlank()) {
            throw new AppException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Chưa cấu hình tài khoản gửi email"
            );
        }

        // Lấy dữ liệu từ API Thống kê hiện có
        Map<String, Object> data =
                thongKeTongQuan.tongQuan(tuNgay, denNgay)
                        .getData();

        String subject =
                "SmashStep - Bao cao tong ket doanh thu "
                        + tuNgay + " den " + denNgay;

        // Thiết kế nội dung HTML cho email
        String html = """
            <html>
            <body style="font-family:Arial,sans-serif;
                         background:#f3f6fb;
                         padding:24px;">

              <div style="max-width:650px;
                          margin:auto;
                          background:white;
                          border-radius:14px;
                          overflow:hidden;">

                <div style="background:#1d4ed8;
                            color:white;
                            padding:24px;
                            text-align:center;">

                  <h2 style="margin:0;">
                    SMASHSTEP
                  </h2>

                  <p>BÁO CÁO TỔNG KẾT DOANH THU</p>
                </div>

                <div style="padding:24px;">

                  <p><b>Thời gian:</b> %s đến %s</p>

                  <table style="width:100%%;
                                border-collapse:collapse;">

                    %s

                  </table>

                  <p style="font-size:12px;
                            color:#64748b;
                            margin-top:24px;">

                    Doanh thu thực tế được tính từ
                    các đơn hàng hoàn thành.

                    Doanh thu dự kiến được tính từ
                    các đơn đã xác nhận hoặc
                    đang chờ lấy hàng.

                  </p>

                  <p style="color:#64748b;">
                    Trân trọng,<br>
                    <b>Hệ thống quản lý SmashStep</b>
                  </p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                tuNgay,
                denNgay,
                taoBangBaoCao(data)
        );

        try {
            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            false,
                            "UTF-8"
                    );

            helper.setFrom(emailGui);
            helper.setTo(request.email().trim());
            helper.setSubject(subject);
            helper.setText(html, true);

            // Gửi email thật qua SMTP
            mailSender.send(message);

        } catch (MailException | MessagingException e) {
            throw new AppException(
                    HttpStatus.BAD_GATEWAY,
                    "Không gửi được email. "
                            + "Vui lòng kiểm tra cấu hình SMTP."
            );
        }

        return ApiResponse.ok(
                "Gửi báo cáo tổng kết doanh thu thành công"
        );
    }

    // Tạo từng dòng trong bảng email
    private String taoDong(String nhan, String giaTri) {
        return """
            <tr>
              <td style="padding:12px;
                         border-bottom:1px solid #e2e8f0;">
                %s
              </td>

              <td style="padding:12px;
                         border-bottom:1px solid #e2e8f0;
                         text-align:right;
                         font-weight:bold;">
                %s
              </td>
            </tr>
            """.formatted(nhan, giaTri);
    }

    // Tạo bảng số liệu thống kê
    private String taoBangBaoCao(Map<String, Object> data) {

        return taoDong(
                "Doanh thu thực tế",
                tien(data.get("totalRevenue"))
        )
                + taoDong(
                "Doanh thu dự kiến",
                tien(data.get("expectedRevenue"))
        )
                + taoDong(
                "Tổng tiền mặt",
                tien(data.get("cashRevenue"))
        )
                + taoDong(
                "Tổng chuyển khoản",
                tien(data.get("transferRevenue"))
        )
                + taoDong(
                "Tổng đơn hoàn thành",
                so(data.get("totalOrders"))
        )
                + taoDong(
                "Giá trị trung bình đơn (AOV)",
                tien(data.get("averageOrderValue"))
        )
                + taoDong(
                "Sản phẩm đã bán",
                so(data.get("productsSold"))
        )
                + taoDong(
                "Khách hàng mới",
                so(data.get("newCustomers"))
        );
    }

    // Định dạng số lượng
    private String so(Object value) {
        if (value == null) return "0";

        return NumberFormat
                .getNumberInstance(
                        Locale.forLanguageTag("vi-VN")
                )
                .format(new BigDecimal(value.toString()));
    }

    // Định dạng tiền Việt Nam
    private String tien(Object value) {
        return so(value) + " đ";
    }
}
