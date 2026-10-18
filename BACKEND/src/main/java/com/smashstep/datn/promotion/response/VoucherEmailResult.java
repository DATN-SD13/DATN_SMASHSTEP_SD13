package com.smashstep.datn.promotion.response;

import lombok.Getter;
import lombok.Setter;

/** SMTP acceptance, not a guarantee of inbox delivery. Not persisted. */
@Getter @Setter
public class VoucherEmailResult {
    private String status = "NOT_REQUESTED";
    private int sent;
    private int failed;
    private String message = "Không có email cần gửi.";
}
