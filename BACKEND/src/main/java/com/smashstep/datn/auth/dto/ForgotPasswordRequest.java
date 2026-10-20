package com.smashstep.datn.auth.dto;

import com.smashstep.datn.common.validation.*;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @jakarta.validation.constraints.Size(max = 255)
    @AccountText(AccountRules.Kind.EMAIL)
    private String email;
}
