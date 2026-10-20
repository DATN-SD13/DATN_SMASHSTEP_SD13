package com.smashstep.datn.auth.dto;

import com.smashstep.datn.common.validation.*;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @AccountText(AccountRules.Kind.LOGIN)
    @jakarta.validation.constraints.Size(max = 254)
    private String username;

    @NotBlank(message = "Mật khẩu không được để trống")
    @jakarta.validation.constraints.Size(max = 100, message = "Mật khẩu đăng nhập tối đa 100 ký tự")
    private String password;

    /**
     * Tuỳ chọn — nếu có sẽ ưu tiên tìm đúng loại tài khoản:
     *   "NHAN_VIEN" → chỉ tìm trong bảng nhan_vien
     *   "KHACH_HANG" → chỉ tìm trong bảng khach_hang
     *   null / bỏ qua → tự động nhận biết
     */
    @jakarta.validation.constraints.Pattern(regexp = "NHAN_VIEN|KHACH_HANG", message = "Loại tài khoản không hợp lệ")
    private String userType;
}
