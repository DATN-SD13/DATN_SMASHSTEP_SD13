package com.smashstep.datn.storefront.checkout;
import com.smashstep.datn.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StorefrontCartController {
    private final CustomerCommerceService commerce;
    @GetMapping("/variants") public ApiResponse<List<CommerceDtos.Variant>> variants(@RequestParam List<Long> ids) {
        return ApiResponse.ok(commerce.variants(ids));
    }
}
