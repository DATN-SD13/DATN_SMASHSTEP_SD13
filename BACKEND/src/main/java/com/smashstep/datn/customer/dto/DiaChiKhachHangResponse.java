package com.smashstep.datn.customer.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DiaChiKhachHangResponse {

    private Long id;

    private String receiverName;

    private String receiverPhone;

    private String province;

    private String district;

    private String ward;

    private String street;

    private Integer addressType;

    private String addressTypeLabel;

    private Boolean isDefault;

    private Integer status;

    private String fullAddress;
}