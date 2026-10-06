package com.smashstep.datn.customer.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class KhachHangResponse {

    private Long id;

    private String code;

    private String username;

    private String name;

    private String email;

    private String phone;

    private LocalDate dob;

    private Integer gender;

    private String genderLabel;

    private String image;

    private Integer status;

    private String statusLabel;

    private Boolean active;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private DiaChiKhachHangResponse defaultAddress;

    private List<DiaChiKhachHangResponse> addresses;
}