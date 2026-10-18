package com.smashstep.datn.employee.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class NhanVienRequest {

    @NotBlank(message = "Tên nhân viên không được để trống")
    @Size(max = 255, message = "Tên nhân viên không được vượt quá 255 ký tự")
    private String name;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^0\\d{9}$",
            message = "Số điện thoại phải gồm 10 số và bắt đầu bằng 0"
    )
    private String phone;

    @NotNull(message = "Vai trò không được để trống")
    private Long roleId;

    @NotNull(message = "Giới tính không được để trống")
    @Min(value = 0, message = "Giới tính chỉ nhận 0, 1 hoặc 2")
    @Max(value = 2, message = "Giới tính chỉ nhận 0, 1 hoặc 2")
    private Integer gender;

    @NotNull(message = "Ngày sinh không được để trống")
    @PastOrPresent(message = "Ngày sinh không được lớn hơn ngày hiện tại")
    private LocalDate dob;

    @Size(max = 100, message = "Tỉnh/thành phố tối đa 100 ký tự")
    private String province;

    @Size(max = 100, message = "Phường/xã tối đa 100 ký tự")
    private String ward;

    @Size(max = 500, message = "Địa chỉ cụ thể tối đa 500 ký tự")
    private String street;

    @Size(max = 1000) private String image;
}
