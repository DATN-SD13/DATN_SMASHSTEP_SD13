package com.smashstep.datn.storefront.review;

import com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ProductReviewController {
    private final ProductReviewService reviews;
    private final ReviewUploadService uploadService;

    @GetMapping("/api/store/products/{id}/reviews")
    public ApiResponse<ProductReviewService.Page> list(@PathVariable Long id, @RequestParam(defaultValue="0") int page, @RequestParam(required=false) Integer stars) {
        return ApiResponse.ok(reviews.list(id,page,stars));
    }

    @GetMapping("/api/customer/products/{id}/review")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<ProductReviewService.Mine> mine(@PathVariable Long id, Authentication auth) {
        return ApiResponse.ok(reviews.mine(id,customer(auth)));
    }

    @PutMapping("/api/customer/products/{id}/review")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<ProductReviewService.Mine> save(@PathVariable Long id, Authentication auth, @Valid @RequestBody ProductReviewService.Input input) {
        return ApiResponse.ok(reviews.save(id,customer(auth),input));
    }

    @PostMapping(value = "/api/customer/reviews/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<Map<String, String>> upload(Authentication auth, @RequestParam Long productId, @RequestPart("file") MultipartFile file) {
        Long owner = customer(auth);
        reviews.requireEligible(productId,owner);
        return ApiResponse.ok(Map.of("url", uploadService.upload(file,owner)));
    }

    private Long customer(Authentication auth) {
        if(auth==null || !(auth.getDetails() instanceof AuthDetails d) || !"KHACH_HANG".equals(d.userType()) || d.userId()==null)
            throw AppException.forbidden("Vui lòng đăng nhập bằng tài khoản khách hàng");
        return d.userId();
    }
}
