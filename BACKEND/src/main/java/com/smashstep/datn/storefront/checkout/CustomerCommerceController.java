package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.auth.security.JwtAuthFilter.AuthDetails;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import static com.smashstep.datn.storefront.checkout.CommerceDtos.*;

@RestController
@RequestMapping("/api/customer/shop")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class CustomerCommerceController {
    private final CustomerCommerceService commerce;
    private final GhnShippingService shipping;
    @PostMapping("/shipping/resolve") public ApiResponse<Location> resolve(Authentication a,@Valid @RequestBody LocationRequest r) {commerce.customer(id(a));return ApiResponse.ok(shipping.resolve(r));}
    @PostMapping("/quote") public ApiResponse<Quote> quote(Authentication a,@Valid @RequestBody Request r) {return ApiResponse.ok(commerce.quote(id(a),r));}
    @PostMapping("/checkout") public ApiResponse<Order> checkout(Authentication a,@Valid @RequestBody Request r) {return ApiResponse.ok(commerce.checkout(id(a),r));}
    @GetMapping("/vouchers") public ApiResponse<List<Voucher>> vouchers(Authentication a,@RequestParam BigDecimal subtotal) {return ApiResponse.ok(commerce.vouchers(id(a),subtotal));}
    @GetMapping("/orders") public ApiResponse<List<Order>> orders(Authentication a,@RequestParam(defaultValue="0") int page) {return ApiResponse.ok(commerce.orders(id(a),page));}
    @GetMapping("/orders/{code}") public ApiResponse<Order> order(Authentication a,@PathVariable String code) {return ApiResponse.ok(commerce.order(id(a),code));}
    @GetMapping("/orders/{code}/payment") public ApiResponse<OnlineBankPayment.Payment> payment(Authentication a,@PathVariable String code) {return ApiResponse.ok(commerce.payment(id(a),code));}
    @PutMapping("/orders/{code}/shipping") public ApiResponse<Order> edit(Authentication a,@PathVariable String code,@Valid @RequestBody Shipping r) {return ApiResponse.ok(commerce.editShipping(id(a),code,r));}
    @PostMapping("/orders/{code}/cancel") public ApiResponse<Order> cancel(Authentication a,@PathVariable String code) {return ApiResponse.ok(commerce.cancel(id(a),code));}
    @GetMapping("/notifications") public ApiResponse<List<Notice>> notices(Authentication a) {return ApiResponse.ok(commerce.notices(id(a)));}
    @GetMapping("/shipping/{level}") public ApiResponse<List<GhnShippingService.Area>> areas(Authentication a,@PathVariable String level,@RequestParam(required=false) Integer parent) {commerce.customer(id(a));return ApiResponse.ok(shipping.areas(level,parent));}
    private Long id(Authentication auth) {
        if(auth==null || !(auth.getDetails() instanceof AuthDetails d) || !"KHACH_HANG".equals(d.userType()) || d.userId()==null)
            throw AppException.forbidden("Vui lòng đăng nhập bằng tài khoản khách hàng");
        return d.userId();
    }
}
