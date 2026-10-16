package com.smashstep.datn.schedule.repository;

import com.smashstep.datn.schedule.entity.CaLamViec;
import com.smashstep.datn.schedule.service.ScheduleAvailability;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.*;

/** JDBC keeps optional scheduling tables out of the mandatory JPA entity model. */
@Repository
@RequiredArgsConstructor
public class CaLamViecRepository {
    private final JdbcTemplate jdbc;
    private final ScheduleAvailability availability;
    static final RowMapper<CaLamViec> MAPPER = (r,i) -> {
        var ca = new CaLamViec();
        ca.setId(r.getLong("id")); ca.setMaCa(r.getString("ma_ca")); ca.setTenCa(r.getString("ten_ca"));
        var start=r.getTime("gio_bat_dau"); var end=r.getTime("gio_ket_thuc");
        ca.setGioBatDau(start==null?null:start.toLocalTime()); ca.setGioKetThuc(end==null?null:end.toLocalTime());
        ca.setMoTa(r.getString("mo_ta")); ca.setTrangThai(r.getObject("trang_thai",Integer.class));
        var created=r.getTimestamp("ngay_tao"); var updated=r.getTimestamp("ngay_cap_nhat");
        ca.setNgayTao(created==null?null:created.toLocalDateTime()); ca.setNgayCapNhat(updated==null?null:updated.toLocalDateTime());
        return ca;
    };
    public List<CaLamViec> findAll() {
        availability.requireAvailable(); return jdbc.query("SELECT * FROM dbo.ca_lam_viec ORDER BY gio_bat_dau,id",MAPPER);
    }
    public List<CaLamViec> findByTrangThaiOrderByGioBatDauAsc(Integer status) {
        availability.requireAvailable(); return jdbc.query("SELECT * FROM dbo.ca_lam_viec WHERE trang_thai=? ORDER BY gio_bat_dau,id",MAPPER,status);
    }
    public Optional<CaLamViec> findById(Long id) {
        availability.requireAvailable(); return jdbc.query("SELECT * FROM dbo.ca_lam_viec WHERE id=?",MAPPER,id).stream().findFirst();
    }
}
