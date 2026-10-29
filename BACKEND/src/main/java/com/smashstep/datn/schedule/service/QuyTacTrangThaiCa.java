package com.smashstep.datn.schedule.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.schedule.entity.LichLamViec;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

public final class QuyTacTrangThaiCa {
    private QuyTacTrangThaiCa(){}
    public static boolean choNhan(LichLamViec row){return Set.of(0,4).contains(Objects.requireNonNullElse(row.getTrangThai(),-1));}
    public static boolean hetHanNhan(LichLamViec row,LocalDateTime now){
        var range=QuyTacLichLamViec.khoang(row.getNgayLam(),row.getIdCaLamViec());
        return now.isAfter(range.batDau().plusMinutes(15)) || !now.isBefore(range.ketThuc());
    }
    public static boolean coTheNhan(LichLamViec row,LocalDateTime now){
        var range=QuyTacLichLamViec.khoang(row.getNgayLam(),row.getIdCaLamViec());
        return choNhan(row) && !now.isBefore(range.batDau().minusMinutes(15)) && !hetHanNhan(row,now);
    }
    public static void kiemTraDieuChinh(LichLamViec row,int next,LocalDateTime now,String reason){
        var current=row.getTrangThai();
        if(next<0 || next>4)throw AppException.badRequest("Trạng thái không hợp lệ (0-4)");
        if(Objects.equals(current,next))return; // Notes may still be corrected.
        if(current==null || Set.of(1,3).contains(current) || next==1 || next==3)
            throw AppException.conflict("Nhận/kết thúc ca phải thực hiện từ ca cá nhân; ca đã nhận hoặc đã kết thúc chỉ được sửa ghi chú");
        if(next!=2 && hetHanNhan(row,now))
            throw AppException.conflict("Đã hết hạn nhận ca; không thể chuyển lịch về chờ hoặc chưa nhận ca");
        if(current==2 && (reason==null || reason.isBlank()))
            throw AppException.badRequest("Cần nhập lý do điều chỉnh vắng mặt trước khi mở lại lịch");
    }
}
