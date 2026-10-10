
package com.smashstep.datn.promotion.service;

import com.smashstep.datn.promotion.entity.ChiTietDotGiamGia;
import com.smashstep.datn.promotion.repository.ChiTietDotGiamGiaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GiaSauGiamService {

    private final ChiTietDotGiamGiaRepository
            chiTietDotGiamGiaRepository;

    public BigDecimal tinhGiaSauGiam(
            Long productDetailId,
            BigDecimal giaGoc
    ) {
        if (giaGoc == null || giaGoc.signum() < 0) {
            throw new IllegalArgumentException(
                    "Giá gốc không hợp lệ"
            );
        }

        List<ChiTietDotGiamGia> promotions =
                chiTietDotGiamGiaRepository
                        .findCurrentlyEffectiveByProductDetailId(
                                productDetailId,
                                LocalDateTime.now()
                        );

        // Không có đợt giảm giá còn hiệu lực
        if (promotions.isEmpty()) {
            return giaGoc;
        }

        // Lấy mức giảm cao nhất nếu có nhiều chương trình
        BigDecimal phanTramGiam = promotions.stream()
                .map(ChiTietDotGiamGia::getPhanTramGiamBienThe)
                .filter(value -> value != null)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Tính giá sau giảm
        return giaGoc.multiply(
                BigDecimal.ONE.subtract(
                        phanTramGiam.divide(
                                BigDecimal.valueOf(100),
                                4,
                                RoundingMode.HALF_UP
                        )
                )
        ).setScale(0, RoundingMode.HALF_UP);
    }
}
