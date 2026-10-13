package com.smashstep.datn.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    /** ten_dang_nhap (nhân viên) hoặc ten_tai_khoan / email (khách hàng) */
    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    /**
     * Phân biệt trang login:
     *   "NHAN_VIEN" — trang Admin/Nhân viên
     *   "KHACH_HANG" — trang Khách hàng
     */
    @NotBlank(message = "Loại người dùng không được để trống")
    private String userType;
}
