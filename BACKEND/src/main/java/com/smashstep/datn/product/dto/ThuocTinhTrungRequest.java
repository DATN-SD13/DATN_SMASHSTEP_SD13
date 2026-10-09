package com.smashstep.datn.product.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThuocTinhTrungRequest {
    @NotBlank @Size(max = 255)
    private String ten;
    @Size(max = 20)
    private String maMauHex;
    @Positive
    private Long excludeId;
}
