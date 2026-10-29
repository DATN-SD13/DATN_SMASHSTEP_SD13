package com.smashstep.datn.schedule.service;

import com.smashstep.datn.invoice.entity.LichSuHoaDon;
import com.smashstep.datn.invoice.entity.LichSuThanhToan;
import com.smashstep.datn.product.service.KhoaGhiSanPham;
import com.smashstep.datn.schedule.repository.LichLamViecRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Immutable allocation audit, committed in the same transaction as the receipt/refund. */
@Service
@RequiredArgsConstructor
public class ShiftPaymentAttribution {
    public static final String PREFIX = "[SHIFT-PAYMENT:1]|";
    private final ScheduleAvailability availability;
    private final LichLamViecRepository schedules;
    private final KhoaGhiSanPham lock;

    public void lockPayments() { lock.khoa("schedule"); }

    public void record(EntityManager em, LichSuThanhToan payment, Long employee) {
        if (employee == null) throw new IllegalArgumentException("Payment actor is required");
        lock.khoa("schedule");
        var time = payment.getThoiGian();
        Long shift = 0L;
        if (availability.available()) {
            var active = schedules.findForEmployee(employee, time.toLocalDate().minusDays(1), time.toLocalDate().plusDays(1))
                    .stream().filter(row -> Integer.valueOf(1).equals(row.getTrangThai()))
                    .filter(row -> time.isBefore(QuyTacLichLamViec.khoang(row.getNgayLam(), row.getIdCaLamViec()).ketThuc()))
                    .filter(row -> !time.isBefore(QuyTacLichLamViec.khoang(row.getNgayLam(), row.getIdCaLamViec()).batDau().minusMinutes(15)))
                    .toList();
            if (active.size() > 1) throw new IllegalStateException("More than one active allocation for payment actor");
            if (!active.isEmpty()) shift = active.get(0).getId();
        }
        em.flush();
        if (payment.getId() == null) throw new IllegalStateException("Persist the payment before recording its allocation");
        var audit = new LichSuHoaDon();
        audit.setIdHoaDon(payment.getIdHoaDon()); audit.setNguoiTao(employee);
        audit.setTrangThai(payment.getIdHoaDon().getTrangThai()); audit.setNgayTao(time);
        audit.setGhiChu(PREFIX + payment.getId() + "|" + shift + "|");
        em.persist(audit);
    }

    public static boolean technical(String note) { return note != null && note.startsWith(PREFIX); }
}
