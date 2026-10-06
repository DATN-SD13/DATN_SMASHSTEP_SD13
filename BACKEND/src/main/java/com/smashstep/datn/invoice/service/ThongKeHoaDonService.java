package com.smashstep.datn.invoice.service;

import com.smashstep.datn.common.exception.AppException;
import com.smashstep.datn.invoice.dto.ThongKeHoaDonDto;
import com.smashstep.datn.invoice.entity.HoaDon;
import com.smashstep.datn.invoice.enums.LoaiHoaDon;
import com.smashstep.datn.invoice.enums.TrangThaiHoaDon;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ThongKeHoaDonService {
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public ThongKeHoaDonDto tongHop(LocalDate tuNgay, LocalDate denNgay, String ma, String loaiDon) {
        if (tuNgay != null && denNgay != null && tuNgay.isAfter(denNgay)) {
            throw AppException.badRequest("Từ ngày không được sau đến ngày");
        }
        LoaiHoaDon type = LoaiHoaDon.phanTich(loaiDon);
        var cb = entityManager.getCriteriaBuilder();
        var query = cb.createQuery(Object[].class);
        var invoice = query.from(HoaDon.class);
        var paidCompleted = cb.and(cb.equal(invoice.get("trangThai"), TrangThaiHoaDon.HOAN_THANH.getMa()),
                cb.isNotNull(invoice.get("ngayThanhToan")));
        var revenue = cb.<BigDecimal>selectCase().when(paidCompleted,
                cb.coalesce(invoice.<BigDecimal>get("thanhTien"), BigDecimal.ZERO)).otherwise(BigDecimal.ZERO);
        var paidCount = cb.<Long>selectCase().when(paidCompleted, 1L).otherwise(0L);
        List<Predicate> filters = new ArrayList<>();
        if (tuNgay != null) filters.add(cb.greaterThanOrEqualTo(invoice.get("ngayTao"), tuNgay.atStartOfDay()));
        if (denNgay != null) filters.add(cb.lessThan(invoice.get("ngayTao"), denNgay.plusDays(1).atStartOfDay()));
        if (ma != null && !ma.isBlank()) {
            filters.add(cb.like(cb.lower(invoice.get("maHoaDon")), "%" + ma.trim().toLowerCase(java.util.Locale.ROOT) + "%"));
        }
        if (type != null) filters.add(cb.equal(invoice.get("loaiHoaDon"), type.getMa()));
        query.multiselect(invoice.get("trangThai"), invoice.get("loaiHoaDon"), cb.count(invoice),
                cb.sum(revenue), cb.sum(paidCount));
        query.where(filters.toArray(Predicate[]::new));
        query.groupBy(invoice.get("trangThai"), invoice.get("loaiHoaDon"));
        return fromRows(entityManager.createQuery(query).getResultList());
    }

    static ThongKeHoaDonDto fromRows(List<Object[]> rows) {
        Map<Integer, Long> statuses = new LinkedHashMap<>();
        Map<Integer, Long> types = new LinkedHashMap<>();
        for (TrangThaiHoaDon status : TrangThaiHoaDon.values()) statuses.put(status.getMa(), 0L);
        for (LoaiHoaDon type : LoaiHoaDon.values()) types.put(type.getMa(), 0L);
        long total = 0;
        long paid = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        for (Object[] row : rows) {
            Integer status = row[0] == null ? null : ((Number) row[0]).intValue();
            Integer type = row[1] == null ? null : ((Number) row[1]).intValue();
            long count = ((Number) row[2]).longValue();
            statuses.merge(status, count, Long::sum);
            types.merge(type, count, Long::sum);
            total += count;
            if (row[3] != null) revenue = revenue.add((BigDecimal) row[3]);
            if (row[4] != null) paid += ((Number) row[4]).longValue();
        }
        List<ThongKeHoaDonDto.TrangThai> statusRows = statuses.entrySet().stream().map(entry -> {
            TrangThaiHoaDon status = TrangThaiHoaDon.tuMa(entry.getKey());
            return new ThongKeHoaDonDto.TrangThai(entry.getKey(), status == null ? "Không xác định" : status.getNhan(),
                    status == null ? "unknown" : status.getLop(), entry.getValue());
        }).toList();
        List<ThongKeHoaDonDto.LoaiDon> typeRows = types.entrySet().stream().map(entry -> {
            LoaiHoaDon type = LoaiHoaDon.tuMa(entry.getKey());
            return new ThongKeHoaDonDto.LoaiDon(entry.getKey(), type == null ? "Không xác định" : type.getNhan(),
                    type == null ? "unknown" : type.getKhoa(), entry.getValue());
        }).toList();
        long processing = statuses.entrySet().stream().filter(entry -> entry.getKey() != null
                && (entry.getKey() <= 4 && entry.getKey() >= 0 || entry.getKey() == 8)).mapToLong(Map.Entry::getValue).sum();
        return new ThongKeHoaDonDto(total, statuses.get(5), statuses.get(6), processing, paid, revenue, statusRows, typeRows);
    }
}
