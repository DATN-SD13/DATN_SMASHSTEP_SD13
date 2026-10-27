package com.smashstep.datn.promotion.service;

import java.net.URI;
import java.util.List;
import java.util.Locale;

/** Table-based email with inline styles for Gmail, mobile and desktop clients. */
public final class OrderEmailTemplate {
    private OrderEmailTemplate() {}
    public record Content(String html,String text) {}
    public static Content render(EmailService.OrderEmail order,boolean update){
        String title=update?"Đơn hàng đã được xác nhận":"Cảm ơn bạn đã đặt hàng!";
        String payment=order.paid()?"Đã ghi nhận thanh toán":
                String.valueOf(order.method()).toLowerCase(Locale.ROOT).matches(".*(cod|khi nhận).*")?"Thanh toán khi nhận hàng":"Chưa ghi nhận thanh toán";
        String discount=order.voucherApplied()?"""
                <tr><td style="padding:7px 0;color:#65758b">Giảm phiếu giảm giá</td><td align="right" style="padding:7px 0;color:#14835c;font-weight:bold">&minus;%s</td></tr>
                """.formatted(e(order.discount())):"";
        String shipping=OrderEmailImages.isGhn(order.shippingCarrier())
                ? "<img src=\"cid:ss-ghn\" width=\"54\" alt=\"GHN\" style=\"display:inline-block;width:54px;height:auto;vertical-align:middle;margin-right:7px\">Phí vận chuyển"
                : "Phí vận chuyển";
        String link=safeUrl(order.url())?"""
                <tr><td align="center" style="padding:4px 30px 26px"><a href="%s" style="display:inline-block;background:#087fbd;border-radius:8px;padding:14px 28px;color:#fff;font-size:14px;font-weight:bold;text-decoration:none">Xem chi tiết đơn hàng &rarr;</a></td></tr>
                """.formatted(e(order.url())):"";
        String html="""
                <!doctype html><html lang="vi"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>%s</title>
                <style>@media only screen and (max-width:620px){.mail-pad{padding-left:18px!important;padding-right:18px!important}.mail-title{font-size:23px!important}.item-image{width:58px!important;height:58px!important}.mail-total{font-size:23px!important}}</style></head>
                <body style="margin:0;padding:0;background:#edf3f7;font-family:Arial,Helvetica,sans-serif;color:#17324d;-webkit-text-size-adjust:100%%">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;mso-hide:all">Đơn hàng #%s · %s · %s</div>
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" bgcolor="#edf3f7"><tr><td align="center" style="padding:28px 10px">
                <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="width:100%%;max-width:600px;background:#fff;border:1px solid #dce7ef;border-radius:16px;overflow:hidden">
                <tr><td class="mail-pad" style="padding:22px 30px;border-top:5px solid #13a6cf;border-bottom:1px solid #e6edf3">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0"><tr><td width="78"><img src="cid:ss-brand" width="68" height="68" alt="SmashStep" style="display:block;width:68px;height:68px;object-fit:contain"></td><td><strong style="font-size:21px;letter-spacing:1px;color:#122c45">SMASHSTEP</strong><br><span style="font-size:9px;letter-spacing:2px;color:#7d91a3;line-height:2">SPORTS SHOES</span></td><td align="right" style="font-size:10px;line-height:1.8;color:#7b8fa2">MÃ ĐƠN HÀNG<br><strong style="font-size:13px;color:#087fbd">#%s</strong></td></tr></table>
                </td></tr>
                <tr><td class="mail-pad" style="padding:28px 30px 22px"><p style="margin:0 0 10px;font-size:10px;letter-spacing:2px;font-weight:bold;color:#078eaf">MỖI BƯỚC CHÂN, MỘT PHONG CÁCH</p>
                  <h1 class="mail-title" style="margin:0 0 14px;font-size:27px;line-height:1.3;color:#122c45">%s</h1>
                  <p style="margin:0;font-size:14px;line-height:1.8;color:#65758b">Xin chào <strong style="color:#17324d">%s</strong>,<br>%s</p>
                </td></tr>
                <tr><td class="mail-pad" style="padding:0 30px 26px"><table role="presentation" width="100%%" cellpadding="0" cellspacing="0" bgcolor="#eff8fc" style="border:1px solid #cfe9f5;border-radius:10px"><tr><td style="padding:17px 19px"><span style="font-size:10px;letter-spacing:1px;color:#678296">TRẠNG THÁI ĐƠN HÀNG</span><br><strong style="display:inline-block;padding-top:8px;font-size:17px;color:#087fbd">%s</strong><p style="margin:8px 0 0;font-size:12px;color:#65758b">%s</p></td></tr></table></td></tr>
                <tr><td class="mail-pad" style="padding:0 30px 22px"><h2 style="margin:0 0 8px;font-size:16px;color:#17324d">Sản phẩm trong đơn</h2>
                  <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse"><thead><tr><th align="left" style="padding:10px 0;font-size:10px;font-weight:normal;color:#8a9bad;border-bottom:1px solid #e1eaf1">SẢN PHẨM</th><th align="right" style="padding:10px 0;font-size:10px;font-weight:normal;color:#8a9bad;border-bottom:1px solid #e1eaf1">THÀNH TIỀN</th></tr></thead><tbody>%s</tbody></table>
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-top:16px;font-size:13px"><tr><td style="padding:7px 0;color:#65758b">Tiền hàng</td><td align="right" style="padding:7px 0;font-weight:bold">%s</td></tr>%s<tr><td style="padding:7px 0;color:#65758b">%s</td><td align="right" style="padding:7px 0;font-weight:bold">%s</td></tr></table>
                </td></tr>
                <tr><td class="mail-pad" style="padding:0 30px 24px"><table role="presentation" width="100%%" cellpadding="0" cellspacing="0" bgcolor="#12324c" style="border-radius:10px"><tr><td style="padding:19px 17px;font-size:13px;font-weight:bold;color:#e9f6ff">Tổng thanh toán</td><td class="mail-total" align="right" style="padding:19px 17px;font-size:26px;font-weight:bold;white-space:nowrap;color:#fff">%s</td></tr></table></td></tr>
                <tr><td class="mail-pad" style="padding:0 30px 24px"><table role="presentation" width="100%%" cellpadding="0" cellspacing="0" bgcolor="#f7f9fb" style="border:1px solid #e4ecf2;border-radius:10px"><tr><td style="padding:19px;font-size:13px;line-height:1.8"><h2 style="margin:0 0 9px;font-size:14px;color:#17324d">Thông tin nhận hàng</h2><strong>%s</strong><br><span style="color:#65758b">%s</span><br><span style="color:#65758b">%s</span><p style="margin:13px 0 0;padding-top:12px;border-top:1px solid #e0e8ef;color:#65758b"><strong style="color:#17324d">Phương thức thanh toán</strong><br>%s</p></td></tr></table></td></tr>
                %s
                <tr><td class="mail-pad" style="padding:0 30px 25px;font-size:12px;line-height:1.8;color:#7c8d9e">Cần hỗ trợ? Liên hệ SmashStep qua mục chat trên website và cung cấp mã đơn hàng <strong style="color:#17324d">#%s</strong>.</td></tr>
                <tr><td class="mail-pad" bgcolor="#f4f8fb" style="padding:20px 30px;text-align:center;border-top:1px solid #e2ebf2"><strong style="font-size:13px;color:#17324d">Cảm ơn bạn đã chọn SmashStep.</strong><p style="margin:8px 0 0;font-size:11px;line-height:1.8;color:#8798a9">Email tự động về đơn hàng của bạn.<br>SmashStep không yêu cầu mật khẩu hoặc mã OTP qua email.</p></td></tr>
                </table></td></tr></table></body></html>
                """.formatted(e(title),e(order.code()),e(order.status()),e(order.total()),e(order.code()),e(title),e(fallback(order.customer(),"bạn")),
                update?"Đơn hàng của bạn đã được xác nhận. SmashStep đang chuẩn bị những đôi giày bạn đã chọn.":"SmashStep đã nhận đơn hàng. Bạn có thể kiểm tra thông tin bên dưới.",
                e(order.status()),e(payment),productRows(order.lines()),e(order.subtotal()),discount,shipping,e(order.shipping()),e(order.total()),
                e(order.receiver()),e(order.phone()),e(order.address()),e(fallback(order.method(),"Theo thông tin đơn hàng")),link,e(order.code()));
        var text=new StringBuilder("SMASHSTEP\n").append(title).append("\nĐơn hàng #").append(order.code()).append("\nTrạng thái: ").append(order.status()).append("\n").append(payment).append("\n\nSản phẩm:\n");
        for(var line:order.lines()==null?List.<EmailService.OrderEmailLine>of():order.lines())text.append(line.name()).append(" · ").append(line.variant()).append(" · SL ").append(line.quantity()).append(" · ").append(line.total()).append('\n');
        text.append("\nTiền hàng: ").append(order.subtotal());
        if(order.voucherApplied())text.append("\nGiảm phiếu giảm giá: -").append(order.discount());
        text.append("\nPhí vận chuyển: ").append(order.shipping()).append("\nTổng thanh toán: ").append(order.total()).append("\n\nNgười nhận: ").append(order.receiver()).append("\nĐiện thoại: ").append(order.phone()).append("\nĐịa chỉ: ").append(order.address()).append("\nThanh toán: ").append(order.method());
        if(safeUrl(order.url()))text.append("\nXem đơn hàng: ").append(order.url());
        text.append("\n\nEmail tự động từ SmashStep. Không chia sẻ mật khẩu hoặc OTP.");
        return new Content(html,text.toString());
    }
    private static String productRows(List<EmailService.OrderEmailLine> lines){
        var result=new StringBuilder();
        for(var line:lines==null?List.<EmailService.OrderEmailLine>of():lines){
            String image=line.image()!=null && line.image().matches("cid:ss-item-[0-9]+")
                    ? "<img class=\"item-image\" src=\""+e(line.image())+"\" width=\"68\" height=\"68\" alt=\""+e(line.name())+"\" style=\"display:block;width:68px;height:68px;object-fit:contain;border:1px solid #e5edf3;border-radius:8px;background:#fff\">"
                    : "<div style=\"width:68px;height:68px;line-height:68px;text-align:center;font-size:9px;color:#9babb9;background:#f1f5f8;border-radius:8px\">Chưa có ảnh</div>";
            result.append("""
                    <tr><td style="padding:15px 8px 15px 0;border-bottom:1px solid #e8eff4"><table role="presentation" cellpadding="0" cellspacing="0"><tr><td style="padding-right:12px;vertical-align:top">%s</td><td style="vertical-align:middle"><strong style="font-size:13px;line-height:1.5;color:#17324d">%s</strong><br><span style="font-size:11px;line-height:1.8;color:#75889c">%s</span><br><span style="font-size:11px;line-height:1.8;color:#75889c">Số lượng: %d</span><br><span style="font-size:10px;line-height:1.8;color:#8b9dad">Đơn giá: %s</span></td></tr></table></td><td align="right" style="padding:15px 0;border-bottom:1px solid #e8eff4;vertical-align:middle;font-size:12px;font-weight:bold;white-space:nowrap;color:#17324d">%s</td></tr>
                    """.formatted(image,e(line.name()),e(line.variant()),line.quantity(),e(line.unitPrice()),e(line.total())));
        }
        return result.toString();
    }
    private static String fallback(String value,String other){return value==null||value.isBlank()?other:value;}
    private static String e(String value){return value==null?"":value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");}
    private static boolean safeUrl(String value){
        if(value==null||value.isBlank())return false;
        try{var uri=URI.create(value);return ("http".equalsIgnoreCase(uri.getScheme())||"https".equalsIgnoreCase(uri.getScheme()))&&uri.getHost()!=null&&uri.getRawUserInfo()==null;}
        catch(IllegalArgumentException ignored){return false;}
    }
}
