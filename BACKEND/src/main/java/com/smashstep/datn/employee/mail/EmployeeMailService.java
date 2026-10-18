package com.smashstep.datn.employee.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmployeeMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${smashstep.website-url:http://localhost:5173}")
    private String frontendUrl;

    public EmployeeMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Gửi email tài khoản cho nhân viên mới.
     *
     * @param toEmail           Email người nhận
     * @param employeeName      Tên nhân viên
     * @param roleName          Tên vai trò
     * @param username          Tên đăng nhập
     * @param temporaryPassword Mật khẩu tạm thời
     */
    public void sendEmployeeAccountEmail(
            String toEmail,
            String employeeName,
            String roleName,
            String username,
            String temporaryPassword
    ) {
        if (isBlank(toEmail)) {
            throw new IllegalArgumentException(
                    "Email nhân viên không được để trống"
            );
        }

        if (isBlank(username)) {
            throw new IllegalArgumentException(
                    "Tên đăng nhập không được để trống"
            );
        }

        if (isBlank(temporaryPassword)) {
            throw new IllegalArgumentException(
                    "Mật khẩu tạm thời không được để trống"
            );
        }

        String safeName = escapeHtml(
                isBlank(employeeName)
                        ? "Nhân viên"
                        : employeeName.trim()
        );

        String safeRole = escapeHtml(
                isBlank(roleName)
                        ? "Nhân viên"
                        : roleName.trim()
        );

        String safeUsername = escapeHtml(username.trim());

        String safePassword = escapeHtml(temporaryPassword);

        String safeFrontendUrl = escapeHtml(
                isBlank(frontendUrl)
                        ? "http://localhost:5173"
                        : frontendUrl.trim()
        );

        String html = buildHtml(
                safeName,
                safeRole,
                safeUsername,
                safePassword,
                safeFrontendUrl
        );

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail);
            helper.setTo(toEmail.trim());

            helper.setSubject(
                    "SmashStep - Tài khoản nội bộ của bạn đã được tạo"
            );

            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new IllegalStateException(
                    "Không thể tạo email tài khoản nhân viên",
                    e
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Không thể gửi email tài khoản nhân viên đến "
                            + toEmail,
                    e
            );
        }
    }

    /**
     * =========================================================
     * METHOD TƯƠNG THÍCH VỚI CODE/TEST CŨ TỪ MAIN
     * =========================================================
     *
     * ManagerAccountServiceTest đang gọi:
     *
     * sendManagerAccountEmail(
     *      email,
     *      employeeName,
     *      username,
     *      temporaryPassword
     * )
     *
     * Không xóa method này nếu ManagerAccountServiceTest
     * vẫn còn sử dụng.
     */
    public void sendManagerAccountEmail(
            String toEmail,
            String employeeName,
            String username,
            String temporaryPassword
    ) {
        sendEmployeeAccountEmail(
                toEmail,
                employeeName,
                "Quản lý",
                username,
                temporaryPassword
        );
    }

    /**
     * Tạo nội dung HTML email.
     */
    private String buildHtml(
            String employeeName,
            String roleName,
            String username,
            String temporaryPassword,
            String loginUrl
    ) {

        String html = """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">

                    <title>SmashStep Internal</title>
                </head>

                <body style="
                    margin:0;
                    padding:0;
                    background:#f4f8ff;
                    font-family:Arial,Helvetica,sans-serif;
                    color:#1f2937;
                ">

                <table
                    width="100%"
                    cellpadding="0"
                    cellspacing="0"
                    border="0"
                    style="background:#f4f8ff;padding:32px 12px;"
                >
                    <tr>
                        <td align="center">

                            <table
                                width="100%"
                                cellpadding="0"
                                cellspacing="0"
                                border="0"
                                style="
                                    max-width:900px;
                                    background:#ffffff;
                                    border-radius:22px;
                                    overflow:hidden;
                                    box-shadow:
                                        0 18px 45px rgba(37,99,235,0.12);
                                "
                            >

                                <tr>

                                    <!-- LEFT -->
                                    <td
                                        width="36%"
                                        valign="top"
                                        style="
                                            background:
                                                linear-gradient(
                                                    145deg,
                                                    #2563eb,
                                                    #2f80ed,
                                                    #60a5fa
                                                );
                                            padding:44px 32px;
                                            color:#ffffff;
                                        "
                                    >

                                        <div
                                            style="
                                                font-size:30px;
                                                font-weight:800;
                                                letter-spacing:-1px;
                                                margin-bottom:5px;
                                            "
                                        >
                                            SmashStep
                                        </div>

                                        <div
                                            style="
                                                font-size:13px;
                                                font-weight:700;
                                                letter-spacing:2px;
                                                opacity:0.9;
                                                text-transform:uppercase;
                                            "
                                        >
                                            Internal
                                        </div>

                                        <div
                                            style="
                                                width:48px;
                                                height:4px;
                                                border-radius:10px;
                                                background:#ffffff;
                                                opacity:0.85;
                                                margin:24px 0;
                                            "
                                        ></div>

                                        <div
                                            style="
                                                font-size:21px;
                                                font-weight:700;
                                                line-height:1.4;
                                                margin-bottom:14px;
                                            "
                                        >
                                            Chào mừng bạn đến với đội ngũ
                                            SmashStep
                                        </div>

                                        <div
                                            style="
                                                font-size:14px;
                                                line-height:1.7;
                                                opacity:0.92;
                                            "
                                        >
                                            Tài khoản nội bộ của bạn đã được
                                            khởi tạo thành công.
                                        </div>

                                        <div style="margin-top:35px;">

                                            <div
                                                style="
                                                    margin-bottom:18px;
                                                    font-size:14px;
                                                    line-height:1.5;
                                                "
                                            >
                                                <span
                                                    style="
                                                        display:inline-block;
                                                        width:30px;
                                                        height:30px;
                                                        line-height:30px;
                                                        text-align:center;
                                                        background:
                                                            rgba(
                                                                255,
                                                                255,
                                                                255,
                                                                0.16
                                                            );
                                                        border-radius:8px;
                                                        margin-right:8px;
                                                    "
                                                >
                                                    ✓
                                                </span>

                                                Quản lý công việc thuận tiện
                                            </div>

                                            <div
                                                style="
                                                    margin-bottom:18px;
                                                    font-size:14px;
                                                    line-height:1.5;
                                                "
                                            >
                                                <span
                                                    style="
                                                        display:inline-block;
                                                        width:30px;
                                                        height:30px;
                                                        line-height:30px;
                                                        text-align:center;
                                                        background:
                                                            rgba(
                                                                255,
                                                                255,
                                                                255,
                                                                0.16
                                                            );
                                                        border-radius:8px;
                                                        margin-right:8px;
                                                    "
                                                >
                                                    ✓
                                                </span>

                                                Truy cập hệ thống SmashStep
                                            </div>

                                            <div
                                                style="
                                                    font-size:14px;
                                                    line-height:1.5;
                                                "
                                            >
                                                <span
                                                    style="
                                                        display:inline-block;
                                                        width:30px;
                                                        height:30px;
                                                        line-height:30px;
                                                        text-align:center;
                                                        background:
                                                            rgba(
                                                                255,
                                                                255,
                                                                255,
                                                                0.16
                                                            );
                                                        border-radius:8px;
                                                        margin-right:8px;
                                                    "
                                                >
                                                    ✓
                                                </span>

                                                Bảo mật thông tin tài khoản
                                            </div>

                                        </div>

                                    </td>

                                    <!-- RIGHT -->
                                    <td
                                        width="64%"
                                        valign="top"
                                        style="
                                            padding:44px 42px;
                                            background:#ffffff;
                                        "
                                    >

                                        <div
                                            style="
                                                color:#64748b;
                                                font-size:14px;
                                                margin-bottom:7px;
                                            "
                                        >
                                            Xin chào,
                                        </div>

                                        <div
                                            style="
                                                font-size:28px;
                                                font-weight:800;
                                                color:#0f172a;
                                                margin-bottom:12px;
                                            "
                                        >
                                            {{EMPLOYEE_NAME}}
                                        </div>

                                        <div style="margin-bottom:24px;">

                                            <span
                                                style="
                                                    display:inline-block;
                                                    padding:7px 14px;
                                                    border-radius:20px;
                                                    background:#eff6ff;
                                                    color:#2563eb;
                                                    border:1px solid #bfdbfe;
                                                    font-size:13px;
                                                    font-weight:700;
                                                "
                                            >
                                                Vai trò: {{ROLE_NAME}}
                                            </span>

                                        </div>

                                        <div
                                            style="
                                                color:#475569;
                                                font-size:14px;
                                                line-height:1.7;
                                                margin-bottom:24px;
                                            "
                                        >
                                            Tài khoản làm việc trên hệ thống
                                            <strong>SmashStep Internal</strong>
                                            của bạn đã được tạo.
                                            Dưới đây là thông tin đăng nhập
                                            ban đầu.
                                        </div>

                                        <!-- ACCOUNT BOX -->
                                        <table
                                            width="100%"
                                            cellpadding="0"
                                            cellspacing="0"
                                            border="0"
                                            style="
                                                background:#f8fbff;
                                                border:1px solid #dbeafe;
                                                border-radius:14px;
                                                margin-bottom:24px;
                                            "
                                        >

                                            <tr>
                                                <td
                                                    style="
                                                        padding:
                                                            20px 22px
                                                            10px 22px;
                                                    "
                                                >

                                                    <div
                                                        style="
                                                            color:#64748b;
                                                            font-size:11px;
                                                            font-weight:700;
                                                            letter-spacing:
                                                                1.2px;
                                                            margin-bottom:7px;
                                                        "
                                                    >
                                                        TÊN TÀI KHOẢN
                                                    </div>

                                                    <div
                                                        style="
                                                            color:#1d4ed8;
                                                            font-size:18px;
                                                            font-weight:800;
                                                            word-break:
                                                                break-all;
                                                        "
                                                    >
                                                        {{USERNAME}}
                                                    </div>

                                                </td>
                                            </tr>

                                            <tr>
                                                <td
                                                    style="
                                                        padding:
                                                            10px 22px
                                                            20px 22px;
                                                    "
                                                >

                                                    <div
                                                        style="
                                                            border-top:
                                                                1px solid
                                                                #dbeafe;
                                                            padding-top:
                                                                16px;
                                                        "
                                                    >

                                                        <div
                                                            style="
                                                                color:#64748b;
                                                                font-size:
                                                                    11px;
                                                                font-weight:
                                                                    700;
                                                                letter-spacing:
                                                                    1.2px;
                                                                margin-bottom:
                                                                    7px;
                                                            "
                                                        >
                                                            MẬT KHẨU
                                                            TẠM THỜI
                                                        </div>

                                                        <div
                                                            style="
                                                                color:#0f172a;
                                                                font-size:
                                                                    18px;
                                                                font-weight:
                                                                    800;
                                                                font-family:
                                                                    Consolas,
                                                                    Monaco,
                                                                    monospace;
                                                                word-break:
                                                                    break-all;
                                                            "
                                                        >
                                                            {{PASSWORD}}
                                                        </div>

                                                    </div>

                                                </td>
                                            </tr>

                                        </table>

                                        <!-- LOGIN BUTTON -->
                                        <table
                                            cellpadding="0"
                                            cellspacing="0"
                                            border="0"
                                            style="margin-bottom:24px;"
                                        >
                                            <tr>
                                                <td
                                                    align="center"
                                                    bgcolor="#2F80ED"
                                                    style="
                                                        border-radius:10px;
                                                    "
                                                >

                                                    <a
                                                        href="{{LOGIN_URL}}"
                                                        style="
                                                            display:
                                                                inline-block;
                                                            padding:
                                                                14px 28px;
                                                            color:#ffffff;
                                                            text-decoration:
                                                                none;
                                                            font-size:14px;
                                                            font-weight:700;
                                                            background:
                                                                #2f80ed;
                                                            border-radius:
                                                                10px;
                                                        "
                                                    >
                                                        Đăng nhập SmashStep
                                                    </a>

                                                </td>
                                            </tr>
                                        </table>

                                        <!-- WARNING -->
                                        <div
                                            style="
                                                background:#fffbeb;
                                                border:1px solid #fde68a;
                                                border-radius:12px;
                                                padding:15px 17px;
                                                margin-bottom:18px;
                                            "
                                        >

                                            <div
                                                style="
                                                    color:#92400e;
                                                    font-size:13px;
                                                    font-weight:700;
                                                    margin-bottom:5px;
                                                "
                                            >
                                                ⚠ Lưu ý bảo mật
                                            </div>

                                            <div
                                                style="
                                                    color:#a16207;
                                                    font-size:12px;
                                                    line-height:1.6;
                                                "
                                            >
                                                Đây là mật khẩu tạm thời.
                                                Vui lòng đổi mật khẩu sau khi
                                                đăng nhập lần đầu và không
                                                chia sẻ thông tin đăng nhập
                                                với người khác.
                                            </div>

                                        </div>

                                        <!-- SECURITY -->
                                        <div
                                            style="
                                                background:#eff6ff;
                                                border-left:
                                                    4px solid #2f80ed;
                                                border-radius:8px;
                                                padding:13px 15px;
                                                color:#475569;
                                                font-size:12px;
                                                line-height:1.6;
                                            "
                                        >
                                            🔒 SmashStep sẽ không yêu cầu bạn
                                            cung cấp mật khẩu qua email,
                                            tin nhắn hoặc điện thoại.
                                        </div>

                                        <div
                                            style="
                                                border-top:
                                                    1px solid #e2e8f0;
                                                margin-top:28px;
                                                padding-top:18px;
                                                color:#94a3b8;
                                                font-size:11px;
                                                line-height:1.6;
                                            "
                                        >
                                            Email này được gửi tự động từ hệ
                                            thống SmashStep Internal.
                                            <br>
                                            Vui lòng không trả lời email này.
                                        </div>

                                    </td>

                                </tr>

                            </table>

                            <div
                                style="
                                    color:#94a3b8;
                                    font-size:11px;
                                    margin-top:18px;
                                "
                            >
                                © SmashStep Internal
                            </div>

                        </td>
                    </tr>
                </table>

                </body>
                </html>
                """;

        /*
         * Dùng replace thay vì String.formatted()
         * để mật khẩu chứa ký tự % không gây lỗi format.
         */
        return html
                .replace("{{EMPLOYEE_NAME}}", employeeName)
                .replace("{{ROLE_NAME}}", roleName)
                .replace("{{USERNAME}}", username)
                .replace("{{PASSWORD}}", temporaryPassword)
                .replace("{{LOGIN_URL}}", loginUrl);
    }

    /**
     * Kiểm tra String rỗng.
     */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Escape dữ liệu trước khi đưa vào HTML email.
     */
    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}