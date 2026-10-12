package com.smashstep.datn.employee.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendManagerAccountEmail(
            String toEmail,
            String employeeName,
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

            helper.setFrom(
                    fromEmail
            );

            helper.setTo(
                    toEmail
            );

            helper.setSubject(
                    "SmashStep - Tài khoản quản lý của bạn đã được tạo"
            );

            helper.setText(
                    buildHtml(
                            employeeName,
                            username,
                            temporaryPassword
                    ),
                    true
            );

            mailSender.send(
                    message
            );

        } catch (MessagingException e) {

            throw new RuntimeException(
                    "Không thể gửi email tài khoản nhân viên.",
                    e
            );
        }
    }

    private String buildHtml(
            String employeeName,
            String username,
            String password
    ) {

        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                    <meta charset="UTF-8">

                    <style>

                        * {
                            box-sizing: border-box;
                        }

                        body {
                            margin: 0;
                            padding: 0;

                            background: #f1f5f9;

                            font-family:
                                Arial,
                                Helvetica,
                                sans-serif;

                            color: #0f172a;
                        }

                        .wrapper {
                            width: 100%;

                            padding: 40px 15px;
                        }

                        .container {
                            width: 100%;
                            max-width: 950px;

                            margin: auto;

                            overflow: hidden;

                            border-radius: 22px;

                            background: #ffffff;

                            box-shadow:
                                0 20px 60px
                                rgba(15, 23, 42, 0.12);
                        }

                        .left {
                            width: 46%;

                            padding: 55px 45px;

                            vertical-align: top;

                            color: #ffffff;

                            background:
                                linear-gradient(
                                    135deg,
                                    #0569b7,
                                    #0ea5e9
                                );
                        }

                        .brand-small {
                            display: inline-block;

                            margin-bottom: 25px;

                            padding:
                                8px 16px;

                            border:
                                1px solid
                                rgba(
                                    255,
                                    255,
                                    255,
                                    0.5
                                );

                            border-radius: 999px;

                            font-size: 12px;
                            font-weight: bold;

                            letter-spacing: 1px;
                        }

                        .brand-title {
                            margin: 0;

                            font-size: 42px;

                            line-height: 1.05;

                            font-weight: 800;
                        }

                        .brand-title span {
                            display: block;

                            font-weight: 400;
                        }

                        .brand-desc {
                            margin-top: 22px;

                            font-size: 15px;

                            line-height: 1.7;

                            color:
                                rgba(
                                    255,
                                    255,
                                    255,
                                    0.88
                                );
                        }

                        .features {
                            margin-top: 70px;
                        }

                        .feature {
                            display: inline-block;

                            width: 31%;

                            vertical-align: top;

                            font-size: 12px;
                        }

                        .feature-icon {
                            margin-bottom: 8px;

                            font-size: 22px;
                        }

                        .feature-title {
                            font-weight: bold;

                            margin-bottom: 5px;
                        }

                        .feature-text {
                            font-size: 10px;

                            color:
                                rgba(
                                    255,
                                    255,
                                    255,
                                    0.8
                                );
                        }

                        .right {
                            width: 54%;

                            padding: 50px;

                            vertical-align: top;

                            background: #ffffff;
                        }

                        .hello {
                            margin: 0;

                            font-size: 21px;
                            font-weight: 500;
                        }

                        .name {
                            margin:
                                5px
                                0
                                25px;

                            color: #0284c7;

                            font-size: 25px;
                            font-weight: 800;
                        }

                        .description {
                            margin-bottom: 25px;

                            color: #64748b;

                            font-size: 14px;
                            line-height: 1.6;
                        }

                        .account-box {
                            margin-bottom: 25px;

                            padding:
                                8px
                                22px;

                            border:
                                1px solid
                                #dbeafe;

                            border-radius: 14px;

                            background: #f8fafc;
                        }

                        .account-row {
                            padding:
                                16px
                                0;

                            border-bottom:
                                1px solid
                                #e2e8f0;
                        }

                        .account-row:last-child {
                            border-bottom: 0;
                        }

                        .label {
                            display: inline-block;

                            width: 45%;

                            color: #0369a1;

                            font-size: 12px;
                            font-weight: 700;
                        }

                        .value {
                            display: inline-block;

                            width: 53%;

                            text-align: right;

                            color: #0f172a;

                            font-size: 15px;
                            font-weight: 700;

                            letter-spacing: 0.5px;
                        }

                        .login-btn {
                            display: block;

                            width: 260px;

                            margin:
                                0
                                auto
                                25px;

                            padding:
                                15px
                                20px;

                            border-radius: 10px;

                            background: #0284c7;

                            color: #ffffff !important;

                            text-align: center;

                            text-decoration: none;

                            font-size: 15px;
                            font-weight: bold;
                        }

                        .warning {
                            padding:
                                15px
                                18px;

                            border-radius: 10px;

                            background: #fff7ed;

                            color: #9a3412;

                            font-size: 12px;

                            line-height: 1.6;
                        }

                        .footer {
                            margin-top: 28px;

                            padding-top: 20px;

                            border-top:
                                1px solid
                                #e2e8f0;

                            color: #94a3b8;

                            font-size: 11px;

                            text-align: center;
                        }

                        @media screen
                        and (max-width: 700px) {

                            .left,
                            .right {
                                display: block;

                                width: 100%;
                            }

                            .left,
                            .right {
                                padding: 35px 25px;
                            }

                            .brand-title {
                                font-size: 34px;
                            }
                        }

                    </style>
                </head>

                <body>

                <div class="wrapper">

                    <table
                        class="container"
                        cellpadding="0"
                        cellspacing="0"
                    >
                        <tr>

                            <td class="left">

                                <div class="brand-small">
                                    SMASHSTEP
                                </div>

                                <h1 class="brand-title">
                                    SmashStep
                                    <span>
                                        Internal
                                    </span>
                                </h1>

                                <div class="brand-desc">
                                    Hệ thống quản lý nội bộ
                                    chuyên nghiệp và hiệu quả
                                    dành cho cửa hàng SmashStep.
                                </div>

                                <div class="features">

                                    <div class="feature">

                                        <div class="feature-icon">
                                            🛡️
                                        </div>

                                        <div class="feature-title">
                                            Bảo mật
                                        </div>

                                        <div class="feature-text">
                                            An toàn thông tin
                                        </div>

                                    </div>

                                    <div class="feature">

                                        <div class="feature-icon">
                                            ⚡
                                        </div>

                                        <div class="feature-title">
                                            Hiệu quả
                                        </div>

                                        <div class="feature-text">
                                            Tối ưu quy trình
                                        </div>

                                    </div>

                                    <div class="feature">

                                        <div class="feature-icon">
                                            👥
                                        </div>

                                        <div class="feature-title">
                                            Kết nối
                                        </div>

                                        <div class="feature-text">
                                            Làm việc cùng nhau
                                        </div>

                                    </div>

                                </div>

                            </td>

                            <td class="right">

                                <h2 class="hello">
                                    👤 Xin chào,
                                </h2>

                                <div class="name">
                                    %s 👋
                                </div>

                                <div class="description">

                                    Tài khoản truy cập
                                    hệ thống của bạn
                                    đã được tạo.

                                    <br>

                                    Vui lòng sử dụng
                                    thông tin dưới đây
                                    để đăng nhập.

                                </div>

                                <div class="account-box">

                                    <div class="account-row">

                                        <span class="label">
                                            👤 USERNAME
                                        </span>

                                        <span class="value">
                                            %s
                                        </span>

                                    </div>

                                    <div class="account-row">

                                        <span class="label">
                                            🔒 MẬT KHẨU TẠM THỜI
                                        </span>

                                        <span class="value">
                                            %s
                                        </span>

                                    </div>

                                </div>

                                <a
                                    class="login-btn"
                                    href="http://localhost:5173"
                                >
                                    ➜ Đăng nhập hệ thống
                                </a>

                                <div class="warning">

                                    ⚠️
                                    Bạn cần đổi mật khẩu
                                    ngay sau lần đăng nhập
                                    đầu tiên để đảm bảo
                                    an toàn tài khoản.

                                </div>

                                <div class="footer">

                                    Email được gửi tự động
                                    từ hệ thống SmashStep.

                                    <br>

                                    Vui lòng không trả lời
                                    email này.

                                </div>

                            </td>

                        </tr>
                    </table>

                </div>

                </body>
                </html>
                """.formatted(
                escapeHtml(employeeName),
                escapeHtml(username),
                escapeHtml(password)
        );
    }

    /*
     * Tránh dữ liệu người dùng
     * phá HTML email.
     */
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