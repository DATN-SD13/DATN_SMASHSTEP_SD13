package com.smashstep.datn.customer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiaChiKhachHangRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(
            max = 255,
            message = "Tên người nhận tối đa 255 ký tự"
    )
    private String receiverName;

    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    @Pattern(
            regexp = "^0\\d{9}$",
            message = "Số điện thoại người nhận phải gồm 10 số và bắt đầu bằng 0"
    )
    private String receiverPhone;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    @Size(max = 100)
    private String province;

    @Size(max = 100)
    private String district;

    @NotBlank(message = "Phường/Xã không được để trống")
    @Size(max = 100)
    private String ward;

    @NotBlank(message = "Địa chỉ cụ thể không được để trống")
    @Size(max = 500)
    private String street;

    @Min(value = 1, message = "Loại địa chỉ chỉ nhận 1 hoặc 2")
    @Max(value = 2, message = "Loại địa chỉ chỉ nhận 1 hoặc 2")
    private Integer addressType = 1;

    private Boolean isDefault = false;
}
