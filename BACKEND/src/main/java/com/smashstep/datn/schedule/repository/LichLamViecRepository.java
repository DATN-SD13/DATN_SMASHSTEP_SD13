package com.smashstep.datn.schedule.repository;

import com.smashstep.datn.employee.entity.NhanVien;
import com.smashstep.datn.schedule.entity.LichLamViec;
import com.smashstep.datn.schedule.service.ScheduleAvailability;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class LichLamViecRepository {
    private final NamedParameterJdbcTemplate jdbc;
    private final ScheduleAvailability availability;
    private static final String FROM = " FROM dbo.lich_lam_viec l JOIN dbo.nhan_vien nv ON nv.id=l.id_nhan_vien JOIN dbo.ca_lam_viec ca ON ca.id=l.id_ca_lam_viec ";
    private static final String SELECT = "SELECT l.id AS lich_id,l.ngay_lam,l.trang_thai AS lich_trang_thai,l.ghi_chu,l.ngay_tao AS lich_ngay_tao,l.ngay_cap_nhat AS lich_ngay_cap_nhat,nv.id AS nv_id,nv.ma_nhan_vien,nv.ten_nhan_vien,nv.hinh_anh,ca.*";
    private static final String ORDER = " ORDER BY l.ngay_lam,ca.gio_bat_dau,l.id ";
    private static final RowMapper<LichLamViec> MAPPER = (r,i) -> {
        var l=new LichLamViec(); var nv=new NhanVien();
        nv.setId(r.getLong("nv_id")); nv.setMaNhanVien(r.getString("ma_nhan_vien")); nv.setTenNhanVien(r.getString("ten_nhan_vien")); nv.setHinhAnh(r.getString("hinh_anh"));
        l.setId(r.getLong("lich_id")); l.setIdNhanVien(nv); l.setIdCaLamViec(CaLamViecRepository.MAPPER.mapRow(r,i));
        var date=r.getDate("ngay_lam"); l.setNgayLam(date==null?null:date.toLocalDate());
        l.setTrangThai(r.getObject("lich_trang_thai",Integer.class)); l.setGhiChu(r.getString("ghi_chu"));
        var created=r.getTimestamp("lich_ngay_tao"); var updated=r.getTimestamp("lich_ngay_cap_nhat");
        l.setNgayTao(created==null?null:created.toLocalDateTime()); l.setNgayCapNhat(updated==null?null:updated.toLocalDateTime());
        return l;
    };
    public Page<LichLamViec> search(LocalDate from,LocalDate to,Long shift,String keyword,Pageable page) {
        availability.requireAvailable();
        String where=" WHERE 1=1 "; var p=new MapSqlParameterSource();
        if(from!=null){where+=" AND l.ngay_lam>=:fromDate";p.addValue("fromDate",java.sql.Date.valueOf(from));}
        if(to!=null){where+=" AND l.ngay_lam<=:toDate";p.addValue("toDate",java.sql.Date.valueOf(to));}
        if(shift!=null){where+=" AND ca.id=:shift";p.addValue("shift",shift);}
        if(keyword!=null&&!keyword.isBlank()){
            where+=" AND (LOWER(nv.ma_nhan_vien) LIKE :keyword OR LOWER(nv.ten_nhan_vien) LIKE :keyword)";
            p.addValue("keyword","%"+keyword.trim().toLowerCase(Locale.ROOT)+"%");
        }
        Long count=jdbc.queryForObject("SELECT COUNT_BIG(*)"+FROM+where,p,Long.class);
        p.addValue("offset",page.getOffset()).addValue("size",page.getPageSize());
        return new PageImpl<>(jdbc.query(SELECT+FROM+where+ORDER+"OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY",p,MAPPER),page,count==null?0:count);
    }
    public List<LichLamViec> findByNgayLamBetween(LocalDate from,LocalDate to) {
        availability.requireAvailable();
        return jdbc.query(SELECT+FROM+" WHERE l.ngay_lam>=:fromDate AND l.ngay_lam<=:toDate"+ORDER,
            new MapSqlParameterSource("fromDate",java.sql.Date.valueOf(from)).addValue("toDate",java.sql.Date.valueOf(to)),MAPPER);
    }
    public Optional<LichLamViec> findById(Long id) {
        availability.requireAvailable();return jdbc.query(SELECT+FROM+" WHERE l.id=:id",Map.of("id",id),MAPPER).stream().findFirst();
    }
    public boolean existsByIdNhanVien_MaNhanVienAndIdCaLamViec_IdAndNgayLam(String employee,Long shift,LocalDate date) {
        availability.requireAvailable();
        Long count=jdbc.queryForObject("SELECT COUNT_BIG(*)"+FROM+" WHERE nv.ma_nhan_vien=:employee AND ca.id=:shift AND l.ngay_lam=:date",
            new MapSqlParameterSource("employee",employee).addValue("shift",shift).addValue("date",java.sql.Date.valueOf(date)),Long.class);
        return count!=null&&count>0;
    }
    public LichLamViec save(LichLamViec l) {
        availability.requireAvailable();
        var p=new MapSqlParameterSource("employee",l.getIdNhanVien().getId()).addValue("shift",l.getIdCaLamViec().getId())
            .addValue("date",java.sql.Date.valueOf(l.getNgayLam())).addValue("status",l.getTrangThai()).addValue("notes",l.getGhiChu())
            .addValue("created",l.getNgayTao()).addValue("updated",l.getNgayCapNhat());
        if(l.getId()==null)l.setId(jdbc.queryForObject("INSERT INTO dbo.lich_lam_viec(id_nhan_vien,id_ca_lam_viec,ngay_lam,trang_thai,ghi_chu,ngay_tao,ngay_cap_nhat) OUTPUT INSERTED.id VALUES(:employee,:shift,:date,:status,:notes,:created,:updated)",p,Long.class));
        else jdbc.update("UPDATE dbo.lich_lam_viec SET trang_thai=:status,ghi_chu=:notes,ngay_cap_nhat=:updated WHERE id=:id",p.addValue("id",l.getId()));
        return l;
    }
    public void delete(LichLamViec l) {
        availability.requireAvailable();jdbc.update("DELETE FROM dbo.lich_lam_viec WHERE id=:id",Map.of("id",l.getId()));
    }
}
