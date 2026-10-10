
package com.smashstep.datn.promotion.controller;

import com.smashstep.datn.promotion.service.GiaSauGiamService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/test-gia-giam")
@RequiredArgsConstructor
public class GiaSauGiamTestController {

    private final GiaSauGiamService giaSauGiamService;

    @GetMapping
    public Map<String, Object> testGiaGiam(
            @RequestParam Long productDetailId,
            @RequestParam BigDecimal giaGoc
    ) {
        BigDecimal giaSauGiam =
                giaSauGiamService.tinhGiaSauGiam(
                        productDetailId,
                        giaGoc
                );

        return Map.of(
                "productDetailId", productDetailId,
                "giaGoc", giaGoc,
                "giaSauGiam", giaSauGiam
        );
    }
}
