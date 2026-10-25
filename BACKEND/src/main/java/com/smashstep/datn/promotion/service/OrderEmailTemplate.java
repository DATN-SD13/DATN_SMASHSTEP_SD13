package com.smashstep.datn.promotion.service;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/** HTML and plain-text order receipts, rendered without sending email. */
public final class OrderEmailTemplate {
    private OrderEmailTemplate() {}
    public record Content(String html, String text) {}

    public static Content render(EmailService.OrderEmail order, boolean update) {
        String title = update ? "Đơn hàng vừa được cập nhật" : "Cảm ơn bạn đã đặt hàng!";
        String greeting = update ? "SmashStep gửi bạn thông tin mới nhất về đơn hàng." : "SmashStep đã nhận được đơn hàng của bạn. Dưới đây là thông tin chi tiết.";
        String preheader = (update ? "Cập nhật đơn " : "Xác nhận đơn ") + order.code() + " · " + order.status() + " · " + order.total();
        String payment = order.paid() ? "Đã ghi nhận thanh toán" : "Thanh toán khi nhận hàng";
        if (!order.paid() && !String.valueOf(order.method()).toLowerCase(Locale.ROOT).matches(".*(cod|khi nhận).*")) payment = "Chưa ghi nhận thanh toán";
        String rows = productRows(order.lines());
        String orderLink = safeUrl(order.url()) ? """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"><tr><td align="center" style="padding:26px 0 8px">
                  <table role="presentation" cellpadding="0" cellspacing="0"><tr><td bgcolor="#1677e8" style="border-radius:9px"><a href="%s" style="display:inline-block;padding:14px 30px;font-size:14px;font-weight:bold;color:#ffffff;text-decoration:none;border-radius:9px">Xem chi tiết đơn hàng &rarr;</a></td></tr></table>
                </td></tr></table>
                """.formatted(escape(order.url())) : "";
        String html = """
                <!doctype html>
                <html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="color-scheme" content="light"><title>%s</title>
                <style>@media only screen and (max-width:620px){.mail-shell{width:100%%!important}.mail-pad{padding-left:20px!important;padding-right:20px!important}.mail-title{font-size:24px!important}.mail-price{font-size:22px!important}.product-name{font-size:13px!important}}</style></head>
                <body style="margin:0;padding:0;background:#eef3f9;color:#18324f;font-family:Arial,Helvetica,sans-serif;-webkit-text-size-adjust:100%%">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;mso-hide:all">%s</div>
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" bgcolor="#eef3f9"><tr><td align="center" style="padding:30px 12px">
                <table role="presentation" class="mail-shell" width="600" cellpadding="0" cellspacing="0" style="width:100%%;max-width:600px;background:#ffffff;border:1px solid #dce5ef;border-radius:18px;overflow:hidden">
                  <tr><td class="mail-pad" bgcolor="#102640" style="padding:28px 32px;border-bottom:4px solid #39c5e7">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"><tr><td><div style="color:#ffffff;font-size:26px;font-weight:900;letter-spacing:1px">SMASH<span style="color:#54d8f4">STEP</span></div><div style="margin-top:7px;color:#abc9e5;font-size:10px;letter-spacing:2px">MOVE YOUR WAY</div></td><td align="right" style="color:#b9d6ef;font-size:11px;letter-spacing:1px">ĐƠN HÀNG<br><span style="display:inline-block;margin-top:7px;color:#ffffff;font-size:14px;font-weight:bold;letter-spacing:0">#%s</span></td></tr></table>
                  </td></tr>
                  <tr><td class="mail-pad" style="padding:30px 32px 18px">
                    <p style="margin:0 0 11px;font-size:11px;font-weight:bold;letter-spacing:2px;color:#1686af">%s</p>
                    <h1 class="mail-title" style="margin:0 0 18px;font-size:29px;line-height:1.3;letter-spacing:-0.6px;color:#102640">%s</h1>
                    <p style="margin:0;font-size:14px;line-height:1.8;color:#64758a">Xin chào <strong style="color:#18324f">%s</strong>,<br>%s</p>
                  </td></tr>
                  <tr><td class="mail-pad" style="padding:0 32px 24px">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" bgcolor="#f0f7ff" style="border:1px solid #d7e9fd;border-radius:12px"><tr><td style="padding:18px 20px"><p style="margin:0 0 7px;font-size:11px;letter-spacing:1px;color:#657e99">TRẠNG THÁI ĐƠN HÀNG</p><strong style="font-size:17px;color:#1266c4">%s</strong><p style="margin:8px 0 0;font-size:12px;line-height:1.6;color:#60758b">%s</p></td></tr></table>
                  </td></tr>
                  <tr><td class="mail-pad" style="padding:0 32px 24px">
                    <h2 style="margin:0 0 7px;font-size:16px;color:#18324f">Sản phẩm trong đơn</h2>
                    <table width="100%%" cellpadding="0" cellspacing="0" style="width:100%%;border-collapse:collapse"><thead><tr><th scope="col" align="left" style="padding:11px 0;border-bottom:1px solid #dfe8f1;font-size:11px;font-weight:normal;color:#7a8b9e">SẢN PHẨM</th><th scope="col" align="right" style="padding:11px 0;border-bottom:1px solid #dfe8f1;font-size:11px;font-weight:normal;color:#7a8b9e">THÀNH TIỀN</th></tr></thead><tbody>%s</tbody></table>
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-top:16px;font-size:13px;line-height:1.7">
                      <tr><td style="padding:5px 0;color:#708198">Tiền hàng</td><td align="right" style="padding:5px 0;font-weight:bold">%s</td></tr>
                      <tr><td style="padding:5px 0;color:#708198">Giảm phiếu giảm giá</td><td align="right" style="padding:5px 0;color:#168456;font-weight:bold">&minus;%s</td></tr>
                      <tr><td style="padding:5px 0 17px;color:#708198">Phí vận chuyển</td><td align="right" style="padding:5px 0 17px;font-weight:bold">%s</td></tr>
                      <tr><td style="padding:17px 0 0;border-top:1px solid #dfe8f1;font-size:15px;font-weight:bold;color:#1266c4">Tổng thanh toán</td><td class="mail-price" align="right" style="padding:17px 0 0;border-top:1px solid #dfe8f1;font-size:26px;line-height:1.3;font-weight:bold;white-space:nowrap;color:#1266c4">%s</td></tr>
                    </table>
                  </td></tr>
                  <tr><td class="mail-pad" style="padding:0 32px 4px">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" bgcolor="#f7f9fc" style="border:1px solid #e6edf5;border-radius:12px"><tr><td style="padding:20px;font-size:13px;line-height:1.8">
                      <h2 style="margin:0 0 10px;font-size:15px;color:#18324f">Thông tin nhận hàng</h2><strong>%s</strong><br><span style="color:#64758a">%s</span><br><span style="color:#64758a">%s</span>
                      <p style="margin:13px 0 0;padding-top:12px;border-top:1px solid #e1e8f0;color:#64758a"><strong style="color:#18324f">Phương thức thanh toán</strong><br>%s</p>
                    </td></tr></table>
                    %s
                  </td></tr>
                  <tr><td class="mail-pad" style="padding:18px 32px 28px"><p style="margin:0;font-size:12px;line-height:1.8;color:#7b8ba0">Bạn có thể xem tiến trình và thông tin nhận hàng tại mục <strong>Đơn hàng của bạn</strong> trên website. Nếu cần hỗ trợ, hãy liên hệ SmashStep qua mục chat.</p></td></tr>
                  <tr><td class="mail-pad" bgcolor="#f7f9fc" style="padding:20px 32px;border-top:1px solid #e5edf5;text-align:center"><strong style="font-size:13px;color:#18324f">Cảm ơn bạn đã chọn SmashStep.</strong><p style="margin:7px 0 0;font-size:11px;line-height:1.8;color:#8b99aa">Email tự động về đơn hàng của bạn.<br>SmashStep không yêu cầu mật khẩu hoặc mã OTP qua email.</p></td></tr>
                </table>
                <p style="margin:18px 0 0;color:#92a1b4;font-size:10px;letter-spacing:1px">SMASHSTEP &middot; EVERY STEP, YOUR STYLE</p>
                </td></tr></table></body></html>
                """.formatted(escape(title),escape(preheader),escape(order.code()),update?"THÔNG TIN MỚI NHẤT":"ĐẶT HÀNG THÀNH CÔNG",escape(title),escape(fallback(order.customer(),"bạn")),escape(greeting),escape(order.status()),escape(payment),rows,
                escape(order.subtotal()),escape(order.discount()),escape(order.shipping()),escape(order.total()),escape(order.receiver()),escape(order.phone()),escape(order.address()),escape(fallback(order.method(),"Theo thông tin đơn hàng")),orderLink);
        StringBuilder text = new StringBuilder("SMASHSTEP\n").append(title).append("\nĐơn hàng #").append(order.code()).append("\nTrạng thái: ").append(order.status()).append("\n").append(payment).append("\n\nSản phẩm:\n");
        for (var line : order.lines() == null ? List.<EmailService.OrderEmailLine>of() : order.lines()) text.append(line.name()).append(" · ").append(line.variant()).append(" · SL ").append(line.quantity()).append(" · ").append(line.total()).append('\n');
        text.append("\nTiền hàng: ").append(order.subtotal()).append("\nGiảm phiếu giảm giá: -").append(order.discount()).append("\nPhí vận chuyển: ").append(order.shipping()).append("\nTổng thanh toán: ").append(order.total())
                .append("\n\nNgười nhận: ").append(order.receiver()).append("\nĐiện thoại: ").append(order.phone()).append("\nĐịa chỉ: ").append(order.address()).append("\nThanh toán: ").append(order.method());
        if (safeUrl(order.url())) text.append("\nXem đơn hàng: ").append(order.url());
        text.append("\n\nEmail tự động từ SmashStep. Không chia sẻ mật khẩu hoặc OTP.");
        return new Content(html,text.toString());
    }

    private static String productRows(List<EmailService.OrderEmailLine> lines) {
        var html = new StringBuilder();
        for (var line : lines == null ? List.<EmailService.OrderEmailLine>of() : lines) html.append("""
                <tr><td style="padding:16px 10px 16px 0;border-bottom:1px solid #edf1f7"><strong class="product-name" style="font-size:14px;line-height:1.5;color:#18324f">%s</strong><br><span style="font-size:12px;line-height:1.8;color:#7b8ba0">%s &middot; SL %d</span><br><span style="font-size:11px;line-height:1.8;color:#91a0b2">Đơn giá: %s</span></td><td align="right" style="padding:16px 0;border-bottom:1px solid #edf1f7;font-size:13px;font-weight:bold;white-space:nowrap;vertical-align:middle;color:#18324f">%s</td></tr>
                """.formatted(escape(line.name()),escape(line.variant()),line.quantity(),escape(line.unitPrice()),escape(line.total())));
        return html.toString();
    }
    private static String fallback(String value,String alternative) { return value == null || value.isBlank() ? alternative : value; }
    private static String escape(String value) { return value == null ? "" : value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;"); }
    private static boolean safeUrl(String value) {
        if (value == null || value.isBlank()) return false;
        try { var uri = URI.create(value); return SetHolder.SCHEMES.contains(uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT)) && uri.getHost() != null && uri.getRawUserInfo() == null; }
        catch (IllegalArgumentException ignored) { return false; }
    }
    private static final class SetHolder { static final java.util.Set<String> SCHEMES = java.util.Set.of("http","https"); }
}
