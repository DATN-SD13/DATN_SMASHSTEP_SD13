package com.smashstep.datn.customer.dto;

import com.smashstep.datn.common.validation.*;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class CustomerProfileRequest {
    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
    @AccountText(AccountRules.Kind.NAME)
    private String name;

    @Pattern(regexp = "^$|^0\\d{9}$", message = "Số điện thoại phải gồm 10 số và bắt đầu bằng 0")
    @AccountText(AccountRules.Kind.PHONE)
    private String phone;

    @Min(0) @Max(2)
    private Integer gender;

    @PastOrPresent(message = "Ngày sinh không được ở tương lai")
    @AccountBirthDate(minAge = 15)
    private LocalDate dob;
}
