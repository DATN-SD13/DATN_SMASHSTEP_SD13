package com.smashstep.datn.schedule.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.schedule.entity.*;
import java.time.*;

public final class QuyTacLichLamViec {
    private QuyTacLichLamViec() {}
    public record KhoangGio(LocalDateTime batDau, LocalDateTime ketThuc) {
        public boolean chong(KhoangGio other) {
            return batDau.isBefore(other.ketThuc) && other.batDau.isBefore(ketThuc);
        }
    }
    public static KhoangGio khoang(LocalDate date, CaLamViec shift) {
        if(date==null || shift==null || shift.getGioBatDau()==null || shift.getGioKetThuc()==null
                || shift.getGioBatDau().equals(shift.getGioKetThuc()))
            throw AppException.badRequest("Ngày hoặc giờ ca làm việc không hợp lệ; giờ bắt đầu không được bằng giờ kết thúc");
        try {
            var start=date.atTime(shift.getGioBatDau());
            var end=date.atTime(shift.getGioKetThuc());
            if(end.isBefore(start))end=end.plusDays(1);
            return new KhoangGio(start,end);
        } catch(DateTimeException e) {throw AppException.badRequest("Ngày làm ngoài phạm vi hỗ trợ");}
    }
    public static boolean chong(LocalDate date,CaLamViec shift,LichLamViec existing) {
        return khoang(date,shift).chong(khoang(existing.getNgayLam(),existing.getIdCaLamViec()));
    }
    public static void khoangNgay(LocalDate from,LocalDate to) {
        if(from==null || to==null || to.isBefore(from)
                || java.time.temporal.ChronoUnit.DAYS.between(from,to)>30)
            throw AppException.badRequest("Khoảng ngày phải hợp lệ và tối đa 31 ngày");
    }
}
