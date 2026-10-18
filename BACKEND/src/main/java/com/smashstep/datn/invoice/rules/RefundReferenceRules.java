package com.smashstep.datn.invoice.rules;
import com.smashstep.datn.common.exception.AppException;
public final class RefundReferenceRules {
    private RefundReferenceRules() {}
    public static String normalize(String input) {
        if(input==null) throw AppException.badRequest("Nhập mã giao dịch hoặc số chứng từ hoàn tiền");
        if(input.codePoints().anyMatch(c -> Character.isISOControl(c)
                || Character.getType(c)==Character.FORMAT || Character.getType(c)==Character.LINE_SEPARATOR
                || Character.getType(c)==Character.PARAGRAPH_SEPARATOR))
            throw AppException.badRequest("Mã giao dịch/chứng từ không được chứa xuống dòng hoặc ký tự điều khiển");
        String normalized=input.replaceAll("^[\\p{Z}\\s]+|[\\p{Z}\\s]+$", "");
        if(normalized.isEmpty()) throw AppException.badRequest("Nhập mã giao dịch hoặc số chứng từ hoàn tiền");
        if(normalized.length()>100) throw AppException.badRequest("Mã giao dịch/chứng từ tối đa 100 ký tự");
        return normalized;
    }
}
