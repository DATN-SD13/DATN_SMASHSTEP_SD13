package com.smashstep.datn.schedule.dto;
import jakarta.validation.constraints.*;
public record ThayNhanVienRequest(@NotBlank @Size(max=50) String maNhanVien) {}
