package com.smashstep.datn.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    /**
     * Tuỳ chọn — nếu có sẽ ưu tiên tìm đúng loại tài khoản:
     *   "NHAN_VIEN" → chỉ tìm trong bảng nhan_vien
     *   "KHACH_HANG" → chỉ tìm trong bảng khach_hang
     *   null / bỏ qua → tự động nhận biết
     */
    private String userType;
}
