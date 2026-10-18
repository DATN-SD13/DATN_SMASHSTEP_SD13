package com.smashstep.datn.invoice.rules;
import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.invoice.entity.*;
import java.math.BigDecimal;
import java.util.*;
public final class DeliveryFailureRules {
    private DeliveryFailureRules() {}
    public static long attempts(List<LichSuHoaDon> rows) {
        return rows.stream().filter(r -> Integer.valueOf(9).equals(r.getTrangThai())).count();
    }
    public static void validate(int from, int to, boolean delivery, long count, String reason) {
        if (!delivery || !(from==3 && to==9 || from==9 && to==3)) throw AppException.badRequest("Chuyển trạng thái giao hàng không hợp lệ");
        if (count>=3) throw AppException.conflict("Đã thất bại 3 lần; cần xử lý hoàn/hủy, không giao lại");
        if (to==9 && (reason==null || reason.isBlank() || reason.trim().length()>800))
            throw AppException.badRequest("Nhập lý do giao hàng thất bại (tối đa 800 ký tự)");
    }
    /** Unknown/pending records require reconciliation, never guess money from invoice total. */
    public static BigDecimal refundDue(HoaDon h, List<LichSuThanhToan> rows) {
        if (rows.stream().anyMatch(r -> !Integer.valueOf(1).equals(r.getTrangThai()) && !Integer.valueOf(2).equals(r.getTrangThai())))
            throw AppException.conflict("Có khoản thanh toán chưa đối soát; chưa thể hoàn/hủy");
        if (rows.isEmpty() && h.getNgayThanhToan()!=null) throw AppException.conflict("Thiếu lịch sử thu tiền; cần đối soát trước khi hoàn/hủy");
        BigDecimal net=BigDecimal.ZERO;
        for(var r:rows) {
            if(r.getSoTien()==null || r.getSoTien().signum()<0) throw AppException.conflict("Số tiền lịch sử không hợp lệ");
            net=net.add(Integer.valueOf(1).equals(r.getTrangThai()) ? r.getSoTien() : r.getSoTien().negate());
        }
        if(net.signum()<0) throw AppException.conflict("Lịch sử hoàn tiền vượt khoản thu");
        return net;
    }
}
