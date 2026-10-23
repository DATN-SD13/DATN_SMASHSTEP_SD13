package com.smashstep.datn.storefront.checkout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Replays durable confirmed receipts after a crash or upgrade; the invoice marker makes replay idempotent. */
@Component
public class VnpayOrderRecovery {
    private static final Logger LOG = LoggerFactory.getLogger(VnpayOrderRecovery.class);
    private final DemoPaymentLedger ledger;
    private final VnpayOrderService orders;
    private final boolean enabled;
    public VnpayOrderRecovery(DemoPaymentLedger ledger, VnpayOrderService orders,
            @Value("${app.payment.vnpay.enabled:false}") boolean enabled) {
        this.ledger = ledger; this.orders = orders; this.enabled = enabled;
    }
    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        if (!enabled) return;
        for (var payment : ledger.paidOrders()) if ("VNPAY_SANDBOX".equals(payment.channel())) {
            try { LOG.info("VNPAY receipt {} is linked to invoice {}", payment.code(), orders.post(payment.code()).code()); }
            catch (Exception e) { LOG.error("Cannot publish confirmed VNPAY receipt {}: {}", payment.code(), e.getMessage()); }
        }
    }
}
