package com.smashstep.datn.schedule.service;

import com.smashstep.datn.common.config.DatabaseCapabilities;
import com.smashstep.datn.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.util.List;

/** Optional team scheduling storage is detected, never created or migrated. */
@Component
@RequiredArgsConstructor
public class ScheduleAvailability {
    public static final String MESSAGE = "Lịch làm việc chưa khả dụng trong cấu hình hiện tại.";
    private final DatabaseCapabilities database;
    public boolean available() {
        return List.of("id","ma_ca","ten_ca","gio_bat_dau","gio_ket_thuc","mo_ta","trang_thai","ngay_tao","ngay_cap_nhat").stream()
                    .allMatch(c -> database.hasColumn("ca_lam_viec",c))
            && List.of("id","id_nhan_vien","id_ca_lam_viec","ngay_lam","trang_thai","ghi_chu","ngay_tao","ngay_cap_nhat").stream()
                    .allMatch(c -> database.hasColumn("lich_lam_viec",c));
    }
    public void requireAvailable() {
        if (!available()) throw new AppException(HttpStatus.SERVICE_UNAVAILABLE,MESSAGE);
    }
}
