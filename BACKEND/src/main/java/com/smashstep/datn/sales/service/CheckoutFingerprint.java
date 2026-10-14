package com.smashstep.datn.sales.service;

import com.smashstep.datn.sales.dto.SalesRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Persisted in payment descriptions, so retries do not depend on current prices or voucher validity. */
public final class CheckoutFingerprint {
    private CheckoutFingerprint() {}
    public static String of(SalesRequest r) {
        StringBuilder b = new StringBuilder();
        for (Object v : Arrays.asList(r.getInvoiceId(), r.getRevision(), r.getEmployeeId(), r.getCustomerId(),
                Objects.toString(r.getNote(), ""), normalizeCode(r.getVoucherCode()), r.getPaidAmount(),
                Boolean.TRUE.equals(r.getTransferConfirmed()))) add(b,v);
        var delivery = r.getDelivery(); add(b,delivery != null && delivery.enabled());
        if (delivery != null && delivery.enabled()) for (Object v : Arrays.asList(delivery.addressId(), delivery.recipientName(),
                delivery.recipientPhone(), delivery.address(), delivery.shippingFee())) add(b,v);
        r.getItems().stream().sorted(Comparator.comparing(SalesRequest.Item::getVariantId)).forEach(i -> {
            add(b,i.getVariantId()); add(b,i.getQuantity()); add(b,i.getUnitPrice());
        });
        for (var p : r.getPayments()) { add(b,p.getMethodId()); add(b,p.getAmount()); }
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b.toString().getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    public static String normalizeCode(String s) { return s == null ? "" : s.trim().toLowerCase(Locale.ROOT); }
    private static void add(StringBuilder b, Object value) {
        String s = value instanceof BigDecimal m ? m.stripTrailingZeros().toPlainString() : Objects.toString(value, "");
        b.append(s.length()).append(':').append(s).append('|');
    }
}
