package com.smashstep.datn.promotion.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DotGiamGiaStatusRequest {

    @NotNull(message = "Trạng thái không được để trống")
    private Integer status;
}