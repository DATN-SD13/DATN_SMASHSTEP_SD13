package com.smashstep.datn.auth.dto;

import com.smashstep.datn.common.validation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Token không được để trống")
    @Size(max = 128)
    @jakarta.validation.constraints.Pattern(regexp = "[A-Za-z0-9_-]{32,128}", message = "Link đặt lại mật khẩu không hợp lệ")
    private String token;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, max = 72, message = "Mật khẩu phải có từ 6 đến 72 ký tự")
    @AccountText(AccountRules.Kind.PASSWORD)
    private String newPassword;
}
