package com.smashstep.datn.auth.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {
    private String accessToken;
    private String tokenType;   // "Bearer"
    private String userType;    // "NHAN_VIEN" | "KHACH_HANG"
    private Long   id;
    private String code;        // maNhanVien hoặc maKhachHang
    private String name;        // tenNhanVien hoặc tenKhachHang
    private String email;
    private String role;        // "ADMIN" | "STAFF" | "CUSTOMER"
    private String avatar;
}
