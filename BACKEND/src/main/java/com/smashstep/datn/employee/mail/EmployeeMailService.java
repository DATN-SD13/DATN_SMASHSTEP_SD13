package com.smashstep.datn.employee.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeMailService {

    private final JavaMailSender mailSender;

    /*
     * Gmail dùng để gửi email.
     *
     * application.properties:
     * spring.mail.username=${MAIL_USERNAME}
     */
    @Value("${spring.mail.username}")
    private String fromEmail;

    /*
     * Link frontend.
     *
     * Mặc định:
     * http://localhost:5173
     */
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;


    // =========================================================
    // GỬI EMAIL TÀI KHOẢN NHÂN VIÊN
    // =========================================================

    // Retain the existing manager account API while sharing the new role-aware template.
    public void sendManagerAccountEmail(String toEmail, String employeeName, String username, String temporaryPassword) {
        sendEmployeeAccountEmail(toEmail, employeeName, "Quản lý", username, temporaryPassword);
    }

    public void sendEmployeeAccountEmail(
            String toEmail,
            String employeeName,
            String roleName,
            String username,
            String temporaryPassword
    ) {

        try {

            MimeMessage message =
                    mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(
                            message,
                            true,
                            "UTF-8"
                    );

            /*
             * Người gửi
             */
            helper.setFrom(
                    fromEmail
            );

            /*
             * Người nhận
             */
            helper.setTo(
                    toEmail
            );

            /*
             * Tiêu đề email
             */
            helper.setSubject(
                    "SmashStep - Tài khoản nội bộ của bạn đã được tạo"
            );

            /*
             * Nội dung HTML
             */
            helper.setText(
                    buildHtml(
                            employeeName,
                            roleName,
                            username,
                            temporaryPassword
                    ),
                    true
            );

            /*
             * Gửi email
             */
            mailSender.send(
                    message
            );

        } catch (
                MessagingException |
                MailException e
        ) {

            throw new RuntimeException(
                    "Không thể gửi email tài khoản tới "
                            + toEmail
                            + ". Vui lòng kiểm tra cấu hình Gmail.",
                    e
            );
        }
    }


    // =========================================================
    // TẠO GIAO DIỆN EMAIL
    // =========================================================

    private String buildHtml(
            String employeeName,
            String roleName,
            String username,
            String temporaryPassword
    ) {

        /*
         * Escape dữ liệu trước khi đưa vào HTML.
         */
        String safeName =
                escapeHtml(employeeName);

        String safeRole =
                escapeHtml(roleName);

        String safeUsername =
                escapeHtml(username);

        String safePassword =
                escapeHtml(temporaryPassword);

        String safeFrontendUrl =
                escapeHtml(frontendUrl);


        /*
         * Email HTML.
         *
         * Màu chính:
         *
         * #2F80ED  -> xanh chính
         * #1D4ED8  -> xanh đậm
         * #2563EB  -> xanh chữ
         * #EFF6FF  -> xanh rất nhạt
         * #BFDBFE  -> viền xanh
         * #F8FBFF  -> nền trắng xanh
         */
        return """
                <!DOCTYPE html>
                <html lang="vi">

                <head>

                    <meta charset="UTF-8">

                    <meta
                        name="viewport"
                        content="width=device-width, initial-scale=1.0"
                    >

                    <title>
                        SmashStep Internal
                    </title>

                </head>


                <body
                    style="
                        margin:0;
                        padding:0;
                        background:#F3F6FB;
                        font-family:
                            Arial,
                            Helvetica,
                            sans-serif;
                        color:#111827;
                    "
                >


                <!-- ================================================= -->
                <!-- BACKGROUND EMAIL -->
                <!-- ================================================= -->

                <table
                    width="100%%"
                    cellpadding="0"
                    cellspacing="0"
                    border="0"
                    style="
                        width:100%%;
                        background:#F3F6FB;
                        padding:40px 15px;
                    "
                >

                    <tr>

                        <td align="center">


                            <!-- ===================================== -->
                            <!-- EMAIL CONTAINER -->
                            <!-- ===================================== -->

                            <table
                                width="920"
                                cellpadding="0"
                                cellspacing="0"
                                border="0"
                                style="
                                    width:100%%;
                                    max-width:920px;
                                    background:#FFFFFF;
                                    border-radius:20px;
                                    overflow:hidden;
                                    box-shadow:
                                        0 15px 45px
                                        rgba(30,64,175,0.12);
                                "
                            >


                                <tr>


                                    <!-- ================================= -->
                                    <!-- PANEL BÊN TRÁI -->
                                    <!-- ================================= -->

                                    <td
                                        width="46%%"
                                        valign="top"
                                        style="
                                            width:46%%;
                                            padding:50px 38px;

                                            color:#FFFFFF;

                                            background:
                                                linear-gradient(
                                                    135deg,
                                                    #1D4ED8,
                                                    #2F80ED
                                                );
                                        "
                                    >


                                        <!-- BRAND BADGE -->

                                        <div
                                            style="
                                                display:inline-block;

                                                padding:
                                                    7px 14px;

                                                border:
                                                    1px solid
                                                    rgba(
                                                        255,
                                                        255,
                                                        255,
                                                        0.55
                                                    );

                                                border-radius:
                                                    999px;

                                                font-size:
                                                    11px;

                                                font-weight:
                                                    700;

                                                letter-spacing:
                                                    1px;
                                            "
                                        >
                                            SMASHSTEP
                                        </div>



                                        <!-- BRAND NAME -->

                                        <div
                                            style="
                                                margin-top:
                                                    25px;

                                                font-size:
                                                    40px;

                                                line-height:
                                                    1.05;

                                                font-weight:
                                                    800;

                                                color:
                                                    #FFFFFF;
                                            "
                                        >
                                            SmashStep
                                        </div>


                                        <div
                                            style="
                                                font-size:
                                                    38px;

                                                line-height:
                                                    1.1;

                                                font-weight:
                                                    300;

                                                color:
                                                    #FFFFFF;
                                            "
                                        >
                                            Internal
                                        </div>



                                        <!-- DESCRIPTION -->

                                        <div
                                            style="
                                                margin-top:
                                                    26px;

                                                max-width:
                                                    280px;

                                                font-size:
                                                    15px;

                                                line-height:
                                                    1.7;

                                                color:
                                                    rgba(
                                                        255,
                                                        255,
                                                        255,
                                                        0.92
                                                    );
                                            "
                                        >
                                            Hệ thống quản lý nội bộ
                                            chuyên nghiệp và hiệu quả
                                            dành cho cửa hàng
                                            SmashStep.
                                        </div>



                                        <!-- ================================= -->
                                        <!-- FEATURE -->
                                        <!-- ================================= -->

                                        <table
                                            width="100%%"
                                            cellpadding="0"
                                            cellspacing="0"
                                            border="0"
                                            style="
                                                margin-top:
                                                    80px;

                                                color:
                                                    #FFFFFF;
                                            "
                                        >

                                            <tr>


                                                <!-- BẢO MẬT -->

                                                <td
                                                    width="33%%"
                                                    valign="top"
                                                    style="
                                                        width:
                                                            33%%;

                                                        padding-right:
                                                            8px;
                                                    "
                                                >

                                                    <div
                                                        style="
                                                            font-size:
                                                                25px;

                                                            margin-bottom:
                                                                10px;
                                                        "
                                                    >
                                                        🛡️
                                                    </div>


                                                    <div
                                                        style="
                                                            font-size:
                                                                13px;

                                                            font-weight:
                                                                700;
                                                        "
                                                    >
                                                        Bảo mật
                                                    </div>


                                                    <div
                                                        style="
                                                            margin-top:
                                                                6px;

                                                            font-size:
                                                                10px;

                                                            line-height:
                                                                1.5;

                                                            color:
                                                                rgba(
                                                                    255,
                                                                    255,
                                                                    255,
                                                                    0.80
                                                                );
                                                        "
                                                    >
                                                        An toàn
                                                        thông tin
                                                    </div>

                                                </td>



                                                <!-- HIỆU QUẢ -->

                                                <td
                                                    width="33%%"
                                                    valign="top"
                                                    style="
                                                        width:
                                                            33%%;

                                                        padding-right:
                                                            8px;
                                                    "
                                                >

                                                    <div
                                                        style="
                                                            font-size:
                                                                25px;

                                                            margin-bottom:
                                                                10px;
                                                        "
                                                    >
                                                        ⚡
                                                    </div>


                                                    <div
                                                        style="
                                                            font-size:
                                                                13px;

                                                            font-weight:
                                                                700;
                                                        "
                                                    >
                                                        Hiệu quả
                                                    </div>


                                                    <div
                                                        style="
                                                            margin-top:
                                                                6px;

                                                            font-size:
                                                                10px;

                                                            line-height:
                                                                1.5;

                                                            color:
                                                                rgba(
                                                                    255,
                                                                    255,
                                                                    255,
                                                                    0.80
                                                                );
                                                        "
                                                    >
                                                        Tối ưu
                                                        quy trình
                                                    </div>

                                                </td>



                                                <!-- KẾT NỐI -->

                                                <td
                                                    width="33%%"
                                                    valign="top"
                                                    style="
                                                        width:
                                                            33%%;
                                                    "
                                                >

                                                    <div
                                                        style="
                                                            font-size:
                                                                25px;

                                                            margin-bottom:
                                                                10px;
                                                        "
                                                    >
                                                        👥
                                                    </div>


                                                    <div
                                                        style="
                                                            font-size:
                                                                13px;

                                                            font-weight:
                                                                700;
                                                        "
                                                    >
                                                        Kết nối
                                                    </div>


                                                    <div
                                                        style="
                                                            margin-top:
                                                                6px;

                                                            font-size:
                                                                10px;

                                                            line-height:
                                                                1.5;

                                                            color:
                                                                rgba(
                                                                    255,
                                                                    255,
                                                                    255,
                                                                    0.80
                                                                );
                                                        "
                                                    >
                                                        Làm việc
                                                        cùng nhau
                                                    </div>

                                                </td>

                                            </tr>

                                        </table>


                                    </td>



                                    <!-- ================================= -->
                                    <!-- PANEL BÊN PHẢI -->
                                    <!-- ================================= -->

                                    <td
                                        width="54%%"
                                        valign="top"
                                        style="
                                            width:54%%;

                                            padding:
                                                45px 40px;

                                            background:
                                                #FFFFFF;
                                        "
                                    >


                                        <!-- GREETING -->

                                        <div
                                            style="
                                                font-size:
                                                    20px;

                                                font-weight:
                                                    500;

                                                color:
                                                    #111827;
                                            "
                                        >
                                            👤 Xin chào,
                                        </div>



                                        <!-- EMPLOYEE NAME -->

                                        <div
                                            style="
                                                margin-top:
                                                    5px;

                                                font-size:
                                                    26px;

                                                line-height:
                                                    1.3;

                                                font-weight:
                                                    800;

                                                color:
                                                    #2563EB;
                                            "
                                        >
                                            %s 👋
                                        </div>



                                        <!-- DESCRIPTION -->

                                        <div
                                            style="
                                                margin-top:
                                                    22px;

                                                color:
                                                    #64748B;

                                                font-size:
                                                    13px;

                                                line-height:
                                                    1.8;
                                            "
                                        >

                                            Tài khoản truy cập hệ thống
                                            SmashStep Internal của bạn
                                            đã được tạo thành công.

                                            <br>

                                            Vui lòng sử dụng thông tin
                                            dưới đây để đăng nhập.

                                        </div>



                                        <!-- ================================= -->
                                        <!-- ROLE BADGE -->
                                        <!-- ================================= -->

                                        <div
                                            style="
                                                margin-top:
                                                    18px;

                                                display:
                                                    inline-block;

                                                padding:
                                                    7px 13px;

                                                border-radius:
                                                    999px;

                                                background:
                                                    #EFF6FF;

                                                color:
                                                    #2563EB;

                                                font-size:
                                                    12px;

                                                font-weight:
                                                    700;

                                                border:
                                                    1px solid
                                                    #DBEAFE;
                                            "
                                        >
                                            Vai trò: %s
                                        </div>



                                        <!-- ================================= -->
                                        <!-- ACCOUNT BOX -->
                                        <!-- ================================= -->

                                        <table
                                            width="100%%"
                                            cellpadding="0"
                                            cellspacing="0"
                                            border="0"
                                            style="
                                                width:
                                                    100%%;

                                                margin-top:
                                                    22px;

                                                border:
                                                    1px solid
                                                    #BFDBFE;

                                                border-radius:
                                                    12px;

                                                background:
                                                    #F8FBFF;

                                                border-collapse:
                                                    separate;

                                                overflow:
                                                    hidden;
                                            "
                                        >


                                            <!-- USERNAME -->

                                            <tr>


                                                <td
                                                    style="
                                                        padding:
                                                            17px 18px;

                                                        border-bottom:
                                                            1px solid
                                                            #BFDBFE;
                                                    "
                                                >

                                                    <div
                                                        style="
                                                            color:
                                                                #2563EB;

                                                            font-size:
                                                                11px;

                                                            font-weight:
                                                                700;
                                                        "
                                                    >
                                                        👤
                                                        &nbsp;
                                                        TÊN TÀI KHOẢN
                                                    </div>

                                                </td>



                                                <td
                                                    align="right"
                                                    style="
                                                        padding:
                                                            17px 18px;

                                                        border-bottom:
                                                            1px solid
                                                            #BFDBFE;

                                                        color:
                                                            #111827;

                                                        font-size:
                                                            15px;

                                                        font-weight:
                                                            800;

                                                        letter-spacing:
                                                            0.5px;
                                                    "
                                                >
                                                    %s
                                                </td>


                                            </tr>



                                            <!-- PASSWORD -->

                                            <tr>


                                                <td
                                                    style="
                                                        padding:
                                                            17px 18px;
                                                    "
                                                >

                                                    <div
                                                        style="
                                                            color:
                                                                #2563EB;

                                                            font-size:
                                                                11px;

                                                            font-weight:
                                                                700;
                                                        "
                                                    >
                                                        🔒
                                                        &nbsp;
                                                        MẬT KHẨU TẠM THỜI
                                                    </div>

                                                </td>



                                                <td
                                                    align="right"
                                                    style="
                                                        padding:
                                                            17px 18px;

                                                        color:
                                                            #111827;

                                                        font-size:
                                                            15px;

                                                        font-weight:
                                                            800;

                                                        letter-spacing:
                                                            1px;
                                                    "
                                                >
                                                    %s
                                                </td>


                                            </tr>


                                        </table>



                                        <!-- ================================= -->
                                        <!-- LOGIN BUTTON -->
                                        <!-- ================================= -->

                                        <table
                                            width="100%%"
                                            cellpadding="0"
                                            cellspacing="0"
                                            border="0"
                                            style="
                                                margin-top:
                                                    26px;
                                            "
                                        >

                                            <tr>

                                                <td align="center">


                                                    <a
                                                        href="%s"

                                                        style="
                                                            display:
                                                                inline-block;

                                                            min-width:
                                                                220px;

                                                            padding:
                                                                15px 25px;

                                                            border-radius:
                                                                9px;

                                                            background:
                                                                #2F80ED;

                                                            color:
                                                                #FFFFFF;

                                                            text-decoration:
                                                                none;

                                                            font-size:
                                                                14px;

                                                            font-weight:
                                                                700;

                                                            box-shadow:
                                                                0 7px 18px
                                                                rgba(
                                                                    47,
                                                                    128,
                                                                    237,
                                                                    0.25
                                                                );
                                                        "
                                                    >
                                                        ➜
                                                        &nbsp;
                                                        Đăng nhập hệ thống
                                                    </a>


                                                </td>

                                            </tr>

                                        </table>



                                        <!-- ================================= -->
                                        <!-- WARNING -->
                                        <!-- ================================= -->

                                        <div
                                            style="
                                                margin-top:
                                                    24px;

                                                padding:
                                                    15px 17px;

                                                border-radius:
                                                    10px;

                                                background:
                                                    #FFF7ED;

                                                border:
                                                    1px solid
                                                    #FED7AA;

                                                color:
                                                    #92400E;

                                                font-size:
                                                    12px;

                                                line-height:
                                                    1.7;
                                            "
                                        >

                                            ⚠️
                                            &nbsp;

                                            Đây là mật khẩu tạm thời.
                                            Vui lòng đổi mật khẩu ngay
                                            sau lần đăng nhập đầu tiên
                                            để đảm bảo an toàn tài khoản.

                                        </div>



                                        <!-- ================================= -->
                                        <!-- SECURITY INFO -->
                                        <!-- ================================= -->

                                        <div
                                            style="
                                                margin-top:
                                                    16px;

                                                padding:
                                                    13px 16px;

                                                border-radius:
                                                    10px;

                                                background:
                                                    #EFF6FF;

                                                color:
                                                    #475569;

                                                font-size:
                                                    11px;

                                                line-height:
                                                    1.7;
                                            "
                                        >

                                            🔐
                                            Vì lý do bảo mật,
                                            không chia sẻ tên tài khoản
                                            và mật khẩu này cho người khác.

                                        </div>



                                        <!-- ================================= -->
                                        <!-- FOOTER -->
                                        <!-- ================================= -->

                                        <div
                                            style="
                                                margin-top:
                                                    27px;

                                                padding-top:
                                                    18px;

                                                border-top:
                                                    1px solid
                                                    #E5E7EB;

                                                color:
                                                    #94A3B8;

                                                font-size:
                                                    10px;

                                                line-height:
                                                    1.7;

                                                text-align:
                                                    center;
                                            "
                                        >

                                            Email được gửi tự động từ
                                            hệ thống

                                            <strong
                                                style="
                                                    color:
                                                        #2563EB;
                                                "
                                            >
                                                SmashStep Internal
                                            </strong>.

                                            <br>

                                            Vui lòng không trả lời
                                            email này.

                                        </div>


                                    </td>


                                </tr>


                            </table>


                            <!-- ===================================== -->
                            <!-- COPYRIGHT -->
                            <!-- ===================================== -->

                            <div
                                style="
                                    margin-top:
                                        18px;

                                    color:
                                        #94A3B8;

                                    font-size:
                                        10px;

                                    text-align:
                                        center;
                                "
                            >
                                © SmashStep Internal
                                · Hệ thống quản lý cửa hàng
                            </div>


                        </td>

                    </tr>

                </table>


                </body>

                </html>
                """.formatted(
                safeName,
                safeRole,
                safeUsername,
                safePassword,
                safeFrontendUrl
        );
    }


    // =========================================================
    // ESCAPE HTML
    // =========================================================

    private String escapeHtml(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace(
                        "&",
                        "&amp;"
                )
                .replace(
                        "<",
                        "&lt;"
                )
                .replace(
                        ">",
                        "&gt;"
                )
                .replace(
                        "\"",
                        "&quot;"
                )
                .replace(
                        "'",
                        "&#39;"
                );
    }
}