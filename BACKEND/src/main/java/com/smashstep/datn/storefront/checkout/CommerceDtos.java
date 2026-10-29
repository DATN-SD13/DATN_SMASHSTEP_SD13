package com.smashstep.datn.storefront.checkout;

import com.smashstep.datn.common.validation.AccountRules;
import com.smashstep.datn.common.validation.AccountText;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class CommerceDtos {
    private CommerceDtos() {}
    public record MapPoint(@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
            @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
            @PositiveOrZero @DecimalMax("100000") Double accuracy) {}
    public record Item(@NotNull @Positive Long variantId, @NotNull @Min(1) @Max(10000) Integer quantity) {
        @com.fasterxml.jackson.annotation.JsonCreator
        public static Item from(@com.fasterxml.jackson.annotation.JsonProperty("variantId") Long id,
                @com.fasterxml.jackson.annotation.JsonProperty("quantity") BigDecimal quantity) {
            try { return new Item(id,quantity==null?null:quantity.intValueExact()); }
            catch(ArithmeticException e) { throw new IllegalArgumentException("Số lượng phải là số nguyên",e); }
        }
    }
    public record Receiver(
            @NotBlank @AccountText(AccountRules.Kind.NAME) String name,
            @NotBlank @AccountText(AccountRules.Kind.PHONE) String phone,
            @AccountText(AccountRules.Kind.EMAIL) String email,
            @NotBlank @Size(max=100) @AccountText(AccountRules.Kind.ADDRESS) String province,
            @Size(max=100) @AccountText(AccountRules.Kind.ADDRESS) String district,
            @NotBlank @Size(max=100) @AccountText(AccountRules.Kind.ADDRESS) String ward,
            @NotBlank @Size(max=500) @AccountText(AccountRules.Kind.ADDRESS) String street,
            @Positive Integer provinceId, @Positive Integer districtId, @Size(max=20) String wardCode) {
        public String address() {
            return java.util.stream.Stream.of(street, ward, district, province)
                    .filter(s -> s != null && !s.isBlank()).map(String::trim)
                    .reduce((a,b) -> a + ", " + b).orElse("");
        }
    }
    public record Request(
            @NotEmpty @Size(max=50) List<@NotNull @Valid Item> items,
            @Size(max=50) String voucherCode, @Positive Long paymentMethodId,
            @Valid Receiver receiver, @Size(max=900) String note,
            @Pattern(regexp="[0-9a-fA-F-]{36}") String requestId,
            @DecimalMin("0") @Digits(integer=16,fraction=2) BigDecimal expectedTotal,
            Boolean saveAddress, Boolean makeDefaultAddress,
            @Valid @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) MapPoint deliveryPoint,
            @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) Boolean receiveOrderEmails) {
        public Request(List<Item> items,String voucherCode,Long paymentMethodId,Receiver receiver,String note,
                String requestId,BigDecimal expectedTotal,Boolean saveAddress,Boolean makeDefaultAddress,MapPoint deliveryPoint) {
            this(items,voucherCode,paymentMethodId,receiver,note,requestId,expectedTotal,saveAddress,makeDefaultAddress,deliveryPoint,null);
        }
        public Request(List<Item> items,String voucherCode,Long paymentMethodId,Receiver receiver,String note,
                String requestId,BigDecimal expectedTotal,Boolean saveAddress,Boolean makeDefaultAddress) {
            this(items,voucherCode,paymentMethodId,receiver,note,requestId,expectedTotal,saveAddress,makeDefaultAddress,null);
        }
        public Request(List<Item> items, String voucherCode, Long paymentMethodId, Receiver receiver,
                String note, String requestId, BigDecimal expectedTotal) {
            this(items,voucherCode,paymentMethodId,receiver,note,requestId,expectedTotal,false,false);
        }
    }
    public record LocationRequest(@NotBlank @Size(max=100) String province, @Size(max=100) String district,
            @NotBlank @Size(max=100) String ward) {}
    public record Location(Integer provinceId, String province, String wardCode, String ward,
            boolean resolved, String message) {}
    public record Shipping(@NotNull @Valid Receiver receiver, @Size(max=900) String note,@Valid MapPoint deliveryPoint) {
        public Shipping(Receiver receiver,String note){this(receiver,note,null);}
    }
    public record Variant(Long variantId, Long productId, String name, String sku, String color,
            String size, String image, int stock, BigDecimal giaGoc, BigDecimal giaSauGiam,
            BigDecimal phanTramGiam, boolean dangGiamGia, boolean available) {}
    public record Line(Variant variant, int quantity, BigDecimal total) {}
    public record Method(Long id, String code, String name, boolean supported, String message) {}
    public record Voucher(String code, String name, Integer type, BigDecimal value,
            BigDecimal minimum, BigDecimal maximum, BigDecimal discount, LocalDateTime expiresAt) {}
    public record Quote(List<Line> lines, BigDecimal subtotal, BigDecimal discount, BigDecimal shipping,
            @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
            BigDecimal total, String voucherCode, List<Method> methods, boolean canCheckout, String message) {}
        public record OrderItem(Long variantId, Long productId, String name, String sku, String color, String size,
            String image, int quantity, BigDecimal unitPrice, BigDecimal total) {
        public OrderItem(Long variantId, String name, String sku, String color, String size,
                String image, int quantity, BigDecimal unitPrice, BigDecimal total) {
            this(variantId, null, name, sku, color, size, image, quantity, unitPrice, total);
        }
    }
    public record Event(Integer status, String label, String note, LocalDateTime at) {}
    public record Order(String code, Integer status, String statusLabel, LocalDateTime createdAt,
            LocalDateTime updatedAt, String receiverName, String phone, String address,
            String paymentMethod, String note, BigDecimal subtotal, BigDecimal discount,
            BigDecimal shipping,
            @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)
            BigDecimal total, boolean editable, List<OrderItem> items, List<Event> history,
            String paymentMethodCode, boolean shippingEditable, MapPoint deliveryPoint) {
        public Order(String code,Integer status,String statusLabel,LocalDateTime createdAt,LocalDateTime updatedAt,
                String receiverName,String phone,String address,String paymentMethod,String note,BigDecimal subtotal,
                BigDecimal discount,BigDecimal shipping,BigDecimal total,boolean editable,List<OrderItem> items,
                List<Event> history,String paymentMethodCode,boolean shippingEditable) {
            this(code,status,statusLabel,createdAt,updatedAt,receiverName,phone,address,paymentMethod,note,subtotal,discount,
                    shipping,total,editable,items,history,paymentMethodCode,shippingEditable,null);
        }
    }
    public record Notice(String id, String title, String message, String href, LocalDateTime at) {}
}

