package com.smashstep.datn.sales.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.payment.entity.PhuongThucThanhToan;
import com.smashstep.datn.sales.dto.SalesRequest;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.*;

/** Amount is money retained by the store; tendered cash is separate and may include change. */
public record PaymentPlan(List<Part> cacKhoanThu, BigDecimal tienDaNhan, BigDecimal tienThua) {
    public record Part(PhuongThucThanhToan phuongThuc, BigDecimal soTien) {}

    public static PaymentPlan lapKeHoach(
            EntityManager entityManager, SalesRequest r, BigDecimal thanhTien) {
        if (r.getDanhSachThanhToan() == null
                || r.getDanhSachThanhToan().isEmpty()
                || r.getDanhSachThanhToan().size() > 2)
            throw AppException.badRequest("Cần 1 hoặc 2 khoản thanh toán");
        Set<Long> ids = new HashSet<>();
        List<Part> cacKhoanThu = new ArrayList<>();
        BigDecimal tongTien = BigDecimal.ZERO,
                tienMat = BigDecimal.ZERO,
                tienChuyenKhoan = BigDecimal.ZERO;
        for (var duLieuNhap : r.getDanhSachThanhToan()) {
            if (duLieuNhap == null
                    || duLieuNhap.getIdPhuongThucThanhToan() == null
                    || !ids.add(duLieuNhap.getIdPhuongThucThanhToan()))
                throw AppException.badRequest("Phương thức thanh toán bị thiếu hoặc trùng");
            var phuongThuc =
                    entityManager.find(
                            PhuongThucThanhToan.class, duLieuNhap.getIdPhuongThucThanhToan());
            if (phuongThuc == null
                    || !Integer.valueOf(1).equals(phuongThuc.getTrangThai())
                    || !Set.of("TIEN_MAT", "CHUYEN_KHOAN").contains(phuongThuc.getMaPhuongThuc()))
                throw AppException.badRequest("Chỉ hỗ trợ tiền mặt và chuyển khoản đang hoạt động");
            var soTien = duLieuNhap.getSoTien();
            if (soTien == null
                    || soTien.signum() < 0
                    || soTien.scale() > 2
                    || soTien.precision() - soTien.scale() > 16
                    || (thanhTien.signum() > 0 && soTien.signum() == 0))
                throw AppException.badRequest(
                        "Số tiền mỗi khoản phải dương (trừ hóa đơn có tổng bằng 0)");
            if ("TIEN_MAT".equals(phuongThuc.getMaPhuongThuc())) tienMat = tienMat.add(soTien);
            else tienChuyenKhoan = tienChuyenKhoan.add(soTien);
            tongTien = tongTien.add(soTien);
            cacKhoanThu.add(new Part(phuongThuc, soTien));
        }
        if (tongTien.compareTo(thanhTien) != 0)
            throw AppException.badRequest("Tổng tiền thực thu phải bằng đúng số tiền cần trả");
        boolean coTienMat =
                cacKhoanThu.stream()
                        .anyMatch(p -> "TIEN_MAT".equals(p.phuongThuc().getMaPhuongThuc()));
        boolean coChuyenKhoan =
                cacKhoanThu.stream()
                        .anyMatch(p -> "CHUYEN_KHOAN".equals(p.phuongThuc().getMaPhuongThuc()));
        if (coChuyenKhoan && tienChuyenKhoan.signum() == 0)
            throw AppException.badRequest(
                    "Đơn 0 đồng cần ghi nhận bằng phương thức tiền mặt, không tạo chuyển khoản 0"
                            + " đồng");
        if (coChuyenKhoan && !Boolean.TRUE.equals(r.getDaXacNhanChuyenKhoan()))
            throw AppException.badRequest(
                    "Nhân viên phải xác nhận đã kiểm tra chuyển khoản thực tế");
        BigDecimal tienMatKhachDua = r.getTienKhachDua();
        if (tienMatKhachDua == null
                || tienMatKhachDua.signum() < 0
                || tienMatKhachDua.scale() > 2
                || tienMatKhachDua.precision() - tienMatKhachDua.scale() > 16)
            throw AppException.badRequest(
                    "Cần nhập số tiền mặt khách đưa hợp lệ (0 nếu chỉ chuyển khoản)");
        if ((!coTienMat && tienMatKhachDua.signum() != 0) || tienMatKhachDua.compareTo(tienMat) < 0)
            throw AppException.badRequest(
                    "Tiền mặt khách đưa chưa đủ hoặc không phù hợp phương thức");
        return new PaymentPlan(
                List.copyOf(cacKhoanThu),
                tienMatKhachDua.add(tienChuyenKhoan),
                tienMatKhachDua.subtract(tienMat));
    }
}
