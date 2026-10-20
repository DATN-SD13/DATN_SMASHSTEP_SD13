package com.smashstep.datn.customer.dto;

import com.smashstep.datn.common.validation.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerPasswordRequest {
    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
    @Size(max = 100)
    private String currentPassword;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Size(min = 6, max = 72, message = "Mật khẩu mới phải có từ 6 đến 72 ký tự")
    @AccountText(AccountRules.Kind.PASSWORD)
    private String newPassword;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu mới")
    @Size(max = 72)
    private String confirmPassword;
}
