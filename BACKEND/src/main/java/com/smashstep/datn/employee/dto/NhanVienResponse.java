package com.smashstep.datn.employee.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NhanVienResponse {

    private Long id;

    private String code;

    private String username;

    private String name;

    private String email;

    private String phone;

    private Integer gender;

    private String genderLabel;

    private LocalDate dob;

    private String street;

    private String ward;

    private String province;

    private String address;

    private Long roleId;

    private String role;

    private Integer status;

    private String statusLabel;

    private Boolean active;

    private String image;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}