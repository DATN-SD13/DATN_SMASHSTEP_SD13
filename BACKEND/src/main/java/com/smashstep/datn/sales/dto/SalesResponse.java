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

        public CatalogItem(Long id, String code, String productCode, String name, String color, String size, BigDecimal price, BigDecimal originalPrice, Integer stock) {
            this.id = id;
            this.code = code;
            this.productCode = productCode;
            this.name = name;
            this.color = color;
            this.size = size;
            this.price = price;
            this.originalPrice = originalPrice;
            this.stock = stock;
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

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof CatalogItem)) return false;
            CatalogItem that = (CatalogItem) other;
            return Objects.equals(id, that.id)
                    && Objects.equals(code, that.code)
                    && Objects.equals(productCode, that.productCode)
                    && Objects.equals(name, that.name)
                    && Objects.equals(color, that.color)
                    && Objects.equals(size, that.size)
                    && Objects.equals(price, that.price)
                    && Objects.equals(originalPrice, that.originalPrice)
                    && Objects.equals(stock, that.stock);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, code, productCode, name, color, size, price, originalPrice, stock);
        }

        @Override
        public String toString() {
            return "CatalogItem[id=" + id
                    + ", code=" + code
                    + ", productCode=" + productCode
                    + ", name=" + name
                    + ", color=" + color
                    + ", size=" + size
                    + ", price=" + price
                    + ", originalPrice=" + originalPrice
                    + ", stock=" + stock
                    + "]";
        }
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

        public Receipt(Long invoiceId, String invoiceCode, BigDecimal subtotal, BigDecimal discount, BigDecimal total, BigDecimal change) {
            this.invoiceId = invoiceId;
            this.invoiceCode = invoiceCode;
            this.subtotal = subtotal;
            this.discount = discount;
            this.total = total;
            this.change = change;
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

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Receipt)) return false;
            Receipt that = (Receipt) other;
            return Objects.equals(invoiceId, that.invoiceId)
                    && Objects.equals(invoiceCode, that.invoiceCode)
                    && Objects.equals(subtotal, that.subtotal)
                    && Objects.equals(discount, that.discount)
                    && Objects.equals(total, that.total)
                    && Objects.equals(change, that.change);
        }

        @Override
        public int hashCode() {
            return Objects.hash(invoiceId, invoiceCode, subtotal, discount, total, change);
        }

        @Override
        public String toString() {
            return "Receipt[invoiceId=" + invoiceId
                    + ", invoiceCode=" + invoiceCode
                    + ", subtotal=" + subtotal
                    + ", discount=" + discount
                    + ", total=" + total
                    + ", change=" + change
                    + "]";
        }
    }
}
