package com.smashstep.datn.auth.dto;

import com.smashstep.datn.common.validation.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
    @AccountText(AccountRules.Kind.NAME)
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255)
    @AccountText(AccountRules.Kind.EMAIL)
    private String email;

    @Pattern(regexp = "^$|^0\\d{9}$", message = "Số điện thoại phải gồm 10 số và bắt đầu bằng 0")
    @AccountText(AccountRules.Kind.PHONE)
    private String phone;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, max = 72, message = "Mật khẩu phải có từ 6 đến 72 ký tự")
    @AccountText(AccountRules.Kind.PASSWORD)
    private String password;
}
