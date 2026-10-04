package com.smashstep.datn.product.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageRequest {
    @NotBlank(message = "URL ảnh không được trống")
    @Size(max = 1000)
    private String urlAnh;
    @NotNull
    private Boolean isAnhChinh;
    @Null(message = "Database hiện tại không hỗ trợ ảnh theo màu; hãy bỏ mauSacId")
    @JsonAlias("idMauSac")
    private Long mauSacId;

    public ImageRequest(String urlAnh, Boolean isAnhChinh) {
        this.urlAnh = urlAnh;
        this.isAnhChinh = isAnhChinh;
    }
}
