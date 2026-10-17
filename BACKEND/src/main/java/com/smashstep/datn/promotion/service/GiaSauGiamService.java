package com.smashstep.datn.promotion.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.pricing.CurrentPriceService;
import com.smashstep.datn.product.repository.SanPhamChiTietRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Preserve the team pricing endpoint while sharing Product/POS server pricing. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GiaSauGiamService {
    private final SanPhamChiTietRepository variants;
    private final CurrentPriceService currentPrices;

    public BigDecimal tinhGiaSauGiam(Long productDetailId, BigDecimal giaGoc) {
        if (giaGoc == null || giaGoc.signum() < 0)
            throw new AppException(HttpStatus.BAD_REQUEST, "Giá gốc không hợp lệ");
        var variant = variants.findById(productDetailId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Biến thể không tồn tại"));
        // Request prices are informational; the persisted base price is authoritative.
        return currentPrices.getPrice(variant, LocalDateTime.now()).getEffectivePrice();
    }
}
