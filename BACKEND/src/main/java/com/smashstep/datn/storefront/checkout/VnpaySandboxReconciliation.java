package com.smashstep.datn.storefront.checkout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** Queries VNPAY itself when an IPN cannot reach localhost. Browser parameters never mark an order paid. */
@Service
public class VnpaySandboxReconciliation {
    private static final Logger LOG = LoggerFactory.getLogger(VnpaySandboxReconciliation.class);
    private static final URI ENDPOINT = URI.create("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction");
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuuMMddHHmmss")
            .withResolverStyle(java.time.format.ResolverStyle.STRICT);
    private static final List<String> REQUEST_FIELDS = List.of("vnp_RequestId", "vnp_Version", "vnp_Command",
            "vnp_TmnCode", "vnp_TxnRef", "vnp_TransactionDate", "vnp_CreateDate", "vnp_IpAddr", "vnp_OrderInfo");
    private static final List<String> RESPONSE_FIELDS = List.of("vnp_ResponseId", "vnp_Command", "vnp_ResponseCode",
            "vnp_Message", "vnp_TmnCode", "vnp_TxnRef", "vnp_Amount", "vnp_BankCode", "vnp_PayDate",
            "vnp_TransactionNo", "vnp_TransactionType", "vnp_TransactionStatus", "vnp_OrderInfo");
    private final VnpaySandboxPayment gateway;
    private final DemoCardPayment sessions;
    private final boolean enabled;
    private final String terminal, secret, serverIp;
    private final JsonMapper json = JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NEVER).version(HttpClient.Version.HTTP_1_1).build();
    private final Map<String, Instant> nextQuery = new HashMap<>();

    public VnpaySandboxReconciliation(VnpaySandboxPayment gateway, DemoCardPayment sessions,
            @Value("${app.payment.vnpay.query-enabled:true}") boolean enabled,
            @Value("${app.payment.vnpay.tmn-code:}") String terminal,
            @Value("${app.payment.vnpay.hash-secret:}") String secret,
            @Value("${app.payment.vnpay.query-ip:127.0.0.1}") String serverIp) {
        this.gateway = gateway; this.sessions = sessions; this.enabled = enabled;
        this.terminal = terminal.trim(); this.secret = secret.trim();
        this.serverIp = serverIp.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}") ? serverIp : "127.0.0.1";
    }

    public VnpaySandboxPayment.Result returned(Map<String, String[]> proof) {
        var checked = gateway.returned(proof);
        if (!checked.valid() || !pending(checked.status())) return checked;
        var snapshot = sessions.gatewaySnapshot(checked.code());
        reconcile(snapshot);
        return gateway.returned(proof);
    }

    public VnpaySandboxPayment.Result owned(Long owner, String code) {
        var snapshot = gateway.owned(owner, code);
        reconcile(snapshot);
        return gateway.ownedResult(owner, code);
    }

    private boolean pending(String status) { return "AWAITING_GATEWAY".equals(status) || "EXPIRED".equals(status); }

    private boolean claim(String code) {
        synchronized (nextQuery) {
            var now = Instant.now();
            nextQuery.values().removeIf(expiry -> expiry.isBefore(now.minus(Duration.ofHours(24))));
            if (now.isBefore(nextQuery.getOrDefault(code, Instant.MIN)) || nextQuery.size() >= 1000 && !nextQuery.containsKey(code))
                return false;
            nextQuery.put(code, now.plusSeconds(60));
            return true;
        }
    }

    private void reconcile(DemoCardPayment.View snapshot) {
        if (!enabled || !gateway.configured() || snapshot == null || !pending(snapshot.status()) || !claim(snapshot.code())) return;
        try {
            var parameters = new LinkedHashMap<String, String>();
            parameters.put("vnp_RequestId", UUID.randomUUID().toString().replace("-", ""));
            parameters.put("vnp_Version", "2.1.0"); parameters.put("vnp_Command", "querydr");
            parameters.put("vnp_TmnCode", terminal); parameters.put("vnp_TxnRef", snapshot.code());
            // QueryDr requires the original CreateDate, not the PayDate returned by the browser.
            parameters.put("vnp_TransactionDate", DATE.format(snapshot.createdAt().atZone(ZONE)));
            parameters.put("vnp_CreateDate", DATE.format(Instant.now().atZone(ZONE)));
            parameters.put("vnp_IpAddr", serverIp); parameters.put("vnp_OrderInfo", "SMASHSTEP SANDBOX " + snapshot.code());
            parameters.put("vnp_SecureHash", VnpaySandboxPayment.hmac(secret, joined(parameters, REQUEST_FIELDS)));
            var request = HttpRequest.newBuilder(ENDPOINT).timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json; charset=UTF-8").header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(parameters), StandardCharsets.UTF_8)).build();
            byte[] body;
            var response = VnpaySandboxHttp.send(client, request);
            try (var input = response.body()) { body = input.readNBytes(65537); }
            if (response.statusCode() != 200 || body.length > 65536) { rejected(snapshot.code(), "HTTP_RESPONSE"); return; }
            var data = json.readTree(body);
            var fields = responseFields(data);
            if (fields == null || !verified(fields)) { rejected(snapshot.code(), "RESPONSE_SIGNATURE"); return; }
            if (!"00".equals(fields.get("vnp_ResponseCode"))) { rejected(snapshot.code(), "QUERY_" + fields.get("vnp_ResponseCode")); return; }
            if (!terminal.equals(fields.get("vnp_TmnCode")) || !snapshot.code().equals(fields.get("vnp_TxnRef"))
                    || !"querydr".equals(fields.get("vnp_Command")) || !"01".equals(fields.get("vnp_TransactionType"))
                    || !Objects.toString(fields.get("vnp_Amount"), "").matches("[0-9]{1,12}")) {
                rejected(snapshot.code(), "TRANSACTION_IDENTITY"); return;
            }
            var amount = new java.math.BigDecimal(fields.get("vnp_Amount")).movePointLeft(2);
            if (amount.compareTo(snapshot.amount()) != 0) { rejected(snapshot.code(), "AMOUNT"); return; }
            // Query success (ResponseCode=00) alone does not mean the payment succeeded.
            if (!"00".equals(fields.get("vnp_TransactionStatus"))) return;
            if (!Objects.toString(fields.get("vnp_TransactionNo"), "").matches("[0-9]{1,30}")) return;
            var paidAt = LocalDateTime.parse(fields.get("vnp_PayDate"), DATE).atZone(ZONE).toInstant();
            if (paidAt.isAfter(Instant.now().plusSeconds(60))) return;
            sessions.confirmGateway(snapshot.code(), amount, true, "VNPAY-SANDBOX-" + fields.get("vnp_TransactionNo"), paidAt, "QUERYDR");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); rejected(snapshot.code(), "INTERRUPTED");
        } catch (com.smashstep.datn.common.exception.AppException e) {
            // A storage failure must remain visible; never show success before the receipt is saved.
            throw e;
        } catch (Exception e) { rejected(snapshot.code(), "CONNECTION_OR_PAYLOAD"); }
    }

    private Map<String, String> responseFields(JsonNode data) {
        if (data == null || !data.isObject() || data.size() > 30) return null;
        var fields = new HashMap<String, String>();
        for (var name : RESPONSE_FIELDS) {
            var value = data.get(name);
            if (value != null && !value.isNull() && !value.isTextual()) return null;
            fields.put(name, value == null || value.isNull() ? "" : value.asText());
        }
        for (var name : List.of("vnp_PromotionCode", "vnp_PromotionAmount", "vnp_SecureHash")) {
            var value = data.get(name);
            if (value != null && !value.isNull() && !value.isTextual()) return null;
            if (value != null && !value.isNull()) fields.put(name, value.asText());
        }
        if (fields.values().stream().anyMatch(value -> value.length() > 1000)) return null;
        return fields;
    }

    private boolean verified(Map<String, String> fields) {
        String hash = fields.get("vnp_SecureHash");
        if (hash == null || !hash.matches("[0-9a-fA-F]{128}")) return false;
        String base = joined(fields, RESPONSE_FIELDS);
        String withPromotion = base + "|" + fields.getOrDefault("vnp_PromotionCode", "") + "|" + fields.getOrDefault("vnp_PromotionAmount", "");
        if (equalHash(hash, VnpaySandboxPayment.hmac(secret, withPromotion))) return true;
        // Older PAY 2.1.0 responses omit both promotion fields from the checksum.
        return !fields.containsKey("vnp_PromotionCode") && !fields.containsKey("vnp_PromotionAmount")
                && equalHash(hash, VnpaySandboxPayment.hmac(secret, base));
    }
    private boolean equalHash(String actual, String expected) {
        return MessageDigest.isEqual(actual.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII), expected.getBytes(StandardCharsets.US_ASCII));
    }
    private String joined(Map<String, String> fields, List<String> names) {
        return names.stream().map(name -> fields.getOrDefault(name, "")).collect(Collectors.joining("|"));
    }
    private void rejected(String code, String reason) { LOG.warn("VNPAY Sandbox QueryDr could not confirm {}: {}", code, reason); }
}
