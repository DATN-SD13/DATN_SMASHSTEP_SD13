package com.smashstep.datn.customer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class KhachHangRequest {

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên tối đa 255 ký tự")
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255)
    private String email;

    @Pattern(
            regexp = "^$|^0\\d{9}$",
            message = "Số điện thoại phải gồm 10 số và bắt đầu bằng 0"
    )
    private String phone;

    @Min(value = 0, message = "Giới tính không hợp lệ")
    @Max(value = 2, message = "Giới tính không hợp lệ")
    private Integer gender;

    @PastOrPresent(message = "Ngày sinh không được lớn hơn ngày hiện tại")
    private LocalDate dob;

    @Size(max = 1000) private String image;

    @Valid
    private DiaChiKhachHangRequest defaultAddress;
}
