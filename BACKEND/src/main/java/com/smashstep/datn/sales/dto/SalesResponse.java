package com.smashstep.datn.sales.dto;

import java.math.BigDecimal;
import java.util.Objects;

public final class SalesResponse {
    private SalesResponse() {}

    public static final class CatalogItem {
        private final Long id;
        private final String code;
        private final String productCode;
        private final String name;
        private final String color;
        private final String size;
        private final BigDecimal price;
        private final BigDecimal originalPrice;
        private final Integer stock;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private final Boolean discounted;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private final BigDecimal discountPercent;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private final String campaignCode;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private final String campaignName;

        public CatalogItem(Long id, String code, String productCode, String name, String color, String size, BigDecimal price, BigDecimal originalPrice, Integer stock) {
            this(id, code, productCode, name, color, size, price, originalPrice, stock, null, null, null, null);
        }

        public CatalogItem(Long id, String code, String productCode, String name, String color, String size, BigDecimal price, BigDecimal originalPrice, Integer stock, Boolean discounted, BigDecimal discountPercent, String campaignCode, String campaignName) {
            this.id = id;
            this.code = code;
            this.productCode = productCode;
            this.name = name;
            this.color = color;
            this.size = size;
            this.price = price;
            this.originalPrice = originalPrice;
            this.stock = stock;
            this.discounted = discounted;
            this.discountPercent = discountPercent;
            this.campaignCode = campaignCode;
            this.campaignName = campaignName;
        }
        public Long id() { return id; }
        public Long getId() { return id; }
        public String code() { return code; }
        public String getCode() { return code; }
        public String productCode() { return productCode; }
        public String getProductCode() { return productCode; }
        public String name() { return name; }
        public String getName() { return name; }
        public String color() { return color; }
        public String getColor() { return color; }
        public String size() { return size; }
        public String getSize() { return size; }
        public BigDecimal price() { return price; }
        public BigDecimal getPrice() { return price; }
        public BigDecimal originalPrice() { return originalPrice; }
        public BigDecimal getOriginalPrice() { return originalPrice; }
        public Integer stock() { return stock; }
        public Integer getStock() { return stock; }
        public Boolean discounted() { return discounted; }
        public Boolean getDiscounted() { return discounted; }
        public BigDecimal discountPercent() { return discountPercent; }
        public BigDecimal getDiscountPercent() { return discountPercent; }
        public String campaignCode() { return campaignCode; }
        public String getCampaignCode() { return campaignCode; }
        public String campaignName() { return campaignName; }
        public String getCampaignName() { return campaignName; }
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof CatalogItem)) return false;
            CatalogItem that = (CatalogItem) other;
            return Objects.equals(id, that.id) && Objects.equals(code, that.code) && Objects.equals(productCode, that.productCode) && Objects.equals(name, that.name) && Objects.equals(color, that.color) && Objects.equals(size, that.size) && Objects.equals(price, that.price) && Objects.equals(originalPrice, that.originalPrice) && Objects.equals(stock, that.stock) && Objects.equals(discounted, that.discounted) && Objects.equals(discountPercent, that.discountPercent) && Objects.equals(campaignCode, that.campaignCode) && Objects.equals(campaignName, that.campaignName);
        }
        @Override public int hashCode() { return Objects.hash(id, code, productCode, name, color, size, price, originalPrice, stock, discounted, discountPercent, campaignCode, campaignName); }
        @Override public String toString() { return "CatalogItem[" + "id=" + id + ", " + "code=" + code + ", " + "productCode=" + productCode + ", " + "name=" + name + ", " + "color=" + color + ", " + "size=" + size + ", " + "price=" + price + ", " + "originalPrice=" + originalPrice + ", " + "stock=" + stock + ", " + "discounted=" + discounted + ", " + "discountPercent=" + discountPercent + ", " + "campaignCode=" + campaignCode + ", " + "campaignName=" + campaignName + "]"; }
    }

    public static final class PaymentMethod {
        private final Long id;
        private final String code;
        private final String name;

        public PaymentMethod(Long id, String code, String name) {
            this.id = id;
            this.code = code;
            this.name = name;
        }

        public Long id() { return id; }
        public Long getId() { return id; }
        public String code() { return code; }
        public String getCode() { return code; }
        public String name() { return name; }
        public String getName() { return name; }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof PaymentMethod)) return false;
            PaymentMethod that = (PaymentMethod) other;
            return Objects.equals(id, that.id)
                    && Objects.equals(code, that.code)
                    && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, code, name);
        }

        @Override
        public String toString() {
            return "PaymentMethod[id=" + id
                    + ", code=" + code
                    + ", name=" + name
                    + "]";
        }
    }

    public static final class Quote {
        private final BigDecimal subtotal;
        private final BigDecimal discount;
        private final BigDecimal total;

        public Quote(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
        }

        public BigDecimal subtotal() { return subtotal; }
        public BigDecimal getSubtotal() { return subtotal; }
        public BigDecimal discount() { return discount; }
        public BigDecimal getDiscount() { return discount; }
        public BigDecimal total() { return total; }
        public BigDecimal getTotal() { return total; }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Quote)) return false;
            Quote that = (Quote) other;
            return Objects.equals(subtotal, that.subtotal)
                    && Objects.equals(discount, that.discount)
                    && Objects.equals(total, that.total);
        }

        @Override
        public int hashCode() {
            return Objects.hash(subtotal, discount, total);
        }

        @Override
        public String toString() {
            return "Quote[subtotal=" + subtotal
                    + ", discount=" + discount
                    + ", total=" + total
                    + "]";
        }
    }

    public static final class Receipt {
        private final Long invoiceId;
        private final String invoiceCode;
        private final BigDecimal subtotal;
        private final BigDecimal discount;
        private final BigDecimal total;
        private final BigDecimal change;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private final Long employeeId;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private final String employeeCode;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private final String employeeName;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private final Long customerId;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private final String customerCode;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private final String customerName;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private final String customerPhone;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private final String paymentMethodCode;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private final String paymentMethodName;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private final String voucherCode;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private final java.time.LocalDateTime createdAt;
        @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        private final BigDecimal paidAmount;

        public Receipt(Long invoiceId, String invoiceCode, BigDecimal subtotal, BigDecimal discount, BigDecimal total, BigDecimal change) {
            this(invoiceId, invoiceCode, subtotal, discount, total, change, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        public Receipt(Long invoiceId, String invoiceCode, BigDecimal subtotal, BigDecimal discount, BigDecimal total, BigDecimal change, Long employeeId, String employeeCode, String employeeName, Long customerId, String customerCode, String customerName, String customerPhone, String paymentMethodCode, String paymentMethodName, String voucherCode, java.time.LocalDateTime createdAt, BigDecimal paidAmount) {
            this.invoiceId = invoiceId;
            this.invoiceCode = invoiceCode;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
            this.change = change;
            this.employeeId = employeeId;
            this.employeeCode = employeeCode;
            this.employeeName = employeeName;
            this.customerId = customerId;
            this.customerCode = customerCode;
            this.customerName = customerName;
            this.customerPhone = customerPhone;
            this.paymentMethodCode = paymentMethodCode;
            this.paymentMethodName = paymentMethodName;
            this.voucherCode = voucherCode;
            this.createdAt = createdAt;
            this.paidAmount = paidAmount;
        }
        public Long invoiceId() { return invoiceId; }
        public Long getInvoiceId() { return invoiceId; }
        public String invoiceCode() { return invoiceCode; }
        public String getInvoiceCode() { return invoiceCode; }
        public BigDecimal subtotal() { return subtotal; }
        public BigDecimal getSubtotal() { return subtotal; }
        public BigDecimal discount() { return discount; }
        public BigDecimal getDiscount() { return discount; }
        public BigDecimal total() { return total; }
        public BigDecimal getTotal() { return total; }
        public BigDecimal change() { return change; }
        public BigDecimal getChange() { return change; }
        public Long employeeId() { return employeeId; }
        public Long getEmployeeId() { return employeeId; }
        public String employeeCode() { return employeeCode; }
        public String getEmployeeCode() { return employeeCode; }
        public String employeeName() { return employeeName; }
        public String getEmployeeName() { return employeeName; }
        public Long customerId() { return customerId; }
        public Long getCustomerId() { return customerId; }
        public String customerCode() { return customerCode; }
        public String getCustomerCode() { return customerCode; }
        public String customerName() { return customerName; }
        public String getCustomerName() { return customerName; }
        public String customerPhone() { return customerPhone; }
        public String getCustomerPhone() { return customerPhone; }
        public String paymentMethodCode() { return paymentMethodCode; }
        public String getPaymentMethodCode() { return paymentMethodCode; }
        public String paymentMethodName() { return paymentMethodName; }
        public String getPaymentMethodName() { return paymentMethodName; }
        public String voucherCode() { return voucherCode; }
        public String getVoucherCode() { return voucherCode; }
        public java.time.LocalDateTime createdAt() { return createdAt; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public BigDecimal paidAmount() { return paidAmount; }
        public BigDecimal getPaidAmount() { return paidAmount; }
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Receipt)) return false;
            Receipt that = (Receipt) other;
            return Objects.equals(invoiceId, that.invoiceId) && Objects.equals(invoiceCode, that.invoiceCode) && Objects.equals(subtotal, that.subtotal) && Objects.equals(discount, that.discount) && Objects.equals(total, that.total) && Objects.equals(change, that.change) && Objects.equals(employeeId, that.employeeId) && Objects.equals(employeeCode, that.employeeCode) && Objects.equals(employeeName, that.employeeName) && Objects.equals(customerId, that.customerId) && Objects.equals(customerCode, that.customerCode) && Objects.equals(customerName, that.customerName) && Objects.equals(customerPhone, that.customerPhone) && Objects.equals(paymentMethodCode, that.paymentMethodCode) && Objects.equals(paymentMethodName, that.paymentMethodName) && Objects.equals(voucherCode, that.voucherCode) && Objects.equals(createdAt, that.createdAt) && Objects.equals(paidAmount, that.paidAmount);
        }
        @Override public int hashCode() { return Objects.hash(invoiceId, invoiceCode, subtotal, discount, total, change, employeeId, employeeCode, employeeName, customerId, customerCode, customerName, customerPhone, paymentMethodCode, paymentMethodName, voucherCode, createdAt, paidAmount); }
        @Override public String toString() { return "Receipt[" + "invoiceId=" + invoiceId + ", " + "invoiceCode=" + invoiceCode + ", " + "subtotal=" + subtotal + ", " + "discount=" + discount + ", " + "total=" + total + ", " + "change=" + change + ", " + "employeeId=" + employeeId + ", " + "employeeCode=" + employeeCode + ", " + "employeeName=" + employeeName + ", " + "customerId=" + customerId + ", " + "customerCode=" + customerCode + ", " + "customerName=" + customerName + ", " + "customerPhone=" + customerPhone + ", " + "paymentMethodCode=" + paymentMethodCode + ", " + "paymentMethodName=" + paymentMethodName + ", " + "voucherCode=" + voucherCode + ", " + "createdAt=" + createdAt + ", " + "paidAmount=" + paidAmount + "]"; }
    }
}
